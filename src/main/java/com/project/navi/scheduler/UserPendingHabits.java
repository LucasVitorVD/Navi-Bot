package com.project.navi.scheduler;

import com.project.navi.domain.Habit;
import com.project.navi.domain.User;

import java.util.List;

public record UserPendingHabits(User user, List<Habit> pendingHabits) {
}
