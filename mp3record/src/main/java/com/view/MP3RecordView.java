package com.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.czt.mp3recorder.MP3Recorder;
import com.zhuyong.mp3record.R;
import com.utils.FileUtils;
import com.utils.LogUtils;

import java.io.File;
import java.io.IOException;


/**
 * 录音控件封装
 * 作者：zhuyong on 2019/2/27 17:22
 * 邮箱：99305919@qq.com
 * 希望每天叫醒你的不是闹钟而是梦想
 */
public class MP3RecordView extends RelativeLayout implements View.OnTouchListener {

    private static final String TAG = "MP3RecordView";
    /**
     * 上下文
     */
    private Context mContext;
    /**
     * 触摸控制View
     */
    private View mRootView;
    /**
     * 录音类
     */
    private MP3Recorder mRecorder;
    /**
     * 录音文件地址
     */
    private String mFilePath = "";
    /**
     * 录音时间标记参数
     */
    private long mDuration;
    /**
     * 音量大小指示图片数组
     */
    protected Drawable[] mImagesVolume;
    /**
     * 录音结果回调接口
     */
    private OnRecordCompleteListener onRecordCompleteListener;


    public MP3RecordView(Context context) {
        this(context, null);
    }

    public MP3RecordView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @SuppressLint("InvalidWakeLockTag")
    public MP3RecordView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        initView();
    }

    /**
     * 正常录音显示根布局
     */
    private LinearLayout mLlayoutRecordingRoot;
    /**
     * 录音音量显示
     */
    private ImageView mImgRecordVolume;
    /**
     * 录音上滑布局
     */
    private ImageView mImgRecordCancel;
    /**
     * 录音时长太短布局
     */
    private ImageView mImgRecordShort;
    /**
     * 录音状态显示
     */
    private TextView mTvRecordHint;

    /**
     * 初始化布局
     */
    private void initView() {
        LayoutInflater.from(mContext).inflate(R.layout.view_recording_layout, this);

        mLlayoutRecordingRoot = findViewById(R.id.llayout_recording_root);
        mImgRecordVolume = findViewById(R.id.img_record_volume);
        mImgRecordCancel = findViewById(R.id.img_record_cancel);
        mImgRecordShort = findViewById(R.id.img_record_short);
        mTvRecordHint = findViewById(R.id.tv_record_hint);

        /**
         * 设置不显示
         */
        this.setVisibility(INVISIBLE);

        mImagesVolume = new Drawable[]{
                ContextCompat.getDrawable(mContext, R.drawable.icon_recording_1),
                ContextCompat.getDrawable(mContext, R.drawable.icon_recording_2),
                ContextCompat.getDrawable(mContext, R.drawable.icon_recording_3),
                ContextCompat.getDrawable(mContext, R.drawable.icon_recording_4),
                ContextCompat.getDrawable(mContext, R.drawable.icon_recording_5),
                ContextCompat.getDrawable(mContext, R.drawable.icon_recording_6),
                ContextCompat.getDrawable(mContext, R.drawable.icon_recording_7)};
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MP3Recorder.MESSAGE_RECORDING://获取录音音量
                    int mVolume = msg.arg1;//0-2000
                    int index = mVolume / 300;
                    mImgRecordVolume.setImageDrawable(mImagesVolume[index]);
                    break;
                case MP3Recorder.MESSAGE_ERROR_PERMISSION://没有麦克风权限
                    Toast.makeText(mContext, "没有麦克风权限", Toast.LENGTH_SHORT).show();
                    resolveError();
                    break;
                case MP3Recorder.MESSAGE_ERROR://录音异常
                    Toast.makeText(mContext, "录音异常,请检查权限是否打开", Toast.LENGTH_SHORT).show();
                    resolveError();
                    break;
                case MP3Recorder.MESSAGE_FINISH://录音完成
                    int duration = msg.arg1;
                    LogUtils.i(TAG, "录音文件地址:" + mFilePath + "   时长duration:" + duration / 1000 + "S");

                    if (onRecordCompleteListener != null) {
                        onRecordCompleteListener.onComplete(mFilePath, duration);
                    }
                    break;
            }
        }
    };

    /**
     * 设置触摸View
     *
     * @param view 触控View 一般为Button
     */
    public void setRootView(View view) {
        this.mRootView = view;
        if (mRootView != null) {
            this.mRootView.setOnTouchListener(this);
        }
    }

    private boolean mIsOnTouch = false;
    private boolean mIsRecording = false;

    /**
     * 点击时间间隔控制参数，防止快速点击造成录音异常
     */
    private long time;


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && System.currentTimeMillis() - time < 1000) {
            LogUtils.i(TAG, "时间间隔小于1秒，不执行以下代码");
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                v.setPressed(true);
                mIsOnTouch = true;
                showMoveUpToCancelHint();
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mIsOnTouch) {
                            LogUtils.i(TAG, "执行以下代码");
                            time = System.currentTimeMillis();
                            mIsRecording = true;
                            startRecording();
                        }
                    }
                }, 300);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (event.getY() < 0) {
                    showReleaseToCancelHint();
                } else {
                    showMoveUpToCancelHint();
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                v.setPressed(false);
                mIsOnTouch = false;
                if (mIsRecording) {
                    if (event.getY() < 0) {
                        resolveError();
                        LogUtils.i(TAG, "取消录音");
                    } else {
                        long length = resolveStopRecord();
                        mIsRecording = false;
                        if (length > 1000) {
                            this.setVisibility(INVISIBLE);
                            Message message = new Message();
                            message.what = MP3Recorder.MESSAGE_FINISH;
                            message.arg1 = (int) length;
                            mHandler.sendMessage(message);
                        } else {
                            showMoveRecordTooShort();
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    setVisibility(INVISIBLE);
                                }
                            }, 1000);
                        }
                    }
                }
                return true;
            default:
                v.setPressed(false);
                LogUtils.i(TAG, "执行到了其他的:" + event.getAction());
                resolveError();
                return false;
        }
    }

    /**
     * 当手指上滑，更新UI
     */
    public void showReleaseToCancelHint() {
        mLlayoutRecordingRoot.setVisibility(INVISIBLE);
        mImgRecordCancel.setVisibility(VISIBLE);
        mImgRecordShort.setVisibility(INVISIBLE);
        mTvRecordHint.setText("松开手指，取消发送");
        mTvRecordHint.setBackgroundResource(R.drawable.bg_red_shape);
    }

    /**
     * 正常录音期间，更新UI
     */
    public void showMoveUpToCancelHint() {
        mLlayoutRecordingRoot.setVisibility(VISIBLE);
        mImgRecordCancel.setVisibility(INVISIBLE);
        mImgRecordShort.setVisibility(INVISIBLE);
        mTvRecordHint.setText("手指上滑，取消发送");
        mTvRecordHint.setBackgroundDrawable(null);
    }

    /**
     * 录音时间太短，更新UI
     */
    public void showMoveRecordTooShort() {
        mLlayoutRecordingRoot.setVisibility(INVISIBLE);
        mImgRecordCancel.setVisibility(INVISIBLE);
        mImgRecordShort.setVisibility(VISIBLE);
        mTvRecordHint.setText("录音时间太短");
        mTvRecordHint.setBackgroundDrawable(null);
    }


    /**
     * 开始录音
     */
    private void startRecording() {
        mFilePath = FileUtils.getAppPath();
        File file = new File(mFilePath);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                Toast.makeText(mContext, "创建文件失败", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        mFilePath = FileUtils.getAppPath() + System.currentTimeMillis() + ".mp3";
        if (mRecorder == null) {
            mRecorder = new MP3Recorder(new File(mFilePath));
            mRecorder.setHandler(mHandler);
        }
        mRecorder.setmRecordFile(new File(mFilePath));

        try {
            mRecorder.start();
            mDuration = System.currentTimeMillis();
            this.setVisibility(VISIBLE);

        } catch (IOException e) {
            e.printStackTrace();
            LogUtils.i(TAG, "录音出现异常：" + e.toString());
            mHandler.sendEmptyMessage(MP3Recorder.MESSAGE_ERROR);
            return;
        } catch (Exception e) {
            e.printStackTrace();
            LogUtils.i(TAG, "录音出现异常：" + e.toString());
            mHandler.sendEmptyMessage(MP3Recorder.MESSAGE_ERROR);
            return;
        }
    }


    /**
     * 停止录音
     *
     * @return 返回录音时间间隔, 即录音时长
     */
    private long resolveStopRecord() {
        if (mRecorder != null && mRecorder.isRecording()) {
            mRecorder.setPause(false);
            mRecorder.stop();
        }
        return System.currentTimeMillis() - mDuration;
    }

    /**
     * 录音异常
     */
    private void resolveError() {
        this.setVisibility(View.INVISIBLE);
        FileUtils.deleteFile(mFilePath);
        mIsOnTouch = false;
        mFilePath = "";
        if (mRecorder != null && mRecorder.isRecording()) {
            mRecorder.stop();
        }
    }

    /**
     * 设置是否打印录音相关参数
     *
     * @param debug
     */
    public void setDebug(boolean debug) {
        LogUtils.IS_DEBUGING = debug;
    }

    /**
     * 录音结果回调
     */

    public void setOnRecordCompleteListener(OnRecordCompleteListener listener) {
        this.onRecordCompleteListener = listener;
    }

    public interface OnRecordCompleteListener {
        void onComplete(String filePath, int duration);
    }

}
