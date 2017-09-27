package tech.picktime.ageCompute;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;

public class ResultActivity extends AppCompatActivity {

    private TextView poemTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_result);
        poemTextView = (TextView)findViewById(R.id.poem);
        poemTextView.setText("童孙未解供耕织\r\n也傍桑阴学种瓜\u3000\u3000\u3000\u3000");       //\u0020 和\t 相当于四分之一个汉字  \u3000 全角空格相当于半个个汉字
    }
}
