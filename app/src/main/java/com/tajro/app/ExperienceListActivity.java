package com.tajro.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URLEncoder;
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
import android.widget.HorizontalScrollView;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.EditText;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Source;

import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExperienceListActivity extends Activity {

    private static final String SUPABASE_URL =
            "https://gorbhuqmkjlkrklhasdh.supabase.co";
    private static final String SUPABASE_PUBLISHABLE_KEY =
            "sb_publishable_a02sM3MABB4afGU90ZBdFA_OTYG6gUs";
    private static final String PROFILE_BUCKET = "profile_photos_public";

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private LinearLayout layout;
    private LinearLayout feedLayout;
    private TextView pullRefreshIndicator;
    private LinearLayout storiesLayout;
    private LinearLayout friendsLayout;
    private boolean refreshBatchActive = false;
    private int refreshTasksRemaining = 0;
    private boolean isPullRefreshing = false;
    private float pullStartY = 0f;
    private boolean pullGestureStarted = false;

    // اشتراک‌گذاری درخواست پروفایل بین کارت‌های یک کاربر؛ جلوگیری از خواندن تکراری Firestore
    private final Map<String, UserProfileData> userProfileCache = new HashMap<>();
    private final Map<String, List<ProfileViewTarget>> pendingProfileTargets = new HashMap<>();
    private final Set<String> userProfileRequests = new HashSet<>();

    // تعداد محدود کارگرها برای دانلود عکس‌ها؛ جلوگیری از ساخت ده‌ها Thread هم‌زمان
    private static final ExecutorService IMAGE_EXECUTOR = Executors.newFixedThreadPool(3);

    private static class UserProfileData {
        final String name;
        final String photoUrl;
        UserProfileData(String name, String photoUrl) {
            this.name = name;
            this.photoUrl = photoUrl;
        }
    }

    private static class ProfileViewTarget {
        final TextView nameView;
        final ImageView imageView;
        final String userId;
        ProfileViewTarget(String userId, TextView nameView, ImageView imageView) {
            this.userId = userId;
            this.nameView = nameView;
            this.imageView = imageView;
        }
    }

    // فرم انتشار تجربه در همین صفحه
    private boolean isPublishing = false;
    private static final int REQUEST_PICK_EXPERIENCE_IMAGE = 7314;
    private static final String EXPERIENCE_UPLOAD_FUNCTION =
            SUPABASE_URL + "/functions/v1/upload-experience-image";
    private Uri selectedImageUri;
    private ImageView imagePreview;
    private TextView imageStatus;
    private EditText experienceTitleField;
    private EditText experienceTextField;
    private Button publishButton;

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
            if (fa.equals("هر تجربه یک درس و هر داستان چراغیست")) return "Every Experience Is a Lesson, Every Story a Light";
            if (fa.equals("داستان‌های ۲۴ ساعته")) return "24-hour Stories";
            if (fa.equals("دوستان")) return "Friends";
            if (fa.equals("داستان من")) return "My Story";
            if (fa.equals("داستان کوتاه خود را بنویسید...")) return "Write a short story...";
            if (fa.equals("داستان جدید")) return "New Story";
            if (fa.equals("انتشار")) return "Publish";
            if (fa.equals("داستان منتشر شد.")) return "Story published.";
            if (fa.equals("هنوز دوستی اضافه نشده است.")) return "No friends added yet.";
            if (fa.equals("این داستان پس از ۲۴ ساعت ناپدید می‌شود.")) return "This story disappears after 24 hours.";

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
            if (fa.equals("هر تجربه یک درس و هر داستان چراغیست")) return "هره تجربه یو درس دی او هره کیسه یوه رڼا ده";
            if (fa.equals("داستان‌های ۲۴ ساعته")) return "۲۴ ساعته کیسې";
            if (fa.equals("دوستان")) return "ملګري";
            if (fa.equals("داستان من")) return "زما کیسه";
            if (fa.equals("داستان کوتاه خود را بنویسید...")) return "خپله لنډه کیسه ولیکئ...";
            if (fa.equals("داستان جدید")) return "نوې کیسه";
            if (fa.equals("انتشار")) return "خپرول";
            if (fa.equals("داستان منتشر شد.")) return "کیسه خپره شوه.";
            if (fa.equals("هنوز دوستی اضافه نشده است.")) return "تر اوسه ملګری نه دی زیات شوی.";
            if (fa.equals("این داستان پس از ۲۴ ساعت ناپدید می‌شود.")) return "دا کیسه له ۲۴ ساعتونو وروسته ورکېږي.";

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
            if (fa.equals("هر تجربه یک درس و هر داستان چراغیست")) return "ہر تجربہ ایک سبق ہے اور ہر کہانی ایک چراغ ہے";
            if (fa.equals("داستان‌های ۲۴ ساعته")) return "۲۴ گھنٹے کی کہانیاں";
            if (fa.equals("دوستان")) return "دوست";
            if (fa.equals("داستان من")) return "میری کہانی";
            if (fa.equals("داستان کوتاه خود را بنویسید...")) return "اپنی مختصر کہانی لکھیں...";
            if (fa.equals("داستان جدید")) return "نئی کہانی";
            if (fa.equals("انتشار")) return "شائع کریں";
            if (fa.equals("داستان منتشر شد.")) return "کہانی شائع ہو گئی۔";
            if (fa.equals("هنوز دوستی اضافه نشده است.")) return "ابھی کوئی دوست شامل نہیں۔";
            if (fa.equals("این داستان پس از ۲۴ ساعت ناپدید می‌شود.")) return "یہ کہانی ۲۴ گھنٹے بعد غائب ہو جائے گی۔";

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
            if (fa.equals("هر تجربه یک درس و هر داستان چراغیست")) return "हर अनुभव एक सबक है और हर कहानी एक रोशनी है";
            if (fa.equals("داستان‌های ۲۴ ساعته")) return "२४ घंटे की कहानियाँ";
            if (fa.equals("دوستان")) return "मित्र";
            if (fa.equals("داستان من")) return "मेरी कहानी";
            if (fa.equals("داستان کوتاه خود را بنویسید...")) return "अपनी छोटी कहानी लिखें...";
            if (fa.equals("داستان جدید")) return "नई कहानी";
            if (fa.equals("انتشار")) return "प्रकाशित करें";
            if (fa.equals("داستان منتشر شد.")) return "कहानी प्रकाशित हुई।";
            if (fa.equals("هنوز دوستی اضافه نشده است.")) return "अभी कोई मित्र नहीं जोड़ा गया है।";
            if (fa.equals("این داستان پس از ۲۴ ساعت ناپدید می‌شود.")) return "यह कहानी २४ घंटे बाद गायब हो जाएगी।";

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

        // متن‌های فرم انتشار تجربه
        if ("en".equals(lang)) {
            if (fa.equals("✍️ ثبت تجربه جدید")) return "✍️ Share an Experience";
            if (fa.equals("عنوان تجربه را بنویسید")) return "Write the experience title";
            if (fa.equals("تجربه خود را با دیگران شریک کنید...")) return "Share your experience with others...";
            if (fa.equals("🚀 انتشار تجربه")) return "🚀 Publish Experience";
            if (fa.equals("🖼 انتخاب عکس")) return "🖼 Choose photo";
            if (fa.equals("عکس انتخاب نشده است")) return "No photo selected";
            if (fa.equals("عکس انتخاب شد؛ هنگام انتشار بارگذاری می‌شود")) return "Photo selected; it will upload when published";
            if (fa.equals("لطفاً ابتدا وارد اکانت خود شوید")) return "Please log in first";
            if (fa.equals("لطفاً عنوان و متن تجربه را وارد کنید")) return "Please enter a title and text";
            if (fa.equals("عنوان تجربه خیلی طولانی است")) return "The title is too long";
            if (fa.equals("متن تجربه خیلی طولانی است")) return "The text is too long";
            if (fa.equals("⏳ در حال بررسی...")) return "⏳ Checking...";
            if (fa.equals("این تجربه قبلاً منتشر شده است ❤️")) return "This experience has already been published ❤️";
            if (fa.equals("تجربه با موفقیت منتشر شد! 🎉")) return "Experience published successfully! 🎉";
            if (fa.equals("خطا در بررسی تجربه: ")) return "Error checking experience: ";
            if (fa.equals("خطا در انتشار تجربه: ")) return "Error publishing experience: ";
        }
        if ("ps".equals(lang)) {
            if (fa.equals("✍️ ثبت تجربه جدید")) return "✍️ نوې تجربه خپره کړئ";
            if (fa.equals("عنوان تجربه را بنویسید")) return "د تجربې سرلیک ولیکئ";
            if (fa.equals("تجربه خود را با دیگران شریک کنید...")) return "خپله تجربه له نورو سره شریکه کړئ...";
            if (fa.equals("🚀 انتشار تجربه")) return "🚀 تجربه خپره کړئ";
            if (fa.equals("🖼 انتخاب عکس")) return "🖼 عکس وټاکئ";
            if (fa.equals("عکس انتخاب نشده است")) return "عکس نه دی ټاکل شوی";
            if (fa.equals("عکس انتخاب شد؛ هنگام انتشار بارگذاری می‌شود")) return "عکس ټاکل شوی؛ د خپرولو پر مهال پورته کېږي";
            if (fa.equals("لطفاً ابتدا وارد اکانت خود شوید")) return "لومړی خپل حساب ته ننوځئ";
            if (fa.equals("لطفاً عنوان و متن تجربه را وارد کنید")) return "د تجربې سرلیک او متن ولیکئ";
            if (fa.equals("عنوان تجربه خیلی طولانی است")) return "د سرلیک اوږدوالی ډېر دی";
            if (fa.equals("متن تجربه خیلی طولانی است")) return "د متن اوږدوالی ډېر دی";
            if (fa.equals("⏳ در حال بررسی...")) return "⏳ د کتلو په حال کې...";
            if (fa.equals("این تجربه قبلاً منتشر شده است ❤️")) return "دا تجربه مخکې خپره شوې ده ❤️";
            if (fa.equals("تجربه با موفقیت منتشر شد! 🎉")) return "تجربه په بریالیتوب خپره شوه! 🎉";
            if (fa.equals("خطا در بررسی تجربه: ")) return "د تجربې په کتلو کې تېروتنه: ";
            if (fa.equals("خطا در انتشار تجربه: ")) return "د تجربې په خپرولو کې تېروتنه: ";
        }
        if ("ur".equals(lang)) {
            if (fa.equals("✍️ ثبت تجربه جدید")) return "✍️ نیا تجربہ شیئر کریں";
            if (fa.equals("عنوان تجربه را بنویسید")) return "تجربے کا عنوان لکھیں";
            if (fa.equals("تجربه خود را با دیگران شریک کنید...")) return "اپنا تجربہ دوسروں کے ساتھ شیئر کریں...";
            if (fa.equals("🚀 انتشار تجربه")) return "🚀 تجربہ شائع کریں";
            if (fa.equals("🖼 انتخاب عکس")) return "🖼 تصویر منتخب کریں";
            if (fa.equals("عکس انتخاب نشده است")) return "کوئی تصویر منتخب نہیں";
            if (fa.equals("لطفاً ابتدا وارد اکانت خود شوید")) return "براہِ کرم پہلے لاگ اِن کریں";
            if (fa.equals("لطفاً عنوان و متن تجربه را وارد کنید")) return "عنوان اور متن درج کریں";
            if (fa.equals("⏳ در حال بررسی...")) return "⏳ جانچ جاری ہے...";
            if (fa.equals("تجربه با موفقیت منتشر شد! 🎉")) return "تجربہ کامیابی سے شائع ہوا! 🎉";
        }
        if ("hi".equals(lang)) {
            if (fa.equals("✍️ ثبت تجربه جدید")) return "✍️ नया अनुभव साझा करें";
            if (fa.equals("عنوان تجربه را بنویسید")) return "अनुभव का शीर्षक लिखें";
            if (fa.equals("تجربه خود را با دیگران شریک کنید...")) return "अपना अनुभव साझा करें...";
            if (fa.equals("🚀 انتشار تجربه")) return "🚀 अनुभव प्रकाशित करें";
            if (fa.equals("🖼 انتخاب عکس")) return "🖼 तस्वीर चुनें";
            if (fa.equals("عکس انتخاب نشده است")) return "कोई तस्वीर नहीं चुनी गई";
            if (fa.equals("لطفاً ابتدا وارد اکانت خود شوید")) return "पहले लॉग इन करें";
            if (fa.equals("لطفاً عنوان و متن تجربه را وارد کنید")) return "शीर्षक और पाठ दर्ज करें";
            if (fa.equals("⏳ در حال بررسی...")) return "⏳ जाँच हो रही है...";
            if (fa.equals("تجربه با موفقیت منتشر شد! 🎉")) return "अनुभव सफलतापूर्वक प्रकाशित हुआ! 🎉";
        }

        // العربية
        if ("ar".equals(lang)) {
            if (fa.equals("هنوز تجربه‌ای منتشر نشده است.\n\n")) return "لم تُنشر أي تجربة بعد.\n\n";
            if (fa.equals("اولین تجربه خود را ثبت کنید! ✍️")) return "كن أول من يشارك تجربته! ✍️";
            if (fa.equals("❤️ واکنش")) return "❤️ تفاعل";
            if (fa.equals("۰")) return "٠";
            if (fa.equals("۱")) return "١";
            if (fa.equals("۲")) return "٢";
            if (fa.equals("۳")) return "٣";
            if (fa.equals("۴")) return "٤";
            if (fa.equals("۵")) return "٥";
            if (fa.equals("۶")) return "٦";
            if (fa.equals("۷")) return "٧";
            if (fa.equals("۸")) return "٨";
            if (fa.equals("۹")) return "٩";
            if (fa.equals("تجربه‌ها")) return "التجارب";
            if (fa.equals("📚 تجربه‌های کاربران")) return "📚 تجارب المستخدمين";
            if (fa.equals("هر تجربه یک درس و هر داستان چراغیست")) return "كل تجربة درس، وكل قصة نور";
            if (fa.equals("⋮ گزینه‌های تجربه")) return "⋮ خيارات التجربة";
            if (fa.equals("شریک‌سازی تجربه")) return "مشاركة التجربة";
            if (fa.equals("هنوز تجربه‌ای منتشر نشده است.\n\nاولین تجربه خود را ثبت کنید! ✍️")) return "لم تُنشر أي تجربة بعد.\\n\\nكن أول من يشارك تجربته! ✍️";
            if (fa.equals("بدون عنوان")) return "بلا عنوان";
            if (fa.equals("کاربر")) return "مستخدم";
            if (fa.equals("برای لایک کردن وارد حساب شوید")) return "يرجى تسجيل الدخول للإعجاب";
            if (fa.equals("✏️ ویرایش")) return "✏️ تعديل";
            if (fa.equals("🗑️ حذف")) return "🗑️ حذف";
            if (fa.equals("مطمئن هستید این تجربه حذف شود؟")) return "هل أنت متأكد من حذف هذه التجربة؟";
            if (fa.equals("لغو")) return "إلغاء";
            if (fa.equals("حذف")) return "حذف";
            if (fa.equals("تجربه حذف شد 🗑️")) return "تم حذف التجربة 🗑️";
            if (fa.equals("خطا در حذف تجربه")) return "حدث خطأ أثناء حذف التجربة";
            if (fa.equals("خطا در دریافت تجربه‌ها")) return "حدث خطأ أثناء تحميل التجارب";
            if (fa.equals("خطا در برداشتن لایک")) return "حدث خطأ أثناء إزالة الإعجاب";
            if (fa.equals("خطا در ثبت لایک")) return "حدث خطأ أثناء تسجيل الإعجاب";
            if (fa.equals("خطا در بررسی لایک")) return "حدث خطأ أثناء التحقق من الإعجاب";
            if (fa.equals("✍️ ثبت تجربه جدید")) return "✍️ نشر تجربة جديدة";
            if (fa.equals("عنوان تجربه را بنویسید")) return "اكتب عنوان التجربة";
            if (fa.equals("تجربه خود را با دیگران شریک کنید...")) return "شارك تجربتك مع الآخرين...";
            if (fa.equals("🚀 انتشار تجربه")) return "🚀 نشر التجربة";
            if (fa.equals("🖼 انتخاب عکس")) return "🖼 اختيار صورة";
            if (fa.equals("عکس انتخاب نشده است")) return "لم يتم اختيار صورة";
            if (fa.equals("عکس انتخاب شد؛ هنگام انتشار بارگذاری می‌شود")) return "تم اختيار الصورة؛ سيتم رفعها عند النشر";
            if (fa.equals("لطفاً ابتدا وارد اکانت خود شوید")) return "يرجى تسجيل الدخول أولاً";
            if (fa.equals("لطفاً عنوان و متن تجربه را وارد کنید")) return "يرجى إدخال عنوان التجربة ونصها";
            if (fa.equals("عنوان تجربه خیلی طولانی است")) return "عنوان التجربة طويل جداً";
            if (fa.equals("متن تجربه خیلی طولانی است")) return "نص التجربة طويل جداً";
            if (fa.equals("⏳ در حال بررسی...")) return "⏳ جارٍ التحقق...";
            if (fa.equals("این تجربه قبلاً منتشر شده است ❤️")) return "تم نشر هذه التجربة مسبقاً ❤️";
            if (fa.equals("تجربه با موفقیت منتشر شد! 🎉")) return "تم نشر التجربة بنجاح! 🎉";
            if (fa.equals("خطا در بررسی تجربه: ")) return "خطأ أثناء التحقق من التجربة: ";
            if (fa.equals("خطا در انتشار تجربه: ")) return "خطأ أثناء نشر التجربة: ";
            if (fa.equals("↓ برای تازه‌سازی، صفحه را پایین بکشید")) return "↓ اسحب الصفحة للأسفل للتحديث";
            if (fa.equals("↓ رها کنید تا تازه‌سازی شود")) return "↓ اترك للتحديث";
            if (fa.equals("در حال تازه‌سازی تجربه‌ها...")) return "جارٍ تحديث التجارب...";
            if (fa.equals("پروفایل من")) return "ملفي الشخصي";
            if (fa.equals("انتخاب عکس تجربه")) return "اختيار صورة التجربة";
            if (fa.equals("باز کردن گالری ممکن نشد")) return "تعذّر فتح المعرض";
            if (fa.equals("⏳ در حال بارگذاری عکس...")) return "⏳ جارٍ تحميل الصورة...";
            if (fa.equals("بارگذاری عکس ناموفق بود: ")) return "فشل رفع الصورة: ";
            if (fa.equals("لطفاً ابتدا وارد حساب خود شوید")) return "يرجى تسجيل الدخول أولاً";
            if (fa.equals("توکن ورود دریافت نشد")) return "لم يتم استلام رمز تسجيل الدخول";
            if (fa.equals("فایل عکس قابل خواندن نیست")) return "تعذّرت قراءة ملف الصورة";
            if (fa.equals("حجم عکس باید کمتر از ۴ مگابایت باشد")) return "يجب أن يكون حجم الصورة أقل من 4 ميغابايت";
            if (fa.equals("نشانی عکس از سرور دریافت نشد")) return "لم يتم استلام رابط الصورة من الخادم";
            if (fa.equals("خطای ناشناخته در آپلود عکس")) return "خطأ غير معروف أثناء رفع الصورة";
            if (fa.equals("دریافت توکن Firebase ناموفق بود")) return "فشل الحصول على رمز Firebase";
            if (fa.equals("خواندن عکس ممکن نشد")) return "تعذّرت قراءة الصورة";
            if (fa.equals("👍 واکنش")) return "👍 تفاعل";
            if (fa.equals("واکنش خود را انتخاب کنید")) return "اختر تفاعلك";
            if (fa.equals("👍 لایک")) return "👍 إعجاب";
            if (fa.equals("❤️ قلب")) return "❤️ قلب";
            if (fa.equals("💬 کامنت")) return "💬 تعليق";
            if (fa.equals("↗ اشتراک‌گذاری")) return "↗ مشاركة";
            if (fa.equals("⬇️ ذخیره عکس در گالری")) return "⬇️ حفظ الصورة في المعرض";
            if (fa.equals("بستن")) return "إغلاق";
            if (fa.equals("تصویر تجربه")) return "صورة التجربة";
            if (fa.equals("⬇️ ذخیره در گالری")) return "⬇️ حفظ في المعرض";
            if (fa.equals("آدرس عکس موجود نیست")) return "رابط الصورة غير متوفر";
            if (fa.equals("برای ذخیره در گالری، نسخهٔ جدید برنامه را نصب کنید")) return "ثبّت الإصدار الجديد من التطبيق لحفظ الصورة في المعرض";
            if (fa.equals("در حال ذخیره عکس...")) return "جارٍ حفظ الصورة...";
            if (fa.equals("تصویر قابل خواندن نیست")) return "تعذّرت قراءة الصورة";
            if (fa.equals("ذخیره‌سازی شروع نشد")) return "لم يبدأ الحفظ";
            if (fa.equals("نوشتن تصویر انجام نشد")) return "تعذّرت كتابة الصورة";
            if (fa.equals("عکس با موفقیت در گالری ذخیره شد 📸")) return "تم حفظ الصورة في المعرض بنجاح 📸";
            if (fa.equals("ذخیره عکس ناموفق بود؛ اتصال اینترنت را بررسی کنید")) return "فشل حفظ الصورة؛ تحقق من اتصال الإنترنت";
            if (fa.equals("اشتراک‌گذاری تجربه")) return "مشاركة التجربة";
            if (fa.equals("💬 کامنت (")) return "💬 التعليقات (";
            if (fa.equals("نظر خود را بنویسید...")) return "اكتب تعليقك...";
            if (fa.equals("کامنت‌ها")) return "التعليقات";
            if (fa.equals("ثبت کامنت")) return "إرسال التعليق";
            if (fa.equals("هنوز کامنتی ثبت نشده است.")) return "لا توجد تعليقات بعد.";
            if (fa.equals("بارگذاری کامنت‌ها ناموفق بود")) return "فشل تحميل التعليقات";
            if (fa.equals("برای ثبت کامنت وارد حساب شوید")) return "يرجى تسجيل الدخول لإضافة تعليق";
            if (fa.equals("متن کامنت را بنویسید")) return "اكتب نص التعليق";
            if (fa.equals("کامنت ثبت شد")) return "تم إرسال التعليق";
            if (fa.equals("ثبت کامنت ناموفق بود")) return "فشل إرسال التعليق";
            if (fa.equals("واکنش‌دهندگان")) return "المتفاعلون";
            if (fa.equals("بارگذاری واکنش‌ها ناموفق بود")) return "فشل تحميل التفاعلات";
            if (fa.equals("همه")) return "الكل";
            if (fa.equals("بارگذاری تعداد قلب‌ها ناموفق بود")) return "فشل تحميل عدد القلوب";
            if (fa.equals("خطا در برداشتن قلب")) return "حدث خطأ أثناء إزالة القلب";
            if (fa.equals("خطا در ثبت قلب")) return "حدث خطأ أثناء تسجيل القلب";
            if (fa.equals("خطا در بررسی قلب")) return "حدث خطأ أثناء التحقق من القلب";
            if (fa.equals("نام کاربری")) return "اسم المستخدم";
            if (fa.equals("درباره من")) return "نبذة عني";
            if (fa.equals("شهر")) return "المدينة";
            if (fa.equals("تخصص")) return "التخصص";
            if (fa.equals("🤝 درخواست دوستی")) return "🤝 طلب صداقة";
            if (fa.equals("📩 درخواست‌های دوستی")) return "📩 طلبات الصداقة";
            if (fa.equals("پروفایل کاربر")) return "الملف الشخصي";
            if (fa.equals("پروفایل بارگذاری نشد؛ اتصال را بررسی کنید.")) return "تعذّر تحميل الملف الشخصي؛ تحقق من الاتصال.";
            if (fa.equals("این کاربر عکس پروفایل ندارد.")) return "هذا المستخدم ليس لديه صورة شخصية.";
            if (fa.equals("✅ دوست هستید")) return "✅ أنتما صديقان";
            if (fa.equals("⏳ درخواست فرستاده شده")) return "⏳ تم إرسال الطلب";
            if (fa.equals("برای درخواست دوستی اول وارد حساب شوید.")) return "يرجى تسجيل الدخول أولاً لإرسال طلب صداقة.";
            if (fa.equals("درخواست دوستی فرستاده شد.")) return "تم إرسال طلب الصداقة.";
            if (fa.equals("ارسال درخواست ناموفق بود؛ قوانین فایربیس را بررسی کنید.")) return "فشل إرسال الطلب؛ تحقق من قواعد Firebase.";
            if (fa.equals("بررسی درخواست دوستی ناموفق بود.")) return "فشل التحقق من طلب الصداقة.";
            if (fa.equals("در حال دریافت نام کاربر…")) return "جارٍ تحميل اسم المستخدم…";
            if (fa.equals("قبول")) return "قبول";
            if (fa.equals("رد")) return "رفض";
            if (fa.equals("درخواست دوستی جدیدی نداری.")) return "ليس لديك طلبات صداقة جديدة.";
            if (fa.equals("درخواست‌های دوستی")) return "طلبات الصداقة";
            if (fa.equals("دریافت درخواست‌ها ناموفق بود؛ قوانین فایربیس را بررسی کنید.")) return "فشل تحميل الطلبات؛ تحقق من قواعد Firebase.";
            if (fa.equals("✅ قبول شد")) return "✅ تم القبول";
            if (fa.equals("رد شد")) return "تم الرفض";
            if (fa.equals("پاسخ ثبت شد.")) return "تم تسجيل الرد.";
            if (fa.equals("ثبت پاسخ ناموفق بود؛ قوانین فایربیس را بررسی کنید.")) return "فشل تسجيل الرد؛ تحقق من قواعد Firebase.";
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

        auth = FirebaseAuth.getInstance();

        // صفحه تجربه‌ها فقط برای کاربران واردشده قابل دسترسی است.
        if (auth.getCurrentUser() == null) {
            Intent intent = new Intent(ExperienceListActivity.this, AccountActivity.class);
            // پس از ورود موفق، مستقیم به بخش تجربه‌ها برگردد.
            intent.putExtra("open_after_login", "experiences");
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

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
        emblem.setTextSize(14.5f); // آیکن سربرگ کمی کوچک‌تر
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
        subtitle.setText(text("هر تجربه یک درس و هر داستان چراغیست"));
        subtitle.setTextSize(14);
        subtitle.setTextColor(Color.rgb(221, 239, 247));
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(dp(4), 0, dp(4), dp(2));
        header.addView(subtitle);

        layout.addView(header);
        layout.addView(buildPublishForm());

        // پروفایل حساب واردشده؛ در جای عنوان «تجربه‌های کاربران»
        layout.addView(buildMyProfileHeader());

        // بخش داستان‌های ۲۴ ساعته و فهرست دوستان؛ مستقل از فید تجربه‌ها
        layout.addView(buildSectionHeading("داستان‌های ۲۴ ساعته", "✨"));
        HorizontalScrollView storiesScroller = new HorizontalScrollView(this);
        storiesScroller.setHorizontalScrollBarEnabled(false);
        storiesLayout = new LinearLayout(this);
        storiesLayout.setOrientation(LinearLayout.HORIZONTAL);
        storiesLayout.setPadding(0, dp(4), 0, dp(8));
        storiesScroller.addView(storiesLayout);
        layout.addView(storiesScroller);

        layout.addView(buildSectionHeading("دوستان", "👥"));
        HorizontalScrollView friendsScroller = new HorizontalScrollView(this);
        friendsScroller.setHorizontalScrollBarEnabled(false);
        friendsLayout = new LinearLayout(this);
        friendsLayout.setOrientation(LinearLayout.HORIZONTAL);
        friendsLayout.setPadding(0, dp(4), 0, dp(8));
        friendsScroller.addView(friendsLayout);
        layout.addView(friendsScroller);

        // نشانگر کوچک تازه‌سازی با کشیدن صفحه به پایین
        pullRefreshIndicator = new TextView(this);
        pullRefreshIndicator.setText(text("↓ برای تازه‌سازی، صفحه را پایین بکشید"));
        pullRefreshIndicator.setTextSize(13);
        pullRefreshIndicator.setTextColor(Color.rgb(12, 91, 120));
        pullRefreshIndicator.setGravity(Gravity.CENTER);
        pullRefreshIndicator.setPadding(dp(8), dp(9), dp(8), dp(9));
        pullRefreshIndicator.setVisibility(View.GONE);
        layout.addView(pullRefreshIndicator, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        feedLayout = new LinearLayout(this);
        feedLayout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(feedLayout);

        setContentView(scrollView);

        // Pull-to-refresh: فقط وقتی فهرست در بالاترین نقطه است و کاربر رو به پایین می‌کشد.
        scrollView.setOnTouchListener((view, event) -> {
            switch (event.getActionMasked()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    pullStartY = event.getY();
                    pullGestureStarted = scrollView.getScrollY() == 0;
                    break;
                case android.view.MotionEvent.ACTION_MOVE:
                    if (pullGestureStarted && scrollView.getScrollY() == 0) {
                        float distance = event.getY() - pullStartY;
                        if (distance > dp(35) && !isPullRefreshing) {
                            pullRefreshIndicator.setVisibility(View.VISIBLE);
                            pullRefreshIndicator.setText(text("↓ رها کنید تا تازه‌سازی شود"));
                        }
                    }
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    if (pullGestureStarted && scrollView.getScrollY() == 0) {
                        float distance = event.getY() - pullStartY;
                        if (distance >= dp(85) && !isPullRefreshing) {
                            isPullRefreshing = true;
                            pullRefreshIndicator.setVisibility(View.VISIBLE);
                            pullRefreshIndicator.setText(text("در حال تازه‌سازی داستان‌ها، دوستان و تجربه‌ها..."));
                            refreshAllSections();
                        } else {
                            pullRefreshIndicator.setVisibility(View.GONE);
                        }
                    }
                    pullGestureStarted = false;
                    break;
            }
            // اجازه می‌دهیم خود ScrollView اسکرول عادی را انجام دهد.
            return false;
        });

        loadExperiences();
        loadStories();
        loadFriends();
    }

    private View buildSectionHeading(String titleText, String emoji) {
        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.HORIZONTAL);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.setPadding(dp(4), dp(12), dp(4), dp(3));
        TextView title = new TextView(this);
        title.setText(emoji + "  " + text(titleText));
        title.setTextSize(17);
        title.setTextColor(Color.rgb(13, 53, 82));
        title.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        heading.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        return heading;
    }

    private void refreshAllSections() {
        if (refreshBatchActive) return;
        refreshBatchActive = true;
        refreshTasksRemaining = 3;
        loadExperiences();
        loadStories();
        loadFriends();
    }

    private void finishRefreshTask() {
        if (!refreshBatchActive) return;
        refreshTasksRemaining--;
        if (refreshTasksRemaining <= 0) {
            refreshBatchActive = false;
            isPullRefreshing = false;
            if (pullRefreshIndicator != null) pullRefreshIndicator.setVisibility(View.GONE);
        }
    }

    private void loadStories() {
        if (storiesLayout == null) return;
        storiesLayout.removeAllViews();
        addStoryCreateTile();
        Timestamp now = Timestamp.now();
        db.collection("stories").whereGreaterThan("expiresAt", now)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (storiesLayout == null) return;
                    List<DocumentSnapshot> active = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Timestamp expiry = doc.getTimestamp("expiresAt");
                        if (expiry != null && expiry.toDate().after(new Date())) active.add(doc);
                    }
                    Collections.sort(active, (a, b) -> {
                        Timestamp ta = a.getTimestamp("createdAt");
                        Timestamp tb = b.getTimestamp("createdAt");
                        long va = ta == null ? 0L : ta.toDate().getTime();
                        long vb = tb == null ? 0L : tb.toDate().getTime();
                        return Long.compare(vb, va);
                    });
                    for (DocumentSnapshot story : active) addStoryTile(story);
                    finishRefreshTask();
                })
                .addOnFailureListener(error -> {
                    // در حالت آفلاین، نسخه کش‌شده داستان‌ها نیز نمایش داده شود.
                    db.collection("stories").whereGreaterThan("expiresAt", Timestamp.now())
                            .get(Source.CACHE)
                            .addOnSuccessListener(cached -> {
                                if (storiesLayout != null) {
                                    for (DocumentSnapshot story : cached.getDocuments()) {
                                        Timestamp expiry = story.getTimestamp("expiresAt");
                                        if (expiry != null && expiry.toDate().after(new Date())) addStoryTile(story);
                                    }
                                }
                                finishRefreshTask();
                            })
                            .addOnFailureListener(e -> finishRefreshTask());
                });
    }

    private void addStoryCreateTile() {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(10), dp(8), dp(10), dp(8));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(17));
        bg.setStroke(dp(1), Color.rgb(205, 220, 228));
        tile.setBackground(bg);
        TextView plus = new TextView(this);
        plus.setText("＋");
        plus.setTextSize(30);
        plus.setTextColor(Color.rgb(13, 113, 132));
        plus.setGravity(Gravity.CENTER);
        tile.addView(plus, new LinearLayout.LayoutParams(dp(58), dp(58)));
        TextView label = new TextView(this);
        label.setText(text("داستان من"));
        label.setTextSize(12);
        label.setTextColor(Color.rgb(34, 57, 69));
        tile.addView(label);
        tile.setOnClickListener(v -> showCreateStoryDialog());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(88), dp(112));
        lp.setMargins(0, 0, dp(9), 0);
        storiesLayout.addView(tile, lp);
    }

    private void addStoryTile(DocumentSnapshot story) {
        String uid = story.getString("userId");
        String storyText = firstNonEmpty(story.getString("text"), "داستان تصویری");
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(8), dp(8), dp(8), dp(8));
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(13, 69, 104), Color.rgb(21, 128, 132)});
        bg.setCornerRadius(dp(17));
        bg.setStroke(dp(2), Color.rgb(218, 183, 93));
        tile.setBackground(bg);
        TextView icon = new TextView(this);
        icon.setText("✦");
        icon.setTextColor(Color.WHITE);
        icon.setTextSize(25);
        icon.setGravity(Gravity.CENTER);
        tile.addView(icon, new LinearLayout.LayoutParams(dp(58), dp(52)));
        TextView preview = new TextView(this);
        preview.setText(storyText);
        preview.setTextColor(Color.WHITE);
        preview.setTextSize(11);
        preview.setMaxLines(2);
        preview.setGravity(Gravity.CENTER);
        tile.addView(preview);
        TextView authorName = new TextView(this);
        authorName.setText(text("کاربر"));
        authorName.setTextColor(Color.rgb(238, 247, 250));
        authorName.setTextSize(9);
        authorName.setGravity(Gravity.CENTER);
        authorName.setMaxLines(1);
        tile.addView(authorName);
        tile.setOnClickListener(v -> showStory(story));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(88), dp(112));
        lp.setMargins(0, 0, dp(9), 0);
        storiesLayout.addView(tile, lp);
        if (uid != null) {
            db.collection("users").document(uid).get().addOnSuccessListener(user -> {
                String name = firstNonEmpty(user.getString("name"), user.getString("username"), "کاربر");
                authorName.setText(name);
                preview.setContentDescription(name + "، " + storyText);
            });
        }
    }

    private void showCreateStoryDialog() {
        EditText input = new EditText(this);
        input.setHint(text("داستان کوتاه خود را بنویسید..."));
        input.setMinLines(3);
        input.setMaxLines(6);
        input.setGravity(Gravity.TOP | Gravity.RIGHT);
        input.setPadding(dp(14), dp(12), dp(14), dp(12));
        new AlertDialog.Builder(this)
                .setTitle(text("داستان جدید"))
                .setView(input)
                .setNegativeButton(text("لغو"), null)
                .setPositiveButton(text("انتشار"), (dialog, which) -> {
                    String value = input.getText().toString().trim();
                    if (value.isEmpty()) {
                        Toast.makeText(this, text("متن داستان خالی است."), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (value.length() > 2000) {
                        Toast.makeText(this, text("حداکثر طول داستان ۲۰۰۰ حرف است."), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    publishTextStory(value);
                }).show();
    }

    private void publishTextStory(String value) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, text("برای انتشار داستان ابتدا وارد حساب شوید."), Toast.LENGTH_SHORT).show();
            return;
        }

        String cleanText = value == null ? "" : value.trim();
        if (cleanText.isEmpty()) {
            Toast.makeText(this, text("متن داستان خالی است."), Toast.LENGTH_SHORT).show();
            return;
        }
        if (cleanText.length() > 2000) {
            Toast.makeText(this, text("حداکثر طول داستان ۲۰۰۰ حرف است."), Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> story = new HashMap<>();
        story.put("userId", user.getUid());
        story.put("text", cleanText);
        story.put("mediaType", "text");
        // createdAt از زمان معتبر سرور ثبت می‌شود. برای رعایت محدودیت ۲۴ ساعته Rules،
        // زمان انقضا کمی کمتر از ۲۴ ساعت تنظیم می‌شود تا اختلاف ساعت معمول گوشی باعث رد درخواست نشود.
        // برای ۲۴ ساعت دقیق از لحظه ثبت سرور، باید expiresAt در Cloud Function ساخته شود.
        long safeExpiryMillis = Timestamp.now().toDate().getTime() + 23L * 60L * 60L * 1000L + 50L * 60L * 1000L;
        story.put("createdAt", FieldValue.serverTimestamp());
        story.put("expiresAt", new Timestamp(new Date(safeExpiryMillis)));

        db.collection("stories").add(story)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, text("داستان منتشر شد."), Toast.LENGTH_SHORT).show();
                    loadStories();
                })
                .addOnFailureListener(e -> {
                    Log.e("TajrobehhaStories", "Story publish failed", e);
                    Toast.makeText(this, text("انتشار داستان ناموفق بود: ") + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showStory(DocumentSnapshot story) {
        Timestamp expiry = story.getTimestamp("expiresAt");
        if (expiry == null || !expiry.toDate().after(new Date())) {
            Toast.makeText(this, text("زمان این داستان تمام شده است."), Toast.LENGTH_SHORT).show();
            loadStories();
            return;
        }
        String uid = story.getString("userId");
        String body = firstNonEmpty(story.getString("text"), "");
        String title = text("داستان");
        if (uid != null) {
            db.collection("users").document(uid).get().addOnSuccessListener(user -> {
                String name = firstNonEmpty(user.getString("name"), user.getString("username"), text("کاربر"));
                TextView content = new TextView(this);
                content.setText(body + "\n\n" + text("این داستان پس از ۲۴ ساعت ناپدید می‌شود."));
                content.setTextSize(18);
                content.setTextColor(Color.rgb(20, 48, 65));
                content.setGravity(Gravity.CENTER);
                content.setPadding(dp(22), dp(28), dp(22), dp(28));
                new AlertDialog.Builder(this).setTitle(name + " · " + title).setView(content)
                        .setPositiveButton(text("بستن"), null).show();
            });
        }
    }

    private void loadFriends() {
        if (friendsLayout == null) return;
        friendsLayout.removeAllViews();
        String myId = auth.getCurrentUser() == null ? "" : auth.getCurrentUser().getUid();
        if (myId.isEmpty()) { finishRefreshTask(); return; }
        final Set<String> friendIds = new HashSet<>();
        final int[] completed = {0};
        com.google.firebase.firestore.Query[] queries = new com.google.firebase.firestore.Query[]{
                db.collection("friendRequests").whereEqualTo("senderId", myId),
                db.collection("friendRequests").whereEqualTo("receiverId", myId)
        };
        for (com.google.firebase.firestore.Query query : queries) {
            query.get().addOnSuccessListener(snapshot -> {
                for (DocumentSnapshot request : snapshot.getDocuments()) {
                    if (!"accepted".equals(request.getString("status"))) continue;
                    String sender = request.getString("senderId");
                    String receiver = request.getString("receiverId");
                    String other = myId.equals(sender) ? receiver : sender;
                    if (other != null && !other.isEmpty() && !myId.equals(other)) friendIds.add(other);
                }
                if (++completed[0] == 2) displayFriends(friendIds);
            }).addOnFailureListener(error -> {
                if (++completed[0] == 2) displayFriends(friendIds);
            });
        }
    }

    private void displayFriends(Set<String> friendIds) {
        if (friendsLayout == null) return;
        friendsLayout.removeAllViews();
        if (friendIds.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(text("هنوز دوستی اضافه نشده است."));
            empty.setTextColor(Color.rgb(100, 117, 126));
            empty.setTextSize(13);
            empty.setPadding(dp(10), dp(15), dp(10), dp(15));
            friendsLayout.addView(empty);
            finishRefreshTask();
            return;
        }
        for (String uid : friendIds) {
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setPadding(dp(7), dp(7), dp(7), dp(7));
            ImageView avatar = new ImageView(this);
            avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
            GradientDrawable circle = new GradientDrawable();
            circle.setColor(Color.rgb(221, 239, 242));
            circle.setShape(GradientDrawable.OVAL);
            circle.setStroke(dp(2), Color.rgb(18, 130, 137));
            avatar.setBackground(circle);
            avatar.setClipToOutline(true);
            item.addView(avatar, new LinearLayout.LayoutParams(dp(54), dp(54)));
            TextView name = new TextView(this);
            name.setText(text("کاربر"));
            name.setTextSize(11);
            name.setTextColor(Color.rgb(34, 57, 69));
            name.setGravity(Gravity.CENTER);
            name.setMaxWidth(dp(78));
            name.setMaxLines(1);
            item.addView(name);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(78), dp(91));
            lp.setMargins(0, 0, dp(6), 0);
            friendsLayout.addView(item, lp);
            db.collection("users").document(uid).get().addOnSuccessListener(user -> {
                String displayName = firstNonEmpty(user.getString("name"), user.getString("username"), user.getString("displayName"), text("کاربر"));
                String photo = firstNonEmpty(user.getString("photoUrl"), user.getString("profilePhoto"), user.getString("profilePhotoUrl"), user.getString("photo"), "");
                name.setText(displayName);
                if (!photo.isEmpty()) loadProfileImage(photo, avatar);
                View.OnClickListener open = v -> showPublicProfile(uid, displayName, photo);
                item.setOnClickListener(open);
            });
        }
        finishRefreshTask();
    }

    // نمایش عکس و نام حساب فعلی در بالای فهرست تجربه‌ها
    private View buildMyProfileHeader() {
        LinearLayout profileCard = new LinearLayout(this);
        profileCard.setOrientation(LinearLayout.HORIZONTAL);
        profileCard.setGravity(Gravity.CENTER_VERTICAL);
        profileCard.setPadding(dp(14), dp(11), dp(14), dp(11));

        GradientDrawable cardBackground = new GradientDrawable();
        cardBackground.setColor(Color.WHITE);
        cardBackground.setCornerRadius(dp(18));
        cardBackground.setStroke(dp(1), Color.rgb(210, 230, 235));
        profileCard.setBackground(cardBackground);
        profileCard.setElevation(dp(2));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, dp(12), 0, dp(8));
        profileCard.setLayoutParams(cardParams);

        ImageView avatar = new ImageView(this);
        avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        GradientDrawable avatarBackground = new GradientDrawable();
        avatarBackground.setColor(Color.rgb(224, 241, 246));
        avatarBackground.setShape(GradientDrawable.OVAL);
        avatarBackground.setStroke(dp(2), Color.rgb(12, 91, 120));
        avatar.setBackground(avatarBackground);
        avatar.setClipToOutline(true);
        profileCard.addView(avatar, new LinearLayout.LayoutParams(dp(52), dp(52))); // آواتار بالای صفحه حدود ۱۰٪ کوچک‌تر

        LinearLayout nameColumn = new LinearLayout(this);
        nameColumn.setOrientation(LinearLayout.VERTICAL);
        nameColumn.setGravity(Gravity.CENTER_VERTICAL);
        nameColumn.setPadding(dp(12), 0, dp(5), 0);

        TextView nameView = new TextView(this);
        nameView.setText(text("کاربر"));
        nameView.setTextSize(17);
        nameView.setTextColor(Color.rgb(8, 65, 90));
        nameView.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        nameView.setSingleLine(true);
        nameView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        nameColumn.addView(nameView);

        TextView profileHint = new TextView(this);
        profileHint.setText(text("پروفایل من"));
        profileHint.setTextSize(12);
        profileHint.setTextColor(Color.rgb(105, 125, 132));
        profileHint.setPadding(0, dp(3), 0, 0);
        nameColumn.addView(profileHint);

        profileCard.addView(nameColumn, new LinearLayout.LayoutParams(
                0, dp(58), 1));

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            final String uid = currentUser.getUid();
            String fallbackName = firstNonEmpty(
                    currentUser.getDisplayName(),
                    text("کاربر"));

            nameView.setText(fallbackName);

            View.OnClickListener openFallbackProfile =
                    v -> showPublicProfile(uid, fallbackName, "");
            profileCard.setOnClickListener(openFallbackProfile);
            avatar.setOnClickListener(openFallbackProfile);
            nameView.setOnClickListener(openFallbackProfile);

            db.collection("users").document(uid).get()
                    .addOnSuccessListener(userDocument -> {
                        if (!userDocument.exists()) return;

                        String name = firstNonEmpty(
                                userDocument.getString("name"),
                                userDocument.getString("username"),
                                userDocument.getString("displayName"),
                                fallbackName);

                        String photoUrl = firstNonEmpty(
                                userDocument.getString("photoUrl"),
                                userDocument.getString("profilePhoto"),
                                userDocument.getString("profilePhotoUrl"),
                                userDocument.getString("photo"),
                                "");

                        nameView.setText(name);
                        if (!photoUrl.isEmpty()) {
                            loadProfileImage(photoUrl, avatar);
                        }

                        View.OnClickListener openMyProfile =
                                v -> showPublicProfile(uid, name, photoUrl);
                        profileCard.setOnClickListener(openMyProfile);
                        avatar.setOnClickListener(openMyProfile);
                        nameView.setOnClickListener(openMyProfile);
                    });
        }

        return profileCard;
    }

    private LinearLayout buildPublishForm() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(16), dp(15), dp(16), dp(16));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(20));
        bg.setStroke(dp(1), Color.rgb(210, 230, 235));
        form.setBackground(bg);
        form.setElevation(dp(2));
        LinearLayout.LayoutParams formParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        formParams.setMargins(0, 0, 0, dp(16));
        form.setLayoutParams(formParams);

        TextView formTitle = new TextView(this);
        formTitle.setText(text("✍️ ثبت تجربه جدید"));
        formTitle.setTextSize(20);
        formTitle.setTextColor(Color.rgb(12, 73, 101));
        formTitle.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        formTitle.setGravity(Gravity.CENTER);
        formTitle.setPadding(0, 0, 0, dp(12));
        form.addView(formTitle);

        Button chooseImageButton = new Button(this);
        chooseImageButton.setText(text("🖼 انتخاب عکس"));
        chooseImageButton.setTextSize(14);
        stylePublishButton(chooseImageButton);
        form.addView(chooseImageButton);

        imageStatus = new TextView(this);
        imageStatus.setText(text("عکس انتخاب نشده است"));
        imageStatus.setTextSize(12);
        imageStatus.setTextColor(Color.rgb(80, 100, 110));
        imageStatus.setGravity(Gravity.CENTER);
        imageStatus.setPadding(0, 0, 0, dp(7));
        form.addView(imageStatus);

        imagePreview = new ImageView(this);
        imagePreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imagePreview.setVisibility(View.GONE);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(180));
        previewParams.setMargins(0, 0, 0, dp(10));
        form.addView(imagePreview, previewParams);

        chooseImageButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            try {
                startActivityForResult(Intent.createChooser(intent, text("انتخاب عکس تجربه")),
                        REQUEST_PICK_EXPERIENCE_IMAGE);
            } catch (Exception e) {
                Toast.makeText(this, text("باز کردن گالری ممکن نشد"), Toast.LENGTH_SHORT).show();
            }
        });

        experienceTitleField = new EditText(this);
        experienceTitleField.setSingleLine(true);
        experienceTitleField.setTextSize(16);
        experienceTitleField.setHint(text("عنوان تجربه را بنویسید"));
        experienceTitleField.setPadding(dp(12), dp(10), dp(12), dp(10));
        form.addView(experienceTitleField);

        experienceTextField = new EditText(this);
        experienceTextField.setTextSize(16);
        experienceTextField.setGravity(Gravity.TOP | Gravity.RIGHT);
        experienceTextField.setMinLines(5);
        experienceTextField.setHint(text("تجربه خود را با دیگران شریک کنید..."));
        experienceTextField.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(150));
        bodyParams.setMargins(0, dp(10), 0, dp(10));
        form.addView(experienceTextField, bodyParams);

        publishButton = new Button(this);
        publishButton.setText(text("🚀 انتشار تجربه"));
        publishButton.setTextSize(15);
        stylePublishButton(publishButton);
        form.addView(publishButton);
        publishButton.setOnClickListener(v -> publishFromInlineForm());
        return form;
    }

    private void stylePublishButton(Button button) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(Color.rgb(12, 73, 101));
        shape.setCornerRadius(dp(18));
        button.setBackground(shape);
        button.setTextColor(Color.WHITE);
        button.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
    }

    private void publishFromInlineForm() {
        if (isPublishing) return;
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, text("لطفاً ابتدا وارد اکانت خود شوید"), Toast.LENGTH_LONG).show();
            return;
        }
        String titleText = experienceTitleField.getText().toString().trim();
        String experience = experienceTextField.getText().toString().trim();
        if (titleText.isEmpty() || experience.isEmpty()) {
            Toast.makeText(this, text("لطفاً عنوان و متن تجربه را وارد کنید"), Toast.LENGTH_SHORT).show();
            return;
        }
        if (titleText.length() > 150) {
            Toast.makeText(this, text("عنوان تجربه خیلی طولانی است"), Toast.LENGTH_SHORT).show();
            return;
        }
        if (experience.length() > 10000) {
            Toast.makeText(this, text("متن تجربه خیلی طولانی است"), Toast.LENGTH_SHORT).show();
            return;
        }
        isPublishing = true;
        publishButton.setEnabled(false);
        publishButton.setAlpha(0.6f);
        publishButton.setText(text("⏳ در حال بررسی..."));
        String contentKey = createContentKey(user.getUid(), titleText, experience);
        db.collection("experiences").whereEqualTo("contentKey", contentKey).limit(1).get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        resetPublishButton();
                        Toast.makeText(this, text("این تجربه قبلاً منتشر شده است ❤️"), Toast.LENGTH_LONG).show();
                        return;
                    }
                    publishInlineExperience(user, titleText, experience, contentKey);
                })
                .addOnFailureListener(e -> {
                    resetPublishButton();
                    Toast.makeText(this, text("خطا در بررسی تجربه: ") + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void publishInlineExperience(FirebaseUser user, String titleText,
                                         String experience, String contentKey) {
        if (selectedImageUri != null) {
            publishButton.setText(text("⏳ در حال بارگذاری عکس..."));
            uploadExperienceImage(selectedImageUri, new ImageUploadCallback() {
                @Override public void onSuccess(String imageUrl) {
                    saveInlineExperience(user, titleText, experience, contentKey, imageUrl);
                }
                @Override public void onError(String error) {
                    resetPublishButton();
                    Toast.makeText(ExperienceListActivity.this,
                            text("بارگذاری عکس ناموفق بود: ") + error, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            saveInlineExperience(user, titleText, experience, contentKey, null);
        }
    }

    private void saveInlineExperience(FirebaseUser user, String titleText,
                                      String experience, String contentKey, String imageUrl) {
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("title", titleText);
        data.put("text", experience);
        data.put("userId", user.getUid());
        // Do not copy the account email into publicly readable experience documents.
        data.put("contentKey", contentKey);
        data.put("likesCount", 0L);
        data.put("timestamp", FieldValue.serverTimestamp());
        if (imageUrl != null && !imageUrl.trim().isEmpty()) data.put("imageUrl", imageUrl);
        db.collection("experiences").add(data)
                .addOnSuccessListener(ref -> {
                    experienceTitleField.setText("");
                    experienceTextField.setText("");
                    selectedImageUri = null;
                    imagePreview.setImageDrawable(null);
                    imagePreview.setVisibility(View.GONE);
                    imageStatus.setText(text("عکس انتخاب نشده است"));
                    resetPublishButton();
                    Toast.makeText(this, text("تجربه با موفقیت منتشر شد! 🎉"), Toast.LENGTH_LONG).show();
                    loadExperiences();
                })
                .addOnFailureListener(e -> {
                    resetPublishButton();
                    Toast.makeText(this, text("خطا در انتشار تجربه: ") + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void resetPublishButton() {
        isPublishing = false;
        if (publishButton != null) {
            publishButton.setEnabled(true);
            publishButton.setAlpha(1f);
            publishButton.setText(text("🚀 انتشار تجربه"));
        }
    }

    private interface ImageUploadCallback {
        void onSuccess(String imageUrl);
        void onError(String error);
    }

    private void loadExperiences() {

        displayedExperienceKeys.clear();

        db.collection("experiences")
                .orderBy(
                        "timestamp",
                        Query.Direction.DESCENDING
                )
                .get()
                .addOnSuccessListener(this::displayExperienceSnapshot)
                .addOnFailureListener(this::loadCachedExperiencesAfterFailure);
    }

    // در صورت قطع اینترنت، فقط تجربه‌هایی را نشان می‌دهد که قبلاً روی همین گوشی کش شده‌اند.
    private void loadCachedExperiencesAfterFailure(Exception networkError) {
        db.collection("experiences")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get(Source.CACHE)
                .addOnSuccessListener(cached -> {
                    if (cached != null && !cached.isEmpty()) {
                        displayExperienceSnapshot(cached);
                    } else {
                        finishRefreshTask();
                        if (!refreshBatchActive) {
                            isPullRefreshing = false;
                            if (pullRefreshIndicator != null) pullRefreshIndicator.setVisibility(View.GONE);
                        }
                        showEmptyMessage();
                        Toast.makeText(this, text("خطا در دریافت تجربه‌ها"), Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(cacheError -> {
                    finishRefreshTask();
                    if (!refreshBatchActive) {
                        isPullRefreshing = false;
                        if (pullRefreshIndicator != null) pullRefreshIndicator.setVisibility(View.GONE);
                    }
                    Toast.makeText(this, text("خطا در دریافت تجربه‌ها"), Toast.LENGTH_SHORT).show();
                    Log.w("ExperiencesOffline", "No cached feed available", cacheError);
                });
    }

    private void displayExperienceSnapshot(com.google.firebase.firestore.QuerySnapshot queryDocumentSnapshots) {
        finishRefreshTask();
        if (!refreshBatchActive) {
            isPullRefreshing = false;
            if (pullRefreshIndicator != null) pullRefreshIndicator.setVisibility(View.GONE);
        }
        feedLayout.removeAllViews();
        displayedExperienceKeys.clear();

        if (queryDocumentSnapshots == null || queryDocumentSnapshots.isEmpty()) {
            showEmptyMessage();
            return;
        }

        FirebaseUser currentUser = auth.getCurrentUser();
        String currentUserId = currentUser == null ? null : currentUser.getUid();
        int displayedCount = 0;
        for (DocumentSnapshot document : queryDocumentSnapshots) {
            String uniqueKey = getExperienceUniqueKey(document);
            if (displayedExperienceKeys.contains(uniqueKey)) continue;
            displayedExperienceKeys.add(uniqueKey);
            createExperienceCard(document, currentUserId);
            displayedCount++;
        }
        if (displayedCount == 0) showEmptyMessage();
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

    private String createContentKey(
            String userId,
            String title,
            String text
    ) {

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
    
    private void uploadExperienceImage(Uri uri, ImageUploadCallback callback) {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            callback.onError(text("لطفاً ابتدا وارد حساب خود شوید"));
            return;
        }

        user.getIdToken(false)
                .addOnSuccessListener(tokenResult -> {
                    String idToken = tokenResult.getToken();

                    if (idToken == null || idToken.isEmpty()) {
                        callback.onError(text("توکن ورود دریافت نشد"));
                        return;
                    }

                    new Thread(() -> {
                        HttpURLConnection connection = null;

                        try {
                            String mime = getContentResolver().getType(uri);
                            if (mime == null ||
                                    !("image/jpeg".equals(mime)
                                    || "image/png".equals(mime)
                                    || "image/webp".equals(mime))) {
                                mime = "image/jpeg";
                            }

                            byte[] imageBytes;
                            try (InputStream input =
                                         getContentResolver().openInputStream(uri);
                                 ByteArrayOutputStream buffer =
                                         new ByteArrayOutputStream()) {
                                if (input == null) {
                                    throw new Exception(text("فایل عکس قابل خواندن نیست"));
                                }

                                byte[] chunk = new byte[8192];
                                int count;
                                int total = 0;
                                final int maxBytes = 4 * 1024 * 1024;

                                while ((count = input.read(chunk)) != -1) {
                                    total += count;
                                    if (total > maxBytes) {
                                        throw new Exception(
                                                text("حجم عکس باید کمتر از ۴ مگابایت باشد"));
                                    }
                                    buffer.write(chunk, 0, count);
                                }
                                imageBytes = buffer.toByteArray();
                            }

                            JSONObject requestBody = new JSONObject();
                            requestBody.put(
                                    "imageBase64",
                                    Base64.encodeToString(
                                            imageBytes,
                                            Base64.NO_WRAP
                                    )
                            );
                            requestBody.put("mimeType", mime);

                            URL url = new URL(EXPERIENCE_UPLOAD_FUNCTION);
                            connection = (HttpURLConnection) url.openConnection();
                            connection.setRequestMethod("POST");
                            connection.setDoOutput(true);
                            connection.setConnectTimeout(30000);
                            connection.setReadTimeout(60000);
                            connection.setRequestProperty(
                                    "apikey", SUPABASE_PUBLISHABLE_KEY);
                            connection.setRequestProperty(
                                    "Authorization", "Bearer " + idToken);
                            connection.setRequestProperty(
                                    "Content-Type", "application/json; charset=UTF-8");
                            connection.setRequestProperty("Accept", "application/json");

                            byte[] body = requestBody.toString().getBytes("UTF-8");
                            try (OutputStream output = connection.getOutputStream()) {
                                output.write(body);
                                output.flush();
                            }

                            int responseCode = connection.getResponseCode();
                            InputStream responseStream =
                                    responseCode >= 200 && responseCode < 300
                                            ? connection.getInputStream()
                                            : connection.getErrorStream();

                            StringBuilder responseText = new StringBuilder();
                            if (responseStream != null) {
                                try (BufferedReader reader = new BufferedReader(
                                        new InputStreamReader(
                                                responseStream, "UTF-8"))) {
                                    String line;
                                    while ((line = reader.readLine()) != null) {
                                        responseText.append(line);
                                    }
                                }
                            }

                            JSONObject responseJson =
                                    new JSONObject(responseText.toString());

                            if (responseCode >= 200 && responseCode < 300
                                    && responseJson.optBoolean("success", false)) {
                                String imageUrl =
                                        responseJson.optString("imageUrl", "");
                                if (imageUrl.isEmpty()) {
                                    throw new Exception(
                                            text("نشانی عکس از سرور دریافت نشد"));
                                }
                                runOnUiThread(() -> callback.onSuccess(imageUrl));
                            } else {
                                String error = responseJson.optString(
                                        "error", "HTTP " + responseCode);
                                runOnUiThread(() -> callback.onError(error));
                            }

                        } catch (Exception e) {
                            String error = e.getMessage() == null
                                    ? text("خطای ناشناخته در آپلود عکس")
                                    : e.getMessage();
                            runOnUiThread(() -> callback.onError(error));
                        } finally {
                            if (connection != null) {
                                connection.disconnect();
                            }
                        }
                    }).start();
                })
                .addOnFailureListener(e ->
                        callback.onError(text("دریافت توکن Firebase ناموفق بود")));
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_PICK_EXPERIENCE_IMAGE
                && resultCode == RESULT_OK
                && data != null
                && data.getData() != null) {
            selectedImageUri = data.getData();
            try {
                imagePreview.setImageURI(selectedImageUri);
                imagePreview.setVisibility(View.VISIBLE);
                imageStatus.setText(text("عکس انتخاب شد؛ هنگام انتشار بارگذاری می‌شود"));
            } catch (Exception e) {
                selectedImageUri = null;
                imagePreview.setVisibility(View.GONE);
                imageStatus.setText(text("خواندن عکس ممکن نشد"));
                Toast.makeText(this, text("خواندن عکس ممکن نشد"), Toast.LENGTH_SHORT).show();
            }
        }
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
                text("هنوز تجربه‌ای منتشر نشده است.\n\n") +
                        text("اولین تجربه خود را ثبت کنید! ✍️")
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

        feedLayout.addView(empty);
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
            // نمایش یکپارچه مانند فید فیسبوک؛ بدون نوار یا حاشیه خالی اطراف عکس.
            experienceImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
            experienceImage.setAdjustViewBounds(false);
            experienceImage.setBackgroundColor(Color.TRANSPARENT);
            LinearLayout.LayoutParams experienceImageParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(420));
            experienceImageParams.setMargins(dp(8), dp(8), dp(8), dp(14));
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
        likeButton.setText(text("👍 واکنش"));
        likeButton.setTextSize(14);
        likeButton.setTextColor(Color.rgb(22, 105, 177));
        likeButton.setGravity(Gravity.CENTER);
        likeButton.setPadding(dp(8), 0, dp(8), 0);
        likeButton.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        likeButton.setBackground(createReactionBackground(
                Color.rgb(239, 247, 255), Color.rgb(193, 220, 246)));
        likeRow.addView(likeButton, new LinearLayout.LayoutParams(dp(92), dp(36)));

        TextView likeCount = new TextView(this);
        likeCount.setText(text("۰")); likeCount.setTextSize(13);
        likeCount.setTextColor(Color.rgb(105, 105, 105));
        likeCount.setGravity(Gravity.CENTER_VERTICAL);
        likeCount.setPadding(dp(4), 0, dp(8), 0);
        likeRow.addView(likeCount, new LinearLayout.LayoutParams(dp(25), dp(36)));

        TextView heartCount = new TextView(this);
        heartCount.setText(text("۰")); heartCount.setTextSize(13);
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
                    .setTitle(text("واکنش خود را انتخاب کنید"))
                    .setItems(new String[]{text("👍 لایک"), text("❤️ قلب")}, (dlg, which) -> {
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
        commentsButton.setText(text("💬 کامنت"));
        commentsButton.setTextSize(14);
        commentsButton.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        commentsButton.setTextColor(Color.rgb(12, 91, 120));
        commentsButton.setGravity(Gravity.CENTER);
        commentsButton.setPadding(dp(10), dp(8), dp(10), dp(8));
        commentsButton.setBackground(createReactionBackground(
                Color.rgb(235, 248, 250), Color.rgb(167, 218, 224)));

        TextView shareButtonCard = new TextView(this);
        shareButtonCard.setText(text("↗ اشتراک‌گذاری"));
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

        feedLayout.addView(card);
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
                if (bitmap == null) throw new IOException(text("تصویر قابل خواندن نیست"));

                String fileName = "Tajrobehha_" + System.currentTimeMillis() + ".jpg";
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                values.put(MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/Tajrobehha");
                values.put(MediaStore.Images.Media.IS_PENDING, 1);

                android.net.Uri imageUri = getContentResolver().insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (imageUri == null) throw new IOException(text("ذخیره‌سازی شروع نشد"));

                try (OutputStream output = getContentResolver().openOutputStream(imageUri)) {
                    if (output == null || !bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)) {
                        getContentResolver().delete(imageUri, null, null);
                        throw new IOException(text("نوشتن تصویر انجام نشد"));
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

    private String experienceCacheKey(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes("UTF-8"));
            StringBuilder out = new StringBuilder();
            for (byte b : digest) out.append(String.format(java.util.Locale.US, "%02x", b));
            return out.toString();
        } catch (Exception e) {
            return Integer.toHexString(String.valueOf(value).hashCode());
        }
    }

    // تصاویر مشاهده‌شده در فضای دائمی خصوصی برنامه ذخیره می‌شوند تا آفلاین باقی بمانند.
    private File experienceCacheDir(String type) {
        String folderName = "tajro_" + type + "_cache";
        File dir = new File(getFilesDir(), folderName);
        if (!dir.exists()) {
            // کش نسخه قبلی را به محل دائمی منتقل می‌کنیم تا دانلودهای قبلی از دست نروند.
            File legacy = new File(getCacheDir(), folderName);
            if (legacy.isDirectory()) {
                //noinspection ResultOfMethodCallIgnored
                dir.mkdirs();
                File[] oldFiles = legacy.listFiles();
                if (oldFiles != null) {
                    for (File oldFile : oldFiles) {
                        File destination = new File(dir, oldFile.getName());
                        if (!destination.exists()) oldFile.renameTo(destination);
                    }
                }
            }
        }
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    // ذخیره دائمی عکس؛ دانلود نیمه‌تمام با Range در تلاش بعدی ادامه می‌یابد.
    private File cacheExperienceImage(String url) {
        File dir = experienceCacheDir("images");
        File target = new File(dir, experienceCacheKey(url));
        if (target.isFile() && target.length() > 0) {
            target.setLastModified(System.currentTimeMillis());
            return target;
        }
        HttpURLConnection connection = null;
        File temp = new File(dir, target.getName() + ".tmp");
        try {
            long offset = temp.isFile() ? temp.length() : 0L;
            if (offset > 15L * 1024L * 1024L) { temp.delete(); offset = 0L; }
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(20000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("Accept-Encoding", "identity");
            if (offset > 0) connection.setRequestProperty("Range", "bytes=" + offset + "-");
            int code = connection.getResponseCode();
            if (offset > 0 && code == HttpURLConnection.HTTP_OK) {
                temp.delete();
                offset = 0L;
            } else if (offset > 0 && code != HttpURLConnection.HTTP_PARTIAL) {
                return null;
            } else if (offset == 0 && (code < 200 || code >= 300)) {
                return null;
            }
            long declared = connection.getContentLengthLong();
            if (declared > 15L * 1024L * 1024L || offset + Math.max(0L, declared) > 15L * 1024L * 1024L) return null;
            long total = offset;
            try (InputStream in = connection.getInputStream();
                 FileOutputStream out = new FileOutputStream(temp, offset > 0)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = in.read(buffer)) != -1) {
                    total += count;
                    if (total > 15L * 1024L * 1024L) throw new IOException("Image too large");
                    out.write(buffer, 0, count);
                }
                out.flush();
                out.getFD().sync();
            }
            if (total == 0) { temp.delete(); return null; }
            String contentRange = connection.getHeaderField("Content-Range");
            if (code == HttpURLConnection.HTTP_PARTIAL && contentRange != null) {
                int slash = contentRange.lastIndexOf('/');
                if (slash > 0) {
                    try {
                        long expected = Long.parseLong(contentRange.substring(slash + 1).trim());
                        if (expected > total || expected != total) return null;
                    } catch (NumberFormatException ignored) { }
                }
            }
            if (!temp.renameTo(target)) return null;
            return target;
        } catch (Exception e) {
            // فایل .tmp حفظ می‌شود تا پس از برگشت اینترنت ادامه دانلود ممکن باشد.
            Log.w("ExperiencesOffline", "Image download interrupted; partial file retained", e);
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private void trimExperienceCache(File dir, long maxBytes) {
        File[] files = dir.listFiles();
        if (files == null) return;
        long total = 0;
        for (File file : files) if (file.isFile() && !file.getName().endsWith(".tmp")) total += file.length();
        java.util.Arrays.sort(files, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
        for (File file : files) {
            if (total <= maxBytes) break;
            if (!file.isFile()) continue;
            long size = file.length();
            if (file.delete()) total -= size;
        }
    }

    private void loadExperienceImage(String imageUrl, ImageView imageView) {
        if (imageUrl == null || imageUrl.trim().isEmpty() || imageView == null) return;
        final String key = imageUrl.trim();
        imageView.setTag(key);
        IMAGE_EXECUTOR.execute(() -> {
            Bitmap bitmap = null;
            try {
                File cached = cacheExperienceImage(key);
                if (cached != null) {
                    BitmapFactory.Options bounds = new BitmapFactory.Options();
                    bounds.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(cached.getAbsolutePath(), bounds);
                    int sample = 1;
                    while (bounds.outWidth / sample > 1200 || bounds.outHeight / sample > 1800) sample *= 2;
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = sample;
                    options.inPreferredConfig = Bitmap.Config.RGB_565;
                    bitmap = BitmapFactory.decodeFile(cached.getAbsolutePath(), options);
                }
            } catch (Exception ignored) { }
            final Bitmap loaded = bitmap;
            if (loaded != null) runOnUiThread(() -> {
                if (!isFinishing() && key.equals(imageView.getTag())) imageView.setImageBitmap(loaded);
                else if (!loaded.isRecycled()) loaded.recycle();
            });
        });
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
        startActivity(Intent.createChooser(intent, text("اشتراک‌گذاری تجربه")));
    }

    private void loadCommentCount(DocumentReference experience, TextView target) {
        experience.collection("comments").get().addOnSuccessListener(q ->
                target.setText(text("💬 کامنت (") + formatCount(q.size()) + ")"));
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
        input.setHint(text("نظر خود را بنویسید..."));
        input.setGravity(Gravity.RIGHT | Gravity.TOP);
        content.addView(input);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(text("کامنت‌ها"))
                .setView(content).setNegativeButton(text("بستن"), null)
                .setPositiveButton(text("ثبت کامنت"), null).create();
        experience.collection("comments").get()
                .addOnSuccessListener(q -> {
                    list.removeAllViews();
                    if (q.isEmpty()) { TextView empty = new TextView(this); empty.setText(text("هنوز کامنتی ثبت نشده است.")); empty.setGravity(Gravity.RIGHT); list.addView(empty); }
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
                            row.setText((name == null || name.trim().isEmpty() ? text("کاربر") : name) + "\n" + (comment == null ? "" : comment));
                        });
                    }
                }).addOnFailureListener(e -> Toast.makeText(this, text("بارگذاری کامنت‌ها ناموفق بود"), Toast.LENGTH_SHORT).show());
        dialog.setOnShowListener(unused -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            FirebaseUser user = auth == null ? null : auth.getCurrentUser();
            String value = input.getText().toString().trim();
            if (user == null) { Toast.makeText(this, text("برای ثبت کامنت وارد حساب شوید"), Toast.LENGTH_SHORT).show(); return; }
            if (value.isEmpty()) { input.setError(text("متن کامنت را بنویسید")); return; }
            java.util.HashMap<String,Object> data = new java.util.HashMap<>();
            data.put("userId", user.getUid()); data.put("text", value); data.put("timestamp", FieldValue.serverTimestamp());
            experience.collection("comments").add(data).addOnSuccessListener(ref -> {
                loadCommentCount(experience, countView); Toast.makeText(this, text("کامنت ثبت شد"), Toast.LENGTH_SHORT).show(); dialog.dismiss();
            }).addOnFailureListener(e -> Toast.makeText(this, text("ثبت کامنت ناموفق بود"), Toast.LENGTH_SHORT).show());
        }));
        dialog.show();
    }

    private void showReactors(DocumentReference experience, String initialTab) {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(12), dp(8), dp(12), dp(8));
        LinearLayout tabs = new LinearLayout(this); tabs.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout people = new LinearLayout(this); people.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll = new ScrollView(this); scroll.addView(people);
        root.addView(tabs); root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(320)));
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(text("واکنش‌دهندگان")).setView(root).setPositiveButton(text("بستن"), null).create();
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
                    TextView name = new TextView(this); name.setText(text("کاربر")); name.setTextSize(15); name.setPadding(dp(10),0,dp(10),0); name.setGravity(Gravity.CENTER_VERTICAL|Gravity.RIGHT);
                    row.addView(name, new LinearLayout.LayoutParams(0, dp(46), 1)); people.addView(row);
                    db.collection("users").document(userId).get().addOnSuccessListener(u -> {
                        String n = u.getString("name"); if (n == null || n.trim().isEmpty()) n = u.getString("username"); if (n == null || n.trim().isEmpty()) n = u.getString("displayName");
                        name.setText(("likes".equals(col) ? "👍 " : "❤️ ") + (n == null || n.trim().isEmpty() ? text("کاربر") : n));
                        String photo = u.getString("photoUrl"); if (photo == null) photo = u.getString("profilePhotoUrl"); if (photo == null) photo = u.getString("profilePhoto"); if (photo == null) photo = u.getString("photo");
                        if (photo != null && !photo.trim().isEmpty()) loadProfileImage(photo, avatar);
                    });
                }
            }).addOnFailureListener(e -> Toast.makeText(this, text("بارگذاری واکنش‌ها ناموفق بود"), Toast.LENGTH_SHORT).show());
        };
        String[] labels = {text("همه"), text("👍 لایک"), text("❤️ قلب")}; String[] keys = {"all", "likes", "hearts"};
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
                                text("بارگذاری تعداد قلب‌ها ناموفق بود"),
                                Toast.LENGTH_SHORT).show());
    }

    private void toggleHeart(DocumentSnapshot experience,
                             String currentUserId,
                             TextView heartButton,
                             TextView heartCount) {

        if (currentUserId == null || currentUserId.trim().isEmpty()) return;

        DocumentReference experienceRef = experience.getReference();
        DocumentReference heartRef = experienceRef.collection("hearts").document(currentUserId);
        DocumentReference likeRef = experienceRef.collection("likes").document(currentUserId);

        db.runTransaction(transaction -> {
            // تمام خواندن‌ها باید پیش از هر نوشتن انجام شوند.
            DocumentSnapshot heartSnap = transaction.get(heartRef);
            DocumentSnapshot likeSnap = transaction.get(likeRef);

            if (heartSnap.exists()) {
                transaction.delete(heartRef);
                return "removed";
            }
            if (likeSnap.exists()) {
                return "has_like";
            }

            java.util.HashMap<String, Object> data = new java.util.HashMap<>();
            data.put("userId", currentUserId);
            data.put("timestamp", FieldValue.serverTimestamp());
            transaction.set(heartRef, data);
            return "added";
        }).addOnSuccessListener(result -> {
            if ("has_like".equals(result)) {
                Toast.makeText(this, text("شما قبلاً این تجربه را لایک کرده‌اید؛ هر تجربه فقط یک واکنش دارد."), Toast.LENGTH_SHORT).show();
            } else {
                heartButton.setText("removed".equals(result) ? text("👍 واکنش") : text("❤️ واکنش"));
            }
            loadHeartStatus(experience, heartCount);
        }).addOnFailureListener(e -> {
            Toast.makeText(this, text("خطا در ثبت یا برداشتن قلب"), Toast.LENGTH_SHORT).show();
            loadHeartStatus(experience, heartCount);
        });
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
                                            ? text("👍 لایک")
                                            : text("👍 لایک")
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
        if (currentUserId == null || currentUserId.trim().isEmpty()) return;

        DocumentReference experienceRef = experience.getReference();
        DocumentReference likeRef = experienceRef.collection("likes").document(currentUserId);
        DocumentReference heartRef = experienceRef.collection("hearts").document(currentUserId);

        db.runTransaction(transaction -> {
            // همه اسناد مورد نیاز قبل از شروع نوشتن خوانده می‌شوند.
            DocumentSnapshot experienceSnap = transaction.get(experienceRef);
            DocumentSnapshot likeSnap = transaction.get(likeRef);
            DocumentSnapshot heartSnap = transaction.get(heartRef);

            if (!experienceSnap.exists()) {
                throw new com.google.firebase.firestore.FirebaseFirestoreException(
                        "Experience not found",
                        com.google.firebase.firestore.FirebaseFirestoreException.Code.NOT_FOUND
                );
            }
            if (heartSnap.exists()) return "has_heart";

            Long savedCount = experienceSnap.getLong("likesCount");
            long count = savedCount == null ? 0L : Math.max(0L, savedCount);

            if (likeSnap.exists()) {
                transaction.delete(likeRef);
                transaction.update(experienceRef, "likesCount", Math.max(0L, count - 1L));
                return "removed";
            }

            java.util.HashMap<String, Object> data = new java.util.HashMap<>();
            data.put("userId", currentUserId);
            data.put("timestamp", FieldValue.serverTimestamp());
            transaction.set(likeRef, data);
            transaction.update(experienceRef, "likesCount", count + 1L);
            return "added";
        }).addOnSuccessListener(result -> {
            if ("has_heart".equals(result)) {
                Toast.makeText(this, text("شما قبلاً برای این تجربه قلب گذاشته‌اید؛ هر تجربه فقط یک واکنش دارد."), Toast.LENGTH_SHORT).show();
            } else {
                likeButton.setText(text("👍 لایک"));
            }
            loadLikeCountFromSubcollection(experienceRef, likeCount);
        }).addOnFailureListener(e -> {
            Toast.makeText(this, text("خطا در ثبت یا برداشتن لایک"), Toast.LENGTH_SHORT).show();
            loadLikeCountFromSubcollection(experienceRef, likeCount);
        });
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
                            .replace(text("۰"), "0")
                            .replace(text("۱"), "1")
                            .replace(text("۲"), "2")
                            .replace(text("۳"), "3")
                            .replace(text("۴"), "4")
                            .replace(text("۵"), "5")
                            .replace(text("۶"), "6")
                            .replace(text("۷"), "7")
                            .replace(text("۸"), "8")
                            .replace(text("۹"), "9");

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
                .replace("0", text("۰"))
                .replace("1", text("۱"))
                .replace("2", text("۲"))
                .replace("3", text("۳"))
                .replace("4", text("۴"))
                .replace("5", text("۵"))
                .replace("6", text("۶"))
                .replace("7", text("۷"))
                .replace("8", text("۸"))
                .replace("9", text("۹"));
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
        if (userId == null || userId.trim().isEmpty()) return;

        ProfileViewTarget target = new ProfileViewTarget(userId, authorName, profileImage);
        UserProfileData cached = userProfileCache.get(userId);
        if (cached != null) {
            applyUserProfile(target, cached);
            return;
        }

        List<ProfileViewTarget> waiting = pendingProfileTargets.get(userId);
        if (waiting == null) {
            waiting = new ArrayList<>();
            pendingProfileTargets.put(userId, waiting);
        }
        waiting.add(target);

        // برای هر کاربر فقط یک درخواست هم‌زمان ارسال می‌شود.
        if (!userProfileRequests.add(userId)) return;

        DocumentReference userRef = db.collection("users").document(userId);
        // ابتدا کش محلی Firestore: اگر موجود باشد نام تقریباً فوری نمایش داده می‌شود.
        userRef.get(Source.CACHE).addOnSuccessListener(cachedDocument -> {
            if (cachedDocument.exists()) {
                UserProfileData data = makeUserProfileData(cachedDocument, fallbackName);
                userProfileCache.put(userId, data);
                List<ProfileViewTarget> cachedTargets = pendingProfileTargets.get(userId);
                if (cachedTargets != null) {
                    for (ProfileViewTarget cachedTarget : cachedTargets) {
                        applyUserProfile(cachedTarget, data);
                    }
                }
            }
        }).addOnCompleteListener(cacheTask -> {
            // سپس نسخه سرور برای دریافت نام/عکس تازه؛ نتیجه کش تا رسیدن سرور حفظ می‌شود.
            userRef.get(Source.SERVER).addOnSuccessListener(serverDocument -> {
                UserProfileData data = makeUserProfileData(serverDocument, fallbackName);
                userProfileCache.put(userId, data);
                applyPendingUserProfile(userId, data);
            }).addOnFailureListener(error -> {
                // اگر شبکه قطع بود، اطلاعات کش‌شده یا نام جایگزین باقی می‌ماند.
                if (userProfileCache.get(userId) == null) {
                    UserProfileData fallback = new UserProfileData(fallbackName, "");
                    applyPendingUserProfile(userId, fallback);
                } else {
                    pendingProfileTargets.remove(userId);
                    userProfileRequests.remove(userId);
                }
            });
        });
    }

    private UserProfileData makeUserProfileData(DocumentSnapshot document, String fallbackName) {
        String name = fallbackName;
        String photoUrl = "";
        if (document != null && document.exists()) {
            name = firstNonEmpty(document.getString("name"),
                    document.getString("username"),
                    document.getString("displayName"), fallbackName);
            photoUrl = firstNonEmpty(document.getString("photoUrl"),
                    document.getString("profilePhoto"),
                    document.getString("profilePhotoUrl"),
                    document.getString("photo"), "");
        }
        return new UserProfileData(name, photoUrl);
    }

    private void applyPendingUserProfile(String userId, UserProfileData data) {
        List<ProfileViewTarget> targets = pendingProfileTargets.remove(userId);
        userProfileRequests.remove(userId);
        if (targets == null) return;
        for (ProfileViewTarget target : targets) applyUserProfile(target, data);
    }

    private void applyUserProfile(ProfileViewTarget target, UserProfileData data) {
        if (target.nameView == null || target.imageView == null) return;
        target.nameView.setText(data.name);
        if (data.photoUrl != null && !data.photoUrl.isEmpty()) {
            target.imageView.setTag(data.photoUrl);
            loadProfileImage(data.photoUrl, target.imageView);
        }
        View.OnClickListener openProfile = v ->
                showPublicProfile(target.userId, data.name, data.photoUrl);
        authorRowClickable(target.imageView, target.nameView, openProfile);
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

            addProfileDetail(panel, text("نام کاربری"), firstNonEmpty(d.getString("username"), d.getString("userName")));
            addProfileDetail(panel, text("درباره من"), firstNonEmpty(d.getString("bio"), d.getString("about"), d.getString("description")));
            addProfileDetail(panel, text("شهر"), firstNonEmpty(d.getString("city"), d.getString("location")));
            addProfileDetail(panel, text("تخصص"), firstNonEmpty(d.getString("profession"), d.getString("job"), d.getString("occupation")));

            String myId = auth.getCurrentUser() == null ? "" : auth.getCurrentUser().getUid();
            if (!uid.equals(myId) && !myId.isEmpty()) {
                Button friendButton = new Button(this);
                friendButton.setText(text("🤝 درخواست دوستی"));
                panel.addView(friendButton, new LinearLayout.LayoutParams(-1, -2));
                setFriendButtonState(uid, friendButton);
                friendButton.setOnClickListener(v -> sendFriendRequest(uid, friendButton));
            } else if (uid.equals(myId)) {
                Button incoming = new Button(this);
                incoming.setText(text("📩 درخواست‌های دوستی"));
                panel.addView(incoming, new LinearLayout.LayoutParams(-1, -2));
                incoming.setOnClickListener(v -> showIncomingFriendRequests());
            }

            new AlertDialog.Builder(this)
                    .setTitle(text("پروفایل کاربر"))
                    .setView(scroll)
                    .setPositiveButton(text("بستن"), null)
                    .show();
        }).addOnFailureListener(e -> Toast.makeText(this, text("پروفایل بارگذاری نشد؛ اتصال را بررسی کنید."), Toast.LENGTH_SHORT).show());
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
            Toast.makeText(this, text("این کاربر عکس پروفایل ندارد."), Toast.LENGTH_SHORT).show();
            return;
        }
        ImageView image = new ImageView(this);
        image.setAdjustViewBounds(true);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        loadProfileImage(photo, image);
        new AlertDialog.Builder(this).setTitle(name).setView(image).setPositiveButton(text("بستن"), null).show();
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
                    if (match == null) button.setText(text("🤝 درخواست دوستی"));
                    else {
                        String status = match.getString("status");
                        button.setText("accepted".equals(status) ? text("✅ دوست هستید") : text("⏳ درخواست فرستاده شده"));
                        button.setEnabled(!"accepted".equals(status));
                    }
                }).addOnFailureListener(e -> button.setText(text("🤝 درخواست دوستی")));
    }

    private void sendFriendRequest(String receiverId, Button button) {
        FirebaseUser current = auth.getCurrentUser();
        if (current == null) {
            Toast.makeText(this, text("برای درخواست دوستی اول وارد حساب شوید."), Toast.LENGTH_SHORT).show();
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
                            button.setText("accepted".equals(status) ? text("✅ دوست هستید") : text("⏳ درخواست فرستاده شده"));
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
                                button.setText(text("⏳ درخواست فرستاده شده"));
                                button.setEnabled(false);
                                Toast.makeText(this, text("درخواست دوستی فرستاده شد."), Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                button.setEnabled(true);
                                Toast.makeText(this, text("ارسال درخواست ناموفق بود؛ قوانین فایربیس را بررسی کنید."), Toast.LENGTH_LONG).show();
                            });
                }).addOnFailureListener(e -> {
                    button.setEnabled(true);
                    Toast.makeText(this, text("بررسی درخواست دوستی ناموفق بود."), Toast.LENGTH_SHORT).show();
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
                        sender.setText(text("در حال دریافت نام کاربر…"));
                        row.addView(sender);
                        if (senderId != null) db.collection("users").document(senderId).get().addOnSuccessListener(u ->
                                sender.setText(firstNonEmpty(u.getString("name"), u.getString("username"), text("کاربر"))));
                        LinearLayout actions = new LinearLayout(this);
                        Button accept = new Button(this); accept.setText(text("قبول"));
                        Button reject = new Button(this); reject.setText(text("رد"));
                        actions.addView(accept, new LinearLayout.LayoutParams(0, -2, 1));
                        actions.addView(reject, new LinearLayout.LayoutParams(0, -2, 1));
                        row.addView(actions);
                        list.addView(row);
                        accept.setOnClickListener(v -> respondFriendRequest(request, "accepted", accept, reject));
                        reject.setOnClickListener(v -> respondFriendRequest(request, "rejected", accept, reject));
                    }
                    if (!hasPending) {
                        TextView empty = new TextView(this);
                        empty.setText(text("درخواست دوستی جدیدی نداری."));
                        empty.setPadding(dp(18), dp(18), dp(18), dp(18));
                        list.addView(empty);
                    }
                    new AlertDialog.Builder(this).setTitle(text("درخواست‌های دوستی")).setView(scroller).setPositiveButton(text("بستن"), null).show();
                }).addOnFailureListener(e -> Toast.makeText(this, text("دریافت درخواست‌ها ناموفق بود؛ قوانین فایربیس را بررسی کنید."), Toast.LENGTH_LONG).show());
    }

    private void respondFriendRequest(DocumentSnapshot request, String status, Button accept, Button reject) {
        request.getReference().update("status", status)
                .addOnSuccessListener(unused -> {
                    accept.setEnabled(false); reject.setEnabled(false);
                    accept.setText("accepted".equals(status) ? text("✅ قبول شد") : text("قبول"));
                    reject.setText("rejected".equals(status) ? text("رد شد") : text("رد"));
                    Toast.makeText(this, text("پاسخ ثبت شد."), Toast.LENGTH_SHORT).show();
                    if ("accepted".equals(status)) loadFriends();
                }).addOnFailureListener(e -> Toast.makeText(this, text("ثبت پاسخ ناموفق بود؛ قوانین فایربیس را بررسی کنید."), Toast.LENGTH_LONG).show());
    }

    // =========================================================
    // بارگذاری عکس پروفایل
    // =========================================================

    private void loadProfileImage(String photoUrl, ImageView imageView) {
        if (photoUrl == null || photoUrl.trim().isEmpty() || imageView == null) return;
        final String storedUrl = photoUrl.trim();
        imageView.setTag(storedUrl);
        File cachedProfile = new File(experienceCacheDir("profiles"), experienceCacheKey(storedUrl));
        if (cachedProfile.isFile() && cachedProfile.length() > 0) {
            cachedProfile.setLastModified(System.currentTimeMillis());
            showCachedProfileImage(cachedProfile, imageView, storedUrl);
        }
        if (storedUrl.contains("/storage/v1/object/public/" + PROFILE_BUCKET + "/") ||
                storedUrl.contains("/storage/v1/object/" + PROFILE_BUCKET + "/")) {
            loadPrivateProfileImage(storedUrl, imageView);
            return;
        }
        IMAGE_EXECUTOR.execute(() -> downloadProfileImage(storedUrl, imageView, storedUrl));
    }

    private void loadPrivateProfileImage(String storedUrl, ImageView imageView) {
        IMAGE_EXECUTOR.execute(() -> {
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
                String idToken = Tasks.await(currentUser.getIdToken(false)).getToken();
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
                if (!signedUrl.isEmpty()) downloadProfileImage(signedUrl, imageView, storedUrl);
            } catch (Exception ignored) {
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    private void showCachedProfileImage(File cached, ImageView imageView, String expectedTag) {
        IMAGE_EXECUTOR.execute(() -> {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(cached.getAbsolutePath(), bounds);
            int sample = 1;
            while (bounds.outWidth / sample > 600 || bounds.outHeight / sample > 600) sample *= 2;
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sample;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap bitmap = BitmapFactory.decodeFile(cached.getAbsolutePath(), options);
            if (bitmap != null) runOnUiThread(() -> {
                if (!isFinishing() && expectedTag.equals(imageView.getTag())) imageView.setImageBitmap(bitmap);
                else if (!bitmap.isRecycled()) bitmap.recycle();
            });
        });
    }

    private void downloadProfileImage(String url, ImageView imageView, String stableCacheKey) {
        HttpURLConnection connection = null;
        File temp = null;
        try {
            File dir = experienceCacheDir("profiles");
            String key = experienceCacheKey(stableCacheKey);
            File cached = new File(dir, key);
            if (!cached.isFile() || cached.length() == 0) {
                connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(20000);
                int code = connection.getResponseCode();
                if (code < 200 || code >= 300) return;
                long size = connection.getContentLengthLong();
                if (size > 8L * 1024L * 1024L) return;
                temp = new File(dir, key + ".tmp");
                long total = 0;
                try (InputStream input = connection.getInputStream(); FileOutputStream output = new FileOutputStream(temp)) {
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        total += count;
                        if (total > 8L * 1024L * 1024L) throw new IOException("Profile too large");
                        output.write(buffer, 0, count);
                    }
                }
                if (total == 0 || !temp.renameTo(cached)) {
                    temp.delete();
                    return;
                }
            }
            showCachedProfileImage(cached, imageView, stableCacheKey);
        } catch (Exception ignored) {
            if (temp != null) temp.delete();
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

                }
