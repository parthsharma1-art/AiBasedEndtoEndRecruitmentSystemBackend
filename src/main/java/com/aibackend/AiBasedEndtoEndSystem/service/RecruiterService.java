package com.aibackend.AiBasedEndtoEndSystem.service;

import com.aibackend.AiBasedEndtoEndSystem.controller.RecruiterController;
import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.entity.Recruiter;
import com.aibackend.AiBasedEndtoEndSystem.exception.HrException;
import com.aibackend.AiBasedEndtoEndSystem.repository.RecruiterRepository;
import com.aibackend.AiBasedEndtoEndSystem.util.UniqueUtiliy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.Optional;

@Service
@Slf4j
public class RecruiterService {
    @Autowired
    private RecruiterRepository hrRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private UniqueUtiliy uniqueUtiliy;

    public UserDTO createNewRecruiter(RecruiterController.RecruiterRequest request) {
        Recruiter recruiter = new Recruiter();
        validateRequest(request);
        Optional<Recruiter> existing = hrRepository.findByMobileNumber(request.getMobileNumber());
        if (existing.isPresent()) {
            return userService.toRecruiterDTO(existing.get());
        }
        recruiter.setId(uniqueUtiliy.getNextNumber("RECRUITER","hr"));
        recruiter.setName(request.getName());
        recruiter.setCompanyName(request.getCompanyName());
        recruiter.setDesignation(request.getDesignation());
        recruiter.setEmail(request.getEmail());
        recruiter.setMobileNumber(request.getMobileNumber());
        recruiter = hrRepository.save(recruiter);
        return userService.toRecruiterDTO(recruiter);
    }

    public Recruiter getHrByMobileNumber(String mobileNumber) {
        log.info("Hr Mobile Number :{}", mobileNumber);
        Optional<Recruiter> hr = hrRepository.findByMobileNumber(mobileNumber);
        if (hr.isPresent()) {
            return hr.get();
        }
        throw new HrException("Hr does not exist " + mobileNumber);
    }

    private void validateRequest(RecruiterController.RecruiterRequest request) {
        if (ObjectUtils.isEmpty(request.getName())) {
            throw new HrException("Hr Name is required");
        }
        if (ObjectUtils.isEmpty(request.getMobileNumber())) {
            throw new HrException("Hr Mobile Number is required");
        }
        if (ObjectUtils.isEmpty(request.getEmail())) {
            throw new HrException("Hr Email is required");
        }
        if (ObjectUtils.isEmpty(request.getAge())) {
            throw new HrException("Hr Age is required");
        }
        if (ObjectUtils.isEmpty(request.getState())) {
            throw new HrException("Hr State is required");
        }
        if (ObjectUtils.isEmpty(request.getCompanyName())) {
            throw new HrException("Hr Company Name is required");
        }
        if (ObjectUtils.isEmpty(request.getCountry())) {
            throw new HrException("Hr Country is required");
        }
        if (ObjectUtils.isEmpty(request.getDesignation())) {
            throw new HrException("Hr Designation is required");
        }
    }


    public Recruiter findById(String id) {
        log.info("Get Hr BY id : {}", id);
        return hrRepository.findById(id).orElse(null);
    }

    public UserDTO getUserLogin(String mobileNumber) {
        log.info("User mobile Number :{}", mobileNumber);
        Optional<Recruiter> recruiter = hrRepository.findByMobileNumber(mobileNumber);
        if (recruiter.isPresent()) {
            return userService.toRecruiterDTO(recruiter.get());
        }
        return null;

    }

}
