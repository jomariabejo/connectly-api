package com.jomariabejo.connectly_api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;



@RestController
@RequestMapping("/test")
public class HelloWorldController {

    @GetMapping("/helloworld")
    public String helloWorld() {
        return "Hello World";
    }
}
