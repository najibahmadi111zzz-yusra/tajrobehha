package com.tajro.app;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExchangeActivity extends BaseActivity {

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

    private final int BLUE = Color.rgb(12, 91, 120);
    private final int BG = Color.rgb(235, 248, 250);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        exchangeData = ExchangeData.get(this);
        currencies = ExchangeData.getCurrencies();

        buildMainScreen();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView makeText(String text, int size) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(size);
        tv.setTextColor(Color.DKGRAY);
        tv.setPadding(dp(8), dp(8), dp(8), dp(8));
        return tv;
    }

    private EditText makeInput(String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setTextSize(16);
        input.setPadding(dp(12), dp(8), dp(12), dp(8));
        input.setSingleLine(true);
        return input;
    }

    private Button makeButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(15);
        button.setTextColor(Color.WHITE);
        button.setBackgroundColor(BLUE);

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
        p.setMargins(dp(6), dp(5), dp(6), dp(5));
        button.setLayoutParams(p);

        return button;
    }

    private void buildMainScreen() {

        ScrollView scrollView = new ScrollView(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(12), dp(12), dp(20));
        root.setBackgroundColor(BG);

        TextView title = makeText("💱 صرافی تجربه‌ها", 25);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(BLUE);
        root.addView(title);

        TextView subtitle = makeText("ثبت و مدیریت خرید و فروش ارز", 15);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle);

        customerNameInput = makeInput("👤 نام مشتری");
        root.addView(customerNameInput);

        phoneInput = makeInput("📞 شماره تماس");
        root.addView(phoneInput);

        root.addView(makeText("💱 ارز", 15));

        currencySpinner = new Spinner(this);
        currencySpinner.setAdapter(
                new ArrayAdapter<String>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        currencies
                )
        );
        root.addView(currencySpinner);

        root.addView(makeText("🔄 نوع معامله", 15));

        typeSpinner = new Spinner(this);

        String[] types = {
                "خرید",
                "فروش"
        };

        typeSpinner.setAdapter(
                new ArrayAdapter<String>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        types
                )
        );

        root.addView(typeSpinner);

        amountInput = makeInput("💰 مقدار ارز");
        amountInput.setInputType(
                android.text.InputType.TYPE_CLASS_NUMBER |
                        android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        );
        root.addView(amountInput);

        rateInput = makeInput("💵 نرخ ارز");
        rateInput.setInputType(
                android.text.InputType.TYPE_CLASS_NUMBER |
                        android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        );
        root.addView(rateInput);

        totalText = makeText("💰 مجموع: 0", 17);
        totalText.setTextColor(BLUE);
        root.addView(totalText);

        balanceText = makeText("💼 موجودی: 0", 16);
        root.addView(balanceText);

        noteInput = makeInput("📝 یادداشت");
        root.addView(noteInput);

        Button calculateButton = makeButton("🧮 محاسبه مجموع");
        root.addView(calculateButton);

        Button saveButton = makeButton("💾 ثبت معامله");
        root.addView(saveButton);

        Button historyButton = makeButton("📜 تاریخچه معاملات");
        root.addView(historyButton);

        Button customersButton = makeButton("👤 مشتریان");
        root.addView(customersButton);

        Button reportButton = makeButton("📊 گزارش");
        root.addView(reportButton);

        Button balancesButton = makeButton("💰 همه موجودی‌ها");
        root.addView(balancesButton);

        Button converterButton = makeButton("🔄 تبدیل ارز");
        root.addView(converterButton);

        Button vaultButton = makeButton("🔐 خزانه");
        root.addView(vaultButton);

        Button settingsButton = makeButton("⚙️ تنظیمات");
        root.addView(settingsButton);

        calculateButton.setOnClickListener(v -> calculateTotal());

        saveButton.setOnClickListener(v -> saveTransaction());

        historyButton.setOnClickListener(v -> showHistory());

        customersButton.setOnClickListener(v -> showCustomers());

        reportButton.setOnClickListener(v -> showReport());

        balancesButton.setOnClickListener(v -> showBalances());

        converterButton.setOnClickListener(v -> showConverter());

        vaultButton.setOnClickListener(v ->
                Toast.makeText(
                        this,
                        "🔐 بخش خزانه",
                        Toast.LENGTH_SHORT
                ).show()
        );

        settingsButton.setOnClickListener(v -> showSettings());

        currencySpinner.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            android.widget.AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {
                        updateRate();
                        updateBalance();
                    }

                    @Override
                    public void onNothingSelected(
                            android.widget.AdapterView<?> parent
                    ) {
                    }
                }
        );

        amountInput.addTextChangedListener(new TextWatcher() {
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
                calculateTotal();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        rateInput.addTextChangedListener(new TextWatcher() {
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
                calculateTotal();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        scrollView.addView(root);
        setContentView(scrollView);

        updateRate();
        updateBalance();
    }

    private void updateRate() {

        if (currencySpinner == null || rateInput == null) return;

        String currency =
                currencySpinner.getSelectedItem().toString();

        if (ExchangeData.AFN.equals(currency)) {
            rateInput.setText("1");
        } else {
            double rate = exchangeData.getRate(currency);

            if (rate > 0) {
                rateInput.setText(formatNumber(rate));
            }
        }
    }

    private void updateBalance() {

        if (currencySpinner == null || balanceText == null) return;

        String currency =
                currencySpinner.getSelectedItem().toString();

        double balance =
                exchangeData.getBalance(currency);

        balanceText.setText(
                "💼 موجودی " + currency + ": " +
                        formatNumber(balance)
        );
    }

    private void calculateTotal() {

        try {
            double amount =
                    Double.parseDouble(
                            amountInput.getText().toString().trim()
                    );

            double rate =
                    Double.parseDouble(
                            rateInput.getText().toString().trim()
                    );

            double total = amount * rate;

            totalText.setText(
                    "💰 مجموع: " +
                            formatNumber(total)
            );

        } catch (Exception e) {

            totalText.setText("💰 مجموع: 0");
        }
    }

    private void saveTransaction() {

        String customerName =
                customerNameInput.getText().toString().trim();

        String phone =
                phoneInput.getText().toString().trim();

        String currency =
                currencySpinner.getSelectedItem().toString();

        String type =
                typeSpinner.getSelectedItem().toString();

        String amountText =
                amountInput.getText().toString().trim();

        String rateText =
                rateInput.getText().toString().trim();

        String note =
                noteInput.getText().toString().trim();

        if (customerName.isEmpty()) {
            customerNameInput.setError("نام مشتری را وارد کنید");
            customerNameInput.requestFocus();
            return;
        }

        if (amountText.isEmpty()) {
            amountInput.setError("مقدار را وارد کنید");
            amountInput.requestFocus();
            return;
        }

        if (rateText.isEmpty()) {
            rateInput.setError("نرخ را وارد کنید");
            rateInput.requestFocus();
            return;
        }

        if (ExchangeData.AFN.equals(currency)) {

            Toast.makeText(
                    this,
                    "افغانی به عنوان ارز معامله انتخاب نشود.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        double amount;
        double rate;

        try {
            amount = Double.parseDouble(amountText);
            rate = Double.parseDouble(rateText);
        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "مقدار یا نرخ نادرست است.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (amount <= 0 || rate <= 0) {

            Toast.makeText(
                    this,
                    "مقدار و نرخ باید بیشتر از صفر باشد.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        double total = amount * rate;

        if ("خرید".equals(type)) {

            double afnBalance =
                    exchangeData.getBalance(ExchangeData.AFN);

            if (afnBalance < total) {

                Toast.makeText(
                        this,
                        "موجودی افغانی کافی نیست.",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

        } else {

            double currencyBalance =
                    exchangeData.getBalance(currency);

            if (currencyBalance < amount) {

                Toast.makeText(
                        this,
                        "موجودی " + currency +
                                " برای فروش کافی نیست.",
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
                    "ثبت معامله انجام نشد.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if ("خرید".equals(type)) {

            exchangeData.subtractBalance(
                    ExchangeData.AFN,
                    total
            );

            exchangeData.addBalance(
                    currency,
                    amount
            );

        } else {

            exchangeData.subtractBalance(
                    currency,
                    amount
            );

            exchangeData.addBalance(
                    ExchangeData.AFN,
                    total
            );
        }

        exchangeData.setRate(currency, rate);

        showTransactionReceipt(
                customerName,
                phone,
                currency,
                type,
                amount,
                rate,
                total,
                note
        );

        customerNameInput.setText("");
        phoneInput.setText("");
        amountInput.setText("");
        noteInput.setText("");

        updateRate();
        updateBalance();
    }

    private void showTransactionReceipt(
            String customerName,
            String phone,
            String currency,
            String type,
            double amount,
            double rate,
            double total,
            String note
    ) {

        String receipt =
                "💱 رسید معامله صرافی تجربه‌ها\n\n" +
                        "شماره رسید: " +
                        System.currentTimeMillis() + "\n" +
                        "تاریخ: " +
                        formatDate(System.currentTimeMillis()) + "\n\n" +
                        "مشتری: " + customerName + "\n" +
                        "شماره تماس: " + phone + "\n" +
                        "نوع معامله: " + type + "\n" +
                        "ارز: " + currency + "\n" +
                        "مقدار: " + formatNumber(amount) + "\n" +
                        "نرخ: " + formatNumber(rate) + "\n" +
                        "مجموع: " + formatNumber(total) + " افغانی\n" +
                        "وضعیت: نقدی\n" +
                        "یادداشت: " + note;

        TextView text = makeText(receipt, 16);

        ScrollView scroll = new ScrollView(this);
        scroll.setPadding(
                dp(10),
                dp(5),
                dp(10),
                dp(5)
        );
        scroll.addView(text);

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle("🧾 رسید معامله")
                        .setView(scroll)
                        .setPositiveButton("بستن", null)
                        .setNegativeButton("کپی", null)
                        .setNeutralButton("اشتراک", null)
                        .create();

        dialog.setOnShowListener(d -> {

            dialog.getButton(
                    AlertDialog.BUTTON_NEGATIVE
            ).setOnClickListener(v -> {

                ClipboardManager clipboard =
                        (ClipboardManager)
                                getSystemService(
                                        Context.CLIPBOARD_SERVICE
                                );

                clipboard.setPrimaryClip(
                        ClipData.newPlainText(
                                "رسید معامله",
                                receipt
                        )
                );

                Toast.makeText(
                        this,
                        "رسید کپی شد.",
                        Toast.LENGTH_SHORT
                ).show();
            });

            dialog.getButton(
                    AlertDialog.BUTTON_NEUTRAL
            ).setOnClickListener(v -> {

                Intent sendIntent =
                        new Intent(Intent.ACTION_SEND);

                sendIntent.setType("text/plain");
                sendIntent.putExtra(
                        Intent.EXTRA_TEXT,
                        receipt
                );

                startActivity(
                        Intent.createChooser(
                                sendIntent,
                                "اشتراک رسید"
                        )
                );
            });
        });

        dialog.show();
    }

    private void showHistory() {

        JSONArray transactions =
                exchangeData.getTransactionsArray();

        if (transactions.length() == 0) {

            new AlertDialog.Builder(this)
                    .setTitle("📜 تاریخچه معاملات")
                    .setMessage("هنوز معامله‌ای ثبت نشده است.")
                    .setPositiveButton("باشه", null)
                    .show();

            return;
        }

        StringBuilder text =
                new StringBuilder();

        for (int i = transactions.length() - 1; i >= 0; i--) {

            JSONObject t =
                    transactions.optJSONObject(i);

            if (t == null) continue;

            text.append("━━━━━━━━━━━━━━\n");

            text.append("👤 ")
                    .append(t.optString("customerName"))
                    .append("\n");

            text.append("🔄 ")
                    .append(t.optString("type"))
                    .append(" ")
                    .append(t.optString("currency"))
                    .append("\n");

            text.append("💰 مقدار: ")
                    .append(
                            formatNumber(
                                    t.optDouble("amount", 0)
                            )
                    )
                    .append("\n");

            text.append("💵 نرخ: ")
                    .append(
                            formatNumber(
                                    t.optDouble("rate", 0)
                            )
                    )
                    .append("\n");

            text.append("💰 مجموع: ")
                    .append(
                            formatNumber(
                                    t.optDouble("total", 0)
                            )
                    )
                    .append(" افغانی\n");

            text.append("📅 ")
                    .append(
                            formatDate(
                                    t.optLong("date", 0)
                            )
                    )
                    .append("\n");

            String note =
                    t.optString("note", "");

            if (!note.isEmpty()) {
                text.append("📝 ")
                        .append(note)
                        .append("\n");
            }
        }

        TextView tv = makeText(text.toString(), 15);

        ScrollView scroll =
                new ScrollView(this);

        scroll.addView(tv);

        new AlertDialog.Builder(this)
                .setTitle("📜 تاریخچه معاملات")
                .setView(scroll)
                .setPositiveButton("بستن", null)
                .show();
    }

    /*
     * =========================================================
     * مشتریان + جستجوی نام مشتری
     * =========================================================
     */

    private void showCustomers() {

        final LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setPadding(
                dp(8),
                dp(4),
                dp(8),
                dp(4)
        );

        final SearchView searchView =
                new SearchView(this);

        searchView.setIconifiedByDefault(false);
        searchView.setQueryHint(
                "🔎 جستجوی نام مشتری"
        );

        layout.addView(searchView);

        final TextView resultText =
                makeText("", 15);

        resultText.setPadding(
                dp(8),
                dp(12),
                dp(8),
                dp(12)
        );

        ScrollView resultScroll =
                new ScrollView(this);

        resultScroll.addView(resultText);

        LinearLayout.LayoutParams resultParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(400)
                );

        resultScroll.setLayoutParams(resultParams);

        layout.addView(resultScroll);

        /*
         * نمایش اولیه همه مشتریان
         */
        resultText.setText(
                buildCustomerText("")
        );

        /*
         * جستجوی زنده هنگام تایپ نام
         */
        searchView.setOnQueryTextListener(
                new SearchView.OnQueryTextListener() {

                    @Override
                    public boolean onQueryTextSubmit(
                            String query
                    ) {
                        resultText.setText(
                                buildCustomerText(query)
                        );

                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(
                            String newText
                    ) {
                        resultText.setText(
                                buildCustomerText(newText)
                        );

                        return true;
                    }
                }
        );

        new AlertDialog.Builder(this)
                .setTitle("👤 مشتریان")
                .setView(layout)
                .setPositiveButton("بستن", null)
                .show();
    }

    /*
     * ساخت لیست مشتریان بر اساس جستجو
     */
    private String buildCustomerText(String query) {

        String normalizedQuery =
                normalizePersian(query);

        List<String> names =
                exchangeData.getCustomerNames();

        StringBuilder result =
                new StringBuilder();

        int count = 0;

        for (String name : names) {

            if (name == null || name.trim().isEmpty()) {
                continue;
            }

            String normalizedName =
                    normalizePersian(name);

            /*
             * اگر جستجو خالی باشد همه را نشان بده.
             * اگر چیزی نوشته شده باشد فقط نام‌های
             * شامل عبارت جستجو نمایش داده می‌شوند.
             */
            if (!normalizedQuery.isEmpty()
                    && !normalizedName.contains(
                    normalizedQuery
            )) {
                continue;
            }

            double debt =
                    exchangeData.getCustomerDebt(name);

            double credit =
                    exchangeData.getCustomerCredit(name);

            double balance =
                    exchangeData.getCustomerBalance(name);

            result.append("━━━━━━━━━━━━━━\n");

            result.append("👤 ")
                    .append(name)
                    .append("\n");

            result.append("🔴 بدهکار: ")
                    .append(formatNumber(debt))
                    .append(" افغانی\n");

            result.append("🟢 طلبکار: ")
                    .append(formatNumber(credit))
                    .append(" افغانی\n");

            result.append("💰 حساب: ")
                    .append(formatNumber(balance))
                    .append(" افغانی\n");

            count++;
        }

        if (count == 0) {

            if (normalizedQuery.isEmpty()) {
                return "هنوز مشتری ثبت نشده است.";
            }

            return "🔎 مشتری با این نام پیدا نشد.";
        }

        return "👥 تعداد مشتری: " +
                count +
                "\n\n" +
                result.toString();
    }

    /*
     * برای اینکه جستجو بین حروف عربی و فارسی هم درست کار کند.
     * مثلاً ي و ی یا ك و ک.
     */
    private String normalizePersian(String text) {

        if (text == null) return "";

        return text
                .trim()
                .replace('ي', 'ی')
                .replace('ى', 'ی')
                .replace('ك', 'ک')
                .toLowerCase(Locale.ROOT);
    }

    private void showReport() {

        double buy =
                exchangeData.getTotalBuy();

        double sell =
                exchangeData.getTotalSell();

        int count =
                exchangeData.getTransactionCount();

        String report =
                "📊 گزارش صرافی\n\n" +
                        "تعداد معاملات: " + count + "\n\n" +
                        "🟢 مجموع خرید: " +
                        formatNumber(buy) +
                        " افغانی\n\n" +
                        "🔵 مجموع فروش: " +
                        formatNumber(sell) +
                        " افغانی";

        new AlertDialog.Builder(this)
                .setTitle("📊 گزارش")
                .setMessage(report)
                .setPositiveButton("بستن", null)
                .show();
    }

    private void showBalances() {

        StringBuilder text =
                new StringBuilder();

        for (String currency :
                ExchangeData.getCurrencies()) {

            double balance =
                    exchangeData.getBalance(currency);

            text.append("💰 ")
                    .append(currency)
                    .append(": ")
                    .append(formatNumber(balance))
                    .append("\n");
        }

        new AlertDialog.Builder(this)
                .setTitle("💰 همه موجودی‌ها")
                .setMessage(text.toString())
                .setPositiveButton("بستن", null)
                .show();
    }

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

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        currencies
                );

        fromSpinner.setAdapter(adapter);
        toSpinner.setAdapter(adapter);

        EditText amount =
                makeInput("مقدار");

        amount.setInputType(
                android.text.InputType.TYPE_CLASS_NUMBER |
                        android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        );

        TextView result =
                makeText("نتیجه: 0", 17);

        Button convert =
                makeButton("🔄 تبدیل");

        layout.addView(fromSpinner);
        layout.addView(toSpinner);
        layout.addView(amount);
        layout.addView(convert);
        layout.addView(result);

        convert.setOnClickListener(v -> {

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
                        getAfghaniRate(from);

                double toRate =
                        getAfghaniRate(to);

                if (fromRate <= 0 ||
                        toRate <= 0) {

                    result.setText(
                            "نرخ تبدیل موجود نیست."
                    );

                    return;
                }

                double afn =
                        value * fromRate;

                double converted =
                        afn / toRate;

                result.setText(
                        formatNumber(value) +
                                " " + from +
                                " = " +
                                formatNumber(converted) +
                                " " + to
                );

            } catch (Exception e) {

                result.setText(
                        "مقدار نادرست است."
                );
            }
        });

        new AlertDialog.Builder(this)
                .setTitle("🔄 تبدیل ارز")
                .setView(layout)
                .setPositiveButton("بستن", null)
                .show();
    }

    private double getAfghaniRate(String currency) {

        if (ExchangeData.AFN.equals(currency)) {
            return 1;
        }

        return exchangeData.getRate(currency);
    }

    private void showSettings() {

        String[] options = {
                "🗑️ حذف تاریخچه معاملات",
                "⚠️ حذف تمام اطلاعات صرافی"
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
                .setTitle("⚠️ تأیید حذف")
                .setMessage(
                        "آیا مطمئن هستید که می‌خواهید " +
                                "تمام تاریخچه معاملات حذف شود؟"
                )
                .setNegativeButton(
                        "خیر",
                        null
                )
                .setPositiveButton(
                        "بله، ادامه",
                        (dialog, which) ->
                                secondTransactionConfirmation()
                )
                .show();
    }

    private void secondTransactionConfirmation() {

        new AlertDialog.Builder(this)
                .setTitle("⚠️ تأیید نهایی")
                .setMessage(
                        "این عملیات قابل برگشت نیست.\n\n" +
                                "آیا واقعاً می‌خواهید " +
                                "تاریخچه معاملات حذف شود؟"
                )
                .setNegativeButton(
                        "انصراف",
                        null
                )
                .setPositiveButton(
                        "حذف شود",
                        (dialog, which) -> {

                            exchangeData.clearTransactions();

                            Toast.makeText(
                                    this,
                                    "تاریخچه معاملات حذف شد.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                )
                .show();
    }

    private void confirmClearAllData() {

        new AlertDialog.Builder(this)
                .setTitle("🚨 هشدار")
                .setMessage(
                        "تمام اطلاعات صرافی، مشتریان، " +
                                "معاملات، موجودی‌ها و نرخ‌ها " +
                                "به حالت اولیه برمی‌گردد.\n\n" +
                                "آیا مطمئن هستید؟"
                )
                .setNegativeButton(
                        "انصراف",
                        null
                )
                .setPositiveButton(
                        "ادامه",
                        (dialog, which) ->
                                secondAllDataConfirmation()
                )
                .show();
    }

    private void secondAllDataConfirmation() {

        new AlertDialog.Builder(this)
                .setTitle("🚨 تأیید نهایی")
                .setMessage(
                        "این عملیات تمام اطلاعات صرافی را پاک " +
                                "و موجودی‌ها را به مقدار اولیه برمی‌گرداند.\n\n" +
                                "آیا واقعاً می‌خواهید ادامه دهید؟"
                )
                .setNegativeButton(
                        "خیر",
                        null
                )
                .setPositiveButton(
                        "بله، حذف همه",
                        (dialog, which) -> {

                            exchangeData.clearAllData();

                            updateRate();
                            updateBalance();

                            Toast.makeText(
                                    this,
                                    "تمام اطلاعات صرافی پاک شد.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                )
                .show();
    }

    private String formatNumber(double value) {

        if (value == (long) value) {
            return String.format(
                    Locale.US,
                    "%d",
                    (long) value
            );
        }

        return String.format(
                Locale.US,
                "%.4f",
                value
        ).replaceAll(
                "0+$",
                ""
        ).replaceAll(
                "\\.$",
                ""
        );
    }

    private String formatDate(long timestamp) {

        SimpleDateFormat sdf =
                new SimpleDateFormat(
                        "yyyy/MM/dd HH:mm",
                        Locale.getDefault()
                );

        return sdf.format(
                new Date(timestamp)
        );
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (exchangeData != null) {
            updateRate();
            updateBalance();
        }
    }
                      }
