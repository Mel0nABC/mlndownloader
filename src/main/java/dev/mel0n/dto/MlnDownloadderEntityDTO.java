package dev.mel0n.dto;

import java.net.URI;

import lombok.Builder;

/**
 * DTO to create new download
 */
@Builder
public record MlnDownloadderEntityDTO(
                URI uri,
                int chunks,
                String fileName) {
}
