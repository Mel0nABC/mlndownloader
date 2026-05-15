package dev.mel0n.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import dev.mel0n.dto.MlnDownloadderEntityDTO;
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
     * Obtain all download activity
     * 
     * @return ResponseEntity with map, message value List<MlnDownloaderEntity> with
     *         all download activity
     */
    @GetMapping("/downloads")
    public ResponseEntity<Map<String, Object>> getAllDownloads() {
        return ResponseEntity
                .ok(Map.of("success", true, "message", this.mlnDownloaderService.getAllDownloads()));
    }

    /**
     * 
     * Delete download activity
     * 
     * @param id Long to delete download activity
     * @return ResponseEntity with map, message value is a String text
     */
    @DeleteMapping("/downloads/{fileName}")
    public ResponseEntity<Map<String, Object>> deleteDownloaded(@PathVariable String fileName) {

        this.mlnDownloaderService.deleteDownload(fileName);

        return ResponseEntity
                .ok(Map.of("success", true, "message", "La descarga se elimino satisfactoriamente"));
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
            @RequestBody MlnDownloadderEntityDTO mlnDownloadderEntityDTO) {

        this.mlnDownloaderService.newDownload(mlnDownloadderEntityDTO);

        return ResponseEntity
                .ok(Map.of("success", true, "message", "Nueva descarga iniciada"));
    }

    /**
     * 
     * To pause or resume download activity
     * 
     * @param id Long from download activity
     * @return ResponseEntity with map, message value is a String text
     */
    @PutMapping("/downloads/{fileName}")
    public ResponseEntity<Map<String, Object>> pauseOrResumeDownload(@PathVariable String fileName) {

        this.mlnDownloaderService.pauseOrResumeDownload(fileName);

        return ResponseEntity.ok(Map.of("success", true, "message", "Descarga pausada"));
    }
}