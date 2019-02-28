package com.zhuyong.audiorecord;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.activity.AudioPlayActivity;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.view.MP3RecordView;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    /**
     * 录音控件
     */
    private MP3RecordView mViewRecord;
    /**
     * 录音文件路径
     */
    private String mAudioPath = "";

    private TextView mTvText;
    private Button mBtnRecord;

    private final RxPermissions rxPermissions = new RxPermissions(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        mTvText = findViewById(R.id.tv_text);
        mViewRecord = findViewById(R.id.view_record);
        mBtnRecord = findViewById(R.id.btn_rwcord);

        /**
         * 设置可触控View
         */
        mViewRecord.setRootView(mBtnRecord);

        /**
         * 设置回调
         */
        mViewRecord.setOnRecordCompleteListener((filePath, duration) -> {
            mAudioPath = filePath;
            String str = "文件地址：" + filePath + "\n\n录音时长:" + duration / 1000 + "秒";
            Log.i("MainActivity", str);
            mTvText.setText(str);
        });


        findViewById(R.id.btn_play).setOnClickListener(v -> {
            if (TextUtils.isEmpty(mAudioPath) || !new File(mAudioPath).exists()) {
                Toast.makeText(MainActivity.this, "文件不存在", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(MainActivity.this, AudioPlayActivity.class);
            intent.putExtra(AudioPlayActivity._AUDIO_PATH, mAudioPath);
            startActivity(intent);

        });
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        requestAudioPermission();
    }


    /**
     * 检测权限
     */
    private void requestAudioPermission() {
        rxPermissions.request(Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe(granted -> {
                    if (!granted) {
                        Toast.makeText(MainActivity.this, "不同意就退出", Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
    }

}
