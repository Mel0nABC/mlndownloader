package dev.mel0n;

import java.net.URI;
import java.net.URISyntaxException;

import dev.mel0n.entity.MlnDownloadEntity;
import dev.mel0n.service.MlnDownloadService;

public class App {

    public static void main(String[] args) {
        Long startAppTime = System.currentTimeMillis();

        MlnDownloadService mlnDownloadService = new MlnDownloadService();

        try {
            URI uri = new URI(
                    "https://es.mirrors.cicku.me/archlinux/iso/2026.05.01/archlinux-2026.05.01-x86_64.iso");
            int chunks = 10;

            MlnDownloadEntity mlnDownloadEntity = mlnDownloadService.getDownloadEntityInfo(uri, chunks);
            mlnDownloadService.startDownload(mlnDownloadEntity);
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Long endAppTime = System.currentTimeMillis();
        System.out.println("TIEMPO TRASCURRIDO EN LA DESCARGA: " + (endAppTime - startAppTime) / 1000);
    }

}
