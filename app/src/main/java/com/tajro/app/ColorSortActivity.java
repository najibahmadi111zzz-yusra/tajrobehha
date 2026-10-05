package com.tajro.app;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
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

        if (gameView != null) {
            gameView.startAnimation();
        }
    }

    class SortGameView extends View {

        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);

        private final Handler handler = new Handler();
        private final Random random = new Random();

        private final SharedPreferences progressPrefs =
                ColorSortActivity.this.getSharedPreferences(
                        "color_sort_progress",
                        MODE_PRIVATE
                );

        /* =========================================================
           AUDIO
           ========================================================= */

        private volatile AudioTrack activeAudioTrack;
        private Thread soundThread;

        private boolean soundEnabled = true;

        /* =========================================================
           ANIMATION
           ========================================================= */

        private float liquidWave = 0f;
        private float bottleGlow = 0f;

        private long victoryStart = 0L;

        private final Random sparkleRandom = new Random(7319);

        private float[] particleX = new float[42];
        private float[] particleY = new float[42];
        private float[] particleSpeed = new float[42];
        private float[] particleSize = new float[42];
        private float[] particlePhase = new float[42];

        /* =========================================================
           GAME DATA
           ========================================================= */

        private final List<List<Integer>> tubes = new ArrayList<>();
        private final List<Move> history = new ArrayList<>();

        private int selectedTube = -1;

        private int level = 1;
        private int moves = 0;

        private int coins = 0;
        private int totalStars = 0;
        private int lastEarnedCoins = 0;

        private boolean levelFinished = false;
        private boolean levelFailed = false;

        private boolean running = true;

        private long animStart = 0L;

        private int animFrom = -1;
        private int animTo = -1;
        private int animColor = -1;
        private int animAmount = 0;

        private float animProgress = 1f;

        /* =========================================================
           HINT
           ========================================================= */

        private boolean showingHint = false;
        private int hintFrom = -1;
        private int hintTo = -1;
        private long hintUntil = 0L;

        /* =========================================================
           ERROR / FAILURE SYSTEM
           ========================================================= */

        private int mistakes = 0;
        private final int MAX_MISTAKES = 3;

        private int errorFrom = -1;
        private int errorTo = -1;

        private long errorStart = 0L;
        private int errorStrength = 0;

        private long failureStart = 0L;

        /* =========================================================
           CONSTANTS
           ========================================================= */

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

        /* =========================================================
           FRAME LOOP
           ========================================================= */

        private final Runnable frame = new Runnable() {

            @Override
            public void run() {

                if (!running) {
                    return;
                }

                liquidWave += 0.13f;
                bottleGlow += 0.035f;

                invalidate();

                handler.postDelayed(this, 16);
            }
        };

        /* =========================================================
           CONSTRUCTOR
           ========================================================= */

        SortGameView() {

            super(ColorSortActivity.this);

            setLayerType(View.LAYER_TYPE_SOFTWARE, null);

            p.setAntiAlias(true);
            stroke.setAntiAlias(true);
            text.setAntiAlias(true);

            setFocusable(true);

            soundEnabled = progressPrefs.getBoolean("sound", true);

            level = Math.max(
                    1,
                    Math.min(
                            MAX_LEVEL,
                            progressPrefs.getInt("level", 1)
                    )
            );

            coins = Math.max(
                    0,
                    progressPrefs.getInt("coins", 0)
            );

            totalStars = Math.max(
                    0,
                    progressPrefs.getInt("stars", 0)
            );

            prepareVictoryParticles();

            resetLevel();
        }

        /* =========================================================
           ANIMATION CONTROL
           ========================================================= */

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

        /* =========================================================
           AUDIO SYSTEM
           ========================================================= */

        private void playGameSound(final int kind) {

            if (!soundEnabled) {
                return;
            }

            stopActiveSound();

            soundThread = new Thread(new Runnable() {

                @Override
                public void run() {

                    final int sampleRate = 44100;

                    int durationMs;

                    if (kind == 1) {
                        durationMs = 120;
                    } else if (kind == 2) {
                        durationMs = 620;
                    } else if (kind == 3) {
                        durationMs = 3400;
                    } else if (kind == 4) {
                        durationMs = 720;
                    } else {
                        durationMs = 150;
                    }

                    final int totalSamples =
                            sampleRate * durationMs / 1000;

                    int minBuffer = AudioTrack.getMinBufferSize(
                            sampleRate,
                            AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT
                    );

                    if (minBuffer <= 0) {
                        minBuffer = 4096;
                    }

                    AudioTrack track = null;

                    try {

                        track = new AudioTrack(
                                AudioManager.STREAM_MUSIC,
                                sampleRate,
                                AudioFormat.CHANNEL_OUT_MONO,
                                AudioFormat.ENCODING_PCM_16BIT,
                                Math.max(minBuffer, 8192),
                                AudioTrack.MODE_STREAM
                        );

                        activeAudioTrack = track;

                        track.play();

                        byte[] buffer = new byte[8192];

                        int position = 0;

                        float phase1 = 0f;
                        float phase2 = 0f;
                        float phase3 = 0f;
                        float phase4 = 0f;

                        float filteredNoise = 0f;

                        while (
                                position < totalSamples
                                        && soundEnabled
                                        && activeAudioTrack == track
                        ) {

                            int frames = Math.min(
                                    buffer.length / 2,
                                    totalSamples - position
                            );

                            for (int i = 0; i < frames; i++) {

                                int sampleIndex = position + i;

                                float q =
                                        sampleIndex /
                                                (float) Math.max(
                                                        1,
                                                        totalSamples - 1
                                                );

                                float value = 0f;

                                /* ---------------------------------
                                   SIMPLE CLICK
                                   --------------------------------- */

                                if (kind == 1) {

                                    float freq = 690f;

                                    phase1 +=
                                            (float) (
                                                    2.0 *
                                                            Math.PI *
                                                            freq /
                                                            sampleRate
                                            );

                                    float envelope =
                                            (float) Math.exp(
                                                    -q * 7.0
                                            );

                                    value =
                                            (float) Math.sin(phase1)
                                                    * envelope
                                                    * 0.22f;

                                }

                                /* ---------------------------------
                                   POUR SOUND
                                   --------------------------------- */

                                else if (kind == 2) {

                                    float raw =
                                            random.nextFloat() * 2f - 1f;

                                    filteredNoise +=
                                            (raw - filteredNoise)
                                                    * 0.075f;

                                    phase1 +=
                                            (float) (
                                                    2.0 *
                                                            Math.PI *
                                                            175 /
                                                            sampleRate
                                            );

                                    phase2 +=
                                            (float) (
                                                    2.0 *
                                                            Math.PI *
                                                            365 /
                                                            sampleRate
                                            );

                                    float envelope =
                                            (float) Math.sin(
                                                    Math.PI * q
                                            );

                                    value =
                                            (
                                                    filteredNoise * 0.70f
                                                            +
                                                            (float) Math.sin(phase1)
                                                                    * 0.13f
                                                            +
                                                            (float) Math.sin(phase2)
                                                                    * 0.07f
                                            )
                                                    * envelope
                                                    * 0.82f;
                                }

                                /* ---------------------------------
                                   LONG VICTORY MELODY
                                   --------------------------------- */

                                else if (kind == 3) {

                                    float[] notes = {
                                            523.25f,
                                            659.25f,
                                            783.99f,
                                            1046.50f,
                                            783.99f,
                                            987.77f,
                                            1174.66f,
                                            1046.50f,
                                            1318.51f,
                                            1567.98f
                                    };

                                    float noteLength =
                                            0.32f;

                                    int noteIndex =
                                            (int) (q * notes.length /
                                                    noteLength);

                                    noteIndex =
                                            noteIndex % notes.length;

                                    float notePos =
                                            (q * 3.125f)
                                                    % 1f;

                                    float melodyFreq =
                                            notes[noteIndex];

                                    phase1 +=
                                            (float) (
                                                    2.0 *
                                                            Math.PI *
                                                            melodyFreq /
                                                            sampleRate
                                            );

                                    phase2 +=
                                            (float) (
                                                    2.0 *
                                                            Math.PI *
                                                            melodyFreq *
                                                            0.5 /
                                                            sampleRate
                                            );

                                    phase3 +=
                                            (float) (
                                                    2.0 *
                                                            Math.PI *
                                                            104.66 /
                                                            sampleRate
                                            );

                                    phase4 +=
                                            (float) (
                                                    2.0 *
                                                            Math.PI *
                                                            156.80 /
                                                            sampleRate
                                            );

                                    float attack =
                                            Math.min(
                                                    1f,
                                                    notePos / 0.10f
                                            );

                                    float release =
                                            Math.min(
                                                    1f,
                                                    (1f - notePos) / 0.18f
                                            );

                                    float noteEnvelope =
                                            Math.min(
                                                    attack,
                                                    release
                                            );

                                    float overall;

                                    if (q < 0.08f) {
                                        overall = q / 0.08f;
                                    } else if (q > 0.90f) {
                                        overall =
                                                (1f - q) / 0.10f;
                                    } else {
                                        overall = 1f;
                                    }

                                    float sparkle =
                                            (float) Math.sin(
                                                    phase1
                                            ) * 0.24f;

                                    float harmony =
                                            (float) Math.sin(
                                                    phase2
                                            ) * 0.09f;

                                    float bass =
                                            (float) Math.sin(
                                                    phase3
                                            ) * 0.07f;

                                    float high =
                                            (float) Math.sin(
                                                    phase4
                                            ) * 0.035f;

                                    value =
                                            (
                                                    sparkle
                                                            + harmony
                                                            + bass
                                                            + high
                                            )
                                                    * noteEnvelope
                                                    * overall;

                                    /*
                                     * Final sparkle section.
                                     */
                                    if (q > 0.76f) {

                                        float sparkleEnv =
                                                (float) Math.sin(
                                                        (q - 0.76f)
                                                                * Math.PI
                                                                * 12f
                                                );

                                        value +=
                                                sparkleEnv
                                                        * 0.035f;
                                    }
                                }

                                /* ---------------------------------
                                   ERROR SOUND
                                   --------------------------------- */

                                else if (kind == 4) {

                                    float envelope =
                                            (float) Math.exp(
                                                    -q * 4.5f
                                            );

                                    float freq =
                                            410f - q * 180f;

                                    phase1 +=
                                            (float) (
                                                    2.0 *
                                                            Math.PI *
                                                            freq /
                                                            sampleRate
                                            );

                                    phase2 +=
                                            (float) (
                                                    2.0 *
                                                            Math.PI *
                                                            freq *
                                                            1.7 /
                                                            sampleRate
                                            );

                                    value =
                                            (
                                                    (float) Math.sin(phase1)
                                                            * 0.19f
                                                            +
                                                            (float) Math.sin(phase2)
                                                                    * 0.07f
                                            )
                                                    * envelope;
                                }

                                int pcm =
                                        Math.max(
                                                -32767,
                                                Math.min(
                                                        32767,
                                                        (int)
                                                                (
                                                                        value
                                                                                * 15000f
                                                                )
                                                )
                                        );

                                buffer[i * 2] =
                                        (byte) (pcm & 255);

                                buffer[i * 2 + 1] =
                                        (byte) ((pcm >> 8) & 255);
                            }

                            track.write(
                                    buffer,
                                    0,
                                    frames * 2
                            );

                            position += frames;
                        }

                        try {
                            track.stop();
                        } catch (Exception ignored) {
                        }

                    } catch (Exception ignored) {

                    } finally {

                        if (track != null) {

                            try {
                                track.release();
                            } catch (Exception ignored) {
                            }
                        }

                        if (activeAudioTrack == track) {
                            activeAudioTrack = null;
                        }
                    }
                }
            });

            soundThread.start();
        }

        private void stopActiveSound() {

            AudioTrack track = activeAudioTrack;

            activeAudioTrack = null;

            if (track != null) {

                try {
                    track.pause();
                } catch (Exception ignored) {
                }

                try {
                    track.flush();
                } catch (Exception ignored) {
                }

                try {
                    track.release();
                } catch (Exception ignored) {
                }
            }
        }

        void releaseAudio() {
            stopActiveSound();
        }

        /* =========================================================
           SAVE
           ========================================================= */

        private void saveProgress() {

            progressPrefs.edit()
                    .putInt("level", level)
                    .putInt("coins", coins)
                    .putInt("stars", totalStars)
                    .putBoolean("sound", soundEnabled)
                    .apply();
        }

        /* =========================================================
           LEVEL RESET
           ========================================================= */

        void resetLevel() {

            selectedTube = -1;

            moves = 0;

            mistakes = 0;

            levelFinished = false;

            levelFailed = false;

            showingHint = false;

            hintFrom = -1;
            hintTo = -1;

            animFrom = -1;
            animTo = -1;
            animColor = -1;
            animAmount = 0;

            animProgress = 1f;

            victoryStart = 0L;
            failureStart = 0L;

            errorFrom = -1;
            errorTo = -1;
            errorStart = 0L;

            tubes.clear();
            history.clear();

            int count = getColorCount();

            for (int c = 0; c < count; c++) {

                List<Integer> tube =
                        new ArrayList<>();

                for (int i = 0; i < CAPACITY; i++) {
                    tube.add(c);
                }

                tubes.add(tube);
            }

            int empty =
                    level < 10
                            ? 2
                            : (level < 20 ? 2 : 3);

            for (int i = 0; i < empty; i++) {
                tubes.add(new ArrayList<Integer>());
            }

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

            if (level <= 3) {
                return 12 + level * 3;
            }

            if (level <= 10) {
                return 25 + level * 4;
            }

            if (level <= 20) {
                return 55 + level * 4;
            }

            return 90 + level * 5;
        }

        /* =========================================================
           PUZZLE SHUFFLE
           ========================================================= */

        private void shufflePuzzle(int count) {

            int previousFrom = -1;
            int previousTo = -1;

            for (int step = 0; step < count; step++) {

                List<Integer> froms =
                        new ArrayList<>();

                for (int i = 0; i < tubes.size(); i++) {

                    if (!tubes.get(i).isEmpty()) {
                        froms.add(i);
                    }
                }

                Collections.shuffle(
                        froms,
                        random
                );

                boolean moved = false;

                for (int from : froms) {

                    if (from == previousTo) {
                        continue;
                    }

                    List<Integer> source =
                            tubes.get(from);

                    if (source.isEmpty()) {
                        continue;
                    }

                    int sourceColor =
                            source.get(
                                    source.size() - 1
                            );

                    int same =
                            getTopSameCount(source);

                    List<Integer> targets =
                            new ArrayList<>();

                    for (int to = 0;
                         to < tubes.size();
                         to++) {

                        if (to == from ||
                                to == previousFrom) {
                            continue;
                        }

                        List<Integer> target =
                                tubes.get(to);

                        if (target.size() >= CAPACITY) {
                            continue;
                        }

                        if (target.isEmpty() ||
                                target.get(
                                        target.size() - 1
                                ) != sourceColor) {

                            targets.add(to);
                        }
                    }

                    if (targets.isEmpty()) {
                        continue;
                    }

                    Collections.shuffle(
                            targets,
                            random
                    );

                    int to =
                            targets.get(0);

                    List<Integer> target =
                            tubes.get(to);

                    int free =
                            CAPACITY -
                                    target.size();

                    int amount =
                            Math.min(
                                    same,
                                    Math.min(
                                            free,
                                            1 + random.nextInt(2)
                                    )
                            );

                    for (int n = 0;
                         n < amount;
                         n++) {

                        target.add(
                                source.remove(
                                        source.size() - 1
                                )
                        );
                    }

                    previousFrom = from;
                    previousTo = to;

                    moved = true;

                    break;
                }

                if (!moved) {
                    break;
                }
            }

            if (isLevelComplete() &&
                    count < 120) {

                shufflePuzzle(count + 15);
            }
        }

        private int getTopSameCount(
                List<Integer> tube
        ) {

            if (tube.isEmpty()) {
                return 0;
            }

            int color =
                    tube.get(
                            tube.size() - 1
                    );

            int count = 0;

            for (int i = tube.size() - 1;
                 i >= 0;
                 i--) {

                if (tube.get(i) == color) {
                    count++;
                } else {
                    break;
                }
            }

            return count;
        }

        /* =========================================================
           DRAW
           ========================================================= */

        @Override
        protected void onDraw(Canvas canvas) {

            super.onDraw(canvas);

            float w = getWidth();
            float h = getHeight();

            drawBackground(
                    canvas,
                    w,
                    h
            );

            drawHeader(
                    canvas,
                    w
            );

            drawShelvesAndTubes(
                    canvas,
                    w,
                    h
            );

            drawControls(
                    canvas,
                    w,
                    h
            );

            drawFooter(
                    canvas,
                    w,
                    h
            );

            if (showingHint &&
                    System.currentTimeMillis()
                            < hintUntil) {

                drawHint(canvas);

            } else {

                showingHint = false;
            }

            if (errorStart > 0 &&
                    SystemClock.uptimeMillis()
                            - errorStart
                            < 650) {

                drawErrorMark(
                        canvas,
                        w,
                        h
                );
            }

            if (levelFinished) {

                drawVictory(
                        canvas,
                        w,
                        h
                );
            }

            if (levelFailed) {

                drawFailure(
                        canvas,
                        w,
                        h
                );
            }

            if (animProgress < 1f) {

                long elapsed =
                        SystemClock.uptimeMillis()
                                - animStart;

                animProgress =
                        Math.min(
                                1f,
                                elapsed / 560f
                        );

                if (animProgress >= 1f) {
                    finishAnimatedMove();
                }

                invalidate();
            }
        }

        /* =========================================================
           BACKGROUND
           ========================================================= */

        private void drawBackground(
                Canvas c,
                float w,
                float h
        ) {

            LinearGradient gradient =
                    new LinearGradient(
                            0,
                            0,
                            0,
                            h,
                            Color.rgb(12, 31, 150),
                            Color.rgb(19, 7, 88),
                            Shader.TileMode.CLAMP
                    );

            p.setShader(gradient);

            c.drawRect(
                    0,
                    0,
                    w,
                    h,
                    p
            );

            p.setShader(null);

            RadialGradient glow =
                    new RadialGradient(
                            w * .5f,
                            h * .38f,
                            w * .8f,
                            Color.argb(
                                    80,
                                    32,
                                    112,
                                    255
                            ),
                            Color.argb(
                                    0,
                                    20,
                                    25,
                                    120
                            ),
                            Shader.TileMode.CLAMP
                    );

            p.setShader(glow);

            c.drawCircle(
                    w * .5f,
                    h * .38f,
                    w * .8f,
                    p
            );

            p.setShader(null);

            p.setColor(
                    Color.argb(
                            70,
                            50,
                            180,
                            255
                    )
            );

            for (int i = 0; i < 12; i++) {

                float x =
                        (i * 97 + 43) % w;

                float y =
                        95 +
                                (
                                        (i * 131)
                                                % Math.max(
                                                1,
                                                (int) h
                                        )
                                );

                c.drawCircle(
                        x,
                        y,
                        2 + (i % 3),
                        p
                );
            }
        }

        /* =========================================================
           HEADER
           ========================================================= */

        private void drawHeader(
                Canvas c,
                float w
        ) {

            float y = 12;

            float margin = 12;

            float homeRight = 78;

            drawRoundPanel(
                    c,
                    margin,
                    y,
                    homeRight,
                    78,
                    Color.rgb(0, 166, 255),
                    Color.rgb(48, 78, 255)
            );

            text.setTypeface(
                    Typeface.DEFAULT_BOLD
            );

            text.setTextAlign(
                    Paint.Align.CENTER
            );

            text.setTextSize(36);

            text.setColor(Color.WHITE);

            c.drawText(
                    "⌂",
                    (margin + homeRight) / 2f,
                    59,
                    text
            );

            float coinWidth = 105;

            float coinLeft =
                    Math.max(
                            homeRight + 8,
                            w - coinWidth - margin
                    );

            float stageLeft =
                    homeRight + 8;

            float stageRight =
                    Math.min(
                            coinLeft - 8,
                            stageLeft + 175
                    );

            if (stageRight <= stageLeft) {
                stageRight = stageLeft + 130;
            }

            drawRoundPanel(
                    c,
                    stageLeft,
                    y,
                    stageRight,
                    78,
                    Color.rgb(111, 26, 238),
                    Color.rgb(190, 37, 255)
            );

            text.setTextSize(16);
            text.setColor(Color.WHITE);

            c.drawText(
                    "★  مرحله " + level,
                    (stageLeft + stageRight) / 2f,
                    59,
                    text
            );

            float starsLeft =
                    stageRight + 8;

            float starsRight =
                    coinLeft - 8;

            if (starsRight > starsLeft + 40) {

                drawRoundPanel(
                        c,
                        starsLeft,
                        y,
                        starsRight,
                        78,
                        Color.rgb(30, 20, 130),
                        Color.rgb(117, 39, 250)
                );

                float center =
                        (starsLeft + starsRight) / 2f;

                drawStar(
                        c,
                        center - 28,
                        39,
                        14,
                        Color.rgb(255, 208, 25)
                );

                drawStar(
                        c,
                        center,
                        39,
                        14,
                        Color.rgb(255, 208, 25)
                );

                drawStar(
                        c,
                        center + 28,
                        39,
                        14,
                        Color.rgb(255, 208, 25)
                );

                p.setColor(
                        Color.rgb(
                                255,
                                207,
                                25
                        )
                );

                c.drawRoundRect(
                        new RectF(
                                starsLeft + 15,
                                61,
                                starsRight - 15,
                                67
                        ),
                        4,
                        4,
                        p
                );
            }

            drawRoundPanel(
                    c,
                    coinLeft,
                    y,
                    w - margin,
                    78,
                    Color.rgb(9, 84, 235),
                    Color.rgb(35, 205, 255)
            );

            drawCoin(
                    c,
                    coinLeft + 25,
                    45
            );

            text.setTextSize(15);
            text.setColor(Color.WHITE);
            text.setTextAlign(Paint.Align.LEFT);

            c.drawText(
                    String.valueOf(coins),
                    coinLeft + 47,
                    52,
                    text
            );

            text.setTextSize(24);

            c.drawText(
                    "+",
                    w - 27,
                    55,
                    text
            );
        }

        /* =========================================================
           SHELVES / TUBES
           ========================================================= */

        private void drawShelvesAndTubes(
                Canvas c,
                float w,
                float h
        ) {

            int count =
                    tubes.size();

            int columns =
                    count <= 6 ? 3 : 5;

            int rows =
                    (int) Math.ceil(
                            count /
                                    (float) columns
                    );

            float top = 116;

            float bottomControls = 275;

            float areaH =
                    Math.max(
                            300,
                            h - top - bottomControls
                    );

            float rowH =
                    areaH /
                            Math.max(
                                    1,
                                    rows
                            );

            float tubeW =
                    Math.min(
                            92,
                            w / (columns + .9f)
                    );

            float tubeH =
                    Math.min(
                            230,
                            rowH * .76f
                    );

            for (int row = 0;
                 row < rows;
                 row++) {

                float shelfY =
                        top +
                                row * rowH +
                                rowH * .83f;

                drawShelf(
                        c,
                        20,
                        shelfY,
                        w - 20,
                        22
                );

                for (int col = 0;
                     col < columns;
                     col++) {

                    int index =
                            row * columns + col;

                    if (index >= count) {
                        break;
                    }

                    float cx =
                            w *
                                    (col + 1f)
                                    /
                                    (columns + 1f);

                    float cy =
                            top +
                                    row * rowH +
                                    rowH * .42f;

                    drawTube(
                            c,
                            index,
                            cx,
                            cy,
                            tubeW,
                            tubeH
                    );
                }
            }
        }

        private void drawShelf(
                Canvas c,
                float left,
                float y,
                float right,
                float height
        ) {

            p.setStyle(Paint.Style.FILL);

            p.setShadowLayer(
                    12,
                    0,
                    8,
                    Color.argb(
                            160,
                            0,
                            0,
                            0
                    )
            );

            p.setShader(
                    new LinearGradient(
                            0,
                            y,
                            0,
                            y + height,
                            Color.rgb(
                                    255,
                                    184,
                                    76
                            ),
                            Color.rgb(
                                    130,
                                    59,
                                    20
                            ),
                            Shader.TileMode.CLAMP
                    )
            );

            c.drawRoundRect(
                    new RectF(
                            left,
                            y,
                            right,
                            y + height
                    ),
                    12,
                    12,
                    p
            );

            p.setShader(null);

            p.clearShadowLayer();

            stroke.setStyle(
                    Paint.Style.STROKE
            );

            stroke.setStrokeWidth(2);

            stroke.setColor(
                    Color.argb(
                            180,
                            255,
                            225,
                            130
                    )
            );

            c.drawRoundRect(
                    new RectF(
                            left,
                            y,
                            right,
                            y + height
                    ),
                    12,
                    12,
                    stroke
            );
        }

        /* =========================================================
           TUBE
           ========================================================= */

        private void drawTube(
                Canvas c,
                int index,
                float cx,
                float cy,
                float tw,
                float th
        ) {

            float left =
                    cx - tw / 2f;

            float right =
                    cx + tw / 2f;

            float top =
                    cy - th / 2f;

            float bottom =
                    cy + th / 2f;

            boolean selected =
                    index == selectedTube;

            boolean pouring =
                    animProgress < 1f &&
                            index == animFrom;

            boolean receiving =
                    animProgress < 1f &&
                            index == animTo;

            List<Integer> tube =
                    tubes.get(index);

            boolean shaking =
                    isTubeShaking(index);

            float shakeX =
                    shaking
                            ? getShakeOffset()
                            : 0f;

            float shakeAngle =
                    shaking
                            ? getShakeAngle()
                            : 0f;

            cx += shakeX;
            left += shakeX;
            right += shakeX;

            if (selected ||
                    pouring ||
                    isTubeComplete(tube)) {

                int glowColor =
                        isTubeComplete(tube)
                                ? Color.rgb(
                                255,
                                213,
                                70
                        )
                                : Color.rgb(
                                50,
                                225,
                                255
                        );

                p.setStyle(
                        Paint.Style.FILL
                );

                p.setColor(
                        Color.argb(
                                selected ? 80 : 42,
                                Color.red(glowColor),
                                Color.green(glowColor),
                                Color.blue(glowColor)
                        )
                );

                p.setShadowLayer(
                        selected ? 28 : 18,
                        0,
                        4,
                        glowColor
                );

                c.drawRoundRect(
                        new RectF(
                                left - 8,
                                top - 8,
                                right + 8,
                                bottom + 8
                        ),
                        28,
                        28,
                        p
                );

                p.clearShadowLayer();
            }

            float angle = 0f;

            if (pouring) {

                float[] target =
                        getTubeGeometry(animTo);

                float direction =
                        target[0] >= cx
                                ? 1f
                                : -1f;

                float wave =
                        (float) Math.sin(
                                animProgress *
                                        Math.PI
                        );

                angle =
                        direction *
                                17f *
                                wave;
            }

            angle += shakeAngle;

            c.save();

            c.rotate(
                    angle,
                    cx,
                    bottom - 18
            );

            p.setStyle(
                    Paint.Style.FILL
            );

            p.setColor(
                    Color.argb(
                            65,
                            0,
                            0,
                            0
                    )
            );

            p.setShadowLayer(
                    16,
                    0,
                    10,
                    Color.argb(
                            180,
                            0,
                            0,
                            0
                    )
            );

            c.drawRoundRect(
                    new RectF(
                            left - 2,
                            top + 13,
                            right + 2,
                            bottom + 2
                    ),
                    25,
                    25,
                    p
            );

            p.clearShadowLayer();

            float neckTop =
                    top - 1;

            float neckBottom =
                    top + 28;

            float neckL =
                    left + 11;

            float neckR =
                    right - 11;

            Path bottlePath =
                    createBottlePath(
                            left,
                            right,
                            top + 12,
                            bottom,
                            neckL,
                            neckR,
                            neckTop,
                            neckBottom
                    );

            Path innerBottlePath =
                    createBottlePath(
                            left + 4,
                            right - 4,
                            top + 18,
                            bottom - 5,
                            neckL + 2,
                            neckR - 2,
                            neckTop + 5,
                            neckBottom - 2
                    );

            p.setShader(
                    new LinearGradient(
                            left,
                            top,
                            right,
                            bottom,
                            Color.argb(
                                    135,
                                    255,
                                    255,
                                    255
                            ),
                            Color.argb(
                                    18,
                                    78,
                                    165,
                                    255
                            ),
                            Shader.TileMode.CLAMP
                    )
            );

            c.drawPath(
                    bottlePath,
                    p
            );

            p.setShader(null);

            p.setShader(
                    new LinearGradient(
                            left,
                            top,
                            right,
                            bottom,
                            Color.argb(
                                    32,
                                    190,
                                    235,
                                    255
                            ),
                            Color.argb(
                                    58,
                                    20,
                                    85,
                                    180
                            ),
                            Shader.TileMode.CLAMP
                    )
            );

            c.drawPath(
                    innerBottlePath,
                    p
            );

            p.setShader(null);

            /* Neck */

            p.setStyle(
                    Paint.Style.FILL
            );

            p.setColor(
                    Color.argb(
                            52,
                            255,
                            255,
                            255
                    )
            );

            c.drawRoundRect(
                    new RectF(
                            neckL,
                            neckTop + 5,
                            neckR,
                            neckBottom
                    ),
                    10,
                    10,
                    p
            );

            stroke.setStyle(
                    Paint.Style.STROKE
            );

            stroke.setStrokeWidth(2.6f);

            stroke.setColor(
                    Color.argb(
                            235,
                            224,
                            250,
                            255
                    )
            );

            c.drawRoundRect(
                    new RectF(
                            neckL,
                            neckTop,
                            neckR,
                            neckBottom
                    ),
                    10,
                    10,
                    stroke
            );

            float innerL =
                    left + 8;

            float innerR =
                    right - 8;

            float innerB =
                    bottom - 8;

            float layerH =
                    (bottom - top - 30)
                            / CAPACITY;

            float visibleUnits =
                    pouring
                            ? Math.max(
                            0f,
                            tube.size()
                                    -
                                    animAmount *
                                            animProgress
                    )
                            : tube.size();

            c.save();

            c.clipPath(
                    innerBottlePath
            );

            for (int j = 0;
                 j < tube.size();
                 j++) {

                float visible =
                        Math.max(
                                0f,
                                Math.min(
                                        1f,
                                        visibleUnits - j
                                )
                        );

                if (visible <= 0f) {
                    continue;
                }

                float lb =
                        innerB -
                                j * layerH;

                float lt =
                        lb -
                                layerH *
                                        visible;

                drawLiquid(
                        c,
                        innerL,
                        lt,
                        innerR,
                        lb,
                        tube.get(j),
                        Math.abs(
                                (j + 1)
                                        -
                                        visibleUnits
                        ) < .06f
                );
            }

            if (receiving &&
                    animColor >= 0) {

                float incoming =
                        animAmount *
                                animProgress;

                float lb =
                        innerB -
                                tube.size() *
                                        layerH;

                float lt =
                        lb -
                                incoming *
                                        layerH;

                if (incoming > 0f) {

                    drawLiquid(
                            c,
                            innerL,
                            lt,
                            innerR,
                            lb,
                            animColor,
                            true
                    );
                }
            }

            c.restore();

            if (pouring) {

                drawPourStream(
                        c,
                        cx,
                        top + 16,
                        animTo,
                        animColor,
                        animProgress,
                        angle
                );
            }

            /* Reflection */

            p.setShader(
                    new LinearGradient(
                            left + 7,
                            0,
                            left + 28,
                            0,
                            Color.argb(
                                    145,
                                    255,
                                    255,
                                    255
                            ),
                            Color.argb(
                                    0,
                                    255,
                                    255,
                                    255
                            ),
                            Shader.TileMode.CLAMP
                    )
            );

            c.drawRoundRect(
                    new RectF(
                            left + 7,
                            top + 22,
                            left + 27,
                            bottom - 14
                    ),
                    9,
                    9,
                    p
            );

            p.setShader(null);

            p.setColor(
                    Color.argb(
                            125,
                            255,
                            255,
                            255
                    )
            );

            c.drawRoundRect(
                    new RectF(
                            left + 13,
                            top + 16,
                            right - 18,
                            top + 20
                    ),
                    3,
                    3,
                    p
            );

            stroke.setStyle(
                    Paint.Style.STROKE
            );

            stroke.setStrokeWidth(
                    selected ? 4.2f : 2.7f
            );

            stroke.setColor(
                    selected
                            ? Color.rgb(
                            83,
                            244,
                            255
                    )
                            : Color.argb(
                            238,
                            225,
                            250,
                            255
                    )
            );

            c.drawPath(
                    bottlePath,
                    stroke
            );

            stroke.setStrokeWidth(2.2f);

            c.drawArc(
                    new RectF(
                            left + 4,
                            bottom - 19,
                            right - 4,
                            bottom + 5
                    ),
                    0,
                    180,
                    false,
                    stroke
            );

            stroke.setStrokeWidth(3f);

            c.drawRoundRect(
                    new RectF(
                            left + 3,
                            top,
                            right - 3,
                            top + 27
                    ),
                    11,
                    11,
                    stroke
            );

            stroke.setStrokeWidth(1.4f);

            stroke.setColor(
                    Color.argb(
                            180,
                            255,
                            255,
                            255
                    )
            );

            c.drawRoundRect(
                    new RectF(
                            left + 7,
                            top + 5,
                            right - 7,
                            top + 18
                    ),
                    7,
                    7,
                    stroke
            );

            c.restore();

            if (isTubeComplete(tube)) {

                drawCap(
                        c,
                        cx,
                        top + 1,
                        tw
                );
            }

            drawBadge(
                    c,
                    cx,
                    bottom + 11,
                    String.valueOf(index + 1),
                    selected
            );
        }

        /* =========================================================
           SHAKE
           ========================================================= */

        private boolean isTubeShaking(int index) {

            if (errorStart <= 0) {
                return false;
            }

            long elapsed =
                    SystemClock.uptimeMillis()
                            - errorStart;

            if (elapsed > 650) {
                return false;
            }

            return index == errorFrom ||
                    index == errorTo;
        }

        private float getShakeOffset() {

            long elapsed =
                    SystemClock.uptimeMillis()
                            - errorStart;

            float progress =
                    Math.min(
                            1f,
                            elapsed / 650f
                    );

            float strength =
                    errorStrength >= 2
                            ? 13f
                            : 8f;

            float envelope =
                    1f - progress;

            return (float)
                    Math.sin(
                            progress *
                                    Math.PI *
                                    9f
                    )
                            *
                            strength
                            *
                            envelope;
        }

        private float getShakeAngle() {

            long elapsed =
                    SystemClock.uptimeMillis()
                            - errorStart;

            float progress =
                    Math.min(
                            1f,
                            elapsed / 650f
                    );

            float strength =
                    errorStrength >= 2
                            ? 6f
                            : 3.5f;

            float envelope =
                    1f - progress;

            return (float)
                    Math.sin(
                            progress *
                                    Math.PI *
                                    8f
                    )
                            *
                            strength
                            *
                            envelope;
        }

        /* =========================================================
           POUR STREAM
           ========================================================= */

        private void drawPourStream(
                Canvas c,
                float sx,
                float sy,
                int targetIndex,
                int colorIndex,
                float progress,
                float angle
        ) {

            if (targetIndex < 0 ||
                    colorIndex < 0 ||
                    colorIndex >= colors.length) {

                return;
            }

            float[] target =
                    getTubeGeometry(
                            targetIndex
                    );

            float tx = target[0];

            float ty =
                    target[1]
                            -
                            target[3] / 2f
                            +
                            36f;

            float eased =
                    progress *
                            progress *
                            (3f - 2f * progress);

            float dir =
                    tx >= sx
                            ? 1f
                            : -1f;

            float startX =
                    sx +
                            (float)
                                    Math.sin(
                                            Math.toRadians(
                                                    angle
                                            )
                                    )
                                    * 14f;

            float startY =
                    sy + 8f;

            float endX = tx;

            float endY =
                    ty +
                            Math.min(
                                    28f,
                                    eased * 34f
                            );

            float bx =
                    (startX + endX) / 2f
                            +
                            dir * 10f;

            float by =
                    (startY + endY) / 2f
                            +
                            13f;

            int base =
                    colors[colorIndex];

            int light =
                    lighten(
                            base,
                            1.43f
                    );

            int dark =
                    darken(
                            base,
                            .50f
                    );

            Path path =
                    new Path();

            path.moveTo(
                    startX - 4.5f,
                    startY
            );

            path.cubicTo(
                    startX - 2,
                    startY + 18,
                    bx - 7,
                    by,
                    endX - 3.5f,
                    endY
            );

            path.lineTo(
                    endX + 3.5f,
                    endY
            );

            path.cubicTo(
                    bx + 7,
                    by,
                    startX + 2,
                    startY + 18,
                    startX + 4.5f,
                    startY
            );

            path.close();

            p.setShader(
                    new LinearGradient(
                            startX,
                            0,
                            endX,
                            0,
                            light,
                            dark,
                            Shader.TileMode.CLAMP
                    )
            );

            p.setShadowLayer(
                    10,
                    0,
                    2,
                    base
            );

            c.drawPath(
                    path,
                    p
            );

            p.clearShadowLayer();

            p.setShader(null);

            p.setColor(
                    Color.argb(
                            145,
                            255,
                            255,
                            255
                    )
            );

            c.drawRoundRect(
                    new RectF(
                            startX - 1.2f,
                            startY + 4,
                            endX + 1.2f,
                            endY - 2
                    ),
                    2,
                    2,
                    p
            );

            int drops =
                    3 +
                            (int)
                                    (progress * 4f);

            for (int i = 0;
                 i < drops;
                 i++) {

                float q =
                        (i + 1f)
                                /
                                (drops + 1f);

                float dx =
                        startX +
                                (endX - startX)
                                        * q;

                float dy =
                        startY +
                                (endY - startY)
                                        * q
                                +
                                (float)
                                        Math.sin(
                                                liquidWave * 2 +
                                                        i
                                        )
                                        * 2.2f;

                p.setColor(
                        Color.argb(
                                190,
                                Color.red(light),
                                Color.green(light),
                                Color.blue(light)
                        )
                );

                c.drawCircle(
                        dx,
                        dy,
                        2f,
                        p
                );
            }

            float splash =
                    (float)
                            Math.sin(
                                    progress *
                                            Math.PI
                            );

            if (splash > .05f) {

                stroke.setStyle(
                        Paint.Style.STROKE
                );

                stroke.setStrokeWidth(
                        1.8f
                );

                stroke.setColor(
                        Color.argb(
                                (int)
                                        (180 * splash),
                                255,
                                255,
                                255
                        )
                );

                float radius =
                        7f +
                                10f *
                                        splash;

                c.drawOval(
                        new RectF(
                                endX - radius,
                                endY - 3,
                                endX + radius,
                                endY + 3
                        ),
                        stroke
                );
            }
        }

        /* =========================================================
           BOTTLE PATH
           ========================================================= */

        private Path createBottlePath(
                float left,
                float right,
                float bodyTop,
                float bottom,
                float neckL,
                float neckR,
                float neckTop,
                float neckBottom
        ) {

            Path path =
                    new Path();

            float shoulder =
                    bodyTop + 24f;

            path.moveTo(
                    neckL,
                    neckTop
            );

            path.lineTo(
                    neckL,
                    neckBottom
            );

            path.cubicTo(
                    neckL,
                    shoulder - 4,
                    left + 3,
                    shoulder - 1,
                    left + 2,
                    shoulder + 18
            );

            path.lineTo(
                    left + 2,
                    bottom - 28
            );

            path.cubicTo(
                    left + 2,
                    bottom - 9,
                    left + 13,
                    bottom,
                    (left + right) / 2f,
                    bottom
            );

            path.cubicTo(
                    right - 13,
                    bottom,
                    right - 2,
                    bottom - 9,
                    right - 2,
                    bottom - 28
            );

            path.lineTo(
                    right - 2,
                    shoulder + 18
            );

            path.cubicTo(
                    right - 3,
                    shoulder - 1,
                    neckR,
                    shoulder - 4,
                    neckR,
                    neckBottom
            );

            path.lineTo(
                    neckR,
                    neckTop
            );

            path.close();

            return path;
        }

        /* =========================================================
           GEOMETRY
           ========================================================= */

        private float[] getTubeGeometry(
                int index
        ) {

            if (index < 0 ||
                    index >= tubes.size()) {

                return null;
            }

            int count =
                    tubes.size();

            int columns =
                    count <= 6 ? 3 : 5;

            int rows =
                    (int)
                            Math.ceil(
                                    count /
                                            (float)
                                                    columns
                            );

            float w =
                    getWidth();

            float h =
                    getHeight();

            float top = 116;

            float bottomControls = 275;

            float areaH =
                    Math.max(
                            300,
                            h -
                                    top -
                                    bottomControls
                    );

            float rowH =
                    areaH /
                            Math.max(
                                    1,
                                    rows
                            );

            float tubeW =
                    Math.min(
                            92,
                            w /
                                    (columns + .9f)
                    );

            float tubeH =
                    Math.min(
                            230,
                            rowH * .76f
                    );

            int row =
                    index / columns;

            int col =
                    index % columns;

            float cx =
                    w *
                            (col + 1f)
                            /
                            (columns + 1f);

            float cy =
                    top +
                            row * rowH +
                            rowH * .42f;

            return new float[]{
                    cx,
                    cy,
                    tubeW,
                    tubeH
            };
        }

        /* =========================================================
           LIQUID
           ========================================================= */

        private void drawLiquid(
                Canvas c,
                float left,
                float top,
                float right,
                float bottom,
                int colorIndex,
                boolean topLayer
        ) {

            if (colorIndex < 0 ||
                    colorIndex >= colors.length ||
                    bottom <= top) {

                return;
            }

            int base =
                    colors[colorIndex];

            int light =
                    lighten(
                            base,
                            1.40f
                    );

            int dark =
                    darken(
                            base,
                            .46f
                    );

            float radius =
                    Math.min(
                            9f,
                            Math.max(
                                    3f,
                                    (bottom - top) *
                                            .22f
                            )
                    );

            p.setStyle(
                    Paint.Style.FILL
            );

            p.setShader(
                    new LinearGradient(
                            0,
                            top,
                            0,
                            bottom,
                            light,
                            dark,
                            Shader.TileMode.CLAMP
                    )
            );

            c.drawRoundRect(
                    new RectF(
                            left,
                            top,
                            right,
                            bottom
                    ),
                    radius,
                    radius,
                    p
            );

            p.setShader(null);

            p.setShader(
                    new LinearGradient(
                            left,
                            0,
                            right,
                            0,
                            Color.argb(
                                    105,
                                    255,
                                    255,
                                    255
                            ),
                            Color.argb(
                                    0,
                                    255,
                                    255,
                                    255
                            ),
                            Shader.TileMode.CLAMP
                    )
            );

            c.drawRoundRect(
                    new RectF(
                            left + 2,
                            top + 2,
                            right - 2,
                            bottom - 2
                    ),
                    radius,
                    radius,
                    p
            );

            p.setShader(null);

            p.setColor(
                    Color.argb(
                            92,
                            255,
                            255,
                            255
                    )
            );

            c.drawRoundRect(
                    new RectF(
                            left + 4,
                            top + 2,
                            left + 12,
                            bottom - 3
                    ),
                    4,
                    4,
                    p
            );

            if (topLayer) {

                drawLiquidSurface(
                        c,
                        left,
                        top,
                        right,
                        colorIndex
                );
            }
        }

        private void drawLiquidSurface(
                Canvas c,
                float left,
                float y,
                float right,
                int colorIndex
        ) {

            if (colorIndex < 0 ||
                    colorIndex >= colors.length) {

                return;
            }

            int base =
                    colors[colorIndex];

            int light =
                    lighten(
                            base,
                            1.46f
                    );

            float wave =
                    (float)
                            Math.sin(
                                    liquidWave * 1.35f
                            )
                            * 2f;

            Path path =
                    new Path();

            path.moveTo(
                    left,
                    y + wave
            );

            for (int i = 1;
                 i <= 14;
                 i++) {

                float q =
                        i / 14f;

                float x =
                        left +
                                (right - left)
                                        * q;

                float yy =
                        y +
                                wave *
                                        (float)
                                                Math.sin(
                                                        q *
                                                                Math.PI *
                                                                2f +
                                                                liquidWave
                                                );

                path.lineTo(
                        x,
                        yy
                );
            }

            path.lineTo(
                    right,
                    y + 12
            );

            path.lineTo(
                    left,
                    y + 12
            );

            path.close();

            p.setShader(
                    new LinearGradient(
                            0,
                            y,
                            0,
                            y + 13,
                            light,
                            base,
                            Shader.TileMode.CLAMP
                    )
            );

            c.drawPath(
                    path,
                    p
            );

            p.setShader(null);

            p.setColor(
                    Color.argb(
                            155,
                            255,
                            255,
                            255
                    )
            );

            c.drawOval(
                    new RectF(
                            left + 5,
                            y - 1 + wave,
                            right - 5,
                            y + 8 + wave
                    ),
                    p
            );
        }

        /* =========================================================
           COMPLETE TUBE
           ========================================================= */

        private boolean isTubeComplete(
                List<Integer> tube
        ) {

            if (tube.size() != CAPACITY) {
                return false;
            }

            int color =
                    tube.get(0);

            for (int i = 1;
                 i < tube.size();
                 i++) {

                if (tube.get(i) != color) {
                    return false;
                }
            }

            return true;
        }

        /* =========================================================
           CAP
           ========================================================= */

        private void drawCap(
                Canvas c,
                float cx,
                float y,
                float tw
        ) {

            float capW =
                    tw * .58f;

            float capH = 27f;

            p.setStyle(
                    Paint.Style.FILL
            );

            p.setShader(
                    new LinearGradient(
                            0,
                            y,
                            0,
                            y + capH,
                            Color.rgb(
                                    255,
                                    222,
                                    132
                            ),
                            Color.rgb(
                                    139,
                                    69,
                                    18
                            ),
                            Shader.TileMode.CLAMP
                    )
            );

            p.setShadowLayer(
                    9,
                    0,
                    3,
                    Color.argb(
                            160,
                            0,
                            0,
                            0
                    )
            );

            c.drawRoundRect(
                    new RectF(
                            cx - capW / 2,
                            y,
                            cx + capW / 2,
                            y + capH
                    ),
                    8,
                    8,
                    p
            );

            p.clearShadowLayer();

            p.setShader(null);

            p.setColor(
                    Color.argb(
                            100,
                            96,
                            48,
                            15
                    )
            );

            for (int i = -3;
                 i <= 3;
                 i++) {

                float xx =
                        cx + i * 6;

                c.drawLine(
                        xx,
                        y + 4,
                        xx +
                                (i % 2) * 2,
                        y + capH - 4,
                        p
                );
            }

            p.setColor(
                    Color.argb(
                            100,
                            255,
                            244,
                            190
                    )
            );

            c.drawRoundRect(
                    new RectF(
                            cx - capW / 2 + 4,
                            y + 3,
                            cx + capW / 2 - 4,
                            y + 8
                    ),
                    4,
                    4,
                    p
            );

            stroke.setStyle(
                    Paint.Style.STROKE
            );

            stroke.setStrokeWidth(2.2f);

            stroke.setColor(
                    Color.rgb(
                            255,
                            239,
                            174
                    )
            );

            c.drawRoundRect(
                    new RectF(
                            cx - capW / 2,
                            y,
                            cx + capW / 2,
                            y + capH
                    ),
                    8,
                    8,
                    stroke
            );
        }

        /* =========================================================
           BADGE
           ========================================================= */

        private void drawBadge(
                Canvas c,
                float cx,
                float cy,
                String value,
                boolean selected
        ) {

            p.setColor(
                    selected
                            ? Color.rgb(
                            255,
                            197,
                            35
                    )
                            : Color.rgb(
                            16,
                            57,
                            160
                    )
            );

            c.drawRoundRect(
                    new RectF(
                            cx - 15,
                            cy - 13,
                            cx + 15,
                            cy + 13
                    ),
                    12,
                    12,
                    p
            );

            text.setTypeface(
                    Typeface.DEFAULT_BOLD
            );

            text.setTextAlign(
                    Paint.Align.CENTER
            );

            text.setTextSize(13);

            text.setColor(Color.WHITE);

            c.drawText(
                    value,
                    cx,
                    cy + 5,
                    text
            );
        }

        /* =========================================================
           CONTROLS
           ========================================================= */

        private void drawControls(
                Canvas c,
                float w,
                float h
        ) {

            float y =
                    h - 205;

            drawCircleButton(
                    c,
                    75,
                    y,
                    52,
                    Color.rgb(
                            124,
                            25,
                            238
                    ),
                    "↶"
            );

            drawCircleButton(
                    c,
                    w / 2f - 105,
                    y,
                    52,
                    Color.rgb(
                            169,
                            24,
                            244
                    ),
                    "?"
            );

            drawRoundPanel(
                    c,
                    w / 2f - 45,
                    y - 32,
                    w / 2f + 145,
                    y + 32,
                    Color.rgb(
                            0,
                            155,
                            255
                    ),
                    Color.rgb(
                            34,
                            227,
                            255
                    )
            );

            text.setTextAlign(
                    Paint.Align.CENTER
            );

            text.setTypeface(
                    Typeface.DEFAULT_BOLD
            );

            text.setTextSize(14);

            text.setColor(Color.WHITE);

            c.drawText(
                    "حرکت‌ها",
                    w / 2f + 50,
                    y - 4,
                    text
            );

            text.setTextSize(25);

            c.drawText(
                    String.valueOf(moves),
                    w / 2f + 50,
                    y + 23,
                    text
            );

            drawCircleButton(
                    c,
                    w - 75,
                    y,
                    52,
                    soundEnabled
                            ? Color.rgb(
                            49,
                            228,
                            36
                    )
                            : Color.rgb(
                            115,
                            115,
                            125
                    ),
                    soundEnabled
                            ? "♪"
                            : "×"
            );

            text.setTextSize(14);

            text.setColor(Color.WHITE);

            c.drawText(
                    "بازگشت",
                    75,
                    y + 78,
                    text
            );

            c.drawText(
                    "راهنما",
                    w / 2f - 105,
                    y + 78,
                    text
            );

            c.drawText(
                    "صدا",
                    w - 75,
                    y + 78,
                    text
            );

            drawBadgeCircle(
                    c,
                    99,
                    y - 43,
                    String.valueOf(
                            Math.max(
                                    0,
                                    MAX_MISTAKES -
                                            mistakes
                            )
                    )
            );

            drawBadgeCircle(
                    c,
                    w / 2f - 81,
                    y - 43,
                    "5"
            );
        }

        /* =========================================================
           FOOTER
           ========================================================= */

        private void drawFooter(
                Canvas c,
                float w,
                float h
        ) {

            float top =
                    h - 112;

            p.setShader(
                    new LinearGradient(
                            0,
                            top,
                            0,
                            h,
                            Color.rgb(
                                    94,
                                    14,
                                    220
                            ),
                            Color.rgb(
                                    31,
                                    10,
                                    115
                            ),
                            Shader.TileMode.CLAMP
                    )
            );

            c.drawRect(
                    0,
                    top,
                    w,
                    h,
                    p
            );

            p.setShader(null);

            text.setTypeface(
                    Typeface.DEFAULT_BOLD
            );

            text.setTextAlign(
                    Paint.Align.CENTER
            );

            text.setTextSize(19);

            text.setColor(Color.WHITE);

            c.drawText(
                    "رنگ‌ها را به درستی مرتب کنید",
                    w / 2f,
                    top + 43,
                    text
            );

            text.setTextSize(12);

            text.setColor(
                    Color.rgb(
                            255,
                            218,
                            60
                    )
            );

            c.drawText(
                    "جوایز مرحله: ★ 40   ★★ 70   ★★★ 100 سکه",
                    w / 2f,
                    top + 73,
                    text
            );

            text.setTextSize(11);

            text.setColor(
                    Color.argb(
                            230,
                            220,
                            235,
                            255
                    )
            );

            c.drawText(
                    "مرحله " +
                            level +
                            " از 30   •   خطا: " +
                            mistakes +
                            "/" +
                            MAX_MISTAKES +
                            "   •   سکه: " +
                            coins,
                    w / 2f,
                    top + 96,
                    text
            );
        }

        /* =========================================================
           HINT
           ========================================================= */

        private void drawHint(Canvas c) {

            float[] a =
                    getTubeGeometry(
                            hintFrom
                    );

            float[] b =
                    getTubeGeometry(
                            hintTo
                    );

            if (a == null ||
                    b == null) {

                return;
            }

            stroke.setStyle(
                    Paint.Style.STROKE
            );

            stroke.setStrokeWidth(6);

            stroke.setColor(
                    Color.argb(
                            230,
                            255,
                            215,
                            30
                    )
            );

            c.drawLine(
                    a[0],
                    a[1],
                    b[0],
                    b[1],
                    stroke
            );

            p.setColor(
                    Color.rgb(
                            255,
                            215,
                            30
                    )
            );

            c.drawCircle(
                    b[0],
                    b[1],
                    9,
                    p
            );
        }

        /* =========================================================
           ERROR MARK
           ========================================================= */

        private void drawErrorMark(
                Canvas c,
                float w,
                float h
        ) {

            float[] geometry = null;

            if (errorTo >= 0) {
                geometry =
                        getTubeGeometry(
                                errorTo
                        );
            } else if (errorFrom >= 0) {
                geometry =
                        getTubeGeometry(
                                errorFrom
                        );
            }

            if (geometry == null) {
                return;
            }

            float x = geometry[0];

            float y =
                    geometry[1]
                            -
                            geometry[3] / 2f
                            -
                            20;

            long elapsed =
                    SystemClock.uptimeMillis()
                            - errorStart;

            float progress =
                    Math.min(
                            1f,
                            elapsed / 650f
                    );

            float alpha =
                    1f - progress;

            float radius =
                    18f +
                            progress * 7f;

            p.setColor(
                    Color.argb(
                            (int)
                                    (230 * alpha),
                            220,
                            25,
                            45
                    )
            );

            c.drawCircle(
                    x,
                    y,
                    radius,
                    p
            );

            stroke.setStyle(
                    Paint.Style.STROKE
            );

            stroke.setStrokeWidth(4);

            stroke.setStrokeCap(
                    Paint.Cap.ROUND
            );

            stroke.setColor(
                    Color.argb(
                            (int)
                                    (255 * alpha),
                            255,
                            255,
                            255
                    )
            );

            c.drawLine(
                    x - 7,
                    y - 7,
                    x + 7,
                    y + 7,
                    stroke
            );

            c.drawLine(
                    x + 7,
                    y - 7,
                    x - 7,
                    y + 7,
                    stroke
            );

            stroke.setStrokeCap(
                    Paint.Cap.BUTT
            );
        }

        /* =========================================================
           VICTORY
           ========================================================= */

        private void drawVictory(
                Canvas c,
                float w,
                float h
        ) {

            p.setColor(
                    Color.argb(
                            210,
                            4,
                            8,
                            45
                    )
            );

            c.drawRect(
                    0,
                    0,
                    w,
                    h,
                    p
            );

            p.setColor(
                    Color.argb(
                            85,
                            80,
                            20,
                            255
                    )
            );

            p.setShadowLayer(
                    30,
                    0,
                    0,
                    Color.MAGENTA
            );

            c.drawRoundRect(
                    new RectF(
                            28,
                            h / 2f - 170,
                            w - 28,
                            h / 2f + 175
                    ),
                    35,
                    35,
                    p
            );

            p.clearShadowLayer();

            drawVictoryParticles(
                    c,
                    w,
                    h
            );

            text.setTextAlign(
                    Paint.Align.CENTER
            );

            text.setTypeface(
                    Typeface.DEFAULT_BOLD
            );

            text.setColor(Color.WHITE);

            text.setTextSize(31);

            c.drawText(
                    "عالی! مرحله کامل شد",
                    w / 2f,
                    h / 2f - 98,
                    text
            );

            int stars =
                    calculateStars();

            text.setTextSize(38);

            text.setColor(
                    Color.rgb(
                            255,
                            210,
                            35
                    )
            );

            String starsText = "";

            for (int i = 0; i < 3; i++) {

                starsText +=
                        i < stars
                                ? "★"
                                : "☆";
            }

            c.drawText(
                    starsText,
                    w / 2f,
                    h / 2f - 38,
                    text
            );

            text.setTextSize(18);

            text.setColor(Color.WHITE);

            c.drawText(
                    "مرحله " +
                            level +
                            " کامل شد",
                    w / 2f,
                    h / 2f + 10,
                    text
            );

            text.setTextSize(15);

            text.setColor(
                    Color.rgb(
                            255,
                            225,
                            80
                    )
            );

            c.drawText(
                    "جایزه: +" +
                            lastEarnedCoins +
                            " سکه",
                    w / 2f,
                    h / 2f + 48,
                    text
            );

            text.setColor(
                    Color.rgb(
                            210,
                            230,
                            255
                    )
            );

            c.drawText(
                    "امتیاز کل: " +
                            coins +
                            " سکه",
                    w / 2f,
                    h / 2f + 78,
                    text
            );

            drawActionButton(
                    c,
                    w / 2f,
                    h / 2f + 122,
                    "ادامه"
            );
        }

        /* =========================================================
           FAILURE
           ========================================================= */

        private void drawFailure(
                Canvas c,
                float w,
                float h
        ) {

            p.setColor(
                    Color.argb(
                            220,
                            20,
                            4,
                            20
                    )
            );

            c.drawRect(
                    0,
                    0,
                    w,
                    h,
                    p
            );

            p.setColor(
                    Color.argb(
                            90,
                            210,
                            15,
                            45
                    )
            );

            p.setShadowLayer(
                    30,
                    0,
                    0,
                    Color.RED
            );

            c.drawRoundRect(
                    new RectF(
                            28,
                            h / 2f - 155,
                            w - 28,
                            h / 2f + 160
                    ),
                    35,
                    35,
                    p
            );

            p.clearShadowLayer();

            text.setTextAlign(
                    Paint.Align.CENTER
            );

            text.setTypeface(
                    Typeface.DEFAULT_BOLD
            );

            text.setColor(
                    Color.rgb(
                            255,
                            75,
                            90
                    )
            );

            text.setTextSize(30);

            c.drawText(
                    "مرحله تمام نشد",
                    w / 2f,
                    h / 2f - 85,
                    text
            );

            text.setTextSize(18);

            text.setColor(Color.WHITE);

            c.drawText(
                    "سه خطا انجام شد",
                    w / 2f,
                    h / 2f - 42,
                    text
            );

            text.setTextSize(15);

            text.setColor(
                    Color.rgb(
                            215,
                            225,
                            255
                    )
            );

            c.drawText(
                    "نگران نباش؛ مرحله دوباره از اول شروع می‌شود.",
                    w / 2f,
                    h / 2f - 5,
                    text
            );

            drawActionButton(
                    c,
                    w / 2f,
                    h / 2f + 65,
                    "دوباره تلاش کن"
            );
        }

        private void drawActionButton(
                Canvas c,
                float x,
                float y,
                String label
        ) {

            float width = 170;
            float height = 54;

            p.setShader(
                    new LinearGradient(
                            x - width / 2,
                            y - height / 2,
                            x + width / 2,
                            y + height / 2,
                            Color.rgb(
                                    0,
                                    180,
                                    255
                            ),
                            Color.rgb(
                                    123,
                                    34,
                                    245
                            ),
                            Shader.TileMode.CLAMP
                    )
            );

            p.setShadowLayer(
                    12,
                    0,
                    5,
                    Color.argb(
                            170,
                            0,
                            0,
                            0
                    )
            );

            c.drawRoundRect(
                    new RectF(
                            x - width / 2,
                            y - height / 2,
                            x + width / 2,
                            y + height / 2
                    ),
                    18,
                    18,
                    p
            );

            p.clearShadowLayer();

            p.setShader(null);

            stroke.setStyle(
                    Paint.Style.STROKE
            );

            stroke.setStrokeWidth(2);

            stroke.setColor(
                    Color.argb(
                            210,
                            255,
                            255,
                            255
                    )
            );

            c.drawRoundRect(
                    new RectF(
                            x - width / 2,
                            y - height / 2,
                            x + width / 2,
                            y + height / 2
                    ),
                    18,
                    18,
                    stroke
            );

            text.setTextAlign(
                    Paint.Align.CENTER
            );

            text.setTypeface(
                    Typeface.DEFAULT_BOLD
            );

            text.setTextSize(17);

            text.setColor(Color.WHITE);

            c.drawText(
                    label,
                    x,
                    y + 6,
                    text
            );
        }

        /* =========================================================
           VICTORY PARTICLES
           ========================================================= */

        private void prepareVictoryParticles() {

            for (int i = 0;
                 i < particleX.length;
                 i++) {

                particleX[i] =
                        sparkleRandom.nextFloat();

                particleY[i] =
                        sparkleRandom.nextFloat();

                particleSpeed[i] =
                        18f +
                                sparkleRandom.nextFloat()
                                        * 55f;

                particleSize[i] =
                        2f +
                                sparkleRandom.nextFloat()
                                        * 4f;

                particlePhase[i] =
                        sparkleRandom.nextFloat()
                                        * 6.28f;
            }
        }

        private void drawVictoryParticles(
                Canvas c,
                float w,
                float h
        ) {

            long now =
                    SystemClock.uptimeMillis();

            float age =
                    victoryStart <= 0
                            ? 0f
                            : (
                            now -
                                    victoryStart
                    ) / 1000f;

            for (int i = 0;
                 i < particleX.length;
                 i++) {

                float x =
                        particleX[i] *
                                w
                                +
                                (float)
                                        Math.sin(
                                                age * 1.4f +
                                                        particlePhase[i]
                                        )
                                        * 14f;

                float y =
                        h * .18f
                                +
                                (
                                        particleY[i]
                                                *
                                                h * .58f
                                )
                                +
                                age *
                                        particleSpeed[i];

                y %= h * .72f;

                y += h * .15f;

                float alpha =
                        190f *
                                Math.max(
                                        0f,
                                        1f -
                                                age / 3.8f
                                );

                p.setColor(
                        Color.argb(
                                (int) alpha,
                                255,
                                190 +
                                        (i % 3) *
                                                20,
                                55
                        )
                );

                c.drawCircle(
                        x,
                        y,
                        particleSize[i],
                        p
                );
            }
        }

        /* =========================================================
           STARS / REWARD
           ========================================================= */

        private int calculateStars() {

            int perfect =
                    getColorCount() * 3;

            if (moves <= perfect) {
                return 3;
            }

            if (moves <= perfect + 7) {
                return 2;
            }

            return 1;
        }

        private int calculateReward(
                int stars
        ) {

            if (stars == 3) {
                return 100;
            }

            if (stars == 2) {
                return 70;
            }

            return 40;
        }

        /* =========================================================
           PANELS / BUTTONS
           ========================================================= */

        private void drawRoundPanel(
                Canvas c,
                float left,
                float top,
                float right,
                float bottom,
                int color1,
                int color2
        ) {

            if (right <= left ||
                    bottom <= top) {
                return;
            }

            p.setStyle(
                    Paint.Style.FILL
            );

            p.setShader(
                    new LinearGradient(
                            left,
                            top,
                            right,
                            bottom,
                            color1,
                            color2,
                            Shader.TileMode.CLAMP
                    )
            );

            p.setShadowLayer(
                    10,
                    0,
                    5,
                    Color.argb(
                            150,
                            0,
                            0,
                            0
                    )
            );

            c.drawRoundRect(
                    new RectF(
                            left,
                            top,
                            right,
                            bottom
                    ),
                    20,
                    20,
                    p
            );

            p.clearShadowLayer();

            p.setShader(null);

            stroke.setStyle(
                    Paint.Style.STROKE
            );

            stroke.setStrokeWidth(2);

            stroke.setColor(
                    Color.argb(
                            180,
                            255,
                            255,
                            255
                    )
            );

            c.drawRoundRect(
                    new RectF(
                            left,
                            top,
                            right,
                            bottom
                    ),
                    20,
                    20,
                    stroke
            );
        }

        private void drawCircleButton(
                Canvas c,
                float x,
                float y,
                float radius,
                int color,
                String symbol
        ) {

            p.setColor(color);

            p.setShadowLayer(
                    12,
                    0,
                    6,
                    Color.argb(
                            160,
                            0,
                            0,
                            0
                    )
            );

            c.drawCircle(
                    x,
                    y,
                    radius,
                    p
            );

            p.clearShadowLayer();

            stroke.setStyle(
                    Paint.Style.STROKE
            );

            stroke.setStrokeWidth(2);

            stroke.setColor(
                    Color.argb(
                            190,
                            255,
                            255,
                            255
                    )
            );

            c.drawCircle(
                    x,
                    y,
                    radius - 2,
                    stroke
            );

            text.setTextAlign(
                    Paint.Align.CENTER
            );

            text.setTypeface(
                    Typeface.DEFAULT_BOLD
            );

            text.setTextSize(29);

            text.setColor(Color.WHITE);

            c.drawText(
                    symbol,
                    x,
                    y + 10,
                    text
            );
        }

        private void drawBadgeCircle(
                Canvas c,
                float x,
                float y,
                String value
        ) {

            p.setColor(
                    Color.rgb(
                            245,
                            24,
                            65
                    )
            );

            c.drawCircle(
                    x,
                    y,
                    16,
                    p
            );

            text.setTextAlign(
                    Paint.Align.CENTER
            );

            text.setTextSize(13);

            text.setTypeface(
                    Typeface.DEFAULT_BOLD
            );

            text.setColor(Color.WHITE);

            c.drawText(
                    value,
                    x,
                    y + 5,
                    text
            );
        }

        private void drawCoin(
                Canvas c,
                float x,
                float y
        ) {

            p.setColor(
                    Color.rgb(
                            255,
                            193,
                            15
                    )
            );

            p.setShadowLayer(
                    7,
                    0,
                    2,
                    Color.rgb(
                            255,
                            120,
                            0
                    )
            );

            c.drawCircle(
                    x,
                    y,
                    19,
                    p
            );

            p.clearShadowLayer();

            stroke.setStyle(
                    Paint.Style.STROKE
            );

            stroke.setStrokeWidth(2);

            stroke.setColor(
                    Color.rgb(
                            255,
                            235,
                            100
                    )
            );

            c.drawCircle(
                    x,
                    y,
                    15,
                    stroke
            );

            text.setTextAlign(
                    Paint.Align.CENTER
            );

            text.setTextSize(18);

            text.setColor(
                    Color.rgb(
                            160,
                            91,
                            0
                    )
            );

            text.setTypeface(
                    Typeface.DEFAULT_BOLD
            );

            c.drawText(
                    "$",
                    x,
                    y + 6,
                    text
            );
        }

        private void drawStar(
                Canvas c,
                float cx,
                float cy,
                float radius,
                int color
        ) {

            p.setColor(color);

            p.setShadowLayer(
                    8,
                    0,
                    2,
                    Color.rgb(
                            255,
                            125,
                            0
                    )
            );

            Path path =
                    new Path();

            for (int i = 0;
                 i < 10;
                 i++) {

                double angle =
                        -Math.PI / 2
                                +
                                i *
                                        Math.PI /
                                                5;

                float r =
                        i % 2 == 0
                                ? radius
                                : radius * .42f;

                float x =
                        cx +
                                (float)
                                        Math.cos(angle)
                                        * r;

                float y =
                        cy +
                                (float)
                                        Math.sin(angle)
                                        * r;

                if (i == 0) {
                    path.moveTo(
                            x,
                            y
                    );
                } else {
                    path.lineTo(
                            x,
                            y
                    );
                }
            }

            path.close();

            c.drawPath(
                    path,
                    p
            );

            p.clearShadowLayer();
        }

        /* =========================================================
           COLOR HELPERS
           ========================================================= */

        private int lighten(
                int color,
                float factor
        ) {

            return Color.rgb(
                    Math.min(
                            255,
                            (int)
                                    (
                                            Color.red(color)
                                                    * factor
                                    )
                    ),
                    Math.min(
                            255,
                            (int)
                                    (
                                            Color.green(color)
                                                    * factor
                                    )
                    ),
                    Math.min(
                            255,
                            (int)
                                    (
                                            Color.blue(color)
                                                    * factor
                                    )
                    )
            );
        }

        private int darken(
                int color,
                float factor
        ) {

            return Color.rgb(
                    Math.max(
                            0,
                            (int)
                                    (
                                            Color.red(color)
                                                    * factor
                                    )
                    ),
                    Math.max(
                            0,
                            (int)
                                    (
                                            Color.green(color)
                                                    * factor
                                    )
                    ),
                    Math.max(
                            0,
                            (int)
                                    (
                                            Color.blue(color)
                                                    * factor
                                    )
                    )
            );
        }

        /* =========================================================
           TOUCH
           ========================================================= */

        @Override
        public boolean onTouchEvent(
                MotionEvent event
        ) {

            if (event.getAction() !=
                    MotionEvent.ACTION_DOWN) {

                return true;
            }

            float x =
                    event.getX();

            float y =
                    event.getY();

            float h =
                    getHeight();

            float w =
                    getWidth();

            /* ---------------------------------
               FAILURE SCREEN
               --------------------------------- */

            if (levelFailed) {

                float retryY =
                        h / 2f + 65;

                if (
                        Math.abs(
                                x - w / 2f
                        ) < 100
                                &&
                                Math.abs(
                                        y - retryY
                                ) < 40
                ) {

                    playGameSound(1);

                    resetLevel();

                    return true;
                }

                return true;
            }

            /* ---------------------------------
               VICTORY SCREEN
               --------------------------------- */

            if (levelFinished) {

                float continueY =
                        h / 2f + 122;

                if (
                        Math.abs(
                                x - w / 2f
                        ) < 105
                                &&
                                Math.abs(
                                        y - continueY
                                ) < 42
                ) {

                    advanceAfterVictory();

                    return true;
                }

                return true;
            }

            /* ---------------------------------
               CONTROLS
               --------------------------------- */

            float controlY =
                    h - 205;

            if (
                    distance(
                            x,
                            y,
                            75,
                            controlY
                    ) < 65
            ) {

                undoMove();

                return true;
            }

            if (
                    distance(
                            x,
                            y,
                            w / 2f - 105,
                            controlY
                    ) < 65
            ) {

                showHint();

                playGameSound(1);

                return true;
            }

            if (
                    distance(
                            x,
                            y,
                            w - 75,
                            controlY
                    ) < 65
            ) {

                soundEnabled =
                        !soundEnabled;

                saveProgress();

                if (soundEnabled) {
                    playGameSound(1);
                }

                invalidate();

                return true;
            }

            /* ---------------------------------
               TUBE
               --------------------------------- */

            int tube =
                    findTube(
                            x,
                            y
                    );

            if (tube >= 0) {

                handleTubeClick(
                        tube
                );
            }

            return true;
        }

        private float distance(
                float x1,
                float y1,
                float x2,
                float y2
        ) {

            float dx =
                    x1 - x2;

            float dy =
                    y1 - y2;

            return (float)
                    Math.sqrt(
                            dx * dx +
                                    dy * dy
                    );
        }

        private int findTube(
                float x,
                float y
        ) {

            for (int i = 0;
                 i < tubes.size();
                 i++) {

                float[] geometry =
                        getTubeGeometry(i);

                float left =
                        geometry[0]
                                -
                                geometry[2] / 2
                                -
                                18;

                float right =
                        geometry[0]
                                +
                                geometry[2] / 2
                                +
                                18;

                float top =
                        geometry[1]
                                -
                                geometry[3] / 2
                                -
                                20;

                float bottom =
                        geometry[1]
                                +
                                geometry[3] / 2
                                +
                                30;

                if (
                        x >= left &&
                                x <= right &&
                                y >= top &&
                                y <= bottom
                ) {

                    return i;
                }
            }

            return -1;
        }

        /* =========================================================
           TUBE CLICK
           ========================================================= */

        private void handleTubeClick(
                int index
        ) {

            if (animProgress < 1f) {
                return;
            }

            if (levelFinished ||
                    levelFailed) {

                return;
            }

            if (selectedTube == -1) {

                if (
                        tubes
                                .get(index)
                                .isEmpty()
                ) {

                    playGameSound(4);

                    showError(
                            index,
                            -1,
                            false
                    );

                    return;
                }

                selectedTube =
                        index;

                playGameSound(1);

                invalidate();

                return;
            }

            if (selectedTube == index) {

                selectedTube = -1;

                invalidate();

                return;
            }

            if (
                    canMove(
                            selectedTube,
                            index
                    )
            ) {

                int from =
                        selectedTube;

                saveMove(
                        from,
                        index
                );

                beginAnimatedMove(
                        from,
                        index
                );

                moves++;

                selectedTube = -1;

            } else {

                showError(
                        selectedTube,
                        index,
                        true
                );

                selectedTube = -1;
            }
        }

        /* =========================================================
           ERROR
           ========================================================= */

        private void showError(
                int from,
                int to,
                boolean countMistake
        ) {

            errorFrom = from;
            errorTo = to;

            if (countMistake) {

                mistakes++;

                errorStrength =
                        mistakes >= 2
                                ? 2
                                : 1;

                playGameSound(4);

                errorStart =
                        SystemClock.uptimeMillis();

                invalidate();

                if (mistakes >= MAX_MISTAKES) {

                    handler.postDelayed(
                            new Runnable() {

                                @Override
                                public void run() {

                                    if (
                                            mistakes >=
                                                    MAX_MISTAKES
                                                    &&
                                                    !levelFinished
                                    ) {

                                        failLevel();
                                    }
                                }
                            },
                            700
                    );
                }

            } else {

                errorStrength = 1;

                errorStart =
                        SystemClock.uptimeMillis();

                invalidate();
            }
        }

        /* =========================================================
           MOVE VALIDATION
           ========================================================= */

        private boolean canMove(
                int from,
                int to
        ) {

            if (
                    from < 0 ||
                            to < 0 ||
                            from >= tubes.size() ||
                            to >= tubes.size()
            ) {

                return false;
            }

            List<Integer> source =
                    tubes.get(from);

            List<Integer> target =
                    tubes.get(to);

            if (
                    source.isEmpty() ||
                            target.size() >= CAPACITY
            ) {

                return false;
            }

            int color =
                    source.get(
                            source.size() - 1
                    );

            return target.isEmpty()
                    ||
                    target.get(
                            target.size() - 1
                    ) == color;
        }

        /* =========================================================
           ANIMATED MOVE
           ========================================================= */

        private void beginAnimatedMove(
                int from,
                int to
        ) {

            List<Integer> source =
                    tubes.get(from);

            animFrom = from;
            animTo = to;

            animColor =
                    source.get(
                            source.size() - 1
                    );

            animAmount =
                    Math.min(
                            getTopSameCount(
                                    source
                            ),
                            CAPACITY -
                                    tubes
                                            .get(to)
                                            .size()
                    );

            animProgress = 0f;

            animStart =
                    SystemClock.uptimeMillis();

            playGameSound(2);

            invalidate();
        }

        private void finishAnimatedMove() {

            if (
                    animFrom < 0 ||
                            animTo < 0
            ) {

                return;
            }

            moveColor(
                    animFrom,
                    animTo
            );

            animFrom = -1;
            animTo = -1;
            animColor = -1;
            animAmount = 0;
            animProgress = 1f;

            if (isLevelComplete()) {

                completeLevel();

            } else {

                playGameSound(1);
            }

            saveProgress();

            invalidate();
        }

        private void moveColor(
                int from,
                int to
        ) {

            List<Integer> source =
                    tubes.get(from);

            List<Integer> target =
                    tubes.get(to);

            if (source.isEmpty()) {
                return;
            }

            int color =
                    source.get(
                            source.size() - 1
                    );

            while (
                    !source.isEmpty()
                            &&
                            target.size() < CAPACITY
                            &&
                            source.get(
                                    source.size() - 1
                            ) == color
            ) {

                target.add(
                        source.remove(
                                source.size() - 1
                        )
                );
            }
        }

        /* =========================================================
           UNDO
           ========================================================= */

        void undoMove() {

            if (
                    levelFinished ||
                            levelFailed ||
                            animProgress < 1f
            ) {

                return;
            }

            if (history.isEmpty()) {

                Toast.makeText(
                        ColorSortActivity.this,
                        "حرکتی برای برگشت وجود ندارد",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            Move move =
                    history.remove(
                            history.size() - 1
                    );

            List<Integer> source =
                    tubes.get(move.to);

            List<Integer> target =
                    tubes.get(move.from);

            for (
                    int i = 0;
                    i < move.amount;
                    i++
            ) {

                if (!source.isEmpty()) {

                    target.add(
                            source.remove(
                                    source.size() - 1
                            )
                    );
                }
            }

            moves =
                    Math.max(
                            0,
                            moves - 1
                    );

            playGameSound(1);

            invalidate();
        }

        /* =========================================================
           SAVE MOVE
           ========================================================= */

        private void saveMove(
                int from,
                int to
        ) {

            List<Integer> source =
                    tubes.get(from);

            if (source.isEmpty()) {
                return;
            }

            int color =
                    source.get(
                            source.size() - 1
                    );

            int amount =
                    Math.min(
                            getTopSameCount(
                                    source
                            ),
                            CAPACITY -
                                    tubes
                                            .get(to)
                                            .size()
                    );

            history.add(
                    new Move(
                            from,
                            to,
                            color,
                            amount
                    )
            );

            if (history.size() > 100) {
                history.remove(0);
            }
        }

        /* =========================================================
           HINT
           ========================================================= */

        void showHint() {

            if (
                    levelFinished ||
                            levelFailed ||
                            animProgress < 1f
            ) {

                return;
            }

            for (
                    int from = 0;
                    from < tubes.size();
                    from++
            ) {

                if (
                        tubes.get(from).isEmpty()
                ) {

                    continue;
                }

                for (
                        int to = 0;
                        to < tubes.size();
                        to++
                ) {

                    if (
                            from != to &&
                                    canMove(
                                            from,
                                            to
                                    )
                    ) {

                        hintFrom = from;
                        hintTo = to;

                        showingHint = true;

                        hintUntil =
                                System.currentTimeMillis()
                                        + 1800;

                        invalidate();

                        return;
                    }
                }
            }

            Toast.makeText(
                    ColorSortActivity.this,
                    "فعلاً حرکت مناسبی پیدا نشد",
                    Toast.LENGTH_SHORT
            ).show();
        }

        /* =========================================================
           COMPLETE LEVEL
           ========================================================= */

        private boolean isLevelComplete() {

            for (
                    List<Integer> tube :
                    tubes
            ) {

                if (tube.isEmpty()) {
                    continue;
                }

                if (tube.size() != CAPACITY) {
                    return false;
                }

                int first =
                        tube.get(0);

                for (
                        int i = 1;
                        i < tube.size();
                        i++
                ) {

                    if (
                            tube.get(i) != first
                    ) {

                        return false;
                    }
                }
            }

            return true;
        }

        private void completeLevel() {

            if (levelFinished ||
                    levelFailed) {

                return;
            }

            levelFinished = true;

            selectedTube = -1;

            int stars =
                    calculateStars();

            lastEarnedCoins =
                    calculateReward(stars);

            coins +=
                    lastEarnedCoins;

            totalStars +=
                    stars;

            victoryStart =
                    SystemClock.uptimeMillis();

            /*
             * فقط یک بار صدای جشن را اجرا می‌کنیم.
             * این صدا حدود ۳.۴ ثانیه طول دارد.
             */
            playGameSound(3);

            saveProgress();

            invalidate();

            handler.postDelayed(
                    new Runnable() {

                        @Override
                        public void run() {

                            if (!levelFinished) {
                                return;
                            }

                            advanceAfterVictory();
                        }
                    },
                    3600
            );
        }

        private void advanceAfterVictory() {

            if (!levelFinished) {
                return;
            }

            levelFinished = false;

            if (level < MAX_LEVEL) {

                level++;

                saveProgress();

                resetLevel();

            } else {

                Toast.makeText(
                        ColorSortActivity.this,
                        "🏆 هر ۳۰ مرحله را کامل کردی!",
                        Toast.LENGTH_LONG
                ).show();

                level = 1;

                saveProgress();

                resetLevel();
            }
        }

        /* =========================================================
           FAILURE
           ========================================================= */

        private void failLevel() {

            if (
                    levelFinished ||
                            levelFailed
            ) {

                return;
            }

            levelFailed = true;

            selectedTube = -1;

            failureStart =
                    SystemClock.uptimeMillis();

            stopActiveSound();

            playGameSound(4);

            invalidate();
        }

        /* =========================================================
           MOVE OBJECT
           ========================================================= */

        class Move {

            int from;
            int to;
            int color;
            int amount;

            Move(
                    int from,
                    int to,
                    int color,
                    int amount
            ) {

                this.from = from;
                this.to = to;
                this.color = color;
                this.amount = amount;
            }
        }
    }
                                     }
