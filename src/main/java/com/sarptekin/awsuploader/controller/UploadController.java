package com.sarptekin.awsuploader.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sarptekin.awsuploader.service.S3StorageService;
import com.sarptekin.awsuploader.service.S3StorageService.FileInfo;

@Validated
@RestController
@RequestMapping("/api")
public class UploadController {

    private final S3StorageService s3StorageService;

    public UploadController(S3StorageService s3StorageService) {
        this.s3StorageService = s3StorageService;
    }

    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", required = false) String folder) {
        try {
            String url = s3StorageService.upload(file, folder);
            return ResponseEntity.ok(url);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/files")
    public ResponseEntity<List<FileInfo>> listFiles() {
        try {
            List<FileInfo> files = s3StorageService.listFiles();
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}


