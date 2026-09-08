package com.tajro.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AddExperienceActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 50, 30, 30);

        TextView title = new TextView(this);
        title.setText("ثبت تجربه");
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 30);

        EditText experience = new EditText(this);
        experience.setHint("تجربه خود را اینجا بنویسید...");
        experience.setGravity(Gravity.TOP);
        experience.setMinLines(8);

        Button saveButton = new Button(this);
        saveButton.setText("ذخیره تجربه");

        layout.addView(title);
        layout.addView(experience);
        layout.addView(saveButton);

        setContentView(layout);
    }
}
