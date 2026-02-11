package com.anrola.onmyway.Utils;

import static java.lang.Math.max;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.util.Log;

public class AudioPlayer {
    private static final String TAG = "AudioPlayer";
    private static MediaPlayer mediaPlayer;
    private static AudioTrack audioTrack;

    /**
     * 播放网络URL音频
     *
     * @param audioUrl 音频地址
     */
    public static void playAudioByUrl(String audioUrl) {
        // 先释放之前的MediaPlayer实例，避免内存泄漏
        releaseMediaPlayer();

        try {
            // 初始化MediaPlayer
            mediaPlayer = new MediaPlayer();
            // 设置音频类型（媒体音量控制）
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());

            // 设置数据源（网络URL）
            mediaPlayer.setDataSource(audioUrl);

            // 异步准备（避免主线程阻塞）
            mediaPlayer.prepareAsync();

            // 准备完成后自动播放
            mediaPlayer.setOnPreparedListener(mp -> {
                Log.d(TAG, "音频准备完成，开始播放");
                mp.start();
                int duration = mp.getDuration();
                Log.d(TAG, "音频时长: " + duration / 1000 + "秒");
            });

            // 播放完成监听
            mediaPlayer.setOnCompletionListener(mp -> {
                Log.d(TAG, "音频播放完成");
                releaseMediaPlayer(); // 播放完成后释放资源
            });

            // 错误监听（关键：处理网络异常、格式不支持等）
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "播放错误: what=" + what + ", extra=" + extra);
                releaseMediaPlayer();
                return true; // 表示已处理错误
            });

            // 7. 缓冲监听（可选：显示加载进度）
            mediaPlayer.setOnBufferingUpdateListener((mp, percent) -> {
                Log.d(TAG, "缓冲进度: " + percent + "%");
            });

        } catch (Exception e) {
            Log.e(TAG, "播放URL音频异常: " + e.getMessage());
            releaseMediaPlayer();
        }
    }

    /**
     * 暂停播放
     */
    private void pauseAudio() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            Log.d(TAG, "音频已暂停");
        }
    }

    /**
     * 继续播放
     */
    private void resumeAudio() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            Log.d(TAG, "音频继续播放");
        }
    }

    /**
     * 停止播放并释放资源（关键）
     */
    public static void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
            Log.d(TAG, "MediaPlayer资源已释放");
        }
    }

    public static void playAudio(byte[] audioBytes) {

        if (audioTrack == null){
            AudioFormat format = new AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(24000)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build();
            int minBufferSize = AudioTrack.getMinBufferSize(
                    format.getSampleRate(),
                    AudioFormat.CHANNEL_OUT_MONO, // 单声道，根据实际返回的音频调整
                    format.getEncoding()
            );
            audioTrack = new AudioTrack.Builder()
                    .setAudioFormat(format)
                    .setBufferSizeInBytes(max(minBufferSize, 10 * 1024))
                    .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_POWER_SAVING)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build();
        }

        if (audioTrack.getPlayState() != AudioTrack.PLAYSTATE_PLAYING){
            audioTrack.play();
        }
        audioTrack.write(audioBytes, 0, audioBytes.length);
    }
}
