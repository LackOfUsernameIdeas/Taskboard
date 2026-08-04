package com.kaloyan.taskboard.core.repository;

import com.kaloyan.taskboard.core.model.Role;
import com.kaloyan.taskboard.core.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByRole(Role role);
}
