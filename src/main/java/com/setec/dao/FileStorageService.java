package com.setec.dao;  // ← Same package as your DAOs

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {

    private String getUploadDir() {
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl != null && databaseUrl.contains("postgres")) {
            return "/tmp/static";
        } else {
            return "myApp/static";
        }
    }

    public String storeFile(MultipartFile file) throws IOException {
        String uploadDir = getUploadDir();
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String originalFileName = file.getOriginalFilename();
        String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        String fileName = UUID.randomUUID() + extension;
        
        Path filePath = Paths.get(uploadDir, fileName);
        file.transferTo(filePath.toFile());

        return fileName;
    }

    public boolean deleteFile(String imageUrl) {
        try {
            String uploadDir = getUploadDir();
            String fileName = imageUrl.replace("/static/", "");
            Path filePath = Paths.get(uploadDir, fileName);
            return filePath.toFile().delete();
        } catch (Exception e) {
            return false;
        }
    }
}