package com.project.navi.repository;

import com.project.navi.domain.HabitReminderMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface HabitReminderMessageRepository extends JpaRepository<HabitReminderMessage, Long> {

    @Query("SELECT hrm FROM HabitReminderMessage hrm JOIN FETCH hrm.habit WHERE hrm.telegramMessageId = :telegramMessageId")
    Optional<HabitReminderMessage> findByTelegramMessageId(@Param("telegramMessageId") Long telegramMessageId);
}
