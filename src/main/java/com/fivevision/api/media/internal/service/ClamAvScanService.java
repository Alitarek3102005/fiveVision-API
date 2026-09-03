package com.fivevision.api.media.internal.service;

import fi.solita.clamav.ClamAVClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.InputStream;

@Service
@Slf4j
public class ClamAvScanService {

    private final S3Client s3Client;
    private final String clamAvHost;
    private final int clamAvPort;

    public ClamAvScanService(S3Client s3Client,
                             @Value("${clamav.host:localhost}") String clamAvHost,
                             @Value("${clamav.port:3310}") int clamAvPort) {
        this.s3Client = s3Client;
        this.clamAvHost = clamAvHost;
        this.clamAvPort = clamAvPort;
    }


    public boolean scanObject(String bucketName, String fileKey) throws Exception {
        log.info("Scanning S3 object {}/{}", bucketName, fileKey);

        try (InputStream s3Stream = getS3ObjectStream(bucketName, fileKey)) {
            ClamAVClient client = new ClamAVClient(clamAvHost, clamAvPort);
            byte[] reply = client.scan(s3Stream);
            String result = new String(reply).trim();
            log.info("ClamAV scan result for {}/{}: {}", bucketName, fileKey, result);

            return result.endsWith("OK") || result.contains("OK");
        }
    }

    private InputStream getS3ObjectStream(String bucketName, String fileKey) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build();
        ResponseInputStream<GetObjectResponse> response = s3Client.getObject(getObjectRequest);
        return response;
    }
}