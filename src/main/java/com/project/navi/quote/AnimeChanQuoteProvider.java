package com.project.navi.quote;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

@Component
public class AnimeChanQuoteProvider implements MotivationalQuoteProvider {

    private static final Logger log = LoggerFactory.getLogger(AnimeChanQuoteProvider.class);

    private final RestClient restClient;

    public AnimeChanQuoteProvider(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.baseUrl("https://api.animechan.io").build();
    }

    @Override
    public Optional<Quote> fetch() {
        try {
            AnimeChanResponse response = restClient.get()
                    .uri("/v1/quotes/random")
                    .retrieve()
                    .body(AnimeChanResponse.class);

            if (response == null || response.data() == null || response.data().content() == null) {
                return Optional.empty();
            }

            AnimeChanResponse.Data data = response.data();
            return Optional.of(new Quote(
                    data.content(),
                    data.character() != null ? data.character().name() : null,
                    data.anime() != null ? data.anime().name() : null));
        } catch (RestClientException e) {
            log.warn("Falha ao buscar frase motivacional", e);
            return Optional.empty();
        }
    }
}
