package com.app.payslip.poc.service;

import com.app.payslip.poc.model.SimilarPayslipDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayslipSimilarityService {

    private static final String METADATA_NATIONAL_ID = "personal.nationalId";
    private static final String METADATA_NAME = "personal.name";
    private static final String METADATA_MONTH_KEY = "period.monthKey";
    private static final String METADATA_PAY_DATE = "period.payDate";
    private static final String METADATA_SOURCE = "source";
    private static final String PAYSLIP_SEARCH_QUERY = "payslip information";
    private static final int RESULT_MULTIPLIER = 10;

    private final VectorStore vectorStore;

    public List<SimilarPayslipDTO> payslipSimilaritySearch(String nationalId, String employeeName, int limit) {
        validateSearchParameters(nationalId, employeeName);

        log.info("Searching payslips: nationalId='{}', employeeName='{}', limit={}",
                nationalId, employeeName, limit);

        Filter.Expression filterExpression = buildFilterExpression(nationalId, employeeName);
        List<Document> documents = searchDocuments(filterExpression, limit);

        log.info("Found {} documents", documents.size());

        return groupDocumentsByPayslip(documents, limit);
    }

    private void validateSearchParameters(String nationalId, String employeeName) {
        boolean hasNationalId = nationalId != null && !nationalId.isBlank();
        boolean hasEmployeeName = employeeName != null && !employeeName.isBlank();

        if (!hasNationalId && !hasEmployeeName) {
            throw new IllegalArgumentException("At least one of nationalId or employeeName must be provided");
        }
    }

    private Filter.Expression buildFilterExpression(String nationalId, String employeeName) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();

        boolean hasNationalId = nationalId != null && !nationalId.isBlank();
        boolean hasEmployeeName = employeeName != null && !employeeName.isBlank();

        if (hasNationalId && hasEmployeeName) {
            return builder.or(
                    builder.eq(METADATA_NATIONAL_ID, nationalId),
                    builder.eq(METADATA_NAME, employeeName)
            ).build();
        }

        if (hasNationalId) {
            return builder.eq(METADATA_NATIONAL_ID, nationalId).build();
        }

        return builder.eq(METADATA_NAME, employeeName).build();
    }

    private List<Document> searchDocuments(Filter.Expression filterExpression, int limit) {
        SearchRequest searchRequest = SearchRequest.builder()
                .query(PAYSLIP_SEARCH_QUERY)
                .topK(limit * RESULT_MULTIPLIER)
                .filterExpression(filterExpression)
                .build();

        return vectorStore.similaritySearch(searchRequest);
    }

    private List<SimilarPayslipDTO> groupDocumentsByPayslip(List<Document> documents, int limit) {
        Map<String, SimilarPayslipDTO> payslipMap = new LinkedHashMap<>();

        for (Document doc : documents) {
            String payslipId = extractPayslipId(doc.getId());

            if (!payslipMap.containsKey(payslipId)) {
                payslipMap.put(payslipId, createSimilarPayslipDTO(doc, payslipId));
            }

            payslipMap.get(payslipId).documentIds().add(doc.getId());
        }

        return limitResults(new ArrayList<>(payslipMap.values()), limit);
    }

    private SimilarPayslipDTO createSimilarPayslipDTO(Document doc, String payslipId) {
        Map<String, Object> metadata = doc.getMetadata();

        return SimilarPayslipDTO.builder()
                .payslipId(payslipId)
                .nationalId(getMetadataString(metadata, METADATA_NATIONAL_ID))
                .name(getMetadataString(metadata, METADATA_NAME))
                .monthKey(getMetadataString(metadata, METADATA_MONTH_KEY))
                .payDate(getMetadataString(metadata, METADATA_PAY_DATE))
                .source(getMetadataString(metadata, METADATA_SOURCE))
                .documentIds(new ArrayList<>())
                .build();
    }

    private String extractPayslipId(String documentId) {
        if (documentId == null) {
            return "unknown";
        }

        String[] parts = documentId.split(":");
        if (parts.length >= 4) {
            return String.join(":", parts[0], parts[1], parts[2], parts[3]);
        }

        return documentId;
    }

    private String getMetadataString(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        return value != null ? value.toString() : "";
    }

    private List<SimilarPayslipDTO> limitResults(List<SimilarPayslipDTO> payslips, int limit) {
        if (payslips.size() > limit) {
            return payslips.subList(0, limit);
        }
        return payslips;
    }
}