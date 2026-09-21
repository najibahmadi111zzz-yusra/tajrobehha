package com.tajro.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExchangeData {

    private static final String PREF_NAME = "tajro_exchange_data";

    private static final String KEY_TRANSACTIONS = "transactions";
    private static final String KEY_CUSTOMERS = "customers";
    private static final String KEY_CUSTODY = "customer_custody";

    private static final String BALANCE_PREFIX = "balance_";
    private static final String RATE_PREFIX = "rate_";

    // ==================================================
    // نرخ خرید و فروش جدید
    // ==================================================

    private static final String BUY_RATE_PREFIX =
            "buy_rate_";

    private static final String SELL_RATE_PREFIX =
            "sell_rate_";

    private static final String KEY_LAST_RATE_UPDATE =
            "last_shahzada_rate_update";

    private static final long RATE_UPDATE_INTERVAL =
            24L * 60L * 60L * 1000L;

    private static final String SHAHZADA_URL =
            "https://sarafi.af/fa/exchange-rates/sarai-shahzada";

    // ==================================================
    // ارزها
    // ==================================================

    public static final String AFN = "افغانی";
    public static final String USD = "دالر";
    public static final String EUR = "یورو";
    public static final String GBP = "پوند انگلیس";
    public static final String SAR = "ریال سعودی";
    public static final String AED = "درهم امارات";
    public static final String IQD = "دینار عراق";
    public static final String INR = "روپیه هند";
    public static final String PKR = "روپیه پاکستانی";
    public static final String TRY = "لیر ترکیه";
    public static final String TOMAN = "تومان";

    private static ExchangeData instance;

    private final SharedPreferences prefs;
    private final Context appContext;

    private ExchangeData(Context context) {

        appContext =
                context.getApplicationContext();

        prefs =
                appContext.getSharedPreferences(
                        PREF_NAME,
                        Context.MODE_PRIVATE
                );

        initializeDefaults();

        // بررسی به‌روزرسانی آنلاین
        updateRatesIfNeeded();
    }

    public static synchronized ExchangeData get(
            Context context
    ) {

        if (instance == null) {
            instance =
                    new ExchangeData(context);
        }

        return instance;
    }

    public static String[] getCurrencies() {

        return new String[]{
                AFN,
                USD,
                EUR,
                GBP,
                SAR,
                AED,
                IQD,
                INR,
                PKR,
                TRY,
                TOMAN
        };
    }

    public static String[] getCurrencyNames() {
        return getCurrencies();
    }

    // ==================================================
    // مقدارهای اولیه
    // ==================================================

    private void initializeDefaults() {

        initializeBalance(AFN, 0);
        initializeBalance(USD, 0);
        initializeBalance(EUR, 0);
        initializeBalance(GBP, 0);
        initializeBalance(SAR, 0);
        initializeBalance(AED, 0);
        initializeBalance(IQD, 0);
        initializeBalance(INR, 0);
        initializeBalance(PKR, 0);
        initializeBalance(TRY, 0);
        initializeBalance(TOMAN, 0);

        // نرخ‌های قبلی برنامه
        // فقط به عنوان پشتیبان

        initializeRate(USD, 70);
        initializeRate(EUR, 82);
        initializeRate(GBP, 95);
        initializeRate(SAR, 18.67);
        initializeRate(AED, 19.05);
        initializeRate(IQD, 0.054);
        initializeRate(INR, 0.84);
        initializeRate(PKR, 0.25);
        initializeRate(TRY, 2);
        initializeRate(TOMAN, 0.0015);

        // نرخ خرید و فروش اولیه
        initializeBuyRate(USD, 70);
        initializeSellRate(USD, 70);

        initializeBuyRate(EUR, 82);
        initializeSellRate(EUR, 82);

        initializeBuyRate(GBP, 95);
        initializeSellRate(GBP, 95);

        initializeBuyRate(SAR, 18.67);
        initializeSellRate(SAR, 18.67);

        initializeBuyRate(AED, 19.05);
        initializeSellRate(AED, 19.05);

        initializeBuyRate(IQD, 0.054);
        initializeSellRate(IQD, 0.054);

        initializeBuyRate(INR, 0.84);
        initializeSellRate(INR, 0.84);

        initializeBuyRate(PKR, 0.25);
        initializeSellRate(PKR, 0.25);

        initializeBuyRate(TRY, 2);
        initializeSellRate(TRY, 2);

        initializeBuyRate(TOMAN, 0.0015);
        initializeSellRate(TOMAN, 0.0015);
    }

    private void initializeBalance(
            String currency,
            double value
    ) {

        if (!prefs.contains(
                BALANCE_PREFIX + currency
        )) {

            setBalance(currency, value);
        }
    }

    private void initializeRate(
            String currency,
            double value
    ) {

        if (!prefs.contains(
                RATE_PREFIX + currency
        )) {

            setRate(currency, value);
        }
    }

    private void initializeBuyRate(
            String currency,
            double value
    ) {

        if (!prefs.contains(
                BUY_RATE_PREFIX + currency
        )) {

            setBuyRate(currency, value);
        }
    }

    private void initializeSellRate(
            String currency,
            double value
    ) {

        if (!prefs.contains(
                SELL_RATE_PREFIX + currency
        )) {

            setSellRate(currency, value);
        }
    }

    // ==================================================
    // بروزرسانی نرخ سرای شهزاده
    // ==================================================

    private void updateRatesIfNeeded() {

        long lastUpdate =
                prefs.getLong(
                        KEY_LAST_RATE_UPDATE,
                        0
                );

        long now =
                System.currentTimeMillis();

        if (lastUpdate > 0
                && now - lastUpdate
                < RATE_UPDATE_INTERVAL) {

            return;
        }

        updateRatesOnline();
    }

    private void updateRatesOnline() {

        Thread thread =
                new Thread(() -> {

                    HttpURLConnection connection =
                            null;

                    try {

                        URL url =
                                new URL(
                                        SHAHZADA_URL
                                );

                        connection =
                                (HttpURLConnection)
                                        url.openConnection();

                        connection.setRequestMethod(
                                "GET"
                        );

                        connection.setConnectTimeout(
                                15000
                        );

                        connection.setReadTimeout(
                                15000
                        );

                        connection.setRequestProperty(
                                "User-Agent",
                                "Mozilla/5.0"
                        );

                        connection.setRequestProperty(
                                "Accept",
                                "text/html,application/xhtml+xml"
                        );

                        int responseCode =
                                connection
                                        .getResponseCode();

                        if (responseCode != 200) {
                            return;
                        }

                        InputStream inputStream =
                                connection
                                        .getInputStream();

                        BufferedReader reader =
                                new BufferedReader(
                                        new InputStreamReader(
                                                inputStream,
                                                "UTF-8"
                                        )
                                );

                        StringBuilder html =
                                new StringBuilder();

                        String line;

                        while (
                                (line =
                                        reader.readLine())
                                        != null
                        ) {

                            html.append(line)
                                    .append("\n");
                        }

                        reader.close();
                        inputStream.close();

                        String page =
                                html.toString();

                        boolean updated =
                                false;

                        // دالر
                        updated |= updateShahzadaRate(
                                page,
                                "USD",
                                USD,
                                false
                        );

                        // یورو
                        updated |= updateShahzadaRate(
                                page,
                                "EUR",
                                EUR,
                                false
                        );

                        // پوند
                        updated |= updateShahzadaRate(
                                page,
                                "GBP",
                                GBP,
                                false
                        );

                        // ریال سعودی
                        updated |= updateShahzadaRate(
                                page,
                                "SAR",
                                SAR,
                                false
                        );

                        // درهم امارات
                        updated |= updateShahzadaRate(
                                page,
                                "AED",
                                AED,
                                false
                        );

                        // دینار عراق
                        updated |= updateShahzadaRate(
                                page,
                                "IQD",
                                IQD,
                                false
                        );

                        // روپیه هند
                        updated |= updateShahzadaRate(
                                page,
                                "INR",
                                INR,
                                false
                        );

                        // روپیه پاکستان
                        updated |= updateShahzadaRate(
                                page,
                                "PKR",
                                PKR,
                                true
                        );

                        // لیر ترکیه
                        updated |= updateShahzadaRate(
                                page,
                                "TRY",
                                TRY,
                                false
                        );

                        // تومان ایران
                        updated |= updateShahzadaRate(
                                page,
                                "IRR",
                                TOMAN,
                                true
                        );

                        if (updated) {

                            prefs.edit()
                                    .putLong(
                                            KEY_LAST_RATE_UPDATE,
                                            System.currentTimeMillis()
                                    )
                                    .apply();
                        }

                    } catch (Exception e) {

                        e.printStackTrace();

                    } finally {

                        if (connection != null) {
                            connection.disconnect();
                        }
                    }

                });

        thread.start();
    }

    // ==================================================
    // خواندن خرید و فروش از صفحه سرای شهزاده
    // ==================================================

    private boolean updateShahzadaRate(
            String html,
            String code,
            String appCurrency,
            boolean thousandUnit
    ) {

        try {

            /*
             * تگ‌های HTML را حذف می‌کنیم
             * تا جدول سایت به متن ساده تبدیل شود.
             */

            String text =
                    html.replaceAll(
                            "<[^>]*>",
                            " "
                    );

            text =
                    text.replace(
                            "&nbsp;",
                            " "
                    );

            text =
                    text.replace(
                            "&#160;",
                            " "
                    );

            text =
                    text.replaceAll(
                            "\\s+",
                            " "
                    );

            /*
             * دنبال ردیف ارز می‌گردیم.
             *
             * مثال:
             *
             * USD - دالر آمریکا 64.55 64.60
             */

            Pattern pattern =
                    Pattern.compile(
                            code
                                    + "\\s*-.*?"
                                    + "(\\d+(?:[.,]\\d+)?)"
                                    + "\\s+"
                                    + "(\\d+(?:[.,]\\d+)?)",
                            Pattern.CASE_INSENSITIVE
                    );

            Matcher matcher =
                    pattern.matcher(text);

            if (!matcher.find()) {
                return false;
            }

            double buy =
                    parseNumber(
                            matcher.group(1)
                    );

            double sell =
                    parseNumber(
                            matcher.group(2)
                    );

            if (buy <= 0 || sell <= 0) {
                return false;
            }

            /*
             * در سایت سرای شهزاده:
             *
             * تومان و روپیه پاکستان
             * با واحد "هزار" نمایش داده می‌شوند.
             *
             * مثلاً:
             * PKR = 226
             *
             * یعنی:
             * 0.226 افغانی برای یک روپیه
             *
             * و:
             * IRR = 0.28
             *
             * یعنی:
             * 0.00028 افغانی برای یک تومان
             *
             * برای اینکه با ساختار فعلی برنامه
             * سازگار باشیم، تبدیل می‌کنیم.
             */

            if (thousandUnit
                    && PKR.equals(appCurrency)) {

                buy = buy / 1000.0;
                sell = sell / 1000.0;
            }

            if (thousandUnit
                    && TOMAN.equals(appCurrency)) {

                buy = buy / 1000.0;
                sell = sell / 1000.0;
            }

            setBuyRate(
                    appCurrency,
                    buy
            );

            setSellRate(
                    appCurrency,
                    sell
            );

            /*
             * برای جلوگیری از خراب شدن بخش‌های
             * قبلی برنامه، rate فعلی را برابر
             * نرخ فروش قرار می‌دهیم.
             */

            setRate(
                    appCurrency,
                    sell
            );

            return true;

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }

    private double parseNumber(
            String value
    ) {

        if (value == null) {
            return 0;
        }

        try {

            String clean =
                    value.trim()
                            .replace(",", "");

            return Double.parseDouble(
                    clean
            );

        } catch (Exception e) {

            return 0;
        }
    }

    // ==================================================
    // نرخ خرید
    // ==================================================

    public double getBuyRate(
            String currency
    ) {

        return prefs.getFloat(
                BUY_RATE_PREFIX + currency,
                (float) getRate(currency)
        );
    }

    public void setBuyRate(
            String currency,
            double rate
    ) {

        if (currency == null || rate < 0) {
            return;
        }

        prefs.edit()
                .putFloat(
                        BUY_RATE_PREFIX + currency,
                        (float) rate
                )
                .apply();
    }

    // ==================================================
    // نرخ فروش
    // ==================================================

    public double getSellRate(
            String currency
    ) {

        return prefs.getFloat(
                SELL_RATE_PREFIX + currency,
                (float) getRate(currency)
        );
    }

    public void setSellRate(
            String currency,
            double rate
    ) {

        if (currency == null || rate < 0) {
            return;
        }

        prefs.edit()
                .putFloat(
                        SELL_RATE_PREFIX + currency,
                        (float) rate
                )
                .apply();
    }

    // ==================================================
    // آخرین بروزرسانی
    // ==================================================

    public long getLastRateUpdateTime() {

        return prefs.getLong(
                KEY_LAST_RATE_UPDATE,
                0
        );
    }

    public boolean hasOnlineRateUpdate() {

        return getLastRateUpdateTime() > 0;
    }

    public void forceUpdateRates() {

        updateRatesOnline();
    }

    // ==================================================
    // موجودی
    // ==================================================

    public double getBalance(
            String currency
    ) {

        return prefs.getFloat(
                BALANCE_PREFIX + currency,
                0
        );
    }

    public void setBalance(
            String currency,
            double amount
    ) {

        if (currency == null || amount < 0) {
            return;
        }

        prefs.edit()
                .putFloat(
                        BALANCE_PREFIX + currency,
                        (float) amount
                )
                .apply();
    }

    public void addBalance(
            String currency,
            double amount
    ) {

        if (currency == null
                || amount <= 0) {

            return;
        }

        setBalance(
                currency,
                getBalance(currency)
                        + amount
        );
    }

    public boolean subtractBalance(
            String currency,
            double amount
    ) {

        if (currency == null
                || amount <= 0) {

            return false;
        }

        double current =
                getBalance(currency);

        if (amount > current) {
            return false;
        }

        setBalance(
                currency,
                current - amount
        );

        return true;
    }

    // ==================================================
    // نرخ قدیمی - دست‌نخورده
    // ==================================================

    public double getRate(
            String currency
    ) {

        return prefs.getFloat(
                RATE_PREFIX + currency,
                0
        );
    }

    public void setRate(
            String currency,
            double rate
    ) {

        if (currency == null
                || rate < 0) {

            return;
        }

        prefs.edit()
                .putFloat(
                        RATE_PREFIX + currency,
                        (float) rate
                )
                .apply();
    }

    // ==================================================
    // مشتری
    // ==================================================

    public void saveCustomer(
            String name,
            String phone
    ) {

        if (name == null
                || name.trim().isEmpty()) {

            return;
        }

        try {

            JSONArray customers =
                    getCustomersArray();

            for (int i = 0;
                 i < customers.length();
                 i++) {

                JSONObject old =
                        customers.getJSONObject(i);

                if (old.optString("name")
                        .equals(name)
                        && old.optString("phone")
                        .equals(phone)) {

                    return;
                }
            }

            JSONObject customer =
                    new JSONObject();

            customer.put(
                    "name",
                    name
            );

            customer.put(
                    "phone",
                    phone == null
                            ? ""
                            : phone
            );

            customers.put(customer);

            prefs.edit()
                    .putString(
                            KEY_CUSTOMERS,
                            customers.toString()
                    )
                    .apply();

        } catch (JSONException e) {

            e.printStackTrace();
        }
    }

    public JSONArray getCustomersArray() {

        String data =
                prefs.getString(
                        KEY_CUSTOMERS,
                        "[]"
                );

        try {

            return new JSONArray(data);

        } catch (JSONException e) {

            return new JSONArray();
        }
    }

    // ==================================================
    // معامله
    // ==================================================

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

        if (amount <= 0
                || rate <= 0
                || currency == null
                || type == null) {

            return false;
        }

        double total =
                amount * rate;

        try {

            saveCustomer(
                    customerName,
                    phone
            );

            JSONObject transaction =
                    new JSONObject();

            long id =
                    System.currentTimeMillis();

            transaction.put(
                    "id",
                    id
            );

            transaction.put(
                    "customerName",
                    customerName == null
                            ? ""
                            : customerName
            );

            transaction.put(
                    "phone",
                    phone == null
                            ? ""
                            : phone
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
                    "accountStatus",
                    accountStatus == null
                            ? "نقدی"
                            : accountStatus
            );

            transaction.put(
                    "note",
                    note == null
                            ? ""
                            : note
            );

            transaction.put(
                    "date",
                    System.currentTimeMillis()
            );

            JSONArray transactions =
                    getTransactionsArray();

            transactions.put(
                    transaction
            );

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

    public JSONArray getTransactionsArray() {

        String data =
                prefs.getString(
                        KEY_TRANSACTIONS,
                        "[]"
                );

        try {

            return new JSONArray(data);

        } catch (JSONException e) {

            return new JSONArray();
        }
    }

    // ==================================================
    // امانت مشتریان
    // ==================================================

    public boolean saveCustody(
            String customerName,
            String phone,
            String currency,
            double amount,
            String note
    ) {

        if (customerName == null
                || customerName.trim().isEmpty()
                || currency == null
                || amount <= 0) {

            return false;
        }

        try {

            JSONArray records =
                    getCustodyArray();

            JSONObject record =
                    new JSONObject();

            record.put(
                    "id",
                    System.currentTimeMillis()
            );

            record.put(
                    "customerName",
                    customerName.trim()
            );

            record.put(
                    "phone",
                    phone == null
                            ? ""
                            : phone.trim()
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
                    note == null
                            ? ""
                            : note.trim()
            );

            record.put(
                    "status",
                    "امانت نزد صرافی"
            );

            record.put(
                    "date",
                    System.currentTimeMillis()
            );

            records.put(record);

            prefs.edit()
                    .putString(
                            KEY_CUSTODY,
                            records.toString()
                    )
                    .apply();

            return true;

        } catch (JSONException e) {

            e.printStackTrace();

            return false;
        }
    }

    public JSONArray getCustodyArray() {

        String data =
                prefs.getString(
                        KEY_CUSTODY,
                        "[]"
                );

        try {

            return new JSONArray(data);

        } catch (JSONException e) {

            return new JSONArray();
        }
    }

    public boolean returnCustody(
            long id
    ) {

        try {

            JSONArray records =
                    getCustodyArray();

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

                    record.put(
                            "status",
                            "تحویل داده شد"
                    );

                    record.put(
                            "returnDate",
                            System.currentTimeMillis()
                    );

                    prefs.edit()
                            .putString(
                                    KEY_CUSTODY,
                                    records.toString()
                            )
                            .apply();

                    return true;
                }
            }

        } catch (JSONException e) {

            e.printStackTrace();
        }

        return false;
    }

    // ==================================================
    // مشتریان
    // ==================================================

    public List<String> getCustomerNames() {

        Set<String> uniqueNames =
                new HashSet<>();

        JSONArray customers =
                getCustomersArray();

        for (int i = 0;
             i < customers.length();
             i++) {

            JSONObject customer =
                    customers.optJSONObject(i);

            if (customer != null) {

                String name =
                        customer.optString(
                                "name"
                        );

                if (!name.isEmpty()) {

                    uniqueNames.add(name);
                }
            }
        }

        return new ArrayList<>(
                uniqueNames
        );
    }

    public double getCustomerDebt(
            String customerName
    ) {

        double debt = 0;

        JSONArray transactions =
                getTransactionsArray();

        for (int i = 0;
             i < transactions.length();
             i++) {

            JSONObject transaction =
                    transactions.optJSONObject(i);

            if (transaction == null) {
                continue;
            }

            if (!transaction.optString(
                    "customerName"
            ).equals(customerName)) {

                continue;
            }

            if ("بدهکار".equals(
                    transaction.optString(
                            "accountStatus"
                    )
            )) {

                debt += transaction.optDouble(
                        "total",
                        0
                );
            }
        }

        return debt;
    }

    public double getCustomerCredit(
            String customerName
    ) {

        double credit = 0;

        JSONArray transactions =
                getTransactionsArray();

        for (int i = 0;
             i < transactions.length();
             i++) {

            JSONObject transaction =
                    transactions.optJSONObject(i);

            if (transaction == null) {
                continue;
            }

            if (!transaction.optString(
                    "customerName"
            ).equals(customerName)) {

                continue;
            }

            if ("طلبکار".equals(
                    transaction.optString(
                            "accountStatus"
                    )
            )) {

                credit += transaction.optDouble(
                        "total",
                        0
                );
            }
        }

        return credit;
    }

    public double getCustomerBalance(
            String customerName
    ) {

        return getCustomerDebt(
                customerName
        ) - getCustomerCredit(
                customerName
        );
    }

    // ==================================================
    // آمار
    // ==================================================

    public double getTotalBuy() {

        return getTotalByType("خرید");
    }

    public double getTotalSell() {

        return getTotalByType("فروش");
    }

    private double getTotalByType(
            String type
    ) {

        double total = 0;

        JSONArray transactions =
                getTransactionsArray();

        for (int i = 0;
             i < transactions.length();
             i++) {

            JSONObject transaction =
                    transactions.optJSONObject(i);

            if (transaction == null) {
                continue;
            }

            if (type.equals(
                    transaction.optString(
                            "type"
                    )
            )) {

                total += transaction.optDouble(
                        "total",
                        0
                );
            }
        }

        return total;
    }

    public int getTransactionCount() {

        return getTransactionsArray()
                .length();
    }

    public void clearTransactions() {

        prefs.edit()
                .remove(KEY_TRANSACTIONS)
                .apply();
    }

    public void clearAllData() {

        prefs.edit()
                .clear()
                .apply();

        initializeDefaults();
    }
            }
