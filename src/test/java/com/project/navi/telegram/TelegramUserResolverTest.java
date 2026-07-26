package com.project.navi.telegram;

import com.project.navi.domain.User;
import com.project.navi.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramUserResolverTest {

    @Mock
    private UserRepository userRepository;

    private TelegramUserResolver resolver() {
        return new TelegramUserResolver(userRepository);
    }

    @Test
    void returnsExistingUserWhenTelegramIdIsKnown() {
        User existingUser = User.builder().id(10L).telegramUserId(42L).name("Lucas").build();
        when(userRepository.findByTelegramUserId(42L)).thenReturn(Optional.of(existingUser));

        org.telegram.telegrambots.meta.api.objects.User sender =
                org.telegram.telegrambots.meta.api.objects.User.builder().id(42L).firstName("Lucas").isBot(false).build();

        User resolved = resolver().resolve(sender);

        assertThat(resolved).isEqualTo(existingUser);
        verify(userRepository, never()).save(any());
    }

    @Test
    void createsUserWithFirstAndLastNameWhenSenderIsUnknown() {
        when(userRepository.findByTelegramUserId(99L)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        org.telegram.telegrambots.meta.api.objects.User sender = org.telegram.telegrambots.meta.api.objects.User.builder()
                .id(99L).firstName("Beto").lastName("Silva").isBot(false).build();

        User resolved = resolver().resolve(sender);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getTelegramUserId()).isEqualTo(99L);
        assertThat(captor.getValue().getName()).isEqualTo("Beto Silva");
        assertThat(resolved.getName()).isEqualTo("Beto Silva");
    }

    @Test
    void createsUserWithFirstNameOnlyWhenLastNameIsAbsent() {
        when(userRepository.findByTelegramUserId(5L)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        org.telegram.telegrambots.meta.api.objects.User sender = org.telegram.telegrambots.meta.api.objects.User.builder()
                .id(5L).firstName("Ana").isBot(false).build();

        User resolved = resolver().resolve(sender);

        assertThat(resolved.getName()).isEqualTo("Ana");
    }
}
