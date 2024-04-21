package dev.badbird.ghuploader.object;

import com.google.gson.JsonObject;
import dev.badbird.ghuploader.GhUploader;
import lombok.Data;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Data
public class RepoInfo {
    private String owner;
    private String repo;
    private long id;

    public static CompletableFuture<RepoInfo> create(Configuration configuration, GhUploader uploader) {
        CompletableFuture<RepoInfo> future = new CompletableFuture<>();
        Request request = new Request.Builder()
                .url("https://api.github.com/repos/" + configuration.getRepo())
                .build();
        uploader.getClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                JsonObject object = GhUploader.getGson().fromJson(response.body().string(), JsonObject.class);
                RepoInfo info = new RepoInfo();
                info.setOwner(object.get("owner").getAsJsonObject().get("login").getAsString());
                info.setRepo(object.get("name").getAsString());
                info.setId(object.get("id").getAsLong());
                future.complete(info);
            }
        });
        return future;
    }
}
