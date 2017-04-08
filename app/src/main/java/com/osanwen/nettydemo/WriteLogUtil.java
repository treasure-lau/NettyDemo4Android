package com.osanwen.nettydemo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import timber.log.Timber;

/**
 * 写日志
 * Created by LiuSaibao on 12/8/2016.
 */

public class WriteLogUtil {

    public static void writeLogByThread(final String log) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                writeLog(log);
            }
        }).start();
    }

    public static void writeLog(String log) {
        String date = DateFormatUtil.format(System.currentTimeMillis(), DateFormatUtil.DateFormatEnum.ymd);
        String dir = AppCacheUtil.getPathByFolderType(AppCacheUtil.FolderType.logs);
        File dirFile = new File(dir, date);
        if (!dirFile.exists()) {
            dirFile.mkdir();
        }
        File file = new File(dirFile, "log.txt");
        try {
            String dateTime = DateFormatUtil.format(System.currentTimeMillis(), DateFormatUtil.DateFormatEnum.ymdhms);
            FileOutputStream fileOut= new FileOutputStream(file, true);
            OutputStreamWriter outputWriter=new OutputStreamWriter(fileOut);
            outputWriter.write(String.format("%s:%s\n", dateTime, log));
            outputWriter.close();
        } catch (IOException e) {
            Timber.e(e, e.getMessage());
        }
    }
}
