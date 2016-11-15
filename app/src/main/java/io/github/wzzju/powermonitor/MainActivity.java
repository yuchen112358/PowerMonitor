package io.github.wzzju.powermonitor;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);//合法的
        //使得需要读取的文件或设备具有可读写权限
        List<String> cmds = new ArrayList<>();
        cmds.add("chmod 666 /dev/sensor_*");
        cmds.add("chmod 666 /sys/devices/10060000.tmu/temp");
        cmds.add("chmod 666 /sys/devices/11800000.mali/clock");
        cmds.add("chmod 666 /sys/devices/system/cpu/cpu*/cpufreq/cpuinfo_cur_freq");
        boolean result = doCmds(cmds);
        if (result) {
            Intent intent = new Intent(MainActivity.this, FloatView.class);
            startService(intent);
            finish();
        } else {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle("About Root");
            dialog.setMessage("Fail to get Root!");
            dialog.setCancelable(false);
            dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    MainActivity.this.finish();
                }
            });
            dialog.show();
        }

    }

    public boolean doCmds(List<String> cmds) {
        Process process = null;
        DataOutputStream os = null;
        boolean flag = false;

        try {
            process = Runtime.getRuntime().exec("su");//执行此举之后，相当于获得了一个shell终端，该shell终端具有su权限
            os = new DataOutputStream(process.getOutputStream());//获得shell进程的输出流os，通过os可以向shell输入命令
            for (String tmpCmd : cmds) {
                os.writeBytes(tmpCmd + "\n");
            }
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
            if (process.exitValue() == 0) {
                flag = true;
                Log.d("*** DEBUG ***", "Get Root successfully!");
            } else {
                flag = false;
                Log.e("*** ERROR ***", "Fail to get Root! ");
            }

        } catch (Exception e) {
            Log.e("*** ERROR ***", "Fail to get Root! " + e.getMessage());
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                if (process != null) {
                    process.destroy();
                }
            } catch (Exception e) {
                Log.e("*** ERROR ***", "Fail to close the output stream of the process! " + e.getMessage());
            }

        }
        return flag;

    }
}
