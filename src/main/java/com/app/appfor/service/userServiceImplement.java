package com.app.appfor.service;


import com.app.appfor.entities.user;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class userServiceImplement implements userService {

    @Override
    public String retrieveAllusers() {
        return "user";
    }

}
