package com.smartdental.repository;

import com.smartdental.entity.User;
import com.smartdental.entity.enums.RoleName;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByGoogleId(String googleId);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findByRolesContainingOrderByLastNameAsc(RoleName role);
}
