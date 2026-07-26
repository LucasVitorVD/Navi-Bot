package com.project.navi.quote;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AnimeChanQuoteProviderTest {

    private static final String EXPECTED_URI = "https://api.animechan.io/v1/quotes/random";

    private AnimeChanQuoteProvider provider;
    private MockRestServiceServer server;

    private void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        provider = new AnimeChanQuoteProvider(builder);
    }

    @Test
    void returnsQuoteFromSuccessfulResponse() {
        setUp();
        server.expect(requestTo(EXPECTED_URI))
                .andRespond(withSuccess("""
                        {"status":"success","data":{"content":"Believe it!",
                        "anime":{"name":"Naruto"},"character":{"name":"Naruto Uzumaki"}}}
                        """, MediaType.APPLICATION_JSON));

        Optional<Quote> quote = provider.fetch();

        assertThat(quote).isPresent();
        assertThat(quote.get().content()).isEqualTo("Believe it!");
        assertThat(quote.get().character()).isEqualTo("Naruto Uzumaki");
        assertThat(quote.get().source()).isEqualTo("Naruto");
    }

    @Test
    void returnsEmptyOnHttpError() {
        setUp();
        server.expect(requestTo(EXPECTED_URI)).andRespond(withServerError());

        assertThat(provider.fetch()).isEmpty();
    }

    @Test
    void returnsEmptyWhenContentIsMissing() {
        setUp();
        server.expect(requestTo(EXPECTED_URI))
                .andRespond(withSuccess("{\"status\":\"success\",\"data\":null}", MediaType.APPLICATION_JSON));

        assertThat(provider.fetch()).isEmpty();
    }
}
