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

    private List<Path> parts = new ArrayList<>();
    private final String SUFIX = "_PART_";
    private App app;

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

            Long length = Long.parseLong(response.headers().map().get("content-length").getFirst());
            Long chunkSize = length / getDownloadThreadSize();

            System.out.println("############################## INFORMACIÓN DEL ARCHIVO ##############################");
            System.out.println();
            System.out.println("FILE SIZE: " + length);
            System.out.println("MULTI PART SIZE: " + chunkSize);
            System.out.println("TOTAL PARTS: " + length / chunkSize);
            System.out.println();
            System.out.println("Status code: " + response.statusCode());
            System.out.println("Accept Ranges: " + response.headers().map().get("accept-ranges").getFirst());
            System.out.println("Tamaño en bytes: " + length);
            System.out.println();
            System.out.println("#####################################################################################");

            Long start = 0L;
            int count = 0;

            List<Thread> threads = new ArrayList<>();

            while (start < length) {

                long finalStart = start;

                Long finalEnd = (start + chunkSize) > length ? length : start + chunkSize;

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

            System.out.print("Descargando..");
            for (Thread t : threads) {
                System.out.print("..");
                t.join();
            }
            System.out.println(" Descarga finalizada");

            this.parts = this.parts.stream()
                    .sorted((p1, p2) -> Integer.compare(Integer.parseInt(p1.getFileName().toString().split(SUFIX)[1]),
                            Integer.parseInt(p2.getFileName().toString().split(SUFIX)[1])))
                    .toList();

            try (OutputStream out = Files.newOutputStream(Path.of(fileName))) {

                for (Path part : this.parts) {
                    Files.copy(part, out);
                }
            }

            for (Path part : this.parts) {
                Files.delete(part);
            }

            Path file = Path.of(fileName);

            if (Files.exists(file)) {
                System.out.println("############################## DESCARGADO ##############################");
                System.out.println("TAMAÑO EN WEB: " + length);
                System.out.println("TAMAÑO EN DISCO: " + Files.size(file));
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

        Scanner scan = new Scanner(System.in);
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

        Scanner scan = new Scanner(System.in);
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
        return size;
    }

    public void downPartFile(HttpClient client, String fileName, Long start, Long end, URI uri, int count, App app) {

        File partFile = new File(fileName + SUFIX + count);

        if (partFile.exists()) {
            start = start + partFile.length();
        }
        System.out.println(partFile.getName() + " - Range bytes=" + start + "-" + end);
        HttpRequest requestFile = HttpRequest.newBuilder()
                .uri(uri)
                .header("Range", "bytes=" + start + "-" + end)
                .GET()
                .build();

        HttpResponse<InputStream> responseFile;
        try {
            responseFile = client.send(requestFile,
                    HttpResponse.BodyHandlers.ofInputStream());

            app.getParts().add(partFile.toPath());

            if (partFile.exists()) {
                Path tmpFile = Path.of(partFile.getName() + "_tmp");
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
