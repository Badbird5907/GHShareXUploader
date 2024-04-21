package dev.badbird.ghuploader.request;

import dev.badbird.ghuploader.GhUploader;
import dev.badbird.ghuploader.object.SignedUploadKey;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Data
@Slf4j
@AllArgsConstructor
public class GHFinishAssetUploadRequest {
    private SignedUploadKey signedUploadKey;

    public CompletableFuture<String> send(GhUploader uploader) {
        CompletableFuture<String> future = new CompletableFuture<>();
        uploader.getPool().execute(() -> {
            String url = "https://github.com" + signedUploadKey.getAssetUploadUrl();
            String authenticityToken = signedUploadKey.getAssetUploadAuthenticityToken();

            Request request = new Request.Builder()
                    .url(url)
                    .header("Cookie", uploader.getConfiguration().getCookies())
                    .header("Cookie", uploader.getConfiguration().getCookies())
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                    .header("accept", "application/json")
                    .header("content-type", "application/json")
                    .header("origin", "https://github.com")
                    .header("x-requested-with", "XMLHttpRequest")
                    .put(
                            new MultipartBody.Builder().setType(MultipartBody.FORM)
                                    .addFormDataPart("authenticity_token", authenticityToken)
                                    .build()
                    ).build();
            uploader.getClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    future.completeExceptionally(e);
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    if (response.isSuccessful()) {
                        log.info("Finished asset upload");
                        future.complete(signedUploadKey.getAsset().getHref());
                    } else {
                        log.error("Failed to finish asset upload: {}", response.body().string());
                        future.completeExceptionally(new IOException("Failed to finish asset upload"));
                    }
                }
            });
        });
        return future;
    }
}
