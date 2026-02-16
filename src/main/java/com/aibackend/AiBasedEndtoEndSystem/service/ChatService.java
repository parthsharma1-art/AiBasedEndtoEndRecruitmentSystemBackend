package com.aibackend.AiBasedEndtoEndSystem.service;

import com.aibackend.AiBasedEndtoEndSystem.controller.CandidateController;
import com.aibackend.AiBasedEndtoEndSystem.controller.RecruiterController;
import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.entity.Candidate;
import com.aibackend.AiBasedEndtoEndSystem.entity.Chat;
import com.aibackend.AiBasedEndtoEndSystem.entity.Recruiter;
import com.aibackend.AiBasedEndtoEndSystem.exception.BadException;
import com.aibackend.AiBasedEndtoEndSystem.repository.ChatRepository;
import com.aibackend.AiBasedEndtoEndSystem.util.UniqueUtility;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ChatService {
    @Autowired
    private ChatRepository repository;
    @Autowired
    private UniqueUtility uniqueUtility;
    @Autowired
    private CandidateService candidateService;
    @Autowired
    private RecruiterService recruiterService;
    @Autowired
    private NotificationService notificationService;

    public Chat createOrUpdateChat(UserDTO user, RecruiterController.ChatRequest request, Chat.Source source) {
        log.info("Creating or updating request if any :{}", request);
        Recruiter recruiter = recruiterService.getRecruiterById(request.getRecruiterId());
        if (ObjectUtils.isEmpty(recruiter)) {
            throw new BadException("Recruiter not found for the id " + request.getRecruiterId());
        }
        Candidate candidate = candidateService.getCandidateById(request.getCandidateId());
        if (ObjectUtils.isEmpty(candidate)) {
            throw new BadException("Candidate not found for the id " + request.getCandidateId());
        }
        Optional<Chat> optionalChat = repository.findByRecruiterIdAndCandidateId(request.getRecruiterId(), request.getCandidateId());
        if (optionalChat.isPresent()) {
            Chat chat = optionalChat.get();
            List<Chat.ChatData> chatDataList = chat.getChatData();
            Chat.ChatData chatData = new Chat.ChatData();
            chatData.setMessage(uniqueUtility.getNextNumber("MESSAGE_ID", "msg"));
            chatData.setCreatedAt(Instant.now());
            chatData.setCreatedBy(user.getId());
            chatData.setMessage(request.getMessage());
            chatData.setSource(source);
            chatDataList.add(chatData);
            chat.setChatData(chatDataList);
            chat.setUpdateAt(Instant.now());
            chat.setUpdatedBy(user.getId());
            chat = save(chat);
            notificationService.createNotification(source, request, chat);
            return chat;
        }
        log.info("Creating new Chat for the request :{}", request);
        Chat chat = new Chat();
        chat.setId(uniqueUtility.getNextNumber("CHAT", "chat"));
        chat.setRecruiterId(user.getId());
        chat.setCandidateId(request.getCandidateId());
        chat.setCreatedAt(Instant.now());
        chat.setCreatedBy(user.getId());
        Chat.ChatData chatData = new Chat.ChatData();
        chatData.setMessageId(uniqueUtility.getNextNumber("MESSAGE_ID", "msg"));
        chatData.setCreatedBy(user.getId());
        chatData.setSource(source);
        chatData.setMessage(request.getMessage());
        List<Chat.ChatData> chatDataList = new ArrayList<>();
        chatDataList.add(chatData);
        chat.setChatData(chatDataList);
        chat = save(chat);
        notificationService.createNotification(source, request, chat);
        return chat;

    }

    public Chat save(Chat chat) {
        log.info("Saving chat data :{}", chat);
        return repository.save(chat);
    }

    public ChatResponse getChats(UserDTO userDTO, String chatId) {
        log.info("Get Chat for the user :{}", chatId);
        Optional<Chat> chat = repository.findById(chatId);
        if (chat.isEmpty()) {
            throw new BadException("Chat not found for the id " + chatId);
        }
        if (!chat.get().getRecruiterId().equals(userDTO.getId()) && !chat.get().getCandidateId().equals(userDTO.getId())) {
            throw new BadException("Invalid access to the chats " + userDTO.getId());
        }
        return toChatResponse(chat.get());
    }

    public List<ChatResponse> getAllChats(UserDTO user, Chat.Source source) {
        log.info("Get all chats for the recruiter :{}", user);
        List<Chat> chatList = null;
        switch (source) {
            case CANDIDATE -> chatList = repository.findByCandidateId(user.getId());
            case RECRUITER -> chatList = repository.findByRecruiterId(user.getId());
        }
        if (ObjectUtils.isEmpty(chatList)) {
            log.info("Chat not found :{}", user);
        }
        List<ChatResponse> chatResponses = new ArrayList<>();
        for (Chat chat : chatList) {
            chatResponses.add(toChatResponse(chat));
        }
        return chatResponses;
    }

    private ChatResponse toChatResponse(Chat chat) {
        log.info("Converting to Chat response :{}", chat);
        ChatResponse chatResponse = new ChatResponse();
        Candidate candidate = candidateService.getCandidateById(chat.getCandidateId());
        Recruiter recruiter = recruiterService.getRecruiterById(chat.getRecruiterId());
        chatResponse.setChat(chat);
        chatResponse.setCandidateResponse(toCandidateRespone(candidate));
        chatResponse.setRecruiterResponse(toRecruiterResponse(recruiter));
        return chatResponse;

    }

    public CandidateController.CandidateResponse toCandidateRespone(Candidate candidate) {
        CandidateController.CandidateResponse response = new CandidateController.CandidateResponse();
        response.setId(candidate.getId());
        response.setName(candidate.getName());
        response.setEmail(candidate.getEmail());
        response.setMobileNumber(candidate.getMobileNumber());
        response.setResumeId(candidate.getResumeId());
        response.setProfileImageId(candidate.getProfileImageId());
        return response;
    }

    public RecruiterController.RecruiterResponse toRecruiterResponse(Recruiter recruiter) {
        RecruiterController.RecruiterResponse response = new RecruiterController.RecruiterResponse();
        response.setId(recruiter.getId());
        response.setName(recruiter.getName());
        response.setEmail(recruiter.getEmail());
        response.setMobileNumber(recruiter.getMobileNumber());
        response.setAge(recruiter.getAge());
        response.setState(recruiter.getState());
        response.setCountry(recruiter.getCountry());
        if (recruiter.getProfileImageId() != null) {
            response.setProfileImageUrl(recruiter.getProfileImageId());
        }
        return response;
    }

    @Data
    public static class ChatResponse {
        private String chatId;
        private RecruiterController.RecruiterResponse recruiterResponse;
        private CandidateController.CandidateResponse candidateResponse;
        private Chat chat;
    }
}
