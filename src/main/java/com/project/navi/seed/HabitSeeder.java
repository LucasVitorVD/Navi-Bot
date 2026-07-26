package com.project.navi.seed;

import com.project.navi.domain.Habit;
import com.project.navi.domain.HabitType;
import com.project.navi.repository.HabitRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HabitSeeder implements ApplicationRunner {

    private final HabitRepository habitRepository;

    public HabitSeeder(HabitRepository habitRepository) {
        this.habitRepository = habitRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        seed();
    }

    void seed() {
        if (habitRepository.count() > 0) {
            return;
        }

        habitRepository.saveAll(List.of(
                Habit.builder().name("Água").type(HabitType.CUMULATIVE).unit("ml").target(3000).build(),
                Habit.builder().name("Estudo").type(HabitType.CUMULATIVE).unit("min").target(180).build(),
                Habit.builder().name("Cardio").type(HabitType.CUMULATIVE).unit("min").target(30).build(),
                Habit.builder().name("Alimentação boa").type(HabitType.BINARY).build()
        ));
    }
}
