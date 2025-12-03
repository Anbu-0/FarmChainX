package com.ecobazaar.ecobazaar.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.nio.file.Files;

@Controller
@ConditionalOnProperty(name = "app.spa.enabled", havingValue = "true", matchIfMissing = true)
public class FrontendController {

    // This serves index.html for ALL non-API routes (Angular handles routing)
    @GetMapping(value = {
        "/",
        "/dashboard",
        "/upload",
        "/products/my",
        "/scanner",
        "/verify/{uuid:^[0-9a-fA-F\\-]{36}$}",  // matches UUID pattern only
        "/verify/**",
        "/login",
        "/register",
        "/**/{path:[^\\.]*}"  // any path without a dot (excludes .js, .css, etc.)
    })
    @ResponseBody
    public Resource index() throws IOException {
        return new ClassPathResource("static/index.html");
    }

    // Optional: nicer error page fallback
    @GetMapping("/error")
    @ResponseBody
    public Resource error() throws IOException {
        return new ClassPathResource("static/index.html");
    }
}
