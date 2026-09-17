package com.tajro.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.media.*;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.io.*;
import java.net.*;
import java.util.*;

public class ChatActivity extends Activity {

    private static final int MIC = 1001, PICK = 1002;

    private static final String SUPABASE_URL =
            "https://gorbhuqmkjlkrklhasdh.supabase.co";

    private static final String SUPABASE_PUBLISHABLE_KEY =
            "sb_publishable_a02sM3MABB4afGU90ZBdFA_OTYG6gUs";

    private static final String MEDIA_BUCKET = "chat_media";
    private static final String VOICE_BUCKET = "voice_messages";

    private LinearLayout messagesLayout;
    private EditText input;
    private ScrollView scroll;
    private Button voiceButton;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private MediaRecorder recorder;
    private MediaPlayer player;
    private String audioPath;
    private boolean recording;

    private int dp(int n) {
        return (int)(n * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        createScreen();

        if (auth.getCurrentUser() == null) {
            auth.signInAnonymously()
                    .addOnSuccessListener(x -> loadMessages())
                    .addOnFailureListener(e ->
                            toast("خطا در اتصال به حساب کاربری"));
        } else {
            loadMessages();
        }
    }

    private void createScreen() {
        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setPadding(dp(10), dp(18), dp(10), dp(8));
        main.setBackgroundColor(Color.rgb(235,248,250));

        TextView title = new TextView(this);
        title.setText("💬 چت تجربه‌ها");
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        main.addView(title, new LinearLayout.LayoutParams(
                -1, dp(55)));

        scroll = new ScrollView(this);
        messagesLayout = new LinearLayout(this);
        messagesLayout.setOrientation(LinearLayout.VERTICAL);
        messagesLayout.setPadding(dp(5),dp(5),dp(5),dp(5));
        scroll.addView(messagesLayout);

        main.addView(scroll, new LinearLayout.LayoutParams(
                -1, 0, 1));

        LinearLayout bottom = new LinearLayout(this);
        bottom.setGravity(Gravity.CENTER_VERTICAL);

        input = new EditText(this);
        input.setHint("پیام خود را بنویسید...");
        input.setTextSize(16);
        input.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        input.setSingleLine(false);

        bottom.addView(input, new LinearLayout.LayoutParams(
                0, dp(55), 1));

        Button send = new Button(this);
        send.setText("📤");
        bottom.addView(send, new LinearLayout.LayoutParams(
                dp(55),dp(55)));

        Button media = new Button(this);
        media.setText("📷");
        bottom.addView(media, new LinearLayout.LayoutParams(
                dp(55),dp(55)));

        voiceButton = new Button(this);
        voiceButton.setText("🎤");
        bottom.addView(voiceButton, new LinearLayout.LayoutParams(
                dp(55),dp(55)));

        main.addView(bottom);
        setContentView(main);

        send.setOnClickListener(v -> sendText());
        media.setOnClickListener(v -> chooseMedia());
        voiceButton.setOnClickListener(v -> toggleRecording());

        input.setOnClickListener(v -> {
            input.requestFocus();
            InputMethodManager im =
                    (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
            if (im != null)
                im.showSoftInput(input,InputMethodManager.SHOW_IMPLICIT);
        });
    }

    private void sendText() {
        String text = input.getText().toString().trim();

        if (text.isEmpty()) {
            toast("لطفاً پیام بنویسید");
            return;
        }

        Map<String,Object> m = new HashMap<>();
        m.put("message",text);
        m.put("type","text");
        m.put("senderId",auth.getCurrentUser().getUid());
        m.put("timestamp",FieldValue.serverTimestamp());
        m.put("deletedForAll",false);
        m.put("deletedFor",new ArrayList<String>());

        db.collection("messages").add(m)
                .addOnSuccessListener(x -> {
                    input.setText("");
                    scrollBottom();
                })
                .addOnFailureListener(e -> toast("ارسال پیام ناموفق بود"));
    }

    private void chooseMedia() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES,
                new String[]{"image/*","video/*"});
        startActivityForResult(i,PICK);
    }

    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode,resultCode,data);

        if (requestCode != PICK || resultCode != RESULT_OK ||
                data == null || data.getData() == null) return;

        Uri uri = data.getData();
        String mime = getContentResolver().getType(uri);

        if (mime == null) {
            toast("نوع فایل مشخص نیست");
            return;
        }

        if (mime.startsWith("image/"))
            uploadMedia(uri,"image",mime);
        else if (mime.startsWith("video/"))
            uploadMedia(uri,"video",mime);
        else
            toast("فقط عکس یا ویدئو انتخاب کنید");
    }

    private void uploadMedia(Uri uri,String type,String mime) {
        toast(type.equals("image") ?
                "📷 در حال ارسال عکس..." :
                "🎥 در حال ارسال ویدئو...");

        new Thread(() -> {
            try {
                String ext = type.equals("image") ? ".jpg" : ".mp4";
                String file = type+"_"+System.currentTimeMillis()+ext;
                String path = "chat/"+auth.getCurrentUser().getUid()+"/"+file;

                String url = SUPABASE_URL+
                        "/storage/v1/object/"+MEDIA_BUCKET+"/"+path;

                HttpURLConnection c = (HttpURLConnection)
                        new URL(url).openConnection();

                c.setRequestMethod("POST");
                c.setDoOutput(true);
                c.setRequestProperty("apikey",SUPABASE_PUBLISHABLE_KEY);
                c.setRequestProperty("Authorization",
                        "Bearer "+SUPABASE_PUBLISHABLE_KEY);
                c.setRequestProperty("Content-Type",mime);
                c.setRequestProperty("x-upsert","false");

                InputStream in = getContentResolver().openInputStream(uri);
                OutputStream out = c.getOutputStream();

                byte[] buf = new byte[8192];
                int n;
                while ((n=in.read(buf))!=-1) out.write(buf,0,n);

                out.close();
                in.close();

                int code = c.getResponseCode();

                if (code >= 200 && code < 300) {
                    String publicUrl = SUPABASE_URL+
                            "/storage/v1/object/public/"+
                            MEDIA_BUCKET+"/"+path;

                    runOnUiThread(() -> saveMedia(publicUrl,type));
                } else {
                    String err = read(c.getErrorStream());
                    runOnUiThread(() ->
                            toast("خطای Supabase: "+code+"\n"+err));
                }

                c.disconnect();

            } catch(Exception e) {
                runOnUiThread(() ->
                        toast("آپلود ناموفق بود:\n"+e.getMessage()));
            }
        }).start();
    }

    private void saveMedia(String url,String type) {
        Map<String,Object> m = new HashMap<>();
        m.put("type",type);
        m.put("mediaUrl",url);
        m.put("senderId",auth.getCurrentUser().getUid());
        m.put("timestamp",FieldValue.serverTimestamp());
        m.put("deletedForAll",false);
        m.put("deletedFor",new ArrayList<String>());

        db.collection("messages").add(m)
                .addOnSuccessListener(x -> {
                    toast(type.equals("image") ?
                            "📷 عکس ارسال شد" : "🎥 ویدئو ارسال شد");
                    scrollBottom();
                })
                .addOnFailureListener(e -> toast("ذخیره پیام ناموفق بود"));
    }

    private void toggleRecording() {
        if (recording) stopRecording();
        else startRecording();
    }

    private void startRecording() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},MIC);
            return;
        }

        try {
            File dir = getExternalCacheDir();
            if (dir == null) dir = getCacheDir();

            audioPath = new File(dir,
                    "voice_"+System.currentTimeMillis()+".3gp")
                    .getAbsolutePath();

            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.setOutputFile(audioPath);
            recorder.prepare();
            recorder.start();

            recording = true;
            voiceButton.setText("⏹️");
            toast("🎙️ در حال ضبط...");

        } catch(Exception e) {
            releaseRecorder();
            toast("شروع ضبط ناموفق بود");
        }
    }

    private void stopRecording() {
        if (recorder == null) return;

        try {
            recorder.stop();
            recorder.release();
            recorder = null;
            recording = false;
            voiceButton.setText("🎤");
            toast("📤 در حال ارسال صدا...");
            uploadAudio();
        } catch(Exception e) {
            releaseRecorder();
            recording = false;
            voiceButton.setText("🎤");
            toast("ضبط صدا ناموفق بود");
        }
    }

    private void releaseRecorder() {
        try {
            if (recorder != null) recorder.release();
        } catch(Exception ignored) {}
        recorder = null;
    }

    private void uploadAudio() {
        File file = new File(audioPath);

        if (!file.exists()) {
            toast("فایل صوتی پیدا نشد");
            return;
        }

        new Thread(() -> {
            try {
                String name = "voice_"+System.currentTimeMillis()+".3gp";
                String path = "chat/"+auth.getCurrentUser().getUid()+"/"+name;

                String url = SUPABASE_URL+
                        "/storage/v1/object/"+VOICE_BUCKET+"/"+path;

                HttpURLConnection c = (HttpURLConnection)
                        new URL(url).openConnection();

                c.setRequestMethod("POST");
                c.setDoOutput(true);
                c.setRequestProperty("apikey",SUPABASE_PUBLISHABLE_KEY);
                c.setRequestProperty("Authorization",
                        "Bearer "+SUPABASE_PUBLISHABLE_KEY);
                c.setRequestProperty("Content-Type","audio/3gpp");
                c.setRequestProperty("x-upsert","false");

                InputStream in = new FileInputStream(file);
                OutputStream out = c.getOutputStream();

                byte[] buf = new byte[8192];
                int n;
                while ((n=in.read(buf))!=-1) out.write(buf,0,n);

                out.close();
                in.close();

                int code = c.getResponseCode();

                if (code >= 200 && code < 300) {
                    String publicUrl = SUPABASE_URL+
                            "/storage/v1/object/public/"+
                            VOICE_BUCKET+"/"+path;

                    runOnUiThread(() -> saveAudio(publicUrl));
                } else {
                    String err = read(c.getErrorStream());
                    runOnUiThread(() ->
                            toast("خطای Supabase صدا: "+code+"\n"+err));
                }

                c.disconnect();

            } catch(Exception e) {
                runOnUiThread(() ->
                        toast("آپلود صدا ناموفق بود:\n"+e.getMessage()));
            }
        }).start();
    }

    private void saveAudio(String url) {
        Map<String,Object> m = new HashMap<>();
        m.put("type","audio");
        m.put("audioUrl",url);
        m.put("senderId",auth.getCurrentUser().getUid());
        m.put("timestamp",FieldValue.serverTimestamp());
        m.put("deletedForAll",false);
        m.put("deletedFor",new ArrayList<String>());

        db.collection("messages").add(m)
                .addOnSuccessListener(x -> {
                    toast("🎤 پیام صوتی ارسال شد");
                    scrollBottom();
                })
                .addOnFailureListener(e -> toast("ذخیره صدا ناموفق بود"));
    }

    private void loadMessages() {
        db.collection("messages")
                .orderBy("timestamp",Query.Direction.ASCENDING)
                .addSnapshotListener((snap,error) -> {

                    if (error != null) {
                        toast("خطا در دریافت پیام‌ها");
                        return;
                    }

                    messagesLayout.removeAllViews();

                    if (snap == null) return;

                    String myId = auth.getCurrentUser().getUid();

                    for (DocumentSnapshot d : snap.getDocuments()) {

                        if (Boolean.TRUE.equals(
                                d.getBoolean("deletedForAll")))
                            continue;

                        Object obj = d.get("deletedFor");
                        if (obj instanceof List &&
                                ((List<?>)obj).contains(myId))
                            continue;

                        String type = d.getString("type");
                        String sender = d.getString("senderId");
                        String id = d.getId();
                        boolean mine = myId.equals(sender);

                        if ("audio".equals(type)) {
                            String u=d.getString("audioUrl");
                            if(u!=null) addAudio(u,mine,id,sender);

                        } else if ("image".equals(type)) {
                            String u=d.getString("mediaUrl");
                            if(u!=null) addImage(u,mine,id,sender);

                        } else if ("video".equals(type)) {
                            String u=d.getString("mediaUrl");
                            if(u!=null) addVideo(u,mine,id,sender);

                        } else {
                            String text=d.getString("message");
                            if(text!=null) addText(text,mine,id,sender);
                        }
                    }

                    scrollBottom();
                });
    }

    private Button deleteButton(String id,String sender,String url,String type) {
        Button b = new Button(this);
        b.setText("🗑️");
        b.setTextSize(13);
        b.setPadding(0,0,0,0);
        b.setOnClickListener(v -> showDeleteMenu(id,sender,url,type));
        return b;
    }

    private View.OnLongClickListener longClick(
            String id,String sender,String url,String type) {
        return v -> {
            showDeleteMenu(id,sender,url,type);
            return true;
        };
    }

    private void addText(String text,boolean mine,String id,String sender) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView tv = new TextView(this);
        tv.setText(mine ? "👤 شما:\n"+text : "👤 کاربر:\n"+text);
        tv.setTextSize(17);
        tv.setGravity(Gravity.RIGHT);
        tv.setPadding(dp(12),dp(10),dp(12),dp(10));

        row.addView(tv,new LinearLayout.LayoutParams(0,-2,1));
        row.addView(deleteButton(id,sender,null,"text"),
                new LinearLayout.LayoutParams(dp(52),dp(52)));

        View.OnLongClickListener l=longClick(id,sender,null,"text");
        tv.setOnLongClickListener(l);
        row.setOnLongClickListener(l);

        messagesLayout.addView(row);
    }

    private void addAudio(String url,boolean mine,String id,String sender) {
        LinearLayout row=new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);

        Button play=new Button(this);
        play.setText(mine ?
                "👤 شما  ▶️ پیام صوتی" :
                "👤 کاربر  ▶️ پیام صوتی");
        play.setTextSize(15);
        play.setOnClickListener(v -> playAudio(url));

        row.addView(play,new LinearLayout.LayoutParams(0,dp(60),1));
        row.addView(deleteButton(id,sender,url,"audio"),
                new LinearLayout.LayoutParams(dp(52),dp(60)));

        View.OnLongClickListener l=longClick(id,sender,url,"audio");
        play.setOnLongClickListener(l);
        row.setOnLongClickListener(l);

        messagesLayout.addView(row);
    }

    private void addImage(String url,boolean mine,String id,String sender) {
        LinearLayout row=new LinearLayout(this);
        row.setGravity(Gravity.TOP);

        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.RIGHT);

        TextView label=new TextView(this);
        label.setText(mine ? "👤 شما  📷 عکس" : "👤 کاربر  📷 عکس");

        ImageView image=new ImageView(this);
        image.setAdjustViewBounds(true);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setLayoutParams(new LinearLayout.LayoutParams(dp(260),dp(260)));
        image.setOnClickListener(v -> openUrl(url));

        box.addView(label);
        box.addView(image);

        row.addView(box,new LinearLayout.LayoutParams(0,-2,1));
        row.addView(deleteButton(id,sender,url,"image"),
                new LinearLayout.LayoutParams(dp(52),dp(52)));

        View.OnLongClickListener l=longClick(id,sender,url,"image");
        label.setOnLongClickListener(l);
        image.setOnLongClickListener(l);
        row.setOnLongClickListener(l);

        messagesLayout.addView(row);

        new Thread(() -> {
            try {
                HttpURLConnection c=(HttpURLConnection)
                        new URL(url).openConnection();
                c.connect();
                InputStream in=c.getInputStream();
                Bitmap bm=BitmapFactory.decodeStream(in);
                in.close();
                c.disconnect();

                runOnUiThread(() -> {
                    if(bm!=null) image.setImageBitmap(bm);
                });
            } catch(Exception ignored) {}
        }).start();
    }

    private void addVideo(String url,boolean mine,String id,String sender) {
        LinearLayout row=new LinearLayout(this);
        row.setGravity(Gravity.TOP);

        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.RIGHT);

        TextView label=new TextView(this);
        label.setText(mine ? "👤 شما  🎥 ویدئو" : "👤 کاربر  🎥 ویدئو");

        VideoView video=new VideoView(this);
        video.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(230)));
        video.setVideoURI(Uri.parse(url));

        Button play=new Button(this);
        play.setText("▶️ پخش ویدئو");
        play.setOnClickListener(v -> video.start());

        box.addView(label);
        box.addView(video);
        box.addView(play);

        row.addView(box,new LinearLayout.LayoutParams(0,-2,1));
        row.addView(deleteButton(id,sender,url,"video"),
                new LinearLayout.LayoutParams(dp(52),dp(52)));

        View.OnLongClickListener l=longClick(id,sender,url,"video");
        label.setOnLongClickListener(l);
        video.setOnLongClickListener(l);
        row.setOnLongClickListener(l);

        messagesLayout.addView(row);
    }

    private void showDeleteMenu(
            String id,String sender,String url,String type) {

        if(auth.getCurrentUser()==null) return;

        boolean mine=auth.getCurrentUser().getUid().equals(sender);

        String[] options=mine ?
                new String[]{"🗑️ حذف برای من",
                        "🗑️ حذف برای هر دو طرف","لغو"} :
                new String[]{"🗑️ حذف برای من","لغو"};

        new AlertDialog.Builder(this)
                .setTitle("حذف پیام")
                .setItems(options,(d,which) -> {

                    if(which==0) {
                        deleteForMe(id);

                    } else if(which==1 && mine) {

                        new AlertDialog.Builder(this)
                                .setTitle("حذف برای هر دو طرف؟")
                                .setMessage("این پیام از چت هر دو طرف حذف می‌شود.")
                                .setNegativeButton("لغو",null)
                                .setPositiveButton("حذف",
                                        (x,w)->deleteForBoth(id,url,type))
                                .show();
                    }
                }).show();
    }

    private void deleteForMe(String id) {
        String uid=auth.getCurrentUser().getUid();

        db.collection("messages").document(id)
                .update("deletedFor",FieldValue.arrayUnion(uid))
                .addOnSuccessListener(x -> toast("پیام برای شما حذف شد"))
                .addOnFailureListener(e -> toast("حذف پیام ناموفق بود"));
    }

    private void deleteForBoth(String id,String url,String type) {
        db.collection("messages").document(id)
                .update("deletedForAll",true)
                .addOnSuccessListener(x -> {
                    toast("پیام برای هر دو طرف حذف شد");

                    if(url!=null && !"text".equals(type))
                        deleteSupabaseFile(url,type);
                })
                .addOnFailureListener(e -> toast("حذف پیام ناموفق بود"));
    }

    private void deleteSupabaseFile(String publicUrl,String type) {
        new Thread(() -> {
            try {
                String bucket="audio".equals(type) ?
                        VOICE_BUCKET : MEDIA_BUCKET;

                String marker="/storage/v1/object/public/"+
                        bucket+"/";

                int i=publicUrl.indexOf(marker);
                if(i<0) return;

                String path=Uri.decode(
                        publicUrl.substring(i+marker.length()));

                String url=SUPABASE_URL+
                        "/storage/v1/object/"+bucket+"/"+path;

                HttpURLConnection c=(HttpURLConnection)
                        new URL(url).openConnection();

                c.setRequestMethod("DELETE");
                c.setRequestProperty("apikey",SUPABASE_PUBLISHABLE_KEY);
                c.setRequestProperty("Authorization",
                        "Bearer "+SUPABASE_PUBLISHABLE_KEY);

                c.getResponseCode();
                c.disconnect();

            } catch(Exception ignored) {}
        }).start();
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));
        } catch(Exception e) {
            toast("باز کردن فایل ناموفق بود");
        }
    }

    private void playAudio(String url) {
        try {
            if(player!=null) {
                player.release();
                player=null;
            }

            player=new MediaPlayer();
            player.setDataSource(url);
            player.setOnPreparedListener(MediaPlayer::start);
            player.setOnCompletionListener(p -> {
                p.release();
                player=null;
            });
            player.prepareAsync();

        } catch(Exception e) {
            toast("پخش صدا ناموفق بود");
        }
    }

    private String read(InputStream in) {
        if(in==null) return "جزئیات خطا دریافت نشد";

        try {
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            byte[] b=new byte[1024];
            int n;

            while((n=in.read(b))!=-1)
                out.write(b,0,n);

            in.close();
            return out.toString("UTF-8");

        } catch(Exception e) {
            return "خطا";
        }
    }

    private void scrollBottom() {
        if(scroll!=null)
            scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    private void toast(String s) {
        runOnUiThread(() ->
                Toast.makeText(this,s,Toast.LENGTH_LONG).show());
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,String[] permissions,int[] results) {

        super.onRequestPermissionsResult(
                requestCode,permissions,results);

        if(requestCode==MIC) {
            if(results.length>0 &&
                    results[0]==PackageManager.PERMISSION_GRANTED)
                startRecording();
            else
                toast("اجازه استفاده از میکروفون داده نشد");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        releaseRecorder();

        if(player!=null) {
            try { player.release(); }
            catch(Exception ignored) {}
            player=null;
        }
    }
}
