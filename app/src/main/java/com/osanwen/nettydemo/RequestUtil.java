package com.osanwen.nettydemo;

import com.google.gson.Gson;

import timber.log.Timber;

/**
 *
 * Created by LiuSaibao on 1/6/2017.
 */

public class RequestUtil {

    public static byte[] getEncryptBytes(Object obj) {
        String json = new Gson().toJson(obj);
        Blowfish blowfish = new Blowfish();
        return blowfish.encryptBytes(json);
    }

    public static byte[] getRequestHeader(byte[] content, int flag, int function) {
        byte[] fixed = {(byte)0xFE, (byte)0xED, (byte)0xFE, (byte) flag, 0x01, 0x02};
        byte[] func = ByteUtil.shortToByteArray((short) function);
        byte[] len = ByteUtil.intToByteArray(content.length);

        byte[] data = new byte[fixed.length + func.length + len.length];
        System.arraycopy(fixed, 0, data, 0, fixed.length);
        System.arraycopy(func, 0, data, fixed.length, func.length);
        System.arraycopy(len, 0, data, fixed.length + func.length, len.length);
        return data;
    }

    public static byte[] getRequestBody(byte[] header, byte[] content) {

        byte[] crc32 = CRC32Util.getCRC32ByteArray(content);

        byte[] result = new byte[header.length + content.length + crc32.length];
        System.arraycopy(header, 0, result, 0, header.length);
        System.arraycopy(content, 0, result, header.length, content.length);
        System.arraycopy(crc32, 0, result, header.length + content.length, crc32.length);
        return result;
    }
}
