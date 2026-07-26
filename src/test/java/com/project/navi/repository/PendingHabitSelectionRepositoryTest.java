package com.project.navi.repository;

import com.project.navi.domain.PendingHabitSelection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PendingHabitSelectionRepositoryTest {

    @Autowired
    private PendingHabitSelectionRepository pendingHabitSelectionRepository;

    @Test
    void savesAndFindsPendingSelectionById() {
        PendingHabitSelection saved = pendingHabitSelectionRepository.save(PendingHabitSelection.builder()
                .telegramChatId(999L)
                .telegramUserId(42L)
                .telegramPhotoFileId("file-abc")
                .captionText("bebi água")
                .originalMessageId(700L)
                .createdAt(Instant.parse("2026-07-27T10:00:00Z"))
                .build());

        Optional<PendingHabitSelection> found = pendingHabitSelectionRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getTelegramChatId()).isEqualTo(999L);
        assertThat(found.get().getTelegramUserId()).isEqualTo(42L);
        assertThat(found.get().getTelegramPhotoFileId()).isEqualTo("file-abc");
        assertThat(found.get().getCaptionText()).isEqualTo("bebi água");
        assertThat(found.get().getOriginalMessageId()).isEqualTo(700L);
    }

    @Test
    void deletingConsumesTheSelectionSoItCannotBeUsedTwice() {
        PendingHabitSelection saved = pendingHabitSelectionRepository.save(PendingHabitSelection.builder()
                .telegramChatId(999L)
                .telegramUserId(42L)
                .telegramPhotoFileId("file-abc")
                .originalMessageId(700L)
                .createdAt(Instant.now())
                .build());

        pendingHabitSelectionRepository.deleteById(saved.getId());

        assertThat(pendingHabitSelectionRepository.findById(saved.getId())).isEmpty();
    }
}
