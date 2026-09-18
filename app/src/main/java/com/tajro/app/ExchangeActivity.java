package com.tajro.app;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
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

    private TextView makeText(
            String text,
            float size
    ) {
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

    private EditText makeInput(
            String hint
    ) {
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

    private Button makeButton(
            String text
    ) {
        Button button = new Button(this);

        button.setText(text);
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
    // آداپتر ارز همراه با پرچم
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
                                + currency
                );

                tv.setTextSize(16);

                tv.setTextColor(
                        Color.DKGRAY
                );

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

        title.setGravity(
                Gravity.CENTER
        );

        main.addView(title);

        TextView subtitle =
                makeText(
                        "ثبت و مدیریت خرید و فروش ارز",
                        17
                );

        subtitle.setGravity(
                Gravity.CENTER
        );

        main.addView(subtitle);

        // =====================================================
        // مشتری
        // =====================================================

        customerNameInput =
                makeInput("نام مشتری");

        main.addView(
                customerNameInput
        );

        phoneInput =
                makeInput("شماره تلفن");

        phoneInput.setInputType(
                InputType.TYPE_CLASS_PHONE
        );

        main.addView(
                phoneInput
        );

        // =====================================================
        // ارز
        // =====================================================

        main.addView(
                makeText(
                        "انتخاب ارز",
                        16
                )
        );

        currencySpinner =
                new Spinner(this);

        ArrayAdapter<String>
                currencyAdapter =
                createCurrencyAdapter(
                        currencies
                );

        currencyAdapter
                .setDropDownViewResource(
                        android.R.layout
                                .simple_spinner_dropdown_item
                );

        currencySpinner.setAdapter(
                currencyAdapter
        );

        main.addView(
                currencySpinner
        );

        // =====================================================
        // نوع معامله
        // =====================================================

        main.addView(
                makeText(
                        "نوع معامله",
                        16
                )
        );

        typeSpinner =
                new Spinner(this);

        String[] types = {
                "خرید",
                "فروش"
        };

        ArrayAdapter<String>
                typeAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout
                                .simple_spinner_item,
                        types
                );

        typeAdapter
                .setDropDownViewResource(
                        android.R.layout
                                .simple_spinner_dropdown_item
                );

        typeSpinner.setAdapter(
                typeAdapter
        );

        main.addView(
                typeSpinner
        );

        // =====================================================
        // مقدار
        // =====================================================

        amountInput =
                makeInput(
                        "مقدار ارز"
                );

        amountInput.setInputType(
                InputType.TYPE_CLASS_NUMBER |
                        InputType.TYPE_NUMBER_FLAG_DECIMAL
        );

        main.addView(
                amountInput
        );

        // =====================================================
        // نرخ
        // =====================================================

        rateInput =
                makeInput(
                        "نرخ هر واحد به افغانی"
                );

        rateInput.setInputType(
                InputType.TYPE_CLASS_NUMBER |
                        InputType.TYPE_NUMBER_FLAG_DECIMAL
        );

        main.addView(
                rateInput
        );

        // =====================================================
        // یادداشت
        // =====================================================

        noteInput =
                makeInput(
                        "یادداشت / توضیحات"
                );

        main.addView(
                noteInput
        );

        // =====================================================
        // مجموع
        // =====================================================

        totalText =
                makeText(
                        "مجموع: 0 افغانی",
                        18
                );

        totalText.setGravity(
                Gravity.CENTER
        );

        main.addView(
                totalText
        );

        // =====================================================
        // موجودی
        // =====================================================

        balanceText =
                makeText(
                        "موجودی: 0",
                        17
                );

        balanceText.setGravity(
                Gravity.CENTER
        );

        main.addView(
                balanceText
        );

        // =====================================================
        // محاسبه
        // =====================================================

        Button calculateButton =
                makeButton(
                        "🧮 محاسبه"
                );

        main.addView(
                calculateButton
        );

        calculateButton.setOnClickListener(
                v -> calculateTotal()
        );

        // =====================================================
        // ثبت
        // =====================================================

        Button saveButton =
                makeButton(
                        "💾 ثبت معامله"
                );

        main.addView(
                saveButton
        );

        saveButton.setOnClickListener(
                v -> saveTransaction()
        );

        // =====================================================
        // تاریخچه
        // =====================================================

        Button historyButton =
                makeButton(
                        "📜 تاریخچه معاملات"
                );

        main.addView(
                historyButton
        );

        historyButton.setOnClickListener(
                v -> showHistory()
        );

        // =====================================================
        // مشتریان
        // =====================================================

        Button customersButton =
                makeButton(
                        "👤 مشتریان"
                );

        main.addView(
                customersButton
        );

        customersButton.setOnClickListener(
                v -> showCustomers()
        );

        // =====================================================
        // گزارش
        // =====================================================

        Button reportButton =
                makeButton(
                        "📊 گزارش صرافی"
                );

        main.addView(
                reportButton
        );

        reportButton.setOnClickListener(
                v -> showReport()
        );

        // =====================================================
        // موجودی همه ارزها
        // =====================================================

        Button balancesButton =
                makeButton(
                        "💰 موجودی همه ارزها"
                );

        main.addView(
                balancesButton
        );

        balancesButton.setOnClickListener(
                v -> showBalances()
        );

        // =====================================================
        // تبدیل ارز
        // =====================================================

        Button converterButton =
                makeButton(
                        "🔄 تبدیل ارز"
                );

        main.addView(
                converterButton
        );

        converterButton.setOnClickListener(
                v -> showConverter()
        );

        // =====================================================
        // گاوصندوق
        // =====================================================

        Button vaultButton =
                makeButton(
                        "🔐 گاوصندوق هوشمند"
                );

        main.addView(
                vaultButton
        );

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

        // =====================================================
        // تنظیمات
        // =====================================================

        Button settingsButton =
                makeButton(
                        "⚙️ تنظیمات"
                );

        main.addView(
                settingsButton
        );

        settingsButton.setOnClickListener(
                v -> showSettings()
        );

        // =====================================================
        // تغییر ارز
        // =====================================================

        currencySpinner
                .setOnItemSelectedListener(
                        new AdapterView
                                .OnItemSelectedListener() {

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

        setContentView(
                scrollView
        );

        updateBalance();
        updateRate();
    }

    // =========================================================
    // بروزرسانی نرخ
    // =========================================================

    private void updateRate() {

        if (currencySpinner == null) {
            return;
        }

        Object selected =
                currencySpinner
                        .getSelectedItem();

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
                exchangeData.getRate(
                        currency
                );

        if (rate > 0) {

            rateInput.setText(
                    formatNumber(rate)
            );
        }
    }

    // =========================================================
    // بروزرسانی موجودی
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
                currencySpinner
                        .getSelectedItem();

        if (selected == null) {
            return;
        }

        String currency =
                selected.toString();

        double balance =
                exchangeData.getBalance(
                        currency
                );

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

            if (
                    amount <= 0 ||
                    rate <= 0
            ) {

                throw new Exception();
            }

            double total =
                    amount * rate;

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
                customerNameInput
                        .getText()
                        .toString()
                        .trim();

        String phone =
                phoneInput
                        .getText()
                        .toString()
                        .trim();

        String currency =
                currencySpinner
                        .getSelectedItem()
                        .toString();

        String type =
                typeSpinner
                        .getSelectedItem()
                        .toString();

        String amountText =
                amountInput
                        .getText()
                        .toString()
                        .trim();

        String rateText =
                rateInput
                        .getText()
                        .toString()
                        .trim();

        String note =
                noteInput
                        .getText()
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
                    Double.parseDouble(
                            amountText
                    );

            rate =
                    Double.parseDouble(
                            rateText
                    );

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "مقدار یا نرخ نادرست است",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (
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

        // -----------------------------------------------------
        // افغانی به عنوان ارز پایه
        // -----------------------------------------------------

        if (ExchangeData.AFN.equals(currency)) {

            Toast.makeText(
                    this,
                    "افغانی ارز پایه است؛ برای معامله ارز خارجی را انتخاب کنید.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        double total =
                amount * rate;

        // -----------------------------------------------------
        // خرید
        // صرافی ارز را از مشتری می‌خرد
        // -----------------------------------------------------

        if ("خرید".equals(type)) {

            double afnBalance =
                    exchangeData.getBalance(
                            ExchangeData.AFN
                    );

            if (afnBalance < total) {

                Toast.makeText(
                        this,
                        "موجودی افغانی صرافی کافی نیست.",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            exchangeData.setBalance(
                    ExchangeData.AFN,
                    afnBalance - total
            );

            exchangeData.addBalance(
                    currency,
                    amount
            );
        }

        // -----------------------------------------------------
        // فروش
        // صرافی ارز را به مشتری می‌فروشد
        // -----------------------------------------------------

        else {

            double currencyBalance =
                    exchangeData.getBalance(
                            currency
                    );

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

            exchangeData.subtractBalance(
                    currency,
                    amount
            );

            exchangeData.addBalance(
                    ExchangeData.AFN,
                    total
            );
        }

        // ذخیره آخرین نرخ
        exchangeData.setRate(
                currency,
                rate
        );

        // ثبت معامله در تاریخچه
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
                    "خطا در ثبت معامله",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        totalText.setText(
                "مجموع: " +
                        formatNumber(total) +
                        " افغانی"
        );

        updateBalance();

        Toast.makeText(
                this,
                "✅ معامله با موفقیت ثبت شد",
                Toast.LENGTH_LONG
        ).show();

        amountInput.setText("");
        noteInput.setText("");
    }

    // =========================================================
    // تاریخچه
    // =========================================================

    private void showHistory() {

        JSONArray transactions =
                exchangeData
                        .getTransactionsArray();

        if (
                transactions == null ||
                transactions.length() == 0
        ) {

            new AlertDialog.Builder(this)
                    .setTitle(
                            "📜 تاریخچه معاملات"
                    )
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
                int i =
                        transactions.length() - 1;
                i >= 0;
                i--
        ) {

            JSONObject transaction =
                    transactions
                            .optJSONObject(i);

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

                                    "\n📅 تاریخ: " +
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
                .setTitle(
                        "📜 تاریخچه معاملات"
                )
                .setView(scroll)
                .setPositiveButton(
                        "بستن",
                        null
                )
                .show();
    }

    // =========================================================
    // مشتریان
    // =========================================================

    private void showCustomers() {

        java.util.List<String> names =
                exchangeData
                        .getCustomerNames();

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

        StringBuilder text =
                new StringBuilder();

        for (String name : names) {

            double debt =
                    exchangeData
                            .getCustomerDebt(
                                    name
                            );

            double credit =
                    exchangeData
                            .getCustomerCredit(
                                    name
                            );

            double balance =
                    exchangeData
                            .getCustomerBalance(
                                    name
                            );

            text.append(
                    "👤 " +
                            name
            );

            text.append(
                    "\nبدهکاری: " +
                            formatNumber(debt)
            );

            text.append(
                    "\nطلبکاری: " +
                            formatNumber(credit)
            );

            text.append(
                    "\nمانده حساب: " +
                            formatNumber(balance)
            );

            text.append(
                    "\n\n"
            );
        }

        new AlertDialog.Builder(this)
                .setTitle(
                        "👤 مشتریان"
                )
                .setMessage(
                        text.toString()
                )
                .setPositiveButton(
                        "بستن",
                        null
                )
                .show();
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
                exchangeData
                        .getTransactionCount();

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
                .setTitle(
                        "📊 گزارش"
                )
                .setMessage(
                        report
                )
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

        for (String currency :
                currencies) {

            double balance =
                    exchangeData
                            .getBalance(
                                    currency
                            );

            double rate =
                    exchangeData
                            .getRate(
                                    currency
                            );

            text.append(
                    getCurrencyFlag(currency) +
                            " " +
                            currency
            );

            text.append(
                    "\nموجودی: " +
                            formatNumber(balance)
            );

            text.append(
                    "\nنرخ: " +
                            formatNumber(rate)
            );

            text.append(
                    "\n\n"
            );
        }

        new AlertDialog.Builder(this)
                .setTitle(
                        "💰 موجودی همه ارزها"
                )
                .setMessage(
                        text.toString()
                )
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

        // -----------------------------------------------------
        // لیست ارز مبدأ با پرچم
        // -----------------------------------------------------

        ArrayAdapter<String>
                adapterFrom =
                createCurrencyAdapter(
                        currencies
                );

        adapterFrom
                .setDropDownViewResource(
                        android.R.layout
                                .simple_spinner_dropdown_item
                );

        // -----------------------------------------------------
        // لیست ارز مقصد با پرچم
        // -----------------------------------------------------

        ArrayAdapter<String>
                adapterTo =
                createCurrencyAdapter(
                        currencies
                );

        adapterTo
                .setDropDownViewResource(
                        android.R.layout
                                .simple_spinner_dropdown_item
                );

        fromSpinner.setAdapter(
                adapterFrom
        );

        toSpinner.setAdapter(
                adapterTo
        );

        EditText amount =
                makeInput(
                        "مقدار برای تبدیل"
                );

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
                makeButton(
                        "🔄 تبدیل"
                );

        layout.addView(
                makeText(
                        "از ارز",
                        15
                )
        );

        layout.addView(
                fromSpinner
        );

        layout.addView(
                makeText(
                        "به ارز",
                        15
                )
        );

        layout.addView(
                toSpinner
        );

        layout.addView(
                amount
        );

        layout.addView(
                convert
        );

        layout.addView(
                result
        );

        convert.setOnClickListener(
                v -> {

                    try {

                        double value =
                                Double.parseDouble(
                                        amount.getText()
                                                .toString()
                                                .trim()
                                );

                        String from =
                                fromSpinner
                                        .getSelectedItem()
                                        .toString();

                        String to =
                                toSpinner
                                        .getSelectedItem()
                                        .toString();

                        double fromRate =
                                getRateToAFN(
                                        from
                                );

                        double toRate =
                                getRateToAFN(
                                        to
                                );

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
                                        formatNumber(
                                                converted
                                        ) +
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
                .setTitle(
                        "🔄 تبدیل ارز"
                )
                .setView(layout)
                .setPositiveButton(
                        "بستن",
                        null
                )
                .show();
    }

    private double getRateToAFN(
            String currency
    ) {

        if (
                ExchangeData.AFN.equals(
                        currency
                )
        ) {

            return 1;
        }

        return exchangeData.getRate(
                currency
        );
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
                .setTitle(
                        "⚙️ تنظیمات صرافی"
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
                        "بستن",
                        null
                )
                .show();
    }

    // =========================================================
    // حذف تاریخچه
    // =========================================================

    private void confirmClearTransactions() {

        new AlertDialog.Builder(this)
                .setTitle(
                        "⚠️ هشدار"
                )
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
                                    .setTitle(
                                            "تأیید نهایی"
                                    )
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

    // =========================================================
    // حذف تمام اطلاعات
    // =========================================================

    private void confirmClearAllData() {

        new AlertDialog.Builder(this)
                .setTitle(
                        "⚠️ هشدار بسیار مهم"
                )
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
                                    .setTitle(
                                            "تأیید نهایی"
                                    )
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

    private String formatNumber(
            double value
    ) {

        if (
                value == Math.floor(value)
        ) {

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
    // تاریخ
    // =========================================================

    private String formatDate(
            long timestamp
    ) {

        if (timestamp <= 0) {
            return "";
        }

        SimpleDateFormat sdf =
                new SimpleDateFormat(
                        "yyyy/MM/dd HH:mm",
                        Locale.getDefault()
                );

        return sdf.format(
                new Date(timestamp)
        );
    }

    // =========================================================
    // برگشت به صفحه
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
