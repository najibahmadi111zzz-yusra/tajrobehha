package com.tajro.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.DocumentSnapshot;

public class ExperienceListActivity extends Activity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private LinearLayout layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        ScrollView scrollView = new ScrollView(this);

        layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 50, 30, 30);

        scrollView.addView(
                layout,
                new ScrollView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        TextView title = new TextView(this);
        title.setText("📚 تجربه‌های کاربران");
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 40);

        layout.addView(title);

        setContentView(scrollView);

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
                                "اولین تجربه خود را ثبت کنید!"
                        );

                        empty.setTextSize(18);
                        empty.setGravity(Gravity.CENTER);

                        layout.addView(empty);

                        return;
                    }

                    FirebaseUser currentUser =
                            auth.getCurrentUser();

                    String currentUserId = null;

                    if (currentUser != null) {
                        currentUserId = currentUser.getUid();
                    }

                    for (DocumentSnapshot document :
                            queryDocumentSnapshots) {

                        String experienceTitle =
                                document.getString("title");

                        String experienceText =
                                document.getString("text");

                        String authorEmail =
                                document.getString("authorEmail");

                        String userId =
                                document.getString("userId");

                        LinearLayout card =
                                new LinearLayout(this);

                        card.setOrientation(
                                LinearLayout.VERTICAL
                        );

                        card.setPadding(
                                20, 20, 20, 20
                        );

                        TextView experience =
                                new TextView(this);

                        experience.setText(
                                "📌 " + experienceTitle +
                                "\n\n" +
                                experienceText +
                                "\n\n👤 " +
                                (authorEmail != null
                                        ? authorEmail
                                        : "کاربر")
                        );

                        experience.setTextSize(18);

                        card.addView(experience);

                        if (currentUserId != null &&
                                userId != null &&
                                currentUserId.equals(userId)) {

                            LinearLayout buttons =
                                    new LinearLayout(this);

                            buttons.setOrientation(
                                    LinearLayout.HORIZONTAL
                            );

                            Button editButton =
                                    new Button(this);

                            editButton.setText("✏️ ویرایش");

                            Button deleteButton =
                                    new Button(this);

                            deleteButton.setText("🗑️ حذف");

                            buttons.addView(
                                    editButton,
                                    new LinearLayout.LayoutParams(
                                            0,
                                            ViewGroup.LayoutParams.WRAP_CONTENT,
                                            1
                                    )
                            );

                            buttons.addView(
                                    deleteButton,
                                    new LinearLayout.LayoutParams(
                                            0,
                                            ViewGroup.LayoutParams.WRAP_CONTENT,
                                            1
                                    )
                            );

                            card.addView(buttons);

                            String documentId =
                                    document.getId();

                            String finalTitle =
                                    experienceTitle;

                            String finalText =
                                    experienceText;

                            editButton.setOnClickListener(v -> {

                                Intent intent =
                                        new Intent(
                                                ExperienceListActivity.this,
                                                EditExperienceActivity.class
                                        );

                                intent.putExtra(
                                        "documentId",
                                        documentId
                                );

                                intent.putExtra(
                                        "title",
                                        finalTitle
                                );

                                intent.putExtra(
                                        "text",
                                        finalText
                                );

                                startActivity(intent);
                            });

                            deleteButton.setOnClickListener(v -> {

                                db.collection("experiences")
                                        .document(documentId)
                                        .delete()
                                        .addOnSuccessListener(unused -> {

                                            Toast.makeText(
                                                    ExperienceListActivity.this,
                                                    "تجربه حذف شد",
                                                    Toast.LENGTH_SHORT
                                            ).show();

                                            layout.removeView(card);
                                        })
                                        .addOnFailureListener(e -> {

                                            Toast.makeText(
                                                    ExperienceListActivity.this,
                                                    "خطا در حذف تجربه",
                                                    Toast.LENGTH_LONG
                                            ).show();
                                        });
                            });
                        }

                        layout.addView(card);
                    }
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            ExperienceListActivity.this,
                            "خطا در دریافت تجربه‌ها",
                            Toast.LENGTH_LONG
                    ).show();
                });
    }
}
