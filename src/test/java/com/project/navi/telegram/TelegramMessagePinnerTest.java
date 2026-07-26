package com.project.navi.telegram;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.PinChatMessage;
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.UnpinChatMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramMessagePinnerTest {

    @Mock
    private TelegramClient telegramClient;

    @Test
    void pinsMessageWhenClientIsConfigured() throws TelegramApiException {
        TelegramMessagePinner pinner = new TelegramMessagePinner(Optional.of(telegramClient));

        pinner.pin(999L, 600);

        ArgumentCaptor<PinChatMessage> captor = ArgumentCaptor.forClass(PinChatMessage.class);
        verify(telegramClient).execute(captor.capture());
        assertThat(captor.getValue().getChatId()).isEqualTo("999");
        assertThat(captor.getValue().getMessageId()).isEqualTo(600);
    }

    @Test
    void unpinsMessageWhenClientIsConfigured() throws TelegramApiException {
        TelegramMessagePinner pinner = new TelegramMessagePinner(Optional.of(telegramClient));

        pinner.unpin(999L, 600);

        ArgumentCaptor<UnpinChatMessage> captor = ArgumentCaptor.forClass(UnpinChatMessage.class);
        verify(telegramClient).execute(captor.capture());
        assertThat(captor.getValue().getChatId()).isEqualTo("999");
        assertThat(captor.getValue().getMessageId()).isEqualTo(600);
    }

    @Test
    void doesNothingWhenClientIsNotConfigured() {
        TelegramMessagePinner pinner = new TelegramMessagePinner(Optional.empty());

        assertThatCode(() -> pinner.pin(999L, 600)).doesNotThrowAnyException();
        assertThatCode(() -> pinner.unpin(999L, 600)).doesNotThrowAnyException();
    }

    @Test
    void swallowsTelegramApiExceptionOnPinFailure() throws TelegramApiException {
        when(telegramClient.execute(any(PinChatMessage.class))).thenThrow(new TelegramApiException("boom"));

        TelegramMessagePinner pinner = new TelegramMessagePinner(Optional.of(telegramClient));

        assertThatCode(() -> pinner.pin(999L, 600)).doesNotThrowAnyException();
    }

    @Test
    void swallowsTelegramApiExceptionOnUnpinFailure() throws TelegramApiException {
        when(telegramClient.execute(any(UnpinChatMessage.class))).thenThrow(new TelegramApiException("boom"));

        TelegramMessagePinner pinner = new TelegramMessagePinner(Optional.of(telegramClient));

        assertThatCode(() -> pinner.unpin(999L, 600)).doesNotThrowAnyException();
    }
}
