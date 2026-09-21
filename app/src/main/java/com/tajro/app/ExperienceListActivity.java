package com.tajro.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;
import android.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.DocumentSnapshot;

import java.net.URL;

public class ExperienceListActivity extends Activity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private LinearLayout layout;

    private int dp(int value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        ScrollView scrollView = new ScrollView(this);

        layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        layout.setPadding(
                dp(16),
                dp(25),
                dp(16),
                dp(25)
        );

        layout.setBackgroundColor(
                Color.rgb(235, 248, 250)
        );

        scrollView.addView(
                layout,
                new ScrollView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        TextView title = new TextView(this);

        title.setText("📚 تجربه‌های کاربران");
        title.setTextSize(28);
        title.setTextColor(
                Color.rgb(8, 65, 90)
        );

        title.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        title.setGravity(Gravity.CENTER);

        title.setPadding(
                0,
                0,
                0,
                dp(25)
        );

        layout.addView(title);

        setContentView(scrollView);

        loadExperiences();
    }

    private void loadExperiences() {

        db.collection("experiences")
                .orderBy(
                        "timestamp",
                        Query.Direction.DESCENDING
                )
                .get()
                .addOnSuccessListener(
                        queryDocumentSnapshots -> {

                            if (queryDocumentSnapshots.isEmpty()) {

                                TextView empty =
                                        new TextView(this);

                                empty.setText(
                                        "هنوز تجربه‌ای منتشر نشده است.\n\n" +
                                        "اولین تجربه خود را ثبت کنید! ✍️"
                                );

                                empty.setTextSize(19);

                                empty.setTextColor(
                                        Color.DKGRAY
                                );

                                empty.setGravity(
                                        Gravity.CENTER
                                );

                                empty.setPadding(
                                        dp(20),
                                        dp(40),
                                        dp(20),
                                        dp(40)
                                );

                                layout.addView(empty);

                                return;
                            }

                            FirebaseUser currentUser =
                                    auth.getCurrentUser();

                            String currentUserId = null;

                            if (currentUser != null) {
                                currentUserId =
                                        currentUser.getUid();
                            }

                            for (DocumentSnapshot document :
                                    queryDocumentSnapshots) {

                                createExperienceCard(
                                        document,
                                        currentUserId
                                );
                            }
                        }
                )
                .addOnFailureListener(
                        e -> {

                            Toast.makeText(
                                    ExperienceListActivity.this,
                                    "خطا در دریافت تجربه‌ها: " +
                                            e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                );
    }

    private void createExperienceCard(
            DocumentSnapshot document,
            String currentUserId
    ) {

        String experienceTitle =
                document.getString("title");

        String experienceText =
                document.getString("text");

        String authorEmail =
                document.getString("authorEmail");

        String userId =
                document.getString("userId");

        if (experienceTitle == null) {
            experienceTitle = "بدون عنوان";
        }

        if (experienceText == null) {
            experienceText = "";
        }

        if (authorEmail == null ||
                authorEmail.isEmpty()) {
            authorEmail = "کاربر";
        }

        String documentId =
                document.getId();

        LinearLayout card =
                new LinearLayout(this);

        card.setOrientation(
                LinearLayout.VERTICAL
        );

        card.setPadding(
                dp(18),
                dp(18),
                dp(18),
                dp(12)
        );

        GradientDrawable cardBackground =
                new GradientDrawable();

        cardBackground.setColor(
                Color.WHITE
        );

        cardBackground.setCornerRadius(
                dp(18)
        );

        card.setBackground(
                cardBackground
        );

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(
                0,
                0,
                0,
                dp(15)
        );

        card.setLayoutParams(cardParams);

        LinearLayout authorRow =
                new LinearLayout(this);

        authorRow.setOrientation(
                LinearLayout.HORIZONTAL
        );

        authorRow.setGravity(
                Gravity.CENTER_VERTICAL
        );

        ImageView profileImage =
                new ImageView(this);

        profileImage.setScaleType(
                ImageView.ScaleType.CENTER_CROP
        );

        GradientDrawable imageBackground =
                new GradientDrawable();

        imageBackground.setColor(
                Color.rgb(225, 240, 245)
        );

        imageBackground.setShape(
                GradientDrawable.OVAL
        );

        profileImage.setBackground(
                imageBackground
        );

        TextView authorName =
                new TextView(this);

        authorName.setText(
                "👤 " + authorEmail
        );

        authorName.setTextSize(17);

        authorName.setTextColor(
                Color.rgb(8, 65, 90)
        );

        authorName.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        authorName.setGravity(
                Gravity.CENTER_VERTICAL
        );

        authorName.setPadding(
                dp(10),
                0,
                0,
                0
        );

        authorRow.addView(
                profileImage,
                new LinearLayout.LayoutParams(
                        dp(52),
                        dp(52)
                )
        );

        authorRow.addView(
                authorName,
                new LinearLayout.LayoutParams(
                        0,
                        dp(52),
                        1
                )
        );

        card.addView(authorRow);

        if (userId != null && !userId.isEmpty()) {

            loadUserProfile(
                    userId,
                    authorName,
                    profileImage,
                    authorEmail
            );
        }

        TextView experience =
                new TextView(this);

        experience.setText(
                "📖 " +
                        experienceTitle +
                        "\n\n" +
                        experienceText
        );

        experience.setTextSize(18);

        experience.setTextColor(
                Color.DKGRAY
        );

        experience.setPadding(
                dp(5),
                dp(15),
                dp(5),
                dp(10)
        );

        card.addView(experience);

        LinearLayout likeRow =
                new LinearLayout(this);

        likeRow.setOrientation(
                LinearLayout.HORIZONTAL
        );

        likeRow.setGravity(
                Gravity.CENTER_VERTICAL
        );

        likeRow.setPadding(
                dp(5),
                dp(4),
                dp(5),
                dp(8)
        );

        TextView likeButton =
                new TextView(this);

        likeButton.setText("♡");
        likeButton.setTextSize(30);
        likeButton.setTextColor(
                Color.rgb(210, 40, 60)
        );
        likeButton.setGravity(Gravity.CENTER);
        likeButton.setPadding(
                dp(4),
                0,
                dp(4),
                0
        );
        likeButton.setClickable(true);

        TextView likeCount =
                new TextView(this);

        likeCount.setText("0");
        likeCount.setTextSize(14);
        likeCount.setTextColor(Color.GRAY);
        likeCount.setGravity(Gravity.CENTER_VERTICAL);
        likeCount.setPadding(
                dp(3),
                0,
                dp(12),
                0
        );

        likeRow.addView(
                likeButton,
                new LinearLayout.LayoutParams(
                        dp(45),
                        dp(45)
                )
        );

        likeRow.addView(
                likeCount,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        dp(45)
                )
        );

        card.addView(likeRow);

        loadLikeStatus(
                document,
                currentUserId,
                likeButton,
                likeCount
        );

        likeButton.setOnClickListener(
                v -> {

                    if (currentUserId == null) {

                        Toast.makeText(
                                ExperienceListActivity.this,
                                "برای لایک کردن وارد حساب شوید",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    toggleLike(
                            document,
                            currentUserId,
                            likeButton,
                            likeCount
                    );
                }
        );

        boolean isMyExperience =
                currentUserId != null &&
                userId != null &&
                currentUserId.equals(userId);

        if (isMyExperience) {

            LinearLayout buttons =
                    new LinearLayout(this);

            buttons.setOrientation(
                    LinearLayout.HORIZONTAL
            );

            buttons.setGravity(
                    Gravity.CENTER
            );

            Button editButton =
                    new Button(this);

            editButton.setText("✏️ ویرایش");
            editButton.setTextSize(12);
            editButton.setMinHeight(0);
            editButton.setMinimumHeight(0);
            editButton.setPadding(
                    dp(4),
                    0,
                    dp(4),
                    0
            );

            Button deleteButton =
                    new Button(this);

            deleteButton.setText("🗑️ حذف");
            deleteButton.setTextSize(12);
            deleteButton.setMinHeight(0);
            deleteButton.setMinimumHeight(0);
            deleteButton.setPadding(
                    dp(4),
                    0,
                    dp(4),
                    0
            );

            buttons.addView(
                    editButton,
                    new LinearLayout.LayoutParams(
                            0,
                            dp(42),
                            1
                    )
            );

            buttons.addView(
                    deleteButton,
                    new LinearLayout.LayoutParams(
                            0,
                            dp(42),
                            1
                    )
            );

            card.addView(buttons);

            String finalTitle =
                    experienceTitle;

            String finalText =
                    experienceText;

            editButton.setOnClickListener(
                    v -> {

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
                    }
            );

            /*
             * حذف دو مرحله‌ای واقعی
             *
             * مرحله اول:
             * فقط دکمه برای حذف آماده می‌شود.
             *
             * مرحله دوم:
             * پیام تأیید نمایش داده می‌شود.
             */
            final boolean[] deleteArmed =
                    {false};

            deleteButton.setOnClickListener(
                    v -> {

                        if (!deleteArmed[0]) {

                            deleteArmed[0] = true;

                            deleteButton.setText(
                                    "⚠️ تأیید حذف"
                            );

                            Toast.makeText(
                                    ExperienceListActivity.this,
                                    "برای حذف، دوباره بزنید",
                                    Toast.LENGTH_SHORT
                            ).show();

                            return;
                        }

                        new AlertDialog.Builder(
                                ExperienceListActivity.this
                        )
                                .setMessage(
                                        "مطمئن هستید حذف شود؟"
                                )
                                .setNegativeButton(
                                        "لغو",
                                        (dialog, which) -> {

                                            deleteArmed[0] = false;

                                            deleteButton.setText(
                                                    "🗑️ حذف"
                                            );
                                        }
                                )
                                .setPositiveButton(
                                        "حذف",
                                        (dialog, which) -> {

                                            db.collection("experiences")
                                                    .document(documentId)
                                                    .delete()
                                                    .addOnSuccessListener(
                                                            unused -> {

                                                                Toast.makeText(
                                                                        ExperienceListActivity.this,
                                                                        "تجربه حذف شد 🗑️",
                                                                        Toast.LENGTH_SHORT
                                                                ).show();

                                                                layout.removeView(
                                                                        card
                                                                );
                                                            }
                                                    )
                                                    .addOnFailureListener(
                                                            e -> {

                                                                deleteArmed[0] =
                                                                        false;

                                                                deleteButton.setText(
                                                                        "🗑️ حذف"
                                                                );

                                                                Toast.makeText(
                                                                        ExperienceListActivity.this,
                                                                        "خطا در حذف تجربه",
                                                                        Toast.LENGTH_LONG
                                                                ).show();
                                                            }
                                                    );
                                        }
                                )
                                .show();
                    }
            );
        }

        layout.addView(card);
    }

    private void loadUserProfile(
            String userId,
            TextView authorName,
            ImageView profileImage,
            String fallbackName
    ) {

        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(
                        userDocument -> {

                            if (!userDocument.exists()) {
                                authorName.setText(
                                        "👤 " + fallbackName
                                );
                                return;
                            }

                            String name =
                                    userDocument.getString("name");

                            if (name == null ||
                                    name.trim().isEmpty()) {

                                name =
                                        userDocument.getString(
                                                "username"
                                        );
                            }

                            if (name == null ||
                                    name.trim().isEmpty()) {

                                name =
                                        userDocument.getString(
                                                "displayName"
                                        );
                            }

                            if (name == null ||
                                    name.trim().isEmpty()) {

                                name = fallbackName;
                            }

                            authorName.setText(
                                    "👤 " + name
                            );

                            String photoUrl = null;

                            String photo1 =
                                    userDocument.getString(
                                            "photoUrl"
                                    );

                            String photo2 =
                                    userDocument.getString(
                                            "profilePhoto"
                                    );

                            String photo3 =
                                    userDocument.getString(
                                            "profilePhotoUrl"
                                    );

                            String photo4 =
                                    userDocument.getString(
                                            "photo"
                                    );

                            if (photo1 != null &&
                                    !photo1.trim().isEmpty()) {

                                photoUrl = photo1;

                            } else if (photo2 != null &&
                                    !photo2.trim().isEmpty()) {

                                photoUrl = photo2;

                            } else if (photo3 != null &&
                                    !photo3.trim().isEmpty()) {

                                photoUrl = photo3;

                            } else if (photo4 != null &&
                                    !photo4.trim().isEmpty()) {

                                photoUrl = photo4;
                            }

                            if (photoUrl != null) {

                                loadProfileImage(
                                        photoUrl,
                                        profileImage
                                );
                            }
                        }
                );
    }

    private void loadProfileImage(
            String photoUrl,
            ImageView imageView
    ) {

        new Thread(
                () -> {

                    try {

                        URL url =
                                new URL(photoUrl);

                        Bitmap bitmap =
                                BitmapFactory
                                        .decodeStream(
                                                url.openConnection()
                                                        .getInputStream()
                                        );

                        if (bitmap != null) {

                            runOnUiThread(
                                    () -> imageView.setImageBitmap(
                                            bitmap
                                    )
                            );
                        }

                    } catch (Exception ignored) {
                    }

                }
        ).start();
    }

    private void loadLikeStatus(
            DocumentSnapshot experience,
            String currentUserId,
            TextView likeButton,
            TextView likeCount
    ) {

        if (currentUserId == null) {

            loadLikeCount(
                    experience,
                    likeButton,
                    likeCount,
                    false
            );

            return;
        }

        experience.getReference()
                .collection("likes")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(
                        likeDocument -> {

                            boolean liked =
                                    likeDocument.exists();

                            if (liked) {
                                likeButton.setText("♥");
                            } else {
                                likeButton.setText("♡");
                            }

                            loadLikeCount(
                                    experience,
                                    likeButton,
                                    likeCount,
                                    liked
                            );
                        }
                )
                .addOnFailureListener(
                        e -> {

                            loadLikeCount(
                                    experience,
                                    likeButton,
                                    likeCount,
                                    false
                            );
                        }
                );
    }

    private void loadLikeCount(
            DocumentSnapshot experience,
            TextView likeButton,
            TextView likeCount,
            boolean liked
    ) {

        experience.getReference()
                .collection("likes")
                .get()
                .addOnSuccessListener(
                        querySnapshot -> {

                            likeCount.setText(
                                    String.valueOf(
                                            querySnapshot.size()
                                    )
                            );

                            if (liked) {
                                likeButton.setText("♥");
                            } else {
                                likeButton.setText("♡");
                            }
                        }
                );
    }

    private void toggleLike(
            DocumentSnapshot experience,
            String currentUserId,
            TextView likeButton,
            TextView likeCount
    ) {

        if (currentUserId == null) {
            return;
        }

        com.google.firebase.firestore.DocumentReference
                likeReference =
                experience.getReference()
                        .collection("likes")
                        .document(currentUserId);

        likeReference.get()
                .addOnSuccessListener(
                        likeDocument -> {

                            if (likeDocument.exists()) {

                                likeReference
                                        .delete()
                                        .addOnSuccessListener(
                                                unused -> {

                                                    likeButton.setText(
                                                            "♡"
                                                    );

                                                    int oldCount =
                                                            getCountFromText(
                                                                    likeCount
                                                            );

                                                    if (oldCount > 0) {
                                                        oldCount--;
                                                    }

                                                    likeCount.setText(
                                                            String.valueOf(
                                                                    oldCount
                                                            )
                                                    );
                                                }
                                        )
                                        .addOnFailureListener(
                                                e -> {

                                                    Toast.makeText(
                                                            ExperienceListActivity.this,
                                                            "خطا در برداشتن لایک",
                                                            Toast.LENGTH_SHORT
                                                    ).show();
                                                }
                                        );

                            } else {

                                java.util.HashMap<String, Object>
                                        likeData =
                                        new java.util.HashMap<>();

                                likeData.put(
                                        "userId",
                                        currentUserId
                                );

                                likeData.put(
                                        "timestamp",
                                        com.google.firebase.firestore.FieldValue.serverTimestamp()
                                );

                                likeReference
                                        .set(likeData)
                                        .addOnSuccessListener(
                                                unused -> {

                                                    likeButton.setText(
                                                            "♥"
                                                    );

                                                    int oldCount =
                                                            getCountFromText(
                                                                    likeCount
                                                            );

                                                    oldCount++;

                                                    likeCount.setText(
                                                            String.valueOf(
                                                                    oldCount
                                                            )
                                                    );
                                                }
                                        )
                                        .addOnFailureListener(
                                                e -> {

                                                    Toast.makeText(
                                                            ExperienceListActivity.this,
                                                            "خطا در ثبت لایک",
                                                            Toast.LENGTH_SHORT
                                                    ).show();
                                                }
                                        );
                            }
                        }
                )
                .addOnFailureListener(
                        e -> {

                            Toast.makeText(
                                    ExperienceListActivity.this,
                                    "خطا در بررسی لایک",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                );
    }

    private int getCountFromText(
            TextView textView
    ) {

        try {

            return Integer.parseInt(
                    textView.getText()
                            .toString()
                            .trim()
            );

        } catch (Exception e) {

            return 0;
        }
    }
    }
