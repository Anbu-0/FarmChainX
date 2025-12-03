package com.ecobazaar.ecobazaar.dto;

public class AuthResponse {
    private String token;
    private String role;
    private String email;
    private Long userId;

    public AuthResponse(String token, String role, String email, Long userId) {
        this.token = token;
        this.role = role;
        this.email = email;
        this.userId = userId;
        
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

}