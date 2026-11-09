package com.tajro.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.Configuration;
import android.view.ViewParent;
import java.util.Locale;
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

public class AIActivity extends Activity {

    private LinearLayout messagesLayout;
    private EditText questionInput;
    private ScrollView scrollView;
    private Button sendButton;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean isSending = false;

    private final int primary = Color.rgb(12, 91, 120);
    private final int darkBlue = Color.rgb(5, 43, 70);
    private final int background = Color.rgb(235, 248, 250);
    private final int textColor = Color.rgb(28, 48, 62);

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
            default: values = new String[]{key,key,key,key,key,key};
        }
        int index = "en".equals(lang) ? 1 : "ps".equals(lang) ? 2 : "ur".equals(lang) ? 3 : "hi".equals(lang) ? 4 : "ar".equals(lang) ? 5 : 0;
        return values[index];
    }

    private String getWelcomeMessage() { return tr("welcome"); }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(darkBlue);
        getWindow().setNavigationBarColor(background);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
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
                new int[]{Color.rgb(22, 151, 194), Color.rgb(22, 74, 151)},
                Color.rgb(80, 220, 255), 50));
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
        bottom.setBackgroundColor(Color.WHITE);

        questionInput = new EditText(this);
        questionInput.setHint(tr("hint"));
        questionInput.setTextDirection("ar".equals(languageCode()) || "ur".equals(languageCode()) ||
                "ps".equals(languageCode()) || "fa".equals(languageCode())
                ? View.TEXT_DIRECTION_RTL : View.TEXT_DIRECTION_LTR);
        questionInput.setLayoutDirection(main.getLayoutDirection());
        questionInput.setTextSize(16);
        questionInput.setTextColor(textColor);
        questionInput.setHintTextColor(Color.GRAY);
        questionInput.setPadding(dp(15), dp(10), dp(15), dp(10));
        questionInput.setMinHeight(dp(50));
        questionInput.setMaxLines(4);
        questionInput.setSingleLine(false);
        questionInput.setImeOptions(EditorInfo.IME_ACTION_SEND);
        questionInput.setBackground(createBackground(
                new int[]{Color.rgb(248, 252, 255), Color.rgb(248, 252, 255)},
                Color.rgb(205, 231, 239), 24));
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
        addMessage(false, getWelcomeMessage(), false);
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

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 1) {
                if (isSending) {
                    Toast.makeText(this, tr("wait"),
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
                messagesLayout.removeAllViews();
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
                            addMessage(false, tr("cleared_msg"), false);
                            Toast.makeText(this, tr("cleared"), Toast.LENGTH_SHORT).show();
                        }).show();
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

        String question = questionInput.getText().toString().trim();
        if (question.isEmpty()) {
            Toast.makeText(this, tr("empty"), Toast.LENGTH_SHORT).show();
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
                String apiKey = BuildConfig.OPENROUTER_API_KEY;
                if (apiKey == null || apiKey.trim().isEmpty()) {
                    throw new Exception(tr("not_configured"));
                }

                URL url = new URL("https://openrouter.ai/api/v1/chat/completions");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(60000);
                connection.setDoOutput(true);
                connection.setRequestProperty("Authorization", "Bearer " + apiKey);
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

                JSONObject body = new JSONObject();
                body.put("model", "openrouter/free");
                JSONArray messages = new JSONArray();
                JSONObject systemMessage = new JSONObject();
                systemMessage.put("role", "system");
                String responseLanguage = "fa".equals(languageCode()) ? "فارسی/دری" :
                        "en".equals(languageCode()) ? "English" :
                        "ps".equals(languageCode()) ? "پښتو" :
                        "ur".equals(languageCode()) ? "اردو" :
                        "hi".equals(languageCode()) ? "हिन्दी" : "العربية";
                systemMessage.put("content",
                        "You are the AI assistant of the TajroBehha app. Always answer in " + responseLanguage + ". " +
                        "Use clear, respectful, useful language. Do not limit yourself to user experiences; " +
                        "answer general, scientific, educational, technology, historical and everyday questions " +
                        "as far as your knowledge allows. If you do not have enough information, say so honestly. " +
                        "Never present fabricated information as fact. For medical questions, give cautious general " +
                        "information and recommend consulting a doctor for serious situations. " +
                        "Organize answers appropriately for the question.");
                messages.put(systemMessage);
                JSONObject userMessage = new JSONObject();
                userMessage.put("role", "user");
                userMessage.put("content", question);
                messages.put(userMessage);
                body.put("messages", messages);

                byte[] data = body.toString().getBytes(StandardCharsets.UTF_8);
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
                        while ((line = reader.readLine()) != null) response.append(line);
                    }
                }
                if (responseCode < 200 || responseCode >= 300) {
                    throw new Exception("OpenRouter HTTP " + responseCode + "\n" + response);
                }

                JSONObject json = new JSONObject(response.toString());
                JSONArray choices = json.getJSONArray("choices");
                if (choices.length() == 0) throw new Exception(tr("no_answer"));
                JSONObject message = choices.getJSONObject(0).getJSONObject("message");
                String answer = message.optString("content", "").trim();
                if (answer.isEmpty()) throw new Exception(tr("empty_answer"));

                String finalAnswer = answer;
                runOnUiThread(() -> {
                    removeMessage(loading);
                    addMessage(false, finalAnswer, true);
                    finishSending();
                });
            } catch (Exception e) {
                String error = e.getMessage();
                if (error == null || error.isEmpty()) error = tr("unknown_error") + e;
                String finalError = error;
                runOnUiThread(() -> {
                    removeMessage(loading);
                    addMessage(false, tr("sorry") + "\n" + finalError, false);
                    finishSending();
                });
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    private void finishSending() {
        isSending = false;
        sendButton.setEnabled(true);
        sendButton.setAlpha(1f);
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
                        ? new int[]{Color.rgb(202, 239, 250), Color.rgb(220, 246, 251)}
                        : new int[]{Color.WHITE, Color.rgb(248, 253, 255)},
                isUser ? Color.rgb(166, 225, 241) : Color.rgb(215, 234, 241), 20));
        LinearLayout.LayoutParams bubbleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bubbleParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        bubbleParams.setMargins(isUser ? dp(24) : 0, 0, isUser ? 0 : dp(24), 0);
        wrapper.addView(bubble, bubbleParams);

        TextView sender = new TextView(this);
        sender.setText(isUser ? tr("you") : tr("assistant"));
        sender.setTextSize(13);
        sender.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        sender.setTextColor(isUser ? primary : Color.rgb(10, 115, 153));
        sender.setGravity(Gravity.RIGHT);
        bubble.addView(sender);

        TextView text = new TextView(this);
        text.setText(message);
        text.setTextSize(16);
        text.setTextColor(textColor);
        text.setGravity(Gravity.RIGHT);
        text.setTextIsSelectable(true);
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
                    new int[]{Color.WHITE, Color.WHITE}, Color.rgb(205, 231, 239), 18));
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
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
