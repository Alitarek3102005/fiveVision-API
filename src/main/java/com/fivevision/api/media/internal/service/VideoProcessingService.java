package com.fivevision.api.media.internal.service;

import com.fivevision.api.media.internal.config.StorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoProcessingService {

    private final S3Client s3Client;
    private final StorageProperties storageProperties;

    private static final int POSTER_WIDTH = 400;

    private static final double POSTER_AT_SECONDS = 1.0;
    private static final double POSTER_FALLBACK_SECONDS = 0.0;

    private static final Duration FFMPEG_TIMEOUT = Duration.ofSeconds(45);

    @Value("${media.video-poster.ffmpeg-path:ffmpeg}")
    private String ffmpegPath;

    @Value("${media.video-poster.enabled:true}")
    private boolean enabled;

    public record Variants(String thumbnailKey) {}

    public Variants generatePoster(String bucket, String originalKey) {
        if (!enabled) {
            log.debug("Video poster generation disabled; skipping {}", originalKey);
            return null;
        }

        Path tmpVideo = null;
        Path tmpPoster = null;
        try {
            tmpVideo = Files.createTempFile("5vision-video-", extensionOf(originalKey));
            try (var in = s3Client.getObject(GetObjectRequest.builder()
                    .bucket(bucket).key(originalKey).build())) {
                Files.copy(in, tmpVideo, StandardCopyOption.REPLACE_EXISTING);
            }
            long videoBytes = Files.size(tmpVideo);
            log.debug("Downloaded {} ({} bytes) for poster extraction", originalKey, videoBytes);

            tmpPoster = Files.createTempFile("5vision-poster-", ".jpg");
            runFfmpegWithFallback(tmpVideo, tmpPoster);

            if (!Files.exists(tmpPoster) || Files.size(tmpPoster) == 0) {
                log.warn("ffmpeg produced no poster for {}", originalKey);
                return null;
            }

            String posterKey = posterKey(originalKey);
            byte[] bytes = Files.readAllBytes(tmpPoster);
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(posterKey)
                            .contentType("image/jpeg")
                            .cacheControl("public, max-age=31536000, immutable")
                            .build(),
                    RequestBody.fromBytes(bytes)
            );

            log.info("Generated video poster for {} → {} ({} B)",
                    originalKey, posterKey, bytes.length);

            return new Variants(posterKey);

        } catch (IOException e) {
            log.warn("Video poster generation unavailable for {}: {}", originalKey, e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Video poster generation failed for {}", originalKey, e);
            return null;
        } finally {
            quietDelete(tmpVideo);
            quietDelete(tmpPoster);
        }
    }

    public String publicUrl(String key) {
        return "%s/%s/%s".formatted(
                storageProperties.getEndpointUrl(),
                storageProperties.getBucketName(),
                key);
    }


    private void runFfmpegWithFallback(Path input, Path output) throws IOException, InterruptedException {
        try {
            runFfmpeg(input, output, POSTER_AT_SECONDS);
            if (Files.exists(output) && Files.size(output) > 0) return;
        } catch (IOException e) {
            log.debug("ffmpeg seek={}s failed ({}); retrying at start",
                    POSTER_AT_SECONDS, e.getMessage());
        }
        runFfmpeg(input, output, POSTER_FALLBACK_SECONDS);
    }

    private void runFfmpeg(Path input, Path output, double seekSeconds)
            throws IOException, InterruptedException {

        List<String> cmd = List.of(
                ffmpegPath,
                "-hide_banner",
                "-loglevel", "error",
                "-y",
                "-ss", String.valueOf(seekSeconds),
                "-i", input.toAbsolutePath().toString(),
                "-frames:v", "1",
                "-vf", "scale=" + POSTER_WIDTH + ":-2",
                "-q:v", "3",
                output.toAbsolutePath().toString()
        );

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);

        Process process = pb.start();

        try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("[ffmpeg] {}", line);
            }
        }

        boolean finished = process.waitFor(FFMPEG_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("ffmpeg timed out after " + FFMPEG_TIMEOUT.toSeconds() + "s");
        }
        if (process.exitValue() != 0) {
            throw new IOException("ffmpeg exited with code " + process.exitValue());
        }
    }

    private String posterKey(String originalKey) {
        int slash = originalKey.lastIndexOf('/');
        String dir = originalKey.substring(0, slash);
        return dir + "/thumb/poster.jpg";
    }

    private String extensionOf(String key) {
        int dot = key.lastIndexOf('.');
        return dot < 0 ? ".mp4" : key.substring(dot);
    }

    private void quietDelete(Path path) {
        if (path == null) return;
        try { Files.deleteIfExists(path); } catch (IOException ignored) {}
    }
}