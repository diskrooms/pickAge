package tech.picktime.ageCompute;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;

public class ProtocolContentActivity extends AppCompatActivity implements View.OnClickListener{

    private ImageButton backToProtocol;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_protocol_content);
        backToProtocol = (ImageButton)findViewById(R.id.backToProtocol);
        backToProtocol.setOnClickListener(this);
    }

    public void onClick(View v){
        switch (v.getId()){
            case R.id.backToProtocol:
                this.finish();
                break;
            default:
                break;
        }
    }
}
