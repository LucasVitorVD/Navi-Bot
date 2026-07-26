package com.project.navi.photo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Optional;

@Component
public class TelegramPhotoStorage implements PhotoStorage {

    private static final Logger log = LoggerFactory.getLogger(TelegramPhotoStorage.class);
    private static final String DEFAULT_EXTENSION = "jpg";

    private final TelegramClient telegramClient;
    private final Path baseDir;

    public TelegramPhotoStorage(Optional<TelegramClient> telegramClient,
                                 @Value("${PHOTO_STORAGE_DIR:./fotos}") String baseDir) {
        this.telegramClient = telegramClient.orElse(null);
        this.baseDir = Path.of(baseDir);
    }

    @Override
    public Optional<String> download(String telegramFileId, LocalDate referenceDate, long telegramMessageId) {
        if (telegramClient == null || telegramFileId == null) {
            return Optional.empty();
        }

        try {
            File file = telegramClient.execute(GetFile.builder().fileId(telegramFileId).build());
            try (InputStream in = telegramClient.downloadFileAsStream(file)) {
                Path dir = baseDir.resolve(referenceDate.toString());
                Files.createDirectories(dir);

                Path target = dir.resolve(telegramMessageId + "." + extensionOf(file.getFilePath()));
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                return Optional.of(target.toString());
            }
        } catch (TelegramApiException | IOException e) {
            log.warn("Falha ao baixar foto do Telegram (file_id={})", telegramFileId, e);
            return Optional.empty();
        }
    }

    private String extensionOf(String filePath) {
        if (filePath == null) {
            return DEFAULT_EXTENSION;
        }
        int dotIndex = filePath.lastIndexOf('.');
        return dotIndex >= 0 ? filePath.substring(dotIndex + 1) : DEFAULT_EXTENSION;
    }
}
