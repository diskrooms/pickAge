package tech.picktime.ageCompute;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.apkfuns.logutils.LogUtils;
import com.pkmmte.view.CircularImageView;
import com.tencent.mm.opensdk.openapi.IWXAPI;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import tech.picktime.ageCompute.Utils.HttpUtils;
import tech.picktime.ageCompute.Utils.ImageUtils;

import static android.R.attr.id;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private Bitmap              originBitmap = null;        //图片bitmap资源
    private ImageView           showImage;                  //展示图片容器
    private CircularImageView   share;                      //分享按钮
    private CircleTextView      ageGuess;                   //猜年龄按钮
    private ImageButton         openGallery;                //打开图库
    private ImageView           photo;                      //打开相机

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
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        //判断用户是否已经同意用户协议
        SharedPreferences preferences = getSharedPreferences("agreeUserProtocol",MODE_WORLD_READABLE);
        int agree = preferences.getInt("agreeUserProtocol", 0);
        if(agree == 0){
            Intent intent = new Intent();
            intent.setClass(getApplicationContext(),ProtocolActivity.class);
            startActivity(intent);
            this.finish();
        }
        //originBitmap = ((BitmapDrawable) ContextCompat.getDrawable(MainActivity.this,R.drawable.wtlw)).getBitmap();

        //存一张原始图片到sdcard中 (该方案废弃，采用底层方法直接传输文件流，但是并不是直接把 Bitmap 转换为字节流
        //Bitmap是被Android封装好的一个对象 在原来图片信息的基础上加入了很多元素。正确的方法是要获取到原始资源的字节流
        //此前一直在错误的方向上走着，越走越错

        /*String dirPath = Environment.getExternalStorageDirectory().getPath()+"/tech.picktime.guessAge";//  /storage/emulated/0
        File dir = new File(dirPath);
        if(!dir.exists()){
            dir.mkdirs();          //mkdirs 创建多级目录  mkdir创建一级目录 如果这一级上没有目录 程序会报错
        }
        path = dirPath+"/wtlw.jpg";
        File file = new File(path);
        try {
            if(!file.exists()) {
                ImageUtils.saveBitmap(originBitmap, path, 20);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        ageGuess = (CircleTextView)findViewById(R.id.guessAge);
        openGallery = (ImageButton) findViewById(R.id.openGallery);
        photo = (ImageView) findViewById(R.id.photo);

        ageGuess.setOnClickListener(this);
        openGallery.setOnClickListener(this);
        photo.setOnClickListener(this);
    }

    public void onClick(View v){
        switch(v.getId()){
            case R.id.guessAge:
                //发送网络请求 将图片发送到face++的人脸检测接口
                //1.base64方式(采用这种方式体积会比原来增大1/3左右,超过2MB去请求face++的接口就会报Request Entity Too Large错误)
                //第一次内部资源图片采取将bitmap转换成base64发送的方式
                if(path.isEmpty()) {
                    //String imgBase64 = ImageUtils.bitmap2Base64(originBitmap);
                    //LogUtils.v(originBitmap.getByteCount());
                    //LogUtils.v(imgBase64.length);
                    /*OkHttpClient okHttpClient = new OkHttpClient();
                        //构造body体
                    RequestBody body = new FormBody.Builder()
                            .add("api_key", faceKey)
                            .add("api_secret", faceSecret)
                            .add("image_base64",imgBase64)
                            .add("return_attributes","gender,age,beauty")
                            .build();*/
                    /*RequestBody body = new MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("api_key", faceKey)
                            .addFormDataPart("api_secret", faceSecret)
                            .addFormDataPart("image_file", "wtlw.jpg", RequestBody.create(MediaType.parse("image/jpg"), mfile))
                            .addFormDataPart("return_attributes","gender,age,beauty")
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
                            LogUtils.v(response.body().string());
                        }
                    });*/

                    //自定义post发送
                    InputStream stream = getResources().openRawResource(R.drawable.wtlw);
                    ByteArrayOutputStream out = new ByteArrayOutputStream(1000);
                    byte[] b = new byte[1000];
                    int n;
                    try {
                        while ((n = stream.read(b)) != -1) {
                            out.write(b, 0, n);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        stream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    byte[] buff = out.toByteArray();

                    final HashMap<String, String> map = new HashMap<>();
                    final HashMap<String, byte[]> byteMap = new HashMap<>();
                    map.put("api_key", faceKey);
                    map.put("api_secret", faceSecret);
                    map.put("return_attributes", "gender,age,beauty");
                    byteMap.put("image_file", buff);

                    try{
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                byte[] bach = new byte[0];
                                try {
                                    bach = HttpUtils.post(faceUrl, map, byteMap);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                String result = new String(bach);
                                LogUtils.v(result);
                            }
                        }).start();

                    }catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    //2.以后用户获取外部sdcard图片文件则用 multipart/form-data 方式直接发送文件
                    //LogUtils.v(path);     ///storage/emulated/0/tencent/QQ_Images/wtlw.jpg  相当于//sdcard/xxx
                    File imgFile = new File(path);
                    //LogUtils.v(imgFile.getName());
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
                }
                break;

            case R.id.openGallery:
                //6.0动态申请权限 检查是否有读取文件权限 有就读取 没有就提示申请权限
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                    //ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE);
                    //没有权限
                    //是否要向用户解释这个权限 返回true则向用户弹出解释对话框 返回false则不解释直接进行权限申请
                    if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)){
                        //LogUtils.v("解释");
                    } else {
                        //LogUtils.v("不解释直接申请");
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

                    }

                } else {
                    //调用系统相册
                    Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    intent.putExtra("type",1);
                    startActivityForResult(intent, 1);
                }
        }
    }

    /**
     * 用户权限申请的回调
     * 结果发送到该函数上
     * @param requestCode   请求权限时发送的自定义整形数
     * @param permission
     * @param grantResults  用户反馈结果
     */
    public void onRequestPermissionsResult(int requestCode, String[] permission, int[] grantResults){
        switch(requestCode){
            case 1:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    //用户同意了授权 调用系统相册
                    Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    intent.putExtra("type",1);
                    startActivityForResult(intent, 1);
                } else {
                    //用户拒绝了授权
                }
        }
    }

    //调用系统相册返回图片路径
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //获取图片路径
        if (requestCode == 1 && resultCode == Activity.RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            String[] filePathColumns = {MediaStore.Images.Media.DATA};
            Cursor c = getContentResolver().query(selectedImage, filePathColumns, null, null, null);
            c.moveToFirst();
            int columnIndex = c.getColumnIndex(filePathColumns[0]);
            String imagePath = c.getString(columnIndex);
            //Intent loadOneImageIntent = new Intent(this,BrowseImageActivity.class);
            //loadOneImageIntent.putExtra("path",imagePath);
            //startActivity(loadOneImageIntent);
            c.close();
            path = imagePath;
        }
    }

}
