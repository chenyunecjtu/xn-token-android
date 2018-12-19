package com.cdkj.baselibrary.utils;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.view.View;
import android.widget.ScrollView;

import com.cdkj.baselibrary.CdApplication;
import com.cdkj.baselibrary.appmanager.AppConfig;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


/**
 * Bitmap操作
 * Created by cdkj on 2017/11/7.
 */

public class BitmapUtils {

    public static int picWidth = 1080;
    public static int picHeight = 1920;

    /**
     * 图片质量压缩法
     *
     * @param
     * @return
     */
    public static byte[] compressImage(String filePath) {

        if (TextUtils.isEmpty(filePath)) {
            return null;
        }

        final BitmapFactory.Options boptions = new BitmapFactory.Options();
        boptions.inJustDecodeBounds = true;//只解析图片边沿，获取宽高
        BitmapFactory.decodeFile(filePath, boptions);
        // 计算缩放比
        boptions.inSampleSize = calculateInSampleSize(boptions, 480, 800);
        // 完整解析图片返回bitmap
        boptions.inJustDecodeBounds = false;

        Bitmap image = BitmapFactory.decodeFile(filePath, boptions);

        image = rotaingImageView(getBitmapDegree(filePath), image); //旋转图片

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (image != null) {
            int quality = 100;
            image.compress(Bitmap.CompressFormat.JPEG, quality, baos);//质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
            int options = 90;
            while ((baos.toByteArray().length / 1024) > 150) {  //循环判断如果压缩后图片是否大于150kb,大于继续压缩
                baos.reset();//重置baos即清空baos
                image.compress(Bitmap.CompressFormat.JPEG, options, baos);//这里压缩options%，把压缩后的数据存放到baos中

                options -= 10;//每次都减少10
                if (options <= 10) {
                    break;
                }
            }
        }
        byte[] byteArray = baos.toByteArray();
        try {
            if (baos != null) {
                baos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        LogUtil.E("图片压缩");

        return byteArray;
    }


    //来自luban的压缩算法
    private static int calculateInSampleSize(BitmapFactory.Options options) {
        int srcWidth = options.outWidth % 2 == 1 ? options.outWidth + 1 : options.outWidth;
        int srcHeight = options.outHeight % 2 == 1 ? options.outHeight + 1 : options.outHeight;

        int longSide = Math.max(srcWidth, srcHeight);
        int shortSide = Math.min(srcWidth, srcHeight);

        float scale = ((float) shortSide / longSide);
        if (scale <= 1 && scale > 0.5625) {
            if (longSide < 1664) {
                return 1;
            } else if (longSide >= 1664 && longSide < 4990) {
                return 2;
            } else if (longSide > 4990 && longSide < 10240) {
                return 4;
            } else {
                return longSide / 1280 == 0 ? 1 : longSide / 1280;
            }
        } else if (scale <= 0.5625 && scale > 0.5) {
            return longSide / 1280 == 0 ? 1 : longSide / 1280;
        } else {
            return (int) Math.ceil(longSide / (1280.0 / scale));
        }
    }

    /*
* 旋转图片
* @param angle
* @param bitmap
* @return Bitmap
*/
    public static Bitmap rotaingImageView(int angle, Bitmap bitmap) {
        try {
            //旋转图片 动作
            Matrix matrix = new Matrix();
            ;
            matrix.postRotate(angle);
            // 创建新的图片
            Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            return resizedBitmap;
        } catch (Exception e) {

        }
        return bitmap;
    }

    /**
     * 读取图片的旋转的角度
     *
     * @param path 图片绝对路径
     * @return 图片的旋转角度
     */
    private static int getBitmapDegree(String path) {
        int degree = 0;
        try {
            // 从指定路径下读取图片，并获取其EXIF信息
            ExifInterface exifInterface = new ExifInterface(path);
            // 获取图片的旋转信息
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            LogUtil.E("图片旋转角度异常" + e);
        }
        LogUtil.E("图片旋转角度" + degree);
        return degree;
    }


    public static int calculateInSampleSize(BitmapFactory.Options options,
                                            int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        return inSampleSize;
    }


    /**
     * 压缩Bitmap的大小
     *
     * @param imagePath     图片文件路径
     * @param requestWidth  压缩到想要的宽度
     * @param requestHeight 压缩到想要的高度
     * @return Bitmap
     */
    public static Bitmap decodeBitmapFromFile(String imagePath, int requestWidth, int requestHeight) {
        if (!TextUtils.isEmpty(imagePath)) {
            if (requestWidth <= 0 || requestHeight <= 0) {
                Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                return bitmap;
            }
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;//不加载图片到内存，仅获得图片宽高
            BitmapFactory.decodeFile(imagePath, options);
            if (options.outHeight == -1 || options.outWidth == -1) {
                try {
                    ExifInterface exifInterface = new ExifInterface(imagePath);
                    int height = exifInterface.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, ExifInterface.ORIENTATION_NORMAL);//获取图片的高度
                    int width = exifInterface.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, ExifInterface.ORIENTATION_NORMAL);//获取图片的宽度
                    options.outWidth = width;
                    options.outHeight = height;

                } catch (IOException e) {
                }
            }
            int degree = getBitmapDegree(imagePath);//获取旋转角度

            options.inSampleSize = calculateInSampleSize(options, requestWidth, requestHeight); //计算获取新的采样率
//            options.inSampleSize = Math.min(options.outWidth / requestWidth, options.outHeight / requestHeight);

            options.inJustDecodeBounds = false;


            if (degree == 0) {
                return BitmapFactory.decodeFile(imagePath, options);
            } else {
                return rotaingImageView(degree, BitmapFactory.decodeFile(imagePath, options));
            }

        } else {
            LogUtil.E("拍照生成图片false");
            return null;
        }

    }


    /**
     * 保存bitmap 图片
     *
     * @param bitmap
     * @param imageName
     * @return
     */
    public static String saveBitmapFile(Bitmap bitmap, String imageName) {

        File file1 = new File(getDirectory());
        if (!file1.exists()) {
            file1.mkdirs();
        }
        String filename = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA)
                .format(new Date());
        String imagename = filename + imageName + ".jpg";
        File file = new File(file1, imagename);
        try {
            if (file.exists()) {
                file.delete();
                file.createNewFile();
            } else {
                file.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (Exception e) {
        }

        // 最后通知图库更新
        try {
            CdApplication.getContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                    Uri.fromFile(file)));
        } catch (Exception e) {
        }

        return file.getPath();
    }


    /**
     * 获得缓存目录
     **/
    public static String getDirectory() {
        String dir = getSDPath() + File.separator + AppConfig.CACHDIR;
        LogUtil.E("拍照图片路径" + dir);
        return dir;
    }


    /**
     * 取SD卡路径
     **/
    private static String getSDPath() {
        File sdDir = null;
        boolean sdCardExist = TextUtils.equals(Environment.getExternalStorageState(), Environment.MEDIA_MOUNTED); // 判断sd卡是否存在
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory(); // 获取根目录
        }
        if (sdDir != null) {
            return sdDir.toString();
        } else {
            return "";
        }
    }


    public static String getImageWidthHeight(String path) {
        BitmapFactory.Options options = new BitmapFactory.Options();

        /**
         * 最关键在此，把options.inJustDecodeBounds = true;
         * 这里再decodeFile()，返回的bitmap为空，但此时调用options.outHeight时，已经包含了图片的高了
         */
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options); // 此时返回的bitmap为null
        /**
         *options.outHeight为原始图片的高
         */
        String imageWidth = options.outWidth + "";
        String imageHeight = options.outHeight + "";
        String size = "_" + imageWidth + "_" + imageHeight;

        System.out.print("size = _" + imageWidth + "_" + imageHeight);
        return size;
    }


    /**
     * 截取scrollview的生产bitmap
     *
     * @param scrollView
     * @return
     */
    public static Bitmap getBitmapByView(ScrollView scrollView) {
        LogUtil.E("aaa图片");
        int h = 0;
        Bitmap bitmap = null;
        // 获取scrollview实际高度
        for (int i = 0; i < scrollView.getChildCount(); i++) {
            if (scrollView.getChildAt(i).getVisibility() != View.VISIBLE) {
                continue;
            }
            h += scrollView.getChildAt(i).getHeight();
            scrollView.getChildAt(i).setBackgroundColor(
                    Color.parseColor("#ffffff"));
        }
        // 创建对应大小的bitmap
        bitmap = Bitmap.createBitmap(scrollView.getWidth(), h,
                Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        scrollView.draw(canvas);
        return bitmap;
    }

    /**
     * 截取view的生产bitmap
     *
     * @param view
     * @return
     */
    public static Bitmap getBitmapByView(View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(),
                Bitmap.Config.ARGB_8888);

        final Canvas canvas = new Canvas(bitmap);

        view.draw(canvas);


//        view.setDrawingCacheEnabled(true);
//        view.buildDrawingCache();
//        Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());

        return bitmap;
    }

    /**
     * 微信分享图片压缩
     *
     * @param image
     * @return
     */
    public static byte[] WeChatBitmapToByteArray(Bitmap image) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);//质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中

        if (baos.toByteArray().length <= 32 * 1024) {
            return baos.toByteArray();
        }

//        final BitmapFactory.Options boptions = new BitmapFactory.Options();

//        boptions.inJustDecodeBounds = true;//只解析图片边沿，获取宽高
//        // 计算缩放比
//        boptions.inSampleSize = calculateInSampleSize(boptions);
//        // 完整解析图片返回bitmap
//        boptions.inJustDecodeBounds = false;
//        image = BitmapFactory.decodeByteArray(baos.toByteArray(), 0, baos.toByteArray().length, boptions);


        int options = 90;
        while ((baos.toByteArray().length) > 32 * 1024) {  //循环判断如果压缩后图片是否大于32kb,大于继续压缩
            baos.reset();//重置baos即清空baos
            image.compress(Bitmap.CompressFormat.JPEG, options, baos);//这里压缩options%，把压缩后的数据存放到baos中
            options -= 10;//每次都减少10
            if (options <= 10) {
                break;
            }
        }

        byte[] byteArray = baos.toByteArray();

        try {
            if (baos != null) {
                baos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return byteArray;
    }

    public static byte[] Bitmap2Bytes(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return baos.toByteArray();
    }

}
