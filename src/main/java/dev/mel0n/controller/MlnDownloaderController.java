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

@Controller
@RequestMapping("/api")
public class MlnDownloaderController {

    private MlnDownloaderService mlnDownloaderService;

    public MlnDownloaderController(MlnDownloaderService mlnDownloaderService) {
        this.mlnDownloaderService = mlnDownloaderService;
    }

    @GetMapping("/main")
    public String main() {
        return "html";
    }

    @PostMapping("/downloads")
    public ResponseEntity<Map<String, Object>> newDownload(
            @RequestBody MlnDownloadderEntityDTO mlnDownloadderEntityDTO) {

        this.mlnDownloaderService.newDownload(mlnDownloadderEntityDTO);

        return ResponseEntity
                .ok(Map.of("success", true, "message", "Descarga iniciada"));
    }

    @PutMapping("/downloads/{id}")
    public ResponseEntity<Map<String, Object>> pauseOrResumeDownload(@PathVariable Long id) {

        return ResponseEntity.ok(Map.of("success", true, "message", "Descarga pausada"));
    }

    @DeleteMapping("/downloads/{id}")
    public ResponseEntity<Map<String, Object>> stopDownload(@PathVariable Long id) {

        return ResponseEntity.ok(Map.of("success", true, "message", "Descarga cancelada"));
    }

}