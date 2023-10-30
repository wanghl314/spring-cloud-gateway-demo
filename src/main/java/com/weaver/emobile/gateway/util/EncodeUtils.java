package com.weaver.emobile.gateway.util;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

public class EncodeUtils {

    /**
     * RSA公钥加密
     * @param plainData
     * @param publicKey
     * @return
     * @throws Exception
     */
    public static String rsaEncrypt(String plainData, String publicKey) throws Exception {
        byte[] keyBytes = Base64.decodeBase64(publicKey);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        RSAPublicKey key = (RSAPublicKey) keyFactory.generatePublic(spec);
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return Base64.encodeBase64String(cipher.doFinal(plainData.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * RSA私钥解密
     * @param encryptedData
     * @param privateKey
     * @return
     * @throws Exception
     */
    public static String rsaDecrypt(String encryptedData, String privateKey) throws Exception {
        byte[] keyBytes = Base64.decodeBase64(privateKey);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        RSAPrivateKey key = (RSAPrivateKey) keyFactory.generatePrivate(spec);
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return new String(cipher.doFinal(Base64.decodeBase64(encryptedData)), StandardCharsets.UTF_8);
    }

    public static String aesEncrypt(String plainData, String key) throws Exception {
        return Base64.encodeBase64String(aesEncrypt(plainData.getBytes(StandardCharsets.UTF_8), key));
    }

    public static byte[] aesEncrypt(byte[] plainData, String key) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return cipher.doFinal(plainData);
    }

    public static String aesDecrypt(String encryptedData, String key) throws Exception {
        return new String(aesDecrypt(Base64.decodeBase64(encryptedData), key), StandardCharsets.UTF_8);
    }

    public static byte[] aesDecrypt(byte[] encryptedData, String key) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return cipher.doFinal(encryptedData);
    }

}
