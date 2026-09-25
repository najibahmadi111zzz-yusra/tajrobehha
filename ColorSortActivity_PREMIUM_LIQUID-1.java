package com.tajro.app;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ColorSortActivity extends Activity {

    private SortGameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        gameView = new SortGameView();
        setContentView(gameView);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (gameView != null) {
            gameView.stopAnimation();
            gameView.releaseAudio();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameView != null) gameView.startAnimation();
    }

    class SortGameView extends View {

        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Handler handler = new Handler();
        private final Random random = new Random();

        // --- Premium visual/audio state ---
        private final SharedPreferences progressPrefs =
                ColorSortActivity.this.getSharedPreferences("color_sort_progress", MODE_PRIVATE);
        private AudioTrack activeAudioTrack;
        private Thread soundThread;
        private boolean soundEnabled = true;
        private float liquidWave = 0f;
        private float bottleGlow = 0f;
        private long victoryStart = 0L;
        private final Random sparkleRandom = new Random(7319);

        private final List<List<Integer>> tubes = new ArrayList<>();
        private final List<Move> history = new ArrayList<>();

        private int selectedTube = -1;
        private int level = 1;
        private int moves = 0;
        private int coins = 0;
        private int totalStars = 0;
        private int lastEarnedCoins = 0;

        private boolean levelFinished = false;
        private boolean running = true;
        private long animStart = 0L;

        private int animFrom = -1;
        private int animTo = -1;
        private int animColor = -1;
        private int animAmount = 0;
        private float animProgress = 1f;

        private boolean showingHint = false;
        private int hintFrom = -1;
        private int hintTo = -1;
        private long hintUntil = 0L;

        private final int MAX_LEVEL = 30;
        private final int CAPACITY = 4;

        private final int[] colors = {
                Color.rgb(255, 53, 69),
                Color.rgb(38, 111, 255),
                Color.rgb(41, 220, 78),
                Color.rgb(255, 193, 25),
                Color.rgb(164, 65, 245),
                Color.rgb(18, 194, 229),
                Color.rgb(255, 80, 180),
                Color.rgb(255, 132, 26),
                Color.rgb(126, 73, 245)
        };

        private final Runnable frame = new Runnable() {
            @Override public void run() {
                if (!running) return;
                liquidWave += 0.13f;
                bottleGlow += 0.035f;
                invalidate();
                handler.postDelayed(this, 16);
            }
        };

        SortGameView() {
            super(ColorSortActivity.this);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            p.setAntiAlias(true);
            stroke.setAntiAlias(true);
            text.setAntiAlias(true);
            setFocusable(true);
            soundEnabled = progressPrefs.getBoolean("sound", true);
            level = Math.max(1, Math.min(MAX_LEVEL, progressPrefs.getInt("level", 1)));
            coins = Math.max(0, progressPrefs.getInt("coins", 0));
            totalStars = Math.max(0, progressPrefs.getInt("stars", 0));
            
            resetLevel();
        }

        void startAnimation() {
            running = true;
            
            handler.removeCallbacks(frame);
            handler.post(frame);
        }

        void stopAnimation() {
            running = false;
            handler.removeCallbacks(frame);
            stopActiveSound();
            saveProgress();
        }

        private void playGameSound(final int kind) {
            if (!soundEnabled) return;
            stopActiveSound();
            soundThread = new Thread(new Runnable() {
                @Override public void run() {
                    final int sr = 44100;
                    final int ms = kind == 2 ? 560 : (kind == 3 ? 760 : 110);
                    final int total = sr * ms / 1000;
                    int min = AudioTrack.getMinBufferSize(sr, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
                    if (min < 2048) min = 2048;
                    AudioTrack track = null;
                    try {
                        track = new AudioTrack(AudioManager.STREAM_MUSIC, sr, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(min,4096), AudioTrack.MODE_STREAM);
                        activeAudioTrack = track; track.play();
                        byte[] b = new byte[4096];
                        java.util.Random rnd = new java.util.Random(7701L + kind);
                        float filtered=0f, a=0f, d=0f; int pos=0;
                        while(pos<total && soundEnabled && activeAudioTrack==track){
                            int frames=Math.min(b.length/2,total-pos);
                            for(int i=0;i<frames;i++){
                                float q=(pos+i)/(float)Math.max(1,total-1);
                                float env=(float)Math.sin(Math.PI*q);
                                float v;
                                if(kind==2){
                                    float raw=rnd.nextFloat()*2f-1f; filtered+=(raw-filtered)*.075f;
                                    a+=(float)(2*Math.PI*175/sr); d+=(float)(2*Math.PI*365/sr);
                                    v=(filtered*.78f+(float)Math.sin(a)*.16f+(float)Math.sin(d)*.055f)*env*.78f;
                                } else if(kind==3){
                                    a+=(float)(2*Math.PI*520/sr); d+=(float)(2*Math.PI*780/sr);
                                    v=((float)Math.sin(a)*.25f+(float)Math.sin(d)*.10f)*env;
                                } else {
                                    a+=(float)(2*Math.PI*680/sr); v=(float)Math.sin(a)*env*.22f;
                                }
                                int pcm=Math.max(-32767,Math.min(32767,(int)(v*15000f)));
                                b[i*2]=(byte)(pcm&255); b[i*2+1]=(byte)((pcm>>8)&255);
                            }
                            track.write(b,0,frames*2); pos+=frames;
                        }
                        try{track.stop();}catch(Exception ignored){}
                    }catch(Exception ignored){}
                    finally{ if(track!=null)try{track.release();}catch(Exception ignored){} if(activeAudioTrack==track)activeAudioTrack=null; }
                }
            });
            soundThread.start();
        }

        private void stopActiveSound(){
            AudioTrack t=activeAudioTrack; activeAudioTrack=null;
            if(t!=null){ try{t.pause();}catch(Exception ignored){} try{t.flush();}catch(Exception ignored){} try{t.release();}catch(Exception ignored){} }
        }

        private void saveProgress() {
            progressPrefs.edit()
                    .putInt("level", level)
                    .putInt("coins", coins)
                    .putInt("stars", totalStars)
                    .putBoolean("sound", soundEnabled)
                    .apply();
        }

        void releaseAudio() {
            stopActiveSound();
        }

        void resetLevel() {
            selectedTube = -1;
            moves = 0;
            levelFinished = false;
            showingHint = false;
            animFrom = -1;
            animTo = -1;
            animProgress = 1f;
            victoryStart = 0L;
            tubes.clear();
            history.clear();

            int count = getColorCount();
            for (int c = 0; c < count; c++) {
                List<Integer> t = new ArrayList<>();
                for (int i = 0; i < CAPACITY; i++) t.add(c);
                tubes.add(t);
            }

            int empty = level < 10 ? 2 : (level < 20 ? 2 : 3);
            for (int i = 0; i < empty; i++) tubes.add(new ArrayList<Integer>());

            shufflePuzzle(getShuffleCount());
            invalidate();
        }

        private int getColorCount() {
            if (level <= 3) return 3;
            if (level <= 6) return 4;
            if (level <= 10) return 5;
            if (level <= 15) return 6;
            if (level <= 20) return 7;
            if (level <= 25) return 8;
            return 9;
        }

        private int getShuffleCount() {
            if (level <= 3) return 12 + level * 3;
            if (level <= 10) return 25 + level * 4;
            if (level <= 20) return 55 + level * 4;
            return 90 + level * 5;
        }

        private void shufflePuzzle(int count) {
            int previousFrom = -1, previousTo = -1;
            for (int step = 0; step < count; step++) {
                List<Integer> froms = new ArrayList<>();
                for (int i = 0; i < tubes.size(); i++) {
                    if (!tubes.get(i).isEmpty()) froms.add(i);
                }
                Collections.shuffle(froms, random);
                boolean moved = false;

                for (int from : froms) {
                    if (from == previousTo) continue;
                    List<Integer> source = tubes.get(from);
                    int sourceColor = source.get(source.size() - 1);
                    int same = getTopSameCount(source);

                    List<Integer> targets = new ArrayList<>();
                    for (int to = 0; to < tubes.size(); to++) {
                        if (to == from || to == previousFrom) continue;
                        List<Integer> target = tubes.get(to);
                        if (target.size() >= CAPACITY) continue;
                        if (target.isEmpty() ||
                                target.get(target.size() - 1) != sourceColor) {
                            targets.add(to);
                        }
                    }
                    if (targets.isEmpty()) continue;

                    Collections.shuffle(targets, random);
                    int to = targets.get(0);
                    List<Integer> target = tubes.get(to);
                    int free = CAPACITY - target.size();
                    int amount = Math.min(same, Math.min(free, 1 + random.nextInt(2)));

                    for (int n = 0; n < amount; n++) {
                        target.add(source.remove(source.size() - 1));
                    }
                    previousFrom = from;
                    previousTo = to;
                    moved = true;
                    break;
                }
                if (!moved) break;
            }

            if (isLevelComplete() && count < 120) shufflePuzzle(count + 15);
        }

        private int getTopSameCount(List<Integer> tube) {
            if (tube.isEmpty()) return 0;
            int c = tube.get(tube.size() - 1);
            int n = 0;
            for (int i = tube.size() - 1; i >= 0; i--) {
                if (tube.get(i) == c) n++;
                else break;
            }
            return n;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth();
            float h = getHeight();

            drawBackground(canvas, w, h);
            drawHeader(canvas, w);
            drawShelvesAndTubes(canvas, w, h);
            drawControls(canvas, w, h);
            drawFooter(canvas, w, h);

            if (showingHint && System.currentTimeMillis() < hintUntil) {
                drawHint(canvas);
            } else {
                showingHint = false;
            }

            if (levelFinished) drawVictory(canvas, w, h);

            if (animProgress < 1f) {
                long elapsed = SystemClock.uptimeMillis() - animStart;
                animProgress = Math.min(1f, elapsed / 560f);
                if (animProgress >= 1f) finishAnimatedMove();
                invalidate();
            }
        }

        private void drawBackground(Canvas c, float w, float h) {
            LinearGradient g = new LinearGradient(
                    0, 0, 0, h,
                    Color.rgb(12, 31, 150),
                    Color.rgb(19, 7, 88),
                    Shader.TileMode.CLAMP
            );
            p.setShader(g);
            c.drawRect(0, 0, w, h, p);
            p.setShader(null);

            RadialGradient glow = new RadialGradient(
                    w * .5f, h * .38f, w * .8f,
                    Color.argb(80, 32, 112, 255),
                    Color.argb(0, 20, 25, 120),
                    Shader.TileMode.CLAMP
            );
            p.setShader(glow);
            c.drawCircle(w * .5f, h * .38f, w * .8f, p);
            p.setShader(null);

            p.setColor(Color.argb(70, 50, 180, 255));
            for (int i = 0; i < 12; i++) {
                float x = (i * 97 + 43) % w;
                float y = 95 + ((i * 131) % Math.max(1, (int) h));
                c.drawCircle(x, y, 2 + (i % 3), p);
            }
        }

        private void drawHeader(Canvas c, float w) {
            float y = 12;
            drawRoundPanel(c, 16, y, 120, 78, Color.rgb(0, 166, 255), Color.rgb(48, 78, 255));
            text.setTypeface(Typeface.DEFAULT_BOLD);
            text.setTextAlign(Paint.Align.CENTER);
            text.setTextSize(38);
            text.setColor(Color.WHITE);
            c.drawText("⌂", 68, 60, text);

            drawRoundPanel(c, 132, y, 315, 78, Color.rgb(111, 26, 238), Color.rgb(190, 37, 255));
            text.setTextSize(18);
            text.setColor(Color.WHITE);
            c.drawText("★  مرحله " + level, 224, 59, text);

            drawRoundPanel(c, 327, y, w - 145, 78, Color.rgb(30, 20, 130), Color.rgb(117, 39, 250));
            drawStar(c, 360, 43, 20, Color.rgb(255, 208, 25));
            drawStar(c, 410, 43, 20, Color.rgb(255, 208, 25));
            drawStar(c, 460, 43, 20, Color.rgb(255, 208, 25));
            p.setColor(Color.rgb(255, 207, 25));
            c.drawRoundRect(new RectF(350, 61, Math.min(w - 180, 475), 68), 5, 5, p);

            float coinLeft = w - 132;
            drawRoundPanel(c, coinLeft, y, w - 12, 78, Color.rgb(9, 84, 235), Color.rgb(35, 205, 255));
            drawCoin(c, coinLeft + 27, 45);
            text.setTextSize(17);
            text.setColor(Color.WHITE);
            text.setTextAlign(Paint.Align.LEFT);
            c.drawText(String.valueOf(coins), coinLeft + 49, 52, text);
            text.setTextSize(27);
            c.drawText("+", w - 28, 55, text);
        }

        private void drawShelvesAndTubes(Canvas c, float w, float h) {
            int count = tubes.size();
            int columns = count <= 6 ? 3 : 5;
            int rows = (int) Math.ceil(count / (float) columns);

            float top = 116;
            float bottomControls = 275;
            float areaH = Math.max(300, h - top - bottomControls);
            float rowH = areaH / Math.max(1, rows);
            float tubeW = Math.min(92, w / (columns + .9f));
            float tubeH = Math.min(230, rowH * .76f);

            for (int row = 0; row < rows; row++) {
                float shelfY = top + row * rowH + rowH * .83f;
                drawShelf(c, 20, shelfY, w - 20, 22);

                for (int col = 0; col < columns; col++) {
                    int idx = row * columns + col;
                    if (idx >= count) break;
                    float cx = w * (col + 1f) / (columns + 1f);
                    float cy = top + row * rowH + rowH * .42f;
                    drawTube(c, idx, cx, cy, tubeW, tubeH);
                }
            }
        }

        private void drawShelf(Canvas c, float left, float y, float right, float height) {
            p.setStyle(Paint.Style.FILL);
            p.setShadowLayer(12, 0, 8, Color.argb(160, 0, 0, 0));
            p.setShader(new LinearGradient(0, y, 0, y + height,
                    Color.rgb(255, 184, 76), Color.rgb(130, 59, 20), Shader.TileMode.CLAMP));
            c.drawRoundRect(new RectF(left, y, right, y + height), 12, 12, p);
            p.setShader(null);
            p.clearShadowLayer();
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(2);
            stroke.setColor(Color.argb(180, 255, 225, 130));
            c.drawRoundRect(new RectF(left, y, right, y + height), 12, 12, stroke);
        }

        private void drawTube(Canvas c, int index, float cx, float cy, float tw, float th) {
            float left = cx - tw / 2f;
            float right = cx + tw / 2f;
            float top = cy - th / 2f;
            float bottom = cy + th / 2f;
            boolean selected = index == selectedTube;
            boolean pouring = animProgress < 1f && index == animFrom;
            boolean receiving = animProgress < 1f && index == animTo;
            List<Integer> tube = tubes.get(index);

            // A subtle glow makes the selected/pouring bottle feel alive.
            if (selected || pouring || isTubeComplete(tube)) {
                int glowColor = isTubeComplete(tube)
                        ? Color.rgb(255, 213, 70)
                        : Color.rgb(50, 225, 255);
                p.setStyle(Paint.Style.FILL);
                p.setColor(Color.argb(selected ? 80 : 42, Color.red(glowColor),
                        Color.green(glowColor), Color.blue(glowColor)));
                p.setShadowLayer(selected ? 28 : 18, 0, 4, glowColor);
                c.drawRoundRect(new RectF(left - 8, top - 8, right + 8, bottom + 8), 28, 28, p);
                p.clearShadowLayer();
            }

            float angle = 0f;
            if (pouring) {
                float direction = getTubeGeometry(animTo)[0] >= cx ? 1f : -1f;
                float wave = (float) Math.sin(animProgress * Math.PI);
                angle = direction * 17f * wave;
            }

            c.save();
            c.rotate(angle, cx, bottom - 18);

            // Outer glass shadow.
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.argb(65, 0, 0, 0));
            p.setShadowLayer(16, 0, 10, Color.argb(180, 0, 0, 0));
            c.drawRoundRect(new RectF(left - 2, top + 13, right + 2, bottom + 2), 25, 25, p);
            p.clearShadowLayer();

            float neckTop = top - 1;
            float neckBottom = top + 28;
            float neckL = left + 11;
            float neckR = right - 11;

            android.graphics.Path bottlePath = createBottlePath(left,right,top+12,bottom,neckL,neckR,neckTop,neckBottom);
            android.graphics.Path innerBottlePath = createBottlePath(left+4,right-4,top+18,bottom-5,neckL+2,neckR-2,neckTop+5,neckBottom-2);

            p.setShader(new LinearGradient(left, top, right, bottom,
                    Color.argb(135,255,255,255), Color.argb(18,78,165,255), Shader.TileMode.CLAMP));
            c.drawPath(bottlePath,p); p.setShader(null);

            p.setShader(new LinearGradient(left, top, right, bottom,
                    Color.argb(32,190,235,255), Color.argb(58,20,85,180), Shader.TileMode.CLAMP));
            c.drawPath(innerBottlePath,p); p.setShader(null);

            // Neck and mouth of the bottle.
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.argb(52, 255, 255, 255));
            c.drawRoundRect(new RectF(neckL, neckTop + 5, neckR, neckBottom), 10, 10, p);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(2.6f);
            stroke.setColor(Color.argb(235, 224, 250, 255));
            c.drawRoundRect(new RectF(neckL, neckTop, neckR, neckBottom), 10, 10, stroke);

            // Liquid is clipped to the inside of the glass and changes level during the pour.
            float innerL = left + 8, innerR = right - 8, innerB = bottom - 8;
            float layerH = (bottom - top - 30) / CAPACITY;
            float visibleUnits = pouring ? Math.max(0f, tube.size() - animAmount * animProgress) : tube.size();

            c.save();
            c.clipPath(innerBottlePath);
            for (int j = 0; j < tube.size(); j++) {
                float visible = Math.max(0f, Math.min(1f, visibleUnits - j));
                if (visible <= 0f) continue;
                float lb = innerB - j * layerH;
                float lt = lb - layerH * visible;
                drawLiquid(c, innerL, lt, innerR, lb, tube.get(j), Math.abs((j+1)-visibleUnits)<.06f);
            }
            if (receiving && animColor >= 0) {
                float incoming = animAmount * animProgress;
                float lb = innerB - tube.size() * layerH;
                float lt = lb - incoming * layerH;
                if (incoming > 0f) drawLiquid(c,innerL,lt,innerR,lb,animColor,true);
            }
            c.restore();

            // The pouring stream is drawn from the tilted mouth toward the target mouth.
            if (pouring) drawPourStream(c, cx, top + 16, animTo, animColor, animProgress, angle);

            // Front glass reflections: broad stripe + thin bright line.
            p.setShader(new LinearGradient(left + 7, 0, left + 28, 0,
                    Color.argb(145, 255, 255, 255), Color.argb(0, 255, 255, 255),
                    Shader.TileMode.CLAMP));
            c.drawRoundRect(new RectF(left + 7, top + 22, left + 27, bottom - 14), 9, 9, p);
            p.setShader(null);

            p.setColor(Color.argb(125, 255, 255, 255));
            c.drawRoundRect(new RectF(left + 13, top + 16, right - 18, top + 20), 3, 3, p);

            // Outer glass outline and reinforced base.
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(selected ? 4.2f : 2.7f);
            stroke.setColor(selected
                    ? Color.rgb(83, 244, 255)
                    : Color.argb(238, 225, 250, 255));
            c.drawPath(bottlePath, stroke);
            stroke.setStrokeWidth(2.2f);
            c.drawArc(new RectF(left + 4, bottom - 19, right - 4, bottom + 5),
                    0, 180, false, stroke);

            // Mouth rim and a second inner rim for a real glass look.
            stroke.setStrokeWidth(3f);
            c.drawRoundRect(new RectF(left + 3, top, right - 3, top + 27), 11, 11, stroke);
            stroke.setStrokeWidth(1.4f);
            stroke.setColor(Color.argb(180, 255, 255, 255));
            c.drawRoundRect(new RectF(left + 7, top + 5, right - 7, top + 18), 7, 7, stroke);

            c.restore();

            // Cork is outside the rotating glass, so it remains seated on the mouth.
            if (isTubeComplete(tube)) drawCap(c, cx, top + 1, tw);
            drawBadge(c, cx, bottom + 11, String.valueOf(index + 1), selected);
        }

        private void drawPourStream(Canvas c,float sx,float sy,int targetIndex,int colorIndex,float progress,float angle){
            if(targetIndex<0||colorIndex<0||colorIndex>=colors.length)return;
            float[] t=getTubeGeometry(targetIndex); float tx=t[0],ty=t[1]-t[3]/2f+36f;
            float eased=progress*progress*(3f-2f*progress); float dir=tx>=sx?1f:-1f;
            float startX=sx+(float)Math.sin(Math.toRadians(angle))*14f,startY=sy+8f;
            float endX=tx,endY=ty+Math.min(28f,eased*34f);
            float bx=(startX+endX)/2f+dir*10f,by=(startY+endY)/2f+13f;
            int base=colors[colorIndex], light=lighten(base,1.43f), dark=darken(base,.50f);
            android.graphics.Path path=new android.graphics.Path();
            path.moveTo(startX-4.5f,startY); path.cubicTo(startX-2,startY+18,bx-7,by,endX-3.5f,endY);
            path.lineTo(endX+3.5f,endY); path.cubicTo(bx+7,by,startX+2,startY+18,startX+4.5f,startY); path.close();
            p.setShader(new LinearGradient(startX,0,endX,0,light,dark,Shader.TileMode.CLAMP)); p.setShadowLayer(10,0,2,base); c.drawPath(path,p); p.clearShadowLayer(); p.setShader(null);
            p.setColor(Color.argb(145,255,255,255));
            c.drawRoundRect(new RectF(startX-1.2f,startY+4,endX+1.2f,endY-2),2,2,p);
            int drops=3+(int)(progress*4f);
            for(int i=0;i<drops;i++){ float q=(i+1f)/(drops+1f); float dx=startX+(endX-startX)*q; float dy=startY+(endY-startY)*q+(float)Math.sin(liquidWave*2+i)*2.2f; p.setColor(Color.argb(190,Color.red(light),Color.green(light),Color.blue(light))); c.drawCircle(dx,dy,2f+.7f*(float)Math.sin(liquidWave+i),p); }
            float splash=(float)Math.sin(progress*Math.PI); if(splash>.05f){ stroke.setStyle(Paint.Style.STROKE); stroke.setStrokeWidth(1.8f); stroke.setColor(Color.argb((int)(180*splash),255,255,255)); float r=7f+10f*splash; c.drawOval(new RectF(endX-r,endY-3,endX+r,endY+3),stroke); }
        }

        private android.graphics.Path createBottlePath(float left,float right,float bodyTop,float bottom,float neckL,float neckR,float neckTop,float neckBottom){
            android.graphics.Path path=new android.graphics.Path();
            float shoulder=bodyTop+24f;
            path.moveTo(neckL,neckTop);
            path.lineTo(neckL,neckBottom);
            path.cubicTo(neckL,shoulder-4,left+3,shoulder-1,left+2,shoulder+18);
            path.lineTo(left+2,bottom-28);
            path.cubicTo(left+2,bottom-9,left+13,bottom,right-25,bottom);
            path.lineTo(right-25,bottom);
            path.cubicTo(right-13,bottom,right-2,bottom-9,right-2,bottom-28);
            path.lineTo(right-2,shoulder+18);
            path.cubicTo(right-3,shoulder-1,neckR,shoulder-4,neckR,neckBottom);
            path.lineTo(neckR,neckTop);
            path.close();
            return path;
        }

        private float[] getTubeGeometry(int idx) {
            int count = tubes.size();
            int columns = count <= 6 ? 3 : 5;
            int rows = (int)Math.ceil(count / (float)columns);
            float w = getWidth(), h = getHeight();
            float top = 116, bottomControls = 275;
            float areaH = Math.max(300, h - top - bottomControls);
            float rowH = areaH / Math.max(1, rows);
            float tubeW = Math.min(92, w / (columns + .9f));
            float tubeH = Math.min(230, rowH * .76f);
            int row = idx / columns, col = idx % columns;
            float cx = w * (col + 1f) / (columns + 1f);
            float cy = top + row * rowH + rowH * .42f;
            return new float[]{cx, cy, tubeW, tubeH};
        }

        private void drawLiquid(Canvas c,float left,float top,float right,float bottom,int colorIndex,boolean topLayer){
            if(colorIndex<0||colorIndex>=colors.length||bottom<=top)return;
            int base=colors[colorIndex], light=lighten(base,1.40f), dark=darken(base,.46f);
            float r=Math.min(9f,Math.max(3f,(bottom-top)*.22f));
            p.setStyle(Paint.Style.FILL); p.setShader(new LinearGradient(0,top,0,bottom,light,dark,Shader.TileMode.CLAMP));
            c.drawRoundRect(new RectF(left,top,right,bottom),r,r,p); p.setShader(null);
            p.setShader(new LinearGradient(left,0,right,0,Color.argb(105,255,255,255),Color.argb(0,255,255,255),Shader.TileMode.CLAMP));
            c.drawRoundRect(new RectF(left+2,top+2,right-2,bottom-2),r,r,p); p.setShader(null);
            p.setShader(new LinearGradient(0,bottom-14,0,bottom,Color.argb(0,Color.red(dark),Color.green(dark),Color.blue(dark)),Color.argb(100,Color.red(dark),Color.green(dark),Color.blue(dark)),Shader.TileMode.CLAMP));
            c.drawRoundRect(new RectF(left+2,bottom-15,right-2,bottom),r,r,p); p.setShader(null);
            p.setColor(Color.argb(92,255,255,255)); c.drawRoundRect(new RectF(left+4,top+2,left+12,bottom-3),4,4,p);
            if(topLayer) drawLiquidSurface(c,left,top,right,colorIndex);
        }

        private void drawLiquidSurface(Canvas c,float left,float y,float right,int colorIndex){
            if(colorIndex<0||colorIndex>=colors.length)return; int base=colors[colorIndex],light=lighten(base,1.46f);
            float wave=(float)Math.sin(liquidWave*1.35f)*2f; android.graphics.Path path=new android.graphics.Path(); path.moveTo(left,y+wave);
            for(int i=1;i<=14;i++){float q=i/14f; float x=left+(right-left)*q; float yy=y+wave*(float)Math.sin(q*Math.PI*2f+liquidWave); path.lineTo(x,yy);}
            path.lineTo(right,y+12); path.lineTo(left,y+12); path.close();
            p.setShader(new LinearGradient(0,y,0,y+13,light,base,Shader.TileMode.CLAMP)); c.drawPath(path,p); p.setShader(null);
            p.setColor(Color.argb(155,255,255,255)); c.drawOval(new RectF(left+5,y-1+wave,right-5,y+8+wave),p);
        }

        private boolean isTubeComplete(List<Integer> t) {
            if (t.size() != CAPACITY) return false;
            int c = t.get(0);
            for (int i = 1; i < t.size(); i++) if (t.get(i) != c) return false;
            return true;
        }

        private void drawCap(Canvas c, float cx, float y, float tw) {
            float capW = tw * .58f;
            float capH = 27f;

            p.setStyle(Paint.Style.FILL);
            p.setShader(new LinearGradient(0, y, 0, y + capH,
                    Color.rgb(255, 222, 132),
                    Color.rgb(139, 69, 18), Shader.TileMode.CLAMP));
            p.setShadowLayer(9, 0, 3, Color.argb(160, 0, 0, 0));
            c.drawRoundRect(new RectF(cx - capW / 2, y, cx + capW / 2, y + capH),
                    8, 8, p);
            p.clearShadowLayer();
            p.setShader(null);

            // Wood grain and metallic-looking rim.
            p.setColor(Color.argb(100, 96, 48, 15));
            for (int i = -3; i <= 3; i++) {
                float xx = cx + i * 6;
                c.drawLine(xx, y + 4, xx + (i % 2) * 2, y + capH - 4, p);
            }
            p.setColor(Color.argb(100, 255, 244, 190));
            c.drawRoundRect(new RectF(cx - capW / 2 + 4, y + 3,
                    cx + capW / 2 - 4, y + 8), 4, 4, p);

            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(2.2f);
            stroke.setColor(Color.rgb(255, 239, 174));
            c.drawRoundRect(new RectF(cx - capW / 2, y, cx + capW / 2, y + capH),
                    8, 8, stroke);
        }

        private void drawBadge(Canvas c, float cx, float cy, String s, boolean selected) {
            p.setColor(selected ? Color.rgb(255, 197, 35) : Color.rgb(16, 57, 160));
            c.drawRoundRect(new RectF(cx - 15, cy - 13, cx + 15, cy + 13), 12, 12, p);
            text.setTypeface(Typeface.DEFAULT_BOLD);
            text.setTextAlign(Paint.Align.CENTER);
            text.setTextSize(13);
            text.setColor(Color.WHITE);
            c.drawText(s, cx, cy + 5, text);
        }

        private void drawControls(Canvas c, float w, float h) {
            float y = h - 205;
            drawCircleButton(c, 75, y, 52, Color.rgb(124, 25, 238), "↶");
            drawCircleButton(c, w / 2f - 105, y, 52, Color.rgb(169, 24, 244), "💡");
            drawRoundPanel(c, w / 2f - 45, y - 32, w / 2f + 145, y + 32,
                    Color.rgb(0, 155, 255), Color.rgb(34, 227, 255));
            text.setTextAlign(Paint.Align.CENTER);
            text.setTypeface(Typeface.DEFAULT_BOLD);
            text.setTextSize(14);
            text.setColor(Color.WHITE);
            c.drawText("حرکت‌ها", w / 2f + 50, y - 4, text);
            text.setTextSize(25);
            c.drawText(String.valueOf(moves), w / 2f + 50, y + 23, text);
            drawCircleButton(c, w - 75, y, 52, soundEnabled ? Color.rgb(49, 228, 36) : Color.rgb(115, 115, 125), soundEnabled ? "🔊" : "🔇");

            text.setTextSize(14);
            text.setColor(Color.WHITE);
            c.drawText("بازگشت", 75, y + 78, text);
            c.drawText("راهنما", w / 2f - 105, y + 78, text);
            c.drawText("صدا", w - 75, y + 78, text);

            // badges
            drawBadgeCircle(c, 99, y - 43, "3");
            drawBadgeCircle(c, w / 2f - 81, y - 43, "5");
        }

        private void drawFooter(Canvas c, float w, float h) {
            float top = h - 112;
            p.setShader(new LinearGradient(0, top, 0, h,
                    Color.rgb(94, 14, 220), Color.rgb(31, 10, 115), Shader.TileMode.CLAMP));
            c.drawRect(0, top, w, h, p);
            p.setShader(null);

            text.setTypeface(Typeface.DEFAULT_BOLD);
            text.setTextAlign(Paint.Align.CENTER);
            text.setTextSize(19);
            text.setColor(Color.WHITE);
            c.drawText("رنگ‌ها را به درستی مرتب کنید", w / 2f, top + 43, text);

            text.setTextSize(12);
            text.setColor(Color.rgb(255, 218, 60));
            c.drawText("جوایز مرحله: ★ 40   ★★ 70   ★★★ 100 سکه", w / 2f, top + 73, text);
            text.setTextSize(11);
            text.setColor(Color.argb(230, 220, 235, 255));
            c.drawText("مرحله " + level + " از 30   •   ستاره‌های کسب‌شده: " + totalStars +
                    "   •   سکه: " + coins, w / 2f, top + 96, text);
        }

        private void drawHint(Canvas c) {
            float[] a = getTubeGeometry(hintFrom);
            float[] b = getTubeGeometry(hintTo);
            if (a == null || b == null) return;
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(5);
            stroke.setColor(Color.rgb(255, 215, 30));
            c.drawLine(a[0], a[1], b[0], b[1], stroke);
        }

        private void drawVictory(Canvas c, float w, float h) {
            p.setColor(Color.argb(205, 4, 8, 45));
            c.drawRect(0, 0, w, h, p);

            p.setColor(Color.argb(80, 80, 20, 255));
            p.setShadowLayer(30, 0, 0, Color.MAGENTA);
            c.drawRoundRect(new RectF(30, h / 2f - 150, w - 30, h / 2f + 150), 35, 35, p);
            p.clearShadowLayer();
            drawVictoryParticles(c, w, h);

            text.setTextAlign(Paint.Align.CENTER);
            text.setTypeface(Typeface.DEFAULT_BOLD);
            text.setColor(Color.WHITE);
            text.setTextSize(32);
            c.drawText("🎉 عالی!", w / 2f, h / 2f - 90, text);

            int stars = calculateStars();
            text.setTextSize(35);
            text.setColor(Color.rgb(255, 210, 35));
            String s = stars >= 1 ? "★" : "☆";
            String starsText = "";
            for (int i = 0; i < 3; i++) starsText += i < stars ? "★" : "☆";
            c.drawText(starsText, w / 2f, h / 2f - 35, text);

            text.setTextSize(18);
            text.setColor(Color.WHITE);
            c.drawText("مرحله " + level + " کامل شد", w / 2f, h / 2f + 10, text);

            text.setTextSize(15);
            text.setColor(Color.rgb(255, 225, 80));
            c.drawText("جایزه: +" + lastEarnedCoins + " سکه", w / 2f, h / 2f + 48, text);

            text.setColor(Color.rgb(210, 230, 255));
            c.drawText("امتیاز کل: " + coins + " سکه", w / 2f, h / 2f + 78, text);
        }

        private void drawVictoryParticles(Canvas c, float w, float h) {
            long now = SystemClock.uptimeMillis();
            float age = victoryStart <= 0 ? 0f : (now - victoryStart) / 1000f;
            for (int i = 0; i < 28; i++) {
                float seedX = (sparkleRandom.nextInt(1000) / 1000f);
                float seedY = (sparkleRandom.nextInt(1000) / 1000f);
                float x = seedX * w;
                float y = h * .22f + ((seedY * h * .55f + age * (35 + i)) % (h * .62f));
                float r = 2f + (i % 4);
                int alpha = (int) (190 * Math.max(0f, 1f - age / 2.4f));
                p.setColor(Color.argb(alpha, 255, 190 + (i % 3) * 20, 55));
                c.drawCircle(x, y, r, p);
            }
        }

        private int calculateStars() {
            int perfect = getColorCount() * 3;
            if (moves <= perfect) return 3;
            if (moves <= perfect + 7) return 2;
            return 1;
        }

        private int calculateReward(int stars) {
            if (stars == 3) return 100;
            if (stars == 2) return 70;
            return 40;
        }

        private void drawRoundPanel(Canvas c, float l, float t, float r, float b, int c1, int c2) {
            p.setStyle(Paint.Style.FILL);
            p.setShader(new LinearGradient(l, t, r, b, c1, c2, Shader.TileMode.CLAMP));
            p.setShadowLayer(10, 0, 5, Color.argb(150, 0, 0, 0));
            c.drawRoundRect(new RectF(l, t, r, b), 20, 20, p);
            p.clearShadowLayer();
            p.setShader(null);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(2);
            stroke.setColor(Color.argb(180, 255, 255, 255));
            c.drawRoundRect(new RectF(l, t, r, b), 20, 20, stroke);
        }

        private void drawCircleButton(Canvas c, float x, float y, float r, int color, String symbol) {
            p.setColor(color);
            p.setShadowLayer(12, 0, 6, Color.argb(160, 0, 0, 0));
            c.drawCircle(x, y, r, p);
            p.clearShadowLayer();
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(2);
            stroke.setColor(Color.argb(190, 255, 255, 255));
            c.drawCircle(x, y, r - 2, stroke);
            text.setTextAlign(Paint.Align.CENTER);
            text.setTypeface(Typeface.DEFAULT_BOLD);
            text.setTextSize(31);
            text.setColor(Color.WHITE);
            c.drawText(symbol, x, y + 11, text);
        }

        private void drawBadgeCircle(Canvas c, float x, float y, String s) {
            p.setColor(Color.rgb(245, 24, 65));
            c.drawCircle(x, y, 16, p);
            text.setTextAlign(Paint.Align.CENTER);
            text.setTextSize(13);
            text.setTypeface(Typeface.DEFAULT_BOLD);
            text.setColor(Color.WHITE);
            c.drawText(s, x, y + 5, text);
        }

        private void drawCoin(Canvas c, float x, float y) {
            p.setColor(Color.rgb(255, 193, 15));
            p.setShadowLayer(7, 0, 2, Color.rgb(255, 120, 0));
            c.drawCircle(x, y, 19, p);
            p.clearShadowLayer();
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(2);
            stroke.setColor(Color.rgb(255, 235, 100));
            c.drawCircle(x, y, 15, stroke);
            text.setTextAlign(Paint.Align.CENTER);
            text.setTextSize(18);
            text.setColor(Color.rgb(160, 91, 0));
            text.setTypeface(Typeface.DEFAULT_BOLD);
            c.drawText("$", x, y + 6, text);
        }

        private void drawStar(Canvas c, float cx, float cy, float r, int color) {
            p.setColor(color);
            p.setShadowLayer(8, 0, 2, Color.rgb(255, 125, 0));
            android.graphics.Path path = new android.graphics.Path();
            for (int i = 0; i < 10; i++) {
                double a = -Math.PI / 2 + i * Math.PI / 5;
                float rr = i % 2 == 0 ? r : r * .42f;
                float x = cx + (float)Math.cos(a) * rr;
                float y = cy + (float)Math.sin(a) * rr;
                if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
            }
            path.close();
            c.drawPath(path, p);
            p.clearShadowLayer();
        }

        private int lighten(int color, float factor) {
            return Color.rgb(
                    Math.min(255, (int)(Color.red(color) * factor)),
                    Math.min(255, (int)(Color.green(color) * factor)),
                    Math.min(255, (int)(Color.blue(color) * factor))
            );
        }

        private int darken(int color, float factor) {
            return Color.rgb(
                    Math.max(0, (int)(Color.red(color) * factor)),
                    Math.max(0, (int)(Color.green(color) * factor)),
                    Math.max(0, (int)(Color.blue(color) * factor))
            );
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            if (e.getAction() != MotionEvent.ACTION_DOWN) return true;

            float x = e.getX(), y = e.getY();
            float h = getHeight(), w = getWidth();

            if (levelFinished) return true;

            float controlY = h - 205;
            if (distance(x, y, 75, controlY) < 65) {
                undoMove();
                return true;
            }
            if (distance(x, y, w / 2f - 105, controlY) < 65) {
                showHint();
                playGameSound(1);
                return true;
            }

            if (distance(x, y, w - 75, controlY) < 65) {
                soundEnabled = !soundEnabled;
                saveProgress();
                if (soundEnabled) playGameSound(3);
                invalidate();
                return true;
            }

            int tube = findTube(x, y);
            if (tube >= 0) {
                handleTubeClick(tube);
            }
            return true;
        }

        private float distance(float x1, float y1, float x2, float y2) {
            float dx = x1 - x2, dy = y1 - y2;
            return (float)Math.sqrt(dx * dx + dy * dy);
        }

        private int findTube(float x, float y) {
            for (int i = 0; i < tubes.size(); i++) {
                float[] g = getTubeGeometry(i);
                float l = g[0] - g[2] / 2 - 18;
                float r = g[0] + g[2] / 2 + 18;
                float t = g[1] - g[3] / 2 - 20;
                float b = g[1] + g[3] / 2 + 30;
                if (x >= l && x <= r && y >= t && y <= b) return i;
            }
            return -1;
        }

        private void handleTubeClick(int idx) {
            if (animProgress < 1f) return;

            if (selectedTube == -1) {
                if (tubes.get(idx).isEmpty()) {
                    Toast.makeText(ColorSortActivity.this, "این شیشه خالی است", Toast.LENGTH_SHORT).show();
                    return;
                }
                selectedTube = idx;
                playGameSound(1);
                invalidate();
                return;
            }

            if (selectedTube == idx) {
                selectedTube = -1;
                invalidate();
                return;
            }

            if (canMove(selectedTube, idx)) {
                saveMove(selectedTube, idx);
                beginAnimatedMove(selectedTube, idx);
                playGameSound(1);
                moves++;
                selectedTube = -1;
            } else {
                Toast.makeText(ColorSortActivity.this, "این حرکت ممکن نیست", Toast.LENGTH_SHORT).show();
                selectedTube = -1;
                invalidate();
            }
        }

        private boolean canMove(int from, int to) {
            if (from < 0 || to < 0 || from >= tubes.size() || to >= tubes.size()) return false;
            List<Integer> source = tubes.get(from);
            List<Integer> target = tubes.get(to);
            if (source.isEmpty() || target.size() >= CAPACITY) return false;
            int color = source.get(source.size() - 1);
            return target.isEmpty() || target.get(target.size() - 1) == color;
        }

        private void beginAnimatedMove(int from, int to) {
            List<Integer> source = tubes.get(from);
            animFrom = from;
            animTo = to;
            animColor = source.get(source.size() - 1);
            animAmount = Math.min(getTopSameCount(source), CAPACITY - tubes.get(to).size());
            animProgress = 0f;
            animStart = SystemClock.uptimeMillis();
            playGameSound(2);
            invalidate();
        }

        private void finishAnimatedMove() {
            if (animFrom < 0 || animTo < 0) return;
            moveColor(animFrom, animTo);
            animFrom = animTo = -1;
            animColor = -1;
            animAmount = 0;
            animProgress = 1f;
            playGameSound(1);
            if (isLevelComplete()) completeLevel();
            saveProgress();
            invalidate();
        }

        private void moveColor(int from, int to) {
            List<Integer> source = tubes.get(from);
            List<Integer> target = tubes.get(to);
            if (source.isEmpty()) return;
            int color = source.get(source.size() - 1);
            while (!source.isEmpty() && target.size() < CAPACITY &&
                    source.get(source.size() - 1) == color) {
                target.add(source.remove(source.size() - 1));
            }
        }

        private void saveMove(int from, int to) {
            List<Integer> source = tubes.get(from);
            int color = source.get(source.size() - 1);
            int amount = Math.min(getTopSameCount(source), CAPACITY - tubes.get(to).size());
            history.add(new Move(from, to, color, amount));
            if (history.size() > 100) history.remove(0);
        }

        void undoMove() {
            if (levelFinished || animProgress < 1f) return;
            if (history.isEmpty()) {
                Toast.makeText(ColorSortActivity.this, "حرکتی برای برگشت وجود ندارد", Toast.LENGTH_SHORT).show();
                return;
            }
            Move m = history.remove(history.size() - 1);
            List<Integer> source = tubes.get(m.to);
            List<Integer> target = tubes.get(m.from);
            for (int i = 0; i < m.amount; i++) {
                if (!source.isEmpty()) target.add(source.remove(source.size() - 1));
            }
            moves = Math.max(0, moves - 1);
            invalidate();
        }

        void showHint() {
            if (levelFinished || animProgress < 1f) return;
            for (int from = 0; from < tubes.size(); from++) {
                if (tubes.get(from).isEmpty()) continue;
                for (int to = 0; to < tubes.size(); to++) {
                    if (from != to && canMove(from, to)) {
                        hintFrom = from;
                        hintTo = to;
                        showingHint = true;
                        hintUntil = System.currentTimeMillis() + 1800;
                        invalidate();
                        return;
                    }
                }
            }
            Toast.makeText(ColorSortActivity.this, "فعلاً حرکت مناسبی پیدا نشد", Toast.LENGTH_SHORT).show();
        }

        private boolean isLevelComplete() {
            for (List<Integer> tube : tubes) {
                if (tube.isEmpty()) continue;
                if (tube.size() != CAPACITY) return false;
                int first = tube.get(0);
                for (int i = 1; i < tube.size(); i++) if (tube.get(i) != first) return false;
            }
            return true;
        }

        private void completeLevel() {
            levelFinished = true;
            int stars = calculateStars();
            lastEarnedCoins = calculateReward(stars);
            coins += lastEarnedCoins;
            totalStars += stars;
            victoryStart = SystemClock.uptimeMillis();
            playGameSound(3);
            playGameSound(1);
            saveProgress();
            invalidate();

            handler.postDelayed(new Runnable() {
                @Override public void run() {
                    if (!levelFinished) return;
                    if (level < MAX_LEVEL) {
                        level++;
                        resetLevel();
                    } else {
                        Toast.makeText(ColorSortActivity.this,
                                "🏆 هر ۳۰ مرحله را کامل کردی!", Toast.LENGTH_LONG).show();
                        level = 1;
                        resetLevel();
                    }
                }
            }, 2200);
        }

        class Move {
            int from, to, color, amount;
            Move(int from, int to, int color, int amount) {
                this.from = from; this.to = to; this.color = color; this.amount = amount;
            }
        }
    }
}
