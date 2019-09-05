package cn.douyaba.www;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.regex.Matcher;

import cn.douyaba.www.dialog.DownLoadDialog;
import cn.douyaba.www.util.DownUtils;
import cn.douyaba.www.util.Patterns;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    EditText et_url;
    TextView tv_suc;
    TextView tv_url;
    ImageView iv_beijing;
    // API
    public String BASE_API = "http://120.206.184.170:8086/QianYi/DouYin/RemoveVideoShuiYin";
    public String BASE_API2 = "http://120.206.184.170:8086/QianYi/DouYin/RemoveVideoShuiYin?videoPath=";

    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE};
    //请求状态码
    private static int REQUEST_PERMISSION_CODE = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        et_url = findViewById(R.id.et_url);
        tv_suc = findViewById(R.id.tv_suc);
        tv_url = findViewById(R.id.tv_url);
        iv_beijing = findViewById(R.id.iv_beijing);

        findViewById(R.id.btn_analysis).setOnClickListener(this);
        findViewById(R.id.btn_upload).setOnClickListener(this);


        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, REQUEST_PERMISSION_CODE);
            }
        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode,  String[] permissions,  int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                Log.i("MainActivity", "申请的权限为：" + permissions[i] + ",申请结果：" + grantResults[i]);
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        String text = getCopy(MainActivity.this);
        if (text != null && text.length() > 4 && text.substring(0,4).equals("#在抖音")){
            // 检测到了抖音链接
            Matcher matcher = Patterns.WEB_URL.matcher(text);
            if (matcher.find()){
                String url = matcher.group();
                et_url.setText(url+"/");
                Toast.makeText(MainActivity.this,"已自动提取抖音链接",Toast.LENGTH_SHORT).show();
            }


        }
    }

    //系统剪贴板-获取:
    public static String getCopy(Context context) {
        // 获取系统剪贴板
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        // 返回数据
        ClipData clipData = clipboard.getPrimaryClip();
        if (clipData != null && clipData.getItemCount() > 0) {
            // 从数据集中获取（粘贴）第一条文本数据
            return clipData.getItemAt(0).getText().toString();
        }
        return null;
    }


    public void sendOkHttpRequest(String address, FormBody.Builder builder , okhttp3.Callback callback) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(address).post(builder.build()).build();
        client.newCall(request).enqueue(callback);
    }
    public void sendOkHttpRequest(String address, okhttp3.Callback callback) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(address).build();
        client.newCall(request).enqueue(callback);
    }

    @Override
    public void onClick(View view) {

        switch (view.getId()){
            case R.id.btn_analysis:
                String url = BASE_API2+et_url.getText().toString().trim();
                if (TextUtils.isEmpty(url)){
                    Toast.makeText(MainActivity.this,"解析地址为空",Toast.LENGTH_SHORT).show();
                    return;
                }
//                FormBody.Builder builder = new FormBody.Builder();
//                builder.add("videoPath",url);
                Log.d("666","url == "+url);
                sendOkHttpRequest(url ,new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this,"解析失败，服务器断开",Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String responseText = response.body().string();
                        try {
                            JSONObject object = new JSONObject(responseText);
//                            if (object.getBoolean("isSuc")){
                                final String videoPath = object.getJSONObject("data").getString("videoUrl");
                                final String imagePath = object.getJSONObject("data").getString("videoCover");

                                Log.d("666","image == "+imagePath);

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        tv_url.setText(videoPath);
                                        tv_suc.setVisibility(View.VISIBLE);

                                        String image2 = imagePath.replaceAll("\r|\n","").replaceAll(" ","");

                                        Log.d("666","image2 == "+image2);
                                        Glide.with(MainActivity.this).load(image2).dontAnimate().into(iv_beijing);

                                        Toast.makeText(MainActivity.this,"解析成功",Toast.LENGTH_SHORT).show();
                                    }
                                });
//                            }else{
//                                runOnUiThread(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        tv_suc.setVisibility(View.INVISIBLE);
//                                        Toast.makeText(MainActivity.this,"解析失败",Toast.LENGTH_SHORT).show();
//                                    }
//                                });
//                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
                break;

            case R.id.btn_upload:
                String upload_url = tv_url.getText().toString().trim();
                if (TextUtils.isEmpty(upload_url)){
                    Toast.makeText(MainActivity.this,"没有获取到下载地址",Toast.LENGTH_SHORT).show();
                    return;
                }
                final DownLoadDialog downLoadDialog = new DownLoadDialog(MainActivity.this);
                downLoadDialog.show();
                DownUtils down = new DownUtils();
                down.download(MainActivity.this, upload_url);
                down.setOnDownloadListener(new DownUtils.OnDownloadListener() {
                    @Override
                    public void downStart() {
                    }

                    @Override
                    public void load(int progress) {
                        downLoadDialog.upload_progress.setProgress(progress);
                    }

                    @Override
                    public void downEnd() {
                        downLoadDialog.cancel();
                        Toast.makeText(MainActivity.this,"保存成功",Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void downFailure() {
                        downLoadDialog.cancel();
                        Toast.makeText(MainActivity.this,"保存失败",Toast.LENGTH_SHORT).show();
                    }
                });
                break;
        }
    }
}
