package com.tajro.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.;
import android.view.;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import java.util.ArrayList;
import java.util.Random;

public class BallGameActivity extends Activity {

private ProBounceView gameView;

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
    private final ArrayList<RectF> spikes = new ArrayList<>();
    private final ArrayList<RectF> rings = new ArrayList<>();
    private final ArrayList<RectF> springs = new ArrayList<>();
    private final ArrayList<Particle> particles = new ArrayList<>();

    private RectF finish;

    private float ballX;
    private float ballY;
    private float ballVX;
    private float ballVY;

    private float spawnX;
    private float spawnY;

    private float cameraX;
    private float cameraY;

    private float groundY;

    private boolean leftPressed;
    private boolean rightPressed;
    private boolean jumpPressed;

    private boolean running;
    private boolean finished;

    private int level = 1;

    private long lastFrame;

    private float worldWidth;
    private float worldHeight;

    private float shake;
    private float time;

    private float doveX;
    private float doveY;
    private float doveStartX;
    private float doveStartY;
    private float doveTargetX;
    private float doveTargetY;
    private long finishStartTime;

    private boolean doveFlying;
    private boolean doveLanded;

    private SharedPreferences preferences;

    private final float gravity = 1450f;
    private final float maxSpeed = 410f;
    private final float acceleration = 1800f;
    private final float friction = 0.82f;
    private final float jumpPower = 650f;

    private final int[] skyTop = {
            0xFF062A20,
            0xFF063D61,
            0xFF11162F,
            0xFFE8F4FF,
            0xFF30140B,
            0xFF020516,
            0xFF126044,
            0xFFFF7C39,
            0xFFB9E7FF,
            0xFF9E5B18
    };

    private final int[] skyBottom = {
            0xFF27A66C,
            0xFF20A6D6,
            0xFF5D2F91,
            0xFFBDEBFF,
            0xFFE45D16,
            0xFF25115D,
            0xFF9BD52D,
            0xFF3E1421,
            0xFF75C5F0,
            0xFFE9C05A
    };

    private final int[] groundColors = {
            0xFF0B7449,
            0xFF087D96,
            0xFF47225F,
            0xFF5E8891,
            0xFF87350E,
            0xFF192052,
            0xFF327A20,
            0xFF79302D,
            0xFF2A7194,
            0xFF7A4616
    };

    public ProBounceView(Context context) {
        super(context);

        preferences =
                context.getSharedPreferences(
                        "tajro_bounce_progress",
                        Context.MODE_PRIVATE
                );

        level = preferences.getInt(
                "saved_level",
                1
        );

        if (level < 1) {
            level = 1;
        }

        if (level > 10) {
            level = 10;
        }

        p.setTypeface(
                Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                )
        );

        setFocusable(true);

        resetLevel();
    }

    private float tile() {
        return Math.max(
                42f,
                Math.min(
                        58f,
                        getWidth() * 0.075f
                )
        );
    }

    private float radius() {
        return tile() * 0.38f;
    }

    private void resetLevel() {

        platforms.clear();
        spikes.clear();
        rings.clear();
        springs.clear();
        particles.clear();

        finished = false;

        doveFlying = false;
        doveLanded = false;

        float t = tile();

        worldWidth =
                11000f +
                        level * 1350f;

        worldHeight =
                Math.max(
                        getHeight() * 2.8f,
                        1750f
                );

        groundY =
                getHeight() * 0.68f;

        spawnX =
                t * 3f;

        spawnY =
                groundY -
                        radius() -
                        5;

        ballX = spawnX;
        ballY = spawnY;

        ballVX = 0;
        ballVY = 0;

        cameraX = 0;
        cameraY = 0;

        buildWorld();

        createInitialParticles();

        invalidate();
    }

    private void buildWorld() {

        float t = tile();

        float x = t * 0.5f;
        float y = groundY;

        int direction = 1;
        int section = 0;

        while (x < worldWidth - t * 7) {

            section++;

            int length =
                    8 +
                            (section * 3 +
                                    level * 2) % 17;

            float left = x;
            float right =
                    x + length * t;

            RectF platform =
                    new RectF(
                            left,
                            y,
                            right,
                            y + t * 1.45f
                    );

            platforms.add(platform);

            addWorldObjects(
                    left,
                    right,
                    y,
                    section,
                    t
            );

            x = right;

            if (section %
                    (4 + level % 3) == 0) {

                direction *= -1;

                float vertical =
                        t *
                                (2.1f +
                                        level *
                                                0.08f);

                if (direction > 0) {
                    y -= vertical;
                } else {
                    y += vertical;
                }

                if (y <
                        getHeight() * 0.28f) {

                    y =
                            getHeight() *
                                    0.38f;

                    direction = -1;
                }

                if (y >
                        getHeight() * 0.76f) {

                    y =
                            getHeight() *
                                    0.63f;

                    direction = 1;
                }

                RectF connector =
                        new RectF(
                                x - t * 1.5f,
                                y,
                                x + t * 2.5f,
                                y + t * 1.45f
                        );

                platforms.add(connector);
            }

            if (section % 6 == 0) {

                float upperY =
                        Math.max(
                                t * 2,
                                y - t * 4.0f
                        );

                platforms.add(
                        new RectF(
                                x + t,
                                upperY,
                                x + t * 10,
                                upperY + t
                        )
                );
            }
        }

        finish =
                new RectF(
                        worldWidth - t * 5.0f,
                        y - t * 2.2f,
                        worldWidth - t * 2.0f,
                        y
                );
    }

    private void addWorldObjects(
            float left,
            float right,
            float y,
            int section,
            float t
    ) {

        if (section > 2 &&
                section %
                        Math.max(
                                4,
                                9 - level / 2
                        ) == 0) {

            float sx =
                    left +
                            (right - left) *
                                    0.48f;

            spikes.add(
                    new RectF(
                            sx,
                            y - t * 0.62f,
                            sx + t * 1.05f,
                            y
                    )
            );

            if (level >= 4) {

                spikes.add(
                        new RectF(
                                sx + t * 1.1f,
                                y - t * 0.62f,
                                sx + t * 2.15f,
                                y
                        )
                );
            }
        }

        if (section % 5 == 0) {

            float rx =
                    left +
                            (right - left) *
                                    0.70f;

            rings.add(
                    new RectF(
                            rx,
                            y - t * 2.15f,
                            rx + t * 1.25f,
                            y - t * 0.90f
                    )
            );
        }

        if (section % 8 == 0) {

            float sx =
                    left +
                            (right - left) *
                                    0.78f;

            springs.add(
                    new RectF(
                            sx,
                            y - t * 0.42f,
                            sx + t * 0.95f,
                            y
                    )
            );
        }

        if (level >= 5 &&
                section % 11 == 0) {

            float upper =
                    y -
                            t *
                                    (3.2f +
                                            level *
                                                    0.12f);

            platforms.add(
                    new RectF(
                            left + t * 2,
                            upper,
                            right - t * 1.5f,
                            upper + t * 0.9f
                    )
            );
        }
    }

    private void createInitialParticles() {

        for (int i = 0; i < 70; i++) {

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
                                    35f + 8,
                            random.nextFloat() *
                                    1.8f + 0.4f
                    )
            );
        }
    }

    private void update(float dt) {

        if (!running) {
            return;
        }

        if (dt > 0.035f) {
            dt = 0.035f;
        }

        time += dt;

        updateParticles(dt);

        if (finished) {

            updateDove();

            return;
        }

        if (leftPressed) {
            ballVX -=
                    acceleration * dt;
        }

        if (rightPressed) {
            ballVX +=
                    acceleration * dt;
        }

        if (!leftPressed &&
                !rightPressed) {

            ballVX *=
                    Math.pow(
                            friction,
                            dt * 60f
                    );
        }

        if (ballVX > maxSpeed) {
            ballVX = maxSpeed;
        }

        if (ballVX < -maxSpeed) {
            ballVX = -maxSpeed;
        }

        ballVY +=
                gravity * dt;

        float oldY = ballY;

        ballX +=
                ballVX * dt;

        ballY +=
                ballVY * dt;

        boolean landed = false;

        for (RectF platform :
                platforms) {

            if (ballX + radius() >
                    platform.left &&
                    ballX - radius() <
                            platform.right &&
                    oldY + radius() <=
                            platform.top &&
                    ballY + radius() >=
                            platform.top &&
                    ballVY >= 0) {

                ballY =
                        platform.top -
                                radius();

                ballVY = 0;

                landed = true;

                break;
            }
        }

        if (landed &&
                jumpPressed) {

            ballVY =
                    -jumpPower;

            jumpPressed = false;

            createJumpParticles();
        }

        for (RectF spring :
                springs) {

            if (circleRect(
                    ballX,
                    ballY,
                    radius(),
                    spring
            )) {

                ballVY =
                        -jumpPower *
                                (1.25f +
                                        level *
                                                0.025f);

                createJumpParticles();
            }
        }

        for (RectF spike :
                spikes) {

            if (circleRect(
                    ballX,
                    ballY,
                    radius() * 0.84f,
                    spike
            )) {

                hit();

                return;
            }
        }

        if (ballY >
                worldHeight +
                        getHeight()) {

            hit();

            return;
        }

        if (ballX < radius()) {

            ballX = radius();

            ballVX = 0;
        }

        if (finish != null &&
                ballX + radius() >
                        finish.left &&
                ballX - radius() <
                        finish.right &&
                ballY + radius() >
                        finish.top &&
                ballY - radius() <
                        finish.bottom) {

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
                (targetCameraX -
                        cameraX) *
                        Math.min(
                                1f,
                                dt * 5.5f
                        );

        cameraY +=
                (targetCameraY -
                        cameraY) *
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
        }
    }

    private void updateDove() {

        if (!finished) {
            return;
        }

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
                            (3f -
                                    2f *
                                            progress);

            doveX =
                    doveStartX +
                            (doveTargetX -
                                    doveStartX) *
                                    smooth;

            float arc =
                    (float)
                            Math.sin(
                                    progress *
                                            Math.PI
                            );

            doveY =
                    doveStartY +
                            (doveTargetY -
                                    doveStartY) *
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

        return dx * dx +
                dy * dy <
                r * r;
    }

    private void hit() {

        shake = 15;

        createHitParticles();

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

        if (finished) {
            return;
        }

        finished = true;

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

        saveProgress();
    }

    private void saveProgress() {

        if (preferences == null) {
            return;
        }

        int old =
                preferences.getInt(
                        "saved_level",
                        1
                );

        if (level > old) {

            preferences.edit()
                    .putInt(
                            "saved_level",
                            level
                    )
                    .apply();
        }
    }

    private void updateParticles(float dt) {

        for (int i =
                particles.size() - 1;
             i >= 0;
             i--) {

            Particle q =
                    particles.get(i);

            q.x +=
                    q.vx * dt;

            q.y +=
                    q.vy * dt;

            q.life -= dt;

            if (q.life <= 0) {
                particles.remove(i);
            }
        }
    }

    private void createJumpParticles() {

        for (int i = 0;
             i < 12;
             i++) {

            Particle q =
                    new Particle(
                            ballX,
                            ballY +
                                    radius(),
                            random.nextFloat() *
                                    80 - 40,
                            -random.nextFloat() *
                                    100 - 30
                    );

            q.life = 0.55f;

            particles.add(q);
        }
    }

    private void createHitParticles() {

        for (int i = 0;
             i < 26;
             i++) {

            Particle q =
                    new Particle(
                            ballX,
                            ballY,
                            random.nextFloat() *
                                    500 - 250,
                            random.nextFloat() *
                                    500 - 250
                    );

            q.life = 0.8f;

            particles.add(q);
        }
    }

    private void createFinishParticles() {

        for (int i = 0;
             i < 70;
             i++) {

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

            particles.add(q);
        }
    }

    private void createDoveParticles() {

        for (int i = 0;
             i < 20;
             i++) {

            Particle q =
                    new Particle(
                            doveX,
                            doveY,
                            random.nextFloat() *
                                    140 - 70,
                            random.nextFloat() *
                                    120 - 80
                    );

            q.life = 0.8f;

            particles.add(q);
        }
    }

    private void drawBackground(
            Canvas canvas
    ) {

        int index = level - 1;

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
        drawAtmosphere(canvas);
        drawParticles(canvas);
    }

    private void drawSkyObjects(
            Canvas canvas
    ) {

        int index = level - 1;

        p.setStyle(Paint.Style.FILL);

        if (index == 0 ||
                index == 6) {

            drawClouds(canvas);
            drawTrees(canvas);

        } else if (index == 1) {

            drawClouds(canvas);
            drawWaterLights(canvas);

        } else if (index == 2 ||
                index == 5) {

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

        } else if (index == 3) {

            p.setColor(
                    0x88FFFFFF
            );

            canvas.drawCircle(
                    getWidth() * 0.80f,
                    getHeight() * 0.18f,
                    48,
                    p
            );

        } else if (index == 4) {

            drawVolcanoSky(canvas);

        } else if (index == 7) {

            drawSunset(canvas);

        } else if (index == 8) {

            drawClouds(canvas);
            drawIcePeaks(canvas);

        } else {

            drawDesert(canvas);
        }
    }

    private void drawClouds(
            Canvas canvas
    ) {

        p.setColor(
                0x35FFFFFF
        );

        for (int i = 0;
             i < 7;
             i++) {

            float x =
                    (i * 220f -
                            cameraX * 0.15f)
                            %
                            (getWidth() + 260);

            if (x < -200) {
                x +=
                        getWidth() +
                                260;
            }

            float y =
                    80 +
                            (i % 4) *
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

        for (int i = 0;
             i < 13;
             i++) {

            float x =
                    (i * 130f -
                            cameraX * 0.20f)
                            %
                            (getWidth() + 180);

            if (x < -100) {
                x +=
                        getWidth() +
                                180;
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

        for (int i = 0;
             i < 45;
             i++) {

            float x =
                    (i * 97f -
                            cameraX * 0.08f)
                            %
                            (getWidth() + 50);

            if (x < 0) {
                x +=
                        getWidth() +
                                50;
            }

            float y =
                    35 +
                            (i * 53f) %
                                    (getHeight() *
                                            0.55f);

            float s =
                    1.5f +
                            (i % 3);

            canvas.drawCircle(
                    x,
                    y,
                    s,
                    p
            );
        }
    }

    private void drawWaterLights(
            Canvas canvas
    ) {

        p.setColor(
                0x55FFFFFF
        );

        for (int i = 0;
             i < 20;
             i++) {

            float x =
                    (i * 150f -
                            cameraX * 0.12f)
                            %
                            (getWidth() + 200);

            if (x < 0) {
                x +=
                        getWidth() +
                                200;
            }

            float y =
                    getHeight() *
                            0.72f +
                            (i % 4) *
                                    22;

            canvas.drawOval(
                    x,
                    y,
                    x + 75,
                    y + 5,
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
                getHeight() * 0.72f
        );

        volcano.lineTo(
                getWidth() * 0.52f,
                getHeight() * 0.28f
        );

        volcano.lineTo(
                getWidth(),
                getHeight() * 0.72f
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
                getWidth() * 0.51f,
                getHeight() * 0.37f,
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

        for (int i = 0;
             i < 6;
             i++) {

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

    private void drawIcePeaks(
            Canvas canvas
    ) {

        p.setColor(
                0x55FFFFFF
        );

        for (int i = 0;
             i < 6;
             i++) {

            float x =
                    i * 190 -
                            cameraX * 0.15f;

            Path peak =
                    new Path();

            peak.moveTo(
                    x,
                    getHeight() * 0.73f
            );

            peak.lineTo(
                    x + 100,
                    getHeight() * 0.30f
            );

            peak.lineTo(
                    x + 205,
                    getHeight() * 0.73f
            );

            peak.close();

            canvas.drawPath(
                    peak,
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

        for (int i = 0;
             i < 9;
             i++) {

            float x =
                    i * 190 -
                            cameraX * 0.10f;

            canvas.drawOval(
                    x,
                    getHeight() * 0.63f,
                    x + 230,
                    getHeight() * 0.78f,
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
                getHeight() * 0.70f,
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
                    sx >
                            getWidth() + 20 ||
                    sy < -20 ||
                    sy >
                            getHeight() + 20) {

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
                    0xFFFFFFFF
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
                    (random.nextFloat() -
                            0.5f) *
                            shake;

            sy =
                    (random.nextFloat() -
                            0.5f) *
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

        drawFinish(canvas);
        drawBall(canvas);

        canvas.restore();
    }

    private void drawPlatform(
            Canvas canvas,
            RectF r
    ) {

        int index = level - 1;

        LinearGradient g =
                new LinearGradient(
                        r.left,
                        r.top,
                        r.left,
                        r.bottom,
                        groundColors[index],
                        darken(
                                groundColors[index]
                        ),
                        Shader.TileMode.CLAMP
                );

        p.setShader(g);
        p.setStyle(
                Paint.Style.FILL
        );

        canvas.drawRoundRect(
                r,
                tile() * 0.18f,
                tile() * 0.18f,
                p
        );

        p.setShader(null);

        p.setColor(
                0xAAE6FF9A
        );

        canvas.drawRoundRect(
                r.left,
                r.top,
                r.right,
                r.top +
                        tile() * 0.16f,
                tile() * 0.08f,
                tile() * 0.08f,
                p
        );

        p.setColor(
                0x33000000
        );

        canvas.drawRoundRect(
                r.left + 5,
                r.bottom - 12,
                r.right - 5,
                r.bottom,
                8,
                8,
                p
        );

        p.setColor(
                0x553DFFB1
        );

        for (float x =
                r.left + 15;
             x <
                     r.right - 10;
             x += 45) {

            canvas.drawLine(
                    x,
                    r.top +
                            tile() * 0.32f,
                    x + 10,
                    r.top +
                            tile() * 0.60f,
                    p
            );
        }
    }

    private int darken(
            int color
    ) {

        int r =
                (int)
                        (
                                Color.red(
                                        color
                                ) *
                                        0.45f
                        );

        int g =
                (int)
                        (
                                Color.green(
                                        color
                                ) *
                                        0.45f
                        );

        int b =
                (int)
                        (
                                Color.blue(
                                        color
                                ) *
                                        0.45f
                        );

        return Color.rgb(
                r,
                g,
                b
        );
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
                        0xFFFFF5F5,
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
                0x99FFFFFF
        );

        canvas.drawCircle(
                r.centerX() - 3,
                r.top +
                        r.height() * 0.28f,
                3,
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

        glow.setStrokeWidth(17);

        glow.setColor(
                0x55FFF15A
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

        p.setStrokeWidth(7);

        LinearGradient rg =
                new LinearGradient(
                        r.left,
                        r.top,
                        r.right,
                        r.bottom,
                        0xFFFFFFFF,
                        0xFFFFA600,
                        Shader.TileMode.CLAMP
                );

        p.setShader(rg);

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
                0xFFFFFFFF
        );

        p.setAlpha(190);

        canvas.drawCircle(
                cx -
                        r.width() *
                                0.16f,
                cy -
                        r.height() *
                                0.16f,
                4,
                p
        );

        p.setAlpha(255);
    }

    private void drawSpring(
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
                        0xFF7EFFFF,
                        0xFF0877FF,
                        Shader.TileMode.CLAMP
                );

        p.setShader(g);

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
                0xFFFFFFFF
        );

        for (int i = 0;
             i < 3;
             i++) {

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

    private void drawFinish(
            Canvas canvas
    ) {

        if (finish == null) {
            return;
        }

        p.setStrokeWidth(6);
        p.setColor(
                0xFFFFFFFF
        );

        canvas.drawLine(
                finish.left + 7,
                finish.top,
                finish.left + 7,
                finish.bottom +
                        tile(),
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

        LinearGradient fg =
                new LinearGradient(
                        finish.left,
                        finish.top,
                        finish.right,
                        finish.bottom,
                        0xFFFF4B62,
                        0xFFC90038,
                        Shader.TileMode.CLAMP
                );

        p.setShader(fg);

        canvas.drawPath(
                shape,
                p
        );

        p.setShader(null);

        p.setColor(
                0xFFFFFFFF
        );

        p.setAlpha(200);

        canvas.drawCircle(
                finish.left + 14,
                finish.top + 12,
                4,
                p
        );

        p.setAlpha(255);
    }

    private void drawBall(
            Canvas canvas
    ) {

        float r = radius();

        p.setColor(
                0x55000000
        );

        canvas.drawOval(
                ballX - r * 1.05f,
                ballY + r * 0.72f,
                ballX + r * 1.05f,
                ballY + r * 1.10f,
                p
        );

        RadialGradient rg =
                new RadialGradient(
                        ballX - r * 0.34f,
                        ballY - r * 0.42f,
                        r * 1.25f,
                        new int[]{
                                0xFFFFFFFF,
                                0xFFFF8A94,
                                0xFFFF183D,
                                0xFF87001E
                        },
                        new float[]{
                                0f,
                                0.22f,
                                0.65f,
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

        p.setColor(
                0xFFFFFFFF
        );

        p.setAlpha(225);

        canvas.drawOval(
                ballX - r * 0.56f,
                ballY - r * 0.68f,
                ballX - r * 0.05f,
                ballY - r * 0.30f,
                p
        );

        p.setAlpha(255);

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
                finish.bottom +
                        t;

        p.setStrokeWidth(5);

        p.setColor(
                0xFFFFFFFF
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
                        flagX +
                                flagWidth,
                        flagTop +
                                flagHeight
                );

        p.setStyle(
                Paint.Style.FILL
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
                        flagHeight * 2f / 3f,
                p
        );

        p.setColor(
                0xFF007A36
        );

        canvas.drawRect(
                afFlag.left,
                afFlag.top +
                        flagHeight * 2f / 3f,
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
                flying ?
                        (float)
                                Math.sin(
                                        time * 13f
                                ) *
                                size *
                                0.22f :
                        0;

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
                                (0.30f +
                                        wing / size)
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
                                (0.25f +
                                        wing / size)
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
                18,
                18,
                getWidth() - 18,
                76,
                28,
                28,
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
                "🔴 مرحله " + level,
                getWidth() * 0.50f,
                56,
                p
        );

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
                    "مرحله کامل شد!",
                    getWidth() / 2f,
                    getHeight() * 0.28f,
                    p
            );

            p.setTextSize(20);

            if (level < 10) {

                canvas.drawText(
                        "🇦🇫  آماده مرحله بعد",
                        getWidth() / 2f,
                        getHeight() * 0.28f + 40,
                        p
                );

            } else {

                canvas.drawText(
                        "🇦🇫  مرحله آخر کامل شد",
                        getWidth() / 2f,
                        getHeight() * 0.28f + 40,
                        p
                );
            }

            p.setTextSize(17);

            canvas.drawText(
                    String.format(
                            java.util.Locale.US,
                            "%.0f ثانیه",
                            Math.max(
                                    0,
                                    6f - seconds
                            )
                    ),
                    getWidth() / 2f,
                    getHeight() * 0.28f + 72,
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

        if (finished) {
            return;
        }

        float size =
                Math.min(
                        88,
                        getWidth() * 0.19f
                );

        float bottom =
                getHeight() - 24;

        drawButton(
                canvas,
                25,
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
                        25,
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
                0x65000000
        );

        canvas.drawRoundRect(
                x,
                y,
                x + size,
                y + size,
                28,
                28,
                p
        );

        p.setStyle(
                Paint.Style.STROKE
        );

        p.setStrokeWidth(2);

        p.setColor(
                0x99FFFFFF
        );

        canvas.drawRoundRect(
                x + 2,
                y + 2,
                x + size - 2,
                y + size - 2,
                28,
                28,
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
                size * 0.42f
        );

        canvas.drawText(
                symbol,
                x + size / 2,
                y + size * 0.66f,
                p
        );

        p.setTextAlign(
                Paint.Align.LEFT
        );
    }

    @Override
    protected void onDraw(
            Canvas canvas
    ) {

        super.onDraw(canvas);

        drawBackground(canvas);
        drawWorld(canvas);

        if (finished) {
            drawDoveAndFlag(canvas);
        }

        drawHud(canvas);
        drawControls(canvas);
    }

    @Override
    public boolean onTouchEvent(
            MotionEvent event
    ) {

        if (finished) {
            return true;
        }

        float x =
                event.getX();

        float y =
                event.getY();

        float size =
                Math.min(
                        88,
                        getWidth() * 0.19f
                );

        float bottom =
                getHeight() - 24;

        float leftX = 25;

        float jumpX =
                getWidth() / 2f -
                        size / 2f;

        float rightX =
                getWidth() -
                        size -
                        25;

        int action =
                event.getActionMasked();

        if (action ==
                MotionEvent.ACTION_DOWN ||
                action ==
                        MotionEvent.ACTION_MOVE) {

            leftPressed = false;
            rightPressed = false;

            if (y >
                    bottom - size) {

                if (x >= leftX &&
                        x <=
                                leftX + size) {

                    leftPressed = true;
                }

                if (x >= rightX &&
                        x <=
                                rightX + size) {

                    rightPressed = true;
                }

                if (x >= jumpX &&
                        x <=
                                jumpX + size) {

                    if (Math.abs(
                            ballVY
                    ) < 35) {

                        jumpPressed = true;
                    }
                }
            }

            return true;
        }

        if (action ==
                MotionEvent.ACTION_UP ||
                action ==
                        MotionEvent.ACTION_CANCEL) {

            leftPressed = false;
            rightPressed = false;

            return true;
        }

        return true;
    }

    public void startGame() {

        running = true;

        lastFrame =
                SystemClock.uptimeMillis();

        removeCallbacks(
                frameRunnable
        );

        post(frameRunnable);
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

                    if (!running) {
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
}

}
