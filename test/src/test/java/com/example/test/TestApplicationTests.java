package com.example.test;

import com.example.test.bean.UserBean;
import com.example.test.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;


@SpringBootTest
public class TestApplicationTests {

    @Autowired
    UserService userService;

}