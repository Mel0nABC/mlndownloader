package dev.mel0n.entity;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import dev.mel0n.converter.PathConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.nio.file.Path;

/**
 * Class to save download activity
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
public class MlnDownloaderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private URI uri;

    @Column(nullable = false, unique = true)
    private String fileName;

    @Column(nullable = false)
    private Long length;

    @Builder.Default
    private int chunks = 1;

    @Convert(converter = PathConverter.class)
    @Column(length = 99999)
    @Builder.Default
    private List<Path> parts = new ArrayList<>();

    @Builder.Default
    @Transient
    private List<Thread> threads = new ArrayList<>();

    @Builder.Default
    private boolean finalDownload = false;

    @Builder.Default
    private Long downloadedBytes = 0L;
}
