package com.project.navi.repository;

import com.project.navi.domain.HabitRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HabitRecordRepository extends JpaRepository<HabitRecord, Long> {
}
