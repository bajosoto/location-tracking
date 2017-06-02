package com.example.administrator.smartphonesensing;

import android.os.Environment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Sergio on 6/2/17.
 */

public class LogWriter {

    private String fileName;
    private File root, dir;
    private static final String DIR_NAME = "SmartPhoneSensing";
    SimpleDateFormat simpleDateFormat;
    String timestamp;

    public LogWriter(String fileName) {
        this.fileName = fileName;
        this.root = android.os.Environment.getExternalStorageDirectory();
        this.dir = new File(root.getAbsolutePath() + "/" + DIR_NAME);
        this.simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy-hh-mm-ss");
        this.timestamp =  simpleDateFormat.format(new Date());

        if (!this.dir.mkdirs()) {
            // file not created
        }
    }

    public File writeToFile(String line, boolean append) {
        if (!isExternalStorageWritable())
            return null;
        File file = new File(this.dir, this.fileName);
        FileWriter fileWriter;
        try {
            fileWriter = new FileWriter(file, append);
            fileWriter.write(line);
            fileWriter.flush();
            fileWriter.close();
            return file;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public File clearFile() {
        if (!isExternalStorageWritable())
            return null;
        File file = new File(this.dir, this.fileName);
        FileWriter fileWriter;
        try {
            fileWriter = new FileWriter(file);
            fileWriter.write(this.timestamp);
            fileWriter.write("\n\n");
            fileWriter.flush();
            fileWriter.close();
            return file;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /* Checks if external storage is available for read and write */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public File getDir() {
        return this.dir;
    }
}
