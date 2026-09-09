package com.tajro.app;
import android.app.Activity;
import android.os.Bundle;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

public class EditExperienceActivity extends Activity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private EditText titleInput;
    private EditText textInput;

    private String documentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        documentId = getIntent().getStringExtra("documentId");

        String title = getIntent().getStringExtra("title");
        String text = getIntent().getStringExtra("text");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(35, 50, 35, 35);

        android.widget.TextView pageTitle =
                new android.widget.TextView(this);

        pageTitle.setText("✏️ ویرایش تجربه");
        pageTitle.setTextSize(26);
        pageTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        pageTitle.setGravity(Gravity.CENTER);
        pageTitle.setPadding(0, 0, 0, 35);

        titleInput = new EditText(this);
        titleInput.setHint("عنوان تجربه");

        textInput = new EditText(this);
        textInput.setHint("متن تجربه");
        textInput.setGravity(Gravity.TOP);
        textInput.setMinLines(7);

        if (title != null) {
            titleInput.setText(title);
        }

        if (text != null) {
            textInput.setText(text);
        }

        Button saveButton = new Button(this);
        saveButton.setText("💾 ذخیره تغییرات");

        layout.addView(pageTitle);
        layout.addView(titleInput);
        layout.addView(textInput);
        layout.addView(saveButton);

        setContentView(layout);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateExperience();
            }
        });
    }

    private void updateExperience() {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            Toast.makeText(
                    this,
                    "لطفاً ابتدا وارد اکانت خود شوید",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        if (documentId == null || documentId.isEmpty()) {
            Toast.makeText(
                    this,
                    "شناسه تجربه پیدا نشد",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        String title = titleInput.getText().toString().trim();
        String text = textInput.getText().toString().trim();

        if (title.isEmpty() || text.isEmpty()) {
            Toast.makeText(
                    this,
                    "لطفاً عنوان و متن تجربه را وارد کنید",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        db.collection("experiences")
                .document(documentId)
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) {
                        Toast.makeText(
                                EditExperienceActivity.this,
                                "این تجربه پیدا نشد",
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    String ownerId =
                            document.getString("userId");

                    if (ownerId == null ||
                            !ownerId.equals(user.getUid())) {

                        Toast.makeText(
                                EditExperienceActivity.this,
                                "شما اجازه ویرایش این تجربه را ندارید",
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    db.collection("experiences")
                            .document(documentId)
                            .update(
                                    "title", title,
                                    "text", text,
                                    "timestamp",
                                    FieldValue.serverTimestamp()
                            )
                            .addOnSuccessListener(unused -> {

                                Toast.makeText(
                                        EditExperienceActivity.this,
                                        "تجربه با موفقیت ویرایش شد ✅",
                                        Toast.LENGTH_LONG
                                ).show();

                                finish();
                            })
                            .addOnFailureListener(e -> {

                                Toast.makeText(
                                        EditExperienceActivity.this,
                                        "خطا در ذخیره تغییرات: "
                                                + e.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();
                            });
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            EditExperienceActivity.this,
                            "خطا در بررسی تجربه",
                            Toast.LENGTH_LONG
                    ).show();
                });
    }
}
