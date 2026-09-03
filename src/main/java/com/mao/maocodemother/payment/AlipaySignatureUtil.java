package com.mao.maocodemother.payment;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

/**
 * 支付宝 RSA2（SHA256withRSA）签名 / 验签工具。
 * <p>
 * 手写实现，不依赖支付宝 SDK，便于离线编译。私钥为 PKCS8（-----BEGIN PRIVATE KEY-----），
 * 公钥为 X.509（-----BEGIN PUBLIC KEY-----）。拼接规则与支付宝开放平台一致：
 * 取除 sign / sign_type 外的全部参数，按 key 升序拼接为 {@code k=v&k=v}（原始值，不编码），再做签名。
 */
public final class AlipaySignatureUtil {

    private static final String CHARSET = "UTF-8";
    private static final String SIGN_ALGORITHM = "SHA256withRSA";

    private AlipaySignatureUtil() {
    }

    /**
     * 对参数集合签名，返回 Base64 编码的签名串。
     */
    public static String sign(Map<String, String> params, String privateKey) throws Exception {
        String content = buildSignContent(params);
        PrivateKey key = loadPrivateKey(privateKey);
        Signature signature = Signature.getInstance(SIGN_ALGORITHM);
        signature.initSign(key);
        signature.update(content.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    /**
     * 校验回调签名。
     */
    public static boolean verify(Map<String, String> params, String publicKey, String sign) throws Exception {
        if (sign == null || sign.isEmpty()) {
            return false;
        }
        String content = buildSignContent(params);
        PublicKey key = loadPublicKey(publicKey);
        Signature signature = Signature.getInstance(SIGN_ALGORITHM);
        signature.initVerify(key);
        signature.update(content.getBytes(StandardCharsets.UTF_8));
        return signature.verify(Base64.getDecoder().decode(sign));
    }

    /**
     * 取除 sign / sign_type 外、按 key 升序拼接的待签名内容。
     */
    public static String buildSignContent(Map<String, String> params) {
        Map<String, String> sorted = new TreeMap<>();
        params.forEach((k, v) -> {
            if (v == null || v.isEmpty()) {
                return;
            }
            if ("sign".equals(k) || "sign_type".equals(k)) {
                return;
            }
            sorted.put(k, v);
        });
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            if (!first) {
                sb.append('&');
            }
            sb.append(entry.getKey()).append('=').append(entry.getValue());
            first = false;
        }
        return sb.toString();
    }

    private static PrivateKey loadPrivateKey(String privateKey) throws Exception {
        byte[] keyBytes = decodePem(privateKey);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
    }

    private static PublicKey loadPublicKey(String publicKey) throws Exception {
        // 支持纯 PKCS8 公钥与 X.509 证书两种形态
        String trimmed = publicKey == null ? "" : publicKey;
        if (trimmed.contains("BEGIN CERTIFICATE")) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(
                    new ByteArrayInputStream(decodePem(publicKey)));
            return cert.getPublicKey();
        }
        byte[] keyBytes = decodePem(publicKey);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance("RSA").generatePublic(keySpec);
    }

    /**
     * 去除 PEM 头尾与换行，返回 Base64 解码后的原始字节。
     */
    private static byte[] decodePem(String pem) {
        if (pem == null) {
            return new byte[0];
        }
        String cleaned = pem.replaceAll("-----(BEGIN|END) [A-Z ]+-----", "")
                .replaceAll("\\s+", "");
        return Base64.getDecoder().decode(cleaned);
    }
}
