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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartException;

import dev.mel0n.dto.MlnDownloadderNewEntityDTO;
import dev.mel0n.entity.MlnDownloaderDownloadFile;
import dev.mel0n.entity.MlnDownloaderPartFile;
import dev.mel0n.exception.FileAlreadyDownloadederException;
import dev.mel0n.exception.FileAlreadyInDownloadListException;
import dev.mel0n.exception.FileAlreadyMerginException;
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
    private List<MlnDownloaderDownloadFile> mlnDownloadList = new ArrayList<>();
    private static final Path DOWNLOAD_FOLDER = Path.of("/home/mel0n/Downloads/PROGRAMACION/mlnDownloader/downloads");

    /**
     * To start new download
     * 
     * @param mlnDownloadderEntityDTO basic information to create new download
     */
    public void newDownload(MlnDownloadderNewEntityDTO mlnDownloaderEntityDTO) {

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

            MlnDownloaderDownloadFile mlnDownloaderEntity = MlnDownloaderDownloadFile.builder()
                    .uri(uri)
                    .filePath(filePath)
                    .length(length)
                    .chunks(chunks)
                    .build();

            mlnDownloadList.add(mlnDownloaderEntity);

            saveDownloadList();

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
    public void startDownload(MlnDownloaderDownloadFile mlnDownloaderEntity) {
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

            new Thread(() -> {
                startDownloadControl(mlnDownloaderEntity);
            }).start();

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
    public void startDownloadControl(MlnDownloaderDownloadFile mlnDownloaderEntity) {

        controlDownloaderSize(mlnDownloaderEntity);

        try {
            for (CompletableFuture<HttpResponse<Path>> t : mlnDownloaderEntity.getFutures()) {
                System.err.println("JOINING");
                t.join();
            }
        } catch (java.util.concurrent.CancellationException e) {
            System.out.println("Se canceló la descarga por parte del usuario");
        }

        Long checkFileSizeOnParts = 0L;

        for (MlnDownloaderPartFile p : mlnDownloaderEntity.getParts()) {
            try {
                checkFileSizeOnParts += Files.size(Path.of(p.getPath()));
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        if (!mlnDownloaderEntity.getLength().equals(checkFileSizeOnParts)) {
            System.out.println("EL TAMAÑO TOTAL NO COINCIDE: LENGTH: " + mlnDownloaderEntity.getLength() + ", PARTS: "
                    + checkFileSizeOnParts);
            return;
        }

        mlnDownloaderEntity.setDownloaded(true);

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
    public void multipartMergeAndDelete(MlnDownloaderDownloadFile mlnDownloadEntity) {

        try {

            if (!mlnDownloadEntity.isDownloaded())
                return;

            mlnDownloadEntity.getParts().stream().sorted(Comparator.comparingInt(path -> {
                return Integer.parseInt(path.toString().split(MlnDownloaderService.SUFIX)[1]);
            }));

            System.out.println("############################ Merge downloaded files ############################");

            try (OutputStream out = Files.newOutputStream(Path.of(mlnDownloadEntity.getFilePath()))) {

                for (MlnDownloaderPartFile part : mlnDownloadEntity.getParts()) {

                    System.out.println("MERGE: " + part.getPath());

                    Files.copy(Path.of(part.getPath()), out);

                }
            }

            System.out.println("################################################################################");

            if (!mlnDownloadEntity.getLength().equals(Files.size(Path.of(mlnDownloadEntity.getFilePath())))) {

                throw new MultipartException("Some problem on merge all files");

            } else {

                System.out.println("######################## Delete downloaded temp files ########################");

                for (MlnDownloaderPartFile part : mlnDownloadEntity.getParts()) {

                    File file = new File(part.getPath());

                    if (file.exists())
                        file.delete();

                    if (!file.exists())
                        System.out.println("DELETE: " + part.getPath());

                }

                System.out.println("##############################################################################");
            }

            if (Files.exists(Path.of(mlnDownloadEntity.getFilePath()))) {
                mlnDownloadEntity.setDownloading(false);
                mlnDownloadEntity.setDownloaded(true);
                mlnDownloadEntity.setFileExist(true);
                mlnDownloadEntity.setDownloadedBytes(mlnDownloadEntity.getLength());
                mlnDownloadEntity.getFutures().clear();
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
    public void downPartFile(MlnDownloaderDownloadFile mlnDownloadEntity, Long start, Long end, String partFileName) {

        if (mlnDownloadEntity.getParts().stream().filter(p -> p.getPath().equals(partFileName)).findFirst().isEmpty()) {

            mlnDownloadEntity.getParts().add(MlnDownloaderPartFile.builder()
                    .path(partFileName)
                    .length(end - start)
                    .actualSize(0L)
                    .start(start)
                    .end(end)
                    .build());

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

        if (mlnDownloadEntity.getFutures() == null)
            mlnDownloadEntity.setFutures(new ArrayList<>());

        mlnDownloadEntity.getFutures().add(future);

        System.out.println("DOWNLOAD: " + partFileName + " - " + "Range bytes=" + start + "-" + end);

    }

    /**
     * Stop and delete actual download
     * 
     * @param fileName name for file to stop
     */
    public void deleteDownload(UUID id) {

        Optional<MlnDownloaderDownloadFile> mOptional = mlnDownloadList.stream()
                .filter(m -> m.getId().equals(id))
                .findFirst();

        if (mOptional.isEmpty())
            throw new FileNotFoundException("El archivo que intenta eliminar no está en la lista de descargas.");

        MlnDownloaderDownloadFile mlnDownloaderEntity = mOptional.get();

        mlnDownloaderEntity.setDownloading(false);

        System.out.println("######################## Delete files ########################");

        mlnDownloaderEntity.getParts().forEach(p -> {

            File partToDelete = new File(p.getPath());

            if (partToDelete.exists()) {
                partToDelete.delete();
                System.out.println("DELETE PART: " + partToDelete);
            }
        });

        File file = new File(mlnDownloaderEntity.getFilePath());

        if (file.exists()) {
            System.out.println("DELETE FILE: " + file);
            file.delete();
        }

        System.out.println("##############################################################");

        System.out.println(mlnDownloadList);
        this.mlnDownloadList.remove(mlnDownloaderEntity);
        System.out.println(mlnDownloadList);

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
    public void pauseOrResumeDownload(UUID id) {

        Optional<MlnDownloaderDownloadFile> mOptional = mlnDownloadList.stream()
                .filter(down -> down.getId().equals(id))
                .findFirst();

        if (mOptional.isEmpty())
            throw new FileNotFoundException("El archivo que quieres pausar no se encuntra en la lista");

        MlnDownloaderDownloadFile mlnDownloaderEntity = mOptional.get();

        if (Files.exists(Path.of(mlnDownloaderEntity.getFilePath())))
            throw new FileAlreadyDownloadederException("El archivo que quieres resumir ya está descargado");

        if (mlnDownloaderEntity.isDownloaded())
            throw new FileAlreadyMerginException("No puedes cancelar ahora el fichero ya se ha descargado");

        if (mlnDownloaderEntity.isDownloading()) {

            System.out.println("################################# Pause download #################################");

            for (CompletableFuture<HttpResponse<Path>> future : mlnDownloaderEntity.getFutures()) {
                try {
                    future.cancel(true);
                } catch (Exception e) {
                    System.out.println("PETOOOOOOOOO");
                }
            }

            System.out.println("PAUSE DOWNLOAD: " + mlnDownloaderEntity.getFilePath());

            System.out.println("##################################################################################");

            mlnDownloaderEntity.setDownloading(false);

            mlnDownloaderEntity.getFutures().clear();

            saveDownloadList();

            return;
        }

        System.out.println("################################# Resume download #################################");

        for (MlnDownloaderPartFile p : mlnDownloaderEntity.getParts()) {

            File fileSizeDownloaded = new File(p.getPath());

            Long start = p.getStart();
            Long finalStart = start + fileSizeDownloaded.length();

            this.downPartFile(mlnDownloaderEntity, finalStart, p.getEnd(), p.getPath());

            System.out.println("RESUME DOWNLOAD -> " + p.getPath());

        }

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

        Optional<MlnDownloaderDownloadFile> mOptional = mlnDownloadList.stream()
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
     * Update size from download file
     * 
     * @param mlnDownloadEntity
     */
    public void controlDownloaderSize(MlnDownloaderDownloadFile mlnDownloadEntity) {

        new Thread(() -> {

            while (mlnDownloadEntity.isDownloading()) {

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                AtomicLong downloaderFilesSize = new AtomicLong(0);

                mlnDownloadEntity.getParts().forEach(p -> {

                    File file = new File(p.getPath());

                    if (file.exists()) {
                        downloaderFilesSize.addAndGet(file.length());
                        p.setActualSize(file.length());
                    }
                });

                if (downloaderFilesSize.get() != 0L) {
                    mlnDownloadEntity.setDownloadedBytes(downloaderFilesSize.get());
                }

                this.saveDownloadList();
            }

            System.out.println("STOP CONTROL DOWNLOAD SIZE: " + mlnDownloadEntity.getFilePath());

        }).start();
        ;

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

    /**
     * Send all download activity
     * 
     * @return list from downloads information
     */
    public List<MlnDownloaderDownloadFile> getMlnDownloadList() {
        return mlnDownloadList;
    }

    public void setMlnDownloadList(List<MlnDownloaderDownloadFile> mlnDownloadList) {
        this.mlnDownloadList = mlnDownloadList;
    }

    public static Path getDOWNLOAD_FOLDER() {
        return DOWNLOAD_FOLDER;
    }

}
