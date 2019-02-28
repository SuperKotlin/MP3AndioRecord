package com.czt.mp3recorder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;

import com.BaseRecorder;
import com.czt.mp3recorder.util.LameUtil;

import java.io.File;
import java.io.IOException;

/**
 * 作者：zhuyong on 2019/2/27 17:22
 * 邮箱：99305919@qq.com
 * 希望每天叫醒你的不是闹钟而是梦想
 */
public class MP3Recorder extends BaseRecorder {
    //=======================AudioRecord Default Settings=======================
    private static final int DEFAULT_AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    /**
     * 以下三项为默认配置参数。Google Android文档明确表明只有以下3个参数是可以在所有设备上保证支持的。
     */
    private static final int DEFAULT_SAMPLING_RATE = 44100;//模拟器仅支持从麦克风输入8kHz采样率
    private static final int DEFAULT_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    /**
     * 下面是对此的封装
     * private static final int DEFAULT_AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
     */
    private static final PCMFormat DEFAULT_AUDIO_FORMAT = PCMFormat.PCM_16BIT;

    //======================Lame Default Settings=====================
    private static final int DEFAULT_LAME_MP3_QUALITY = 7;
    /**
     * 与DEFAULT_CHANNEL_CONFIG相关，因为是mono单声，所以是1
     */
    private static final int DEFAULT_LAME_IN_CHANNEL = 1;
    /**
     * Encoded bit rate. MP3 file will be encoded with bit rate 32kbps
     */
    private static final int DEFAULT_LAME_MP3_BIT_RATE = 32;

    //==================================================================

    /**
     * 自定义 每160帧作为一个周期，通知一下需要进行编码
     */
    private static final int FRAME_COUNT = 160;

    public static final int MESSAGE_RECORDING = 0x11;
    public static final int MESSAGE_ERROR = 0x12;
    public static final int MESSAGE_ERROR_PERMISSION = 0x13;
    public static final int MESSAGE_FINISH = 0x14;

    private AudioRecord mAudioRecord = null;
    private DataEncodeThread mEncodeThread;
    private File mRecordFile;
    private Handler mHandler;

    private short[] mPCMBuffer;
    private boolean mIsRecording = false;
    private boolean mSendError;
    private boolean mPause;
    //缓冲数量
    private int mBufferSize;

    /**
     * Default constructor. Setup recorder with default sampling rate 1 channel,
     * 16 bits pcm
     *
     * @param recordFile target file
     */
    public MP3Recorder(File recordFile) {
        mRecordFile = recordFile;
    }

    public File getmRecordFile() {
        return mRecordFile;
    }

    public void setmRecordFile(File mRecordFile) {
        this.mRecordFile = mRecordFile;
    }

    /**
     * Start recording. Create an encoding thread. Start record from this
     * thread.
     *
     * @throws IOException initAudioRecorder throws
     */
    public void start() throws IOException {
        if (mIsRecording) {
            return;
        }
        mIsRecording = true; // 提早，防止init或startRecording被多次调用
        initAudioRecorder();
        try {
            mAudioRecord.startRecording();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        new Thread() {
            boolean isError = false;

            @Override
            public void run() {
                //设置线程权限
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
                while (mIsRecording) {
                    int readSize = mAudioRecord.read(mPCMBuffer, 0, mBufferSize);

                    if (readSize == AudioRecord.ERROR_INVALID_OPERATION ||
                            readSize == AudioRecord.ERROR_BAD_VALUE) {
                        if (mHandler != null && !mSendError) {
                            mSendError = true;
                            mHandler.sendEmptyMessage(MESSAGE_ERROR_PERMISSION);
                            mIsRecording = false;
                            isError = true;
                        }
                    } else {

                        if (readSize > 0) {
                            if (mPause) {
                                continue;
                            }
                            mEncodeThread.addTask(mPCMBuffer, readSize);
                            calculateRealVolume(mPCMBuffer, readSize);
                            Message message = new Message();
                            message.what = MESSAGE_RECORDING;
                            message.arg1 = getVolume();
                            mHandler.sendMessage(message);
                        } else {
                            if (mHandler != null && !mSendError) {
                                mSendError = true;
                                mHandler.sendEmptyMessage(MESSAGE_ERROR_PERMISSION);
                                mIsRecording = false;
                                isError = true;
                            }
                        }
                    }
                }
                try {
                    // release and finalize audioRecord
                    mAudioRecord.stop();
                    mAudioRecord.release();
                    mAudioRecord = null;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                // stop the encoding thread and try to wait
                // until the thread finishes its job
                if (isError) {
                    mEncodeThread.sendErrorMessage();
                } else {
                    mEncodeThread.sendStopMessage();
                }
            }

        }.start();
    }

    /**
     * 获取真实的音量。 [算法来自三星]
     *
     * @return 真实音量
     */
    @Override
    public int getRealVolume() {
        return mVolume;
    }

    /**
     * 获取相对音量。 超过最大值时取最大值。
     *
     * @return 音量
     */
    public int getVolume() {
        if (mVolume >= MAX_VOLUME) {
            return MAX_VOLUME;
        }
        return mVolume;
    }

    private static final int MAX_VOLUME = 2000;

    /**
     * 根据资料假定的最大值。 实测时有时超过此值。
     *
     * @return 最大音量值。
     */
    public int getMaxVolume() {
        return MAX_VOLUME;
    }

    public void stop() {
        mPause = false;
        mIsRecording = false;
    }

    public boolean isRecording() {
        return mIsRecording;
    }

    /**
     * Initialize audio recorder
     */
    private void initAudioRecorder() throws IOException {
        mBufferSize = AudioRecord.getMinBufferSize(DEFAULT_SAMPLING_RATE,
                DEFAULT_CHANNEL_CONFIG, DEFAULT_AUDIO_FORMAT.getAudioFormat());

        int bytesPerFrame = DEFAULT_AUDIO_FORMAT.getBytesPerFrame();
        /* Get number of samples. Calculate the buffer size
         * (round up to the factor of given frame size)
         * 使能被整除，方便下面的周期性通知
         * */
        int frameSize = mBufferSize / bytesPerFrame;
        if (frameSize % FRAME_COUNT != 0) {
            frameSize += (FRAME_COUNT - frameSize % FRAME_COUNT);
            mBufferSize = frameSize * bytesPerFrame;
        }

        /* Setup audio recorder */
        mAudioRecord = new AudioRecord(DEFAULT_AUDIO_SOURCE,
                DEFAULT_SAMPLING_RATE, DEFAULT_CHANNEL_CONFIG, DEFAULT_AUDIO_FORMAT.getAudioFormat(),
                mBufferSize);

        mPCMBuffer = new short[mBufferSize];
        /*
         * Initialize lame buffer
         * mp3 sampling rate is the same as the recorded pcm sampling rate
         * The bit rate is 32kbps
         *
         */
        LameUtil.init(DEFAULT_SAMPLING_RATE, DEFAULT_LAME_IN_CHANNEL, DEFAULT_SAMPLING_RATE, DEFAULT_LAME_MP3_BIT_RATE, DEFAULT_LAME_MP3_QUALITY);
        // Create and run thread used to encode data
        // The thread will
        mEncodeThread = new DataEncodeThread(mRecordFile, mBufferSize);
        mEncodeThread.start();
        mAudioRecord.setRecordPositionUpdateListener(mEncodeThread, mEncodeThread.getHandler());
        mAudioRecord.setPositionNotificationPeriod(FRAME_COUNT);
    }

    public boolean isPause() {
        return mPause;
    }

    /**
     * 是否暂停
     */
    public void setPause(boolean pause) {
        this.mPause = pause;
    }

    /**
     * 设置回调
     */
    public void setHandler(Handler handler) {
        this.mHandler = handler;
    }


    public static void deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            if (file.isFile()) {
                file.delete();
            } else {
                String[] filePaths = file.list();
                for (String path : filePaths) {
                    deleteFile(filePath + File.separator + path);
                }
                file.delete();
            }
        }
    }

}