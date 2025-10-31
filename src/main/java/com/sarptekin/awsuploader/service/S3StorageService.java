package com.sarptekin.awsuploader.service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Service
public class S3StorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);
    
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucket;
    private final String region;
    private final boolean publicReadAcl;

    public S3StorageService(
            S3Client s3Client,
            S3Presigner s3Presigner,
            @Value("${aws.s3.bucket}") String bucket,
            @Value("${aws.region}") String region,
            @Value("${aws.s3.publicReadAcl:true}") boolean publicReadAcl) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucket = bucket;
        this.region = region;
        this.publicReadAcl = publicReadAcl;
    }

    public String upload(MultipartFile file, String folder) throws IOException {
        String originalFilename = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String key = buildObjectKey(folder, originalFilename);

        log.info("Uploading file: {} to bucket: {} with key: {}", originalFilename, bucket, key);

        PutObjectRequest.Builder putReq = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key);

        if (file.getContentType() != null && !file.getContentType().isEmpty()) {
            putReq.contentType(file.getContentType());
        }

        if (publicReadAcl) {
            putReq.acl(ObjectCannedACL.PUBLIC_READ);
            log.debug("Setting PUBLIC_READ ACL");
        }

        try {
            s3Client.putObject(putReq.build(), RequestBody.fromBytes(file.getBytes()));
            log.info("Successfully uploaded file: {}", key);
        } catch (S3Exception e) {
            // If ACL error and public ACL was requested, retry without ACL
            if (publicReadAcl && (e.awsErrorDetails().errorCode().equals("AccessControlListNotSupported") 
                    || e.awsErrorDetails().errorCode().equals("InvalidRequest"))) {
                log.warn("Public ACL not supported (bucket blocks public ACLs), retrying without ACL");
                PutObjectRequest.Builder retryBuilder = PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key);
                if (file.getContentType() != null && !file.getContentType().isEmpty()) {
                    retryBuilder.contentType(file.getContentType());
                }
                s3Client.putObject(retryBuilder.build(), RequestBody.fromBytes(file.getBytes()));
                log.info("Successfully uploaded file without ACL: {}", key);
            } else {
                log.error("S3 upload failed: {} - {}", e.awsErrorDetails().errorCode(), e.awsErrorDetails().errorMessage(), e);
                throw new IOException("S3 upload failed: " + e.awsErrorDetails().errorMessage(), e);
            }
        } catch (Exception e) {
            log.error("Upload failed with unexpected error", e);
            throw new IOException("Upload failed: " + e.getMessage(), e);
        }

        return buildPresignedUrl(key);
    }

    private String buildPresignedUrl(String key) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(
                    GetObjectPresignRequest.builder()
                            .signatureDuration(Duration.ofHours(24)) // URL valid for 24 hours
                            .getObjectRequest(getObjectRequest)
                            .build());

            return presignedRequest.url().toString();
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for key: {}", key, e);
            // Fallback to public URL format if presigning fails
            return buildPublicUrl(bucket, region, key);
        }
    }

    private static String buildObjectKey(String folder, String originalFilename) {
        String safeName = URLEncoder.encode(originalFilename, StandardCharsets.UTF_8);
        String unique = UUID.randomUUID().toString();
        if (folder == null || folder.isBlank()) {
            return unique + "-" + safeName;
        }
        String trimmed = folder.replaceAll("^/+|/+$", "");
        return trimmed + "/" + unique + "-" + safeName;
    }

    private static String buildPublicUrl(String bucket, String region, String key) {
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
    }

    public List<FileInfo> listFiles() {
        List<FileInfo> files = new ArrayList<>();
        try {
            ListObjectsV2Request listReq = ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .build();

            ListObjectsV2Response listResp = s3Client.listObjectsV2(listReq);
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());

            for (S3Object s3Object : listResp.contents()) {
                String key = s3Object.key();
                String url = buildPresignedUrl(key);
                String name = key;
                // Extract filename from key (last part after /)
                int lastSlash = key.lastIndexOf('/');
                if (lastSlash >= 0) {
                    name = key.substring(lastSlash + 1);
                }
                
                String size = formatFileSize(s3Object.size());
                String lastModified = s3Object.lastModified() != null 
                    ? formatter.format(s3Object.lastModified()) 
                    : "Unknown";

                files.add(new FileInfo(name, key, url, size, lastModified));
            }

            log.info("Listed {} files from bucket {}", files.size(), bucket);
        } catch (S3Exception e) {
            log.error("Failed to list files: {} - {}", e.awsErrorDetails().errorCode(), e.awsErrorDetails().errorMessage(), e);
        } catch (Exception e) {
            log.error("Failed to list files", e);
        }
        return files;
    }

    private static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    public static class FileInfo {
        private final String name;
        private final String key;
        private final String url;
        private final String size;
        private final String lastModified;

        public FileInfo(String name, String key, String url, String size, String lastModified) {
            this.name = name;
            this.key = key;
            this.url = url;
            this.size = size;
            this.lastModified = lastModified;
        }

        public String getName() {
            return name;
        }

        public String getKey() {
            return key;
        }

        public String getUrl() {
            return url;
        }

        public String getSize() {
            return size;
        }

        public String getLastModified() {
            return lastModified;
        }
    }
}


