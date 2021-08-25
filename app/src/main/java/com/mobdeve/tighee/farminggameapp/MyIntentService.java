package com.mobdeve.tighee.farminggameapp;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import java.util.Random;

public class MyIntentService extends IntentService {

    public static final String START_INTENT_ACTION = "main.activity.start.product.intent.action";
    public static final String FINISH_INTENT_ACTION = "main.activity.finish.product.intent.action";
    public static final String PROGRESS_UPDATE_INTENT_ACTION = "main.activity.progress.update.intent.action";

    private static final String TAG = "MyIntentService";

    public static final String START_LOG_KEY = "START_LOG_KEY";
    public static final String END_LOG_KEY = "END_LOG_KEY";
    public static final String MONEY_KEY = "MONEY_KEY";
    public static final String PROGRESS_KEY = "PROGRESS_KEY";

    private Random r = new Random();

    private boolean on_destroy_called = false;

    public MyIntentService() {
        super("MyIntentService");
    }

    /*
    * NUM TO TASK REFERENCE:
    *   0 --> CORN
    *   1 --> GRAPES
    *   2 --> APPLES
    * */
    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "onHandleIntent: ");
        if(intent != null) {
            final String action = intent.getAction();
            if(action.equals(MainActivity.BUY_INTENT_ACTION)) {
                int task_type = intent.getIntExtra(MainActivity.TASK_TYPE_KEY, 0);
                int task_count = intent.getIntExtra(MainActivity.TASK_COUNT_KEY, 0);

                int wait_time, money_generated;
                switch(task_type) {
                    case 0: // corn
                        wait_time = 4;
                        money_generated = r.nextInt(3) + 10; // 10-13
                        break;
                    case 1:
                        wait_time = 20;
                        money_generated = r.nextInt(11) + 30; // 30-40
                        break;
                    default:
                        wait_time = 8;
                        money_generated = r.nextInt(5) + 15; // 15-20
                        break;
                }

                if(!on_destroy_called) {
                    send_start_broadcast(
                            "Now working on task #" + task_count + "...",
                            task_type);
                }

                wait_time = wait_time * 1000;
                try {
                    for(int i = 0; i < 100; i += 10) {
                        Thread.sleep(wait_time / 10);
                        if(!on_destroy_called) {
                            send_progress_update_broadcast(i);
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if(!on_destroy_called) {
                    send_end_broadcast(
                            "Working on task #" + task_count + " has finished. Generated " + money_generated + " money.",
                            money_generated);
                }
            }
        }
    }

    private void send_start_broadcast(String message, int task_type) {
        Intent i = new Intent();
        i.setAction(START_INTENT_ACTION);
        i.putExtra(START_LOG_KEY, message);
        i.putExtra(MainActivity.TASK_TYPE_KEY, task_type);
        sendBroadcast(i);
    }

    private void send_progress_update_broadcast(int current_progress) {
        Intent i = new Intent();
        i.setAction(PROGRESS_UPDATE_INTENT_ACTION);
        i.putExtra(PROGRESS_KEY, current_progress);
        sendBroadcast(i);
    }

    private void send_end_broadcast(String message, int money_generated) {
        Intent i = new Intent();
        i.setAction(FINISH_INTENT_ACTION);
        i.putExtra(END_LOG_KEY, message);
        i.putExtra(MONEY_KEY, money_generated);
        sendBroadcast(i);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        on_destroy_called = true;
        Log.d(TAG, "onDestroy: called");
    }
}
























