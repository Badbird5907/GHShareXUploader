package dev.badbird.ghuploader.request;

import dev.badbird.ghuploader.GhUploader;
import dev.badbird.ghuploader.object.SignedUploadKey;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Data
@Slf4j
@AllArgsConstructor
public class S3UploadRequest {
    private SignedUploadKey signedUploadKey;
    private File file;

    public CompletableFuture<String> send(GhUploader uploader) {
        CompletableFuture<String> future = new CompletableFuture<>();
        uploader.getPool().execute(() -> {
            String url = signedUploadKey.getUploadUrl();
            String fileName = file.getName();
            Request request = new Request.Builder()
                    .url(url)
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                    .post(signedUploadKey.getForm().toRequestBody(RequestBody.create(
                            MediaType.parse("application/octet-stream"),
                            file
                    ), fileName)).build();
            uploader.getClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    future.completeExceptionally(e);
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    if (response.isSuccessful()) {
                        log.info("Uploaded file to {}", signedUploadKey.getAsset().getHref());
                        future.complete(signedUploadKey.getAsset().getHref());
                    } else {
                        log.error("Failed to upload file: {}", response.body().string());
                        future.completeExceptionally(new IOException("Failed to upload file"));
                    }
                }
            });
        });
        return future;
    }

}
