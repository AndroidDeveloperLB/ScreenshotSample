package com.lb.screenshot_sample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.mtsahakis.mediaprojectiondemo.ScreenshotManager;


public class MainActivity extends Activity {

    private static final int REQUEST_ID = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.checkIfPossibleToRecordButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                ScreenshotManager.INSTANCE.requestScreenshotPermission(MainActivity.this, REQUEST_ID);
            }
        });
        findViewById(R.id.takeScreenshotButton).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                if(!ScreenshotManager.INSTANCE.takeScreenshot(MainActivity.this))
                    Toast.makeText(MainActivity.this, "failed to take screenshot for some reason", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ID)
            ScreenshotManager.INSTANCE.onActivityResult(resultCode, data);
    }
}
