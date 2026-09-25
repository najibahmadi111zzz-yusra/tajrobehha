package com.tajro.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.Context;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;

import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class ExperienceListActivity extends Activity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private LinearLayout layout;

    /*
     * برای جلوگیری از نمایش یک تجربه تکراری
     */
    private Set<String> displayedExperienceKeys =
            new HashSet<>();

    // ================================
    // اعمال زبان
    // ================================

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(
                LanguageManager.applyLanguage(newBase)
        );
    }

    // ================================
    // ترجمه
    // ================================

    private String text(String fa) {

        String lang =
                LanguageManager.getLanguage(this);

        // English
        if ("en".equals(lang)) {

            if (fa.equals("📚 تجربه‌های کاربران"))
                return "📚 Users' Experiences";

            if (fa.equals("هنوز تجربه‌ای منتشر نشده است.\n\nاولین تجربه خود را ثبت کنید! ✍️"))
                return "No experiences have been published yet.\n\nShare your first experience! ✍️";

            if (fa.equals("بدون عنوان"))
                return "No title";

            if (fa.equals("کاربر"))
                return "User";

            if (fa.equals("برای لایک کردن وارد حساب شوید"))
                return "Please log in to like";

            if (fa.equals("✏️ ویرایش"))
                return "✏️ Edit";

            if (fa.equals("🗑️ حذف"))
                return "🗑️ Delete";

            if (fa.equals("مطمئن هستید این تجربه حذف شود؟"))
                return "Are you sure you want to delete this experience?";

            if (fa.equals("لغو"))
                return "Cancel";

            if (fa.equals("حذف"))
                return "Delete";

            if (fa.equals("تجربه حذف شد 🗑️"))
                return "Experience deleted 🗑️";

            if (fa.equals("خطا در حذف تجربه"))
                return "Error deleting experience";

            if (fa.equals("خطا در دریافت تجربه‌ها"))
                return "Error loading experiences";

            if (fa.equals("خطا در برداشتن لایک"))
                return "Error removing like";

            if (fa.equals("خطا در ثبت لایک"))
                return "Error adding like";

            if (fa.equals("خطا در بررسی لایک"))
                return "Error checking like";
        }

        // پښتو
        if ("ps".equals(lang)) {

            if (fa.equals("📚 تجربه‌های کاربران"))
                return "📚 د کاروونکو تجربې";

            if (fa.equals("هنوز تجربه‌ای منتشر نشده است.\n\nاولین تجربه خود را ثبت کنید! ✍️"))
                return "تر اوسه هېڅ تجربه نه ده خپره شوې.\n\nخپله لومړۍ تجربه ثبت کړئ! ✍️";

            if (fa.equals("بدون عنوان"))
                return "بې سرلیک";

            if (fa.equals("کاربر"))
                return "کارن";

            if (fa.equals("برای لایک کردن وارد حساب شوید"))
                return "د خوښولو لپاره خپل حساب ته ننوځئ";

            if (fa.equals("✏️ ویرایش"))
                return "✏️ سمون";

            if (fa.equals("🗑️ حذف"))
                return "🗑️ ړنګول";

            if (fa.equals("مطمئن هستید این تجربه حذف شود؟"))
                return "ایا ډاډه یاست چې دا تجربه ړنګه شي؟";

            if (fa.equals("لغو"))
                return "لغوه";

            if (fa.equals("حذف"))
                return "ړنګول";

            if (fa.equals("تجربه حذف شد 🗑️"))
                return "تجربه ړنګه شوه 🗑️";

            if (fa.equals("خطا در حذف تجربه"))
                return "د تجربې په ړنګولو کې تېروتنه";

            if (fa.equals("خطا در دریافت تجربه‌ها"))
                return "د تجربو په ترلاسه کولو کې تېروتنه";

            if (fa.equals("خطا در برداشتن لایک"))
                return "د خوښونې په لرې کولو کې تېروتنه";

            if (fa.equals("خطا در ثبت لایک"))
                return "د خوښونې په ثبتولو کې تېروتنه";

            if (fa.equals("خطا در بررسی لایک"))
                return "د خوښونې په کتلو کې تېروتنه";
        }

        // اردو
        if ("ur".equals(lang)) {

            if (fa.equals("📚 تجربه‌های کاربران"))
                return "📚 صارفین کے تجربات";

            if (fa.equals("هنوز تجربه‌ای منتشر نشده است.\n\nاولین تجربه خود را ثبت کنید! ✍️"))
                return "ابھی تک کوئی تجربہ شائع نہیں ہوا۔\n\nاپنا پہلا تجربہ شیئر کریں! ✍️";

            if (fa.equals("بدون عنوان"))
                return "بغیر عنوان";

            if (fa.equals("کاربر"))
                return "صارف";

            if (fa.equals("برای لایک کردن وارد حساب شوید"))
                return "لائک کرنے کے لیے اپنے اکاؤنٹ میں لاگ اِن کریں";

            if (fa.equals("✏️ ویرایش"))
                return "✏️ ترمیم";

            if (fa.equals("🗑️ حذف"))
                return "🗑️ حذف";

            if (fa.equals("مطمئن هستید این تجربه حذف شود؟"))
                return "کیا آپ واقعی یہ تجربہ حذف کرنا چاہتے ہیں؟";

            if (fa.equals("لغو"))
                return "منسوخ";

            if (fa.equals("حذف"))
                return "حذف";

            if (fa.equals("تجربه حذف شد 🗑️"))
                return "تجربہ حذف ہوگیا 🗑️";

            if (fa.equals("خطا در حذف تجربه"))
                return "تجربہ حذف کرنے میں خرابی";

            if (fa.equals("خطا در دریافت تجربه‌ها"))
                return "تجربات حاصل کرنے میں خرابی";

            if (fa.equals("خطا در برداشتن لایک"))
                return "لائک ہٹانے میں خرابی";

            if (fa.equals("خطا در ثبت لایک"))
                return "لائک کرنے میں خرابی";

            if (fa.equals("خطا در بررسی لایک"))
                return "لائک چیک کرنے میں خرابی";
        }

        // हिन्दी
        if ("hi".equals(lang)) {

            if (fa.equals("📚 تجربه‌های کاربران"))
                return "📚 उपयोगकर्ताओं के अनुभव";

            if (fa.equals("هنوز تجربه‌ای منتشر نشده است.\n\nاولین تجربه خود را ثبت کنید! ✍️"))
                return "अभी तक कोई अनुभव प्रकाशित नहीं हुआ है।\n\nअपना पहला अनुभव साझा करें! ✍️";

            if (fa.equals("بدون عنوان"))
                return "बिना शीर्षक";

            if (fa.equals("کاربر"))
                return "उपयोगकर्ता";

            if (fa.equals("برای لایک کردن وارد حساب شوید"))
                return "लाइक करने के लिए अपने खाते में लॉग इन करें";

            if (fa.equals("✏️ ویرایش"))
                return "✏️ संपादित करें";

            if (fa.equals("🗑️ حذف"))
                return "🗑️ हटाएँ";

            if (fa.equals("مطمئن هستید این تجربه حذف شود؟"))
                return "क्या आप वाकई इस अनुभव को हटाना चाहते हैं?";

            if (fa.equals("لغو"))
                return "रद्द करें";

            if (fa.equals("حذف"))
                return "हटाएँ";

            if (fa.equals("تجربه حذف شد 🗑️"))
                return "अनुभव हटा दिया गया 🗑️";

            if (fa.equals("خطا در حذف تجربه"))
                return "अनुभव हटाने में त्रुटि";

            if (fa.equals("خطا در دریافت تجربه‌ها"))
                return "अनुभव प्राप्त करने में त्रुटि";

            if (fa.equals("خطا در برداشتن لایک"))
                return "लाइक हटाने में त्रुटि";

            if (fa.equals("خطا در ثبت لایک"))
                return "लाइक करने में त्रुटि";

            if (fa.equals("خطا در بررسی لایک"))
                return "लाइक जाँचने में त्रुटि";
        }

        return fa;
    }

    // ================================
    // اندازه
    // ================================

    private int dp(int value) {

        return (int) (
                value *
                        getResources()
                                .getDisplayMetrics()
                                .density
        );
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        ScrollView scrollView =
                new ScrollView(this);

        layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setPadding(
                dp(16),
                dp(24),
                dp(16),
                dp(30)
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

        TextView title =
                new TextView(this);

        title.setText(
                text("📚 تجربه‌های کاربران")
        );

        title.setTextSize(27);

        title.setTextColor(
                Color.rgb(8, 65, 90)
        );

        title.setTypeface(
                Typeface.create(
                        "sans-serif",
                        Typeface.BOLD
                )
        );

        title.setGravity(
                Gravity.CENTER
        );

        title.setPadding(
                0,
                0,
                0,
                dp(22)
        );

        layout.addView(title);

        setContentView(scrollView);

        loadExperiences();
    }

    private void loadExperiences() {

        displayedExperienceKeys.clear();

        db.collection("experiences")
                .orderBy(
                        "timestamp",
                        Query.Direction.DESCENDING
                )
                .get()
                .addOnSuccessListener(
                        queryDocumentSnapshots -> {

                            layout.removeViews(
                                    1,
                                    Math.max(
                                            0,
                                            layout.getChildCount() - 1
                                    )
                            );

                            displayedExperienceKeys.clear();

                            if (queryDocumentSnapshots.isEmpty()) {

                                showEmptyMessage();

                                return;
                            }

                            FirebaseUser currentUser =
                                    auth.getCurrentUser();

                            String currentUserId =
                                    null;

                            if (currentUser != null) {

                                currentUserId =
                                        currentUser.getUid();
                            }

                            int displayedCount = 0;

                            for (
                                    DocumentSnapshot document :
                                    queryDocumentSnapshots
                            ) {

                                String uniqueKey =
                                        getExperienceUniqueKey(
                                                document
                                        );

                                if (displayedExperienceKeys
                                        .contains(uniqueKey)) {

                                    continue;
                                }

                                displayedExperienceKeys
                                        .add(uniqueKey);

                                createExperienceCard(
                                        document,
                                        currentUserId
                                );

                                displayedCount++;
                            }

                            if (displayedCount == 0) {

                                showEmptyMessage();
                            }
                        }
                )
                .addOnFailureListener(
                        e -> {

                            Toast.makeText(
                                    ExperienceListActivity.this,
                                    text(
                                            "خطا در دریافت تجربه‌ها"
                                    ),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                );
    }

    private String getExperienceUniqueKey(
            DocumentSnapshot document
    ) {

        String contentKey =
                document.getString(
                        "contentKey"
                );

        if (contentKey != null &&
                !contentKey.trim().isEmpty()) {

            return contentKey;
        }

        String userId =
                document.getString("userId");

        String title =
                document.getString("title");

        String text =
                document.getString("text");

        if (userId == null) {
            userId = "";
        }

        if (title == null) {
            title = "";
        }

        if (text == null) {
            text = "";
        }

        String value =
                userId +
                        "|" +
                        normalizeText(title) +
                        "|" +
                        normalizeText(text);

        return Integer.toHexString(
                value.hashCode()
        );
    }

    private String normalizeText(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(
                        java.util.Locale.ROOT
                );
    }

    private void showEmptyMessage() {

        TextView empty =
                new TextView(this);

        empty.setText(
                text(
                        "هنوز تجربه‌ای منتشر نشده است.\n\n" +
                                "اولین تجربه خود را ثبت کنید! ✍️"
                )
        );

        empty.setTextSize(18);

        empty.setTextColor(
                Color.rgb(80, 90, 95)
        );

        empty.setTypeface(
                Typeface.create(
                        "sans-serif",
                        Typeface.NORMAL
                )
        );

        empty.setGravity(
                Gravity.CENTER
        );

        empty.setPadding(
                dp(20),
                dp(45),
                dp(20),
                dp(45)
        );

        layout.addView(empty);
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

        if (experienceTitle == null ||
                experienceTitle.trim().isEmpty()) {

            experienceTitle =
                    text("بدون عنوان");
        }

        if (experienceText == null) {
            experienceText = "";
        }

        if (authorEmail == null ||
                authorEmail.trim().isEmpty()) {

            authorEmail =
                    text("کاربر");
        }

        String documentId =
                document.getId();

        LinearLayout card =
                new LinearLayout(this);

        card.setOrientation(
                LinearLayout.VERTICAL
        );

        card.setPadding(
                dp(17),
                dp(16),
                dp(17),
                dp(11)
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

        authorName.setTextSize(16);

        authorName.setTextColor(
                Color.rgb(8, 65, 90)
        );

        authorName.setTypeface(
                Typeface.create(
                        "sans-serif-medium",
                        Typeface.NORMAL
                )
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
                        dp(54),
                        dp(54)
                )
        );

        authorRow.addView(
                authorName,
                new LinearLayout.LayoutParams(
                        0,
                        dp(54),
                        1
                )
        );

        card.addView(authorRow);

        if (userId != null &&
                !userId.isEmpty()) {

            loadUserProfile(
                    userId,
                    authorName,
                    profileImage,
                    authorEmail
            );
        }

        TextView experienceTitleView =
                new TextView(this);

        experienceTitleView.setText(
                experienceTitle
        );

        experienceTitleView.setTextSize(19);

        experienceTitleView.setTextColor(
                Color.rgb(8, 65, 90)
        );

        experienceTitleView.setTypeface(
                Typeface.create(
                        "sans-serif-medium",
                        Typeface.NORMAL
                )
        );

        experienceTitleView.setGravity(
                Gravity.RIGHT
        );

        experienceTitleView.setPadding(
                dp(5),
                dp(17),
                dp(5),
                dp(3)
        );

        card.addView(
                experienceTitleView
        );

        TextView experience =
                new TextView(this);

        experience.setText(
                experienceText
        );

        experience.setTextSize(17);

        experience.setTextColor(
                Color.rgb(45, 50, 53)
        );

        experience.setTypeface(
                Typeface.create(
                        "sans-serif",
                        Typeface.NORMAL
                )
        );

        experience.setGravity(
                Gravity.RIGHT
        );

        experience.setLineSpacing(
                dp(3),
                1.05f
        );

        experience.setPadding(
                dp(5),
                dp(5),
                dp(5),
                dp(12)
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
                dp(3),
                dp(2),
                dp(3),
                dp(5)
        );

        TextView likeButton =
                new TextView(this);

        likeButton.setText("♡");
        likeButton.setTextSize(28);

        likeButton.setTextColor(
                Color.rgb(210, 40, 60)
        );

        likeButton.setGravity(
                Gravity.CENTER
        );

        likeButton.setPadding(
                0,
                0,
                0,
                0
        );

        likeButton.setClickable(true);

        TextView likeCount =
                new TextView(this);

        likeCount.setText("0");

        likeCount.setTextSize(13);

        likeCount.setTextColor(
                Color.rgb(100, 100, 100)
        );

        likeCount.setTypeface(
                Typeface.create(
                        "sans-serif-medium",
                        Typeface.NORMAL
                )
        );

        likeCount.setGravity(
                Gravity.CENTER_VERTICAL
        );

        likeCount.setPadding(
                dp(1),
                0,
                dp(10),
                0
        );

        likeRow.addView(
                likeButton,
                new LinearLayout.LayoutParams(
                        dp(40),
                        dp(40)
                )
        );

        likeRow.addView(
                likeCount,
                new LinearLayout.LayoutParams(
                        dp(35),
                        dp(40)
                )
        );

        TextView dateTime =
                new TextView(this);

        dateTime.setText(
                getExperienceDateTime(
                        document
                )
        );

        dateTime.setTextSize(10);

        dateTime.setTextColor(
                Color.rgb(135, 135, 135)
        );

        dateTime.setGravity(
                Gravity.CENTER_VERTICAL |
                        Gravity.RIGHT
        );

        dateTime.setSingleLine(true);

        LinearLayout.LayoutParams dateParams =
                new LinearLayout.LayoutParams(
                        0,
                        dp(35),
                        1
                );

        likeRow.addView(
                dateTime,
                dateParams
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
                                text(
                                        "برای لایک کردن وارد حساب شوید"
                                ),
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

            editButton.setText(
                    text("✏️ ویرایش")
            );

            Button deleteButton =
                    new Button(this);

            deleteButton.setText(
                    text("🗑️ حذف")
            );

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

            deleteButton.setOnClickListener(
                    v -> {

                        new AlertDialog.Builder(
                                ExperienceListActivity.this
                        )
                                .setMessage(
                                        text(
                                                "مطمئن هستید این تجربه حذف شود؟"
                                        )
                                )
                                .setNegativeButton(
                                        text("لغو"),
                                        null
                                )
                                .setPositiveButton(
                                        text("حذف"),
                                        (dialog, which) -> {

                                            db.collection(
                                                    "experiences"
                                            )
                                                    .document(
                                                            documentId
                                                    )
                                                    .delete()
                                                    .addOnSuccessListener(
                                                            unused -> {

                                                                Toast.makeText(
                                                                        ExperienceListActivity.this,
                                                                        text(
                                                                                "تجربه حذف شد 🗑️"
                                                                        ),
                                                                        Toast.LENGTH_SHORT
                                                                ).show();

                                                                layout.removeView(
                                                                        card
                                                                );
                                                            }
                                                    )
                                                    .addOnFailureListener(
                                                            e -> {

                                                                Toast.makeText(
                                                                        ExperienceListActivity.this,
                                                                        text(
                                                                                "خطا در حذف تجربه"
                                                                        ),
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

    private void loadLikeStatus(
            DocumentSnapshot experience,
            String currentUserId,
            TextView likeButton,
            TextView likeCount
    ) {

        DocumentReference likesReference =
                experience.getReference()
                        .collection("likes")
                        .document(
                                currentUserId == null
                                        ? "anonymous"
                                        : currentUserId
                        );

        likesReference.get()
                .addOnSuccessListener(
                        likeDocument -> {

                            boolean liked =
                                    likeDocument.exists();

                            likeButton.setText(
                                    liked
                                            ? "♥"
                                            : "♡"
                            );

                            loadLikeCount(
                                    experience,
                                    likeCount
                            );
                        }
                )
                .addOnFailureListener(
                        e -> loadLikeCount(
                                experience,
                                likeCount
                        )
                );
    }

    private void loadLikeCount(
            DocumentSnapshot experience,
            TextView likeCount
    ) {

        Long savedCount =
                experience.getLong(
                        "likesCount"
                );

        if (savedCount != null) {

            likeCount.setText(
                    formatCount(savedCount)
            );
        }

        experience.getReference()
                .collection("likes")
                .get()
                .addOnSuccessListener(
                        querySnapshot -> {

                            long count =
                                    querySnapshot.size();

                            likeCount.setText(
                                    formatCount(count)
                            );
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

        DocumentReference experienceReference =
                experience.getReference();

        DocumentReference likeReference =
                experienceReference
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

                                                    decreaseLikeCount(
                                                            experienceReference,
                                                            likeCount
                                                    );
                                                }
                                        )
                                        .addOnFailureListener(
                                                e -> {

                                                    Toast.makeText(
                                                            ExperienceListActivity.this,
                                                            text(
                                                                    "خطا در برداشتن لایک"
                                                            ),
                                                            Toast.LENGTH_SHORT
                                                    ).show();
                                                }
                                        );

                            } else {

                                java.util.HashMap<
                                        String,
                                        Object
                                        > likeData =
                                        new java.util.HashMap<>();

                                likeData.put(
                                        "userId",
                                        currentUserId
                                );

                                likeData.put(
                                        "timestamp",
                                        FieldValue.serverTimestamp()
                                );

                                likeReference
                                        .set(likeData)
                                        .addOnSuccessListener(
                                                unused -> {

                                                    likeButton.setText(
                                                            "♥"
                                                    );

                                                    increaseLikeCount(
                                                            experienceReference,
                                                            likeCount
                                                    );
                                                }
                                        )
                                        .addOnFailureListener(
                                                e -> {

                                                    Toast.makeText(
                                                            ExperienceListActivity.this,
                                                            text(
                                                                    "خطا در ثبت لایک"
                                                            ),
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
                                    text(
                                            "خطا در بررسی لایک"
                                    ),
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                );
    }

    private void increaseLikeCount(
            DocumentReference experienceReference,
            TextView likeCount
    ) {

        experienceReference
                .update(
                        "likesCount",
                        FieldValue.increment(1)
                )
                .addOnSuccessListener(
                        unused -> {

                            int oldCount =
                                    getCountFromText(
                                            likeCount
                                    );

                            likeCount.setText(
                                    formatCount(
                                            oldCount + 1
                                    )
                            );
                        }
                )
                .addOnFailureListener(
                        e -> {

                            loadLikeCountFromSubcollection(
                                    experienceReference,
                                    likeCount
                            );
                        }
                );
    }

    private void decreaseLikeCount(
            DocumentReference experienceReference,
            TextView likeCount
    ) {

        experienceReference
                .update(
                        "likesCount",
                        FieldValue.increment(-1)
                )
                .addOnSuccessListener(
                        unused -> {

                            int oldCount =
                                    getCountFromText(
                                            likeCount
                                    );

                            if (oldCount > 0) {
                                oldCount--;
                            }

                            likeCount.setText(
                                    formatCount(
                                            oldCount
                                    )
                            );
                        }
                )
                .addOnFailureListener(
                        e -> {

                            loadLikeCountFromSubcollection(
                                    experienceReference,
                                    likeCount
                            );
                        }
                );
    }

    private void loadLikeCountFromSubcollection(
            DocumentReference experienceReference,
            TextView likeCount
    ) {

        experienceReference
                .collection("likes")
                .get()
                .addOnSuccessListener(
                        snapshot -> {

                            likeCount.setText(
                                    formatCount(
                                            snapshot.size()
                                    )
                            );
                        }
                );
    }

    private String formatCount(long count) {

        return toPersianDigits(
                String.valueOf(count)
        );
    }

    private int getCountFromText(
            TextView textView
    ) {

        try {

            String value =
                    textView.getText()
                            .toString()
                            .trim();

            value =
                    value
                            .replace("۰", "0")
                            .replace("۱", "1")
                            .replace("۲", "2")
                            .replace("۳", "3")
                            .replace("۴", "4")
                            .replace("۵", "5")
                            .replace("۶", "6")
                            .replace("۷", "7")
                            .replace("۸", "8")
                            .replace("۹", "9");

            return Integer.parseInt(value);

        } catch (Exception e) {

            return 0;
        }
    }

    private String getExperienceDateTime(
            DocumentSnapshot document
    ) {

        try {

            Timestamp timestamp =
                    document.getTimestamp(
                            "timestamp"
                    );

            if (timestamp == null) {
                return "";
            }

            Date date =
                    timestamp.toDate();

            Calendar calendar =
                    Calendar.getInstance();

            calendar.setTime(date);

            int gy =
                    calendar.get(Calendar.YEAR);

            int gm =
                    calendar.get(Calendar.MONTH) + 1;

            int gd =
                    calendar.get(Calendar.DAY_OF_MONTH);

            int hour =
                    calendar.get(Calendar.HOUR_OF_DAY);

            int minute =
                    calendar.get(Calendar.MINUTE);

            int second =
                    calendar.get(Calendar.SECOND);

            int[] jalali =
                    gregorianToJalali(
                            gy,
                            gm,
                            gd
                    );

            String dateText =
                    String.format(
                            java.util.Locale.US,
                            "%04d/%02d/%02d - %02d:%02d:%02d",
                            jalali[0],
                            jalali[1],
                            jalali[2],
                            hour,
                            minute,
                            second
                    );

            return toPersianDigits(
                    dateText
            );

        } catch (Exception e) {

            return "";
        }
    }

    private int[] gregorianToJalali(
            int gy,
            int gm,
            int gd
    ) {

        int[] gDaysInMonth = {
                31, 28, 31, 30, 31, 30,
                31, 31, 30, 31, 30, 31
        };

        int[] jDaysInMonth = {
                31, 31, 31, 31, 31, 31,
                30, 30, 30, 30, 30, 29
        };

        int gy2 =
                gy - 1600;

        int gm2 =
                gm - 1;

        int gd2 =
                gd - 1;

        int gDayNo =
                365 * gy2
                        + (gy2 + 3) / 4
                        - (gy2 + 99) / 100
                        + (gy2 + 399) / 400;

        for (int i = 0; i < gm2; ++i) {

            gDayNo +=
                    gDaysInMonth[i];
        }

        if (gm2 > 1 &&
                (
                        (gy % 4 == 0 &&
                                gy % 100 != 0)
                                ||
                                (gy % 400 == 0)
                )) {

            gDayNo++;
        }

        gDayNo += gd2;

        int jDayNo =
                gDayNo - 79;

        int jNp =
                jDayNo / 12053;

        jDayNo %= 12053;

        int jy =
                979 +
                        33 * jNp +
                        4 * (jDayNo / 1461);

        jDayNo %= 1461;

        if (jDayNo >= 366) {

            jy +=
                    (jDayNo - 1) / 365;

            jDayNo =
                    (jDayNo - 1) % 365;
        }

        int jm;
        int jd;

        for (
                jm = 0;
                jm < 11 &&
                        jDayNo >=
                                jDaysInMonth[jm];
                ++jm
        ) {

            jDayNo -=
                    jDaysInMonth[jm];
        }

        jd =
                jDayNo + 1;

        return new int[]{
                jy,
                jm + 1,
                jd
        };
    }

    private String toPersianDigits(
            String value
    ) {

        return value
                .replace("0", "۰")
                .replace("1", "۱")
                .replace("2", "۲")
                .replace("3", "۳")
                .replace("4", "۴")
                .replace("5", "۵")
                .replace("6", "۶")
                .replace("7", "۷")
                .replace("8", "۸")
                .replace("9", "۹");
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
                                        "👤 " +
                                                fallbackName
                                );

                                return;
                            }

                            String name =
                                    userDocument.getString(
                                            "name"
                                    );

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

                                name =
                                        fallbackName;
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

                                photoUrl =
                                        photo1;

                            } else if (
                                    photo2 != null &&
                                            !photo2.trim().isEmpty()
                            ) {

                                photoUrl =
                                        photo2;

                            } else if (
                                    photo3 != null &&
                                            !photo3.trim().isEmpty()
                            ) {

                                photoUrl =
                                        photo3;

                            } else if (
                                    photo4 != null &&
                                            !photo4.trim().isEmpty()
                            ) {

                                photoUrl =
                                        photo4;
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
                                    () ->
                                            imageView
                                                    .setImageBitmap(
                                                            bitmap
                                                    )
                            );
                        }

                    } catch (Exception ignored) {
                    }
                }
        ).start();
    }
            }
