package com.jomariabejo.connectly_api.controller;

import com.jomariabejo.connectly_api.common.ApiPaths;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Unauthenticated public endpoints (permitted via {@code /v1/public/**} in security config).
 */
@RestController
@RequestMapping(ApiPaths.V1_PUBLIC)
public class PublicController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello, Connectly API is running!";
    }
}
