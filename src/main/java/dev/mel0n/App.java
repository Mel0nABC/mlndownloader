package dev.mel0n;

import java.io.File;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class App {

    private static final String DATA_FOLDER = "data";

    public static void main(String[] args) {

        // Check and create data folter
        File file = new File(DATA_FOLDER);

        if (!file.exists())
            file.mkdir();

        SpringApplication.run(App.class, args);

    }
}
