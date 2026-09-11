package com.tajro.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setBackgroundColor(Color.rgb(235, 248, 250));

        TextView title = new TextView(this);
        title.setText("📖 تجربه‌ها");
        title.setTextSize(32);
        title.setTextColor(Color.rgb(8, 65, 90));
        title.setGravity(Gravity.CENTER);

        TextView message = new TextView(this);
        message.setText("\nبرنامه با موفقیت اجرا شد ✅");
        message.setTextSize(20);
        message.setGravity(Gravity.CENTER);

        layout.addView(title);
        layout.addView(message);

        setContentView(layout);
    }
}
