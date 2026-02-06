package com.aibackend.AiBasedEndtoEndSystem.service;

import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.entity.Candidate;
import com.aibackend.AiBasedEndtoEndSystem.entity.Recruiter;
import com.aibackend.AiBasedEndtoEndSystem.exception.UserNotFoundException;
import com.aibackend.AiBasedEndtoEndSystem.controller.PublicController;
import com.aibackend.AiBasedEndtoEndSystem.entity.User;
import com.aibackend.AiBasedEndtoEndSystem.repository.UserRepository;
import com.aibackend.AiBasedEndtoEndSystem.util.UniqueUtiliy;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UniqueUtiliy uniqueUtiliy;

    public UserDTO createUser(PublicController.UserRequest request) {
        log.info("New User data :{}", request);
        Optional<User> existing = userRepository.findByMobileNumber(request.getMobileNumber());
        if (!ObjectUtils.isEmpty(existing)) {
            log.info("Existing User :{}", existing);
            return toUserDTO(existing.get());
        }
        User user = new User();
        user.setId(uniqueUtiliy.getNextNumber("USER","us"));
        user.setAge(request.getAge());
        user.setName(request.getName());
        user.setMobileNumber(request.getMobileNumber());
        user.setEmail(request.getEmail());
        User savedUser = userRepository.save(user);
        log.info("Saved User Details :{}", savedUser);
        return toUserDTO(user);
    }

    public UserDTO toUserDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUserEmail(user.getEmail());
        dto.setUsername(user.getName());
        dto.setMobileNumber(user.getMobileNumber());
        return dto;
    }

    public UserDTO toRecruiterDTO(Recruiter recruiter) {
        UserDTO dto = new UserDTO();
        dto.setId(recruiter.getId());
        dto.setUserEmail(recruiter.getEmail());
        dto.setUsername(recruiter.getName());
        dto.setMobileNumber(recruiter.getMobileNumber());
        return dto;
    }

    public UserDTO toCandidateDTO(Candidate candidate) {
        UserDTO dto = new UserDTO();
        dto.setId(candidate.getId());
        dto.setUserEmail(candidate.getEmail());
        dto.setUsername(candidate.getName());
        dto.setMobileNumber(candidate.getMobileNumber());
        return dto;
    }


    public UserDTO getUserByMobileNumber(PublicController.LoginRequest request) {
        Optional<User> user=null;
        if(!ObjectUtils.isEmpty(request.getMobileNumber())){
            user = userRepository.findByMobileNumber(request.getMobileNumber());
        }else{
            user=userRepository.findByEmail(request.getEmail());
        }
        if (ObjectUtils.isEmpty(user)) {
            throw new UserNotFoundException("This User do not exist " + request);
        }
        return toUserDTO(user.get());
    }

    public List<User> getAllUser() {
        List<User> users = userRepository.findAll();
        return users;
    }

    public User getUserById(String userId) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isPresent()) {
            return user.get();
        }
        return null;
    }

    public User findByName(String name) {
        Optional<User> user = userRepository.findByName(name);
        if (user.isPresent()) {
            return user.get();
        }
        return null;
    }
}
