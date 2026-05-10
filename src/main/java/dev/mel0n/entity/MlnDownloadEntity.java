package dev.mel0n.entity;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.nio.file.Path;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
public class MlnDownloadEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private URI uri;
    private String fileName;
    private Long length;
    private int chunks;

    @Builder.Default
    private List<Path> parts = new ArrayList<>();

    @Builder.Default
    private List<Thread> threads = new ArrayList<>();

    @Builder.Default
    private boolean finalDownload = false;

    @Builder.Default
    private Long downloadedBytes = 0L;
}
