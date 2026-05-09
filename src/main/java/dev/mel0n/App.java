package dev.mel0n;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * Multi thread download, only to testing and learn.
 *
 */
public class App {

    private Scanner scan = new Scanner(System.in);
    private List<Path> parts = new ArrayList<>();
    private List<Thread> threads = new ArrayList<>();
    private final String SUFIX = "_PART_";
    private App app;
    private boolean finalDownload = false;
    private Long downloaded = 0L;
    private Long length = 0L;

    public static void main(String[] args) {
        Long startAppTime = System.currentTimeMillis();

        App startApp = new App();
        startApp.setApp(startApp);
        startApp.start();

        Long endAppTime = System.currentTimeMillis();
        System.out.println("TIEMPO TRASCURRIDO EN LA DESCARGA: " + (endAppTime - startAppTime) / 1000);
    }

    public void start() {
        try {

            URI uri = app.getDownloadLink();

            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());

            String path = uri.getPath();
            String fileName = path.substring(path.lastIndexOf("/") + 1);

            this.length = Long.parseLong(response.headers().map().get("content-length").getFirst());
            Long chunkSize = length / getDownloadThreadSize();

            System.out.println("############################## INFORMACIÓN DEL ARCHIVO ##############################");
            System.out.println();
            System.out.println("FILE SIZE: " + this.length);
            System.out.println("MULTI PART SIZE: " + chunkSize);
            System.out.println("TOTAL PARTS: " + this.length / chunkSize);
            System.out.println();
            System.out.println("Status code: " + response.statusCode());
            System.out.println("Accept Ranges: " + response.headers().map().get("accept-ranges").getFirst());
            System.out.println("Tamaño en bytes: " + this.length);
            System.out.println();
            System.out.println("#####################################################################################");

            Long start = 0L;
            int count = 0;

            while (start < this.length) {

                long finalStart = start;

                Long finalEnd = (start + chunkSize) > this.length ? this.length : start + chunkSize - 1;

                int finalCount = count;

                Thread thread = new Thread(() -> {
                    app.downPartFile(client, fileName, finalStart, finalEnd, uri, finalCount,
                            this.app);
                });

                thread.start();
                threads.add(thread);

                start = finalEnd + 1;
                count++;

            }

            System.out.println("Descargando..");

            new Thread(() -> {
                while (!app.finalDownload) {
                    app.downloaded = 1L;
                    parts.forEach(p -> {
                        File file = new File(p.toUri());
                        app.downloaded += file.length() / 1000000L;
                    });
                    Long totalMbytes = app.length / 1000000L;
                    float percent = ((float) app.downloaded / (float) totalMbytes) * 100;
                    int percentInt = (int) percent;

                    System.out.print(
                            "DOWNLOADED : " + percentInt + "%, " + app.downloaded + "/" + totalMbytes + " MByte\r");
                }
                System.out.println();

            }).start();

            for (Thread t : threads) {
                t.join();
            }

            this.finalDownload = true;

            this.parts = this.parts.stream()
                    .sorted((p1, p2) -> Integer.compare(Integer.parseInt(p1.getFileName().toString().split(SUFIX)[1]),
                            Integer.parseInt(p2.getFileName().toString().split(SUFIX)[1])))
                    .toList();

            try (OutputStream out = Files.newOutputStream(Path.of(fileName))) {

                String point = "....";
                for (Path part : this.parts) {
                    System.out.print("Uniendo partes ..." + point + "\r");
                    Files.copy(part, out);
                }
                System.out.println();
            }

            System.out.println("Todas las partes unidas");

            for (Path part : this.parts) {
                Files.delete(part);
            }

            Path file = Path.of(fileName);

            if (Files.exists(file)) {
                System.out.println("############################## DESCARGADO ##############################");
                System.out.println("TAMAÑO EN WEB: " + length + " bytes");
                System.out.println("TAMAÑO EN DISCO: " + Files.size(file) + " bytes");
                System.out.println("DESCARGADO: " + (length == Files.size(file) ? "CORRECTAMENTE" : "POSIBLES FALLOS"));
                System.out.println("########################################################################");
            }

        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public URI getDownloadLink() throws URISyntaxException {

        URI uri = null;
        String link = "";
        while (true) {

            System.out.println("Por favor, indica el link para descargar:");

            link = scan.nextLine();

            System.out.println();

            uri = new URI(link);

            if (uri.getScheme() == null) {
                System.out.println("NO ES URL VÁLIDA, DEBES INDICAR HTTP/HTTPS\n");
            } else {
                break;
            }

        }
        return uri;
    }

    public int getDownloadThreadSize() throws URISyntaxException {

        int size = 0;

        while (true) {

            System.out.println("Por favor, indica la cantidad de hilos para la descarga simultánea:");

            if (scan.hasNextInt()) {
                size = scan.nextInt();
                System.out.println();
                break;
            } else {
                System.out.println("Debes indicar un número entero");
                scan.next();
            }
        }

        scan.close();

        return size;
    }

    public void downPartFile(HttpClient client, String fileName, Long start, Long end, URI uri, int count, App app) {

        File partFile = new File(fileName + SUFIX + count);

        // System.out.println("count: " + partFile.exists());

        app.getParts().add(partFile.toPath());

        if (partFile.exists()) {
            start = start + partFile.length();
        }

        Long startMbytes = start / 1000 / 1000;
        Long endMbytes = end / 1000 / 1000;

        System.out.println(partFile.getName() + " - Range Mbytes: " + startMbytes + "/" + endMbytes);

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

        } catch (

        IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
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

    public List<Path> getParts() {
        return parts;
    }

    public void setParts(List<Path> parts) {
        this.parts = parts;
    }

    public App getApp() {
        return app;
    }

    public void setApp(App app) {
        this.app = app;
    }

}
