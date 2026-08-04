package com.jomariabejo.connectly_api.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/user")
@Hidden  // internal smoke-test endpoint, kept out of the published API docs
@RestController
public class TestUserController {

    @GetMapping("/dashboard")
    public String userDashboard() {
        return "I am at another user controller";
    }
}