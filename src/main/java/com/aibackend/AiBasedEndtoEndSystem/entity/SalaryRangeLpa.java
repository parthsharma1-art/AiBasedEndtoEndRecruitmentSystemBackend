package com.aibackend.AiBasedEndtoEndSystem.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * LPA bounds stored in MongoDB (plain integers — avoids Spring {@code Range} / {@code Optional} mapping issues).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalaryRangeLpa {
    private Integer min;
    private Integer max;

    public static SalaryRangeLpa of(int min, int max) {
        return new SalaryRangeLpa(Math.min(min, max), Math.max(min, max));
    }
}
