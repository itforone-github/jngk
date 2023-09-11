package co.kr.itforone.jngk;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.webkit.WebView;
import android.widget.Magnifier;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ViewerBase extends WebView {

    int inputType = EditorInfo.TYPE_NULL;

    Context mContent;
    float mScaleX, mScaleY;
    Bitmap __bmpBack;
    static Magnifier __magnifier = null;

    public ViewerBase(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ViewerBase(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ViewerBase(Context context) {
        super(context);
    }



    public void setInputType(int type) {
        inputType = type;
    }

    public int getInputType() {
        return inputType;
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        InputConnection ic = super.onCreateInputConnection(outAttrs);
        outAttrs.privateImeOptions="defaultInputmode=korean";

        return ic;
    }
}
