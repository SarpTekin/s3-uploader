package com.sarptekin.awsuploader.service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class S3StorageService {

    private final S3Client s3Client;
    private final String bucket;
    private final String region;
    private final boolean publicReadAcl;

    public S3StorageService(
            S3Client s3Client,
            @Value("${aws.s3.bucket}") String bucket,
            @Value("${aws.region}") String region,
            @Value("${aws.s3.publicReadAcl:true}") boolean publicReadAcl) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.region = region;
        this.publicReadAcl = publicReadAcl;
    }

    public String upload(MultipartFile file, String folder) throws IOException {
        String originalFilename = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String key = buildObjectKey(folder, originalFilename);

        PutObjectRequest.Builder putReq = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType());

        if (publicReadAcl) {
            putReq.acl(ObjectCannedACL.PUBLIC_READ);
        }

        s3Client.putObject(putReq.build(), RequestBody.fromBytes(file.getBytes()));

        return buildPublicUrl(bucket, region, key);
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
}


