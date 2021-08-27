package com.mobdeve.tighee.farminggameapp;

import androidx.appcompat.app.AppCompatActivity;

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

    private TextView moneyTv;
    private Button buyBtn;
    private LinearLayout currentProductHll;
    private LinearLayout gameLogVll;
    private LinearLayout currentProductVll;
    private ScrollView gameLogSv;
    private ProgressBar progressBar;

    private int money;
    private int jobCount;

    private ExecutorService executorService;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.executorService = Executors.newSingleThreadExecutor();
        this.handler = new Handler();

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

                    ProductionRunnable productionRunnable = new ProductionRunnable(jobCount);
                    executorService.execute(productionRunnable);

                    addToGameLog(jobCount, "Production task queued.");
                    scrollToBottom();

                    jobCount++;
                } else {
                    Toast.makeText(
                            MainActivity.this,
                            "Not enough money... kono Dio da!.",
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: called");
        this.executorService.shutdownNow();
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

    private class ProductionRunnable implements Runnable {
        private int taskId;

        public ProductionRunnable(int taskId) {
            this.taskId = taskId;
        }

        @Override
        public void run() {
            try {
                Log.d(TAG, "thread: id is" + this.taskId);

                // Randomly create a new product
                Product product = Product.generateProduct();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        addToGameLog(taskId, "Work on " + product.getName() + " product started.");
                        addCurrentTaskViews(product.getImageId(), product.getName(), product.getProductionTime());
                        scrollToBottom();
                    }
                });
                Log.d(TAG, "thread: created product: " + product.getName());

                // Work on the actual product (i.e. sleep...) + send updates
                int i;
                for(i = 0; i < product.getProductionTime() && !Thread.currentThread().isInterrupted(); i++) {
                    Log.d(TAG, "thread: working on product... " + i);
                    int ctr = i;

                    Thread.sleep(1000);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setProgress(ctr);
                        }
                    });
                }

                // Production finished
                int generatedMoney = product.generateMoney();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        updateMoney(generatedMoney);
                        addToGameLog(taskId, "Work on " + product.getName() + " has finished. Generated " + generatedMoney + " money.");
                        removeCurrentTaskViews();
                    }
                });
                Log.d(TAG, "thread: finished.");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}