package com.project.navi.quantity;

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
public class GeminiApiMinutesExtractor implements GeminiMinutesExtractor {

    private static final Logger log = LoggerFactory.getLogger(GeminiApiMinutesExtractor.class);

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeminiApiMinutesExtractor(RestClient.Builder restClientBuilder,
                                      @Value("${GEMINI_API_KEY:}") String apiKey,
                                      @Value("${gemini.model:gemini-2.5-flash}") String model) {
        this.restClient = restClientBuilder.baseUrl("https://generativelanguage.googleapis.com").build();
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public Optional<Integer> extractMinutes(String captionText) {
        if (captionText == null || captionText.isBlank() || apiKey.isBlank()) {
            return Optional.empty();
        }

        try {
            GeminiResponse response = restClient.post()
                    .uri("/v1beta/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildRequest(captionText))
                    .retrieve()
                    .body(GeminiResponse.class);

            return parseMinutes(response);
        } catch (RestClientException e) {
            log.warn("Falha ao chamar a API do Gemini para interpretar quantidade", e);
            return Optional.empty();
        }
    }

    private GeminiRequest buildRequest(String captionText) {
        String prompt = """
                Extraia a quantidade de minutos de exercício ou estudo mencionada no texto a seguir, \
                escrito por um usuário de um bot de hábitos em português. Responda somente com o número \
                inteiro de minutos. Se não for possível determinar um valor, responda com null.

                Texto: "%s"
                """.formatted(captionText);

        Map<String, Object> schema = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "minutes", Map.of("type", "INTEGER", "nullable", true)
                )
        );

        return new GeminiRequest(
                List.of(new GeminiRequest.Content("user", List.of(new GeminiRequest.Part(prompt)))),
                new GeminiRequest.GenerationConfig("application/json", schema)
        );
    }

    private Optional<Integer> parseMinutes(GeminiResponse response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            return Optional.empty();
        }

        String json = response.candidates().get(0).content().parts().get(0).text();
        try {
            JsonNode node = objectMapper.readTree(json);
            JsonNode minutes = node.get("minutes");
            if (minutes == null || minutes.isNull() || !minutes.isInt() || minutes.asInt() <= 0) {
                return Optional.empty();
            }
            return Optional.of(minutes.asInt());
        } catch (Exception e) {
            log.warn("Resposta do Gemini não pôde ser interpretada como JSON: {}", json);
            return Optional.empty();
        }
    }
}
