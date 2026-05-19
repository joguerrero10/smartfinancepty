package com.smartfinancepty.finance.service.storage;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

@Service
@Profile("prod-cloudinary") // activar con: SPRING_PROFILES_ACTIVE=prod,prod-cloudinary
@Slf4j
public class CloudinaryStorageService implements StorageService {

    @Value("${app.storage.cloudinary.cloud-name}")
    private String cloudName;

    @Value("${app.storage.cloudinary.api-key}")
    private String apiKey;

    @Value("${app.storage.cloudinary.api-secret}")
    private String apiSecret;

    // TODO Fase 5: inyectar Cloudinary bean
    // @Autowired private Cloudinary cloudinary;

    @Override
    public StorageResult upload(MultipartFile file, String folder) {
        // TODO Fase 5: implementar con Cloudinary SDK
        // Map params = ObjectUtils.asMap(
        // "folder", folder,
        // "resource_type", "auto"
        // );
        // Map result = cloudinary.uploader().upload(file.getBytes(), params);
        // String publicId = (String) result.get("public_id");
        // String url = (String) result.get("secure_url");
        // return
        // StorageResult.builder().fileUrl(url).storageKey(publicId).provider("CLOUDINARY").build();

        String publicId = folder + "/" + UUID.randomUUID();
        String url = "https://res.cloudinary.com/" + cloudName + "/image/upload/" + publicId;
        log.info("☁️ Cloudinary upload (mock): {}", file.getOriginalFilename());

        return StorageResult.builder().fileUrl(url).storageKey(publicId).storedFilename(publicId)
                .provider("CLOUDINARY").build();
    }

    @Override
    public void delete(String storageKey) {
        // TODO Fase 5: cloudinary.uploader().destroy(storageKey, ObjectUtils.emptyMap());
        log.info("🗑️ Cloudinary delete (mock): {}", storageKey);
    }

    @Override
    public String getProviderName() {
        return "CLOUDINARY";
    }
}
