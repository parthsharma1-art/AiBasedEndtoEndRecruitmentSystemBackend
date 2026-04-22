package com.aibackend.AiBasedEndtoEndSystem.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Full AI interview view for a job application: session summary (scores, status, job context) plus
 * all turns with parsed score/feedback where available.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiInterviewFullDetailDto {
    private AiInterviewSummaryResponse summary;
    private List<AiInterviewTurnOutDto> turns;
}
