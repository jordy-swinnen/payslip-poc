package com.app.payslip.poc.service;

import com.app.payslip.poc.config.PromptConfigProperties;
import com.app.payslip.poc.model.ExtractedPayslipDataDTO;
import com.app.payslip.poc.model.PayslipAskResponseDTO;
import com.app.payslip.poc.model.PayslipAskResponseDTO.PayslipAskCitationDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayslipAskService {

    private static final double PAYSLIP_SIMILARITY_THRESHOLD = 0.55;
    private static final int PAYSLIP_TOP_K = 6;
    private static final double DEFINITION_SIMILARITY_THRESHOLD = 0.50;
    private static final int DEFINITION_TOP_K = 4;
    private static final String DOCUMENT_SEPARATOR = "\n---\n";
    private static final String UNKNOWN_DOC_ID = "unknown";
    private static final String TYPE_DEFINITION = "definition";
    private static final int TEXT_PREVIEW_LENGTH = 120;

    private final PayslipExtractionService extractionService;
    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final PromptConfigProperties promptConfig;
    private final SemanticDefinitionMatcher semanticDefinitionMatcher;

    public PayslipAskResponseDTO ask(MultipartFile payslipFile, String question) throws IOException {
        log.info("Processing payslip question: {}", question);

        ExtractedPayslipDataDTO extractedData = extractionService.scrapeAndIndexPayslip(payslipFile);
        PayslipIdentifiers identifiers = PayslipIdentifiers.from(extractedData);

        List<Document> retrievedDocuments = retrieveRelevantDocuments(question, identifiers);
        ChatResponse chatResponse = generateAnswer(question, retrievedDocuments, identifiers);

        return buildAnswerResponse(chatResponse, retrievedDocuments);
    }

    private List<Document> retrieveRelevantDocuments(String question, PayslipIdentifiers identifiers) {
        List<Document> payslipDocuments = retrievePayslipDocuments(question, identifiers);
        List<Document> definitionDocuments = retrieveDefinitionDocuments(question);

        List<Document> semanticDefinitions = semanticDefinitionMatcher.findRequiredDefinitions(
                question,
                definitionDocuments
        );

        return mergeAndDeduplicateDocuments(payslipDocuments, definitionDocuments, semanticDefinitions);
    }

    private List<Document> retrievePayslipDocuments(String question, PayslipIdentifiers identifiers) {
        DocumentRetriever retriever = createPayslipRetriever(identifiers);
        return retriever.retrieve(new Query(question));
    }

    private List<Document> retrieveDefinitionDocuments(String question) {
        DocumentRetriever retriever = createDefinitionRetriever();
        return retriever.retrieve(new Query(question));
    }

    private List<Document> mergeAndDeduplicateDocuments(
            List<Document> payslipDocs,
            List<Document> definitionDocs,
            List<Document> semanticDefs
    ) {
        boolean noPayslipMatch = payslipDocs.isEmpty();

        List<Document> combined = new ArrayList<>();
        if (noPayslipMatch && !semanticDefs.isEmpty()) {
            combined.addAll(semanticDefs);
        } else {
            combined.addAll(payslipDocs);
            combined.addAll(definitionDocs);
            combined.addAll(semanticDefs);
        }

        return deduplicateByDocId(combined);
    }

    private List<Document> deduplicateByDocId(List<Document> documents) {
        Map<String, Document> byDocId = new LinkedHashMap<>();
        for (Document doc : documents) {
            String docId = extractDocId(doc.getMetadata());
            byDocId.putIfAbsent(docId, doc);
        }
        return new ArrayList<>(byDocId.values());
    }

    private DocumentRetriever createPayslipRetriever(PayslipIdentifiers identifiers) {
        Filter.Expression filter = buildPayslipFilter(identifiers);
        return VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(PAYSLIP_SIMILARITY_THRESHOLD)
                .topK(PAYSLIP_TOP_K)
                .filterExpression(filter)
                .build();
    }

    private Filter.Expression buildPayslipFilter(PayslipIdentifiers identifiers) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();

        FilterExpressionBuilder.Op idFilter = builder.or(
                builder.eq("personal.nationalId", identifiers.nationalId()),
                builder.eq("employment.employeeNumber", identifiers.employeeNumber())
        );

        return builder.and(idFilter, builder.eq("period.monthKey", identifiers.monthKey())).build();
    }

    private DocumentRetriever createDefinitionRetriever() {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        return VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(DEFINITION_SIMILARITY_THRESHOLD)
                .topK(DEFINITION_TOP_K)
                .filterExpression(builder.eq("type", TYPE_DEFINITION).build())
                .build();
    }

    private ChatResponse generateAnswer(String question, List<Document> documents, PayslipIdentifiers identifiers) {
        DocumentRetriever combinedRetriever = createCombinedRetriever(identifiers);
        RetrievalAugmentationAdvisor advisor = createRetrievalAdvisor(combinedRetriever);

        String contextBlock = buildContextBlock(documents);
        String citationsText = buildCitationsText(documents);

        return chatClient.prompt()
                .advisors(advisor)
                .system(promptConfig.getPayslip().getSystemAsk())
                .advisors(advisorSpec -> advisorSpec.param("question_answer_context", contextBlock))
                .advisors(advisorSpec -> advisorSpec.param("citations", citationsText))
                .user(question)
                .call()
                .chatResponse();
    }

    private RetrievalAugmentationAdvisor createRetrievalAdvisor(DocumentRetriever retriever) {
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(retriever)
                .queryAugmenter(ContextualQueryAugmenter.builder().allowEmptyContext(false).build())
                .build();
    }

    private DocumentRetriever createCombinedRetriever(PayslipIdentifiers identifiers) {
        DocumentRetriever payslipRetriever = createPayslipRetriever(identifiers);
        DocumentRetriever definitionRetriever = createDefinitionRetriever();

        return query -> {
            List<Document> combined = new ArrayList<>();
            combined.addAll(payslipRetriever.retrieve(query));
            combined.addAll(definitionRetriever.retrieve(query));
            return combined;
        };
    }

    private String buildContextBlock(List<Document> documents) {
        return documents.stream()
                .map(Document::getText)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(text -> !text.isEmpty())
                .collect(Collectors.joining(DOCUMENT_SEPARATOR));
    }

    private String buildCitationsText(List<Document> documents) {
        return documents.stream()
                .map(doc -> extractDocId(doc.getMetadata()))
                .filter(docId -> !docId.isEmpty())
                .distinct()
                .collect(Collectors.joining(", "));
    }

    private PayslipAskResponseDTO buildAnswerResponse(ChatResponse chatResponse, List<Document> documents) {
        String answerText = extractAnswerText(chatResponse);
        List<PayslipAskCitationDTO> citations = buildCitations(documents);

        return new PayslipAskResponseDTO(answerText, citations);
    }

    private String extractAnswerText(ChatResponse chatResponse) {
        return Optional.ofNullable(chatResponse)
                .map(ChatResponse::getResult)
                .map(result -> result.getOutput().getText())
                .orElse("");
    }

    private List<PayslipAskCitationDTO> buildCitations(List<Document> documents) {
        return documents.stream()
                .map(this::createCitation)
                .distinct()
                .toList();
    }

    private PayslipAskCitationDTO createCitation(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        String docId = extractDocId(metadata);

        return new PayslipAskCitationDTO(
                docId,
                getMetadataAsString(metadata, "section"),
                getMetadataAsString(metadata, "source"),
                getMetadataAsString(metadata, "period.monthKey"),
                getMetadataAsString(metadata, "employer.number"),
                getMetadataAsString(metadata, "employment.employeeNumber"),
                getMetadataAsString(metadata, "personal.nationalId"),
                extractTextPreview(document.getText()),
                buildSectionUrl(docId)
        );
    }

    private String extractTextPreview(String text) {
        if (text == null) {
            return "";
        }
        return text.substring(0, Math.min(text.length(), TEXT_PREVIEW_LENGTH));
    }

    private String buildSectionUrl(String docId) {
        return "/api/payslip/section/" + docId;
    }

    private String extractDocId(Map<String, Object> metadata) {
        return getMetadataAsString(metadata, "docId", UNKNOWN_DOC_ID);
    }

    private String getMetadataAsString(Map<String, Object> metadata, String key) {
        return getMetadataAsString(metadata, key, "");
    }

    private String getMetadataAsString(Map<String, Object> metadata, String key, String defaultValue) {
        return Optional.ofNullable(metadata.get(key))
                .map(String::valueOf)
                .orElse(defaultValue);
    }

    record PayslipIdentifiers(String nationalId, String employeeNumber, String monthKey) {
        static PayslipIdentifiers from(ExtractedPayslipDataDTO dto) {
            String nationalId = Optional.ofNullable(dto.personal())
                    .map(ExtractedPayslipDataDTO.PersonalInfo::nationalId)
                    .orElse("");

            String employeeNumber = Optional.ofNullable(dto.employment())
                    .map(ExtractedPayslipDataDTO.EmploymentInfo::employeeNumber)
                    .orElse("");

            String monthKey = Optional.ofNullable(dto.period())
                    .map(ExtractedPayslipDataDTO.PeriodInfo::periodStart)
                    .map(date -> "%d-%02d".formatted(date.getYear(), date.getMonthValue()))
                    .orElse("");

            return new PayslipIdentifiers(nationalId, employeeNumber, monthKey);
        }
    }
}