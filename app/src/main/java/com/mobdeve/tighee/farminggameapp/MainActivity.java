package com.mobdeve.tighee.farminggameapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.OneTimeWorkRequest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private TextView moneyTv;
    private Button buyBtn;
    private LinearLayout currentProductHll;
    private LinearLayout gameLogVll;
    private LinearLayout currentProductVll;
    private ScrollView gameLogSv;
    private ProgressBar progressBar;

    private int money;
    private int jobCount;

    private boolean registeredReceiver = false;

    private BroadcastReceiver myReceiver;
    private IntentFilter myFilter;

    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.executorService = Executors.newSingleThreadExecutor();

        this.moneyTv = findViewById(R.id.moneyTv);
        this.buyBtn = findViewById(R.id.buyBtn);
        this.currentProductHll = findViewById(R.id.currentProductHll);
        this.gameLogVll = findViewById(R.id.gameLogVll);
        this.gameLogSv = findViewById(R.id.gameLogSv);
        this.currentProductVll = findViewById(R.id.currentProductVll);

        this.money = 30;
        this.moneyTv.setText("Money: " + this.money);

        this.jobCount = 1;

        addNoTaskView();

        this.buyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(money >= 10) {
                    updateMoney(-10);
                    executorService.execute(new ProductionRunnable(jobCount, MainActivity.this));
                    addToGameLog(jobCount, "Production task queued.");
                    jobCount++;
                } else {
                    Toast.makeText(
                            MainActivity.this,
                            "Not enough money... kono Dio da!.",
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        this.myReceiver = new MyBroadcastReceiver();
        this.myFilter = new IntentFilter();
        this.myFilter.addAction(Constants.START_INTENT_ACTION);
        this.myFilter.addAction(Constants.PROGRESS_INTENT_ACTION);
        this.myFilter.addAction(Constants.FINISH_INTENT_ACTION);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(!this.registeredReceiver) {
            registerReceiver(this.myReceiver, this.myFilter);
            registeredReceiver = true;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(this.registeredReceiver) {
            unregisterReceiver(this.myReceiver);
            this.registeredReceiver = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: called");
    }

    private void addNoTaskView() {
        TextView tv = new TextView(this);
        tv.setText("No current task running.");

        this.currentProductHll.addView(tv);
        this.currentProductVll.setMinimumHeight(220);
    }

    private void addToGameLog(int jobCount, String message) {
        String log = "Task #" + jobCount + ": " + message;

        TextView temp_tv = new TextView(MainActivity.this);
        temp_tv.setText(log);

        gameLogVll.addView(temp_tv);
    }

    private void updateMoney(int m) {
        this.money = this.money + m;
        this.moneyTv.setText("Money: " + this.money);
    }

    private void scrollToBottom() {
        gameLogSv.post(new Runnable() {
            @Override
            public void run() {
                gameLogSv.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    public class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive:");

            int jobCount = intent.getIntExtra(Constants.JOB_ID_KEY, 0);
            String productName = intent.getStringExtra(Constants.NAME_KEY);

            if(intent.getAction().equals(Constants.START_INTENT_ACTION)) {
                int imageId = intent.getIntExtra(Constants.IMAGE_KEY, 0);
                int productionTime = intent.getIntExtra(Constants.TIME_KEY, 0);

                addToGameLog(jobCount, "Work on " + productName + " product started.");
                addCurrentTaskViews(imageId, productName, productionTime);

                scrollToBottom();
            } else if(intent.getAction().equals(Constants.FINISH_INTENT_ACTION)) {
                int generatedMoney = intent.getIntExtra(Constants.MONEY_KEY, 0);

                updateMoney(generatedMoney);
                addToGameLog(jobCount, "Work on " + productName + " has finished. Generated " + generatedMoney + " money.");
                removeCurrentTaskViews();

                scrollToBottom();
            } else if(intent.getAction().equals(Constants.PROGRESS_INTENT_ACTION)) {
                int currProgress = intent.getIntExtra(Constants.PROGRESS_KEY, 0);

                progressBar.setProgress(currProgress);
            }
        }
    }

    private void addCurrentTaskViews(int imageId, String productName, int productionTime) {
        this.currentProductHll.removeAllViews();

        ImageView iv = new ImageView(this);
        iv.setImageResource(imageId);
        iv.setAdjustViewBounds(true);
        iv.setMaxHeight(150);
        iv.setMaxHeight(150);
        this.currentProductHll.addView(iv);

        TextView tv = new TextView(this);
        tv.setText("  " + productName);
        tv.setGravity(Gravity.CENTER_VERTICAL);
        this.currentProductHll.addView(tv);

        this.progressBar = new ProgressBar(this,null, android.R.attr.progressBarStyleHorizontal);
        this.progressBar.setMax(productionTime);
        this.progressBar.setProgress(0);
        this.currentProductVll.addView(progressBar);
    }

    private void removeCurrentTaskViews() {
        this.currentProductHll.removeAllViews();

        addNoTaskView();

        View v = this.currentProductVll.getChildAt(0);
        this.currentProductVll.removeAllViews();
        this.currentProductVll.addView(v);
    }
}