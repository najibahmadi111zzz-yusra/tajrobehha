package com.tajro.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

public class BallGameActivity extends Activity {

    GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(235, 248, 250));

        gameView = new GameView();

        root.addView(
                gameView,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(android.view.Gravity.CENTER);
        controls.setPadding(12, 12, 12, 20);

        Button left = new Button(this);
        left.setText("◀");
        left.setTextSize(28);

        Button jump = new Button(this);
        jump.setText("⬆");
        jump.setTextSize(28);

        Button right = new Button(this);
        right.setText("▶");
        right.setTextSize(28);

        controls.addView(
                left,
                new LinearLayout.LayoutParams(0, 90, 1)
        );

        controls.addView(
                jump,
                new LinearLayout.LayoutParams(0, 90, 1)
        );

        controls.addView(
                right,
                new LinearLayout.LayoutParams(0, 90, 1)
        );

        root.addView(controls);

        setContentView(root);

        left.setOnClickListener(v -> {
            gameView.moveLeft();
        });

        right.setOnClickListener(v -> {
            gameView.moveRight();
        });

        jump.setOnClickListener(v -> {
            gameView.jump();
        });
    }

    class GameView extends View {

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        float ballX = 100;
        float ballY = 300;

        float velocityY = 0;

        boolean jumping = false;

        float groundY = 500;

        int currentCell = 0;

        GameView() {
            super(BallGameActivity.this);

            paint.setTypeface(
                    android.graphics.Typeface.DEFAULT_BOLD
            );

            setFocusable(true);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            float width = getWidth();
            float height = getHeight();

            canvas.drawColor(
                    Color.rgb(235, 248, 250)
            );

            groundY = height - 180;

            // عنوان
            paint.setColor(Color.rgb(12, 91, 120));
            paint.setTextSize(42);
            paint.setTextAlign(Paint.Align.CENTER);

            canvas.drawText(
                    "🎮 توپ در خانه‌ها",
                    width / 2,
                    55,
                    paint
            );

            // شماره مرحله
            paint.setTextSize(25);

            canvas.drawText(
                    "مرحله ۱",
                    width / 2,
                    95,
                    paint
            );

            // خانه‌های مسیر
            float cellSize = 75;
            float startX = 30;

            for (int i = 0; i < 8; i++) {

                float x = startX + i * cellSize;

                paint.setColor(
                        Color.rgb(210, 230, 235)
                );

                RectF cell = new RectF(
                        x,
                        groundY,
                        x + 65,
                        groundY + 65
                );

                canvas.drawRoundRect(
                        cell,
                        14,
                        14,
                        paint
                );

                paint.setColor(
                        Color.rgb(160, 190, 200)
                );

                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(3);

                canvas.drawRoundRect(
                        cell,
                        14,
                        14,
                        paint
                );

                paint.setStyle(Paint.Style.FILL);
            }

            // خانه پایان
            paint.setColor(
                    Color.rgb(80, 180, 100)
            );

            RectF finish = new RectF(
                    startX + 7 * cellSize,
                    groundY,
                    startX + 7 * cellSize + 65,
                    groundY + 65
            );

            canvas.drawRoundRect(
                    finish,
                    14,
                    14,
                    paint
            );

            paint.setColor(Color.WHITE);
            paint.setTextSize(20);
            paint.setTextAlign(Paint.Align.CENTER);

            canvas.drawText(
                    "🏁",
                    finish.centerX(),
                    finish.centerY() + 8,
                    paint
            );

            // توپ
            paint.setColor(
                    Color.rgb(30, 120, 220)
            );

            canvas.drawCircle(
                    ballX,
                    ballY,
                    27,
                    paint
            );

            // درخشش توپ
            paint.setColor(
                    Color.rgb(150, 220, 255)
            );

            canvas.drawCircle(
                    ballX - 9,
                    ballY - 9,
                    7,
                    paint
            );

            // فیزیک پرش
            if (jumping) {

                velocityY += 1.1f;
                ballY += velocityY;

                if (ballY >= groundY - 27) {

                    ballY = groundY - 27;

                    velocityY = 0;

                    jumping = false;
                }

                invalidate();
            }
        }

        void moveLeft() {

            ballX -= 45;

            if (ballX < 35) {
                ballX = 35;
            }

            currentCell =
                    Math.max(
                            0,
                            currentCell - 1
                    );

            invalidate();
        }

        void moveRight() {

            ballX += 45;

            float maxX =
                    getWidth() - 35;

            if (ballX > maxX) {
                ballX = maxX;
            }

            currentCell =
                    Math.min(
                            7,
                            currentCell + 1
                    );

            if (currentCell == 7) {

                ballX =
                        30 + 7 * 75 + 32;

                android.widget.Toast.makeText(
                        BallGameActivity.this,
                        "🎉 مرحله تمام شد!",
                        android.widget.Toast.LENGTH_SHORT
                ).show();
            }

            invalidate();
        }

        void jump() {

            if (!jumping) {

                jumping = true;

                velocityY = -18;

                invalidate();
            }
        }

        @Override
        public boolean onTouchEvent(
                MotionEvent event
        ) {

            if (
                    event.getAction()
                            == MotionEvent.ACTION_DOWN
            ) {

                float x =
                        event.getX();

                if (
                        x < getWidth() / 3
                ) {

                    moveLeft();

                } else if (
                        x > getWidth() * 2 / 3
                ) {

                    moveRight();

                } else {

                    jump();
                }

                return true;
            }

            return true;
        }
    }
}
