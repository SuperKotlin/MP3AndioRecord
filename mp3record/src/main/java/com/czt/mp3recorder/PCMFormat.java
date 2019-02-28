package com.czt.mp3recorder;

import android.media.AudioFormat;


/**
 * 作者：zhuyong on 2019/2/27 17:22
 * 邮箱：99305919@qq.com
 * 希望每天叫醒你的不是闹钟而是梦想
 */
public enum PCMFormat {
	PCM_8BIT (1, AudioFormat.ENCODING_PCM_8BIT),
	PCM_16BIT (2, AudioFormat.ENCODING_PCM_16BIT);
	
	private int bytesPerFrame;
	private int audioFormat;
	
	PCMFormat(int bytesPerFrame, int audioFormat) {
		this.bytesPerFrame = bytesPerFrame;
		this.audioFormat = audioFormat;
	}
	public int getBytesPerFrame() {
		return bytesPerFrame;
	}
	public int getAudioFormat() {
		return audioFormat;
	}
}
