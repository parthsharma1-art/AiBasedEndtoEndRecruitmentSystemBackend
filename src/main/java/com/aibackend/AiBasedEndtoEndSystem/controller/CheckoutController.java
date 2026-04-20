package com.aibackend.AiBasedEndtoEndSystem.controller;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.entity.Checkout;
import com.aibackend.AiBasedEndtoEndSystem.entity.SubscriptionPlan.SubscriptionPlanType;
import com.aibackend.AiBasedEndtoEndSystem.service.CheckoutService;
import com.aibackend.AiBasedEndtoEndSystem.util.SecurityUtils;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService paymentService;

    @PostMapping("/create")
    public Checkout createCheckout(@RequestBody CheckoutRequest request) throws Exception {
        UserDTO user = SecurityUtils.getLoggedInUser();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
        }
        return paymentService.createCheckoutFromRequest(user, request);
    }

    @GetMapping("/list")
    public List<CheckoutDto> getCheckoutListForLoggedInRecruiter() {
        UserDTO user = SecurityUtils.getLoggedInUser();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
        }
        List<Checkout> checkouts = paymentService.getCheckoutsForRecruiter(user);
        List<CheckoutDto> list = new ArrayList<>();
        for (Checkout checkout : checkouts) {
            list.add(toCheckoutDto(checkout));
        }
        return list;
    }

    private static CheckoutDto toCheckoutDto(Checkout checkout) {
        CheckoutDto dto = new CheckoutDto();
        dto.setId(checkout.getId());
        dto.setCompanyId(checkout.getCompanyId());
        dto.setRecruiterId(checkout.getRecruiterId());
        dto.setCompanyName(checkout.getCompanyName());
        dto.setPriceInPaise(checkout.getPriceInPaise());
        dto.setDurationDays(checkout.getDurationDays());
        dto.setDescription(checkout.getDescription());
        dto.setType(checkout.getType());
        dto.setStatus(checkout.getStatus());
        dto.setCreatedAt(checkout.getCreatedAt());
        dto.setUpdatedAt(checkout.getUpdatedAt());
        return dto;
    }

    @Data
    public static class CheckoutRequest {
        private Long priceInPaise;
        private Integer durationDays;
        private String description;
        private SubscriptionPlanType type;
    }

    @Data
    public static class CheckoutDto {
        private String id;
        private String companyId;
        private String recruiterId;
        private String companyName;
        private Long priceInPaise;
        private Integer durationDays;
        private String description;
        private Instant endDate;
        private SubscriptionPlanType type;
        private Checkout.CheckoutStatus status;
        private Instant createdAt;
        private Instant updatedAt;
    }
}