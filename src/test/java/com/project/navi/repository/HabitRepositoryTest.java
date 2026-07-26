package com.project.navi.repository;

import com.project.navi.domain.Habit;
import com.project.navi.domain.HabitType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class HabitRepositoryTest {

    @Autowired
    private HabitRepository habitRepository;

    @Test
    void savesAndFindsCumulativeHabitById() {
        Habit habit = Habit.builder()
                .name("Água")
                .type(HabitType.CUMULATIVE)
                .unit("ml")
                .target(3000)
                .build();

        Habit saved = habitRepository.save(habit);

        Optional<Habit> found = habitRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getType()).isEqualTo(HabitType.CUMULATIVE);
        assertThat(found.get().getUnit()).isEqualTo("ml");
        assertThat(found.get().getTarget()).isEqualTo(3000);
    }

    @Test
    void savesBinaryHabitWithoutUnitOrTarget() {
        Habit habit = Habit.builder()
                .name("Alimentação boa")
                .type(HabitType.BINARY)
                .build();

        Habit saved = habitRepository.save(habit);

        Optional<Habit> found = habitRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getUnit()).isNull();
        assertThat(found.get().getTarget()).isNull();
    }

    @Test
    void findsHabitByExactName() {
        habitRepository.save(Habit.builder()
                .name("Água")
                .type(HabitType.CUMULATIVE)
                .unit("ml")
                .target(3000)
                .build());

        assertThat(habitRepository.findByName("Água")).isPresent();
        assertThat(habitRepository.findByName("Cardio")).isEmpty();
    }
}
