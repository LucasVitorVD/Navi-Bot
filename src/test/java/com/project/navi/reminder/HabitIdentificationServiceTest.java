package com.project.navi.reminder;

import com.project.navi.domain.Habit;
import com.project.navi.domain.HabitReminderMessage;
import com.project.navi.domain.HabitType;
import com.project.navi.repository.HabitReminderMessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HabitIdentificationServiceTest {

    @Mock
    private HabitReminderMessageRepository habitReminderMessageRepository;

    @Test
    void identifiesHabitWhenReplyMatchesAKnownReminderMessage() {
        Habit habit = Habit.builder()
                .id(1L)
                .name("Água")
                .type(HabitType.CUMULATIVE)
                .unit("ml")
                .target(3000)
                .build();

        HabitReminderMessage reminderMessage = HabitReminderMessage.builder()
                .habit(habit)
                .telegramMessageId(555L)
                .referenceDate(LocalDate.of(2026, 7, 27))
                .build();

        when(habitReminderMessageRepository.findByTelegramMessageId(555L))
                .thenReturn(Optional.of(reminderMessage));

        HabitIdentificationService service = new HabitIdentificationService(habitReminderMessageRepository);

        Optional<Habit> identified = service.identifyHabit(555L);

        assertThat(identified).contains(habit);
    }

    @Test
    void returnsEmptyWhenReplyDoesNotMatchAnyReminderMessage() {
        when(habitReminderMessageRepository.findByTelegramMessageId(999L))
                .thenReturn(Optional.empty());

        HabitIdentificationService service = new HabitIdentificationService(habitReminderMessageRepository);

        assertThat(service.identifyHabit(999L)).isEmpty();
    }
}
