package com.aibackend.AiBasedEndtoEndSystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.service.UserService;
import com.aibackend.AiBasedEndtoEndSystem.util.JwtUtil;
import com.aibackend.AiBasedEndtoEndSystem.util.SecurityUtils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/public")
@Slf4j
public class PublicController {
    @Autowired
    private UserService userService;
    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public UserResponse register(@RequestBody UserRequest request) throws Exception {
        UserDTO user = userService.createUser(request);
        user.setRole("User");
        JwtUtil.Token token = jwtUtil.generateClientToken(user);
        log.info("This is the new token i have created :{}", token);
        return toUserResponse(user, token);
    }

    @PostMapping("/login")
    public UserResponse login(@RequestBody LoginRequest request) throws Exception {
        UserDTO user = userService.getUserByMobileNumber(request);
        user.setRole("User");
        JwtUtil.Token token = jwtUtil.generateClientToken(user);
        return toUserResponse(user, token);
    }

    public UserResponse toUserResponse(UserDTO user, JwtUtil.Token token) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getUserEmail());
        response.setMobileNumber(user.getMobileNumber());
        response.setName(user.getUsername());
        response.setToken(token);
        return response;
    }

    @Data
    public static class UserResponse {
        private String id;
        private String name;
        private String email;
        private String mobileNumber;
        private JwtUtil.Token token;
    }

    @GetMapping("/get")
    public UserDTO getUser(@RequestHeader("Authorization") String authHeader) {
        UserDTO user = SecurityUtils.getLoggedInUser();
        if (user == null)
            throw new ResponseStatusException(UNAUTHORIZED, "Not authenticated");
        log.info("Logged User Details is  :{}", user);
        return SecurityUtils.getLoggedInUser();
    }

    @Data
    public static class UserRequest {
        private String name;
        private String mobileNumber;
        private Integer age;
        private String email;
    }

    @Data
    public static class LoginRequest {
        private String mobileNumber;
        private String email;
    }
}
