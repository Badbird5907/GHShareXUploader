package dev.badbird.ghuploader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.badbird.ghuploader.object.Configuration;
import dev.badbird.ghuploader.object.RepoInfo;
import dev.badbird.ghuploader.request.SignedUploadKeyRequest;
import io.javalin.Javalin;
import io.javalin.http.UploadedFile;
import io.javalin.json.JsonMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Type;
import java.util.concurrent.ForkJoinPool;

@Slf4j
@Getter
@RequiredArgsConstructor
public class GhUploader {
    @Getter
    static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");

    private final Configuration configuration;
    private RepoInfo repo;
    private OkHttpClient client = new OkHttpClient();
    private ForkJoinPool pool = new ForkJoinPool();

    public void start() {
        long start = System.currentTimeMillis();
        log.info("Starting GhUploader on port {}", configuration.getPort());
        JsonMapper gsonMapper = new JsonMapper() {
            @Override
            public String toJsonString(@NotNull Object obj, @NotNull Type type) {
                return gson.toJson(obj, type);
            }

            @Override
            public <T> T fromJsonString(@NotNull String json, @NotNull Type targetType) {
                return gson.fromJson(json, targetType);
            }
        };
        Javalin.create(cfg -> cfg.jsonMapper(gsonMapper))
                .before(ctx -> {
                    if (repo == null) {
                        ctx.status(500).result("Repo info not loaded yet");
                    }
                })
                .get("/", ctx -> ctx.result("Hello World"))
                .get("/csrf", (ctx) -> {
                    // get .js-data-upload-policy-url-csrf
                    String csrf = Jsoup.connect("https://github.com/" + configuration.getRepo() + "/issues/new").header("Cookie", configuration.getCookies()).get().select("input.js-data-upload-policy-url-csrf").get(0).attr("value");
                    ctx.result(csrf);
                })
                .get("/uploadkey", (ctx) -> {
                    new SignedUploadKeyRequest("test.gif", 485848, "image/gif", null, repo.getId()).send(this).thenAccept(ctx::json).join();
                })
                .put("/upload", (ctx) -> {
                    UploadedFile file = ctx.uploadedFile("file");
                    String name = file.filename();
                    log.info("Received file {} with content type {}", name, file.contentType());
                    // save to temp
                    File tempFile = new File("temp/" + name);
                    if (!tempFile.exists()) {
                        tempFile.getParentFile().mkdirs();
                        tempFile.createNewFile();
                    }
                    tempFile.deleteOnExit();
                    file.content().transferTo(new FileOutputStream(tempFile));

                    new SignedUploadKeyRequest(name, tempFile.length(), file.contentType(), null, repo.getId()).send(this)
                            .thenCompose(key -> key.upload(tempFile, this)).thenAccept(ctx::result)
                            /*.thenRun(tempFile::delete)*/.join();
                })
                .start(configuration.getPort());
        log.info("GhUploader started on port {}", configuration.getPort());
        log.info("Grabbing repo info...");
        RepoInfo.create(configuration, this).thenAccept(info -> repo = info).join();
        long end = System.currentTimeMillis();
        log.info("Startup completed in {}ms", end - start);
    }
}
