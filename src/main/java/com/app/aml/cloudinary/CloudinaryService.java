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
}