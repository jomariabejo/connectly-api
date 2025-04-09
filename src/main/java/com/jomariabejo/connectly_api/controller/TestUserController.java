package com.jomariabejo.connectly_api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/user")
@RestController
public class TestUserController {

    @GetMapping("/dashboard")
    public String userDashboard() {
        return "I am at another user controller";
    }
}