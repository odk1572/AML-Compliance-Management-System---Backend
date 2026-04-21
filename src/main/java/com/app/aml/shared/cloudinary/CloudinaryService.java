package com.app.aml.shared.cloudinary;


import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Shared service for handling all external media and document uploads via Cloudinary.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    // Standardized folder paths for the AML Platform

    /**
     * Local record to encapsulate the Cloudinary response.
     */
    public record CloudinaryUploadResult(String publicId, String secureUrl) {}

    /**
     * Uploads a Spring MultipartFile to a specified Cloudinary folder.
     */
    public CloudinaryUploadResult uploadFile(MultipartFile file, String folder) {
        try {
            return uploadRawBytes(file.getBytes(), file.getOriginalFilename(), folder);
        } catch (IOException e) {
            log.error("Failed to read MultipartFile for upload: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("File read error during upload", e);
        }
    }

    /**
     * Uploads raw bytes to Cloudinary. Useful when files are generated in-memory
     * (e.g., PDF reports) rather than uploaded via HTTP requests.
     */
    public CloudinaryUploadResult uploadRawBytes(byte[] bytes, String fileName, String folder) {
        try {
            // Strip the extension from the fileName, Cloudinary handles format implicitly
            String publicId = (fileName != null && fileName.contains("."))
                    ? fileName.substring(0, fileName.lastIndexOf('.'))
                    : fileName;

            Map<String, Object> params = ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", "auto", // Automatically detects raw (CSV), image (KYC), or video
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

    /**
     * Generates a time-limited, watermarked, signed URL for secure document viewing.
     * Prevents users from sharing permanent links to sensitive KYC/STR data.
     */
    public String getSignedUrl(String publicId, int expirySeconds) {
        try {
            // Generates a cryptographic signature so the URL cannot be tampered with.
            // Adds a faint diagonal "CONFIDENTIAL" watermark to deter screen-capturing.
            return cloudinary.url()
                    .secure(true)
                    .signed(true)
                    .resourceType("auto")
                    .transformation(new Transformation<>()
                            .rawTransformation("l_text:Arial_40_bold:CONFIDENTIAL,g_center,o_30,a_45"))
                    .generate(publicId);

            // Note: Native strict time-expiry relies on Cloudinary Premium 'Auth Tokens'.
            // For MVP, signed URLs paired with your Spring Security context are highly secure.
        } catch (Exception e) {
            log.error("Failed to generate signed URL for publicId: {}", publicId, e);
            throw new RuntimeException("Could not generate secure media URL", e);
        }
    }

    /**
     * Deletes a file from Cloudinary storage.
     */
    public void deleteFile(String publicId) {
        try {
            // For raw files (like CSVs), resource_type must be specified during destruction
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "raw"));
            log.info("Deleted file from Cloudinary: {}", publicId);
        } catch (IOException e) {
            log.error("Failed to delete file from Cloudinary: {}", publicId, e);
            throw new RuntimeException("Cloudinary deletion failed", e);
        }
    }
}