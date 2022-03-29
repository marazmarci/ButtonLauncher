package com.brouken.wear.butcher;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.wear.widget.WearableLinearLayoutManager;
import androidx.wear.widget.WearableRecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static com.brouken.wear.butcher.Utils.log;

public class AppPickerActivity extends Activity {

    private WearableRecyclerView mWearableRecyclerView;
    private CustomRecyclerAdapter mCustomRecyclerAdapter;

    private List<ResolveInfo> pkgAppsList;
    private final List<ResolveInfo> assistantApps = new ArrayList<>();

    private Context mContext;

    private String pref;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = this;

        pref = getIntent().getStringExtra("pref");

        // Add all launchable apps
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        pkgAppsList = mContext.getPackageManager().queryIntentActivities( mainIntent, 0);

        // Add "Google"
        Intent assistIntent = new Intent("android.intent.action.VOICE_ASSIST");
        assistIntent.addCategory(Intent.CATEGORY_DEFAULT);
        List<ResolveInfo> pkgAssistAppsList = mContext.getPackageManager().queryIntentActivities( assistIntent, 0);

        for (ResolveInfo resolveInfo : pkgAssistAppsList) {
            if (resolveInfo.activityInfo.packageName.equals(getPackageName())) {
                continue;
            }

            assistantApps.add(resolveInfo);
            pkgAppsList.add(resolveInfo);
        }

        // Remove Button Launcher from list
        pkgAppsList.removeIf(resolveInfo -> resolveInfo.activityInfo.packageName.equals(getPackageName()));

        // TODO: don't add if not set
        Intent timerIntent = new Intent("com.brouken.wear.butcher.intent.action.AUTO_TIMER", null);
        List<ResolveInfo> timerAppList = mContext.getPackageManager().queryIntentActivities(timerIntent, 0);
        pkgAppsList.addAll(timerAppList);

        Intent noActionIntent = new Intent("com.brouken.wear.butcher.intent.action.NO_ACTION", null);
        List<ResolveInfo> noActionAppList = mContext.getPackageManager().queryIntentActivities(noActionIntent, 0);
        pkgAppsList.addAll(noActionAppList);

        pkgAppsList.sort(new ResolveInfo.DisplayNameComparator(mContext.getPackageManager()));
        pkgAppsList.add(0, null);

        setContentView(R.layout.activity_app);

        mWearableRecyclerView = findViewById(R.id.recycler_view);

        //mWearableRecyclerView.setCircularScrollingGestureEnabled(true);
        //mWearableRecyclerView.setBezelFraction(0.5f);
        //mWearableRecyclerView.setScrollDegreesPerScreen(90);

        mWearableRecyclerView.setEdgeItemsCenteringEnabled(true);

        // Customizes scrolling so items farther away form center are smaller.
        ScalingScrollLayoutCallback scalingScrollLayoutCallback = new ScalingScrollLayoutCallback();
        mWearableRecyclerView.setLayoutManager(new WearableLinearLayoutManager(this, scalingScrollLayoutCallback));

        // Improves performance because we know changes in content do not change the layout size of
        // the RecyclerView.
        mWearableRecyclerView.setHasFixedSize(true);

        mCustomRecyclerAdapter = new CustomRecyclerAdapter(pkgAppsList.toArray(new ResolveInfo[pkgAppsList.size()]),this);

        mWearableRecyclerView.setAdapter(mCustomRecyclerAdapter);

        // TODO: HACK
        /*
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                String name = PreferenceManager.getDefaultSharedPreferences(mContext).getString("typeface", JustTimeWatchFace.preference_typeface);
                mWearableRecyclerView.scrollToPosition(fonts.indexOf(name));
            }
        }, 200);
        */
    }

    private boolean isAssistApp(String pkg, String cls) {
        return assistantApps.stream()
                .map(resolveInfo -> resolveInfo.activityInfo)
                .anyMatch(info -> info.packageName.equals(pkg) && info.name.equals(cls));
    }


    public void itemSelected(String pkg, String cls) {
        Intent intent=new Intent();
        intent.putExtra("pref", pref);

        if (pkg != null && cls != null) {
            String action = Intent.ACTION_MAIN;
            String category = Intent.CATEGORY_LAUNCHER;

            if (isAssistApp(pkg, cls)) {
                action = "android.intent.action.VOICE_ASSIST";
                category = Intent.CATEGORY_DEFAULT;
            }

            intent.putExtra("action", action);
            intent.putExtra("category", category);
            intent.putExtra("app", pkg + "/" + cls + "/" + action + "/" + category);
        }

        intent.putExtra("pkg", pkg);
        intent.putExtra("cls", cls);
        setResult(RESULT_OK, intent);
        finish();
    }

    private static final class CustomRecyclerAdapter extends WearableRecyclerView.Adapter<CustomRecyclerAdapter.ViewHolder> {

        final private ResolveInfo[] mDataSet;
        final private AppPickerActivity mAppPickerActivity;

        public static class ViewHolder extends android.support.wearable.view.WearableRecyclerView.ViewHolder {

            private final TextView mTextView;
            private final ImageView mImageView;

            public ViewHolder(View view) {
                super(view);
                mTextView = view.findViewById(R.id.textView);
                mImageView = view.findViewById(R.id.imageView);
            }

            @Override
            public String toString() { return (String) mTextView.getText(); }
        }

        public CustomRecyclerAdapter(ResolveInfo[] dataSet, AppPickerActivity appPickerActivity) {
            mDataSet = dataSet;
            mAppPickerActivity = appPickerActivity;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.recycler_row_item, viewGroup, false);

            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, final int position) {
            log("Element " + position + " set.");

            viewHolder.mTextView.setOnClickListener(view -> {
                if (mDataSet[position] == null) {
                    mAppPickerActivity.itemSelected(null, null);
                    return;
                }

                String pkg = mDataSet[position].activityInfo.packageName;
                String cls = mDataSet[position].activityInfo.name;
                mAppPickerActivity.itemSelected(pkg, cls);

            });

            // Replaces content of view with correct element from data set
            //viewHolder.mTextView.setText(mDataSet[position]);
            if (mDataSet[position] == null) {
                viewHolder.mTextView.setText(R.string.none);
                viewHolder.mImageView.setImageDrawable(null);
                return;
            }

            viewHolder.mTextView.setText(mDataSet[position].activityInfo.loadLabel(mAppPickerActivity.getPackageManager()).toString());
            viewHolder.mImageView.setImageDrawable(mDataSet[position].loadIcon(mAppPickerActivity.getPackageManager()));
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return mDataSet.length;
        }
    }


}
