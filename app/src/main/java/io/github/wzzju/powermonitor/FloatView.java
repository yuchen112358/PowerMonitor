package io.github.wzzju.powermonitor;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Toast;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

//using the achartengine library to plot the chart line

/**
 * Created by yuchen on 16-7-3.
 */
/*To guarantee that the view window isn't covered by other applications,
 *  the view window is developed as a service, and the chart line is draw by achartengine.
*/

/*Plotting a line chart using the achartengine, step as follow：
 *
 *
 * point data    -->   XYseries(TimeSeries)   ---MULTIPLE-->  XYMultipleSeriesDataset
 *																		             \
 * 																				   	  \
 * 																		      	       ----> PLOT
 *      color                                                               		  /
 *     		  \															   			 /
 *     		   --->   XYSeriesRender       ---MULTIPLE--->  XYMultipleSeriesRender
 *            /
 * point style
 *
*/

public class FloatView extends Service {
    /*
     * A WindowManager manages all the views in a Service.
     * Here we use a WindowManager to manage a view which display our data.
     * To dynamic display the data, we use a timer to update the display data regularly.
     */
    private WindowManager wm;
    private LayoutParams wmParams;
    private GraphicalView chart;
    private TimerTask task;
    private long addX;
    private double addA15W = -1.0;
    private double addA7W = -1.0;
    private double addGpuW = -1.0;
    private double addMemW = -1.0;
    private Timer timer = new Timer();
    private Handler handler;
    private static final String TAG = "FloatView";

    private TimeSeries a15W;
    private TimeSeries a7W;
    private TimeSeries gpuW;
    private TimeSeries memW;
    private XYMultipleSeriesDataset mdataset = new XYMultipleSeriesDataset();

    private static boolean SENSOR_OPEN = false;
    private static String[] nonSensorData = new String[6];
    private static String[] sensorData;

    private long firstTime = 0;

    //This array caches the time index
    Date[] xcache = new Date[60];
    //This array caches the display data
    double[] ycache = new double[60];

    public static boolean getSensorStatus() {
        return SENSOR_OPEN;
    }

    public static String[] getSensorData() {
        return sensorData;
    }

    public static String[] getNoSensorData() {
        return nonSensorData;
    }



    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        NativeLib.Init();
        if (NativeLib.OpenINA231() != 0) {
            SENSOR_OPEN = false;
            Toast.makeText(FloatView.this, "OpenINA231 error!", Toast.LENGTH_LONG).show();
        } else {
            SENSOR_OPEN = true;
            Toast.makeText(FloatView.this, "OpenINA231 sucessfully!", Toast.LENGTH_LONG).show();
        }
        Intent notificationIntent = new Intent(this, NotifyActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Power Show")
                .setContentText("功耗监测中......")
                .setContentIntent(pendingIntent)
                .build();// getNotification()
        startForeground(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // create a line chart (a view) using the prepared dataset and renderer
        chart = ChartFactory.getTimeChartView(this, getDateDrawDataset(), getDrawRenderer("Volt"), "hh:mm:ss");


        // create a WindowManager and set the layout using the prepared params
        wm = (WindowManager) getApplication().getSystemService(Context.WINDOW_SERVICE);
        wmParams = new LayoutParams();
        LayoutParams mParams = getParams(0, 0);

        // display the view
        wm.addView(chart, mParams);

        // prepare a timer and the corresponding handler to update the chart
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                //update the chart
                updateChart();
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
        chart.setOnTouchListener(new View.OnTouchListener() {
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
                        if (secondTime - firstTime > 800) {
                            Toast.makeText(FloatView.this, "再按一次关闭数据更新服务!", Toast.LENGTH_SHORT).show();
                            firstTime = secondTime;//更新firstTime
                        } else {
//                            wm.removeView(chart);//仅仅关闭图线更新
                            stopSelf();//关闭图线更新及数据更新
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) event.getRawX() - lastX;
                        int dy = (int) event.getRawY() - lastY;
                        wmParams.x = paramX + dx;
                        wmParams.y = paramY + dy;
                        // 更新悬浮窗位置
                        wm.updateViewLayout(chart, wmParams);
                        break;
                }
                return true;
            }
        });
        return super.onStartCommand(intent, flags, startId);
    }

    // window manager layout setting
    public LayoutParams getParams(int x, int y) {

        wmParams.type = LayoutParams.TYPE_SYSTEM_ALERT;
        wmParams.format = PixelFormat.RGBA_8888;// 设置图片格式，效果为背景透明
        wmParams.flags = LayoutParams.FLAG_NOT_FOCUSABLE;
        wmParams.flags = wmParams.flags | LayoutParams.FLAG_FORCE_NOT_FULLSCREEN;
        wmParams.flags = wmParams.flags | LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
        wmParams.flags = wmParams.flags | LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        wmParams.alpha = 1.0f;
        wmParams.gravity = Gravity.LEFT | Gravity.TOP;
        wmParams.x = x;
        wmParams.y = y;
        wmParams.width = 360;
        wmParams.height = 400;

        return wmParams;
    }

    // this function update the line chart
    private void updateChart() {
        if (SENSOR_OPEN) {
//            nonSensorData[0] = NativeLib.GetGPUCurFreq();
//            nonSensorData[1] = NativeLib.GetCPUTemp(0);
//            nonSensorData[2] = NativeLib.GetCPUTemp(1);
//            nonSensorData[3] = NativeLib.GetCPUTemp(2);
//            nonSensorData[4] = NativeLib.GetCPUTemp(3);
//            nonSensorData[5] = NativeLib.GetCPUTemp(4);//GPU温度
//            NativeLib.GetAllCPUCurLoad();//可以导出CPU的负载
//            for(int i = 0; i < 8;i++){
//              NativeLib.GetCPUCurLoad(i);//可以分别导出每个CPU的负载
//            }
            //res:="a15V,a15A,a15W,a7V,a7A,a7W,gpuV,gpuA,gpuW,memV,memA,memW\n"
            String vaw = NativeLib.GetINA231();
            String[] vawArray;
            vawArray = vaw.split(",");
            sensorData = vawArray;//这样可以从外部访问这些数据
            if (vawArray.length == 12) {
                addA15W = Double.parseDouble(vawArray[2]);
                addA7W = Double.parseDouble(vawArray[5]);
                addGpuW = Double.parseDouble(vawArray[8]);
                addMemW = Double.parseDouble(vawArray[11]);
            }
        }
        int length = a15W.getItemCount();
        if (length >= 60) length = 60;
        // determine the data to show in the view according to the result of reading the proc file
        addX = new Date().getTime();
        // prepare the new series
        for (int j = 0; j < length; j++) {
            xcache[j] = new Date((long) a15W.getX(j));
            ycache[j] = a15W.getY(j);
        }
        a15W.clear();
        a15W.add(new Date(addX), addA15W);
        for (int j = 0; j < length; j++) {
            a15W.add(xcache[j], ycache[j]);
        }
        // prepare the new series
        for (int j = 0; j < length; j++) {
            xcache[j] = new Date((long) a7W.getX(j));
            ycache[j] = a7W.getY(j);
        }
        a7W.clear();
        a7W.add(new Date(addX), addA7W);
        for (int j = 0; j < length; j++) {
            a7W.add(xcache[j], ycache[j]);
        }
        // prepare the new series
        for (int j = 0; j < length; j++) {
            xcache[j] = new Date((long) gpuW.getX(j));
            ycache[j] = gpuW.getY(j);
        }
        gpuW.clear();
        gpuW.add(new Date(addX), addGpuW);
        for (int j = 0; j < length; j++) {
            gpuW.add(xcache[j], ycache[j]);
        }
        // prepare the new series
        for (int j = 0; j < length; j++) {
            xcache[j] = new Date((long) memW.getX(j));
            ycache[j] = memW.getY(j);
        }
        memW.clear();
        memW.add(new Date(addX), addMemW);
        for (int j = 0; j < length; j++) {
            memW.add(xcache[j], ycache[j]);
        }
        // update the series
        mdataset.removeSeries(a15W);
        mdataset.addSeries(a15W);
        mdataset.removeSeries(a7W);
        mdataset.addSeries(a7W);
        mdataset.removeSeries(gpuW);
        mdataset.addSeries(gpuW);
        mdataset.removeSeries(memW);
        mdataset.addSeries(memW);
        chart.invalidate();
    }

    // this function set the renderer style
    private XYMultipleSeriesRenderer getDrawRenderer(String color) {
        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        renderer.setChartTitle("Power / Watt"); // line title
        renderer.setChartTitleTextSize(20);
        renderer.setXTitle("TIME");    // title of X axis
        renderer.setAxisTitleTextSize(16);
        renderer.setAxesColor(Color.GRAY);
        renderer.setLabelsTextSize(15);    //label size
        renderer.setLabelsColor(Color.GRAY);
        renderer.setLegendTextSize(15);    //legend text size
        renderer.setShowLegend(true);
        renderer.setXLabelsColor(Color.GRAY);
        renderer.setYLabelsColor(0, Color.GRAY);
        renderer.setYLabelsAlign(Paint.Align.LEFT);
        renderer.setMargins(new int[]{20, 30, 40, 0});
        renderer.setMarginsColor(Color.GRAY);
        renderer.setPanEnabled(false, false);
        renderer.setShowGrid(true);
        renderer.setYAxisMax(4);
        renderer.setYAxisMin(0);
        renderer.setInScroll(true);  //调整大小
        renderer.setMarginsColor(Color.argb(0, 0xff, 0, 0));
        //next set the line style
        XYSeriesRenderer a15R = new XYSeriesRenderer();

        // color option for multiple line
        a15R.setColor(Color.YELLOW);

        a15R.setChartValuesTextSize(15);
        a15R.setLineWidth(1);
        a15R.setChartValuesSpacing(3);
        a15R.setPointStyle(PointStyle.POINT);
        a15R.setFillPoints(true);
        renderer.addSeriesRenderer(a15R);

        //next set the line style
        XYSeriesRenderer a7R = new XYSeriesRenderer();

        // color option for multiple line
        a7R.setColor(Color.BLUE);

        a7R.setChartValuesTextSize(15);
        a7R.setLineWidth(1);
        a7R.setChartValuesSpacing(3);
        a7R.setPointStyle(PointStyle.POINT);
        a7R.setFillPoints(true);
        renderer.addSeriesRenderer(a7R);

        //next set the line style
        XYSeriesRenderer gpuR = new XYSeriesRenderer();

        // color option for multiple line
        gpuR.setColor(Color.RED);

        gpuR.setChartValuesTextSize(15);
        gpuR.setLineWidth(1);
        gpuR.setChartValuesSpacing(3);
        gpuR.setPointStyle(PointStyle.POINT);
        gpuR.setFillPoints(true);
        renderer.addSeriesRenderer(gpuR);

        //next set the line style
        XYSeriesRenderer memR = new XYSeriesRenderer();

        // color option for multiple line
        memR.setColor(Color.CYAN);

        memR.setChartValuesTextSize(15);
        memR.setLineWidth(1);
        memR.setChartValuesSpacing(3);
        memR.setPointStyle(PointStyle.POINT);
        memR.setFillPoints(true);
        renderer.addSeriesRenderer(memR);

        return renderer;
    }

    // this function prepare the dataset
    private XYMultipleSeriesDataset getDateDrawDataset() {
        final int nr = 10;
        long value = new Date().getTime();
        Random r = new Random();
        a15W = new TimeSeries("A15 Watt");
        for (int k = 0; k < nr; k++) {
            a15W.add(new Date(value + k * 1000), 2 * r.nextDouble());
        }
        mdataset.addSeries(a15W);

        a7W = new TimeSeries("A7 Watt");
        for (int k = 0; k < nr; k++) {
            a7W.add(new Date(value + k * 1000), 2 * r.nextDouble());
        }
        mdataset.addSeries(a7W);

        gpuW = new TimeSeries("GPU Watt");
        for (int k = 0; k < nr; k++) {
            gpuW.add(new Date(value + k * 1000), 2 * r.nextDouble());
        }
        mdataset.addSeries(gpuW);

        memW = new TimeSeries("Mem Watt");
        for (int k = 0; k < nr; k++) {
            memW.add(new Date(value + k * 1000), 2 * r.nextDouble());
        }
        mdataset.addSeries(memW);

        Log.i(TAG, mdataset.toString());
        return mdataset;

    }

    // the destroy function of the service
    @Override
    public void onDestroy() {
        super.onDestroy();
        timer.cancel();
        SENSOR_OPEN = false;
        NativeLib.CloseINA231();
        wm.removeView(chart);
    }

}