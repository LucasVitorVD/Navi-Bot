package com.project.navi.telegram;

import com.project.navi.domain.Habit;
import com.project.navi.domain.HabitRecord;
import com.project.navi.domain.HabitType;
import com.project.navi.domain.User;
import com.project.navi.photo.PhotoStorage;
import com.project.navi.progress.HabitProgressCalculator;
import com.project.navi.quantity.HabitQuantityInterpreter;
import com.project.navi.repository.HabitRecordRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

/**
 * Registra um hábito a partir de uma foto (com legenda opcional) já associada a um
 * {@link Habit} conhecido — seja porque a reply identificou o hábito diretamente
 * (Etapa 4), seja porque a pessoa escolheu pelo fluxo de fallback com botões.
 */
@Component
public class HabitRegistrationService {

    private static final LocalTime DAILY_SUMMARY_TIME = LocalTime.of(23, 30);

    private final TelegramUserResolver telegramUserResolver;
    private final HabitQuantityInterpreter habitQuantityInterpreter;
    private final HabitProgressCalculator habitProgressCalculator;
    private final HabitRecordConfirmationMessageFormatter confirmationMessageFormatter;
    private final TelegramReplySender telegramReplySender;
    private final HabitRecordRepository habitRecordRepository;
    private final PhotoStorage photoStorage;
    private final Clock clock;

    public HabitRegistrationService(TelegramUserResolver telegramUserResolver,
                                     HabitQuantityInterpreter habitQuantityInterpreter,
                                     HabitProgressCalculator habitProgressCalculator,
                                     HabitRecordConfirmationMessageFormatter confirmationMessageFormatter,
                                     TelegramReplySender telegramReplySender,
                                     HabitRecordRepository habitRecordRepository,
                                     PhotoStorage photoStorage,
                                     Clock clock) {
        this.telegramUserResolver = telegramUserResolver;
        this.habitQuantityInterpreter = habitQuantityInterpreter;
        this.habitProgressCalculator = habitProgressCalculator;
        this.confirmationMessageFormatter = confirmationMessageFormatter;
        this.telegramReplySender = telegramReplySender;
        this.habitRecordRepository = habitRecordRepository;
        this.photoStorage = photoStorage;
        this.clock = clock;
    }

    public void register(org.telegram.telegrambots.meta.api.objects.User sender, Habit habit, String captionText,
                          String photoFileId, Long chatId, Long messageId) {
        User user = telegramUserResolver.resolve(sender);

        Integer quantity = null;
        if (habit.getType() == HabitType.CUMULATIVE) {
            Optional<Integer> interpreted = habitQuantityInterpreter.interpret(user, habit, captionText);
            if (interpreted.isEmpty()) {
                telegramReplySender.reply(chatId, messageId.intValue(), habitQuantityInterpreter.failureMessageFor(habit));
                return;
            }
            quantity = interpreted.get();
        }

        LocalDate referenceDate = LocalDate.now(clock);
        String localPhotoPath = photoStorage.download(photoFileId, referenceDate, messageId).orElse(null);

        habitRecordRepository.save(HabitRecord.builder()
                .user(user)
                .habit(habit)
                .referenceDate(referenceDate)
                .createdAt(Instant.now(clock))
                .captionText(captionText)
                .extractedQuantity(quantity)
                .telegramPhotoFileId(photoFileId)
                .localPhotoPath(localPhotoPath)
                .telegramMessageId(messageId)
                .build());

        int remaining = habit.getType() == HabitType.CUMULATIVE
                ? habitProgressCalculator.remaining(user, habit, referenceDate)
                : 0;
        boolean afterDailySummary = !LocalTime.now(clock).isBefore(DAILY_SUMMARY_TIME);
        telegramReplySender.reply(chatId, messageId.intValue(),
                confirmationMessageFormatter.confirmationFor(user, habit, quantity, remaining, afterDailySummary));
    }
}
