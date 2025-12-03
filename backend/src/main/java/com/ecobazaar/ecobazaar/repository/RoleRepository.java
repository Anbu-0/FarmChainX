package com.ecobazaar.ecobazaar.repository;

import com.ecobazaar.ecobazaar.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String Name);
    boolean existsByName(String Name);
}
