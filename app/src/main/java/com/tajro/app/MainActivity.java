package com.tajro.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private int dp(int value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density);
    }

    private Button createButton(String text) {

        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(16);
        button.setTextColor(Color.WHITE);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.rgb(12, 91, 120));
        background.setCornerRadius(dp(18));

        button.setBackground(background);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        0,
                        dp(65),
                        1
                );

        params.setMargins(dp(6), dp(6), dp(6), dp(6));

        button.setLayoutParams(params);

        return button;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scrollView = new ScrollView(this);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(35), dp(20), dp(25));

        GradientDrawable pageBackground =
                new GradientDrawable();

        pageBackground.setColor(
                Color.rgb(235, 248, 250)
        );

        layout.setBackground(pageBackground);

        scrollView.addView(
                layout,
                new ScrollView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        TextView logo = new TextView(this);
        logo.setText("📖💡");
        logo.setTextSize(52);
        logo.setGravity(Gravity.CENTER);

        TextView title = new TextView(this);
        title.setText("تجربه‌ها");
        title.setTextSize(34);
        title.setTextColor(Color.rgb(8, 65, 90));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(5), 0, dp(8));

        TextView welcome = new TextView(this);
        welcome.setText(
                "تجربه‌های خود را ثبت کنید و با دیگران شریک شوید"
        );
        welcome.setTextSize(17);
        welcome.setTextColor(Color.DKGRAY);
        welcome.setGravity(Gravity.CENTER);
        welcome.setPadding(
                dp(10),
                dp(5),
                dp(10),
                dp(30)
        );

        layout.addView(logo);
        layout.addView(title);
        layout.addView(welcome);

        /* ردیف اول */

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);

        Button addButton =
                createButton("✍️ ثبت تجربه");

        Button listButton =
                createButton("📚 دیدن تجربه‌ها");

        row1.addView(addButton);
        row1.addView(listButton);

        /* ردیف دوم */

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);

        Button chatButton =
                createButton("💬 چت");

        Button voiceButton =
                createButton("🎤 پیام صوتی");

        row2.addView(chatButton);
        row2.addView(voiceButton);

        /* ردیف سوم */

        LinearLayout row3 = new LinearLayout(this);
        row3.setOrientation(LinearLayout.HORIZONTAL);

        Button aiButton =
                createButton("🤖 دستیار هوشمند");

        Button accountButton =
                createButton("👤 اکانت من");

        row3.addView(aiButton);
        row3.addView(accountButton);

        /* ردیف چهارم */

        LinearLayout row4 = new LinearLayout(this);
        row4.setOrientation(LinearLayout.HORIZONTAL);

        Button settingsButton =
                createButton("⚙️ تنظیمات");

        Button aboutButton =
                createButton("ℹ️ درباره برنامه");

        row4.addView(settingsButton);
        row4.addView(aboutButton);

        layout.addView(row1);
        layout.addView(row2);
        layout.addView(row3);
        layout.addView(row4);

        TextView footer = new TextView(this);
        footer.setText("تجربه‌ها • یاد بگیر • شریک کن");
        footer.setTextSize(14);
        footer.setTextColor(Color.GRAY);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(30), 0, dp(10));

        layout.addView(footer);

        /* ثبت تجربه */

        addButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        Intent intent = new Intent(
                                MainActivity.this,
                                AddExperienceActivity.class
                        );

                        startActivity(intent);
                    }
                }
        );

        /* دیدن تجربه‌ها */

        listButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        Intent intent = new Intent(
                                MainActivity.this,
                                ExperienceListActivity.class
                        );

                        startActivity(intent);
                    }
                }
        );

        /* گزینه‌های آماده برای مرحله بعد */

        chatButton.setOnClickListener(v ->
                Toast.makeText(
                        MainActivity.this,
                        "بخش چت به‌زودی فعال می‌شود",
                        Toast.LENGTH_SHORT
                ).show()
        );

        voiceButton.setOnClickListener(v ->
                Toast.makeText(
                        MainActivity.this,
                        "بخش پیام صوتی به‌زودی فعال می‌شود",
                        Toast.LENGTH_SHORT
                ).show()
        );

        aiButton.setOnClickListener(v ->
                Toast.makeText(
                        MainActivity.this,
                        "دستیار هوشمند در مرحله بعد فعال می‌شود",
                        Toast.LENGTH_SHORT
                ).show()
        );

        accountButton.setOnClickListener(v ->
                Toast.makeText(
                        MainActivity.this,
                        "اکانت امن را در مرحله بعد فعال می‌کنیم",
                        Toast.LENGTH_SHORT
                ).show()
        );

        settingsButton.setOnClickListener(v ->
                Toast.makeText(
                        MainActivity.this,
                        "تنظیمات به‌زودی اضافه می‌شود",
                        Toast.LENGTH_SHORT
                ).show()
        );

        aboutButton.setOnClickListener(v ->
                Toast.makeText(
                        MainActivity.this,
                        "اپلیکیشن تجربه‌ها",
                        Toast.LENGTH_SHORT
                ).show()
        );

        setContentView(scrollView);
    }
}
