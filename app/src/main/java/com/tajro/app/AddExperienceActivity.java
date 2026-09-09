package com.tajro.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.Map;

public class AddExperienceActivity extends Activity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(35, 50, 35, 35);

        TextView title = new TextView(this);
        title.setText("✍️ ثبت تجربه جدید");
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 35);

        EditText experienceTitle = new EditText(this);
        experienceTitle.setHint("عنوان تجربه را بنویسید");

        EditText experienceText = new EditText(this);
        experienceText.setHint("تجربه خود را با دیگران شریک کنید...");
        experienceText.setGravity(Gravity.TOP);
        experienceText.setMinLines(6);

        Button publishButton = new Button(this);
        publishButton.setText("🚀 انتشار تجربه");

        publishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                FirebaseUser user = auth.getCurrentUser();

                if (user == null) {
                    Toast.makeText(
                            AddExperienceActivity.this,
                            "لطفاً ابتدا وارد اکانت خود شوید",
                            Toast.LENGTH_LONG
                    ).show();

                    return;
                }

                String titleText =
                        experienceTitle.getText().toString().trim();

                String experience =
                        experienceText.getText().toString().trim();

                if (titleText.isEmpty() || experience.isEmpty()) {

                    Toast.makeText(
                            AddExperienceActivity.this,
                            "لطفاً عنوان و متن تجربه را وارد کنید",
                            Toast.LENGTH_SHORT
                    ).show();

                    return;
                }

                Map<String, Object> experienceData =
                        new HashMap<>();

                experienceData.put("title", titleText);
                experienceData.put("text", experience);
                experienceData.put("userId", user.getUid());
                experienceData.put("authorEmail", user.getEmail());
                experienceData.put(
                        "timestamp",
                        FieldValue.serverTimestamp()
                );

                db.collection("experiences")
                        .add(experienceData)
                        .addOnSuccessListener(documentReference -> {

                            Toast.makeText(
                                    AddExperienceActivity.this,
                                    "تجربه با موفقیت منتشر شد! 🎉",
                                    Toast.LENGTH_LONG
                            ).show();

                            experienceTitle.setText("");
                            experienceText.setText("");

                        })
                        .addOnFailureListener(e -> {

                            Toast.makeText(
                                    AddExperienceActivity.this,
                                    "خطا در انتشار تجربه: "
                                            + e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        });
            }
        });

        layout.addView(title);
        layout.addView(experienceTitle);
        layout.addView(experienceText);
        layout.addView(publishButton);

        setContentView(layout);
    }
}
