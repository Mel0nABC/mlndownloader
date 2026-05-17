/* SPDX-FileCopyrightText: 2025 Mel0nABC

 SPDX-License-Identifier: MIT */
package dev.mel0n.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import dev.mel0n.dto.MlnDownloadderNewEntityDTO;
import dev.mel0n.service.MlnDownloaderService;

/**
 * Controller to manage download options
 */
@Controller
@RequestMapping("/api")
public class MlnDownloaderController {

    private MlnDownloaderService mlnDownloaderService;

    /**
     * Constructir to inyect dependencies
     * 
     * @param mlnDownloaderService
     */
    public MlnDownloaderController(MlnDownloaderService mlnDownloaderService) {
        this.mlnDownloaderService = mlnDownloaderService;
    }

    /**
     * 
     * Delete download activity
     * 
     * @param fileName String file name to delete download activity
     * @return ResponseEntity with map, message value is a String text
     */
    @DeleteMapping("/downloads/{id}")
    public ResponseEntity<Map<String, Object>> deleteDownloaded(@PathVariable String id) {

        this.mlnDownloaderService.deleteDownload(UUID.fromString(id));

        return ResponseEntity
                .ok(Map.of("success", true, "message", "La descarga se elimino satisfactoriamente"));
    }

    /**
     * To clean all finished downloads from memory list
     * 
     * @return ResponseEntity with map, message value is a String text
     */
    @DeleteMapping("/downloads")
    public ResponseEntity<Map<String, Object>> cleanFinishDownloads() {

        this.mlnDownloaderService.cleanFinishDownloads();

        return ResponseEntity
                .ok(Map.of("success", true, "message", "Se eliminaron las descargas finalizadas"));
    }

    /**
     * 
     * Create new download and start automatic
     * 
     * @param mlnDownloadderEntityDTO basic informatión tu create new download
     * @return ResponseEntity with map, message value is a String text
     */
    @PostMapping("/downloads")
    public ResponseEntity<Map<String, Object>> newDownload(
            @RequestBody MlnDownloadderNewEntityDTO mlnDownloadderEntityDTO) {

        this.mlnDownloaderService.newDownload(mlnDownloadderEntityDTO);

        return ResponseEntity
                .ok(Map.of("success", true, "message", "Nueva descarga iniciada"));
    }

    /**
     * 
     * To pause or resume download activity
     * 
     * @param fileName String from download file name
     * @return ResponseEntity with map, message value is a String text
     */
    @PutMapping("/downloads/{id}")
    public ResponseEntity<Map<String, Object>> pauseOrResumeDownload(@PathVariable String id) {

        this.mlnDownloaderService.pauseOrResumeDownload(UUID.fromString(id));

        return ResponseEntity.ok(Map.of("success", true, "message", "Descarga pausada"));
    }

    /**
     * When automatic merge have some error, client send new merge petition
     * 
     * @param id
     * @return
     */
    @PostMapping("/downloads/{id}")
    public ResponseEntity<Map<String, Object>> forceMergeFiles(@PathVariable String id) {

        this.mlnDownloaderService.forceMergeFilesFromClient(UUID.fromString(id));

        return ResponseEntity.ok(Map.of("success", true, "message", "Ficheros unidos satisfactoriamente"));
    }
}