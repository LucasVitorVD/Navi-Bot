package com.project.navi.quantity;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiApiMinutesExtractorTest {

    private static final String EXPECTED_URI =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent";

    private GeminiApiMinutesExtractor extractor;
    private MockRestServiceServer server;

    private void setUp(String apiKey) {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        extractor = new GeminiApiMinutesExtractor(builder, apiKey, "gemini-2.5-flash-lite");
    }

    @Test
    void extractsMinutesFromSuccessfulResponse() {
        setUp("fake-api-key");
        server.expect(requestTo(EXPECTED_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "fake-api-key"))
                .andRespond(withSuccess("""
                        {"candidates":[{"content":{"parts":[{"text":"{\\"minutes\\":40}"}]}}]}
                        """, MediaType.APPLICATION_JSON));

        Optional<Integer> result = extractor.extractMinutes("estudei 40 minutos de java");

        assertThat(result).contains(40);
        server.verify();
    }

    @Test
    void returnsEmptyWhenGeminiReturnsNullMinutes() {
        setUp("fake-api-key");
        server.expect(requestTo(EXPECTED_URI))
                .andRespond(withSuccess("""
                        {"candidates":[{"content":{"parts":[{"text":"{\\"minutes\\":null}"}]}}]}
                        """, MediaType.APPLICATION_JSON));

        assertThat(extractor.extractMinutes("oi")).isEmpty();
    }

    @Test
    void returnsEmptyOnHttpError() {
        setUp("fake-api-key");
        server.expect(requestTo(EXPECTED_URI))
                .andRespond(withServerError());

        assertThat(extractor.extractMinutes("estudei bastante hoje")).isEmpty();
    }

    @Test
    void returnsEmptyOnMalformedJsonInResponseText() {
        setUp("fake-api-key");
        server.expect(requestTo(EXPECTED_URI))
                .andRespond(withSuccess("""
                        {"candidates":[{"content":{"parts":[{"text":"isso não é json"}]}}]}
                        """, MediaType.APPLICATION_JSON));

        assertThat(extractor.extractMinutes("estudei bastante hoje")).isEmpty();
    }

    @Test
    void returnsEmptyWithoutCallingApiWhenKeyIsBlank() {
        setUp("");

        assertThat(extractor.extractMinutes("estudei 40 minutos")).isEmpty();
    }

    @Test
    void returnsEmptyWithoutCallingApiWhenCaptionIsBlank() {
        setUp("fake-api-key");

        assertThat(extractor.extractMinutes("   ")).isEmpty();
    }
}
