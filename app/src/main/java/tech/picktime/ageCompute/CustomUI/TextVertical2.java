package tech.picktime.ageCompute.CustomUI;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;

import com.apkfuns.logutils.LogUtils;

import tech.picktime.ageCompute.R;


/**
 * Created by jsb-hdp-0 on 2017/9/23.
 */

public class TextVertical2 extends AppCompatTextView {

    public TextVertical2(Context context, AttributeSet attrs){
        super(context,attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs,new int[]{R.styleable.TextVertical2_bgColor});
        //LogUtils.v(attrs);
        String text = typedArray.getString(R.styleable.TextVertical2_bgColor);
        LogUtils.v(text);
    }

    public void onDraw(Canvas canvas){
        String text = "窈窕淑女窈窕淑女窈窕淑女窈窕淑女窈窕淑女窈窕淑女";
        Paint mPaint = new Paint();
        mPaint.setColor(Color.YELLOW);
        canvas.drawRect(0, 0, getMeasuredWidth(), getMeasuredHeight(), mPaint);
        Rect mBound = new Rect();
        mPaint.getTextBounds(text, 0, text.length(), mBound);

        mPaint.setColor(Color.BLUE);
        //canvas.drawText(mTitleText, getWidth() / 2 - mBound.width() / 2, getHeight() / 2 + mBound.height() / 2, mPaint);
        Path path = new Path();
        path.lineTo(0,500);             //定义字符路径

        float w;
        float top = 18;
        float left = getWidth()-10;

        final int len = text.length();
        float py = 0 + top;

        for(int i=0; i<len; i ++){
            char c = text.charAt(i);
            w = mPaint.measureText(text, i, i+1);//获取字符宽度
            StringBuffer b = new StringBuffer();
            b.append(c);
            /*if(py > 81){//定义字的范围
                return;
            }*/
            if(isChinese(c)){
                py += w;

                canvas.drawText(b.toString(), left, py, mPaint); //中文处理方法
            }else {
                canvas.drawTextOnPath(b.toString(), path, py, -left-2, mPaint);//其他文字处理方法
                py += w;
            }
        }
    }

    private boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS) {
            return true;
        }
        return false;
    }
}
