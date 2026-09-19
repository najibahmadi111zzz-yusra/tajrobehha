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

        // فقط یک Dialog ساخته می‌شود.
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
                            "💰 مبلغ: "
                    )
                            .append(
                                    formatNumber(amount)
                            )
                            .append("\n");

                    text.append(
                            "📌 وضعیت: "
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
                            "تحویل امانت",
                            (dialog, which) ->
                                    showReturnCustody()
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
    // تحویل امانت
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

                ids.add(id);

                items.add(
                        getCurrencyFlag(currency)
                                + " "
                                + name
                                + " — "
                                + formatNumber(amount)
                                + " "
                                + currency
                );
            }

            if (items.isEmpty()) {

                new AlertDialog.Builder(this)
                        .setTitle(
                                "تحویل امانت"
                        )
                        .setMessage(
                                "امانت فعالی برای تحویل وجود ندارد."
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
                            "📦 انتخاب امانت برای تحویل"
                    )
                    .setItems(
                            itemArray,
                            (dialog, which) -> {

                                long id =
                                        ids.get(which);

                                confirmReturnCustody(id);
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

    private void confirmReturnCustody(long id) {

        new AlertDialog.Builder(this)
                .setTitle(
                        "⚠️ تأیید تحویل امانت"
                )
                .setMessage(
                        "آیا مطمئن هستید که این امانت " +
                        "به صاحب آن تحویل داده شده است؟\n\n" +
                        "رکورد امانت حذف نمی‌شود و فقط " +
                        "وضعیت آن به «تحویل داده شد» تغییر می‌کند."
                )
                .setNegativeButton(
                        "خیر",
                        null
                )
                .setPositiveButton(
                        "بله، تحویل شد",
                        (dialog, which) -> {

                            boolean success =
                                    data.returnCustody(id);

                            if (success) {

                                Toast.makeText(
                                        this,
                                        "✅ امانت تحویل داده شد.",
                                        Toast.LENGTH_LONG
                                ).show();

                            } else {

                                Toast.makeText(
                                        this,
                                        "ثبت تحویل امانت ناموفق بود.",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                )
                .show();
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

    private String formatDate(long time) {

        try {

            SimpleDateFormat format =
                    new SimpleDateFormat(
                            "yyyy/MM/dd HH:mm",
                            Locale.getDefault()
                    );

            return format.format(
                    new Date(time)
            );

        } catch (Exception e) {

            return "";
        }
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
