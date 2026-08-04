package com.jomariabejo.connectly_api.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;



@Hidden  // internal smoke-test endpoint, kept out of the published API docs
@RestController
@RequestMapping("/test")
public class HelloWorldController {

    @GetMapping("/helloworld")
    public String helloWorld() {
        return "Hello World";
    }
}
