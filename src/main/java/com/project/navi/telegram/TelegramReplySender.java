package com.project.navi.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.Optional;

@Component
public class TelegramReplySender {

    private static final Logger log = LoggerFactory.getLogger(TelegramReplySender.class);

    private final TelegramClient telegramClient;

    public TelegramReplySender(Optional<TelegramClient> telegramClient) {
        this.telegramClient = telegramClient.orElse(null);
    }

    /**
     * Envia uma mensagem, opcionalmente como reply a outra (replyToMessageId pode ser null
     * para uma mensagem avulsa, como os lembretes enviados pelo scheduler). Retorna o id da
     * mensagem enviada, útil para rastreabilidade (ex: HabitReminderMessage).
     */
    public Optional<Integer> reply(Long chatId, Integer replyToMessageId, String text) {
        return reply(chatId, replyToMessageId, text, null);
    }

    /**
     * Mesmo comportamento de {@link #reply(Long, Integer, String)}, mas anexando um teclado
     * (ex: botões inline para o fallback de seleção de hábito).
     */
    public Optional<Integer> reply(Long chatId, Integer replyToMessageId, String text, ReplyKeyboard replyMarkup) {
        if (telegramClient == null) {
            log.warn("Bot do Telegram não configurado (TELEGRAM_BOT_TOKEN ausente); mensagem não enviada: {}", text);
            return Optional.empty();
        }

        try {
            Message sent = telegramClient.execute(SendMessage.builder()
                    .chatId(chatId.toString())
                    .replyToMessageId(replyToMessageId)
                    .text(text)
                    .replyMarkup(replyMarkup)
                    .build());
            return Optional.of(sent.getMessageId());
        } catch (TelegramApiException e) {
            log.warn("Falha ao enviar mensagem via Telegram", e);
            return Optional.empty();
        }
    }
}
