package com.osanwen.nettydemo;

import java.util.zip.CRC32;

/**
 *
 * Created by LiuSaibao on 11/23/2016.
 */

public class CRC32Util {
    public static String getCRC32Hex(byte[] b) {
        CRC32 crc32 = new CRC32();
        crc32.update(b);
        // 在CRC32前面进入补0操作，防止CRC32转16进制字符串错误
        String result = "0000000" + Long.toHexString(crc32.getValue()).toUpperCase();
        // 最后从字符串的第8位开始截取
        return result.substring(result.length() - 8);
    }

    public static byte[] getCRC32ByteArray(byte[] data) {
        return ByteUtil.intToByteArray((int)getCRC32Long(data));
    }

    public static long getCRC32Long(byte[] data) {
        byte[] flag = {1};
        byte[] sign = "SanWen@2017".getBytes();
        byte[] result = new byte[flag.length + data.length + sign.length];
        // 将flag,data,sign字节数组有序的加入到result字节数组中，并对result字节数组进行CRC32
        System.arraycopy(flag, 0, result, 0, flag.length);
        System.arraycopy(data, 0, result, flag.length, data.length);
        System.arraycopy(sign, 0, result, flag.length + data.length, sign.length);
        return getCRC32(result);
    }

    public static long getCRC32(byte[] data) {
        CRC32 crc32 = new CRC32();
        crc32.update(data);
        return crc32.getValue();
    }
}
