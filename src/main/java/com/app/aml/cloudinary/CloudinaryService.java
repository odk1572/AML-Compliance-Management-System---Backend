package com.app.aml.cloudinary;


import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public record CloudinaryUploadResult(String publicId, String secureUrl) {}

    public CloudinaryUploadResult uploadFile(MultipartFile file, String folder) {
        try {
            return uploadRawBytes(file.getBytes(), file.getOriginalFilename(), folder);
        } catch (IOException e) {
            log.error("Failed to read MultipartFile for upload: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("File read error during upload", e);
        }
    }

    public CloudinaryUploadResult uploadRawBytes(byte[] bytes, String fileName, String folder) {
        try {
            String publicId = (fileName != null && fileName.contains("."))
                    ? fileName.substring(0, fileName.lastIndexOf('.'))
                    : fileName;

            Map<String, Object> params = ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", "auto",
                    "public_id", publicId
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(bytes, params);

            return new CloudinaryUploadResult(
                    uploadResult.get("public_id").toString(),
                    uploadResult.get("secure_url").toString()
            );

        } catch (IOException e) {
            log.error("Cloudinary upload failed for file: {}", fileName, e);
            throw new RuntimeException("Cloudinary upload failed", e);
        }
    }

    public String getSignedUrl(String publicId, int expirySeconds) {
        try {
            return cloudinary.url()
                    .secure(true)
                    .signed(true)
                    .resourceType("auto")
                    .transformation(new Transformation<>()
                            .rawTransformation("l_text:Arial_40_bold:CONFIDENTIAL,g_center,o_30,a_45"))
                    .generate(publicId);

        } catch (Exception e) {
            log.error("Failed to generate signed URL for publicId: {}", publicId, e);
            throw new RuntimeException("Could not generate secure media URL", e);
        }
    }

    public void deleteFile(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "raw"));
            log.info("Deleted file from Cloudinary: {}", publicId);
        } catch (IOException e) {
            log.error("Failed to delete file from Cloudinary: {}", publicId, e);
            throw new RuntimeException("Cloudinary deletion failed", e);
        }
    }
}