package com.smartfinancepty.finance.service.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

@Service
@Profile({"default", "dev", "test"}) // activo en dev y test, no en prod
@Slf4j
public class LocalStorageService implements StorageService {

    @Value("${app.storage.local.path:uploads}")
    private String uploadPath;

    @Value("${app.storage.local.base-url:http://localhost:8080/files}")
    private String baseUrl;

    @Override
    public StorageResult upload(MultipartFile file, String folder) {
        try {
            String extension = getExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID() + "." + extension;
            String storageKey = folder + "/" + filename;

            Path targetDir = Paths.get(uploadPath, folder);
            Files.createDirectories(targetDir);

            Path targetFile = targetDir.resolve(filename);
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);

            String fileUrl = baseUrl + "/" + storageKey;
            log.info("📁 LOCAL upload: {} → {}", file.getOriginalFilename(), targetFile);

            return StorageResult.builder().fileUrl(fileUrl).storageKey(storageKey)
                    .storedFilename(filename).provider("LOCAL").build();

        } catch (IOException e) {
            throw new RuntimeException("Error al guardar archivo localmente: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Path file = Paths.get(uploadPath, storageKey);
            Files.deleteIfExists(file);
            log.info("🗑️ LOCAL delete: {}", storageKey);
        } catch (IOException e) {
            log.error("Error al eliminar archivo local: {}", storageKey, e);
        }
    }

    @Override
    public String getProviderName() {
        return "LOCAL";
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains("."))
            return "bin";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
