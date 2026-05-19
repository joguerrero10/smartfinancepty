package com.smartfinancepty.finance.service.storage;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

@Service
@Profile("prod-s3") // activar con: SPRING_PROFILES_ACTIVE=prod,prod-s3
@Slf4j
public class S3StorageService implements StorageService {

    @Value("${app.storage.s3.bucket}")
    private String bucket;

    @Value("${app.storage.s3.region:us-east-1}")
    private String region;

    // TODO Fase 5: inyectar S3Client de AWS SDK v2
    // @Autowired private S3Client s3Client;

    @Override
    public StorageResult upload(MultipartFile file, String folder) {
        // TODO Fase 5: implementar con AWS SDK v2
        // String extension = getExtension(file.getOriginalFilename());
        // String key = folder + "/" + UUID.randomUUID() + "." + extension;
        //
        // PutObjectRequest request = PutObjectRequest.builder()
        // .bucket(bucket)
        // .key(key)
        // .contentType(file.getContentType())
        // .build();
        //
        // s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
        //
        // String url = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
        // return StorageResult.builder().fileUrl(url).storageKey(key).provider("S3").build();

        String key = folder + "/" + UUID.randomUUID();
        String url = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
        log.info("☁️ S3 upload (mock): {}", file.getOriginalFilename());

        return StorageResult.builder().fileUrl(url).storageKey(key).storedFilename(key)
                .provider("S3").build();
    }

    @Override
    public void delete(String storageKey) {
        // TODO Fase 5:
        // s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(storageKey).build());
        log.info("🗑️ S3 delete (mock): {}", storageKey);
    }

    @Override
    public String getProviderName() {
        return "S3";
    }
}
