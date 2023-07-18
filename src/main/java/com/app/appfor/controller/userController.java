package com.app.appfor.controller;
import org.springframework.web.bind.annotation.GetMapping;
import com.app.appfor.service.userService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/app")
public class userController {
    @Autowired
    userService userservice;

    @GetMapping("/userlist")
    public String showuserlist()
    {
        return userservice.retrieveAllusers();
    }



}
