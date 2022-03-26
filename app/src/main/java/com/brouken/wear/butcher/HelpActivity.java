package com.brouken.wear.butcher;

import android.graphics.Point;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.input.WearableButtons;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;

public class HelpActivity extends WearableActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_help);

        TextView textView = findViewById(R.id.textView);

        if (getResources().getConfiguration().isScreenRound()) {
            ScrollView scrollView = findViewById(R.id.scrollView);
            LinearLayout layoutTop = findViewById(R.id.layoutTop);
            LinearLayout layoutBottom = findViewById(R.id.layoutBottom);

            Point size = new Point();
            getWindowManager().getDefaultDisplay().getSize(size);
            int padding = size.x / 10;

            scrollView.setPadding(padding, 0, padding, 0);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int)(padding * 2.5f));
            layoutTop.setLayoutParams(params);
            layoutBottom.setLayoutParams(params);
        }


        String labelPrimary = "";
        String labelSecondary = "";
        int buttonCount = WearableButtons.getButtonCount(this);

        if (buttonCount >= 1) {
            labelPrimary = "(" + WearableButtons.getButtonLabel(this, KeyEvent.KEYCODE_STEM_PRIMARY).toString().toLowerCase() + ")";

            ArrayList<String> labels = new ArrayList<>();
            for (int i = 1; i < buttonCount; i++) {
                String label = WearableButtons.getButtonLabel(this, KeyEvent.KEYCODE_STEM_PRIMARY + i).toString().toLowerCase();
                labels.add(label);
            }

            labelSecondary = "(" + TextUtils.join(", ", labels) + ")";
        }
        
        textView.setText(getString(R.string.help_screen_text, labelPrimary, labelSecondary));
    }
}
