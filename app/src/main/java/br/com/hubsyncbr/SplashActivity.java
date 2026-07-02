package br.com.hubsyncbr;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SplashActivity extends Activity {
    private static final int BG = Color.rgb(7, 10, 18);
    private static final int TEXT = Color.rgb(236, 240, 255);
    private static final int MUTED = Color.rgb(154, 163, 184);
    private static final int PURPLE = Color.rgb(139, 92, 246);
    private static final int BLUE = Color.rgb(56, 189, 248);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window w = getWindow();
        w.setStatusBarColor(BG);
        w.setNavigationBarColor(BG);
        prepareBrowserPrefs();
        buildSplash();
        new Handler().postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 1450);
    }

    private void prepareBrowserPrefs() {
        SharedPreferences prefs = getSharedPreferences("hub", MODE_PRIVATE);
        SharedPreferences.Editor e = prefs.edit();
        if (!prefs.contains("homepage_mode")) e.putString("homepage_mode", "hub");
        if (!prefs.contains("custom_homepage")) e.putString("custom_homepage", "https://www.google.com");
        if (!prefs.contains("search_engine")) e.putString("search_engine", "google");
        if (!prefs.contains("nav_autohide")) e.putBoolean("nav_autohide", true);
        if (!prefs.contains("open_new_window_home")) e.putBoolean("open_new_window_home", true);
        e.apply();
    }

    private void buildSplash() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(24), dp(24), dp(24), dp(24));
        root.setBackgroundColor(BG);

        TextView logo = new TextView(this);
        logo.setGravity(Gravity.CENTER);
        logo.setText("▣▣");
        logo.setTextSize(50);
        logo.setTypeface(Typeface.DEFAULT_BOLD);
        logo.setTextColor(BLUE);
        logo.setBackground(cardBg(Color.rgb(12, 16, 28), PURPLE, dp(26), 1));
        LinearLayout.LayoutParams lpLogo = new LinearLayout.LayoutParams(dp(116), dp(116));
        root.addView(logo, lpLogo);

        TextView title = new TextView(this);
        title.setText("HubSyncBr");
        title.setGravity(Gravity.CENTER);
        title.setTextColor(TEXT);
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams lpTitle = new LinearLayout.LayoutParams(-1, dp(48));
        lpTitle.setMargins(0, dp(22), 0, 0);
        root.addView(title, lpTitle);

        TextView sub = new TextView(this);
        sub.setText("Watch more. Sync more.");
        sub.setGravity(Gravity.CENTER);
        sub.setTextColor(MUTED);
        sub.setTextSize(14);
        root.addView(sub, new LinearLayout.LayoutParams(-1, dp(30)));

        TextView loading = new TextView(this);
        loading.setText("Preparando seu workspace...");
        loading.setGravity(Gravity.CENTER);
        loading.setTextColor(BLUE);
        loading.setTextSize(12);
        LinearLayout.LayoutParams lpLoad = new LinearLayout.LayoutParams(-1, dp(34));
        lpLoad.setMargins(0, dp(12), 0, 0);
        root.addView(loading, lpLoad);

        ViewCompatFade(root);
        setContentView(root);
    }

    private void ViewCompatFade(LinearLayout root) {
        AnimationSet set = new AnimationSet(true);
        AlphaAnimation fade = new AlphaAnimation(0f, 1f);
        fade.setDuration(620);
        ScaleAnimation scale = new ScaleAnimation(0.96f, 1f, 0.96f, 1f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
        scale.setDuration(620);
        set.addAnimation(fade);
        set.addAnimation(scale);
        root.startAnimation(set);
    }

    private GradientDrawable cardBg(int fill, int stroke, int radius, int strokeWidth) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(fill);
        bg.setCornerRadius(radius);
        if (strokeWidth > 0) bg.setStroke(dp(strokeWidth), stroke);
        return bg;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
