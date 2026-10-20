package com.tajro.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.Context;
import android.content.Intent;
import android.content.ContentValues;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
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
import android.widget.EditText;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;

import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

public class ExperienceListActivity extends Activity {

    private static final String SUPABASE_URL =
            "https://gorbhuqmkjlkrklhasdh.supabase.co";
    private static final String SUPABASE_PUBLISHABLE_KEY =
            "sb_publishable_a02sM3MABB4afGU90ZBdFA_OTYG6gUs";
    private static final String PROFILE_BUCKET = "profile_photos_public";

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private LinearLayout layout;

    /*
     * جلوگیری از نمایش یک تجربه تکراری
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

            if (fa.equals("تجربه‌ها")) return "Experiences";
            if (fa.equals("📚 تجربه‌های کاربران")) return "📚 Users' Experiences";

            if (fa.equals("⋮ گزینه‌های تجربه")) return "⋮ Experience options";
            if (fa.equals("شریک‌سازی تجربه")) return "Share experience";

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

            if (fa.equals("تجربه‌ها")) return "تجربې";
            if (fa.equals("📚 تجربه‌های کاربران")) return "📚 د کاروونکو تجربې";

            if (fa.equals("⋮ گزینه‌های تجربه")) return "⋮ د تجربې اختیارونه";
            if (fa.equals("شریک‌سازی تجربه")) return "تجربه شریکول";

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

            if (fa.equals("تجربه‌ها")) return "تجربات";
            if (fa.equals("📚 تجربه‌های کاربران")) return "📚 صارفین کے تجربات";

            if (fa.equals("⋮ گزینه‌های تجربه")) return "⋮ تجربے کے اختیارات";
            if (fa.equals("شریک‌سازی تجربه")) return "تجربہ شیئر کریں";

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

            if (fa.equals("تجربه‌ها")) return "अनुभव";
            if (fa.equals("📚 تجربه‌های کاربران")) return "📚 उपयोगकर्ताओं के अनुभव";

            if (fa.equals("⋮ گزینه‌های تجربه")) return "⋮ अनुभव विकल्प";
            if (fa.equals("شریک‌سازی تجربه")) return "अनुभव साझा करें";

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

        scrollView.setFillViewport(true);

        layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setPadding(
                dp(14),
                dp(18),
                dp(14),
                dp(30)
        );

        layout.setBackgroundColor(
                Color.rgb(238, 248, 250)
        );

        scrollView.addView(
                layout,
                new ScrollView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        // ================================
        // سربرگ اختصاصی صفحه «تجربه‌ها»
        // ================================

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setGravity(Gravity.CENTER);
        header.setPadding(dp(18), dp(18), dp(18), dp(18));

        GradientDrawable headerBackground = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{
                        Color.rgb(10, 28, 72),
                        Color.rgb(17, 52, 105),
                        Color.rgb(12, 73, 101)
                });
        headerBackground.setCornerRadius(dp(24));
        headerBackground.setStroke(dp(1), Color.rgb(190, 159, 82));
        header.setBackground(headerBackground);
        header.setElevation(dp(4));

        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        headerParams.setMargins(0, 0, 0, dp(18));
        header.setLayoutParams(headerParams);

        TextView emblem = new TextView(this);
        emblem.setText("📖  💡");
        emblem.setTextSize(35);
        emblem.setGravity(Gravity.CENTER);
        emblem.setPadding(dp(8), 0, dp(8), dp(5));
        header.addView(emblem);

        TextView title = new TextView(this);
        title.setText(text("تجربه‌ها"));
        title.setTextSize(29);
        title.setTextColor(Color.WHITE);
        title.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        title.setGravity(Gravity.CENTER);
        title.setLetterSpacing(0.02f);
        title.setPadding(dp(4), dp(2), dp(4), dp(5));
        header.addView(title);

        View goldAccent = new View(this);
        GradientDrawable accentBackground = new GradientDrawable();
        accentBackground.setColor(Color.rgb(220, 184, 91));
        accentBackground.setCornerRadius(dp(3));
        goldAccent.setBackground(accentBackground);
        LinearLayout.LayoutParams accentParams = new LinearLayout.LayoutParams(dp(58), dp(3));
        accentParams.setMargins(0, dp(3), 0, dp(9));
        header.addView(goldAccent, accentParams);

        TextView subtitle = new TextView(this);
        subtitle.setText("هر تجربه یک درس و هر داستان چراغیست");
        subtitle.setTextSize(14);
        subtitle.setTextColor(Color.rgb(221, 239, 247));
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(dp(4), 0, dp(4), dp(2));
        header.addView(subtitle);

        layout.addView(header);

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

    // =========================================================
    // کارت حرفه‌ای تجربه
    // =========================================================

    private void createExperienceCard(
            DocumentSnapshot document,
            String currentUserId
    ) {

        String experienceTitle =
                document.getString("title");

        String experienceText =
                document.getString("text");

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

        String experienceImageUrl = document.getString("imageUrl");
        if (experienceImageUrl == null || experienceImageUrl.trim().isEmpty()) {
            experienceImageUrl = document.getString("photoUrl");
        }

        String documentId =
                document.getId();

        // =====================================================
        // کارت اصلی
        // =====================================================

        LinearLayout card =
                new LinearLayout(this);

        card.setOrientation(
                LinearLayout.VERTICAL
        );

        card.setPadding(
                dp(16),
                dp(15),
                dp(16),
                dp(10)
        );

        GradientDrawable cardBackground =
                new GradientDrawable();

        cardBackground.setColor(
                Color.WHITE
        );

        cardBackground.setCornerRadius(
                dp(20)
        );

        cardBackground.setStroke(
                dp(1),
                Color.rgb(220, 235, 239)
        );

        card.setBackground(
                cardBackground
        );

        card.setElevation(
                dp(3)
        );

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(
                dp(1),
                dp(3),
                dp(1),
                dp(15)
        );

        card.setLayoutParams(cardParams);

        // =====================================================
        // ردیف پروفایل
        // =====================================================

        LinearLayout authorRow =
                new LinearLayout(this);

        authorRow.setOrientation(
                LinearLayout.HORIZONTAL
        );

        authorRow.setGravity(
                Gravity.CENTER_VERTICAL
        );

        authorRow.setPadding(
                dp(2),
                dp(1),
                dp(2),
                dp(8)
        );

        // =====================================================
        // عکس پروفایل دایره‌ای
        // =====================================================

        ImageView profileImage =
                new ImageView(this);

        profileImage.setScaleType(
                ImageView.ScaleType.CENTER_CROP
        );

        GradientDrawable imageBackground =
                new GradientDrawable();

        imageBackground.setColor(
                Color.rgb(224, 241, 246)
        );

        imageBackground.setShape(
                GradientDrawable.OVAL
        );

        imageBackground.setStroke(
                dp(2),
                Color.rgb(205, 228, 234)
        );

        profileImage.setBackground(
                imageBackground
        );

        // باعث می‌شود خود تصویر نیز داخل دایره بریده شود
        profileImage.setClipToOutline(true);

        // =====================================================
        // نام کاربر
        // =====================================================

        TextView authorName =
                new TextView(this);

        authorName.setText(
                text("کاربر")
        );

        authorName.setTextSize(16);

        authorName.setTextColor(
                Color.rgb(8, 65, 90)
        );

        authorName.setTypeface(
                Typeface.create(
                        "sans-serif-medium",
                        Typeface.BOLD
                )
        );

        authorName.setGravity(
                Gravity.CENTER_VERTICAL
        );

        authorName.setSingleLine(true);

        authorName.setEllipsize(
                android.text.TextUtils.TruncateAt.END
        );

        authorName.setPadding(
                dp(11),
                0,
                dp(4),
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

        // =====================================================
        // گرفتن نام و عکس واقعی پروفایل
        // =====================================================

        if (userId != null &&
                !userId.trim().isEmpty()) {

            loadUserProfile(
                    userId,
                    authorName,
                    profileImage,
                    text("کاربر")
            );
        }

        // =====================================================
        // خط ظریف زیر مشخصات کاربر
        // =====================================================

        View separator =
                new View(this);

        separator.setBackgroundColor(
                Color.rgb(235, 242, 244)
        );

        card.addView(
                separator,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(1)
                )
        );

        // =====================================================
        // عنوان تجربه
        // =====================================================

        TextView experienceTitleView =
                new TextView(this);

        experienceTitleView.setText(
                experienceTitle
        );

        experienceTitleView.setTextSize(19);

        experienceTitleView.setTextColor(
                Color.rgb(7, 60, 82)
        );

        experienceTitleView.setTypeface(
                Typeface.create(
                        "sans-serif",
                        Typeface.BOLD
                )
        );

        experienceTitleView.setGravity(
                Gravity.RIGHT
        );

        experienceTitleView.setLineSpacing(
                dp(2),
                1.05f
        );

        experienceTitleView.setPadding(
                dp(5),
                dp(15),
                dp(5),
                dp(4)
        );

        card.addView(
                experienceTitleView
        );

        // =====================================================
        // متن تجربه
        // =====================================================

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
                dp(4),
                1.08f
        );

        experience.setPadding(
                dp(5),
                dp(5),
                dp(5),
                dp(12)
        );

        card.addView(
                experience
        );

        // تصویر تجربه (در صورت وجود)
        if (experienceImageUrl != null && !experienceImageUrl.trim().isEmpty()) {
            ImageView experienceImage = new ImageView(this);
            experienceImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
            experienceImage.setAdjustViewBounds(true);
            experienceImage.setBackgroundColor(Color.rgb(235, 242, 244));
            LinearLayout.LayoutParams experienceImageParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(300));
            experienceImageParams.setMargins(0, dp(3), 0, dp(10));
            card.addView(experienceImage, experienceImageParams);
            loadExperienceImage(experienceImageUrl, experienceImage);
            final String imageToOpen = experienceImageUrl;
            experienceImage.setOnClickListener(v -> showExperienceImage(imageToOpen));
        }

        // =====================================================
        // ردیف لایک + تاریخ
        // =====================================================

        LinearLayout likeRow =
                new LinearLayout(this);

        likeRow.setOrientation(
                LinearLayout.HORIZONTAL
        );

        likeRow.setGravity(
                Gravity.CENTER_VERTICAL
        );

        likeRow.setPadding(
                dp(2),
                dp(2),
                dp(2),
                dp(3)
        );

        // واکنش یکپارچه: لمس کوتاه لایک، نگه‌داشتن برای انتخاب لایک یا قلب
        TextView likeButton = new TextView(this);
        likeButton.setText("👍 واکنش");
        likeButton.setTextSize(14);
        likeButton.setTextColor(Color.rgb(22, 105, 177));
        likeButton.setGravity(Gravity.CENTER);
        likeButton.setPadding(dp(8), 0, dp(8), 0);
        likeButton.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        likeButton.setBackground(createReactionBackground(
                Color.rgb(239, 247, 255), Color.rgb(193, 220, 246)));
        likeRow.addView(likeButton, new LinearLayout.LayoutParams(dp(92), dp(36)));

        TextView likeCount = new TextView(this);
        likeCount.setText("۰"); likeCount.setTextSize(13);
        likeCount.setTextColor(Color.rgb(105, 105, 105));
        likeCount.setGravity(Gravity.CENTER_VERTICAL);
        likeCount.setPadding(dp(4), 0, dp(8), 0);
        likeRow.addView(likeCount, new LinearLayout.LayoutParams(dp(25), dp(36)));

        TextView heartCount = new TextView(this);
        heartCount.setText("۰"); heartCount.setTextSize(13);
        heartCount.setTextColor(Color.rgb(211, 48, 72));
        heartCount.setGravity(Gravity.CENTER_VERTICAL);
        heartCount.setPadding(dp(2), 0, dp(5), 0);
        likeRow.addView(heartCount, new LinearLayout.LayoutParams(dp(25), dp(36)));
        likeCount.setOnClickListener(v -> showReactors(document.getReference(), "all"));
        heartCount.setOnClickListener(v -> showReactors(document.getReference(), "all"));
        likeButton.setOnClickListener(v -> {
            if (currentUserId == null) {
                Toast.makeText(ExperienceListActivity.this, text("برای لایک کردن وارد حساب شوید"), Toast.LENGTH_SHORT).show();
                return;
            }
            toggleLike(document, currentUserId, likeButton, likeCount);
        });
        likeButton.setOnLongClickListener(v -> {
            new AlertDialog.Builder(ExperienceListActivity.this)
                    .setTitle("واکنش خود را انتخاب کنید")
                    .setItems(new String[]{"👍 لایک", "❤️ قلب"}, (dlg, which) -> {
                        if (currentUserId == null) {
                            Toast.makeText(ExperienceListActivity.this, text("برای لایک کردن وارد حساب شوید"), Toast.LENGTH_SHORT).show();
                        } else if (which == 0) {
                            toggleLike(document, currentUserId, likeButton, likeCount);
                        } else {
                            toggleHeart(document, currentUserId, likeButton, heartCount);
                        }
                    }).show();
            return true;
        });

        // =====================================================
        // تاریخ و ساعت
        // =====================================================

        TextView dateTime =
                new TextView(this);

        dateTime.setText(
                getExperienceDateTime(
                        document
                )
        );

        dateTime.setTextSize(10);

        dateTime.setTextColor(
                Color.rgb(135, 145, 148)
        );

        dateTime.setGravity(
                Gravity.CENTER_VERTICAL |
                        Gravity.RIGHT
        );

        dateTime.setSingleLine(true);

        dateTime.setEllipsize(
                android.text.TextUtils.TruncateAt.END
        );

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

        // وضعیت جداگانهٔ لایک و قلب
        loadLikeStatus(document, currentUserId, likeButton, likeCount);
        loadHeartStatus(document, heartCount);

        // کامنت و اشتراک‌گذاری مستقیماً روی کارت اصلی
        LinearLayout socialRow = new LinearLayout(this);
        socialRow.setOrientation(LinearLayout.HORIZONTAL);
        socialRow.setGravity(Gravity.CENTER_VERTICAL);
        socialRow.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        TextView commentsButton = new TextView(this);
        commentsButton.setText("💬 کامنت");
        commentsButton.setTextSize(14);
        commentsButton.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        commentsButton.setTextColor(Color.rgb(12, 91, 120));
        commentsButton.setGravity(Gravity.CENTER);
        commentsButton.setPadding(dp(10), dp(8), dp(10), dp(8));
        commentsButton.setBackground(createReactionBackground(
                Color.rgb(235, 248, 250), Color.rgb(167, 218, 224)));

        TextView shareButtonCard = new TextView(this);
        shareButtonCard.setText("↗ اشتراک‌گذاری");
        shareButtonCard.setTextSize(14);
        shareButtonCard.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        shareButtonCard.setTextColor(Color.rgb(113, 83, 25));
        shareButtonCard.setGravity(Gravity.CENTER);
        shareButtonCard.setPadding(dp(10), dp(8), dp(10), dp(8));
        shareButtonCard.setBackground(createReactionBackground(
                Color.rgb(255, 249, 231), Color.rgb(230, 207, 145)));

        LinearLayout.LayoutParams commentParams = new LinearLayout.LayoutParams(0, dp(44), 1);
        commentParams.setMargins(dp(3), dp(5), dp(5), dp(2));
        LinearLayout.LayoutParams shareParams = new LinearLayout.LayoutParams(0, dp(44), 1);
        shareParams.setMargins(dp(5), dp(5), dp(3), dp(2));
        socialRow.addView(commentsButton, commentParams);
        socialRow.addView(shareButtonCard, shareParams);
        card.addView(socialRow);
        loadCommentCount(document.getReference(), commentsButton);
        commentsButton.setOnClickListener(v -> showComments(document.getReference(), commentsButton));

        // نسخه‌های ثابت برای استفاده داخل lambda اشتراک‌گذاری
        final String shareTitle = experienceTitle;
        final String shareText = experienceText;
        final String shareImageUrl = experienceImageUrl;
        shareButtonCard.setOnClickListener(v -> shareExperience(
                detailSafe(shareTitle),
                detailSafe(shareText),
                shareImageUrl
        ));

        // جزئیات برای همه کاربران؛ ویرایش و حذف فقط داخل صفحه جزئیات و برای مالک
        Button detailsButton = new Button(this);
        detailsButton.setText(text("⋮ گزینه‌های تجربه"));
        detailsButton.setTextSize(13);
        LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        detailsParams.setMargins(0, dp(5), 0, 0);
        card.addView(detailsButton, detailsParams);

        final String detailTitle = experienceTitle;
        final String detailText = experienceText;
        final String detailImageUrl = experienceImageUrl;
        final boolean isMyExperience = currentUserId != null && userId != null
                && currentUserId.equals(userId);

        detailsButton.setOnClickListener(v -> showExperienceDetails(
                documentId, detailTitle, detailText, detailImageUrl,
                getExperienceDateTime(document), isMyExperience, card));

        layout.addView(card);
    }

    // =========================================================
    // جزئیات کامل تجربه + ویرایش/حذف فقط برای مالک
    // =========================================================

    private void showExperienceDetails(
            String documentId,
            String titleValue,
            String bodyValue,
            String imageUrl,
            String dateValue,
            boolean isOwner,
            View cardToRemove) {

        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(10), dp(16), dp(10));
        scroll.addView(box);

        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            ImageView image = new ImageView(this);
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            image.setAdjustViewBounds(true);
            box.addView(image, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(250)));
            loadExperienceImage(imageUrl, image);
            image.setOnClickListener(v -> showExperienceImage(imageUrl));

            // دکمهٔ ذخیرهٔ عکس در گالری، داخل جزئیات تجربه
            Button saveImageButton = new Button(this);
            saveImageButton.setText(text("⬇️ ذخیره عکس در گالری"));
            saveImageButton.setTextSize(14);
            saveImageButton.setAllCaps(false);
            saveImageButton.setTextColor(Color.WHITE);
            android.graphics.drawable.GradientDrawable saveBg =
                    new android.graphics.drawable.GradientDrawable();
            saveBg.setColor(Color.rgb(12, 125, 105));
            saveBg.setCornerRadius(dp(14));
            saveImageButton.setBackground(saveBg);
            LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
            saveParams.setMargins(0, dp(8), 0, dp(8));
            box.addView(saveImageButton, saveParams);
            saveImageButton.setOnClickListener(v -> saveExperienceImage(imageUrl));
        }

        TextView titleView = new TextView(this);
        titleView.setText(titleValue == null ? text("بدون عنوان") : titleValue);
        titleView.setTextSize(21);
        titleView.setTextColor(Color.rgb(7, 60, 82));
        titleView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        titleView.setGravity(Gravity.RIGHT);
        titleView.setPadding(0, dp(12), 0, dp(8));
        box.addView(titleView);

        TextView bodyView = new TextView(this);
        bodyView.setText(bodyValue == null ? "" : bodyValue);
        bodyView.setTextSize(17);
        bodyView.setTextColor(Color.rgb(45, 50, 53));
        bodyView.setGravity(Gravity.RIGHT);
        bodyView.setLineSpacing(dp(4), 1.08f);
        box.addView(bodyView);

        if (dateValue != null && !dateValue.isEmpty()) {
            TextView dateView = new TextView(this);
            dateView.setText(dateValue);
            dateView.setTextSize(12);
            dateView.setTextColor(Color.GRAY);
            dateView.setPadding(0, dp(12), 0, dp(4));
            box.addView(dateView);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(text("⋮ گزینه‌های تجربه"))
                .setView(scroll)
                .setNegativeButton(text("بستن"), null)
                .create();

        if (isOwner) {
            dialog.setButton(AlertDialog.BUTTON_NEUTRAL, text("✏️ ویرایش"),
                    (d, which) -> {
                        Intent intent = new Intent(ExperienceListActivity.this,
                                EditExperienceActivity.class);
                        intent.putExtra("documentId", documentId);
                        intent.putExtra("title", titleValue == null ? "" : titleValue);
                        intent.putExtra("text", bodyValue == null ? "" : bodyValue);
                        startActivity(intent);
                    });
            dialog.setButton(AlertDialog.BUTTON_POSITIVE, text("🗑️ حذف"),
                    (d, which) -> new AlertDialog.Builder(ExperienceListActivity.this)
                            .setMessage(text("مطمئن هستید این تجربه حذف شود؟"))
                            .setNegativeButton(text("لغو"), null)
                            .setPositiveButton(text("حذف"), (confirm, choice) ->
                                    db.collection("experiences").document(documentId).delete()
                                            .addOnSuccessListener(unused -> {
                                                Toast.makeText(ExperienceListActivity.this,
                                                        text("تجربه حذف شد 🗑️"), Toast.LENGTH_SHORT).show();
                                                if (cardToRemove.getParent() == layout) layout.removeView(cardToRemove);
                                            })
                                            .addOnFailureListener(e -> Toast.makeText(
                                                    ExperienceListActivity.this,
                                                    text("خطا در حذف تجربه"), Toast.LENGTH_LONG).show()))
                            .show());
        }
        dialog.show();
        if (isOwner) {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(Color.rgb(12, 91, 120));
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.rgb(190, 45, 55));
        }
    }

    private void showExperienceImage(String imageUrl) {
        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setAdjustViewBounds(true);
        image.setBackgroundColor(Color.BLACK);
        loadExperienceImage(imageUrl, image);
        int maxHeight = (int)(getResources().getDisplayMetrics().heightPixels * 0.72f);
        new AlertDialog.Builder(this)
                .setTitle(text("تصویر تجربه"))
                .setView(image)
                .setNegativeButton(text("بستن"), null)
                .setPositiveButton(text("⬇️ ذخیره در گالری"),
                        (dialog, which) -> saveExperienceImage(imageUrl))
                .show();
    }

    /**
     * دانلود عکس تجربه و ذخیرهٔ آن در پوشهٔ Pictures/Tajrobehha؛
     * در اندروید ۱۰ به بالا از MediaStore استفاده می‌شود و اجازهٔ ذخیره‌سازی
     * جداگانه لازم نیست.
     */
    private void saveExperienceImage(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            Toast.makeText(this, text("آدرس عکس موجود نیست"), Toast.LENGTH_SHORT).show();
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Toast.makeText(this,
                    text("برای ذخیره در گالری، نسخهٔ جدید برنامه را نصب کنید"),
                    Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(this, text("در حال ذخیره عکس..."), Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            Bitmap bitmap = null;
            try {
                HttpURLConnection connection =
                        (HttpURLConnection) new URL(imageUrl).openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(20000);
                connection.setInstanceFollowRedirects(true);
                try (InputStream input = connection.getInputStream()) {
                    bitmap = BitmapFactory.decodeStream(input);
                } finally {
                    connection.disconnect();
                }
                if (bitmap == null) throw new IOException("تصویر قابل خواندن نیست");

                String fileName = "Tajrobehha_" + System.currentTimeMillis() + ".jpg";
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                values.put(MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/Tajrobehha");
                values.put(MediaStore.Images.Media.IS_PENDING, 1);

                android.net.Uri imageUri = getContentResolver().insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (imageUri == null) throw new IOException("ذخیره‌سازی شروع نشد");

                try (OutputStream output = getContentResolver().openOutputStream(imageUri)) {
                    if (output == null || !bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)) {
                        getContentResolver().delete(imageUri, null, null);
                        throw new IOException("نوشتن تصویر انجام نشد");
                    }
                }
                ContentValues finished = new ContentValues();
                finished.put(MediaStore.Images.Media.IS_PENDING, 0);
                getContentResolver().update(imageUri, finished, null, null);
                runOnUiThread(() -> Toast.makeText(ExperienceListActivity.this,
                        text("عکس با موفقیت در گالری ذخیره شد 📸"), Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(ExperienceListActivity.this,
                        text("ذخیره عکس ناموفق بود؛ اتصال اینترنت را بررسی کنید"),
                        Toast.LENGTH_LONG).show());
            } finally {
                if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle();
            }
        }).start();
    }

    private void loadExperienceImage(String imageUrl, ImageView imageView) {
        new Thread(() -> {
            try {
                java.net.HttpURLConnection connection =
                        (java.net.HttpURLConnection) new URL(imageUrl).openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(20000);
                connection.setInstanceFollowRedirects(true);
                Bitmap bitmap;
                try (java.io.InputStream input = connection.getInputStream()) {
                    bitmap = BitmapFactory.decodeStream(input);
                } finally {
                    connection.disconnect();
                }
                if (bitmap != null) runOnUiThread(() -> imageView.setImageBitmap(bitmap));
            } catch (Exception ignored) { }
        }).start();
    }

    // =========================================================
    // لایک
    // =========================================================

    private String detailSafe(String value) { return value == null ? "" : value; }

    private void shareExperience(String title, String body, String imageUrl) {
        StringBuilder out = new StringBuilder();
        if (!title.trim().isEmpty()) out.append(title.trim()).append("\n\n");
        out.append(body == null ? "" : body.trim());
        if (imageUrl != null && !imageUrl.trim().isEmpty()) out.append("\n\n").append(imageUrl.trim());
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, out.toString());
        startActivity(Intent.createChooser(intent, "اشتراک‌گذاری تجربه"));
    }

    private void loadCommentCount(DocumentReference experience, TextView target) {
        experience.collection("comments").get().addOnSuccessListener(q ->
                target.setText("💬 کامنت (" + formatCount(q.size()) + ")"));
    }

    private void showComments(DocumentReference experience, TextView countView) {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(14), dp(10), dp(14), dp(8));
        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        content.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(260)));
        EditText input = new EditText(this);
        input.setHint("نظر خود را بنویسید...");
        input.setGravity(Gravity.RIGHT | Gravity.TOP);
        content.addView(input);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("کامنت‌ها")
                .setView(content).setNegativeButton("بستن", null)
                .setPositiveButton("ثبت کامنت", null).create();
        experience.collection("comments").get()
                .addOnSuccessListener(q -> {
                    list.removeAllViews();
                    if (q.isEmpty()) { TextView empty = new TextView(this); empty.setText("هنوز کامنتی ثبت نشده است."); empty.setGravity(Gravity.RIGHT); list.addView(empty); }
                    for (DocumentSnapshot d : q.getDocuments()) {
                        String uid = d.getString("userId");
                        String comment = d.getString("text");
                        TextView row = new TextView(this);
                        row.setText((comment == null ? "" : comment) + "\n");
                        row.setTextSize(15); row.setTextColor(Color.rgb(45,50,53)); row.setGravity(Gravity.RIGHT);
                        row.setPadding(0, dp(8), 0, dp(8));
                        list.addView(row);
                        if (uid != null) db.collection("users").document(uid).get().addOnSuccessListener(u -> {
                            String name = u.getString("name"); if (name == null || name.trim().isEmpty()) name = u.getString("username");
                            row.setText((name == null || name.trim().isEmpty() ? "کاربر" : name) + "\n" + (comment == null ? "" : comment));
                        });
                    }
                }).addOnFailureListener(e -> Toast.makeText(this, "بارگذاری کامنت‌ها ناموفق بود", Toast.LENGTH_SHORT).show());
        dialog.setOnShowListener(unused -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            FirebaseUser user = auth == null ? null : auth.getCurrentUser();
            String value = input.getText().toString().trim();
            if (user == null) { Toast.makeText(this, "برای ثبت کامنت وارد حساب شوید", Toast.LENGTH_SHORT).show(); return; }
            if (value.isEmpty()) { input.setError("متن کامنت را بنویسید"); return; }
            java.util.HashMap<String,Object> data = new java.util.HashMap<>();
            data.put("userId", user.getUid()); data.put("text", value); data.put("timestamp", FieldValue.serverTimestamp());
            experience.collection("comments").add(data).addOnSuccessListener(ref -> {
                loadCommentCount(experience, countView); Toast.makeText(this, "کامنت ثبت شد", Toast.LENGTH_SHORT).show(); dialog.dismiss();
            }).addOnFailureListener(e -> Toast.makeText(this, "ثبت کامنت ناموفق بود", Toast.LENGTH_SHORT).show());
        }));
        dialog.show();
    }

    private void showReactors(DocumentReference experience, String initialTab) {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(12), dp(8), dp(12), dp(8));
        LinearLayout tabs = new LinearLayout(this); tabs.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout people = new LinearLayout(this); people.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll = new ScrollView(this); scroll.addView(people);
        root.addView(tabs); root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(320)));
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("واکنش‌دهندگان").setView(root).setPositiveButton("بستن", null).create();
        final String[] selected = {"all"};
        Runnable[] render = new Runnable[1];
        render[0] = () -> {
            people.removeAllViews();
            ArrayList<String> collections = new ArrayList<>();
            if ("all".equals(selected[0]) || "likes".equals(selected[0])) collections.add("likes");
            if ("all".equals(selected[0]) || "hearts".equals(selected[0])) collections.add("hearts");
            for (String col : collections) experience.collection(col).get().addOnSuccessListener(q -> {
                for (DocumentSnapshot d : q.getDocuments()) {
                    String uid = d.getString("userId"); if (uid == null) uid = d.getId();
                    final String userId = uid;
                    LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(0, dp(6), 0, dp(6));
                    ImageView avatar = new ImageView(this); avatar.setImageResource(android.R.drawable.sym_def_app_icon);
                    row.addView(avatar, new LinearLayout.LayoutParams(dp(42), dp(42)));
                    TextView name = new TextView(this); name.setText("کاربر"); name.setTextSize(15); name.setPadding(dp(10),0,dp(10),0); name.setGravity(Gravity.CENTER_VERTICAL|Gravity.RIGHT);
                    row.addView(name, new LinearLayout.LayoutParams(0, dp(46), 1)); people.addView(row);
                    db.collection("users").document(userId).get().addOnSuccessListener(u -> {
                        String n = u.getString("name"); if (n == null || n.trim().isEmpty()) n = u.getString("username"); if (n == null || n.trim().isEmpty()) n = u.getString("displayName");
                        name.setText(("likes".equals(col) ? "👍 " : "❤️ ") + (n == null || n.trim().isEmpty() ? "کاربر" : n));
                        String photo = u.getString("photoUrl"); if (photo == null) photo = u.getString("profilePhotoUrl"); if (photo == null) photo = u.getString("profilePhoto"); if (photo == null) photo = u.getString("photo");
                        if (photo != null && !photo.trim().isEmpty()) loadProfileImage(photo, avatar);
                    });
                }
            }).addOnFailureListener(e -> Toast.makeText(this, "بارگذاری واکنش‌ها ناموفق بود", Toast.LENGTH_SHORT).show());
        };
        String[] labels = {"همه", "👍 لایک", "❤️ قلب"}; String[] keys = {"all", "likes", "hearts"};
        for (int i=0;i<labels.length;i++) { final String key=keys[i]; Button b=new Button(this); b.setText(labels[i]); tabs.addView(b,new LinearLayout.LayoutParams(0,dp(44),1)); b.setOnClickListener(v->{selected[0]=key;render[0].run();}); }
        dialog.show(); render[0].run();
    }

    private GradientDrawable createReactionBackground(int fill, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(18));
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private void loadHeartStatus(DocumentSnapshot experience,
                                 TextView heartCount) {
        CollectionReference hearts = experience.getReference()
                .collection("hearts");
        hearts.get().addOnSuccessListener(snapshot ->
                heartCount.setText(formatCount(snapshot.size())))
                .addOnFailureListener(e ->
                        Toast.makeText(ExperienceListActivity.this,
                                "بارگذاری تعداد قلب‌ها ناموفق بود",
                                Toast.LENGTH_SHORT).show());
    }

    private void toggleHeart(DocumentSnapshot experience,
                             String currentUserId,
                             TextView heartButton,
                             TextView heartCount) {
        CollectionReference hearts = experience.getReference()
                .collection("hearts");
        DocumentReference heart = hearts.document(currentUserId);
        heart.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                heart.delete().addOnSuccessListener(unused -> {
                    heartButton.setText("👍 واکنش");
                    hearts.get().addOnSuccessListener(items ->
                            heartCount.setText(formatCount(items.size())));
                }).addOnFailureListener(e -> Toast.makeText(this,
                        "خطا در برداشتن قلب", Toast.LENGTH_SHORT).show());
            } else {
                java.util.HashMap<String, Object> data = new java.util.HashMap<>();
                data.put("userId", currentUserId);
                data.put("timestamp", FieldValue.serverTimestamp());
                heart.set(data).addOnSuccessListener(unused -> {
                    heartButton.setText("❤️ واکنش");
                    hearts.get().addOnSuccessListener(items ->
                            heartCount.setText(formatCount(items.size())));
                }).addOnFailureListener(e -> Toast.makeText(this,
                        "خطا در ثبت قلب", Toast.LENGTH_SHORT).show());
            }
        }).addOnFailureListener(e -> Toast.makeText(this,
                "خطا در بررسی قلب", Toast.LENGTH_SHORT).show());
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
                                            ? "👍 لایک"
                                            : "👍 لایک"
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
                                                            "👍 لایک"
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
                                                            "👍 لایک"
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

    // =========================================================
    // تاریخ
    // =========================================================

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

            String lang = LanguageManager.getLanguage(this);
            String dateText;

            // زبان انگلیسی: تاریخ میلادی؛ زبان‌های دری/فارسی و دیگر زبان‌های راست‌به‌چپ: هجری شمسی.
            if ("en".equals(lang)) {
                dateText = String.format(
                        java.util.Locale.US,
                        "%04d/%02d/%02d - %02d:%02d:%02d",
                        gy, gm, gd, hour, minute, second
                );
                return dateText;
            }

            int[] jalali = gregorianToJalali(gy, gm, gd);
            dateText = String.format(
                    java.util.Locale.US,
                    "%04d/%02d/%02d - %02d:%02d:%02d",
                    jalali[0], jalali[1], jalali[2], hour, minute, second
            );
            return toPersianDigits(dateText);

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

    // =========================================================
    // اطلاعات پروفایل
    // =========================================================

    private void loadUserProfile(
            String userId,
            TextView authorName,
            ImageView profileImage,
            String fallbackName
    ) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(userDocument -> {
                    String name = fallbackName;
                    String photoUrl = "";
                    if (userDocument.exists()) {
                        name = firstNonEmpty(
                                userDocument.getString("name"),
                                userDocument.getString("username"),
                                userDocument.getString("displayName"),
                                fallbackName
                        );
                        photoUrl = firstNonEmpty(
                                userDocument.getString("photoUrl"),
                                userDocument.getString("profilePhoto"),
                                userDocument.getString("profilePhotoUrl"),
                                userDocument.getString("photo"),
                                ""
                        );
                    }
                    authorName.setText(name);
                    final String finalName = name;
                    final String finalPhotoUrl = photoUrl;
                    if (!photoUrl.isEmpty()) {
                        loadProfileImage(photoUrl, profileImage);
                    }
                    View.OnClickListener openProfile = v ->
                            showPublicProfile(userId, finalName, finalPhotoUrl);
                    authorName.setOnClickListener(openProfile);
                    profileImage.setOnClickListener(openProfile);
                    authorRowClickable(profileImage, authorName, openProfile);
                });
    }

    private void authorRowClickable(ImageView image, TextView name, View.OnClickListener listener) {
        image.setClickable(true);
        name.setClickable(true);
        image.setOnClickListener(listener);
        name.setOnClickListener(listener);
    }

    private String firstNonEmpty(String... values) {
        if (values != null) {
            for (String value : values) {
                if (value != null && !value.trim().isEmpty()) return value.trim();
            }
        }
        return "";
    }

    // پروفایل عمومی؛ فقط اطلاعات قابل نمایش از سند users خوانده می‌شود.
    private void showPublicProfile(String uid, String fallbackName, String photoUrl) {
        if (uid == null || uid.trim().isEmpty()) return;
        db.collection("users").document(uid).get().addOnSuccessListener(d -> {
            String name = firstNonEmpty(d.getString("name"), d.getString("username"), d.getString("displayName"), fallbackName);
            String photo = firstNonEmpty(d.getString("photoUrl"), d.getString("profilePhoto"), d.getString("profilePhotoUrl"), d.getString("photo"), photoUrl, "");
            LinearLayout panel = new LinearLayout(this);
            panel.setOrientation(LinearLayout.VERTICAL);
            panel.setGravity(Gravity.CENTER_HORIZONTAL);
            panel.setPadding(dp(18), dp(14), dp(18), dp(12));
            ScrollView scroll = new ScrollView(this);
            scroll.addView(panel);

            ImageView avatar = new ImageView(this);
            avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
            GradientDrawable avatarBg = new GradientDrawable();
            avatarBg.setColor(Color.rgb(230, 241, 245));
            avatarBg.setShape(GradientDrawable.OVAL);
            avatarBg.setStroke(dp(2), Color.rgb(12, 91, 120));
            avatar.setBackground(avatarBg);
            avatar.setClipToOutline(true);
            panel.addView(avatar, new LinearLayout.LayoutParams(dp(150), dp(150)));
            if (!photo.isEmpty()) loadProfileImage(photo, avatar);
            avatar.setOnClickListener(v -> showLargeProfilePhoto(photo, name));

            TextView nameView = new TextView(this);
            nameView.setText(name);
            nameView.setTextSize(21);
            nameView.setTypeface(null, Typeface.BOLD);
            nameView.setTextColor(Color.rgb(8, 65, 90));
            nameView.setGravity(Gravity.CENTER);
            nameView.setPadding(0, dp(12), 0, dp(8));
            panel.addView(nameView, new LinearLayout.LayoutParams(-1, -2));

            addProfileDetail(panel, "نام کاربری", firstNonEmpty(d.getString("username"), d.getString("userName")));
            addProfileDetail(panel, "درباره من", firstNonEmpty(d.getString("bio"), d.getString("about"), d.getString("description")));
            addProfileDetail(panel, "شهر", firstNonEmpty(d.getString("city"), d.getString("location")));
            addProfileDetail(panel, "تخصص", firstNonEmpty(d.getString("profession"), d.getString("job"), d.getString("occupation")));

            String myId = auth.getCurrentUser() == null ? "" : auth.getCurrentUser().getUid();
            if (!uid.equals(myId) && !myId.isEmpty()) {
                Button friendButton = new Button(this);
                friendButton.setText("🤝 درخواست دوستی");
                panel.addView(friendButton, new LinearLayout.LayoutParams(-1, -2));
                setFriendButtonState(uid, friendButton);
                friendButton.setOnClickListener(v -> sendFriendRequest(uid, friendButton));
            } else if (uid.equals(myId)) {
                Button incoming = new Button(this);
                incoming.setText("📩 درخواست‌های دوستی");
                panel.addView(incoming, new LinearLayout.LayoutParams(-1, -2));
                incoming.setOnClickListener(v -> showIncomingFriendRequests());
            }

            new AlertDialog.Builder(this)
                    .setTitle("پروفایل کاربر")
                    .setView(scroll)
                    .setPositiveButton("بستن", null)
                    .show();
        }).addOnFailureListener(e -> Toast.makeText(this, "پروفایل بارگذاری نشد؛ اتصال را بررسی کنید.", Toast.LENGTH_SHORT).show());
    }

    private void addProfileDetail(LinearLayout panel, String label, String value) {
        if (value == null || value.trim().isEmpty()) return;
        TextView detail = new TextView(this);
        detail.setText(label + ": " + value);
        detail.setTextSize(15);
        detail.setTextColor(Color.rgb(55, 65, 70));
        detail.setGravity(Gravity.RIGHT);
        detail.setPadding(dp(4), dp(7), dp(4), dp(7));
        panel.addView(detail, new LinearLayout.LayoutParams(-1, -2));
    }

    private void showLargeProfilePhoto(String photo, String name) {
        if (photo == null || photo.trim().isEmpty()) {
            Toast.makeText(this, "این کاربر عکس پروفایل ندارد.", Toast.LENGTH_SHORT).show();
            return;
        }
        ImageView image = new ImageView(this);
        image.setAdjustViewBounds(true);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        loadProfileImage(photo, image);
        new AlertDialog.Builder(this).setTitle(name).setView(image).setPositiveButton("بستن", null).show();
    }

    private void setFriendButtonState(String otherUid, Button button) {
        String myId = auth.getCurrentUser() == null ? "" : auth.getCurrentUser().getUid();
        if (myId.isEmpty()) return;
        db.collection("friendRequests").whereEqualTo("senderId", myId).get()
                .addOnSuccessListener(q -> {
                    DocumentSnapshot match = null;
                    for (DocumentSnapshot item : q.getDocuments()) {
                        if (otherUid.equals(item.getString("receiverId"))) { match = item; break; }
                    }
                    if (match == null) button.setText("🤝 درخواست دوستی");
                    else {
                        String status = match.getString("status");
                        button.setText("accepted".equals(status) ? "✅ دوست هستید" : "⏳ درخواست فرستاده شده");
                        button.setEnabled(!"accepted".equals(status));
                    }
                }).addOnFailureListener(e -> button.setText("🤝 درخواست دوستی"));
    }

    private void sendFriendRequest(String receiverId, Button button) {
        FirebaseUser current = auth.getCurrentUser();
        if (current == null) {
            Toast.makeText(this, "برای درخواست دوستی اول وارد حساب شوید.", Toast.LENGTH_SHORT).show();
            return;
        }
        String senderId = current.getUid();
        if (senderId.equals(receiverId)) return;
        button.setEnabled(false);
        db.collection("friendRequests").whereEqualTo("senderId", senderId).get()
                .addOnSuccessListener(existing -> {
                    for (DocumentSnapshot item : existing.getDocuments()) {
                        if (receiverId.equals(item.getString("receiverId"))) {
                            String status = item.getString("status");
                            button.setText("accepted".equals(status) ? "✅ دوست هستید" : "⏳ درخواست فرستاده شده");
                            button.setEnabled(!"accepted".equals(status));
                            return;
                        }
                    }
                    Map<String, Object> request = new java.util.HashMap<>();
                    request.put("senderId", senderId);
                    request.put("receiverId", receiverId);
                    request.put("status", "pending");
                    request.put("createdAt", FieldValue.serverTimestamp());
                    db.collection("friendRequests").add(request)
                            .addOnSuccessListener(ref -> {
                                button.setText("⏳ درخواست فرستاده شده");
                                button.setEnabled(false);
                                Toast.makeText(this, "درخواست دوستی فرستاده شد.", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                button.setEnabled(true);
                                Toast.makeText(this, "ارسال درخواست ناموفق بود؛ قوانین فایربیس را بررسی کنید.", Toast.LENGTH_LONG).show();
                            });
                }).addOnFailureListener(e -> {
                    button.setEnabled(true);
                    Toast.makeText(this, "بررسی درخواست دوستی ناموفق بود.", Toast.LENGTH_SHORT).show();
                });
    }

    private void showIncomingFriendRequests() {
        FirebaseUser current = auth.getCurrentUser();
        if (current == null) return;
        String myId = current.getUid();
        db.collection("friendRequests").whereEqualTo("receiverId", myId).get()
                .addOnSuccessListener(snapshot -> {
                    LinearLayout list = new LinearLayout(this);
                    list.setOrientation(LinearLayout.VERTICAL);
                    ScrollView scroller = new ScrollView(this);
                    scroller.addView(list);
                    boolean hasPending = false;
                    for (DocumentSnapshot request : snapshot.getDocuments()) {
                        if (!"pending".equals(request.getString("status"))) continue;
                        hasPending = true;
                        String senderId = request.getString("senderId");
                        LinearLayout row = new LinearLayout(this);
                        row.setOrientation(LinearLayout.VERTICAL);
                        row.setPadding(dp(10), dp(8), dp(10), dp(8));
                        TextView sender = new TextView(this);
                        sender.setText("در حال دریافت نام کاربر…");
                        row.addView(sender);
                        if (senderId != null) db.collection("users").document(senderId).get().addOnSuccessListener(u ->
                                sender.setText(firstNonEmpty(u.getString("name"), u.getString("username"), "کاربر")));
                        LinearLayout actions = new LinearLayout(this);
                        Button accept = new Button(this); accept.setText("قبول");
                        Button reject = new Button(this); reject.setText("رد");
                        actions.addView(accept, new LinearLayout.LayoutParams(0, -2, 1));
                        actions.addView(reject, new LinearLayout.LayoutParams(0, -2, 1));
                        row.addView(actions);
                        list.addView(row);
                        accept.setOnClickListener(v -> respondFriendRequest(request, "accepted", accept, reject));
                        reject.setOnClickListener(v -> respondFriendRequest(request, "rejected", accept, reject));
                    }
                    if (!hasPending) {
                        TextView empty = new TextView(this);
                        empty.setText("درخواست دوستی جدیدی نداری.");
                        empty.setPadding(dp(18), dp(18), dp(18), dp(18));
                        list.addView(empty);
                    }
                    new AlertDialog.Builder(this).setTitle("درخواست‌های دوستی").setView(scroller).setPositiveButton("بستن", null).show();
                }).addOnFailureListener(e -> Toast.makeText(this, "دریافت درخواست‌ها ناموفق بود؛ قوانین فایربیس را بررسی کنید.", Toast.LENGTH_LONG).show());
    }

    private void respondFriendRequest(DocumentSnapshot request, String status, Button accept, Button reject) {
        request.getReference().update("status", status, "respondedAt", FieldValue.serverTimestamp())
                .addOnSuccessListener(unused -> {
                    accept.setEnabled(false); reject.setEnabled(false);
                    accept.setText("accepted".equals(status) ? "✅ قبول شد" : "قبول");
                    reject.setText("rejected".equals(status) ? "رد شد" : "رد");
                    Toast.makeText(this, "پاسخ ثبت شد.", Toast.LENGTH_SHORT).show();
                }).addOnFailureListener(e -> Toast.makeText(this, "ثبت پاسخ ناموفق بود؛ قوانین فایربیس را بررسی کنید.", Toast.LENGTH_LONG).show());
    }

    // =========================================================
    // بارگذاری عکس پروفایل
    // =========================================================

    private void loadProfileImage(String photoUrl, ImageView imageView) {
        if (photoUrl == null || photoUrl.trim().isEmpty() || imageView == null) return;
        final String storedUrl = photoUrl.trim();
        if (storedUrl.contains("/storage/v1/object/public/" + PROFILE_BUCKET + "/") ||
                storedUrl.contains("/storage/v1/object/" + PROFILE_BUCKET + "/")) {
            loadPrivateProfileImage(storedUrl, imageView);
            return;
        }
        new Thread(() -> downloadProfileImage(storedUrl, imageView)).start();
    }

    private void loadPrivateProfileImage(String storedUrl, ImageView imageView) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                String marker = "/" + PROFILE_BUCKET + "/";
                int markerIndex = storedUrl.indexOf(marker);
                if (markerIndex < 0) return;
                String objectPath = storedUrl.substring(markerIndex + marker.length());
                int queryIndex = objectPath.indexOf('?');
                if (queryIndex >= 0) objectPath = objectPath.substring(0, queryIndex);
                objectPath = URLDecoder.decode(objectPath, "UTF-8");

                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser == null) return;
                String idToken = currentUser.getIdToken(false).getResult().getToken();
                URL functionUrl = new URL(SUPABASE_URL + "/functions/v1/profile-photo-url");
                connection = (HttpURLConnection) functionUrl.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setConnectTimeout(20000);
                connection.setReadTimeout(30000);
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setRequestProperty("apikey", SUPABASE_PUBLISHABLE_KEY);
                connection.setRequestProperty("Authorization", "Bearer " + idToken);
                JSONObject body = new JSONObject();
                body.put("path", objectPath);
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(body.toString().getBytes(StandardCharsets.UTF_8));
                }
                int code = connection.getResponseCode();
                InputStream response = code >= 200 && code < 300
                        ? connection.getInputStream() : connection.getErrorStream();
                if (response == null || code < 200 || code >= 300) return;
                java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int count;
                while ((count = response.read(buffer)) != -1) bytes.write(buffer, 0, count);
                response.close();
                String signedUrl = new JSONObject(bytes.toString("UTF-8")).optString("url", "");
                if (!signedUrl.isEmpty()) downloadProfileImage(signedUrl, imageView);
            } catch (Exception ignored) {
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    private void downloadProfileImage(String url, ImageView imageView) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(20000);
            Bitmap bitmap;
            try (InputStream input = connection.getInputStream()) {
                bitmap = BitmapFactory.decodeStream(input);
            }
            if (bitmap != null) runOnUiThread(() -> imageView.setImageBitmap(bitmap));
        } catch (Exception ignored) {
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}
