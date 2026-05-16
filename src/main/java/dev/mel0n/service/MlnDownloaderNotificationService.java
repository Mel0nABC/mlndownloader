package dev.mel0n.service;

import java.util.List;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import dev.mel0n.dto.MlnDownloaderDownloadFileDTO;
import dev.mel0n.entity.MlnDownloaderDownloadFile;

@Service
public class MlnDownloaderNotificationService {

    private final SimpMessagingTemplate template;

    public MlnDownloaderNotificationService(SimpMessagingTemplate template, MlnDownloaderService mlnDownloaderService) {
        this.template = template;
        new Thread(() -> {
            starNotificationThread(mlnDownloaderService);
        }).start();
    }

    public void sendFiles(List<MlnDownloaderDownloadFileDTO> list) {
        template.convertAndSend("/api/downloads", list);
    }

    public void starNotificationThread(MlnDownloaderService mlnDownloaderService) {
        while (true) {
            try {
                Thread.sleep(500);

                sendFiles(mlnDownloaderService.getMlnDownloadList().stream().map(MlnDownloaderDownloadFile::toDTO)
                        .toList());

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
