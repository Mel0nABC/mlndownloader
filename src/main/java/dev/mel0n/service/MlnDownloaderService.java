package dev.mel0n.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
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
    private final Path DOWNLOAD_FOLDER = Path.of("/home/mel0n/Downloads/PROGRAMACION/mlnDownloader/downloads");

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
        String filePath = DOWNLOAD_FOLDER + "/" + mlnDownloaderEntityDTO.fileName();
        Long length = 0L;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());

            length = Long.parseLong(response.headers().map().get("content-length").getFirst());

            checkNewDownloadExist(filePath, length);

            MlnDownloaderEntity mlnDownloaderEntity = MlnDownloaderEntity.builder()
                    .uri(uri)
                    .filePath(filePath)
                    .length(length)
                    .chunks(chunks)
                    .build();

            mlnDownloadList.add(mlnDownloaderEntity);

            startDownload(mlnDownloaderEntity);

            saveDownloadList();

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

                String partFileName = mlnDownloaderEntity.getFilePath() + SUFIX + count;

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

    /**
     * When all download parts are created, start all processos to download parts.
     * Wait to finish download parts and merge now
     * 
     * @param mlnDownloaderEntity
     */
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

            mlnDownloadEntity.getParts().entrySet().stream().sorted(Comparator.comparingInt(path -> {
                return Integer.parseInt(path.toString().split(MlnDownloaderService.SUFIX)[1]);
            }));

            System.out.println("############################ Merge downloaded files ############################");

            try (OutputStream out = Files.newOutputStream(Path.of(mlnDownloadEntity.getFilePath()))) {

                for (String part : mlnDownloadEntity.getParts().keySet()) {

                    System.out.println("MERGE: " + part);

                    Files.copy(Path.of(part), out);

                }
            }

            System.out.println("################################################################################");

            if (!mlnDownloadEntity.getLength().equals(mlnDownloadEntity.getDownloadedBytes())) {

                throw new MultipartException("Some problem to merge all files");

            } else {

                System.out.println("######################## Delete downloaded temp files ########################");

                for (String part : mlnDownloadEntity.getParts().keySet()) {

                    File file = new File(part);

                    if (file.exists())
                        file.delete();

                    if (!file.exists())
                        System.out.println("DELETE: " + part);

                }

                System.out.println("##############################################################################");
            }

            if (Files.exists(Path.of(mlnDownloadEntity.getFilePath()))) {
                mlnDownloadEntity.setDownloading(false);
                mlnDownloadEntity.setDownloaded(true);
                mlnDownloadEntity.setDownloadedBytes(mlnDownloadEntity.getLength());
                mlnDownloadEntity.getWorkers().clear();
            }

            saveDownloadList();

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

        if (mlnDownloadEntity.getParts().get(partFileName) == null) {
            mlnDownloadEntity.getParts().put(partFileName, start + "-" + end);
        }

        HttpRequest requestFile = HttpRequest.newBuilder()
                .uri(mlnDownloadEntity.getUri())
                .header("Range", "bytes=" + start + "-" + end)
                .GET()
                .build();

        CompletableFuture<HttpResponse<Path>> future = client.sendAsync(
                requestFile,
                HttpResponse.BodyHandlers.ofFile(
                        Path.of(partFileName),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.APPEND));

        mlnDownloadEntity.getFutures().add(future);

        System.out.println("DOWNLOAD: " + partFileName + " - " + "Range bytes=" + start + "-" + end);

    }

    /**
     * Stop and delete actual download
     * 
     * @param fileName name for file to stop
     */
    public void deleteDownload(String fileName) {

        Optional<MlnDownloaderEntity> mOptional = mlnDownloadList.stream()
                .filter(m -> m.getFilePath().endsWith(fileName))
                .findFirst();

        if (mOptional.isEmpty())
            throw new FileNotFoundException("El archivo que intenta eliminar no está en la lista de descargas.");

        MlnDownloaderEntity mlnDownloaderEntity = mOptional.get();

        if (mlnDownloaderEntity.isDownloading())
            pauseOrResumeDownload(fileName);

        System.out.println("######################## Delete files ########################");

        mlnDownloaderEntity.getParts().keySet().forEach(p -> {

            File partToDelete = new File(p.toString());

            if (partToDelete.exists()) {
                partToDelete.delete();
                System.out.println("DELETE PART: " + partToDelete);
            }

            File file = new File(DOWNLOAD_FOLDER + "/" + fileName);

            if (file.exists()) {
                System.out.println("DELETE FILE: " + file);
                file.delete();
            }

        });

        System.out.println("##############################################################");

        mlnDownloadList.remove(mOptional.get());

        saveDownloadList();
    }

    /**
     * Clean downloaded files from memory
     */
    public void cleanFinishDownloads() {
        this.mlnDownloadList = new ArrayList<>(mlnDownloadList.stream().filter(m -> !m.isDownloaded()).toList());
        saveDownloadList();
        System.out.println("######################## CLEANED FINISH DOWNLOADS ########################");
    }

    /**
     * Pause or resume download with file name
     * 
     * @param fileName
     */
    public void pauseOrResumeDownload(String fileName) {

        String filePath = DOWNLOAD_FOLDER + "/" + fileName;

        if (Files.exists(Path.of(filePath)))
            throw new FileAlreadyDownloadederException("El archivo que quieres resumir ya está descargado");

        Optional<MlnDownloaderEntity> mOptional = mlnDownloadList.stream()
                .filter(down -> down.getFilePath().endsWith(fileName))
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

            saveDownloadList();

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

        saveDownloadList();

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

        Optional<MlnDownloaderEntity> mOptional = mlnDownloadList.stream()
                .filter(m -> m.getFilePath().endsWith(fileName))
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

    /**
     * Update size from download file
     * 
     * @param mlnDownloadEntity
     */
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

                this.saveDownloadList();
            }

        });

        threadSetDownloadedSize.start();

        mlnDownloadEntity.getWorkers().add(threadSetDownloadedSize);
    }

    /**
     * Start new thread to control download speed
     * 
     * @param mlnDownloadEntity
     */
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

    /**
     * To save list files information
     */
    public void saveDownloadList() {

        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("data.bin"))) {

            out.writeObject(this.mlnDownloadList);

        } catch (java.io.FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public List<MlnDownloaderEntity> getMlnDownloadList() {
        return mlnDownloadList;
    }

    public void setMlnDownloadList(List<MlnDownloaderEntity> mlnDownloadList) {
        this.mlnDownloadList = mlnDownloadList;
    }

    public Path getDOWNLOAD_FOLDER() {
        return DOWNLOAD_FOLDER;
    }

}
