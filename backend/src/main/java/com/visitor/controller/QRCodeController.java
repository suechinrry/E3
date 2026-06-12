package com.visitor.controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.visitor.common.Result;
import com.visitor.entity.Appointment;
import com.visitor.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.*;

@Tag(name = "二维码")
@RestController
@RequestMapping
public class QRCodeController {

    private final AppointmentService appointmentService;

    public QRCodeController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @Operation(summary = "生成预约二维码（返回 base64）")
    @GetMapping("/appointment/{id}/qrcode")
    public Result<Map<String, Object>> generate(@PathVariable Integer id) throws Exception {
        Appointment a = appointmentService.getById(id);
        if (a == null) return Result.error("预约不存在");

        String content = "appointment_" + id;

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, 300, 300,
                Map.of(EncodeHintType.MARGIN, 1));

        int w = matrix.getWidth();
        int h = matrix.getHeight();
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                image.setRGB(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        baos.flush();
        String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
        baos.close();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("appointmentId", id);
        result.put("base64", "data:image/png;base64," + base64);
        result.put("content", content);
        return Result.success(result);
    }
}
