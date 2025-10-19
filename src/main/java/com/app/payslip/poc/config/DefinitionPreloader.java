package com.app.payslip.poc.config;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class DefinitionPreloader {

    @Bean
    ApplicationRunner preloadDefinitions(VectorStore vectorStore) {
        return args -> {
            List<Document> documents = new ArrayList<>();
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath:rag/definitions/*.*");
            for (Resource resource : resources) {
                String text = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                Map<String, Object> metadata = new LinkedHashMap<>();
                metadata.put("type", "definition");
                metadata.put("topic", topicFromName(resource.getFilename()));
                metadata.put("source", resource.getFilename());
                documents.add(new Document(text, metadata));
            }
            if (!documents.isEmpty()) {
                vectorStore.add(documents);
            }
        };
    }

    private String topicFromName(String fileName) {
        if (fileName == null) return "general";
        String base = fileName.replaceFirst("\\.[^.]+$", "");
        return base.replace('_', '-').toLowerCase();
    }
}
