package com.tajro.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExchangeData {

    private static final String PREF_NAME = "tajro_exchange_data";

    private static final String KEY_TRANSACTIONS = "transactions";
    private static final String KEY_CUSTOMERS = "customers";

    private static final String BALANCE_PREFIX = "balance_";
    private static final String RATE_PREFIX = "rate_";

    // ارزهای اصلی
    public static final String AFN = "افغانی";
    public static final String USD = "دالر";
    public static final String EUR = "یورو";
    public static final String TRY = "لیر";
    public static final String PKR = "روپیه پاکستانی";
    public static final String TOMAN = "تومان";

    private static ExchangeData instance;

    private final SharedPreferences prefs;

    private ExchangeData(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        initializeDefaults();
    }

    public static synchronized ExchangeData get(Context context) {
        if (instance == null) {
            instance = new ExchangeData(context);
        }
        return instance;
    }

    // --------------------------------------------------
    // اطلاعات اولیه
    // --------------------------------------------------

    private void initializeDefaults() {

        if (!prefs.contains(BALANCE_PREFIX + AFN)) {
            setBalance(AFN, 100000);
        }

        if (!prefs.contains(BALANCE_PREFIX + USD)) {
            setBalance(USD, 0);
        }

        if (!prefs.contains(BALANCE_PREFIX + EUR)) {
            setBalance(EUR, 0);
        }

        if (!prefs.contains(BALANCE_PREFIX + TRY)) {
            setBalance(TRY, 0);
        }

        if (!prefs.contains(BALANCE_PREFIX + PKR)) {
            setBalance(PKR, 0);
        }

        if (!prefs.contains(BALANCE_PREFIX + TOMAN)) {
            setBalance(TOMAN, 0);
        }

        // نرخ‌های نمونه اولیه
        if (!prefs.contains(RATE_PREFIX + USD)) {
            setRate(USD, 70);
        }

        if (!prefs.contains(RATE_PREFIX + EUR)) {
            setRate(EUR, 82);
        }

        if (!prefs.contains(RATE_PREFIX + TRY)) {
            setRate(TRY, 2);
        }

        if (!prefs.contains(RATE_PREFIX + PKR)) {
            setRate(PKR, 0.25);
        }

        if (!prefs.contains(RATE_PREFIX + TOMAN)) {
            setRate(TOMAN, 0.0015);
        }
    }

    // --------------------------------------------------
    // موجودی ارزها
    // --------------------------------------------------

    public double getBalance(String currency) {
        return prefs.getFloat(
                BALANCE_PREFIX + currency,
                0
        );
    }

    public void setBalance(String currency, double amount) {
        prefs.edit()
                .putFloat(BALANCE_PREFIX + currency, (float) amount)
                .apply();
    }

    public void addBalance(String currency, double amount) {
        double current = getBalance(currency);
        setBalance(currency, current + amount);
    }

    public boolean subtractBalance(String currency, double amount) {

        double current = getBalance(currency);

        if (amount > current) {
            return false;
        }

        setBalance(currency, current - amount);
        return true;
    }

    // --------------------------------------------------
    // نرخ ارز
    // --------------------------------------------------

    public double getRate(String currency) {

        return prefs.getFloat(
                RATE_PREFIX + currency,
                0
        );
    }

    public void setRate(String currency, double rate) {

        prefs.edit()
                .putFloat(RATE_PREFIX + currency, (float) rate)
                .apply();
    }

    // --------------------------------------------------
    // ثبت مشتری
    // --------------------------------------------------

    public void saveCustomer(String name, String phone) {

        if (name == null || name.trim().isEmpty()) {
            return;
        }

        try {

            JSONArray customers = getCustomersArray();

            for (int i = 0; i < customers.length(); i++) {

                JSONObject old = customers.getJSONObject(i);

                if (old.optString("name").equals(name)
                        && old.optString("phone").equals(phone)) {
                    return;
                }
            }

            JSONObject customer = new JSONObject();

            customer.put("name", name);
            customer.put("phone", phone);

            customers.put(customer);

            prefs.edit()
                    .putString(KEY_CUSTOMERS, customers.toString())
                    .apply();

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public JSONArray getCustomersArray() {

        String data = prefs.getString(KEY_CUSTOMERS, "[]");

        try {
            return new JSONArray(data);
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    // --------------------------------------------------
    // ثبت معامله
    // --------------------------------------------------

    public boolean saveTransaction(
            String customerName,
            String phone,
            String currency,
            String type,
            double amount,
            double rate,
            String accountStatus,
            String note
    ) {

        if (amount <= 0) {
            return false;
        }

        double total = amount * rate;

        try {

            // ذخیره مشتری
            saveCustomer(customerName, phone);

            // اطلاعات معامله
            JSONObject transaction = new JSONObject();

            transaction.put("id", System.currentTimeMillis());

            transaction.put(
                    "customerName",
                    customerName == null ? "" : customerName
            );

            transaction.put(
                    "phone",
                    phone == null ? "" : phone
            );

            transaction.put("currency", currency);
            transaction.put("type", type);
            transaction.put("amount", amount);
            transaction.put("rate", rate);
            transaction.put("total", total);

            transaction.put(
                    "accountStatus",
                    accountStatus == null ? "نقدی" : accountStatus
            );

            transaction.put(
                    "note",
                    note == null ? "" : note
            );

            transaction.put(
                    "date",
                    System.currentTimeMillis()
            );

            JSONArray transactions = getTransactionsArray();

            transactions.put(transaction);

            prefs.edit()
                    .putString(
                            KEY_TRANSACTIONS,
                            transactions.toString()
                    )
                    .apply();

            return true;

        } catch (JSONException e) {

            e.printStackTrace();
            return false;
        }
    }

    // --------------------------------------------------
    // تمام معاملات
    // --------------------------------------------------

    public JSONArray getTransactionsArray() {

        String data = prefs.getString(
                KEY_TRANSACTIONS,
                "[]"
        );

        try {
            return new JSONArray(data);
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    // --------------------------------------------------
    // لیست نام مشتریان
    // --------------------------------------------------

    public List<String> getCustomerNames() {

        Set<String> uniqueNames = new HashSet<>();

        JSONArray customers = getCustomersArray();

        for (int i = 0; i < customers.length(); i++) {

            JSONObject customer = customers.optJSONObject(i);

            if (customer != null) {

                String name = customer.optString("name");

                if (!name.isEmpty()) {
                    uniqueNames.add(name);
                }
            }
        }

        return new ArrayList<>(uniqueNames);
    }

    // --------------------------------------------------
    // حساب یک مشتری
    // --------------------------------------------------

    public double getCustomerDebt(String customerName) {

        double debt = 0;

        JSONArray transactions = getTransactionsArray();

        for (int i = 0; i < transactions.length(); i++) {

            JSONObject transaction =
                    transactions.optJSONObject(i);

            if (transaction == null) {
                continue;
            }

            if (!transaction.optString("customerName")
                    .equals(customerName)) {
                continue;
            }

            if ("بدهکار".equals(
                    transaction.optString("accountStatus"))) {

                debt += transaction.optDouble("total", 0);
            }
        }

        return debt;
    }

    public double getCustomerCredit(String customerName) {

        double credit = 0;

        JSONArray transactions = getTransactionsArray();

        for (int i = 0; i < transactions.length(); i++) {

            JSONObject transaction =
                    transactions.optJSONObject(i);

            if (transaction == null) {
                continue;
            }

            if (!transaction.optString("customerName")
                    .equals(customerName)) {
                continue;
            }

            if ("طلبکار".equals(
                    transaction.optString("accountStatus"))) {

                credit += transaction.optDouble("total", 0);
            }
        }

        return credit;
    }

    public double getCustomerBalance(String customerName) {

        return getCustomerDebt(customerName)
                - getCustomerCredit(customerName);
    }

    // --------------------------------------------------
    // آمار معاملات
    // --------------------------------------------------

    public double getTotalBuy() {

        return getTotalByType("خرید");
    }

    public double getTotalSell() {

        return getTotalByType("فروش");
    }

    private double getTotalByType(String type) {

        double total = 0;

        JSONArray transactions = getTransactionsArray();

        for (int i = 0; i < transactions.length(); i++) {

            JSONObject transaction =
                    transactions.optJSONObject(i);

            if (transaction == null) {
                continue;
            }

            if (type.equals(
                    transaction.optString("type"))) {

                total += transaction.optDouble(
                        "total",
                        0
                );
            }
        }

        return total;
    }

    public int getTransactionCount() {

        return getTransactionsArray().length();
    }

    // --------------------------------------------------
    // پاک کردن تمام معاملات
    // --------------------------------------------------

    public void clearTransactions() {

        prefs.edit()
                .remove(KEY_TRANSACTIONS)
                .apply();
    }

    // --------------------------------------------------
    // پاک کردن تمام اطلاعات صرافی
    // --------------------------------------------------

    public void clearAllData() {

        prefs.edit()
                .clear()
                .apply();

        initializeDefaults();
    }
          }
