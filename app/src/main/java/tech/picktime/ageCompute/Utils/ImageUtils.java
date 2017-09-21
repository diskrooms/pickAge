package tech.picktime.ageCompute.Utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import com.apkfuns.logutils.LogUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by root on 17-9-12.
 * 图片相关工具类 based on FileUtil
 */
public class ImageUtils extends FileUtil{
    /**
     *  将bitmap保存为图片 注意读写权限
     *  @param bitmap 图片资源
     *  @param path  要保存的位置(路径)
     *  @param scale 压缩比例 0-100
     */
    public static boolean saveBitmap(Bitmap bitmap, String path,int scale) throws IOException {
        File file = new File(path);
        if (file.exists()) {
            //file.delete();
            LogUtils.v(path+"===>error,the file exists");
        }
        FileOutputStream out;
        try {
            out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, scale, out);
            out.flush();
            out.close();
            return true;
        } catch (FileNotFoundException e) {
            //文件夹不存在会报这个异常
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 生成一个随机图片名称
     * @param prefix 名称前缀
     * @param suffix 名称后缀
     * @return picName 生成的图片名称
     */
    public static String createPicName(String prefix,String suffix){
        Date date = new Date();
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String time = dateFormat.format(date);
        String picName = prefix+'_'+time+'.'+suffix;
        return picName;
    }

    /**
     * bitmap转为base64
     * @param bitmap 图片bitmap资源
     * @return
     */
    public static String bitmap2Base64(Bitmap bitmap) {
        String result = null;
        ByteArrayOutputStream baos = null;
        try {
            if (bitmap != null) {
                baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                baos.flush();
                baos.close();
                byte[] bitmapBytes = baos.toByteArray();
                result = Base64.encodeToString(bitmapBytes, Base64.DEFAULT);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (baos != null) {
                    baos.flush();
                    baos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * bitmap转为bytes 字节流
     * @param bitmap 图片bitmap资源
     * @return byte 字节流
     */
    public static byte[] bitmap2Bytes(Bitmap bitmap) {
        String result = null;
        ByteArrayOutputStream baos = null;
        byte[] bitmapBytes = null;
        try {
            if (bitmap != null) {
                baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                baos.flush();
                baos.close();
                bitmapBytes = baos.toByteArray();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (baos != null) {
                    baos.flush();
                    baos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return bitmapBytes;
    }

    /**
     * 图片压缩 不保存图片 接收bitmap 返回一个压缩的体量更小的bitmap
     * bitmap 待压缩的图片资源
     * type  图片压缩格式 1 JPEG  2 PNG  3 WEBP
     * scale 压缩比例
     */
    public static Bitmap compressBitmap(Bitmap originBitmap,int type,int scale) {
        Bitmap newBitmap = null;
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            //将bitmap放入流中
            switch (type){
                case 1:
                    originBitmap.compress(Bitmap.CompressFormat.JPEG, scale, byteArrayOutputStream);
                    break;
                case 2:
                    originBitmap.compress(Bitmap.CompressFormat.PNG, scale, byteArrayOutputStream);
                    break;
                case 3:
                    originBitmap.compress(Bitmap.CompressFormat.WEBP, scale, byteArrayOutputStream);
                    break;
                default:
                    break;
            }
            byte[] bytes = byteArrayOutputStream.toByteArray();
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
            newBitmap = BitmapFactory.decodeStream(byteArrayInputStream, null, null);
        }catch (Exception e){
            e.printStackTrace();
        }
        return newBitmap;
    }

    /**
     * 图片压缩至某一个阈值 threshold 之下
     * @param originBitmap 输入的原始 Bitmap
     * @param type   图片压缩格式 1 JPEG 2 PNG 3 WEBP
     * @Param threshold 压缩到该阈值 保证图片压缩到该阈值下 单位为kb
     */
    public static Bitmap compressBitmap2Threshold(Bitmap originBitmap,int type,int threshold){
        Bitmap newBitmap = null;
        Bitmap.CompressFormat format = null;
        if(type == 1){
            format = Bitmap.CompressFormat.JPEG;
        } else if(type == 2){
            format = Bitmap.CompressFormat.PNG;
        } else {
            format = Bitmap.CompressFormat.WEBP;
        }
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            originBitmap.compress(format,100,byteArrayOutputStream);
            byte[] bytes = byteArrayOutputStream.toByteArray();
            int length = bytes.length / 1024;
            //TODO for循环先验证还是先执行
            for(int i = 90; (i >= 0) && (length > threshold); i = i - 10){
                byteArrayOutputStream.reset();
                originBitmap.compress(format,i,byteArrayOutputStream);
                bytes = byteArrayOutputStream.toByteArray();
                length = bytes.length / 1024;
            }
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
            newBitmap = BitmapFactory.decodeStream(byteArrayInputStream);
        } catch (Exception e){
            e.printStackTrace();
        }
        return newBitmap;
    }


}