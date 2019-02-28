package com.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.AudioPlayer;
import com.utils.LogUtils;
import com.utils.ScreenUtils;
import com.zhuyong.mp3record.R;

import java.text.SimpleDateFormat;


/**
 * 录音播放页面
 * 作者：zhuyong on 2019/2/27 17:22
 * 邮箱：99305919@qq.com
 * 希望每天叫醒你的不是闹钟而是梦想
 */
public class AudioPlayActivity extends FragmentActivity {

    private final static String TAG = "AudioPlayActivity";

    private LinearLayout mLlayoutTipRoot;
    private ProgressBar mProgressAudio;
    private TextView mTvTime;
    private ProgressBar mProgressLoading;
    private AudioPlayer mAudioPlayer;
    private SimpleDateFormat formatter = new SimpleDateFormat("mm:ss");
    /**
     * 总时长
     */
    private int mTotalDuration;

    public static final String _AUDIO_PATH = "audio_path";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_play);
        initView();
    }

    private void initView() {

        mLlayoutTipRoot = findViewById(R.id.llayout_tip_root);
        mProgressAudio = findViewById(R.id.progress_audio);
        mTvTime = findViewById(R.id.tv_time);
        mProgressLoading = findViewById(R.id.progress_loading);

        mLlayoutTipRoot.setLayoutParams(new LinearLayout.LayoutParams(ScreenUtils.getScreenWidth(this)
                - ScreenUtils.dip2px(this, 40), ViewGroup.LayoutParams.WRAP_CONTENT));

        mAudioPlayer = new AudioPlayer(AudioPlayActivity.this, new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case AudioPlayer.HANDLER_CUR_TIME://更新的时间
                        int curPosition = (int) msg.obj;
                        mTvTime.setText(toTime(curPosition) + " / " + toTime(mTotalDuration));
                        LogUtils.i(TAG, "curPosition:" + curPosition);
                        mProgressAudio.setProgress(curPosition);
                        break;
                    case AudioPlayer.HANDLER_COMPLETE://播放结束
                        finish();
                        break;
                    case AudioPlayer.HANDLER_PREPARED://播放开始
                        mProgressLoading.setVisibility(View.GONE);
                        mTotalDuration = (int) msg.obj;
                        mTvTime.setText("00:00 / " + toTime(mTotalDuration));
                        mProgressAudio.setMax(mTotalDuration);
                        break;
                    case AudioPlayer.HANDLER_ERROR://播放错误
                        Toast.makeText(AudioPlayActivity.this, "播放错误", Toast.LENGTH_SHORT).show();
                        finish();
                        break;
                }

            }
        });

        mAudioPlayer.playUrl(getIntent().getStringExtra(_AUDIO_PATH));
    }

    /**
     * 时间格式化
     *
     * @param time
     * @return
     */
    private String toTime(long time) {
        return formatter.format(time);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mAudioPlayer != null) {
            mAudioPlayer.pause();
            mAudioPlayer.stop();
        }
    }

}
