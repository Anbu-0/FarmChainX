package com.ecobazaar.ecobazaar.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.ecobazaar.ecobazaar.dto.AuthResponse;
import com.ecobazaar.ecobazaar.dto.LoginRequest;
import com.ecobazaar.ecobazaar.dto.RegisterRequest;
import com.ecobazaar.ecobazaar.model.Role;
import com.ecobazaar.ecobazaar.model.User;
import com.ecobazaar.ecobazaar.repository.RoleRepository;
import com.ecobazaar.ecobazaar.repository.UserRepository;
import com.ecobazaar.ecobazaar.security.JwtUtil;

import java.util.Set;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already exists!");
        }

        String roleInput = request.getRole().trim().toUpperCase();

        if (!Set.of("CONSUMER", "FARMER", "DISTRIBUTOR", "RETAILER")
                .contains(roleInput)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only Consumer, Farmer, Distributor, Retailer allowed");
        }

        Role role = roleRepository.findByName("ROLE_" + roleInput)
                .orElseThrow(() -> new RuntimeException("Role not found in DB"));

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles(Set.of(role));
        userRepository.save(user);

        return new AuthResponse(null, role.getName(), request.getEmail(), user.getId());
    }




    public AuthResponse login(LoginRequest login) {

        User user = userRepository.findByEmail(login.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(login.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password!");
        }

        // ✅ Check if user has admin role
        boolean isAdmin = user.getRoles()
                .stream()
                .anyMatch(r -> r.getName().equals("ROLE_ADMIN"));

        // ✅ Primary role: ADMIN if present, else first assigned role
        String primaryRole = isAdmin
                ? "ROLE_ADMIN"
                : user.getRoles()
                    .stream()
                    .map(Role::getName)
                    .findFirst()
                    .orElse("ROLE_CONSUMER");

        // ✅ Generate token
        String token = jwtUtil.generateToken(user.getEmail(), primaryRole, user.getId());

        return new AuthResponse(token, primaryRole, user.getEmail(), user.getId());
    }
}