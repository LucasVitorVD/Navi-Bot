package com.project.navi.progress;

import com.project.navi.domain.Habit;
import com.project.navi.domain.HabitRecord;
import com.project.navi.domain.HabitType;
import com.project.navi.domain.User;
import com.project.navi.repository.HabitRecordRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Component
public class HabitProgressCalculator {

    private final HabitRecordRepository habitRecordRepository;

    public HabitProgressCalculator(HabitRecordRepository habitRecordRepository) {
        this.habitRecordRepository = habitRecordRepository;
    }

    public HabitProgress calculate(User user, Habit habit, LocalDate date) {
        List<HabitRecord> records = habitRecordRepository.findByUserAndHabitAndReferenceDate(user, habit, date);

        if (habit.getType() == HabitType.BINARY) {
            return new HabitProgress(habit, records.isEmpty() ? 0 : 100);
        }

        int total = records.stream()
                .map(HabitRecord::getExtractedQuantity)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        int target = habit.getTarget();
        int percentage = target <= 0 ? 0 : Math.min(100, total * 100 / target);
        return new HabitProgress(habit, percentage);
    }
}
