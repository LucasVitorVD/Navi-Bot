package com.project.navi.repository;

import com.project.navi.domain.Habit;
import com.project.navi.domain.HabitRecord;
import com.project.navi.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface HabitRecordRepository extends JpaRepository<HabitRecord, Long> {

    List<HabitRecord> findByUserAndHabitAndReferenceDate(User user, Habit habit, LocalDate referenceDate);
}
