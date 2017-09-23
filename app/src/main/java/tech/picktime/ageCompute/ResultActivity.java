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
        poemTextView.setText("童孙未解供耕织\r\n也傍桑阴学种瓜\t\t\t\t\t\t\t\t");
    }
}
