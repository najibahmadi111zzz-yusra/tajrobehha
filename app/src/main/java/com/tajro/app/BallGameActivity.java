package com.tajro.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

public class BallGameActivity extends Activity {

    private GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setBackgroundColor(
                Color.rgb(235, 248, 250)
        );

        gameView =
                new GameView();

        root.addView(
                gameView,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        LinearLayout controls =
                new LinearLayout(this);

        controls.setGravity(
                Gravity.CENTER
        );

        controls.setPadding(
                12,
                8,
                12,
                18
        );

        Button left =
                new Button(this);

        left.setText("◀");
        left.setTextSize(27);

        Button jump =
                new Button(this);

        jump.setText("⬆");
        jump.setTextSize(27);

        Button right =
                new Button(this);

        right.setText("▶");
        right.setTextSize(27);

        controls.addView(
                left,
                new LinearLayout.LayoutParams(
                        0,
                        90,
                        1
                )
        );

        controls.addView(
                jump,
                new LinearLayout.LayoutParams(
                        0,
                        90,
                        1
                )
        );

        controls.addView(
                right,
                new LinearLayout.LayoutParams(
                        0,
                        90,
                        1
                )
        );

        root.addView(controls);

        setContentView(root);

        left.setOnClickListener(
                v -> gameView.moveLeft()
        );

        right.setOnClickListener(
                v -> gameView.moveRight()
        );

        jump.setOnClickListener(
                v -> gameView.jump()
        );
    }

    class GameView extends View {

        Paint paint =
                new Paint(Paint.ANTI_ALIAS_FLAG);

        float ballX;
        float ballY;

        float velocityY = 0;

        boolean jumping = false;

        int currentCell = 0;

        int level = 1;

        final int MAX_LEVEL = 10;

        float groundY;

        float cellSize = 70;

        int cellCount;

        GameView() {

            super(BallGameActivity.this);

            paint.setTypeface(
                    android.graphics.Typeface.DEFAULT_BOLD
            );

            setFocusable(true);

            resetLevel();
        }

        void resetLevel() {

            currentCell = 0;

            ballX = 65;

            ballY = 300;

            velocityY = 0;

            jumping = false;

            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {

            super.onDraw(canvas);

            float width =
                    getWidth();

            float height =
                    getHeight();

            canvas.drawColor(
                    Color.rgb(235, 248, 250)
            );

            groundY =
                    height - 175;

            cellCount =
                    Math.min(
                            8 + level / 2,
                            11
                    );

            cellSize =
                    Math.min(
                            72,
                            (width - 40) /
                                    (float) cellCount
                    );

            // ==============================
            // عنوان
            // ==============================

            paint.setStyle(
                    Paint.Style.FILL
            );

            paint.setTextAlign(
                    Paint.Align.CENTER
            );

            paint.setColor(
                    Color.rgb(12, 91, 120)
            );

            paint.setTextSize(34);

            canvas.drawText(
                    "🎮 توپ در خانه‌ها",
                    width / 2,
                    48,
                    paint
            );

            paint.setTextSize(22);

            canvas.drawText(
                    "مرحله " + level + " از " + MAX_LEVEL,
                    width / 2,
                    82,
                    paint
            );

            // ==============================
            // مسیر
            // ==============================

            float totalWidth =
                    cellCount * cellSize;

            float startX =
                    (width - totalWidth) / 2f;

            for (int i = 0;
                 i < cellCount;
                 i++) {

                float x =
                        startX +
                        i * cellSize;

                RectF cell =
                        new RectF(
                                x + 3,
                                groundY,
                                x + cellSize - 5,
                                groundY + 62
                        );

                if (i == cellCount - 1) {

                    paint.setColor(
                            Color.rgb(
                                    65,
                                    175,
                                    95
                            )
                    );

                } else {

                    if (i % 2 == 0) {

                        paint.setColor(
                                Color.rgb(
                                        210,
                                        230,
                                        235
                                )
                        );

                    } else {

                        paint.setColor(
                                Color.rgb(
                                        195,
                                        220,
                                        228
                                )
                        );
                    }
                }

                paint.setStyle(
                        Paint.Style.FILL
                );

                canvas.drawRoundRect(
                        cell,
                        14,
                        14,
                        paint
                );

                paint.setStyle(
                        Paint.Style.STROKE
                );

                paint.setStrokeWidth(3);

                paint.setColor(
                        Color.rgb(
                                120,
                                165,
                                175
                        )
                );

                canvas.drawRoundRect(
                        cell,
                        14,
                        14,
                        paint
                );

                paint.setStyle(
                        Paint.Style.FILL
                );

                if (i == cellCount - 1) {

                    paint.setTextSize(22);
                    paint.setColor(Color.WHITE);

                    canvas.drawText(
                            "★",
                            cell.centerX(),
                            cell.centerY() + 8,
                            paint
                    );
                }
            }

            // ==============================
            // بعضی موانع در مراحل بالاتر
            // ==============================

            if (level >= 3) {

                for (int i = 2;
                     i < cellCount - 1;
                     i += 3) {

                    float x =
                            startX +
                            i * cellSize +
                            cellSize / 2;

                    paint.setColor(
                            Color.rgb(
                                    225,
                                    105,
                                    80
                            )
                    );

                    canvas.drawCircle(
                            x,
                            groundY - 8,
                            9,
                            paint
                    );
                }
            }

            // ==============================
            // توپ
            // ==============================

            paint.setColor(
                    Color.rgb(
                            30,
                            120,
                            220
                    )
            );

            canvas.drawCircle(
                    ballX,
                    ballY,
                    26,
                    paint
            );

            paint.setColor(
                    Color.rgb(
                            155,
                            225,
                            255
                    )
            );

            canvas.drawCircle(
                    ballX - 8,
                    ballY - 9,
                    7,
                    paint
            );

            // ==============================
            // فیزیک پرش
            // ==============================

            if (jumping) {

                velocityY +=
                        1.0f +
                        (level * 0.04f);

                ballY += velocityY;

                if (ballY >=
                        groundY - 26) {

                    ballY =
                            groundY - 26;

                    velocityY = 0;

                    jumping = false;
                }

                invalidate();
            }
        }

        void moveLeft() {

            float step =
                    cellSize;

            ballX -= step;

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

            float step =
                    cellSize;

            ballX += step;

            float maxX =
                    getWidth() - 35;

            if (ballX > maxX) {

                ballX = maxX;
            }

            currentCell =
                    Math.min(
                            cellCount - 1,
                            currentCell + 1
                    );

            if (currentCell ==
                    cellCount - 1) {

                finishLevel();

                return;
            }

            invalidate();
        }

        void finishLevel() {

            if (level < MAX_LEVEL) {

                Toast.makeText(
                        BallGameActivity.this,
                        "🎉 مرحله " +
                                level +
                                " تمام شد!",
                        Toast.LENGTH_SHORT
                ).show();

                level++;

                resetLevel();

            } else {

                Toast.makeText(
                        BallGameActivity.this,
                        "🏆 آفرین! همه ۱۰ مرحله تمام شد!",
                        Toast.LENGTH_LONG
                ).show();

                level = 1;

                resetLevel();
            }
        }

        void jump() {

            if (!jumping) {

                jumping = true;

                velocityY =
                        -17 -
                        (level * 0.2f);

                invalidate();
            }
        }

        @Override
        public boolean onTouchEvent(
                MotionEvent event
        ) {

            if (event.getAction() ==
                    MotionEvent.ACTION_DOWN) {

                float x =
                        event.getX();

                float third =
                        getWidth() / 3f;

                if (x < third) {

                    moveLeft();

                } else if (
                        x > third * 2
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
