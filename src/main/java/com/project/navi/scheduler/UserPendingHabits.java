package com.project.navi.scheduler;

import com.project.navi.domain.User;
import com.project.navi.progress.HabitProgress;

import java.util.List;

public record UserPendingHabits(User user, List<HabitProgress> pendingHabits) {
}
