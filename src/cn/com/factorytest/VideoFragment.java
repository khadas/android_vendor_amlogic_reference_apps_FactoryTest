package cn.com.factorytest;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.VideoView;
import android.os.Build;
import android.os.SystemClock;
import java.io.IOException;

import java.util.Date;

public class VideoFragment extends android.app.Fragment {
    private static final String TAG = Tools.TAG;
    private static final int MSG_UPDATE_TIME = 0;
    private static final int RETRY_DELAY = 3000;

    private VideoView mVideoView;
    private TextView mTestTime;
    private String mVideoUri;
    private long m_StartTime = SystemClock.elapsedRealtime();
    static int ageing_test_step = 0;
    static int led_status = 0;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_UPDATE_TIME) {
                updateTestTime();
                sendEmptyMessageDelayed(MSG_UPDATE_TIME, 1000);
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_video, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mVideoView = view.findViewById(R.id.VideoView);
        mTestTime = view.findViewById(R.id.TestTime);

        initVideoPlayer();
    }

    private void initVideoPlayer() {
        mVideoUri = "android.resource://" + getActivity().getPackageName() + "/" + R.raw.testvideo;

        mVideoView.setOnPreparedListener(mp -> {
            Log.i(TAG, "Video prepared");
            mp.setLooping(true);
            mp.start();
        });

        mVideoView.setOnErrorListener((mp, what, extra) -> {
            Log.e(TAG, "Playback error: " + what + "/" + extra);
            retryPlayback();
            return true;
        });

        startPlayback();
    }

    private void startPlayback() {
        try {
            mVideoView.setVideoURI(Uri.parse(mVideoUri));
            mVideoView.start();
        } catch (Exception e) {
            Log.e(TAG, "Video init failed", e);
            retryPlayback();
        }
    }

    private void retryPlayback() {
        mHandler.postDelayed(() -> {
            Log.w(TAG, "Retrying video playback");
            startPlayback();
        }, RETRY_DELAY);
    }

    public void resumePlayback() {
        if (!mVideoView.isPlaying()) {
            mVideoView.resume();
        }
    }

    private void updateTestTime() {
	    if (1 == MainActivity.ageing_flag) {
	        if (2 == ageing_test_step && 2 != led_status) {
            	if (MainActivity.test_board.equals("VIM4") || MainActivity.test_board.equals("VIM1S")) {
	                try {
	                    Tools.execCommand(new String[]{"sh", "-c", "echo 0 0 > /sys/class/leds/state_led/breath"});
	                } catch (IOException e) {
	                    e.printStackTrace();
	                }
	            } else {
	                Tools.writeFile(Tools.White_Led, "heartbeat");//default-on off heartbeat
	            }
	            led_status = 2;
	        } else {
	            if (1 == led_status) {
                	if (MainActivity.test_board.equals("VIM4") || MainActivity.test_board.equals("VIM1S")) {
	                    try {
	                        Tools.execCommand(new String[]{"sh", "-c", "echo 0 0 > /sys/class/leds/state_led/state_brightness"});
	                    } catch (IOException e) {
	                        e.printStackTrace();
	                    }
	                } else {
	                    Tools.writeFile(Tools.White_Led, "off");//default-on off heartbeat
	                }
	                led_status = 0;
	            } else if (0 == led_status) {
                	if (MainActivity.test_board.equals("VIM4") || MainActivity.test_board.equals("VIM1S")) {
	                    try {
	                        Tools.execCommand(new String[]{"sh", "-c", "echo 0 255 > /sys/class/leds/state_led/state_brightness"});
	                    } catch (IOException e) {
	                        e.printStackTrace();
	                    }
	                } else {
	                    Tools.writeFile(Tools.White_Led, "default-on");//default-on off heartbeat
	                }
	                led_status = 1;
	            }
	        }
	    }
	    mTestTime.setText(getTime());
    }

    @Override
    public void onResume() {
        super.onResume();
        mHandler.sendEmptyMessage(MSG_UPDATE_TIME);
    }

    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeMessages(MSG_UPDATE_TIME);
        mVideoView.pause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mVideoView.stopPlayback();
    }

    private String getTime() {
        Date newDate = new Date();

        long between = (SystemClock.elapsedRealtime() - m_StartTime) / 1000;
        long day1 = between / (24 * 3600);
        long hour1 = between % (24 * 3600) / 3600;
        long hour_ageing = between / 3600;
        long minute1 = between % 3600 / 60;
        long second1 = between % 60;

        if (1 == MainActivity.ageing_flag) {
            if ((hour_ageing >= MainActivity.ageing_time) && 1 == ageing_test_step) {
                Tools.writeFile(Tools.ageing_status, "1");
                ageing_test_step = 2;
            } else if (1 == MainActivity.ageing_time && 1 == ageing_test_step && minute1 >= MainActivity.ageing_time) {
                Tools.writeFile(Tools.ageing_status, "1");
                ageing_test_step = 2;
            }else if (0 == ageing_test_step) {
                Tools.writeFile(Tools.ageing_status, "0");
                ageing_test_step = 1;
            }
        }
        if (between > (60 * 60 * MainActivity.ageing_time)) {
            return getResources().getString(R.string.long_test_finish);
        } else {
            return String.format("%d : %02d : %02d : %02d", day1, Math.abs(hour1), Math.abs(minute1), Math.abs(second1));
        }
    }
}