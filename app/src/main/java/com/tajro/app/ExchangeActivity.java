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
        transactionTitle.setPadding(0, 30, 0, 
