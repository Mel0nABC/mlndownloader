package dev.mel0n.dto;

import java.net.URI;
import java.util.List;

import dev.mel0n.entity.MlnDownloaderPartFile;
import lombok.Builder;

/**
 * Dto to send information to front
 */
@Builder
public record MlnDownloaderDownloadFileDTO(URI uri,
        String filePath,
        Long length,
        int chunks,
        List<MlnDownloaderPartFile> parts,
        boolean isDownloading,
        boolean isDownloaded,
        boolean isFileExist,
        Long downloadedBytes) {

}
