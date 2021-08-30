package com.mobdeve.tighee.farminggameapp;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

/*
 * On initialization, our custom Runnable object needs a job ID, which it would use to broadcast its
 * current progress to the log. This handles the main logic of the task, which involves (1) creating
 * a random product instance, (2) running the Thread.sleep to simulate working on the task, and (3)
 * finishing the task. At each step, the runnable utilizes sendBroadcast method call to send updates
 * to the UI thread -- where our MyBroadcastReceiver is waiting. The logic for randomly generating
 * a product and randomly generating the output money is found in Product.java.
 *
 * In comparison to the other solution to this game app, we don't use handlers but utilize an
 * indirect bridge to the main thread -- broadcasts -- to update the UI. The BroadcastReceiver runs
 * on the main thread.
 *
 * Side note: I could have simplified the "putting" of information into the intents by not creating
 * a new intent all the time. The job and name are repeating and won't handle the naman, so this is
 * possible, but so things are clear, I decided to not do that.
 * */
public class ProductionRunnable implements Runnable {
    private final static String TAG = "ProductionTaskLog";

    private int taskId;
    private Context context;

    public ProductionRunnable(int taskId, Context context) {
        this.taskId = taskId;
        this.context = context;
    }

    /* Input data breakdown:
     *      0 -> Corn
     *      1 -> Grapes
     *      2 -> Apples
     * */

    @Override
    public void run() {
        Log.d(TAG, "run: id is" + this.taskId);

        // Randomly create a new product
        Product product = Product.generateProduct();
        Log.d(TAG, "run: created product: " + product.getName());

        // Send a message to the MainActivity that a product is starting production
        Intent i = new Intent();
        i.setAction(Constants.START_INTENT_ACTION);
        i.putExtra(Constants.NAME_KEY, product.getName());
        i.putExtra(Constants.IMAGE_KEY, product.getImageId());
        i.putExtra(Constants.TIME_KEY, product.getProductionTime());
        i.putExtra(Constants.JOB_ID_KEY, this.taskId);
        this.context.sendBroadcast(i);

        // Work on the actual product (i.e. sleep...) + send updates
        for(int ctr = 0; ctr < product.getProductionTime(); ctr++) {
            Log.d(TAG, "doWork: working on product... " + ctr);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            i = new Intent();
            i.setAction(Constants.PROGRESS_INTENT_ACTION);
            i.putExtra(Constants.NAME_KEY, product.getName());
            i.putExtra(Constants.PROGRESS_KEY, ctr);
            i.putExtra(Constants.JOB_ID_KEY, this.taskId);
            this.context.sendBroadcast(i);
        }

        Log.d(TAG, "doWork: finished.");

        // Production finished
        i = new Intent();
        i.setAction(Constants.FINISH_INTENT_ACTION);
        i.putExtra(Constants.NAME_KEY, product.getName());
        i.putExtra(Constants.MONEY_KEY, product.generateMoney());
        i.putExtra(Constants.JOB_ID_KEY, this.taskId);
        this.context.sendBroadcast(i);
    }
}
