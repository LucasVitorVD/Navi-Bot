package com.project.navi.repository;

import com.project.navi.domain.Habit;
import com.project.navi.domain.HabitReminderMessage;
import com.project.navi.domain.HabitType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataAccessException;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class HabitReminderMessageRepositoryTest {

    @Autowired
    private HabitRepository habitRepository;

    @Autowired
    private HabitReminderMessageRepository habitReminderMessageRepository;

    @Test
    void savesAndFindsReminderMessageByTelegramMessageId() {
        Habit habit = habitRepository.save(Habit.builder()
                .name("Água")
                .type(HabitType.CUMULATIVE)
                .unit("ml")
                .target(3000)
                .build());

        habitReminderMessageRepository.save(HabitReminderMessage.builder()
                .habit(habit)
                .telegramMessageId(555L)
                .referenceDate(LocalDate.of(2026, 7, 27))
                .build());

        Optional<HabitReminderMessage> found = habitReminderMessageRepository.findByTelegramMessageId(555L);

        assertThat(found).isPresent();
        assertThat(found.get().getHabit().getId()).isEqualTo(habit.getId());
        assertThat(found.get().getReferenceDate()).isEqualTo(LocalDate.of(2026, 7, 27));
    }

    @Test
    void findByTelegramMessageIdReturnsEmptyWhenNoReminderMatches() {
        assertThat(habitReminderMessageRepository.findByTelegramMessageId(999L)).isEmpty();
    }

    @Test
    void rejectsDuplicateTelegramMessageId() {
        Habit habit = habitRepository.save(Habit.builder()
                .name("Estudo")
                .type(HabitType.CUMULATIVE)
                .unit("min")
                .target(180)
                .build());

        habitReminderMessageRepository.saveAndFlush(HabitReminderMessage.builder()
                .habit(habit)
                .telegramMessageId(777L)
                .referenceDate(LocalDate.of(2026, 7, 27))
                .build());

        assertThatThrownBy(() -> habitReminderMessageRepository.saveAndFlush(HabitReminderMessage.builder()
                .habit(habit)
                .telegramMessageId(777L)
                .referenceDate(LocalDate.of(2026, 7, 27))
                .build()))
                .isInstanceOf(DataAccessException.class);
    }
}
