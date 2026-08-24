# Deployment runbook — smartdental-backend (AWS EC2)

This runbook provisions a single Ubuntu LTS EC2 instance running the backend + PostgreSQL via Docker Compose,
fronted optionally by Nginx + Let's Encrypt. It assumes the paired `smartdental-frontend` is deployed separately
(same instance or another), with `FRONTEND_BASE_URL` / `CORS_ALLOWED_ORIGINS` pointed at it.

## 1. AWS infrastructure setup

1. **Launch an EC2 instance**
   - AMI: Ubuntu Server 22.04/24.04 LTS
   - Instance type: `t3.small` or larger (2 vCPU / 2GB+ RAM recommended once Postgres + JVM are both running)
   - Storage: 20GB+ gp3
2. **Allocate and associate an Elastic IP** so the DNS record and OAuth2/SES config don't break on instance restart.
3. **Security group** — inbound rules:

   | Port | Purpose | Source |
   |---|---|---|
   | 22 | SSH | Your IP / bastion only |
   | 80 | HTTP (Certbot challenge + redirect) | 0.0.0.0/0 |
   | 443 | HTTPS | 0.0.0.0/0 |
   | 8080 | Backend API (skip if proxied through Nginx on 80/443) | 0.0.0.0/0 or restrict to frontend host |
   | 5173 | Frontend dev preview (non-production only) | Your IP |

4. **DNS** — point an A record (e.g. `api.smartdental.example.com`) at the Elastic IP.

## 2. AWS SES configuration

1. In the SES console, **verify an identity**: either the sending domain (recommended — enables DKIM) or a
   single sender email address (`SES_SENDER_EMAIL`).
2. While your account is in the SES **sandbox**, you can only send to verified recipient addresses — request
   production access before go-live.
3. **IAM policy** for the backend's SES access (attach to an IAM user for static keys, or an IAM role if using
   instance profiles):

   ```json
   {
     "Version": "2012-10-17",
     "Statement": [
       {
         "Effect": "Allow",
         "Action": ["ses:SendEmail", "ses:SendRawEmail"],
         "Resource": "*"
       }
     ]
   }
   ```

4. **Credentials**: either
   - attach the IAM role to the EC2 instance profile and leave `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY`
     empty (the AWS SDK falls back to the instance's default credential chain), **or**
   - create an IAM user with the policy above, generate an access key pair, and set
     `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` as GitHub secrets (injected into `.env` on deploy).
5. Set `SES_ENABLED=true` and `SES_SENDER_EMAIL` to the verified identity.

## 3. GitHub Actions CI/CD setup

`.github/workflows/deploy.yml` runs `mvn verify` on every push/PR, and deploys to EC2 on push to `main`.
Configure these repository secrets (**Settings → Secrets and variables → Actions**):

| Secret | Description |
|---|---|
| `EC2_HOST` | Elastic IP or DNS name of the instance |
| `EC2_USER` | SSH user (`ubuntu` for standard Ubuntu AMIs) |
| `EC2_SSH_KEY` | Private key (PEM) matching the instance's key pair |
| `EC2_APP_DIR` | Absolute path on the instance where this repo is cloned, e.g. `/home/ubuntu/smartdental-backend` |
| `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | Production database credentials |
| `JWT_SECRET` | Long random string, **different from local dev** |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | Google OAuth2 credentials (see below) |
| `AWS_REGION`, `SES_SENDER_EMAIL`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` | SES config from step 2 |
| `AI_PROVIDER`, `AI_API_KEY` | `openai` or `gemini`, plus its API key |
| `CORS_ALLOWED_ORIGINS`, `FRONTEND_BASE_URL` | Production frontend origin |

**Google OAuth2 setup**: in the Google Cloud Console, create an OAuth 2.0 Client ID (Web application) with an
authorized redirect URI of `https://api.smartdental.example.com/login/oauth2/code/google`.

## 4. Container execution on EC2

```bash
# One-time setup
sudo apt update && sudo apt install -y docker.io docker-compose-plugin git
sudo usermod -aG docker $USER   # log out/in for this to take effect

git clone https://github.com/<your-org>/smartdental-backend.git
cd smartdental-backend
cp .env.example .env   # then fill in production values, or let CI write .env on deploy
```

Manual deploy (the GitHub Actions job does this automatically on push to `main`):

```bash
git pull origin main
docker compose up -d --build
docker compose logs -f backend   # tail startup logs
```

**Database migrations**: this project uses `JPA_DDL_AUTO=update` for simplicity (Hibernate reconciles the
schema at startup). For a stricter production process, set `JPA_DDL_AUTO=validate` and manage migrations with
a dedicated tool (Flyway/Liquibase) applied before `docker compose up`.

Zero-downtime-ish redeploy: `docker compose up -d --build` recreates only the changed containers; the
`postgres` volume persists data across redeploys.

## 5. SSL & reverse proxy (optional but recommended)

Run Nginx on the host (not in Compose) to terminate TLS and proxy to the backend container on `8080`:

```nginx
# /etc/nginx/sites-available/smartdental-backend
server {
    listen 80;
    server_name api.smartdental.example.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

```bash
sudo ln -s /etc/nginx/sites-available/smartdental-backend /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx

sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d api.smartdental.example.com
```

Certbot rewrites the server block to listen on 443 with the issued certificate and sets up auto-renewal via a
systemd timer. After this, close inbound port 8080 in the security group and only expose it to `localhost`.

## Rollback

```bash
git log --oneline -5          # find the last known-good commit
git checkout <commit-sha>
docker compose up -d --build
```

Because the Postgres volume is untouched by redeploys, rolling back the app code does not affect existing data
unless a migration in the rolled-back window changed the schema incompatibly.
