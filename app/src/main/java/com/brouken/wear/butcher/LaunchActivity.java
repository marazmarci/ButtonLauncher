package com.brouken.wear.butcher;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.wearable.activity.WearableActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;

import static com.brouken.wear.butcher.Utils.log;

public class LaunchActivity extends WearableActivity {

    private ImageView mImageView;
    private ImageView mImageView2;
    private ImageView mImageView3;
    private ProgressBar mProgressBar;

    ObjectAnimator animator;

    private boolean launchedViaAssist = false;
    private boolean launchedViaCustom = false;

    private boolean vibrate = true;
    private int timeout = 3000;

    boolean longPressed = false;

    LaunchActions mLaunchActions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch2);

        log("onCreate()");
        loadConfig();

        mImageView = findViewById(R.id.imageView);
        //mImageView2 = findViewById(R.id.imageView2);
        //mImageView3 = findViewById(R.id.imageView3);
        mProgressBar = findViewById(R.id.progressBar);

        animator = ObjectAnimator.ofInt(mProgressBar, "progress", timeout);
        mProgressBar.setMax(timeout);

        // Enables Always-on
        //setAmbientEnabled();

        //Uri ref = ActivityCompat.getReferrer(LaunchActivity.this);
        // launcher: android-app://com.google.android.wearable.app
        // home long press/assist: android-app://android
        // configurable button: android-app://com.google.android.apps.wearable.settings, because intent action is null

        handleStart(getIntent());

        mLaunchActions = new LaunchActions(this, launchedViaAssist);

        if (!isFinishing()) {
            loadIcon();
            loadConfig();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        log("onNewIntent()");

        String action = intent.getAction();
        if (action != null) {
            if (action.equals(Intent.ACTION_ASSIST)) {
                final String app = mLaunchActions.getAppForButton(0, true);
                launchApp(app, false);
            }
        }
    }

    private void handleStart(Intent intent) {
        boolean launchedViaLauncher = false;

        String action = intent.getAction();
        if (action != null) {
            if (action.equals(Intent.ACTION_ASSIST))
                launchedViaAssist = true;
            else if (action.equals(Intent.ACTION_MAIN))
                launchedViaLauncher = true;
        } else
            launchedViaCustom = true;

        if (launchedViaLauncher) {
            Intent config = new Intent(this, ConfigActivity.class);
            startActivity(config);
            finish();
        } else {
            if (!launchedViaAssist)
                vibrate();
        }
    }

    private void loadIcon() {
        try {
            final String app = mLaunchActions.getAppForButton(-1, false);

            if (app == null)
                return;

            String[] appParts = app.split("/");

            ComponentName componentName = new ComponentName(appParts[0], appParts[1]);
            Drawable icon = getPackageManager().getActivityIcon(componentName);
            mImageView.setImageDrawable(icon);

            mImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    launchApp(app, true);
                }
            });
        } catch (PackageManager.NameNotFoundException e) {}
    }

    @Override
    protected void onStart() {
        super.onStart();
        log("onStart()");

        if (mLaunchActions.hasOnlyDefaultAction()) {
            String app = mLaunchActions.getAppForButton(-1, false);
            launchApp(app, !launchedViaAssist);
        } else {
            if (mLaunchActions.hasDefaultAction())
                startCountdown();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        log("onStop()");

        if (!isFinishing())
            finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        log("onDestroy()");
    }

    private void startCountdown() {
        animator.setDuration(timeout);
        animator.setInterpolator(new LinearInterpolator());

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);

                log("onAnimationEnd()");

                String app = mLaunchActions.getAppForButton(-1, false);
                launchApp(app, true);
            }
        });

        animator.start();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        log("onKeyDown");

        if (keyCode >= KeyEvent.KEYCODE_STEM_1 && keyCode <= KeyEvent.KEYCODE_STEM_3) {
            event.startTracking();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {

        log("onKeyLongPress");

        if (keyCode >= KeyEvent.KEYCODE_STEM_1 && keyCode <= KeyEvent.KEYCODE_STEM_3) {
            longPressed = true;

            String app = mLaunchActions.getAppForButton(keyCode - KeyEvent.KEYCODE_STEM_PRIMARY, true);
            launchApp(app, true);

            return true;
        }

        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {

        log("onKeyUp");

        if (keyCode >= KeyEvent.KEYCODE_STEM_1 && keyCode <= KeyEvent.KEYCODE_STEM_3) {
            if (!longPressed) {
                String app = mLaunchActions.getAppForButton(keyCode - KeyEvent.KEYCODE_STEM_PRIMARY, false);
                launchApp(app, true);
            }
            return true;
        }

        return super.onKeyUp(keyCode, event);
    }

    private void vibrate() {
        if (!vibrate)
            return;

        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        long[] pattern = {0, 20};
        vibrator.vibrate(pattern, -1);
    }

    private void launchApp(String app, boolean vibrate) {
        if (app == null)
            return;

        String[] parts = app.split("/");

        String pkg = parts[0];
        String cls = parts[1];
        String action = Intent.ACTION_MAIN;
        String category = Intent.CATEGORY_LAUNCHER;

        if (parts.length > 2)
            action = parts[2];
        if (parts.length > 3)
            category = parts[3];

        launchApp(pkg, cls, action, category, vibrate);
    }

    private void launchApp(String pkg, String cls, String action, String category, boolean vibrate) {
        if (animator != null)
            animator.pause();

        if (isFinishing())
            return;

        ComponentName componentName = new ComponentName(pkg, cls);
        Intent intent=new Intent(action);
        intent.addCategory(category);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setComponent(componentName);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);

            if (vibrate)
                vibrate();
        }

        finish();
    }

    private void loadConfig() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        vibrate = sharedPreferences.getBoolean("vibrate", vibrate);
        timeout = Integer.parseInt(sharedPreferences.getString("timeout", Integer.toString(timeout)));
    }
}
