package tech.picktime.ageCompute;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

/**
 * Created by jsb-hdp-0 on 2017/9/19.
 */

public class MoveImageView extends android.support.v7.widget.AppCompatImageView {
    public MoveImageView(Context context) {
        super(context);
    }
    public MoveImageView(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }
    public MoveImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    public void setLocation(int x, int y) {
        this.setFrame(x, y - this.getHeight(), x + this.getWidth(), y);
    }
    // 移动
    public boolean autoMouse(MotionEvent event) {
        boolean rb = false;
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                this.setLocation((int) event.getX(), (int) event.getY());
                rb = true;
                break;
        }
        return rb;
    }
}

//public class TestImageViewMove extends AppCompatActivity {
//    private MoveImageView moveImageView;
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        //setContentView(R.layout.main);
//        //moveImageView = (MoveImageView) this.findViewById(R.id.ImageView01);
//    }
//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        moveImageView.autoMouse(event);
//        return false;
//    }
//}