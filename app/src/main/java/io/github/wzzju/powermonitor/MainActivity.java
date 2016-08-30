package io.github.wzzju.powermonitor;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);//合法的
        Intent intent = new Intent(MainActivity.this,FloatView.class);
        startService(intent);
        finish();
    }
}
