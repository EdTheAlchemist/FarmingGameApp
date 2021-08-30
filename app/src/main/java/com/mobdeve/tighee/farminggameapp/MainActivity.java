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

    // Components for handling concurrent processes
    private ExecutorService executorService;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Here we define an ExecutorService, which will help us in managing incoming tasks. We
        // use a single thread executor to force all tasks to be executed in a queue like fashion.
        this.executorService = Executors.newSingleThreadExecutor();
        // Initializing a default handler associates it to the Main Thread's looper.
        this.handler = new Handler();

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

                    // This is our custom runnable object. See the object below for more info.
                    ProductionRunnable productionRunnable = new ProductionRunnable(jobCount);
                    executorService.execute(productionRunnable);

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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: called");
        // When the app shuts down, we want to make sure that no other tasks in the queue will
        // execute. Shutdown now forces an interrupt exception to occur.
        this.executorService.shutdownNow();
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

        // This is a UI thing to scroll to the bottom since we're adding views at the bottom.
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
    * This is our custom runnable class. On initialization, it needs a job ID, which it would use to
    * broadcast its current progress to the log via a handler. This handles the main logic of the
    * task, which involves (1) creating a random product instance, (2) running the Thread.sleep to
    * simulate working on the task, and (3) finishing the task. At each step, the runnable utilizes
    * a handler to send updates to the UI thread. The logic for randomly generating a product and
    * randomly generating the output money is found in Product.java.
    *
    * In comparison to the other solution to this game app, we don't use broadcasts but utilize a
    * direct bridge to the main thread -- the handler -- to update the UI.
    * */
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
                // Updates UI
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        addToGameLog(taskId, "Work on " + product.getName() + " product started.");
                        addCurrentTaskViews(product.getImageId(), product.getName(), product.getProductionTime());
                    }
                });
                Log.d(TAG, "thread: created product: " + product.getName());

                // Work on the actual product (i.e. sleep...) + send updates
                for(int i = 0; i < product.getProductionTime() && !Thread.currentThread().isInterrupted(); i++) {
                    Log.d(TAG, "thread: working on product... " + i);

                    Thread.sleep(1000);

                    int ctr = i;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setProgress(ctr);
                        }
                    });
                }

                // Production finished, so generate the money and update the UI
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