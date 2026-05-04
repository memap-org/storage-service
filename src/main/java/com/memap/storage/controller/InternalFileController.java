package com.memap.storage.controller;

import com.memap.storage.dto.response.ApiResponse;
import com.memap.storage.service.IFileService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/file/internal")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InternalFileController {

    IFileService fileService;

    @DeleteMapping("/{fileId}")
    public ApiResponse<String> deleteInternal(@PathVariable String fileId) {
        fileService.deleteInternal(fileId);
        return ApiResponse.<String>builder()
                .result("File deleted successfully")
                .build();
    }
}
