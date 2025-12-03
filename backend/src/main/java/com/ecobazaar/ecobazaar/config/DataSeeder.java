package com.ecobazaar.ecobazaar.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import com.ecobazaar.ecobazaar.model.Role;
import com.ecobazaar.ecobazaar.repository.RoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.ecobazaar.ecobazaar.model.User;
import com.ecobazaar.ecobazaar.repository.UserRepository;

import java.util.Set;


@Component
public class DataSeeder implements CommandLineRunner {

	private final RoleRepository roleRepo;
    private final UserRepository userRepo;
    private final PasswordEncoder encoder;

    public DataSeeder(RoleRepository roleRepo, UserRepository userRepo, PasswordEncoder encoder) {
        this.roleRepo = roleRepo;
        this.userRepo = userRepo;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {

        // ✅ 1) Create all roles
        String[] roles = {"ROLE_CONSUMER","ROLE_FARMER","ROLE_DISTRIBUTOR","ROLE_RETAILER","ROLE_ADMIN"};
        for (String r : roles) {
            if (!roleRepo.existsByName(r)) {
                Role role = new Role();
                role.setName(r);
                roleRepo.save(role);
            }
        }
        System.out.println("✅ Role seeding completed successfully!");

        // ✅ 2) Create default admin
        String email = "admin@farmchainx.com";

        if (!userRepo.existsByEmail(email)) {
            Role adminRole = roleRepo.findByName("ROLE_ADMIN").orElseThrow();

            User admin = new User();
            admin.setName("Admin");
            admin.setEmail(email);
            admin.setPassword(encoder.encode("admin123"));
            admin.setRoles(Set.of(adminRole));

            userRepo.save(admin);

            System.out.println("✅ Default admin created: " + email + " | password: admin123");
        }
    }
}