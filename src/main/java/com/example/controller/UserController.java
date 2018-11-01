package com.example.controller;

import com.example.service.UserService;
import com.springmvc.annotation.Autowired;
import com.springmvc.annotation.Controller;
import com.springmvc.annotation.RequestMapping;
import com.springmvc.annotation.RequestParam;

@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;


    @RequestMapping("/get")
    public void show(@RequestParam("name") String name) {
        System.out.println(name);
    }
}
