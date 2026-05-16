package dev.mel0n.entity;

import java.io.Serializable;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import dev.mel0n.dto.MlnDownloaderDownloadFileDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.nio.file.Path;
import java.util.UUID;

/**
 * Class to save download activity
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class MlnDownloaderDownloadFile implements Serializable {

    @Builder.Default
    private UUID id = UUID.randomUUID();
    private URI uri;
    private String filePath;
    private Long length;

    @Builder.Default
    private int chunks = 1;

    @Builder.Default
    private List<MlnDownloaderPartFile> parts = new ArrayList<>();

    @Builder.Default
    private transient List<CompletableFuture<HttpResponse<Path>>> futures = new ArrayList<>();

    @Builder.Default
    private boolean isDownloading = true;

    @Builder.Default
    private boolean isDownloaded = false;

    @Builder.Default
    private boolean isFileExist = false;

    @Builder.Default
    private Long downloadedBytes = 0L;

    public MlnDownloaderDownloadFileDTO toDTO() {
        return MlnDownloaderDownloadFileDTO.builder()
                .id(id)
                .uri(uri)
                .filePath(filePath)
                .length(length)
                .chunks(chunks)
                .parts(parts)
                .isDownloading(isDownloading)
                .isDownloaded(isDownloaded)
                .isFileExist(isFileExist)
                .downloadedBytes(downloadedBytes)
                .build();
    }

}