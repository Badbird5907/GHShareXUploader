package dev.badbird.ghuploader.object;

import com.google.gson.annotations.SerializedName;
import dev.badbird.ghuploader.GhUploader;
import dev.badbird.ghuploader.request.GHFinishAssetUploadRequest;
import dev.badbird.ghuploader.request.S3UploadRequest;
import lombok.Data;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Data
public class SignedUploadKey {
    @SerializedName("upload_url")
    private String uploadUrl;
    private Header header;
    private Asset asset;
    private Form form;
    @SerializedName("same_origin")
    private boolean sameOrigin;
    @SerializedName("asset_upload_url")
    private String assetUploadUrl;
    @SerializedName("upload_authenticity_token")
    private String uploadAuthenticityToken;
    @SerializedName("asset_upload_authenticity_token")
    private String assetUploadAuthenticityToken;

    public CompletableFuture<String> upload(File file, GhUploader ghUploader) {
        return new S3UploadRequest(this, file).send(ghUploader)
                .thenCompose(s -> new GHFinishAssetUploadRequest(this).send(ghUploader));
    }

    @Data
    public static class Form {
        private String key;
        private String acl;
        private String policy;
        @SerializedName("X-Amz-Algorithm")
        private String xAmzAlgorithm;
        @SerializedName("X-Amz-Credential")
        private String xAmzCredential;
        @SerializedName("X-Amz-Date")
        private String xAmzDate;
        @SerializedName("X-Amz-Signature")
        private String xAmzSignature;
        @SerializedName("Content-Type")
        private String contentType;
        @SerializedName("Cache-Control")
        private String cacheControl;
        @SerializedName("x-amz-meta-Surrogate-Control")
        private String xAmzMetaSurrogateControl;

        public RequestBody toRequestBody(RequestBody file, String fileName) {
            MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("key", key)
                    .addFormDataPart("acl", acl)
                    .addFormDataPart("policy", policy)
                    .addFormDataPart("X-Amz-Algorithm", xAmzAlgorithm)
                    .addFormDataPart("X-Amz-Credential", xAmzCredential)
                    .addFormDataPart("X-Amz-Date", xAmzDate)
                    .addFormDataPart("X-Amz-Signature", xAmzSignature)
                    .addFormDataPart("Content-Type", contentType)
                    .addFormDataPart("Cache-Control", cacheControl)
                    .addFormDataPart("x-amz-meta-Surrogate-Control", xAmzMetaSurrogateControl)
                    .addFormDataPart("file", fileName, file);
            return builder.build();
        }
    }
    public static class Header {}
    @Data
    public static class Asset {
        private int id;
        private String name;
        private int size;
        @SerializedName("content_type")
        private String contentType;
        private String href;
        @SerializedName("original_name")
        private String originalName;
    }
}
