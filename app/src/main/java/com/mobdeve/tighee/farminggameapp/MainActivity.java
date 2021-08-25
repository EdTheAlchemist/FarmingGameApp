package com.mobdeve.tighee.farminggameapp;

import androidx.appcompat.app.AppCompatActivity;
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
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    public static final String BUY_INTENT_ACTION = "main.activity.buy.product.intent.action";
    public static final String TASK_TYPE_KEY = "TASK_TYPE_KEY";
    public static final String TASK_COUNT_KEY = "TASK_COUNT_KEY";
    private static final String TAG = "MainActivity";

    private TextView money_tv;
    private Button buy_btn;
    private LinearLayout current_product_hll;
    private LinearLayout game_log_vll;
    private LinearLayout current_product_vll;
    private ScrollView game_log_sv;
    private ProgressBar progressBar;

    private int money;
    private int task_count;
    private Random r;
    private boolean registered_receiver = false;

    private int[] icon_ids = {
            R.drawable.corn,
            R.drawable.grapes,
            R.drawable.shiny_apple,
    };

    private BroadcastReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findUiElements();
        initializeVariablesAndViews();
    }

    private void findUiElements() {
        this.money_tv = findViewById(R.id.money_tv);
        this.buy_btn = findViewById(R.id.buy_btn);
        this.current_product_hll = findViewById(R.id.current_product_hll);
        this.game_log_vll = findViewById(R.id.game_log_vll);
        this.game_log_sv = findViewById(R.id.game_log_sv);
        this.current_product_vll = findViewById(R.id.current_product_vll);
    }

    private void initializeVariablesAndViews() {
        this.r = new Random();

        this.money = 30;
        this.money_tv.setText("Money: " + this.money);

        this.task_count = 0;

        this.buy_btn.setText("START RANDOM TASK");
        this.buy_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: ");
                if(money >= 10) {
                    update_money(-10);
                    task_count++;

                    int task_type = r.nextInt(3);

                    add_to_game_log("Queued task #" + task_count + ": growing " + get_task_name(task_type) + "...");

                    scroll_to_bottom();

                    Intent i = new Intent(MainActivity.this, MyIntentService.class);
                    i.setAction(BUY_INTENT_ACTION);
                    i.putExtra(TASK_TYPE_KEY, task_type);
                    i.putExtra(TASK_COUNT_KEY, task_count);
                    startService(i);
                } else {
                    Toast.makeText(
                            MainActivity.this,
                            "Not enough money. Exercise patience.",
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        add_no_current_task_text_view();
    }

    private void add_no_current_task_text_view() {
        TextView tv = new TextView(this);
        tv.setText("No current task running.");
        this.current_product_hll.addView(tv);
        this.current_product_vll.setMinimumHeight(220);
    }

    private void add_to_game_log(String log) {
        TextView temp_tv = new TextView(MainActivity.this);
        temp_tv.setText(log);
        game_log_vll.addView(temp_tv);
    }

    private void update_money(int m) {
        this.money = this.money + m;
        this.money_tv.setText("Money: " + this.money);
    }

    private String get_task_name(int task_type) {
        String task;
        if(task_type == 0) {
            task = "Corn";
        } else if(task_type == 1) {
            task = "Grapes";
        } else {
            task = "Apples";
        }
        return task;
    }

    private void add_current_task_view(int task_type) {
        this.current_product_hll.removeAllViews();

        ImageView iv = new ImageView(this);
        iv.setImageResource(icon_ids[task_type]);
        iv.setAdjustViewBounds(true);
        iv.setMaxHeight(150);
        iv.setMaxHeight(150);
        this.current_product_hll.addView(iv);

        TextView tv = new TextView(this);
        tv.setText("  " + get_task_name(task_type));
        tv.setGravity(Gravity.CENTER_VERTICAL);
        this.current_product_hll.addView(tv);

        this.progressBar = new ProgressBar(this,null, android.R.attr.progressBarStyleHorizontal);
        this.progressBar.setProgress(0);
        this.current_product_vll.addView(progressBar);
    }

    private void remove_current_task_view() {
        this.current_product_hll.removeAllViews();

        add_no_current_task_text_view();

        View v = this.current_product_vll.getChildAt(0);
        this.current_product_vll.removeAllViews();
        this.current_product_vll.addView(v);
    }

    public class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(MyIntentService.START_INTENT_ACTION)) {
                Log.d(TAG, "onReceive:");
                add_to_game_log(intent.getStringExtra(MyIntentService.START_LOG_KEY));
                add_current_task_view(intent.getIntExtra(MainActivity.TASK_TYPE_KEY, 0));
                scroll_to_bottom();
            } else if(intent.getAction().equals(MyIntentService.FINISH_INTENT_ACTION)) {
                add_to_game_log(intent.getStringExtra(MyIntentService.END_LOG_KEY));
                update_money(intent.getIntExtra(MyIntentService.MONEY_KEY, 0));
                remove_current_task_view();
                scroll_to_bottom();
            } else if(intent.getAction().equals(MyIntentService.PROGRESS_UPDATE_INTENT_ACTION)) {
                update_progress_bar(intent.getIntExtra(MyIntentService.PROGRESS_KEY, 0));
            }
        }
    }

    private void update_progress_bar(int p) {
        this.progressBar.post(new Runnable() {
            @Override
            public void run() {
                progressBar.setProgress(p);
            }
        });
    }

    private void scroll_to_bottom() {
        game_log_sv.post(new Runnable() {
            @Override
            public void run() {
                game_log_sv.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(!registered_receiver) {
            this.receiver = new MyBroadcastReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(MyIntentService.START_INTENT_ACTION);
            filter.addAction(MyIntentService.FINISH_INTENT_ACTION);
            filter.addAction(MyIntentService.PROGRESS_UPDATE_INTENT_ACTION);
            registerReceiver(this.receiver, filter);
            registered_receiver = true;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(registered_receiver) {
            Log.d(TAG, "onPause: unregistered");
            unregisterReceiver(this.receiver);
            registered_receiver = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: called");
        stopService(new Intent(this, MyIntentService.class));
    }
}