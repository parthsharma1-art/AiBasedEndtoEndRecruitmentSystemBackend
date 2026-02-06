package com.aibackend.AiBasedEndtoEndSystem.controller;

import com.aibackend.AiBasedEndtoEndSystem.util.SecurityUtils;
import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.exception.BadException;
import com.aibackend.AiBasedEndtoEndSystem.service.UserService;
import com.aibackend.AiBasedEndtoEndSystem.util.JwtUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/public")
@Slf4j
public class PublicController {
    @Autowired
    private UserService userService;
    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public UserResponse register(@RequestBody UserRequest request) throws Exception{
        UserDTO user = userService.createUser(request);
        user.setRole("User");
        JwtUtil.Token token = jwtUtil.generateClientToken(user);
        log.info("This is the new token i have created :{}", token);
        return toUserResponse(user,token);
    }

    @PostMapping("/login")
    public UserResponse login(@RequestBody LoginRequest request) throws Exception{
        UserDTO user = userService.getUserByMobileNumber(request);
        user.setRole("User");
        JwtUtil.Token token = jwtUtil.generateClientToken(user);
        return toUserResponse(user,token);
    }

    public UserResponse toUserResponse(UserDTO user, JwtUtil.Token token){
        UserResponse response=new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getUserEmail());
        response.setMobileNumber(user.getMobileNumber());
        response.setName(user.getUsername());
        response.setToken(token);
        return response;
    }
    
    @Data
    public static class UserResponse{
        private String id;
        private String name;
        private String email;
        private String mobileNumber;
        private JwtUtil.Token token;
    }

    @GetMapping("/get")
    public UserDTO getUser(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BadException("Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7); // remove "Bearer "
        log.info("Token for the Recruiter :{}", token);
        return SecurityUtils.getLoggedInUser(token, jwtUtil.getKey());
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
