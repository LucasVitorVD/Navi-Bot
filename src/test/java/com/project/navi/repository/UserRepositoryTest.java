package com.project.navi.repository;

import com.project.navi.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataAccessException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void savesAndFindsUserByTelegramUserId() {
        User user = User.builder()
                .telegramUserId(123456789L)
                .name("Lucas")
                .telegramProfilePhotoFileId("file-id-abc")
                .createdAt(Instant.now())
                .build();

        User saved = userRepository.save(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(userRepository.findByTelegramUserId(123456789L))
                .isPresent()
                .get()
                .extracting(User::getName)
                .isEqualTo("Lucas");
    }

    @Test
    void rejectsDuplicateTelegramUserId() {
        userRepository.saveAndFlush(User.builder()
                .telegramUserId(111L)
                .name("Ana")
                .createdAt(Instant.now())
                .build());

        // O dialeto community do SQLite não traduz a violação para o subtipo
        // DataIntegrityViolationException; cai no supertipo DataAccessException.
        assertThatThrownBy(() -> userRepository.saveAndFlush(User.builder()
                .telegramUserId(111L)
                .name("Outra Ana")
                .createdAt(Instant.now())
                .build()))
                .isInstanceOf(DataAccessException.class);
    }
}
