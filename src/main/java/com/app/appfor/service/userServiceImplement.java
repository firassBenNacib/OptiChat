package com.app.appfor.service;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Service
@Slf4j
@AllArgsConstructor
public class userServiceImplement implements userService {

    @Override
    public String retrieveAllusers() {
        return "user";
    }

}
