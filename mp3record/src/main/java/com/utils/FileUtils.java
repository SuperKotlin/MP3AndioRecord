package com.utils;

import android.os.Environment;

import java.io.File;


/**
 * 文件工具类
 * 作者：zhuyong on 2019/2/27 17:22
 * 邮箱：99305919@qq.com
 * 希望每天叫醒你的不是闹钟而是梦想
 */
public class FileUtils {
    private static final String SD_PATH = Environment.getExternalStorageDirectory().getPath();
    private static final String DATA_PATH = Environment.getDataDirectory().getPath();
    private static final String SD_STATE = Environment.getExternalStorageState();
    public static final String NAME = "MP3AudioGroup";

    public static String getAppPath() {
        StringBuilder sb = new StringBuilder();
        if (SD_STATE.equals(
                android.os.Environment.MEDIA_MOUNTED)) {
            sb.append(SD_PATH);
        } else {
            sb.append(DATA_PATH);
        }
        sb.append(File.separator);
        sb.append(NAME);
        sb.append(File.separator);
        return sb.toString();
//        return Environment.getExternalStorageDirectory() + "/AAA测试目录/";
    }


    public static void deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            if (file.isFile()) {
                file.delete();
            } else {
                String[] filePaths = file.list();
                for (String path : filePaths) {
                    deleteFile(filePath + File.separator + path);
                }
                file.delete();
            }
        }
    }
}
