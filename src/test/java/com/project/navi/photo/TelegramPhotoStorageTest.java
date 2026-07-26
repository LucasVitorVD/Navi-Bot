package com.project.navi.photo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramPhotoStorageTest {

    @Mock
    private TelegramClient telegramClient;

    @TempDir
    private Path tempDir;

    private TelegramPhotoStorage storage(Optional<TelegramClient> client) {
        return new TelegramPhotoStorage(client, tempDir.toString());
    }

    @Test
    void downloadsAndSavesPhotoUnderReferenceDateFolder() throws Exception {
        when(telegramClient.execute(any(GetFile.class)))
                .thenReturn(telegramFile("photos/file_1.jpg"));
        when(telegramClient.downloadFileAsStream(any(File.class)))
                .thenReturn(new ByteArrayInputStream("fake-image-bytes".getBytes()));

        Optional<String> result = storage(Optional.of(telegramClient))
                .download("abc", LocalDate.of(2026, 7, 27), 600L);

        assertThat(result).isPresent();
        Path saved = Path.of(result.get());
        assertThat(saved).exists();
        assertThat(saved.getParent().getFileName().toString()).isEqualTo("2026-07-27");
        assertThat(saved.getFileName().toString()).isEqualTo("600.jpg");
        assertThat(Files.readString(saved)).isEqualTo("fake-image-bytes");
    }

    @Test
    void fallsBackToJpgExtensionWhenFilePathHasNoExtension() throws TelegramApiException {
        when(telegramClient.execute(any(GetFile.class)))
                .thenReturn(telegramFile("photos/file_no_ext"));
        when(telegramClient.downloadFileAsStream(any(File.class)))
                .thenReturn(new ByteArrayInputStream("bytes".getBytes()));

        Optional<String> result = storage(Optional.of(telegramClient))
                .download("abc", LocalDate.of(2026, 7, 27), 601L);

        assertThat(Path.of(result.get()).getFileName().toString()).isEqualTo("601.jpg");
    }

    @Test
    void returnsEmptyWhenClientIsNotConfigured() {
        Optional<String> result = storage(Optional.empty())
                .download("abc", LocalDate.of(2026, 7, 27), 600L);

        assertThat(result).isEmpty();
    }

    @Test
    void returnsEmptyWhenFileIdIsNull() {
        Optional<String> result = storage(Optional.of(telegramClient))
                .download(null, LocalDate.of(2026, 7, 27), 600L);

        assertThat(result).isEmpty();
    }

    @Test
    void returnsEmptyWhenTelegramApiCallFails() throws TelegramApiException {
        when(telegramClient.execute(any(GetFile.class))).thenThrow(new TelegramApiException("boom"));

        Optional<String> result = storage(Optional.of(telegramClient))
                .download("abc", LocalDate.of(2026, 7, 27), 600L);

        assertThat(result).isEmpty();
    }

    private File telegramFile(String filePath) {
        File file = new File();
        file.setFilePath(filePath);
        return file;
    }
}
