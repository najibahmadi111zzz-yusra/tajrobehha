package com.tajro.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.content.Context;
import android.os.Handler;
import java.util.*;

public class BallGameActivity extends Activity {

    private GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        gameView = new GameView(this);
        setContentView(gameView);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (gameView != null) gameView.pauseGame();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameView != null) gameView.resumeGame();
    }

    public class GameView extends View {

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();

        private float ballX;
        private float ballY;
        private float ballVX;
        private float ballVY;

        private float cameraX;
        private float cameraY;

        private boolean left;
        private boolean right;
        private boolean jumping;

        private boolean gameRunning = true;
        private boolean levelFinished = false;

        private int level = 1;

        private long lastTime;

        private final float gravity = 1250f;
        private final float moveSpeed = 330f;
        private final float jumpPower = 610f;

        private final ArrayList<RectF> floors = new ArrayList<>();
        private final ArrayList<RectF> walls = new ArrayList<>();
        private final ArrayList<RectF> spikes = new ArrayList<>();
        private final ArrayList<RectF> rings = new ArrayList<>();
        private final ArrayList<RectF> springs = new ArrayList<>();

        private RectF finish;

        private float worldWidth;
        private float worldHeight;

        private int backgroundType;

        private final Handler handler = new Handler();

        private final int[][] backgrounds = {
                {0xFF062A20, 0xFF0A5F45, 0xFF18A66B},
                {0xFF071A3B, 0xFF145A9C, 0xFF36B6E8},
                {0xFF24104A, 0xFF7027A8, 0xFFE04CCB},
                {0xFF301306, 0xFF9A4217, 0xFFFFA52E},
                {0xFF061B28, 0xFF075B72, 0xFF18C4B8},
                {0xFF15102F, 0xFF3C2780, 0xFF8065FF},
                {0xFF0A2410, 0xFF347A18, 0xFF9BD42D},
                {0xFF28091B, 0xFF8C194E, 0xFFF05D8E},
                {0xFF08182B, 0xFF174E78, 0xFF56A7D8},
                {0xFF182006, 0xFF526D12, 0xFFD0E63B}
        };

        public GameView(Context context) {
            super(context);

            paint.setStrokeWidth(3);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);

            setFocusable(true);
            resetLevel();

            lastTime = System.nanoTime();
        }

        private float tile() {
            return Math.max(48f, getWidth() * 0.075f);
        }

        private void resetLevel() {
            floors.clear();
            walls.clear();
            spikes.clear();
            rings.clear();
            springs.clear();

            levelFinished = false;
            backgroundType = (level - 1) % backgrounds.length;

            float t = tile();

            worldWidth = 10000 + level * 1100;
            worldHeight = Math.max(getHeight() * 2.4f, 1500f);

            ballX = t * 2.5f;
            ballY = getHeight() * 0.62f;

            ballVX = 0;
            ballVY = 0;

            cameraX = 0;
            cameraY = 0;

            float x = t * 0.5f;
            float y = getHeight() * 0.70f;

            int direction = 1;

            while (x < worldWidth - t * 6) {

                int length = 9 + ((int)(x / t) + level * 3) % 16;

                RectF platform = new RectF(
                        x,
                        y,
                        x + length * t,
                        y + t * 1.35f
                );

                floors.add(platform);

                if (((int)(x / t) + level) % 7 == 0) {
                    spikes.add(new RectF(
                            x + length * t * 0.45f,
                            y - t * 0.55f,
                            x + length * t * 0.45f + t,
                            y
                    ));
                }

                if (((int)(x / t) + level) % 5 == 0) {
                    rings.add(new RectF(
                            x + length * t * 0.72f,
                            y - t * 2.0f,
                            x + length * t * 0.72f + t * 1.1f,
                            y - t * 0.9f
                    ));
                }

                if (((int)(x / t) + level) % 9 == 0) {
                    springs.add(new RectF(
                            x + length * t * 0.83f,
                            y - t * 0.35f,
                            x + length * t * 0.83f + t * 0.8f,
                            y
                    ));
                }

                x += length * t;

                float change = (2 + ((int)(x / t) + level) % 4) * t;

                if (direction > 0) {
                    y -= change * 0.42f;
                    if (y < getHeight() * 0.30f) {
                        y = getHeight() * 0.48f;
                        direction = -1;
                    }
                } else {
                    y += change * 0.42f;
                    if (y > getHeight() * 0.75f) {
                        y = getHeight() * 0.62f;
                        direction = 1;
                    }
                }

                if (level >= 3 && ((int)(x / t)) % 13 == 0) {
                    RectF upper = new RectF(
                            x - t * 3,
                            y - t * 3.2f,
                            x + t * 8,
                            y - t * 2.4f
                    );
                    floors.add(upper);
                }
            }

            finish = new RectF(
                    worldWidth - t * 4.5f,
                    y - t * 2.0f,
                    worldWidth - t * 2.2f,
                    y + t * 0.1f
            );

            invalidate();
        }

        private void update(float dt) {

            if (!gameRunning || levelFinished) return;

            if (dt > 0.04f) dt = 0.04f;

            if (left) {
                ballVX -= moveSpeed * 4.5f * dt;
            }

            if (right) {
                ballVX += moveSpeed * 4.5f * dt;
            }

            if (!left && !right) {
                ballVX *= Math.pow(0.0008, dt);
            }

            if (ballVX > moveSpeed) ballVX = moveSpeed;
            if (ballVX < -moveSpeed) ballVX = -moveSpeed;

            ballVY += gravity * dt;

            float oldY = ballY;

            ballX += ballVX * dt;
            ballY += ballVY * dt;

            float radius = tBall();

            boolean landed = false;

            for (RectF floor : floors) {

                if (ballX + radius > floor.left &&
                        ballX - radius < floor.right &&
                        oldY + radius <= floor.top &&
                        ballY + radius >= floor.top &&
                        ballVY >= 0) {

                    ballY = floor.top - radius;
                    ballVY = 0;
                    landed = true;
                    break;
                }
            }

            if (landed && jumping) {
                ballVY = -jumpPower;
                jumping = false;
            }

            for (RectF spring : springs) {

                if (ballX + radius > spring.left &&
                        ballX - radius < spring.right &&
                        ballY + radius > spring.top &&
                        ballY - radius < spring.bottom) {

                    ballVY = -jumpPower * 1.35f;
                }
            }

            for (RectF spike : spikes) {

                if (circleRect(ballX, ballY, radius, spike)) {
                    restartAfterHit();
                    return;
                }
            }

            if (ballY > worldHeight + 300) {
                restartAfterHit();
                return;
            }

            if (ballX < radius) {
                ballX = radius;
                ballVX = 0;
            }

            if (ballX > worldWidth - radius) {
                ballX = worldWidth - radius;
            }

            if (finish != null &&
                    ballX > finish.left &&
                    ballX < finish.right &&
                    ballY > finish.top - radius * 2 &&
                    ballY < finish.bottom + radius * 2) {

                completeLevel();
                return;
            }

            cameraX = ballX - getWidth() * 0.34f;
            cameraY = ballY - getHeight() * 0.54f;

            if (cameraX < 0) cameraX = 0;
            if (cameraX > worldWidth - getWidth()) {
                cameraX = worldWidth - getWidth();
            }

            float maxCameraY = Math.max(0, worldHeight - getHeight());

            if (cameraY < 0) cameraY = 0;
            if (cameraY > maxCameraY) cameraY = maxCameraY;
        }

        private float tBall() {
            return Math.max(20f, tile() * 0.36f);
        }

        private boolean circleRect(
                float cx,
                float cy,
                float r,
                RectF rect
        ) {
            float nx = Math.max(rect.left, Math.min(cx, rect.right));
            float ny = Math.max(rect.top, Math.min(cy, rect.bottom));

            float dx = cx - nx;
            float dy = cy - ny;

            return dx * dx + dy * dy < r * r;
        }

        private void restartAfterHit() {
            ballX = tile() * 2.5f;
            ballY = getHeight() * 0.62f;
            ballVX = 0;
            ballVY = 0;
            cameraX = 0;
            cameraY = 0;
        }

        private void completeLevel() {

            levelFinished = true;

            handler.postDelayed(() -> {

                if (level < 10) {
                    level++;
                    resetLevel();
                } else {
                    level = 1;
                    resetLevel();
                }

            }, 1200);
        }

        private void drawBackground(Canvas canvas) {

            int[] colors = backgrounds[backgroundType];

            LinearGradient gradient = new LinearGradient(
                    0,
                    0,
                    0,
                    getHeight(),
                    colors[0],
                    colors[2],
                    Shader.TileMode.CLAMP
            );

            paint.setShader(gradient);
            canvas.drawRect(
                    0,
                    0,
                    getWidth(),
                    getHeight(),
                    paint
            );
            paint.setShader(null);

            paint.setAlpha(35);

            for (int i = 0; i < 18; i++) {

                float px =
                        (i * 190f - cameraX * 0.12f)
                                % (getWidth() + 240);

                float py =
                        80 + (i * 83f - cameraY * 0.08f)
                                % (getHeight() + 100);

                canvas.drawCircle(
                        px,
                        py,
                        40 + (i % 4) * 16,
                        paint
                );
            }

            paint.setAlpha(255);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2);
            paint.setAlpha(25);

            float grid = tile() * 1.5f;

            for (float x = -grid; x < getWidth() + grid; x += grid) {
                canvas.drawLine(
                        x,
                        0,
                        x,
                        getHeight(),
                        paint
                );
            }

            for (float y = -grid; y < getHeight() + grid; y += grid) {
                canvas.drawLine(
                        0,
                        y,
                        getWidth(),
                        y,
                        paint
                );
            }

            paint.setStyle(Paint.Style.FILL);
            paint.setAlpha(255);
        }

        private void drawWorld(Canvas canvas) {

            canvas.save();
            canvas.translate(-cameraX, -cameraY);

            drawDecorations(canvas);

            for (RectF floor : floors) {
                drawFloor(canvas, floor);
            }

            for (RectF spike : spikes) {
                drawSpike(canvas, spike);
            }

            for (RectF ring : rings) {
                drawRing(canvas, ring);
            }

            for (RectF spring : springs) {
                drawSpring(canvas, spring);
            }

            drawFinish(canvas);

            drawBall(canvas);

            canvas.restore();
        }

        private void drawDecorations(Canvas canvas) {

            paint.setAlpha(45);

            for (int i = 0; i < 40; i++) {

                float x = i * 310f + 100;

                float y =
                        120 +
                                ((i * 173) %
                                        (int)Math.max(300, worldHeight - 200));

                paint.setStyle(Paint.Style.FILL);

                canvas.drawCircle(
                        x,
                        y,
                        18 + (i % 5) * 8,
                        paint
                );
            }

            paint.setAlpha(255);

            for (int i = 0; i < 25; i++) {

                float x = i * 410f + 150;
                float y = getHeight() * 0.84f;

                paint.setStyle(Paint.Style.FILL);
                paint.setAlpha(55);

                canvas.drawCircle(
                        x,
                        y,
                        55 + (i % 3) * 18,
                        paint
                );
            }

            paint.setAlpha(255);
        }

        private void drawFloor(Canvas canvas, RectF r) {

            float radius = tile() * 0.20f;

            paint.setStyle(Paint.Style.FILL);

            LinearGradient g = new LinearGradient(
                    r.left,
                    r.top,
                    r.left,
                    r.bottom,
                    0xFF71E58B,
                    0xFF075A35,
                    Shader.TileMode.CLAMP
            );

            paint.setShader(g);

            canvas.drawRoundRect(
                    r,
                    radius,
                    radius,
                    paint
            );

            paint.setShader(null);

            paint.setColor(0xFFB8FF91);
            canvas.drawRoundRect(
                    r.left,
                    r.top,
                    r.right,
                    r.top + tile() * 0.18f,
                    radius,
                    radius,
                    paint
            );

            paint.setColor(0xFF063A29);
            paint.setAlpha(100);

            canvas.drawRoundRect(
                    r.left + 5,
                    r.bottom - 10,
                    r.right - 5,
                    r.bottom,
                    radius,
                    radius,
                    paint
            );

            paint.setAlpha(255);

            paint.setColor(0xFF1F8F55);
            paint.setStrokeWidth(3);

            for (float x = r.left + 15; x < r.right; x += 42) {

                canvas.drawLine(
                        x,
                        r.top + tile() * 0.28f,
                        x + 8,
                        r.top + tile() * 0.55f,
                        paint
                );
            }
        }

        private void drawSpike(Canvas canvas, RectF r) {

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFFFF5361);

            path.reset();

            path.moveTo(r.left, r.bottom);
            path.lineTo(
                    r.centerX(),
                    r.top
            );
            path.lineTo(
                    r.right,
                    r.bottom
            );
            path.close();

            canvas.drawPath(path, paint);

            paint.setColor(0xFFFFD0D0);
            paint.setAlpha(150);

            path.reset();

            path.moveTo(
                    r.centerX(),
                    r.top + 5
            );
            path.lineTo(
                    r.centerX() + 5,
                    r.bottom - 5
            );

            canvas.drawPath(path, paint);

            paint.setAlpha(255);
        }

        private void drawRing(Canvas canvas, RectF r) {

            float cx = r.centerX();
            float cy = r.centerY();

            glowPaint.setStyle(Paint.Style.STROKE);
            glowPaint.setStrokeWidth(14);
            glowPaint.setColor(0x55FFE86B);

            canvas.drawCircle(
                    cx,
                    cy,
                    r.width() * 0.43f,
                    glowPaint
            );

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(7);

            LinearGradient ringGradient = new LinearGradient(
                    r.left,
                    r.top,
                    r.right,
                    r.bottom,
                    0xFFFFF06A,
                    0xFFFF8A24,
                    Shader.TileMode.CLAMP
            );

            paint.setShader(ringGradient);

            canvas.drawCircle(
                    cx,
                    cy,
                    r.width() * 0.38f,
                    paint
            );

            paint.setShader(null);
            paint.setStyle(Paint.Style.FILL);
        }

        private void drawSpring(Canvas canvas, RectF r) {

            paint.setColor(0xFF27C9FF);
            paint.setStyle(Paint.Style.FILL);

            canvas.drawRoundRect(
                    r,
                    8,
                    8,
                    paint
            );

            paint.setColor(0xFFEFFFFF);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3);

            for (int i = 0; i < 3; i++) {

                float yy =
                        r.top +
                                7 +
                                i * r.height() * 0.27f;

                canvas.drawLine(
                        r.left + 5,
                        yy,
                        r.right - 5,
                        yy,
                        paint
                );
            }

            paint.setStyle(Paint.Style.FILL);
        }

        private void drawFinish(Canvas canvas) {

            if (finish == null) return;

            paint.setStrokeWidth(5);
            paint.setColor(0xFFE9F6FF);

            canvas.drawLine(
                    finish.left + 5,
                    finish.top,
                    finish.left + 5,
                    finish.bottom + tile(),
                    paint
            );

            Path flag = new Path();

            flag.moveTo(
                    finish.left + 7,
                    finish.top
            );

            flag.lineTo(
                    finish.right,
                    finish.top + tile() * 0.35f
            );

            flag.lineTo(
                    finish.left + 7,
                    finish.top + tile() * 0.75f
            );

            flag.close();

            paint.setColor(0xFFFF3F4E);
            canvas.drawPath(flag, paint);

            paint.setColor(0xFFFFFFFF);
            paint.setAlpha(170);

            canvas.drawCircle(
                    finish.left + 12,
                    finish.top + 12,
                    4,
                    paint
            );

            paint.setAlpha(255);
        }

        private void drawBall(Canvas canvas) {

            float r = tBall();

            float shadowY = ballY + r * 0.92f;

            paint.setColor(0x55000000);
            canvas.drawOval(
                    ballX - r * 0.95f,
                    shadowY - r * 0.22f,
                    ballX + r * 0.95f,
                    shadowY + r * 0.22f,
                    paint
            );

            RadialGradient ballGradient = new RadialGradient(
                    ballX - r * 0.35f,
                    ballY - r * 0.42f,
                    r * 1.25f,
                    new int[]{
                            0xFFFFE8E8,
                            0xFFFF4C5A,
                            0xFFC50022,
                            0xFF690014
                    },
                    new float[]{
                            0f,
                            0.25f,
                            0.72f,
                            1f
                    },
                    Shader.TileMode.CLAMP
            );

            paint.setShader(ballGradient);

            canvas.drawCircle(
                    ballX,
                    ballY,
                    r,
                    paint
            );

            paint.setShader(null);

            paint.setColor(0xFFFFFFFF);
            paint.setAlpha(210);

            canvas.drawOval(
                    ballX - r * 0.52f,
                    ballY - r * 0.67f,
                    ballX - r * 0.05f,
                    ballY - r * 0.30f,
                    paint
            );

            paint.setAlpha(255);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2);
            paint.setColor(0x88FFFFFF);

            canvas.drawCircle(
                    ballX,
                    ballY,
                    r - 2,
                    paint
            );

            paint.setStyle(Paint.Style.FILL);
        }

        private void drawHud(Canvas canvas) {

            paint.setColor(0x77000000);

            canvas.drawRoundRect(
                    18,
                    18,
                    getWidth() - 18,
                    74,
                    28,
                    28,
                    paint
            );

            paint.setColor(Color.WHITE);
            paint.setTextSize(23);
            paint.setTypeface(Typeface.create(
                    Typeface.DEFAULT,
                    Typeface.BOLD
            ));

            paint.setTextAlign(Paint.Align.CENTER);

            canvas.drawText(
                    "🔴 مرحله " + level,
                    getWidth() / 2f,
                    55,
                    paint
            );

            paint.setTextAlign(Paint.Align.LEFT);
        }

        private void drawControls(Canvas canvas) {

            float size = Math.min(
                    86,
                    getWidth() * 0.19f
            );

            float bottom = getHeight() - 25;

            drawControl(
                    canvas,
                    28,
                    bottom - size,
                    size,
                    "◀"
            );

            drawControl(
                    canvas,
                    getWidth() / 2f - size / 2f,
                    bottom - size,
                    size,
                    "⬆"
            );

            drawControl(
                    canvas,
                    getWidth() - size - 28,
                    bottom - size,
                    size,
                    "▶"
            );
        }

        private void drawControl(
                Canvas canvas,
                float x,
                float y,
                float size,
                String symbol
        ) {

            paint.setColor(0x66000000);

            canvas.drawRoundRect(
                    x,
                    y,
                    x + size,
                    y + size,
                    25,
                    25,
                    paint
            );

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2);
            paint.setColor(0x99FFFFFF);

            canvas.drawRoundRect(
                    x + 2,
                    y + 2,
                    x + size - 2,
                    y + size - 2,
                    25,
                    25,
                    paint
            );

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.WHITE);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(size * 0.42f);

            canvas.drawText(
                    symbol,
                    x + size / 2,
                    y + size * 0.65f,
                    paint
            );

            paint.setTextAlign(Paint.Align.LEFT);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            drawBackground(canvas);
            drawWorld(canvas);
            drawHud(canvas);
            drawControls(canvas);
        }

        @Override
        public boolean onTouchEvent(android.view.MotionEvent event) {

            float x = event.getX();
            float y = event.getY();

            float size = Math.min(
                    86,
                    getWidth() * 0.19f
            );

            float bottom = getHeight() - 25;

            float leftX = 28;
            float jumpX = getWidth() / 2f - size / 2f;
            float rightX = getWidth() - size - 28;

            if (event.getAction() == MotionEvent.ACTION_DOWN ||
                    event.getAction() == MotionEvent.ACTION_MOVE) {

                left = false;
                right = false;

                if (y > bottom - size) {

                    if (x >= leftX &&
                            x <= leftX + size) {
                        left = true;
                    }

                    if (x >= rightX &&
                            x <= rightX + size) {
                        right = true;
                    }

                    if (x >= jumpX &&
                            x <= jumpX + size) {

                        if (Math.abs(ballVY) < 30) {
                            jumping = true;
                        }
                    }
                }

                invalidate();
                return true;
            }

            if (event.getAction() == MotionEvent.ACTION_UP ||
                    event.getAction() == MotionEvent.ACTION_CANCEL) {

                left = false;
                right = false;

                invalidate();
                return true;
            }

            return true;
        }

        private void gameLoop() {

            if (!gameRunning) return;

            long now = System.nanoTime();

            float dt =
                    (now - lastTime) / 1_000_000_000f;

            lastTime = now;

            update(dt);
            invalidate();

            postOnAnimation(this::gameLoop);
        }

        public void pauseGame() {
            gameRunning = false;
        }

        public void resumeGame() {

            gameRunning = true;
            lastTime = System.nanoTime();

            removeCallbacksAndMessages();

            gameLoop();
        }

        private void removeCallbacksAndMessages() {
            removeCallbacks(gameLoopRunnable);
        }

        private final Runnable gameLoopRunnable = new Runnable() {
            @Override
            public void run() {
                gameLoop();
            }
        };

        private void postOnAnimation(
                final Runnable runnable
        ) {
            postDelayed(runnable, 16);
        }
    }
                }
