package com.osanwen.nettydemo;

import android.os.Environment;

import java.io.File;

/**
 * 本地缓存
 * Created by liusaibao on 2015/7/31.
 */
public class AppCacheUtil {

    public static String rootFolder = "NettyDemo";

    public enum FolderType {
        logs("logs");

        private String folder;

        FolderType(String folder) {
            this.folder = folder;
        }

        public String getFolder() {
            return folder;
        }
    }

    private static File getCacheRootFile() {
        File cacheRootDir = null;

        // 判断sd卡是否存在
        boolean sdCardExist = Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED);
        if (sdCardExist) {
            // 获取根目录
            cacheRootDir = new File(Environment.getExternalStorageDirectory(), rootFolder);
        } else {
            cacheRootDir = new File(rootFolder);
        }

        if (!cacheRootDir.exists()) {
            cacheRootDir.mkdir();// 如果路径不存在就先创建路径
        }
        return cacheRootDir;
    }

    public static String getPathByFolderType(FolderType ft) {
        File cacheDir = new File(getCacheRootFile(), ft.getFolder());
        if (!cacheDir.exists()) {
            cacheDir.mkdir();// 如果路径不存在就先创建路径
        }
        return cacheDir.getPath();
    }
}
