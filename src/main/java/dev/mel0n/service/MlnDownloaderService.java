package dev.mel0n.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartException;

import dev.mel0n.dto.MlnDownloadderEntityDTO;
import dev.mel0n.entity.MlnDownloaderEntity;
import dev.mel0n.exception.FileAlreadyDownloadederException;
import dev.mel0n.repository.MlnDownloaderReposiroty;

/**
 * Multi thread download, only to testing and learn.
 *
 */
@Service
public class MlnDownloaderService {

    private MlnDownloaderReposiroty mlnDownloaderReposiroty;

    private HttpClient client = HttpClient.newHttpClient();
    private final String SUFIX = "_PART_";
    private final Long BYTE_TO_MBYTE = 1000000L;
    private final int TO_PERCENT = 100;
    private final int SOME_BYTE = 1;

    public MlnDownloaderService(MlnDownloaderReposiroty mlnDownloaderReposiroty) {
        this.mlnDownloaderReposiroty = mlnDownloaderReposiroty;
    }

    public void newDownload(MlnDownloadderEntityDTO mlnDownloadderEntityDTO) {

        URI uri = mlnDownloadderEntityDTO.uri();
        int chunks = mlnDownloadderEntityDTO.chunks();
        String fileName = "";
        Long length = 0L;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());

            String path = uri.getPath();
            fileName = path.substring(path.lastIndexOf("/") + SOME_BYTE);
            length = Long.parseLong(response.headers().map().get("content-length").getFirst());

            checkNewDownloadExist(fileName, length);

            mlnDownloaderReposiroty.save(MlnDownloaderEntity.builder()
                    .uri(uri)
                    .fileName(fileName)
                    .length(length)
                    .chunks(chunks)
                    .build());

            Optional<MlnDownloaderEntity> mOptional = mlnDownloaderReposiroty.findByFileName(fileName);

            if (!mOptional.isEmpty())
                startDownload(mOptional.get());

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }

    public void startDownload(MlnDownloaderEntity mlnDownloadEntity) {
        try {

            Long length = mlnDownloadEntity.getLength();
            Long chunkSize = length / mlnDownloadEntity.getChunks();

            Long start = 0L;
            int count = 0;

            while (start < length) {

                long finalStart = start;

                Long finalEnd = (start + chunkSize) > length ? length : start + chunkSize - SOME_BYTE;

                int finalCount = count;

                String partFileName = mlnDownloadEntity.getFileName() + SUFIX + count;

                Thread thread = new Thread(() -> {
                    this.downPartFile(client, mlnDownloadEntity.getFileName(), finalStart, finalEnd,
                            mlnDownloadEntity.getUri(), finalCount, mlnDownloadEntity, partFileName);
                });

                thread.setName(partFileName);

                thread.start();

                mlnDownloadEntity.getThreads().add(thread);

                start = finalEnd + SOME_BYTE;
                count++;

            }

            System.out.println("################################# Descargando #################################");

            new Thread(() -> {
                while (!mlnDownloadEntity.isFinalDownload()) {

                    AtomicLong downloaderFilesSize = new AtomicLong(0);

                    mlnDownloadEntity.getParts().forEach(p -> {
                        try {

                            if (Files.exists(p))
                                downloaderFilesSize.addAndGet(Files.size(p));

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });

                    mlnDownloadEntity.setDownloadedBytes(downloaderFilesSize.get());
                }

            }).start();

            mlnDownloadEntity.getParts().forEach(p -> {

                Thread threadSpeed = new Thread(() -> {
                    this.getDownloadSpeed(p.toString(), mlnDownloadEntity);
                });
                threadSpeed.setName(p.toString());
                threadSpeed.start();
            });

            for (Thread t : mlnDownloadEntity.getThreads()) {
                t.join();
            }

            mlnDownloadEntity.setFinalDownload(true);

            mlnDownloadEntity.setParts(mlnDownloadEntity.getParts().stream()
                    .sorted((p1, p2) -> Integer.compare(Integer.parseInt(p1.getFileName().toString().split(SUFIX)[1]),
                            Integer.parseInt(p2.getFileName().toString().split(SUFIX)[1])))
                    .toList());

            new Thread(() -> {
                multipartMergeAndDelete(mlnDownloadEntity);
            }).start();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void multipartMergeAndDelete(MlnDownloaderEntity mlnDownloadEntity) {

        try {

            try (OutputStream out = Files.newOutputStream(Path.of(mlnDownloadEntity.getFileName()))) {

                for (Path part : mlnDownloadEntity.getParts()) {

                    Files.copy(part, out);

                }
            }

            if (!mlnDownloadEntity.getLength().equals(mlnDownloadEntity.getDownloadedBytes())) {

                throw new MultipartException("Some problem to merge all files");

            } else {

                for (Path part : mlnDownloadEntity.getParts()) {
                    Files.delete(part);
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void downPartFile(HttpClient client, String fileName, Long start, Long end, URI uri, int count,
            MlnDownloaderEntity mlnDownloadEntity, String partFileName) {

        File partFile = new File(partFileName);

        mlnDownloadEntity.getParts().add(partFile.toPath());

        if (partFile.exists()) {
            start = start + partFile.length();
        }

        HttpRequest requestFile = HttpRequest.newBuilder()
                .uri(uri)
                .header("Range", "bytes=" + start + "-" + end)
                .GET()
                .build();

        HttpResponse<InputStream> responseFile;
        try {
            responseFile = client.send(requestFile,
                    HttpResponse.BodyHandlers.ofInputStream());

            if (partFile.exists()) {

                if (start.equals(end)) {
                    return;
                }

                try (InputStream in = responseFile.body();
                        OutputStream out = Files.newOutputStream(partFile.toPath(),
                                StandardOpenOption.APPEND)) {

                    in.transferTo(out);
                }

            } else {

                try (InputStream in = responseFile.body();
                        OutputStream out = Files.newOutputStream(partFile.toPath())) {

                    in.transferTo(out);
                }
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }

    public void checkNewDownloadExist(String fileName, Long length) {

        File checkFile = new File(fileName);

        Optional<MlnDownloaderEntity> mOptional = mlnDownloaderReposiroty.findByFileName(fileName);

        if (checkFile.exists() & (checkFile.length() == length) | !mOptional.isEmpty()) {
            throw new FileAlreadyDownloadederException(checkFile.getAbsolutePath());
        }
    }

    public void checkSum(String fileName, String fileCheckSum)
            throws FileNotFoundException, IOException, InterruptedException {

        try (InputStream is = new FileInputStream(fileName)) {

            String sha256 = DigestUtils.sha256Hex(is);

            System.out.println(sha256);
        }
        ProcessBuilder pb = new ProcessBuilder("sha256sum", "-c", fileCheckSum);

        Process p = pb.start();

        // Leer stdout
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(p.getInputStream()));

        String linea;

        while ((linea = reader.readLine()) != null) {

            System.out.println(linea);
        }

        int exitCode = p.waitFor();

        System.out.println("Código de salida: " + exitCode);
    }

    public Long getByteToMbyte(Long bytes) {
        return bytes / BYTE_TO_MBYTE;
    }

    public void getDownloadSpeed(String partFilename, MlnDownloaderEntity mlnDownloadEntity) {

        while (!mlnDownloadEntity.isFinalDownload()) {
            File file = new File(partFilename);

            if (file.exists()) {

                if (partFilename.endsWith("_PART_1")) {

                    Long startSize = file.length();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Long endSize = file.length();
                }

            }
        }

    }

}
