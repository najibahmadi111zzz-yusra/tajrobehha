package com.tajro.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        data = ExchangeData.get(this);

        themeColor = ThemeManager.getThemeColor(this);

        currencies = ExchangeData.getCurrencies();

        buildScreen();

        updateBalance();
    }

    // ==================================================
    // صفحه اصلی
    // ==================================================

    private void buildScreen() {

        LinearLayout root = new LinearLayout(this);

        root.setOrientation(LinearLayout.VERTICAL);

        root.setPadding(
                30,
                30,
                30,
                30
        );

        root.setGravity(
                Gravity.CENTER_HORIZONTAL
        );

        root.setBackgroundColor(
                getLightThemeColor()
        );

        TextView title = new TextView(this);

        title.setText("🔐 گاوصندوق هوشمند");

        title.setTextSize(26);

        title.setTextColor(themeColor);

        title.setGravity(Gravity.CENTER);

        title.setPadding(
                0,
                10,
                0,
                20
        );

        root.addView(title);

        TextView info = new TextView(this);

        info.setText(
                "محل امن برای ثبت دارایی‌های صرافی\n" +
                "موجودی صرافی و امانت مشتریان کاملاً جدا هستند."
        );

        info.setTextSize(16);

        info.setGravity(Gravity.CENTER);

        info.setPadding(
                0,
                0,
                0,
                20
        );

        root.addView(info);

        // ==========================================
        // ارز گاوصندوق
        // ==========================================

        currencySpinner = new Spinner(this);

        createCurrencyAdapter(currencySpinner);

        root.addView(currencySpinner);

        balanceText = new TextView(this);

        balanceText.setTextSize(22);

        balanceText.setTextColor(themeColor);

        balanceText.setGravity(Gravity.CENTER);

        balanceText.setPadding(
                0,
                20,
                0,
                20
        );

        root.addView(balanceText);

        amountInput = new EditText(this);

        amountInput.setHint("مبلغ");

        amountInput.setInputType(
                InputType.TYPE_CLASS_NUMBER |
                InputType.TYPE_NUMBER_FLAG_DECIMAL
        );

        root.addView(amountInput);

        noteInput = new EditText(this);

        noteInput.setHint("یادداشت / دلیل");

        root.addView(noteInput);

        Button depositButton = new Button(this);

        depositButton.setText(
                "➕ گذاشتن در گاوصندوق"
        );

        styleButton(depositButton);

        root.addView(depositButton);

        Button withdrawButton = new Button(this);

        withdrawButton.setText(
                "➖ برداشت از گاوصندوق"
        );

        styleButton(withdrawButton);

        root.addView(withdrawButton);

        Button historyButton = new Button(this);

        historyButton.setText(
                "📋 تاریخچه گاوصندوق"
        );

        styleButton(historyButton);

        root.addView(historyButton);

        // ==========================================
        // امانت مشتریان
        // ==========================================

        TextView custodyTitle = new TextView(this);

        custodyTitle.setText(
                "🔐 امانت مشتریان"
        );

        custodyTitle.setTextSize(22);

        custodyTitle.setTextColor(themeColor);

        custodyTitle.setGravity(Gravity.CENTER);

        custodyTitle.setPadding(
                0,
                30,
                0,
                10
        );

        root.addView(custodyTitle);

        TextView custodyInfo = new TextView(this);

        custodyInfo.setText(
                "امانت مشتری از دارایی صرافی جداست.\n" +
                "ثبت امانت هیچ تغییری در موجودی خود صرافی ایجاد نمی‌کند."
        );

        custodyInfo.setTextSize(15);

        custodyInfo.setGravity(Gravity.CENTER);

        custodyInfo.setPadding(
                0,
                0,
                0,
                15
        );

        root.addView(custodyInfo);

        Button addCustodyButton = new Button(this);

        addCustodyButton.setText(
                "🔐 ثبت امانت جدید"
        );

        styleButton(addCustodyButton);

        root.addView(addCustodyButton);

        Button custodyHistoryButton = new Button(this);

        custodyHistoryButton.setText(
                "📋 امانت‌های مشتریان"
        );

        styleButton(custodyHistoryButton);

        root.addView(custodyHistoryButton);

        // جستجوی مشتری
        Button searchCustodyButton = new Button(this);

        searchCustodyButton.setText(
                "🔍 جستجوی مشتری و امانت"
        );

        styleButton(searchCustodyButton);

        root.addView(searchCustodyButton);

        // رسیدها
        Button receiptsButton = new Button(this);

        receiptsButton.setText(
                "🧾 رسیدهای امانت"
        );

        styleButton(receiptsButton);

        root.addView(receiptsButton);

        // ==========================================
        // بازگشت
        // ==========================================

        Button backButton = new Button(this);

        backButton.setText(
                "⬅️ بازگشت"
        );

        styleButton(backButton);

        root.addView(backButton);

        // ==========================================
        // رویدادها
        // ==========================================

        currencySpinner.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            android.widget.AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {
                        updateBalance();
                    }

                    @Override
                    public void onNothingSelected(
                            android.widget.AdapterView<?> parent
                    ) {
                    }
                }
        );

        depositButton.setOnClickListener(
                v -> deposit()
        );

        withdrawButton.setOnClickListener(
                v -> withdraw()
        );

        historyButton.setOnClickListener(
                v -> showHistory()
        );

        addCustodyButton.setOnClickListener(
                v -> showAddCustodyDialog()
        );

        custodyHistoryButton.setOnClickListener(
                v -> showCustodyHistory()
        );

        searchCustodyButton.setOnClickListener(
                v -> showCustomerSearch()
        );

        receiptsButton.setOnClickListener(
                v -> showReceiptHistory()
        );

        backButton.setOnClickListener(
                v -> finish()
        );

        setContentView(root);
    }

    // ==================================================
    // ارز + پرچم
    // ==================================================

    private void createCurrencyAdapter(Spinner spinner) {

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
                                        + currencies[position]
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
                                        + currencies[position]
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

    private String getCurrencyFlag(String currency) {

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
                    "لطفاً مبلغ درست وارد کنید.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String currency = getSelectedCurrency();

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
                "مبلغ با موفقیت وارد گاوصندوق شد.",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void withdraw() {

        double amount = getAmount();

        if (amount <= 0) {

            Toast.makeText(
                    this,
                    "لطفاً مبلغ درست وارد کنید.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String currency = getSelectedCurrency();

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
                    "موجودی کافی نیست.\n" +
                    "موجودی فعلی: "
                            + formatNumber(current),
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
                "مبلغ از گاوصندوق برداشت شد.",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void updateBalance() {

        if (currencySpinner == null
                || currencySpinner.getSelectedItem() == null
                || balanceText == null) {
            return;
        }

        String currency = getSelectedCurrency();

        double balance =
                data.getBalance(currency);

        balanceText.setText(
                getCurrencyFlag(currency)
                        + " موجودی "
                        + currency
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
                "نام مشتری"
        );

        custodyNameInput.setSingleLine(true);

        layout.addView(
                custodyNameInput
        );

        custodyPhoneInput =
                new EditText(this);

        custodyPhoneInput.setHint(
                "شماره تلفن"
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
                "مبلغ امانت"
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
                "یادداشت / توضیح امانت"
        );

        layout.addView(
                custodyNoteInput
        );

        final AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                "🔐 ثبت امانت مشتری"
                        )
                        .setMessage(
                                "این مبلغ متعلق به مشتری است و " +
                                "به موجودی خود صرافی اضافه نمی‌شود."
                        )
                        .setView(layout)
                        .setNegativeButton(
                                "انصراف",
                                null
                        )
                        .setPositiveButton(
                                "ثبت امانت",
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
                                            "لطفاً ارز امانت را انتخاب کنید.",
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
                                            "نام مشتری را وارد کنید"
                                    );

                                    custodyNameInput.requestFocus();

                                    return;
                                }

                                if (amountText.isEmpty()) {

                                    custodyAmountInput.setError(
                                            "مبلغ امانت را وارد کنید"
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
                                            "مبلغ درست وارد کنید"
                                    );

                                    custodyAmountInput.requestFocus();

                                    return;
                                }

                                if (amount <= 0) {

                                    custodyAmountInput.setError(
                                            "مبلغ باید بیشتر از صفر باشد"
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
                                            "ثبت امانت انجام نشد.",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    return;
                                }

                                dialog.dismiss();

                                Toast.makeText(
                                        this,
                                        "🔐 امانت با موفقیت ثبت شد.",
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
                        "هنوز هیچ امانتی ثبت نشده است."
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
                                    record.optLong("id", 0)
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
                            .append(currency)
                            .append("\n");

                    text.append(
                            "👤 مشتری: "
                    )
                            .append(name)
                            .append("\n");

                    if (!phone.isEmpty()) {

                        text.append(
                                "📞 شماره: "
                        )
                                .append(phone)
                                .append("\n");
                    }

                    text.append(
                            "💰 مبلغ کل: "
                    )
                            .append(
                                    formatNumber(amount)
                            )
                            .append("\n");

                    text.append(
                            "💵 تحویل/دریافت ثبت‌شده: "
                    )
                            .append(
                                    formatNumber(received)
                            )
                            .append("\n");

                    text.append(
                            "💰 باقی‌مانده: "
                    )
                            .append(
                                    formatNumber(remaining)
                            )
                            .append("\n");

                    if (remaining <= 0) {

                        text.append(
                                "🟢 وضعیت: تسویه شده\n"
                        );

                    } else {

                        text.append(
                                "🟠 وضعیت: باقی دارد\n"
                        );
                    }

                    text.append(
                            "📌 وضعیت امانت: "
                    )
                            .append(status)
                            .append("\n");

                    if (date > 0) {

                        text.append(
                                "📅 تاریخ: "
                        )
                                .append(
                                        formatDate(date)
                                )
                                .append("\n");
                    }

                    if (!note.isEmpty()) {

                        text.append(
                                "📝 یادداشت: "
                        )
                                .append(note)
                                .append("\n");
                    }
                }
            }

            new AlertDialog.Builder(this)
                    .setTitle(
                            "🔐 امانت‌های مشتریان"
                    )
                    .setMessage(
                            text.toString()
                    )
                    .setPositiveButton(
                            "تحویل / ثبت رسید",
                            (dialog, which) ->
                                    showReturnCustody()
                    )
                    .setNeutralButton(
                            "🔍 جستجو",
                            (dialog, which) ->
                                    showCustomerSearch()
                    )
                    .setNegativeButton(
                            "بستن",
                            null
                    )
                    .show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "خطا در خواندن امانت‌ها.",
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
                "نام یا شماره مشتری"
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
                                "🔍 جستجوی مشتری"
                        )
                        .setView(layout)
                        .setPositiveButton(
                                "جستجو",
                                null
                        )
                        .setNegativeButton(
                                "انصراف",
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
                                            "نام یا شماره را وارد کنید"
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

    private void showSearchResults(String query) {

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
                        .append(currency)
                        .append("\n");

                result.append(
                        "💰 کل: "
                )
                        .append(
                                formatNumber(amount)
                        )
                        .append("\n");

                result.append(
                        "💵 ثبت‌شده: "
                )
                        .append(
                                formatNumber(received)
                        )
                        .append("\n");

                result.append(
                        "💰 باقی: "
                )
                        .append(
                                formatNumber(remaining)
                        )
                        .append("\n");

                if (remaining <= 0) {

                    result.append(
                            "🟢 تسویه شده\n"
                    );

                } else {

                    result.append(
                            "🟠 باقی دارد\n"
                    );
                }

                result.append(
                        "📌 "
                )
                        .append(status)
                        .append("\n");

                if (remaining > 0
                        && "امانت نزد صرافی".equals(status)) {

                    result.append(
                            "🧾 برای ثبت پرداخت/تحویل، " +
                            "روی «ثبت رسید» بزنید.\n"
                    );
                }
            }

            if (count == 0) {

                result.append(
                        "❌ مشتری پیدا نشد."
                );
            }

            AlertDialog resultDialog =
                    new AlertDialog.Builder(this)
                            .setTitle(
                                    "🔍 نتیجه جستجو"
                            )
                            .setMessage(
                                    result.toString()
                            )
                            .setPositiveButton(
                                    "ثبت رسید",
                                    (dialog, which) ->
                                            showReturnCustody()
                            )
                            .setNegativeButton(
                                    "بستن",
                                    null
                            )
                            .create();

            resultDialog.show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "خطا در جستجوی مشتری.",
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
                                + " — کل "
                                + formatNumber(amount)
                                + " "
                                + currency
                                + " — باقی "
                                + formatNumber(remaining)
                );
            }

            if (items.isEmpty()) {

                new AlertDialog.Builder(this)
                        .setTitle(
                                "تحویل امانت"
                        )
                        .setMessage(
                                "امانت فعالی برای ثبت رسید وجود ندارد."
                        )
                        .setPositiveButton(
                                "باشه",
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
                            "🧾 انتخاب مشتری برای رسید"
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
                            "انصراف",
                            null
                    )
                    .show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "خطا در خواندن امانت‌ها.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // ==================================================
    // ثبت رسید
    // ==================================================

    private void showReceiptDialog(long custodyId) {

        JSONObject custody =
                findCustodyById(custodyId);

        if (custody == null) {

            Toast.makeText(
                    this,
                    "رکورد امانت پیدا نشد.",
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
                "👤 مشتری: " + name +
                "\n" +
                (phone.isEmpty()
                        ? ""
                        : "📞 شماره: " + phone + "\n") +
                "💰 مبلغ کل: "
                        + formatNumber(total)
                        + " "
                        + currency +
                "\n" +
                "💵 قبلاً ثبت شده: "
                        + formatNumber(received)
                        + " "
                        + currency +
                "\n" +
                "🟠 باقی‌مانده: "
                        + formatNumber(remaining)
                        + " "
                        + currency
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
                "مبلغ این رسید"
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
                "توضیح رسید"
        );

        layout.addView(note);

        final AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                "🧾 ثبت رسید"
                        )
                        .setMessage(
                                "رسید پس از ثبت قابل ویرایش یا حذف نیست."
                        )
                        .setView(layout)
                        .setNegativeButton(
                                "انصراف",
                                null
                        )
                        .setPositiveButton(
                                "ثبت رسید",
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
                                            "مبلغ را وارد کنید"
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
                                            "مبلغ درست وارد کنید"
                                    );

                                    return;
                                }

                                if (receiptAmount <= 0) {

                                    amount.setError(
                                            "مبلغ باید بیشتر از صفر باشد"
                                    );

                                    return;
                                }

                                if (receiptAmount > remaining) {

                                    amount.setError(
                                            "مبلغ بیشتر از باقی‌مانده است"
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
                                            "ثبت رسید انجام نشد.",
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
                    "🟢 تسویه کامل شد";

        } else {

            status =
                    "🟠 هنوز باقی دارد";
        }

        String message =
                "🧾 رسید با موفقیت ثبت شد.\n\n" +
                "🔢 کد رسید: "
                        + code
                        + "\n" +
                "👤 مشتری: "
                        + customer
                        + "\n" +
                "💰 مبلغ رسید: "
                        + formatNumber(amount)
                        + " "
                        + currency
                        + "\n" +
                "💵 باقی‌مانده: "
                        + formatNumber(remaining)
                        + " "
                        + currency
                        + "\n" +
                status
                        + "\n\n" +
                "📅 تاریخ: "
                        + formatDate(
                                System.currentTimeMillis()
                        );

        new AlertDialog.Builder(this)
                .setTitle(
                        "✅ رسید ثبت شد"
                )
                .setMessage(message)
                .setPositiveButton(
                        "باشه",
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
                        "هنوز هیچ رسیدی ثبت نشده است."
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
                            "👤 مشتری: "
                    )
                            .append(name)
                            .append("\n");

                    if (!phone.isEmpty()) {

                        text.append(
                                "📞 شماره: "
                        )
                                .append(phone)
                                .append("\n");
                    }

                    text.append(
                            "💰 مبلغ: "
                    )
                            .append(
                                    formatNumber(amount)
                            )
                            .append(" ")
                            .append(currency)
                            .append("\n");

                    if (date > 0) {

                        text.append(
                                "📅 تاریخ: "
                        )
                                .append(
                                        formatDate(date)
                                )
                                .append("\n");
                    }

                    if (!note.isEmpty()) {

                        text.append(
                                "📝 توضیح: "
                        )
                                .append(note)
                                .append("\n");
                    }

                    text.append(
                            "🔒 غیرقابل ویرایش/حذف"
                    )
                            .append("\n");
                }
            }

            new AlertDialog.Builder(this)
                    .setTitle(
                            "🧾 رسیدهای امانت"
                    )
                    .setMessage(
                            text.toString()
                    )
                    .setPositiveButton(
                            "بستن",
                            null
                    )
                    .show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "خطا در خواندن رسیدها.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // ==================================================
    // پیدا کردن امانت
    // ==================================================

    private JSONObject findCustodyById(long id) {

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
                        "هنوز هیچ عملیاتی ثبت نشده است."
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
                            .append(type)
                            .append("\n");

                    text.append(
                            "ارز: "
                    )
                            .append(currency)
                            .append("\n");

                    text.append(
                            "مبلغ: "
                    )
                            .append(
                                    formatNumber(amount)
                            )
                            .append("\n");

                    if (date > 0) {

                        text.append(
                                "تاریخ: "
                        )
                                .append(
                                        formatDate(date)
                                )
                                .append("\n");
                    }

                    if (!note.isEmpty()) {

                        text.append(
                                "یادداشت: "
                        )
                                .append(note)
                                .append("\n");
                    }
                }
            }

            new AlertDialog.Builder(this)
                    .setTitle(
                            "📋 تاریخچه گاوصندوق"
                    )
                    .setMessage(
                            text.toString()
                    )
                    .setPositiveButton(
                            "بستن",
                            null
                    )
                    .show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "خطا در خواندن تاریخچه.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // ==================================================
    // ابزارها
    // ==================================================

    private String formatNumber(double number) {

        return String.format(
                Locale.US,
                "%,.2f",
                number
        );
    }

    // ==================================================
    // تاریخ هجری شمسی افغانستان
    // ==================================================

    private String formatDate(long time) {

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

    private String getAfghanMonthName(int month) {

        String[] months = {
                "حمل",
                "ثور",
                "جوزا",
                "سرطان",
                "اسد",
                "سنبله",
                "میزان",
                "عقرب",
                "قوس",
                "جدی",
                "دلو",
                "حوت"
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

    private void styleButton(Button button) {

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
