package com.sarptekin.awsuploader;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;

public class S3Uploader {
    public static void main(String[] args) {
        S3Client s3 = S3Client.builder()
                .region(Region.EU_CENTRAL_1)
                .credentialsProvider(ProfileCredentialsProvider.create())
                .build();

        ListBucketsResponse response = s3.listBuckets();
        response.buckets().forEach(bucket -> System.out.println(bucket.name()));
    }
}