package com.project.navi.quote;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Envolve o provider de frases (hoje só {@link AnimeChanQuoteProvider}) e traduz o conteúdo
 * para português via Gemini antes de repassar. Se a tradução falhar, cai de volta na frase
 * original em vez de esconder a citação inteira.
 */
@Component
@Primary
public class TranslatedQuoteProvider implements MotivationalQuoteProvider {

    private final AnimeChanQuoteProvider delegate;
    private final GeminiQuoteTranslator translator;

    public TranslatedQuoteProvider(AnimeChanQuoteProvider delegate, GeminiQuoteTranslator translator) {
        this.delegate = delegate;
        this.translator = translator;
    }

    @Override
    public Optional<Quote> fetch() {
        return delegate.fetch().map(quote -> translator.translateToPortuguese(quote.content())
                .map(translated -> new Quote(translated, quote.character(), quote.source()))
                .orElse(quote));
    }
}
