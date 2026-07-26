package com.project.navi.progress;

import com.project.navi.domain.Habit;
import com.project.navi.domain.HabitRecord;
import com.project.navi.domain.HabitType;
import com.project.navi.domain.User;
import com.project.navi.repository.HabitRecordRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Objects;

@Component
public class HabitProgressCalculator {

    private final HabitRecordRepository habitRecordRepository;

    public HabitProgressCalculator(HabitRecordRepository habitRecordRepository) {
        this.habitRecordRepository = habitRecordRepository;
    }

    public HabitProgress calculate(User user, Habit habit, LocalDate date) {
        if (habit.getType() == HabitType.BINARY) {
            boolean hasRecord = !habitRecordRepository.findByUserAndHabitAndReferenceDate(user, habit, date).isEmpty();
            return new HabitProgress(habit, hasRecord ? 100 : 0);
        }

        int total = sumQuantities(user, habit, date);
        int target = habit.getTarget();
        int percentage = target <= 0 ? 0 : Math.min(100, total * 100 / target);
        return new HabitProgress(habit, percentage);
    }

    public int remaining(User user, Habit habit, LocalDate date) {
        int total = sumQuantities(user, habit, date);
        return Math.max(0, habit.getTarget() - total);
    }

    private int sumQuantities(User user, Habit habit, LocalDate date) {
        return habitRecordRepository.findByUserAndHabitAndReferenceDate(user, habit, date).stream()
                .map(HabitRecord::getExtractedQuantity)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
    }
}
