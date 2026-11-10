package com.tajro.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

public class BallGameActivity extends Activity {

    private ProBounceView gameView;

    // متن‌های بازی بر اساس زبان انتخاب‌شده در برنامه
    private String tr(String fa, String en, String ps, String ur, String hi, String ar) {
        String language = LanguageManager.getLanguage(this);
        switch (language) {
            case "en": return en;
            case "ps": return ps;
            case "ur": return ur;
            case "hi": return hi;
            case "ar": return ar;
            default: return fa;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        gameView = new ProBounceView(this);
        setContentView(gameView);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (gameView != null) {
            gameView.saveProgress();
            gameView.stopGame();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (gameView != null) {
            gameView.startGame();
        }
    }

    public class ProBounceView extends View {

        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path shape = new Path();
        private final Random random = new Random(1987);

        private final ArrayList<RectF> platforms = new ArrayList<>();
        private final ArrayList<RectF> walls = new ArrayList<>();
        private final ArrayList<RectF> spikes = new ArrayList<>();
        private final ArrayList<RectF> rings = new ArrayList<>();
        private final ArrayList<RectF> springs = new ArrayList<>();
        private final ArrayList<MovingObstacle> movingObstacles = new ArrayList<>();
        private final ArrayList<MovingGate> movingGates = new ArrayList<>();
        private final ArrayList<Particle> particles = new ArrayList<>();

        private RectF finish;
        private RectF finishRing; // حلقه بزرگ آبی دروازه پایان مرحله

        private float ballX, ballY, ballVX, ballVY;
        private float spawnX, spawnY;
        private float cameraX, cameraY;
        private float groundY;
        private float worldWidth, worldHeight;
        private float shake, time;

        private boolean leftPressed, rightPressed, jumpPressed;
        private boolean running, finished, viewReady;
        private boolean doveFlying, doveLanded;

        private int level = 1;
        private int score = 0;
        private int collectedRings = 0;

        // سکه‌های دائمی بازیکن و جایزه مرحله جاری
        private int coins = 0;
        private int lastLevelReward = 0;

        private long lastFrame;
        private long finishStartTime;

        private float doveX, doveY;
        private float doveStartX, doveStartY;
        private float doveTargetX, doveTargetY;

        private SharedPreferences preferences;
        private ToneGenerator tone;

        private final float gravity = 1550f;
        private final float maxSpeed = 700f;
        private final float acceleration = 3000f;
        private final float friction = 0.80f;
        private final float jumpPower = 1060f;
        private final float highBounce =1350f;

        private final int[] skyTop = {
                0xFF48C8E8,
                0xFF43B8E8,
                0xFF4658A8,
                0xFF38B8D6,
                0xFFEF8148,
                0xFF10183E,
                0xFF42B79C,
                0xFFEC7650,
                0xFF63C5E7,
                0xFFE5A83F
        };

        private final int[] skyBottom = {
                0xFFB9F5FF,
                0xFF75D8F2,
                0xFF171A5D,
                0xFF0A7897,
                0xFFF0A32F,
                0xFF080D2A,
                0xFFB6E57B,
                0xFF47202B,
                0xFF75CFF0,
                0xFF774016
        };

        public ProBounceView(Context context) {
            super(context);

            preferences = context.getSharedPreferences(
                    "tajro_bounce_progress",
                    Context.MODE_PRIVATE
            );

            level = preferences.getInt("saved_level", 1);
            coins = preferences.getInt("saved_coins", 0);

            if (coins < 0) coins = 0;
            if (level < 1) level = 1;
            if (level > 10) level = 10;

            p.setTypeface(
                    Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            );

            setFocusable(true);

            try {
                tone = new ToneGenerator(
                        AudioManager.STREAM_MUSIC,
                        70
                );
            } catch (Exception ignored) {
                tone = null;
            }
        }

        @Override
        protected void onSizeChanged(
                int w,
                int h,
                int oldw,
                int oldh
        ) {
            super.onSizeChanged(w, h, oldw, oldh);

            if (w > 0 && h > 0) {
                viewReady = true;
                resetLevel();
                startGame();
            }
        }

        private float tile() {
            return Math.max(
                    42f,
                    Math.min(58f, getWidth() * 0.075f)
            );
        }

        // توپ عمداً بزرگ‌تر شده است.
        private float radius() {
            return tile() * 0.62f;
        }

        private void resetLevel() {

            if (getWidth() <= 0 || getHeight() <= 0) {
                return;
            }

            platforms.clear();
            walls.clear();
            spikes.clear();
            rings.clear();
            collectedRings = 0;
            springs.clear();
            movingObstacles.clear();
            movingGates.clear();
            particles.clear();

            finished = false;
            doveFlying = false;
            doveLanded = false;

            leftPressed = false;
            rightPressed = false;
            jumpPressed = false;

            shake = 0;
            time = 0;

            float t = tile();

            /*
             * طول مراحل افزایش بسیار زیادی دارد.
             *
             * مرحله 1 تقریباً 50,000 پیکسل
             * مرحله 2 تقریباً 64,000 پیکسل
             * مرحله 10 تقریباً 79,000 پیکسل
             *
             * بنابراین حتی با سرعت زیاد، مسیر کوتاه و ساده نیست.
             */
            float progress = (level - 1) / 9f;

            worldWidth =
                    50000f +
                    progress * 29000f;

            // مرحله اول دقیقاً حدود ۵۰٪ کوتاه‌تر می‌شود.
            if (level == 1) {
                worldWidth = 25000f;
            }

            if (level == 2) {
                worldWidth = 65000f;
            }

            if (level == 10) {
                worldWidth = 79000f;
            }

            worldHeight = Math.max(
                    getHeight() * 4.2f,
                    2500f
            );

            groundY = getHeight() * 0.68f;

            spawnX = t * 3f;
            spawnY = groundY - radius();

            ballX = spawnX;
            ballY = spawnY;

            ballVX = 0;
            ballVY = 0;

            cameraX = 0;
            cameraY = 0;

            buildWorld();
            createInitialParticles();

            /*
             * شروع بازی بدون تأخیر مصنوعی.
             */
            playStartSound();

            invalidate();
        }

        private void buildWorld() {

            float t = tile();

            float x = t * 0.5f;
            float y = groundY;

            int section = 0;

            while (x < worldWidth - t * 10f) {

                section++;

                int length;

                if (level <= 3) {
                    length =
                            8 +
                            (section * 5 + level * 2) % 10;
                } else if (level <= 6) {
                    length =
                            7 +
                            (section * 7 + level * 3) % 9;
                } else {
                    length =
                            6 +
                            (section * 9 + level * 4) % 8;
                }

                float left = x;
                float right = x + length * t;

                /*
                 * زمین اصلی خشتی.
                 */
                platforms.add(
                        new RectF(
                                left,
                                y,
                                right,
                                y + t * 1.45f
                        )
                );

                /*
                 * سکوهای بالایی.
                 */
                if (section % 2 == 0) {

                    float upperY =
                            y -
                            t * (
                                    1.8f +
                                    (section % 4) * 0.35f
                            );

                    float upperLeft =
                            left + t * 1.2f;

                    float upperRight =
                            Math.min(
                                    right - t * 0.5f,
                                    upperLeft +
                                            t * (
                                                    3.0f +
                                                    (section % 4)
                                            )
                            );

                    if (upperRight >
                            upperLeft + t * 1.7f) {

                        platforms.add(
                                new RectF(
                                        upperLeft,
                                        upperY,
                                        upperRight,
                                        upperY +
                                                t * 0.78f
                                )
                        );
                    }
                }

                /*
                 * پله‌های بیشتر.
                 */
                if (section % 4 == 0) {

                    float stepX =
                            left + t * 1.5f;

                    for (int s = 0; s < 4; s++) {

                        float stepY =
                                y -
                                t * (
                                        0.65f +
                                        s * 0.72f
                                );

                        platforms.add(
                                new RectF(
                                        stepX +
                                                s * t * 1.9f,
                                        stepY,
                                        stepX +
                                                s * t * 1.9f +
                                                t * 2.3f,
                                        stepY +
                                                t * 0.72f
                                )
                        );
                    }
                }

                /*
                 * گودی‌های عمیق.
                 */
                if (section > 3 &&
                        section % 5 == 0) {

                    float pitX =
                            left + t * 2.4f;

                    float pitWidth =
                            t * (
                                    3.5f +
                                    Math.min(
                                            4,
                                            level * 0.35f
                                    )
                            );

                    /*
                     * دو دیواره طرفین گودی.
                     */
                    walls.add(
                            new RectF(
                                    pitX,
                                    y - t * 0.3f,
                                    pitX + t * 0.75f,
                                    y + t * 2.4f
                            )
                    );

                    walls.add(
                            new RectF(
                                    pitX + pitWidth,
                                    y - t * 0.3f,
                                    pitX + pitWidth +
                                            t * 0.75f,
                                    y + t * 2.4f
                            )
                    );

                    /*
                     * کف پایین گودی.
                     */
                    platforms.add(
                            new RectF(
                                    pitX + t * 0.75f,
                                    y + t * 2.15f,
                                    pitX + pitWidth,
                                    y + t * 2.75f
                            )
                    );
                }

                addWorldObjects(
                        left,
                        right,
                        y,
                        section,
                        t
                );

                // دروازه‌های کشویی در مراحل ۳ به بعد؛ با باز و بسته‌شدن، راه عبور زمان‌بندی‌شده می‌سازند.
                if (level >= 3 && section % 7 == 0) {
                    float gateX = left + (right - left) * 0.48f;
                    float gateY = y - t * (2.25f + (section % 3) * 0.25f);
                    movingGates.add(new MovingGate(
                            gateX, gateY, t * 0.72f, t * 2.15f,
                            0.75f + level * 0.035f, section % 2 == 0
                    ));
                }

                /*
                 * دیوارهای خشتی بلند.
                 */
                if (section > 2 &&
                        section % 3 == 0) {

                    float wallX =
                            left +
                                    t *
                                    (
                                            3.0f +
                                            (section % 3)
                                    );

                    float wallHeight =
                            t *
                            (
                                    1.8f +
                                    (section % 5) * 0.55f +
                                    level * 0.08f
                            );

                    walls.add(
                            new RectF(
                                    wallX,
                                    y - wallHeight,
                                    wallX + t * 1.10f,
                                    y
                            )
                    );

                    /*
                     * حفره/راه عبور در کنار دیوار.
                     */
                    if (section % 6 == 0) {

                        walls.add(
                                new RectF(
                                        wallX + t * 3.1f,
                                        y - t * 1.2f,
                                        wallX + t * 4.1f,
                                        y
                                )
                        );
                    }
                }

                /*
                 * تونل‌های خشتی بزرگ.
                 */
                if (level >= 4 &&
                        section % 8 == 0) {

                    float tunnelX =
                            left + t * 1.5f;

                    float tunnelW =
                            t *
                            (
                                    7f +
                                    Math.min(
                                            5f,
                                            level * 0.45f
                                    )
                            );

                    float tunnelTop =
                            y -
                            t *
                            (
                                    3.0f +
                                    (section % 3) * 0.45f
                            );

                    walls.add(
                            new RectF(
                                    tunnelX,
                                    tunnelTop,
                                    tunnelX + t * 1.1f,
                                    y
                            )
                    );

                    walls.add(
                            new RectF(
                                    tunnelX + tunnelW,
                                    tunnelTop,
                                    tunnelX + tunnelW +
                                            t * 1.1f,
                                    y
                            )
                    );

                    platforms.add(
                            new RectF(
                                    tunnelX,
                                    tunnelTop - t * 0.85f,
                                    tunnelX + tunnelW +
                                            t * 1.1f,
                                    tunnelTop
                            )
                    );
                }

                /*
                 * در مراحل بالاتر دیوارها بیشتر می‌شوند.
                 */
                if (level >= 7 &&
                        section % 6 == 0) {

                    float blockX =
                            left + t * 4.0f;

                    for (int b = 0; b < 3; b++) {

                        walls.add(
                                new RectF(
                                        blockX +
                                                b * t * 1.7f,
                                        y -
                                                t *
                                                (
                                                        1.4f +
                                                        (b % 2) *
                                                        1.0f
                                                ),
                                        blockX +
                                                b * t * 1.7f +
                                                t * 0.9f,
                                        y
                                )
                        );
                    }
                }

                x = right;

                /*
                 * تغییر ارتفاع مسیر.
                 */
                if (section % 4 == 0) {

                    float change =
                            t *
                            (
                                    0.75f +
                                    level * 0.11f
                            );

                    if ((section / 4) % 2 == 0) {
                        y -= change;
                    } else {
                        y += change;
                    }

                    if (y < getHeight() * 0.31f) {
                        y = getHeight() * 0.37f;
                    }

                    if (y > getHeight() * 0.68f) {
                        y = getHeight() * 0.59f;
                    }

                    platforms.add(
                            new RectF(
                                    x,
                                    y,
                                    x + t * 3.2f,
                                    y + t * 0.95f
                            )
                    );

                    x += t * 3.2f;
                }
            }

            float finishY = y;

            /*
             * منطقه پایان.
             */
            platforms.add(
                    new RectF(
                            worldWidth - t * 20f,
                            finishY,
                            worldWidth - t * 11f,
                            finishY + t * 1.45f
                    )
            );

            platforms.add(
                    new RectF(
                            worldWidth - t * 11f,
                            finishY - t * 1.6f,
                            worldWidth - t * 6f,
                            finishY - t * 0.5f
                    )
            );

            finish =
                    new RectF(
                            worldWidth - t * 5.2f,
                            finishY - t * 2.4f,
                            worldWidth - t * 1.5f,
                            finishY
                    );

            // حلقه بزرگ و درخشان آبی؛ توپ باید از داخل آن عبور کند.
            float portalSize = t * 2.75f;
            float portalCenterX = finish.left + t * 1.25f;
            float portalCenterY = finish.top - t * 0.95f;
            finishRing = new RectF(
                    portalCenterX - portalSize / 2f,
                    portalCenterY - portalSize / 2f,
                    portalCenterX + portalSize / 2f,
                    portalCenterY + portalSize / 2f
            );
        }

        private void addWorldObjects(
                float left,
                float right,
                float y,
                int section,
                float t
        ) {

            /*
             * موانع نوک‌تیز.
             */
            int spikeEvery =
                    level <= 3
                            ? 8
                            : level <= 6
                            ? 6
                            : 5;

            if (section > 3 &&
                    section % spikeEvery == 0) {

                float sx =
                        left +
                                (right - left) *
                                        (0.45f +
                                                (section % 3) *
                                                        0.12f);

                int count =
                        level >= 7
                                ? 2 + section % 3
                                : 1 + section % 2;

                for (int i = 0; i < count; i++) {

                    spikes.add(
                            new RectF(
                                    sx + i * t * 0.82f,
                                    y - t * 0.78f,
                                    sx +
                                            i * t * 0.82f +
                                            t * 0.82f,
                                    y
                            )
                    );
                }
            }

            /*
             * حلقه‌های بیشتر.
             */
            if (section % 2 == 0 ||
                    (level >= 7 &&
                            section % 3 == 0)) {

                float rx =
                        left +
                                (right - left) * 0.70f;

                float ry =
                        y -
                                t *
                                (
                                        1.9f +
                                        (section % 4) * 0.35f
                                );

                rings.add(
                        new RectF(
                                rx,
                                ry,
                                rx + t * 1.85f,
                                ry + t * 1.85f
                        )
                );

                if (level >= 6 &&
                        section % 5 == 0) {

                    rings.add(
                            new RectF(
                                    rx + t * 1.8f,
                                    ry - t * 1.1f,
                                    rx + t * 3.25f,
                                    ry + t * 0.35f
                            )
                    );
                }
            }

            /*
             * پرتاب‌کننده‌های بیشتر.
             */
            int springEvery =
                    level <= 3 ? 4 :
                    level <= 6 ? 3 : 2;

            if (section % springEvery == 0) {

                // پرتاب‌کننده را از زیر دیوار بیرون می‌آوریم تا کاملاً دیده و قابل لمس باشد.
                float sx = Math.min(
                        left + (right - left) * 0.72f + t * 0.45f,
                        right - t * 1.45f
                );
                float springTop = y - t * 0.72f;

                springs.add(
                        new RectF(
                                sx,
                                springTop,
                                sx + t * 1.15f,
                                y
                        )
                );

                /*
                 * در مراحل بالاتر دو پرتاب‌کننده.
                 */
                if (level >= 6 &&
                        section % 4 == 0) {

                    springs.add(
                            new RectF(
                                    sx + t * 2.0f,
                                    y - t * 0.52f,
                                    sx + t * 3.15f,
                                    y
                            )
                    );
                }
            }

            /*
             * موانع متحرک/چرخشی.
             */
            if (level >= 3 &&
                    section % 5 == 0) {

                float ox =
                        left +
                                (right - left) * 0.50f;

                float oy =
                        y -
                                t *
                                (
                                        1.5f +
                                        (section % 3) * 0.35f
                                );

                movingObstacles.add(
                        new MovingObstacle(
                                ox,
                                oy,
                                t * 0.58f,
                                65f +
                                        level * 8f,
                                section % 2 == 0
                        )
                );
            }

            /*
             * ابزار ویژه در مراحل بالا.
             */
            if (level >= 5 &&
                    section % 9 == 0) {

                float bx =
                        left + t * 4.0f;

                float by =
                        y - t * 3.0f;

                rings.add(
                        new RectF(
                                bx,
                                by,
                                bx + t * 1.9f,
                                by + t * 1.9f
                        )
                );

                springs.add(
                        new RectF(
                                bx + t * 2.1f,
                                y - t * 0.5f,
                                bx + t * 3.25f,
                                y
                        )
                );
            }
        }

        private void update(float dt) {

            if (!running || !viewReady) return;

            if (dt > 0.035f) dt = 0.035f;
            if (dt < 0) dt = 0;

            time += dt;

            updateParticles(dt);
            updateMovingObstacles();
            updateMovingGates(dt);

            if (finished) {
                updateDove();
                return;
            }

            if (leftPressed) {
                ballVX -= acceleration * dt;
            }

            if (rightPressed) {
                ballVX += acceleration * dt;
            }

            ballVX =
                    Math.max(
                            -maxSpeed,
                            Math.min(
                                    maxSpeed,
                                    ballVX
                            )
                    );

            ballVY += gravity * dt;

            float oldY = ballY;
            float oldBottom = oldY + radius();

            ballX += ballVX * dt;
            ballY += ballVY * dt;

            boolean landed = false;

            for (RectF platform : platforms) {

                if (ballX + radius() > platform.left &&
                        ballX - radius() < platform.right &&
                        oldBottom <= platform.top &&
                        ballY + radius() >= platform.top &&
                        ballVY >= 0) {

                    ballY =
                            platform.top -
                                    radius();

                    // فرود عادی توپ را متوقف می‌کند؛ پرش فقط با فشار کاربر است.
                    if (jumpPressed) {
                        ballVY = -jumpPower * 1.10f;
                    } else {
                        ballVY = 0f;
                    }

                    // یک فشار فقط یک پرش ایجاد می‌کند؛ نگه‌داشتن دکمه
                    // دیگر باعث پرش‌های خودکار پشت سرهم نمی‌شود.
                    jumpPressed = false;
                    landed = true;

                    createJumpParticles();

                    break;
                }
            }

            for (RectF wall : walls) {

                if (circleRect(
                        ballX,
                        ballY,
                        radius(),
                        wall
                )) {

                    if (ballVX > 0 &&
                            ballX < wall.centerX()) {

                        ballX =
                                wall.left -
                                        radius();

                    } else if (
                            ballVX < 0 &&
                                    ballX >
                                            wall.centerX()
                    ) {

                        ballX =
                                wall.right +
                                        radius();

                    } else if (
                            ballY >
                                    wall.centerY()
                    ) {

                        ballY =
                                wall.bottom +
                                        radius();

                        if (ballVY < 0) {
                            ballVY = 0;
                        }

                    } else {

                        ballY =
                                wall.top -
                                        radius();

                        if (ballVY > 0) {
                            ballVY = 0;
                        }
                    }

                    ballVX *= -0.18f;
                }
            }

            /*
             * پرتاب‌کننده‌ها.
             */
            for (RectF spring : springs) {

                if (circleRect(
                        ballX,
                        ballY,
                        radius(),
                        spring
                )) {

                    ballVY =
                            -highBounce *
                                    (
                                            1.0f +
                                                    Math.min(
                                                            0.24f,
                                                            level *
                                                                    0.014f
                                                    )
                                    );

                    createHighJumpParticles();

                    playJumpSound();

                    break;
                }
            }

            /*
             * حلقه‌ها.
             */
            for (int i = rings.size() - 1;
                 i >= 0;
                 i--) {

                if (circleRect(
                        ballX,
                        ballY,
                        radius() * 0.85f,
                        rings.get(i)
                )) {

                    RectF ring =
                            rings.remove(i);

                    collectedRings++;
                    score += 100;

                    createRingParticles(
                            ring.centerX(),
                            ring.centerY()
                    );

                    playRingSound();
                }
            }

            /*
             * موانع متحرک.
             */
            for (MovingObstacle obstacle :
                    movingObstacles) {

                RectF hitRect =
                        obstacle.getRect();

                if (circleRect(
                        ballX,
                        ballY,
                        radius() * 0.85f,
                        hitRect
                )) {
                    // چرخ‌دنده مانع مرگ نیست؛ توپ را با نیروی فنری به بیرون پرتاب می‌کند.
                    float dx = ballX - obstacle.currentX;
                    float dy = ballY - obstacle.currentY;
                    float distance = (float) Math.sqrt(dx * dx + dy * dy);
                    if (distance < 0.001f) {
                        dx = ballVX == 0 ? 1f : ballVX;
                        dy = ballVY == 0 ? -1f : ballVY;
                        distance = (float) Math.sqrt(dx * dx + dy * dy);
                    }
                    float nx = dx / distance;
                    float ny = dy / distance;
                    float minDistance = obstacle.radius * 1.75f + radius() * 0.88f;
                    ballX = obstacle.currentX + nx * minDistance;
                    ballY = obstacle.currentY + ny * minDistance;

                    float outwardSpeed = ballVX * nx + ballVY * ny;
                    float kick = 520f + Math.min(260f, obstacle.speed * 1.15f);
                    if (outwardSpeed < kick) {
                        float impulse = kick - outwardSpeed;
                        ballVX += nx * impulse;
                        ballVY += ny * impulse;
                    }
                    // چرخش چرخ‌دنده کمی نیروی مماسی هم به توپ می‌دهد.
                    float tangent = obstacle.clockwise ? 115f : -115f;
                    ballVX += -ny * tangent;
                    ballVY += nx * tangent;
                    ballVX = Math.max(-maxSpeed * 1.55f, Math.min(maxSpeed * 1.55f, ballVX));
                    ballVY = Math.max(-1550f, Math.min(1550f, ballVY));
                    shake = Math.max(shake, 4f);
                    createRingParticles(obstacle.currentX, obstacle.currentY);
                    playJumpSound();
                }
            }

            for (MovingGate gate : movingGates) {
                RectF gateRect = gate.getRect();
                if (circleRect(ballX, ballY, radius() * 0.9f, gateRect)) {
                    // برخورد به دروازه فقط توپ را عقب می‌زند؛ باعث شروع دوباره مرحله نمی‌شود.
                    float dx = ballX - gateRect.centerX();
                    float dy = ballY - gateRect.centerY();
                    if (Math.abs(dx) > Math.abs(dy) * 0.72f) {
                        ballX = dx < 0 ? gateRect.left - radius() : gateRect.right + radius();
                        ballVX = dx < 0 ? -Math.max(260f, Math.abs(ballVX) * 0.72f)
                                : Math.max(260f, Math.abs(ballVX) * 0.72f);
                    } else {
                        ballY = dy < 0 ? gateRect.top - radius() : gateRect.bottom + radius();
                        ballVY = dy < 0 ? -Math.max(420f, Math.abs(ballVY) * 0.72f)
                                : Math.max(260f, Math.abs(ballVY) * 0.55f);
                    }
                    createRingParticles(gateRect.centerX(), gateRect.centerY());
                    playJumpSound();
                }
            }

            for (RectF spike : spikes) {

                if (circleRect(
                        ballX,
                        ballY,
                        radius() * 0.82f,
                        spike
                )) {

                    hit();

                    return;
                }
            }

            if (ballY >
                    worldHeight + getHeight()) {

                hit();

                return;
            }

            if (ballX < radius()) {

                ballX = radius();
                ballVX = 0;
            }

            if (finishRing != null &&
                    circleRect(ballX, ballY, radius(), finishRing)) {

                finishLevel();

                return;
            }

            float targetCameraX =
                    ballX -
                            getWidth() * 0.34f;

            float targetCameraY =
                    ballY -
                            getHeight() * 0.53f;

            cameraX +=
                    (
                            targetCameraX -
                                    cameraX
                    ) *
                            Math.min(
                                    1f,
                                    dt * 5.5f
                            );

            cameraY +=
                    (
                            targetCameraY -
                                    cameraY
                    ) *
                            Math.min(
                                    1f,
                                    dt * 4.2f
                            );

            cameraX =
                    Math.max(
                            0,
                            Math.min(
                                    cameraX,
                                    Math.max(
                                            0,
                                            worldWidth -
                                                    getWidth()
                                    )
                            )
                    );

            cameraY =
                    Math.max(
                            0,
                            Math.min(
                                    cameraY,
                                    Math.max(
                                            0,
                                            worldHeight -
                                                    getHeight()
                                    )
                            )
                    );

            if (shake > 0) {

                shake *= 0.88f;

                if (shake < 0.1f) {
                    shake = 0;
                }
            }
        }

        private void updateMovingObstacles() {

            for (MovingObstacle obstacle :
                    movingObstacles) {

                obstacle.angle +=
                        obstacle.speed *
                                0.016f *
                                (obstacle.clockwise
                                        ? 1
                                        : -1);

                obstacle.currentX =
                        obstacle.baseX +
                                (float)
                                        Math.sin(
                                                time *
                                                        (
                                                                1.0f +
                                                                        level *
                                                                                0.08f
                                                        )
                                        ) *
                                        tile() *
                                        (
                                                1.0f +
                                                        level *
                                                                0.08f
                                        );

                obstacle.currentY =
                        obstacle.baseY +
                                (float)
                                        Math.cos(
                                                time *
                                                        0.8f
                                        ) *
                                        tile() *
                                        0.55f;
            }
        }

        private void updateMovingGates(float dt) {
            for (MovingGate gate : movingGates) {
                gate.phase += dt * gate.speed;
                // دروازه با موج نرم بالا و پایین می‌رود و گاهی مسیر را باز می‌گذارد.
                gate.offsetY = (float) Math.sin(gate.phase) * gate.travel;
            }
        }

        private void updateDove() {

            long elapsed =
                    SystemClock.uptimeMillis() -
                            finishStartTime;

            float progress =
                    Math.min(
                            1f,
                            elapsed / 2400f
                    );

            if (doveFlying) {

                float smooth =
                        progress *
                                progress *
                                (
                                        3f -
                                                2f *
                                                        progress
                                );

                doveX =
                        doveStartX +
                                (
                                        doveTargetX -
                                                doveStartX
                                ) *
                                        smooth;

                float arc =
                        (float)
                                Math.sin(
                                        progress *
                                                Math.PI
                                );

                doveY =
                        doveStartY +
                                (
                                        doveTargetY -
                                                doveStartY
                                ) *
                                        smooth -
                                arc * 150f;

                if (progress >= 1f) {

                    doveFlying = false;
                    doveLanded = true;

                    doveX = doveTargetX;
                    doveY = doveTargetY;

                    createDoveParticles();
                }
            }

            if (elapsed >= 6000) {

                if (level < 10) {

                    level++;

                    saveProgress();

                    resetLevel();

                } else {

                    saveProgress();

                    finished = false;
                    doveFlying = false;
                    doveLanded = false;

                    ballX = spawnX;
                    ballY = spawnY;

                    ballVX = 0;
                    ballVY = 0;

                    cameraX = 0;
                    cameraY = 0;

                    leftPressed = false;
                    rightPressed = false;
                    jumpPressed = false;

                    createInitialParticles();

                    playStartSound();
                }
            }
        }

        private boolean circleRect(
                float cx,
                float cy,
                float r,
                RectF rect
        ) {

            float nx =
                    Math.max(
                            rect.left,
                            Math.min(
                                    cx,
                                    rect.right
                            )
                    );

            float ny =
                    Math.max(
                            rect.top,
                            Math.min(
                                    cy,
                                    rect.bottom
                            )
                    );

            float dx = cx - nx;
            float dy = cy - ny;

            return dx * dx + dy * dy < r * r;
        }

        private void hit() {

            shake = 18;

            createHitParticles();

            playHitSound();

            ballX = spawnX;
            ballY = spawnY;

            ballVX = 0;
            ballVY = 0;

            cameraX = 0;
            cameraY = 0;

            jumpPressed = false;

            doveFlying = false;
            doveLanded = false;
        }

        private void finishLevel() {

            if (finished) return;

            finished = true;
            score += 500 + collectedRings * 25;

            // جایزه هر مرحله: پاداش پایه + پاداش سختی + سکه‌های جمع‌آوری‌شده
            lastLevelReward =
                    25 +
                    level * 10 +
                    collectedRings * 15 +
                    (level == 10 ? 200 : 0);
            coins += lastLevelReward;

            leftPressed = false;
            rightPressed = false;
            jumpPressed = false;

            ballVX = 0;
            ballVY = 0;

            finishStartTime =
                    SystemClock.uptimeMillis();

            doveFlying = true;
            doveLanded = false;

            float t = tile();

            doveStartX =
                    ballX +
                            getWidth() * 0.65f;

            doveStartY =
                    Math.max(
                            90,
                            ballY -
                                    getHeight() *
                                            0.22f
                    );

            doveTargetX =
                    finish.left +
                            t * 1.7f;

            doveTargetY =
                    finish.top -
                            t * 0.15f;

            doveX = doveStartX;
            doveY = doveStartY;

            createFinishParticles();

            playFinishSound();

            saveProgress();
        }

        public void saveProgress() {

            if (preferences == null) {
                return;
            }

            int old =
                    preferences.getInt(
                            "saved_level",
                            1
                    );

            preferences.edit()
                    .putInt("saved_coins", coins)
                    .putInt("saved_level", Math.max(old, level))
                    .apply();
        }

        private void updateParticles(float dt) {

            for (int i = particles.size() - 1;
                 i >= 0;
                 i--) {

                Particle q =
                        particles.get(i);

                q.x += q.vx * dt;
                q.y += q.vy * dt;

                q.vy += 120f * dt;

                q.life -= dt;

                if (q.life <= 0) {
                    particles.remove(i);
                }
            }
        }

        private void createInitialParticles() {

            for (int i = 0; i < 60; i++) {

                particles.add(
                        new Particle(
                                random.nextFloat() *
                                        1600f,
                                random.nextFloat() *
                                        Math.max(
                                                600,
                                                getHeight()
                                        ),
                                random.nextFloat() *
                                        30f + 5,
                                random.nextFloat() *
                                        1.5f + 0.3f
                        )
                );
            }
        }

        private void createJumpParticles() {

            for (int i = 0; i < 10; i++) {

                Particle q =
                        new Particle(
                                ballX,
                                ballY + radius(),
                                random.nextFloat() *
                                        100 - 50,
                                -random.nextFloat() *
                                        100 - 30
                        );

                q.life = 0.55f;

                particles.add(q);
            }
        }

        private void createHighJumpParticles() {

            for (int i = 0; i < 20; i++) {

                Particle q =
                        new Particle(
                                ballX,
                                ballY + radius(),
                                random.nextFloat() *
                                        180 - 90,
                                -random.nextFloat() *
                                        190 - 40
                        );

                q.life = 0.75f;
                q.size += 2;

                particles.add(q);
            }
        }

        private void createHitParticles() {

            for (int i = 0; i < 25; i++) {

                Particle q =
                        new Particle(
                                ballX,
                                ballY,
                                random.nextFloat() *
                                        520 - 260,
                                random.nextFloat() *
                                        520 - 260
                        );

                q.life = 0.9f;
                q.size += 2;

                particles.add(q);
            }
        }

        private void createRingParticles(
                float x,
                float y
        ) {

            for (int i = 0; i < 18; i++) {

                Particle q =
                        new Particle(
                                x,
                                y,
                                random.nextFloat() *
                                        260 - 130,
                                random.nextFloat() *
                                        260 - 130
                        );

                q.life = 0.75f;
                q.size += 2;

                particles.add(q);
            }
        }

        private void createFinishParticles() {

            for (int i = 0; i < 65; i++) {

                Particle q =
                        new Particle(
                                ballX,
                                ballY,
                                random.nextFloat() *
                                        800 - 400,
                                random.nextFloat() *
                                        700 - 500
                        );

                q.life = 1.5f;
                q.size += 1;

                particles.add(q);
            }
        }

        private void createDoveParticles() {

            for (int i = 0; i < 20; i++) {

                Particle q =
                        new Particle(
                                doveX,
                                doveY,
                                random.nextFloat() *
                                        160 - 80,
                                random.nextFloat() *
                                        140 - 90
                        );

                q.life = 0.8f;

                particles.add(q);
            }
        }

        private void playStartSound() {
            playTone(
                    ToneGenerator.TONE_PROP_BEEP2,
                    90
            );
        }

        private void playHitSound() {
            playTone(
                    ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD,
                    180
            );
        }

        private void playFinishSound() {
            playTone(
                    ToneGenerator.TONE_PROP_ACK,
                    350
            );
        }

        private void playRingSound() {
            playTone(
                    ToneGenerator.TONE_PROP_BEEP,
                    80
            );
        }

        private void playJumpSound() {
            playTone(
                    ToneGenerator.TONE_DTMF_5,
                    65
            );
        }

        private void playTone(
                final int toneType,
                final int duration
        ) {

            if (tone == null) return;

            try {
                tone.startTone(
                        toneType,
                        duration
                );
            } catch (Exception ignored) {
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {

            super.onDraw(canvas);

            drawBackground(canvas);

            if (viewReady) {

                drawWorld(canvas);

                if (finished) {
                    drawDoveAndFlag(canvas);
                }

                drawHud(canvas);
                drawControls(canvas);
            }
        }

        /*
         * پس‌زمینه آبشاری.
         */
        private void drawBackground(Canvas canvas) {

            int index =
                    Math.max(
                            0,
                            Math.min(
                                    9,
                                    level - 1
                            )
                    );

            LinearGradient gradient =
                    new LinearGradient(
                            0,
                            0,
                            0,
                            getHeight(),
                            skyTop[index],
                            skyBottom[index],
                            Shader.TileMode.CLAMP
                    );

            p.setShader(gradient);

            canvas.drawRect(
                    0,
                    0,
                    getWidth(),
                    getHeight(),
                    p
            );

            p.setShader(null);

            drawSkyObjects(canvas);

            /*
             * آبشار اصلی.
             */
            drawWaterfall(canvas);

            drawAtmosphere(canvas);

            drawParticles(canvas);
        }

        private void drawWaterfall(Canvas canvas) {

            float waterX =
                    getWidth() * 0.78f;

            float top =
                    getHeight() * 0.18f;

            float bottom =
                    getHeight() * 0.78f;

            /*
             * صخره پشت آبشار.
             */
            p.setColor(0x553A665C);

            Path rock = new Path();

            rock.moveTo(
                    waterX - 145,
                    top
            );

            rock.lineTo(
                    waterX - 85,
                    top - 80
            );

            rock.lineTo(
                    waterX + 90,
                    top - 45
            );

            rock.lineTo(
                    waterX + 155,
                    top + 20
            );

            rock.lineTo(
                    waterX + 125,
                    bottom
            );

            rock.lineTo(
                    waterX - 160,
                    bottom
            );

            rock.close();

            canvas.drawPath(
                    rock,
                    p
            );

            /*
             * چند نوار آب.
             */
            for (int i = 0; i < 8; i++) {

                float offset =
                        i * 24f;

                float wave =
                        (float)
                                Math.sin(
                                        time *
                                                2.2f +
                                                i
                                ) *
                                14f;

                float x =
                        waterX -
                                100 +
                                offset +
                                wave;

                LinearGradient water =
                        new LinearGradient(
                                x,
                                top,
                                x + 12,
                                bottom,
                                0xDDFFFFFF,
                                0x5539CFF4,
                                Shader.TileMode.CLAMP
                        );

                p.setShader(water);

                Path stream =
                        new Path();

                stream.moveTo(
                        x,
                        top +
                                (i % 3) * 22
                );

                stream.cubicTo(
                        x - 30,
                        top + 120,
                        x + 30,
                        top + 260,
                        x - 10,
                        bottom
                );

                stream.lineTo(
                        x + 15,
                        bottom
                );

                stream.cubicTo(
                        x + 55,
                        top + 270,
                        x - 5,
                        top + 120,
                        x + 20,
                        top
                );

                stream.close();

                canvas.drawPath(
                        stream,
                        p
                );

                p.setShader(null);
            }

            /*
             * مه آبشار.
             */
            p.setColor(0x55FFFFFF);

            for (int i = 0; i < 12; i++) {

                float x =
                        waterX -
                                150 +
                                (
                                        i * 31
                                ) %
                                        300;

            float y =
        bottom -
                20 +
                (float) Math.sin(
                        time * 2 + i
                ) * 18;
                
                canvas.drawCircle(
                        x,
                        y,
                        5 + i % 4,
                        p
                );
            }
        }

        private void drawSkyObjects(
                Canvas canvas
        ) {

            int index =
                    Math.max(
                            0,
                            Math.min(
                                    9,
                                    level - 1
                            )
                    );

            if (index == 0 ||
                    index == 1 ||
                    index == 3 ||
                    index == 6 ||
                    index == 8) {

                drawClouds(canvas);
                drawTrees(canvas);

            } else if (
                    index == 2 ||
                            index == 5
            ) {

                drawStars(canvas);

                p.setColor(
                        0x66FFFFFF
                );

                canvas.drawCircle(
                        getWidth() * 0.78f,
                        getHeight() * 0.18f,
                        48,
                        p
                );

            } else if (index == 4) {

                drawVolcanoSky(canvas);

            } else if (index == 7) {

                drawSunset(canvas);

            } else {

                drawDesert(canvas);
            }
        }

        private void drawClouds(
                Canvas canvas
        ) {

            p.setColor(
                    0x45FFFFFF
            );

            for (int i = 0; i < 7; i++) {

                float x =
                        (
                                i * 220f -
                                        cameraX * 0.15f
                        ) %
                                (
                                        getWidth() +
                                                260
                                );

                if (x < -200) {
                    x += getWidth() + 260;
                }

                float y =
                        80 +
                                (
                                        i % 4
                                ) *
                                        70;

                canvas.drawOval(
                        x,
                        y,
                        x + 160,
                        y + 45,
                        p
                );

                canvas.drawCircle(
                        x + 45,
                        y - 15,
                        38,
                        p
                );

                canvas.drawCircle(
                        x + 100,
                        y - 8,
                        32,
                        p
                );
            }
        }

        private void drawTrees(
                Canvas canvas
        ) {

            p.setColor(
                    0x45000000
            );

            for (int i = 0; i < 13; i++) {

                float x =
                        (
                                i * 130f -
                                        cameraX * 0.20f
                        ) %
                                (
                                        getWidth() +
                                                180
                                );

                if (x < -100) {
                    x += getWidth() + 180;
                }

                float base =
                        getHeight() *
                                0.77f;

                canvas.drawRect(
                        x + 35,
                        base - 125,
                        x + 50,
                        base,
                        p
                );

                Path tree =
                        new Path();

                tree.moveTo(
                        x,
                        base - 40
                );

                tree.lineTo(
                        x + 42,
                        base - 155
                );

                tree.lineTo(
                        x + 84,
                        base - 40
                );

                tree.close();

                canvas.drawPath(
                        tree,
                        p
                );
            }
        }

        private void drawStars(
                Canvas canvas
        ) {

            p.setColor(
                    0xAAFFFFFF
            );

            for (int i = 0; i < 45; i++) {

                float x =
                        (
                                i * 97f -
                                        cameraX * 0.08f
                        ) %
                                (
                                        getWidth() + 50
                                );

                if (x < 0) {
                    x += getWidth() + 50;
                }

                float y =
                        35 +
                                (
                                        i * 53f
                                ) %
                                        (
                                                getHeight() *
                                                        0.55f
                                        );

                canvas.drawCircle(
                        x,
                        y,
                        1.5f +
                                (
                                        i % 3
                                ),
                        p
                );
            }
        }

        private void drawVolcanoSky(
                Canvas canvas
        ) {

            p.setColor(
                    0x557F1800
            );

            Path volcano =
                    new Path();

            volcano.moveTo(
                    0,
                    getHeight() *
                            0.72f
            );

            volcano.lineTo(
                    getWidth() *
                            0.52f,
                    getHeight() *
                            0.28f
            );

            volcano.lineTo(
                    getWidth(),
                    getHeight() *
                            0.72f
            );

            volcano.close();

            canvas.drawPath(
                    volcano,
                    p
            );

            p.setColor(
                    0x88FFB52E
            );

            canvas.drawCircle(
                    getWidth() *
                            0.51f,
                    getHeight() *
                            0.37f,
                    8,
                    p
            );
        }

        private void drawSunset(
                Canvas canvas
        ) {

            p.setColor(
                    0xAAFFD66B
            );

            canvas.drawCircle(
                    getWidth() * 0.76f,
                    getHeight() * 0.27f,
                    68,
                    p
            );

            p.setColor(
                    0x44FFFFFF
            );

            for (int i = 0; i < 6; i++) {

                canvas.drawOval(
                        0,
                        getHeight() *
                                0.55f +
                                i * 32,
                        getWidth(),
                        getHeight() *
                                0.59f +
                                i * 32,
                        p
                );
            }
        }

        private void drawDesert(
                Canvas canvas
        ) {

            p.setColor(
                    0x40FFFFFF
            );

            for (int i = 0; i < 9; i++) {

                float x =
                        i * 190 -
                                cameraX * 0.10f;

                canvas.drawOval(
                        x,
                        getHeight() *
                                0.63f,
                        x + 230,
                        getHeight() *
                                0.78f,
                        p
                );
            }
        }

        private void drawAtmosphere(
                Canvas canvas
        ) {

            p.setColor(
                    0x18000000
            );

            canvas.drawRect(
                    0,
                    getHeight() *
                            0.70f,
                    getWidth(),
                    getHeight(),
                    p
            );
        }

        private void drawParticles(
                Canvas canvas
        ) {

            for (Particle q :
                    particles) {

                float sx =
                        q.x -
                                cameraX * 0.25f;

                float sy =
                        q.y -
                                cameraY * 0.18f;

                if (sx < -20 ||
                        sx > getWidth() + 20 ||
                        sy < -20 ||
                        sy > getHeight() + 20) {

                    continue;
                }

                p.setAlpha(
                        (int)
                                (
                                        255 *
                                                Math.min(
                                                        1,
                                                        q.life
                                                )
                                )
                );

                p.setColor(
                        Color.WHITE
                );

                canvas.drawCircle(
                        sx,
                        sy,
                        q.size,
                        p
                );
            }

            p.setAlpha(255);
        }

        private void drawWorld(
                Canvas canvas
        ) {

            canvas.save();

            float sx = 0;
            float sy = 0;

            if (shake > 1) {

                sx =
                        (
                                random.nextFloat() -
                                        0.5f
                        ) *
                                shake;

                sy =
                        (
                                random.nextFloat() -
                                        0.5f
                        ) *
                                shake;
            }

            canvas.translate(
                    -cameraX + sx,
                    -cameraY + sy
            );

            for (RectF platform :
                    platforms) {

                drawPlatform(
                        canvas,
                        platform
                );
            }

            for (RectF wall :
                    walls) {

                drawBrickWall(
                        canvas,
                        wall
                );
            }

            for (MovingGate gate : movingGates) {
                drawMovingGate(canvas, gate);
            }

            for (RectF spike :
                    spikes) {

                drawSpike(
                        canvas,
                        spike
                );
            }

            for (RectF ring :
                    rings) {

                drawRing(
                        canvas,
                        ring
                );
            }

            for (RectF spring :
                    springs) {

                drawSpring(
                        canvas,
                        spring
                );
            }

            for (MovingObstacle obstacle :
                    movingObstacles) {

                drawMovingObstacle(
                        canvas,
                        obstacle
                );
            }

            if (finishRing != null) {
                drawRing(canvas, finishRing);
                drawPortalSparkles(canvas, finishRing);
            }

            drawFinish(
                    canvas
            );

            drawBall(
                    canvas
            );

            canvas.restore();
        }

        private void drawPlatform(
                Canvas canvas,
                RectF r
        ) {

            p.setStyle(
                    Paint.Style.FILL
            );

            p.setShader(
                    new LinearGradient(
                            r.left,
                            r.top,
                            r.left,
                            r.bottom,
                            0xFFE74A19,
                            0xFF74160D,
                            Shader.TileMode.CLAMP
                    )
            );

            canvas.drawRect(
                    r,
                    p
            );

            p.setShader(null);

            drawBrickLines(
                    canvas,
                    r
            );

            p.setColor(
                    0xFFFF7028
            );

            canvas.drawRect(
                    r.left,
                    r.top,
                    r.right,
                    r.top +
                            tile() *
                                    0.11f,
                    p
            );

            p.setStyle(
                    Paint.Style.STROKE
            );

            p.setStrokeWidth(2);

            p.setColor(
                    0x55FFFFFF
            );

            canvas.drawRect(
                    r,
                    p
            );

            p.setStyle(
                    Paint.Style.FILL
            );
        }

        private void drawBrickWall(
                Canvas canvas,
                RectF r
        ) {

            p.setStyle(
                    Paint.Style.FILL
            );

            p.setShader(
                    new LinearGradient(
                            r.left,
                            r.top,
                            r.left,
                            r.bottom,
                            0xFFD94718,
                            0xFF68110C,
                            Shader.TileMode.CLAMP
                    )
            );

            canvas.drawRect(
                    r,
                    p
            );

            p.setShader(null);

            drawBrickLines(
                    canvas,
                    r
            );

            /*
             * لبه خشتی برجسته.
             */
            p.setColor(
                    0x55FFFFFF
            );

            canvas.drawRect(
                    r.left,
                    r.top,
                    r.right,
                    r.top +
                            tile() * 0.10f,
                    p
            );

            p.setStyle(
                    Paint.Style.STROKE
            );

            p.setStrokeWidth(2);

            p.setColor(
                    0x66FFD0A0
            );

            canvas.drawRect(
                    r,
                    p
            );

            // سایه‌ی کناره‌ها برای برجسته‌شدن دیوارها.
            p.setStyle(Paint.Style.FILL);
            p.setColor(0x66300805);
            float edge = Math.max(2f, tile() * 0.09f);
            canvas.drawRect(
                    Math.max(r.left, r.right - edge),
                    r.top + edge,
                    r.right,
                    r.bottom,
                    p
            );

            // لبه‌ی روشن بالای دیوار مثل سنگ‌کاری سه‌بعدی.
            p.setColor(0x88FFB06A);
            canvas.drawRect(
                    r.left,
                    r.top,
                    r.right,
                    Math.min(r.bottom, r.top + edge * 0.55f),
                    p
            );

            // نقش‌های ظریف و منظم روی دیوار، بدون تغییر برخورد بازی.
            float motif = Math.max(5f, tile() * 0.16f);
            float cx = r.centerX();
            float cy = r.centerY();
            if (r.width() > tile() * 0.85f &&
                    r.height() > tile() * 1.2f) {
                p.setColor(0x44FFD7A0);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(Math.max(1.5f, tile() * 0.035f));
                canvas.drawRoundRect(
                        cx - motif,
                        cy - motif * 1.35f,
                        cx + motif,
                        cy + motif * 1.35f,
                        motif * 0.25f,
                        motif * 0.25f,
                        p
                );
                p.setStyle(Paint.Style.FILL);
                p.setColor(0x55910F0B);
                canvas.drawCircle(cx, cy, Math.max(1.5f, motif * 0.24f), p);
            }

            p.setStyle(
                    Paint.Style.FILL
            );
        }

        private void drawBrickLines(
                Canvas canvas,
                RectF r
        ) {

            float t = tile();

            p.setStrokeWidth(
                    Math.max(
                            1.5f,
                            t * 0.035f
                    )
            );

            p.setColor(
                    0x552B0804
            );

            for (
                    float yy =
                            r.top + t * 0.72f;
                    yy < r.bottom;
                    yy += t * 0.72f
            ) {

                canvas.drawLine(
                        r.left,
                        yy,
                        r.right,
                        yy,
                        p
                );
            }

            int row = 0;

            for (
                    float yy = r.top;
                    yy < r.bottom;
                    yy += t * 0.72f
            ) {

                float offset =
                        row % 2 == 0
                                ? t * 0.50f
                                : 0;

                for (
                        float xx =
                                r.left + offset;
                        xx < r.right;
                        xx += t
                ) {

                    canvas.drawLine(
                            xx,
                            yy,
                            xx,
                            Math.min(
                                    yy +
                                            t * 0.72f,
                                    r.bottom
                            ),
                            p
                    );
                }

                row++;
            }
        }

        private void drawMovingGate(Canvas canvas, MovingGate gate) {
            RectF r = gate.getRect();
            p.setShader(new LinearGradient(r.left, r.top, r.right, r.bottom,
                    0xFF7DEBFF, 0xFF1265A8, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(r, tile() * 0.16f, tile() * 0.16f, p);
            p.setShader(null);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(Math.max(2f, tile() * 0.06f));
            p.setColor(0xCCFFFFFF);
            canvas.drawRoundRect(r, tile() * 0.16f, tile() * 0.16f, p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xAA063B72);
            for (float yy = r.top + tile() * 0.25f; yy < r.bottom; yy += tile() * 0.34f) {
                canvas.drawLine(r.left + 4, yy, r.right - 4, yy, p);
            }
        }

        private void drawSpike(
                Canvas canvas,
                RectF r
        ) {

            p.setStyle(
                    Paint.Style.FILL
            );

            LinearGradient g =
                    new LinearGradient(
                            r.left,
                            r.top,
                            r.right,
                            r.bottom,
                            0xFFFFFFFF,
                            0xFFE30031,
                            Shader.TileMode.CLAMP
                    );

            p.setShader(g);

            shape.reset();

            shape.moveTo(
                    r.left,
                    r.bottom
            );

            shape.lineTo(
                    r.centerX(),
                    r.top
            );

            shape.lineTo(
                    r.right,
                    r.bottom
            );

            shape.close();

            canvas.drawPath(
                    shape,
                    p
            );

            p.setShader(null);

            p.setColor(
                    0xAAFFFFFF
            );

            canvas.drawCircle(
                    r.centerX() - 2,
                    r.top +
                            r.height() *
                                    0.28f,
                    2.5f,
                    p
            );
        }

        private void drawRing(
                Canvas canvas,
                RectF r
        ) {

            float cx =
                    r.centerX();

            float cy =
                    r.centerY();

            glow.setStyle(
                    Paint.Style.STROKE
            );

            glow.setStrokeWidth(Math.max(12f, r.width() * 0.10f));

            glow.setColor(
                    0x6633CFFF
            );

            canvas.drawCircle(
                    cx,
                    cy,
                    r.width() * 0.42f,
                    glow
            );

            p.setStyle(
                    Paint.Style.STROKE
            );

            p.setStrokeWidth(Math.max(6f, r.width() * 0.055f));

            p.setShader(
                    new LinearGradient(
                            r.left,
                            r.top,
                            r.right,
                            r.bottom,
                            0xFFE8FFFF,
                            0xFF008BFF,
                            Shader.TileMode.CLAMP
                    )
            );

            canvas.drawCircle(
                    cx,
                    cy,
                    r.width() * 0.38f,
                    p
            );

            p.setShader(null);

            p.setStyle(
                    Paint.Style.FILL
            );

            p.setColor(
                    Color.WHITE
            );

            p.setAlpha(210);

            canvas.drawCircle(
                    cx -
                            r.width() * 0.16f,
                    cy -
                            r.height() * 0.16f,
                    4,
                    p
            );

            p.setAlpha(255);
        }

        private void drawPortalSparkles(Canvas canvas, RectF r) {
            float cx = r.centerX();
            float cy = r.centerY();
            float pulse = 1f + (float) Math.sin(time * 4.2f) * 0.045f;
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(Math.max(2f, tile() * 0.045f));
            p.setColor(0xAAE9FFFF);
            canvas.drawCircle(cx, cy, r.width() * 0.47f * pulse, p);
            p.setStyle(Paint.Style.FILL);
            for (int i = 0; i < 4; i++) {
                double a = time * 1.7 + i * Math.PI / 2.0;
                float x = cx + (float) Math.cos(a) * r.width() * 0.58f;
                float y = cy + (float) Math.sin(a) * r.height() * 0.58f;
                p.setColor(0xFFEFFFFF);
                canvas.drawCircle(x, y, Math.max(2f, tile() * 0.07f), p);
            }
        }

        private void drawSpring(
                Canvas canvas,
                RectF r
        ) {

            p.setShader(
                    new LinearGradient(
                            r.left,
                            r.top,
                            r.right,
                            r.bottom,
                            0xFFFFE000,
                            0xFFFF6500,
                            Shader.TileMode.CLAMP
                    )
            );

            canvas.drawRoundRect(
                    r,
                    10,
                    10,
                    p
            );

            p.setShader(null);

            p.setStyle(
                    Paint.Style.STROKE
            );

            p.setStrokeWidth(3);

            p.setColor(
                    Color.WHITE
            );

            for (int i = 0; i < 3; i++) {

                float yy =
                        r.top +
                                7 +
                                i *
                                        r.height() *
                                        0.27f;

                canvas.drawLine(
                        r.left + 5,
                        yy,
                        r.right - 5,
                        yy,
                        p
                );
            }

            p.setStyle(
                    Paint.Style.FILL
            );
        }

        private void drawMovingObstacle(
                Canvas canvas,
                MovingObstacle obstacle
        ) {

            float x =
                    obstacle.currentX;

            float y =
                    obstacle.currentY;

            float r =
                    obstacle.radius;

            canvas.save();

            canvas.rotate(
                    obstacle.angle,
                    x,
                    y
            );

            p.setColor(
                    0xFF5E1B12
            );

            canvas.drawCircle(
                    x,
                    y,
                    r * 1.15f,
                    p
            );

            p.setColor(
                    0xFFD9411E
            );

            canvas.drawCircle(
                    x,
                    y,
                    r,
                    p
            );

            p.setColor(
                    0xFFFF8B37
            );

            for (int i = 0; i < 4; i++) {

                float a =
                        i *
                                (float)
                                        Math.PI /
                                        2f;

                float x2 =
                        x +
                                (float)
                                        Math.cos(a) *
                                        r *
                                        1.7f;

                float y2 =
                        y +
                                (float)
                                        Math.sin(a) *
                                        r *
                                        1.7f;

                p.setStrokeWidth(
                        Math.max(
                                5,
                                tile() * 0.11f
                        )
                );

                canvas.drawLine(
                        x,
                        y,
                        x2,
                        y2,
                        p
                );
            }

            canvas.restore();
        }

        private void drawFinish(
                Canvas canvas
        ) {

            if (finish == null) return;

            p.setStrokeWidth(6);

            p.setColor(
                    Color.WHITE
            );

            canvas.drawLine(
                    finish.left + 7,
                    finish.top,
                    finish.left + 7,
                    finish.bottom + tile(),
                    p
            );

            shape.reset();

            shape.moveTo(
                    finish.left + 9,
                    finish.top
            );

            shape.lineTo(
                    finish.right,
                    finish.top +
                            tile() * 0.35f
            );

            shape.lineTo(
                    finish.left + 9,
                    finish.top +
                            tile() * 0.75f
            );

            shape.close();

            p.setShader(
                    new LinearGradient(
                            finish.left,
                            finish.top,
                            finish.right,
                            finish.bottom,
                            0xFFFF4B62,
                            0xFFC90038,
                            Shader.TileMode.CLAMP
                    )
            );

            canvas.drawPath(
                    shape,
                    p
            );

            p.setShader(null);
        }

        /*
         * توپ بزرگ‌تر و واقعی‌تر.
         */
        private void drawBall(
                Canvas canvas
        ) {

            float r =
                    radius();

            /*
             * سایه.
             */
            p.setColor(
                    0x55000000
            );

            canvas.drawOval(
                    ballX - r * 1.18f,
                    ballY + r * 0.72f,
                    ballX + r * 1.18f,
                    ballY + r * 1.12f,
                    p
            );

            /*
             * بدنه توپ قرمز.
             */
            RadialGradient rg =
                    new RadialGradient(
                            ballX - r * 0.34f,
                            ballY - r * 0.42f,
                            r * 1.28f,
                            new int[]{
                                    0xFFFFFFFF,
                                    0xFFFFA8AF,
                                    0xFFFF3048,
                                    0xFFD40027,
                                    0xFF750018
                            },
                            new float[]{
                                    0f,
                                    0.18f,
                                    0.52f,
                                    0.78f,
                                    1f
                            },
                            Shader.TileMode.CLAMP
                    );

            p.setShader(rg);

            canvas.drawCircle(
                    ballX,
                    ballY,
                    r,
                    p
            );

            p.setShader(null);

            /*
             * نوارهای ظریف روی توپ.
             */
            p.setStyle(
                    Paint.Style.STROKE
            );

            p.setStrokeWidth(
                    Math.max(
                            2,
                            r * 0.055f
                    )
            );

            p.setColor(
                    0x55FFFFFF
            );

            canvas.drawArc(
                    ballX - r * 0.78f,
                    ballY - r * 0.72f,
                    ballX + r * 0.78f,
                    ballY + r * 0.72f,
                    25,
                    105,
                    false,
                    p
            );

            canvas.drawArc(
                    ballX - r * 0.70f,
                    ballY - r * 0.82f,
                    ballX + r * 0.70f,
                    ballY + r * 0.82f,
                    205,
                    80,
                    false,
                    p
            );

            p.setStyle(
                    Paint.Style.FILL
            );

            /*
             * برق روی توپ.
             */
            p.setColor(
                    Color.WHITE
            );

            p.setAlpha(235);

            canvas.drawOval(
                    ballX - r * 0.57f,
                    ballY - r * 0.72f,
                    ballX - r * 0.04f,
                    ballY - r * 0.30f,
                    p
            );

            p.setAlpha(255);

            /*
             * لبه.
             */
            p.setStyle(
                    Paint.Style.STROKE
            );

            p.setStrokeWidth(2.5f);

            p.setColor(
                    0xAAFFFFFF
            );

            canvas.drawCircle(
                    ballX,
                    ballY,
                    r - 2,
                    p
            );

            p.setStyle(
                    Paint.Style.FILL
            );
        }

        private void drawDoveAndFlag(
                Canvas canvas
        ) {

            if (!finished ||
                    finish == null) {
                return;
            }

            canvas.save();

            canvas.translate(
                    -cameraX,
                    -cameraY
            );

            float t = tile();

            float flagX =
                    finish.left + 7;

            float flagTop =
                    finish.top -
                            t * 0.55f;

            float flagBottom =
                    finish.bottom + t;

            p.setStrokeWidth(5);

            p.setColor(
                    Color.WHITE
            );

            canvas.drawLine(
                    flagX,
                    flagTop,
                    flagX,
                    flagBottom,
                    p
            );

            float flagWidth =
                    t * 2.0f;

            float flagHeight =
                    t * 1.20f;

            RectF afFlag =
                    new RectF(
                            flagX,
                            flagTop,
                            flagX + flagWidth,
                            flagTop +
                                    flagHeight
                    );

            p.setColor(
                    0xFF000000
            );

            canvas.drawRect(
                    afFlag.left,
                    afFlag.top,
                    afFlag.right,
                    afFlag.top +
                            flagHeight / 3f,
                    p
            );

            p.setColor(
                    0xFFFF0000
            );

            canvas.drawRect(
                    afFlag.left,
                    afFlag.top +
                            flagHeight / 3f,
                    afFlag.right,
                    afFlag.top +
                            flagHeight * 2f /
                                    3f,
                    p
            );

            p.setColor(
                    0xFF007A36
            );

            canvas.drawRect(
                    afFlag.left,
                    afFlag.top +
                            flagHeight * 2f /
                                    3f,
                    afFlag.right,
                    afFlag.bottom,
                    p
            );

            p.setStyle(
                    Paint.Style.STROKE
            );

            p.setStrokeWidth(2);

            p.setColor(
                    0xAAFFFFFF
            );

            canvas.drawRect(
                    afFlag,
                    p
            );

            p.setStyle(
                    Paint.Style.FILL
            );

            if (doveFlying ||
                    doveLanded) {

                drawDove(
                        canvas,
                        doveX,
                        doveY,
                        doveFlying
                );
                // کبوتر دوم کمی عقب‌تر پرواز می‌کند تا جشن پایان مرحله زنده‌تر شود.
                drawDove(
                        canvas,
                        doveX - tile() * 1.25f,
                        doveY + tile() * 0.62f,
                        doveFlying
                );
            }

            canvas.restore();
        }

        private void drawDove(
                Canvas canvas,
                float x,
                float y,
                boolean flying
        ) {

            float size =
                    tile() * 0.85f;

            float wing =
                    flying
                            ? (float)
                                    Math.sin(
                                            time * 13f
                                    ) *
                                    size * 0.22f
                            : 0;

            p.setStyle(
                    Paint.Style.FILL
            );

            p.setColor(
                    0xFFF7F7F7
            );

            canvas.drawOval(
                    x - size * 0.60f,
                    y - size * 0.22f,
                    x + size * 0.62f,
                    y + size * 0.32f,
                    p
            );

            canvas.drawCircle(
                    x + size * 0.52f,
                    y - size * 0.18f,
                    size * 0.30f,
                    p
            );

            Path leftWing =
                    new Path();

            leftWing.moveTo(
                    x - size * 0.12f,
                    y
            );

            leftWing.lineTo(
                    x - size * 0.85f,
                    y -
                            size *
                                    (
                                            0.30f +
                                                    wing /
                                                            size
                                    )
            );

            leftWing.lineTo(
                    x - size * 0.42f,
                    y + size * 0.18f
            );

            leftWing.close();

            canvas.drawPath(
                    leftWing,
                    p
            );

            p.setColor(
                    0xFFD7D7D7
            );

            Path rightWing =
                    new Path();

            rightWing.moveTo(
                    x + size * 0.10f,
                    y
            );

            rightWing.lineTo(
                    x + size * 0.50f,
                    y -
                            size *
                                    (
                                            0.25f +
                                                    wing /
                                                            size
                                    )
            );

            rightWing.lineTo(
                    x + size * 0.36f,
                    y + size * 0.16f
            );

            rightWing.close();

            canvas.drawPath(
                    rightWing,
                    p
            );

            p.setColor(
                    0xFFFFB300
            );

            Path beak =
                    new Path();

            beak.moveTo(
                    x + size * 0.77f,
                    y - size * 0.18f
            );

            beak.lineTo(
                    x + size * 1.05f,
                    y - size * 0.08f
            );

            beak.lineTo(
                    x + size * 0.77f,
                    y
            );

            beak.close();

            canvas.drawPath(
                    beak,
                    p
            );

            p.setColor(
                    Color.BLACK
            );

            canvas.drawCircle(
                    x + size * 0.61f,
                    y - size * 0.25f,
                    3,
                    p
            );
        }

        private void drawHud(
                Canvas canvas
        ) {

            p.setColor(
                    0x72000000
            );

            canvas.drawRoundRect(
                    16,
                    14,
                    getWidth() - 16,
                    124,
                    26,
                    26,
                    p
            );

            p.setColor(
                    Color.WHITE
            );

            p.setTextAlign(
                    Paint.Align.CENTER
            );

            p.setTextSize(23);

            canvas.drawText(
                    "🔴 " + tr("مرحله ", "Level ", "مرحله ", "مرحلہ ", "स्तर ", "المرحلة ") + level,
                    getWidth() * 0.50f,
                    56,
                    p
            );

            p.setTextSize(15);
            p.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(
                    tr("امتیاز: ", "Score: ", "نمرې: ", "اسکور: ", "अंक: ", "النقاط: ") + score,
                    28,
                    105,
                    p
            );

            p.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(
                    tr("حلقه: ", "Rings: ", "حلقې: ", "حلقے: ", "छल्ले: ", "الحلقات: ") + collectedRings,
                    getWidth() * 0.50f,
                    105,
                    p
            );

            // نشان طلایی کیف پول سکه‌ها
            float coinX = getWidth() - 105;
            float coinY = 99;
            p.setColor(0xFFFFD54F);
            canvas.drawCircle(coinX, coinY - 5, 11, p);
            p.setColor(0xFFFFA000);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2.5f);
            canvas.drawCircle(coinX, coinY - 5, 8, p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.WHITE);
            p.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(String.valueOf(coins), coinX + 16, 101, p);
            p.setTextAlign(Paint.Align.CENTER);

            if (finished) {

                long elapsed =
                        SystemClock.uptimeMillis() -
                                finishStartTime;

                float seconds =
                        Math.min(
                                6f,
                                elapsed / 1000f
                        );

                p.setColor(
                        0xEEFFFFFF
                );

                p.setTextSize(31);

                canvas.drawText(
                        tr("مرحله کامل شد!", "Level completed!", "مرحله بشپړه شوه!", "مرحلہ مکمل ہو گیا!", "स्तर पूरा हुआ!", "اكتملت المرحلة!"),
                        getWidth() / 2f,
                        getHeight() * 0.28f,
                        p
                );

                p.setTextSize(20);

                if (level < 10) {

                    canvas.drawText(
                            tr("آماده مرحله بعد", "Ready for next level", "راتلونکې مرحلې ته چمتو", "اگلے مرحلے کے لیے تیار", "अगले स्तर के लिए तैयार", "استعد للمرحلة التالية"),
                            getWidth() / 2f,
                            getHeight() *
                                    0.28f +
                                    40,
                            p
                    );

                } else {

                    canvas.drawText(
                            tr("آخرین مرحله کامل شد!", "Final level completed!", "وروستۍ مرحله بشپړه شوه!", "آخری مرحلہ مکمل ہو گیا!", "अंतिम स्तर पूरा हुआ!", "اكتملت المرحلة الأخيرة!"),
                            getWidth() / 2f,
                            getHeight() *
                                    0.28f +
                                    40,
                            p
                    );
                }

                p.setTextSize(19);
                p.setColor(0xFFFFD54F);
                canvas.drawText(
                        "+" + lastLevelReward + " " +
                                tr("سکه جایزه", "bonus coins", "د انعام سکې", "انعامی سکے", "बोनस सिक्के", "عملات مكافأة"),
                        getWidth() / 2f,
                        getHeight() * 0.28f + 76,
                        p
                );

                p.setColor(0xFF5D7188);
                p.setTextSize(15);
                canvas.drawText(
                        tr("موجودی سکه: ", "Coin balance: ", "د سکو ټولټال: ", "سکوں کا بیلنس: ", "सिक्कों का शेष: ", "رصيد العملات: ") + coins,
                        getWidth() / 2f,
                        getHeight() * 0.28f + 104,
                        p
                );

                p.setColor(0xFF5D7188);
                p.setTextSize(15);
                canvas.drawText(
                        String.format(
                                Locale.US,
                                tr("%.0f ثانیه", "%.0f seconds", "%.0f ثانیې", "%.0f سیکنڈ", "%.0f सेकंड", "%.0f ثانية"),
                                Math.max(0, 6f - seconds)
                        ),
                        getWidth() / 2f,
                        getHeight() * 0.28f + 130,
                        p
                );
            }

            p.setTextAlign(
                    Paint.Align.LEFT
            );
        }

        private void drawControls(
                Canvas canvas
        ) {

            if (finished) return;

            /*
             * دکمه‌ها بزرگ‌تر و بالاتر.
             */
            float size =
                    Math.min(
                            112,
                            getWidth() * 0.23f
                    );

            float bottom =
                    getHeight() - 105;

            drawButton(
                    canvas,
                    24,
                    bottom - size,
                    size,
                    "◀"
            );

            drawButton(
                    canvas,
                    getWidth() / 2f -
                            size / 2f,
                    bottom - size,
                    size,
                    "⬆"
            );

            drawButton(
                    canvas,
                    getWidth() -
                            size -
                            24,
                    bottom - size,
                    size,
                    "▶"
            );
        }

        private void drawButton(
                Canvas canvas,
                float x,
                float y,
                float size,
                String symbol
        ) {

            p.setColor(
                    0x80000000
            );

            canvas.drawRoundRect(
                    x,
                    y,
                    x + size,
                    y + size,
                    34,
                    34,
                    p
            );

            p.setStyle(
                    Paint.Style.STROKE
            );

            p.setStrokeWidth(3);

            p.setColor(
                    0xCCFFFFFF
            );

            canvas.drawRoundRect(
                    x + 2,
                    y + 2,
                    x + size - 2,
                    y + size - 2,
                    34,
                    34,
                    p
            );

            p.setStyle(
                    Paint.Style.FILL
            );

            p.setColor(
                    Color.WHITE
            );

            p.setTextAlign(
                    Paint.Align.CENTER
            );

            p.setTextSize(
                    size * 0.45f
            );

            canvas.drawText(
                    symbol,
                    x + size / 2f,
                    y + size * 0.66f,
                    p
            );

            p.setTextAlign(
                    Paint.Align.LEFT
            );
        }

        private void updatePressedPointers(MotionEvent event, int excludedPointer) {
            leftPressed = false;
            rightPressed = false;

            float size = Math.min(112, getWidth() * 0.23f);
            float bottom = getHeight() - 105;
            float top = bottom - size;
            float leftX = 24;
            float rightX = getWidth() - size - 24;

            for (int i = 0; i < event.getPointerCount(); i++) {
                if (i == excludedPointer) continue;

                float px = event.getX(i);
                float py = event.getY(i);

                if (py >= top && py <= bottom) {
                    if (px >= leftX && px <= leftX + size) leftPressed = true;
                    if (px >= rightX && px <= rightX + size) rightPressed = true;
                }
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (!viewReady || finished) return true;

            int action = event.getActionMasked();
            int index = event.getActionIndex();

            if (action == MotionEvent.ACTION_DOWN ||
                    action == MotionEvent.ACTION_POINTER_DOWN) {
                float x = event.getX(index);
                float y = event.getY(index);

                float size = Math.min(112, getWidth() * 0.23f);
                float bottom = getHeight() - 105;
                float top = bottom - size;
                float jumpX = getWidth() / 2f - size / 2f;

                // پرش فقط با لمس تازه دکمه وسط اجرا می‌شود.
                if (y >= top && y <= bottom &&
                        x >= jumpX && x <= jumpX + size) {
                    jumpPressed = true;
                }
                updatePressedPointers(event, -1);
                invalidate();
                return true;
            }

            if (action == MotionEvent.ACTION_MOVE) {
                updatePressedPointers(event, -1);
                invalidate();
                return true;
            }

            if (action == MotionEvent.ACTION_POINTER_UP ||
                    action == MotionEvent.ACTION_UP) {
                updatePressedPointers(event, index);
                if (action == MotionEvent.ACTION_UP) {
                    leftPressed = false;
                    rightPressed = false;
                }
                invalidate();
                return true;
            }

            if (action == MotionEvent.ACTION_CANCEL) {
                leftPressed = false;
                rightPressed = false;
                jumpPressed = false;
                invalidate();
                return true;
            }

            return true;
        }

        public void startGame() {

            if (!viewReady) {

                running = true;

                return;
            }

            running = true;

            lastFrame =
                    SystemClock.uptimeMillis();

            removeCallbacks(
                    frameRunnable
            );

            post(
                    frameRunnable
            );
        }

        public void stopGame() {

            running = false;

            removeCallbacks(
                    frameRunnable
            );

            saveProgress();
        }

        private final Runnable frameRunnable =
                new Runnable() {

                    @Override
                    public void run() {

                        if (!running ||
                                !viewReady) {
                            return;
                        }

                        long now =
                                SystemClock.uptimeMillis();

                        float dt =
                                (
                                        now -
                                                lastFrame
                                ) /
                                        1000f;

                        lastFrame = now;

                        update(dt);

                        invalidate();

                        postDelayed(
                                this,
                                16
                        );
                    }
                };

        private class Particle {

            float x;
            float y;
            float vx;
            float vy;
            float size;
            float life;

            Particle(
                    float x,
                    float y,
                    float vx,
                    float vy
            ) {

                this.x = x;
                this.y = y;
                this.vx = vx;
                this.vy = vy;

                this.size =
                        2 +
                                random.nextFloat() *
                                        5;

                this.life =
                        0.8f +
                                random.nextFloat() *
                                        1.4f;
            }
        }

        private class MovingObstacle {

            float baseX;
            float baseY;

            float currentX;
            float currentY;

            float radius;
            float speed;
            float angle;

            boolean clockwise;

            MovingObstacle(
                    float x,
                    float y,
                    float radius,
                    float speed,
                    boolean clockwise
            ) {

                this.baseX = x;
                this.baseY = y;

                this.currentX = x;
                this.currentY = y;

                this.radius = radius;
                this.speed = speed;

                this.clockwise = clockwise;

                this.angle = 0;
            }

            RectF getRect() {

                return new RectF(
                        currentX - radius * 1.75f,
                        currentY - radius * 1.75f,
                        currentX + radius * 1.75f,
                        currentY + radius * 1.75f
                );
            }
        }

        private class MovingGate {
            float x, baseY, width, height, speed, travel, phase, offsetY;

            MovingGate(float x, float y, float width, float height,
                       float speed, boolean openPhase) {
                this.x = x;
                this.baseY = y;
                this.width = width;
                this.height = height;
                this.speed = speed;
                this.travel = height * 0.72f;
                this.phase = openPhase ? 0f : (float) Math.PI;
            }

            RectF getRect() {
                return new RectF(
                        x - width / 2f,
                        baseY + offsetY,
                        x + width / 2f,
                        baseY + offsetY + height
                );
            }
        }
    }
}
