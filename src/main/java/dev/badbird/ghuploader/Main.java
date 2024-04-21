package dev.badbird.ghuploader;

import dev.badbird.ghuploader.object.Configuration;
import lombok.SneakyThrows;

import java.io.File;
import java.nio.file.Files;

public class Main {

    @SneakyThrows
    public static void main(String[] args) {
        File config = new File("config.json");
        if(!config.exists()){
            System.out.println("Config file not found, creating one...");
            Configuration configuration = new Configuration();
            configuration.save();
            System.out.println("Config file created, please fill it out and restart the program");
            System.exit(0);
            return;
        }

        String json = new String(Files.readAllBytes(config.toPath()));
        Configuration configuration = GhUploader.gson.fromJson(json, Configuration.class);
        new GhUploader(configuration).start();
    }
}