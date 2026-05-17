package dev.mel0n.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import dev.mel0n.dto.MlnDownloaderDownloadFileDTO;
import dev.mel0n.entity.MlnDownloaderDownloadFile;

/**
 * Service to send some information to boadcast
 */
@Service
public class MlnDownloaderNotificationService {

    private final SimpMessagingTemplate template;

    public MlnDownloaderNotificationService(SimpMessagingTemplate template, MlnDownloaderService mlnDownloaderService) {
        this.template = template;
        new Thread(() -> {
            starNotificationThread(mlnDownloaderService);
        }).start();
    }

    /**
     * Generate new thread to notify information
     * 
     * @param mlnDownloaderService obtain list downloads
     */
    public void starNotificationThread(MlnDownloaderService mlnDownloaderService) {
        while (true) {
            try {
                Thread.sleep(500);

                sendFiles(mlnDownloaderService.getMlnDownloadList().stream().map(MlnDownloaderDownloadFile::toDTO)
                        .toList());

                try {

                    Path path = MlnDownloaderService.getDOWNLOAD_FOLDER();

                    FileStore fileStore = Files.getFileStore(path);

                    Long totalSpace = fileStore.getTotalSpace() / 1000 / 1000 / 1000;
                    Long freeSpace = fileStore.getUsableSpace() / 1000 / 1000 / 1000;

                    sendDiscStatus(freeSpace + " / " + totalSpace + " GB");
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Notify to topic downloads information
     * 
     * @param list download information list
     */
    public void sendFiles(List<MlnDownloaderDownloadFileDTO> list) {
        template.convertAndSend("/topic/downloads", list);
    }

    /***
     * Notify to topic disc information
     * 
     * @param disc string with free and total space
     */
    public void sendDiscStatus(String disc) {
        template.convertAndSend("/topic/disc_info", disc);
    }

}
