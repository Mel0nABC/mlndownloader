package dev.mel0n.entity;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

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
public class MlnDownloadEntity {
    private Long id;
    private URI uri;
    private String fileName;
    private Long length;
    private int chunks;

    @Builder.Default
    private List<Path> parts = new ArrayList<>();

    @Builder.Default
    private boolean finalDownload = false;

    @Builder.Default
    private Long downloaded = 0L;
}
