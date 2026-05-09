package com.example.taxassistant.files;

import com.example.taxassistant.files.dto.FileUploadResponse;
import com.example.taxassistant.security.UserPrincipal;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/businesses/{businessId}/files")
public class FileUploadController {

    private final FileUploadService fileUploadService;

    public FileUploadController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FileUploadResponse upload(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID businessId,
            @RequestPart("file") MultipartFile file
    ) {
        return fileUploadService.upload(principal.getId(), businessId, file);
    }
}

