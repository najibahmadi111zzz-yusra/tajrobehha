package com.tajro.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class VaultActivity extends Activity {

    private ExchangeData data;

    private TextView balanceText;

    private EditText amountInput;
    private EditText noteInput;

    private EditText custodyNameInput;
    private EditText custodyPhoneInput;
    private EditText custodyAmountInput;
    private EditText custodyNoteInput;

    private Spinner currencySpinner;
    private Spinner custodyCurrencySpinner;

    private int themeColor;

    private String[] currencies;

    private String appliedLanguage;

    // --------------------------------------------------
    // دفتر رسیدهای امانت
    // --------------------------------------------------

    private static final String RECEIPT_PREFS =
            "tajro_custody_receipts";

    private static final String RECEIPT_DATA =
            "receipts";

    private static final String RECEIPT_COUNTER =
            "receipt_counter";

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(
                LanguageManager.applyLanguage(newBase)
        );
    }

    private String text(
            String fa,
            String en,
            String ps,
            String ur,
            String hi
    ) {
        String lang =
                LanguageManager.getLanguage(this);

        if ("en".equals(lang)) {
            return en;
        }

        if ("ps".equals(lang)) {
            return ps;
        }

        if ("ur".equals(lang)) {
            return ur;
        }

        if ("hi".equals(lang)) {
            return hi;
        }

        return fa;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        appliedLanguage =
                LanguageManager.getLanguage(this);

        data = ExchangeData.get(this);

        themeColor =
                ThemeManager.getThemeColor(this);

        currencies =
                ExchangeData.getCurrencies();

        buildScreen();

        updateBalance();
    }

    @Override
    protected void onResume() {
        super.onResume();

        String currentLanguage =
                LanguageManager.getLanguage(this);

        if (appliedLanguage != null
                && !currentLanguage.equals(appliedLanguage)) {

            recreate();
            return;
        }

        updateBalance();
    }

    // ==================================================
    // صفحه اصلی
    // ==================================================

    private void buildScreen() {

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setClipToPadding(false);
        scrollView.setBackgroundColor(getLightThemeColor());

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(24));
        root.setGravity(Gravity.CENTER_HORIZONTAL);

        // سربرگ مدرن
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setGravity(Gravity.CENTER);
        header.setPadding(dp(16), dp(18), dp(16), dp(18));
        header.setBackground(rounded(Color.rgb(16, 48, 70), dp(22)));

        TextView title = new TextView(this);
        title.setText(text("🔐 گاوصندوق هوشمند", "🔐 Smart Vault",
                "🔐 هوښیار خوندي صندوق", "🔐 اسمارٹ سیف", "🔐 स्मार्ट वॉल्ट"));
        title.setTextSize(23);
        title.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        header.addView(title);

        TextView info = new TextView(this);
        info.setText(text(
                "مدیریت امن دارایی‌ها و امانت مشتریان",
                "Securely manage assets and customer deposits",
                "د شتمنیو او د پیرودونکو امانتونو خوندي مدیریت",
                "اثاثوں اور صارفین کی امانتوں کا محفوظ انتظام",
                "संपत्ति और ग्राहक जमा का सुरक्षित प्रबंधन"));
        info.setTextSize(13);
        info.setTextColor(Color.rgb(211, 226, 235));
        info.setGravity(Gravity.CENTER);
        info.setPadding(0, dp(7), 0, 0);
        header.addView(info);
        root.addView(header, matchWrap());

        // کارت موجودی
        LinearLayout balanceCard = new LinearLayout(this);
        balanceCard.setOrientation(LinearLayout.VERTICAL);
        balanceCard.setPadding(dp(16), dp(12), dp(16), dp(12));
        balanceCard.setGravity(Gravity.CENTER);
        balanceCard.setBackground(rounded(Color.WHITE, dp(18)));
        LinearLayout.LayoutParams balanceParams = matchWrap();
        balanceParams.setMargins(0, dp(12), 0, dp(12));
        root.addView(balanceCard, balanceParams);

        TextView balanceLabel = new TextView(this);
        balanceLabel.setText(text("موجودی گاوصندوق", "Vault balance",
                "د خوندي صندوق موجودي", "سیف کا بیلنس", "वॉल्ट बैलेंस"));
        balanceLabel.setTextSize(13);
        balanceLabel.setTextColor(Color.DKGRAY);
        balanceLabel.setGravity(Gravity.CENTER);
        balanceCard.addView(balanceLabel);

        currencySpinner = new Spinner(this);
        createCurrencyAdapter(currencySpinner);
        LinearLayout.LayoutParams spinnerParams = wrap();
        spinnerParams.setMargins(0, dp(4), 0, dp(2));
        balanceCard.addView(currencySpinner, spinnerParams);

        balanceText = new TextView(this);
        balanceText.setTextSize(23);
        balanceText.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        balanceText.setTextColor(themeColor);
        balanceText.setGravity(Gravity.CENTER);
        balanceText.setPadding(0, dp(5), 0, dp(2));
        balanceCard.addView(balanceText);

        // کارت عملیات اصلی
        LinearLayout operationCard = sectionCard();
        addSectionTitle(operationCard,
                text("مدیریت موجودی", "Balance management", "د موجودۍ مدیریت", "بیلنس مینجمنٹ", "बैलेंस प्रबंधन"));

        amountInput = makeVaultInput(text("مبلغ", "Amount", "مقدار", "رقم", "राशि"),
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        operationCard.addView(amountInput, matchWrap());

        noteInput = makeVaultInput(text("یادداشت / دلیل (اختیاری)", "Note / reason (optional)",
                "یادښت / دلیل (اختیاري)", "نوٹ / وجہ (اختیاری)", "नोट / कारण (वैकल्पिक)"),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        operationCard.addView(noteInput, matchWrap());

        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setGravity(Gravity.CENTER);
        Button depositButton = compactButton(text("➕ واریز", "➕ Deposit", "➕ جمع", "➕ جمع", "➕ जमा"), Color.rgb(28, 132, 104));
        Button withdrawButton = compactButton(text("➖ برداشت", "➖ Withdraw", "➖ ایستل", "➖ نکالیں", "➖ निकालें"), Color.rgb(190, 91, 66));
        LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(0, dp(48), 1f);
        half.setMargins(dp(4), dp(8), dp(4), dp(2));
        actionRow.addView(depositButton, new LinearLayout.LayoutParams(0, dp(48), 1f));
        actionRow.addView(withdrawButton, new LinearLayout.LayoutParams(0, dp(48), 1f));
        operationCard.addView(actionRow, matchWrap());

        Button historyButton = compactButton(text("📋 تاریخچه گاوصندوق", "📋 Vault history",
                "📋 د خوندي صندوق تاریخچه", "📋 سیف کی تاریخ", "📋 वॉल्ट इतिहास"), themeColor);
        operationCard.addView(historyButton, matchWrap());
        root.addView(operationCard, matchWrap());

        // کارت امانت مشتریان
        LinearLayout custodyCard = sectionCard();
        addSectionTitle(custodyCard, text("امانت مشتریان", "Customer deposits",
                "د پیرودونکو امانتونه", "صارفین کی امانتیں", "ग्राहक जमा"));
        TextView custodyInfo = new TextView(this);
        custodyInfo.setText(text(
                "امانت مشتریان از دارایی صرافی جدا نگهداری می‌شود.",
                "Customer deposits are kept separate from exchange assets.",
                "د پیرودونکو امانتونه د صرافۍ له شتمنیو جلا ساتل کېږي.",
                "صارفین کی امانتیں صرافی کے اثاثوں سے الگ رکھی جاتی ہیں۔",
                "ग्राहक जमा राशि एक्सचेंज की संपत्ति से अलग रखी जाती है।"));
        custodyInfo.setTextSize(12);
        custodyInfo.setTextColor(Color.DKGRAY);
        custodyInfo.setGravity(Gravity.CENTER);
        custodyInfo.setPadding(dp(4), 0, dp(4), dp(8));
        custodyCard.addView(custodyInfo, matchWrap());

        Button addCustodyButton = compactButton(text("🔐 ثبت امانت جدید", "🔐 Add deposit",
                "🔐 نوې امانت ثبت", "🔐 نئی امانت درج کریں", "🔐 नई जमा दर्ज करें"), themeColor);
        Button custodyHistoryButton = compactButton(text("📋 امانت‌های مشتریان", "📋 Customer deposits",
                "📋 د پیرودونکو امانتونه", "📋 صارفین کی امانتیں", "📋 ग्राहक जमा"), themeColor);
        Button searchCustodyButton = compactButton(text("🔍 جستجوی مشتری و امانت", "🔍 Search customer/deposit",
                "🔍 د پیرودونکي او امانت لټون", "🔍 صارف اور امانت تلاش", "🔍 ग्राहक/जमा खोजें"), themeColor);
        Button receiptsButton = compactButton(text("🧾 رسیدهای امانت", "🧾 Deposit receipts",
                "🧾 د امانت رسیدونه", "🧾 امانت کی رسیدیں", "🧾 जमा रसीदें"), themeColor);
        custodyCard.addView(addCustodyButton, matchWrap());
        custodyCard.addView(custodyHistoryButton, matchWrap());
        custodyCard.addView(searchCustodyButton, matchWrap());
        custodyCard.addView(receiptsButton, matchWrap());
        root.addView(custodyCard, matchWrap());

        Button backButton = compactButton(text("⬅️ بازگشت", "⬅️ Back", "⬅️ شاته", "⬅️ واپس", "⬅️ वापस"), Color.rgb(91, 105, 116));
        LinearLayout.LayoutParams backParams = matchWrap();
        backParams.setMargins(0, dp(12), 0, 0);
        root.addView(backButton, backParams);

        // رویدادها؛ منطق مالی قبلی بدون تغییر حفظ شده است
        currencySpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) { updateBalance(); }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
        depositButton.setOnClickListener(v -> deposit());
        withdrawButton.setOnClickListener(v -> withdraw());
        historyButton.setOnClickListener(v -> showHistory());
        addCustodyButton.setOnClickListener(v -> showAddCustodyDialog());
        custodyHistoryButton.setOnClickListener(v -> showCustodyHistory());
        searchCustodyButton.setOnClickListener(v -> showCustomerSearch());
        receiptsButton.setOnClickListener(v -> showReceiptHistory());
        backButton.setOnClickListener(v -> finish());

        scrollView.addView(root);
        setContentView(scrollView);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private LinearLayout sectionCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackground(rounded(Color.WHITE, dp(18)));
        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(0, dp(5), 0, dp(8));
        card.setLayoutParams(params);
        return card;
    }

    private void addSectionTitle(LinearLayout parent, String value) {
        TextView label = new TextView(this);
        label.setText(value);
        label.setTextSize(17);
        label.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        label.setTextColor(themeColor);
        label.setGravity(Gravity.CENTER);
        label.setPadding(0, 0, 0, dp(8));
        parent.addView(label, matchWrap());
    }

    private EditText makeVaultInput(String hint, int inputType) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setInputType(inputType);
        input.setTextSize(14);
        input.setSingleLine(inputType != (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES));
        input.setPadding(dp(12), dp(7), dp(12), dp(7));
        input.setBackground(rounded(Color.rgb(246, 248, 250), dp(11)));
        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(0, dp(4), 0, dp(5));
        input.setLayoutParams(params);
        return input;
    }

    private Button compactButton(String label, int color) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(13);
        button.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(dp(8), 0, dp(8), 0);
        button.setBackground(rounded(color, dp(12)));
        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(0, dp(4), 0, dp(4));
        button.setLayoutParams(params);
        return button;
    }


    // ==================================================
    // ارز + پرچم
    // ==================================================

    private void createCurrencyAdapter(
            Spinner spinner
    ) {

        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(
                        this,
                        android.R.layout.simple_spinner_item,
                        currencies
                ) {

                    @Override
                    public View getView(
                            int position,
                            View convertView,
                            android.view.ViewGroup parent
                    ) {

                        TextView text =
                                (TextView) super.getView(
                                        position,
                                        convertView,
                                        parent
                                );

                        text.setText(
                                getCurrencyFlag(
                                        currencies[position]
                                )
                                        + " "
                                        + getCurrencyDisplayName(
                                        currencies[position]
                                )
                        );

                        text.setTextSize(17);

                        return text;
                    }

                    @Override
                    public View getDropDownView(
                            int position,
                            View convertView,
                            android.view.ViewGroup parent
                    ) {

                        TextView text =
                                (TextView) super.getDropDownView(
                                        position,
                                        convertView,
                                        parent
                                );

                        text.setText(
                                getCurrencyFlag(
                                        currencies[position]
                                )
                                        + " "
                                        + getCurrencyDisplayName(
                                        currencies[position]
                                )
                        );

                        text.setTextSize(17);

                        return text;
                    }
                };

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinner.setAdapter(adapter);
    }

    private String getCurrencyDisplayName(
            String currency
    ) {

        if (ExchangeData.AFN.equals(currency)) {
            return text(
                    "افغانی",
                    "Afghani",
                    "افغانۍ",
                    "افغانی",
                    "अफ़गानी"
            );
        }

        if (ExchangeData.USD.equals(currency)) {
            return text(
                    "دالر",
                    "US Dollar",
                    "ډالر",
                    "ڈالر",
                    "डॉलर"
            );
        }

        if (ExchangeData.EUR.equals(currency)) {
            return text(
                    "یورو",
                    "Euro",
                    "یورو",
                    "یورو",
                    "यूरो"
            );
        }

        if (ExchangeData.GBP.equals(currency)) {
            return text(
                    "پوند انگلیس",
                    "British Pound",
                    "برتانوي پونډ",
                    "برطانوی پاؤنڈ",
                    "ब्रिटिश पाउंड"
            );
        }

        if (ExchangeData.SAR.equals(currency)) {
            return text(
                    "ریال سعودی",
                    "Saudi Riyal",
                    "سعودي ریال",
                    "سعودی ریال",
                    "सऊदी रियाल"
            );
        }

        if (ExchangeData.AED.equals(currency)) {
            return text(
                    "درهم امارات",
                    "UAE Dirham",
                    "اماراتي درهم",
                    "اماراتی درہم",
                    "यूएई दिरहम"
            );
        }

        if (ExchangeData.IQD.equals(currency)) {
            return text(
                    "دینار عراق",
                    "Iraqi Dinar",
                    "عراقي دینار",
                    "عراقی دینار",
                    "इराकी दीनार"
            );
        }

        if (ExchangeData.INR.equals(currency)) {
            return text(
                    "روپیه هند",
                    "Indian Rupee",
                    "هندي روپۍ",
                    "بھارتی روپیہ",
                    "भारतीय रुपया"
            );
        }

        if (ExchangeData.PKR.equals(currency)) {
            return text(
                    "روپیه پاکستانی",
                    "Pakistani Rupee",
                    "پاکستانۍ روپۍ",
                    "پاکستانی روپیہ",
                    "पाकिस्तानी रुपया"
            );
        }

        if (ExchangeData.TRY.equals(currency)) {
            return text(
                    "لیر ترکیه",
                    "Turkish Lira",
                    "ترکي لیره",
                    "ترکی لیرا",
                    "तुर्की लीरा"
            );
        }

        if (ExchangeData.TOMAN.equals(currency)) {
            return text(
                    "تومان",
                    "Toman",
                    "تومان",
                    "تومان",
                    "तोमान"
            );
        }

        return currency;
    }

    private String getCurrencyFlag(
            String currency
    ) {

        if (ExchangeData.AFN.equals(currency))
            return "🇦🇫";

        if (ExchangeData.USD.equals(currency))
            return "🇺🇸";

        if (ExchangeData.EUR.equals(currency))
            return "🇪🇺";

        if (ExchangeData.GBP.equals(currency))
            return "🇬🇧";

        if (ExchangeData.SAR.equals(currency))
            return "🇸🇦";

        if (ExchangeData.AED.equals(currency))
            return "🇦🇪";

        if (ExchangeData.IQD.equals(currency))
            return "🇮🇶";

        if (ExchangeData.INR.equals(currency))
            return "🇮🇳";

        if (ExchangeData.PKR.equals(currency))
            return "🇵🇰";

        if (ExchangeData.TRY.equals(currency))
            return "🇹🇷";

        if (ExchangeData.TOMAN.equals(currency))
            return "🇮🇷";

        return "🌐";
    }

    // ==================================================
    // گاوصندوق
    // ==================================================

    private String getSelectedCurrency() {

        if (currencySpinner == null
                || currencySpinner.getSelectedItem() == null) {

            return ExchangeData.AFN;
        }

        return currencySpinner
                .getSelectedItem()
                .toString();
    }

    private double getAmount() {

        String text =
                amountInput.getText()
                        .toString()
                        .trim();

        if (text.isEmpty()) {
            return 0;
        }

        try {
            return Double.parseDouble(text);
        } catch (Exception e) {
            return 0;
        }
    }

    private void deposit() {

        double amount = getAmount();

        if (amount <= 0) {

            Toast.makeText(
                    this,
                    text(
                            "لطفاً مبلغ درست وارد کنید.",
                            "Please enter a valid amount.",
                            "مهرباني وکړئ سمه اندازه دننه کړئ.",
                            "براہ کرم درست رقم درج کریں۔",
                            "कृपया सही राशि दर्ज करें।"
                    ),
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String currency =
                getSelectedCurrency();

        data.addBalance(
                currency,
                amount
        );

        saveVaultRecord(
                "ورود",
                currency,
                amount,
                noteInput.getText().toString()
        );

        clearInputs();

        updateBalance();

        Toast.makeText(
                this,
                text(
                        "مبلغ با موفقیت وارد گاوصندوق شد.",
                        "Amount was successfully added to the vault.",
                        "مقدار په بریالیتوب سره خوندي صندوق ته داخل شو.",
                        "رقم کامیابی سے سیف میں جمع ہو گئی۔",
                        "राशि सफलतापूर्वक वॉल्ट में जमा हो गई।"
                ),
                Toast.LENGTH_SHORT
        ).show();
    }

    private void withdraw() {

        double amount = getAmount();

        if (amount <= 0) {

            Toast.makeText(
                    this,
                    text(
                            "لطفاً مبلغ درست وارد کنید.",
                            "Please enter a valid amount.",
                            "مهرباني وکړئ سمه اندازه دننه کړئ.",
                            "براہ کرم درست رقم درج کریں۔",
                            "कृपया सही राशि दर्ज करें।"
                    ),
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String currency =
                getSelectedCurrency();

        boolean success =
                data.subtractBalance(
                        currency,
                        amount
                );

        if (!success) {

            double current =
                    data.getBalance(currency);

            Toast.makeText(
                    this,
                    text(
                            "موجودی کافی نیست.\n" +
                                    "موجودی فعلی: " +
                                    formatNumber(current),

                            "Insufficient balance.\n" +
                                    "Current balance: " +
                                    formatNumber(current),

                            "موجودي کافي نه دی.\n" +
                                    "اوسنی موجودي: " +
                                    formatNumber(current),

                            "بیلنس کافی نہیں ہے۔\n" +
                                    "موجودہ بیلنس: " +
                                    formatNumber(current),

                            "पर्याप्त बैलेंस नहीं है।\n" +
                                    "वर्तमान बैलेंस: " +
                                    formatNumber(current)
                    ),
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        saveVaultRecord(
                "خروج",
                currency,
                amount,
                noteInput.getText().toString()
        );

        clearInputs();

        updateBalance();

        Toast.makeText(
                this,
                text(
                        "مبلغ از گاوصندوق برداشت شد.",
                        "Amount was withdrawn from the vault.",
                        "مقدار له خوندي صندوق څخه وایستل شو.",
                        "رقم سیف سے نکال لی گئی۔",
                        "राशि वॉल्ट से निकाल ली गई।"
                ),
                Toast.LENGTH_SHORT
        ).show();
    }

    private void updateBalance() {

        if (currencySpinner == null
                || currencySpinner.getSelectedItem() == null
                || balanceText == null) {

            return;
        }

        String currency =
                getSelectedCurrency();

        double balance =
                data.getBalance(currency);

        balanceText.setText(
                getCurrencyFlag(currency)
                        + " "
                        + text(
                        "موجودی ",
                        "Balance ",
                        "موجودي ",
                        "بیلنس ",
                        "बैलेंस "
                )
                        + getCurrencyDisplayName(currency)
                        + "\n"
                        + formatNumber(balance)
        );
    }

    // ==================================================
    // ثبت امانت
    // ==================================================

    private void showAddCustodyDialog() {

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setPadding(
                35,
                10,
                35,
                10
        );

        custodyNameInput =
                new EditText(this);

        custodyNameInput.setHint(
                text(
                        "نام مشتری",
                        "Customer name",
                        "د پیرودونکي نوم",
                        "صارف کا نام",
                        "ग्राहक का नाम"
                )
        );

        custodyNameInput.setSingleLine(true);

        layout.addView(
                custodyNameInput
        );

        custodyPhoneInput =
                new EditText(this);

        custodyPhoneInput.setHint(
                text(
                        "شماره تلفن",
                        "Phone number",
                        "د تلیفون شمېره",
                        "فون نمبر",
                        "फ़ोन नंबर"
                )
        );

        custodyPhoneInput.setInputType(
                InputType.TYPE_CLASS_PHONE
        );

        custodyPhoneInput.setSingleLine(true);

        layout.addView(
                custodyPhoneInput
        );

        custodyCurrencySpinner =
                new Spinner(this);

        createCurrencyAdapter(
                custodyCurrencySpinner
        );

        layout.addView(
                custodyCurrencySpinner
        );

        custodyAmountInput =
                new EditText(this);

        custodyAmountInput.setHint(
                text(
                        "مبلغ امانت",
                        "Deposit amount",
                        "د امانت مقدار",
                        "امانت کی رقم",
                        "जमा राशि"
                )
        );

        custodyAmountInput.setInputType(
                InputType.TYPE_CLASS_NUMBER |
                        InputType.TYPE_NUMBER_FLAG_DECIMAL
        );

        custodyAmountInput.setSingleLine(true);

        layout.addView(
                custodyAmountInput
        );

        custodyNoteInput =
                new EditText(this);

        custodyNoteInput.setHint(
                text(
                        "یادداشت / توضیح امانت",
                        "Deposit note / description",
                        "د امانت یادښت / توضیح",
                        "امانت کا نوٹ / وضاحت",
                        "जमा नोट / विवरण"
                )
        );

        layout.addView(
                custodyNoteInput
        );

        final AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                text(
                                        "🔐 ثبت امانت مشتری",
                                        "🔐 Add Customer Deposit",
                                        "🔐 د پیرودونکي امانت ثبتول",
                                        "🔐 صارف کی امانت درج کریں",
                                        "🔐 ग्राहक की जमा दर्ज करें"
                                )
                        )
                        .setMessage(
                                text(
                                        "این مبلغ متعلق به مشتری است و " +
                                                "به موجودی خود صرافی اضافه نمی‌شود.",

                                        "This amount belongs to the customer " +
                                                "and is not added to the exchange's own balance.",

                                        "دا مقدار د پیرودونکي دی او " +
                                                "د صرافۍ خپل موجودي ته نه اضافه کېږي.",

                                        "یہ رقم صارف کی ہے اور " +
                                                "صرافی کے اپنے بیلنس میں شامل نہیں ہوتی۔",

                                        "यह राशि ग्राहक की है और " +
                                                "एक्सचेंज के अपने बैलेंस में नहीं जोड़ी जाती।"
                                )
                        )
                        .setView(layout)
                        .setNegativeButton(
                                text(
                                        "انصراف",
                                        "Cancel",
                                        "لغوه",
                                        "منسوخ",
                                        "रद्द करें"
                                ),
                                null
                        )
                        .setPositiveButton(
                                text(
                                        "ثبت امانت",
                                        "Add Deposit",
                                        "امانت ثبت کړئ",
                                        "امانت درج کریں",
                                        "जमा दर्ज करें"
                                ),
                                null
                        )
                        .create();

        dialog.setOnShowListener(
                d -> {

                    Button positive =
                            dialog.getButton(
                                    AlertDialog.BUTTON_POSITIVE
                            );

                    positive.setOnClickListener(
                            v -> {

                                String name =
                                        custodyNameInput
                                                .getText()
                                                .toString()
                                                .trim();

                                String phone =
                                        custodyPhoneInput
                                                .getText()
                                                .toString()
                                                .trim();

                                if (custodyCurrencySpinner
                                        .getSelectedItem() == null) {

                                    Toast.makeText(
                                            this,
                                            text(
                                                    "لطفاً ارز امانت را انتخاب کنید.",
                                                    "Please select the deposit currency.",
                                                    "مهرباني وکړئ د امانت اسعار وټاکئ.",
                                                    "براہ کرم امانت کی کرنسی منتخب کریں۔",
                                                    "कृपया जमा मुद्रा चुनें।"
                                            ),
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    return;
                                }

                                String currency =
                                        custodyCurrencySpinner
                                                .getSelectedItem()
                                                .toString();

                                String amountText =
                                        custodyAmountInput
                                                .getText()
                                                .toString()
                                                .trim();

                                String note =
                                        custodyNoteInput
                                                .getText()
                                                .toString()
                                                .trim();

                                if (name.isEmpty()) {

                                    custodyNameInput.setError(
                                            text(
                                                    "نام مشتری را وارد کنید",
                                                    "Enter customer name",
                                                    "د پیرودونکي نوم دننه کړئ",
                                                    "صارف کا نام درج کریں",
                                                    "ग्राहक का नाम दर्ज करें"
                                            )
                                    );

                                    custodyNameInput.requestFocus();

                                    return;
                                }

                                if (amountText.isEmpty()) {

                                    custodyAmountInput.setError(
                                            text(
                                                    "مبلغ امانت را وارد کنید",
                                                    "Enter deposit amount",
                                                    "د امانت مقدار دننه کړئ",
                                                    "امانت کی رقم درج کریں",
                                                    "जमा राशि दर्ज करें"
                                            )
                                    );

                                    custodyAmountInput.requestFocus();

                                    return;
                                }

                                double amount;

                                try {

                                    amount =
                                            Double.parseDouble(
                                                    amountText
                                            );

                                } catch (Exception e) {

                                    custodyAmountInput.setError(
                                            text(
                                                    "مبلغ درست وارد کنید",
                                                    "Enter a valid amount",
                                                    "سمه اندازه دننه کړئ",
                                                    "درست رقم درج کریں",
                                                    "सही राशि दर्ज करें"
                                            )
                                    );

                                    custodyAmountInput.requestFocus();

                                    return;
                                }

                                if (amount <= 0) {

                                    custodyAmountInput.setError(
                                            text(
                                                    "مبلغ باید بیشتر از صفر باشد",
                                                    "Amount must be greater than zero",
                                                    "مقدار باید له صفر څخه زیات وي",
                                                    "رقم صفر سے زیادہ ہونی چاہیے",
                                                    "राशि शून्य से अधिक होनी चाहिए"
                                            )
                                    );

                                    custodyAmountInput.requestFocus();

                                    return;
                                }

                                boolean saved =
                                        data.saveCustody(
                                                name,
                                                phone,
                                                currency,
                                                amount,
                                                note
                                        );

                                if (!saved) {

                                    Toast.makeText(
                                            this,
                                            text(
                                                    "ثبت امانت انجام نشد.",
                                                    "Deposit could not be saved.",
                                                    "امانت ثبت نه شو.",
                                                    "امانت درج نہیں ہو سکی۔",
                                                    "जमा सेव नहीं हो सकी।"
                                            ),
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    return;
                                }

                                dialog.dismiss();

                                Toast.makeText(
                                        this,
                                        text(
                                                "🔐 امانت با موفقیت ثبت شد.",
                                                "🔐 Deposit saved successfully.",
                                                "🔐 امانت په بریالیتوب ثبت شو.",
                                                "🔐 امانت کامیابی سے درج ہو گئی۔",
                                                "🔐 जमा सफलतापूर्वक दर्ज हुई।"
                                        ),
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                    );
                }
        );

        dialog.show();
    }

    // ==================================================
    // تاریخچه امانت
    // ==================================================

    private void showCustodyHistory() {

        try {

            JSONArray records =
                    data.getCustodyArray();

            StringBuilder text =
                    new StringBuilder();

            if (records.length() == 0) {

                text.append(
                        text(
                                "هنوز هیچ امانتی ثبت نشده است.",
                                "No deposits have been registered yet.",
                                "تر اوسه هېڅ امانت نه دی ثبت شوی.",
                                "ابھی تک کوئی امانت درج نہیں ہوئی۔",
                                "अभी तक कोई जमा दर्ज नहीं हुई है।"
                        )
                );

            } else {

                for (int i = records.length() - 1;
                     i >= 0;
                     i--) {

                    JSONObject record =
                            records.optJSONObject(i);

                    if (record == null) {
                        continue;
                    }

                    String name =
                            record.optString(
                                    "customerName"
                            );

                    String phone =
                            record.optString(
                                    "phone"
                            );

                    String currency =
                            record.optString(
                                    "currency"
                            );

                    double amount =
                            record.optDouble(
                                    "amount",
                                    0
                            );

                    String status =
                            record.optString(
                                    "status",
                                    "امانت نزد صرافی"
                            );

                    String note =
                            record.optString(
                                    "note"
                            );

                    long date =
                            record.optLong(
                                    "date",
                                    0
                            );

                    double received =
                            getTotalReceiptsForCustody(
                                    record.optLong(
                                            "id",
                                            0
                                    )
                            );

                    double remaining =
                            Math.max(
                                    0,
                                    amount - received
                            );

                    text.append(
                            "━━━━━━━━━━━━━━\n"
                    );

                    text.append(
                            getCurrencyFlag(currency)
                    )
                            .append(" ")
                            .append(
                                    getCurrencyDisplayName(
                                            currency
                                    )
                            )
                            .append("\n");

                    text.append(
                            text(
                                    "👤 مشتری: ",
                                    "👤 Customer: ",
                                    "👤 پیرودونکی: ",
                                    "👤 صارف: ",
                                    "👤 ग्राहक: "
                            )
                    )
                            .append(name)
                            .append("\n");

                    if (!phone.isEmpty()) {

                        text.append(
                                text(
                                        "📞 شماره: ",
                                        "📞 Phone: ",
                                        "📞 شمېره: ",
                                        "📞 نمبر: ",
                                        "📞 नंबर: "
                                )
                        )
                                .append(phone)
                                .append("\n");
                    }

                    text.append(
                            text(
                                    "💰 مبلغ کل: ",
                                    "💰 Total amount: ",
                                    "💰 ټوله اندازه: ",
                                    "💰 کل رقم: ",
                                    "💰 कुल राशि: "
                            )
                    )
                            .append(
                                    formatNumber(amount)
                            )
                            .append(" ")
                            .append(
                                    getCurrencyDisplayName(
                                            currency
                                    )
                            )
                            .append("\n");

                    text.append(
                            text(
                                    "💵 تحویل/دریافت ثبت‌شده: ",
                                    "💵 Recorded received/returned: ",
                                    "💵 ثبت شوې ورکړه/تحویلي: ",
                                    "💵 درج شدہ وصولی/واپسی: ",
                                    "💵 दर्ज प्राप्त/वापसी: "
                            )
                    )
                            .append(
                                    formatNumber(received)
                            )
                            .append("\n");

                    text.append(
                            text(
                                    "💰 باقی‌مانده: ",
                                    "💰 Remaining: ",
                                    "💰 پاتې: ",
                                    "💰 باقی: ",
                                    "💰 शेष: "
                            )
                    )
                            .append(
                                    formatNumber(remaining)
                            )
                            .append("\n");

                    if (remaining <= 0) {

                        text.append(
                                text(
                                        "🟢 وضعیت: تسویه شده\n",
                                        "🟢 Status: Settled\n",
                                        "🟢 حالت: تصفیه شوی\n",
                                        "🟢 حیثیت: مکمل ادا شدہ\n",
                                        "🟢 स्थिति: पूरा भुगतान\n"
                                )
                        );

                    } else {

                        text.append(
                                text(
                                        "🟠 وضعیت: باقی دارد\n",
                                        "🟠 Status: Remaining\n",
                                        "🟠 حالت: پاتې لري\n",
                                        "🟠 حیثیت: باقی ہے\n",
                                        "🟠 स्थिति: बाकी है\n"
                                )
                        );
                    }

                    text.append(
                            text(
                                    "📌 وضعیت امانت: ",
                                    "📌 Deposit status: ",
                                    "📌 د امانت حالت: ",
                                    "📌 امانت کی حیثیت: ",
                                    "📌 जमा स्थिति: "
                            )
                    )
                            .append(
                                    getDisplayedCustodyStatus(
                                            status
                                    )
                            )
                            .append("\n");

                    if (date > 0) {

                        text.append(
                                text(
                                        "📅 تاریخ: ",
                                        "📅 Date: ",
                                        "📅 نېټه: ",
                                        "📅 تاریخ: ",
                                        "📅 तारीख: "
                                )
                        )
                                .append(
                                        formatDate(date)
                                )
                                .append("\n");
                    }

                    if (!note.isEmpty()) {

                        text.append(
                                text(
                                        "📝 یادداشت: ",
                                        "📝 Note: ",
                                        "📝 یادښت: ",
                                        "📝 نوٹ: ",
                                        "📝 नोट: "
                                )
                        )
                                .append(note)
                                .append("\n");
                    }
                }
            }

            new AlertDialog.Builder(this)
                    .setTitle(
                            text(
                                    "🔐 امانت‌های مشتریان",
                                    "🔐 Customer Deposits",
                                    "🔐 د پیرودونکو امانتونه",
                                    "🔐 صارفین کی امانتیں",
                                    "🔐 ग्राहकों की जमा"
                            )
                    )
                    .setMessage(
                            text.toString()
                    )
                    .setPositiveButton(
                            text(
                                    "تحویل / ثبت رسید",
                                    "Return / Record Receipt",
                                    "تحویلي / رسید ثبتول",
                                    "واپسی / رسید درج کریں",
                                    "वापसी / रसीद दर्ज करें"
                            ),
                            (dialog, which) ->
                                    showReturnCustody()
                    )
                    .setNeutralButton(
                            text(
                                    "🔍 جستجو",
                                    "🔍 Search",
                                    "🔍 لټون",
                                    "🔍 تلاش",
                                    "🔍 खोजें"
                            ),
                            (dialog, which) ->
                                    showCustomerSearch()
                    )
                    .setNegativeButton(
                            text(
                                    "بستن",
                                    "Close",
                                    "بندول",
                                    "بند کریں",
                                    "बंद करें"
                            ),
                            null
                    )
                    .show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    text(
                            "خطا در خواندن امانت‌ها.",
                            "Error reading deposits.",
                            "د امانتونو په لوستلو کې تېروتنه.",
                            "امانتیں پڑھنے میں خرابی۔",
                            "जमा पढ़ने में त्रुटि।"
                    ),
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // ==================================================
    // جستجوی مشتری
    // ==================================================

    private void showCustomerSearch() {

        final EditText searchInput =
                new EditText(this);

        searchInput.setHint(
                text(
                        "نام یا شماره مشتری",
                        "Customer name or phone",
                        "د پیرودونکي نوم یا شمېره",
                        "صارف کا نام یا نمبر",
                        "ग्राहक का नाम या नंबर"
                )
        );

        searchInput.setSingleLine(true);

        searchInput.setPadding(
                30,
                15,
                30,
                15
        );

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setPadding(
                25,
                10,
                25,
                5
        );

        layout.addView(searchInput);

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                text(
                                        "🔍 جستجوی مشتری",
                                        "🔍 Search Customer",
                                        "🔍 د پیرودونکي لټون",
                                        "🔍 صارف تلاش کریں",
                                        "🔍 ग्राहक खोजें"
                                )
                        )
                        .setView(layout)
                        .setPositiveButton(
                                text(
                                        "جستجو",
                                        "Search",
                                        "لټون",
                                        "تلاش",
                                        "खोजें"
                                ),
                                null
                        )
                        .setNegativeButton(
                                text(
                                        "انصراف",
                                        "Cancel",
                                        "لغوه",
                                        "منسوخ",
                                        "रद्द करें"
                                ),
                                null
                        )
                        .create();

        dialog.setOnShowListener(
                d -> {

                    Button button =
                            dialog.getButton(
                                    AlertDialog.BUTTON_POSITIVE
                            );

                    button.setOnClickListener(
                            v -> {

                                String query =
                                        searchInput
                                                .getText()
                                                .toString()
                                                .trim();

                                if (query.isEmpty()) {

                                    searchInput.setError(
                                            text(
                                                    "نام یا شماره را وارد کنید",
                                                    "Enter name or phone",
                                                    "نوم یا شمېره دننه کړئ",
                                                    "نام یا نمبر درج کریں",
                                                    "नाम या नंबर दर्ज करें"
                                            )
                                    );

                                    return;
                                }

                                dialog.dismiss();

                                showSearchResults(query);
                            }
                    );
                }
        );

        dialog.show();
    }

    private void showSearchResults(
            String query
    ) {

        try {

            JSONArray records =
                    data.getCustodyArray();

            String q =
                    query.toLowerCase(
                            Locale.ROOT
                    );

            StringBuilder result =
                    new StringBuilder();

            int count = 0;

            for (int i = records.length() - 1;
                 i >= 0;
                 i--) {

                JSONObject record =
                        records.optJSONObject(i);

                if (record == null) {
                    continue;
                }

                String name =
                        record.optString(
                                "customerName"
                        );

                String phone =
                        record.optString(
                                "phone"
                        );

                String nameLower =
                        name.toLowerCase(
                                Locale.ROOT
                        );

                String phoneLower =
                        phone.toLowerCase(
                                Locale.ROOT
                        );

                if (!nameLower.contains(q)
                        && !phoneLower.contains(q)) {

                    continue;
                }

                count++;

                long id =
                        record.optLong(
                                "id",
                                0
                        );

                String currency =
                        record.optString(
                                "currency"
                        );

                double amount =
                        record.optDouble(
                                "amount",
                                0
                        );

                double received =
                        getTotalReceiptsForCustody(id);

                double remaining =
                        Math.max(
                                0,
                                amount - received
                        );

                String status =
                        record.optString(
                                "status",
                                "امانت نزد صرافی"
                        );

                result.append(
                        "━━━━━━━━━━━━━━\n"
                );

                result.append(
                        "👤 "
                )
                        .append(name)
                        .append("\n");

                if (!phone.isEmpty()) {

                    result.append(
                            "📞 "
                    )
                            .append(phone)
                            .append("\n");
                }

                result.append(
                        getCurrencyFlag(currency)
                )
                        .append(" ")
                        .append(
                                getCurrencyDisplayName(
                                        currency
                                )
                        )
                        .append("\n");

                result.append(
                        text(
                                "💰 کل: ",
                                "💰 Total: ",
                                "💰 ټول: ",
                                "💰 کل: ",
                                "💰 कुल: "
                        )
                )
                        .append(
                                formatNumber(amount)
                        )
                        .append("\n");

                result.append(
                        text(
                                "💵 ثبت‌شده: ",
                                "💵 Recorded: ",
                                "💵 ثبت شوي: ",
                                "💵 درج شدہ: ",
                                "💵 दर्ज: "
                        )
                )
                        .append(
                                formatNumber(received)
                        )
                        .append("\n");

                result.append(
                        text(
                                "💰 باقی: ",
                                "💰 Remaining: ",
                                "💰 پاتې: ",
                                "💰 باقی: ",
                                "💰 शेष: "
                        )
                )
                        .append(
                                formatNumber(remaining)
                        )
                        .append("\n");

                if (remaining <= 0) {

                    result.append(
                            text(
                                    "🟢 تسویه شده\n",
                                    "🟢 Settled\n",
                                    "🟢 تصفیه شوی\n",
                                    "🟢 مکمل ادا شدہ\n",
                                    "🟢 पूरा भुगतान\n"
                            )
                    );

                } else {

                    result.append(
                            text(
                                    "🟠 باقی دارد\n",
                                    "🟠 Remaining\n",
                                    "🟠 پاتې لري\n",
                                    "🟠 باقی ہے\n",
                                    "🟠 बाकी है\n"
                            )
                    );
                }

                result.append(
                        "📌 "
                )
                        .append(
                                getDisplayedCustodyStatus(
                                        status
                                )
                        )
                        .append("\n");

                if (remaining > 0
                        && "امانت نزد صرافی".equals(status)) {

                    result.append(
                            text(
                                    "🧾 برای ثبت پرداخت/تحویل، " +
                                            "روی «ثبت رسید» بزنید.\n",

                                    "🧾 To record a payment/return, " +
                                            "tap “Record Receipt”.\n",

                                    "🧾 د تادیې/تحویلي ثبتولو لپاره، " +
                                            "«رسید ثبتول» ووهئ.\n",

                                    "🧾 ادائیگی/واپسی درج کرنے کے لیے " +
                                            "«رسید درج کریں» دبائیں۔\n",

                                    "🧾 भुगतान/वापसी दर्ज करने के लिए " +
                                            "«रसीद दर्ज करें» दबाएँ।\n"
                            )
                    );
                }
            }

            if (count == 0) {

                result.append(
                        text(
                                "❌ مشتری پیدا نشد.",
                                "❌ Customer not found.",
                                "❌ پیرودونکی پیدا نه شو.",
                                "❌ صارف نہیں ملا۔",
                                "❌ ग्राहक नहीं मिला।"
                        )
                );
            }

            AlertDialog resultDialog =
                    new AlertDialog.Builder(this)
                            .setTitle(
                                    text(
                                            "🔍 نتیجه جستجو",
                                            "🔍 Search Results",
                                            "🔍 د لټون پایله",
                                            "🔍 تلاش کا نتیجہ",
                                            "🔍 खोज परिणाम"
                                    )
                            )
                            .setMessage(
                                    result.toString()
                            )
                            .setPositiveButton(
                                    text(
                                            "ثبت رسید",
                                            "Record Receipt",
                                            "رسید ثبتول",
                                            "رسید درج کریں",
                                            "रसीद दर्ज करें"
                                    ),
                                    (dialog, which) ->
                                            showReturnCustody()
                            )
                            .setNegativeButton(
                                    text(
                                            "بستن",
                                            "Close",
                                            "بندول",
                                            "بند کریں",
                                            "बंद करें"
                                    ),
                                    null
                            )
                            .create();

            resultDialog.show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    text(
                            "خطا در جستجوی مشتری.",
                            "Error searching customer.",
                            "د پیرودونکي په لټون کې تېروتنه.",
                            "صارف تلاش کرنے میں خرابی۔",
                            "ग्राहक खोजने में त्रुटि।"
                    ),
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // ==================================================
    // تحویل امانت / ثبت رسید
    // ==================================================

    private void showReturnCustody() {

        try {

            JSONArray records =
                    data.getCustodyArray();

            ArrayList<Long> ids =
                    new ArrayList<>();

            ArrayList<String> items =
                    new ArrayList<>();

            for (int i = 0;
                 i < records.length();
                 i++) {

                JSONObject record =
                        records.optJSONObject(i);

                if (record == null) {
                    continue;
                }

                String status =
                        record.optString(
                                "status",
                                "امانت نزد صرافی"
                        );

                if (!"امانت نزد صرافی".equals(status)) {
                    continue;
                }

                long id =
                        record.optLong(
                                "id",
                                0
                        );

                String name =
                        record.optString(
                                "customerName"
                        );

                String currency =
                        record.optString(
                                "currency"
                        );

                double amount =
                        record.optDouble(
                                "amount",
                                0
                        );

                double received =
                        getTotalReceiptsForCustody(id);

                double remaining =
                        Math.max(
                                0,
                                amount - received
                        );

                if (remaining <= 0) {
                    continue;
                }

                ids.add(id);

                items.add(
                        getCurrencyFlag(currency)
                                + " "
                                + name
                                + " — "
                                + text(
                                "کل ",
                                "Total ",
                                "ټول ",
                                "کل ",
                                "कुल "
                        )
                                + formatNumber(amount)
                                + " "
                                + getCurrencyDisplayName(currency)
                                + " — "
                                + text(
                                "باقی ",
                                "Remaining ",
                                "پاتې ",
                                "باقی ",
                                "शेष "
                        )
                                + formatNumber(remaining)
                );
            }

            if (items.isEmpty()) {

                new AlertDialog.Builder(this)
                        .setTitle(
                                text(
                                        "تحویل امانت",
                                        "Return Deposit",
                                        "امانت تحویلي",
                                        "امانت واپس کریں",
                                        "जमा वापस करें"
                                )
                        )
                        .setMessage(
                                text(
                                        "امانت فعالی برای ثبت رسید وجود ندارد.",
                                        "There is no active deposit for recording a receipt.",
                                        "د رسید ثبتولو لپاره فعاله امانت نشته.",
                                        "رسید درج کرنے کے لیے کوئی فعال امانت موجود نہیں۔",
                                        "रसीद दर्ज करने के लिए कोई सक्रिय जमा नहीं है।"
                                )
                        )
                        .setPositiveButton(
                                text(
                                        "باشه",
                                        "OK",
                                        "سمه ده",
                                        "ٹھیک ہے",
                                        "ठीक है"
                                ),
                                null
                        )
                        .show();

                return;
            }

            String[] itemArray =
                    items.toArray(
                            new String[0]
                    );

            new AlertDialog.Builder(this)
                    .setTitle(
                            text(
                                    "🧾 انتخاب مشتری برای رسید",
                                    "🧾 Select Customer for Receipt",
                                    "🧾 د رسید لپاره پیرودونکی وټاکئ",
                                    "🧾 رسید کے لیے صارف منتخب کریں",
                                    "🧾 रसीद के लिए ग्राहक चुनें"
                            )
                    )
                    .setItems(
                            itemArray,
                            (dialog, which) -> {

                                long id =
                                        ids.get(which);

                                showReceiptDialog(id);
                            }
                    )
                    .setNegativeButton(
                            text(
                                    "انصراف",
                                    "Cancel",
                                    "لغوه",
                                    "منسوخ",
                                    "रद्द करें"
                            ),
                            null
                    )
                    .show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    text(
                            "خطا در خواندن امانت‌ها.",
                            "Error reading deposits.",
                            "د امانتونو په لوستلو کې تېروتنه.",
                            "امانتیں پڑھنے میں خرابی۔",
                            "जमा पढ़ने में त्रुटि।"
                    ),
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // ==================================================
    // ثبت رسید
    // ==================================================

    private void showReceiptDialog(
            long custodyId
    ) {

        JSONObject custody =
                findCustodyById(custodyId);

        if (custody == null) {

            Toast.makeText(
                    this,
                    text(
                            "رکورد امانت پیدا نشد.",
                            "Deposit record not found.",
                            "د امانت ریکارډ پیدا نه شو.",
                            "امانت کا ریکارڈ نہیں ملا۔",
                            "जमा रिकॉर्ड नहीं मिला।"
                    ),
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String name =
                custody.optString(
                        "customerName"
                );

        String phone =
                custody.optString(
                        "phone"
                );

        String currency =
                custody.optString(
                        "currency"
                );

        double total =
                custody.optDouble(
                        "amount",
                        0
                );

        double received =
                getTotalReceiptsForCustody(
                        custodyId
                );

        double remaining =
                Math.max(
                        0,
                        total - received
                );

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setPadding(
                35,
                10,
                35,
                10
        );

        TextView info =
                new TextView(this);

        info.setText(
                text(
                        "👤 مشتری: ",
                        "👤 Customer: ",
                        "👤 پیرودونکی: ",
                        "👤 صارف: ",
                        "👤 ग्राहक: "
                )
                        + name
                        + "\n"
                        +
                        (phone.isEmpty()
                                ? ""
                                : text(
                                "📞 شماره: ",
                                "📞 Phone: ",
                                "📞 شمېره: ",
                                "📞 نمبر: ",
                                "📞 नंबर: "
                        )
                                + phone
                                + "\n")
                        +
                        text(
                                "💰 مبلغ کل: ",
                                "💰 Total amount: ",
                                "💰 ټوله اندازه: ",
                                "💰 کل رقم: ",
                                "💰 कुल राशि: "
                        )
                        + formatNumber(total)
                        + " "
                        + getCurrencyDisplayName(currency)
                        + "\n"
                        +
                        text(
                                "💵 قبلاً ثبت شده: ",
                                "💵 Previously recorded: ",
                                "💵 مخکې ثبت شوی: ",
                                "💵 پہلے درج شدہ: ",
                                "💵 पहले दर्ज: "
                        )
                        + formatNumber(received)
                        + " "
                        + getCurrencyDisplayName(currency)
                        + "\n"
                        +
                        text(
                                "🟠 باقی‌مانده: ",
                                "🟠 Remaining: ",
                                "🟠 پاتې: ",
                                "🟠 باقی: ",
                                "🟠 शेष: "
                        )
                        + formatNumber(remaining)
                        + " "
                        + getCurrencyDisplayName(currency)
        );

        info.setTextSize(16);

        info.setPadding(
                0,
                0,
                0,
                20
        );

        layout.addView(info);

        EditText amount =
                new EditText(this);

        amount.setHint(
                text(
                        "مبلغ این رسید",
                        "Receipt amount",
                        "د دې رسید مقدار",
                        "اس رسید کی رقم",
                        "इस रसीद की राशि"
                )
        );

        amount.setInputType(
                InputType.TYPE_CLASS_NUMBER |
                        InputType.TYPE_NUMBER_FLAG_DECIMAL
        );

        amount.setSingleLine(true);

        layout.addView(amount);

        EditText note =
                new EditText(this);

        note.setHint(
                text(
                        "توضیح رسید",
                        "Receipt note",
                        "د رسید توضیح",
                        "رسید کی وضاحت",
                        "रसीद विवरण"
                )
        );

        layout.addView(note);

        final AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                text(
                                        "🧾 ثبت رسید",
                                        "🧾 Record Receipt",
                                        "🧾 رسید ثبتول",
                                        "🧾 رسید درج کریں",
                                        "🧾 रसीद दर्ज करें"
                                )
                        )
                        .setMessage(
                                text(
                                        "رسید پس از ثبت قابل ویرایش یا حذف نیست.",
                                        "The receipt cannot be edited or deleted after registration.",
                                        "رسید له ثبتېدو وروسته نه سمېږي او نه حذفېږي.",
                                        "رسید درج ہونے کے بعد اس میں ترمیم یا حذف نہیں کیا جا سکتا۔",
                                        "रसीद दर्ज करने के बाद इसे संपादित या हटाया नहीं जा सकता।"
                                )
                        )
                        .setView(layout)
                        .setNegativeButton(
                                text(
                                        "انصراف",
                                        "Cancel",
                                        "لغوه",
                                        "منسوخ",
                                        "रद्द करें"
                                ),
                                null
                        )
                        .setPositiveButton(
                                text(
                                        "ثبت رسید",
                                        "Record Receipt",
                                        "رسید ثبتول",
                                        "رسید درج کریں",
                                        "रसीद दर्ज करें"
                                ),
                                null
                        )
                        .create();

        dialog.setOnShowListener(
                d -> {

                    Button positive =
                            dialog.getButton(
                                    AlertDialog.BUTTON_POSITIVE
                            );

                    positive.setOnClickListener(
                            v -> {

                                String amountText =
                                        amount.getText()
                                                .toString()
                                                .trim();

                                if (amountText.isEmpty()) {

                                    amount.setError(
                                            text(
                                                    "مبلغ را وارد کنید",
                                                    "Enter amount",
                                                    "مقدار دننه کړئ",
                                                    "رقم درج کریں",
                                                    "राशि दर्ज करें"
                                            )
                                    );

                                    return;
                                }

                                double receiptAmount;

                                try {

                                    receiptAmount =
                                            Double.parseDouble(
                                                    amountText
                                            );

                                } catch (Exception e) {

                                    amount.setError(
                                            text(
                                                    "مبلغ درست وارد کنید",
                                                    "Enter a valid amount",
                                                    "سمه اندازه دننه کړئ",
                                                    "درست رقم درج کریں",
                                                    "सही राशि दर्ज करें"
                                            )
                                    );

                                    return;
                                }

                                if (receiptAmount <= 0) {

                                    amount.setError(
                                            text(
                                                    "مبلغ باید بیشتر از صفر باشد",
                                                    "Amount must be greater than zero",
                                                    "مقدار باید له صفر څخه زیات وي",
                                                    "رقم صفر سے زیادہ ہونی چاہیے",
                                                    "राशि शून्य से अधिक होनी चाहिए"
                                            )
                                    );

                                    return;
                                }

                                if (receiptAmount > remaining) {

                                    amount.setError(
                                            text(
                                                    "مبلغ بیشتر از باقی‌مانده است",
                                                    "Amount exceeds the remaining balance",
                                                    "مقدار له پاتې اندازې څخه زیات دی",
                                                    "رقم باقی رقم سے زیادہ ہے",
                                                    "राशि शेष से अधिक है"
                                            )
                                    );

                                    return;
                                }

                                String receiptCode =
                                        createReceiptCode();

                                boolean saved =
                                        saveImmutableReceipt(
                                                receiptCode,
                                                custodyId,
                                                name,
                                                phone,
                                                currency,
                                                receiptAmount,
                                                note.getText()
                                                        .toString()
                                                        .trim()
                                        );

                                if (!saved) {

                                    Toast.makeText(
                                            this,
                                            text(
                                                    "ثبت رسید انجام نشد.",
                                                    "Receipt could not be saved.",
                                                    "رسید ثبت نه شو.",
                                                    "رسید درج نہیں ہو سکی۔",
                                                    "रसीद सेव नहीं हो सकी।"
                                            ),
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    return;
                                }

                                double newRemaining =
                                        Math.max(
                                                0,
                                                remaining
                                                        - receiptAmount
                                        );

                                // اگر کل مبلغ تسویه شد،
                                // وضعیت امانت نیز تحویل داده شده می‌شود.
                                if (newRemaining <= 0) {

                                    data.returnCustody(
                                            custodyId
                                    );
                                }

                                dialog.dismiss();

                                showReceiptCreated(
                                        receiptCode,
                                        name,
                                        currency,
                                        receiptAmount,
                                        newRemaining
                                );
                            }
                    );
                }
        );

        dialog.show();
    }

    // ==================================================
    // رسید جدید
    // ==================================================

    private String createReceiptCode() {

        android.content.SharedPreferences prefs =
                getSharedPreferences(
                        RECEIPT_PREFS,
                        MODE_PRIVATE
                );

        long counter =
                prefs.getLong(
                        RECEIPT_COUNTER,
                        0
                );

        counter++;

        prefs.edit()
                .putLong(
                        RECEIPT_COUNTER,
                        counter
                )
                .apply();

        return String.format(
                Locale.US,
                "REC-%06d",
                counter
        );
    }

    private boolean saveImmutableReceipt(
            String receiptCode,
            long custodyId,
            String customerName,
            String phone,
            String currency,
            double amount,
            String note
    ) {

        try {

            android.content.SharedPreferences prefs =
                    getSharedPreferences(
                            RECEIPT_PREFS,
                            MODE_PRIVATE
                    );

            String old =
                    prefs.getString(
                            RECEIPT_DATA,
                            "[]"
                    );

            JSONArray receipts =
                    new JSONArray(old);

            JSONObject receipt =
                    new JSONObject();

            receipt.put(
                    "receiptCode",
                    receiptCode
            );

            receipt.put(
                    "custodyId",
                    custodyId
            );

            receipt.put(
                    "customerName",
                    customerName
            );

            receipt.put(
                    "phone",
                    phone == null ? "" : phone
            );

            receipt.put(
                    "currency",
                    currency
            );

            receipt.put(
                    "amount",
                    amount
            );

            receipt.put(
                    "note",
                    note == null ? "" : note
            );

            receipt.put(
                    "date",
                    System.currentTimeMillis()
            );

            receipt.put(
                    "immutable",
                    true
            );

            receipts.put(receipt);

            /*
             * عمداً هیچ متدی برای ویرایش یا حذف رسید
             * در این Activity وجود ندارد.
             */
            prefs.edit()
                    .putString(
                            RECEIPT_DATA,
                            receipts.toString()
                    )
                    .apply();

            return true;

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }

    private void showReceiptCreated(
            String code,
            String customer,
            String currency,
            double amount,
            double remaining
    ) {

        String status;

        if (remaining <= 0) {

            status =
                    text(
                            "🟢 تسویه کامل شد",
                            "🟢 Fully settled",
                            "🟢 په بشپړه توګه تصفیه شو",
                            "🟢 مکمل ادا ہو گیا",
                            "🟢 पूरा भुगतान हो गया"
                    );

        } else {

            status =
                    text(
                            "🟠 هنوز باقی دارد",
                            "🟠 Amount remains",
                            "🟠 لا هم پاتې لري",
                            "🟠 ابھی رقم باقی ہے",
                            "🟠 राशि अभी बाकी है"
                    );
        }

        String message =
                text(
                        "🧾 رسید با موفقیت ثبت شد.\n\n",
                        "🧾 Receipt saved successfully.\n\n",
                        "🧾 رسید په بریالیتوب ثبت شو.\n\n",
                        "🧾 رسید کامیابی سے درج ہو گئی۔\n\n",
                        "🧾 रसीद सफलतापूर्वक दर्ज हुई।\n\n"
                ) +
                        text(
                                "🔢 کد رسید: ",
                                "🔢 Receipt code: ",
                                "🔢 د رسید کوډ: ",
                                "🔢 رسید کوڈ: ",
                                "🔢 रसीद कोड: "
                        )
                        + code
                        + "\n"
                        +
                        text(
                                "👤 مشتری: ",
                                "👤 Customer: ",
                                "👤 پیرودونکی: ",
                                "👤 صارف: ",
                                "👤 ग्राहक: "
                        )
                        + customer
                        + "\n"
                        +
                        text(
                                "💰 مبلغ رسید: ",
                                "💰 Receipt amount: ",
                                "💰 د رسید مقدار: ",
                                "💰 رسید کی رقم: ",
                                "💰 रसीद राशि: "
                        )
                        + formatNumber(amount)
                        + " "
                        + getCurrencyDisplayName(currency)
                        + "\n"
                        +
                        text(
                                "💵 باقی‌مانده: ",
                                "💵 Remaining: ",
                                "💵 پاتې: ",
                                "💵 باقی: ",
                                "💵 शेष: "
                        )
                        + formatNumber(remaining)
                        + " "
                        + getCurrencyDisplayName(currency)
                        + "\n"
                        + status
                        + "\n\n"
                        +
                        text(
                                "📅 تاریخ: ",
                                "📅 Date: ",
                                "📅 نېټه: ",
                                "📅 تاریخ: ",
                                "📅 तारीख: "
                        )
                        + formatDate(
                        System.currentTimeMillis()
                );

        new AlertDialog.Builder(this)
                .setTitle(
                        text(
                                "✅ رسید ثبت شد",
                                "✅ Receipt Recorded",
                                "✅ رسید ثبت شو",
                                "✅ رسید درج ہو گئی",
                                "✅ रसीद दर्ज हुई"
                        )
                )
                .setMessage(message)
                .setPositiveButton(
                        text(
                                "باشه",
                                "OK",
                                "سمه ده",
                                "ٹھیک ہے",
                                "ठीक है"
                        ),
                        null
                )
                .show();
    }

    // ==================================================
    // نمایش رسیدهای قبلی
    // ==================================================

    private void showReceiptHistory() {

        try {

            android.content.SharedPreferences prefs =
                    getSharedPreferences(
                            RECEIPT_PREFS,
                            MODE_PRIVATE
                    );

            String old =
                    prefs.getString(
                            RECEIPT_DATA,
                            "[]"
                    );

            JSONArray receipts =
                    new JSONArray(old);

            StringBuilder text =
                    new StringBuilder();

            if (receipts.length() == 0) {

                text.append(
                        text(
                                "هنوز هیچ رسیدی ثبت نشده است.",
                                "No receipts have been registered yet.",
                                "تر اوسه هېڅ رسید نه دی ثبت شوی.",
                                "ابھی تک کوئی رسید درج نہیں ہوئی۔",
                                "अभी तक कोई रसीद दर्ज नहीं हुई है।"
                        )
                );

            } else {

                for (int i = receipts.length() - 1;
                     i >= 0;
                     i--) {

                    JSONObject receipt =
                            receipts.optJSONObject(i);

                    if (receipt == null) {
                        continue;
                    }

                    String code =
                            receipt.optString(
                                    "receiptCode"
                            );

                    String name =
                            receipt.optString(
                                    "customerName"
                            );

                    String phone =
                            receipt.optString(
                                    "phone"
                            );

                    String currency =
                            receipt.optString(
                                    "currency"
                            );

                    double amount =
                            receipt.optDouble(
                                    "amount",
                                    0
                            );

                    String note =
                            receipt.optString(
                                    "note"
                            );

                    long date =
                            receipt.optLong(
                                    "date",
                                    0
                            );

                    text.append(
                            "━━━━━━━━━━━━━━\n"
                    );

                    text.append(
                            "🧾 "
                    )
                            .append(code)
                            .append("\n");

                    text.append(
                            text(
                                    "👤 مشتری: ",
                                    "👤 Customer: ",
                                    "👤 پیرودونکی: ",
                                    "👤 صارف: ",
                                    "👤 ग्राहक: "
                            )
                    )
                            .append(name)
                            .append("\n");

                    if (!phone.isEmpty()) {

                        text.append(
                                text(
                                        "📞 شماره: ",
                                        "📞 Phone: ",
                                        "📞 شمېره: ",
                                        "📞 نمبر: ",
                                        "📞 नंबर: "
                                )
                        )
                                .append(phone)
                                .append("\n");
                    }

                    text.append(
                            text(
                                    "💰 مبلغ: ",
                                    "💰 Amount: ",
                                    "💰 مقدار: ",
                                    "💰 رقم: ",
                                    "💰 राशि: "
                            )
                    )
                            .append(
                                    formatNumber(amount)
                            )
                            .append(" ")
                            .append(
                                    getCurrencyDisplayName(
                                            currency
                                    )
                            )
                            .append("\n");

                    if (date > 0) {

                        text.append(
                                text(
                                        "📅 تاریخ: ",
                                        "📅 Date: ",
                                        "📅 نېټه: ",
                                        "📅 تاریخ: ",
                                        "📅 तारीख: "
                                )
                        )
                                .append(
                                        formatDate(date)
                                )
                                .append("\n");
                    }

                    if (!note.isEmpty()) {

                        text.append(
                                text(
                                        "📝 توضیح: ",
                                        "📝 Description: ",
                                        "📝 توضیح: ",
                                        "📝 وضاحت: ",
                                        "📝 विवरण: "
                                )
                        )
                                .append(note)
                                .append("\n");
                    }

                    text.append(
                            text(
                                    "🔒 غیرقابل ویرایش/حذف",
                                    "🔒 Cannot be edited/deleted",
                                    "🔒 نه سمېږي او نه حذفېږي",
                                    "🔒 ترمیم یا حذف نہیں کیا جا سکتا",
                                    "🔒 संपादित/हटाया नहीं जा सकता"
                            )
                    )
                            .append("\n");
                }
            }

            new AlertDialog.Builder(this)
                    .setTitle(
                            text(
                                    "🧾 رسیدهای امانت",
                                    "🧾 Deposit Receipts",
                                    "🧾 د امانت رسیدونه",
                                    "🧾 امانت کی رسیدیں",
                                    "🧾 जमा रसीदें"
                            )
                    )
                    .setMessage(
                            text.toString()
                    )
                    .setPositiveButton(
                            text(
                                    "بستن",
                                    "Close",
                                    "بندول",
                                    "بند کریں",
                                    "बंद करें"
                            ),
                            null
                    )
                    .show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    text(
                            "خطا در خواندن رسیدها.",
                            "Error reading receipts.",
                            "د رسیدونو په لوستلو کې تېروتنه.",
                            "رسیدیں پڑھنے میں خرابی۔",
                            "रसीद पढ़ने में त्रुटि।"
                    ),
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // ==================================================
    // پیدا کردن امانت
    // ==================================================

    private JSONObject findCustodyById(
            long id
    ) {

        try {

            JSONArray records =
                    data.getCustodyArray();

            for (int i = 0;
                 i < records.length();
                 i++) {

                JSONObject record =
                        records.optJSONObject(i);

                if (record == null) {
                    continue;
                }

                if (record.optLong(
                        "id",
                        0
                ) == id) {

                    return record;
                }
            }

        } catch (Exception e) {

            e.printStackTrace();
        }

        return null;
    }

    // ==================================================
    // جمع رسیدهای یک امانت
    // ==================================================

    private double getTotalReceiptsForCustody(
            long custodyId
    ) {

        double total = 0;

        try {

            android.content.SharedPreferences prefs =
                    getSharedPreferences(
                            RECEIPT_PREFS,
                            MODE_PRIVATE
                    );

            String old =
                    prefs.getString(
                            RECEIPT_DATA,
                            "[]"
                    );

            JSONArray receipts =
                    new JSONArray(old);

            for (int i = 0;
                 i < receipts.length();
                 i++) {

                JSONObject receipt =
                        receipts.optJSONObject(i);

                if (receipt == null) {
                    continue;
                }

                if (receipt.optLong(
                        "custodyId",
                        0
                ) == custodyId) {

                    total +=
                            receipt.optDouble(
                                    "amount",
                                    0
                            );
                }
            }

        } catch (Exception e) {

            e.printStackTrace();
        }

        return total;
    }

    // ==================================================
    // تاریخچه گاوصندوق
    // ==================================================

    private void saveVaultRecord(
            String type,
            String currency,
            double amount,
            String note
    ) {

        android.content.SharedPreferences prefs =
                getSharedPreferences(
                        "tajro_vault_records",
                        MODE_PRIVATE
                );

        String oldData =
                prefs.getString(
                        "records",
                        "[]"
                );

        try {

            JSONArray records =
                    new JSONArray(oldData);

            JSONObject record =
                    new JSONObject();

            record.put(
                    "id",
                    System.currentTimeMillis()
            );

            record.put(
                    "type",
                    type
            );

            record.put(
                    "currency",
                    currency
            );

            record.put(
                    "amount",
                    amount
            );

            record.put(
                    "note",
                    note == null ? "" : note
            );

            record.put(
                    "date",
                    System.currentTimeMillis()
            );

            records.put(record);

            prefs.edit()
                    .putString(
                            "records",
                            records.toString()
                    )
                    .apply();

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    private void showHistory() {

        android.content.SharedPreferences prefs =
                getSharedPreferences(
                        "tajro_vault_records",
                        MODE_PRIVATE
                );

        String oldData =
                prefs.getString(
                        "records",
                        "[]"
                );

        try {

            JSONArray records =
                    new JSONArray(oldData);

            StringBuilder text =
                    new StringBuilder();

            if (records.length() == 0) {

                text.append(
                        text(
                                "هنوز هیچ عملیاتی ثبت نشده است.",
                                "No operations have been recorded yet.",
                                "تر اوسه هېڅ عملیات نه دي ثبت شوي.",
                                "ابھی تک کوئی کارروائی درج نہیں ہوئی۔",
                                "अभी तक कोई ऑपरेशन दर्ज नहीं हुआ है।"
                        )
                );

            } else {

                for (int i = records.length() - 1;
                     i >= 0;
                     i--) {

                    JSONObject record =
                            records.getJSONObject(i);

                    String type =
                            record.optString("type");

                    String currency =
                            record.optString("currency");

                    double amount =
                            record.optDouble("amount");

                    String note =
                            record.optString("note");

                    long date =
                            record.optLong(
                                    "date",
                                    0
                            );

                    text.append(
                            "────────────\n"
                    );

                    text.append(
                            getCurrencyFlag(currency)
                    )
                            .append(" ")
                            .append(
                                    getDisplayedVaultType(
                                            type
                                    )
                            )
                            .append("\n");

                    text.append(
                            text(
                                    "ارز: ",
                                    "Currency: ",
                                    "اسعار: ",
                                    "کرنسی: ",
                                    "मुद्रा: "
                            )
                    )
                            .append(
                                    getCurrencyDisplayName(
                                            currency
                                    )
                            )
                            .append("\n");

                    text.append(
                            text(
                                    "مبلغ: ",
                                    "Amount: ",
                                    "مقدار: ",
                                    "رقم: ",
                                    "राशि: "
                            )
                    )
                            .append(
                                    formatNumber(amount)
                            )
                            .append("\n");

                    if (date > 0) {

                        text.append(
                                text(
                                        "تاریخ: ",
                                        "Date: ",
                                        "نېټه: ",
                                        "تاریخ: ",
                                        "तारीख: "
                                )
                        )
                                .append(
                                        formatDate(date)
                                )
                                .append("\n");
                    }

                    if (!note.isEmpty()) {

                        text.append(
                                text(
                                        "یادداشت: ",
                                        "Note: ",
                                        "یادښت: ",
                                        "نوٹ: ",
                                        "नोट: "
                                )
                        )
                                .append(note)
                                .append("\n");
                    }
                }
            }

            new AlertDialog.Builder(this)
                    .setTitle(
                            text(
                                    "📋 تاریخچه گاوصندوق",
                                    "📋 Vault History",
                                    "📋 د خوندي صندوق تاریخچه",
                                    "📋 سیف کی تاریخچہ",
                                    "📋 वॉल्ट इतिहास"
                            )
                    )
                    .setMessage(
                            text.toString()
                    )
                    .setPositiveButton(
                            text(
                                    "بستن",
                                    "Close",
                                    "بندول",
                                    "بند کریں",
                                    "बंद करें"
                            ),
                            null
                    )
                    .show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    text(
                            "خطا در خواندن تاریخچه.",
                            "Error reading history.",
                            "د تاریخچې په لوستلو کې تېروتنه.",
                            "تاریخچہ پڑھنے میں خرابی۔",
                            "इतिहास पढ़ने में त्रुटि।"
                    ),
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // ==================================================
    // وضعیت‌های ذخیره‌شده — فقط برای نمایش ترجمه می‌شوند
    // ==================================================

    private String getDisplayedCustodyStatus(
            String status
    ) {

        if ("امانت نزد صرافی".equals(status)) {

            return text(
                    "امانت نزد صرافی",
                    "Deposit with Exchange",
                    "د صرافۍ سره امانت",
                    "صرافی کے پاس امانت",
                    "एक्सचेंज के पास जमा"
            );
        }

        if ("تحویل داده شد".equals(status)) {

            return text(
                    "تحویل داده شد",
                    "Returned",
                    "تحویل شو",
                    "واپس کر دی گئی",
                    "वापस कर दी गई"
            );
        }

        return status;
    }

    private String getDisplayedVaultType(
            String type
    ) {

        if ("ورود".equals(type)) {

            return text(
                    "ورود",
                    "Deposit",
                    "داخلول",
                    "جمع",
                    "जमा"
            );
        }

        if ("خروج".equals(type)) {

            return text(
                    "خروج",
                    "Withdrawal",
                    "ایستل",
                    "نکاسی",
                    "निकासी"
            );
        }

        return type;
    }

    // ==================================================
    // ابزارها
    // ==================================================

    private String formatNumber(
            double number
    ) {

        return String.format(
                Locale.US,
                "%,.2f",
                number
        );
    }

    // ==================================================
    // تاریخ هجری شمسی افغانستان
    // ==================================================

    private String formatDate(
            long time
    ) {

        try {

            Date date =
                    new Date(time);

            SimpleDateFormat gregorian =
                    new SimpleDateFormat(
                            "yyyy-MM-dd HH:mm",
                            Locale.US
                    );

            String[] parts =
                    gregorian.format(date)
                            .split(" ");

            String[] ymd =
                    parts[0].split("-");

            int gy =
                    Integer.parseInt(ymd[0]);

            int gm =
                    Integer.parseInt(ymd[1]);

            int gd =
                    Integer.parseInt(ymd[2]);

            int[] jalali =
                    gregorianToAfghanSolar(
                            gy,
                            gm,
                            gd
                    );

            String timePart =
                    parts.length > 1
                            ? parts[1]
                            : "";

            String monthName =
                    getAfghanMonthName(
                            jalali[1]
                    );

            return jalali[0]
                    + " "
                    + monthName
                    + " "
                    + jalali[2]
                    + " - "
                    + timePart;

        } catch (Exception e) {

            return "";
        }
    }

    private String getAfghanMonthName(
            int month
    ) {

        String[] months = {
                text(
                        "حمل",
                        "Hamal",
                        "وری",
                        "حمل",
                        "हमल"
                ),
                text(
                        "ثور",
                        "Sawr",
                        "غویی",
                        "ثور",
                        "सौर"
                ),
                text(
                        "جوزا",
                        "Jawza",
                        "غبرګولی",
                        "جوزا",
                        "जौज़ा"
                ),
                text(
                        "سرطان",
                        "Saratan",
                        "چنګاښ",
                        "سرطان",
                        "सरतान"
                ),
                text(
                        "اسد",
                        "Asad",
                        "زمری",
                        "اسد",
                        "असद"
                ),
                text(
                        "سنبله",
                        "Sonbola",
                        "وږی",
                        "سنبلہ",
                        "सुनबुला"
                ),
                text(
                        "میزان",
                        "Mizan",
                        "تله",
                        "میزان",
                        "मिज़ान"
                ),
                text(
                        "عقرب",
                        "Aqrab",
                        "لړم",
                        "عقرب",
                        "अक़रब"
                ),
                text(
                        "قوس",
                        "Qaws",
                        "لیندۍ",
                        "قوس",
                        "क़ौस"
                ),
                text(
                        "جدی",
                        "Jadi",
                        "مرغومی",
                        "جدی",
                        "जदी"
                ),
                text(
                        "دلو",
                        "Dalwa",
                        "سلواغه",
                        "دلو",
                        "दलवा"
                ),
                text(
                        "حوت",
                        "Hut",
                        "کب",
                        "حوت",
                        "हूत"
                )
        };

        if (month >= 1 && month <= 12) {

            return months[month - 1];
        }

        return "";
    }

    /*
     * تبدیل تاریخ میلادی به هجری شمسی افغانستان.
     * ماه‌های افغانستان:
     * حمل، ثور، جوزا، سرطان، اسد، سنبله،
     * میزان، عقرب، قوس، جدی، دلو، حوت
     */
    private int[] gregorianToAfghanSolar(
            int gy,
            int gm,
            int gd
    ) {

        int[] gDaysInMonth = {
                31, 28, 31, 30, 31, 30,
                31, 31, 30, 31, 30, 31
        };

        int gy2 = gy - 1600;
        int gm2 = gm - 1;
        int gd2 = gd - 1;

        int gDayNo =
                365 * gy2
                        + (gy2 + 3) / 4
                        - (gy2 + 99) / 100
                        + (gy2 + 399) / 400;

        for (int i = 0; i < gm2; i++) {

            gDayNo +=
                    gDaysInMonth[i];
        }

        if (gm2 > 1
                && (
                (gy % 4 == 0 && gy % 100 != 0)
                        || (gy % 400 == 0)
        )) {

            gDayNo++;
        }

        gDayNo += gd2;

        int jDayNo =
                gDayNo - 79;

        int jNp =
                jDayNo / 12053;

        jDayNo =
                jDayNo % 12053;

        int jy =
                979
                        + 33 * jNp
                        + 4 * (jDayNo / 1461);

        jDayNo =
                jDayNo % 1461;

        if (jDayNo >= 366) {

            jy +=
                    (jDayNo - 1) / 365;

            jDayNo =
                    (jDayNo - 1) % 365;
        }

        int jm;
        int jd;

        if (jDayNo < 186) {

            jm =
                    1
                            + jDayNo / 31;

            jd =
                    1
                            + jDayNo % 31;

        } else {

            jm =
                    7
                            + (jDayNo - 186) / 30;

            jd =
                    1
                            + (jDayNo - 186) % 30;
        }

        return new int[]{
                jy,
                jm,
                jd
        };
    }

    private void clearInputs() {

        amountInput.setText("");

        noteInput.setText("");
    }

    private void styleButton(
            Button button
    ) {

        GradientDrawable background =
                new GradientDrawable();

        background.setColor(themeColor);

        background.setCornerRadius(24);

        button.setBackground(background);

        button.setTextColor(
                Color.WHITE
        );
    }

    private int getLightThemeColor() {

        int red =
                Color.red(themeColor);

        int green =
                Color.green(themeColor);

        int blue =
                Color.blue(themeColor);

        red =
                red
                        + (255 - red)
                        * 92
                        / 100;

        green =
                green
                        + (255 - green)
                        * 92
                        / 100;

        blue =
                blue
                        + (255 - blue)
                        * 92
                        / 100;

        return Color.rgb(
                red,
                green,
                blue
        );
    }
            }
