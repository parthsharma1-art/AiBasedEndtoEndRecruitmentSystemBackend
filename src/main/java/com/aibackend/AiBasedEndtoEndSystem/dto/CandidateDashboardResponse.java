package com.aibackend.AiBasedEndtoEndSystem.dto;

import java.util.List;

import com.aibackend.AiBasedEndtoEndSystem.controller.CandidateApplyJobController.CandidateAppliedJobResponse;
import com.aibackend.AiBasedEndtoEndSystem.controller.PublicCompanyJobsController.PublicJobResponse;

import lombok.Data;

@Data
public class CandidateDashboardResponse {

    private ApplicationSummary summary;
    private List<CandidateAppliedJobResponse> applications;
    private List<PublicJobResponse> jobsNotApplied;

    @Data
    public static class ApplicationSummary {
        private int totalApplications;
        private int shortlisted;
        private int applied;
        private int testScheduled;
        private int underReview;
        private int rejected;
    }
}
