package com.tajro.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChatActivity extends Activity {

    private static final int REQUEST_MIC = 1001;
    private static final int PICK_MEDIA = 1002;
    private static final int PICK_PROFILE = 1003;
    private static final int REQUEST_SAVE = 1004;

    private static final String SUPABASE_URL =
            "https://gorbhuqmkjlkrklhasdh.supabase.co";

    private static final String SUPABASE_PUBLISHABLE_KEY =
            "sb_publishable_a02sM3MABB4afGU90ZBdFA_OTYG6gUs";

    private static final String CHAT_BUCKET = "chat_media";
    private static final String VOICE_BUCKET = "voice_messages";

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private LinearLayout root;
    private LinearLayout usersContainer;
    private LinearLayout messagesContainer;
    private ScrollView messagesScroll;

    private EditText messageInput;
    private ImageButton sendButton;
    private ImageButton mediaButton;
    private ImageButton voiceButton;

    private TextView titleText;
    private TextView statusText;
    private ImageView headerAvatar;
    private TextView headerOnlineDot;
    private TextView chatMenuButton;

    private String myId;
    private String receiverId;
    private String receiverName;
    private String receiverPhotoUrl;
    private String currentChatId;

    private boolean insideChat = false;
    private boolean recording = false;
    private boolean typing = false;
    private boolean blocked = false;

    private ListenerRegistration sentMessageListener;
    private ListenerRegistration receivedMessageListener;
    private ListenerRegistration receiverListener;
    private ListenerRegistration typingListener;
    private ListenerRegistration blockListener;
    private ListenerRegistration reverseBlockListener;

    private boolean blockedByMe = false;
    private boolean blockedByReceiver = false;
    private int blockChecksReady = 0;

    private MediaRecorder recorder;
    private MediaPlayer player;
    private String audioPath;

    private final android.os.Handler typingHandler =
            new android.os.Handler();

    private final Map<String, DocumentSnapshot> messageCache =
            new HashMap<>();

    private final Set<String> hiddenUserIds =
            new HashSet<>();

    // کاربران مسدودشده؛ فقط برای جلوگیری از نمایش اشتباه وضعیت آنلاین
    private final Set<String> blockedUserIds =
            new HashSet<>();

    private int themeColor = Color.rgb(12, 91, 120);

    private String pendingSaveUrl;
    private String pendingSaveName = "tajrobehha.jpg";

    private String appliedLanguage;

    private interface SupabaseUploadCallback {
        void onSuccess(String publicUrl);
        void onError(String error);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(
                LanguageManager.applyLanguage(newBase)
        );
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        appliedLanguage =
                LanguageManager.getLanguage(this);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {

            Toast.makeText(
                    this,
                    tr("لطفاً اول وارد حساب شوید"),
                    Toast.LENGTH_LONG
            ).show();

            try {
                startActivity(
                        new Intent(this, AccountActivity.class)
                );
            } catch (Exception ignored) {
            }

            finish();
            return;
        }

        myId = user.getUid();

        ensureUserProfile();
        createUsersScreen();
        loadUsers();
    }

    @Override
    protected void onResume() {
        super.onResume();

        String currentLanguage =
                LanguageManager.getLanguage(this);

        if (appliedLanguage != null
                && !currentLanguage.equals(appliedLanguage)) {

            recreate();
        }
    }

    private String tr(String fa) {
        String lang = LanguageManager.getLanguage(this);

        if ("en".equals(lang)) {
            switch (fa) {
                case "لطفاً اول وارد حساب شوید": return "Please log in first";
                case "💬 کاربران": return "💬 Users";
                case "یک کاربر را انتخاب کنید تا چت خصوصی باز شود.": return "Select a user to open a private chat.";
                case "هنوز کاربر دیگری پیدا نشد.": return "No other users found yet.";
                case "خطا در دریافت کاربران": return "Error loading users";
                case "کاربر": return "User";
                case " 🚫 مسدود": return " 🚫 Blocked";
                case " آنلاین": return " Online";
                case " آفلاین": return " Offline";
                case "👁 مخفی کردن / رفع مخفی": return "👁 Hide / Unhide";
                case "🚫 بلاک / رفع مسدودیت": return "🚫 Block / Unblock";
                case "مخفی بودن کاربر رفع شد": return "User is no longer hidden";
                case "کاربر مخفی شد": return "User hidden";
                case "مسدودیت کاربر رفع شد": return "User unblocked";
                case "کاربر بلاک شد؛ ارسال پیام و صدا متوقف شد": return "User blocked; messages and voice are stopped";
                case "📷 تغییر عکس پروفایل": return "📷 Change profile photo";
                case "🔒 تنظیمات حریم خصوصی": return "🔒 Privacy settings";
                case "🚫 فهرست مسدودشده‌ها": return "🚫 Blocked users";
                case "پروفایل": return "Profile";
                case "بستن": return "Close";
                case "هیچ کاربری مسدود نشده است.": return "No users are blocked.";
                case "   •   رفع مسدودی": return "   •   Unblock";
                case "رفع مسدودی": return "Unblock";
                case "آیا می‌خواهید «": return "Do you want to unblock «";
                case "» را از مسدودی خارج کنید؟": return "»?";
                case "لغو": return "Cancel";
                case "مسدودی برداشته شد": return "Unblocked";
                case "دریافت فهرست مسدودشده‌ها انجام نشد": return "Failed to load blocked users";
                case "در حال بررسی...": return "Checking...";
                case "پیام خود را بنویسید...": return "Write your message...";
                case "پیام حذف شد": return "Message deleted";
                case "▶️ پخش پیام صوتی": return "▶️ Play voice message";
                case "حذف برای من": return "Delete for me";
                case "حذف برای همه": return "Delete for everyone";
                case "حذف پیام ناموفق بود": return "Failed to delete message";
                case "این کاربر بلاک شده است": return "This user is blocked";
                case "خطا در ارسال پیام": return "Error sending message";
                case "آنلاین": return "Online";
                case "آفلاین": return "Offline";
                case "در حال نوشتن...": return "Typing...";
                case "🚫 این کاربر مسدود است": return "🚫 This user is blocked";
                case "🚫 مسدود شده": return "🚫 Blocked";
                case "این کاربر مسدود شده است": return "This user is blocked";
                case "این کاربر شما را مسدود کرده است": return "This user has blocked you";
                case "این نوع فایل پشتیبانی نمی‌شود": return "This file type is not supported";
                case "در حال ارسال فایل...": return "Sending file...";
                case "خطای ارسال:\n": return "Send error:\n";
                case "این کاربر بلاک شده است؛ فایل ارسال نشد": return "This user is blocked; file was not sent";
                case "خطا در ذخیره پیام فایل": return "Error saving file message";
                case "🎤 در حال ضبط... دوباره بزنید تا ارسال شود": return "🎤 Recording... tap again to send";
                case "خطا در شروع ضبط: ": return "Error starting recording: ";
                case "ضبط صدا ناموفق بود": return "Voice recording failed";
                case "این کاربر بلاک شده است؛ پیام صوتی ارسال نشد": return "This user is blocked; voice message was not sent";
                case "فایل صوتی پیدا نشد": return "Audio file not found";
                case "فایل صوتی خالی است": return "Audio file is empty";
                case "در حال ارسال پیام صوتی...": return "Sending voice message...";
                case "خطای ارسال پیام صوتی:\n": return "Error sending voice message:\n";
                case "خطا در ذخیره پیام صوتی": return "Error saving voice message";
                case "پخش صدا ناموفق بود": return "Failed to play audio";
                case "خطا در پخش صدا": return "Error playing audio";
                case "در حال ارسال عکس پروفایل...": return "Uploading profile photo...";
                case "عکس پروفایل ذخیره شد": return "Profile photo saved";
                case "خطای عکس پروفایل:\n": return "Profile photo error:\n";
                case "فایل وجود ندارد": return "File does not exist";
                case "فایل خالی است": return "File is empty";
                case "فایل قابل خواندن نیست": return "File is not readable";
                case "💾 ذخیره عکس در گالری": return "💾 Save image to gallery";
                case "تصویر": return "Image";
                case "گالری قابل دسترسی نیست": return "Gallery is not accessible";
                case "فضای ذخیره‌سازی باز نشد": return "Storage could not be opened";
                case "✅ عکس در گالری ذخیره شد": return "✅ Image saved to gallery";
                case "ذخیره مستقیم در گالری در این نسخه اندروید پشتیبانی نمی‌شود": return "Direct gallery saving is not supported on this Android version";
                case "خطا در ذخیره عکس": return "Error saving image";
                case "خطای ذخیره عکس:\n": return "Image save error:\n";
                case "اجازه میکروفون داده نشد": return "Microphone permission was denied";
                case "🔕 بی‌صدا کردن اعلان‌های این چت": return "🔕 Mute notifications for this chat";
                case "👤 مشاهده پروفایل": return "👤 View profile";
                case "🗑️ حذف کامل چت": return "🗑️ Delete entire chat";
                case "🚫 مسدود کردن": return "🚫 Block";
                case "تنظیمات چت": return "Chat settings";
                case "اعلان‌های این چت بی‌صدا شد": return "Chat notifications muted";
                case "⚠️ حذف کامل چت": return "⚠️ Delete entire chat";
                case "آیا مطمئن هستید که می‌خواهید تمام این گفتگو را حذف کنید؟": return "Are you sure you want to delete this entire conversation?";
                case "مرحله اول": return "Step 1";
                case "تأیید نهایی حذف": return "Final deletion confirmation";
                case "این کار تمام پیام‌های این گفتگو را حذف می‌کند و قابل برگشت نیست. ادامه می‌دهید؟": return "This will delete all messages in this conversation and cannot be undone. Continue?";
                case "حذف کامل": return "Delete all";
                case "مسدود کردن کاربر": return "Block user";
                case "آیا می‌خواهید این کاربر را مسدود کنید؟": return "Do you want to block this user?";
                case "مسدود کردن": return "Block";
                case "نمایش آنلاین بودن": return "Show online status";
                case "نمایش آخرین بازدید": return "Show last seen";
                case "نمایش «در حال نوشتن…»": return "Show “typing…”";
                case "نمایش رسید خوانده شدن ✓✓": return "Show read receipts ✓✓";
                case "ذخیره": return "Save";
                case "تنظیمات ذخیره شد": return "Settings saved";
                case "ذخیره تنظیمات ناموفق بود": return "Failed to save settings";
                case "گزارش شما ثبت شد": return "Your report was submitted";
                case "ثبت گزارش ناموفق بود": return "Failed to submit report";
                case "چت از حساب شما پاک شد": return "Chat deleted from your account";
                case "پاک کردن چت ناموفق بود": return "Failed to delete chat";
                case "دسترسی به پیام‌های چت ناموفق بود": return "Failed to access chat messages";
                case "کاربر انتخاب نشده است": return "No user selected";
                case "کاربر مسدود شد؛ پیام و صدا متوقف شد": return "User blocked; messages and voice stopped";
                case "خطای نامشخص": return "Unknown error";
                case "مسدود کردن ناموفق بود:\n": return "Failed to block:\n";
                case "🚫 فهرست مسدودشدهها": return "🚫 Blocked users";
                case "آیا میخواهید «": return "Do you want to unblock «";
                case "دریافت فهرست مسدودشدهها انجام نشد": return "Failed to load blocked users";
                case "این نوع فایل پشتیبانی نمیشود": return "This file type is not supported";
                case "فضای ذخیرهسازی باز نشد": return "Storage could not be opened";
                case "ذخیره مستقیم در گالری در این نسخه اندروید پشتیبانی نمیشود": return "Direct gallery saving is not supported on this Android version";
                case "🔕 بیصدا کردن اعلانهای این چت": return "🔕 Mute notifications for this chat";
                case "اعلانهای این چت بیصدا شد": return "Chat notifications muted";
                case "آیا مطمئن هستید که میخواهید تمام این گفتگو را حذف کنید؟": return "Are you sure you want to delete this entire conversation?";
                case "این کار تمام پیامهای این گفتگو را حذف میکند و قابل برگشت نیست. ادامه میدهید؟": return "This will delete all messages in this conversation and cannot be undone. Continue?";
                case "آیا میخواهید این کاربر را مسدود کنید؟": return "Do you want to block this user?";
                case "دسترسی به پیامهای چت ناموفق بود": return "Failed to access chat messages";
            }
        }

        if ("ps".equals(lang)) {
            switch (fa) {
                case "لطفاً اول وارد حساب شوید": return "مهرباني وکړئ لومړی حساب ته ننوځئ";
                case "💬 کاربران": return "💬 کاروونکي";
                case "یک کاربر را انتخاب کنید تا چت خصوصی باز شود.": return "د شخصي چټ د خلاصولو لپاره یو کاروونکی وټاکئ.";
                case "هنوز کاربر دیگری پیدا نشد.": return "تر اوسه بل کاروونکی ونه موندل شو.";
                case "خطا در دریافت کاربران": return "د کاروونکو په ترلاسه کولو کې تېروتنه";
                case "کاربر": return "کاروونکی";
                case " 🚫 مسدود": return " 🚫 بند شوی";
                case " آنلاین": return " آنلاین";
                case " آفلاین": return " آفلاین";
                case "👁 مخفی کردن / رفع مخفی": return "👁 پټول / له پټه ایستل";
                case "🚫 بلاک / رفع مسدودیت": return "🚫 بندول / بندیز لرې کول";
                case "مخفی بودن کاربر رفع شد": return "د کاروونکي پټوالی لرې شو";
                case "کاربر مخفی شد": return "کاروونکی پټ شو";
                case "مسدودیت کاربر رفع شد": return "د کاروونکي بندیز لرې شو";
                case "کاربر بلاک شد؛ ارسال پیام و صدا متوقف شد": return "کاروونکی بند شو؛ پیغامونه او غږ بند شول";
                case "📷 تغییر عکس پروفایل": return "📷 د پروفایل انځور بدلول";
                case "🔒 تنظیمات حریم خصوصی": return "🔒 د محرمیت تنظیمات";
                case "🚫 فهرست مسدودشده‌ها": return "🚫 بند شوي کاروونکي";
                case "پروفایل": return "پروفایل";
                case "بستن": return "بندول";
                case "هیچ کاربری مسدود نشده است.": return "هیڅ کاروونکی بند شوی نه دی.";
                case "   •   رفع مسدودی": return "   •   بندیز لرې کول";
                case "رفع مسدودی": return "بندیز لرې کول";
                case "آیا می‌خواهید «": return "ایا غواړئ «";
                case "» را از مسدودی خارج کنید؟": return "» له بندیز څخه لرې کړئ؟";
                case "لغو": return "لغوه";
                case "مسدودی برداشته شد": return "بندیز لرې شو";
                case "دریافت فهرست مسدودشده‌ها انجام نشد": return "د بند شوو کاروونکو لېست ترلاسه نه شو";
                case "در حال بررسی...": return "کتنه روانه ده...";
                case "پیام خود را بنویسید...": return "خپل پیغام ولیکئ...";
                case "پیام حذف شد": return "پیغام ړنګ شو";
                case "▶️ پخش پیام صوتی": return "▶️ غږیز پیغام غږول";
                case "حذف برای من": return "یوازې زما لپاره ړنګول";
                case "حذف برای همه": return "د ټولو لپاره ړنګول";
                case "حذف پیام ناموفق بود": return "د پیغام ړنګول ناکام شول";
                case "این کاربر بلاک شده است": return "دا کاروونکی بند شوی دی";
                case "خطا در ارسال پیام": return "د پیغام په لېږلو کې تېروتنه";
                case "آنلاین": return "آنلاین";
                case "آفلاین": return "آفلاین";
                case "در حال نوشتن...": return "د لیکلو په حال کې...";
                case "🚫 این کاربر مسدود است": return "🚫 دا کاروونکی بند شوی دی";
                case "🚫 مسدود شده": return "🚫 بند شوی";
                case "این کاربر مسدود شده است": return "دا کاروونکی بند شوی دی";
                case "این کاربر شما را مسدود کرده است": return "دې کاروونکي تاسو بند کړي یاست";
                case "این نوع فایل پشتیبانی نمی‌شود": return "د دې فایل ډول ملاتړ نه کېږي";
                case "در حال ارسال فایل...": return "فایل لېږل کېږي...";
                case "خطای ارسال:\n": return "د لېږلو تېروتنه:\n";
                case "این کاربر بلاک شده است؛ فایل ارسال نشد": return "دا کاروونکی بند شوی؛ فایل ونه لېږل شو";
                case "خطا در ذخیره پیام فایل": return "د فایل پیغام په خوندي کولو کې تېروتنه";
                case "🎤 در حال ضبط... دوباره بزنید تا ارسال شود": return "🎤 ثبت روان دی... د لېږلو لپاره بیا کېکاږئ";
                case "خطا در شروع ضبط: ": return "د ثبتولو په پیل کې تېروتنه: ";
                case "ضبط صدا ناموفق بود": return "د غږ ثبتول ناکام شول";
                case "این کاربر بلاک شده است؛ پیام صوتی ارسال نشد": return "دا کاروونکی بند شوی؛ غږیز پیغام ونه لېږل شو";
                case "فایل صوتی پیدا نشد": return "غږیز فایل ونه موندل شو";
                case "فایل صوتی خالی است": return "غږیز فایل تش دی";
                case "در حال ارسال پیام صوتی...": return "غږیز پیغام لېږل کېږي...";
                case "خطای ارسال پیام صوتی:\n": return "د غږیز پیغام په لېږلو کې تېروتنه:\n";
                case "خطا در ذخیره پیام صوتی": return "د غږیز پیغام په خوندي کولو کې تېروتنه";
                case "پخش صدا ناموفق بود": return "غږ غږول ناکام شول";
                case "خطا در پخش صدا": return "د غږ په غږولو کې تېروتنه";
                case "در حال ارسال عکس پروفایل...": return "د پروفایل انځور اپلوډ کېږي...";
                case "عکس پروفایل ذخیره شد": return "د پروفایل انځور خوندي شو";
                case "خطای عکس پروفایل:\n": return "د پروفایل انځور تېروتنه:\n";
                case "فایل وجود ندارد": return "فایل شتون نه لري";
                case "فایل خالی است": return "فایل تش دی";
                case "فایل قابل خواندن نیست": return "فایل د لوستلو وړ نه دی";
                case "💾 ذخیره عکس در گالری": return "💾 انځور ګالري ته خوندي کړئ";
                case "تصویر": return "انځور";
                case "گالری قابل دسترسی نیست": return "ګالري ته لاسرسی نشته";
                case "فضای ذخیره‌سازی باز نشد": return "ذخیره ځای پرانیستل نه شول";
                case "✅ عکس در گالری ذخیره شد": return "✅ انځور ګالري ته خوندي شو";
                case "ذخیره مستقیم در گالری در این نسخه اندروید پشتیبانی نمی‌شود": return "په دې Android نسخه کې ګالري ته مستقیم خوندي کول نه ملاتړ کېږي";
                case "خطا در ذخیره عکس": return "د انځور په خوندي کولو کې تېروتنه";
                case "خطای ذخیره عکس:\n": return "د انځور خوندي کولو تېروتنه:\n";
                case "اجازه میکروفون داده نشد": return "د مایکروفون اجازه ورنه کړل شوه";
                case "🔕 بی‌صدا کردن اعلان‌های این چت": return "🔕 د دې چټ خبرتیاوې غلې کړئ";
                case "👤 مشاهده پروفایل": return "👤 پروفایل وګورئ";
                case "🗑️ حذف کامل چت": return "🗑️ ټول چټ ړنګ کړئ";
                case "🚫 مسدود کردن": return "🚫 بندول";
                case "تنظیمات چت": return "د چټ تنظیمات";
                case "اعلان‌های این چت بی‌صدا شد": return "د چټ خبرتیاوې غلې شوې";
                case "⚠️ حذف کامل چت": return "⚠️ ټول چټ ړنګ کړئ";
                case "آیا مطمئن هستید که می‌خواهید تمام این گفتگو را حذف کنید؟": return "ایا ډاډه یاست چې دا ټوله خبرې اترې ړنګول غواړئ؟";
                case "مرحله اول": return "لومړی پړاو";
                case "تأیید نهایی حذف": return "د ړنګولو وروستۍ تایید";
                case "این کار تمام پیام‌های این گفتگو را حذف می‌کند و قابل برگشت نیست. ادامه می‌دهید؟": return "دا به د دې خبرو ټول پیغامونه ړنګ کړي او بېرته نه راګرځي. دوام ورکړئ؟";
                case "حذف کامل": return "ټول ړنګول";
                case "مسدود کردن کاربر": return "کاروونکی بندول";
                case "آیا می‌خواهید این کاربر را مسدود کنید؟": return "ایا غواړئ دا کاروونکی بند کړئ؟";
                case "مسدود کردن": return "بندول";
                case "نمایش آنلاین بودن": return "آنلاین حالت ښکاره کول";
                case "نمایش آخرین بازدید": return "وروستی لیدل شوی وخت ښکاره کول";
                case "نمایش «در حال نوشتن…»": return "«د لیکلو په حال کې…» ښکاره کول";
                case "نمایش رسید خوانده شدن ✓✓": return "د لوستل کېدو رسید ✓✓ ښکاره کول";
                case "ذخیره": return "خوندي کول";
                case "تنظیمات ذخیره شد": return "تنظیمات خوندي شول";
                case "ذخیره تنظیمات ناموفق بود": return "د تنظیماتو خوندي کول ناکام شول";
                case "گزارش شما ثبت شد": return "ستاسو راپور ثبت شو";
                case "ثبت گزارش ناموفق بود": return "د راپور ثبتول ناکام شول";
                case "چت از حساب شما پاک شد": return "چټ ستاسو له حساب څخه ړنګ شو";
                case "پاک کردن چت ناموفق بود": return "د چټ ړنګول ناکام شول";
                case "دسترسی به پیام‌های چت ناموفق بود": return "د چټ پیغامونو ته لاسرسی ناکام شو";
                case "کاربر انتخاب نشده است": return "هیڅ کاروونکی نه دی ټاکل شوی";
                case "کاربر مسدود شد؛ پیام و صدا متوقف شد": return "کاروونکی بند شو؛ پیغامونه او غږ ودرول شول";
                case "خطای نامشخص": return "ناڅرګنده تېروتنه";
                case "مسدود کردن ناموفق بود:\n": return "د بندولو ناکامي:\n";
                case "🚫 فهرست مسدودشدهها": return "🚫 بند شوي کاروونکي";
                case "آیا میخواهید «": return "ایا غواړئ «";
                case "دریافت فهرست مسدودشدهها انجام نشد": return "د بند شوو کاروونکو لېست ترلاسه نه شو";
                case "این نوع فایل پشتیبانی نمیشود": return "د دې فایل ډول ملاتړ نه کېږي";
                case "فضای ذخیرهسازی باز نشد": return "ذخیره ځای پرانیستل نه شول";
                case "ذخیره مستقیم در گالری در این نسخه اندروید پشتیبانی نمیشود": return "په دې Android نسخه کې ګالري ته مستقیم خوندي کول نه ملاتړ کېږي";
                case "🔕 بیصدا کردن اعلانهای این چت": return "🔕 د دې چټ خبرتیاوې غلې کړئ";
                case "اعلانهای این چت بیصدا شد": return "د چټ خبرتیاوې غلې شوې";
                case "آیا مطمئن هستید که میخواهید تمام این گفتگو را حذف کنید؟": return "ایا ډاډه یاست چې دا ټوله خبرې اترې ړنګول غواړئ؟";
                case "این کار تمام پیامهای این گفتگو را حذف میکند و قابل برگشت نیست. ادامه میدهید؟": return "دا به د دې خبرو ټول پیغامونه ړنګ کړي او بېرته نه راګرځي. دوام ورکړئ؟";
                case "آیا میخواهید این کاربر را مسدود کنید؟": return "ایا غواړئ دا کاروونکی بند کړئ؟";
                case "دسترسی به پیامهای چت ناموفق بود": return "د چټ پیغامونو ته لاسرسی ناکام شو";
            }
        }

        if ("ur".equals(lang)) {
            switch (fa) {
                case "لطفاً اول وارد حساب شوید": return "براہِ کرم پہلے اکاؤنٹ میں لاگ اِن کریں";
                case "💬 کاربران": return "💬 صارفین";
                case "یک کاربر را انتخاب کنید تا چت خصوصی باز شود.": return "نجی چیٹ کھولنے کے لیے صارف منتخب کریں۔";
                case "هنوز کاربر دیگری پیدا نشد.": return "ابھی کوئی دوسرا صارف نہیں ملا۔";
                case "خطا در دریافت کاربران": return "صارفین حاصل کرنے میں خرابی";
                case "کاربر": return "صارف";
                case " 🚫 مسدود": return " 🚫 مسدود";
                case " آنلاین": return " آن لائن";
                case " آفلاین": return " آف لائن";
                case "👁 مخفی کردن / رفع مخفی": return "👁 چھپائیں / ظاہر کریں";
                case "🚫 بلاک / رفع مسدودیت": return "🚫 بلاک / ان بلاک";
                case "مخفی بودن کاربر رفع شد": return "صارف کو ظاہر کر دیا گیا";
                case "کاربر مخفی شد": return "صارف چھپا دیا گیا";
                case "مسدودیت کاربر رفع شد": return "صارف کو ان بلاک کر دیا گیا";
                case "کاربر بلاک شد؛ ارسال پیام و صدا متوقف شد": return "صارف بلاک ہے؛ پیغامات اور آواز بند کر دیے گئے";
                case "📷 تغییر عکس پروفایل": return "📷 پروفائل تصویر تبدیل کریں";
                case "🔒 تنظیمات حریم خصوصی": return "🔒 رازداری کی ترتیبات";
                case "🚫 فهرست مسدودشده‌ها": return "🚫 مسدود صارفین";
                case "پروفایل": return "پروفائل";
                case "بستن": return "بند کریں";
                case "هیچ کاربری مسدود نشده است.": return "کوئی صارف مسدود نہیں ہے۔";
                case "   •   رفع مسدودی": return "   •   ان بلاک";
                case "رفع مسدودی": return "ان بلاک";
                case "آیا می‌خواهید «": return "کیا آپ «";
                case "» را از مسدودی خارج کنید؟": return "» کو ان بلاک کرنا چاہتے ہیں؟";
                case "لغو": return "منسوخ کریں";
                case "مسدودی برداشته شد": return "مسدودی ختم کر دی گئی";
                case "دریافت فهرست مسدودشده‌ها انجام نشد": return "مسدود صارفین کی فہرست حاصل نہیں ہو سکی";
                case "در حال بررسی...": return "جانچ جاری ہے...";
                case "پیام خود را بنویسید...": return "اپنا پیغام لکھیں...";
                case "پیام حذف شد": return "پیغام حذف کر دیا گیا";
                case "▶️ پخش پیام صوتی": return "▶️ صوتی پیغام چلائیں";
                case "حذف برای من": return "میرے لیے حذف کریں";
                case "حذف برای همه": return "سب کے لیے حذف کریں";
                case "حذف پیام ناموفق بود": return "پیغام حذف نہیں ہو سکا";
                case "این کاربر بلاک شده است": return "یہ صارف بلاک ہے";
                case "خطا در ارسال پیام": return "پیغام بھیجنے میں خرابی";
                case "آنلاین": return "آن لائن";
                case "آفلاین": return "آف لائن";
                case "در حال نوشتن...": return "لکھ رہا ہے...";
                case "🚫 این کاربر مسدود است": return "🚫 یہ صارف بلاک ہے";
                case "🚫 مسدود شده": return "🚫 مسدود";
                case "این کاربر مسدود شده است": return "یہ صارف بلاک ہے";
                case "این کاربر شما را مسدود کرده است": return "اس صارف نے آپ کو بلاک کر دیا ہے";
                case "این نوع فایل پشتیبانی نمی‌شود": return "اس فائل کی قسم سپورٹ نہیں ہے";
                case "در حال ارسال فایل...": return "فائل بھیجی جا رہی ہے...";
                case "خطای ارسال:\n": return "بھیجنے میں خرابی:\n";
                case "این کاربر بلاک شده است؛ فایل ارسال نشد": return "یہ صارف بلاک ہے؛ فائل نہیں بھیجی گئی";
                case "خطا در ذخیره پیام فایل": return "فائل پیغام محفوظ کرنے میں خرابی";
                case "🎤 در حال ضبط... دوباره بزنید تا ارسال شود": return "🎤 ریکارڈنگ جاری ہے... بھیجنے کے لیے دوبارہ دبائیں";
                case "خطا در شروع ضبط: ": return "ریکارڈنگ شروع کرنے میں خرابی: ";
                case "ضبط صدا ناموفق بود": return "صوتی ریکارڈنگ ناکام رہی";
                case "این کاربر بلاک شده است؛ پیام صوتی ارسال نشد": return "یہ صارف بلاک ہے؛ صوتی پیغام نہیں بھیجا گیا";
                case "فایل صوتی پیدا نشد": return "آڈیو فائل نہیں ملی";
                case "فایل صوتی خالی است": return "آڈیو فائل خالی ہے";
                case "در حال ارسال پیام صوتی...": return "صوتی پیغام بھیجا جا رہا ہے...";
                case "خطای ارسال پیام صوتی:\n": return "صوتی پیغام بھیجنے میں خرابی:\n";
                case "خطا در ذخیره پیام صوتی": return "صوتی پیغام محفوظ کرنے میں خرابی";
                case "پخش صدا ناموفق بود": return "آواز چلانے میں ناکامی";
                case "خطا در پخش صدا": return "آواز چلانے میں خرابی";
                case "در حال ارسال عکس پروفایل...": return "پروفائل تصویر اپ لوڈ ہو رہی ہے...";
                case "عکس پروفایل ذخیره شد": return "پروفائل تصویر محفوظ ہو گئی";
                case "خطای عکس پروفایل:\n": return "پروفائل تصویر کی خرابی:\n";
                case "فایل وجود ندارد": return "فائل موجود نہیں ہے";
                case "فایل خالی است": return "فائل خالی ہے";
                case "فایل قابل خواندن نیست": return "فائل قابلِ مطالعہ نہیں ہے";
                case "💾 ذخیره عکس در گالری": return "💾 تصویر گیلری میں محفوظ کریں";
                case "تصویر": return "تصویر";
                case "گالری قابل دسترسی نیست": return "گیلری تک رسائی نہیں ہے";
                case "فضای ذخیره‌سازی باز نشد": return "اسٹوریج نہیں کھل سکا";
                case "✅ عکس در گالری ذخیره شد": return "✅ تصویر گیلری میں محفوظ ہو گئی";
                case "ذخیره مستقیم در گالری در این نسخه اندروید پشتیبانی نمی‌شود": return "اس Android ورژن میں گیلری میں براہِ راست محفوظ کرنا سپورٹ نہیں ہے";
                case "خطا در ذخیره عکس": return "تصویر محفوظ کرنے میں خرابی";
                case "خطای ذخیره عکس:\n": return "تصویر محفوظ کرنے کی خرابی:\n";
                case "اجازه میکروفون داده نشد": return "مائیکروفون کی اجازت نہیں دی گئی";
                case "🔕 بی‌صدا کردن اعلان‌های این چت": return "🔕 اس چیٹ کی اطلاعات خاموش کریں";
                case "👤 مشاهده پروفایل": return "👤 پروفائل دیکھیں";
                case "🗑️ حذف کامل چت": return "🗑️ پوری چیٹ حذف کریں";
                case "🚫 مسدود کردن": return "🚫 بلاک کریں";
                case "تنظیمات چت": return "چیٹ کی ترتیبات";
                case "اعلان‌های این چت بی‌صدا شد": return "چیٹ کی اطلاعات خاموش کر دی گئی ہیں";
                case "⚠️ حذف کامل چت": return "⚠️ پوری چیٹ حذف کریں";
                case "آیا مطمئن هستید که می‌خواهید تمام این گفتگو را حذف کنید؟": return "کیا آپ واقعی پوری گفتگو حذف کرنا چاہتے ہیں؟";
                case "مرحله اول": return "پہلا مرحلہ";
                case "تأیید نهایی حذف": return "حذف کی آخری تصدیق";
                case "این کار تمام پیام‌های این گفتگو را حذف می‌کند و قابل برگشت نیست. ادامه می‌دهید؟": return "یہ اس گفتگو کے تمام پیغامات حذف کر دے گا اور واپس نہیں ہو سکتا۔ جاری رکھیں؟";
                case "حذف کامل": return "مکمل حذف";
                case "مسدود کردن کاربر": return "صارف کو بلاک کریں";
                case "آیا می‌خواهید این کاربر را مسدود کنید؟": return "کیا آپ اس صارف کو بلاک کرنا چاہتے ہیں؟";
                case "مسدود کردن": return "بلاک کریں";
                case "نمایش آنلاین بودن": return "آن لائن حالت دکھائیں";
                case "نمایش آخرین بازدید": return "آخری بار دیکھا گیا دکھائیں";
                case "نمایش «در حال نوشتن…»": return "“ٹائپ کر رہا ہے…” دکھائیں";
                case "نمایش رسید خوانده شدن ✓✓": return "پڑھے جانے کی رسید ✓✓ دکھائیں";
                case "ذخیره": return "محفوظ کریں";
                case "تنظیمات ذخیره شد": return "ترتیبات محفوظ ہو گئیں";
                case "ذخیره تنظیمات ناموفق بود": return "ترتیبات محفوظ نہیں ہو سکیں";
                case "گزارش شما ثبت شد": return "آپ کی رپورٹ جمع ہو گئی";
                case "ثبت گزارش ناموفق بود": return "رپورٹ جمع نہیں ہو سکی";
                case "چت از حساب شما پاک شد": return "چیٹ آپ کے اکاؤنٹ سے حذف کر دی گئی";
                case "پاک کردن چت ناموفق بود": return "چیٹ حذف نہیں ہو سکی";
                case "دسترسی به پیام‌های چت ناموفق بود": return "چیٹ پیغامات تک رسائی ناکام رہی";
                case "کاربر انتخاب نشده است": return "کوئی صارف منتخب نہیں کیا گیا";
                case "کاربر مسدود شد؛ پیام و صدا متوقف شد": return "صارف بلاک ہے؛ پیغامات اور آواز روک دی گئی";
                case "خطای نامشخص": return "نامعلوم خرابی";
                case "مسدود کردن ناموفق بود:\n": return "بلاک کرنے میں ناکامی:\n";
                case "🚫 فهرست مسدودشدهها": return "🚫 مسدود صارفین";
                case "آیا میخواهید «": return "کیا آپ «";
                case "دریافت فهرست مسدودشدهها انجام نشد": return "مسدود صارفین کی فہرست حاصل نہیں ہو سکی";
                case "این نوع فایل پشتیبانی نمیشود": return "اس فائل کی قسم سپورٹ نہیں ہے";
                case "فضای ذخیرهسازی باز نشد": return "اسٹوریج نہیں کھل سکا";
                case "ذخیره مستقیم در گالری در این نسخه اندروید پشتیبانی نمیشود": return "اس Android ورژن میں گیلری میں براہِ راست محفوظ کرنا سپورٹ نہیں ہے";
                case "🔕 بیصدا کردن اعلانهای این چت": return "🔕 اس چیٹ کی اطلاعات خاموش کریں";
                case "اعلانهای این چت بیصدا شد": return "چیٹ کی اطلاعات خاموش کر دی گئی ہیں";
                case "آیا مطمئن هستید که میخواهید تمام این گفتگو را حذف کنید؟": return "کیا آپ واقعی پوری گفتگو حذف کرنا چاہتے ہیں؟";
                case "این کار تمام پیامهای این گفتگو را حذف میکند و قابل برگشت نیست. ادامه میدهید؟": return "یہ اس گفتگو کے تمام پیغامات حذف کر دے گا اور واپس نہیں ہو سکتا۔ جاری رکھیں؟";
                case "آیا میخواهید این کاربر را مسدود کنید؟": return "کیا آپ اس صارف کو بلاک کرنا چاہتے ہیں؟";
                case "دسترسی به پیامهای چت ناموفق بود": return "چیٹ پیغامات تک رسائی ناکام رہی";
            }
        }

        if ("hi".equals(lang)) {
            switch (fa) {
                case "لطفاً اول وارد حساب شوید": return "कृपया पहले खाते में लॉग इन करें";
                case "💬 کاربران": return "💬 उपयोगकर्ता";
                case "یک کاربر را انتخاب کنید تا چت خصوصی باز شود.": return "निजी चैट खोलने के लिए उपयोगकर्ता चुनें।";
                case "هنوز کاربر دیگری پیدا نشد.": return "अभी कोई अन्य उपयोगकर्ता नहीं मिला।";
                case "خطا در دریافت کاربران": return "उपयोगकर्ताओं को प्राप्त करने में त्रुटि";
                case "کاربر": return "उपयोगकर्ता";
                case " 🚫 مسدود": return " 🚫 अवरुद्ध";
                case " آنلاین": return " ऑनलाइन";
                case " آفلاین": return " ऑफ़लाइन";
                case "👁 مخفی کردن / رفع مخفی": return "👁 छिपाएँ / दिखाएँ";
                case "🚫 بلاک / رفع مسدودیت": return "🚫 ब्लॉक / अनब्लॉक";
                case "مخفی بودن کاربر رفع شد": return "उपयोगकर्ता को दिखाया गया";
                case "کاربر مخفی شد": return "उपयोगकर्ता छिपा दिया गया";
                case "مسدودیت کاربر رفع شد": return "उपयोगकर्ता को अनब्लॉक कर दिया गया";
                case "کاربر بلاک شد؛ ارسال پیام و صدا متوقف شد": return "उपयोगकर्ता ब्लॉक है; संदेश और आवाज़ रोक दी गई";
                case "📷 تغییر عکس پروفایل": return "📷 प्रोफ़ाइल फ़ोटो बदलें";
                case "🔒 تنظیمات حریم خصوصی": return "🔒 गोपनीयता सेटिंग्स";
                case "🚫 فهرست مسدودشده‌ها": return "🚫 अवरुद्ध उपयोगकर्ता";
                case "پروفایل": return "प्रोफ़ाइल";
                case "بستن": return "बंद करें";
                case "هیچ کاربری مسدود نشده است.": return "कोई उपयोगकर्ता अवरुद्ध नहीं है।";
                case "   •   رفع مسدودی": return "   •   अनब्लॉक";
                case "رفع مسدودی": return "अनब्लॉक";
                case "آیا می‌خواهید «": return "क्या आप «";
                case "» را از مسدودی خارج کنید؟": return "» को अनब्लॉक करना चाहते हैं?";
                case "لغو": return "रद्द करें";
                case "مسدودی برداشته شد": return "अनब्लॉक किया गया";
                case "دریافت فهرست مسدودشده‌ها انجام نشد": return "अवरुद्ध उपयोगकर्ताओं की सूची प्राप्त नहीं हो सकी";
                case "در حال بررسی...": return "जाँच हो रही है...";
                case "پیام خود را بنویسید...": return "अपना संदेश लिखें...";
                case "پیام حذف شد": return "संदेश हटा दिया गया";
                case "▶️ پخش پیام صوتی": return "▶️ वॉइस संदेश चलाएँ";
                case "حذف برای من": return "मेरे लिए हटाएँ";
                case "حذف برای همه": return "सबके लिए हटाएँ";
                case "حذف پیام ناموفق بود": return "संदेश हटाया नहीं जा सका";
                case "این کاربر بلاک شده است": return "यह उपयोगकर्ता ब्लॉक है";
                case "خطا در ارسال پیام": return "संदेश भेजने में त्रुटि";
                case "آنلاین": return "ऑनलाइन";
                case "آفلاین": return "ऑफ़लाइन";
                case "در حال نوشتن...": return "टाइप कर रहा है...";
                case "🚫 این کاربر مسدود است": return "🚫 यह उपयोगकर्ता ब्लॉक है";
                case "🚫 مسدود شده": return "🚫 अवरुद्ध";
                case "این کاربر مسدود شده است": return "यह उपयोगकर्ता ब्लॉक है";
                case "این کاربر شما را مسدود کرده است": return "इस उपयोगकर्ता ने आपको ब्लॉक किया है";
                case "این نوع فایل پشتیبانی نمی‌شود": return "यह फ़ाइल प्रकार समर्थित नहीं है";
                case "در حال ارسال فایل...": return "फ़ाइल भेजी जा रही है...";
                case "خطای ارسال:\n": return "भेजने में त्रुटि:\n";
                case "این کاربر بلاک شده است؛ فایل ارسال نشد": return "यह उपयोगकर्ता ब्लॉक है; फ़ाइल नहीं भेजी गई";
                case "خطا در ذخیره پیام فایل": return "फ़ाइल संदेश सहेजने में त्रुटि";
                case "🎤 در حال ضبط... دوباره بزنید تا ارسال شود": return "🎤 रिकॉर्डिंग... भेजने के लिए फिर दबाएँ";
                case "خطا در شروع ضبط: ": return "रिकॉर्डिंग शुरू करने में त्रुटि: ";
                case "ضبط صدا ناموفق بود": return "वॉइस रिकॉर्डिंग विफल हुई";
                case "این کاربر بلاک شده است؛ پیام صوتی ارسال نشد": return "यह उपयोगकर्ता ब्लॉक है; वॉइस संदेश नहीं भेजा गया";
                case "فایل صوتی پیدا نشد": return "ऑडियो फ़ाइल नहीं मिली";
                case "فایل صوتی خالی است": return "ऑडियो फ़ाइल खाली है";
                case "در حال ارسال پیام صوتی...": return "वॉइस संदेश भेजा जा रहा है...";
                case "خطای ارسال پیام صوتی:\n": return "वॉइस संदेश भेजने में त्रुटि:\n";
                case "خطا در ذخیره پیام صوتی": return "वॉइस संदेश सहेजने में त्रुटि";
                case "پخش صدا ناموفق بود": return "ऑडियो चलाना विफल हुआ";
                case "خطا در پخش صدا": return "ऑडियो चलाने में त्रुटि";
                case "در حال ارسال عکس پروفایل...": return "प्रोफ़ाइल फ़ोटो अपलोड हो रही है...";
                case "عکس پروفایل ذخیره شد": return "प्रोफ़ाइल फ़ोटो सहेजी गई";
                case "خطای عکس پروفایل:\n": return "प्रोफ़ाइल फ़ोटो त्रुटि:\n";
                case "فایل وجود ندارد": return "फ़ाइल मौजूद नहीं है";
                case "فایل خالی است": return "फ़ाइल खाली है";
                case "فایل قابل خواندن نیست": return "फ़ाइल पढ़ने योग्य नहीं है";
                case "💾 ذخیره عکس در گالری": return "💾 गैलरी में फ़ोटो सहेजें";
                case "تصویر": return "छवि";
                case "گالری قابل دسترسی نیست": return "गैलरी उपलब्ध नहीं है";
                case "فضای ذخیره‌سازی باز نشد": return "स्टोरेज नहीं खोला जा सका";
                case "✅ عکس در گالری ذخیره شد": return "✅ फ़ोटो गैलरी में सहेजी गई";
                case "ذخیره مستقیم در گالری در این نسخه اندروید پشتیبانی نمی‌شود": return "इस Android संस्करण में गैलरी में सीधे सहेजना समर्थित नहीं है";
                case "خطا در ذخیره عکس": return "फ़ोटो सहेजने में त्रुटि";
                case "خطای ذخیره عکس:\n": return "फ़ोटो सहेजने में त्रुटि:\n";
                case "اجازه میکروفون داده نشد": return "माइक्रोफ़ोन की अनुमति नहीं दी गई";
                case "🔕 بی‌صدا کردن اعلان‌های این چت": return "🔕 इस चैट की सूचनाएँ म्यूट करें";
                case "👤 مشاهده پروفایل": return "👤 प्रोफ़ाइल देखें";
                case "🗑️ حذف کامل چت": return "🗑️ पूरी चैट हटाएँ";
                case "🚫 مسدود کردن": return "🚫 ब्लॉक करें";
                case "تنظیمات چت": return "चैट सेटिंग्स";
                case "اعلان‌های این چت بی‌صدا شد": return "चैट सूचनाएँ म्यूट कर दी गईं";
                case "⚠️ حذف کامل چت": return "⚠️ पूरी चैट हटाएँ";
                case "آیا مطمئن هستید که می‌خواهید تمام این گفتگو را حذف کنید؟": return "क्या आप वाकई पूरी बातचीत हटाना चाहते हैं?";
                case "مرحله اول": return "चरण 1";
                case "تأیید نهایی حذف": return "हटाने की अंतिम पुष्टि";
                case "این کار تمام پیام‌های این گفتگو را حذف می‌کند و قابل برگشت نیست. ادامه می‌دهید؟": return "यह इस बातचीत के सभी संदेश हटा देगा और इसे वापस नहीं किया जा सकता। जारी रखें?";
                case "حذف کامل": return "सब हटाएँ";
                case "مسدود کردن کاربر": return "उपयोगकर्ता को ब्लॉक करें";
                case "آیا می‌خواهید این کاربر را مسدود کنید؟": return "क्या आप इस उपयोगकर्ता को ब्लॉक करना चाहते हैं?";
                case "مسدود کردن": return "ब्लॉक करें";
                case "نمایش آنلاین بودن": return "ऑनलाइन स्थिति दिखाएँ";
                case "نمایش آخرین بازدید": return "अंतिम बार देखा गया दिखाएँ";
                case "نمایش «در حال نوشتن…»": return "“टाइप कर रहा है…” दिखाएँ";
                case "نمایش رسید خوانده شدن ✓✓": return "पढ़े जाने की रसीद ✓✓ दिखाएँ";
                case "ذخیره": return "सहेजें";
                case "تنظیمات ذخیره شد": return "सेटिंग्स सहेजी गईं";
                case "ذخیره تنظیمات ناموفق بود": return "सेटिंग्स सहेजी نہیں जा सकीं";
                case "گزارش شما ثبت شد": return "आपकी रिपोर्ट दर्ज हो गई";
                case "ثبت گزارش ناموفق بود": return "रिपोर्ट दर्ज नहीं हो सकी";
                case "چت از حساب شما پاک شد": return "चैट आपके खाते से हटा दी गई";
                case "پاک کردن چت ناموفق بود": return "चैट हटाई नहीं जा सकी";
                case "دسترسی به پیام‌های چت ناموفق بود": return "चैट संदेशों तक पहुँच विफल हुई";
                case "کاربر انتخاب نشده است": return "कोई उपयोगकर्ता चयनित नहीं है";
                case "کاربر مسدود شد؛ پیام و صدا متوقف شد": return "उपयोगकर्ता ब्लॉक है; संदेश और आवाज़ रोक दी गई";
                case "خطای نامشخص": return "अज्ञात त्रुटि";
                case "مسدود کردن ناموفق بود:\n": return "ब्लॉक करने में विफल:\n";
                case "🚫 فهرست مسدودشدهها": return "🚫 अवरुद्ध उपयोगकर्ता";
                case "آیا میخواهید «": return "क्या आप «";
                case "دریافت فهرست مسدودشدهها انجام نشد": return "अवरुद्ध उपयोगकर्ताओं की सूची प्राप्त नहीं हो सकी";
                case "این نوع فایل پشتیبانی نمیشود": return "यह फ़ाइल प्रकार समर्थित नहीं है";
                case "فضای ذخیرهسازی باز نشد": return "स्टोरेज नहीं खोला जा सका";
                case "ذخیره مستقیم در گالری در این نسخه اندروید پشتیبانی نمیشود": return "इस Android संस्करण में गैलरी में सीधे सहेजना समर्थित नहीं है";
                case "🔕 بیصدا کردن اعلانهای این چت": return "🔕 इस चैट की सूचनाएँ म्यूट करें";
                case "اعلانهای این چت بیصدا شد": return "चैट सूचनाएँ म्यूट कर दी गईं";
                case "آیا مطمئن هستید که میخواهید تمام این گفتگو را حذف کنید؟": return "क्या आप वाकई पूरी बातचीत हटाना चाहते हैं?";
                case "این کار تمام پیامهای این گفتگو را حذف میکند و قابل برگشت نیست. ادامه میدهید؟": return "यह इस बातचीत के सभी संदेश हटा देगा और इसे वापस नहीं किया जा सकता। जारी रखें?";
                case "آیا میخواهید این کاربر را مسدود کنید؟": return "क्या आप इस उपयोगकर्ता को ब्लॉक करना चाहते हैं?";
                case "دسترسی به پیامهای چت ناموفق بود": return "चैट संदेशों तक पहुँच विफल हुई";
            }
        }

        return fa;
    }

    private int dp(int value) {
        return (int) (
                value *
                        getResources()
                                .getDisplayMetrics()
                                .density +
                        0.5f
        );
    }

    private GradientDrawable bg(
            int color,
            float radius
    ) {

        GradientDrawable d =
                new GradientDrawable();

        d.setColor(color);

        d.setCornerRadius(
                dp((int) radius)
        );

        return d;
    }

    private TextView text(
            String value,
            int size
    ) {

        TextView t =
                new TextView(this);

        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(Color.DKGRAY);

        t.setPadding(
                dp(8),
                dp(6),
                dp(8),
                dp(6)
        );

        return t;
    }

    private ImageView avatarView(int size) {

        ImageView image =
                new ImageView(this);

        image.setLayoutParams(
                new LinearLayout.LayoutParams(
                        dp(size),
                        dp(size)
                )
        );

        image.setScaleType(
                ImageView.ScaleType.CENTER_CROP
        );

        image.setImageResource(
                android.R.drawable.ic_menu_gallery
        );

        image.setBackground(
                bg(Color.LTGRAY, 50)
        );

        return image;
    }

    private void ensureUserProfile() {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) return;

        Map<String, Object> data =
                new HashMap<>();

        data.put("userId", myId);

        data.put(
                "email",
                user.getEmail() == null
                        ? ""
                        : user.getEmail()
        );

        data.put("online", true);

        data.put(
                "lastSeen",
                FieldValue.serverTimestamp()
        );

        data.put("typingTo", "");

        if (user.getPhoneNumber() != null) {

            data.put(
                    "phoneNumber",
                    user.getPhoneNumber()
            );
        }

        if (user.getDisplayName() != null &&
                !user.getDisplayName()
                        .trim()
                        .isEmpty()) {

            data.put(
                    "name",
                    user.getDisplayName()
            );
        }

        db.collection("users")
                .document(myId)
                .set(
                        data,
                        SetOptions.merge()
                );
    }

    private void createUsersScreen() {

        insideChat = false;

        root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setBackgroundColor(
                Color.rgb(235, 248, 250)
        );

        LinearLayout header =
                new LinearLayout(this);

        header.setGravity(
                Gravity.CENTER_VERTICAL
        );

        header.setPadding(
                dp(10),
                dp(8),
                dp(10),
                dp(8)
        );

        header.setBackgroundColor(
                themeColor
        );

        ImageButton profile =
                new ImageButton(this);

        profile.setImageResource(
                android.R.drawable.ic_menu_myplaces
        );

        profile.setBackgroundColor(
                Color.TRANSPARENT
        );

        profile.setOnClickListener(
                v -> showMyProfile()
        );

    header.addView(
        profile,
        new LinearLayout.LayoutParams(
                dp(48),
                dp(48)
        )
);

TextView myProfileMenu =
        text("⋮", 26);

myProfileMenu.setTextColor(
        Color.WHITE
);

myProfileMenu.setGravity(
        Gravity.CENTER
);

myProfileMenu.setOnClickListener(
        v -> showChatPrivacySettings()
);

header.addView(
        myProfileMenu,
        new LinearLayout.LayoutParams(
                dp(40),
                dp(48)
        )
);

titleText =
        text(tr("💬 کاربران"), 21);

        titleText.setTextColor(
                Color.WHITE
        );

        titleText.setTypeface(
                null,
                Typeface.BOLD
        );

        LinearLayout.LayoutParams titleParams =
                new LinearLayout.LayoutParams(
                        0,
                        -2,
                        1
                );

        header.addView(
                titleText,
                titleParams
        );

        ImageButton refresh =
                new ImageButton(this);

        refresh.setImageResource(
                android.R.drawable.ic_popup_sync
        );

        refresh.setBackgroundColor(
                Color.TRANSPARENT
        );

        refresh.setOnClickListener(
                v -> loadUsers()
        );

        header.addView(
                refresh,
                new LinearLayout.LayoutParams(
                        dp(48),
                        dp(48)
                )
        );

        root.addView(header);

        TextView info =
                text(
                        tr("یک کاربر را انتخاب کنید تا چت خصوصی باز شود."),
                        14
                );

        info.setGravity(
                Gravity.CENTER
        );

        root.addView(info);

        ScrollView scroll =
                new ScrollView(this);

        usersContainer =
                new LinearLayout(this);

        usersContainer.setOrientation(
                LinearLayout.VERTICAL
        );

        usersContainer.setPadding(
                dp(8),
                dp(4),
                dp(8),
                dp(12)
        );

        scroll.addView(usersContainer);

        root.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                )
        );

        setContentView(root);
    }

    private void loadUsers() {

        if (usersContainer == null) return;

        usersContainer.removeAllViews();

        db.collection("hiddenUsers")
                .whereEqualTo(
                        "ownerId",
                        myId
                )
                .get()
                .addOnSuccessListener(
                        snapshot -> {

                            hiddenUserIds.clear();

                            for (
                                    DocumentSnapshot d :
                                    snapshot.getDocuments()
                            ) {

                                String id =
                                        d.getString(
                                                "hiddenUserId"
                                        );

                                if (id != null) {
                                    hiddenUserIds.add(id);
                                }
                            }

                            loadBlockedUsers();
                        }
                )
                .addOnFailureListener(
                        e -> loadBlockedUsers()
                );
    }

    private void loadBlockedUsers() {

        blockedUserIds.clear();

        db.collection("blocks")
                .get()
                .addOnSuccessListener(
                        snapshot -> {

                            for (
                                    DocumentSnapshot d :
                                    snapshot.getDocuments()
                            ) {

                                String ownerId =
                                        d.getString("ownerId");

                                String blockedId =
                                        d.getString("blockedUserId");

                                if (myId.equals(ownerId) &&
                                        blockedId != null &&
                                        !blockedId.isEmpty()) {
                                    blockedUserIds.add(blockedId);
                                }

                                if (myId.equals(blockedId) &&
                                        ownerId != null &&
                                        !ownerId.isEmpty()) {
                                    blockedUserIds.add(ownerId);
                                }
                            }

                            loadVisibleUsers();
                        }
                )
                .addOnFailureListener(
                        e -> loadVisibleUsers()
                );
    }

    private void loadVisibleUsers() {

        db.collection("users")
                .get()
                .addOnSuccessListener(
                        snapshot -> {

                            usersContainer
                                    .removeAllViews();

                            List<DocumentSnapshot> users =
                                    new ArrayList<>();

                            Set<String> ids =
                                    new HashSet<>();

                            for (
                                    DocumentSnapshot d :
                                    snapshot.getDocuments()
                            ) {

                                // شناسه واقعی حساب را از userId می‌گیریم، نه فقط Document ID.
                                // اگر یک حساب دو سند داشته باشد، فقط یک‌بار نمایش داده می‌شود.
                                String id =
                                        d.getString("userId");

                                if (id == null ||
                                        id.trim().isEmpty()) {
                                    id = d.getId();
                                }

                                if (id.equals(myId)) {
                                    continue;
                                }

                                if (hiddenUserIds.contains(id) ||
                                        hiddenUserIds.contains(d.getId())) {
                                    continue;
                                }

                                if (!ids.add(id)) {
                                    continue;
                                }

                                users.add(d);
                            }

                            Collections.sort(
                                    users,
                                    Comparator.comparing(
                                            d -> {

                                                String n =
                                                        d.getString(
                                                                "name"
                                                        );

                                                return n == null
                                                        ? ""
                                                        : n;
                                            },
                                            String.CASE_INSENSITIVE_ORDER
                                    )
                            );

                            if (users.isEmpty()) {

                                TextView empty =
                                        text(
                                                tr("هنوز کاربر دیگری پیدا نشد."),
                                                16
                                        );

                                empty.setGravity(
                                        Gravity.CENTER
                                );

                                usersContainer
                                        .addView(empty);

                                return;
                            }

                            for (
                                    DocumentSnapshot d :
                                    users
                            ) {

                                addUserItem(d);
                            }
                        }
                )
                .addOnFailureListener(
                        e ->
                                Toast.makeText(
                                        this,
                                        tr("خطا در دریافت کاربران"),
                                        Toast.LENGTH_SHORT
                                ).show()
                );
    }

    private void addUserItem(
            DocumentSnapshot d
    ) {

        String uid =
                d.getString("userId");

        if (uid == null ||
                uid.trim().isEmpty()) {
            uid = d.getId();
        }

        String name =
                d.getString("name");

        if (name == null ||
                name.trim().isEmpty()) {

            name =
                    d.getString("email");
        }

        if (name == null ||
                name.trim().isEmpty()) {

            name = tr("کاربر");
        }

        String photoUrl =
                d.getString("photoUrl");

        Boolean online =
                d.getBoolean("online");

        // کاربر بلاک‌شده نباید در فهرست به‌صورت آنلاین نمایش داده شود.
        boolean isBlockedUser =
                blockedUserIds.contains(uid);

        if (isBlockedUser) {
            online = false;
        }

        final boolean finalBlockedUser = isBlockedUser;
        final String finalUid = uid;
        final String finalName = name;
        final String finalPhotoUrl = photoUrl;

        LinearLayout card =
                new LinearLayout(this);

        card.setOrientation(
                LinearLayout.HORIZONTAL
        );

        card.setGravity(
                Gravity.CENTER_VERTICAL
        );

        card.setPadding(
                dp(10),
                dp(8),
                dp(10),
                dp(8)
        );

        card.setBackground(
                bg(Color.WHITE, 18)
        );

        LinearLayout.LayoutParams cp =
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                );

        cp.setMargins(
                0,
                dp(5),
                0,
                dp(5)
        );

        card.setLayoutParams(cp);

        ImageView avatar =
                avatarView(76);

        if (finalPhotoUrl != null &&
                !finalPhotoUrl.isEmpty()) {

            loadImage(
                    finalPhotoUrl,
                    avatar
            );
        }

        avatar.setOnClickListener(
                v ->
                        showProfileDialog(
                                finalUid,
                                finalName,
                                finalPhotoUrl
                        )
        );

        card.addView(avatar);

        LinearLayout info =
                new LinearLayout(this);

        info.setOrientation(
                LinearLayout.VERTICAL
        );

        info.setPadding(
                dp(10),
                0,
                dp(5),
                0
        );

        TextView nameText =
                text(
                        finalName,
                        17
                );

        nameText.setTypeface(
                null,
                Typeface.BOLD
        );

        LinearLayout statusRow =
                new LinearLayout(this);

        statusRow.setGravity(
                Gravity.CENTER_VERTICAL
        );

        TextView dot =
                text(
                        online != null &&
                                online
                                ? "●"
                                : "○",
                        14
                );

        dot.setTextColor(
                online != null && online
                        ? Color.rgb(0, 160, 70)
                        : Color.GRAY
        );

        TextView status =
                text(
                        finalBlockedUser
                                ? tr(" 🚫 مسدود")
                                : (online != null && online
                                        ? tr(" آنلاین")
                                        : tr(" آفلاین")),
                        13
                );

        if (finalBlockedUser) {
            dot.setText("🚫");
            dot.setTextColor(Color.rgb(190, 40, 40));
            status.setTextColor(Color.rgb(190, 40, 40));
        }

        statusRow.addView(dot);
        statusRow.addView(status);

        info.addView(nameText);
        info.addView(statusRow);

        card.addView(
                info,
                new LinearLayout.LayoutParams(
                        0,
                        -2,
                        1
                )
        );

        TextView menu =
                text("⋮", 26);

        menu.setGravity(
                Gravity.CENTER
        );

        menu.setOnClickListener(
                v ->
                        showUserMenu(
                                finalUid,
                                finalName
                        )
        );

        card.addView(
                menu,
                new LinearLayout.LayoutParams(
                        dp(40),
                        dp(60)
                )
        );

        View.OnClickListener open =
                v ->
                        openPrivateChat(
                                finalUid,
                                finalName,
                                finalPhotoUrl
                        );

        card.setOnClickListener(open);
        nameText.setOnClickListener(open);
        info.setOnClickListener(open);

        usersContainer.addView(card);
    }
    
private void showUserMenu(
        String uid,
        String name
) {

    String[] items = {
            tr("👁 مخفی کردن / رفع مخفی"),
            tr("🚫 بلاک / رفع مسدودیت")
    };

    new AlertDialog.Builder(this)
            .setTitle(name)
            .setItems(
                    items,
                    (d, which) -> {

                        if (which == 0) {

                            // بررسی وضعیت مخفی بودن
                            db.collection("hiddenUsers")
                                    .whereEqualTo("ownerId", myId)
                                    .whereEqualTo("hiddenUserId", uid)
                                    .get()
                                    .addOnSuccessListener(snapshot -> {

                                        if (!snapshot.isEmpty()) {

                                            // رفع مخفی
                                            for (com.google.firebase.firestore.DocumentSnapshot document
                                                    : snapshot.getDocuments()) {

                                                db.collection("hiddenUsers")
                                                        .document(document.getId())
                                                        .delete();
                                            }

                                            Toast.makeText(
                                                    this,
                                                    tr("مخفی بودن کاربر رفع شد"),
                                                    Toast.LENGTH_SHORT
                                            ).show();

                                            loadUsers();

                                        } else {

                                            // مخفی کردن
                                            Map<String, Object> data =
                                                    new HashMap<>();

                                            data.put(
                                                    "ownerId",
                                                    myId
                                            );

                                            data.put(
                                                    "hiddenUserId",
                                                    uid
                                            );

                                            data.put(
                                                    "hiddenUserName",
                                                    name
                                            );

                                            db.collection("hiddenUsers")
                                                    .add(data)
                                                    .addOnSuccessListener(
                                                            x -> {
                                                                Toast.makeText(
                                                                        this,
                                                                        tr("کاربر مخفی شد"),
                                                                        Toast.LENGTH_SHORT
                                                                ).show();

                                                                loadUsers();
                                                            }
                                                    );
                                        }
                                    });

                        } else {

                            // بررسی وضعیت بلاک
                            db.collection("blocks")
                                    .whereEqualTo("ownerId", myId)
                                    .whereEqualTo("blockedUserId", uid)
                                    .get()
                                    .addOnSuccessListener(snapshot -> {

                                        if (!snapshot.isEmpty()) {

                                            // رفع مسدودیت
                                            for (com.google.firebase.firestore.DocumentSnapshot document
                                                    : snapshot.getDocuments()) {

                                                db.collection("blocks")
                                                        .document(document.getId())
                                                        .delete();
                                            }

                                            if (receiverId != null &&
                                                    receiverId.equals(uid)) {
                                                blockedByMe = false;
                                                blocked = blockedByReceiver;
                                                blockChecksReady = 2;
                                                updateBlockStateAfterCheck();
                                            }

                                            Toast.makeText(
                                                    this,
                                                    tr("مسدودیت کاربر رفع شد"),
                                                    Toast.LENGTH_SHORT
                                            ).show();

                                            loadUsers();

                                        } else {

                                            // بلاک کردن
                                            Map<String, Object> data =
                                                    new HashMap<>();

                                            data.put(
                                                    "ownerId",
                                                    myId
                                            );

                                            data.put(
                                                    "blockedUserId",
                                                    uid
                                            );

                                            data.put(
                                                    "blockedUserName",
                                                    name
                                            );

                                            db.collection("blocks")
                                                    .document(
                                                            myId + "_" + uid
                                                    )
                                                    .set(data)
                                                    .addOnSuccessListener(
                                                            x -> {
                                                                if (receiverId != null &&
                                                                        receiverId.equals(uid)) {
                                                                    blockedByMe = true;
                                                                    blocked = true;
                                                                    blockChecksReady = 2;
                                                                    updateBlockStateAfterCheck();
                                                                }

                                                                Toast.makeText(
                                                                        this,
                                                                        tr("کاربر بلاک شد؛ ارسال پیام و صدا متوقف شد"),
                                                                        Toast.LENGTH_SHORT
                                                                ).show();

                                                                loadUsers();
                                                            }
                                                    );
                                        }
                                    });
                        }
                    }
            )
            .show();
}

    private void showMyProfile() {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) return;

        db.collection("users")
                .document(myId)
                .get()
                .addOnSuccessListener(
                        d -> {

                            String name =
                                    d.getString("name");

                            String photo =
                                    d.getString("photoUrl");

                            if (name == null) {
                                name = tr("کاربر");
                            }

                            showProfileDialog(
                                    myId,
                                    name,
                                    photo
                            );
                        }
                );
    }

    private void showProfileDialog(
            String uid,
            String name,
            String photoUrl
    ) {

        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL
        );

        box.setGravity(
                Gravity.CENTER
        );

        box.setPadding(
                dp(20),
                dp(15),
                dp(20),
                dp(15)
        );

        ImageView image =
                avatarView(220);

        if (photoUrl != null &&
                !photoUrl.isEmpty()) {

            loadImage(
                    photoUrl,
                    image
            );
        }

        image.setOnClickListener(
                v -> {

                    if (photoUrl != null &&
                            !photoUrl.isEmpty()) {

                        showImageViewer(
                                photoUrl
                        );
                    }
                }
        );

        box.addView(image);

        TextView nameText =
                text(
                        name,
                        20
                );

        nameText.setGravity(
                Gravity.CENTER
        );

        nameText.setTypeface(
                null,
                Typeface.BOLD
        );

        box.addView(nameText);

        Button change =
                new Button(this);

        change.setText(
                tr("📷 تغییر عکس پروفایل")
        );

        if (!uid.equals(myId)) {

            change.setVisibility(
                    View.GONE
            );
        }

        change.setOnClickListener(
                v -> pickProfilePhoto()
        );

        box.addView(change);
        
if (uid.equals(myId)) {

    Button privacy =
            new Button(this);

    privacy.setText(
            tr("🔒 تنظیمات حریم خصوصی")
    );

    privacy.setOnClickListener(
            v -> showChatPrivacySettings()
    );

    box.addView(privacy);


    Button blocked =
            new Button(this);

    blocked.setText(
            tr("🚫 فهرست مسدودشده‌ها")
    );

    blocked.setOnClickListener(
            v -> showBlockedUsers()
    );

    box.addView(blocked);
}
        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(tr("پروفایل"))
                        .setView(box)
                        .setPositiveButton(
                                tr("بستن"),
                                null
                        )
                        .create();

        dialog.show();
    }

    private void showBlockedUsers() {

    db.collection("blocks")
            .whereEqualTo(
                    "ownerId",
                    myId
            )
            .get()
            .addOnSuccessListener(
                    snapshot -> {

                        if (snapshot.isEmpty()) {

                            new AlertDialog.Builder(this)
                                    .setTitle(
                                            tr("🚫 فهرست مسدودشده‌ها")
                                    )
                                    .setMessage(
                                            tr("هیچ کاربری مسدود نشده است.")
                                    )
                                    .setPositiveButton(
                                            tr("بستن"),
                                            null
                                    )
                                    .show();

                            return;
                        }

                        LinearLayout box =
                                new LinearLayout(this);

                        box.setOrientation(
                                LinearLayout.VERTICAL
                        );

                        box.setPadding(
                                dp(20),
                                dp(10),
                                dp(20),
                                dp(10)
                        );

                        for (
                                DocumentSnapshot doc :
                                snapshot.getDocuments()
                        ) {

                            String uid =
                                    doc.getString(
                                            "blockedUserId"
                                    );

                            String name =
                                    doc.getString(
                                            "blockedUserName"
                                    );

                            if (uid == null ||
                                    uid.isEmpty()) {
                                continue;
                            }

                            if (name == null ||
                                    name.isEmpty()) {
                                name = tr("کاربر");
                            }

                            Button unblock =
                                    new Button(this);

                            unblock.setText(
                                    "🚫 " +
                                    name +
                                    tr("   •   رفع مسدودی")
                            );

                            String finalName = name;

                            unblock.setOnClickListener(
                                    v -> {

                                        new AlertDialog.Builder(
                                                this
                                        )
                                                .setTitle(
                                                        tr("رفع مسدودی")
                                                )
                                                .setMessage(
                                                        tr("آیا می‌خواهید «") +
                                                        finalName +
                                                        tr("» را از مسدودی خارج کنید؟")
                                                )
                                                .setNegativeButton(
                                                        tr("لغو"),
                                                        null
                                                )
                                                .setPositiveButton(
                                                        tr("رفع مسدودی"),
                                                        (d, w) -> {

                                                            db.collection(
                                                                    "blocks"
                                                            )
                                                                    .document(
                                                                            doc.getId()
                                                                    )
                                                                    .delete()
                                                                    .addOnSuccessListener(
                                                                            x -> {

                                                                                Toast.makeText(
                                                                                        this,
                                                                                        tr("مسدودی برداشته شد"),
                                                                                        Toast.LENGTH_SHORT
                                                                                ).show();

                                                                                showBlockedUsers();
                                                                            }
                                                                    );
                                                        }
                                                )
                                                .show();
                                    }
                            );

                            box.addView(
                                    unblock
                            );
                        }

                        new AlertDialog.Builder(this)
                                .setTitle(
                                        tr("🚫 فهرست مسدودشده‌ها")
                                )
                                .setView(box)
                                .setPositiveButton(
                                        tr("بستن"),
                                        null
                                )
                                .show();
                    }
            )
            .addOnFailureListener(
                    e -> Toast.makeText(
                            this,
                            tr("دریافت فهرست مسدودشده‌ها انجام نشد"),
                            Toast.LENGTH_SHORT
                    ).show()
            );
}
    private void openPrivateChat(
            String uid,
            String name,
            String photoUrl
    ) {

        receiverId = uid;
        receiverName = name;
receiverPhotoUrl = photoUrl;
        
        currentChatId =
                makeChatId(
                        myId,
                        receiverId
                );

        messageCache.clear();

        insideChat = true;
        blocked = false;

        createChatScreen(photoUrl);

        listenReceiver();
        listenTyping();
        listenBlock();
    }

    private String makeChatId(
            String a,
            String b
    ) {

        if (a.compareTo(b) < 0) {
            return a + "_" + b;
        }

        return b + "_" + a;
    }

    private void createChatScreen(
            String photoUrl
    ) {

        root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setBackgroundColor(
                Color.rgb(235, 248, 250)
        );

        LinearLayout header =
                new LinearLayout(this);

        header.setGravity(
                Gravity.CENTER_VERTICAL
        );

        header.setPadding(
                dp(6),
                dp(6),
                dp(6),
                dp(6)
        );

        header.setBackgroundColor(
                themeColor
        );

        ImageButton back =
                new ImageButton(this);

        back.setImageResource(
                android.R.drawable.ic_media_previous
        );

        back.setBackgroundColor(
                Color.TRANSPARENT
        );

        back.setOnClickListener(
                v -> showUsers()
        );

        header.addView(
                back,
                new LinearLayout.LayoutParams(
                        dp(48),
                        dp(48)
                )
        );

        headerAvatar =
                avatarView(62);

        if (photoUrl != null &&
                !photoUrl.isEmpty()) {

            loadImage(
                    photoUrl,
                    headerAvatar
            );
        }

        headerAvatar.setOnClickListener(
                v -> {

                    if (photoUrl != null &&
                            !photoUrl.isEmpty()) {

                        showImageViewer(
                                photoUrl
                        );
                    }
                }
        );

        header.addView(headerAvatar);

        LinearLayout titles =
                new LinearLayout(this);

        titles.setOrientation(
                LinearLayout.VERTICAL
        );

        titles.setPadding(
                dp(8),
                0,
                0,
                0
        );

        titleText =
                text(
                        receiverName,
                        18
                );

        titleText.setTextColor(
                Color.WHITE
        );

        titleText.setTypeface(
                null,
                Typeface.BOLD
        );

        statusText =
                text(
                        tr("در حال بررسی..."),
                        13
                );

        statusText.setTextColor(
                Color.WHITE
        );

        titles.addView(titleText);
        titles.addView(statusText);

        header.addView(
                titles,
                new LinearLayout.LayoutParams(
                        0,
                        -2,
                        1
                )
        );

        headerOnlineDot =
                text("●", 15);

        headerOnlineDot.setTextColor(
                Color.GREEN
        );
        
        TextView chatMenu = new TextView(this);
chatMenu.setText("⋮");
chatMenu.setTextColor(Color.WHITE);
chatMenu.setTextSize(28);
chatMenu.setGravity(Gravity.CENTER);
chatMenu.setPadding(8, 0, 8, 0);

chatMenu.setOnClickListener(v -> showChatMenu());

header.addView(chatMenu,
        new LinearLayout.LayoutParams(
                48,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        header.addView(
                headerOnlineDot
        );

        root.addView(header);

        messagesScroll =
                new ScrollView(this);

        messagesContainer =
                new LinearLayout(this);

        messagesContainer.setOrientation(
                LinearLayout.VERTICAL
        );

        messagesContainer.setPadding(
                dp(8),
                dp(8),
                dp(8),
                dp(8)
        );

        messagesScroll.addView(
                messagesContainer
        );

        root.addView(
                messagesScroll,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                )
        );

        LinearLayout bottom =
                new LinearLayout(this);

        bottom.setGravity(
                Gravity.CENTER_VERTICAL
        );

        bottom.setPadding(
                dp(6),
                dp(5),
                dp(6),
                dp(5)
        );

        bottom.setBackgroundColor(
                Color.WHITE
        );

        mediaButton =
                new ImageButton(this);

        mediaButton.setImageResource(
                android.R.drawable.ic_menu_gallery
        );

        mediaButton.setBackgroundColor(
                Color.TRANSPARENT
        );

        mediaButton.setPadding(
                dp(5),
                dp(5),
                dp(5),
                dp(5)
        );

        mediaButton.setOnClickListener(
                v -> pickMedia()
        );

        bottom.addView(
                mediaButton,
                new LinearLayout.LayoutParams(
                        dp(56),
                        dp(56)
                )
        );

        messageInput =
                new EditText(this);

        messageInput.setHint(
                tr("پیام خود را بنویسید...")
        );

        messageInput.setTextSize(15);
        messageInput.setSingleLine(false);

        bottom.addView(
                messageInput,
                new LinearLayout.LayoutParams(
                        0,
                        -2,
                        1
                )
        );

        voiceButton =
                new ImageButton(this);

        voiceButton.setImageResource(
                android.R.drawable.ic_btn_speak_now
        );

        voiceButton.setBackgroundColor(
                Color.TRANSPARENT
        );

        voiceButton.setPadding(
                dp(5),
                dp(5),
                dp(5),
                dp(5)
        );

        voiceButton.setOnClickListener(
                v -> toggleRecording()
        );

        bottom.addView(
                voiceButton,
                new LinearLayout.LayoutParams(
                        dp(58),
                        dp(58)
                )
        );

        sendButton =
                new ImageButton(this);

        sendButton.setImageResource(
                android.R.drawable.ic_menu_send
        );

        sendButton.setBackgroundColor(
                Color.TRANSPARENT
        );

        sendButton.setOnClickListener(
                v -> sendText()
        );

        bottom.addView(
                sendButton,
                new LinearLayout.LayoutParams(
                        dp(52),
                        dp(52)
                )
        );

        root.addView(bottom);

        messageInput.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after
                    ) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count
                    ) {

                        updateTyping(
                                !s.toString()
                                        .trim()
                                        .isEmpty()
                        );
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s
                    ) {
                    }
                }
        );

        setContentView(root);
    }

    private void listenMessages() {

        removeMessageListeners();

        sentMessageListener =
                db.collection("messages")
                        .whereEqualTo(
                                "chatId",
                                currentChatId
                        )
                        .whereEqualTo(
                                "senderId",
                                myId
                        )
                        .addSnapshotListener(
                                (snapshot, error) ->
                                        handleMessages(
                                                snapshot
                                        )
                        );

        receivedMessageListener =
                db.collection("messages")
                        .whereEqualTo(
                                "chatId",
                                currentChatId
                        )
                        .whereEqualTo(
                                "receiverId",
                                myId
                        )
                        .addSnapshotListener(
                                (snapshot, error) ->
                                        handleMessages(
                                                snapshot
                                        )
                        );
    }

    private void handleMessages(
            QuerySnapshot snapshot
    ) {

        if (snapshot == null ||
                messagesContainer == null) {

            return;
        }

        for (
                DocumentChange change :
                snapshot.getDocumentChanges()
        ) {

            messageCache.put(
                    change.getDocument().getId(),
                    change.getDocument()
            );
        }

        renderMessages();
    }

    private void renderMessages() {

        if (messagesContainer == null) {
            return;
        }

        messagesContainer.removeAllViews();

        List<DocumentSnapshot> list =
                new ArrayList<>(
                        messageCache.values()
                );

        Collections.sort(
                list,
                (a, b) -> {

                    com.google.firebase.Timestamp ta =
                            a.getTimestamp(
                                    "timestamp"
                            );

                    com.google.firebase.Timestamp tb =
                            b.getTimestamp(
                                    "timestamp"
                            );

                    if (ta == null &&
                            tb == null) {

                        return 0;
                    }

                    if (ta == null) {
                        return -1;
                    }

                    if (tb == null) {
                        return 1;
                    }

                    return ta.compareTo(tb);
                }
        );

        for (
                DocumentSnapshot d :
                list
        ) {

            renderOneMessage(d);
        }

        if (messagesScroll != null) {

            messagesScroll.post(
                    () ->
                            messagesScroll.fullScroll(
                                    View.FOCUS_DOWN
                            )
            );
        }
    }

    private boolean isDeletedForMe(
            DocumentSnapshot d
    ) {

        List<String> deletedFor =
                (List<String>)
                        d.get("deletedFor");

        return deletedFor != null &&
                deletedFor.contains(myId);
    }

    private void renderOneMessage(
            DocumentSnapshot d
    ) {

        if (isDeletedForMe(d)) {
            return;
        }

        Boolean deleted =
                d.getBoolean(
                        "deletedForAll"
                );

        String sender =
                d.getString("senderId");

        if (deleted != null &&
                deleted) {

            addSimpleMessage(
                    tr("پیام حذف شد"),
                    sender,
                    d.getId()
            );

            return;
        }

        String type =
                d.getString("type");

        if (type == null) {
            type = "text";
        }

        if ("text".equals(type)) {

            String message =
                    d.getString("message");

            if (message == null) {

                message =
                        d.getString("text");
            }

            if (message == null) {
                message = "";
            }

            addTextMessage(
                    message,
                    sender,
                    d.getId()
            );

        } else if ("image".equals(type)) {

            String url =
                    d.getString("mediaUrl");

            if (url != null) {

                addMediaMessage(
                        url,
                        false,
                        sender,
                        d.getId()
                );
            }

        } else if ("video".equals(type)) {

            String url =
                    d.getString("mediaUrl");

            if (url != null) {

                addMediaMessage(
                        url,
                        true,
                        sender,
                        d.getId()
                );
            }

        } else if ("audio".equals(type)) {

            String url =
                    d.getString("audioUrl");

            if (url != null) {

                addAudioMessage(
                        url,
                        sender,
                        d.getId()
                );
            }
        }
    }

    private void addSimpleMessage(
            String message,
            String sender,
            String messageId
    ) {

        addTextMessage(
                message,
                sender,
                messageId
        );
    }

    private void addTextMessage(
            String message,
            String sender,
            String messageId
    ) {

        TextView bubble =
                text(
                        message,
                        16
                );

        boolean mine =
                myId.equals(sender);

        bubble.setTextColor(
                mine
                        ? Color.WHITE
                        : Color.DKGRAY
        );

        bubble.setBackground(
                bg(
                        mine
                                ? themeColor
                                : Color.WHITE,
                        16
                )
        );

        bubble.setPadding(
                dp(13),
                dp(9),
                dp(13),
                dp(9)
        );

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        p.setMargins(
                dp(8),
                dp(4),
                dp(8),
                dp(4)
        );

        p.gravity =
                mine
                        ? Gravity.END
                        : Gravity.START;

        messagesContainer.addView(
                bubble,
                p
        );

        if (messageId != null) {

            bubble.setOnLongClickListener(
                    v -> {

                        showDeleteMenu(
                                messageId
                        );

                        return true;
                    }
            );
        }
    }

    private void addMediaMessage(
            String url,
            boolean video,
            String sender,
            String messageId
    ) {

        ImageView image =
                new ImageView(this);

        image.setLayoutParams(
                new LinearLayout.LayoutParams(
                        dp(300),
                        dp(300)
                )
        );

        image.setScaleType(
                ImageView.ScaleType.CENTER_CROP
        );

        image.setBackground(
                bg(Color.WHITE, 14)
        );

        image.setPadding(
                dp(2),
                dp(2),
                dp(2),
                dp(2)
        );

        loadImage(
                url,
                image
        );

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(
                        dp(300),
                        dp(300)
                );

        boolean mine =
                myId.equals(sender);

        p.gravity =
                mine
                        ? Gravity.END
                        : Gravity.START;

        p.setMargins(
                dp(6),
                dp(5),
                dp(6),
                dp(5)
        );

        messagesContainer.addView(
                image,
                p
        );

        if (!video) {

            image.setOnClickListener(
                    v ->
                            showImageViewer(url)
            );
        }

        if (messageId != null) {

            image.setOnLongClickListener(
                    v -> {

                        showDeleteMenu(
                                messageId
                        );

                        return true;
                    }
            );
        }
    }

    private void addAudioMessage(
            String url,
            String sender,
            String messageId
    ) {

        Button play =
                new Button(this);

        play.setText(
                tr("▶️ پخش پیام صوتی")
        );

        play.setTextSize(14);

        play.setOnClickListener(
                v -> playAudio(url)
        );

        boolean mine =
                myId.equals(sender);

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        p.gravity =
                mine
                        ? Gravity.END
                        : Gravity.START;

        p.setMargins(
                dp(6),
                dp(4),
                dp(6),
                dp(4)
        );

        messagesContainer.addView(
                play,
                p
        );

        if (messageId != null) {

            play.setOnLongClickListener(
                    v -> {

                        showDeleteMenu(
                                messageId
                        );

                        return true;
                    }
            );
        }
    }

    private void showDeleteMenu(
            String messageId
    ) {

        String[] items = {
                tr("حذف برای من"),
                tr("حذف برای همه")
        };

        new AlertDialog.Builder(this)
                .setItems(
                        items,
                        (d, which) -> {

                            if (which == 0) {

                                deleteForMe(
                                        messageId
                                );

                            } else {

                                deleteForAll(
                                        messageId
                                );
                            }
                        }
                )
                .show();
    }

    private void deleteForMe(
            String messageId
    ) {

        db.collection("messages")
                .document(messageId)
                .update(
                        "deletedFor",
                        FieldValue.arrayUnion(
                                myId
                        )
                )
                .addOnSuccessListener(
                        x -> {

                            messageCache.remove(
                                    messageId
                            );

                            renderMessages();

                        }
                )
                .addOnFailureListener(
                        e ->
                                Toast.makeText(
                                        this,
                                        tr("حذف پیام ناموفق بود"),
                                        Toast.LENGTH_SHORT
                                ).show()
                );
    }

    private void deleteForAll(
            String messageId
    ) {

        db.collection("messages")
                .document(messageId)
                .update(
                        "deletedForAll",
                        true
                )
                .addOnSuccessListener(
                        x -> renderMessages()
                )
                .addOnFailureListener(
                        e ->
                                Toast.makeText(
                                        this,
                                        tr("حذف پیام ناموفق بود"),
                                        Toast.LENGTH_SHORT
                                ).show()
                );
    }

    private void sendText() {

        if (blocked) {

            Toast.makeText(
                    this,
                    tr("این کاربر بلاک شده است"),
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (messageInput == null) {
            return;
        }

        String message =
                messageInput
                        .getText()
                        .toString()
                        .trim();

        if (message.isEmpty()) {
            return;
        }

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "chatId",
                currentChatId
        );

        data.put(
                "senderId",
                myId
        );

        data.put(
                "receiverId",
                receiverId
        );

        data.put(
                "type",
                "text"
        );

        data.put(
                "message",
                message
        );

        data.put(
                "timestamp",
                FieldValue.serverTimestamp()
        );

        data.put(
                "read",
                false
        );

        data.put(
                "deletedForAll",
                false
        );

        data.put(
                "deletedFor",
                new ArrayList<>()
        );

        db.collection("messages")
                .add(data)
                .addOnSuccessListener(
                        x ->
                                messageInput
                                        .setText("")
                )
                .addOnFailureListener(
                        e ->
                                Toast.makeText(
                                        this,
                                        tr("خطا در ارسال پیام"),
                                        Toast.LENGTH_SHORT
                                ).show()
                );
    }

    private void updateTyping(
            boolean value
    ) {

        if (receiverId == null) {
            return;
        }

        typing = value;

        db.collection("users")
                .document(myId)
                .update(
                        "typingTo",
                        value
                                ? receiverId
                                : ""
                );

        typingHandler
                .removeCallbacksAndMessages(
                        null
                );

        if (value) {

            typingHandler.postDelayed(
                    () -> {

                        db.collection("users")
                                .document(myId)
                                .update(
                                        "typingTo",
                                        ""
                                );

                        typing = false;
                    },
                    2500
            );
        }
    }

    private void listenReceiver() {

        if (receiverListener != null) {
            receiverListener.remove();
        }

        receiverListener =
                db.collection("users")
                        .document(receiverId)
                        .addSnapshotListener(
                                (d, error) -> {

                                    if (d == null ||
                                            !d.exists()) {
                                        return;
                                    }

                                    Boolean online =
                                            d.getBoolean(
                                                    "online"
                                            );

                                    if (online != null &&
                                            online) {

                                        statusText.setText(
                                                tr("آنلاین")
                                        );

                                        headerOnlineDot
                                                .setTextColor(
                                                        Color.rgb(
                                                                0,
                                                                180,
                                                                70
                                                        )
                                                );

                                    } else {

                                        statusText.setText(
                                                tr("آفلاین")
                                        );

                                        headerOnlineDot
                                                .setTextColor(
                                                        Color.GRAY
                                                );
                                    }
                                }
                        );
    }

    private void listenTyping() {

        if (typingListener != null) {
            typingListener.remove();
        }

        typingListener =
                db.collection("users")
                        .document(receiverId)
                        .addSnapshotListener(
                                (d, error) -> {

                                    if (d == null ||
                                            !d.exists()) {
                                        return;
                                    }

                                    String typingTo =
                                            d.getString(
                                                    "typingTo"
                                            );

                                    if (receiverId
                                            .equals(myId)) {
                                        return;
                                    }

                                    if (myId.equals(
                                            typingTo
                                    )) {

                                        statusText.setText(
                                                tr("در حال نوشتن...")
                                        );
                                    }
                                }
                        );
    }

    private void listenBlock() {

        if (blockListener != null) {
            blockListener.remove();
            blockListener = null;
        }

        if (reverseBlockListener != null) {
            reverseBlockListener.remove();
            reverseBlockListener = null;
        }

        blockedByMe = false;
        blockedByReceiver = false;
        blockChecksReady = 0;

        if (receiverId == null || receiverId.isEmpty()) {
            blocked = false;
            listenMessages();
            return;
        }

        blockListener =
                db.collection("blocks")
                        .whereEqualTo("ownerId", myId)
                        .whereEqualTo("blockedUserId", receiverId)
                        .addSnapshotListener(
                                (snapshot, error) -> {
                                    blockedByMe =
                                            error == null &&
                                            snapshot != null &&
                                            !snapshot.isEmpty();
                                    blockChecksReady++;
                                    updateBlockStateAfterCheck();
                                }
                        );

        reverseBlockListener =
                db.collection("blocks")
                        .whereEqualTo("ownerId", receiverId)
                        .whereEqualTo("blockedUserId", myId)
                        .addSnapshotListener(
                                (snapshot, error) -> {
                                    blockedByReceiver =
                                            error == null &&
                                            snapshot != null &&
                                            !snapshot.isEmpty();
                                    blockChecksReady++;
                                    updateBlockStateAfterCheck();
                                }
                        );
    }

    private void updateBlockStateAfterCheck() {

        if (blockChecksReady < 2) {
            return;
        }

        boolean newBlocked =
                blockedByMe || blockedByReceiver;

        boolean changed = blocked != newBlocked;
        blocked = newBlocked;

        if (blocked) {

            removeMessageListeners();
            messageCache.clear();

            if (messagesContainer != null) {
                messagesContainer.removeAllViews();
            }

            cancelRecordingBecauseBlocked();

            if (messageInput != null) {
                messageInput.setEnabled(false);
                messageInput.setHint(tr("🚫 این کاربر مسدود است"));
            }

            if (sendButton != null) {
                sendButton.setEnabled(false);
            }

            if (mediaButton != null) {
                mediaButton.setEnabled(false);
            }

            if (voiceButton != null) {
                voiceButton.setEnabled(false);
            }

            if (statusText != null) {
                statusText.setText(tr("🚫 مسدود شده"));
            }

            if (changed) {
                Toast.makeText(
                        this,
                        blockedByMe
                                ? tr("این کاربر مسدود شده است")
                                : tr("این کاربر شما را مسدود کرده است"),
                        Toast.LENGTH_SHORT
                ).show();
            }

        } else {

            if (messageInput != null) {
                messageInput.setEnabled(true);
                messageInput.setHint(tr("پیام خود را بنویسید..."));
            }

            if (sendButton != null) {
                sendButton.setEnabled(true);
            }

            if (mediaButton != null) {
                mediaButton.setEnabled(true);
            }

            if (voiceButton != null) {
                voiceButton.setEnabled(true);
            }

            listenMessages();
        }
    }

    private void cancelRecordingBecauseBlocked() {

        if (!recording && recorder == null) {
            return;
        }

        recording = false;

        try {
            if (recorder != null) {
                try {
                    recorder.stop();
                } catch (Exception ignored) {
                }
                try {
                    recorder.reset();
                } catch (Exception ignored) {
                }
                try {
                    recorder.release();
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }

        recorder = null;

        if (audioPath != null) {
            try {
                File f = new File(audioPath);
                if (f.exists()) {
                    f.delete();
                }
            } catch (Exception ignored) {
            }
        }

        audioPath = null;

        if (voiceButton != null) {
            voiceButton.setImageResource(
                    android.R.drawable.ic_btn_speak_now
            );
        }
    }

    private void pickMedia() {

        Intent intent =
                new Intent(
                        Intent.ACTION_OPEN_DOCUMENT
                );

        intent.setType("*/*");

        intent.putExtra(
                Intent.EXTRA_MIME_TYPES,
                new String[]{
                        "image/*",
                        "video/*"
                }
        );

        intent.addCategory(
                Intent.CATEGORY_OPENABLE
        );

        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION |
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        );

        startActivityForResult(
                intent,
                PICK_MEDIA
        );
    }

    private void pickProfilePhoto() {

        Intent intent =
                new Intent(
                        Intent.ACTION_OPEN_DOCUMENT
                );

        intent.setType("image/*");

        intent.addCategory(
                Intent.CATEGORY_OPENABLE
        );

        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION |
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        );

        startActivityForResult(
                intent,
                PICK_PROFILE
        );
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (resultCode != RESULT_OK ||
                data == null ||
                data.getData() == null) {

            return;
        }

        Uri uri =
                data.getData();

        try {

            getContentResolver()
                    .takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );

        } catch (Exception ignored) {
        }

        if (requestCode == PICK_MEDIA) {

            uploadMedia(uri);

        } else if (requestCode == PICK_PROFILE) {

            uploadProfilePhoto(uri);

        } else if (requestCode == REQUEST_SAVE) {

            if (pendingSaveUrl != null) {

                saveImageToGallery(
                        pendingSaveUrl,
                        pendingSaveName
                );
            }
        }
    }

    private void uploadMedia(
            Uri uri
    ) {

        if (blocked) {

            Toast.makeText(
                    this,
                    tr("این کاربر بلاک شده است"),
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String mime =
                getContentResolver()
                        .getType(uri);

        if (mime == null) {

            mime =
                    "application/octet-stream";
        }

        boolean image =
                mime.startsWith("image/");

        boolean video =
                mime.startsWith("video/");

        if (!image && !video) {

            Toast.makeText(
                    this,
                    tr("این نوع فایل پشتیبانی نمی‌شود"),
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String ext;

        if (image) {

            if (mime.contains("png")) {

                ext = "png";

            } else if (mime.contains("webp")) {

                ext = "webp";

            } else {

                ext = "jpg";
            }

        } else {

            if (mime.contains("3gp")) {

                ext = "3gp";

            } else {

                ext = "mp4";
            }
        }

        String objectPath =
                "chat/" +
                        System.currentTimeMillis() +
                        "_" +
                        myId +
                        "." +
                        ext;

        String finalMime =
                mime;

        Toast.makeText(
                this,
                tr("در حال ارسال فایل..."),
                Toast.LENGTH_SHORT
        ).show();

        uploadToSupabase(
                uri,
                CHAT_BUCKET,
                objectPath,
                finalMime,
                new SupabaseUploadCallback() {

                    @Override
                    public void onSuccess(
                            String url
                    ) {

                        saveMediaMessage(
                                image
                                        ? "image"
                                        : "video",
                                url
                        );
                    }

                    @Override
                    public void onError(
                            String error
                    ) {

                        Toast.makeText(
                                ChatActivity.this,
                                tr("خطای ارسال:\n") +
                                        error,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void saveMediaMessage(
            String type,
            String url
    ) {

        if (blocked) {
            Toast.makeText(
                    this,
                    tr("این کاربر بلاک شده است؛ فایل ارسال نشد"),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "chatId",
                currentChatId
        );

        data.put(
                "senderId",
                myId
        );

        data.put(
                "receiverId",
                receiverId
        );

        data.put(
                "type",
                type
        );

        data.put(
                "mediaUrl",
                url
        );

        data.put(
                "timestamp",
                FieldValue.serverTimestamp()
        );

        data.put(
                "read",
                false
        );

        data.put(
                "deletedForAll",
                false
        );

        data.put(
                "deletedFor",
                new ArrayList<>()
        );

        db.collection("messages")
                .add(data)
                .addOnFailureListener(
                        e ->
                                Toast.makeText(
                                        this,
                                        tr("خطا در ذخیره پیام فایل"),
                                        Toast.LENGTH_SHORT
                                ).show()
                );
    }

    private void toggleRecording() {

        if (recording) {

            stopRecording();

        } else {

            startRecording();
        }
    }

    /*
     * فقط بخش ضبط صدا با VoiceActivity هماهنگ شده:
     * 3GP + AMR_NB
     */
    private void startRecording() {

        if (blocked) {

            Toast.makeText(
                    this,
                    tr("این کاربر بلاک شده است"),
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
        ) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.RECORD_AUDIO
                    },
                    REQUEST_MIC
            );

            return;
        }

        try {

            File file =
                    new File(
                            getCacheDir(),
                            "voice_" +
                                    System.currentTimeMillis() +
                                    ".3gp"
                    );

            audioPath =
                    file.getAbsolutePath();

            recorder =
                    new MediaRecorder();

            recorder.setAudioSource(
                    MediaRecorder.AudioSource.MIC
            );

            recorder.setOutputFormat(
                    MediaRecorder.OutputFormat.THREE_GPP
            );

            recorder.setAudioEncoder(
                    MediaRecorder.AudioEncoder.AMR_NB
            );

            recorder.setOutputFile(
                    audioPath
            );

            recorder.prepare();
            recorder.start();

            recording = true;

            voiceButton.setImageResource(
                    android.R.drawable.ic_media_pause
            );

            Toast.makeText(
                    this,
                    tr("🎤 در حال ضبط... دوباره بزنید تا ارسال شود"),
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {

            recording = false;

            Toast.makeText(
                    this,
                    tr("خطا در شروع ضبط: ") +
                            e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();

            releaseRecorder();
        }
    }

    private void stopRecording() {

        if (recorder == null) {
            return;
        }

        try {

            recorder.stop();

        } catch (Exception e) {

            recording = false;

            releaseRecorder();

            Toast.makeText(
                    this,
                    tr("ضبط صدا ناموفق بود"),
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        recording = false;

        releaseRecorder();

        voiceButton.setImageResource(
                android.R.drawable.ic_btn_speak_now
        );

        uploadVoice(audioPath);
    }

    private void releaseRecorder() {

        try {

            if (recorder != null) {

                recorder.reset();
                recorder.release();
            }

        } catch (Exception ignored) {
        }

        recorder = null;
    }

    /*
     * ارسال صدا نیز دقیقاً با VoiceActivity هماهنگ شده:
     * فایل 3GP
     * MIME = audio/3gpp
     * bucket = voice_messages
     * فایل در ریشه bucket قرار می‌گیرد.
     */
    private void uploadVoice(
            String path
    ) {

        if (blocked) {
            Toast.makeText(
                    this,
                    tr("این کاربر بلاک شده است؛ پیام صوتی ارسال نشد"),
                    Toast.LENGTH_SHORT
            ).show();

            try {
                if (path != null) {
                    File f = new File(path);
                    if (f.exists()) f.delete();
                }
            } catch (Exception ignored) {
            }

            return;
        }

        if (path == null) {

            Toast.makeText(
                    this,
                    tr("فایل صوتی پیدا نشد"),
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        File file =
                new File(path);

        if (!file.exists() ||
                file.length() <= 0) {

            Toast.makeText(
                    this,
                    tr("فایل صوتی خالی است"),
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String fileName =
                System.currentTimeMillis() +
                        "_" +
                        myId +
                        ".3gp";

        /*
         * مثل VoiceActivity:
         * فایل مستقیم داخل bucket قرار می‌گیرد.
         */
        String objectPath =
                fileName;

        Toast.makeText(
                this,
                tr("در حال ارسال پیام صوتی..."),
                Toast.LENGTH_SHORT
        ).show();

        uploadToSupabase(
                Uri.fromFile(file),
                VOICE_BUCKET,
                objectPath,
                "audio/3gpp",
                new SupabaseUploadCallback() {

                    @Override
                    public void onSuccess(
                            String url
                    ) {

                        saveAudioMessage(url);

                        try {
                            file.delete();
                        } catch (Exception ignored) {
                        }
                    }

                    @Override
                    public void onError(
                            String error
                    ) {

                        Toast.makeText(
                                ChatActivity.this,
                                tr("خطای ارسال پیام صوتی:\n") +
                                        error,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void saveAudioMessage(
            String url
    ) {

        if (blocked) {
            Toast.makeText(
                    this,
                    tr("این کاربر بلاک شده است؛ پیام صوتی ارسال نشد"),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "chatId",
                currentChatId
        );

        data.put(
                "senderId",
                myId
        );

        data.put(
                "receiverId",
                receiverId
        );

        data.put(
                "type",
                "audio"
        );

        data.put(
                "audioUrl",
                url
        );

        data.put(
                "timestamp",
                FieldValue.serverTimestamp()
        );

        data.put(
                "read",
                false
        );

        data.put(
                "deletedForAll",
                false
        );

        data.put(
                "deletedFor",
                new ArrayList<>()
        );

        db.collection("messages")
                .add(data)
                .addOnFailureListener(
                        e ->
                                Toast.makeText(
                                        this,
                                        tr("خطا در ذخیره پیام صوتی"),
                                        Toast.LENGTH_SHORT
                                ).show()
                );
    }

    private void playAudio(
            String url
    ) {

        try {

            if (player != null) {

                player.release();
                player = null;
            }

            player =
                    new MediaPlayer();

            player.setDataSource(url);

            player.setOnPreparedListener(
                    MediaPlayer::start
            );

            player.setOnCompletionListener(
                    mp -> {

                        mp.release();
                        player = null;
                    }
            );

            player.setOnErrorListener(
                    (mp, what, extra) -> {

                        Toast.makeText(
                                this,
                                tr("پخش صدا ناموفق بود"),
                                Toast.LENGTH_SHORT
                        ).show();

                        return true;
                    }
            );

            player.prepareAsync();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    tr("خطا در پخش صدا"),
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void uploadProfilePhoto(
            Uri uri
    ) {

        String mime =
                getContentResolver()
                        .getType(uri);

        if (mime == null ||
                !mime.startsWith("image/")) {

            mime = "image/jpeg";
        }

        String ext;

        if (mime.contains("png")) {

            ext = "png";

        } else if (mime.contains("webp")) {

            ext = "webp";

        } else {

            ext = "jpg";
        }

        String objectPath =
                "profiles/profile_" +
                        myId +
                        "_" +
                        System.currentTimeMillis() +
                        "." +
                        ext;

        String finalMime =
                mime;

        Toast.makeText(
                this,
                tr("در حال ارسال عکس پروفایل..."),
                Toast.LENGTH_SHORT
        ).show();

        uploadToSupabase(
                uri,
                CHAT_BUCKET,
                objectPath,
                finalMime,
                new SupabaseUploadCallback() {

                    @Override
                    public void onSuccess(
                            String url
                    ) {

                        Map<String, Object> data =
                                new HashMap<>();

                        data.put(
                                "photoUrl",
                                url
                        );

                        db.collection("users")
                                .document(myId)
                                .set(
                                        data,
                                        SetOptions.merge()
                                )
                                .addOnSuccessListener(
                                        x -> {

                                            Toast.makeText(
                                                    ChatActivity.this,
                                                    tr("عکس پروفایل ذخیره شد"),
                                                    Toast.LENGTH_SHORT
                                            ).show();

                                            loadUsers();
                                        }
                                );
                    }

                    @Override
                    public void onError(
                            String error
                    ) {

                        Toast.makeText(
                                ChatActivity.this,
                                tr("خطای عکس پروفایل:\n") +
                                        error,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void uploadToSupabase(
            Uri uri,
            String bucket,
            String objectPath,
            String contentType,
            SupabaseUploadCallback callback
    ) {

        new Thread(
                () -> {

                    HttpURLConnection connection =
                            null;

                    InputStream input =
                            null;

                    OutputStream output =
                            null;

                    try {

                        String endpoint =
                                SUPABASE_URL +
                                        "/storage/v1/object/" +
                                        bucket +
                                        "/" +
                                        encodePath(
                                                objectPath
                                        );

                        URL url =
                                new URL(endpoint);

                        connection =
                                (HttpURLConnection)
                                        url.openConnection();

                        connection.setRequestMethod(
                                "POST"
                        );

                        connection.setDoOutput(true);
                        connection.setDoInput(true);

                        connection.setConnectTimeout(
                                30000
                        );

                        connection.setReadTimeout(
                                60000
                        );

                        /*
                         * فقط apikey.
                         * Authorization Bearer برای
                         * publishable key استفاده نمی‌شود.
                         */
                        connection.setRequestProperty(
                                "apikey",
                                SUPABASE_PUBLISHABLE_KEY
                        );

                        connection.setRequestProperty(
                                "Accept",
                                "application/json"
                        );

                        connection.setRequestProperty(
                                "Content-Type",
                                contentType
                        );

                        long size = -1;

                        if ("file".equals(
                                uri.getScheme()
                        )) {

                            File file =
                                    new File(
                                            uri.getPath()
                                    );

                            if (!file.exists()) {

                                throw new Exception(
                                        tr("فایل وجود ندارد")
                                );
                            }

                            if (file.length() <= 0) {

                                throw new Exception(
                                        tr("فایل خالی است")
                                );
                            }

                            size =
                                    file.length();

                            input =
                                    new FileInputStream(
                                            file
                                    );

                        } else {

                            input =
                                    getContentResolver()
                                            .openInputStream(
                                                    uri
                                            );

                            if (input == null) {

                                throw new Exception(
                                        tr("فایل قابل خواندن نیست")
                                );
                            }

                            size =
                                    getUriSize(uri);
                        }

                        if (size > 0) {

                            connection
                                    .setFixedLengthStreamingMode(
                                            size
                                    );
                        }

                        output =
                                connection
                                        .getOutputStream();

                        byte[] buffer =
                                new byte[8192];

                        int count;

                        while (
                                (count =
                                        input.read(
                                                buffer
                                        )) != -1
                        ) {

                            output.write(
                                    buffer,
                                    0,
                                    count
                            );
                        }

                        output.flush();
                        output.close();
                        output = null;

                        int code =
                                connection
                                        .getResponseCode();

                        if (code >= 200 &&
                                code < 300) {

                            String publicUrl =
                                    getSupabasePublicUrl(
                                            bucket,
                                            objectPath
                                    );

                            runOnUiThread(
                                    () ->
                                            callback.onSuccess(
                                                    publicUrl
                                            )
                            );

                        } else {

                            String body =
                                    readErrorResponse(
                                            connection
                                    );

                            String message =
                                    "HTTP " +
                                            code +
                                            " " +
                                            connection
                                                    .getResponseMessage();

                            if (body != null &&
                                    !body.trim()
                                            .isEmpty()) {

                                message +=
                                        "\n" +
                                                body;
                            }

                            String finalMessage =
                                    message;

                            runOnUiThread(
                                    () ->
                                            callback.onError(
                                                    finalMessage
                                            )
                            );
                        }

                    } catch (Exception e) {

                        String message =
                                e.getMessage();

                        if (message == null ||
                                message.trim()
                                        .isEmpty()) {

                            message =
                                    e.getClass()
                                            .getSimpleName();
                        }

                        String finalMessage =
                                message;

                        runOnUiThread(
                                () ->
                                        callback.onError(
                                                finalMessage
                                        )
                        );

                    } finally {

                        try {

                            if (output != null) {
                                output.close();
                            }

                        } catch (Exception ignored) {
                        }

                        try {

                            if (input != null) {
                                input.close();
                            }

                        } catch (Exception ignored) {
                        }

                        if (connection != null) {
                            connection.disconnect();
                        }
                    }

                }
        ).start();
    }

    private long getUriSize(
            Uri uri
    ) {

        try {

            android.database.Cursor cursor =
                    getContentResolver()
                            .query(
                                    uri,
                                    new String[]{
                                            OpenableColumns.SIZE
                                    },
                                    null,
                                    null,
                                    null
                            );

            if (cursor != null) {

                try {

                    if (cursor.moveToFirst()) {

                        int index =
                                cursor.getColumnIndex(
                                        OpenableColumns.SIZE
                                );

                        if (index >= 0 &&
                                !cursor.isNull(index)) {

                            return cursor.getLong(
                                    index
                            );
                        }
                    }

                } finally {

                    cursor.close();
                }
            }

        } catch (Exception ignored) {
        }

        return -1;
    }

    private String readErrorResponse(
            HttpURLConnection connection
    ) {

        InputStream stream =
                null;

        try {

            stream =
                    connection.getErrorStream();

            if (stream == null) {

                stream =
                        connection.getInputStream();
            }

            if (stream == null) {
                return "";
            }

            java.util.Scanner scanner =
                    new java.util.Scanner(
                            stream,
                            "UTF-8"
                    ).useDelimiter("\\A");

            return scanner.hasNext()
                    ? scanner.next()
                    : "";

        } catch (Exception e) {

            return "";

        } finally {

            try {

                if (stream != null) {
                    stream.close();
                }

            } catch (Exception ignored) {
            }
        }
    }

    private String encodePath(
            String path
    ) throws Exception {

        String[] parts =
                path.split("/");

        StringBuilder result =
                new StringBuilder();

        for (
                int i = 0;
                i < parts.length;
                i++
        ) {

            if (i > 0) {
                result.append("/");
            }

            result.append(
                    URLEncoder.encode(
                            parts[i],
                            "UTF-8"
                    ).replace(
                            "+",
                            "%20"
                    )
            );
        }

        return result.toString();
    }

    private String getSupabasePublicUrl(
            String bucket,
            String path
    ) {

        try {

            return SUPABASE_URL +
                    "/storage/v1/object/public/" +
                    bucket +
                    "/" +
                    encodePath(path);

        } catch (Exception e) {

            return SUPABASE_URL +
                    "/storage/v1/object/public/" +
                    bucket +
                    "/" +
                    path;
        }
    }

    private void loadImage(
            String url,
            ImageView imageView
    ) {

        new Thread(
                () -> {

                    HttpURLConnection connection =
                            null;

                    try {

                        connection =
                                (HttpURLConnection)
                                        new URL(url)
                                                .openConnection();

                        connection.setConnectTimeout(
                                15000
                        );

                        connection.setReadTimeout(
                                30000
                        );

                        connection.setRequestProperty(
                                "apikey",
                                SUPABASE_PUBLISHABLE_KEY
                        );

                        connection.setRequestProperty(
                                "Authorization",
                                "Bearer " +
                                        SUPABASE_PUBLISHABLE_KEY
                        );

                        InputStream input =
                                connection
                                        .getInputStream();

                        Bitmap bitmap =
                                BitmapFactory
                                        .decodeStream(
                                                input
                                        );

                        input.close();

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

                    } finally {

                        if (connection != null) {
                            connection.disconnect();
                        }
                    }

                }
        ).start();
    }

    private void showImageViewer(
            String url
    ) {

        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL
        );

        box.setGravity(
                Gravity.CENTER
        );

        box.setPadding(
                dp(8),
                dp(8),
                dp(8),
                dp(8)
        );

        ImageView image =
                new ImageView(this);

        image.setScaleType(
                ImageView.ScaleType.FIT_CENTER
        );

        box.addView(
                image,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(380)
                )
        );

        Button save =
                new Button(this);

        save.setText(
                tr("💾 ذخیره عکس در گالری")
        );

        box.addView(
                save,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );

        loadImage(
                url,
                image
        );

        save.setOnClickListener(
                v -> {

                    pendingSaveUrl =
                            url;

                    pendingSaveName =
                            "tajrobehha_" +
                                    System.currentTimeMillis() +
                                    ".jpg";

                    saveImageToGallery(
                            pendingSaveUrl,
                            pendingSaveName
                    );
                }
        );

        new AlertDialog.Builder(this)
                .setTitle(tr("تصویر"))
                .setView(box)
                .setPositiveButton(
                        tr("بستن"),
                        null
                )
                .show();
    }

    private void saveImageToGallery(
            String url,
            String fileName
    ) {

        new Thread(
                () -> {

                    try {

                        HttpURLConnection connection =
                                (HttpURLConnection)
                                        new URL(url)
                                                .openConnection();

                        connection.setConnectTimeout(
                                20000
                        );

                        connection.setReadTimeout(
                                30000
                        );

                        connection.setRequestProperty(
                                "apikey",
                                SUPABASE_PUBLISHABLE_KEY
                        );

                        connection.setRequestProperty(
                                "Authorization",
                                "Bearer " +
                                        SUPABASE_PUBLISHABLE_KEY
                        );

                        InputStream input =
                                connection
                                        .getInputStream();

                        if (Build.VERSION.SDK_INT >=
                                Build.VERSION_CODES.Q) {

                            ContentValues values =
                                    new ContentValues();

                            values.put(
                                    MediaStore.Images.Media
                                            .DISPLAY_NAME,
                                    fileName
                            );

                            values.put(
                                    MediaStore.Images.Media
                                            .MIME_TYPE,
                                    "image/jpeg"
                            );

                            values.put(
                                    MediaStore.Images.Media
                                            .RELATIVE_PATH,
                                    Environment
                                            .DIRECTORY_PICTURES +
                                            "/تجربه‌ها"
                            );

                            values.put(
                                    MediaStore.Images.Media
                                            .IS_PENDING,
                                    1
                            );

                            Uri imageUri =
                                    getContentResolver()
                                            .insert(
                                                    MediaStore.Images.Media
                                                            .getContentUri(
                                                                    MediaStore
                                                                            .VOLUME_EXTERNAL_PRIMARY
                                                            ),
                                                    values
                                            );

                            if (imageUri == null) {

                                throw new Exception(
                                        tr("گالری قابل دسترسی نیست")
                                );
                            }

                            OutputStream output =
                                    getContentResolver()
                                            .openOutputStream(
                                                    imageUri
                                            );

                            if (output == null) {

                                throw new Exception(
                                        tr("فضای ذخیره‌سازی باز نشد")
                                );
                            }

                            byte[] buffer =
                                    new byte[8192];

                            int count;

                            while (
                                    (count =
                                            input.read(
                                                    buffer
                                            )) != -1
                            ) {

                                output.write(
                                        buffer,
                                        0,
                                        count
                                );
                            }

                            output.flush();
                            output.close();
                            input.close();

                            ContentValues done =
                                    new ContentValues();

                            done.put(
                                    MediaStore.Images.Media
                                            .IS_PENDING,
                                    0
                            );

                            getContentResolver()
                                    .update(
                                            imageUri,
                                            done,
                                            null,
                                            null
                                    );

                            runOnUiThread(
                                    () ->
                                            Toast.makeText(
                                                    this,
                                                    tr("✅ عکس در گالری ذخیره شد"),
                                                    Toast.LENGTH_SHORT
                                            ).show()
                            );

                        } else {

                            input.close();

                            runOnUiThread(
                                    () ->
                                            Toast.makeText(
                                                    this,
                                                    tr("ذخیره مستقیم در گالری در این نسخه اندروید پشتیبانی نمی‌شود"),
                                                    Toast.LENGTH_LONG
                                            ).show()
                            );
                        }

                        connection.disconnect();

                    } catch (Exception e) {

                        String error =
                                e.getMessage();

                        if (error == null) {

                            error =
                                    tr("خطا در ذخیره عکس");
                        }

                        String finalError =
                                error;

                        runOnUiThread(
                                () ->
                                        Toast.makeText(
                                                this,
                                                tr("خطای ذخیره عکس:\n") +
                                                        finalError,
                                                Toast.LENGTH_LONG
                                        ).show()
                        );
                    }

                }
        ).start();
    }

    private void removeMessageListeners() {

        if (sentMessageListener != null) {

            sentMessageListener.remove();
            sentMessageListener = null;
        }

        if (receivedMessageListener != null) {

            receivedMessageListener.remove();
            receivedMessageListener = null;
        }
    }

    private void removeChatListeners() {

        removeMessageListeners();

        if (receiverListener != null) {

            receiverListener.remove();
            receiverListener = null;
        }

        if (typingListener != null) {

            typingListener.remove();
            typingListener = null;
        }

        if (blockListener != null) {

            blockListener.remove();
            blockListener = null;
        }
 
        if (reverseBlockListener != null) {

            reverseBlockListener.remove();
            reverseBlockListener = null;
        }

        blockedByMe = false;
        blockedByReceiver = false;
        blockChecksReady = 0;
    }

    private void showUsers() {

        removeChatListeners();

        insideChat = false;
        receiverId = null;
        receiverName = null;
        currentChatId = null;

        messageCache.clear();

        createUsersScreen();
        loadUsers();
    }

    @Override
    protected void onResume() {

        super.onResume();

        if (myId != null) {

            db.collection("users")
                    .document(myId)
                    .set(
                            new HashMap<String, Object>() {{
                                put(
                                        "online",
                                        true
                                );

                                put(
                                        "lastSeen",
                                        FieldValue
                                                .serverTimestamp()
                                );
                            }},
                            SetOptions.merge()
                    );
        }
    }

    @Override
    protected void onPause() {

        super.onPause();

        if (myId != null) {

            Map<String, Object> data =
                    new HashMap<>();

            data.put(
                    "online",
                    false
            );

            data.put(
                    "lastSeen",
                    FieldValue
                            .serverTimestamp()
            );

            data.put(
                    "typingTo",
                    ""
            );

            db.collection("users")
                    .document(myId)
                    .set(
                            data,
                            SetOptions.merge()
                    );
        }
    }

    @Override
    protected void onDestroy() {

        removeChatListeners();

        try {

            if (recorder != null) {
                recorder.release();
            }

        } catch (Exception ignored) {
        }

        recorder = null;

        try {

            if (player != null) {
                player.release();
            }

        } catch (Exception ignored) {
        }

        player = null;

        super.onDestroy();
    }

     @Override
public void onBackPressed() {

    if (insideChat) {

        showUsers();

    } else {

        super.onBackPressed();
    }
}

@Override
public void onRequestPermissionsResult(
        int requestCode,
        String[] permissions,
        int[] grantResults
) {

    super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
    );

    if (requestCode == REQUEST_MIC) {

        if (grantResults.length > 0 &&
                grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED) {

            startRecording();

        } else {

            Toast.makeText(
                    this,
                    tr("اجازه میکروفون داده نشد"),
                    Toast.LENGTH_SHORT
            ).show();
        }
    }
}

private void showChatMenu() {

    String[] options = {
            tr("🔕 بی‌صدا کردن اعلان‌های این چت"),
            tr("👤 مشاهده پروفایل"),
            tr("🔒 تنظیمات حریم خصوصی"),
            tr("🗑️ حذف کامل چت"),
            tr("🚫 مسدود کردن")
    };

    new AlertDialog.Builder(this)
            .setTitle(tr("تنظیمات چت"))
            .setItems(
                    options,
                    (dialog, which) -> {

                        if (which == 0) {

                            Toast.makeText(
                                    ChatActivity.this,
                                    tr("اعلان‌های این چت بی‌صدا شد"),
                                    Toast.LENGTH_SHORT
                            ).show();

                        } else if (which == 1) {

                            showProfileDialog(
                                    receiverId,
                                    receiverName,
                                    receiverPhotoUrl
                            );

                        } else if (which == 2) {

                            showChatPrivacySettings();

                        } else if (which == 3) {

                            new AlertDialog.Builder(
                                    ChatActivity.this
                            )
                                    .setTitle(tr("⚠️ حذف کامل چت"))
                                    .setMessage(
                                            tr("آیا مطمئن هستید که می‌خواهید تمام این گفتگو را حذف کنید؟")
                                    )
                                    .setNegativeButton(
                                            tr("لغو"),
                                            null
                                    )
                                    .setPositiveButton(
                                            tr("مرحله اول"),
                                            (d, w) -> {

                                                new AlertDialog.Builder(
                                                        ChatActivity.this
                                                )
                                                        .setTitle(
                                                                tr("تأیید نهایی حذف")
                                                        )
                                                        .setMessage(
                                                                tr("این کار تمام پیام‌های این گفتگو را حذف می‌کند و قابل برگشت نیست. ادامه می‌دهید؟")
                                                        )
                                                        .setNegativeButton(
                                                                tr("لغو"),
                                                                null
                                                        )
                                                        .setPositiveButton(
                                                                tr("حذف کامل"),
                                                                (d2, w2) ->
                                                                        deleteCurrentChatForMe()
                                                        )
                                                        .show();
                                            }
                                    )
                                    .show();

                        } else if (which == 4) {

                            new AlertDialog.Builder(
                                    ChatActivity.this
                            )
                                    .setTitle(tr("مسدود کردن کاربر"))
                                    .setMessage(
                                            tr("آیا می‌خواهید این کاربر را مسدود کنید؟")
                                    )
                                    .setNegativeButton(
                                            tr("لغو"),
                                            null
                                    )
                                    .setPositiveButton(
                                            tr("مسدود کردن"),
                                            (d, w) ->
                                                    blockCurrentUser()
                                    )
                                    .show();
                        }
                    }
            )
            .show();
}

private void showChatPrivacySettings() {

    final String[] items = {
            tr("نمایش آنلاین بودن"),
            tr("نمایش آخرین بازدید"),
            tr("نمایش «در حال نوشتن…»"),
            tr("نمایش رسید خوانده شدن ✓✓")
    };

    final boolean[] checked = {
            true,
            true,
            true,
            true
    };

    new AlertDialog.Builder(this)
            .setTitle(tr("🔒 تنظیمات حریم خصوصی"))
            .setMultiChoiceItems(
                    items,
                    checked,
                    (dialog, which, isChecked) ->
                            checked[which] = isChecked
            )
            .setNegativeButton(
                    tr("لغو"),
                    null
            )
            .setPositiveButton(
                    tr("ذخیره"),
                    (dialog, which) -> {

                        Map<String, Object> privacy =
                                new HashMap<>();

                        privacy.put(
                                "showOnline",
                                checked[0]
                        );

                        privacy.put(
                                "showLastSeen",
                                checked[1]
                        );

                        privacy.put(
                                "showTyping",
                                checked[2]
                        );

                        privacy.put(
                                "readReceipts",
                                checked[3]
                        );

                        db.collection("users")
                                .document(myId)
                                .set(
                                        privacy,
                                        SetOptions.merge()
                                )
                                .addOnSuccessListener(
                                        v ->
                                                Toast.makeText(
                                                        ChatActivity.this,
                                                        tr("تنظیمات ذخیره شد"),
                                                        Toast.LENGTH_SHORT
                                                ).show()
                                )
                                .addOnFailureListener(
                                        e ->
                                                Toast.makeText(
                                                        ChatActivity.this,
                                                        tr("ذخیره تنظیمات ناموفق بود"),
                                                        Toast.LENGTH_SHORT
                                                ).show()
                                );
                    }
            )
            .show();
}

private void reportCurrentUser() {

    Map<String, Object> report =
            new HashMap<>();

    report.put(
            "reporterId",
            myId
    );

    report.put(
            "reportedUserId",
            receiverId
    );

    report.put(
            "chatId",
            currentChatId
    );

    report.put(
            "timestamp",
            FieldValue.serverTimestamp()
    );

    db.collection("reports")
            .add(report)
            .addOnSuccessListener(
                    v ->
                            Toast.makeText(
                                    ChatActivity.this,
                                    tr("گزارش شما ثبت شد"),
                                    Toast.LENGTH_SHORT
                            ).show()
            )
            .addOnFailureListener(
                    e ->
                            Toast.makeText(
                                    ChatActivity.this,
                                    tr("ثبت گزارش ناموفق بود"),
                                    Toast.LENGTH_SHORT
                            ).show()
            );
}

private void deleteCurrentChatForMe() {

    if (currentChatId == null ||
            currentChatId.isEmpty()) {

        return;
    }

    db.collection("chats")
            .document(currentChatId)
            .collection("messages")
            .get()
            .addOnSuccessListener(
                    snapshot -> {

                        com.google.firebase.firestore.WriteBatch batch =
                                db.batch();

                        for (
                                DocumentSnapshot doc :
                                snapshot.getDocuments()
                        ) {

                            batch.update(
                                    doc.getReference(),
                                    "deletedFor",
                                    FieldValue.arrayUnion(
                                            myId
                                    )
                            );
                        }

                        batch.commit()
                                .addOnSuccessListener(
                                        v -> {

                                            messageCache.clear();

                                            if (messagesContainer != null) {

                                                messagesContainer
                                                        .removeAllViews();
                                            }

                                            Toast.makeText(
                                                    ChatActivity.this,
                                                    tr("چت از حساب شما پاک شد"),
                                                    Toast.LENGTH_SHORT
                                            ).show();
                                        }
                                )
                                .addOnFailureListener(
                                        e ->
                                                Toast.makeText(
                                                        ChatActivity.this,
                                                        tr("پاک کردن چت ناموفق بود"),
                                                        Toast.LENGTH_SHORT
                                                ).show()
                                );
                    }
            )
            .addOnFailureListener(
                    e ->
                            Toast.makeText(
                                    ChatActivity.this,
                                    tr("دسترسی به پیام‌های چت ناموفق بود"),
                                    Toast.LENGTH_SHORT
                            ).show()
            );
}

private void blockCurrentUser() {

    if (receiverId == null ||
            receiverId.isEmpty()) {

        Toast.makeText(
                ChatActivity.this,
                tr("کاربر انتخاب نشده است"),
                Toast.LENGTH_SHORT
        ).show();

        return;
    }

    String blockId =
            myId + "_" + receiverId;

    Map<String, Object> data =
            new HashMap<>();

    data.put(
            "ownerId",
            myId
    );

    data.put(
            "blockedUserId",
            receiverId
    );

    data.put(
            "blockedUserName",
            receiverName == null ||
                    receiverName.trim().isEmpty()
                    ? tr("کاربر")
                    : receiverName
    );

    data.put(
            "timestamp",
            FieldValue.serverTimestamp()
    );

    db.collection("blocks")
            .document(blockId)
            .set(data)
            .addOnSuccessListener(
                    v -> {

                        blockedByMe = true;
                        blocked = true;
                        blockChecksReady = 2;

                        updateBlockStateAfterCheck();

                        Toast.makeText(
                                ChatActivity.this,
                                tr("کاربر مسدود شد؛ پیام و صدا متوقف شد"),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
            )
            .addOnFailureListener(
                    e -> {

                        String error =
                                e.getMessage();

                        if (error == null ||
                                error.trim().isEmpty()) {

                            error =
                                    tr("خطای نامشخص");
                        }

                        Toast.makeText(
                                ChatActivity.this,
                                tr("مسدود کردن ناموفق بود:\n") +
                                        error,
                                Toast.LENGTH_LONG
                        ).show();
                    }
            );

}
    
    }
