
package com.app.payslip.poc.model;

import lombok.Builder;

import java.util.Map;

@Builder
public record PayslipSectionDTO(
        String docId,
        String section,
        String content,
        String source,
        Map<String, Object> metadata
) {
}