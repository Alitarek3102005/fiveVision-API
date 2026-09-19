package com.fivevision.api.media.internal.service;

import com.fivevision.api.media.internal.config.StorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageProcessingService {

    private final S3Client s3Client;
    private final StorageProperties storageProperties;

    private static final int THUMB_MAX_DIM = 400;
    private static final int LARGE_MAX_DIM = 1920;

    public Variants generateVariants(String bucket, String originalKey) {
        try (InputStream in = s3Client.getObject(GetObjectRequest.builder()
                .bucket(bucket).key(originalKey).build())) {

            byte[] originalBytes = in.readAllBytes();
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(originalBytes));
            if (original == null) {
                log.warn("Not a decodable image, skipping variants: {}", originalKey);
                return null;
            }

            ByteArrayOutputStream thumbOut = new ByteArrayOutputStream();
            ByteArrayOutputStream largeOut = new ByteArrayOutputStream();

            Thumbnails.of(new ByteArrayInputStream(originalBytes))
                    .size(THUMB_MAX_DIM, THUMB_MAX_DIM)
                    .outputFormat("jpg")
                    .outputQuality(0.75)
                    .toOutputStream(thumbOut);

            Thumbnails.of(new ByteArrayInputStream(originalBytes))
                    .size(LARGE_MAX_DIM, LARGE_MAX_DIM)
                    .outputFormat("jpg")
                    .outputQuality(0.85)
                    .toOutputStream(largeOut);

            String thumbKey = variantKey(originalKey, "thumb");
            String largeKey = variantKey(originalKey, "large");

            upload(bucket, thumbKey, thumbOut.toByteArray());
            upload(bucket, largeKey, largeOut.toByteArray());

            log.info("Generated variants for {} → thumb={} ({} B), large={} ({} B)",
                    originalKey, thumbKey, thumbOut.size(), largeKey, largeOut.size());

            return new Variants(thumbKey, largeKey);

        } catch (Exception e) {
            log.error("Variant generation failed for {}", originalKey, e);
            return null;
        }
    }


    public String publicUrl(String key) {
        return "%s/%s/%s".formatted(
                storageProperties.getEndpointUrl(),
                storageProperties.getBucketName(),
                key);
    }

    private String variantKey(String originalKey, String variant) {
        int slash = originalKey.lastIndexOf('/');
        String dir = originalKey.substring(0, slash);
        String fileName = originalKey.substring(slash + 1);
        String baseName = fileName.replaceAll("\\.[^.]+$", "");
        return dir + "/" + variant + "/" + baseName + ".jpg";
    }

    private void upload(String bucket, String key, byte[] bytes) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType("image/jpeg")
                        .cacheControl("public, max-age=31536000, immutable")
                        .build(),
                RequestBody.fromBytes(bytes)
        );
    }

    public record Variants(String thumbnailKey, String largeKey) {}
}