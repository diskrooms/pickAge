package tech.picktime.ageCompute;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.apkfuns.logutils.LogUtils;
import com.pkmmte.view.CircularImageView;
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX;
import com.tencent.mm.opensdk.modelmsg.WXImageObject;
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;

import tech.picktime.ageCompute.Utils.ImageUtils;

import static java.lang.Integer.parseInt;


public class BrowseImageActivity extends AppCompatActivity implements View.OnClickListener{

    static {
        //System.loadLibrary("OpenCV");                         //导入动态链接库
        //System.loadLibrary("opencv_java3");
    }
    private Bitmap originBitmap;
    private ImageView showImage;
    //分享按钮
    private CircularImageView share;

    //微信分享api实例
    private IWXAPI api;
    // APP_ID 替换为你的应用从官方网站申请到的合法appId
    public static final String APP_ID = "wx083841cff060f353";
    //缩略图尺寸
    private static final int THUMB_SIZE = 100;
    //图片路径
    private String path = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        api = WXAPIFactory.createWXAPI(this, APP_ID);     //初始化分享api实例

        setContentView(R.layout.browse_image);
        Intent intent = getIntent();
        String type = intent.getStringExtra("type");
        //LogUtils.v(type);
        if(type.equals("1")){
            path = intent.getStringExtra("path");
            //加载图片
            LogUtils.v(path);
            originBitmap = BitmapFactory.decodeFile(path);
            showImage = (ImageView)findViewById(R.id.browseImage);
            share = (CircularImageView)findViewById(R.id.share);
            //检测图片旋转角度 如果被旋转了 就再旋转回来
            int degree = CommonUtils.getPicDegree(path);
            Bitmap newBitmap = CommonUtils.rotateBitmapDegree(originBitmap,degree);
            originBitmap = newBitmap;                   //必须 原图已经recycle 所以要重新赋值
            showImage.setImageBitmap(newBitmap);
        } else {
            //byte[] bitmapByte = intent.getByteArrayExtra("bmp");
            //originBitmap = BitmapFactory.decodeByteArray(bitmapByte, 0, bitmapByte.length);
            //showImage.setImageBitmap(originBitmap);
        }

    }

    public void onClick(View v){

    }

    private String buildTransaction(final String type) {
        return (type == null) ? String.valueOf(System.currentTimeMillis()) : type + System.currentTimeMillis();
    }
}
