package com.mao.maocodemother.utils;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Random;

/**
 * 服务端图形验证码工具（零依赖，使用 JDK 自带 java.awt / javax.imageio 生成 PNG）。
 * 用于公开「发送验证码」接口前的人机校验，挡住自动化轰炸脚本。
 */
public final class CaptchaUtil {

    /** 排除易混淆字符（0/O、1/I/l） */
    private static final String CHAR_POOL = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    private static final int CODE_LEN = 4;
    private static final int FONT_SIZE = 26;

    private CaptchaUtil() {
    }

    /**
     * 生成随机验证码文本（4 位）
     */
    public static String generateCode() {
        Random r = new Random();
        StringBuilder sb = new StringBuilder(CODE_LEN);
        for (int i = 0; i < CODE_LEN; i++) {
            sb.append(CHAR_POOL.charAt(r.nextInt(CHAR_POOL.length())));
        }
        return sb.toString();
    }

    /**
     * 根据验证码文本绘制干扰图片，返回 base64 的 PNG data URL（含 "data:image/png;base64," 前缀）。
     */
    public static String generateBase64Image(String code) throws Exception {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        // 背景
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        // 干扰线
        Random r = new Random();
        for (int i = 0; i < 6; i++) {
            g.setColor(randomColor(r));
            g.drawLine(r.nextInt(WIDTH), r.nextInt(HEIGHT), r.nextInt(WIDTH), r.nextInt(HEIGHT));
        }
        // 干扰点
        for (int i = 0; i < 30; i++) {
            g.setColor(randomColor(r));
            g.fillRect(r.nextInt(WIDTH), r.nextInt(HEIGHT), 1, 1);
        }
        // 字符（每个字符轻微旋转 + 随机颜色）
        g.setFont(new Font("Arial", Font.BOLD, FONT_SIZE));
        for (int i = 0; i < code.length(); i++) {
            g.setColor(randomColor(r));
            double theta = (r.nextDouble() - 0.5) * 0.5;
            g.rotate(theta, 18 + i * 24, 28);
            g.drawString(String.valueOf(code.charAt(i)), 16 + i * 24, 30);
            g.rotate(-theta, 16 + i * 24, 28);
        }
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    private static Color randomColor(Random r) {
        return new Color(r.nextInt(160), r.nextInt(160), r.nextInt(160));
    }
}
