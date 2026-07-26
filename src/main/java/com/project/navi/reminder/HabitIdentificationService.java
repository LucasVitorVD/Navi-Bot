package com.project.navi.reminder;

import com.project.navi.domain.Habit;
import com.project.navi.domain.HabitReminderMessage;
import com.project.navi.repository.HabitReminderMessageRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class HabitIdentificationService {

    private final HabitReminderMessageRepository habitReminderMessageRepository;

    public HabitIdentificationService(HabitReminderMessageRepository habitReminderMessageRepository) {
        this.habitReminderMessageRepository = habitReminderMessageRepository;
    }

    public Optional<Habit> identifyHabit(Long repliedToTelegramMessageId) {
        return habitReminderMessageRepository.findByTelegramMessageId(repliedToTelegramMessageId)
                .map(HabitReminderMessage::getHabit);
    }
}
