package com.tajro.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExchangeActivity extends Activity {

    private SharedPreferences oldPrefs;

    private Spinner currencySpinner;
    private Spinner typeSpinner;

    private EditText customerInput;
    private EditText phoneInput;
    private EditText amountInput;
    private EditText rateInput;
    private EditText noteInput;

    private TextView totalText;
    private TextView balanceText;
    private TextView customerBalanceText;
    private TextView historyText;
    private TextView reportText;
    private TextView ratesText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        oldPrefs = getSharedPreferences(
                "exchange_data",
                MODE_PRIVATE
        );

        migrateOldDataIfNeeded();
        createInterface();
    }

    private int dp(int value) {
        return (int) (
                value * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }

    private TextView title(String text, int size) {

        TextView view = new TextView(this);

        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(Color.rgb(8, 65, 90));
        view.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        view.setPadding(
                0,
                dp(15),
                0,
                dp(10)
        );

        return view;
    }

    private Button makeButton(String text) {

        Button button = new Button(this);

        button.setText(text);
        button.setTextSize(16);
        button.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        return button;
    }

    private EditText input(String hint) {

        EditText edit = new EditText(this);

        edit.setHint(hint);
        edit.setTextSize(16);

        edit.setPadding(
                dp(10),
                dp(8),
                dp(10),
                dp(8)
        );

        edit.setInputType(
                InputType.TYPE_CLASS_TEXT
        );

        return edit;
    }

    private void createInterface() {

        ScrollView scrollView =
                new ScrollView(this);

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setPadding(
                dp(20),
                dp(20),
                dp(20),
                dp(30)
        );

        TextView mainTitle =
                title("💱 صرافی هوشمند", 30);

        mainTitle.setGravity(
                Gravity.CENTER
        );

        layout.addView(mainTitle);

        TextView smart =
                new TextView(this);

        smart.setText(
                "🧠 حسابداری خودکار معاملات و مشتریان"
        );

        smart.setTextSize(17);
        smart.setGravity(Gravity.CENTER);

        smart.setPadding(
                0,
                0,
                0,
                dp(20)
        );

        layout.addView(smart);

        // ---------------- نرخ ارز ----------------

        layout.addView(
                title("📊 نرخ ارز", 22)
        );

        ratesText =
                new TextView(this);

        ratesText.setTextSize(16);
        ratesText.setPadding(
                0,
                0,
                0,
                dp(10)
        );

        layout.addView(ratesText);

        TextView rateNote =
                new TextView(this);

        rateNote.setText(
                "ℹ️ نرخ معامله را هنگام ثبت وارد کنید."
        );

        rateNote.setTextSize(14);

        rateNote.setPadding(
                0,
                dp(8),
                0,
                dp(15)
        );

        layout.addView(rateNote);

        // ---------------- مشتری ----------------

        layout.addView(
                title("👤 اطلاعات مشتری", 22)
        );

        customerInput =
                input("نام مشتری");

        layout.addView(customerInput);

        phoneInput =
                input("شماره تماس مشتری");

        phoneInput.setInputType(
                InputType.TYPE_CLASS_PHONE
        );

        layout.addView(phoneInput);

        // ---------------- ارز ----------------

        currencySpinner =
                new Spinner(this);

        List<String> currencyList =
                ExchangeData.getCurrencies();

        ArrayAdapter<String> currencyAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        currencyList
                );

        currencyAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        currencySpinner.setAdapter(
                currencyAdapter
        );

        layout.addView(currencySpinner);

        // ---------------- نوع معامله ----------------

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

        layout.addView(typeSpinner);

        // ---------------- مقدار ----------------

        amountInput =
                input("مقدار ارز");

        amountInput.setInputType(
                InputType.TYPE_CLASS_NUMBER |
                InputType.TYPE_NUMBER_FLAG_DECIMAL
        );

        layout.addView(amountInput);

        // ---------------- نرخ ----------------

        rateInput =
                input("نرخ هر واحد به افغانی");

        rateInput.setInputType(
                InputType.TYPE_CLASS_NUMBER |
                InputType.TYPE_NUMBER_FLAG_DECIMAL
        );

        layout.addView(rateInput);

        // ---------------- توضیحات ----------------

        noteInput =
                input("توضیحات معامله (اختیاری)");

        layout.addView(noteInput);

        // ---------------- محاسبه ----------------

        Button calculate =
                makeButton("🧮 محاسبه مبلغ کل");

        layout.addView(calculate);

        totalText =
                new TextView(this);

        totalText.setText(
                "مبلغ کل: 0 افغانی"
        );

        totalText.setTextSize(21);

        totalText.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        totalText.setPadding(
                0,
                dp(15),
                0,
                dp(15)
        );

        layout.addView(totalText);

        // ---------------- ثبت معامله ----------------

        Button save =
                makeButton("✅ ثبت معامله");

        layout.addView(save);

        // ---------------- حساب مشتری ----------------

        layout.addView(
                title("📒 حساب مشتری", 22)
        );

        customerBalanceText =
                new TextView(this);

        customerBalanceText.setTextSize(17);

        customerBalanceText.setPadding(
                0,
                dp(5),
                0,
                dp(15)
        );

        layout.addView(
                customerBalanceText
        );

        Button showCustomer =
                makeButton(
                        "🔎 نمایش حساب مشتری"
                );

        layout.addView(showCustomer);

        // ---------------- موجودی ----------------

        layout.addView(
                title("🔐 گاوصندوق و موجودی", 22)
        );

        balanceText =
                new TextView(this);

        balanceText.setTextSize(18);

        balanceText.setPadding(
                0,
                dp(5),
                0,
                dp(10)
        );

        layout.addView(balanceText);

        // ---------------- گاوصندوق ----------------

        Button vaultButton =
                makeButton(
                        "🔐 ورود به گاوصندوق هوشمند"
                );

        layout.addView(vaultButton);

        // ---------------- تبدیل ----------------

        layout.addView(
                title("🔄 تبدیل ارز", 22)
        );

        Button converter =
                makeButton(
                        "🔄 استفاده از تبدیل ارز"
                );

        layout.addView(converter);

        // ---------------- تاریخچه ----------------

        layout.addView(
                title("📋 تاریخچه معاملات", 22)
        );

        historyText =
                new TextView(this);

        historyText.setTextSize(16);

        historyText.setPadding(
                0,
                dp(5),
                0,
                dp(15)
        );

        layout.addView(historyText);

        // ---------------- تنظیمات ----------------

        Button settingsButton =
                makeButton(
                        "⚙️ تنظیمات صرافی"
                );

        layout.addView(settingsButton);

        // ---------------- گزارش ----------------

        layout.addView(
                title("📊 گزارش صرافی", 22)
        );

        reportText =
                new TextView(this);

        reportText.setTextSize(17);

        reportText.setPadding(
                0,
                dp(5),
                0,
                dp(20)
        );

        layout.addView(reportText);

        updateRatesText();
        updateBalanceText();
        updateHistory();
        updateReport();

        calculate.setOnClickListener(
                v -> calculateTotal()
        );

        save.setOnClickListener(
                v -> saveTransaction()
        );

        showCustomer.setOnClickListener(
                v -> showCustomerAccount()
        );

        converter.setOnClickListener(
                v -> showConverter()
        );

        vaultButton.setOnClickListener(
                v -> {

                    Intent intent =
                            new Intent(
                                    ExchangeActivity.this,
                                    VaultActivity.class
                            );

                    startActivity(intent);
                }
        );

        settingsButton.setOnClickListener(
                v -> showExchangeSettings()
        );

        scrollView.addView(layout);

        setContentView(scrollView);
    }

    private void updateRatesText() {

        StringBuilder text =
                new StringBuilder();

        text.append("🇦🇫 افغانی: 1\n");

        for (
                String currency :
                ExchangeData.getCurrencies()
        ) {

            if (
                    currency.equals(
                            ExchangeData.AFN
                    )
            ) {
                continue;
            }

            double rate =
                    ExchangeData.getRate(
                            currency
                    );

            text.append("💱 ")
                    .append(currency)
                    .append(": ")
                    .append(format(rate))
                    .append(" افغانی\n");
        }

        ratesText.setText(
                text.toString()
        );
    }

    private void calculateTotal() {

        try {

            double amount =
                    Double.parseDouble(
                            amountInput
                                    .getText()
                                    .toString()
                    );

            double rate =
                    Double.parseDouble(
                            rateInput
                                    .getText()
                                    .toString()
                    );

            if (amount <= 0 || rate <= 0) {
                throw new Exception();
            }

            double total =
                    amount * rate;

            totalText.setText(
                    "💰 مبلغ کل: " +
                    format(total) +
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

    private void saveTransaction() {

        try {

            String customer =
                    customerInput
                            .getText()
                            .toString()
                            .trim();

            String phone =
                    phoneInput
                            .getText()
                            .toString()
                            .trim();

            double amount =
                    Double.parseDouble(
                            amountInput
                                    .getText()
                                    .toString()
                    );

            double rate =
                    Double.parseDouble(
                            rateInput
                                    .getText()
                                    .toString()
                    );

            if (customer.length() == 0) {

                Toast.makeText(
                        this,
                        "نام مشتری را وارد کنید",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            if (amount <= 0 || rate <= 0) {
                throw new Exception();
            }

            double total =
                    amount * rate;

            String currency =
                    currencySpinner
                            .getSelectedItem()
                            .toString();

            String type =
                    typeSpinner
                            .getSelectedItem()
                            .toString();

            String note =
                    noteInput
                            .getText()
                            .toString()
                            .trim();

            /*
             * خرید:
             * صراف ارز را از مشتری می‌خرد.
             * موجودی ارز زیاد می‌شود و افغانی کم می‌شود.
             *
             * فروش:
             * صراف ارز را به مشتری می‌فروشد.
             * موجودی ارز کم می‌شود و افغانی زیاد می‌شود.
             */

            double currentAfn =
                    ExchangeData.getBalance(
                            ExchangeData.AFN
                    );

            double currentCurrency =
                    ExchangeData.getBalance(
                            currency
                    );

            if (type.equals("خرید")) {

                if (currentAfn < total) {

                    Toast.makeText(
                            this,
                            "❌ موجودی افغانی برای این خرید کافی نیست",
                            Toast.LENGTH_LONG
                    ).show();

                    return;
                }

                ExchangeData.setBalance(
                        ExchangeData.AFN,
                        currentAfn - total
                );

                ExchangeData.setBalance(
                        currency,
                        currentCurrency + amount
                );

            } else {

                if (currentCurrency < amount) {

                    Toast.makeText(
                            this,
                            "❌ موجودی " +
                            currency +
                            " برای فروش کافی نیست",
                            Toast.LENGTH_LONG
                    ).show();

                    return;
                }

                ExchangeData.setBalance(
                        currency,
                        currentCurrency - amount
                );

                ExchangeData.setBalance(
                        ExchangeData.AFN,
                        currentAfn + total
                );
            }

            String date =
                    new SimpleDateFormat(
                            "yyyy/MM/dd HH:mm",
                            Locale.getDefault()
                    ).format(
                            new Date()
                    );

            long receiptNumber =
                    System.currentTimeMillis();

            JSONObject transaction =
                    new JSONObject();

            transaction.put(
                    "customer",
                    customer
            );

            transaction.put(
                    "phone",
                    phone
            );

            transaction.put(
                    "currency",
                    currency
            );

            transaction.put(
                    "type",
                    type
            );

            transaction.put(
                    "amount",
                    amount
            );

            transaction.put(
                    "rate",
                    rate
            );

            transaction.put(
                    "total",
                    total
            );

            transaction.put(
                    "note",
                    note
            );

            transaction.put(
                    "date",
                    date
            );

            transaction.put(
                    "receipt",
                    receiptNumber
            );

            ExchangeData.saveTransaction(
                    transaction
            );

            totalText.setText(
                    "💰 مبلغ کل: " +
                    format(total) +
                    " افغانی"
            );

            updateBalanceText();
            updateHistory();
            updateReport();

            Toast.makeText(
                    this,
                    "✅ معامله ثبت شد\nرسید: " +
                    receiptNumber,
                    Toast.LENGTH_LONG
            ).show();

            amountInput.setText("");
            rateInput.setText("");
            noteInput.setText("");

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "اطلاعات معامله را درست وارد کنید",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void updateBalanceText() {

        StringBuilder text =
                new StringBuilder();

        text.append(
                "💰 موجودی فعلی\n\n"
        );

        for (
                String currency :
                ExchangeData.getCurrencies()
        ) {

            double balance =
                    ExchangeData.getBalance(
                            currency
                    );

            text.append("💱 ")
                    .append(currency)
                    .append(": ")
                    .append(format(balance))
                    .append("\n");
        }

        balanceText.setText(
                text.toString()
        );
    }

    private void updateHistory() {

        try {

            JSONArray history =
                    ExchangeData.getTransactions();

            if (history.length() == 0) {

                historyText.setText(
                        "هنوز معامله‌ای ثبت نشده است."
                );

                return;
            }

            StringBuilder text =
                    new StringBuilder();

            for (
                    int i = history.length() - 1;
                    i >= 0;
                    i--
            ) {

                JSONObject item =
                        history.getJSONObject(i);

                text.append(
                        "━━━━━━━━━━━━━━\n"
                );

                text.append(
                        "👤 مشتری: "
                ).append(
                        item.optString(
                                "customer"
                        )
                );

                text.append(
                        "\n💱 "
                ).append(
                        item.optString("type")
                ).append(
                        " "
                ).append(
                        item.optString("currency")
                );

                text.append(
                        "\n📦 مقدار: "
                ).append(
                        format(
                                item.optDouble(
                                        "amount"
                                )
                        )
                );

                text.append(
                        "\n💰 مبلغ: "
                ).append(
                        format(
                                item.optDouble(
                                        "total"
                                )
                        )
                ).append(
                        " افغانی"
                );

                text.append(
                        "\n📅 "
                ).append(
                        item.optString("date")
                );

                text.append(
                        "\n🧾 رسید: "
                ).append(
                        item.optLong("receipt")
                );

                text.append("\n");
            }

            historyText.setText(
                    text.toString()
            );

        } catch (Exception e) {

            historyText.setText(
                    "خطا در خواندن تاریخچه"
            );
        }
    }

    private void updateReport() {

        try {

            JSONArray history =
                    ExchangeData.getTransactions();

            int buys = 0;
            int sells = 0;

            double buyTotal = 0;
            double sellTotal = 0;

            for (
                    int i = 0;
                    i < history.length();
                    i++
            ) {

                JSONObject item =
                        history.getJSONObject(i);

                double total =
                        item.optDouble(
                                "total"
                        );

                String type =
                        item.optString(
                                "type"
                        );

                if (type.equals("خرید")) {

                    buys++;
                    buyTotal += total;

                } else {

                    sells++;
                    sellTotal += total;
                }
            }

            double result =
                    sellTotal - buyTotal;

            String status;

            if (result > 0) {

                status =
                        "🟢 فروش بیشتر از خرید";

            } else if (result < 0) {

                status =
                        "🔴 خرید بیشتر از فروش";

            } else {

                status =
                        "⚪ خرید و فروش برابر";
            }

            String report =
                    "📊 گزارش کلی\n\n" +
                    "🟢 تعداد خرید: " +
                    buys +
                    "\n" +
                    "💰 مجموع خرید: " +
                    format(buyTotal) +
                    " افغانی\n\n" +
                    "🔴 تعداد فروش: " +
                    sells +
                    "\n" +
                    "💰 مجموع فروش: " +
                    format(sellTotal) +
                    " افغانی\n\n" +
                    "📈 اختلاف فروش و خرید: " +
                    format(result) +
                    " افغانی\n\n" +
                    status;

            reportText.setText(report);

        } catch (Exception e) {

            reportText.setText(
                    "گزارش هنوز موجود نیست."
            );
        }
    }

    private void showCustomerAccount() {

        String customer =
                customerInput
                        .getText()
                        .toString()
                        .trim();

        if (customer.length() == 0) {

            Toast.makeText(
                    this,
                    "نام مشتری را وارد کنید",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        try {

            JSONArray history =
                    ExchangeData.getTransactions();

            double bought = 0;
            double sold = 0;

            int count = 0;

            for (
                    int i = 0;
                    i < history.length();
                    i++
            ) {

                JSONObject item =
                        history.getJSONObject(i);

                if (
                        item.optString(
                                "customer"
                        ).equals(customer)
                ) {

                    count++;

                    double total =
                            item.optDouble(
                                    "total"
                            );

                    if (
                            item.optString(
                                    "type"
                            ).equals("خرید")
                    ) {

                        bought += total;

                    } else {

                        sold += total;
                    }
                }
            }

            double balance =
                    sold - bought;

            String status;

            if (balance > 0) {

                status =
                        "🟢 مشتری طلبکار است";

            } else if (balance < 0) {

                status =
                        "🔴 مشتری بدهکار است";

            } else {

                status =
                        "⚪ حساب تسویه است";
            }

            customerBalanceText.setText(
                    "👤 " +
                    customer +
                    "\n\n" +
                    "🧾 تعداد معاملات: " +
                    count +
                    "\n" +
                    "🟢 مجموع خرید: " +
                    format(bought) +
                    " افغانی\n" +
                    "🔴 مجموع فروش: " +
                    format(sold) +
                    " افغانی\n\n" +
                    "💰 مانده حساب: " +
                    format(
                            Math.abs(balance)
                    ) +
                    " افغانی\n" +
                    status
            );

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "خطا در حساب مشتری",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void showConverter() {

        try {

            double amount =
                    Double.parseDouble(
                            amountInput
                                    .getText()
                                    .toString()
                    );

            double rate =
                    Double.parseDouble(
                            rateInput
                                    .getText()
                                    .toString()
                    );

            if (amount <= 0 || rate <= 0) {
                throw new Exception();
            }

            double result =
                    amount * rate;

            totalText.setText(
                    "🔄 تبدیل ارز\n\n" +
                    format(amount) +
                    " × " +
                    format(rate) +
                    "\n\n= " +
                    format(result) +
                    " افغانی"
            );

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "برای تبدیل، مقدار و نرخ را وارد کنید",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // ---------------- تنظیمات امن صرافی ----------------

    private void showExchangeSettings() {

        String[] options = {
                "🗑️ حذف تاریخچه معاملات"
        };

        new android.app.AlertDialog.Builder(this)
                .setTitle("⚙️ تنظیمات صرافی")
                .setItems(
                        options,
                        (dialog, which) -> {

                            if (which == 0) {
                                confirmClearHistory();
                            }
                        }
                )
                .setNegativeButton(
                        "بستن",
                        null
                )
                .show();
    }

    private void confirmClearHistory() {

        new android.app.AlertDialog.Builder(this)
                .setTitle("⚠️ هشدار مهم")
                .setMessage(
                        "تمام معاملات ثبت‌شده حذف خواهند شد.\n\n" +
                        "این کار را فقط در صورتی انجام دهید که کاملاً مطمئن باشید."
                )
                .setNegativeButton(
                        "انصراف",
                        null
                )
                .setPositiveButton(
                        "ادامه",
                        (dialog, which) -> {

                            new android.app.AlertDialog.Builder(this)
                                    .setTitle("🔴 تأیید نهایی")
                                    .setMessage(
                                            "آیا واقعاً می‌خواهید تمام تاریخچه معاملات را حذف کنید؟"
                                    )
                                    .setNegativeButton(
                                            "خیر",
                                            null
                                    )
                                    .setPositiveButton(
                                            "بله، حذف شود",
                                            (dialog2, which2) ->
                                                    clearHistory()
                                    )
                                    .show();
                        }
                )
                .show();
    }

    private void clearHistory() {

        ExchangeData.clearTransactions();

        updateHistory();
        updateReport();

        Toast.makeText(
                this,
                "تاریخچه پاک شد",
                Toast.LENGTH_SHORT
        ).show();
    }

    /*
     * انتقال اطلاعات قدیمی:
     *
     * اطلاعات نسخه قبلی حذف نمی‌شود.
     * فقط یک بار موجودی‌های قدیمی به سیستم مرکزی منتقل می‌شوند.
     */
    private void migrateOldDataIfNeeded() {

        SharedPreferences central =
                getSharedPreferences(
                        "tajro_exchange_data",
                        MODE_PRIVATE
                );

        boolean migrated =
                central.getBoolean(
                        "old_exchange_migrated",
                        false
                );

        if (migrated) {
            return;
        }

        migrateBalance(
                central,
                "afghani",
                ExchangeData.AFN
        );

        migrateBalance(
                central,
                "dollar",
                ExchangeData.USD
        );

        migrateBalance(
                central,
                "euro",
                ExchangeData.EUR
        );

        migrateBalance(
                central,
                "toman",
                ExchangeData.TOMAN
        );

        migrateBalance(
                central,
                "lira",
                ExchangeData.TRY
        );

        migrateBalance(
                central,
                "rupee",
                ExchangeData.PKR
        );

        central.edit()
                .putBoolean(
                        "old_exchange_migrated",
                        true
                )
                .apply();
    }

    private void migrateBalance(
            SharedPreferences central,
            String oldKey,
            String currency
    ) {

        if (!oldPrefs.contains(oldKey)) {
            return;
        }

        try {

            long bits =
                    oldPrefs.getLong(
                            oldKey,
                            Double.doubleToLongBits(0)
                    );

            double oldValue =
                    Double.longBitsToDouble(bits);

            double current =
                    ExchangeData.getBalance(
                            currency
                    );

            /*
             * اگر سیستم مرکزی مقدار پیش‌فرض دارد،
             * مقدار قدیمی را فقط وقتی وارد می‌کنیم
             * که مقدار واقعی قدیمی وجود داشته باشد.
             */
            if (oldValue != 0) {

                ExchangeData.setBalance(
                        currency,
                        oldValue
                );

            } else if (current == 0) {

                ExchangeData.setBalance(
                        currency,
                        0
                );
            }

        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (balanceText != null) {
            updateRatesText();
            updateBalanceText();
            updateHistory();
            updateReport();
        }
    }

    private String format(double value) {

        return String.format(
                Locale.US,
                "%.2f",
                value
        );
    }
    }
