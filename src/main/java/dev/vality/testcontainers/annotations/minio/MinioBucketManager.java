package dev.vality.testcontainers.annotations.minio;

import io.minio.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.testcontainers.containers.GenericContainer;

import java.util.List;

import static dev.vality.testcontainers.annotations.util.SpringApplicationPropertiesLoader.loadDefaultLibraryProperty;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class MinioBucketManager {

    static void ensureBucket(GenericContainer<?> container, String bucketName) {
        try (var client = client(container)) {
            var exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to create MinIO bucket '" + bucketName + "'", ex);
        }
    }

    static void cleanupBucket(
            GenericContainer<?> container,
            String bucketName,
            List<String> excludedPrefixes) {
        try (var client = client(container)) {
            var objects = client.listObjects(ListObjectsArgs.builder()
                    .bucket(bucketName)
                    .recursive(true)
                    .build());
            for (var objectResult : objects) {
                var objectName = objectResult.get().objectName();
                if (excludedPrefixes.stream().noneMatch(objectName::startsWith)) {
                    client.removeObject(RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build());
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to clean MinIO bucket '" + bucketName + "'", ex);
        }
    }

    private static MinioClient client(GenericContainer<?> container) {
        var endpoint = "http://%s:%d".formatted(container.getHost(), container.getMappedPort(9000));
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(
                        loadDefaultLibraryProperty(MinioTestcontainerFactory.MINIO_USER),
                        loadDefaultLibraryProperty(MinioTestcontainerFactory.MINIO_PASSWORD))
                .build();
    }
}
