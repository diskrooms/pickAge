package tech.picktime.ageCompute;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.apkfuns.logutils.LogUtils;

public class ProtocolActivity extends AppCompatActivity implements View.OnClickListener,CompoundButton.OnCheckedChangeListener{

    private TextView userProtocol;
    private CheckBox agreeProtocol;
    private TextView agree;

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
        userProtocol.setOnClickListener(this);

        agreeProtocol = (CheckBox) findViewById(R.id.agreeProtocol);
        agreeProtocol.setOnCheckedChangeListener(this);

        agree = (TextView) findViewById(R.id.agree);
        agree.setOnClickListener(this);
    }

    public void onClick(View v){
        switch (v.getId()){
            case R.id.userProtocol:
                Intent intent = new Intent(getApplicationContext(),ProtocolContentActivity.class);
                startActivity(intent);
                break;
            case R.id.agree:
                //检查用户是否已同意协议
                if(agreeProtocol.isChecked()) {
                    SharedPreferences preferences = getSharedPreferences("agreeUserProtocol",MODE_WORLD_READABLE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putInt("agreeUserProtocol", 1);
                    editor.commit();
                    Intent intent_ = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent_);
                    finish();
                } else {
                    Toast.makeText(getApplicationContext(),"请您勾选用户协议",Toast.LENGTH_LONG);
                }
                break;
            default:
                break;
        }
    }


    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        LogUtils.v(b);
    }
}
