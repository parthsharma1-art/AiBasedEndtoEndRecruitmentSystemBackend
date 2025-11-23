package com.aibackend.AiBasedEndtoEndSystem.controller;

import com.aibackend.AiBasedEndtoEndSystem.util.SecurityUtils;
import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.entity.User;
import com.aibackend.AiBasedEndtoEndSystem.exception.BadException;
import com.aibackend.AiBasedEndtoEndSystem.service.UserService;
import com.aibackend.AiBasedEndtoEndSystem.util.JwtUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/public")
@Slf4j
public class PublicController {
    @Autowired
    private UserService userService;
    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody UserRequest request) {
        UserDTO user = userService.createUser(request);
        String token = jwtUtil.generateToken(user);
        log.info("This is the new token i have created :{}",token);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(Map.of("token", token));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequest login) {
        UserDTO user = userService.getUserByMobileNumber(login.getMobileNumber());
        String token = jwtUtil.generateToken(user);
        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(Map.of("token", token));
    }

    @GetMapping("/all")
    public List<User> getAllUser() {
        User userId =(User) SecurityUtils.getLoggedInPrincipal();
        log.info("User user :{}", userId);
        if (userId.getName().equals("Parth Sharma")) {
            return userService.getAllUser();
        }
        throw new BadException("not authorized");
    }

    @Data
    public static class UserRequest {
        private String name;
        private String mobileNumber;
        private Integer age;
        private String state;
        private String userType;
    }

    @Data
    public static class LoginRequest {
        private String mobileNumber;
    }
}
