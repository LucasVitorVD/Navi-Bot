package com.project.navi.repository;

import com.project.navi.domain.Habit;
import com.project.navi.domain.User;
import com.project.navi.domain.UserHabitConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserHabitConfigRepository extends JpaRepository<UserHabitConfig, Long> {

    Optional<UserHabitConfig> findByUserAndHabit(User user, Habit habit);
}
