package com.tajro.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExchangeActivity extends Activity {

    private ExchangeData exchangeData;

    private Spinner currencySpinner;
    private Spinner typeSpinner;

    private EditText customerNameInput;
    private EditText phoneInput;
    private EditText amountInput;
    private EditText rateInput;
    private EditText noteInput;

    private TextView totalText;
    private TextView balanceText;

    private String[] currencies;

    private String appliedLanguage;

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

        exchangeData = ExchangeData.get(this);
        currencies = ExchangeData.getCurrencies();

        createScreen();
    }

    // =========================================================
    // زبان
    // =========================================================

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

    private String getDisplayedTransactionType(
            String canonical
    ) {

        if ("خرید".equals(canonical)) {
            return text(
                    "خرید",
                    "Buy",
                    "پېرود",
                    "خرید",
                    "खरीद"
            );
        }

        if ("فروش".equals(canonical)) {
            return text(
                    "فروش",
                    "Sell",
                    "پلور",
                    "فروخت",
                    "बिक्री"
            );
        }

        return canonical;
    }

    private String getDisplayedAccountStatus(
            String canonical
    ) {

        if ("نقدی".equals(canonical)) {
            return text(
                    "نقدی",
                    "Cash",
                    "نغدي",
                    "نقد",
                    "नकद"
            );
        }

        return canonical;
    }

    // =========================================================
    // اندازه
    // =========================================================

    private int dp(int value) {

        return (int) (
                value *
                        getResources()
                                .getDisplayMetrics()
                                .density +
                        0.5f
        );
    }

    private TextView makeText(
            String value,
            float size
    ) {

        TextView tv =
                new TextView(this);

        tv.setText(value);
        tv.setTextSize(size);

        tv.setPadding(
                dp(8),
                dp(10),
                dp(8),
                dp(10)
        );

        return tv;
    }

    private EditText makeInput(
            String hint
    ) {

        EditText input =
                new EditText(this);

        input.setHint(hint);
        input.setTextSize(16);

        input.setPadding(
                dp(12),
                dp(8),
                dp(12),
                dp(8)
        );

        return input;
    }

    private Button makeButton(
            String value
    ) {

        Button button =
                new Button(this);

        button.setText(value);
        button.setTextSize(15);

        return button;
    }

    // =========================================================
    // پرچم ارز
    // =========================================================

    private String getCurrencyFlag(
            String currency
    ) {

        if (ExchangeData.AFN.equals(currency)) {
            return "🇦🇫";
        }

        if (ExchangeData.USD.equals(currency)) {
            return "🇺🇸";
        }

        if (ExchangeData.EUR.equals(currency)) {
            return "🇪🇺";
        }

        if (ExchangeData.GBP.equals(currency)) {
            return "🇬🇧";
        }

        if (ExchangeData.SAR.equals(currency)) {
            return "🇸🇦";
        }

        if (ExchangeData.AED.equals(currency)) {
            return "🇦🇪";
        }

        if (ExchangeData.IQD.equals(currency)) {
            return "🇮🇶";
        }

        if (ExchangeData.INR.equals(currency)) {
            return "🇮🇳";
        }

        if (ExchangeData.PKR.equals(currency)) {
            return "🇵🇰";
        }

        if (ExchangeData.TRY.equals(currency)) {
            return "🇹🇷";
        }

        if (ExchangeData.TOMAN.equals(currency)) {
            return "🇮🇷";
        }

        return "🌐";
    }

    // =========================================================
    // آداپتر ارز
    // =========================================================

    private ArrayAdapter<String> createCurrencyAdapter(
            String[] items
    ) {

        return new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                items
        ) {

            @Override
            public View getView(
                    int position,
                    View convertView,
                    android.view.ViewGroup parent
            ) {

                TextView tv =
                        (TextView) super.getView(
                                position,
                                convertView,
                                parent
                        );

                String currency =
                        items[position];

                tv.setText(
                        getCurrencyFlag(currency)
                                + "  "
                                + getCurrencyDisplayName(currency)
                );

                tv.setTextSize(16);
                tv.setTextColor(Color.DKGRAY);

                tv.setGravity(
                        Gravity.CENTER_VERTICAL
                );

                tv.setPadding(
                        dp(12),
                        dp(8),
                        dp(12),
                        dp(8)
                );

                return tv;
            }

            @Override
            public View getDropDownView(
                    int position,
                    View convertView,
                    android.view.ViewGroup parent
            ) {

                TextView tv =
                        (TextView) super.getDropDownView(
                                position,
                                convertView,
                                parent
                        );

                String currency =
                        items[position];

                tv.setText(
                        getCurrencyFlag(currency)
                                + "  "
                                + getCurrencyDisplayName(currency)
                );

                tv.setTextSize(16);

                tv.setPadding(
                        dp(12),
                        dp(12),
                        dp(12),
                        dp(12)
                );

                tv.setGravity(
                        Gravity.CENTER_VERTICAL
                );

                return tv;
            }
        };
    }

    // =========================================================
    // صفحه اصلی
    // =========================================================

    private void createScreen() {

        ScrollView scrollView =
                new ScrollView(this);

        LinearLayout main =
                new LinearLayout(this);

        main.setOrientation(
                LinearLayout.VERTICAL
        );

        main.setPadding(
                dp(12),
                dp(12),
                dp(12),
                dp(25)
        );

        scrollView.addView(main);

        TextView title =
                makeText(
                        text(
                                "💱 صرافی تجربه‌ها",
                                "💱 Tajro Exchange",
                                "💱 د تجربو صرافي",
                                "💱 تجربہ ایکسچینج",
                                "💱 तजरो एक्सचेंज"
                        ),
                        25
                );

        title.setGravity(Gravity.CENTER);

        main.addView(title);

        TextView subtitle =
                makeText(
                        text(
                                "ثبت و مدیریت خرید و فروش ارز",
                                "Register and manage currency buying and selling",
                                "د اسعارو پېر او پلور ثبت او مدیریت",
                                "کرنسی کی خرید و فروخت کا اندراج اور انتظام",
                                "मुद्रा खरीद और बिक्री का प्रबंधन"
                        ),
                        17
                );

        subtitle.setGravity(Gravity.CENTER);

        main.addView(subtitle);

        customerNameInput =
                makeInput(
                        text(
                                "نام مشتری",
                                "Customer name",
                                "د پېرودونکي نوم",
                                "صارف کا نام",
                                "ग्राहक का नाम"
                        )
                );

        main.addView(customerNameInput);

        phoneInput =
                makeInput(
                        text(
                                "شماره تلفن",
                                "Phone number",
                                "د ټیلیفون شمېره",
                                "فون نمبر",
                                "फ़ोन नंबर"
                        )
                );

        phoneInput.setInputType(
                InputType.TYPE_CLASS_PHONE
        );

        main.addView(phoneInput);

        main.addView(
                makeText(
                        text(
                                "انتخاب ارز",
                                "Select currency",
                                "اسعار وټاکئ",
                                "کرنسی منتخب کریں",
                                "मुद्रा चुनें"
                        ),
                        16
                )
        );

        currencySpinner =
                new Spinner(this);

        ArrayAdapter<String> currencyAdapter =
                createCurrencyAdapter(currencies);

        currencyAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        currencySpinner.setAdapter(
                currencyAdapter
        );

        main.addView(currencySpinner);

        main.addView(
                makeText(
                        text(
                                "نوع معامله",
                                "Transaction type",
                                "د معاملې ډول",
                                "معاملے کی قسم",
                                "लेन-देन का प्रकार"
                        ),
                        16
                )
        );

        typeSpinner =
                new Spinner(this);

        String[] types = {
                "خرید",
                "فروش"
        };

        String[] displayedTypes = {
                getDisplayedTransactionType("خرید"),
                getDisplayedTransactionType("فروش")
        };

        ArrayAdapter<String> typeAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        displayedTypes
                );

        typeAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        typeSpinner.setAdapter(typeAdapter);

        main.addView(typeSpinner);

        amountInput =
                makeInput(
                        text(
                                "مقدار ارز",
                                "Currency amount",
                                "د اسعارو اندازه",
                                "کرنسی کی مقدار",
                                "मुद्रा की मात्रा"
                        )
                );

        amountInput.setInputType(
                InputType.TYPE_CLASS_NUMBER |
                        InputType.TYPE_NUMBER_FLAG_DECIMAL
        );

        main.addView(amountInput);

        rateInput =
                makeInput(
                        text(
                                "نرخ هر واحد به افغانی",
                                "Rate per unit in Afghani",
                                "د هر واحد نرخ په افغانۍ",
                                "فی یونٹ افغانی نرخ",
                                "प्रति इकाई अफ़गानी दर"
                        )
                );

        rateInput.setInputType(
                InputType.TYPE_CLASS_NUMBER |
                        InputType.TYPE_NUMBER_FLAG_DECIMAL
        );

        main.addView(rateInput);

        noteInput =
                makeInput(
                        text(
                                "یادداشت / توضیحات",
                                "Note / description",
                                "یادښت / توضیحات",
                                "نوٹ / وضاحت",
                                "नोट / विवरण"
                        )
                );

        main.addView(noteInput);

        totalText =
                makeText(
                        text(
                                "مجموع: 0 افغانی",
                                "Total: 0 Afghani",
                                "ټول: ۰ افغانۍ",
                                "کل: 0 افغانی",
                                "कुल: 0 अफ़गानी"
                        ),
                        18
                );

        totalText.setGravity(Gravity.CENTER);

        main.addView(totalText);

        balanceText =
                makeText(
                        text(
                                "موجودی: 0",
                                "Balance: 0",
                                "موجودي: ۰",
                                "بیلنس: 0",
                                "शेष: 0"
                        ),
                        17
                );

        balanceText.setGravity(Gravity.CENTER);

        main.addView(balanceText);

        Button calculateButton =
                makeButton(
                        text(
                                "🧮 محاسبه",
                                "🧮 Calculate",
                                "🧮 حساب",
                                "🧮 حساب کریں",
                                "🧮 गणना"
                        )
                );

        main.addView(calculateButton);

        calculateButton.setOnClickListener(
                v -> calculateTotal()
        );

        Button saveButton =
                makeButton(
                        text(
                                "💾 ثبت معامله",
                                "💾 Save transaction",
                                "💾 معامله ثبت کړئ",
                                "💾 معاملہ محفوظ کریں",
                                "💾 लेन-देन सहेजें"
                        )
                );

        main.addView(saveButton);

        saveButton.setOnClickListener(
                v -> saveTransaction()
        );

        Button historyButton =
                makeButton(
                        text(
                                "📜 تاریخچه معاملات",
                                "📜 Transaction history",
                                "📜 د معاملو تاریخچه",
                                "📜 معاملات کی تاریخ",
                                "📜 लेन-देन इतिहास"
                        )
                );

        main.addView(historyButton);

        historyButton.setOnClickListener(
                v -> showHistory()
        );

        Button customersButton =
                makeButton(
                        text(
                                "👤 مشتریان",
                                "👤 Customers",
                                "👤 پېرودونکي",
                                "👤 صارفین",
                                "👤 ग्राहक"
                        )
                );

        main.addView(customersButton);

        customersButton.setOnClickListener(
                v -> showCustomers()
        );

        Button reportButton =
                makeButton(
                        text(
                                "📊 گزارش صرافی",
                                "📊 Exchange report",
                                "📊 د صرافۍ راپور",
                                "📊 ایکسچینج رپورٹ",
                                "📊 एक्सचेंज रिपोर्ट"
                        )
                );

        main.addView(reportButton);

        reportButton.setOnClickListener(
                v -> showReport()
        );

        // =====================================================
        // مدیریت موجودی
        // =====================================================

        Button manageBalanceButton =
                makeButton(
                        text(
                                "💰 مدیریت موجودی",
                                "💰 Balance management",
                                "💰 د موجودۍ مدیریت",
                                "💰 بیلنس کا انتظام",
                                "💰 शेष प्रबंधन"
                        )
                );

        main.addView(manageBalanceButton);

        manageBalanceButton.setOnClickListener(
                v -> showBalanceManager()
        );

        Button balancesButton =
                makeButton(
                        text(
                                "💰 موجودی همه ارزها",
                                "💰 All currency balances",
                                "💰 د ټولو اسعارو موجودي",
                                "💰 تمام کرنسیوں کا بیلنس",
                                "💰 सभी मुद्राओं का शेष"
                        )
                );

        main.addView(balancesButton);

        balancesButton.setOnClickListener(
                v -> showBalances()
        );

        Button converterButton =
                makeButton(
                        text(
                                "🔄 تبدیل ارز",
                                "🔄 Currency converter",
                                "🔄 د اسعارو بدلول",
                                "🔄 کرنسی تبدیل کریں",
                                "🔄 मुद्रा परिवर्तक"
                        )
                );

        main.addView(converterButton);

        converterButton.setOnClickListener(
                v -> showConverter()
        );

        Button vaultButton =
                makeButton(
                        text(
                                "🔐 گاوصندوق هوشمند",
                                "🔐 Smart vault",
                                "🔐 سمارټ خزانه",
                                "🔐 اسمارٹ والٹ",
                                "🔐 स्मार्ट तिजोरी"
                        )
                );

        main.addView(vaultButton);

        vaultButton.setOnClickListener(
                v -> {

                    try {

                        startActivity(
                                new Intent(
                                        this,
                                        VaultActivity.class
                                )
                        );

                    } catch (Exception e) {

                        Toast.makeText(
                                this,
                                text(
                                        "گاوصندوق در دسترس نیست",
                                        "Vault is not available",
                                        "خزانه شتون نه لري",
                                        "والٹ دستیاب نہیں ہے",
                                        "तिजोरी उपलब्ध नहीं है"
                                ),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );

        Button settingsButton =
                makeButton(
                        text(
                                "⚙️ تنظیمات",
                                "⚙️ Settings",
                                "⚙️ تنظیمات",
                                "⚙️ ترتیبات",
                                "⚙️ सेटिंग्स"
                        )
                );

        main.addView(settingsButton);

        settingsButton.setOnClickListener(
                v -> showSettings()
        );

        currencySpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {

                        updateBalance();
                        updateRate();
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent
                    ) {
                    }
                }
        );

        setContentView(scrollView);

        updateBalance();
        updateRate();
    }

    // =========================================================
    // مدیریت موجودی
    // =========================================================

    private void showBalanceManager() {

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setPadding(
                dp(15),
                dp(5),
                dp(15),
                dp(5)
        );

        Spinner spinner =
                new Spinner(this);

        ArrayAdapter<String> adapter =
                createCurrencyAdapter(currencies);

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinner.setAdapter(adapter);

        TextView currentText =
                makeText(
                        text(
                                "موجودی فعلی: 0",
                                "Current balance: 0",
                                "اوسنی موجودي: ۰",
                                "موجودہ بیلنس: 0",
                                "वर्तमान शेष: 0"
                        ),
                        17
                );

        currentText.setGravity(
                Gravity.CENTER
        );

        EditText amount =
                makeInput(
                        text(
                                "مقدار موجودی جدید",
                                "New balance amount",
                                "د نوې موجودۍ اندازه",
                                "نئے بیلنس کی مقدار",
                                "नए शेष की मात्रा"
                        )
                );

        amount.setInputType(
                InputType.TYPE_CLASS_NUMBER |
                        InputType.TYPE_NUMBER_FLAG_DECIMAL
        );

        Button save =
                makeButton(
                        text(
                                "💾 ذخیره موجودی",
                                "💾 Save balance",
                                "💾 موجودي خوندي کړئ",
                                "💾 بیلنس محفوظ کریں",
                                "💾 शेष सहेजें"
                        )
                );

        layout.addView(
                makeText(
                        text(
                                "ارز را انتخاب کنید",
                                "Select currency",
                                "اسعار وټاکئ",
                                "کرنسی منتخب کریں",
                                "मुद्रा चुनें"
                        ),
                        16
                )
        );

        layout.addView(spinner);
        layout.addView(currentText);
        layout.addView(amount);
        layout.addView(save);

        spinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {

                        String currency =
                                currencies[position];

                        currentText.setText(
                                text(
                                        "موجودی فعلی: ",
                                        "Current balance: ",
                                        "اوسنی موجودي: ",
                                        "موجودہ بیلنس: ",
                                        "वर्तमान शेष: "
                                ) +
                                        getCurrencyFlag(currency) +
                                        " " +
                                        formatNumber(
                                                exchangeData
                                                        .getBalance(
                                                                currency
                                                        )
                                        )
                        );
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent
                    ) {
                    }
                }
        );

        save.setOnClickListener(
                v -> {

                    String currency =
                            spinner
                                    .getSelectedItem()
                                    .toString();

                    String value =
                            amount.getText()
                                    .toString()
                                    .trim();

                    if (value.isEmpty()) {

                        Toast.makeText(
                                this,
                                text(
                                        "مقدار موجودی را وارد کنید",
                                        "Enter the balance amount",
                                        "د موجودۍ اندازه ولیکئ",
                                        "بیلنس کی مقدار درج کریں",
                                        "शेष की मात्रा दर्ज करें"
                                ),
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    try {

                        double newBalance =
                                Double.parseDouble(value);

                        if (
                                Double.isNaN(newBalance) ||
                                Double.isInfinite(newBalance) ||
                                newBalance < 0
                        ) {
                            throw new Exception();
                        }

                        exchangeData.setBalance(
                                currency,
                                newBalance
                        );

                        updateBalance();

                        currentText.setText(
                                text(
                                        "موجودی فعلی: ",
                                        "Current balance: ",
                                        "اوسنی موجودي: ",
                                        "موجودہ بیلنس: ",
                                        "वर्तमान शेष: "
                                ) +
                                        getCurrencyFlag(currency) +
                                        " " +
                                        formatNumber(newBalance)
                        );

                        amount.setText("");

                        Toast.makeText(
                                this,
                                text(
                                        "✅ موجودی " +
                                                getCurrencyDisplayName(currency) +
                                                " ذخیره شد",
                                        "✅ Balance of " +
                                                getCurrencyDisplayName(currency) +
                                                " saved",
                                        "✅ د " +
                                                getCurrencyDisplayName(currency) +
                                                " موجودي خوندي شوه",
                                        "✅ " +
                                                getCurrencyDisplayName(currency) +
                                                " کا بیلنس محفوظ ہوگیا",
                                        "✅ " +
                                                getCurrencyDisplayName(currency) +
                                                " का शेष सहेजा गया"
                                ),
                                Toast.LENGTH_SHORT
                        ).show();

                    } catch (Exception e) {

                        Toast.makeText(
                                this,
                                text(
                                        "مقدار موجودی نادرست است",
                                        "Invalid balance amount",
                                        "د موجودۍ اندازه ناسمه ده",
                                        "بیلنس کی مقدار غلط ہے",
                                        "शेष की मात्रा गलत है"
                                ),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );

        new AlertDialog.Builder(this)
                .setTitle(
                        text(
                                "💰 مدیریت موجودی",
                                "💰 Balance management",
                                "💰 د موجودۍ مدیریت",
                                "💰 بیلنس کا انتظام",
                                "💰 शेष प्रबंधन"
                        )
                )
                .setView(layout)
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
    }

    // =========================================================
    // نرخ
    // =========================================================

    private void updateRate() {

        if (currencySpinner == null) {
            return;
        }

        Object selected =
                currencySpinner.getSelectedItem();

        if (selected == null) {
            return;
        }

        String currency =
                selected.toString();

        if (ExchangeData.AFN.equals(currency)) {

            rateInput.setText("1");
            return;
        }

        double rate =
                exchangeData.getRate(currency);

        if (rate > 0) {

            rateInput.setText(
                    formatNumber(rate)
            );
        }
    }

    // =========================================================
    // موجودی
    // =========================================================

    private void updateBalance() {

        if (
                exchangeData == null ||
                currencySpinner == null ||
                balanceText == null
        ) {
            return;
        }

        Object selected =
                currencySpinner.getSelectedItem();

        if (selected == null) {
            return;
        }

        String currency =
                selected.toString();

        double balance =
                exchangeData.getBalance(currency);

        balanceText.setText(
                text(
                        "موجودی ",
                        "Balance ",
                        "موجودي ",
                        "بیلنس ",
                        "शेष "
                ) +
                        getCurrencyFlag(currency) +
                        " " +
                        getCurrencyDisplayName(currency) +
                        ": " +
                        formatNumber(balance)
        );
    }

    // =========================================================
    // محاسبه
    // =========================================================

    private void calculateTotal() {

        try {

            double amount =
                    Double.parseDouble(
                            amountInput
                                    .getText()
                                    .toString()
                                    .trim()
                    );

            double rate =
                    Double.parseDouble(
                            rateInput
                                    .getText()
                                    .toString()
                                    .trim()
                    );

            if (amount <= 0 || rate <= 0) {
                throw new Exception();
            }

            double total = amount * rate;

            totalText.setText(
                    text(
                            "مجموع: ",
                            "Total: ",
                            "ټول: ",
                            "کل: ",
                            "कुल: "
                    ) +
                            formatNumber(total) +
                            " " +
                            text(
                                    "افغانی",
                                    "Afghani",
                                    "افغانۍ",
                                    "افغانی",
                                    "अफ़गानी"
                            )
            );

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    text(
                            "مقدار و نرخ را درست وارد کنید",
                            "Enter a valid amount and rate",
                            "اندازه او نرخ سم ولیکئ",
                            "مقدار اور نرخ درست درج کریں",
                            "मात्रा और दर सही दर्ज करें"
                    ),
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // =========================================================
    // ثبت معامله
    // =========================================================

    private void saveTransaction() {

        String customerName =
                customerNameInput.getText()
                        .toString()
                        .trim();

        String phone =
                phoneInput.getText()
                        .toString()
                        .trim();

        Object currencyObject =
                currencySpinner.getSelectedItem();

        if (currencyObject == null) {

            Toast.makeText(
                    this,
                    text(
                            "ارز و نوع معامله را انتخاب کنید",
                            "Select currency and transaction type",
                            "اسعار او د معاملې ډول وټاکئ",
                            "کرنسی اور معاملے کی قسم منتخب کریں",
                            "मुद्रा और लेन-देन का प्रकार चुनें"
                    ),
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String currency =
                currencyObject.toString();

        int typePosition =
                typeSpinner.getSelectedItemPosition();

        String type;

        if (typePosition == 0) {
            type = "خرید";
        } else {
            type = "فروش";
        }

        String amountText =
                amountInput.getText()
                        .toString()
                        .trim();

        String rateText =
                rateInput.getText()
                        .toString()
                        .trim();

        String note =
                noteInput.getText()
                        .toString()
                        .trim();

        if (amountText.isEmpty()) {

            Toast.makeText(
                    this,
                    text(
                            "مقدار ارز را وارد کنید",
                            "Enter the currency amount",
                            "د اسعارو اندازه ولیکئ",
                            "کرنسی کی مقدار درج کریں",
                            "मुद्रा की मात्रा दर्ज करें"
                    ),
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (rateText.isEmpty()) {

            Toast.makeText(
                    this,
                    text(
                            "نرخ ارز را وارد کنید",
                            "Enter the exchange rate",
                            "د اسعارو نرخ ولیکئ",
                            "کرنسی کی شرح درج کریں",
                            "विनिमय दर दर्ज करें"
                    ),
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        double amount;
        double rate;

        try {

            amount =
                    Double.parseDouble(amountText);

            rate =
                    Double.parseDouble(rateText);

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    text(
                            "مقدار یا نرخ نادرست است",
                            "Invalid amount or rate",
                            "اندازه یا نرخ ناسم دی",
                            "مقدار یا شرح غلط ہے",
                            "मात्रा या दर गलत है"
                    ),
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (
                Double.isNaN(amount) ||
                Double.isInfinite(amount) ||
                Double.isNaN(rate) ||
                Double.isInfinite(rate) ||
                amount <= 0 ||
                rate <= 0
        ) {

            Toast.makeText(
                    this,
                    text(
                            "مقدار و نرخ باید بیشتر از صفر باشد",
                            "Amount and rate must be greater than zero",
                            "اندازه او نرخ باید له صفر څخه زیات وي",
                            "مقدار اور شرح صفر سے زیادہ ہونی چاہیے",
                            "मात्रा और दर शून्य से अधिक होनी चाहिए"
                    ),
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (ExchangeData.AFN.equals(currency)) {

            Toast.makeText(
                    this,
                    text(
                            "افغانی ارز پایه است؛ ارز خارجی را انتخاب کنید.",
                            "Afghani is the base currency; select a foreign currency.",
                            "افغانۍ بنسټیز اسعار دي؛ بهرني اسعار وټاکئ.",
                            "افغانی بنیادی کرنسی ہے؛ غیر ملکی کرنسی منتخب کریں۔",
                            "अफ़गानी आधार मुद्रा है; विदेशी मुद्रा चुनें।"
                    ),
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        double total = amount * rate;

        double afnBalance =
                exchangeData.getBalance(
                        ExchangeData.AFN
                );

        double currencyBalance =
                exchangeData.getBalance(currency);

        if ("خرید".equals(type)) {

            if (afnBalance < total) {

                Toast.makeText(
                        this,
                        text(
                                "موجودی افغانی صرافی کافی نیست.",
                                "The exchange does not have enough Afghani balance.",
                                "د صرافۍ افغانۍ موجودي کافي نه ده.",
                                "ایکسچینج میں افغانی کا بیلنس کافی نہیں ہے۔",
                                "एक्सचेंज में अफ़गानी का शेष पर्याप्त नहीं है।"
                        ),
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

        } else {

            if (currencyBalance < amount) {

                Toast.makeText(
                        this,
                        text(
                                "موجودی " +
                                        getCurrencyDisplayName(currency) +
                                        " کافی نیست.",
                                "Not enough " +
                                        getCurrencyDisplayName(currency) +
                                        " balance.",
                                "د " +
                                        getCurrencyDisplayName(currency) +
                                        " موجودي کافي نه ده.",
                                getCurrencyDisplayName(currency) +
                                        " کا بیلنس کافی نہیں ہے۔",
                                getCurrencyDisplayName(currency) +
                                        " का शेष पर्याप्त नहीं है।"
                        ),
                        Toast.LENGTH_LONG
                ).show();

                return;
            }
        }

        boolean saved =
                exchangeData.saveTransaction(
                        customerName,
                        phone,
                        currency,
                        type,
                        amount,
                        rate,
                        "نقدی",
                        note
                );

        if (!saved) {

            Toast.makeText(
                    this,
                    text(
                            "خطا در ثبت معامله؛ موجودی تغییر نکرد.",
                            "Transaction could not be saved; balance was not changed.",
                            "د معاملې په ثبتولو کې ستونزه؛ موجودي بدله نه شوه.",
                            "معاملہ محفوظ نہیں ہوا؛ بیلنس تبدیل نہیں ہوا۔",
                            "लेन-देन सहेजा नहीं गया; शेष नहीं बदला।"
                    ),
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if ("خرید".equals(type)) {

            exchangeData.setBalance(
                    ExchangeData.AFN,
                    afnBalance - total
            );

            exchangeData.addBalance(
                    currency,
                    amount
            );

        } else {

            exchangeData.setBalance(
                    currency,
                    currencyBalance - amount
            );

            exchangeData.addBalance(
                    ExchangeData.AFN,
                    total
            );
        }

        exchangeData.setRate(
                currency,
                rate
        );

        totalText.setText(
                text(
                        "مجموع: ",
                        "Total: ",
                        "ټول: ",
                        "کل: ",
                        "कुल: "
                ) +
                        formatNumber(total) +
                        " " +
                        text(
                                "افغانی",
                                "Afghani",
                                "افغانۍ",
                                "افغانی",
                                "अफ़गानी"
                        )
        );

        updateBalance();

        long transactionId =
                System.currentTimeMillis();

        String date =
                formatDate(transactionId);

        String receipt =
                createReceiptText(
                        transactionId,
                        customerName,
                        phone,
                        currency,
                        type,
                        amount,
                        rate,
                        total,
                        note,
                        date
                );

        customerNameInput.setText("");
        phoneInput.setText("");
        amountInput.setText("");
        rateInput.setText("");
        noteInput.setText("");

        updateRate();

        Toast.makeText(
                this,
                text(
                        "✅ معامله با موفقیت ثبت شد",
                        "✅ Transaction saved successfully",
                        "✅ معامله په بریالیتوب ثبت شوه",
                        "✅ معاملہ کامیابی سے محفوظ ہوگیا",
                        "✅ लेन-देन सफलतापूर्वक सहेजा गया"
                ),
                Toast.LENGTH_SHORT
        ).show();

        showTransactionReceipt(receipt);
    }

    // =========================================================
    // رسید
    // =========================================================

    private String createReceiptText(
            long transactionId,
            String customerName,
            String phone,
            String currency,
            String type,
            double amount,
            double rate,
            double total,
            String note,
            String date
    ) {

        StringBuilder receipt =
                new StringBuilder();

        receipt.append(
                "━━━━━━━━━━━━━━━━━━━━\n"
        );

        receipt.append(
                "       💱 " +
                        text(
                                "تجربه‌ها",
                                "Tajrobehha",
                                "تجربې",
                                "تجربے",
                                "तजरोबेहा"
                        ) +
                        "\n"
        );

        receipt.append(
                "       🧾 " +
                        text(
                                "رسید معامله",
                                "Transaction receipt",
                                "د معاملې رسید",
                                "معاملے کی رسید",
                                "लेन-देन रसीद"
                        ) +
                        "\n"
        );

        receipt.append(
                "━━━━━━━━━━━━━━━━━━━━\n\n"
        );

        receipt.append(
                "🆔 " +
                        text(
                                "شناسه معامله: ",
                                "Transaction ID: ",
                                "د معاملې پېژندنه: ",
                                "معاملے کی شناخت: ",
                                "लेन-देन आईडी: "
                        )
        );

        receipt.append(transactionId);
        receipt.append("\n");

        receipt.append(
                "📅 " +
                        text(
                                "تاریخ هجری: ",
                                "Hijri date: ",
                                "هجري نېټه: ",
                                "ہجری تاریخ: ",
                                "हिजरी तारीख: "
                        )
        );

        receipt.append(date);
        receipt.append("\n\n");

        receipt.append(
                "👤 " +
                        text(
                                "مشتری: ",
                                "Customer: ",
                                "پېرودونکی: ",
                                "صارف: ",
                                "ग्राहक: "
                        )
        );

        receipt.append(
                customerName.isEmpty()
                        ? text(
                                "بدون نام",
                                "No name",
                                "بې نومه",
                                "بغیر نام",
                                "बिना नाम"
                        )
                        : customerName
        );

        receipt.append("\n");

        if (!phone.isEmpty()) {

            receipt.append(
                    "📱 " +
                            text(
                                    "تلفن: ",
                                    "Phone: ",
                                    "ټیلیفون: ",
                                    "فون: ",
                                    "फ़ोन: "
                            )
            );

            receipt.append(phone);
            receipt.append("\n");
        }

        receipt.append("\n");

        receipt.append(
                "🔄 " +
                        text(
                                "نوع معامله: ",
                                "Transaction type: ",
                                "د معاملې ډول: ",
                                "معاملے کی قسم: ",
                                "लेन-देन प्रकार: "
                        )
        );

        receipt.append(
                getDisplayedTransactionType(type)
        );

        receipt.append("\n");

        receipt.append(
                "💱 " +
                        text(
                                "ارز: ",
                                "Currency: ",
                                "اسعار: ",
                                "کرنسی: ",
                                "मुद्रा: "
                        )
        );

        receipt.append(
                getCurrencyFlag(currency)
        );

        receipt.append(" ");
        receipt.append(
                getCurrencyDisplayName(currency)
        );
        receipt.append("\n");

        receipt.append(
                "📦 " +
                        text(
                                "مقدار: ",
                                "Amount: ",
                                "اندازه: ",
                                "مقدار: ",
                                "मात्रा: "
                        )
        );

        receipt.append(
                formatNumber(amount)
        );

        receipt.append("\n");

        receipt.append(
                "📈 " +
                        text(
                                "نرخ هر واحد: ",
                                "Rate per unit: ",
                                "د هر واحد نرخ: ",
                                "فی یونٹ شرح: ",
                                "प्रति इकाई दर: "
                        )
        );

        receipt.append(
                formatNumber(rate)
        );

        receipt.append(
                " " +
                        text(
                                "افغانی",
                                "Afghani",
                                "افغانۍ",
                                "افغانی",
                                "अफ़गानी"
                        ) +
                        "\n"
        );

        receipt.append(
                "💰 " +
                        text(
                                "مبلغ نهایی: ",
                                "Final amount: ",
                                "وروستۍ اندازه: ",
                                "حتمی رقم: ",
                                "अंतिम राशि: "
                        )
        );

        receipt.append(
                formatNumber(total)
        );

        receipt.append(
                " " +
                        text(
                                "افغانی",
                                "Afghani",
                                "افغانۍ",
                                "افغانی",
                                "अफ़गानी"
                        ) +
                        "\n"
        );

        if (!note.isEmpty()) {

            receipt.append("\n");
            receipt.append(
                    "📝 " +
                            text(
                                    "یادداشت: ",
                                    "Note: ",
                                    "یادښت: ",
                                    "نوٹ: ",
                                    "नोट: "
                            )
            );
            receipt.append(note);
            receipt.append("\n");
        }

        receipt.append("\n");
        receipt.append(
                "💵 " +
                        text(
                                "وضعیت حساب: ",
                                "Account status: ",
                                "د حساب حالت: ",
                                "اکاؤنٹ کی حالت: ",
                                "खाते की स्थिति: "
                        ) +
                        getDisplayedAccountStatus("نقدی") +
                        "\n"
        );

        receipt.append(
                "━━━━━━━━━━━━━━━━━━━━\n"
        );

        receipt.append(
                "        " +
                        text(
                                "تشکر از شما 🌹",
                                "Thank you 🌹",
                                "له تاسو مننه 🌹",
                                "آپ کا شکریہ 🌹",
                                "धन्यवाद 🌹"
                        ) +
                        "\n"
        );

        receipt.append(
                "━━━━━━━━━━━━━━━━━━━━"
        );

        return receipt.toString();
    }

    // =========================================================
    // نمایش رسید
    // =========================================================

    private void showTransactionReceipt(
            String receipt
    ) {

        TextView receiptView =
                makeText(receipt, 16);

        receiptView.setTextIsSelectable(true);
        receiptView.setGravity(Gravity.RIGHT);

        receiptView.setPadding(
                dp(18),
                dp(15),
                dp(18),
                dp(15)
        );

        ScrollView scrollView =
                new ScrollView(this);

        scrollView.addView(receiptView);

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                text(
                                        "🧾 رسید معامله",
                                        "🧾 Transaction receipt",
                                        "🧾 د معاملې رسید",
                                        "🧾 معاملے کی رسید",
                                        "🧾 लेन-देन रसीद"
                                )
                        )
                        .setView(scrollView)
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
                        .setNeutralButton(
                                text(
                                        "📋 کپی",
                                        "📋 Copy",
                                        "📋 کاپي",
                                        "📋 کاپی",
                                        "📋 कॉपी"
                                ),
                                null
                        )
                        .setPositiveButton(
                                text(
                                        "📤 اشتراک‌گذاری",
                                        "📤 Share",
                                        "📤 شریکول",
                                        "📤 شیئر کریں",
                                        "📤 साझा करें"
                                ),
                                null
                        )
                        .create();

        dialog.setOnShowListener(
                d -> {

                    Button copyButton =
                            dialog.getButton(
                                    AlertDialog.BUTTON_NEUTRAL
                            );

                    Button shareButton =
                            dialog.getButton(
                                    AlertDialog.BUTTON_POSITIVE
                            );

                    copyButton.setOnClickListener(
                            v -> copyReceipt(receipt)
                    );

                    shareButton.setOnClickListener(
                            v -> shareReceipt(receipt)
                    );
                }
        );

        dialog.show();
    }

    private void copyReceipt(
            String receipt
    ) {

        ClipboardManager clipboard =
                (ClipboardManager)
                        getSystemService(
                                CLIPBOARD_SERVICE
                        );

        if (clipboard != null) {

            clipboard.setPrimaryClip(
                    ClipData.newPlainText(
                            text(
                                    "رسید معامله تجربه‌ها",
                                    "Tajrobehha transaction receipt",
                                    "د تجربو د معاملې رسید",
                                    "تجربے کی معاملے کی رسید",
                                    "तजरोबेहा लेन-देन रसीद"
                            ),
                            receipt
                    )
            );

            Toast.makeText(
                    this,
                    text(
                            "📋 رسید کپی شد",
                            "📋 Receipt copied",
                            "📋 رسید کاپي شو",
                            "📋 رسید کاپی ہوگئی",
                            "📋 रसीद कॉपी की गई"
                    ),
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void shareReceipt(
            String receipt
    ) {

        Intent shareIntent =
                new Intent(Intent.ACTION_SEND);

        shareIntent.setType("text/plain");

        shareIntent.putExtra(
                Intent.EXTRA_TITLE,
                text(
                        "🧾 رسید معامله تجربه‌ها",
                        "🧾 Tajrobehha transaction receipt",
                        "🧾 د تجربو د معاملې رسید",
                        "🧾 تجربے کی معاملے کی رسید",
                        "🧾 तजरोबेहा लेन-देन रसीद"
                )
        );

        shareIntent.putExtra(
                Intent.EXTRA_SUBJECT,
                text(
                        "رسید معامله تجربه‌ها",
                        "Tajrobehha transaction receipt",
                        "د تجربو د معاملې رسید",
                        "تجربے کی معاملے کی رسید",
                        "तजरोबेहा लेन-देन रसीद"
                )
        );

        shareIntent.putExtra(
                Intent.EXTRA_TEXT,
                receipt
        );

        if (shareIntent.resolveActivity(
                getPackageManager()
        ) != null) {

            startActivity(
                    Intent.createChooser(
                            shareIntent,
                            text(
                                    "📤 ارسال رسید با...",
                                    "📤 Send receipt with...",
                                    "📤 رسید په دې سره ولېږئ...",
                                    "📤 رسید اس کے ذریعے بھیجیں...",
                                    "📤 रसीद इसके साथ भेजें..."
                            )
                    )
            );

        } else {

            Toast.makeText(
                    this,
                    text(
                            "برنامه‌ای برای اشتراک‌گذاری پیدا نشد",
                            "No app found for sharing",
                            "د شریکولو لپاره کوم اپ ونه موندل شو",
                            "شیئر کرنے کے لیے کوئی ایپ نہیں ملی",
                            "साझा करने के लिए कोई ऐप नहीं मिला"
                    ),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // =========================================================
    // تاریخچه
    // =========================================================

    private void showHistory() {

        JSONArray transactions =
                exchangeData.getTransactionsArray();

        if (
                transactions == null ||
                transactions.length() == 0
        ) {

            new AlertDialog.Builder(this)
                    .setTitle(
                            text(
                                    "📜 تاریخچه معاملات",
                                    "📜 Transaction history",
                                    "📜 د معاملو تاریخچه",
                                    "📜 معاملات کی تاریخ",
                                    "📜 लेन-देन इतिहास"
                            )
                    )
                    .setMessage(
                            text(
                                    "هنوز معامله‌ای ثبت نشده است.",
                                    "No transactions have been recorded yet.",
                                    "تر اوسه هېڅ معامله نه ده ثبت شوې.",
                                    "ابھی تک کوئی معاملہ درج نہیں ہوا۔",
                                    "अभी तक कोई लेन-देन दर्ज नहीं हुआ है।"
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

        LinearLayout list =
                new LinearLayout(this);

        list.setOrientation(
                LinearLayout.VERTICAL
        );

        list.setPadding(
                dp(10),
                dp(5),
                dp(10),
                dp(10)
        );

        for (
                int i = transactions.length() - 1;
                i >= 0;
                i--
        ) {

            JSONObject transaction =
                    transactions.optJSONObject(i);

            if (transaction == null) {
                continue;
            }

            String customer =
                    transaction.optString(
                            "customerName",
                            "بدون نام"
                    );

            String currency =
                    transaction.optString(
                            "currency",
                            ""
                    );

            String type =
                    transaction.optString(
                            "type",
                            ""
                    );

            double amount =
                    transaction.optDouble(
                            "amount",
                            0
                    );

            double rate =
                    transaction.optDouble(
                            "rate",
                            0
                    );

            double total =
                    transaction.optDouble(
                            "total",
                            0
                    );

            String phone =
                    transaction.optString(
                            "phone",
                            ""
                    );

            String accountStatus =
                    transaction.optString(
                            "accountStatus",
                            "نقدی"
                    );

            String note =
                    transaction.optString(
                            "note",
                            ""
                    );

            long date =
                    transaction.optLong(
                            "date",
                            0
                    );

            String itemText =
                    "👤 " +
                            text(
                                    "مشتری: ",
                                    "Customer: ",
                                    "پېرودونکی: ",
                                    "صارف: ",
                                    "ग्राहक: "
                            ) +
                            customer +

                            "\n📱 " +
                            text(
                                    "تلفن: ",
                                    "Phone: ",
                                    "ټیلیفون: ",
                                    "فون: ",
                                    "फ़ोन: "
                            ) +
                            phone +

                            "\n💱 " +
                            text(
                                    "نوع: ",
                                    "Type: ",
                                    "ډول: ",
                                    "قسم: ",
                                    "प्रकार: "
                            ) +
                            getDisplayedTransactionType(type) +

                            "\n💵 " +
                            text(
                                    "ارز: ",
                                    "Currency: ",
                                    "اسعار: ",
                                    "کرنسی: ",
                                    "मुद्रा: "
                            ) +
                            getCurrencyFlag(currency) +
                            " " +
                            getCurrencyDisplayName(currency) +

                            "\n📦 " +
                            text(
                                    "مقدار: ",
                                    "Amount: ",
                                    "اندازه: ",
                                    "مقدار: ",
                                    "मात्रा: "
                            ) +
                            formatNumber(amount) +

                            "\n📈 " +
                            text(
                                    "نرخ: ",
                                    "Rate: ",
                                    "نرخ: ",
                                    "شرح: ",
                                    "दर: "
                            ) +
                            formatNumber(rate) +

                            "\n💰 " +
                            text(
                                    "مجموع: ",
                                    "Total: ",
                                    "ټول: ",
                                    "کل: ",
                                    "कुल: "
                            ) +
                            formatNumber(total) +
                            " " +
                            text(
                                    "افغانی",
                                    "Afghani",
                                    "افغانۍ",
                                    "افغانی",
                                    "अफ़गानी"
                            ) +

                            "\n📋 " +
                            text(
                                    "حساب: ",
                                    "Account: ",
                                    "حساب: ",
                                    "اکاؤنٹ: ",
                                    "खाता: "
                            ) +
                            getDisplayedAccountStatus(accountStatus) +

                            "\n📝 " +
                            text(
                                    "یادداشت: ",
                                    "Note: ",
                                    "یادښت: ",
                                    "نوٹ: ",
                                    "नोट: "
                            ) +
                            note +

                            "\n📅 " +
                            text(
                                    "تاریخ هجری: ",
                                    "Hijri date: ",
                                    "هجري نېټه: ",
                                    "ہجری تاریخ: ",
                                    "हिजरी तारीख: "
                            ) +
                            formatDate(date);

            TextView item =
                    makeText(
                            itemText,
                            15
                    );

            list.addView(item);

            View line =
                    new View(this);

            line.setLayoutParams(
                    new LinearLayout.LayoutParams(
                            -1,
                            dp(1)
                    )
            );

            list.addView(line);
        }

        ScrollView scroll =
                new ScrollView(this);

        scroll.addView(list);

        new AlertDialog.Builder(this)
                .setTitle(
                        text(
                                "📜 تاریخچه معاملات",
                                "📜 Transaction history",
                                "📜 د معاملو تاریخچه",
                                "📜 معاملات کی تاریخ",
                                "📜 लेन-देन इतिहास"
                        )
                )
                .setView(scroll)
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
    }

    // =========================================================
    // مشتریان + جستجو
    // =========================================================

    private void showCustomers() {

        List<String> names =
                exchangeData.getCustomerNames();

        if (
                names == null ||
                names.isEmpty()
        ) {

            new AlertDialog.Builder(this)
                    .setTitle(
                            text(
                                    "👤 مشتریان",
                                    "👤 Customers",
                                    "👤 پېرودونکي",
                                    "👤 صارفین",
                                    "👤 ग्राहक"
                            )
                    )
                    .setMessage(
                            text(
                                    "هنوز مشتری ثبت نشده است.",
                                    "No customers have been registered yet.",
                                    "تر اوسه هېڅ پېرودونکی نه دی ثبت شوی.",
                                    "ابھی تک کوئی صارف درج نہیں ہوا۔",
                                    "अभी तक कोई ग्राहक दर्ज नहीं हुआ है।"
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

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setPadding(
                dp(10),
                dp(5),
                dp(10),
                dp(5)
        );

        SearchView searchView =
                new SearchView(this);

        searchView.setIconifiedByDefault(false);

        searchView.setQueryHint(
                text(
                        "🔎 جستجوی نام مشتری",
                        "🔎 Search customer name",
                        "🔎 د پېرودونکي نوم ولټوئ",
                        "🔎 صارف کا نام تلاش کریں",
                        "🔎 ग्राहक का नाम खोजें"
                )
        );

        layout.addView(searchView);

        TextView resultText =
                makeText("", 16);

        resultText.setPadding(
                dp(8),
                dp(15),
                dp(8),
                dp(15)
        );

        ScrollView resultScroll =
                new ScrollView(this);

        resultScroll.addView(resultText);

        LinearLayout.LayoutParams resultParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(400)
                );

        resultScroll.setLayoutParams(
                resultParams
        );

        layout.addView(resultScroll);

        resultText.setText(
                buildCustomerSearchResult(
                        names,
                        ""
                )
        );

        searchView.setOnQueryTextListener(
                new SearchView.OnQueryTextListener() {

                    @Override
                    public boolean onQueryTextSubmit(
                            String query
                    ) {

                        resultText.setText(
                                buildCustomerSearchResult(
                                        names,
                                        query
                                )
                        );

                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(
                            String newText
                    ) {

                        resultText.setText(
                                buildCustomerSearchResult(
                                        names,
                                        newText
                                )
                        );

                        return true;
                    }
                }
        );

        new AlertDialog.Builder(this)
                .setTitle(
                        text(
                                "👤 مشتریان",
                                "👤 Customers",
                                "👤 پېرودونکي",
                                "👤 صارفین",
                                "👤 ग्राहक"
                        )
                )
                .setView(layout)
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
    }

    // =========================================================
    // ساخت نتایج جستجوی مشتری
    // =========================================================

    private String buildCustomerSearchResult(
            List<String> names,
            String query
    ) {

        String search =
                normalizePersian(query);

        StringBuilder result =
                new StringBuilder();

        int count = 0;

        for (String name : names) {

            if (
                    name == null ||
                    name.trim().isEmpty()
            ) {
                continue;
            }

            String normalizedName =
                    normalizePersian(name);

            if (
                    !search.isEmpty() &&
                    !normalizedName.contains(search)
            ) {
                continue;
            }

            double debt =
                    exchangeData.getCustomerDebt(name);

            double credit =
                    exchangeData.getCustomerCredit(name);

            double balance =
                    exchangeData.getCustomerBalance(name);

            result.append("━━━━━━━━━━━━━━━━\n");

            result.append("👤 ")
                    .append(name)
                    .append("\n");

            result.append(
                    "🔴 " +
                            text(
                                    "بدهکاری: ",
                                    "Debt: ",
                                    "پور: ",
                                    "واجب الادا: ",
                                    "ऋण: "
                            )
            )
                    .append(formatNumber(debt))
                    .append(" ")
                    .append(
                            text(
                                    "افغانی\n",
                                    "Afghani\n",
                                    "افغانۍ\n",
                                    "افغانی\n",
                                    "अफ़गानी\n"
                            )
                    );

            result.append(
                    "🟢 " +
                            text(
                                    "طلبکاری: ",
                                    "Credit: ",
                                    "طلب: ",
                                    "قرض: ",
                                    "लेनदार: "
                            )
            )
                    .append(formatNumber(credit))
                    .append(" ")
                    .append(
                            text(
                                    "افغانی\n",
                                    "Afghani\n",
                                    "افغانۍ\n",
                                    "افغانی\n",
                                    "अफ़गानी\n"
                            )
                    );

            result.append(
                    "💰 " +
                            text(
                                    "مانده حساب: ",
                                    "Account balance: ",
                                    "د حساب پاتې: ",
                                    "اکاؤنٹ بیلنس: ",
                                    "खाते का शेष: "
                            )
            )
                    .append(formatNumber(balance))
                    .append(" ")
                    .append(
                            text(
                                    "افغانی\n",
                                    "Afghani\n",
                                    "افغانۍ\n",
                                    "افغانی\n",
                                    "अफ़गानी\n"
                            )
                    );

            count++;
        }

        if (count == 0) {

            return text(
                    "🔎 مشتری با این نام پیدا نشد.",
                    "🔎 No customer found with this name.",
                    "🔎 په دې نوم کوم پېرودونکی ونه موندل شو.",
                    "🔎 اس نام سے کوئی صارف نہیں ملا۔",
                    "🔎 इस नाम का कोई ग्राहक नहीं मिला।"
            );
        }

        return text(
                "👥 تعداد نتیجه: ",
                "👥 Results: ",
                "👥 د پایلو شمېر: ",
                "👥 نتائج کی تعداد: ",
                "👥 परिणाम: "
        ) +
                count +
                "\n\n" +
                result.toString();
    }

    // =========================================================
    // یکسان‌سازی حروف فارسی برای جستجو
    // =========================================================

    private String normalizePersian(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .trim()
                .replace('ي', 'ی')
                .replace('ى', 'ی')
                .replace('ك', 'ک')
                .toLowerCase(Locale.ROOT);
    }

    // =========================================================
    // گزارش
    // =========================================================

    private void showReport() {

        double buy =
                exchangeData.getTotalBuy();

        double sell =
                exchangeData.getTotalSell();

        int count =
                exchangeData.getTransactionCount();

        double difference =
                sell - buy;

        String report =
                "📊 " +
                        text(
                                "گزارش صرافی",
                                "Exchange report",
                                "د صرافۍ راپور",
                                "ایکسچینج رپورٹ",
                                "एक्सचेंज रिपोर्ट"
                        ) +

                        "\n\n" +
                        text(
                                "تعداد معاملات: ",
                                "Transactions: ",
                                "معاملې: ",
                                "معاملات: ",
                                "लेन-देन: "
                        ) +
                        count +

                        "\n\n" +
                        text(
                                "مجموع خرید: ",
                                "Total purchases: ",
                                "ټول پېرود: ",
                                "کل خرید: ",
                                "कुल खरीद: "
                        ) +
                        formatNumber(buy) +
                        " " +
                        text(
                                "افغانی",
                                "Afghani",
                                "افغانۍ",
                                "افغانی",
                                "अफ़गानी"
                        ) +

                        "\n\n" +
                        text(
                                "مجموع فروش: ",
                                "Total sales: ",
                                "ټول پلور: ",
                                "کل فروخت: ",
                                "कुल बिक्री: "
                        ) +
                        formatNumber(sell) +
                        " " +
                        text(
                                "افغانی",
                                "Afghani",
                                "افغانۍ",
                                "افغانی",
                                "अफ़गानी"
                        ) +

                        "\n\n" +
                        text(
                                "اختلاف فروش و خرید: ",
                                "Sales minus purchases: ",
                                "د پلور او پېرود توپیر: ",
                                "فروخت اور خرید کا فرق: ",
                                "बिक्री और खरीद का अंतर: "
                        ) +
                        formatNumber(difference) +
                        " " +
                        text(
                                "افغانی",
                                "Afghani",
                                "افغانۍ",
                                "افغانی",
                                "अफ़गानी"
                        );

        new AlertDialog.Builder(this)
                .setTitle(
                        text(
                                "📊 گزارش",
                                "📊 Report",
                                "📊 راپور",
                                "📊 رپورٹ",
                                "📊 रिपोर्ट"
                        )
                )
                .setMessage(report)
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
    }

    // =========================================================
    // موجودی همه ارزها
    // =========================================================

    private void showBalances() {

        StringBuilder result =
                new StringBuilder();

        for (String currency : currencies) {

            double balance =
                    exchangeData.getBalance(currency);

            double rate =
                    exchangeData.getRate(currency);

            result.append(
                    getCurrencyFlag(currency)
            );

            result.append(" ");
            result.append(
                    getCurrencyDisplayName(currency)
            );

            result.append(
                    "\n" +
                            text(
                                    "موجودی: ",
                                    "Balance: ",
                                    "موجودي: ",
                                    "بیلنس: ",
                                    "शेष: "
                            )
            );

            result.append(
                    formatNumber(balance)
            );

            result.append(
                    "\n" +
                            text(
                                    "نرخ: ",
                                    "Rate: ",
                                    "نرخ: ",
                                    "شرح: ",
                                    "दर: "
                            )
            );

            result.append(
                    formatNumber(rate)
            );

            result.append("\n\n");
        }

        new AlertDialog.Builder(this)
                .setTitle(
                        text(
                                "💰 موجودی همه ارزها",
                                "💰 All currency balances",
                                "💰 د ټولو اسعارو موجودي",
                                "💰 تمام کرنسیوں کا بیلنس",
                                "💰 सभी मुद्राओं का शेष"
                        )
                )
                .setMessage(result.toString())
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
    }

    // =========================================================
    // تبدیل ارز
    // =========================================================

    private void showConverter() {

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setPadding(
                dp(15),
                dp(5),
                dp(15),
                dp(5)
        );

        Spinner fromSpinner =
                new Spinner(this);

        Spinner toSpinner =
                new Spinner(this);

        ArrayAdapter<String> adapterFrom =
                createCurrencyAdapter(currencies);

        adapterFrom.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        ArrayAdapter<String> adapterTo =
                createCurrencyAdapter(currencies);

        adapterTo.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        fromSpinner.setAdapter(adapterFrom);
        toSpinner.setAdapter(adapterTo);

        EditText amount =
                makeInput(
                        text(
                                "مقدار برای تبدیل",
                                "Amount to convert",
                                "د بدلولو اندازه",
                                "تبدیل کرنے کی مقدار",
                                "परिवर्तित करने की मात्रा"
                        )
                );

        amount.setInputType(
                InputType.TYPE_CLASS_NUMBER |
                        InputType.TYPE_NUMBER_FLAG_DECIMAL
        );

        TextView result =
                makeText(
                        text(
                                "نتیجه: 0",
                                "Result: 0",
                                "پایله: ۰",
                                "نتیجہ: 0",
                                "परिणाम: 0"
                        ),
                        17
                );

        Button convert =
                makeButton(
                        text(
                                "🔄 تبدیل",
                                "🔄 Convert",
                                "🔄 بدلول",
                                "🔄 تبدیل کریں",
                                "🔄 बदलें"
                        )
                );

        layout.addView(
                makeText(
                        text(
                                "از ارز",
                                "From currency",
                                "له اسعارو",
                                "کس کرنسی سے",
                                "किस मुद्रा से"
                        ),
                        15
                )
        );

        layout.addView(fromSpinner);

        layout.addView(
                makeText(
                        text(
                                "به ارز",
                                "To currency",
                                "ته اسعارو",
                                "کس کرنسی میں",
                                "किस मुद्रा में"
                        ),
                        15
                )
        );

        layout.addView(toSpinner);

        layout.addView(amount);
        layout.addView(convert);
        layout.addView(result);

        convert.setOnClickListener(
                v -> {

                    try {

                        double value =
                                Double.parseDouble(
                                        amount.getText()
                                                .toString()
                                                .trim()
                                );

                        if (value <= 0) {
                            throw new Exception();
                        }

                        String from =
                                fromSpinner
                                        .getSelectedItem()
                                        .toString();

                        String to =
                                toSpinner
                                        .getSelectedItem()
                                        .toString();

                        double fromRate =
                                getRateToAFN(from);

                        double toRate =
                                getRateToAFN(to);

                        if (
                                fromRate <= 0 ||
                                toRate <= 0
                        ) {
                            throw new Exception();
                        }

                        double afn =
                                value * fromRate;

                        double converted =
                                afn / toRate;

                        result.setText(
                                text(
                                        "نتیجه: ",
                                        "Result: ",
                                        "پایله: ",
                                        "نتیجہ: ",
                                        "परिणाम: "
                                ) +
                                        formatNumber(converted) +
                                        " " +
                                        getCurrencyFlag(to) +
                                        " " +
                                        getCurrencyDisplayName(to)
                        );

                    } catch (Exception e) {

                        Toast.makeText(
                                this,
                                text(
                                        "مقدار را درست وارد کنید",
                                        "Enter a valid amount",
                                        "سمه اندازه ولیکئ",
                                        "درست مقدار درج کریں",
                                        "सही मात्रा दर्ज करें"
                                ),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );

        new AlertDialog.Builder(this)
                .setTitle(
                        text(
                                "🔄 تبدیل ارز",
                                "🔄 Currency converter",
                                "🔄 د اسعارو بدلول",
                                "🔄 کرنسی تبدیل کریں",
                                "🔄 मुद्रा परिवर्तक"
                        )
                )
                .setView(layout)
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
    }

    private double getRateToAFN(
            String currency
    ) {

        if (ExchangeData.AFN.equals(currency)) {
            return 1;
        }

        return exchangeData.getRate(currency);
    }

    // =========================================================
    // تنظیمات
    // =========================================================

    private void showSettings() {

        String[] options = {
                text(
                        "🗑 پاک کردن تاریخچه معاملات",
                        "🗑 Clear transaction history",
                        "🗑 د معاملو تاریخچه پاکول",
                        "🗑 معاملات کی تاریخ صاف کریں",
                        "🗑 लेन-देन इतिहास साफ करें"
                ),
                text(
                        "⚠️ پاک کردن تمام اطلاعات صرافی",
                        "⚠️ Clear all exchange data",
                        "⚠️ د صرافۍ ټول معلومات پاکول",
                        "⚠️ تمام ایکسچینج ڈیٹا صاف کریں",
                        "⚠️ सभी एक्सचेंज डेटा साफ करें"
                )
        };

        new AlertDialog.Builder(this)
                .setTitle(
                        text(
                                "⚙️ تنظیمات صرافی",
                                "⚙️ Exchange settings",
                                "⚙️ د صرافۍ تنظیمات",
                                "⚙️ ایکسچینج ترتیبات",
                                "⚙️ एक्सचेंज सेटिंग्स"
                        )
                )
                .setItems(
                        options,
                        (dialog, which) -> {

                            if (which == 0) {
                                confirmClearTransactions();
                            } else {
                                confirmClearAllData();
                            }
                        }
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
    }

    private void confirmClearTransactions() {

        new AlertDialog.Builder(this)
                .setTitle(
                        text(
                                "⚠️ هشدار",
                                "⚠️ Warning",
                                "⚠️ خبرداری",
                                "⚠️ انتباہ",
                                "⚠️ चेतावनी"
                        )
                )
                .setMessage(
                        text(
                                "آیا مطمئن هستید که تاریخچه معاملات حذف شود؟",
                                "Are you sure you want to delete the transaction history?",
                                "ایا ډاډه یاست چې د معاملو تاریخچه پاکه شي؟",
                                "کیا آپ واقعی معاملات کی تاریخ حذف کرنا چاہتے ہیں؟",
                                "क्या आप वाकई लेन-देन इतिहास हटाना चाहते हैं؟"
                        )
                )
                .setNegativeButton(
                        text(
                                "لغو",
                                "Cancel",
                                "لغوه",
                                "منسوخ",
                                "रद्द करें"
                        ),
                        null
                )
                .setPositiveButton(
                        text(
                                "ادامه",
                                "Continue",
                                "دوام",
                                "جاری رکھیں",
                                "जारी रखें"
                        ),
                        (dialog, which) -> {

                            new AlertDialog.Builder(this)
                                    .setTitle(
                                            text(
                                                    "تأیید نهایی",
                                                    "Final confirmation",
                                                    "وروستی تایید",
                                                    "حتمی تصدیق",
                                                    "अंतिम पुष्टि"
                                            )
                                    )
                                    .setMessage(
                                            text(
                                                    "این عملیات تاریخچه معاملات را حذف می‌کند.",
                                                    "This will delete the transaction history.",
                                                    "دا کار به د معاملو تاریخچه پاکه کړي.",
                                                    "یہ کارروائی معاملات کی تاریخ حذف کر دے گی۔",
                                                    "यह कार्रवाई लेन-देन इतिहास हटा देगी।"
                                            )
                                    )
                                    .setNegativeButton(
                                            text(
                                                    "لغو",
                                                    "Cancel",
                                                    "لغوه",
                                                    "منسوخ",
                                                    "रद्द करें"
                                            ),
                                            null
                                    )
                                    .setPositiveButton(
                                            text(
                                                    "حذف",
                                                    "Delete",
                                                    "حذف",
                                                    "حذف کریں",
                                                    "हटाएं"
                                            ),
                                            (d, w) -> {

                                                exchangeData
                                                        .clearTransactions();

                                                Toast.makeText(
                                                        this,
                                                        text(
                                                                "تاریخچه معاملات حذف شد",
                                                                "Transaction history deleted",
                                                                "د معاملو تاریخچه پاکه شوه",
                                                                "معاملات کی تاریخ حذف ہوگئی",
                                                                "लेन-देन इतिहास हटा दिया गया"
                                                        ),
                                                        Toast.LENGTH_LONG
                                                ).show();
                                            }
                                    )
                                    .show();
                        }
                )
                .show();
    }

    private void confirmClearAllData() {

        new AlertDialog.Builder(this)
                .setTitle(
                        text(
                                "⚠️ هشدار بسیار مهم",
                                "⚠️ Very important warning",
                                "⚠️ ډېره مهمه خبرداری",
                                "⚠️ بہت اہم انتباہ",
                                "⚠️ बहुत महत्वपूर्ण चेतावनी"
                        )
                )
                .setMessage(
                        text(
                                "تمام موجودی‌ها، معاملات، مشتریان و نرخ‌ها حذف می‌شوند.",
                                "All balances, transactions, customers and rates will be deleted.",
                                "ټولې موجودۍ، معاملې، پېرودونکي او نرخونه به پاک شي.",
                                "تمام بیلنس، معاملات، صارفین اور شرحیں حذف ہو جائیں گی۔",
                                "सभी शेष, लेन-देन, ग्राहक और दरें हटा दी जाएंगी।"
                        )
                )
                .setNegativeButton(
                        text(
                                "لغو",
                                "Cancel",
                                "لغوه",
                                "منسوخ",
                                "रद्द करें"
                        ),
                        null
                )
                .setPositiveButton(
                        text(
                                "ادامه",
                                "Continue",
                                "دوام",
                                "جاری رکھیں",
                                "जारी रखें"
                        ),
                        (dialog, which) -> {

                            new AlertDialog.Builder(this)
                                    .setTitle(
                                            text(
                                                    "تأیید نهایی",
                                                    "Final confirmation",
                                                    "وروستی تایید",
                                                    "حتمی تصدیق",
                                                    "अंतिम पुष्टि"
                                            )
                                    )
                                    .setMessage(
                                            text(
                                                    "آیا واقعاً می‌خواهید تمام اطلاعات صرافی پاک شود؟",
                                                    "Do you really want to delete all exchange data?",
                                                    "ایا رښتیا غواړئ د صرافۍ ټول معلومات پاک کړئ؟",
                                                    "کیا آپ واقعی تمام ایکسچینج ڈیٹا حذف کرنا چاہتے ہیں؟",
                                                    "क्या आप वाकई सभी एक्सचेंज डेटा हटाना चाहते हैं؟"
                                            )
                                    )
                                    .setNegativeButton(
                                            text(
                                                    "لغو",
                                                    "Cancel",
                                                    "لغوه",
                                                    "منسوخ",
                                                    "रद्द करें"
                                            ),
                                            null
                                    )
                                    .setPositiveButton(
                                            text(
                                                    "حذف همه",
                                                    "Delete all",
                                                    "ټول حذف کړئ",
                                                    "سب حذف کریں",
                                                    "सभी हटाएं"
                                            ),
                                            (d, w) -> {

                                                exchangeData
                                                        .clearAllData();

                                                updateBalance();
                                                updateRate();

                                                Toast.makeText(
                                                        this,
                                                        text(
                                                                "تمام اطلاعات پاک شد",
                                                                "All data deleted",
                                                                "ټول معلومات پاک شول",
                                                                "تمام معلومات حذف ہوگئے",
                                                                "सभी डेटा हटा दिया गया"
                                                        ),
                                                        Toast.LENGTH_LONG
                                                ).show();
                                            }
                                    )
                                    .show();
                        }
                )
                .show();
    }

    // =========================================================
    // فرمت عدد
    // =========================================================

    private String formatNumber(
            double value
    ) {

        if (value == Math.floor(value)) {

            return String.format(
                    Locale.US,
                    "%.0f",
                    value
            );
        }

        return String.format(
                Locale.US,
                "%.4f",
                value
        );
    }

    // =========================================================
    // تبدیل میلادی به هجری شمسی
    // =========================================================

    private String formatDate(
            long timestamp
    ) {

        if (timestamp <= 0) {
            return "";
        }

        Calendar cal =
                Calendar.getInstance();

        cal.setTime(
                new Date(timestamp)
        );

        int gy =
                cal.get(Calendar.YEAR);

        int gm =
                cal.get(Calendar.MONTH) + 1;

        int gd =
                cal.get(Calendar.DAY_OF_MONTH);

        int[] jalali =
                gregorianToJalali(
                        gy,
                        gm,
                        gd
                );

        String hour =
                String.format(
                        Locale.US,
                        "%02d",
                        cal.get(Calendar.HOUR_OF_DAY)
                );

        String minute =
                String.format(
                        Locale.US,
                        "%02d",
                        cal.get(Calendar.MINUTE)
                );

        return String.format(
                Locale.US,
                "%04d/%02d/%02d - %s:%s",
                jalali[0],
                jalali[1],
                jalali[2],
                hour,
                minute
        );
    }

    // =========================================================
    // تبدیل دقیق تاریخ میلادی به جلالی
    // =========================================================

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

        int gy2 = gy - 1600;
        int gm2 = gm - 1;
        int gd2 = gd - 1;

        int gDayNo =
                365 * gy2
                        + (gy2 + 3) / 4
                        - (gy2 + 99) / 100
                        + (gy2 + 399) / 400;

        for (int i = 0; i < gm2; ++i) {
            gDayNo += gDaysInMonth[i];
        }

        if (
                gm2 > 1 &&
                (
                        gy % 4 == 0 &&
                        gy % 100 != 0
                )
                ||
                gy % 400 == 0
        ) {
            gDayNo++;
        }

        gDayNo += gd2;

        int jDayNo =
                gDayNo - 79;

        int jNp =
                jDayNo / 12053;

        jDayNo %= 12053;

        int jy =
                979 + 33 * jNp
                        + 4 * (jDayNo / 1461);

        jDayNo %= 1461;

        if (jDayNo >= 366) {

            jy +=
                    (jDayNo - 1) / 365;

            jDayNo =
                    (jDayNo - 1) % 365;
        }

        int jm;

        for (
                jm = 0;
                jm < 11 &&
                        jDayNo >= jDaysInMonth[jm];
                ++jm
        ) {
            jDayNo -=
                    jDaysInMonth[jm];
        }

        int jd =
                jDayNo + 1;

        return new int[]{
                jy,
                jm + 1,
                jd
        };
    }

    // =========================================================
    // برگشت / تغییر زبان
    // =========================================================

    @Override
    protected void onResume() {

        super.onResume();

        String currentLanguage =
                LanguageManager.getLanguage(this);

        if (
                appliedLanguage != null &&
                !currentLanguage.equals(appliedLanguage)
        ) {

            recreate();
            return;
        }

        if (exchangeData != null) {

            updateBalance();
            updateRate();
        }
    }
    }
