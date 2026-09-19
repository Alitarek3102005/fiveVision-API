package com.fivevision.api.media.internal.service;

import fi.solita.clamav.ClamAVClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.net.SocketTimeoutException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class ClamAvScanService {

    private final S3Client s3Client;
    private final String clamAvHost;
    private final int clamAvPort;
    private final int scanTimeoutMs;
    private final int pingTimeoutMs;

    public ClamAvScanService(
            S3Client s3Client,
            @Value("${clamav.host:localhost}") String clamAvHost,
            @Value("${clamav.port:3310}") int clamAvPort,
            @Value("${clamav.scan-timeout-ms:60000}") int scanTimeoutMs,
            @Value("${clamav.ping-timeout-ms:3000}") int pingTimeoutMs) {
        this.s3Client = s3Client;
        this.clamAvHost = clamAvHost;
        this.clamAvPort = clamAvPort;
        this.scanTimeoutMs = scanTimeoutMs;
        this.pingTimeoutMs = pingTimeoutMs;
    }

    public boolean ping() {
        try {
            ClamAVClient client = new ClamAVClient(clamAvHost, clamAvPort, pingTimeoutMs);
            return client.ping();
        } catch (Exception e) {
            log.warn("ClamAV ping failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean scanObject(String bucket, String key) {
        CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            try (ResponseInputStream<GetObjectResponse> s3Stream =
                         s3Client.getObject(request)) {

                ClamAVClient client = new ClamAVClient(clamAvHost, clamAvPort, scanTimeoutMs);
                byte[] reply = client.scan(s3Stream);
                log.info("ClamAV scan result for {}/{}: {}",
                        bucket, key, new String(reply).trim());
                return ClamAVClient.isCleanReply(reply);

            } catch (Exception e) {
                throw new RuntimeException("ClamAV scan failed: " + e.getMessage(), e);
            }
        });

        try {
            return future.get(scanTimeoutMs + 5_000L, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new RuntimeException(
                    "ClamAV scan timed out after " + scanTimeoutMs + " ms", e);
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof SocketTimeoutException) {
                throw new RuntimeException(
                        "ClamAV scan timed out (socket). The engine took too long to respond.",
                        cause);
            }
            throw new RuntimeException(
                    "Malware scan failed due to system/network error: " + cause.getMessage(),
                    cause);
        }
    }
}