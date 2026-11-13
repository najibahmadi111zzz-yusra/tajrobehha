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
import android.media.MediaMetadataRetriever;
import android.media.MediaRecorder;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.VideoView;
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
import com.google.android.gms.tasks.Tasks;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private static final String PROFILE_BUCKET = "profile_photos_public";

    // تعداد محدود کارگرهای رسانه برای جلوگیری از فشار Threadهای هم‌زمان روی گوشی‌های ضعیف
    private static final ExecutorService MEDIA_EXECUTOR = Executors.newFixedThreadPool(3);

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private LinearLayout root;
    private LinearLayout usersRoot;
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
    private ImageButton myHeaderProfileButton;
    private ImageView myProfileDialogImage;

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
    private ListenerRegistration unreadMessagesListener;
    private final Map<String, Integer> unreadCounts = new HashMap<>();
    private String pendingNotificationMessageId;

    private boolean blockedByMe = false;
    private boolean blockedByReceiver = false;
    private int blockChecksReady = 0;

    private MediaRecorder recorder;
    private MediaPlayer player;
    private String audioPath;
    private TextView recordingStatusText;
    private long recordingStartedAt = 0L;
    private final android.os.Handler voiceUiHandler = new android.os.Handler();
    private final android.os.Handler audioPlaybackHandler = new android.os.Handler();
    private TextView activePlaybackStatus;
    private boolean scrollToBottomOnNextRender = false;
    private final Runnable recordingTicker = new Runnable() {
        @Override public void run() {
            if (!recording || recordingStatusText == null) return;
            long elapsed = Math.max(0L, System.currentTimeMillis() - recordingStartedAt);
            recordingStatusText.setText(formatAudioTime(elapsed) );
            voiceUiHandler.postDelayed(this, 250L);
        }
    };
    private final Runnable playbackTicker = new Runnable() {
        @Override public void run() {
            if (player == null || activePlaybackStatus == null) return;
            try {
                int position = player.getCurrentPosition();
                int duration = player.getDuration();
                activePlaybackStatus.setText(formatAudioTime(position) + " / " + formatAudioTime(duration));
                if (player.isPlaying()) audioPlaybackHandler.postDelayed(this, 250L);
            } catch (Exception ignored) {
            }
        }
    };

    private boolean incomingInitialSnapshotReady = false;
    private final Set<String> soundedIncomingMessageIds = new HashSet<>();

    private final android.os.Handler typingHandler =
            new android.os.Handler();

    private final Map<String, DocumentSnapshot> messageCache =
            new HashMap<>();
    // هر پیام یک ظرف مستقل دارد؛ پیام‌های دیگر با تغییر یک پیام بازسازی نمی‌شوند.
    private final Map<String, LinearLayout> messageViews = new HashMap<>();
    private final Set<String> dirtyMessageIds = new HashSet<>();
    private LinearLayout currentMessageContainer;

    private final Set<String> hiddenUserIds =
            new HashSet<>();

    // حذف از فهرست فقط روی همین دستگاه ذخیره می‌شود؛ پیام یا حساب حذف نمی‌شود.
    private final Set<String> removedChatUserIds =
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
                Intent accountIntent =
                        new Intent(this, AccountActivity.class);
                accountIntent.putExtra("open_after_login", "chat");
                startActivity(accountIntent);
            } catch (Exception ignored) {
            }

            finish();
            return;
        }

        myId = user.getUid();

        android.content.SharedPreferences chatPrefs =
                getSharedPreferences("tajro_chat_list_" + myId, MODE_PRIVATE);
        removedChatUserIds.clear();
        removedChatUserIds.addAll(chatPrefs.getStringSet(
                "removed_users", new HashSet<String>()));

        ensureUserProfile();
        createUsersScreen();
        listenUnreadMessages();
        loadUsers();
        openNotificationMessage(getIntent());
    }



    private String tr(String fa) {
        String lang = LanguageManager.getLanguage(this);

        if ("ar".equals(lang)) {
            switch (fa) {
                case "لطفاً اول وارد حساب شوید": return "يرجى تسجيل الدخول أولاً";
                case "💬 کاربران": return "💬 المستخدمون";
                case "یک کاربر را انتخاب کنید تا چت خصوصی باز شود.": return "اختر مستخدماً لفتح محادثة خاصة.";
                case "هنوز کاربر دیگری پیدا نشد.": return "لم يتم العثور على مستخدم آخر بعد.";
                case "خطا در دریافت کاربران": return "حدث خطأ أثناء تحميل المستخدمين";
                case "کاربر": return "مستخدم";
                case " 🚫 مسدود": return " 🚫 محظور";
                case " آنلاین": return " متصل";
                case " آفلاین": return " غير متصل";
                case "👁 مخفی کردن / رفع مخفی": return "👁 إخفاء / إلغاء الإخفاء";
                case "🚫 بلاک / رفع مسدودیت": return "🚫 حظر / إلغاء الحظر";
                case "مخفی بودن کاربر رفع شد": return "تم إلغاء إخفاء المستخدم";
                case "کاربر مخفی شد": return "تم إخفاء المستخدم";
                case "مسدودیت کاربر رفع شد": return "تم إلغاء حظر المستخدم";
                case "کاربر بلاک شد؛ ارسال پیام و صدا متوقف شد": return "تم حظر المستخدم؛ تم إيقاف الرسائل والصوت";
                case "📷 تغییر عکس پروفایل": return "📷 تغيير صورة الملف الشخصي";
                case "🔒 تنظیمات حریم خصوصی": return "🔒 إعدادات الخصوصية";
                case "🚫 فهرست مسدودشده‌ها": return "🚫 قائمة المستخدمين المحظورين";
                case "پروفایل": return "الملف الشخصي";
                case "بستن": return "إغلاق";
                case "هیچ کاربری مسدود نشده است.": return "لا يوجد مستخدمون محظورون.";
                case "   •   رفع مسدودی": return "   •   إلغاء الحظر";
                case "رفع مسدودی": return "إلغاء الحظر";
                case "آیا می‌خواهید «": return "هل تريد إلغاء حظر «";
                case "» را از مسدودی خارج کنید؟": return "»؟";
                case "لغو": return "إلغاء";
                case "مسدودی برداشته شد": return "تم إلغاء الحظر";
                case "دریافت فهرست مسدودشده‌ها انجام نشد": return "تعذر تحميل قائمة المستخدمين المحظورين";
                case "در حال بررسی...": return "جارٍ التحقق...";
                case "پیام خود را بنویسید...": return "اكتب رسالتك...";
                case "پیام حذف شد": return "تم حذف الرسالة";
                case "▶️ پخش پیام صوتی": return "▶️ تشغيل الرسالة الصوتية";
                case "حذف برای من": return "حذف لديّ";
                case "حذف برای همه": return "حذف لدى الجميع";
                case "حذف پیام ناموفق بود": return "فشل حذف الرسالة";
                case "این کاربر بلاک شده است": return "هذا المستخدم محظور";
                case "خطا در ارسال پیام": return "حدث خطأ أثناء إرسال الرسالة";
                case "آنلاین": return "متصل";
                case "آفلاین": return "غير متصل";
                case "در حال نوشتن...": return "يكتب الآن...";
                case "🚫 این کاربر مسدود است": return "🚫 هذا المستخدم محظور";
                case "🚫 مسدود شده": return "🚫 محظور";
                case "این کاربر مسدود شده است": return "تم حظر هذا المستخدم";
                case "این کاربر شما را مسدود کرده است": return "قام هذا المستخدم بحظرك";
                case "این نوع فایل پشتیبانی نمی‌شود": return "نوع الملف هذا غير مدعوم";
                case "در حال ارسال فایل...": return "جارٍ إرسال الملف...";
                case "خطای ارسال:\n": return "خطأ في الإرسال:\n";
                case "این کاربر بلاک شده است؛ فایل ارسال نشد": return "هذا المستخدم محظور؛ لم يتم إرسال الملف";
                case "خطا در ذخیره پیام فایل": return "حدث خطأ أثناء حفظ رسالة الملف";
                case "🎤 در حال ضبط... دوباره بزنید تا ارسال شود": return "🎤 جارٍ التسجيل... اضغط مرة أخرى للإرسال";
                case "خطا در شروع ضبط: ": return "خطأ في بدء التسجيل: ";
                case "ضبط صدا ناموفق بود": return "فشل تسجيل الصوت";
                case "این کاربر بلاک شده است؛ پیام صوتی ارسال نشد": return "هذا المستخدم محظور؛ لم يتم إرسال الرسالة الصوتية";
                case "فایل صوتی پیدا نشد": return "لم يتم العثور على الملف الصوتي";
                case "فایل صوتی خالی است": return "الملف الصوتي فارغ";
                case "در حال ارسال پیام صوتی...": return "جارٍ إرسال الرسالة الصوتية...";
                case "خطای ارسال پیام صوتی:\n": return "خطأ في إرسال الرسالة الصوتية:\n";
                case "خطا در ذخیره پیام صوتی": return "حدث خطأ أثناء حفظ الرسالة الصوتية";
                case "پخش صدا ناموفق بود": return "فشل تشغيل الصوت";
                case "خطا در پخش صدا": return "حدث خطأ أثناء تشغيل الصوت";
                case "در حال ارسال عکس پروفایل...": return "جارٍ رفع صورة الملف الشخصي...";
                case "عکس پروفایل ذخیره شد": return "تم حفظ صورة الملف الشخصي";
                case "خطای عکس پروفایل:\n": return "خطأ في صورة الملف الشخصي:\n";
                case "فایل وجود ندارد": return "الملف غير موجود";
                case "فایل خالی است": return "الملف فارغ";
                case "فایل قابل خواندن نیست": return "تعذرت قراءة الملف";
                case "💾 ذخیره عکس در گالری": return "💾 حفظ الصورة في المعرض";
                case "تصویر": return "صورة";
                case "گالری قابل دسترسی نیست": return "المعرض غير متاح";
                case "فضای ذخیره‌سازی باز نشد": return "تعذر فتح مساحة التخزين";
                case "✅ عکس در گالری ذخیره شد": return "✅ تم حفظ الصورة في المعرض";
                case "ذخیره مستقیم در گالری در این نسخه اندروید پشتیبانی نمی‌شود": return "لا يدعم إصدار أندرويد هذا الحفظ المباشر في المعرض";
                case "خطا در ذخیره عکس": return "حدث خطأ أثناء حفظ الصورة";
                case "خطای ذخیره عکس:\n": return "خطأ في حفظ الصورة:\n";
                case "اجازه میکروفون داده نشد": return "لم يتم منح إذن استخدام الميكروفون";
                case "🔕 بی‌صدا کردن اعلان‌های این چت": return "🔕 كتم إشعارات هذه المحادثة";
                case "👤 مشاهده پروفایل": return "👤 عرض الملف الشخصي";
                case "🗑️ حذف کامل چت": return "🗑️ حذف المحادثة بالكامل";
                case "🚫 مسدود کردن": return "🚫 حظر";
                case "تنظیمات چت": return "إعدادات المحادثة";
                case "اعلان‌های این چت بی‌صدا شد": return "تم كتم إشعارات هذه المحادثة";
                case "⚠️ حذف کامل چت": return "⚠️ حذف المحادثة بالكامل";
                case "آیا مطمئن هستید که می‌خواهید تمام این گفتگو را حذف کنید؟": return "هل أنت متأكد من أنك تريد حذف هذه المحادثة بالكامل؟";
                case "مرحله اول": return "الخطوة الأولى";
                case "تأیید نهایی حذف": return "التأكيد النهائي للحذف";
                case "این کار تمام پیام‌های این گفتگو را حذف می‌کند و قابل برگشت نیست. ادامه می‌دهید؟": return "سيؤدي هذا إلى حذف جميع رسائل المحادثة نهائياً. هل تريد المتابعة؟";
                case "حذف کامل": return "حذف الكل";
                case "مسدود کردن کاربر": return "حظر المستخدم";
                case "آیا می‌خواهید این کاربر را مسدود کنید؟": return "هل تريد حظر هذا المستخدم؟";
                case "مسدود کردن": return "حظر";
                case "نمایش آنلاین بودن": return "إظهار حالة الاتصال";
                case "نمایش آخرین بازدید": return "إظهار آخر ظهور";
                case "نمایش «در حال نوشتن…»": return "إظهار «يكتب الآن…»";
                case "نمایش رسید خوانده شدن ✓✓": return "إظهار إشعارات القراءة ✓✓";
                case "ذخیره": return "حفظ";
                case "تنظیمات ذخیره شد": return "تم حفظ الإعدادات";
                case "ذخیره تنظیمات ناموفق بود": return "فشل حفظ الإعدادات";
                case "گزارش شما ثبت شد": return "تم إرسال بلاغك";
                case "ثبت گزارش ناموفق بود": return "فشل إرسال البلاغ";
                case "چت از حساب شما پاک شد": return "تم حذف المحادثة من حسابك";
                case "پاک کردن چت ناموفق بود": return "فشل حذف المحادثة";
                case "دسترسی به پیام‌های چت ناموفق بود": return "تعذر الوصول إلى رسائل المحادثة";
                case "کاربر انتخاب نشده است": return "لم يتم اختيار مستخدم";
                case "کاربر مسدود شد؛ پیام و صدا متوقف شد": return "تم حظر المستخدم؛ تم إيقاف الرسائل والصوت";
                case "خطای نامشخص": return "خطأ غير معروف";
                case "مسدود کردن ناموفق بود:\n": return "فشل الحظر:\n";
                case "🚫 فهرست مسدودشدهها": return "🚫 قائمة المستخدمين المحظورين";
                case "آیا میخواهید «": return "هل تريد إلغاء حظر «";
                case "دریافت فهرست مسدودشدهها انجام نشد": return "تعذر تحميل قائمة المستخدمين المحظورين";
                case "این نوع فایل پشتیبانی نمیشود": return "نوع الملف هذا غير مدعوم";
                case "فضای ذخیرهسازی باز نشد": return "تعذر فتح مساحة التخزين";
                case "ذخیره مستقیم در گالری در این نسخه اندروید پشتیبانی نمیشود": return "لا يدعم إصدار أندرويد هذا الحفظ المباشر في المعرض";
                case "🔕 بیصدا کردن اعلانهای این چت": return "🔕 كتم إشعارات هذه المحادثة";
                case "اعلانهای این چت بیصدا شد": return "تم كتم إشعارات هذه المحادثة";
                case "آیا مطمئن هستید که میخواهید تمام این گفتگو را حذف کنید؟": return "هل أنت متأكد من أنك تريد حذف هذه المحادثة بالكامل؟";
                case "این کار تمام پیامهای این گفتگو را حذف میکند و قابل برگشت نیست. ادامه میدهید؟": return "سيؤدي هذا إلى حذف جميع رسائل المحادثة نهائياً. هل تريد المتابعة؟";
                case "آیا میخواهید این کاربر را مسدود کنید؟": return "هل تريد حظر هذا المستخدم؟";
                case "دسترسی به پیامهای چت ناموفق بود": return "تعذر الوصول إلى رسائل المحادثة";
            }
        }

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
        // عکس باید تمام قاب دایره‌ای را با برش متناسب پُر کند.
        image.setPadding(0, 0, 0, 0);
        image.setAdjustViewBounds(false);

        image.setImageResource(
                android.R.drawable.ic_menu_gallery
        );

        GradientDrawable circleBackground =
                new GradientDrawable();
        circleBackground.setColor(Color.LTGRAY);
        circleBackground.setShape(GradientDrawable.OVAL);
        image.setBackground(circleBackground);
        image.setClipToOutline(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            image.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
        }

        return image;
    }

    // خواندن عکس پروفایل از نام فیلدهای جدید و قدیمی
    private String getProfilePhotoUrl(DocumentSnapshot document) {
        if (document == null) {
            return null;
        }

        String[] fields = {
                "photoUrl",
                "profilePhotoUrl",
                "profilePhoto",
                "photo"
        };

        for (String field : fields) {
            String value = document.getString(field);
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }

        return null;
    }

    // تازه‌سازی عکس پروفایل خود کاربر پس از آپلود
    private void refreshMyProfileAvatar() {
        if (myId == null || myHeaderProfileButton == null) {
            return;
        }

        db.collection("users")
                .document(myId)
                .get()
                .addOnSuccessListener(document -> {
                    String photo = getProfilePhotoUrl(document);
                    if (photo != null && !photo.isEmpty()) {
                        loadImage(photo, myHeaderProfileButton);
                    } else {
                        myHeaderProfileButton.setImageResource(
                                android.R.drawable.ic_menu_myplaces
                        );
                    }
                })
                .addOnFailureListener(error ->
                        Log.e(
                                "PROFILE_IMAGE",
                                "Could not refresh own profile photo",
                                error
                        )
                );
    }

    private void ensureUserProfile() {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) return;

        Map<String, Object> data =
                new HashMap<>();

        data.put("userId", myId);

        // Email and phone must not be stored in the publicly readable user profile.
        // Delete legacy values from this user's profile when it is refreshed.
        data.put("email", FieldValue.delete());
        data.put("phoneNumber", FieldValue.delete());

        data.put("online", true);

        data.put(
                "lastSeen",
                FieldValue.serverTimestamp()
        );

        data.put("typingTo", "");

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
        usersRoot = root;

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        // تم تیره حرفه‌ای برای فهرست گفتگوها
        root.setBackgroundColor(Color.rgb(10, 15, 24));

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

        header.setBackgroundColor(Color.rgb(18, 29, 45));

        myHeaderProfileButton =
                new ImageButton(this);

        ImageButton profile = myHeaderProfileButton;

        GradientDrawable profileCircleBackground =
                new GradientDrawable();
        profileCircleBackground.setColor(Color.LTGRAY);
        profileCircleBackground.setShape(GradientDrawable.OVAL);
        profile.setBackground(profileCircleBackground);
        profile.setClipToOutline(true);

        profile.setScaleType(
                ImageView.ScaleType.CENTER_CROP
        );
        // دکمه ImageButton در بعضی تم‌ها padding داخلی دارد؛ آن را حذف می‌کنیم
        // تا عکس خود کاربر تمام دایره ۴۸dp را بدون قاب خالی بپوشاند.
        profile.setPadding(0, 0, 0, 0);
        profile.setCropToPadding(true);

        profile.setImageResource(
                android.R.drawable.ic_menu_myplaces
        );

        profile.setOnClickListener(
                v -> showMyProfile()
        );

        db.collection("users")
                .document(myId)
                .get()
                .addOnSuccessListener(
                        d -> {

                            String myPhoto =
                                    getProfilePhotoUrl(d);

                            if (myPhoto != null &&
                                    !myPhoto.trim().isEmpty()) {

                                loadImage(
                                        myPhoto,
                                        profile
                                );
                            }
                        }
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

        ImageButton searchUsersButton =
                new ImageButton(this);

        searchUsersButton.setImageResource(
                android.R.drawable.ic_menu_search
        );

        searchUsersButton.setColorFilter(
                Color.WHITE
        );

        searchUsersButton.setBackgroundColor(
                Color.TRANSPARENT
        );

        searchUsersButton.setContentDescription(
                "جستجوی کاربران"
        );

        searchUsersButton.setOnClickListener(
                v -> showUserSearchDialog()
        );

        header.addView(
                searchUsersButton,
                new LinearLayout.LayoutParams(
                        dp(48),
                        dp(48)
                )
        );

titleText =
        text("💬", 24);

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

        ScrollView scroll =
                new ScrollView(this);

        usersContainer =
                new LinearLayout(this);

        usersContainer.setOrientation(
                LinearLayout.VERTICAL
        );

        usersContainer.setPadding(
                dp(10), dp(8), dp(10), dp(16)
        );
        usersContainer.setBackgroundColor(Color.rgb(10, 15, 24));

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

    private void showUserSearchDialog() {

        final LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL
        );

        box.setPadding(
                dp(12),
                dp(4),
                dp(12),
                dp(4)
        );

        final EditText searchInput =
                new EditText(this);

        searchInput.setSingleLine(true);
        searchInput.setHint(
                "نام نمایشی، ایمیل یا شماره تلفن"
        );

        box.addView(
                searchInput,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(52)
                )
        );

        final ScrollView resultsScroll =
                new ScrollView(this);

        final LinearLayout results =
                new LinearLayout(this);

        results.setOrientation(
                LinearLayout.VERTICAL
        );

        resultsScroll.addView(results);

        box.addView(
                resultsScroll,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                )
        );

        final AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle("🔎 جستجوی کاربران")
                        .setView(box)
                        .setNegativeButton("بستن", null)
                        .create();

        db.collection("users")
                .get()
                .addOnSuccessListener(
                        snapshot -> {

                            final List<DocumentSnapshot> allUsers =
                                    new ArrayList<>();

                            for (DocumentSnapshot d : snapshot) {
                                if (!myId.equals(d.getId())) {
                                    allUsers.add(d);
                                }
                            }

                            TextWatcher watcher =
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

                                            String query =
                                                    s.toString()
                                                            .trim()
                                                            .toLowerCase();

                                            results.removeAllViews();

                                            if (query.isEmpty()) {

                                                TextView hint =
                                                        text(
                                                                "نام کاربر را جستجو کنید",
                                                                15
                                                        );

                                                hint.setPadding(
                                                        dp(8),
                                                        dp(14),
                                                        dp(8),
                                                        dp(14)
                                                );

                                                results.addView(hint);
                                                return;
                                            }

                                            List<DocumentSnapshot> matches =
                                                    new ArrayList<>();

                                            for (DocumentSnapshot d : allUsers) {

                                                String name =
                                                        d.getString("name");
                                                if (name == null) name = "";
                                                String n = name.trim().toLowerCase();

                                                // Search is strictly by display name; never by email or phone.
                                                if (!n.isEmpty() && n.contains(query)) {
                                                    matches.add(d);
                                                }
                                            }

                                            Collections.sort(
                                                    matches,
                                                    (a, b) -> {
                                                        Boolean ao = a.getBoolean("online");
                                                        Boolean bo = b.getBoolean("online");
                                                        boolean aon = Boolean.TRUE.equals(ao);
                                                        boolean bon = Boolean.TRUE.equals(bo);
                                                        if (aon != bon) {
                                                            return aon ? -1 : 1;
                                                        }
                                                        return 0;
                                                    }
                                            );

                                            if (matches.isEmpty()) {

                                                TextView empty =
                                                        text(
                                                                "کاربری با این مشخصات پیدا نشد",
                                                                15
                                                        );

                                                empty.setPadding(
                                                        dp(8),
                                                        dp(14),
                                                        dp(8),
                                                        dp(14)
                                                );

                                                results.addView(empty);
                                                return;
                                            }

                                            for (DocumentSnapshot d : matches) {

                                                String uid = d.getId();
                                                String name = d.getString("name");
                                                String photo = getProfilePhotoUrl(d);
                                                boolean online = Boolean.TRUE.equals(
                                                        d.getBoolean("online")
                                                );

                                                if (name == null || name.trim().isEmpty()) {
                                                    name = tr("کاربر");
                                                }

                                                final String finalName = name;
                                                final String finalPhoto = photo;
                                                final String finalUid = uid;

                                                LinearLayout row =
                                              new LinearLayout(ChatActivity.this);

                                                row.setGravity(
                                                        Gravity.CENTER_VERTICAL
                                                );

                                                row.setPadding(
                                                        dp(8),
                                                        dp(8),
                                                        dp(8),
                                                        dp(8)
                                                );

                                                ImageView avatar =
                                                        avatarView(52);

                                                if (photo != null &&
                                                        !photo.trim().isEmpty()) {
                                                    loadImage(photo, avatar);
                                                }

                                                row.addView(avatar);

                                                LinearLayout info =
                                                   new LinearLayout(ChatActivity.this);

                                                info.setOrientation(
                                                        LinearLayout.VERTICAL
                                                );

                                                TextView nameView =
                                                        text(finalName, 16);
                                                nameView.setTypeface(
                                                        null,
                                                        Typeface.BOLD
                                                );

                                                TextView statusView =
                                                        text(
                                                                online ? "● فعال" : "○ غیرفعال",
                                                                12
                                                        );

                                                info.addView(nameView);
                                                info.addView(statusView);

                                                row.addView(
                                                        info,
                                                        new LinearLayout.LayoutParams(
                                                                0,
                                                                -2,
                                                                1
                                                        )
                                                );

                                                View.OnClickListener open =
                                                        v -> {
                                                            dialog.dismiss();
                                                            openPrivateChat(
                                                                    finalUid,
                                                                    finalName,
                                                                    finalPhoto
                                                            );
                                                        };

                                                row.setOnClickListener(open);
                                                avatar.setOnClickListener(open);
                                                nameView.setOnClickListener(open);
                                                info.setOnClickListener(open);

                                                results.addView(row);
                                            }
                                        }

                                        @Override
                                        public void afterTextChanged(
                                                Editable s
                                        ) {
                                        }
                                    };

                            searchInput.addTextChangedListener(watcher);
                        }
                )
                .addOnFailureListener(
                        e -> Toast.makeText(
                                this,
                                "جستجوی کاربران ناموفق بود",
                                Toast.LENGTH_SHORT
                        ).show()
                );

        dialog.show();
    }

    private void listenUnreadMessages() {
        if (unreadMessagesListener != null) {
            unreadMessagesListener.remove();
            unreadMessagesListener = null;
        }
        unreadMessagesListener = db.collection("messages")
                .whereEqualTo("receiverId", myId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) {
                        Log.w("ChatUnread", "Unread message listener failed", error);
                        return;
                    }
                    Map<String, Integer> counts = new HashMap<>();
                    for (DocumentSnapshot d : snapshot.getDocuments()) {
                        if (Boolean.TRUE.equals(d.getBoolean("read"))
                                || Boolean.TRUE.equals(d.getBoolean("deletedForAll"))) {
                            continue;
                        }
                        List<String> deletedFor = (List<String>) d.get("deletedFor");
                        if (deletedFor != null && deletedFor.contains(myId)) continue;
                        String sender = d.getString("senderId");
                        if (sender != null && !sender.equals(myId)) {
                            Integer old = counts.get(sender);
                            counts.put(sender, old == null ? 1 : old + 1);
                        }
                    }
                    unreadCounts.clear();
                    unreadCounts.putAll(counts);
                    if (!insideChat && usersContainer != null) loadUsers();
                });
    }

    private void openNotificationMessage(Intent intent) {
        if (intent == null) return;
        String messageId = intent.getStringExtra("messageId");
        if (messageId == null || messageId.trim().isEmpty()) return;
        intent.removeExtra("messageId");
        db.collection("messages").document(messageId).get()
                .addOnSuccessListener(message -> {
                    if (!message.exists()) return;
                    String senderId = message.getString("senderId");
                    String receiver = message.getString("receiverId");
                    String chatId = message.getString("chatId");
                    if (senderId == null || receiver == null || chatId == null
                            || !myId.equals(receiver) || senderId.equals(myId)) return;
                    db.collection("users").document(senderId).get()
                            .addOnSuccessListener(profile -> {
                                String name = profile.getString("name");
                                if (name == null || name.trim().isEmpty()) name = profile.getString("displayName");
                                if (name == null || name.trim().isEmpty()) name = profile.getString("username");
                                if (name == null || name.trim().isEmpty()) name = tr("کاربر");
                                String photo = getProfilePhotoUrl(profile);
                                pendingNotificationMessageId = messageId;
                                openPrivateChat(senderId, name, photo);
                            })
                            .addOnFailureListener(e -> Log.e("ChatNotification", "Could not load sender profile", e));
                })
                .addOnFailureListener(e -> Log.e("ChatNotification", "Could not load notified message", e));
    }

    private void loadUsers() {

        if (usersContainer == null) return;

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

        // Firestore rules only allow reading block documents owned by this user.
        // An unfiltered collection read is denied by the current security rules.
        db.collection("blocks")
                .whereEqualTo("ownerId", myId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot d : snapshot.getDocuments()) {
                        String blockedId = d.getString("blockedUserId");
                        if (blockedId != null && !blockedId.trim().isEmpty()) {
                            blockedUserIds.add(blockedId);
                        }
                    }
                    loadVisibleUsers();
                })
                .addOnFailureListener(e -> {
                    Log.w("ChatActivity", "Could not load my blocked users", e);
                    loadVisibleUsers();
                });
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
                                        hiddenUserIds.contains(d.getId()) ||
                                        removedChatUserIds.contains(id)) {
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

    // نمایش تاریخ آخرین بازدید بر اساس زمان ذخیره‌شده در سرور Firebase.
    private String formatLastSeen(com.google.firebase.Timestamp timestamp) {
        if (timestamp == null) return tr("آفلاین");
        java.util.Date date = timestamp.toDate();
        java.util.Calendar now = java.util.Calendar.getInstance();
        java.util.Calendar seen = java.util.Calendar.getInstance();
        seen.setTime(date);
        java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat(
                "HH:mm", java.util.Locale.getDefault());
        String time = timeFormat.format(date);
        if (now.get(java.util.Calendar.YEAR) == seen.get(java.util.Calendar.YEAR)
                && now.get(java.util.Calendar.DAY_OF_YEAR) == seen.get(java.util.Calendar.DAY_OF_YEAR)) {
            return "آخرین بازدید: امروز، " + time;
        }
        java.util.Calendar yesterday = (java.util.Calendar) now.clone();
        yesterday.add(java.util.Calendar.DAY_OF_YEAR, -1);
        if (yesterday.get(java.util.Calendar.YEAR) == seen.get(java.util.Calendar.YEAR)
                && yesterday.get(java.util.Calendar.DAY_OF_YEAR) == seen.get(java.util.Calendar.DAY_OF_YEAR)) {
            return "آخرین بازدید: دیروز، " + time;
        }
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat(
                "yyyy/MM/dd", java.util.Locale.getDefault());
        return "آخرین بازدید: " + dateFormat.format(date) + "، " + time;
    }

    private boolean isSameMessageDay(com.google.firebase.Timestamp first,
                                     com.google.firebase.Timestamp second) {
        if (first == null || second == null) return false;
        java.util.Calendar a = java.util.Calendar.getInstance();
        java.util.Calendar b = java.util.Calendar.getInstance();
        a.setTime(first.toDate());
        b.setTime(second.toDate());
        return a.get(java.util.Calendar.YEAR) == b.get(java.util.Calendar.YEAR)
                && a.get(java.util.Calendar.DAY_OF_YEAR) == b.get(java.util.Calendar.DAY_OF_YEAR);
    }

    private String formatMessageDay(com.google.firebase.Timestamp timestamp) {
        if (timestamp == null) return "";
        java.util.Date date = timestamp.toDate();
        java.util.Calendar now = java.util.Calendar.getInstance();
        java.util.Calendar messageDay = java.util.Calendar.getInstance();
        messageDay.setTime(date);
        if (now.get(java.util.Calendar.YEAR) == messageDay.get(java.util.Calendar.YEAR)
                && now.get(java.util.Calendar.DAY_OF_YEAR) == messageDay.get(java.util.Calendar.DAY_OF_YEAR)) {
            return "امروز";
        }
        java.util.Calendar yesterday = (java.util.Calendar) now.clone();
        yesterday.add(java.util.Calendar.DAY_OF_YEAR, -1);
        if (yesterday.get(java.util.Calendar.YEAR) == messageDay.get(java.util.Calendar.YEAR)
                && yesterday.get(java.util.Calendar.DAY_OF_YEAR) == messageDay.get(java.util.Calendar.DAY_OF_YEAR)) {
            return "دیروز";
        }
        return new java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.getDefault()).format(date);
    }

    private String formatMessageTime(com.google.firebase.Timestamp timestamp) {
        if (timestamp == null) return "";
        return new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                .format(timestamp.toDate());
    }

    private void addMessageDateLabel(LinearLayout item, String label) {
        TextView dateLabel = text(label, 12);
        dateLabel.setTextColor(Color.WHITE);
        dateLabel.setGravity(Gravity.CENTER);
        dateLabel.setBackground(bg(Color.rgb(75, 90, 110), 20));
        dateLabel.setPadding(dp(12), dp(4), dp(12), dp(4));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER;
        params.setMargins(0, dp(10), 0, dp(7));
        item.addView(dateLabel, params);
    }

    private void addMessageTimeLabel(LinearLayout item, com.google.firebase.Timestamp timestamp) {
        String time = formatMessageTime(timestamp);
        if (time.isEmpty()) return;
        TextView timeLabel = text(time, 11);
        timeLabel.setTextColor(Color.GRAY);
        timeLabel.setGravity(Gravity.CENTER);
        timeLabel.setPadding(dp(5), 0, dp(5), dp(2));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = myId.equals(item.getTag()) ? Gravity.END : Gravity.START;
        params.setMargins(dp(8), 0, dp(8), dp(5));
        item.addView(timeLabel, params);
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

            name = tr("کاربر");
        }

        String photoUrl =
                getProfilePhotoUrl(d);

        Boolean online =
                d.getBoolean("online");

        // آخرین heartbeat باید تازه باشد؛ در غیر این صورت کاربر آفلاین است.
        com.google.firebase.Timestamp lastSeen =
                d.getTimestamp("lastSeen");
        long presenceAgeMs = lastSeen == null
                ? Long.MAX_VALUE
                : System.currentTimeMillis() - lastSeen.toDate().getTime();
        boolean presenceFresh = presenceAgeMs >= 0
                && presenceAgeMs <= 150_000L;
        online = Boolean.TRUE.equals(online) && presenceFresh;

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

        GradientDrawable userCardBackground = new GradientDrawable();
        userCardBackground.setColor(Color.rgb(24, 35, 51));
        userCardBackground.setCornerRadius(dp(17));
        userCardBackground.setStroke(dp(1), Color.rgb(43, 59, 79));
        card.setBackground(userCardBackground);
        card.setElevation(dp(2));

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
                avatarView(58);

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

        nameText.setTypeface(null, Typeface.BOLD);
        nameText.setTextColor(Color.rgb(239, 244, 250));

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
                        ? Color.rgb(55, 205, 135)
                        : Color.GRAY
        );

        String userStatusText;
        if (finalBlockedUser) {
            userStatusText = tr(" 🚫 مسدود");
        } else if (Boolean.TRUE.equals(online)) {
            userStatusText = tr(" آنلاین");
        } else {
            userStatusText = " " + formatLastSeen(lastSeen);
        }
        TextView status = text(userStatusText, 12);
        status.setTextColor(Color.rgb(157, 173, 193));

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

        int unreadCount = unreadCounts.containsKey(finalUid) ? unreadCounts.get(finalUid) : 0;
        if (unreadCount > 0) {
            TextView unreadBadge = text(unreadCount > 99 ? "99+" : String.valueOf(unreadCount), 13);
            unreadBadge.setTextColor(Color.WHITE);
            unreadBadge.setGravity(Gravity.CENTER);
            unreadBadge.setPadding(dp(8), dp(4), dp(8), dp(4));
            unreadBadge.setBackground(bg(Color.rgb(0, 160, 75), 30));
            card.addView(unreadBadge, new LinearLayout.LayoutParams(dp(38), dp(32)));
        }

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
            tr("🚫 بلاک / رفع مسدودیت"),
            "حذف از فهرست چت"
    };

    AlertDialog userActionsDialog = new AlertDialog.Builder(this)
            .setTitle("مدیریت گفتگو")
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

                        } else if (which == 2) {

                            new AlertDialog.Builder(this)
                                    .setTitle("حذف از فهرست چت")
                                    .setMessage("این کار فقط کاربر را از فهرست این دستگاه پنهان می‌کند؛ حساب و پیام‌ها حذف نمی‌شوند.")
                                    .setNegativeButton(tr("انصراف"), null)
                                    .setPositiveButton(tr("حذف"), (confirm, result) -> {
                                        removedChatUserIds.add(uid);
                                        android.content.SharedPreferences prefs =
                                                getSharedPreferences("tajro_chat_list_" + myId, MODE_PRIVATE);
                                        prefs.edit().putStringSet(
                                                "removed_users", new HashSet<>(removedChatUserIds)).apply();
                                        loadUsers();
                                    })
                                    .show();

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
            .create();
    userActionsDialog.setOnShowListener(dialog -> {
        if (userActionsDialog.getWindow() != null) {
            userActionsDialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(Color.rgb(20, 31, 47)));
        }
        int titleId = getResources().getIdentifier("alertTitle", "id", "android");
        TextView titleView = userActionsDialog.findViewById(titleId);
        if (titleView != null) titleView.setTextColor(Color.WHITE);
        android.widget.ListView list = userActionsDialog.getListView();
        if (list != null) {
            list.setBackgroundColor(Color.rgb(20, 31, 47));
            list.setDivider(new android.graphics.drawable.ColorDrawable(Color.rgb(48, 64, 84)));
            list.setDividerHeight(dp(1));
            list.setSelector(android.R.drawable.list_selector_background);
        }
    });
    userActionsDialog.show();
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
                                    getProfilePhotoUrl(d);

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

        if (uid != null && uid.equals(myId)) {
            myProfileDialogImage = image;
        }

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
        styleProfileActionButton(change);

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
    styleProfileActionButton(privacy);

    privacy.setOnClickListener(
            v -> showChatPrivacySettings()
    );

    box.addView(privacy);


    Button blocked =
            new Button(this);

    blocked.setText(
            tr("🚫 فهرست مسدودشده‌ها")
    );
    styleProfileActionButton(blocked);

    blocked.setOnClickListener(
            v -> showBlockedUsers()
    );

    box.addView(blocked);
}
        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(tr(uid != null && uid.equals(myId)
                                ? "پروفایل من"
                                : "پروفایل"))
                        .setView(box)
                        .setPositiveButton(
                                tr("بستن"),
                                null
                        )
                        .create();

        dialog.show();
        styleChatDialog(dialog);
    }

    private void styleProfileActionButton(Button button) {
        if (button == null) return;
        android.graphics.drawable.GradientDrawable background =
                new android.graphics.drawable.GradientDrawable();
        background.setColor(Color.rgb(34, 70, 94));
        background.setCornerRadius(dp(12));
        background.setStroke(dp(1), Color.rgb(62, 105, 132));
        button.setBackground(background);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setPadding(dp(14), dp(10), dp(14), dp(10));
        button.setElevation(dp(2));
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
    private String formatAudioTime(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private void scrollMessagesToBottom() {
        if (messagesScroll == null) return;
        messagesScroll.post(() -> {
            if (messagesScroll != null) messagesScroll.fullScroll(View.FOCUS_DOWN);
        });
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
        messageViews.clear();
        dirtyMessageIds.clear();

        insideChat = true;
        scrollToBottomOnNextRender = true;
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

        // Near-white conversation canvas, matching the requested premium assistant style.
        root.setBackgroundColor(Color.rgb(248, 251, 253));

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

        GradientDrawable headerBackground = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.rgb(5, 48, 72), Color.rgb(8, 83, 108)}
        );
        header.setBackground(headerBackground);

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
                avatarView(56);

        headerAvatar.setClipToOutline(true);

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
                dp(10),
                dp(12),
                dp(10),
                dp(12)
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
                dp(10),
                dp(8),
                dp(10),
                dp(8)
        );
        bottom.setBackgroundColor(Color.rgb(18, 29, 45));
        bottom.setElevation(dp(5));

        mediaButton =
                new ImageButton(this);

        mediaButton.setImageResource(
                android.R.drawable.ic_menu_gallery
        );

        mediaButton.setBackground(bg(Color.rgb(31, 45, 63), 18));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaButton.setImageTintList(android.content.res.ColorStateList.valueOf(Color.rgb(8, 65, 91)));
        }

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
        messageInput.setTextColor(Color.rgb(35, 53, 64));
        messageInput.setHintTextColor(Color.rgb(137, 151, 160));
        messageInput.setPadding(dp(16), dp(11), dp(16), dp(11));
        GradientDrawable inputBackground = bg(Color.rgb(249, 252, 253), 28);
        inputBackground.setStroke(dp(1), Color.rgb(210, 229, 235));
        messageInput.setBackground(inputBackground);

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

        voiceButton.setBackground(bg(Color.rgb(239, 247, 249), 18));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            voiceButton.setImageTintList(android.content.res.ColorStateList.valueOf(Color.rgb(8, 65, 91)));
        }

        voiceButton.setPadding(
                dp(5),
                dp(5),
                dp(5),
                dp(5)
        );

        voiceButton.setOnClickListener(
                v -> toggleRecording()
        );

        recordingStatusText = new TextView(this);
        recordingStatusText.setText("00:00");
        recordingStatusText.setTextSize(11);
        recordingStatusText.setTextColor(Color.rgb(8, 65, 91));
        recordingStatusText.setGravity(Gravity.CENTER);
        recordingStatusText.setVisibility(View.GONE);
        bottom.addView(recordingStatusText, new LinearLayout.LayoutParams(dp(42), dp(48)));

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

        GradientDrawable sendBackground = new GradientDrawable();
        sendBackground.setShape(GradientDrawable.OVAL);
        sendBackground.setColor(Color.rgb(5, 55, 79));
        sendButton.setBackground(sendBackground);
        sendButton.setPadding(dp(13), dp(13), dp(13), dp(13));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            sendButton.setImageTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
            sendButton.setElevation(dp(3));
        }

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

        incomingInitialSnapshotReady = false;
        soundedIncomingMessageIds.clear();

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
                                                snapshot,
                                                false
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
                                                snapshot,
                                                true
                                        )
                        );
    }

    private void handleMessages(
            QuerySnapshot snapshot,
            boolean incoming
    ) {

        if (snapshot == null ||
                messagesContainer == null) {

            return;
        }

        for (
                DocumentChange change :
                snapshot.getDocumentChanges()
        ) {

            DocumentSnapshot document =
                    change.getDocument();

            String id =
                    document.getId();

            if (change.getType() == DocumentChange.Type.REMOVED) {
                messageCache.remove(id);
                dirtyMessageIds.remove(id);
                continue;
            }

            messageCache.put(
                    id,
                    document
            );
            // پیام تغییرکرده فقط خودش دوباره ساخته می‌شود.
            dirtyMessageIds.add(id);

            if (incoming) {

                Boolean read =
                        document.getBoolean("read");

                if (read == null || !read) {

                    db.collection("messages")
                            .document(id)
                            .update("read", true);
                }

                if (incomingInitialSnapshotReady &&
                        change.getType() ==
                                DocumentChange.Type.ADDED) {

                    if (soundedIncomingMessageIds.add(id)) {
                        playIncomingMessageSound();
                    }
                }
            }
        }

        if (incoming) {
            incomingInitialSnapshotReady = true;
        }

        renderMessages();
    }

    private Ringtone incomingChatRingtone;

    // صدای اعلان خوش‌آهنگ برای تمام پیام‌های ورودی:
    // متن، عکس، ویدئو و پیام صوتی از همین مسیر اعلان می‌گیرند.
    private void playIncomingMessageSound() {
        try {
            AudioManager audioManager =
                    (AudioManager) getSystemService(Context.AUDIO_SERVICE);

            if (audioManager == null) {
                return;
            }

            int ringerMode = audioManager.getRingerMode();

            if (ringerMode == AudioManager.RINGER_MODE_SILENT) {
                // در حالت بی‌صدا، صدای اعلان را پخش نکن.
                return;
            }

            if (ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
                // در حالت لرزه فقط لرزش انجام شود.
                vibrateForIncomingMessage();
                return;
            }

            Uri notificationUri =
                    RingtoneManager.getDefaultUri(
                            RingtoneManager.TYPE_NOTIFICATION
                    );

            if (notificationUri == null) {
                notificationUri =
                        RingtoneManager.getDefaultUri(
                                RingtoneManager.TYPE_RINGTONE
                        );
            }

            if (notificationUri != null) {
                if (incomingChatRingtone != null &&
                        incomingChatRingtone.isPlaying()) {
                    incomingChatRingtone.stop();
                }

                incomingChatRingtone =
                        RingtoneManager.getRingtone(
                                getApplicationContext(),
                                notificationUri
                        );

                if (incomingChatRingtone != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        incomingChatRingtone.setAudioAttributes(
                                new android.media.AudioAttributes.Builder()
                                        .setUsage(
                                                android.media.AudioAttributes.USAGE_NOTIFICATION
                                        )
                                        .setContentType(
                                                android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION
                                        )
                                        .build()
                        );
                    } else {
                        incomingChatRingtone.setStreamType(
                                AudioManager.STREAM_NOTIFICATION
                        );
                    }

                    incomingChatRingtone.play();
                }
            }
        } catch (Exception e) {
            Log.e("CHAT_NOTIFICATION", "Could not play incoming message sound", e);
        }
    }

    private void vibrateForIncomingMessage() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                android.os.VibratorManager manager =
                        (android.os.VibratorManager) getSystemService(
                                Context.VIBRATOR_MANAGER_SERVICE
                        );
                if (manager != null) {
                    android.os.Vibrator vibrator = manager.getDefaultVibrator();
                    if (vibrator != null && vibrator.hasVibrator()) {
                        vibrator.vibrate(
                                android.os.VibrationEffect.createWaveform(
                                        new long[]{0, 180, 100, 180},
                                        -1
                                )
                        );
                    }
                }
            } else {
                android.os.Vibrator vibrator =
                        (android.os.Vibrator) getSystemService(
                                Context.VIBRATOR_SERVICE
                        );
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(
                                android.os.VibrationEffect.createWaveform(
                                        new long[]{0, 180, 100, 180},
                                        -1
                                )
                        );
                    } else {
                        vibrator.vibrate(new long[]{0, 180, 100, 180}, -1);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("CHAT_NOTIFICATION", "Could not vibrate for incoming message", e);
        }
    }

    private void renderMessages() {

        if (messagesContainer == null) {
            return;
        }

        final int oldScrollY = messagesScroll == null ? 0 : messagesScroll.getScrollY();
        final boolean wasAtBottom = messagesScroll != null &&
                messagesScroll.getChildCount() > 0 &&
                oldScrollY + messagesScroll.getHeight() >=
                        messagesScroll.getChildAt(0).getHeight() - dp(80);

        List<DocumentSnapshot> list = new ArrayList<>(messageCache.values());
        Collections.sort(list, (a, b) -> {
            com.google.firebase.Timestamp ta = a.getTimestamp("timestamp");
            com.google.firebase.Timestamp tb = b.getTimestamp("timestamp");
            if (ta == null && tb == null) return a.getId().compareTo(b.getId());
            if (ta == null) return -1;
            if (tb == null) return 1;
            int compare = ta.compareTo(tb);
            return compare != 0 ? compare : a.getId().compareTo(b.getId());
        });

        Set<String> visibleIds = new HashSet<>();
        for (DocumentSnapshot d : list) {
            if (!isDeletedForMe(d)) visibleIds.add(d.getId());
        }

        // فقط ظرف پیام‌هایی را حذف کن که دیگر وجود ندارند یا برای من حذف شده‌اند.
        for (String existingId : new ArrayList<>(messageViews.keySet())) {
            if (!visibleIds.contains(existingId)) {
                LinearLayout oldView = messageViews.remove(existingId);
                if (oldView != null && oldView.getParent() == messagesContainer) {
                    messagesContainer.removeView(oldView);
                }
                dirtyMessageIds.remove(existingId);
            }
        }

        // پیام جدید یا تغییرکرده را در ظرف مستقل خود بساز.
        com.google.firebase.Timestamp previousVisibleTimestamp = null;
        for (DocumentSnapshot d : list) {
            String id = d.getId();
            if (!visibleIds.contains(id)) continue;
            com.google.firebase.Timestamp messageTimestamp = d.getTimestamp("timestamp");
            boolean firstMessageOfDay = messageTimestamp != null
                    && (previousVisibleTimestamp == null
                    || !isSameMessageDay(previousVisibleTimestamp, messageTimestamp));
            if (!messageViews.containsKey(id) || dirtyMessageIds.contains(id)) {
                LinearLayout oldView = messageViews.remove(id);
                int oldIndex = oldView != null && oldView.getParent() == messagesContainer
                        ? messagesContainer.indexOfChild(oldView) : -1;
                if (oldView != null && oldView.getParent() == messagesContainer) {
                    messagesContainer.removeView(oldView);
                }

                LinearLayout item = new LinearLayout(this);
                item.setOrientation(LinearLayout.VERTICAL);
                item.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                currentMessageContainer = item;
                item.setTag(d.getString("senderId"));
                if (firstMessageOfDay) {
                    addMessageDateLabel(item, formatMessageDay(messageTimestamp));
                }
                renderOneMessage(d);
                addMessageTimeLabel(item, messageTimestamp);
                currentMessageContainer = null;
                messageViews.put(id, item);

                int targetIndex = oldIndex >= 0
                        ? Math.min(oldIndex, messagesContainer.getChildCount())
                        : messagesContainer.getChildCount();
                messagesContainer.addView(item, targetIndex);
                dirtyMessageIds.remove(id);
            }
            if (messageTimestamp != null) previousVisibleTimestamp = messageTimestamp;
        }

        // ترتیب را فقط در صورت نیاز اصلاح کن؛ viewهای موجود بی‌دلیل بازسازی نمی‌شوند.
        List<String> visibleOrder = new ArrayList<>();
        for (DocumentSnapshot d : list) {
            if (visibleIds.contains(d.getId())) visibleOrder.add(d.getId());
        }
        for (int i = 0; i < visibleOrder.size(); i++) {
            String id = visibleOrder.get(i);
            LinearLayout item = messageViews.get(id);
            if (item == null) continue;
            int currentIndex = messagesContainer.indexOfChild(item);
            if (currentIndex != i) {
                messagesContainer.removeView(item);
                messagesContainer.addView(item, Math.min(i, messagesContainer.getChildCount()));
            }
        }

        if (messagesScroll != null) {
            if (pendingNotificationMessageId != null
                    && messageViews.containsKey(pendingNotificationMessageId)) {
                final String targetId = pendingNotificationMessageId;
                pendingNotificationMessageId = null;
                messagesScroll.post(() -> {
                    View target = messageViews.get(targetId);
                    if (target != null) messagesScroll.smoothScrollTo(0, target.getTop());
                });
                return;
            }
            final boolean forceBottom = scrollToBottomOnNextRender;
            scrollToBottomOnNextRender = false;
            messagesScroll.post(() -> {
                if (forceBottom || wasAtBottom) {
                    messagesScroll.fullScroll(View.FOCUS_DOWN);
                } else {
                    messagesScroll.scrollTo(0, oldScrollY);
                }
            });
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
                    d.getId(),
                    Boolean.TRUE.equals(
                            d.getBoolean("read")
                    )
            );

        } else if ("image".equals(type)) {

            String url =
                    d.getString("mediaUrl");

            if (url != null) {

                addMediaMessage(
                        url,
                        false,
                        sender,
                        d.getId(),
                        Boolean.TRUE.equals(
                                d.getBoolean("read")
                        )
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
                        d.getId(),
                        Boolean.TRUE.equals(
                                d.getBoolean("read")
                        )
                );
            }

        } else if ("audio".equals(type)) {

            String url =
                    d.getString("audioUrl");

            if (url != null) {

                addAudioMessage(
                        url,
                        sender,
                        d.getId(),
                        Boolean.TRUE.equals(
                                d.getBoolean("read")
                        )
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
                messageId,
                false
        );
    }

    private void addTextMessage(
            String message,
            String sender,
            String messageId,
            boolean read
    ) {

        TextView bubble =
                text(
                        message,
                        16
                );

        boolean mine =
                myId.equals(sender);

        if (mine) {
            bubble.setText(
                    message +
                            (read ? "  ✓✓" : "  ✓")
            );
        }

        // هر دو نوع پیام در زمینه تیره چت باید خوانا باشند.
        // فقط رنگ متن پیام دریافتی از تیره به سفید تغییر کرده است.
        bubble.setTextColor(Color.WHITE);

        GradientDrawable bubbleBackground = bg(
                mine ? Color.rgb(8, 65, 91) : Color.rgb(34, 46, 63),
                18
        );
        if (!mine) {
            bubbleBackground.setStroke(dp(1), Color.rgb(220, 233, 239));
        }
        bubble.setBackground(bubbleBackground);
        bubble.setElevation(dp(1));

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

        currentMessageContainer.addView(
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
            String messageId,
            boolean read
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
                bg(Color.rgb(34, 46, 63), 14)
        );

        image.setPadding(
                dp(2),
                dp(2),
                dp(2),
                dp(2)
        );

        // برای ویدیو پس‌زمینه تیره و آیکون پخش نشان می‌دهیم؛
        // پس از دریافت فایل، فریم واقعی ویدیو به‌عنوان پیش‌نمایش قرار می‌گیرد.
        image.setTag(url);
        if (video) {
            image.setBackground(bg(Color.rgb(32, 38, 46), 14));
            image.setImageResource(android.R.drawable.ic_media_play);
            image.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            image.setAlpha(1f);
            loadVideoThumbnail(url, image);
        } else {
            image.setImageResource(android.R.drawable.ic_menu_gallery);
            image.setAlpha(0.55f);
            loadImage(url, image);
        }

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

        currentMessageContainer.addView(
                image,
                p
        );

        if (mine) {
            addReceiptView(read, p.gravity);
        }

        if (!video) {
            image.setOnClickListener(v -> showImageViewer(url));
        } else {
            image.setOnClickListener(v -> playVideo(url, image));
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
            String messageId,
            boolean read
    ) {

        Button play =
                new Button(this);

        play.setText(
                tr("▶️ پخش پیام صوتی")
        );

        play.setTextSize(14);
        play.setTextColor(Color.rgb(8, 65, 91));
        GradientDrawable audioButtonBackground = bg(Color.rgb(31, 45, 63), 16);
        audioButtonBackground.setStroke(dp(1), Color.rgb(210, 229, 235));
        play.setBackground(audioButtonBackground);
        play.setAllCaps(false);

        TextView playbackStatus = new TextView(this);
        playbackStatus.setText("00:00 / --:--");
        playbackStatus.setTextSize(11);
        playbackStatus.setGravity(Gravity.CENTER);

        play.setOnClickListener(
                v -> playAudio(url, playbackStatus)
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

        currentMessageContainer.addView(
                play,
                p
        );
        LinearLayout.LayoutParams playbackStatusParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        playbackStatusParams.gravity = p.gravity;
        playbackStatusParams.setMargins(dp(8), 0, dp(8), dp(3));
        currentMessageContainer.addView(playbackStatus, playbackStatusParams);

        if (mine) {
            addReceiptView(read, p.gravity);
        }

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

    private void addReceiptView(
            boolean read,
            int gravity
    ) {

        TextView receipt =
                text(
                        read ? "✓✓" : "✓",
                        12
                );

        receipt.setTextColor(
                Color.GRAY
        );

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        p.gravity = gravity;

        p.setMargins(
                dp(6),
                0,
                dp(6),
                dp(1)
        );

        currentMessageContainer.addView(
                receipt,
                p
        );
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

    /**
     * درخواست اعلان را پس از ذخیره موفق پیام در Firestore می‌فرستد.
     * فقط messageId ارسال می‌شود؛ متن و لینک رسانه هرگز ارسال نمی‌شوند.
     */
    private void sendChatNotification(String messageId) {

        if (messageId == null || messageId.trim().isEmpty()) {
            return;
        }

        FirebaseUser currentUser = auth != null ? auth.getCurrentUser() : null;
        if (currentUser == null) {
            Log.w("CHAT_NOTIFICATION", "No signed-in Firebase user");
            return;
        }

        currentUser.getIdToken(false)
                .addOnSuccessListener(tokenResult -> {

                    String firebaseToken = tokenResult.getToken();
                    if (firebaseToken == null || firebaseToken.isEmpty()) {
                        Log.e("CHAT_NOTIFICATION", "Firebase ID token is empty");
                        return;
                    }

                    new Thread(() -> {
                        HttpURLConnection connection = null;

                        try {
                            URL endpoint = new URL(
                                    SUPABASE_URL + "/functions/v1/send-chat-notification"
                            );

                            connection = (HttpURLConnection) endpoint.openConnection();
                            connection.setRequestMethod("POST");
                            connection.setConnectTimeout(15000);
                            connection.setReadTimeout(15000);
                            connection.setDoOutput(true);
                            connection.setRequestProperty(
                                    "Authorization", "Bearer " + firebaseToken
                            );
                            connection.setRequestProperty(
                                    "apikey", SUPABASE_PUBLISHABLE_KEY
                            );
                            connection.setRequestProperty(
                                    "Content-Type", "application/json; charset=UTF-8"
                            );
                            connection.setRequestProperty("Accept", "application/json");

                            JSONObject requestBody = new JSONObject();
                            requestBody.put("messageId", messageId);

                            byte[] payload = requestBody.toString().getBytes("UTF-8");
                            connection.setFixedLengthStreamingMode(payload.length);

                            try (OutputStream output = connection.getOutputStream()) {
                                output.write(payload);
                                output.flush();
                            }

                            int responseCode = connection.getResponseCode();
                            if (responseCode < 200 || responseCode >= 300) {
                                Log.e(
                                        "CHAT_NOTIFICATION",
                                        "Edge Function returned HTTP " + responseCode
                                );
                            } else {
                                Log.i(
                                        "CHAT_NOTIFICATION",
                                        "Notification request accepted for message " + messageId
                                );
                            }

                        } catch (Exception error) {
                            Log.e("CHAT_NOTIFICATION", "Notification request failed", error);

                        } finally {
                            if (connection != null) {
                                connection.disconnect();
                            }
                        }
                    }, "chat-notification").start();
                })
                .addOnFailureListener(error ->
                        Log.e("CHAT_NOTIFICATION", "Could not get Firebase ID token", error)
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

        // شناسه از پیش ساخته‌شده مانع ایجاد پیام تکراری در تلاش مجدد می‌شود.
        final String messageId = UUID.randomUUID().toString();
        final String textBeingSent = message;
        messageInput.setText("");
        scrollMessagesToBottom();
        db.collection("messages").document(messageId)
                .set(data)
                .addOnSuccessListener(x -> sendChatNotification(messageId))
                .addOnFailureListener(e -> {
                    if (messageInput != null && messageInput.getText().toString().trim().isEmpty()) {
                        messageInput.setText(textBeingSent);
                    }
                    Toast.makeText(this, tr("خطا در ارسال پیام"), Toast.LENGTH_SHORT).show();
                });
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
                                    com.google.firebase.Timestamp lastSeen =
                                            d.getTimestamp("lastSeen");
                                    long age = lastSeen == null
                                            ? Long.MAX_VALUE
                                            : System.currentTimeMillis() - lastSeen.toDate().getTime();
                                    boolean presenceFresh = age >= 0 && age <= 150_000L;
                                    boolean actuallyOnline = Boolean.TRUE.equals(online) && presenceFresh;

                                    if (actuallyOnline) {

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

                                        statusText.setText(formatLastSeen(lastSeen));

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
            messageViews.clear();
            dirtyMessageIds.clear();

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
        voiceUiHandler.removeCallbacks(recordingTicker);
        if (recordingStatusText != null) recordingStatusText.setVisibility(View.GONE);

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

        boolean image = mime.startsWith("image/");

        // ارسال و آپلود ویدیو عمداً غیرفعال است؛ پیام‌های ویدیویی قدیمی فقط برای سازگاری قابل مشاهده می‌مانند.
        if (!image) {

            Toast.makeText(
                    this,
                    tr("این نوع فایل پشتیبانی نمی‌شود"),
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String ext;
        if (mime.contains("png")) {
            ext = "png";
        } else if (mime.contains("webp")) {
            ext = "webp";
        } else if (mime.contains("gif")) {
            ext = "gif";
        } else {
            ext = "jpg";
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
                                "image",
                                url,
                                objectPath
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
            String url,
            String storagePath
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

        // مسیر دقیق فایل در Supabase برای حذف امن هنگام حذف حساب
        data.put("storageBucket", CHAT_BUCKET);
        data.put("storagePath", storagePath);

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
                .addOnSuccessListener(x ->
                        sendChatNotification(x.getId())
                )
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
            recordingStartedAt = System.currentTimeMillis();
            if (recordingStatusText != null) {
                recordingStatusText.setText("00:00");
                recordingStatusText.setVisibility(View.VISIBLE);
            }
            voiceUiHandler.removeCallbacks(recordingTicker);
            voiceUiHandler.post(recordingTicker);

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
            voiceUiHandler.removeCallbacks(recordingTicker);
            if (recordingStatusText != null) recordingStatusText.setVisibility(View.GONE);

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

        if (recorder == null) return;

        final String completedAudioPath = audioPath;
        boolean stopSucceeded = false;
        try {
            recorder.stop();
            stopSucceeded = true;
        } catch (Exception e) {
            Log.w("CHAT_VOICE", "Recorder could not finalize audio", e);
        } finally {
            recording = false;
            voiceUiHandler.removeCallbacks(recordingTicker);
            releaseRecorder();
            if (recordingStatusText != null) recordingStatusText.setVisibility(View.GONE);
            if (voiceButton != null) voiceButton.setImageResource(android.R.drawable.ic_btn_speak_now);
        }

        if (!stopSucceeded || completedAudioPath == null) {
            Toast.makeText(this, tr("ضبط صدا ناموفق بود؛ لطفاً دوباره تلاش کنید"), Toast.LENGTH_SHORT).show();
            return;
        }

        File finishedFile = new File(completedAudioPath);
        if (!finishedFile.isFile() || finishedFile.length() < 128L) {
            try { if (finishedFile.exists()) finishedFile.delete(); } catch (Exception ignored) {}
            Toast.makeText(this, tr("فایل صوتی خالی یا ناقص است؛ کمی بیشتر ضبط کنید"), Toast.LENGTH_SHORT).show();
            return;
        }

        uploadVoice(completedAudioPath);
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

                        saveAudioMessage(url, objectPath);

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
            String url,
            String storagePath
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

        // مسیر دقیق فایل در Supabase برای حذف امن هنگام حذف حساب
        data.put("storageBucket", VOICE_BUCKET);
        data.put("storagePath", storagePath);

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
                .addOnSuccessListener(x ->
                        sendChatNotification(x.getId())
                )
                .addOnFailureListener(
                        e ->
                                Toast.makeText(
                                        this,
                                        tr("خطا در ذخیره پیام صوتی"),
                                        Toast.LENGTH_SHORT
                                ).show()
                );
    }

    private void playAudio(String url, TextView statusView) {
        if (url == null || url.trim().isEmpty()) return;
        MEDIA_EXECUTOR.execute(() -> {
            File audio = getCachedMediaFile(url, "audio");
            if (audio == null) {
                audio = downloadMediaToCache(url, "audio", 30L * 1024L * 1024L);
            }
            final File readyAudio = audio;
            runOnUiThread(() -> {
                if (readyAudio == null || !readyAudio.isFile()) {
                    Toast.makeText(this, tr("صدا هنوز ذخیره نشده است؛ پس از وصل‌شدن اینترنت دوباره تلاش کنید"), Toast.LENGTH_SHORT).show();
                    return;
                }
                playCachedAudio(readyAudio.getAbsolutePath(), statusView);
            });
        });
    }

    private void playCachedAudio(String localPath, TextView statusView) {
        try {
            audioPlaybackHandler.removeCallbacks(playbackTicker);
            if (player != null) {
                try { player.stop(); } catch (Exception ignored) {}
                player.release();
                player = null;
            }
            activePlaybackStatus = statusView;
            player = new MediaPlayer();
            player.setDataSource(localPath);
            player.setOnPreparedListener(mp -> {
                int duration = mp.getDuration();
                if (activePlaybackStatus != null) {
                    activePlaybackStatus.setText("00:00 / " + formatAudioTime(duration));
                }
                mp.start();
                audioPlaybackHandler.post(playbackTicker);
            });
            player.setOnCompletionListener(mp -> {
                audioPlaybackHandler.removeCallbacks(playbackTicker);
                if (activePlaybackStatus != null) {
                    try { activePlaybackStatus.setText(formatAudioTime(mp.getDuration()) + " / " + formatAudioTime(mp.getDuration())); }
                    catch (Exception ignored) {}
                }
                mp.release();
                if (player == mp) player = null;
                activePlaybackStatus = null;
            });
            player.setOnErrorListener((mp, what, extra) -> {
                audioPlaybackHandler.removeCallbacks(playbackTicker);
                Toast.makeText(this, tr("پخش صدا ناموفق بود"), Toast.LENGTH_SHORT).show();
                mp.release();
                if (player == mp) player = null;
                activePlaybackStatus = null;
                return true;
            });
            player.prepareAsync();
        } catch (Exception e) {
            Log.e("ChatMediaCache", "Audio playback failed", e);
            Toast.makeText(this, tr("خطا در پخش صدا"), Toast.LENGTH_SHORT).show();
        }
    }

    // فریم اول ویدیو را از نسخه کش‌شده می‌خواند تا پیام ویدیو سفید و خالی نماند.
    private void loadVideoThumbnail(String url, ImageView target) {
        if (url == null || target == null) return;
        // برای جلوگیری از مصرف ناخواسته اینترنت، فقط نسخه‌ای را می‌خوانیم که قبلاً کش شده است.
        File cached = getCachedMediaFile(url, "video");
        if (cached != null) loadVideoThumbnailFromFile(cached, url, target);
    }

    private void loadVideoThumbnailFromFile(File video, String url, ImageView target) {
        if (video == null || target == null) return;
        new Thread(() -> {
            Bitmap frame = null;
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            try {
                retriever.setDataSource(video.getAbsolutePath());
                frame = retriever.getFrameAtTime(1_000_000L,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                if (frame == null) {
                    frame = retriever.getFrameAtTime(0L,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                }
            } catch (Exception e) {
                Log.w("ChatVideo", "Could not create video thumbnail", e);
            } finally {
                try { retriever.release(); } catch (Exception ignored) { }
            }
            final Bitmap thumbnail = frame;
            if (thumbnail != null) {
                runOnUiThread(() -> {
                    if (target.getTag() != null && url.equals(target.getTag())) {
                        target.setImageBitmap(thumbnail);
                        target.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        target.setAlpha(1f);
                    }
                });
            }
        }, "chat-video-thumbnail").start();
    }

    private void playVideo(String url, ImageView preview) {
        if (url == null || url.trim().isEmpty()) return;
        Toast.makeText(this, tr("در حال آماده‌سازی ویدیو..."), Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            File video = getCachedMediaFile(url, "video");
            if (video == null) video = downloadMediaToCache(url, "video", 100L * 1024L * 1024L);
            final File readyVideo = video;
            runOnUiThread(() -> {
                if (readyVideo == null || !readyVideo.isFile() || readyVideo.length() == 0) {
                    Toast.makeText(this, tr("ویدیو دریافت نشد؛ اینترنت یا دسترسی فایل را بررسی کنید"), Toast.LENGTH_SHORT).show();
                    return;
                }
                loadVideoThumbnailFromFile(readyVideo, url, preview);
                VideoView videoView = new VideoView(this);
                LinearLayout holder = new LinearLayout(this);
                holder.setGravity(Gravity.CENTER);
                holder.setBackgroundColor(Color.BLACK);
                holder.setPadding(dp(8), dp(8), dp(8), dp(8));
                holder.addView(videoView, new LinearLayout.LayoutParams(-1, dp(280)));
                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle(tr("ویدیو"))
                        .setView(holder)
                        .setPositiveButton(tr("بستن"), (d, w) -> videoView.stopPlayback())
                        .create();
                dialog.setOnDismissListener(d -> { try { videoView.stopPlayback(); } catch (Exception ignored) {} });
                videoView.setOnPreparedListener(mp -> {
                    mp.setLooping(false);
                    videoView.start();
                });
                videoView.setOnErrorListener((mp, what, extra) -> {
                    Log.e("ChatVideo", "VideoView error what=" + what + ", extra=" + extra);
                    Toast.makeText(this, tr("قالب این ویدیو در گوشی پشتیبانی نمی‌شود یا فایل ناقص است"), Toast.LENGTH_LONG).show();
                    return true;
                });
                // ابتدا پنجره و سطح نمایش ساخته شود، سپس مسیر فایل به VideoView داده شود.
                dialog.show();
                if (dialog.getWindow() != null) dialog.getWindow().setLayout(-1, dp(360));
                videoView.setVideoPath(readyVideo.getAbsolutePath());
            });
        }).start();
    }

    // کش خصوصی و محدود برای رسانه‌های چت؛ فایل‌ها داخل فضای خصوصی برنامه می‌مانند.
    // رسانه‌های بازشده در filesDir نگهداری می‌شوند؛ برخلاف cacheDir سیستم آن‌ها را خودکار پاک نمی‌کند.
    private File chatMediaCacheDir() {
        File dir = new File(getFilesDir(), "tajro_chat_media");
        if (!dir.exists()) {
            // انتقال کش نسخه قبلی برای حفظ فایل‌هایی که قبلاً دانلود شده‌اند.
            File legacy = new File(getCacheDir(), "tajro_chat_media");
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

    private String chatCacheKey(String value) {
        try {
            String stable = value == null ? "" : value.trim();
            int query = stable.indexOf('?');
            if (query >= 0) stable = stable.substring(0, query);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(stable.getBytes("UTF-8"));
            StringBuilder out = new StringBuilder();
            for (byte b : digest) out.append(String.format(java.util.Locale.US, "%02x", b));
            return out.toString();
        } catch (Exception e) {
            return Integer.toHexString(String.valueOf(value).hashCode());
        }
    }

    private File getCachedMediaFile(String stableKey, String type) {
        File file = new File(chatMediaCacheDir(), type + "_" + chatCacheKey(stableKey));
        if (file.isFile() && file.length() > 0) {
            file.setLastModified(System.currentTimeMillis());
            return file;
        }
        return null;
    }

    private File downloadMediaToCache(String url, String type, long maxFileBytes) {
        return downloadMediaToCache(url, type, maxFileBytes, url);
    }

    // دانلود قابل ادامه: اگر اینترنت وسط کار قطع شود، فایل .tmp باقی می‌ماند
    // و دفعه بعد با HTTP Range از همان نقطه ادامه می‌یابد (در صورت پشتیبانی سرور).
    private File downloadMediaToCache(String url, String type, long maxFileBytes, String stableKey) {
        HttpURLConnection connection = null;
        File dir = chatMediaCacheDir();
        File target = new File(dir, type + "_" + chatCacheKey(stableKey));
        File temp = new File(dir, target.getName() + ".tmp");
        try {
            if (target.isFile() && target.length() > 0) return target;
            long offset = temp.isFile() ? temp.length() : 0L;
            if (offset > maxFileBytes) { temp.delete(); offset = 0L; }
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(20000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("Accept-Encoding", "identity");
            if (offset > 0) connection.setRequestProperty("Range", "bytes=" + offset + "-");
            if (url.startsWith(SUPABASE_URL)) connection.setRequestProperty("apikey", SUPABASE_PUBLISHABLE_KEY);
            int code = connection.getResponseCode();
            if (offset > 0 && code == HttpURLConnection.HTTP_OK) {
                // سرور Range را نپذیرفته؛ از ابتدا دانلود کن تا فایل خراب ترکیب نشود.
                temp.delete();
                offset = 0L;
            } else if (offset > 0 && code != HttpURLConnection.HTTP_PARTIAL) {
                return null;
            } else if (offset == 0 && (code < 200 || code >= 300)) {
                return null;
            }
            long declared = connection.getContentLengthLong();
            if (declared > maxFileBytes || offset + Math.max(0L, declared) > maxFileBytes) return null;
            long total = offset;
            try (InputStream in = connection.getInputStream();
                 FileOutputStream out = new FileOutputStream(temp, offset > 0)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = in.read(buffer)) != -1) {
                    total += count;
                    if (total > maxFileBytes) throw new java.io.IOException("Media exceeds cache limit");
                    out.write(buffer, 0, count);
                }
                out.flush();
                out.getFD().sync();
            }
            if (total == 0) { temp.delete(); return null; }
            // اگر پاسخ Range ناقص بوده باشد، فایل موقت حفظ می‌شود تا تلاش بعدی ادامه دهد.
            String contentRange = connection.getHeaderField("Content-Range");
            if (code == HttpURLConnection.HTTP_PARTIAL && contentRange != null) {
                int slash = contentRange.lastIndexOf('/');
                if (slash > 0) {
                    try {
                        long expected = Long.parseLong(contentRange.substring(slash + 1).trim());
                        if (expected > total) return null;
                        if (expected != total) return null;
                    } catch (NumberFormatException ignored) { }
                }
            }
            if (!temp.renameTo(target)) return null;
            return target;
        } catch (Exception e) {
            // فایل موقت عمداً حذف نمی‌شود؛ تلاش بعدی می‌تواند ادامه دهد.
            Log.w("ChatMediaCache", "Media download interrupted; partial file retained", e);
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private void trimChatMediaCache(long maxBytes) {
        File[] files = chatMediaCacheDir().listFiles();
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


    
private void uploadProfilePhoto(Uri uri) {
    String mime = getContentResolver().getType(uri);

    if (mime == null || !mime.startsWith("image/")) {
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

    final String finalMime = mime;

    // ابتدا آدرس عکس قبلی را می‌خوانیم.
    db.collection("users").document(myId).get()
        .addOnSuccessListener(userDoc -> {
            String oldPhotoUrl = userDoc.getString("photoUrl");

            String objectPath = "profiles/profile_" + myId + "_"
                    + System.currentTimeMillis() + "." + ext;

            Toast.makeText(this, tr("در حال ارسال عکس پروفایل..."),
                    Toast.LENGTH_SHORT).show();

            uploadToSupabase(uri, PROFILE_BUCKET, objectPath, finalMime,
                new SupabaseUploadCallback() {
                    @Override
                    public void onSuccess(String url) {
                        Map<String, Object> data = new HashMap<>();
                        data.put("photoUrl", url);
                        // مسیر دقیق عکس پروفایل برای حذف فایل متعلق به همین حساب
                        data.put("photoStorageBucket", PROFILE_BUCKET);
                        data.put("photoStoragePath", objectPath);

                        db.collection("users").document(myId)
                            .set(data, SetOptions.merge())
                            .addOnSuccessListener(x -> {
                                Toast.makeText(ChatActivity.this,
                                        tr("عکس پروفایل ذخیره شد"),
                                        Toast.LENGTH_SHORT).show();

                                loadUsers();
                                refreshMyProfileAvatar();

                                if (myProfileDialogImage != null) {
                                    loadImage(url, myProfileDialogImage);
                                }

                                // فقط پس از ذخیره موفق عکس جدید،
                                // مسیر عکس قبلی را برای حذف بررسی می‌کنیم.
                                if (oldPhotoUrl != null
                                        && !oldPhotoUrl.trim().isEmpty()
                                        && !oldPhotoUrl.equals(url)) {
                                    deleteOldProfilePhoto(oldPhotoUrl);
                                }
                            })
                            .addOnFailureListener(e ->
                                Toast.makeText(ChatActivity.this,
                                    tr("ذخیره عکس در حساب ناموفق بود"),
                                    Toast.LENGTH_LONG).show()
                            );
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(ChatActivity.this,
                                tr("خطای عکس پروفایل:\n") + error,
                                Toast.LENGTH_LONG).show();
                    }
                });
        })
        .addOnFailureListener(e ->
            Toast.makeText(this, tr("خواندن عکس قبلی ناموفق بود"),
                    Toast.LENGTH_LONG).show()
        );
}

    
private void deleteOldProfilePhoto(String oldPhotoUrl) {
    if (oldPhotoUrl == null || oldPhotoUrl.trim().isEmpty()) {
        return;
    }

    String marker = "/" + PROFILE_BUCKET + "/";
    int markerIndex = oldPhotoUrl.indexOf(marker);

    if (markerIndex < 0) {
        return;
    }

    String encodedPath = oldPhotoUrl.substring(markerIndex + marker.length());
    int queryIndex = encodedPath.indexOf('?');
    if (queryIndex >= 0) {
        encodedPath = encodedPath.substring(0, queryIndex);
    }

    final String objectPath;
    try {
        objectPath = java.net.URLDecoder.decode(encodedPath, "UTF-8");
    } catch (Exception e) {
        return;
    }

    // فقط مسیر عکس قدیمی متعلق به همین کاربر پذیرفته شود.
    String prefix = "profiles/profile_" + myId + "_";
    if (!objectPath.startsWith(prefix)) {
        return;
    }

    String filename = objectPath.substring(prefix.length());
    if (!filename.matches("\\d+\\.(jpg|jpeg|png|webp)")) {
        return;
    }

    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    if (currentUser == null) {
        return;
    }

    currentUser.getIdToken(false)
        .addOnSuccessListener(tokenResult -> {
            String idToken = tokenResult.getToken();
            if (idToken == null || idToken.isEmpty()) {
                return;
            }

            new Thread(() -> {
                HttpURLConnection connection = null;

                try {
                    URL functionUrl = new URL(
                        SUPABASE_URL + "/functions/v1/delete-old-profile-photo"
                    );

                    connection = (HttpURLConnection) functionUrl.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setDoOutput(true);
                    connection.setConnectTimeout(20000);
                    connection.setReadTimeout(30000);
                    connection.setRequestProperty(
                        "Content-Type", "application/json; charset=UTF-8"
                    );
                    connection.setRequestProperty("apikey", SUPABASE_PUBLISHABLE_KEY);
                    connection.setRequestProperty("Authorization", "Bearer " + idToken);

                    JSONObject body = new JSONObject();
                    body.put("path", objectPath);

                    try (OutputStream output = connection.getOutputStream()) {
                        output.write(body.toString().getBytes(
                            java.nio.charset.StandardCharsets.UTF_8
                        ));
                    }

                    int code = connection.getResponseCode();

                    if (code < 200 || code >= 300) {
                        InputStream errorStream = connection.getErrorStream();
                        String errorText = "";

                        if (errorStream != null) {
                            try (InputStream input = errorStream;
                                 java.io.ByteArrayOutputStream bytes =
                                     new java.io.ByteArrayOutputStream()) {
                                byte[] buffer = new byte[2048];
                                int count;
                                while ((count = input.read(buffer)) != -1) {
                                    bytes.write(buffer, 0, count);
                                }
                                errorText = bytes.toString("UTF-8");
                            }
                        }

                        Log.e("DeleteOldPhoto",
                            "HTTP " + code + ": " + errorText);
                    } else {
                        Log.d("DeleteOldPhoto", "Old photo deletion requested.");
                    }

                } catch (Exception e) {
                    Log.e("DeleteOldPhoto", "Failed to call deletion function", e);
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }).start();
        })
        .addOnFailureListener(e ->
            Log.e("DeleteOldPhoto", "Could not get Firebase token", e)
        );
}

    private void uploadToSupabase(
            Uri uri,
            String bucket,
            String objectPath,
            String contentType,
            SupabaseUploadCallback callback
    ) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            File tempFile = null;
            try {
                // بعضی DocumentProviderها اندازه نادرست می‌دهند؛ ابتدا فایل را کامل
                // در حافظه موقت کپی می‌کنیم تا Content-Length دقیق باشد.
                tempFile = File.createTempFile("tajro_upload_", ".tmp", getCacheDir());
                try (InputStream source = getContentResolver().openInputStream(uri);
                     OutputStream cacheOut = new java.io.FileOutputStream(tempFile)) {
                    if (source == null) throw new Exception(tr("فایل قابل خواندن نیست"));
                    byte[] buffer = new byte[16 * 1024];
                    int count;
                    while ((count = source.read(buffer)) != -1) {
                        cacheOut.write(buffer, 0, count);
                    }
                    cacheOut.flush();
                }
                long exactSize = tempFile.length();
                if (exactSize <= 0) throw new Exception(tr("فایل خالی است"));

                String endpoint = SUPABASE_URL + "/storage/v1/object/" + bucket + "/" + encodePath(objectPath);
                connection = (HttpURLConnection) new URL(endpoint).openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(60000);
                connection.setRequestProperty("apikey", SUPABASE_PUBLISHABLE_KEY);
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Content-Type", contentType);
                connection.setFixedLengthStreamingMode(exactSize);

                try (InputStream fileInput = new FileInputStream(tempFile);
                     OutputStream networkOutput = connection.getOutputStream()) {
                    byte[] buffer = new byte[16 * 1024];
                    int count;
                    while ((count = fileInput.read(buffer)) != -1) {
                        networkOutput.write(buffer, 0, count);
                    }
                    networkOutput.flush();
                }

                int code = connection.getResponseCode();
                if (code >= 200 && code < 300) {
                    String publicUrl = getSupabasePublicUrl(bucket, objectPath);
                    runOnUiThread(() -> callback.onSuccess(publicUrl));
                } else {
                    String body = readErrorResponse(connection);
                    String message = "HTTP " + code + " " + connection.getResponseMessage();
                    if (body != null && !body.trim().isEmpty()) message += "\n" + body;
                    final String error = message;
                    runOnUiThread(() -> callback.onError(error));
                }
            } catch (Exception e) {
                String message = e.getMessage();
                if (message == null || message.trim().isEmpty()) message = e.getClass().getSimpleName();
                final String error = message;
                runOnUiThread(() -> callback.onError(error));
            } finally {
                if (connection != null) connection.disconnect();
                if (tempFile != null && tempFile.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    tempFile.delete();
                }
            }
        }, "supabase-upload").start();
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
    if (url == null || url.trim().isEmpty() || imageView == null) return;

    final String originalUrl = url.trim();
    imageView.setTag(originalUrl);

    // عکس‌های پروفایل در باکت خصوصی هستند؛ ابتدا از تابع لینک موقت می‌گیریم.
    if (originalUrl.contains("/storage/v1/object/public/" + PROFILE_BUCKET + "/") ||
            originalUrl.contains("/storage/v1/object/" + PROFILE_BUCKET + "/")) {
        loadPrivateProfileImage(originalUrl, imageView);
        return;
    }

    downloadImageToView(originalUrl, imageView);
}

private void loadPrivateProfileImage(String storedUrl, ImageView imageView) {
    MEDIA_EXECUTOR.execute(() -> {
        File cachedProfile = getCachedMediaFile(storedUrl, "image");
        if (cachedProfile != null) {
            showChatCachedImage(cachedProfile, imageView, storedUrl);
            return;
        }
        HttpURLConnection connection = null;
        try {
            String marker = "/" + PROFILE_BUCKET + "/";
            int markerIndex = storedUrl.indexOf(marker);
            if (markerIndex < 0) {
                Log.e("ChatImage", "Profile object path not found in URL");
                return;
            }

            String encodedPath = storedUrl.substring(markerIndex + marker.length());
            int queryIndex = encodedPath.indexOf('?');
            if (queryIndex >= 0) encodedPath = encodedPath.substring(0, queryIndex);
            String objectPath = java.net.URLDecoder.decode(encodedPath, "UTF-8");

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                Log.e("ChatImage", "No signed-in Firebase user for profile image");
                return;
            }

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

            JSONObject requestBody = new JSONObject();
            requestBody.put("path", objectPath);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(requestBody.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                output.flush();
            }

            int code = connection.getResponseCode();
            InputStream responseStream = code >= 200 && code < 300
                    ? connection.getInputStream() : connection.getErrorStream();
            String responseText = "";
            if (responseStream != null) {
                java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int count;
                while ((count = responseStream.read(buffer)) != -1) bytes.write(buffer, 0, count);
                responseText = bytes.toString("UTF-8");
                responseStream.close();
            }

            if (code < 200 || code >= 300) {
                Log.e("ChatImage", "Profile URL function HTTP " + code + ": " + responseText);
                return;
            }

            String signedUrl = new JSONObject(responseText).optString("url", "");
            if (signedUrl.isEmpty()) {
                Log.e("ChatImage", "Profile URL function returned no URL: " + responseText);
                return;
            }

            downloadImageToView(signedUrl, imageView, storedUrl);
        } catch (Exception e) {
            Log.e("ChatImage", "Private profile image failed", e);
        } finally {
            if (connection != null) connection.disconnect();
        }
    });
}

// فقط برای عکس پروفایل خود کاربر در گوشه بالای صفحه:
// تصویر را با برش CENTER_CROP به یک دایره واقعی تبدیل می‌کند؛
// بنابراین حتی اگر خود فایل عکس مربع باشد، گوشه‌های مربعی دیده نمی‌شوند.
private Bitmap makeCircularProfileBitmap(Bitmap source) {
    if (source == null || source.isRecycled()) return source;

    int width = source.getWidth();
    int height = source.getHeight();
    int size = Math.min(width, height);
    if (size <= 0) return source;

    Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
    android.graphics.Canvas canvas = new android.graphics.Canvas(output);
    android.graphics.Paint paint = new android.graphics.Paint(
            android.graphics.Paint.ANTI_ALIAS_FLAG |
                    android.graphics.Paint.FILTER_BITMAP_FLAG
    );
    android.graphics.BitmapShader shader = new android.graphics.BitmapShader(
            source,
            android.graphics.Shader.TileMode.CLAMP,
            android.graphics.Shader.TileMode.CLAMP
    );

    float scale = Math.max((float) size / width, (float) size / height);
    float scaledWidth = width * scale;
    float scaledHeight = height * scale;
    float dx = (size - scaledWidth) / 2f;
    float dy = (size - scaledHeight) / 2f;
    android.graphics.Matrix matrix = new android.graphics.Matrix();
    matrix.setScale(scale, scale);
    matrix.postTranslate(dx, dy);
    shader.setLocalMatrix(matrix);
    paint.setShader(shader);

    float radius = size / 2f;
    canvas.drawCircle(radius, radius, radius, paint);
    return output;
}

private void downloadImageToView(String url, ImageView imageView) {
    downloadImageToView(url, imageView, url);
}

private void downloadImageToView(String url, ImageView imageView, String stableCacheKey) {
    if (url == null || imageView == null) return;
    Object expectedTag = imageView.getTag();
    MEDIA_EXECUTOR.execute(() -> {
        File cached = getCachedMediaFile(stableCacheKey, "image");
        if (cached == null) cached = downloadMediaToCache(url, "image", 15L * 1024L * 1024L, stableCacheKey);
        if (cached != null) showChatCachedImage(cached, imageView, expectedTag);
        else Log.w("ChatImage", "Image is not cached and could not be downloaded");
    });
}

private void showChatCachedImage(File file, ImageView imageView, Object expectedTag) {
    if (file == null || imageView == null) return;
    MEDIA_EXECUTOR.execute(() -> {
        Bitmap bitmap = null;
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
            int sample = 1;
            while (bounds.outWidth / sample > 1200 || bounds.outHeight / sample > 1600) sample *= 2;
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sample;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        } catch (Exception e) {
            Log.w("ChatImage", "Cached image decode failed", e);
        }
        final Bitmap decoded = bitmap;
        if (decoded != null) runOnUiThread(() -> {
            if (isFinishing() || (expectedTag != null && !expectedTag.equals(imageView.getTag()))) {
                if (!decoded.isRecycled()) decoded.recycle();
                return;
            }
            Bitmap shown = imageView == myHeaderProfileButton ? makeCircularProfileBitmap(decoded) : decoded;
            if (imageView == myHeaderProfileButton) {
                imageView.setPadding(0, 0, 0, 0);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            }
            imageView.setImageBitmap(shown);
            imageView.setAlpha(1f);
            if (shown != decoded && !decoded.isRecycled()) decoded.recycle();
        });
    });
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

    private void saveImageToGallery(String url, String fileName) {
        new Thread(() -> {
            HttpURLConnection imageConnection = null;
            Uri imageUri = null;
            try {
                if (url == null || url.trim().isEmpty()) {
                    throw new Exception("آدرس عکس خالی است");
                }

                String downloadUrl = url.trim();

                // عکس پروفایل در باکت خصوصی است؛ برای آن لینک امضاشده بگیر.
                if (downloadUrl.contains("/storage/v1/object/public/" + PROFILE_BUCKET + "/") ||
                        downloadUrl.contains("/storage/v1/object/" + PROFILE_BUCKET + "/")) {
                    String marker = "/" + PROFILE_BUCKET + "/";
                    int markerIndex = downloadUrl.indexOf(marker);
                    if (markerIndex < 0) {
                        throw new Exception("مسیر عکس پروفایل در آدرس پیدا نشد");
                    }

                    String objectPath = downloadUrl.substring(markerIndex + marker.length());
                    int queryIndex = objectPath.indexOf('?');
                    if (queryIndex >= 0) objectPath = objectPath.substring(0, queryIndex);
                    objectPath = java.net.URLDecoder.decode(objectPath, "UTF-8");

                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (currentUser == null) {
                        throw new Exception("برای دریافت عکس باید وارد حساب شوید");
                    }

                    String idToken = com.google.android.gms.tasks.Tasks.await(
                            currentUser.getIdToken(false)).getToken();
                    if (idToken == null || idToken.trim().isEmpty()) {
                        throw new Exception("توکن ورود Firebase دریافت نشد");
                    }

                    HttpURLConnection functionConnection = null;
                    try {
                        URL functionUrl = new URL(SUPABASE_URL + "/functions/v1/profile-photo-url");
                        functionConnection = (HttpURLConnection) functionUrl.openConnection();
                        functionConnection.setRequestMethod("POST");
                        functionConnection.setDoOutput(true);
                        functionConnection.setConnectTimeout(20000);
                        functionConnection.setReadTimeout(30000);
                        functionConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                        functionConnection.setRequestProperty("apikey", SUPABASE_PUBLISHABLE_KEY);
                        functionConnection.setRequestProperty("Authorization", "Bearer " + idToken);

                        JSONObject body = new JSONObject();
                        body.put("path", objectPath);
                        try (OutputStream out = functionConnection.getOutputStream()) {
                            out.write(body.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        }

                        int functionCode = functionConnection.getResponseCode();
                        InputStream responseStream = functionCode >= 200 && functionCode < 300
                                ? functionConnection.getInputStream() : functionConnection.getErrorStream();
                        String responseText = "";
                        if (responseStream != null) {
                            try (InputStream in = responseStream;
                                 java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream()) {
                                byte[] buffer = new byte[4096];
                                int count;
                                while ((count = in.read(buffer)) != -1) bytes.write(buffer, 0, count);
                                responseText = bytes.toString("UTF-8");
                            }
                        }
                        if (functionCode < 200 || functionCode >= 300) {
                            throw new Exception("HTTP " + functionCode + " از تابع عکس: " + responseText);
                        }

                        JSONObject response = new JSONObject(responseText);
                        downloadUrl = response.optString("url", "");
                        if (downloadUrl.isEmpty()) {
                            throw new Exception("تابع عکس لینک امضاشده برنگرداند: " + responseText);
                        }
                    } finally {
                        if (functionConnection != null) functionConnection.disconnect();
                    }
                }

                URL imageUrl = new URL(downloadUrl);
                imageConnection = (HttpURLConnection) imageUrl.openConnection();
                imageConnection.setRequestMethod("GET");
                imageConnection.setConnectTimeout(20000);
                imageConnection.setReadTimeout(30000);
                imageConnection.setInstanceFollowRedirects(true);
                imageConnection.setRequestProperty("Accept", "image/*");
                // لینک امضاشده خودش مجوز دارد؛ کلید عمومی را به‌عنوان Bearer نفرست.
                imageConnection.connect();

                int imageCode = imageConnection.getResponseCode();
                if (imageCode < 200 || imageCode >= 300) {
                    String serverMessage = "";
                    InputStream errorStream = imageConnection.getErrorStream();
                    if (errorStream != null) {
                        try (InputStream in = errorStream;
                             java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream()) {
                            byte[] buffer = new byte[2048];
                            int count;
                            while ((count = in.read(buffer)) != -1) bytes.write(buffer, 0, count);
                            serverMessage = bytes.toString("UTF-8");
                        }
                    }
                    throw new Exception("HTTP " + imageCode + (serverMessage.isEmpty() ? "" : ": " + serverMessage));
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                    values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                    values.put(MediaStore.Images.Media.RELATIVE_PATH,
                            Environment.DIRECTORY_PICTURES + "/تجربه‌ها");
                    values.put(MediaStore.Images.Media.IS_PENDING, 1);

                    imageUri = getContentResolver().insert(
                            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values);
                    if (imageUri == null) throw new Exception(tr("گالری قابل دسترسی نیست"));

                    try (InputStream input = imageConnection.getInputStream();
                         OutputStream output = getContentResolver().openOutputStream(imageUri)) {
                        if (output == null) throw new Exception(tr("فضای ذخیره‌سازی باز نشد"));
                        byte[] buffer = new byte[8192];
                        int count;
                        while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                        output.flush();
                    }

                    ContentValues done = new ContentValues();
                    done.put(MediaStore.Images.Media.IS_PENDING, 0);
                    getContentResolver().update(imageUri, done, null, null);

                    runOnUiThread(() -> Toast.makeText(this,
                            tr("✅ عکس در گالری ذخیره شد"), Toast.LENGTH_SHORT).show());
                } else {
                    runOnUiThread(() -> Toast.makeText(this,
                            tr("ذخیره مستقیم در گالری در این نسخه اندروید پشتیبانی نمی‌شود"),
                            Toast.LENGTH_LONG).show());
                }
            } catch (Exception e) {
                if (imageUri != null) {
                    try { getContentResolver().delete(imageUri, null, null); } catch (Exception ignored) { }
                }
                String error = e.getMessage();
                if (error == null || error.trim().isEmpty()) error = tr("خطا در ذخیره عکس");
                final String finalError = error;
                Log.e("ChatImage", "Save profile/chat image failed", e);
                runOnUiThread(() -> Toast.makeText(this,
                        tr("خطای ذخیره عکس:\n") + finalError, Toast.LENGTH_LONG).show());
            } finally {
                if (imageConnection != null) imageConnection.disconnect();
            }
        }).start();
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
        messageViews.clear();
        dirtyMessageIds.clear();

        if (usersRoot != null) {
            root = usersRoot;
            setContentView(usersRoot);
        } else {
            createUsersScreen();
        }
        loadUsers();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        openNotificationMessage(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // وضعیت آنلاین در سطح کل برنامه توسط TajroApplication مدیریت می‌شود.
        // اینجا عمداً وضعیت را تغییر نمی‌دهیم تا جابه‌جایی بین صفحه‌ها
        // کاربر را اشتباهاً آفلاین نکند.

        String currentLanguage =
                LanguageManager.getLanguage(this);

        if (appliedLanguage != null
                && !currentLanguage.equals(appliedLanguage)) {
            recreate();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Presence lifecycle is managed globally by TajroApplication.
    }

    @Override
    protected void onDestroy() {

        removeChatListeners();
        voiceUiHandler.removeCallbacks(recordingTicker);
        audioPlaybackHandler.removeCallbacks(playbackTicker);
        if (unreadMessagesListener != null) {
            unreadMessagesListener.remove();
            unreadMessagesListener = null;
        }

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

    AlertDialog.Builder builder = new AlertDialog.Builder(this)
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
            );

    // ظاهر پنجره تنظیمات چت؛ منطق و ترتیب عملکرد گزینه‌ها بدون تغییر می‌ماند.
    AlertDialog menuDialog = builder.create();
    menuDialog.show();
    styleChatDialog(menuDialog);
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

    AlertDialog privacyDialog = new AlertDialog.Builder(this)
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
            .create();

    privacyDialog.show();
    styleChatDialog(privacyDialog);
}

private void styleChatDialog(AlertDialog dialog) {
    if (dialog == null) return;

    android.graphics.drawable.GradientDrawable panel =
            new android.graphics.drawable.GradientDrawable();
    panel.setColor(Color.rgb(18, 35, 53));
    panel.setCornerRadius(dp(20));
    panel.setStroke(dp(1), Color.rgb(48, 83, 108));

    if (dialog.getWindow() != null) {
        dialog.getWindow().setBackgroundDrawable(panel);
    }

    TextView title = dialog.findViewById(android.R.id.alertTitle);
    if (title != null) {
        title.setTextColor(Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(dp(20), dp(12), dp(20), dp(8));
    }

    android.widget.ListView list = dialog.getListView();
    if (list != null) {
        list.setBackgroundColor(Color.rgb(18, 35, 53));
        list.setDivider(new android.graphics.drawable.ColorDrawable(Color.rgb(43, 68, 88)));
        list.setDividerHeight(dp(1));
        for (int i = 0; i < list.getChildCount(); i++) {
            View child = list.getChildAt(i);
            if (child instanceof android.widget.CheckedTextView) {
                ((android.widget.CheckedTextView) child).setTextColor(Color.WHITE);
            } else if (child instanceof android.widget.TextView) {
                ((android.widget.TextView) child).setTextColor(Color.WHITE);
            }
        }
    }

    int[] buttonIds = {
            android.R.id.button1,
            android.R.id.button2,
            android.R.id.button3
    };
    for (int id : buttonIds) {
        Button button = dialog.findViewById(id);
        if (button != null) {
            button.setTextColor(Color.rgb(245, 211, 132));
            button.setAllCaps(false);
        }
    }
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

    if (currentChatId == null || currentChatId.isEmpty() ||
            myId == null || myId.isEmpty()) {
        return;
    }

    // Messages are stored in the root "messages" collection, not
    // chats/{chatId}/messages. Query each participant direction separately
    // so each query is provably permitted by the Firestore rules.
    db.collection("messages")
            .whereEqualTo("chatId", currentChatId)
            .whereEqualTo("senderId", myId)
            .get()
            .addOnSuccessListener(sentSnapshot ->
                    db.collection("messages")
                            .whereEqualTo("chatId", currentChatId)
                            .whereEqualTo("receiverId", myId)
                            .get()
                            .addOnSuccessListener(receivedSnapshot -> {
                                Map<String, DocumentSnapshot> unique = new java.util.LinkedHashMap<>();
                                for (DocumentSnapshot doc : sentSnapshot.getDocuments()) {
                                    unique.put(doc.getId(), doc);
                                }
                                for (DocumentSnapshot doc : receivedSnapshot.getDocuments()) {
                                    unique.put(doc.getId(), doc);
                                }
                                List<DocumentSnapshot> docs = new ArrayList<>(unique.values());
                                updateDeletedForMeInBatches(docs, 0);
                            })
                            .addOnFailureListener(e -> showDeleteChatAccessError(e))
            )
            .addOnFailureListener(e -> showDeleteChatAccessError(e));
}

private void showDeleteChatAccessError(Exception error) {
    Log.e("ChatActivity", "Could not read chat messages for delete-for-me", error);
    Toast.makeText(
            ChatActivity.this,
            tr("دسترسی به پیام‌های چت ناموفق بود"),
            Toast.LENGTH_LONG
    ).show();
}

// Firestore batches are limited to 500 writes; use smaller batches safely.
private void updateDeletedForMeInBatches(
        List<DocumentSnapshot> docs,
        int startIndex
) {
    if (startIndex >= docs.size()) {
        messageCache.clear();
        messageViews.clear();
        dirtyMessageIds.clear();
        if (messagesContainer != null) {
            messagesContainer.removeAllViews();
        }
        Toast.makeText(
                ChatActivity.this,
                tr("چت از حساب شما پاک شد"),
                Toast.LENGTH_SHORT
        ).show();
        return;
    }

    int endIndex = Math.min(startIndex + 400, docs.size());
    com.google.firebase.firestore.WriteBatch batch = db.batch();
    for (int i = startIndex; i < endIndex; i++) {
        DocumentSnapshot doc = docs.get(i);
        batch.update(doc.getReference(), "deletedFor", FieldValue.arrayUnion(myId));
    }

    batch.commit()
            .addOnSuccessListener(v -> updateDeletedForMeInBatches(docs, endIndex))
            .addOnFailureListener(e -> {
                Log.e("ChatActivity", "Could not mark chat messages deleted for me", e);
                Toast.makeText(
                        ChatActivity.this,
                        tr("پاک کردن چت ناموفق بود"),
                        Toast.LENGTH_LONG
                ).show();
            });
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

    // Apply the block locally immediately: hide online status and disable chat
    // while the Firestore write is pending.
    final String targetUid = receiverId;
    blockedUserIds.add(targetUid);
    blockedByMe = true;
    blocked = true;
    blockChecksReady = 2;
    updateBlockStateAfterCheck();

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

                        Toast.makeText(
                                ChatActivity.this,
                                tr("کاربر مسدود شد؛ پیام و صدا متوقف شد"),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
            )
            .addOnFailureListener(
                    e -> {

                        // The server rejected the block; roll back the optimistic local state.
                        blockedUserIds.remove(targetUid);
                        blockedByMe = false;
                        blocked = blockedByReceiver;
                        blockChecksReady = 2;
                        updateBlockStateAfterCheck();

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
