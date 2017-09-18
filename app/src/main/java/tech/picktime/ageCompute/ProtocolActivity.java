package tech.picktime.ageCompute;

import android.graphics.Paint;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.apkfuns.logutils.LogUtils;

public class ProtocolActivity extends AppCompatActivity {

    private TextView userProtocol;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //去掉标题栏
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        //去掉状态栏
        Window window = getWindow();
        window.setFlags(WindowManager.LayoutParams. FLAG_FULLSCREEN , WindowManager.LayoutParams. FLAG_FULLSCREEN);
        //window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_protocol);
        userProtocol = (TextView) findViewById(R.id.userProtocol);
        userProtocol.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);        //设置下划线
    }
}
