package com.mobdeve.tighee.farminggameapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
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

    // Views for the activity
    private TextView moneyTv;
    private Button buyBtn;
    private LinearLayout currentProductHll;
    private LinearLayout gameLogVll;
    private LinearLayout currentProductVll;
    private ScrollView gameLogSv;
    private ProgressBar progressBar;

    // Keeps track of our money
    private int money;
    // Keeps track of the number of jobs sent off. doubles as the job ID.
    private int jobCount;

    // Components needed for the BroadcastReceiver
    private boolean registeredReceiver = false;
    private BroadcastReceiver myReceiver;
    private IntentFilter myFilter;

    // Component for handling the queuing of background tasks
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Here we define an ExecutorService, which will help us in managing incoming tasks. We
        // use a single thread executor to force all tasks to be executed in a queue like fashion.
        this.executorService = Executors.newSingleThreadExecutor();

        // View initialization
        this.moneyTv = findViewById(R.id.moneyTv);
        this.buyBtn = findViewById(R.id.buyBtn);
        this.currentProductHll = findViewById(R.id.currentProductHll);
        this.gameLogVll = findViewById(R.id.gameLogVll);
        this.gameLogSv = findViewById(R.id.gameLogSv);
        this.currentProductVll = findViewById(R.id.currentProductVll);

        // Initialization of the money and its view
        this.money = 300;
        this.moneyTv.setText("Money: " + this.money);

        // Need I explain this?
        this.jobCount = 1;

        // See method below for explanation. TLDR; it sets viws for when there's no task
        addNoTaskView();

        this.buyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Checks if the use has enough money
                if(money >= 10) {
                    // Generic function to modify the value and the view
                    updateMoney(-10);

                    // Here, we create our ProductionRunnable object, which handles the tasks
                    // needed. See ProductionRunnable.java for more info.
                    executorService.execute(new ProductionRunnable(jobCount, MainActivity.this));

                    // Once a task task has been scheduled, we add to the game log
                    addToGameLog(jobCount, "Production task queued.");

                    // Increment the job count; As this is also the job ID, we'd want this to be
                    // unique
                    jobCount++;
                } else {
                    Toast.makeText(
                            MainActivity.this,
                            "Not enough money... kono Dio da!.",
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        // Logic for the BroadcastReceiver. We use the IntentFilter to focus the receiver to only
        // specific actions.
        this.myReceiver = new MyBroadcastReceiver();
        this.myFilter = new IntentFilter();
        this.myFilter.addAction(Constants.START_INTENT_ACTION);
        this.myFilter.addAction(Constants.PROGRESS_INTENT_ACTION);
        this.myFilter.addAction(Constants.FINISH_INTENT_ACTION);
    }

    // We turn the receiver "on" in case the receiver had been turned off or on initialization.
    @Override
    protected void onStart() {
        super.onStart();
        if(!this.registeredReceiver) {
            registerReceiver(this.myReceiver, this.myFilter);
            registeredReceiver = true;
        }
    }

    // We turn the receiver "off" when the user is leaving as we don't want the receiver listening
    // when the user is not interacting with the activity.
    @Override
    protected void onPause() {
        super.onPause();
        if(this.registeredReceiver) {
            unregisterReceiver(this.myReceiver);
            this.registeredReceiver = false;
        }
    }

    // This is just so we can have the log tell us when the activity is destroyed and be able to
    // observe how other processes might still be running. Ideally, we wouldn't want to see anything
    // after onDestroy is called.
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: called");
    }

    // This handles adding the appropriate views informing the user that there are no tasks pending.
    // This can happen on start or when all tasks actuall finish and a lull is present.
    private void addNoTaskView() {
        TextView tv = new TextView(this);
        tv.setText("No current task running.");

        this.currentProductHll.addView(tv);
        this.currentProductVll.setMinimumHeight(220);
    }

    // We aren't doing anything complex with the game log and simply append a new TextView with an
    // incoming message. The jobCount variable is needed so we know which job is currently doing
    // something.
    private void addToGameLog(int jobCount, String message) {
        String log = "Task #" + jobCount + ": " + message;

        TextView temp_tv = new TextView(MainActivity.this);
        temp_tv.setText(log);

        gameLogVll.addView(temp_tv);
        gameLogSv.fullScroll(ScrollView.FOCUS_DOWN);
    }

    // A simple method that simplifies modifying the money and its view. This is done both on buying
    // a task and on receiving that a task had been completed.
    private void updateMoney(int m) {
        this.money = this.money + m;
        this.moneyTv.setText("Money: " + this.money);
    }

    // This method handles creating the views for showing a task is being worked on.
    // This was an older project that I decided to update and I didn't want to to through the hassle
    // of changing this to a fragment. Still, this can be optimized.
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

    // This handles removing the current task views.
    private void removeCurrentTaskViews() {
        this.currentProductHll.removeAllViews();

        addNoTaskView();

        View v = this.currentProductVll.getChildAt(0);
        this.currentProductVll.removeAllViews();
        this.currentProductVll.addView(v);
    }

    /*
     * This is our custom BroadcastReceiver class. Its job is to listen for when the background task
     *  finishes something. It basically responds to when the task starts, is being worked on, and
     * finishes. It mainly handles making changes to the UI. Logic here runs on the main thread.
     * */
    public class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive:");

            // All incoming intents  had a jobCount and productName, so we get them here.
            int jobCount = intent.getIntExtra(Constants.JOB_ID_KEY, 0);
            String productName = intent.getStringExtra(Constants.NAME_KEY);

            if(intent.getAction().equals(Constants.START_INTENT_ACTION)) {
                // For when the intent was for a production start. Here, we get the information
                // needed to display that a task is being worked on.
                int imageId = intent.getIntExtra(Constants.IMAGE_KEY, 0);
                int productionTime = intent.getIntExtra(Constants.TIME_KEY, 0);

                addToGameLog(jobCount, "Work on " + productName + " product started.");
                addCurrentTaskViews(imageId, productName, productionTime);
            } else if(intent.getAction().equals(Constants.FINISH_INTENT_ACTION)) {
                // When a task has finished, we want to get the money generated and update the UI
                // accordingly.
                int generatedMoney = intent.getIntExtra(Constants.MONEY_KEY, 0);

                updateMoney(generatedMoney);
                addToGameLog(jobCount, "Work on " + productName + " has finished. Generated " + generatedMoney + " money.");
                removeCurrentTaskViews();
            } else if(intent.getAction().equals(Constants.PROGRESS_INTENT_ACTION)) {
                // For handling updates. This happens every second due to the logic in our Runnable
                // object
                int currProgress = intent.getIntExtra(Constants.PROGRESS_KEY, 0);

                progressBar.setProgress(currProgress);
            }
        }
    }
}