package com.project.navi.quote;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TranslatedQuoteProviderTest {

    @Mock
    private AnimeChanQuoteProvider delegate;

    @Mock
    private GeminiQuoteTranslator translator;

    private TranslatedQuoteProvider provider() {
        return new TranslatedQuoteProvider(delegate, translator);
    }

    @Test
    void returnsQuoteWithTranslatedContentWhenTranslationSucceeds() {
        Quote original = new Quote("Believe it!", "Naruto Uzumaki", "Naruto");
        when(delegate.fetch()).thenReturn(Optional.of(original));
        when(translator.translateToPortuguese("Believe it!")).thenReturn(Optional.of("Acredite!"));

        Optional<Quote> result = provider().fetch();

        assertThat(result).contains(new Quote("Acredite!", "Naruto Uzumaki", "Naruto"));
    }

    @Test
    void returnsOriginalQuoteWhenTranslationFails() {
        Quote original = new Quote("Believe it!", "Naruto Uzumaki", "Naruto");
        when(delegate.fetch()).thenReturn(Optional.of(original));
        when(translator.translateToPortuguese("Believe it!")).thenReturn(Optional.empty());

        Optional<Quote> result = provider().fetch();

        assertThat(result).contains(original);
    }

    @Test
    void returnsEmptyWithoutTranslatingWhenDelegateHasNoQuote() {
        when(delegate.fetch()).thenReturn(Optional.empty());

        Optional<Quote> result = provider().fetch();

        assertThat(result).isEmpty();
        verify(translator, never()).translateToPortuguese(org.mockito.ArgumentMatchers.any());
    }
}
