package com.project.navi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "habit_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HabitRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "habit_id", nullable = false)
    private Habit habit;

    @Column(name = "reference_date", nullable = false)
    private LocalDate referenceDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "caption_text")
    private String captionText;

    @Column(name = "extracted_quantity")
    private Integer extractedQuantity;

    @Column(name = "telegram_photo_file_id")
    private String telegramPhotoFileId;

    @Column(name = "local_photo_path")
    private String localPhotoPath;

    @Column(name = "telegram_message_id", nullable = false)
    private Long telegramMessageId;
}
