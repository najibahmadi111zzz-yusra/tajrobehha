package com.tajro.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
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

import java.text.SimpleDateFormat;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        exchangeData = ExchangeData.get(this);
        currencies = ExchangeData.getCurrencies();

        createScreen();
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

    private TextView makeText(String text, float size) {

        TextView tv = new TextView(this);

        tv.setText(text);
        tv.setTextSize(size);

        tv.setPadding(
                dp(8),
                dp(10),
                dp(8),
                dp(10)
        );

        return tv;
    }

    private EditText makeInput(String hint) {

        EditText input = new EditText(this);

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

    private Button makeButton(String text) {

        Button button = new Button(this);

        button.setText(text);
        button.setTextSize(15);

        return button;
    }

    // =========================================================
    // پرچم ارز
    // =========================================================

    private String getCurrencyFlag(String currency) {

        if (ExchangeData.AFN.equals(currency)) return "🇦🇫";
        if (ExchangeData.USD.equals(currency)) return "🇺🇸";
        if (ExchangeData.EUR.equals(currency)) return "🇪🇺";
        if (ExchangeData.GBP.equals(currency)) return "🇬🇧";
        if (ExchangeData.SAR.equals(currency)) return "🇸🇦";
        if (ExchangeData.AED.equals(currency)) return "🇦🇪";
        if (ExchangeData.IQD.equals(currency)) return "🇮🇶";
        if (ExchangeData.INR.equals(currency)) return "🇮🇳";
        if (ExchangeData.PKR.equals(currency)) return "🇵🇰";
        if (ExchangeData.TRY.equals(currency)) return "🇹🇷";
        if (ExchangeData.TOMAN.equals(currency)) return "🇮🇷";

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

                String currency = items[position];

                tv.setText(
                        getCurrencyFlag(currency)
                                + "  "
                                + currency
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

                String currency = items[position];

                tv.setText(
                        getCurrencyFlag(currency)
                                + "  "
                                + currency
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
                        "💱 صرافی تجربه‌ها",
                        25
                );

        title.setGravity(Gravity.CENTER);

        main.addView(title);

        TextView subtitle =
                makeText(
                        "ثبت و مدیریت خرید و فروش ارز",
                        17
                );

        subtitle.setGravity(Gravity.CENTER);

        main.addView(subtitle);

        customerNameInput =
                makeInput("نام مشتری");

        main.addView(customerNameInput);

        phoneInput =
                makeInput("شماره تلفن");

        phoneInput.setInputType(
                InputType.TYPE_CLASS_PHONE
        );

        main.addView(phoneInput);

        main.addView(
                makeText("انتخاب ارز", 16)
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
                makeText("نوع معامله", 16)
        );

        typeSpinner =
                new Spinner(this);

        String[] types = {
                "خرید",
                "فروش"
        };

        ArrayAdapter<String> typeAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        types
                );

        typeAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        typeSpinner.setAdapter(typeAdapter);

        main.addView(typeSpinner);

        amountInput =
                makeInput("مقدار ارز");

        amountInput.setInputType(
                InputType.TYPE_CLASS_NUMBER |
                        InputType.TYPE_NUMBER_FLAG_DECIMAL
        );

        main.addView(amountInput);

        rateInput =
                makeInput("نرخ هر واحد به افغانی");

        rateInput.setInputType(
                InputType.TYPE_CLASS_NUMBER |
                        InputType.TYPE_NUMBER_FLAG_DECIMAL
        );

        main.addView(rateInput);

        noteInput =
                makeInput("یادداشت / توضیحات");

        main.addView(noteInput);

        totalText =
                makeText(
                        "مجموع: 0 افغانی",
                        18
                );

        totalText.setGravity(Gravity.CENTER);

        main.addView(totalText);

        balanceText =
                makeText(
                        "موجودی: 0",
                        17
                );

        balanceText.setGravity(Gravity.CENTER);

        main.addView(balanceText);

        Button calculateButton =
                makeButton("🧮 محاسبه");

        main.addView(calculateButton);

        calculateButton.setOnClickListener(
                v -> calculateTotal()
        );

        Button saveButton =
                makeButton("💾 ثبت معامله");

        main.addView(saveButton);

        saveButton.setOnClickListener(
                v -> saveTransaction()
        );

        Button historyButton =
                makeButton("📜 تاریخچه معاملات");

        main.addView(historyButton);

        historyButton.setOnClickListener(
                v -> showHistory()
        );

        Button customersButton =
                makeButton("👤 مشتریان");

        main.addView(customersButton);

        customersButton.setOnClickListener(
                v -> showCustomers()
        );

        Button reportButton =
                makeButton("📊 گزارش صرافی");

        main.addView(reportButton);

        reportButton.setOnClickListener(
                v -> showReport()
        );

        // =====================================================
        // مدیریت موجودی
        // =====================================================

        Button manageBalanceButton =
                makeButton("💰 مدیریت موجودی");

        main.addView(manageBalanceButton);

        manageBalanceButton.setOnClickListener(
                v -> showBalanceManager()
        );

        Button balancesButton =
                makeButton("💰 موجودی همه ارزها");

        main.addView(balancesButton);

        balancesButton.setOnClickListener(
                v -> showBalances()
        );

        Button converterButton =
                makeButton("🔄 تبدیل ارز");

        main.addView(converterButton);

        converterButton.setOnClickListener(
                v -> showConverter()
        );

        Button vaultButton =
                makeButton("🔐 گاوصندوق هوشمند");

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
                                "گاوصندوق در دسترس نیست",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );

        Button settingsButton =
                makeButton("⚙️ تنظیمات");

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
                        "موجودی فعلی: 0",
                        17
                );

        currentText.setGravity(
                Gravity.CENTER
        );

        EditText amount =
                makeInput(
                        "مقدار موجودی جدید"
                );

        amount.setInputType(
                InputType.TYPE_CLASS_NUMBER |
                        InputType.TYPE_NUMBER_FLAG_DECIMAL
        );

        Button save =
                makeButton(
                        "💾 ذخیره موجودی"
                );

        layout.addView(
                makeText(
                        "ارز را انتخاب کنید",
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
                                "موجودی فعلی: " +
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
                                "مقدار موجودی را وارد کنید",
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
                                "موجودی فعلی: " +
                                        getCurrencyFlag(currency) +
                                        " " +
                                        formatNumber(newBalance)
                        );

                        amount.setText("");

                        Toast.makeText(
                                this,
                                "✅ موجودی " +
                                        currency +
                                        " ذخیره شد",
                                Toast.LENGTH_SHORT
                        ).show();

                    } catch (Exception e) {

                        Toast.makeText(
                                this,
                                "مقدار موجودی نادرست است",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );

        new AlertDialog.Builder(this)
                .setTitle("💰 مدیریت موجودی")
                .setView(layout)
                .setNegativeButton(
                        "بستن",
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
                "موجودی " +
                        getCurrencyFlag(currency) +
                        " " +
                        currency +
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
                    "مجموع: " +
                            formatNumber(total) +
                            " افغانی"
            );

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "مقدار و نرخ را درست وارد کنید",
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

        Object typeObject =
                typeSpinner.getSelectedItem();

        if (
                currencyObject == null ||
                typeObject == null
        ) {
            Toast.makeText(
                    this,
                    "ارز و نوع معامله را انتخاب کنید",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String currency =
                currencyObject.toString();

        String type =
                typeObject.toString();

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
                    "مقدار ارز را وارد کنید",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (rateText.isEmpty()) {

            Toast.makeText(
                    this,
                    "نرخ ارز را وارد کنید",
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
                    "مقدار یا نرخ نادرست است",
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
                    "مقدار و نرخ باید بیشتر از صفر باشد",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (ExchangeData.AFN.equals(currency)) {

            Toast.makeText(
                    this,
                    "افغانی ارز پایه است؛ ارز خارجی را انتخاب کنید.",
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
                        "موجودی افغانی صرافی کافی نیست.",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

        } else {

            if (currencyBalance < amount) {

                Toast.makeText(
                        this,
                        "موجودی " +
                                currency +
                                " کافی نیست.",
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
                    "خطا در ثبت معامله؛ موجودی تغییر نکرد.",
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
                "مجموع: " +
                        formatNumber(total) +
                        " افغانی"
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
                "✅ معامله با موفقیت ثبت شد",
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
                "       💱 تجربه‌ها\n"
        );

        receipt.append(
                "       🧾 رسید معامله\n"
        );

        receipt.append(
                "━━━━━━━━━━━━━━━━━━━━\n\n"
        );

        receipt.append(
                "🆔 شناسه معامله: "
        );

        receipt.append(transactionId);
        receipt.append("\n");

        receipt.append(
                "📅 تاریخ هجری: "
        );

        receipt.append(date);
        receipt.append("\n\n");

        receipt.append(
                "👤 مشتری: "
        );

        receipt.append(
                customerName.isEmpty()
                        ? "بدون نام"
                        : customerName
        );

        receipt.append("\n");

        if (!phone.isEmpty()) {

            receipt.append(
                    "📱 تلفن: "
            );

            receipt.append(phone);
            receipt.append("\n");
        }

        receipt.append("\n");

        receipt.append(
                "🔄 نوع معامله: "
        );

        receipt.append(type);
        receipt.append("\n");

        receipt.append(
                "💱 ارز: "
        );

        receipt.append(
                getCurrencyFlag(currency)
        );

        receipt.append(" ");
        receipt.append(currency);
        receipt.append("\n");

        receipt.append(
                "📦 مقدار: "
        );

        receipt.append(
                formatNumber(amount)
        );

        receipt.append("\n");

        receipt.append(
                "📈 نرخ هر واحد: "
        );

        receipt.append(
                formatNumber(rate)
        );

        receipt.append(" افغانی\n");

        receipt.append(
                "💰 مبلغ نهایی: "
        );

        receipt.append(
                formatNumber(total)
        );

        receipt.append(
                " افغانی\n"
        );

        if (!note.isEmpty()) {

            receipt.append("\n");
            receipt.append("📝 یادداشت: ");
            receipt.append(note);
            receipt.append("\n");
        }

        receipt.append("\n");
        receipt.append("💵 وضعیت حساب: نقدی\n");

        receipt.append(
                "━━━━━━━━━━━━━━━━━━━━\n"
        );

        receipt.append(
                "        تشکر از شما 🌹\n"
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
                        .setTitle("🧾 رسید معامله")
                        .setView(scrollView)
                        .setNegativeButton(
                                "بستن",
                                null
                        )
                        .setNeutralButton(
                                "📋 کپی",
                                null
                        )
                        .setPositiveButton(
                                "📤 اشتراک‌گذاری",
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

    private void copyReceipt(String receipt) {

        ClipboardManager clipboard =
                (ClipboardManager)
                        getSystemService(
                                CLIPBOARD_SERVICE
                        );

        if (clipboard != null) {

            clipboard.setPrimaryClip(
                    ClipData.newPlainText(
                            "رسید معامله تجربه‌ها",
                            receipt
                    )
            );

            Toast.makeText(
                    this,
                    "📋 رسید کپی شد",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void shareReceipt(String receipt) {

        Intent shareIntent =
                new Intent(Intent.ACTION_SEND);

        shareIntent.setType("text/plain");

        shareIntent.putExtra(
                Intent.EXTRA_TITLE,
                "🧾 رسید معامله تجربه‌ها"
        );

        shareIntent.putExtra(
                Intent.EXTRA_SUBJECT,
                "رسید معامله تجربه‌ها"
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
                            "📤 ارسال رسید با..."
                    )
            );

        } else {

            Toast.makeText(
                    this,
                    "برنامه‌ای برای اشتراک‌گذاری پیدا نشد",
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
                    .setTitle("📜 تاریخچه معاملات")
                    .setMessage(
                            "هنوز معامله‌ای ثبت نشده است."
                    )
                    .setPositiveButton(
                            "باشه",
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

            TextView item =
                    makeText(
                            "👤 مشتری: " +
                                    customer +

                                    "\n📱 تلفن: " +
                                    phone +

                                    "\n💱 نوع: " +
                                    type +

                                    "\n💵 ارز: " +
                                    getCurrencyFlag(currency) +
                                    " " +
                                    currency +

                                    "\n📦 مقدار: " +
                                    formatNumber(amount) +

                                    "\n📈 نرخ: " +
                                    formatNumber(rate) +

                                    "\n💰 مجموع: " +
                                    formatNumber(total) +
                                    " افغانی" +

                                    "\n📋 حساب: " +
                                    accountStatus +

                                    "\n📝 یادداشت: " +
                                    note +

                                    "\n📅 تاریخ هجری: " +
                                    formatDate(date),

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
                .setTitle("📜 تاریخچه معاملات")
                .setView(scroll)
                .setPositiveButton(
                        "بستن",
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
                    .setTitle("👤 مشتریان")
                    .setMessage(
                            "هنوز مشتری ثبت نشده است."
                    )
                    .setPositiveButton(
                            "باشه",
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
                "🔎 جستجوی نام مشتری"
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
                .setTitle("👤 مشتریان")
                .setView(layout)
                .setPositiveButton(
                        "بستن",
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

        StringBuilder text =
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

            text.append("━━━━━━━━━━━━━━━━\n");

            text.append("👤 ")
                    .append(name)
                    .append("\n");

            text.append("🔴 بدهکاری: ")
                    .append(formatNumber(debt))
                    .append(" افغانی\n");

            text.append("🟢 طلبکاری: ")
                    .append(formatNumber(credit))
                    .append(" افغانی\n");

            text.append("💰 مانده حساب: ")
                    .append(formatNumber(balance))
                    .append(" افغانی\n");

            count++;
        }

        if (count == 0) {

            return "🔎 مشتری با این نام پیدا نشد.";
        }

        return "👥 تعداد نتیجه: " +
                count +
                "\n\n" +
                text.toString();
    }

    // =========================================================
    // یکسان‌سازی حروف فارسی برای جستجو
    // =========================================================

    private String normalizePersian(String text) {

        if (text == null) {
            return "";
        }

        return text
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
                "📊 گزارش صرافی" +

                        "\n\nتعداد معاملات: " +
                        count +

                        "\n\nمجموع خرید: " +
                        formatNumber(buy) +
                        " افغانی" +

                        "\n\nمجموع فروش: " +
                        formatNumber(sell) +
                        " افغانی" +

                        "\n\nاختلاف فروش و خرید: " +
                        formatNumber(difference) +
                        " افغانی";

        new AlertDialog.Builder(this)
                .setTitle("📊 گزارش")
                .setMessage(report)
                .setPositiveButton(
                        "بستن",
                        null
                )
                .show();
    }

    // =========================================================
    // موجودی همه ارزها
    // =========================================================

    private void showBalances() {

        StringBuilder text =
                new StringBuilder();

        for (String currency : currencies) {

            double balance =
                    exchangeData.getBalance(currency);

            double rate =
                    exchangeData.getRate(currency);

            text.append(
                    getCurrencyFlag(currency)
            );

            text.append(" ");
            text.append(currency);

            text.append(
                    "\nموجودی: "
            );

            text.append(
                    formatNumber(balance)
            );

            text.append(
                    "\nنرخ: "
            );

            text.append(
                    formatNumber(rate)
            );

            text.append("\n\n");
        }

        new AlertDialog.Builder(this)
                .setTitle("💰 موجودی همه ارزها")
                .setMessage(text.toString())
                .setPositiveButton(
                        "بستن",
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
                makeInput("مقدار برای تبدیل");

        amount.setInputType(
                InputType.TYPE_CLASS_NUMBER |
                        InputType.TYPE_NUMBER_FLAG_DECIMAL
        );

        TextView result =
                makeText(
                        "نتیجه: 0",
                        17
                );

        Button convert =
                makeButton("🔄 تبدیل");

        layout.addView(
                makeText("از ارز", 15)
        );

        layout.addView(fromSpinner);

        layout.addView(
                makeText("به ارز", 15)
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
                                "نتیجه: " +
                                        formatNumber(converted) +
                                        " " +
                                        getCurrencyFlag(to) +
                                        " " +
                                        to
                        );

                    } catch (Exception e) {

                        Toast.makeText(
                                this,
                                "مقدار را درست وارد کنید",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );

        new AlertDialog.Builder(this)
                .setTitle("🔄 تبدیل ارز")
                .setView(layout)
                .setPositiveButton(
                        "بستن",
                        null
                )
                .show();
    }

    private double getRateToAFN(String currency) {

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
                "🗑 پاک کردن تاریخچه معاملات",
                "⚠️ پاک کردن تمام اطلاعات صرافی"
        };

        new AlertDialog.Builder(this)
                .setTitle("⚙️ تنظیمات صرافی")
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
                        "بستن",
                        null
                )
                .show();
    }

    private void confirmClearTransactions() {

        new AlertDialog.Builder(this)
                .setTitle("⚠️ هشدار")
                .setMessage(
                        "آیا مطمئن هستید که تاریخچه معاملات حذف شود؟"
                )
                .setNegativeButton(
                        "لغو",
                        null
                )
                .setPositiveButton(
                        "ادامه",
                        (dialog, which) -> {

                            new AlertDialog.Builder(this)
                                    .setTitle("تأیید نهایی")
                                    .setMessage(
                                            "این عملیات تاریخچه معاملات را حذف می‌کند."
                                    )
                                    .setNegativeButton(
                                            "لغو",
                                            null
                                    )
                                    .setPositiveButton(
                                            "حذف",
                                            (d, w) -> {

                                                exchangeData
                                                        .clearTransactions();

                                                Toast.makeText(
                                                        this,
                                                        "تاریخچه معاملات حذف شد",
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
                .setTitle("⚠️ هشدار بسیار مهم")
                .setMessage(
                        "تمام موجودی‌ها، معاملات، مشتریان و نرخ‌ها حذف می‌شوند."
                )
                .setNegativeButton(
                        "لغو",
                        null
                )
                .setPositiveButton(
                        "ادامه",
                        (dialog, which) -> {

                            new AlertDialog.Builder(this)
                                    .setTitle("تأیید نهایی")
                                    .setMessage(
                                            "آیا واقعاً می‌خواهید تمام اطلاعات صرافی پاک شود؟"
                                    )
                                    .setNegativeButton(
                                            "لغو",
                                            null
                                    )
                                    .setPositiveButton(
                                            "حذف همه",
                                            (d, w) -> {

                                                exchangeData
                                                        .clearAllData();

                                                updateBalance();
                                                updateRate();

                                                Toast.makeText(
                                                        this,
                                                        "تمام اطلاعات پاک شد",
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

    private String formatNumber(double value) {

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

    private String formatDate(long timestamp) {

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

    // تبدیل دقیق تاریخ میلادی به جلالی
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
    // برگشت
    // =========================================================

    @Override
    protected void onResume() {

        super.onResume();

        if (exchangeData != null) {

            updateBalance();
            updateRate();
        }
    }
                   }
