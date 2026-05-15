package dev.mel0n.entity;

import java.io.Serializable;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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
public class MlnDownloaderEntity implements Serializable {

    private URI uri;
    private String filePath;
    private Long length;

    @Builder.Default
    private int chunks = 1;

    @Builder.Default
    private Map<String, String> parts = new HashMap<>();

    @Builder.Default
    private transient List<CompletableFuture<HttpResponse<Path>>> futures = new ArrayList<>();

    @Builder.Default
    private boolean isDownloading = true;

    @Builder.Default
    private boolean isDownloaded = false;

    @Builder.Default
    private Long downloadedBytes = 0L;

    @Builder.Default
    private transient List<Thread> workers = new ArrayList<>();
}