package com.app.payslip.poc.service;

import com.app.payslip.poc.model.PayslipSectionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayslipSectionService {

    private static final String METADATA_DOC_ID = "docId";
    private static final String METADATA_SECTION = "section";
    private static final String METADATA_SOURCE = "source";
    public static final String EXACT_DOC_ID_EMPTY_QUERY = "";

    private final VectorStore vectorStore;

    public Optional<PayslipSectionDTO> getSectionByDocId(String docId) {
        log.info("Retrieving payslip section with docId: {}", docId);

        List<Document> documents = searchByDocId(docId);

        if (documents.isEmpty()) {
            log.warn("No document found with docId: {}", docId);
            return Optional.empty();
        }

        if (documents.size() > 1) {
            log.warn("Multiple documents found with docId: {}. Returning the first one.", docId);
        }

        Document document = documents.get(0);
        return Optional.of(mapToSectionDTO(document));
    }

    private List<Document> searchByDocId(String docId) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();

        SearchRequest searchRequest = SearchRequest.builder()
                .query(EXACT_DOC_ID_EMPTY_QUERY)
                .topK(5)
                .filterExpression(builder.eq(METADATA_DOC_ID, docId).build())
                .build();

        return vectorStore.similaritySearch(searchRequest);
    }

    private PayslipSectionDTO mapToSectionDTO(Document document) {
        Map<String, Object> metadata = document.getMetadata();

        return PayslipSectionDTO.builder()
                .docId(getMetadataAsString(metadata, METADATA_DOC_ID))
                .section(getMetadataAsString(metadata, METADATA_SECTION))
                .content(document.getText())
                .source(getMetadataAsString(metadata, METADATA_SOURCE))
                .metadata(metadata)
                .build();
    }

    private String getMetadataAsString(Map<String, Object> metadata, String key) {
        return Optional.ofNullable(metadata.get(key))
                .map(String::valueOf)
                .orElse("");
    }
}