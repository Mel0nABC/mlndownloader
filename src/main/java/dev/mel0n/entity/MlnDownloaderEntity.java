package dev.mel0n.entity;

import java.net.URI;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.nio.file.Path;

import dev.mel0n.service.MlnDownloaderService;

/**
 * Class to save download activity
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class MlnDownloaderEntity {

    private URI uri;
    private String fileName;
    private Long length;

    @Builder.Default
    private int chunks = 1;

    @Builder.Default
    private Map<Path, String> parts = new TreeMap<>(
            Comparator.comparingInt(path -> {
                return Integer.parseInt(path.toString().split(MlnDownloaderService.SUFIX)[1]);
            }));

    @Builder.Default
    private List<CompletableFuture<HttpResponse<Path>>> futures = new ArrayList<>();

    @Builder.Default
    private boolean isDownloading = true;

    @Builder.Default
    private boolean isDownloaded = false;

    @Builder.Default
    private Long downloadedBytes = 0L;

    @Builder.Default
    private List<Thread> workers = new ArrayList<>();
}