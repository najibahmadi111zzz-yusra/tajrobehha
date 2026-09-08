package com.tajro.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.DocumentSnapshot;

public class ExperienceListActivity extends Activity {

    private FirebaseFirestore db;
    private LinearLayout layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();

        layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 50, 30, 30);

        TextView title = new TextView(this);
        title.setText("📚 تجربه‌های کاربران");
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 40);

        layout.addView(title);

        setContentView(layout);

        loadExperiences();
    }

    private void loadExperiences() {

        db.collection("experiences")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    if (queryDocumentSnapshots.isEmpty()) {

                        TextView empty = new TextView(this);
                        empty.setText(
                                "هنوز تجربه‌ای منتشر نشده است.\n\n" +
                                "شما می‌توانید اولین تجربه خود را ثبت کنید! ✍️"
                        );
                        empty.setTextSize(18);
                        empty.setGravity(Gravity.CENTER);

                        layout.addView(empty);

                        return;
                    }

                    for (DocumentSnapshot document : queryDocumentSnapshots) {

                        String experienceTitle =
                                document.getString("title");

                        String experienceText =
                                document.getString("text");

                        TextView experience = new TextView(this);

                        experience.setText(
                                "📌 " + experienceTitle +
                                "\n\n" +
                                experienceText
                        );

                        experience.setTextSize(18);
                        experience.setPadding(20, 20, 20, 30);

                        layout.addView(experience);
                    }

                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            ExperienceListActivity.this,
                            "خطا در دریافت تجربه‌ها: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }
}

بعد از جایگزینی، Save کن و دوباره از طریق Codemagic یک Build جدید بگیر.

وقتی APK جدید را نصب کردی:
ثبت تجربه → یک تجربه منتشر کن → دیدن تجربه‌ها

باید تجربه‌ای که منتشر کردی آنجا نمایش داده شود. 🚀❤️
