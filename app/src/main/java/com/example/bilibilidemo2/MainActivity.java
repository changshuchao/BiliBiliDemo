package com.example.bilibilidemo2;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.VideoView;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import master.flame.danmaku.controller.DrawHandler;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.IDanmakus;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.ui.widget.DanmakuView;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private boolean showDanmaku;

    private DanmakuView danmakuView;

    private DanmakuContext danmakuContext;

    /*
    * 需要创建一个弹幕的解析器才行，这里直接创建了一个全局的BaseDanmakuParser
    * */
    private BaseDanmakuParser parser = new BaseDanmakuParser() {
        @Override
        protected IDanmakus parse() {
            return new Danmakus();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        VideoView videoView = (VideoView)findViewById(R.id.video_view);

        String uri = "android.resource://" + getPackageName() + "/" + R.raw.example;
        videoView.setVideoURI(Uri.parse(uri));
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        videoView.start();

        /*
        * 先是获取到了DanmakuView控件的实例，
        * 然后调用了enableDanmakuDrawingCache()方法来提升绘制效率，
        * 又调用了setCallback()方法来设置回调函数。
        * */
        danmakuView = (DanmakuView)findViewById(R.id.danmuku_view);
        danmakuView.enableDanmakuDrawingCache(true);
        danmakuView.setCallback(new DrawHandler.Callback() {
            @Override
            public void prepared() {
                showDanmaku = true;
                danmakuView.start();
                generateSomeDanmaku();
            }

            @Override
            public void updateTimer(DanmakuTimer timer) {

            }

            @Override
            public void danmakuShown(BaseDanmaku danmaku) {

            }

            @Override
            public void drawingFinished() {

            }
        });
        /*
        * 调用DanmakuContext.create()方法创建了一个DanmakuContext的实例，DanmakuContext可以用于对弹幕的各种全局配置进行设定，
        * 如设置字体、设置最大显示行数等。这里我们并没有什么特殊的要求，因此一切都保持默认。
        * */
        danmakuContext = DanmakuContext.create();
        /*
        * 调用DanmakuView的prepare()方法来进行准备，准备完成后会自动调用刚才设置的回调函数中的prepared()方法，
        * 然后我们在这里再调用DanmakuView的start()方法，这样DanmakuView就可以开始正常工作了。
        * */
        danmakuView.prepare(parser, danmakuContext);


        final LinearLayout operation_layout = (LinearLayout)findViewById(R.id.operation_layout);
        Button send = (Button)findViewById(R.id.send);
        final EditText edit_text = (EditText)findViewById(R.id.edit_text);

        danmakuView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (operation_layout.getVisibility() == View.GONE) {
                    operation_layout.setVisibility(View.VISIBLE);
                } else {
                    operation_layout.setVisibility(View.GONE);
                }
            }
        });

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String content = edit_text.getText().toString();
                if (!TextUtils.isEmpty(content)) {
                    addDanmaku(content, true);
                    edit_text.setText("");
                }
            }
        });

        /*
        * 由于系统输入法弹出的时候会导致焦点丢失，从而退出沉浸式模式，
        * 因此这里还对系统全局的UI变化进行了监听，保证程序一直可以处于沉浸式模式。
        * */
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener (new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if (visibility == View.SYSTEM_UI_FLAG_VISIBLE) {
                    onWindowFocusChanged(true);
                }
            }
        });

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus && Build.VERSION.SDK_INT >=19){
            View view = getWindow().getDecorView();
            view.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    /**
     * 向弹幕View中添加一条弹幕
     * @param content
     *          弹幕的具体内容
     * @param  withBorder
     *          弹幕是否有边框
     */
    private void addDanmaku(String content, boolean withBorder) {
        /*
        * 观察addDanmaku()方法，这个方法就是用于向DanmakuView中添加一条弹幕消息的。
        * 其中首先调用了createDanmaku()方法来创建一个BaseDanmaku实例，TYPE_SCROLL_RL表示这是一条从右向左滚动的弹幕，
        * 然后我们就可以对弹幕的内容、字体大小、颜色、显示时间等各种细节进行配置了。
        * 注意addDanmaku()方法中有一个withBorder参数，这个参数用于指定弹幕消息是否带有边框，这样才好将自己发送的弹幕和别人发送的弹幕进行区分。
        * */
        BaseDanmaku danmaku = danmakuContext.mDanmakuFactory.createDanmaku(BaseDanmaku.TYPE_SCROLL_RL);
        danmaku.text = content;
        danmaku.padding = 5;
        danmaku.textSize = sp2px(20);
        danmaku.textColor = Color.WHITE;
        danmaku.setTime(danmakuView.getCurrentTime());
        if (withBorder) {
            danmaku.borderColor = Color.GREEN;
        }
        danmakuView.addDanmaku(danmaku);
    }

    /**
     * 随机生成一些弹幕内容以供测试
     */
    private void generateSomeDanmaku() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(showDanmaku) {
                    int time = new Random().nextInt(300);
                    String content = "" + time + time;
                    addDanmaku(content, false);
                    try {
                        Thread.sleep(time);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    /**
     * sp转px的方法。
     */
    public int sp2px(float spValue) {
        final float fontScale = getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (danmakuView != null && danmakuView.isPrepared()) {
            danmakuView.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (danmakuView != null && danmakuView.isPrepared() && danmakuView.isPaused()) {
            danmakuView.resume();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        showDanmaku = false;
        if (danmakuView != null) {
            danmakuView.release();
            danmakuView = null;
        }
    }
}
