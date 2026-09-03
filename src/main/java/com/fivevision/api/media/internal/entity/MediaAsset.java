package com.fivevision.api.media.internal.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.domain.Persistable;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "media_assets")
//@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaAsset implements Persistable<UUID> {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "uploader_id")
    private UUID uploaderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 50, nullable = false)
    private MediaType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    @Builder.Default
    private MediaStatus status = MediaStatus.PROCESSING;

    @Column(name = "bucket_name", length = 100, nullable = false)
    private String bucketName;

    @Column(name = "file_key", length = 512, nullable = false)
    private String fileKey;

    @Column(name = "cdn_url", length = 512, nullable = false)
    private String cdnUrl;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "mime_type", length = 50)
    private String mimeType;

    @Column(name = "resolution_width")
    private Integer resolutionWidth;

    @Column(name = "resolution_height")
    private Integer resolutionHeight;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "camera_model", length = 100)
    private String cameraModel;

    @Column(name = "lens_info", length = 100)
    private String lensInfo;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Column(name = "deleted_by")
    private UUID deletedBy;

    @Override
    public boolean isNew() {
        return version == null;
    }
}