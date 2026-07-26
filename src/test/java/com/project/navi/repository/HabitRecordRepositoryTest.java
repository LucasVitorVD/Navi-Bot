package com.project.navi.repository;

import com.project.navi.domain.Habit;
import com.project.navi.domain.HabitRecord;
import com.project.navi.domain.HabitType;
import com.project.navi.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class HabitRecordRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HabitRepository habitRepository;

    @Autowired
    private HabitRecordRepository habitRecordRepository;

    @Test
    void savesRecordWithInterpretedQuantityAndPhotoReferences() {
        User user = userRepository.save(User.builder()
                .telegramUserId(7L)
                .name("Ana")
                .createdAt(Instant.now())
                .build());

        Habit habit = habitRepository.save(Habit.builder()
                .name("Estudo")
                .type(HabitType.CUMULATIVE)
                .unit("min")
                .target(180)
                .build());

        HabitRecord record = HabitRecord.builder()
                .user(user)
                .habit(habit)
                .referenceDate(LocalDate.of(2026, 7, 27))
                .createdAt(Instant.now())
                .captionText("estudei 40 minutos de java")
                .extractedQuantity(40)
                .telegramPhotoFileId("tg-file-id")
                .localPhotoPath("/app/fotos/2026-07-27/ana-1.jpg")
                .telegramMessageId(9876L)
                .build();

        HabitRecord saved = habitRecordRepository.save(record);

        Optional<HabitRecord> found = habitRecordRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getUser().getId()).isEqualTo(user.getId());
        assertThat(found.get().getHabit().getId()).isEqualTo(habit.getId());
        assertThat(found.get().getExtractedQuantity()).isEqualTo(40);
        assertThat(found.get().getCaptionText()).isEqualTo("estudei 40 minutos de java");
        assertThat(found.get().getReferenceDate()).isEqualTo(LocalDate.of(2026, 7, 27));
    }

    @Test
    void allowsMultipleRecordsForSameUserHabitAndDay() {
        User user = userRepository.save(User.builder()
                .telegramUserId(8L)
                .name("Beto")
                .createdAt(Instant.now())
                .build());

        Habit habit = habitRepository.save(Habit.builder()
                .name("Água")
                .type(HabitType.CUMULATIVE)
                .unit("ml")
                .target(3000)
                .build());

        LocalDate today = LocalDate.of(2026, 7, 27);

        habitRecordRepository.save(HabitRecord.builder()
                .user(user).habit(habit).referenceDate(today)
                .createdAt(Instant.now()).extractedQuantity(500)
                .telegramMessageId(1L).build());

        habitRecordRepository.save(HabitRecord.builder()
                .user(user).habit(habit).referenceDate(today)
                .createdAt(Instant.now()).extractedQuantity(500)
                .telegramMessageId(2L).build());

        assertThat(habitRecordRepository.findAll()).hasSize(2);
    }
}
