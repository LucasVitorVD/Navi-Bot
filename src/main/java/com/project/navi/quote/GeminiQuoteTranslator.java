package com.project.navi.quote;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class GeminiQuoteTranslator {

    private static final Logger log = LoggerFactory.getLogger(GeminiQuoteTranslator.class);

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeminiQuoteTranslator(RestClient.Builder restClientBuilder,
                                  @Value("${GEMINI_API_KEY:}") String apiKey,
                                  @Value("${gemini.model:gemini-2.5-flash}") String model) {
        this.restClient = restClientBuilder.baseUrl("https://generativelanguage.googleapis.com").build();
        this.apiKey = apiKey;
        this.model = model;
    }

    public Optional<String> translateToPortuguese(String text) {
        if (text == null || text.isBlank() || apiKey.isBlank()) {
            return Optional.empty();
        }

        try {
            GeminiResponse response = restClient.post()
                    .uri("/v1beta/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildRequest(text))
                    .retrieve()
                    .body(GeminiResponse.class);

            return parseTranslation(response);
        } catch (RestClientException e) {
            log.warn("Falha ao chamar a API do Gemini para traduzir a frase motivacional", e);
            return Optional.empty();
        }
    }

    private GeminiRequest buildRequest(String text) {
        String prompt = """
                Traduza a frase a seguir para português do Brasil, mantendo o tom motivacional e o sentido \
                original. Responda somente com a tradução, sem aspas nem comentários adicionais.

                Frase: "%s"
                """.formatted(text);

        Map<String, Object> schema = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "translation", Map.of("type", "STRING", "nullable", true)
                )
        );

        return new GeminiRequest(
                List.of(new GeminiRequest.Content("user", List.of(new GeminiRequest.Part(prompt)))),
                new GeminiRequest.GenerationConfig("application/json", schema)
        );
    }

    private Optional<String> parseTranslation(GeminiResponse response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            return Optional.empty();
        }

        String json = response.candidates().get(0).content().parts().get(0).text();
        try {
            JsonNode node = objectMapper.readTree(json);
            JsonNode translation = node.get("translation");
            if (translation == null || translation.isNull() || !translation.isTextual() || translation.asText().isBlank()) {
                return Optional.empty();
            }
            return Optional.of(translation.asText());
        } catch (Exception e) {
            log.warn("Resposta do Gemini não pôde ser interpretada como JSON: {}", json);
            return Optional.empty();
        }
    }
}
