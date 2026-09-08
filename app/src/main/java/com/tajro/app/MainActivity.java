package com.tajro.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.graphics.Typeface;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(40, 60, 40, 60);

        TextView title = new TextView(this);
        title.setText("تجربه‌ها");
        title.setTextSize(32);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);

        TextView welcome = new TextView(this);
        welcome.setText("تجربه‌های خود را ثبت و با دیگران شریک شوید");
        welcome.setTextSize(18);
        welcome.setGravity(Gravity.CENTER);
        welcome.setPadding(0, 30, 0, 50);

        Button addButton = new Button(this);
        addButton.setText("➕ ثبت یک تجربه");

        Button listButton = new Button(this);
        listButton.setText("📚 دیدن تجربه‌ها");

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(
                    MainActivity.this,
                    AddExperienceActivity.class
                );
                startActivity(intent);
            }
        });

        listButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(
                    MainActivity.this,
                    ExperienceListActivity.class
                );
                startActivity(intent);
            }
        });

        layout.addView(title);
        layout.addView(welcome);
        layout.addView(addButton);
        layout.addView(listButton);

        setContentView(layout);
    }
}
خیلی مهم
