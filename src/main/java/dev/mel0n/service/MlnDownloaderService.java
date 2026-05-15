package dev.mel0n.service;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartException;

import dev.mel0n.dto.MlnDownloadderEntityDTO;
import dev.mel0n.entity.MlnDownloaderEntity;
import dev.mel0n.exception.FileAlreadyDownloadederException;
import dev.mel0n.exception.FileAlreadyInDownloadListException;
import dev.mel0n.exception.FileNotFoundException;

/**
 * Multi thread download service
 *
 */
@Service
public class MlnDownloaderService {

    private HttpClient client = HttpClient.newHttpClient();
    public static final String SUFIX = "_PART_";
    private final Long BYTE_TO_MBYTE = 1000000L;
    private final int SOME_BYTE = 1;
    private List<MlnDownloaderEntity> mlnDownloadList = new ArrayList<>();

    /**
     * Send all download activity
     * 
     * @return list from downloads information
     */
    public List<MlnDownloaderEntity> getAllDownloads() {
        return this.mlnDownloadList;
    }

    /**
     * To start new download
     * 
     * @param mlnDownloadderEntityDTO basic information to create new download
     */
    public void newDownload(MlnDownloadderEntityDTO mlnDownloaderEntityDTO) {

        URI uri = mlnDownloaderEntityDTO.uri();
        int chunks = mlnDownloaderEntityDTO.chunks();
        String fileName = mlnDownloaderEntityDTO.fileName();
        Long length = 0L;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());

            length = Long.parseLong(response.headers().map().get("content-length").getFirst());

            checkNewDownloadExist(fileName, length);

            MlnDownloaderEntity mlnDownloaderEntity = MlnDownloaderEntity.builder()
                    .uri(uri)
                    .fileName(fileName)
                    .length(length)
                    .chunks(chunks)
                    .build();

            mlnDownloadList.add(mlnDownloaderEntity);

            startDownload(mlnDownloaderEntity);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }

    /**
     * When new download is create, start this download
     * 
     * @param mlnDownloaderEntity
     */
    public void startDownload(MlnDownloaderEntity mlnDownloaderEntity) {
        try {

            Long length = mlnDownloaderEntity.getLength();
            Long chunkSize = length / mlnDownloaderEntity.getChunks();

            Long start = 0L;
            int count = 0;

            System.out.println("################################# Descargando #################################");

            while (start < length) {

                long finalStart = start;

                Long finalEnd = (start + chunkSize) > length ? length : start + chunkSize - SOME_BYTE;

                String partFileName = mlnDownloaderEntity.getFileName() + SUFIX + count;

                this.downPartFile(mlnDownloaderEntity, finalStart, finalEnd, partFileName);

                start = finalEnd + SOME_BYTE;
                count++;

            }

            System.out.println("###############################################################################");

            startDownloadControl(mlnDownloaderEntity);

        } catch (Exception e) {
            // System.out.println("PARADOOOOOOOOOOOOOOOOO");
        }
    }

    public void startDownloadControl(MlnDownloaderEntity mlnDownloaderEntity) {
        controlDownloaderSize(mlnDownloaderEntity);
        startSpeedControl(mlnDownloaderEntity);

        for (CompletableFuture<HttpResponse<Path>> t : mlnDownloaderEntity.getFutures()) {
            System.err.println("JOINING");
            t.join();
        }

        new Thread(() -> {
            multipartMergeAndDelete(mlnDownloaderEntity);
        }).start();

        System.out.println("FINISH DOWNLOAD AND MERGE");
    }

    /**
     * When download have multi files, merge all in finish file
     * 
     * @param mlnDownloadEntity
     */
    public void multipartMergeAndDelete(MlnDownloaderEntity mlnDownloadEntity) {

        try {

            stopDownloadWorkers(mlnDownloadEntity);

            System.out.println("############################ Merge downloaded files ############################");

            try (OutputStream out = Files.newOutputStream(Path.of(mlnDownloadEntity.getFileName()))) {

                for (Path part : mlnDownloadEntity.getParts().keySet()) {

                    System.out.println("MERGE: " + part);

                    Files.copy(part, out);

                }
            }

            System.out.println("################################################################################");

            if (!mlnDownloadEntity.getLength().equals(mlnDownloadEntity.getDownloadedBytes())) {

                throw new MultipartException("Some problem to merge all files");

            } else {

                System.out.println("######################## Delete downloaded temp files ########################");

                for (Path part : mlnDownloadEntity.getParts().keySet()) {

                    File file = new File(part.toString());

                    if (file.exists())
                        file.delete();

                    if (!file.exists())
                        System.out.println("DELETE: " + part);

                }

                System.out.println("##############################################################################");
            }

            if (Files.exists(Path.of(mlnDownloadEntity.getFileName()))) {
                mlnDownloadEntity.setDownloading(false);
                mlnDownloadEntity.setDownloaded(true);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * When download have multi part, get independent downloads
     * 
     * @param mlnDownloadEntity download information
     * @param start             start chunk range
     * @param end               end chunk range
     * @param partFileName      chunk file name
     */
    public void downPartFile(MlnDownloaderEntity mlnDownloadEntity, Long start, Long end, String partFileName) {

        File partFile = new File(partFileName);

        if (mlnDownloadEntity.getParts().get(partFile.toPath()) == null) {
            mlnDownloadEntity.getParts().put(partFile.toPath(), start + "-" + end);
        }

        HttpRequest requestFile = HttpRequest.newBuilder()
                .uri(mlnDownloadEntity.getUri())
                .header("Range", "bytes=" + start + "-" + end)
                .GET()
                .build();

        CompletableFuture<HttpResponse<Path>> future = client.sendAsync(
                requestFile,
                HttpResponse.BodyHandlers.ofFile(
                        partFile.toPath(),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.APPEND));

        mlnDownloadEntity.getFutures().add(future);

        System.out.println("DOWNLOAD: " + partFile + " - " + "Range bytes=" + start + "-" + end);

    }

    public void deleteDownload(String fileName) {

        File file = new File(fileName);

        if (!file.exists())
            throw new FileNotFoundException("El archivo que intenta eliminar no existe.");

        pauseOrResumeDownload(fileName);

        Optional<MlnDownloaderEntity> mOptional = mlnDownloadList.stream().filter(m -> m.getFileName().equals(fileName))
                .findFirst();

        if (mOptional.isEmpty())
            throw new FileNotFoundException("El archivo que intenta eliminar no está en la lista de descargas.");

        mOptional.get().getParts().keySet().forEach(p -> {

            File partToDelete = new File(p.toString());

            if (partToDelete.exists())
                partToDelete.delete();

            if (file.exists())
                file.delete();

        });

        mlnDownloadList.remove(mOptional.get());

        System.out.println(mlnDownloadList);

    }

    public void pauseOrResumeDownload(String fileName) {

        Optional<MlnDownloaderEntity> mOptional = mlnDownloadList.stream()
                .filter(down -> down.getFileName().equals(fileName))
                .findFirst();

        if (mOptional.isEmpty())
            throw new FileNotFoundException("El archivo que quieres pausar no se encuntra en la lista");

        MlnDownloaderEntity mlnDownloaderEntity = mOptional.get();

        if (mlnDownloaderEntity.isDownloading()) {

            System.out.println("################################# Pause download #################################");

            for (CompletableFuture<HttpResponse<Path>> future : mlnDownloaderEntity.getFutures()) {
                try {
                    future.cancel(true);
                    System.out.println("PAUSE DOWLOAD: " + fileName);
                } catch (Exception e) {
                    System.out.println("PETOOOOOOOOO");
                }
            }

            System.out.println("##################################################################################");

            stopDownloadWorkers(mlnDownloaderEntity);

            mlnDownloaderEntity.setDownloading(false);

            mlnDownloaderEntity.getFutures().clear();

            return;
        }

        System.out.println("################################# Resume download #################################");

        mlnDownloaderEntity.getParts().keySet().forEach(p -> {

            File fileSizeDownloaded = new File(p.toString());

            String startAndEnd = mlnDownloaderEntity.getParts().get(p);

            Long start = Long.parseLong(startAndEnd.split("-")[0]);
            Long finalStart = start + fileSizeDownloaded.length();

            Long finalEnd = Long.parseLong(startAndEnd.split("-")[1]);

            this.downPartFile(mlnDownloaderEntity, finalStart, finalEnd, p.toString());

            System.out.println("RESUME DOWNLOAD -> " + p);

        });

        mlnDownloaderEntity.setDownloading(true);

        startDownloadControl(mlnDownloaderEntity);

        System.out.println("##################################################################################");
    }

    /**
     * Comprove if downloading file exist in database and local folder
     * 
     * @param fileName file name to check
     * @param length   file size in bytes
     */
    public void checkNewDownloadExist(String fileName, Long length) {

        File checkFile = new File(fileName);

        if (checkFile.exists() & (checkFile.length() == length)) {
            throw new FileAlreadyDownloadederException("El archivo ya existe localmente");
        }

        Optional<MlnDownloaderEntity> mOptional = mlnDownloadList.stream().filter(m -> m.getFileName().equals(fileName))
                .findFirst();

        if (!mOptional.isEmpty()) {
            throw new FileAlreadyInDownloadListException("Este archivo está en la lista de descarga");
        }

    }

    /**
     * Convert bytes to mbytes
     * 
     * @param bytes bytes size
     * @return Long with mbytes size
     */
    public Long getByteToMbyte(Long bytes) {
        return bytes / BYTE_TO_MBYTE;
    }

    /**
     * Calculate downloaded bytes in one second
     * 
     * @param partFilename
     * @param mlnDownloadEntity
     */
    public void getDownloadSpeed(String partFilename, MlnDownloaderEntity mlnDownloadEntity) {

        while (!mlnDownloadEntity.isDownloaded()) {
            File file = new File(partFilename);

            if (file.exists()) {

                if (partFilename.endsWith("_PART_1")) {

                    Long startSize = file.length();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {

                    }
                    Long endSize = file.length();
                }

            }
        }

    }

    public void controlDownloaderSize(MlnDownloaderEntity mlnDownloadEntity) {

        Thread threadSetDownloadedSize = new Thread(() -> {
            while (!mlnDownloadEntity.isDownloaded()) {

                AtomicLong downloaderFilesSize = new AtomicLong(0);

                mlnDownloadEntity.getParts().keySet().forEach(p -> {

                    File file = new File(p.toString());

                    if (file.exists())
                        downloaderFilesSize.addAndGet(file.length());

                });

                mlnDownloadEntity.setDownloadedBytes(downloaderFilesSize.get());
            }

        });

        threadSetDownloadedSize.start();

        mlnDownloadEntity.getWorkers().add(threadSetDownloadedSize);
    }

    public void startSpeedControl(MlnDownloaderEntity mlnDownloadEntity) {
        mlnDownloadEntity.getParts().keySet().forEach(p -> {
            Thread threadSpeed = new Thread(() -> {
                this.getDownloadSpeed(p.toString(), mlnDownloadEntity);
            });
            threadSpeed.setName(p.toString());
            threadSpeed.start();
            mlnDownloadEntity.getWorkers().add(threadSpeed);
        });
    }

    /**
     * To stop thread
     * 
     * @param mlnDownloaderEntity
     */
    public void stopDownloadWorkers(MlnDownloaderEntity mlnDownloaderEntity) {
        for (Thread t : mlnDownloaderEntity.getWorkers()) {
            t.interrupt();
        }
    }

}
