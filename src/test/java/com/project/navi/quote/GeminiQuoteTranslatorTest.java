package com.project.navi.quote;

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

class GeminiQuoteTranslatorTest {

    private static final String EXPECTED_URI =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    private GeminiQuoteTranslator translator;
    private MockRestServiceServer server;

    private void setUp(String apiKey) {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        translator = new GeminiQuoteTranslator(builder, apiKey, "gemini-2.5-flash");
    }

    @Test
    void translatesTextFromSuccessfulResponse() {
        setUp("fake-api-key");
        server.expect(requestTo(EXPECTED_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "fake-api-key"))
                .andRespond(withSuccess("""
                        {"candidates":[{"content":{"parts":[{"text":"{\\"translation\\":\\"Acredite!\\"}"}]}}]}
                        """, MediaType.APPLICATION_JSON));

        Optional<String> result = translator.translateToPortuguese("Believe it!");

        assertThat(result).contains("Acredite!");
        server.verify();
    }

    @Test
    void returnsEmptyWhenGeminiReturnsNullTranslation() {
        setUp("fake-api-key");
        server.expect(requestTo(EXPECTED_URI))
                .andRespond(withSuccess("""
                        {"candidates":[{"content":{"parts":[{"text":"{\\"translation\\":null}"}]}}]}
                        """, MediaType.APPLICATION_JSON));

        assertThat(translator.translateToPortuguese("Believe it!")).isEmpty();
    }

    @Test
    void returnsEmptyOnHttpError() {
        setUp("fake-api-key");
        server.expect(requestTo(EXPECTED_URI))
                .andRespond(withServerError());

        assertThat(translator.translateToPortuguese("Believe it!")).isEmpty();
    }

    @Test
    void returnsEmptyOnMalformedJsonInResponseText() {
        setUp("fake-api-key");
        server.expect(requestTo(EXPECTED_URI))
                .andRespond(withSuccess("""
                        {"candidates":[{"content":{"parts":[{"text":"isso não é json"}]}}]}
                        """, MediaType.APPLICATION_JSON));

        assertThat(translator.translateToPortuguese("Believe it!")).isEmpty();
    }

    @Test
    void returnsEmptyWithoutCallingApiWhenKeyIsBlank() {
        setUp("");

        assertThat(translator.translateToPortuguese("Believe it!")).isEmpty();
    }

    @Test
    void returnsEmptyWithoutCallingApiWhenTextIsBlank() {
        setUp("fake-api-key");

        assertThat(translator.translateToPortuguese("   ")).isEmpty();
    }
}
