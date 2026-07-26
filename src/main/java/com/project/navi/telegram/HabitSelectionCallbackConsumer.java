package com.project.navi.telegram;

import com.project.navi.domain.Habit;
import com.project.navi.domain.PendingHabitSelection;
import com.project.navi.repository.HabitRepository;
import com.project.navi.repository.PendingHabitSelectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.Optional;

/**
 * Consome o clique nos botões enviados por {@link HabitSelectionPrompter}: resolve a
 * seleção pendente, registra o hábito via {@link HabitRegistrationService} e limpa a
 * seleção consumida.
 */
@Component
public class HabitSelectionCallbackConsumer {

    private static final Logger log = LoggerFactory.getLogger(HabitSelectionCallbackConsumer.class);
    private static final String CALLBACK_PREFIX = "habit-select:";

    private final PendingHabitSelectionRepository pendingHabitSelectionRepository;
    private final HabitRepository habitRepository;
    private final HabitRegistrationService habitRegistrationService;
    private final TelegramClient telegramClient;

    public HabitSelectionCallbackConsumer(PendingHabitSelectionRepository pendingHabitSelectionRepository,
                                           HabitRepository habitRepository,
                                           HabitRegistrationService habitRegistrationService,
                                           Optional<TelegramClient> telegramClient) {
        this.pendingHabitSelectionRepository = pendingHabitSelectionRepository;
        this.habitRepository = habitRepository;
        this.habitRegistrationService = habitRegistrationService;
        this.telegramClient = telegramClient.orElse(null);
    }

    public void consume(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        if (callbackQuery == null || callbackQuery.getData() == null || !callbackQuery.getData().startsWith(CALLBACK_PREFIX)) {
            return;
        }

        String[] parts = callbackQuery.getData().substring(CALLBACK_PREFIX.length()).split(":");
        if (parts.length != 2) {
            return;
        }

        Optional<Long> pendingId = parseLong(parts[0]);
        Optional<Long> habitId = parseLong(parts[1]);
        if (pendingId.isEmpty() || habitId.isEmpty()) {
            return;
        }

        Optional<PendingHabitSelection> pendingOpt = pendingHabitSelectionRepository.findById(pendingId.get());
        Optional<Habit> habitOpt = habitRepository.findById(habitId.get());
        if (pendingOpt.isEmpty() || habitOpt.isEmpty()) {
            answerCallback(callbackQuery.getId(), "Isso já foi processado ou expirou.");
            return;
        }

        PendingHabitSelection pending = pendingOpt.get();
        User tapper = callbackQuery.getFrom();
        if (!pending.getTelegramUserId().equals(tapper.getId())) {
            answerCallback(callbackQuery.getId(), "Essa pergunta não é sua!");
            return;
        }

        habitRegistrationService.register(tapper, habitOpt.get(), pending.getCaptionText(),
                pending.getTelegramPhotoFileId(), pending.getTelegramChatId(), pending.getOriginalMessageId());

        pendingHabitSelectionRepository.deleteById(pending.getId());
        answerCallback(callbackQuery.getId(), null);
    }

    private Optional<Long> parseLong(String value) {
        try {
            return Optional.of(Long.valueOf(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private void answerCallback(String callbackQueryId, String text) {
        if (telegramClient == null) {
            return;
        }
        try {
            telegramClient.execute(AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackQueryId)
                    .text(text)
                    .build());
        } catch (TelegramApiException e) {
            log.warn("Falha ao responder callback_query (id={})", callbackQueryId, e);
        }
    }
}
