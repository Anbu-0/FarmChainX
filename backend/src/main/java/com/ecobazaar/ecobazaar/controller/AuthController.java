package com.ecobazaar.ecobazaar.controller;

import com.ecobazaar.ecobazaar.dto.AuthResponse;
import com.ecobazaar.ecobazaar.dto.LoginRequest;
import com.ecobazaar.ecobazaar.dto.RegisterRequest;
import com.ecobazaar.ecobazaar.service.AuthService;
import com.ecobazaar.ecobazaar.repository.UserRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
	public ResponseEntity<?> register(@RequestBody RegisterRequest register) {
	    if (userRepository.existsByEmail(register.getEmail())) {
	        return ResponseEntity.badRequest().body(
	            Map.of("error", "Email already exists!")
	        );
	    }

	    AuthResponse response = authService.register(register);
	    return ResponseEntity.ok(response);
	}



	
	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest login){
		return ResponseEntity.ok(authService.login(login));
	}

}