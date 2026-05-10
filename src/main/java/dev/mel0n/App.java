package dev.mel0n;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import dev.mel0n.entity.MlnDownloadEntity;
import dev.mel0n.service.MlnDownloadService;

public class App {

    public static void main(String[] args) {

        try {

            Long startAppTime = System.currentTimeMillis();

            MlnDownloadService mlnDownloadService = new MlnDownloadService();

            URI uri = new URI(
                    "https://es.mirrors.cicku.me/archlinux/iso/2026.05.01/archlinux-2026.05.01-x86_64.iso");
            int chunks = 20;

            MlnDownloadEntity mlnDownloadEntity = mlnDownloadService.getDownloadEntityInfo(uri, chunks);
            mlnDownloadService.startDownload(mlnDownloadEntity);

            Long endAppTime = System.currentTimeMillis();
            System.out.println("TIEMPO TRASCURRIDO EN LA DESCARGA: " + (endAppTime - startAppTime) / 1000);

            if (Files.exists(Path.of(mlnDownloadEntity.getFileName()))) {
                System.out.println("############################## DESCARGADO ##############################");
                System.out.println("TAMAÑO EN WEB: " + mlnDownloadEntity.getLength() + " bytes");
                System.out
                        .println("TAMAÑO EN DISCO: " + Files.size(Path.of(mlnDownloadEntity.getFileName())) + " bytes");
                System.out.println("DESCARGADO: "
                        + (mlnDownloadEntity.getLength() == Files.size(Path.of(mlnDownloadEntity.getFileName()))
                                ? "CORRECTAMENTE"
                                : "POSIBLES FALLOS"));
                System.out.println("########################################################################");
            }

        } catch (URISyntaxException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}
