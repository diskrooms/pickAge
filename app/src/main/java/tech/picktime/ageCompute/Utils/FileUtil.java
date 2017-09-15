package tech.picktime.ageCompute.Utils;

import java.io.File;
import java.io.FileInputStream;

/**
 * Created by jsb-hdp-0 on 2017/9/14.
 * 文件相关工具类
 */

public class FileUtil {

    /**
     * 根据文件的磁盘路径计算文件大小
     * @param filePath 文件路径
     * @return size 文件尺寸  正常为long整形字节数 文件不存在返回-1
     */
    public static long getSize(String filePath){
        File file = new File(filePath);
        long size = 0;
        try {
            if(file.exists()){
                FileInputStream fileInputStream = new FileInputStream(file);
                size = fileInputStream.available();
            } else {
                return -1;
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return size;
    }

    
}
