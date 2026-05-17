package dev.mel0n.entity;

import java.nio.file.Path;

import lombok.Builder;
import lombok.ToString;

@Builder
@ToString
public class MlnDownloaderDiscInfo {

    private Path path;
    private Long totalSpace;
    private Long freeSpace;
    private boolean isWritable;
    private boolean isReadable;
    private boolean isExecutable;

}
