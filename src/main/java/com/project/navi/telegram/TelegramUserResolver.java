package com.project.navi.telegram;

import com.project.navi.domain.User;
import com.project.navi.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class TelegramUserResolver {

    private final UserRepository userRepository;

    public TelegramUserResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User resolve(org.telegram.telegrambots.meta.api.objects.User sender) {
        return userRepository.findByTelegramUserId(sender.getId())
                .orElseGet(() -> userRepository.save(User.builder()
                        .telegramUserId(sender.getId())
                        .name(resolveName(sender))
                        .createdAt(Instant.now())
                        .build()));
    }

    private String resolveName(org.telegram.telegrambots.meta.api.objects.User sender) {
        String lastName = sender.getLastName();
        return (lastName == null || lastName.isBlank())
                ? sender.getFirstName()
                : sender.getFirstName() + " " + lastName;
    }
}
