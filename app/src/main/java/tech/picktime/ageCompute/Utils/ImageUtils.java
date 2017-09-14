package tech.picktime.ageCompute.Utils;

import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;
import com.apkfuns.logutils.LogUtils;

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
     *  将bitmap保存为图片
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
     * @param bitmap
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
}