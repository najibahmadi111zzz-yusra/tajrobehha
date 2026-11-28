package com.tajro.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.view.ViewParent;
import android.text.util.Linkify;
import android.text.method.LinkMovementMethod;
import java.util.Locale;
import java.util.ArrayList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AIActivity extends Activity {

    private LinearLayout messagesLayout;
    private EditText questionInput;
    private ScrollView scrollView;
    private Button sendButton;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean isSending = false;

    // Supabase Edge Function configuration. The publishable key is public;
    // the OpenRouter secret remains only in Supabase Function Secrets.
    private static final String SUPABASE_URL =
            "https://gorbhuqmkjlkrklhasdh.supabase.co";
    private static final String SUPABASE_PUBLISHABLE_KEY =
            "sb_publishable_xCkd5NsZ3jcQ6IqKKzm_Lg_UtF_ToDD";
    private static final String AI_CHAT_ENDPOINT =
            SUPABASE_URL + "/functions/v1/ai-chat";

    private static final int MAX_MESSAGES = 17;
    private static final int MAX_MESSAGE_CHARS = 8000;
    private static final int MAX_TOTAL_CHARS = 24000;

    private final ArrayList<ChatMessage> conversationHistory = new ArrayList<>();
    private SharedPreferences historyPreferences;
    private String activeChatId = "";
    private String activeChatTitle = "";
    private static final String HISTORY_KEY = "saved_ai_conversations_v1";
    private static final int MAX_SAVED_CHATS = 40;

    private static final class ChatMessage {
        final String role;
        final String content;

        ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    private int primary = Color.rgb(12, 91, 120);
    private int darkBlue = Color.rgb(5, 43, 70);
    private int background = Color.rgb(235, 248, 250);
    private int textColor = Color.rgb(28, 48, 62);
    private int surfaceColor = Color.WHITE;
    private int inputColor = Color.rgb(248, 252, 255);
    private int borderColor = Color.rgb(205, 231, 239);
    private int secondaryTextColor = Color.rgb(10, 115, 153);
    private int userBubbleStart = Color.rgb(202, 239, 250);
    private int userBubbleEnd = Color.rgb(220, 246, 251);
    private int assistantBubbleStart = Color.WHITE;
    private int assistantBubbleEnd = Color.rgb(248, 253, 255);
    private int appliedThemeColor;
    private boolean appliedNightMode;

    private void applyAssistantTheme() {
        appliedThemeColor = ThemeManager.getThemeColor(this);
        appliedNightMode = ThemeManager.isNightMode(this);
        primary = appliedThemeColor;
        darkBlue = Color.rgb(
                (int) (Color.red(primary) * 0.48f),
                (int) (Color.green(primary) * 0.48f),
                (int) (Color.blue(primary) * 0.48f));

        if (appliedNightMode) {
            background = Color.rgb(18, 25, 33);
            textColor = Color.rgb(235, 241, 247);
            surfaceColor = Color.rgb(28, 38, 49);
            inputColor = Color.rgb(37, 49, 62);
            borderColor = Color.rgb(63, 81, 98);
            secondaryTextColor = Color.rgb(157, 207, 226);
            userBubbleStart = Color.rgb(34, 73, 91);
            userBubbleEnd = Color.rgb(39, 83, 101);
            assistantBubbleStart = Color.rgb(30, 42, 54);
            assistantBubbleEnd = Color.rgb(35, 48, 61);
        } else {
            background = Color.rgb(235, 248, 250);
            textColor = Color.rgb(28, 48, 62);
            surfaceColor = Color.WHITE;
            inputColor = Color.rgb(248, 252, 255);
            borderColor = Color.rgb(205, 231, 239);
            secondaryTextColor = Color.rgb(10, 115, 153);
            userBubbleStart = Color.rgb(202, 239, 250);
            userBubbleEnd = Color.rgb(220, 246, 251);
            assistantBubbleStart = Color.WHITE;
            assistantBubbleEnd = Color.rgb(248, 253, 255);
        }
    }

    private String languageCode() {
        return LanguageManager.getLanguage(this);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageManager.applyLanguage(newBase));
    }

    private String tr(String key) {
        String lang = languageCode();
        String[] values;
        switch (key) {
            case "welcome": values = new String[]{
                    "سلام! 🌟 من دستیار هوشمند تجربه‌ها هستم.\nدربارهٔ آموزش، تکنولوژی، زندگی روزمره و موضوعات گوناگون سؤال بپرس. تلاش می‌کنم پاسخ روشن و مفیدی بدهم.",
                    "Hello! 🌟 I am TajroBehha's AI assistant.\nAsk me about education, technology, daily life, and many other topics. I'll do my best to give clear and useful answers.",
                    "سلام! 🌟 زه د تجربه‌ها هوښیار مرستیال یم.\nد زده‌کړو، ټکنالوژۍ، ورځني ژوند او بېلابېلو موضوعاتو په اړه پوښتنې وکړئ.",
                    "السلام علیکم! 🌟 میں تجربہ‌ها کا ذہین معاون ہوں۔\nتعلیم، ٹیکنالوجی، روزمرہ زندگی اور مختلف موضوعات کے بارے میں سوال پوچھیں۔",
                    "नमस्ते! 🌟 मैं TajroBehha का स्मार्ट सहायक हूँ।\nशिक्षा, तकनीक, रोज़मर्रा की ज़िंदगी और अन्य विषयों पर सवाल पूछें।",
                    "مرحباً! 🌟 أنا المساعد الذكي لتجربه‌ها.\nاسألني عن التعليم والتكنولوجيا والحياة اليومية وموضوعات متنوعة، وسأحاول تقديم إجابة واضحة ومفيدة."
            }; break;
            case "title": values = new String[]{"دستیار هوشمند", "AI Assistant", "هوښیار مرستیال", "ذہین معاون", "स्मार्ट सहायक", "المساعد الذكي"}; break;
            case "subtitle": values = new String[]{"همراه دانای شما", "Your knowledgeable companion", "ستاسو پوه ملګری", "آپ کا باخبر ساتھی", "आपका जानकार साथी", "رفيقك المعرفي"}; break;
            case "menu": values = new String[]{"منوی دستیار", "Assistant menu", "د مرستیال غورنۍ", "معاون کا مینو", "सहायक मेनू", "قائمة المساعد"}; break;
            case "welcome_title": values = new String[]{"✨  بپرس، یاد بگیر و تجربه کن\nسؤالت را بنویس؛ من برای پاسخ‌گویی آماده‌ام.", "✨ Ask, learn, and explore\nWrite your question; I'm ready to help.", "✨ وپوښتئ، زده کړه وکړئ او تجربه ترلاسه کړئ\nخپله پوښتنه ولیکئ.", "✨ پوچھیں، سیکھیں اور تجربہ کریں\nاپنا سوال لکھیں، میں تیار ہوں۔", "✨ पूछें, सीखें और अनुभव करें\nअपना सवाल लिखें।", "✨ اسأل وتعلّم واكتشف\nاكتب سؤالك، أنا مستعد للمساعدة."}; break;
            case "hint": values = new String[]{"پیام خود را بنویسید...", "Write your message...", "خپل پیغام ولیکئ...", "اپنا پیغام لکھیں...", "अपना संदेश लिखें...", "اكتب رسالتك..."}; break;
            case "new_chat": values = new String[]{"گفتگوی جدید", "New chat", "نوې خبرې اترې", "نئی گفتگو", "नई बातचीत", "محادثة جديدة"}; break;
            case "clear_chat": values = new String[]{"پاک‌کردن گفتگو", "Clear chat", "خبرې اترې پاکول", "گفتگو صاف کریں", "बातचीत साफ़ करें", "مسح المحادثة"}; break;
            case "about": values = new String[]{"درباره دستیار", "About assistant", "د مرستیال په اړه", "معاون کے بارے میں", "सहायक के बारे में", "حول المساعد"}; break;
            case "wait": values = new String[]{"لطفاً صبر کنید تا پاسخ فعلی دریافت شود.", "Please wait for the current answer.", "مهرباني وکړئ د اوسني ځواب لپاره انتظار وکړئ.", "براہِ کرم موجودہ جواب کا انتظار کریں۔", "कृपया मौजूदा जवाब की प्रतीक्षा करें।", "يرجى الانتظار حتى يكتمل الرد الحالي."}; break;
            case "new_started": values = new String[]{"گفتگوی جدید آغاز شد", "New chat started", "نوې خبرې اترې پیل شوې", "نئی گفتگو شروع ہو گئی", "नई बातचीत शुरू हुई", "بدأت محادثة جديدة"}; break;
            case "clear_confirm": values = new String[]{"آیا می‌خواهید پیام‌های این گفتگو پاک شوند؟", "Do you want to clear this conversation?", "ایا غواړئ دا خبرې اترې پاکې کړئ؟", "کیا آپ یہ گفتگو صاف کرنا چاہتے ہیں؟", "क्या आप इस बातचीत को साफ़ करना चाहते हैं?", "هل تريد مسح رسائل هذه المحادثة؟"}; break;
            case "cancel": values = new String[]{"انصراف", "Cancel", "لغوه", "منسوخ", "रद्द करें", "إلغاء"}; break;
            case "clear": values = new String[]{"پاک کن", "Clear", "پاکول", "صاف کریں", "साफ़ करें", "امسح"}; break;
            case "close": values = new String[]{"بستن", "Close", "بندول", "بند کریں", "बंद करें", "إغلاق"}; break;
            case "cleared_msg": values = new String[]{"گفتگو پاک شد. 🌟 هر سؤال تازه‌ای داری بپرس.", "Chat cleared. 🌟 Ask me anything new.", "خبرې اترې پاکې شوې. 🌟 نوې پوښتنه وکړئ.", "گفتگو صاف ہو گئی۔ 🌟 کوئی نیا سوال پوچھیں۔", "बातचीत साफ़ हो गई। 🌟 नया सवाल पूछें।", "تم مسح المحادثة. 🌟 اسألني أي سؤال جديد."}; break;
            case "cleared": values = new String[]{"گفتگو پاک شد", "Chat cleared", "خبرې اترې پاکې شوې", "گفتگو صاف ہو گئی", "बातचीत साफ़ हुई", "تم مسح المحادثة"}; break;
            case "about_msg": values = new String[]{"دستیار هوشمند تجربه‌ها\n\nهمراه شما برای پرسش، یادگیری و دریافت پاسخ درباره موضوعات گوناگون.", "TajroBehha AI Assistant\n\nHere to help you ask, learn, and get answers on many topics.", "د تجربه‌ها هوښیار مرستیال\n\nستاسو د پوښتنو او زده‌کړو ملګری.", "تجربہ‌ها کا ذہین معاون\n\nمختلف موضوعات پر سوالات اور سیکھنے میں آپ کا ساتھی۔", "TajroBehha स्मार्ट सहायक\n\nविभिन्न विषयों पर सवालों और सीखने में आपका साथी।", "المساعد الذكي لتجربه‌ها\n\nرفيقك في السؤال والتعلّم والحصول على إجابات في موضوعات متنوعة."}; break;
            case "empty": values = new String[]{"لطفاً سؤال خود را بنویسید", "Please write your question", "مهرباني وکړئ خپله پوښتنه ولیکئ", "براہِ کرم اپنا سوال لکھیں", "कृपया अपना सवाल लिखें", "يرجى كتابة سؤالك"}; break;
            case "thinking": values = new String[]{"در حال فکر کردن... ✨", "Thinking... ✨", "فکر کوم... ✨", "سوچ رہا ہوں... ✨", "सोच रहा हूँ... ✨", "أفكر... ✨"}; break;
            case "login_required": values = new String[]{
                    "برای استفاده از دستیار، ابتدا وارد حساب خود شوید.",
                    "Please sign in to use the AI assistant.",
                    "د مرستیال کارولو لپاره لومړی خپل حساب ته ننوځئ.",
                    "ذہین معاون استعمال کرنے کے لیے پہلے اپنے اکاؤنٹ میں سائن اِن کریں۔",
                    "AI सहायक का उपयोग करने के लिए पहले अपने खाते में साइन इन करें।",
                    "يرجى تسجيل الدخول أولاً لاستخدام المساعد الذكي."
            }; break;
            case "question_too_long": values = new String[]{
                    "متن هر پیام نباید بیشتر از ۸۰۰۰ نویسه باشد.",
                    "Each message must be 8,000 characters or fewer.",
                    "د هر پیغام اوږدوالی باید تر ۸۰۰۰ تورو زیات نه وي.",
                    "ہر پیغام 8,000 حروف سے زیادہ نہیں ہونا چاہیے۔",
                    "हर संदेश 8,000 अक्षरों से अधिक नहीं होना चाहिए।",
                    "يجب ألا يتجاوز طول كل رسالة 8000 حرف."
            }; break;
            case "not_configured": values = new String[]{"کلید OpenRouter در برنامه تنظیم نشده است.", "OpenRouter key is not configured in the app.", "د OpenRouter کیلي په اپ کې نه ده ټاکل شوې.", "ایپ میں OpenRouter کلید سیٹ نہیں ہے۔", "ऐप में OpenRouter कुंजी सेट नहीं है।", "مفتاح OpenRouter غير مُعدّ في التطبيق."}; break;
            case "no_answer": values = new String[]{"پاسخی از مدل دریافت نشد.", "No response was received from the model.", "له ماډل څخه ځواب ترلاسه نه شو.", "ماڈل سے جواب موصول نہیں ہوا۔", "मॉडल से कोई जवाब नहीं मिला।", "لم يتم استلام رد من النموذج."}; break;
            case "empty_answer": values = new String[]{"پاسخ مدل خالی بود.", "The model returned an empty answer.", "د ماډل ځواب تش و.", "ماڈل کا جواب خالی تھا۔", "मॉडल का जवाब खाली था।", "كان رد النموذج فارغاً."}; break;
            case "sorry": values = new String[]{"متأسفم، پاسخ دریافت نشد.", "Sorry, the answer could not be retrieved.", "بښنه غواړم، ځواب ترلاسه نه شو.", "معذرت، جواب موصول نہیں ہو سکا۔", "क्षमा करें, जवाब प्राप्त नहीं हुआ।", "عذراً، تعذّر الحصول على الرد."}; break;
            case "unknown_error": values = new String[]{"خطای نامشخص: ", "Unknown error: ", "نامعلومه تېروتنه: ", "نامعلومہ خرابی: ", "अज्ञात त्रुटि: ", "خطأ غير معروف: "}; break;
            case "you": values = new String[]{"شما", "You", "تاسو", "آپ", "आप", "أنت"}; break;
            case "assistant": values = new String[]{"✦ دستیار هوشمند", "✦ AI Assistant", "✦ هوښیار مرستیال", "✦ ذہین معاون", "✦ स्मार्ट सहायक", "✦ المساعد الذكي"}; break;
            case "copy": values = new String[]{"▣  کپی پاسخ", "▣  Copy answer", "▣  ځواب کاپي", "▣  جواب کاپی کریں", "▣  जवाब कॉपी करें", "▣  نسخ الرد"}; break;
            case "copy_label": values = new String[]{"پاسخ دستیار", "Assistant answer", "د مرستیال ځواب", "معاون کا جواب", "सहायक का जवाब", "رد المساعد"}; break;
            case "copied": values = new String[]{"پاسخ کپی شد", "Answer copied", "ځواب کاپي شو", "جواب کاپی ہو گیا", "जवाब कॉपी हुआ", "تم نسخ الرد"}; break;
            case "history": values = new String[]{"گفتگوهای قبلی", "Chat history", "د خبرو اترو مخینه", "گفتگوؤں کی فہرست", "बातचीत का इतिहास", "سجل المحادثات"}; break;
            case "history_empty": values = new String[]{"هنوز گفتگویی ذخیره نشده است.", "No saved conversations yet.", "تر اوسه کومې خبرې نه دي ساتل شوې.", "ابھی کوئی گفتگو محفوظ نہیں ہے۔", "अभी कोई बातचीत सहेजी नहीं गई है।", "لا توجد محادثات محفوظة بعد."}; break;
            case "rename": values = new String[]{"تغییر نام", "Rename", "نوم بدلول", "نام تبدیل کریں", "नाम बदलें", "إعادة التسمية"}; break;
            case "delete": values = new String[]{"حذف", "Delete", "ړنګول", "حذف کریں", "हटाएँ", "حذف"}; break;
            case "rename_hint": values = new String[]{"نام گفتگو", "Conversation name", "د خبرو نوم", "گفتگو کا نام", "बातचीत का नाम", "اسم المحادثة"}; break;
            case "deleted": values = new String[]{"گفتگو حذف شد", "Conversation deleted", "خبرې اترې ړنګې شوې", "گفتگو حذف ہو گئی", "बातचीत हटा दी गई", "تم حذف المحادثة"}; break;
            case "history_saved": values = new String[]{"گفتگو در این دستگاه ذخیره شد", "Conversation saved on this device", "خبرې اترې په دې وسیله کې وساتل شوې", "گفتگو اس ڈیوائس پر محفوظ ہوئی", "बातचीत इस डिवाइस पर सहेजी गई", "حُفظت المحادثة على هذا الجهاز"}; break;
            case "delete_confirm": values = new String[]{"این گفتگو حذف شود؟", "Delete this conversation?", "دا خبرې اترې ړنګې شي؟", "کیا یہ گفتگو حذف کر دی جائے؟", "क्या यह बातचीत हटाई जाए?", "هل تريد حذف هذه المحادثة؟"}; break;
            case "save_error": values = new String[]{"ذخیره گفتگو انجام نشد.", "Could not save the conversation.", "خبرې اترې ونه ساتل شوې.", "گفتگو محفوظ نہیں ہو سکی۔", "बातचीत सहेजी नहीं जा सकी।", "تعذر حفظ المحادثة."}; break;
            default: values = new String[]{key,key,key,key,key,key};
        }
        int index = "en".equals(lang) ? 1 : "ps".equals(lang) ? 2 : "ur".equals(lang) ? 3 : "hi".equals(lang) ? 4 : "ar".equals(lang) ? 5 : 0;
        return values[index];
    }

    private String getWelcomeMessage() { return tr("welcome"); }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyAssistantTheme();

        getWindow().setStatusBarColor(darkBlue);
        getWindow().setNavigationBarColor(background);
        getWindow().getDecorView().setSystemUiVisibility(
                appliedNightMode ? 0 : View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        );

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setLayoutDirection("ar".equals(languageCode()) || "ur".equals(languageCode()) ||
                "ps".equals(languageCode()) || "fa".equals(languageCode())
                ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        main.setBackgroundColor(background);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(13), dp(16), dp(13));
        header.setBackground(createBackground(new int[]{darkBlue, primary}, 0, 0));

        TextView backButton = new TextView(this);
        backButton.setText("‹");
        backButton.setTextSize(38);
        backButton.setTextColor(Color.WHITE);
        backButton.setGravity(Gravity.CENTER);
        header.addView(backButton, new LinearLayout.LayoutParams(dp(40), dp(48)));
        backButton.setOnClickListener(v -> finish());

        TextView robotIcon = new TextView(this);
        robotIcon.setText("✦");
        robotIcon.setTextSize(30);
        robotIcon.setTextColor(Color.WHITE);
        robotIcon.setGravity(Gravity.CENTER);
        robotIcon.setBackground(createBackground(
                new int[]{primary, darkBlue},
                appliedNightMode ? Color.rgb(100, 164, 190) : Color.rgb(80, 220, 255), 50));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(52), dp(52));
        iconParams.setMargins(dp(8), 0, dp(12), 0);
        header.addView(robotIcon, iconParams);

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(this);
        title.setText(tr("title"));
        title.setTextSize(21);
        title.setTextColor(Color.WHITE);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        TextView subtitle = new TextView(this);
        subtitle.setText(tr("subtitle"));
        subtitle.setTextSize(12);
        subtitle.setTextColor(Color.rgb(160, 238, 237));
        titleBox.addView(title);
        titleBox.addView(subtitle);
        header.addView(titleBox, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView menuIcon = new TextView(this);
        menuIcon.setText("⋮");
        menuIcon.setTextSize(28);
        menuIcon.setTextColor(Color.WHITE);
        menuIcon.setGravity(Gravity.CENTER);
        menuIcon.setContentDescription(tr("menu"));
        header.addView(menuIcon, new LinearLayout.LayoutParams(dp(40), dp(48)));
        menuIcon.setOnClickListener(v -> showAssistantMenu(menuIcon));
        main.addView(header);

        TextView welcome = new TextView(this);
        welcome.setText(tr("welcome_title"));
        welcome.setTextSize(14);
        welcome.setTextColor(primary);
        welcome.setGravity(Gravity.CENTER);
        welcome.setPadding(dp(12), dp(12), dp(12), dp(12));
        main.addView(welcome);

        scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setClipToPadding(false);
        messagesLayout = new LinearLayout(this);
        messagesLayout.setOrientation(LinearLayout.VERTICAL);
        messagesLayout.setLayoutDirection(main.getLayoutDirection());
        messagesLayout.setPadding(dp(12), dp(8), dp(12), dp(12));
        scrollView.addView(messagesLayout, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        main.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        bottom.setPadding(dp(10), dp(9), dp(10), dp(10));
        bottom.setBackgroundColor(surfaceColor);

        questionInput = new EditText(this);
        questionInput.setHint(tr("hint"));
        questionInput.setTextDirection("ar".equals(languageCode()) || "ur".equals(languageCode()) ||
                "ps".equals(languageCode()) || "fa".equals(languageCode())
                ? View.TEXT_DIRECTION_RTL : View.TEXT_DIRECTION_LTR);
        questionInput.setLayoutDirection(main.getLayoutDirection());
        questionInput.setTextSize(16);
        questionInput.setTextColor(textColor);
        questionInput.setHintTextColor(appliedNightMode
                ? Color.rgb(166, 180, 193) : Color.GRAY);
        questionInput.setPadding(dp(15), dp(10), dp(15), dp(10));
        questionInput.setMinHeight(dp(50));
        questionInput.setMaxLines(4);
        questionInput.setSingleLine(false);
        questionInput.setImeOptions(EditorInfo.IME_ACTION_SEND);
        questionInput.setBackground(createBackground(
                new int[]{inputColor, inputColor}, borderColor, 24));
        bottom.addView(questionInput, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        sendButton = new Button(this);
        sendButton.setText("➤");
        sendButton.setTextSize(23);
        sendButton.setTextColor(Color.WHITE);
        sendButton.setAllCaps(false);
        sendButton.setMinWidth(0);
        sendButton.setMinimumWidth(0);
        sendButton.setPadding(0, 0, 0, 0);
        sendButton.setBackground(createBackground(new int[]{primary, darkBlue}, 0, 50));
        LinearLayout.LayoutParams sendParams = new LinearLayout.LayoutParams(dp(54), dp(54));
        sendParams.setMargins(dp(8), 0, 0, 0);
        bottom.addView(sendButton, sendParams);
        main.addView(bottom);

        setContentView(main);
        initializeLocalHistory();
        if (savedInstanceState != null) {
            activeChatId = savedInstanceState.getString("ai_active_chat_id", activeChatId);
            activeChatTitle = savedInstanceState.getString("ai_active_chat_title", activeChatTitle);
        }
        if (savedInstanceState != null) {
            restoreConversationHistory(
                    savedInstanceState.getString("ai_conversation_history", ""));
        }
        if (savedInstanceState != null &&
                savedInstanceState.containsKey("ai_messages")) {
            ArrayList<String> savedMessages =
                    savedInstanceState.getStringArrayList("ai_messages");
            boolean[] savedSenders =
                    savedInstanceState.getBooleanArray("ai_senders");
            boolean[] savedCopyFlags =
                    savedInstanceState.getBooleanArray("ai_copy_flags");
            if (savedMessages != null && savedSenders != null &&
                    savedCopyFlags != null && savedMessages.size() == savedSenders.length &&
                    savedMessages.size() == savedCopyFlags.length) {
                for (int i = 0; i < savedMessages.size(); i++) {
                    addMessage(savedSenders[i], savedMessages.get(i),
                            savedCopyFlags[i]);
                }
            } else {
                addMessage(false, getWelcomeMessage(), false);
            }
        } else {
            addMessage(false, getWelcomeMessage(), false);
        }
        if (savedInstanceState != null) {
            questionInput.setText(savedInstanceState.getString("ai_input", ""));
        }
        sendButton.setOnClickListener(v -> sendQuestion());
        questionInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendQuestion();
                return true;
            }
            return false;
        });
    }

    private void showAssistantMenu(View anchor) {
        PopupMenu popup = new PopupMenu(AIActivity.this, anchor);
        popup.getMenu().add(0, 1, 0, tr("new_chat"));
        popup.getMenu().add(0, 2, 1, tr("clear_chat"));
        popup.getMenu().add(0, 3, 2, tr("about"));
        popup.getMenu().add(0, 4, 3, tr("history"));

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 1) {
                if (isSending) {
                    Toast.makeText(this, tr("wait"),
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
                saveActiveConversation();
                messagesLayout.removeAllViews();
                conversationHistory.clear();
                activeChatId = String.valueOf(System.currentTimeMillis());
                activeChatTitle = "";
                addMessage(false, getWelcomeMessage(), false);
                questionInput.setText("");
                Toast.makeText(this, tr("new_started"), Toast.LENGTH_SHORT).show();
                return true;
            }
            if (id == 2) {
                new AlertDialog.Builder(AIActivity.this)
                        .setTitle(tr("clear_chat"))
                        .setMessage(tr("clear_confirm"))
                        .setNegativeButton(tr("cancel"), (dialog, which) -> dialog.dismiss())
                        .setPositiveButton(tr("clear"), (dialog, which) -> {
                            if (isSending) {
                                Toast.makeText(this,
                                        "لطفاً پس از پایان پاسخ فعلی دوباره تلاش کنید.",
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }
                            messagesLayout.removeAllViews();
                            conversationHistory.clear();
                            removeSavedConversation(activeChatId);
                            activeChatId = String.valueOf(System.currentTimeMillis());
                            activeChatTitle = "";
                            addMessage(false, tr("cleared_msg"), false);
                            Toast.makeText(this, tr("cleared"), Toast.LENGTH_SHORT).show();
                        }).show();
                return true;
            }
            if (id == 4) {
                showConversationHistory();
                return true;
            }
            if (id == 3) {
                new AlertDialog.Builder(AIActivity.this)
                        .setTitle(tr("title"))
                        .setMessage(tr("about_msg"))
                         .setPositiveButton(tr("close"), null)
                        .show();
                return true;
            }
            return false;
        });
        popup.show();
    }

    // Conversation history is stored locally per Firebase account. It is not cloud-synced.
    private void initializeLocalHistory() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        historyPreferences = getSharedPreferences("ai_history_" + user.getUid(), MODE_PRIVATE);
        if (activeChatId == null || activeChatId.isEmpty()) {
            activeChatId = String.valueOf(System.currentTimeMillis());
        }
    }

    private String makeTitle(String question) {
        String title = question == null ? "" : question.trim().replace('\n', ' ');
        if (title.length() > 42) title = title.substring(0, 42).trim() + "…";
        return title.isEmpty() ? tr("new_chat") : title;
    }

    private JSONArray readSavedChats() {
        if (historyPreferences == null) return new JSONArray();
        try {
            return new JSONArray(historyPreferences.getString(HISTORY_KEY, "[]"));
        } catch (Exception ignored) {
            return new JSONArray();
        }
    }

    private void saveActiveConversation() {
        if (historyPreferences == null || activeChatId == null || activeChatId.isEmpty()
                || conversationHistory.isEmpty()) return;
        try {
            JSONArray old = readSavedChats();
            JSONArray updated = new JSONArray();
            JSONObject current = new JSONObject();
            current.put("id", activeChatId);
            current.put("title", activeChatTitle.isEmpty() ? tr("new_chat") : activeChatTitle);
            current.put("updated", System.currentTimeMillis());
            JSONArray messages = new JSONArray();
            for (ChatMessage item : conversationHistory) {
                JSONObject entry = new JSONObject();
                entry.put("role", item.role);
                entry.put("content", item.content);
                messages.put(entry);
            }
            current.put("messages", messages);
            updated.put(current);
            for (int i = 0; i < old.length() && updated.length() < MAX_SAVED_CHATS; i++) {
                JSONObject item = old.optJSONObject(i);
                if (item == null || activeChatId.equals(item.optString("id"))) continue;
                updated.put(item);
            }
            historyPreferences.edit().putString(HISTORY_KEY, updated.toString()).apply();
        } catch (Exception e) {
            Toast.makeText(this, tr("save_error"), Toast.LENGTH_SHORT).show();
        }
    }

    private void showConversationHistory() {
        if (isSending) {
            Toast.makeText(this, tr("wait"), Toast.LENGTH_SHORT).show();
            return;
        }
        JSONArray chats = readSavedChats();
        if (chats.length() == 0) {
            new AlertDialog.Builder(this).setTitle(tr("history"))
                    .setMessage(tr("history_empty"))
                    .setPositiveButton(tr("close"), null).show();
            return;
        }
        ArrayList<JSONObject> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        for (int i = 0; i < chats.length(); i++) {
            JSONObject item = chats.optJSONObject(i);
            if (item == null) continue;
            entries.add(item);
            labels.add(item.optString("title", tr("new_chat")));
        }
        String[] options = labels.toArray(new String[0]);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(tr("history"))
                .setItems(options, (d, which) -> openSavedConversation(entries.get(which)))
                .setNegativeButton(tr("close"), null).create();
        dialog.setOnShowListener(v -> {
            android.widget.ListView list = dialog.getListView();
            list.setOnItemLongClickListener((parent, view, position, id) -> {
                showConversationActions(entries.get(position));
                dialog.dismiss();
                return true;
            });
        });
        dialog.show();
    }

    private void openSavedConversation(JSONObject saved) {
        if (isSending || saved == null) return;
        try {
            saveActiveConversation();
            activeChatId = saved.optString("id", String.valueOf(System.currentTimeMillis()));
            activeChatTitle = saved.optString("title", tr("new_chat"));
            conversationHistory.clear();
            JSONArray messages = saved.optJSONArray("messages");
            if (messages != null) {
                int start = Math.max(0, messages.length() - MAX_MESSAGES);
                for (int i = start; i < messages.length(); i++) {
                    JSONObject item = messages.optJSONObject(i);
                    if (item == null) continue;
                    String role = item.optString("role", "");
                    String content = item.optString("content", "");
                    if (("user".equals(role) || "assistant".equals(role))
                            && !content.trim().isEmpty() && content.length() <= MAX_MESSAGE_CHARS) {
                        conversationHistory.add(new ChatMessage(role, content));
                    }
                }
            }
            messagesLayout.removeAllViews();
            if (conversationHistory.isEmpty()) addMessage(false, getWelcomeMessage(), false);
            else for (ChatMessage item : conversationHistory) {
                addMessage("user".equals(item.role), item.content, "assistant".equals(item.role));
            }
            questionInput.setText("");
        } catch (Exception ignored) {
            Toast.makeText(this, tr("save_error"), Toast.LENGTH_SHORT).show();
        }
    }

    private void showConversationActions(JSONObject saved) {
        String[] actions = {tr("rename"), tr("delete")};
        new AlertDialog.Builder(this).setTitle(saved.optString("title", tr("history")))
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) renameSavedConversation(saved);
                    else confirmDeleteConversation(saved);
                }).show();
    }

    private void renameSavedConversation(JSONObject saved) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(saved.optString("title", ""));
        input.setHint(tr("rename_hint"));
        new AlertDialog.Builder(this).setTitle(tr("rename")).setView(input)
                .setNegativeButton(tr("cancel"), null)
                .setPositiveButton(tr("rename"), (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) name = tr("new_chat");
                    if (name.length() > 60) name = name.substring(0, 60);
                    try {
                        saved.put("title", name);
                        if (activeChatId.equals(saved.optString("id"))) activeChatTitle = name;
                        replaceSavedChat(saved);
                    } catch (Exception ignored) {
                        Toast.makeText(this, tr("save_error"), Toast.LENGTH_SHORT).show();
                    }
                }).show();
    }

    private void confirmDeleteConversation(JSONObject saved) {
        new AlertDialog.Builder(this).setTitle(tr("delete"))
                .setMessage(tr("delete_confirm"))
                .setNegativeButton(tr("cancel"), null)
                .setPositiveButton(tr("delete"), (d, w) -> {
                    JSONArray old = readSavedChats();
                    JSONArray updated = new JSONArray();
                    String id = saved.optString("id", "");
                    for (int i = 0; i < old.length(); i++) {
                        JSONObject item = old.optJSONObject(i);
                        if (item != null && !id.equals(item.optString("id"))) updated.put(item);
                    }
                    if (historyPreferences != null) historyPreferences.edit()
                            .putString(HISTORY_KEY, updated.toString()).apply();
                    if (id.equals(activeChatId)) {
                        activeChatId = String.valueOf(System.currentTimeMillis());
                        activeChatTitle = "";
                        conversationHistory.clear();
                        messagesLayout.removeAllViews();
                        addMessage(false, getWelcomeMessage(), false);
                    }
                    Toast.makeText(this, tr("deleted"), Toast.LENGTH_SHORT).show();
                }).show();
    }

    private void removeSavedConversation(String chatId) {
        if (historyPreferences == null || chatId == null) return;
        JSONArray old = readSavedChats();
        JSONArray updated = new JSONArray();
        for (int i = 0; i < old.length(); i++) {
            JSONObject item = old.optJSONObject(i);
            if (item != null && !chatId.equals(item.optString("id"))) updated.put(item);
        }
        historyPreferences.edit().putString(HISTORY_KEY, updated.toString()).apply();
    }

    private void replaceSavedChat(JSONObject changed) {
        JSONArray old = readSavedChats();
        JSONArray updated = new JSONArray();
        String id = changed.optString("id", "");
        for (int i = 0; i < old.length(); i++) {
            JSONObject item = old.optJSONObject(i);
            if (item == null) continue;
            updated.put(id.equals(item.optString("id")) ? changed : item);
        }
        if (historyPreferences != null) historyPreferences.edit()
                .putString(HISTORY_KEY, updated.toString()).apply();
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable createBackground(int[] colors, int strokeColor, int radius) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, colors);
        drawable.setCornerRadius(dp(radius));
        if (strokeColor != 0) {
            drawable.setStroke(dp(1), strokeColor);
        }
        return drawable;
    }

    private void sendQuestion() {
        if (isSending) return;

        final String question = questionInput.getText().toString().trim();
        if (question.isEmpty()) {
            Toast.makeText(this, tr("empty"), Toast.LENGTH_SHORT).show();
            return;
        }
        if (question.length() > MAX_MESSAGE_CHARS) {
            Toast.makeText(this, tr("question_too_long"), Toast.LENGTH_LONG).show();
            return;
        }

        // Capture the language on the UI thread; do not access Activity resources
        // from the background executor.
        final String responseLanguage = responseLanguageName(languageCode());
        final String loginRequiredMessage = tr("login_required");
        final String sorryMessage = tr("sorry");
        final String emptyAnswerMessage = tr("empty_answer");
        final String unknownErrorPrefix = tr("unknown_error");

        // Prepare a bounded snapshot of the successful conversation history.
        // The current user message is added last, as required by ai-chat.
        final ArrayList<ChatMessage> requestHistory =
                new ArrayList<>(conversationHistory);
        while (requestHistory.size() > MAX_MESSAGES - 1) {
            requestHistory.remove(0);
        }

        int totalChars = question.length();
        for (ChatMessage item : requestHistory) {
            totalChars += item.content.length();
        }
        while (!requestHistory.isEmpty() && totalChars > MAX_TOTAL_CHARS) {
            ChatMessage removed = requestHistory.remove(0);
            totalChars -= removed.content.length();
        }

        final JSONArray requestMessages = new JSONArray();
        try {
            for (ChatMessage item : requestHistory) {
                JSONObject jsonMessage = new JSONObject();
                jsonMessage.put("role", item.role);
                jsonMessage.put("content", item.content);
                requestMessages.put(jsonMessage);
            }
            JSONObject currentUserMessage = new JSONObject();
            currentUserMessage.put("role", "user");
            currentUserMessage.put("content", question);
            requestMessages.put(currentUserMessage);
        } catch (Exception e) {
            Toast.makeText(this, tr("sorry"), Toast.LENGTH_SHORT).show();
            return;
        }

        isSending = true;
        sendButton.setEnabled(false);
        sendButton.setAlpha(0.6f);
        addMessage(true, question, false);
        questionInput.setText("");
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(questionInput.getWindowToken(), 0);
        final TextView loading = addMessage(false, tr("thinking"), false);

        executor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                if (firebaseUser == null) {
                    throw new Exception(loginRequiredMessage);
                }

                String firebaseIdToken = Tasks.await(
                        firebaseUser.getIdToken(false), 30, TimeUnit.SECONDS
                ).getToken();

                if (firebaseIdToken == null || firebaseIdToken.trim().isEmpty()) {
                    throw new Exception(loginRequiredMessage);
                }

                URL url = new URL(AI_CHAT_ENDPOINT);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(60000);
                connection.setDoOutput(true);
                connection.setRequestProperty(
                        "Authorization", "Bearer " + firebaseIdToken);
                connection.setRequestProperty(
                        "apikey", SUPABASE_PUBLISHABLE_KEY);
                connection.setRequestProperty(
                        "Content-Type", "application/json; charset=UTF-8");
                connection.setRequestProperty("Accept", "application/json");

                JSONObject body = new JSONObject();
                body.put("messages", requestMessages);
                body.put("language", responseLanguage);

                byte[] data = body.toString().getBytes(StandardCharsets.UTF_8);
                connection.setFixedLengthStreamingMode(data.length);
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(data);
                    output.flush();
                }

                int responseCode = connection.getResponseCode();
                InputStream inputStream = responseCode >= 200 && responseCode < 300
                        ? connection.getInputStream() : connection.getErrorStream();

                StringBuilder response = new StringBuilder();
                if (inputStream != null) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                    }
                }

                JSONObject responseJson;
                try {
                    responseJson = new JSONObject(response.toString());
                } catch (Exception parseError) {
                    throw new Exception("HTTP " + responseCode
                            + ": " + sorryMessage);
                }

                if (responseCode < 200 || responseCode >= 300) {
                    String serverError = responseJson.optString("error", "").trim();
                    if (serverError.isEmpty()) {
                        serverError = "HTTP " + responseCode;
                    }
                    throw new Exception(serverError);
                }

                String answer = responseJson.optString("reply", "").trim();
                if (answer.isEmpty()) {
                    throw new Exception(emptyAnswerMessage);
                }

                final String finalAnswer = answer;
                runOnUiThread(() -> {
                    removeMessage(loading);

                    // Commit only successful turns to context history.
                    conversationHistory.clear();
                    conversationHistory.addAll(requestHistory);
                    conversationHistory.add(new ChatMessage("user", question));
                    conversationHistory.add(new ChatMessage("assistant", finalAnswer));
                    if (activeChatTitle.isEmpty()) activeChatTitle = makeTitle(question);
                    saveActiveConversation();

                    addMessage(false, finalAnswer, true);
                    finishSending();
                });
            } catch (Exception e) {
                String error = e.getMessage();
                if (error == null || error.trim().isEmpty()) {
                    error = unknownErrorPrefix + e.getClass().getSimpleName();
                }
                final String finalError = error;
                runOnUiThread(() -> {
                    removeMessage(loading);
                    addMessage(false, sorryMessage + "\\n" + finalError, false);
                    finishSending();
                });
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    private String responseLanguageName(String code) {
        if ("en".equals(code)) return "English";
        if ("ps".equals(code)) return "پښتو";
        if ("ur".equals(code)) return "اردو";
        if ("hi".equals(code)) return "हिन्दी";
        if ("ar".equals(code)) return "العربية";
        return "فارسی/دری";
    }

    private void restoreConversationHistory(String savedHistory) {
        conversationHistory.clear();
        if (savedHistory == null || savedHistory.trim().isEmpty()) return;

        try {
            JSONArray array = new JSONArray(savedHistory);
            int start = Math.max(0, array.length() - 18);
            for (int i = start; i < array.length(); i++) {
                JSONObject item = array.optJSONObject(i);
                if (item == null) continue;
                String role = item.optString("role", "");
                String content = item.optString("content", "").trim();
                if (!("user".equals(role) || "assistant".equals(role))
                        || content.isEmpty()
                        || content.length() > MAX_MESSAGE_CHARS) {
                    continue;
                }
                conversationHistory.add(new ChatMessage(role, content));
            }

            int totalChars = 0;
            for (ChatMessage item : conversationHistory) {
                totalChars += item.content.length();
            }
            while (!conversationHistory.isEmpty()
                    && (conversationHistory.size() > 18
                    || totalChars > MAX_TOTAL_CHARS)) {
                ChatMessage removed = conversationHistory.remove(0);
                totalChars -= removed.content.length();
            }
        } catch (Exception ignored) {
            conversationHistory.clear();
        }
    }

    private String serializeConversationHistory() {
        JSONArray array = new JSONArray();
        for (ChatMessage item : conversationHistory) {
            JSONObject json = new JSONObject();
            try {
                json.put("role", item.role);
                json.put("content", item.content);
                array.put(json);
            } catch (Exception ignored) {
                // Skip an individual malformed entry; valid entries are preserved.
            }
        }
        return array.toString();
    }

    private void finishSending() {
        isSending = false;
        sendButton.setEnabled(true);
        sendButton.setAlpha(1f);
        recreateIfThemeChanged();
    }

    private TextView addMessage(boolean isUser, String message, boolean allowCopy) {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setGravity(isUser ? Gravity.END : Gravity.START);
        LinearLayout.LayoutParams wrapperParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        wrapperParams.setMargins(0, dp(5), 0, dp(7));
        messagesLayout.addView(wrapper, wrapperParams);

        LinearLayout bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setPadding(dp(14), dp(12), dp(14), dp(10));
        bubble.setBackground(createBackground(
                isUser
                        ? new int[]{userBubbleStart, userBubbleEnd}
                        : new int[]{assistantBubbleStart, assistantBubbleEnd},
                borderColor, 20));
        LinearLayout.LayoutParams bubbleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bubbleParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        bubbleParams.setMargins(isUser ? dp(24) : 0, 0, isUser ? 0 : dp(24), 0);
        wrapper.addView(bubble, bubbleParams);

        TextView sender = new TextView(this);
        sender.setText(isUser ? tr("you") : tr("assistant"));
        sender.setTextSize(13);
        sender.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        sender.setTextColor(isUser ? primary : secondaryTextColor);
        sender.setGravity(Gravity.RIGHT);
        bubble.addView(sender);

        TextView text = new TextView(this);
        text.setText(message);
        text.setTextSize(16);
        text.setTextColor(textColor);
        text.setGravity(Gravity.RIGHT);
        text.setTextIsSelectable(true);
        Linkify.addLinks(text, Linkify.WEB_URLS);
        text.setMovementMethod(LinkMovementMethod.getInstance());
        text.setLineSpacing(dp(3), 1.0f);
        text.setPadding(0, dp(8), 0, dp(4));
        bubble.addView(text);

        if (allowCopy) {
            Button copyButton = new Button(this);
            copyButton.setText(tr("copy"));
            copyButton.setTextSize(12);
            copyButton.setTextColor(primary);
            copyButton.setAllCaps(false);
            copyButton.setMinHeight(0);
            copyButton.setMinimumHeight(0);
            copyButton.setPadding(dp(10), dp(2), dp(10), dp(2));
            copyButton.setBackground(createBackground(
                    new int[]{surfaceColor, surfaceColor}, borderColor, 18));
            LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, dp(38));
            copyParams.gravity = Gravity.START;
            copyParams.topMargin = dp(6);
            bubble.addView(copyButton, copyParams);
            copyButton.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(ClipData.newPlainText(tr("copy_label"), message));
                Toast.makeText(this, tr("copied"), Toast.LENGTH_SHORT).show();
            });
        }

        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        return text;
    }

    private void removeMessage(TextView messageView) {
        if (messageView != null) ViewParentHelper.remove(messageView);
    }

    private static class ViewParentHelper {
        static void remove(View view) {
            if (view.getParent() instanceof ViewGroup) {
                ((ViewGroup) view.getParent()).removeView(view);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        ArrayList<String> savedMessages = new ArrayList<>();
        ArrayList<Boolean> savedSenders = new ArrayList<>();
        ArrayList<Boolean> savedCopyFlags = new ArrayList<>();
        if (messagesLayout != null) {
            for (int i = 0; i < messagesLayout.getChildCount(); i++) {
                View wrapperView = messagesLayout.getChildAt(i);
                if (!(wrapperView instanceof LinearLayout)) continue;
                LinearLayout wrapper = (LinearLayout) wrapperView;
                if (wrapper.getChildCount() == 0 ||
                        !(wrapper.getChildAt(0) instanceof LinearLayout)) continue;
                LinearLayout bubble = (LinearLayout) wrapper.getChildAt(0);
                if (bubble.getChildCount() < 2 ||
                        !(bubble.getChildAt(0) instanceof TextView) ||
                        !(bubble.getChildAt(1) instanceof TextView)) continue;
                TextView sender = (TextView) bubble.getChildAt(0);
                TextView message = (TextView) bubble.getChildAt(1);
                savedSenders.add(sender.getText().toString().equals(tr("you")));
                savedMessages.add(message.getText().toString());
                savedCopyFlags.add(bubble.getChildCount() > 2);
            }
        }
        outState.putStringArrayList("ai_messages", savedMessages);
        boolean[] senderArray = new boolean[savedSenders.size()];
        boolean[] copyFlagsArray = new boolean[savedCopyFlags.size()];
        for (int i = 0; i < savedSenders.size(); i++) {
            senderArray[i] = savedSenders.get(i);
            copyFlagsArray[i] = savedCopyFlags.get(i);
        }
        outState.putBooleanArray("ai_senders", senderArray);
        outState.putBooleanArray("ai_copy_flags", copyFlagsArray);
        if (questionInput != null) {
            outState.putString("ai_input", questionInput.getText().toString());
        }
        outState.putString(
                "ai_conversation_history", serializeConversationHistory());
        outState.putString("ai_active_chat_id", activeChatId);
        outState.putString("ai_active_chat_title", activeChatTitle);
    }

    private void recreateIfThemeChanged() {
        if (isSending) return;
        int currentThemeColor = ThemeManager.getThemeColor(this);
        boolean currentNightMode = ThemeManager.isNightMode(this);
        if (appliedThemeColor != currentThemeColor ||
                appliedNightMode != currentNightMode) {
            recreate();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        recreateIfThemeChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
