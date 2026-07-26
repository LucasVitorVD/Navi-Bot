package com.project.navi.telegram;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramReplySenderTest {

    @Mock
    private TelegramClient telegramClient;

    @Test
    void sendsMessageWhenClientIsConfigured() throws TelegramApiException {
        TelegramReplySender sender = new TelegramReplySender(Optional.of(telegramClient));

        sender.reply(123L, 600, "Pode reformular?");

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramClient).execute(captor.capture());

        SendMessage sent = captor.getValue();
        assertThat(sent.getChatId()).isEqualTo("123");
        assertThat(sent.getReplyToMessageId()).isEqualTo(600);
        assertThat(sent.getText()).isEqualTo("Pode reformular?");
    }

    @Test
    void doesNothingWhenClientIsNotConfigured() {
        TelegramReplySender sender = new TelegramReplySender(Optional.empty());

        assertThatCode(() -> sender.reply(123L, 600, "Pode reformular?")).doesNotThrowAnyException();
    }

    @Test
    void swallowsTelegramApiExceptionOnSendFailure() throws TelegramApiException {
        when(telegramClient.execute(org.mockito.ArgumentMatchers.any(SendMessage.class)))
                .thenThrow(new TelegramApiException("boom"));

        TelegramReplySender sender = new TelegramReplySender(Optional.of(telegramClient));

        assertThatCode(() -> sender.reply(123L, 600, "Pode reformular?")).doesNotThrowAnyException();
    }
}
