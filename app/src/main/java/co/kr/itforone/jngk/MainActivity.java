package co.kr.itforone.jngk;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.databinding.DataBindingUtil;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLDecoder;

import co.kr.itforone.jngk.databinding.ActivityMainBinding;
import util.BackPressCloseHandler;
import util.Common;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    private BackPressCloseHandler backPressCloseHandler;
    boolean isIndex = true;
    final int FILECHOOSER_NORMAL_REQ_CODE=1200,FILECHOOSER_LOLLIPOP_REQ_CODE=1300;
    ValueCallback<Uri> filePathCallbackNormal;
    ValueCallback<Uri[]> filePathCallbackLollipop;
    Uri mCapturedImageURI;
    String firstUrl = "";
    public Activity thisActivity = null;
    public WebView webView;
    // 자동로그인
    public SharedPreferences preferences;  // 로그인데이터저장
    public SharedPreferences.Editor pEditor;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        binding = DataBindingUtil.setContentView(this,R.layout.activity_main);
        binding.setMainData(this);

        // 자동로그인
        preferences = getSharedPreferences("member", Activity.MODE_PRIVATE);
        if(preferences!=null) {
            pEditor = preferences.edit();
        }
        
        webView = binding.webView;

        //화면을 계속 켜짐
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // 웹뷰를 실행할 때 메모리 누수가 심하지 않게 설정하는 것
        if(Build.VERSION.SDK_INT>=24){
            try{
                Method m = StrictMode.class.getMethod("disableDeathOnFileUriExposure");
                m.invoke(null);
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        firstUrl=getString(R.string.url);//웹뷰에 처음 실행할 것을 지정하는 url 주소입니다.
        Intent intent = getIntent();
        //푸시가 있을 때는 아래에 있는 소스가 실행이 됩니다.
        try{
            if(!intent.getExtras().getString("goUrl").equals("")){
                //firstUrl=intent.getExtras().getString("goUrl")+"?"+intent.getExtras().getString("mb_3");
                firstUrl=intent.getExtras().getString("goUrl");
            }
        }catch (Exception e){

        }

        /* cookie */
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CookieSyncManager.createInstance(this);
        }
        //스플래시 실행
        Intent startIntent = new Intent(MainActivity.this, SplashActivity.class);
        startActivity(startIntent);
        webViewSetting();

        //webView.addJavascriptInterface(new WebviewJavainterface(this, this), "Android");
    }
    @SuppressLint("ResourceAsColor")
    @RequiresApi(api = Build.VERSION_CODES.ECLAIR_MR1)
    public void webViewSetting() {
        Common.setTOKEN(this);
        WebSettings setting = binding.webView.getSettings();//웹뷰 세팅용
        if(Build.VERSION.SDK_INT >= 21) {
            binding.webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        setting.setAllowFileAccess(true);//웹에서 파일 접근 여부
        setting.setAppCacheEnabled(true);//캐쉬 사용여부
        setting.setGeolocationEnabled(true);//위치 정보 사용여부
        setting.setDatabaseEnabled(true);//HTML5에서 db 사용여부
        setting.setDomStorageEnabled(true);//HTML5에서 DOM 사용여부
        setting.setCacheMode(WebSettings.LOAD_DEFAULT);//캐시 사용모드 LOAD_NO_CACHE는 캐시를 사용않는다는 뜻
        setting.setJavaScriptEnabled(true);//자바스크립트 사용여부
        setting.setSupportMultipleWindows(false);//윈도우 창 여러개를 사용할 것인지의 여부 무조건 false로 하는 게 좋음
        setting.setUseWideViewPort(true);//웹에서 view port 사용여부
        setting.setTextZoom(100);
        setting.setSupportZoom(true);
        binding.webView.setWebChromeClient(chrome);//웹에서 경고창이나 또는 컴펌창을 띄우기 위한 메서드
        binding.webView.setWebViewClient(client);//웹페이지 관련된 메서드 페이지 이동할 때 또는 페이지가 로딩이 끝날 때 주로 쓰임
        binding.webView.setDownloadListener(download); // 파일 다운로드
        // 자동로그인 - APP_VER 추가
        setting.setUserAgentString("Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.84 Mobile Safari/537.36"+"/AJNGK/APP_VER=1"); // AJNGK : PHP에서 앱으로 로그인 했는지 체크할 수 있음
        binding.webView.addJavascriptInterface(new WebJavascriptEvent(), "Android");
        //뒤로가기 버튼을 눌렀을 때 클래스로 제어함
        backPressCloseHandler = new BackPressCloseHandler(this);
        //Toast.makeText(MainActivity.this, MainActivity.this.getFilesDir().toString(), Toast.LENGTH_SHORT).show();
        
        // 자동로그인
        // 로그인데이터 get전달
        firstUrl += (firstUrl.contains("?"))? "&" : "?";
        firstUrl += "app_mb_id=" + preferences.getString("appLoginId", "");
        
        binding.webView.loadUrl(firstUrl);
    }

    DownloadListener download;
    {
        download = new DownloadListener() {
            //@RequiresApi(api = Build.VERSION_CODES.N) // minSdkVersion 버전보다 낮을 경우 컴파일 에러
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                Log.d("api version", String.valueOf(Build.VERSION_CODES.N));
                Log.d("download url ", url);
                Log.d("download userAgent ", userAgent);
                Log.d("contentDisposition", contentDisposition);
                Log.d("download mimetype ", mimetype);
                Log.d("download contentLength ", String.valueOf(contentLength));

                try {
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                    DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                    contentDisposition = URLDecoder.decode(contentDisposition, "UTF-8"); // 디코딩 (파일이름이 한글일 경우 깨져서)
                    String fileName = contentDisposition.replace("attachment;filename=", ""); // contentDisposition 로그 확인 후 파일명 앞까지 자름 (php에서 file_download.php 사용)
                   //String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);

                    // 세션 유지를 위해 쿠키 세팅하기
                    String cookie = CookieManager.getInstance().getCookie(url);
                    request.addRequestHeader("Cookie", cookie);
                    request.setMimeType(mimetype); // 확장자를 안드로이드에게 알려줌
                    request.addRequestHeader("User-Agent", userAgent);
                    request.setDescription("Downloading File...");
                    request.setAllowedOverMetered(true);
                    request.setAllowedOverRoaming(true);
                    request.setTitle(fileName);
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName); // 첫번째 인자 : 저장경로 / 두번째 인자 : 저장할 파일이름

                    dm.enqueue(request);
                    Toast.makeText(getApplicationContext(), "Downloading File...", Toast.LENGTH_SHORT).show();
                    //registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

                } catch (UnsupportedEncodingException e) {
                    if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(thisActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                            Toast.makeText(getBaseContext(), "파일 다운로드 권한을 허용해주십시오.", Toast.LENGTH_LONG).show();
                            ActivityCompat.requestPermissions(thisActivity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1004);
                        }
                        else {
                            Toast.makeText(getBaseContext(), "파일 다운로드 권한을 허용해주십시오.", Toast.LENGTH_LONG).show();
                            ActivityCompat.requestPermissions(thisActivity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1004);
                        }
                    }
                }
            }

            /*BroadcastReceiver onComplete = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Toast.makeText(getApplicationContext(), "Downloading Complete", Toast.LENGTH_SHORT).show();
                }
            };*/
        };
    }

    WebChromeClient chrome;
    {
        chrome = new WebChromeClient() {
            //새창 띄우기 여부
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                return false;
            }

            //경고창 띄우기
            @Override
            public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage("\n" + message + "\n")
                        .setCancelable(false)
                        .setPositiveButton("확인",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        result.confirm();
                                    }
                                }).create().show();
                return true;
            }

            //컴펌 띄우기
            @Override
            public boolean onJsConfirm(WebView view, String url, String message,
                                       final JsResult result) {
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage("\n" + message + "\n")
                        .setCancelable(false)
                        .setPositiveButton("확인",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        result.confirm();
                                    }
                                })
                        .setNegativeButton("취소",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        result.cancel();
                                    }
                                }).create().show();
                return true;
            }

            //현재 위치 정보 사용여부 묻기
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                // Should implement this function.
                final String myOrigin = origin;
                final GeolocationPermissions.Callback myCallback = callback;
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Request message");
                builder.setMessage("Allow current location?");
                builder.setPositiveButton("Allow", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        myCallback.invoke(myOrigin, true, false);
                    }

                });
                builder.setNegativeButton("Decline", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        myCallback.invoke(myOrigin, false, false);
                    }

                });
                AlertDialog alert = builder.create();
                alert.show();
            }
            // For Android < 3.0
            public void openFileChooser(ValueCallback<Uri> uploadMsg) {
                openFileChooser(uploadMsg, "");
            }

            // For Android 3.0+
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
                filePathCallbackNormal = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("video/*"); // 21.03.02 동영상 첨부하기 위하여 타입 변경
                startActivityForResult(Intent.createChooser(i, "File Chooser"), FILECHOOSER_NORMAL_REQ_CODE);
            }

            // For Android 4.1+
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
                openFileChooser(uploadMsg, acceptType);
            }


            // For Android 5.0+\
            // SDK 21 이상부터 웹뷰에서 파일 첨부를 해주는 기능입니다.
            public boolean onShowFileChooser(
                    WebView webView, ValueCallback<Uri[]> filePathCallback,
                    FileChooserParams fileChooserParams) {
                //카메라 프로바이더로 이용해서 파일을 가져오는 방식입니다.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {// API 24 이상 일경우..
                    File imageStorageDir = new File(MainActivity.this.getFilesDir() + "/jngk", "jngk");
                    if (!imageStorageDir.exists()) {
                        // Create AndroidExampleFolder at sdcard
                        imageStorageDir.mkdirs();
                    }
                    // Create camera captured image file path and name

                    //Toast.makeText(mainActivity.getApplicationContext(),imageStorageDir.toString(),Toast.LENGTH_LONG).show();
                    File file = new File(imageStorageDir, "IMG_" + String.valueOf(System.currentTimeMillis()) + ".jpg");
                    Uri providerURI = FileProvider.getUriForFile(MainActivity.this, MainActivity.this.getPackageName() + ".provider", file);
                    mCapturedImageURI = providerURI;

                } else {// API 24 미만 일경우..

                    File imageStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "jngk");
                    if (!imageStorageDir.exists()) {
                        // Create AndroidExampleFolder at sdcard
                        imageStorageDir.mkdirs();
                    }
                    // Create camera captured image file path and name
                    File file = new File(imageStorageDir + File.separator + "IMG_" + String.valueOf(System.currentTimeMillis()) + ".jpg");
                    mCapturedImageURI = Uri.fromFile(file);
                }
                if (filePathCallbackLollipop != null) {
//                    filePathCallbackLollipop.onReceiveValue(null);
                    filePathCallbackLollipop = null;
                }
                filePathCallbackLollipop = filePathCallback;
                Intent captureIntent = new Intent(); // 21.03.02 파일 선택만 띄우기 위하여 괄호 내용 지움 (MediaStore.ACTION_IMAGE_CAPTURE)
                captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI);

                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("video/*"); // 21.03.02 동영상 첨부하기 위하여 타입 변경

                // Create file chooser intent
                Intent chooserIntent = Intent.createChooser(i, "Image Chooser");
                // Set camera intent to file chooser
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Parcelable[]{captureIntent});

                // On select image call onActivityResult method of activity
                startActivityForResult(chooserIntent, FILECHOOSER_LOLLIPOP_REQ_CODE);
                return true;

            }

        };
    }

    WebViewClient client;
    {
        client = new WebViewClient() {
            /*// 웹뷰 빈화면으로 표시되는 오류 때문에 설정 -
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed(); // SSL 에러가 발생해도 계속 진행!
            }*/

            //페이지 로딩중일 때 (마시멜로) 6.0 이후에는 쓰지 않음
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {

                Log.d("url",url);
                if (url.equals(getString(R.string.url)) || url.equals(getString(R.string.domain))) {
                    isIndex=true;
                } else {
                    isIndex=false;
                }
                if(url.startsWith("tel:")){
                    Intent tel = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
                    startActivity(tel);
                    return true;
                }
                if (url.startsWith("intent:")) {

                    try {
                        Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                        Intent existPackage = getPackageManager().getLaunchIntentForPackage(intent.getPackage());
                        if (existPackage != null) {
                            startActivity(intent);
                        } else {
                            Intent marketIntent = new Intent(Intent.ACTION_VIEW);
                            marketIntent.setData(Uri.parse("market://details?id=" + intent.getPackage()));
                            startActivity(marketIntent);
                        }
                        return true;
                    } catch (Exception e) {
                        Log.d("error1",e.toString());
                        e.printStackTrace();
                    }
                }
                return false;
            }
            //페이지 로딩이 다 끝났을 때
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                binding.webLayout.setRefreshing(false);

                Log.d("url",url);
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    CookieSyncManager.getInstance().sync();
                } else {
                    CookieManager.getInstance().flush();
                }
                Log.d("mb_id", Common.getPref(MainActivity.this,"ss_mb_id",""));
                //로그인할 때
                if(url.startsWith(getString(R.string.domain)+"bbs/login.php")||url.startsWith(getString(R.string.domain)+"bbs/register_form.php")){
                    view.loadUrl("javascript:fcmKey('"+ Common.TOKEN+"')");
                }

                if (url.equals(getString(R.string.url)) || url.equals(getString(R.string.domain)) || url.equals(getString(R.string.admin_url)) || url.startsWith(getString(R.string.domain)+"bbs/login.php")) {
                    isIndex=true;
                } else {
                    isIndex=false;
                }
                Log.d("isIndex",isIndex+"");
                binding.webLayout.setRefreshing(false);
                //메인 화면이 아닌 페이지는 새로고침을 할 수 있고
                if(isIndex==false) {
                    binding.webLayout.setEnabled(true);
                    binding.webLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                        @Override
                        public void onRefresh() {
                            binding.webView.clearCache(true);
                            binding.webView.reload();
                            binding.webLayout.setRefreshing(false);
                        }
                    });
                    //메인화면이면은 새로고침을 할 수 없습니다.
                }else{
                    binding.webLayout.setEnabled(false);
                }

                // 자동로그인 시 토큰 값 변경하기 위하여 추가(?)
                //webView.loadUrl("javascript:fcmUpdate('"+ Common.TOKEN +"')");
            }
            //페이지 오류가 났을 때 6.0 이후에는 쓰이지 않음
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                clearAppData(MainActivity.this);
            }
        };
    }
    //다시 들어왔을 때
    @Override
    protected void onResume() {
        super.onResume();
        /* cookie */
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CookieSyncManager.getInstance().startSync();
        }




        Log.d("newtork","onResume");


        //netCheck.networkCheck();
    }
    //홈버튼 눌러서 바탕화면 나갔을 때
    @Override
    protected void onPause() {
        super.onPause();
        /* cookie */
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CookieSyncManager.getInstance().stopSync();
        }

    }
    //뒤로가기를 눌렀을 때
    public void onBackPressed() {
        //super.onBackPressed();
        //웹뷰에서 히스토리가 남아있으면 뒤로가기 함
        Log.d("isIndex",isIndex+"");
        if(!isIndex) {
            if (binding.webView.canGoBack()) {
                binding.webView.goBack();
            } else if (binding.webView.canGoBack() == false) {
                backPressCloseHandler.onBackPressed();
            }
        }else{
            backPressCloseHandler.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        //여기서부터 카메라와 갤러리에서 가져오는 값이 조금씩 달라집니다.
        Uri[] results = null;
        if(resultCode==RESULT_OK) {
            //롤리팝 이전 버전 소스는 일단 뺐습니다.
            if(requestCode == FILECHOOSER_LOLLIPOP_REQ_CODE){
                Uri[] result = new Uri[0];
                Log.d("filePath1", mCapturedImageURI.toString());

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {


                    if (resultCode == RESULT_OK) {
                        result = (intent == null) ? new Uri[]{mCapturedImageURI} : WebChromeClient.FileChooserParams.parseResult(resultCode, intent);
                    }
                    filePathCallbackLollipop.onReceiveValue(result);
                    //크롭 액티비티로 이동
                    /*CropImage.activity(result[0])
                             .setGuidelines(CropImageView.Guidelines.ON)//가이드라인을 보여줄 것인지 여부
                             .setAspectRatio(1,1)//가로 세로 1:1로 자르기 기능 * 1:1 4:3 16:9로 정해져 있어요
                             .setCropShape(CropImageView.CropShape.OVAL)//사각형과 동그라미를 선택할 수 있어요 OVAL은 동그라미 RECTAGLE은 사각형이예요 안 넣으면 사각형이예요
                             .start(this);*/
                }
            }
            else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {

                CropImage.ActivityResult result = CropImage.getActivityResult(intent);
                if (resultCode == RESULT_OK) {
                    Uri resultUri = result.getUri();
                    Uri[] result2 = new Uri[0];
                    result2 =  new Uri[]{resultUri} ;

                    Log.d("image-uri", resultUri.toString());
                    filePathCallbackLollipop.onReceiveValue(result2);
                } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                    Exception error = result.getError();
                }
            }

        }else{
            try {
                if (filePathCallbackLollipop != null) {
                    filePathCallbackLollipop.onReceiveValue(null);
                    filePathCallbackLollipop = null;
                }
            }catch (Exception e){

            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d("onConfigurationChanged" , "onConfigurationChanged");
        if(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){//세로 전환시
            Log.d("onConfigurationChanged" , "Configuration.ORIENTATION_PORTRAIT");
        }else if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE){ //가로전환시
            Log.d("onConfigurationChanged", "Configuration.ORIENTATION_LANDSCAPE");
        }else{

        }
    }

    //로그인 로그아웃
    class WebJavascriptEvent{
        @JavascriptInterface
        public void setLogin(String mb_id){
            Log.d("login","로그인");
            Common.savePref(getApplicationContext(),"ss_mb_id",mb_id);
        }
        @JavascriptInterface
        public void setLogout(){
            Log.d("logout","로그아웃");
            Common.savePref(getApplicationContext(),"ss_mb_id","");
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        @JavascriptInterface
        public void doShare(String url){
            Intent sharedIntent = new Intent();
            sharedIntent.setAction(Intent.ACTION_SEND);
            sharedIntent.setType("text/plain");
            sharedIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
            sharedIntent.putExtra(Intent.EXTRA_TEXT, url);
            Intent chooser = Intent.createChooser(sharedIntent, "공유");
            startActivity(chooser);
        }
        @JavascriptInterface
        public void landscape(){
            // UNSPECIFIED - 시스템이 설정한 방향, LANDSCAPE - 가로, PORTRAIT - 세로
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            //Toast.makeText(MainActivity.this,"테스트",Toast.LENGTH_SHORT).show();
        }
        @JavascriptInterface
        public void init_orientation() {
            //Toast.makeText(MainActivity.this,"테스트",Toast.LENGTH_SHORT).show();
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
        @JavascriptInterface
        public void updateLoginInfo(final String mb_id) { // 자동로그인
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    pEditor.putString("appLoginId", mb_id);
                    pEditor.apply();

                    Log.d("로그:updateLoginInfo()", mb_id);
                }
            });
        }
    }
    //앱 데이터 삭제하기
    public  void clearAppData(Context context) {
        File cache = context.getCacheDir();  //캐시 폴더 호출
        File appDir = new File(cache.getParent());  //App Data 삭제를 위해 캐시 폴더의 부모폴더까지 호출
        if(appDir.exists()) {
            String[] children = appDir.list();
            for(String s : children) {
                //App Data 폴더의 리스트를 deleteDir 를 통해 하위 디렉토리 삭제
                deleteDir(new File(appDir, s));
            }
        }
    }
    public  boolean deleteDir(File dir) {
        if(dir != null && dir.isDirectory()) {
            String[] children = dir.list();

            //파일 리스트를 반복문으로 호출
            for(int i=0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if(!success) {
                    return false;
                }
            }
        }
        //캐시와 데이터를 삭제한 후에 다시 새로고침하기
        //참고로 데이터 삭제가 되면 다시 로그인을 해야 함
        binding.webView.reload();
        //디렉토리가 비어있거나 파일이므로 삭제 처리
        return dir.delete();
    }
}