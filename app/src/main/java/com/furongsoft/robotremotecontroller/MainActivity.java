package com.furongsoft.robotremotecontroller;

import android.Manifest;
import android.annotation.SuppressLint;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.example.captain_miao.grantap.CheckPermission;
import com.example.captain_miao.grantap.listeners.PermissionListener;
import com.furongsoft.robotremotecontroller.databinding.ActivityMainBinding;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.speech.util.JsonParser;

import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity {
    private static final String APP_ID = "5a54bf87";
    private ActivityMainBinding binding;
    private MyHandler handler;
    private boolean isRecognizeTextEnabled = false;
    private StringBuffer recognizedText = new StringBuffer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        CheckPermission
                .from(this)
                .setPermissions(Manifest.permission.RECORD_AUDIO, Manifest.permission.INTERNET)
                .setRationaleConfirmText("应用需要使用录音权限")
                .setDeniedMsg("拒绝授权应用将无法正常工作")
                .setPermissionListener(new PermissionListener() {
                    @Override
                    public void permissionGranted() {
                        Toast.makeText(MainActivity.this, "已获取录音权限", Toast.LENGTH_SHORT).show();
                        initialize();
                    }

                    @Override
                    public void permissionDenied() {
                        MainActivity.this.finish();
                    }
                })
                .check();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initialize() {
        SpeechUtility.createUtility(this, SpeechConstant.APPID + "=" + APP_ID);
        SpeechRecognizer sr = SpeechRecognizer.createRecognizer(this, (int var1) -> {
        });
        sr.setParameter(SpeechConstant.DOMAIN, "iat");
        sr.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
        sr.setParameter(SpeechConstant.ACCENT, "mandarin");

        handler = new MyHandler(new WeakReference<>(this), sr);

        binding.btnMainActivityRecognizing.setOnTouchListener((View view, MotionEvent motionEvent) -> {
            view.performClick();

            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isRecognizeTextEnabled = true;
                    recognizedText = new StringBuffer();
                    handler.sendEmptyMessage(0);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    isRecognizeTextEnabled = false;
                    handler.removeMessages(0);
                    break;
                default:
                    break;
            }

            return false;
        });
    }

    private static class MyHandler extends Handler {
        private final WeakReference<MainActivity> activity;
        private final SpeechRecognizer sr;
        private final RecognizerListener listener;

        MyHandler(WeakReference<MainActivity> activity, SpeechRecognizer sr) {
            super();

            this.activity = activity;
            this.sr = sr;
            listener = new RecognizerListener() {
                @Override
                public void onVolumeChanged(int i, byte[] bytes) {
                }

                @Override
                public void onBeginOfSpeech() {
                }

                @Override
                public void onEndOfSpeech() {
                }

                @Override
                public void onResult(RecognizerResult recognizerResult, boolean b) {
                    String text = JsonParser.parseIatResult(recognizerResult.getResultString());
                    if (activity.get() != null) {
                        activity.get().recognizedText.append(text);
                        if (activity.get().isRecognizeTextEnabled) {
                            MyHandler.this.sendEmptyMessage(0);
                        } else {
                            Toast.makeText(activity.get(), activity.get().recognizedText.toString(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                @Override
                public void onError(SpeechError speechError) {
                    MyHandler.this.sendEmptyMessage(0);
                }

                @Override
                public void onEvent(int i, int i1, int i2, Bundle bundle) {
                }
            };
        }

        @Override
        public void handleMessage(Message msg) {
            if ((activity.get() != null) && activity.get().isRecognizeTextEnabled) {
                sr.startListening(listener);
            }

            super.handleMessage(msg);
        }
    }
}
