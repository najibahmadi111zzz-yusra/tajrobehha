package com.tajro.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

public class VaultActivity extends Activity {

    private ExchangeData data;

    private TextView balanceText;
    private EditText amountInput;
    private EditText noteInput;
    private Spinner currencySpinner;

    private final String[] currencies = {
            ExchangeData.AFN,
            ExchangeData.USD,
            ExchangeData.EUR,
            ExchangeData.TRY,
            ExchangeData.PKR,
            ExchangeData.TOMAN
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        data = ExchangeData.get(this);

        buildScreen();
        updateBalance();
    }

    private void buildScreen() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(30, 30, 30, 30);
        root.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = new TextView(this);
        title.setText("🔐 گاوصندوق هوشمند");
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 10, 0, 25);

        root.addView(title);

        TextView info = new TextView(this);
        info.setText(
                "محل امن برای ثبت پول و دارایی‌های صرافی\n" +
                "تمام ورود و خروج‌ها ثبت می‌شود."
        );
        info.setTextSize(16);
        info.setGravity(Gravity.CENTER);
        info.setPadding(0, 0, 0, 25);

        root.addView(info);

        currencySpinner = new Spinner(this);

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        currencies
                );

        currencySpinner.setAdapter(adapter);

        root.addView(currencySpinner);

        balanceText = new TextView(this);
        balanceText.setTextSize(22);
        balanceText.setGravity(Gravity.CENTER);
        balanceText.setPadding(0, 25, 0, 25);

        root.addView(balanceText);

        amountInput = new EditText(this);
        amountInput.setHint("مبلغ");
        amountInput.setInputType(
                android.text.InputType.TYPE_CLASS_NUMBER |
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        );

        root.addView(amountInput);

        noteInput = new EditText(this);
        noteInput.setHint("یادداشت / دلیل");
        root.addView(noteInput);

        Button depositButton = new Button(this);
        depositButton.setText("➕ گذاشتن در گاوصندوق");
        root.addView(depositButton);

        Button withdrawButton = new Button(this);
        withdrawButton.setText("➖ برداشت از گاوصندوق");
        root.addView(withdrawButton);

        Button historyButton = new Button(this);
        historyButton.setText("📋 تاریخچه گاوصندوق");
        root.addView(historyButton);

        Button backButton = new Button(this);
        backButton.setText("⬅️ بازگشت");
        root.addView(backButton);

        depositButton.setOnClickListener(v -> deposit());

        withdrawButton.setOnClickListener(v -> withdraw());

        historyButton.setOnClickListener(v -> showHistory());

        backButton.setOnClickListener(v -> finish());

        setContentView(root);
    }

    private String getSelectedCurrency() {
        return currencySpinner.getSelectedItem().toString();
    }

    private double getAmount() {

        String text = amountInput.getText()
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

        data.addBalance(currency, amount);

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
                data.subtractBalance(currency, amount);

        if (!success) {

            Toast.makeText(
                    this,
                    "موجودی کافی نیست.",
                    Toast.LENGTH_SHORT
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

        String currency = getSelectedCurrency();

        double balance =
                data.getBalance(currency);

        balanceText.setText(
                "موجودی " + currency +
                "\n" +
                formatNumber(balance)
        );
    }

    private String formatNumber(double number) {

        return String.format(
                java.util.Locale.US,
                "%,.2f",
                number
        );
    }

    private void clearInputs() {

        amountInput.setText("");
        noteInput.setText("");
    }

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

            record.put("type", type);
            record.put("currency", currency);
            record.put("amount", amount);
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

                    text.append("────────────\n");

                    text.append(type)
                            .append("\n");

                    text.append("ارز: ")
                            .append(currency)
                            .append("\n");

                    text.append("مبلغ: ")
                            .append(formatNumber(amount))
                            .append("\n");

                    if (!note.isEmpty()) {

                        text.append("یادداشت: ")
                                .append(note)
                                .append("\n");
                    }
                }
            }

            new android.app.AlertDialog.Builder(this)
                    .setTitle("📋 تاریخچه گاوصندوق")
                    .setMessage(text.toString())
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
          }
