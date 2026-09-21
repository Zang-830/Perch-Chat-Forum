package com.campus.community.file.storage;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
@ConditionalOnProperty(prefix = "app.storage", name = "type", havingValue = "minio")
public class MinioImageStorage implements ImageStorage {
    private final MinioClient client;
    private final String bucket;
    private volatile boolean bucketReady;

    public MinioImageStorage(@Value("${app.minio.endpoint}") String endpoint,
                             @Value("${app.minio.access-key}") String accessKey,
                             @Value("${app.minio.secret-key}") String secretKey,
                             @Value("${app.minio.bucket}") String bucket) {
        this.client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        this.bucket = bucket;
    }

    @Override
    public void store(String objectName, byte[] content, String contentType) throws Exception {
        ensureBucket();
        client.putObject(PutObjectArgs.builder()
                .bucket(bucket)
                .object(objectName)
                .data(content, content.length)
                .contentType(contentType)
                .build());
    }

    @Override
    public InputStream open(String objectName) throws Exception {
        ensureBucket();
        return client.getObject(GetObjectArgs.builder()
                .bucket(bucket)
                .object(objectName)
                .build());
    }

    @Override
    public void delete(String objectName) {
        try {
            ensureBucket();
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
        } catch (Exception ignored) {
            // Best-effort cleanup after a failed database write.
        }
    }

    private void ensureBucket() throws Exception {
        if (bucketReady) {
            return;
        }
        synchronized (this) {
            if (bucketReady) {
                return;
            }
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
            bucketReady = true;
        }
    }
}
