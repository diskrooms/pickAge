package tech.picktime.ageCompute;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentCallbacks;
import android.content.ComponentCallbacks2;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.LoginFilter;
import android.util.Log;
import android.util.Size;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.apkfuns.logutils.LogUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;

import tech.picktime.ageCompute.Utils.ImageUtils;

import static android.view.KeyEvent.KEYCODE_BACK;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends BaseActivity implements View.OnClickListener{

    private static final int STATE_PREVIEW = 0;     //Camera state: Showing camera preview.

    private static final int STATE_WAITING_LOCK = 1;        //Camera state: Waiting for the focus to be locked.

    private static final int STATE_WAITING_PRECAPTURE = 2;  //Camera state: Waiting for the exposure to be precapture state.

    private static final int STATE_WAITING_NON_PRECAPTURE = 3;  //Camera state: Waiting for the exposure state to be something other than precapture.

    private static final int STATE_PICTURE_TAKEN = 4;   //Camera state: Picture was taken.

    //成员属性
    private long mClickTime = 0;                            //存储点击返回按钮的时间戳
    private AutoFitTextureView textureView;
    private CameraDevice mCameraDevice;                     //摄像头设备对象
    private String mCameraId;                               //摄像头ID 0前置摄像头 1后置摄像头
    private CameraDevice.StateCallback DeviceStateCallback; //摄像头设备状态回调
    private Handler backgroundhandler;
    private HandlerThread backgroundhandlerThread;
    private ImageReader mImageReader;
    private CameraManager mCameraManager;                               //摄像头管理器
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private CameraCaptureSession mCaptureSession;
    private int mSensorOrientation;
    private Size mPreviewSize;                                          //预览尺寸
    private Size currentSize;                                           //本机全屏尺寸
    private File mFile;
    private int mState = STATE_PREVIEW; //
    private CaptureRequest.Builder previewRequestBuilder;
    private String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE}; //需要动态申请的权限集合
    //UI控件
    private ImageButton openGallery;                                    //打开图库按钮
    private ImageView photo;                                            //点击拍照按钮
    private GridView gridView;                                          //显示图片排列容器

    static {
        //System.loadLibrary("hellojni");
        //System.loadLibrary("libOpenCV");
    }

    /**
     * Saves a JPEG {@link Image} into the specified {@link File}.
     */
    private static class ImageSaver implements Runnable {

        private final Image mImage;
        private final File mFile;
        public ImageSaver(Image image, File file) {
            mImage = image;
            mFile = file;
        }

        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(mFile);
                output.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            //Log.v("width",width+" ");
            //Log.v("height",height+" ");
            try {
                initSystem();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            //configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }

    };

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            backgroundhandler.post(new ImageSaver(reader.acquireNextImage(), mFile));

        }

    };

    //成员属性
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);*/
        //初始化
        init();
        //LogUtils.v("onCreate");
    }

    /*protected void onStart() {
        super.onStart();
        //LogUtils.v("onStart");
    }*/

    public void onResume() {
        super.onResume();
        //startBackgroundThread();
        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (textureView.isAvailable()) {
            try {
                initSystem();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        } else {
            textureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
        //LogUtils.v("onResume");
    }

    protected void onPause(){
        super.onPause();
        //关闭摄像头
        closeCamera();
        stopBackgroundThread();
        //LogUtils.v("onPause");
    }


    //给surfaceHolder添加回调监控surfaceHolder状态
    private void init() {
        //Toast.makeText(this, NativeMethod.getStringName(), Toast.LENGTH_SHORT).show();
        Window window = getWindow();
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_photo);
        textureView = (AutoFitTextureView) findViewById(R.id.textureview);
        openGallery = (ImageButton) findViewById(R.id.openGallery);
        photo = (ImageView) findViewById(R.id.photo);
        openGallery.setOnClickListener(this);
        photo.setOnClickListener(this);

        //添加回调监控surfaceview状态
        //surfaceHolder.addCallback(new surfaceHolderCallback());
        //本机全屏宽高比
        currentSize = new Size(CommonUtils.getScreenHeight(this),CommonUtils.getScreenWidth(this));

        //
        mFile = new File(getExternalFilesDir(null), ImageUtils.createPicName("_ageCompute_","jpg"));
    }



    /**
     * 选择合适的预览尺寸
     * @param choices
     * @param textureViewWidth
     * @param textureViewHeight
     * @param maxWidth
     * @param maxHeight
     * @param aspectRatio
     * @return
     */
    private static Size chooseSuitableSize(Size[] choices, int textureViewWidth, int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        //LogUtils.v(choices);
        //LogUtils.v(textureViewWidth);
        //LogUtils.v(textureViewHeight);
        //LogUtils.v(maxWidth);
        //LogUtils.v(maxHeight);
        //LogUtils.v(aspectRatio);

        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        //可选的宽高要比 最大尺寸maxWidth和maxHeight要小 并且要和当前尺寸宽高比aspectRatio保持一致
        //如果宽和高都比textureView大 就入选 bigEnough 否则就入选 notBigEnough
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }
        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.

        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CommonUtils.CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CommonUtils.CompareSizesByArea());
        } else {
            LogUtils.d("Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    /**
     * 设置合适的图像尺寸
     * 防止摄像头预览图像变形
     */
    private void setUpCameraOutput(int width,int height){
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        try {
            //LogUtils.v(manager.getCameraIdList());
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

                // We don't use a front facing camera in this sample.
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }

                // For still image captures, we use the largest available size.
                Size largest = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new CommonUtils.CompareSizesByArea());

                mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(), ImageFormat.JPEG, /*maxImages*/2);

                //mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, backgroundhandler);
                // Find out if we need to swap dimension to get the preview size relative to sensor
                // coordinate.
                int displayRotation = getWindowManager().getDefaultDisplay().getRotation();
                //noinspection ConstantConditions
                mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                boolean swappedDimensions = false;
                switch (displayRotation) {
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_180:
                        if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                            swappedDimensions = true;
                        }
                        break;
                    case Surface.ROTATION_90:
                    case Surface.ROTATION_270:
                        if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                            swappedDimensions = true;
                        }
                        break;
                    default:
                        LogUtils.d("Display rotation is invalid: " + displayRotation);
                }

                Point displaySize = new Point();
                getWindowManager().getDefaultDisplay().getSize(displaySize);
                int rotatedPreviewWidth = width;
                int rotatedPreviewHeight = height;
                int maxPreviewWidth = displaySize.x;
                int maxPreviewHeight = displaySize.y;
                if (swappedDimensions) {
                    rotatedPreviewWidth = height;
                    rotatedPreviewHeight = width;
                    maxPreviewWidth = displaySize.y;
                    maxPreviewHeight = displaySize.x;
                }
                /*if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                    maxPreviewWidth = MAX_PREVIEW_WIDTH;
                }

                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                    maxPreviewHeight = MAX_PREVIEW_HEIGHT;
                }*/

                // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
                // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                // garbage capture data.
                //LogUtils.v(rotatedPreviewWidth);
                //LogUtils.v(rotatedPreviewHeight);
                //LogUtils.v(maxPreviewWidth);
                //LogUtils.v(maxPreviewHeight);
                //LogUtils.v(largest);
                //LogUtils.v(map.getOutputSizes(SurfaceTexture.class));

                mPreviewSize =   chooseSuitableSize(map.getOutputSizes(SurfaceTexture.class), rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth, maxPreviewHeight, currentSize);

                // We fit the aspect ratio of TextureView to the size of preview we picked.
                int orientation = getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    textureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                } else {
                    textureView.setAspectRatio(mPreviewSize.getHeight(),mPreviewSize.getWidth());
                }
                // Check if the flash is supported.
                Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                //mFlashSupported = available == null ? false : available;
                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            //ErrorDialog.newInstance(getString(R.string.camera_error)).show(getChildFragmentManager(), FRAGMENT_DIALOG);
        }
    }


    private void configureTransform(int viewWidth, int viewHeight) {
        //Activity activity = getActivity();
        if (null == textureView || null == mPreviewSize || null == MainActivity.this) {
            return;
        }
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        textureView.setTransform(matrix);
    }

    /**
     * 初始化系统
     */
    private void initSystem() throws CameraAccessException {
        backgroundhandlerThread = new HandlerThread("Camera2");
        backgroundhandlerThread.start();
        backgroundhandler = new Handler(backgroundhandlerThread.getLooper());
        mCameraId = "" + CameraCharacteristics.LENS_FACING_FRONT;   //前置摄像头
        int width = CommonUtils.getScreenWidth(MainActivity.this);
        int height = CommonUtils.getScreenHeight(MainActivity.this);

        setUpCameraOutput(width,height);
        configureTransform(width, height);
        //mImageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);

        // 拿到拍照照片数据
        //1.直接拿到bitmap数据传入initent 容易数据溢出
        /*mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                mCameraDevice.close();
                Image image = reader.acquireNextImage();
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                Intent captureIntent = new Intent(MainActivity.this,BrowseImageActivity.class);
                captureIntent.putExtra("type","2");
                //captureIntent.putExtra("bmp",bytes); //数据太长会报 TransactionTooLargeException 异常
                startActivity(captureIntent);
                buffer.get(bytes);//由缓冲区存入字节数组
                final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bitmap != null) {
                    iv_show.setImageBitmap(bitmap);
                    LogUtils.v(bitmap);
                }
                Intent howOldIntent = new Intent(MainActivity.this,BrowseImageActivity.class);
                startActivity(howOldIntent);
            }
        }, backgroundhandler);*/

        //2.先把图片存入sdcard或者缓存 然后再到另一个acvitiy上去取
        mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, backgroundhandler);

        //获取摄像头管理
        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mCameraManager.openCamera(mCameraId, stateCallback, backgroundhandler);
    }

    /**
     * 摄像头创建监听
     */
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {//打开摄像头
            //LogUtils.v("open camera");
            mCameraDevice = camera;
            //开启预览
            openPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {//关闭摄像头
            //LogUtils.v("close camera");
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            Toast.makeText(MainActivity.this, "摄像头开启失败,请前往设置或者安全中心开启摄像头权限", Toast.LENGTH_SHORT).show();

        }
    };


    /**
     * 开始预览
     */
    private void openPreview() {
        try {
            // 创建预览需要的CaptureRequest.Builder
            previewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            // 将SurfaceView的surface作为CaptureRequest.Builder的目标
            SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(),mPreviewSize.getHeight());
            Surface surface = new Surface(surfaceTexture);
            previewRequestBuilder.addTarget(surface);

            // 创建CameraCaptureSession，该对象负责管理处理预览请求和拍照请求
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() // ③
            {
                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                    if (null == mCameraDevice) {
                        return;
                    }
                    // 当摄像头已经准备好时，开始显示预览
                    mCaptureSession = cameraCaptureSession;
                    try {
                        //检测是否支持自动对焦
                        ////2017-09-13修改 start
                        if(isAutoFocusSupported()){
                            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
                        } else {
                            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        }
                        ////2017-09-13修改 start

                        // 打开闪光灯
                        //previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                        //previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_SCENE_MODE_SNOW);
                        // 显示预览
                        CaptureRequest previewRequest = previewRequestBuilder.build();
                        mCaptureSession.setRepeatingRequest(previewRequest, mCaptureCallback, backgroundhandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(MainActivity.this, "配置失败", Toast.LENGTH_SHORT).show();
                }
            }, backgroundhandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 连续按两次回退键退出应用
     * @param keyCode   按键代码
     * @param keyEvent
     * @return
     */
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent){
        if(keyCode == KEYCODE_BACK){
            long currentTime = System.currentTimeMillis();
            if((currentTime - mClickTime) > 2000){
                mClickTime = currentTime;
                Toast.makeText(getApplicationContext(), "再按一次退出", Toast.LENGTH_SHORT).show();
            } else {
                this.finish();
                System.exit(0);

            }
        }
        return true;
    }


    /**
     * Closes the current {@link CameraDevice}.
     */
    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        if(backgroundhandlerThread != null) {
            backgroundhandlerThread.quitSafely();
            try {
                backgroundhandlerThread.join();
                backgroundhandlerThread = null;
                backgroundhandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void showToast(final String s){
        runOnUiThread(new Runnable(){
            public void run(){
                Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show();
            }
        });
    }

    //指派点击事件
    public void onClick(View view){
        switch (view.getId()){
            //拍照
            case R.id.photo:
                photo();
                break;
            //打开相册
            case R.id.openGallery:
                //gridView.setAdapter(customGridViewAdapter);
                //启动自定义相册
                //Intent openGalleryIntent = new Intent(this,ListImageActivity.class);
                //startActivity(openGalleryIntent);

                //6.0动态申请权限 检查是否有读取文件权限 有就读取文件 没有就提示申请文件
                if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                    //ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE);
                    //没有权限
                    //是否要向用户解释这个权限 返回true则向用户弹出解释对话框 返回false则不解释直接进行权限申请
                    if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,Manifest.permission.READ_EXTERNAL_STORAGE)){
                        //LogUtils.v("解释");
                    } else {
                        //LogUtils.v("不解释直接申请");
                        ActivityCompat.requestPermissions(MainActivity.this, permissions, 1);

                    }

                } else {
                    //调用系统相册
                    Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    intent.putExtra("type",1);
                    startActivityForResult(intent, 1);
                }
                break;
            default:
                break;
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
            Intent loadOneImageIntent = new Intent(this,BrowseImageActivity.class);
            loadOneImageIntent.putExtra("path",imagePath);
            startActivity(loadOneImageIntent);
            c.close();
        }
    }



    //拍照
    public void photo(){
        try {
            //LogUtils.v(previewRequestBuilder);
            // This is how to tell the camera to lock focus.
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the lock.
            mState = STATE_WAITING_LOCK;
            //拍照
            mCaptureSession.capture(previewRequestBuilder.build(), mCaptureCallback, backgroundhandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * A {@link CameraCaptureSession.CaptureCallback} that handles events related to JPEG capture.
     */
    private CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) {
            switch (mState) {
                case STATE_PREVIEW: {
                    // We have nothing to do when the camera preview is working normally.
                    //2017-09-13添加 start
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState != null && !afState.equals(mLastAfState)) {
                        switch (afState) {
                            case CaptureResult.CONTROL_AF_STATE_INACTIVE:
                                //LogUtils.v("CaptureResult.CONTROL_AF_STATE_INACTIVE");
                                lockAutoFocus();
                                break;
                            case CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN:
                                //LogUtils.v("CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN");
                                break;
                            case CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED:
                                //LogUtils.v("CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED");
                                mUiHandler.removeCallbacks(mLockAutoFocusRunnable);
                                mUiHandler.postDelayed(mLockAutoFocusRunnable, LOCK_FOCUS_DELAY_ON_FOCUSED);
                                break;
                            case CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED:
                                mUiHandler.removeCallbacks(mLockAutoFocusRunnable);
                                mUiHandler.postDelayed(mLockAutoFocusRunnable, LOCK_FOCUS_DELAY_ON_UNFOCUSED);
                                //LogUtils.v("CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED");
                                break;
                            case CaptureResult.CONTROL_AF_STATE_PASSIVE_UNFOCUSED:
                                mUiHandler.removeCallbacks(mLockAutoFocusRunnable);
                                //mUiHandler.postDelayed(mLockAutoFocusRunnable, LOCK_FOCUS_DELAY_ON_UNFOCUSED);
                                //LogUtils.v("CaptureResult.CONTROL_AF_STATE_PASSIVE_UNFOCUSED");
                                break;
                            case CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN:
                                //LogUtils.v("CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN");
                                break;
                            case CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED:
                                mUiHandler.removeCallbacks(mLockAutoFocusRunnable);
                                //mUiHandler.postDelayed(mLockAutoFocusRunnable, LOCK_FOCUS_DELAY_ON_FOCUSED);
                                //LogUtils.v("CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED");
                                break;
                        }
                    }
                    mLastAfState = afState;
                    //2017-09-13添加 end
                    break;
                }
                case STATE_WAITING_LOCK: {
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    //LogUtils.v(afState);  //CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED
                    if (afState == null) {
                        captureStillPicture();
                    } else if (afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED || afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
                        // CONTROL_AE_STATE can be null on some devices
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = STATE_PICTURE_TAKEN;
                            captureStillPicture();
                        } else {
                            runPrecaptureSequence();
                        }
                    }
                    break;
                }
                case STATE_WAITING_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        mState = STATE_WAITING_NON_PRECAPTURE;
                    }
                    break;
                }
                case STATE_WAITING_NON_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mState = STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    }
                    break;
                }
            }
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            process(result);
        }

    };

    /**
     * Capture a still picture. This method should be called when we get a response in
     * {@link #mCaptureCallback} from both {@link #}.
     */
    private void captureStillPicture() {
        //LogUtils.v("captureStillPicture");

        try {
            final Activity activity = MainActivity.this;
            if (null == activity || null == mCameraDevice) {
                return;
            }
            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);

            //点击拍照最关键一行
            captureBuilder.addTarget(mImageReader.getSurface());

            // Use the same AE and AF modes as the preview.
            ////2017-09-13修改 start
            if(isAutoFocusSupported()){
                captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_AUTO);
            } else {
                captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            }
            ////2017-09-13修改 end
            //setAutoFlash(captureBuilder);

            // Orientation
            //int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            //captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));

            CameraCaptureSession.CaptureCallback CaptureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    showToast("Saved: " + mFile);
                    //启动 BrowseImageActivity
                    Intent intent = new Intent(MainActivity.this,BrowseImageActivity.class);
                    intent.putExtra("type","1");
                    intent.putExtra("path",mFile.toString());
                    startActivity(intent);
                }
            };

            mCaptureSession.stopRepeating();
            mCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Run the precapture sequence for capturing a still image. This method should be called when
     * we get a response in {@link #mCaptureCallback} from {@link }.
     */
    private void runPrecaptureSequence() {
        try {
            // This is how to tell the camera to trigger.
            previewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the precapture sequence to be set.
            mState = STATE_WAITING_PRECAPTURE;
            mCaptureSession.capture(previewRequestBuilder.build(), mCaptureCallback,
                    backgroundhandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 捕获屏幕放置状态
     * AndroidManifest设定了方向 该方法失效
     */

    public void onConfigurationChanged(Configuration configuration){
        super.onConfigurationChanged(configuration);
        //Toast.makeText(this,"abc",Toast.LENGTH_LONG).show();
    }


    //切换前后置摄像头
    /*public static final String CAMERA_FRONT = "1";
    public static final String CAMERA_BACK = "0";

    private String cameraId = CAMERA_BACK;
    public void switchCamera() {
        if (cameraId.equals(CAMERA_FRONT)) {
            cameraId = CAMERA_BACK;
            closeCamera();
            reopenCamera();
            switchCameraButton.setImageResource(R.drawable.ic_camera_front);

        } else if (cameraId.equals(CAMERA_BACK)) {
            cameraId = CAMERA_FRONT;
            closeCamera();
            reopenCamera();
            switchCameraButton.setImageResource(R.drawable.ic_camera_back);
        }
    }

    public void reopenCamera() {
        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }*/

    /**
     * Unlock the focus. This method should be called when still image capture sequence is
     * finished.
     */
    private void unlockFocus() {
        try {
            // Reset the auto-focus trigger
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            //setAutoFlash(previewRequestBuilder);
            mCaptureSession.capture(previewRequestBuilder.build(), mCaptureCallback,
                    backgroundhandler);
            // After this, the camera will go back to the normal state of preview.
            mState = STATE_PREVIEW;
            CaptureRequest previewRequest = previewRequestBuilder.build();
            mCaptureSession.setRepeatingRequest(previewRequest, mCaptureCallback,
                    backgroundhandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    ////////////////////////////////////////
    ////2017-09-13添加 start
    private static final long LOCK_FOCUS_DELAY_ON_FOCUSED = 5000;
    private static final long LOCK_FOCUS_DELAY_ON_UNFOCUSED = 1000;

    private Integer mLastAfState = null;
    private Handler mUiHandler = new Handler(); // UI handler
    private Runnable mLockAutoFocusRunnable = new Runnable() {
        @Override
        public void run() {
            lockAutoFocus();
        }
    };

    public void lockAutoFocus() {
        try {
            // This is how to tell the camera to lock focus.
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
            CaptureRequest captureRequest = previewRequestBuilder.build();
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, null); // prevent CONTROL_AF_TRIGGER_START from calling over and over again
            if(mCaptureSession != null) {
                mCaptureSession.capture(captureRequest, mCaptureCallback, backgroundhandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @return
     */
    private float getMinimumFocusDistance() {
        if (mCameraId == null)
            return 0;

        Float minimumLens = null;
        try {
            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            CameraCharacteristics c = manager.getCameraCharacteristics(mCameraId);
            minimumLens = c.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
        } catch (Exception e) {
            LogUtils.v("isHardwareLevelSupported Error", e);
        }
        if (minimumLens != null)
            return minimumLens;
        return 0;
    }

    /**
     *
     * @return
     */
    private boolean isAutoFocusSupported() {
        return  isHardwareLevelSupported(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) || getMinimumFocusDistance() > 0;
    }

    // Returns true if the device supports the required hardware level, or better.
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private boolean isHardwareLevelSupported(int requiredLevel) {
        boolean res = false;
        if (mCameraId == null)
            return res;
        try {
            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            CameraCharacteristics cameraCharacteristics = manager.getCameraCharacteristics(mCameraId);

            int deviceLevel = cameraCharacteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
            switch (deviceLevel) {
                case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3:
                    //LogUtils.v("Camera support level: INFO_SUPPORTED_HARDWARE_LEVEL_3");
                    break;
                case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL:
                    //LogUtils.v("Camera support level: INFO_SUPPORTED_HARDWARE_LEVEL_FULL");
                    break;
                case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY:
                    //LogUtils.v("Camera support level: INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY");
                    break;
                case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED:
                    //LogUtils.v("Camera support level: INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED");
                    break;
                default:
                    //LogUtils.v("Unknown INFO_SUPPORTED_HARDWARE_LEVEL: " + deviceLevel);
                    break;
            }


            if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                res = requiredLevel == deviceLevel;
            } else {
                // deviceLevel is not LEGACY, can use numerical sort
                res = requiredLevel <= deviceLevel;
            }

        } catch (Exception e) {
            LogUtils.v("isHardwareLevelSupported Error", e);
        }
        return res;
    }
    ////2017-09-13添加 end
}
