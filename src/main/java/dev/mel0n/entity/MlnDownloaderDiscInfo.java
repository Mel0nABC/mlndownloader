package dev.mel0n.entity;

import java.nio.file.Path;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
/**
 * Class to capture disc information
 */
public class MlnDownloaderDiscInfo {

    private Path path;
    private Long totalSpace;
    private Long freeSpace;
    private boolean isWritable;
    private boolean isReadable;
    private boolean isExecutable;
    private boolean isSpaceSuficient;

}
