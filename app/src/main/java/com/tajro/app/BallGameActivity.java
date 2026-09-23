package com.tajro.app;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class BallGameActivity extends Activity {

    private BounceView gameView;
    private TextView levelText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Color.rgb(30, 55, 42));

        levelText = new TextView(this);
        levelText.setTextColor(Color.WHITE);
        levelText.setTextSize(19);
        levelText.setGravity(Gravity.CENTER);
        levelText.setPadding(0, 18, 0, 12);

        root.addView(
                levelText,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        gameView = new BounceView();

        root.addView(
                gameView,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER);
        controls.setPadding(8, 8, 8, 16);

        Button left = new Button(this);
        left.setText("◀");
        left.setTextSize(25);
        left.setTextColor(Color.WHITE);
        left.setAllCaps(false);

        Button up = new Button(this);
        up.setText("⬆");
        up.setTextSize(25);
        up.setTextColor(Color.WHITE);
        up.setAllCaps(false);

        Button right = new Button(this);
        right.setText("▶");
        right.setTextSize(25);
        right.setTextColor(Color.WHITE);
        right.setAllCaps(false);

        controls.addView(left, buttonParams());
        controls.addView(up, buttonParams());
        controls.addView(right, buttonParams());

        root.addView(controls);

        setContentView(root);

        left.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN ||
                    event.getAction() == MotionEvent.ACTION_MOVE) {
                gameView.moveLeft = true;
            } else if (event.getAction() == MotionEvent.ACTION_UP ||
                    event.getAction() == MotionEvent.ACTION_CANCEL) {
                gameView.moveLeft = false;
            }
            return true;
        });

        right.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN ||
                    event.getAction() == MotionEvent.ACTION_MOVE) {
                gameView.moveRight = true;
            } else if (event.getAction() == MotionEvent.ACTION_UP ||
                    event.getAction() == MotionEvent.ACTION_CANCEL) {
                gameView.moveRight = false;
            }
            return true;
        });

        up.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                gameView.jump();
            }
            return true;
        });

        updateLevelText();
    }

    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(
                        0,
                        70,
                        1
                );
        p.setMargins(6, 0, 6, 0);
        return p;
    }

    private void updateLevelText() {
        if (levelText != null && gameView != null) {
            levelText.setText(
                    "🔴 Bounce • مرحله " +
                            gameView.level +
                            " از ۱۰"
            );
        }
    }

    class BounceView extends View {

        private final Paint paint =
                new Paint(Paint.ANTI_ALIAS_FLAG);

        private final List<RectF> platforms =
                new ArrayList<>();

        private final List<RectF> spikes =
                new ArrayList<>();

        private final List<RectF> rings =
                new ArrayList<>();

        private final List<RectF> holes =
                new ArrayList<>();

        private float ballX;
        private float ballY;
        private float velocityX;
        private float velocityY;

        private float cameraX;

        private boolean moveLeft;
        private boolean moveRight;

        private boolean jumping;

        private int level = 1;

        private final int maxLevel = 10;

        private final float ballRadius = 17f;

        private long lastTime;

        BounceView() {
            super(BallGameActivity.this);

            paint.setStrokeWidth(4);
            setFocusable(true);

            post(() -> {
                createLevel();
                lastTime = System.currentTimeMillis();
                invalidate();
            });
        }

        void createLevel() {

            platforms.clear();
            spikes.clear();
            rings.clear();
            holes.clear();

            cameraX = 0;

            float y = 430;

            int length =
                    3000 +
                            level * 900;

            float x = 80;

            platforms.add(
                    new RectF(
                            x,
                            y,
                            x + 300,
                            y + 35
                    )
            );

            float currentX = x + 300;
            float currentY = y;

            int sections =
                    18 + level * 4;

            for (int i = 0; i < sections; i++) {

                int pattern =
                        (i + level * 2) % 6;

                float sectionWidth =
                        170 +
                                ((i * 37 + level * 19) % 130);

                if (pattern == 0) {
                    currentY -= 95;
                } else if (pattern == 1) {
                    currentY += 100;
                } else if (pattern == 2) {
                    currentY -= 55;
                } else if (pattern == 3) {
                    currentY += 55;
                } else if (pattern == 4) {
                    currentY -= 120;
                } else {
                    currentY += 120;
                }

                if (currentY < 190) {
                    currentY = 190;
                }

                if (currentY > 600) {
                    currentY = 600;
                }

                float gap =
                        25 +
                                ((i * 13 + level * 7) % 35);

                currentX += gap;

                RectF platform =
                        new RectF(
                                currentX,
                                currentY,
                                currentX + sectionWidth,
                                currentY + 35
                        );

                platforms.add(platform);

                if (i % 3 == 1) {
                    spikes.add(
                            new RectF(
                                    currentX + sectionWidth * 0.35f,
                                    currentY - 28,
                                    currentX + sectionWidth * 0.50f,
                                    currentY
                            )
                    );
                }

                if (i % 4 == 0) {
                    rings.add(
                            new RectF(
                                    currentX + sectionWidth * 0.70f,
                                    currentY - 85,
                                    currentX + sectionWidth * 0.70f + 42,
                                    currentY - 43
                            )
                    );
                }

                if (i % 5 == 2) {
                    holes.add(
                            new RectF(
                                    currentX + sectionWidth * 0.55f,
                                    currentY + 35,
                                    currentX + sectionWidth * 0.82f,
                                    currentY + 70
                            )
                    );
                }

                currentX += sectionWidth;
            }

            platforms.add(
                    new RectF(
                            currentX + 40,
                            currentY,
                            currentX + 430,
                            currentY + 35
                    )
            );

            rings.add(
                    new RectF(
                            currentX + 250,
                            currentY - 90,
                            currentX + 300,
                            currentY - 40
                    )
            );

            ballX = 125;
            ballY = y - ballRadius - 3;

            velocityX = 0;
            velocityY = 0;

            jumping = false;

            invalidate();
        }

        void jump() {

            if (!jumping) {
                velocityY = -620;
                jumping = true;
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {

            super.onDraw(canvas);

            int width = getWidth();
            int height = getHeight();

            drawBackground(canvas, width, height);

            canvas.save();
            canvas.translate(-cameraX, 0);

            drawWorld(canvas);

            drawBall(canvas);

            canvas.restore();

            drawProgress(canvas, width);

            long now =
                    System.currentTimeMillis();

            if (lastTime == 0) {
                lastTime = now;
            }

            float dt =
                    Math.min(
                            0.035f,
                            (now - lastTime) / 1000f
                    );

            lastTime = now;

            updateGame(dt);

            postInvalidateDelayed(16);
        }

        private void drawBackground(
                Canvas canvas,
                int width,
                int height
        ) {

            paint.setStyle(Paint.Style.FILL);

            paint.setColor(
                    Color.rgb(105, 175, 135)
            );

            canvas.drawRect(
                    0,
                    0,
                    width,
                    height,
                    paint
            );

            paint.setColor(
                    Color.rgb(75, 145, 105)
            );

            for (int i = 0; i < width + 200; i += 90) {

                float hillX =
                        i -
                                (cameraX * 0.18f) % 90;

                RectF hill =
                        new RectF(
                                hillX,
                                height - 160,
                                hillX + 130,
                                height + 30
                        );

                canvas.drawOval(
                        hill,
                        paint
                );
            }

            paint.setColor(
                    Color.rgb(155, 210, 165)
            );

            for (int i = 0; i < width + 150; i += 140) {

                float treeX =
                        i -
                                (cameraX * 0.28f) % 140;

                canvas.drawCircle(
                        treeX,
                        90,
                        30,
                        paint
                );

                canvas.drawCircle(
                        treeX + 30,
                        105,
                        25,
                        paint
                );

                canvas.drawCircle(
                        treeX - 25,
                        110,
                        23,
                        paint
                );
            }
        }

        private void drawWorld(Canvas canvas) {

            paint.setStyle(Paint.Style.FILL);

            for (RectF platform : platforms) {

                paint.setColor(
                        Color.rgb(48, 92, 65)
                );

                canvas.drawRoundRect(
                        platform,
                        8,
                        8,
                        paint
                );

                paint.setColor(
                        Color.rgb(120, 205, 95)
                );

                RectF grass =
                        new RectF(
                                platform.left,
                                platform.top,
                                platform.right,
                                platform.top + 9
                        );

                canvas.drawRoundRect(
                        grass,
                        5,
                        5,
                        paint
                );

                paint.setColor(
                        Color.rgb(75, 135, 75)
                );

                for (float x = platform.left + 12;
                     x < platform.right - 5;
                     x += 28) {

                    canvas.drawRect(
                            x,
                            platform.top + 12,
                            x + 8,
                            platform.bottom - 4,
                            paint
                    );
                }
            }

            for (RectF spike : spikes) {

                paint.setColor(
                        Color.rgb(225, 55, 55)
                );

                float center =
                        (spike.left + spike.right) / 2;

                float[] pointsX = {
                        spike.left,
                        center,
                        spike.right
                };

                float[] pointsY = {
                        spike.bottom,
                        spike.top,
                        spike.bottom
                };

                android.graphics.Path path =
                        new android.graphics.Path();

                path.moveTo(
                        pointsX[0],
                        pointsY[0]
                );

                path.lineTo(
                        pointsX[1],
                        pointsY[1]
                );

                path.lineTo(
                        pointsX[2],
                        pointsY[2]
                );

                path.close();

                canvas.drawPath(
                        path,
                        paint
                );
            }

            for (RectF ring : rings) {

                paint.setStyle(
                        Paint.Style.STROKE
                );

                paint.setStrokeWidth(8);

                paint.setColor(
                        Color.rgb(245, 205, 45)
                );

                canvas.drawOval(
                        ring,
                        paint
                );

                paint.setStrokeWidth(3);

                paint.setColor(
                        Color.WHITE
                );

                canvas.drawOval(
                        ring,
                        paint
                );

                paint.setStyle(
                        Paint.Style.FILL
                );
            }

            for (RectF hole : holes) {

                paint.setColor(
                        Color.rgb(25, 45, 35)
                );

                canvas.drawOval(
                        hole,
                        paint
                );

                paint.setColor(
                        Color.rgb(45, 70, 50)
                );

                canvas.drawOval(
                        new RectF(
                                hole.left + 8,
                                hole.top + 4,
                                hole.right - 8,
                                hole.bottom - 4
                        ),
                        paint
                );
            }

            RectF finish =
                    platforms.get(
                            platforms.size() - 1
                    );

            paint.setColor(
                    Color.rgb(240, 215, 50)
            );

            canvas.drawRect(
                    finish.left + 30,
                    finish.top - 70,
                    finish.left + 38,
                    finish.top,
                    paint
            );

            for (int i = 0; i < 5; i++) {

                paint.setColor(
                        i % 2 == 0
                                ? Color.WHITE
                                : Color.rgb(230, 55, 55)
                );

                canvas.drawRect(
                        finish.left + 38 + i * 16,
                        finish.top - 70,
                        finish.left + 54 + i * 16,
                        finish.top - 52,
                        paint
                );
            }

            paint.setColor(Color.WHITE);
            paint.setTextSize(22);
            paint.setTypeface(
                    android.graphics.Typeface.DEFAULT_BOLD
            );

            canvas.drawText(
                    "FINISH",
                    finish.left + 20,
                    finish.top - 82,
                    paint
            );
        }

        private void drawBall(Canvas canvas) {

            paint.setStyle(Paint.Style.FILL);

            paint.setColor(
                    Color.argb(
                            70,
                            0,
                            0,
                            0
                    )
            );

            canvas.drawCircle(
                    ballX + 6,
                    ballY + 8,
                    ballRadius + 2,
                    paint
            );

            paint.setColor(
                    Color.rgb(220, 35, 35)
            );

            canvas.drawCircle(
                    ballX,
                    ballY,
                    ballRadius,
                    paint
            );

            paint.setColor(
                    Color.rgb(255, 105, 105)
            );

            canvas.drawCircle(
                    ballX - 6,
                    ballY - 7,
                    6,
                    paint
            );

            paint.setColor(
                    Color.rgb(160, 20, 20)
            );

            paint.setStyle(
                    Paint.Style.STROKE
            );

            paint.setStrokeWidth(3);

            canvas.drawCircle(
                    ballX,
                    ballY,
                    ballRadius,
                    paint
            );

            paint.setStyle(
                    Paint.Style.FILL
            );
        }

        private void drawProgress(
                Canvas canvas,
                int width
        ) {

            paint.setColor(
                    Color.argb(
                            110,
                            0,
                            0,
                            0
                    )
            );

            canvas.drawRoundRect(
                    new RectF(
                            12,
                            10,
                            width - 12,
                            22
                    ),
                    8,
                    8,
                    paint
            );

            float total =
                    platforms.get(
                            platforms.size() - 1
                    ).right;

            float progress =
                    Math.min(
                            1f,
                            ballX / total
                    );

            paint.setColor(
                    Color.rgb(
                            235,
                            215,
                            55
                    )
            );

            canvas.drawRoundRect(
                    new RectF(
                            12,
                            10,
                            12 +
                                    (width - 24) *
                                            progress,
                            22
                    ),
                    8,
                    8,
                    paint
            );
        }

        private void updateGame(float dt) {

            float speed = 260;

            if (moveLeft) {
                velocityX = -speed;
            } else if (moveRight) {
                velocityX = speed;
            } else {
                velocityX *= 0.88f;
            }

            velocityY += 1450 * dt;

            float oldY = ballY;

            ballX += velocityX * dt;
            ballY += velocityY * dt;

            if (ballX < 25) {
                ballX = 25;
                velocityX = 0;
            }

            boolean landed = false;

            for (RectF platform : platforms) {

                if (ballX + ballRadius > platform.left &&
                        ballX - ballRadius < platform.right &&
                        oldY + ballRadius <= platform.top &&
                        ballY + ballRadius >= platform.top &&
                        velocityY >= 0) {

                    ballY =
                            platform.top -
                                    ballRadius;

                    velocityY = 0;
                    jumping = false;
                    landed = true;

                    break;
                }
            }

            if (!landed && velocityY > 0) {
                jumping = true;
            }

            for (RectF spike : spikes) {

                if (circleIntersects(
                        ballX,
                        ballY,
                        ballRadius,
                        spike
                )) {

                    failLevel();
                    return;
                }
            }

            for (RectF hole : holes) {

                if (circleIntersects(
                        ballX,
                        ballY,
                        ballRadius,
                        hole
                )) {

                    failLevel();
                    return;
                }
            }

            for (RectF ring : rings) {

                if (circleIntersects(
                        ballX,
                        ballY,
                        ballRadius,
                        ring
                )) {

                    ring.set(
                            ring.left,
                            -1000,
                            ring.right,
                            -950
                    );
                }
            }

            RectF finish =
                    platforms.get(
                            platforms.size() - 1
                    );

            if (ballX > finish.right - 70) {

                completeLevel();
                return;
            }

            if (ballY > getHeight() + 250) {

                failLevel();
                return;
            }

            float targetCamera =
                    ballX -
                            getWidth() * 0.38f;

            if (targetCamera < 0) {
                targetCamera = 0;
            }

            float maxCamera =
                    Math.max(
                            0,
                            platforms.get(
                                    platforms.size() - 1
                            ).right -
                                    getWidth()
                    );

            if (targetCamera > maxCamera) {
                targetCamera = maxCamera;
            }

            cameraX +=
                    (targetCamera - cameraX) *
                            Math.min(
                                    1f,
                                    dt * 5
                            );
        }

        private boolean circleIntersects(
                float cx,
                float cy,
                float radius,
                RectF rect
        ) {

            float nearestX =
                    Math.max(
                            rect.left,
                            Math.min(
                                    cx,
                                    rect.right
                            )
                    );

            float nearestY =
                    Math.max(
                            rect.top,
                            Math.min(
                                    cy,
                                    rect.bottom
                            )
                    );

            float dx =
                    cx - nearestX;

            float dy =
                    cy - nearestY;

            return dx * dx + dy * dy <
                    radius * radius;
        }

        private void failLevel() {

            Toast.makeText(
                    BallGameActivity.this,
                    "💥 اوه! دوباره امتحان کن",
                    Toast.LENGTH_SHORT
            ).show();

            createLevel();
        }

        private void completeLevel() {

            if (level < maxLevel) {

                Toast.makeText(
                        BallGameActivity.this,
                        "🎉 مرحله " +
                                level +
                                " تمام شد!",
                        Toast.LENGTH_SHORT
                ).show();

                level++;

                updateLevelText();

                createLevel();

            } else {

                Toast.makeText(
                        BallGameActivity.this,
                        "🏆 عالی! همه ۱۰ مرحله را تمام کردی!",
                        Toast.LENGTH_LONG
                ).show();

                level = 1;

                updateLevelText();

                createLevel();
            }
        }

        @Override
        public boolean onTouchEvent(
                MotionEvent event
        ) {

            if (event.getAction() ==
                    MotionEvent.ACTION_DOWN) {

                if (event.getX() <
                        getWidth() / 3f) {

                    moveLeft = true;

                } else if (event.getX() >
                        getWidth() * 2f / 3f) {

                    moveRight = true;

                } else {

                    jump();
                }

                return true;
            }

            if (event.getAction() ==
                    MotionEvent.ACTION_UP ||
                    event.getAction() ==
                            MotionEvent.ACTION_CANCEL) {

                moveLeft = false;
                moveRight = false;

                return true;
            }

            return true;
        }
    }
        }
