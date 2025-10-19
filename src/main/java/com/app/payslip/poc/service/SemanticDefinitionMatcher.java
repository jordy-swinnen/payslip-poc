
package com.app.payslip.poc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SemanticDefinitionMatcher {

    private static final double SEMANTIC_SIMILARITY_THRESHOLD = 0.55;
    private static final int MAX_SEMANTIC_DEFINITIONS = 3;

    private final VectorStore vectorStore;

    public List<Document> findRequiredDefinitions(String question, List<Document> alreadyRetrievedDefinitions) {
        log.debug("Performing semantic definition search for question: {}", question);

        List<Document> semanticMatches = performSemanticDefinitionSearch(question);

        Set<String> alreadyIncludedSources = extractSourceNames(alreadyRetrievedDefinitions);

        List<Document> additionalDefinitions = semanticMatches.stream()
                .filter(doc -> !alreadyIncludedSources.contains(getSourceName(doc)))
                .toList();

        log.debug("Found {} additional semantic definitions (total matches: {}, already included: {})",
                additionalDefinitions.size(), semanticMatches.size(), alreadyIncludedSources.size());

        return additionalDefinitions;
    }

    private List<Document> performSemanticDefinitionSearch(String question) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();

        SearchRequest searchRequest = SearchRequest.builder()
                .query(question)
                .topK(MAX_SEMANTIC_DEFINITIONS)
                .similarityThreshold(SEMANTIC_SIMILARITY_THRESHOLD)
                .filterExpression(builder.eq("type", "definition").build())
                .build();

        return vectorStore.similaritySearch(searchRequest);
    }

    private Set<String> extractSourceNames(List<Document> documents) {
        return documents.stream()
                .map(this::getSourceName)
                .filter(source -> !source.isEmpty())
                .collect(Collectors.toSet());
    }

    private String getSourceName(Document doc) {
        Object source = doc.getMetadata().get("source");
        return source != null ? String.valueOf(source) : "";
    }
}