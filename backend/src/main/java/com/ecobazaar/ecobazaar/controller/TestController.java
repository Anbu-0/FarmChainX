package com.ecobazaar.ecobazaar.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/test")
    public String test() {
        return "🎉 EcoBazaar is running successfully! Day 1 setup complete!";
    }
    
//    @GetMapping("/")
//    public String home() {
//        return "Welcome to EcoBazaar - Farm to Fork Traceability System!<br>" +
//               "Security Status: ACTIVE<br>" +
//               "Try: http://localhost:8080/api/auth/register";
// new change
//    }
}