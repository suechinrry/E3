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
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Tag(name = "二维码")
@RestController
@RequestMapping
public class QRCodeController {

    private final AppointmentService appointmentService;
    private final String qrSecret;

    public QRCodeController(AppointmentService appointmentService,
                            @Value("${jwt.secret}") String qrSecret) {
        this.appointmentService = appointmentService;
        this.qrSecret = qrSecret;
    }

    /**
     * QR 有效期（秒），默认 2 小时
     */
    private static final long QR_EXPIRE_SECONDS = 2 * 3600;

    @Operation(summary = "生成预约二维码（返回 base64，含 HMAC 签名防伪）")
    @GetMapping("/appointment/{id}/qrcode")
    public Result<Map<String, Object>> generate(@PathVariable Integer id, HttpServletRequest req) throws Exception {
        Appointment a = appointmentService.getById(id);
        if (a == null) return Result.error("预约不存在");

        // 数据级权限校验：访客只能查看自己的预约二维码（游客预约 visitorId 为 null，拒绝注册访客访问）
        String role = (String) req.getAttribute("role");
        Integer userId = (Integer) req.getAttribute("userId");
        if ("visitor".equals(role) && (a.getVisitorId() == null || !userId.equals(a.getVisitorId()))) {
            return Result.error(403, "无权查看他人预约二维码");
        }

        // 构建带签名的二维码内容：APPT:{id}:{exp}:{sign}
        long exp = System.currentTimeMillis() / 1000 + QR_EXPIRE_SECONDS;
        String payload = "APPT:" + id + ":" + exp;
        String sign = hmacSha256(payload, qrSecret);
        String content = payload + ":" + sign;

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

    /**
     * 验证二维码签名（供 GuardController 调用）
     * @return 预约 ID，验证失败返回 -1
     */
    static int verifyQrCode(String qrCode, String secret) {
        if (qrCode == null || qrCode.isBlank()) return -1;

        // 兼容旧格式：appointment_{id}
        if (qrCode.startsWith("appointment_")) {
            try {
                return Integer.parseInt(qrCode.replace("appointment_", ""));
            } catch (Exception e) {
                return -1;
            }
        }

        // 新格式：APPT:{id}:{exp}:{sign}
        if (!qrCode.startsWith("APPT:")) return -1;
        String[] parts = qrCode.split(":");
        if (parts.length != 4) return -1;

        try {
            int id = Integer.parseInt(parts[1]);
            long exp = Long.parseLong(parts[2]);
            String sign = parts[3];

            // 校验有效期
            if (System.currentTimeMillis() / 1000 > exp) return -1;

            // 校验签名
            String payload = "APPT:" + id + ":" + exp;
            String expectedSign = hmacSha256(payload, secret);
            if (!expectedSign.equals(sign)) return -1;

            return id;
        } catch (Exception e) {
            return -1;
        }
    }

    static String hmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec spec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(spec);
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (Exception e) {
            throw new RuntimeException("HMAC 签名失败", e);
        }
    }
}
