package dev.badbird.ghuploader.object;

import dev.badbird.ghuploader.GhUploader;
import dev.badbird.ghuploader.Main;
import lombok.Data;

import java.io.File;
import java.nio.file.Files;

@Data
public class Configuration {
    private String cookies = "yourghcookies"; // i'm lazy so we're just going to send all the cookies
    private String repo = "NationalSecurityAgency/Ghidra";
    private int port = 13377;
    private String key = "key";

    public void save() {
        File file = new File("config.json");
        String json = GhUploader.getGson().toJson(this);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            Files.write(file.toPath(), json.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
