package com.project.navi.telegram;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramReplySenderTest {

    @Mock
    private TelegramClient telegramClient;

    @Test
    void sendsReplyAndReturnsSentMessageId() throws TelegramApiException {
        when(telegramClient.execute(any(SendMessage.class))).thenReturn(Message.builder().messageId(999).build());

        TelegramReplySender sender = new TelegramReplySender(Optional.of(telegramClient));

        Optional<Integer> result = sender.reply(123L, 600, "Pode reformular?");

        assertThat(result).contains(999);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramClient).execute(captor.capture());

        SendMessage sent = captor.getValue();
        assertThat(sent.getChatId()).isEqualTo("123");
        assertThat(sent.getReplyToMessageId()).isEqualTo(600);
        assertThat(sent.getText()).isEqualTo("Pode reformular?");
    }

    @Test
    void sendsStandaloneMessageWhenReplyToMessageIdIsNull() throws TelegramApiException {
        when(telegramClient.execute(any(SendMessage.class))).thenReturn(Message.builder().messageId(700).build());

        TelegramReplySender sender = new TelegramReplySender(Optional.of(telegramClient));

        Optional<Integer> result = sender.reply(123L, null, "💧 Bora beber água hoje!");

        assertThat(result).contains(700);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramClient).execute(captor.capture());
        assertThat(captor.getValue().getReplyToMessageId()).isNull();
    }

    @Test
    void sendsMessageWithInlineKeyboardWhenProvided() throws TelegramApiException {
        when(telegramClient.execute(any(SendMessage.class))).thenReturn(Message.builder().messageId(800).build());
        InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder().build();

        TelegramReplySender sender = new TelegramReplySender(Optional.of(telegramClient));

        Optional<Integer> result = sender.reply(123L, 600, "Qual hábito é esse?", keyboard);

        assertThat(result).contains(800);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramClient).execute(captor.capture());
        assertThat(captor.getValue().getReplyMarkup()).isSameAs(keyboard);
    }

    @Test
    void returnsEmptyWhenClientIsNotConfigured() {
        TelegramReplySender sender = new TelegramReplySender(Optional.empty());

        assertThat(sender.reply(123L, 600, "Pode reformular?")).isEmpty();
    }

    @Test
    void returnsEmptyWhenSendFails() throws TelegramApiException {
        when(telegramClient.execute(any(SendMessage.class))).thenThrow(new TelegramApiException("boom"));

        TelegramReplySender sender = new TelegramReplySender(Optional.of(telegramClient));

        assertThat(sender.reply(123L, 600, "Pode reformular?")).isEmpty();
    }
}
