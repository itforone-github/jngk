package co.kr.itforone.jngk;

import android.app.Activity;
import android.util.Log;
import android.webkit.JavascriptInterface;

public class WebviewJavainterface {
    Activity activity;
    MainActivity mainActivity;

    public WebviewJavainterface(Activity activity, MainActivity mainActivity) {
        this.activity = activity;
        this.mainActivity = mainActivity;
    }
}
