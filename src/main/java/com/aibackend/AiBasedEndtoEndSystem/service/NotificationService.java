package com.aibackend.AiBasedEndtoEndSystem.service;

import com.aibackend.AiBasedEndtoEndSystem.controller.RecruiterController;
import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.entity.Candidate;
import com.aibackend.AiBasedEndtoEndSystem.entity.Chat;
import com.aibackend.AiBasedEndtoEndSystem.entity.JobPostings;
import com.aibackend.AiBasedEndtoEndSystem.entity.Notification;
import com.aibackend.AiBasedEndtoEndSystem.repository.NotificationRepository;
import com.aibackend.AiBasedEndtoEndSystem.util.UniqueUtility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class NotificationService {
    @Autowired
    private UniqueUtility uniqueUtility;
    @Autowired
    private NotificationRepository repository;
    @Autowired
    @Lazy
    private ChatService chatService;

    public void createNotification(Chat.Source source,
                                   String message,
                                   Chat chat) {

        log.info("Create new notification for the encrypted message :{}", message);
        Notification notification = new Notification();
        notification.setId(uniqueUtility.getNextNumber("NOTIFICATION", "notification"));
        notification.setCreatedAt(Instant.now());
        notification.setMessage(message);
        notification.setRelativeId(chat.getId());
        notification.setTitle("New Message");
        switch (source) {
            case CANDIDATE:
                notification.setSenderId(chat.getCandidateId());
                notification.setReceiverId(chat.getRecruiterId());
                break;
            case RECRUITER:
                notification.setSenderId(chat.getRecruiterId());
                notification.setReceiverId(chat.getCandidateId());
                break;
            default:
                throw new IllegalArgumentException("Unknown chat source: " + source);
        }
        notification.setRecruiterId(chat.getRecruiterId());
        notification.setCandidateId(chat.getCandidateId());
        notification.setSource(source);
        saveNotification(notification);
    }


    public Notification saveNotification(Notification notification) {
        log.info("Saving notification :{}", notification.getId());
        return repository.save(notification);
    }

    public Boolean markAllRead(UserDTO user) {
        log.info("Mark all read notification for the user :{}", user);
        List<Notification> notifications = repository.findByReceiverIdOrderByCreatedAtDesc(user.getId());
        if (notifications.isEmpty()) {
            return null;
        }
        for (Notification notification : notifications) {
            notification.setRead(Boolean.TRUE);
            notification.setUpdatedAt(Instant.now());
            saveNotification(notification);
        }
        return Boolean.TRUE;
    }

    public List<RecruiterController.NotificationResponse> getAllNotification(UserDTO user) {
        List<RecruiterController.NotificationResponse> list = new ArrayList<>();
        log.info("User is here :{}", user);
        List<Notification> notifications = repository.findByReceiverIdOrderByCreatedAtDesc(user.getId());
        if (notifications.isEmpty()) {
            log.info("No notification found for the user");
            return null;
        }
        for (Notification notification : notifications) {
            RecruiterController.NotificationResponse response = new RecruiterController.NotificationResponse();
            response.setId(notification.getId());
            response.setTitle(notification.getTitle());
            String message = chatService.safeDecrypt(notification.getMessage());
            response.setMessage(message);
            response.setRead(notification.getRead());
            response.setRelativeId(notification.getRelativeId());
            list.add(response);
        }
        return list;

    }

    public Boolean markReadNotification(UserDTO user, String id) {
        log.info("Mark read notification for the user :{}", user);
        Notification notifications = repository.findById(id).orElse(null);
        if (ObjectUtils.isEmpty(notifications)) {
            return Boolean.FALSE;
        }
        notifications.setRead(Boolean.TRUE);
        notifications.setUpdatedAt(Instant.now());
        saveNotification(notifications);
        return Boolean.TRUE;
    }

    public void createJobNotification(Candidate candidate, String message, JobPostings jobPostings) {
        log.info("Create new notification for Applied Job :{}", message);
        Notification notification = new Notification();
        notification.setId(uniqueUtility.getNextNumber("NOTIFICATION", "notification"));
        notification.setCreatedAt(Instant.now());
        notification.setMessage(message);
        notification.setReceiverId(candidate.getId());
        notification.setRelativeId(jobPostings.getId());
        notification.setTitle("Job Application Submitted");
        notification.setCandidateId(candidate.getId());
        notification.setSource(Chat.Source.CANDIDATE);
        saveNotification(notification);
    }

    public void createAiScreeningResultNotification(
            Candidate candidate,
            JobPostings job,
            boolean shortlisted,
            String jobApplicationId) {
        if (candidate == null || job == null) {
            return;
        }
        String jobTitle = job.getTitle() != null && !job.getTitle().isBlank() ? job.getTitle() : "this role";
        Notification notification = new Notification();
        notification.setId(uniqueUtility.getNextNumber("NOTIFICATION", "notification"));
        notification.setCreatedAt(Instant.now());
        notification.setReceiverId(candidate.getId());
        notification.setCandidateId(candidate.getId());
        notification.setRelativeId(jobApplicationId != null && !jobApplicationId.isBlank() ? jobApplicationId : job.getId());
        notification.setSource(Chat.Source.CANDIDATE);
        notification.setRead(Boolean.FALSE);
        if (shortlisted) {
            notification.setTitle("AI screening — Under review");
            notification.setMessage(
                    "Your application for \"" + jobTitle + "\" passed AI screening and is now under recruiter review.");
        } else {
            notification.setTitle("Application update");
            notification.setMessage(
                    "Your application for \"" + jobTitle + "\" did not pass AI screening for this role.");
        }
        saveNotification(notification);
        log.info("AI screening notification saved for candidate {} jobApplication {}", candidate.getId(), jobApplicationId);
    }
}
