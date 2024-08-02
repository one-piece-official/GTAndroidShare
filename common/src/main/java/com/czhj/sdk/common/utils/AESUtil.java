package com.czhj.sdk.common.utils;

import static android.util.Base64.NO_WRAP;

import android.text.TextUtils;
import android.util.Base64;

import com.czhj.sdk.common.Constants;

import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AESUtil {

    private static final int GCM_NONCE_LENGTH = 12; // in bytes
    private static final int GCM_TAG_LENGTH = 128; // in bytes
    private static volatile byte[] nonce = null;

    public static synchronized byte[] generateNonce() {
        if (nonce == null) {
            nonce = new byte[GCM_NONCE_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(nonce);
        }
        return nonce;
    }

    private static byte[] DeOREncrypt(byte[] data, String sKey, int model, String tag) throws Exception {
        if (data.length == 0 || TextUtils.isEmpty(sKey)) {
            return data;
        }

        byte[] iv = !TextUtils.isEmpty(tag) ? Base64.decode(tag, NO_WRAP) : generateNonce();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = null;
        byte[] raw = sKey.getBytes("utf-8");
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");


        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(model, skeySpec, spec);
        }

        return cipher.doFinal(data);
    }

    public static byte[] Encrypt(byte[] data, String sKey, String tag) throws Exception {
        if (data.length == 0 || TextUtils.isEmpty(sKey)) {
            return data;
        }
        try {
            return DeOREncrypt(data, sKey, Cipher.ENCRYPT_MODE, tag);

        } catch (Exception e) {

        }
        return null;
    }

    public static byte[] Encrypt(byte[] data, String sKey) throws Exception {
        return Encrypt(data, sKey, null);
    }

    public static String EncryptString(String sSrc, String sKey, String tag) {

        if (TextUtils.isEmpty(sSrc) || TextUtils.isEmpty(sKey)) {
            return sSrc;
        }
        try {
            byte[] encrypt = Encrypt(sSrc.getBytes("utf-8"), sKey, tag);
            return Base64.encodeToString(encrypt, NO_WRAP);

        } catch (Exception e) {

        }

        return null;
    }

    public static String EncryptString(String sSrc, String sKey) {
        return EncryptString(sSrc, sKey, Constants.GCMNONCE);
    }

    public static String EncryptStringServer(String sSrc, String sKey) {
        return EncryptString(sSrc, sKey, null);
    }

    public static String DecryptString(String sSrc, String sKey) {
        return DecryptString(sSrc, sKey, Constants.GCMNONCE);
    }

    public static String DecryptStringServer(String sSrc, String sKey) {
        return DecryptString(sSrc, sKey, null);
    }

    // 解密
    public static String DecryptString(String sSrc, String sKey, String tag) {
        if (TextUtils.isEmpty(sSrc) || TextUtils.isEmpty(sKey)) {
            return sSrc;
        }
        try {
            byte[] encrypt = DeOREncrypt(Base64.decode(sSrc.getBytes("utf-8"), NO_WRAP), sKey, Cipher.DECRYPT_MODE, tag);
            return new String(encrypt, "utf-8");
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
