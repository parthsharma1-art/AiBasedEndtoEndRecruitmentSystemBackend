package com.aibackend.AiBasedEndtoEndSystem.controller;

import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.entity.User;
import com.aibackend.AiBasedEndtoEndSystem.exception.BadException;
import com.aibackend.AiBasedEndtoEndSystem.util.JwtUtil;
import com.aibackend.AiBasedEndtoEndSystem.util.SecurityUtils;
import com.aibackend.AiBasedEndtoEndSystem.entity.Recruiter;
import com.aibackend.AiBasedEndtoEndSystem.service.RecruiterService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/hr")
@Slf4j
public class RecruiterController {
    @Autowired
    private RecruiterService recruiterService;
    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/create")
    public String createNewHR(@RequestBody HrDTO request) {
        log.info("New Hr Details :{}", request);
        UserDTO userDTO = recruiterService.createNewRecruiter(request);
        return jwtUtil.generateToken(userDTO);
    }

    @GetMapping("/get/{mobileNumber}")
    public Recruiter getUser(@PathVariable String mobileNumber) {
        UserDTO loggedInUser = SecurityUtils.getLoggedInUser();
        if (!loggedInUser.getRole().equals(User.Role.RECRUITER)) {
            throw new BadException("Only Recruiter can access this endpoint");
        }
        Recruiter recruiter = recruiterService.getHrByMobileNumber(mobileNumber);
        if (recruiter == null) {
            throw new BadException("Recruiter not found");
        }
        return recruiter;
    }




    @Data
    public static class HrDTO {
        private String name;
        private String mobileNumber;
        private String email;
        private String companyName;
        private Integer age;
        private String state;
        private String country;
        private String designation;
    }
}
