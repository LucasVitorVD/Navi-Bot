package com.project.navi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Guarda uma foto recebida sem reply a nenhum lembrete conhecido, enquanto aguarda a
 * pessoa escolher o hábito correspondente por um botão (fallback de identificação).
 */
@Entity
@Table(name = "pending_habit_selections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingHabitSelection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "telegram_chat_id", nullable = false)
    private Long telegramChatId;

    @Column(name = "telegram_user_id", nullable = false)
    private Long telegramUserId;

    @Column(name = "telegram_photo_file_id", nullable = false)
    private String telegramPhotoFileId;

    @Column(name = "caption_text")
    private String captionText;

    @Column(name = "original_message_id", nullable = false)
    private Long originalMessageId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
