package com.msb.supsale.controller;

import com.msb.supsale.dto.CccdDto;
import com.msb.supsale.service.OcrService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;

@RestController
@RequestMapping("/api/v1/ocr")
public class OcrController {
    private final OcrService ocrService;

    public OcrController(OcrService ocrService) { this.ocrService = ocrService; }

    @PostMapping("/cccd")
    public CccdDto extractCccd(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "sessionId", defaultValue = "") String sessionId,
            @RequestParam(value = "phone", defaultValue = "") String phone) {
        try {
            byte[] bytes = file.getBytes();
            String base64 = Base64.getEncoder().encodeToString(bytes);
            String contentType = file.getContentType() != null ? file.getContentType() : "image/jpeg";
            String dataUrl = "data:" + contentType + ";base64," + base64;
            return ocrService.extractCccd(dataUrl, sessionId, phone);
        } catch (Exception e) {
            CccdDto error = new CccdDto();
            error.setSaved(false);
            error.setMessage("Không thể đọc file: " + e.getMessage());
            return error;
        }
    }
}
