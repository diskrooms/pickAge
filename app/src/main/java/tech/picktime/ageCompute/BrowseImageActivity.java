package tech.picktime.ageCompute;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

import com.apkfuns.logutils.LogUtils;
import com.pkmmte.view.CircularImageView;
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX;
import com.tencent.mm.opensdk.modelmsg.WXImageObject;
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import tech.picktime.ageCompute.Utils.ImageUtils;


public class BrowseImageActivity extends AppCompatActivity implements View.OnClickListener{

    //导入动态链接库
    static {
        //System.loadLibrary("OpenCV");
        //System.loadLibrary("opencv_java3");
    }
    private Bitmap              originBitmap;       //图片bitmap资源
    private ImageView           showImage;          //展示图片容器
    private CircularImageView   share;              //分享按钮
    private CircleTextView      ageGuess;           //猜年龄按钮

    //微信分享api实例
    private IWXAPI api;
    // APP_ID 替换为你的应用从官方网站申请到的合法appId
    public static final String APP_ID = "wx083841cff060f353";
    //缩略图尺寸
    private static final int THUMB_SIZE = 100;
    //本地图片文件路径
    private String path = "";

    //face++ url
    private static final String faceUrl = "https://api-cn.faceplusplus.com/facepp/v3/detect";

    //face++ key
    private static final String faceKey = "oBeHtmSwrYpe4jLhrYnbJH4jknvEZoOZ";   //试用版
    //private static final String faceKey = "XCTi9-nfIa5-51Xm6kMFxDIy7yFBcTCL"; //正式版
    //face++ secret
    private static final String faceSecret = "iBgppyrxA_f4e4nTbkXy8rQemTpU3ULF";    //试用版
    //private static final String faceSecret = "aL6OMAhPwgiKziOdIU-pjmfoinmcDV15";  //正式版

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        api = WXAPIFactory.createWXAPI(this, APP_ID);     //初始化分享api实例

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.browse_image);
        Intent intent = getIntent();
        int type = intent.getIntExtra("type",1);
        //LogUtils.v(type);
        if(type == 1){
            //打开系统相册或者自定义打开相册
            path = intent.getStringExtra("path");
            //加载图片
            //LogUtils.v(path);
            originBitmap = BitmapFactory.decodeFile(path);
            showImage = (ImageView)findViewById(R.id.browseImage);
            //share = (CircularImageView)findViewById(R.id.share);
            //检测图片旋转角度 如果被旋转了 就再旋转回来
            int degree = CommonUtils.getPicDegree(path);
            Bitmap newBitmap = CommonUtils.rotateBitmapDegree(originBitmap,degree);
            originBitmap = newBitmap;                   //必须 原图已经recycle 所以要重新赋值
            showImage.setImageBitmap(originBitmap);
        } else {
            //打开摄像头拍照传递过去的图片

            //byte[] bitmapByte = intent.getByteArrayExtra("bmp");
            //originBitmap = BitmapFactory.decodeByteArray(bitmapByte, 0, bitmapByte.length);
            //showImage.setImageBitmap(originBitmap);
        }

        ageGuess = (CircleTextView)findViewById(R.id.ageGuess);
        ageGuess.setOnClickListener(this);
    }

    public void onClick(View v){
        switch(v.getId()){
            case R.id.ageGuess:
                //发送网络请求 将图片发送到face++的人脸检测接口
                //1.base64方式(采用这种方式体积会比原来增大1/3左右,超过2MB去请求face++的接口就会报Request Entity Too Large错误)
                    //将bitmap转换成base64
                    /*String imgBase64 = ImageUtils.bitmap2Base64(originBitmap);
                    //LogUtils.v(imgBase64.length());
                    OkHttpClient okHttpClient = new OkHttpClient();
                        //构造body体
                    RequestBody body = new FormBody.Builder()
                            .add("api_key", faceKey)
                            .add("api_secret", faceSecret)
                            .add("image_base64",imgBase64)
                            .build();
                        //构造请求
                    Request request = new Request.Builder()
                            .url(faceUrl)
                            .post(body)
                            .build();
                        //异步发送请求
                    okHttpClient.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            e.printStackTrace();
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            LogUtils.v(response);
                        }
                    });*/
                //2.multipart/form-data 方式直接发送文件
                    File imgFile = new File(path);
                    OkHttpClient okHttpClient = new OkHttpClient();
                    RequestBody body = new MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("api_key", faceKey)
                            .addFormDataPart("api_secret", faceSecret)
                            .addFormDataPart("image_file", imgFile.getName(), RequestBody.create(MediaType.parse("image/jpg"), imgFile))
                            .addFormDataPart("return_attributes","gender,age,beauty")
                            .build();
                    Request request = new Request.Builder()
                            .url(faceUrl)
                            .post(body)
                            .build();
                    //异步发送请求
                    okHttpClient.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            e.printStackTrace();
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            LogUtils.v(response.body().string());

                            //Toast.makeText(BrowseImageActivity.this,"abc",Toast.LENGTH_LONG);
                        }
                    });
                break;
        }
    }

    private String buildTransaction(final String type) {
        return (type == null) ? String.valueOf(System.currentTimeMillis()) : type + System.currentTimeMillis();
    }
}
