package dev.badbird.ghuploader.request;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import dev.badbird.ghuploader.GhUploader;
import dev.badbird.ghuploader.object.Configuration;
import dev.badbird.ghuploader.object.SignedUploadKey;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Data
@Slf4j
@AllArgsConstructor
public class SignedUploadKeyRequest {
    private String name;
    private long size;
    @SerializedName("content_type")
    private String contentType;
    @SerializedName("authenticity_token")
    private String authenticityToken;
    @SerializedName("repository_id")
    private long repositoryId;

    public RequestBody toRequestBody() {
        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("name", name)
                .addFormDataPart("size", String.valueOf(size))
                .addFormDataPart("content_type", contentType)
                .addFormDataPart("authenticity_token", authenticityToken)
                .addFormDataPart("repository_id", String.valueOf(repositoryId));
        return builder.build();
    }

    public CompletableFuture<SignedUploadKey> send(GhUploader uploader) {
        CompletableFuture<SignedUploadKey> future = new CompletableFuture<>();
        uploader.getPool().execute(() -> {
            // getting authenticity token
            try {
                Configuration configuration = uploader.getConfiguration();
                if (authenticityToken == null) {
                    this.authenticityToken = Jsoup.connect("https://github.com/" + configuration.getRepo() + "/issues/new").header("Cookie", configuration.getCookies()).get()
                            .select("input.js-data-upload-policy-url-csrf").get(0).attr("value");
                }
                String assetsUrl = "https://github.com/upload/policies/assets";

                Request request = new Request.Builder()
                        .url(assetsUrl)
                        .header("Cookie", uploader.getConfiguration().getCookies())
                        .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                        .header("accept", "application/json")
                        .header("content-type", "application/json")
                        .header("origin", "https://github.com")
                        .header("x-requested-with", "XMLHttpRequest")
                        .post(toRequestBody()).build();
                uploader.getClient().newCall(request).enqueue(new Callback() {
                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                        String json = response.body().string();
                        log.info("Got response code: {}", response.code());
                        if (response.isSuccessful()) {
                            SignedUploadKey key = GhUploader.getGson().fromJson(json, SignedUploadKey.class);
                            future.complete(key);
                            log.info("Got signed upload key!");
                        } else {
                            future.completeExceptionally(new RuntimeException("Failed to get signed upload key"));
                        }
                    }

                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        future.completeExceptionally(e);
                    }
                });

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        return future;
    }
}
