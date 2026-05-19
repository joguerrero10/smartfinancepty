package com.smartfinancepty.finance.controllers.attachment;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/files")
@Profile("!prod") // solo en dev — en prod los sirve S3 o Cloudinary
@Slf4j
public class FileResourceController {

    @Value("${app.storage.local.path:uploads}")
    private String uploadPath;

    @GetMapping("/**")
    public ResponseEntity<Resource> serveFile(@RequestParam String path) {
        try {
            Path filePath = Paths.get(uploadPath).resolve(path).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = determineContentType(path);

            return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private String determineContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg"))
            return "image/jpeg";
        if (lower.endsWith(".png"))
            return "image/png";
        if (lower.endsWith(".webp"))
            return "image/webp";
        if (lower.endsWith(".pdf"))
            return "application/pdf";
        if (lower.endsWith(".xml"))
            return "application/xml";
        return "application/octet-stream";
    }
}
