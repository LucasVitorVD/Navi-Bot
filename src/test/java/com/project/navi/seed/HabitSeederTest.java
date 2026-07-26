package com.project.navi.seed;

import com.project.navi.domain.Habit;
import com.project.navi.domain.HabitType;
import com.project.navi.repository.HabitRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class HabitSeederTest {

    @Autowired
    private HabitRepository habitRepository;

    @Test
    void seedsAllFourFixedHabitsWhenTableIsEmpty() {
        new HabitSeeder(habitRepository).seed();

        List<Habit> habits = habitRepository.findAll();

        assertThat(habits)
                .extracting(Habit::getName, Habit::getType, Habit::getUnit, Habit::getTarget)
                .containsExactlyInAnyOrder(
                        tuple("Água", HabitType.CUMULATIVE, "ml", 3000),
                        tuple("Estudo", HabitType.CUMULATIVE, "min", 180),
                        tuple("Cardio", HabitType.CUMULATIVE, "min", 30),
                        tuple("Alimentação saudável", HabitType.BINARY, null, null)
                );
    }

    @Test
    void doesNotDuplicateHabitsWhenSeedRunsAgain() {
        HabitSeeder seeder = new HabitSeeder(habitRepository);

        seeder.seed();
        seeder.seed();

        assertThat(habitRepository.count()).isEqualTo(4);
    }
}
