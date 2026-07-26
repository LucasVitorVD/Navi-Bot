package com.project.navi.telegram;

import com.project.navi.domain.Habit;
import com.project.navi.reminder.HabitIdentificationService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.photo.PhotoSize;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class HabitReplyUpdateConsumer implements LongPollingSingleThreadUpdateConsumer {

    private final HabitIdentificationService habitIdentificationService;
    private final HabitRegistrationService habitRegistrationService;
    private final HabitSelectionPrompter habitSelectionPrompter;

    public HabitReplyUpdateConsumer(HabitIdentificationService habitIdentificationService,
                                     HabitRegistrationService habitRegistrationService,
                                     HabitSelectionPrompter habitSelectionPrompter) {
        this.habitIdentificationService = habitIdentificationService;
        this.habitRegistrationService = habitRegistrationService;
        this.habitSelectionPrompter = habitSelectionPrompter;
    }

    @Override
    public void consume(Update update) {
        Message message = update.getMessage();
        if (message == null || !message.hasPhoto()) {
            return;
        }

        String photoFileId = largestPhoto(message.getPhoto());

        if (!message.isReply()) {
            habitSelectionPrompter.prompt(message.getChatId(), message.getMessageId(), message.getFrom(),
                    photoFileId, message.getCaption());
            return;
        }

        long repliedToMessageId = message.getReplyToMessage().getMessageId().longValue();
        Optional<Habit> habit = habitIdentificationService.identifyHabit(repliedToMessageId);
        if (habit.isEmpty()) {
            return;
        }

        habitRegistrationService.register(message.getFrom(), habit.get(), message.getCaption(),
                photoFileId, message.getChatId(), message.getMessageId().longValue());
    }

    private String largestPhoto(List<PhotoSize> sizes) {
        return sizes.stream()
                .max(Comparator.comparing(PhotoSize::getWidth, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(PhotoSize::getFileId)
                .orElse(null);
    }
}
