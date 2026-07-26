package com.project.navi.telegram;

import com.project.navi.domain.Habit;
import com.project.navi.domain.HabitRecord;
import com.project.navi.domain.HabitType;
import com.project.navi.domain.User;
import com.project.navi.quantity.HabitQuantityInterpreter;
import com.project.navi.reminder.HabitIdentificationService;
import com.project.navi.repository.HabitRecordRepository;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.photo.PhotoSize;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class HabitReplyUpdateConsumer implements LongPollingSingleThreadUpdateConsumer {

    private final HabitIdentificationService habitIdentificationService;
    private final TelegramUserResolver telegramUserResolver;
    private final HabitQuantityInterpreter habitQuantityInterpreter;
    private final TelegramReplySender telegramReplySender;
    private final HabitRecordRepository habitRecordRepository;

    public HabitReplyUpdateConsumer(HabitIdentificationService habitIdentificationService,
                                     TelegramUserResolver telegramUserResolver,
                                     HabitQuantityInterpreter habitQuantityInterpreter,
                                     TelegramReplySender telegramReplySender,
                                     HabitRecordRepository habitRecordRepository) {
        this.habitIdentificationService = habitIdentificationService;
        this.telegramUserResolver = telegramUserResolver;
        this.habitQuantityInterpreter = habitQuantityInterpreter;
        this.telegramReplySender = telegramReplySender;
        this.habitRecordRepository = habitRecordRepository;
    }

    @Override
    public void consume(Update update) {
        Message message = update.getMessage();
        if (message == null || !message.isReply() || !message.hasPhoto()) {
            return;
        }

        long repliedToMessageId = message.getReplyToMessage().getMessageId().longValue();
        Optional<Habit> habit = habitIdentificationService.identifyHabit(repliedToMessageId);
        if (habit.isEmpty()) {
            return;
        }

        User user = telegramUserResolver.resolve(message.getFrom());
        Habit resolvedHabit = habit.get();

        Integer quantity = null;
        if (resolvedHabit.getType() == HabitType.CUMULATIVE) {
            Optional<Integer> interpreted = habitQuantityInterpreter.interpret(user, resolvedHabit, message.getCaption());
            if (interpreted.isEmpty()) {
                telegramReplySender.reply(message.getChatId(), message.getMessageId(),
                        habitQuantityInterpreter.failureMessageFor(resolvedHabit));
                return;
            }
            quantity = interpreted.get();
        }

        habitRecordRepository.save(HabitRecord.builder()
                .user(user)
                .habit(resolvedHabit)
                .referenceDate(LocalDate.now())
                .createdAt(Instant.now())
                .captionText(message.getCaption())
                .extractedQuantity(quantity)
                .telegramPhotoFileId(largestPhoto(message.getPhoto()))
                .telegramMessageId(message.getMessageId().longValue())
                .build());
    }

    private String largestPhoto(List<PhotoSize> sizes) {
        return sizes.stream()
                .max(Comparator.comparing(PhotoSize::getWidth, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(PhotoSize::getFileId)
                .orElse(null);
    }
}
