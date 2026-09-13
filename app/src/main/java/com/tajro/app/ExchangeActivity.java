package com.tajro.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ExchangeActivity extends Activity {

    private Spinner currencySpinner;
    private Spinner typeSpinner;

    private EditText amountInput;
    private EditText rateInput;

    private TextView totalText;
    private TextView receiptText;
    private TextView balanceText;

    private double afghaniBalance = 100000;
    private double dollarBalance = 0;
    private double euroBalance = 0;
    private double tomanBalance = 0;
    private double liraBalance = 0;
    private double rupeeBalance = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createInterface();
    }

    private void createInterface() {

        ScrollView scrollView = new ScrollView(this);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 30, 30, 30);

        TextView title = new TextView(this);
        title.setText("💱 صرافی");
        title.setTextSize(30);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 30);

        layout.addView(title);

        TextView ratesTitle = new TextView(this);
        ratesTitle.setText("📊 نرخ ارز");
        ratesTitle.setTextSize(22);
        ratesTitle.setTypeface(null, Typeface.BOLD);

        layout.addView(ratesTitle);

        TextView rates = new TextView(this);
        rates.setText(
                "🇺🇸 دلار: 70 افغانی\n" +
                "🇪🇺 یورو: 82 افغانی\n" +
                "🇹🇷 لیره: 2 افغانی\n" +
                "🇵🇰 کلدار: 0.25 افغانی\n" +
                "🇮🇷 تومان: 0.0015 افغانی"
        );
        rates.setTextSize(18);
        rates.setPadding(0, 15, 0, 25);

        layout.addView(rates);

        Button updateRatesButton = new Button(this);
        updateRatesButton.setText("🔄 به‌روزرسانی نرخ روز");

        updateRatesButton.setOnClickListener(v ->
                Toast.makeText(
                        this,
                        "فعلاً نرخ‌ها نمونه هستند؛ اتصال نرخ آنلاین در مرحله بعد اضافه می‌شود",
                        Toast.LENGTH_LONG
                )
        );

        layout.addView(updateRatesButton);

        TextView transactionTitle = new TextView(this);
        transactionTitle.setText("🧾 ثبت معامله");
        transactionTitle.setTextSize(22);
        transactionTitle.setTypeface(null, Typeface.BOLD);
        transactionTitle.setPadding(0, 30, 0, 15);

        layout.addView(transactionTitle);

        currencySpinner = new Spinner(this);

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
                        android.R.layout.simple_spinner_dropdown_item,
                        currencies
                );

        currencySpinner.setAdapter(currencyAdapter);

        layout.addView(currencySpinner);

        typeSpinner = new Spinner(this);

        String[] types = {
                "خرید",
                "فروش"
        };

        ArrayAdapter<String> typeAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        types
                );

        typeSpinner.setAdapter(typeAdapter);

        layout.addView(typeSpinner);

        amountInput = new EditText(this);
        amountInput.setHint("مقدار ارز");
        amountInput.setInputType(2);

        layout.addView(amountInput);

        rateInput = new EditText(this);
        rateInput.setHint("نرخ هر واحد به افغانی");
        rateInput.setInputType(2);

        layout.addView(rateInput);

        Button calculateButton = new Button(this);
        calculateButton.setText("🧮 محاسبه");

        layout.addView(calculateButton);

        totalText = new TextView(this);
        totalText.setText("مبلغ کل: 0 افغانی");
        totalText.setTextSize(20);
        totalText.setTypeface(null, Typeface.BOLD);
        totalText.setPadding(0, 20, 0, 20);

        layout.addView(totalText);

        Button saveButton = new Button(this);
        saveButton.setText("✅ ثبت معامله");

        layout.addView(saveButton);

        balanceText = new TextView(this);
        balanceText.setTextSize(19);
        balanceText.setPadding(0, 30, 0, 20);

        layout.addView(balanceText);

        updateBalanceText();

        receiptText = new TextView(this);
        receiptText.setTextSize(18);
        receiptText.setPadding(0, 20, 0, 30);

        layout.addView(receiptText);

        calculateButton.setOnClickListener(v ->
                calculateTotal()
        );

        saveButton.setOnClickListener(v ->
                saveTransaction()
        );

        scrollView.addView(layout);

        setContentView(scrollView);
    }

    private void calculateTotal() {

        try {

            double amount =
                    Double.parseDouble(
                            amountInput.getText().toString()
                    );

            double rate =
                    Double.parseDouble(
                            rateInput.getText().toString()
                    );

            double total = amount * rate;

            totalText.setText(
                    "مبلغ کل: " +
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

            double amount =
                    Double.parseDouble(
                            amountInput.getText().toString()
                    );

            double rate =
                    Double.parseDouble(
                            rateInput.getText().toString()
                    );

            double total = amount * rate;

            String currency =
                    currencySpinner
                            .getSelectedItem()
                            .toString();

            String type =
                    typeSpinner
                            .getSelectedItem()
                            .toString();

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

            String date =
                    new SimpleDateFormat(
                            "yyyy/MM/dd HH:mm",
                            Locale.getDefault()
                    ).format(new Date());

            String receipt =
                    "━━━━━━━━━━━━━━\n" +
                    "🧾 رسید معامله\n" +
                    "━━━━━━━━━━━━━━\n" +
                    "شماره رسید: " +
                    System.currentTimeMillis() +
                    "\n\n" +
                    "نوع معامله: " +
                    type +
                    "\n" +
                    "ارز: " +
                    currency +
                    "\n" +
                    "مقدار: " +
                    format(amount) +
                    "\n" +
                    "نرخ: " +
                    format(rate) +
                    " افغانی\n" +
                    "مبلغ کل: " +
                    format(total) +
                    " افغانی\n" +
                    "تاریخ: " +
                    date +
                    "\n" +
                    "━━━━━━━━━━━━━━";

            receiptText.setText(receipt);

            updateBalanceText();

            amountInput.setText("");
            rateInput.setText("");

            Toast.makeText(
                    this,
                    "معامله با موفقیت ثبت شد",
                    Toast.LENGTH_LONG
            ).show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "لطفاً مقدار و نرخ را درست وارد کنید",
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

        String balance =
                "💰 موجودی صرافی\n\n" +
                "افغانی: " +
                format(afghaniBalance) +
                "\n" +
                "دلار: " +
                format(dollarBalance) +
                "\n" +
                "یورو: " +
                format(euroBalance) +
                "\n" +
                "تومان: " +
                format(tomanBalance) +
                "\n" +
                "لیره: " +
                format(liraBalance) +
                "\n" +
                "کلدار: " +
                format(rupeeBalance);

        balanceText.setText(balance);
    }

    private String format(double value) {

        return String.format(
                Locale.US,
                "%.2f",
                value
        );
    }
}
