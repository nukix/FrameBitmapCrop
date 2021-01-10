package com.uso6.bitmapcrop;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.text.TextUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Sdcard 工具
 */

public class SDCardUtil {

    // 判断SD卡是否被挂载
    public static boolean isSDCardMounted() {
        return Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED);
    }

    /// 创建指定目录
    public static void creatdir(){
        File dirFirstFolder = new File(Environment.getExternalStorageDirectory().toString()+"/walkPro");
        if(!dirFirstFolder.exists())
        { //如果该文件夹不存在，则进行创建
            dirFirstFolder.mkdirs();//创建文件夹
        }
    }

    /**
     * @return /storage/emulated/0/Android/data/com.bar.scan.allcode.insec/cache
     */
    public static File getSDCardCacheDir(Context context) {
        if (isSDCardMounted()) {
            return context.getExternalCacheDir();
        }
        return null;
    }

    // 获取SD卡的根目录
    public static String getSDCardBaseDir() {
        if (isSDCardMounted()) {
            return Environment.getExternalStorageDirectory().getAbsolutePath();
        }
        return null;
    }

    // 从SDCard中寻找指定目录下的文件，返回Bitmap
    public static Bitmap loadBitmapFromSDCard(String filePath){
        File file = new File(filePath);
        if(file.exists()){
            Bitmap bitmap = BitmapFactory.decodeFile(filePath);
            return bitmap;
        }
        return  null;
    }

    private static Bitmap compressBySize(String pathName, int targetWidth,
                                         int targetHeight) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;// 不去真的解析图片，只是获取图片的头部信息，包含宽高等；
        Bitmap bitmap = BitmapFactory.decodeFile(pathName, opts);
        // 得到图片的宽度、高度；
        float imgWidth = opts.outWidth;
        float imgHeight = opts.outHeight;
        // 分别计算图片宽度、高度与目标宽度、高度的比例；取大于等于该比例的最小整数；
        int widthRatio = (int) Math.ceil(imgWidth / (float) targetWidth);
        int heightRatio = (int) Math.ceil(imgHeight / (float) targetHeight);
        opts.inSampleSize = 1;
        if (widthRatio > 1 || widthRatio > 1) {
            if (widthRatio > heightRatio) {
                opts.inSampleSize = widthRatio;
            } else {
                opts.inSampleSize = heightRatio;
            }
        }
        // 设置好缩放比例后，加载图片进内容；
        opts.inJustDecodeBounds = false;
        bitmap = BitmapFactory.decodeFile(pathName, opts);
        return bitmap;
    }

    private static Bitmap compressBySize(byte[] bm, int targetWidth,
                                         int targetHeight) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;// 不去真的解析图片，只是获取图片的头部信息，包含宽高等；

        // 得到图片的宽度、高度；
        float imgWidth = opts.outWidth;
        float imgHeight = opts.outHeight;
        // 分别计算图片宽度、高度与目标宽度、高度的比例；取大于等于该比例的最小整数；
        int widthRatio = (int) Math.ceil(imgWidth / (float) targetWidth);
        int heightRatio = (int) Math.ceil(imgHeight / (float) targetHeight);
        opts.inSampleSize = 1;
        if (widthRatio > 1 || widthRatio > 1) {
            if (widthRatio > heightRatio) {
                opts.inSampleSize = widthRatio;
            } else {
                opts.inSampleSize = heightRatio;
            }
        }
        // 设置好缩放比例后，加载图片进内容；
        opts.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeByteArray(bm,0,bm.length, opts);
        return bitmap;
    }

    //将Bitmap存放到指定目录下并命名
    public static void saveBitmap(Bitmap bitmap, String filePath, String name){
        //   /storage/emulated/0/PictureSelector/CameraImage/  s00.JPEG
        File file = new File(filePath, name);
        try {
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            String path = file.getPath();
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveBitmap(Bitmap bitmap, String filePath){
        //   /storage/emulated/0/PictureSelector/CameraImage/  s00.JPEG
        if (TextUtils.isEmpty(filePath)) {
            //LogUtil.e("nukix", "SDCardUtil saveBitmap filePath is null!");
            return;
        }
        File file = new File(filePath);
        try {
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            String path = file.getPath();
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static File sdCardSoundCache = null;
    /**
     * 用于存放音频临时文件
     * @return /storage/emulated/0/Android/data/com.bar.scan.allcode.insec/cache/sound
     */
    public static File getSDCardSoundCache(Context context) {
        if (sdCardSoundCache != null) {
            return sdCardSoundCache;
        }

        File sdCardCache = getSDCardCacheDir(context);
        if (sdCardCache == null) {
            return null;
        }

        sdCardSoundCache = new File(sdCardCache, "sound");
        if (!sdCardSoundCache.exists()) {
            sdCardSoundCache.mkdirs();
        }

        return sdCardSoundCache;
    }

    private static File sdCardCrash = null;
    /**
     * 用于存放Crash临时文件
     * @return /storage/emulated/0/Android/data/com.communicate.tranlate/cache/crash
     */
    public static File getSDCardCrashCache(Context context) {
        if (sdCardCrash != null) {
            return sdCardCrash;
        }

        File sdCardCache = getSDCardCacheDir(context);
        if (sdCardCache == null) {
            return null;
        }

        sdCardCrash = new File(sdCardCache, "crash");
        if (!sdCardCrash.exists()) {
            sdCardCrash.mkdirs();
        }

        return sdCardCrash;
    }

    /**
     * @return /storage/emulated/0/Android/data/com.bar.scan.allcode.insec/files
     */
    public static File getSDCardFilesDir(Context context) {
        if (isSDCardMounted()) {
            return context.getExternalFilesDir("");
        }
        return null;
    }

    private static File sdCardPictures = null;
    /**
     * 用于存放图片临时文件
     * @return /storage/emulated/0/Android/data/com.communicate.tranlate/cache/pictures
     */
    public static File getSDCardPicturesCache(Context context) {
        if (sdCardPictures != null) {
            return sdCardPictures;
        }

        File sdCardCache = getSDCardCacheDir(context);
        if (sdCardCache == null) {
            return null;
        }

        sdCardPictures = new File(sdCardCache, "pictures");
        if (!sdCardPictures.exists()) {
            sdCardPictures.mkdirs();
        }

        return sdCardPictures;
    }

}
