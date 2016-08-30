package io.github.wzzju.powermonitor;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

public class NotifyActivity extends AppCompatActivity {

    private WindowManager wm;
    private WindowManager.LayoutParams wmParams;
    private TimerTask task;
    private Handler handler;
    private long firstTime = 0;
    private Timer timer = new Timer();
    //mView中的控件
    private View mView;
    private TextView gpuText;
    private TextView GPUTempText;
    private TextView temp5Text;
    private TextView temp6Text;
    private TextView temp7Text;
    private TextView temp8Text;
    private TextView a15V;
    private TextView a7V;
    private TextView gpuV;
    private TextView memV;
    private TextView a15A;
    private TextView a7A;
    private TextView gpuA;
    private TextView memA;
    private TextView a15W;
    private TextView a7W;
    private TextView gpuW;
    private TextView memW;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notify);
        Toast.makeText(NotifyActivity.this, "功耗监测中......", Toast.LENGTH_LONG).show();
        // create a WindowManager and set the layout using the prepared params
        wm = (WindowManager) getApplication().getSystemService(Context.WINDOW_SERVICE);
        wmParams = new WindowManager.LayoutParams();
        WindowManager.LayoutParams mParams = getParams(0, 0);

        LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mView = inflater.inflate(R.layout.activity_notify_float, null);
        // display the view
        wm.addView(mView, mParams);
        init();
        // prepare a timer and the corresponding handler to update the chart
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                //update event
                if (FloatView.getSensorStatus()) {
                    //res:="a15V,a15A,a15W,a7V,a7A,a7W,gpuV,gpuA,gpuW,memV,memA,memW\n"
                    String[] vawArray;
                    vawArray = FloatView.getSensorData();
                    if (vawArray.length == 12) {
                        a15V.setText(vawArray[0]);
                        a15A.setText(vawArray[1]);
                        a15W.setText(vawArray[2]);
                        a7V.setText(vawArray[3]);
                        a7A.setText(vawArray[4]);
                        a7W.setText(vawArray[5]);
                        gpuV.setText(vawArray[6]);
                        gpuA.setText(vawArray[7]);
                        gpuW.setText(vawArray[8]);
                        memV.setText(vawArray[9]);
                        memA.setText(vawArray[10]);
                        memW.setText(vawArray[11]);
                    }
                    String[] nonSensorData = FloatView.getNoSensorData();
                    if(nonSensorData.length == 6) {
                        gpuText.setText(nonSensorData[0]);
                        temp5Text.setText(nonSensorData[1]);
                        temp6Text.setText(nonSensorData[2]);
                        temp7Text.setText(nonSensorData[3]);
                        temp8Text.setText(nonSensorData[4]);
                        GPUTempText.setText(nonSensorData[5]);

                    }
                }
                super.handleMessage(msg);
            }
        };
        task = new TimerTask() {
            @Override
            public void run() {
                //timer setting
                Message message = new Message();
                message.what = 200;
                handler.sendMessage(message);
            }

        };

        //start the timer
        timer.schedule(task, Calendar.getInstance().getTime(), 50);
        mView.setOnTouchListener(new View.OnTouchListener() {
            int lastX, lastY;
            int paramX, paramY;

            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        lastX = (int) event.getRawX();
                        lastY = (int) event.getRawY();
                        paramX = wmParams.x;
                        paramY = wmParams.y;
                        long secondTime = System.currentTimeMillis();
                        if(secondTime -firstTime > 800){
                            Toast.makeText(getApplicationContext(),"再按一次关闭窗口!",Toast.LENGTH_SHORT).show();
                            firstTime = secondTime;//更新firstTime
                        }else{
                            timer.cancel();
                            wm.removeView(mView);
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) event.getRawX() - lastX;
                        int dy = (int) event.getRawY() - lastY;
                        wmParams.x = paramX + dx;
                        wmParams.y = paramY + dy;
                        // 更新悬浮窗位置
                        wm.updateViewLayout(mView, wmParams);
                        break;
                }
                return true;
            }
        });
        finish();
    }
    // window manager layout setting
    public WindowManager.LayoutParams getParams(int x, int y) {

        wmParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        wmParams.format = PixelFormat.TRANSLUCENT;// 设置图片格式，效果为背景透明
        wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        wmParams.flags = wmParams.flags | WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN;
        wmParams.flags = wmParams.flags | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
        wmParams.flags = wmParams.flags | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        wmParams.alpha = 1.0f;
        wmParams.gravity = Gravity.LEFT | Gravity.TOP;
        wmParams.x = x;
        wmParams.y = y;
        wmParams.width = 520;
        wmParams.height = 260;

        return wmParams;
    }
    private void init() {
        gpuText = (TextView) mView.findViewById(R.id.gpu);
        GPUTempText = (TextView) mView.findViewById(R.id.GPUTemp);
        temp5Text = (TextView) mView.findViewById(R.id.temp5);
        temp6Text = (TextView) mView.findViewById(R.id.temp6);
        temp7Text = (TextView) mView.findViewById(R.id.temp7);
        temp8Text = (TextView) mView.findViewById(R.id.temp8);
        a15V = (TextView) mView.findViewById(R.id.a15V);
        a15A = (TextView) mView.findViewById(R.id.a15A);
        a15W = (TextView) mView.findViewById(R.id.a15W);
        a7V = (TextView) mView.findViewById(R.id.a7V);
        a7A = (TextView) mView.findViewById(R.id.a7A);
        a7W = (TextView) mView.findViewById(R.id.a7W);
        gpuV = (TextView) mView.findViewById(R.id.gpuV);
        gpuA = (TextView) mView.findViewById(R.id.gpuA);
        gpuW = (TextView) mView.findViewById(R.id.gpuW);
        memV = (TextView) mView.findViewById(R.id.memV);
        memA = (TextView) mView.findViewById(R.id.memA);
        memW = (TextView) mView.findViewById(R.id.memW);
    }
}
