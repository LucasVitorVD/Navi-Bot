package com.project.navi.photo;

import java.time.LocalDate;
import java.util.Optional;

public interface PhotoStorage {

    Optional<String> download(String telegramFileId, LocalDate referenceDate, long telegramMessageId);
}
