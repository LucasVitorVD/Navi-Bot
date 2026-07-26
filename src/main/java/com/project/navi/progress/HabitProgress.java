package com.project.navi.progress;

import com.project.navi.domain.Habit;

public record HabitProgress(Habit habit, int percentage) {

    public boolean isCompleted() {
        return percentage >= 100;
    }
}
