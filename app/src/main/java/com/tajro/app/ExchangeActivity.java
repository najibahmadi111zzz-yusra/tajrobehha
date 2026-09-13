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
import java.util.Locale;

public class ExchangeActivity extends Activity {

    private SharedPreferences prefs;

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

    private double afghaniBalance;
    private double dollarBalance;
    private double euroBalance;
    private double tomanBalance;
    private double liraBalance;
    private double rupeeBalance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences(
                "exchange_data",
                MODE_PRIVATE
        );

        loadBalances();
        createInterface();
    }

    private int dp(int value) {
        return (int) (
                value * getResources()
                        .getDisplayMetrics().density
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

        TextView rates =
                new TextView(this);

        rates.setText(
                "🇺🇸 دلار: نرخ را وارد کنید\n" +
                "🇪🇺 یورو: نرخ را وارد کنید\n" +
                "🇮🇷 تومان: نرخ را وارد کنید\n" +
                "🇹🇷 لیره: نرخ را وارد کنید\n" +
                "🇵🇰 کلدار: نرخ را وارد کنید"
        );

        rates.setTextSize(17);
        layout.addView(rates);

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

        // ---------------- ثبت مشتری ----------------

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

        // ---------------- نوع ارز ----------------

        currencySpinner =
                new Spinner(this);

        String[] currencies = {
                "دلار آمریکا",
                "یورو",
                "تومان",
                "لیره ترکیه",
                "کلدار پاکستان"
        };

        ArrayAdapter<String> currencyAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        currencies
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

        // ---------------- دکمه گاوصندوق هوشمند ----------------

        Button vaultButton =
                makeButton(
                        "🔐 ورود به گاوصندوق هوشمند"
                );

        layout.addView(vaultButton);

        // ---------------- تبدیل ارز ----------------

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

        Button clearHistory =
                makeButton(
                        "🗑️ پاک کردن تاریخچه"
                );

        layout.addView(clearHistory);

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

        updateBalanceText();
        updateHistory();
        updateReport();

        // ---------------- دکمه‌ها ----------------

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

        clearHistory.setOnClickListener(
                v -> clearHistory()
        );

        // باز کردن گاوصندوق
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

        scrollView.addView(layout);

        setContentView(scrollView);
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

            if (type.equals("خرید")) {

                addCurrency(
                        currency,
                        amount
                );

                afghaniBalance -= total;

            } else {

                removeCurrency(
                        currency,
                        amount
                );

                afghaniBalance += total;
            }

            saveBalances();

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

            String oldHistory =
                    prefs.getString(
                            "history",
                            "[]"
                    );

            JSONArray history =
                    new JSONArray(
                            oldHistory
                    );

            history.put(
                    transaction
            );

            prefs.edit()
                    .putString(
                            "history",
                            history.toString()
                    )
                    .apply();

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

    private void addCurrency(
            String currency,
            double amount
    ) {

        if (currency.equals("دلار آمریکا")) {
            dollarBalance += amount;

        } else if (currency.equals("یورو")) {
            euroBalance += amount;

        } else if (currency.equals("تومان")) {
            tomanBalance += amount;

        } else if (currency.equals("لیره ترکیه")) {
            liraBalance += amount;

        } else if (currency.equals("کلدار پاکستان")) {
            rupeeBalance += amount;
        }
    }

    private void removeCurrency(
            String currency,
            double amount
    ) {

        if (currency.equals("دلار آمریکا")) {
            dollarBalance -= amount;

        } else if (currency.equals("یورو")) {
            euroBalance -= amount;

        } else if (currency.equals("تومان")) {
            tomanBalance -= amount;

        } else if (currency.equals("لیره ترکیه")) {
            liraBalance -= amount;

        } else if (currency.equals("کلدار پاکستان")) {
            rupeeBalance -= amount;
        }
    }

    private void updateBalanceText() {

        String text =
                "💰 موجودی فعلی\n\n" +
                "🇦🇫 افغانی: " +
                format(afghaniBalance) +
                "\n" +
                "🇺🇸 دلار: " +
                format(dollarBalance) +
                "\n" +
                "🇪🇺 یورو: " +
                format(euroBalance) +
                "\n" +
                "🇮🇷 تومان: " +
                format(tomanBalance) +
                "\n" +
                "🇹🇷 لیره: " +
                format(liraBalance) +
                "\n" +
                "🇵🇰 کلدار: " +
                format(rupeeBalance);

        balanceText.setText(text);
    }

    private void updateHistory() {

        try {

            String saved =
                    prefs.getString(
                            "history",
                            "[]"
                    );

            JSONArray history =
                    new JSONArray(saved);

            if (history.length() == 0) {

                historyText.setText(
                        "هنوز معامله‌ای ثبت نشده است."
                );

                return;
            }

            StringBuilder text =
                    new StringBuilder();

            for (int i =
                    history.length() - 1;
                    i >= 0;
                    i--) {

                JSONObject item =
                        history.getJSONObject(i);

                text.append(
                        "━━━━━━━━━━━━━━\n"
                );

                text.append(
                        "👤 مشتری: "
                ).append(
                        item.optString("customer")
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

            String saved =
                    prefs.getString(
                            "history",
                            "[]"
                    );

            JSONArray history =
                    new JSONArray(saved);

            int buys = 0;
            int sells = 0;

            double buyTotal = 0;
            double sellTotal = 0;

            for (int i = 0;
                    i < history.length();
                    i++) {

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
                    new JSONArray(
                            prefs.getString(
                                    "history",
                                    "[]"
                            )
                    );

            double bought = 0;
            double sold = 0;

            int count = 0;

            for (int i = 0;
                    i < history.length();
                    i++) {

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
                    format(Math.abs(balance)) +
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

    private void clearHistory() {

        prefs.edit()
                .putString(
                        "history",
                        "[]"
                )
                .apply();

        updateHistory();
        updateReport();

        Toast.makeText(
                this,
                "تاریخچه پاک شد",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void loadBalances() {

        afghaniBalance =
                Double.longBitsToDouble(
                        prefs.getLong(
                                "afghani",
                                Double.doubleToLongBits(
                                        100000
                                )
                        )
                );

        dollarBalance =
                Double.longBitsToDouble(
                        prefs.getLong(
                                "dollar",
                                Double.doubleToLongBits(0)
                        )
                );

        euroBalance =
                Double.longBitsToDouble(
                        prefs.getLong(
                                "euro",
                                Double.doubleToLongBits(0)
                        )
                );

        tomanBalance =
                Double.longBitsToDouble(
                        prefs.getLong(
                                "toman",
                                Double.doubleToLongBits(0)
                        )
                );

        liraBalance =
                Double.longBitsToDouble(
                        prefs.getLong(
                                "lira",
                                Double.doubleToLongBits(0)
                        )
                );

        rupeeBalance =
                Double.longBitsToDouble(
                        prefs.getLong(
                                "rupee",
                                Double.doubleToLongBits(0)
                        )
                );
    }

    private void saveBalances() {

        prefs.edit()
                .putLong(
                        "afghani",
                        Double.doubleToLongBits(
                                afghaniBalance
                        )
                )
                .putLong(
                        "dollar",
                        Double.doubleToLongBits(
                                dollarBalance
                        )
                )
                .putLong(
                        "euro",
                        Double.doubleToLongBits(
                                euroBalance
                        )
                )
                .putLong(
                        "toman",
                        Double.doubleToLongBits(
                                tomanBalance
                        )
                )
                .putLong(
                        "lira",
                        Double.doubleToLongBits(
                                liraBalance
                        )
                )
                .putLong(
                        "rupee",
                        Double.doubleToLongBits(
                                rupeeBalance
                        )
                )
                .apply();
    }

    private String format(double value) {

        return String.format(
                Locale.US,
                "%.2f",
                value
        );
    }
}
