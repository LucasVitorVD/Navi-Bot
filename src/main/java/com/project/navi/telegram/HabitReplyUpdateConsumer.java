package com.project.navi.telegram;

import com.project.navi.domain.Habit;
import com.project.navi.domain.HabitRecord;
import com.project.navi.domain.User;
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
    private final HabitRecordRepository habitRecordRepository;

    public HabitReplyUpdateConsumer(HabitIdentificationService habitIdentificationService,
                                     TelegramUserResolver telegramUserResolver,
                                     HabitRecordRepository habitRecordRepository) {
        this.habitIdentificationService = habitIdentificationService;
        this.telegramUserResolver = telegramUserResolver;
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

        habitRecordRepository.save(HabitRecord.builder()
                .user(user)
                .habit(habit.get())
                .referenceDate(LocalDate.now())
                .createdAt(Instant.now())
                .captionText(message.getCaption())
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
