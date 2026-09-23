package com.tajro.app;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
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

public class ColorSortActivity extends Activity {

    private SortGameView gameView;
    private TextView levelText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setGravity(
                Gravity.CENTER_HORIZONTAL
        );

        root.setBackgroundColor(
                Color.rgb(235, 248, 250)
        );

        TextView title =
                new TextView(this);

        title.setText("🎨 سورت رنگ‌ها");
        title.setTextSize(30);
        title.setTextColor(
                Color.rgb(12, 91, 120)
        );

        title.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        title.setGravity(
                Gravity.CENTER
        );

        title.setPadding(
                0,
                25,
                0,
                8
        );

        root.addView(
                title,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        levelText =
                new TextView(this);

        levelText.setTextSize(19);
        levelText.setTextColor(Color.DKGRAY);
        levelText.setGravity(Gravity.CENTER);
        levelText.setPadding(
                0,
                5,
                0,
                15
        );

        root.addView(
                levelText,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        gameView =
                new SortGameView();

        root.addView(
                gameView,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        LinearLayout bottom =
                new LinearLayout(this);

        bottom.setGravity(
                Gravity.CENTER
        );

        bottom.setPadding(
                12,
                8,
                12,
                18
        );

        Button restart =
                new Button(this);

        restart.setText("🔄 شروع دوباره");
        restart.setTextSize(17);
        restart.setAllCaps(false);

        bottom.addView(
                restart,
                new LinearLayout.LayoutParams(
                        0,
                        70,
                        1
                )
        );

        root.addView(bottom);

        setContentView(root);

        restart.setOnClickListener(
                v -> gameView.resetLevel()
        );

        updateLevelText();
    }

    private void updateLevelText() {

        if (levelText != null) {

            levelText.setText(
                    "مرحله " +
                            gameView.level +
                            "  •  رنگ‌ها را مرتب کن"
            );
        }
    }

    class SortGameView extends View {

        private final Paint paint =
                new Paint(Paint.ANTI_ALIAS_FLAG);

        private final List<List<Integer>> tubes =
                new ArrayList<>();

        private int selectedTube = -1;

        private int level = 1;

        private final int MAX_LEVEL = 10;

        private final int EMPTY = -1;

        private final int[] colors = {

                Color.rgb(235, 75, 75),
                Color.rgb(65, 130, 225),
                Color.rgb(75, 180, 95),
                Color.rgb(245, 175, 55),
                Color.rgb(155, 85, 200),
                Color.rgb(30, 175, 175),
                Color.rgb(245, 105, 165),
                Color.rgb(125, 90, 55)
        };

        SortGameView() {

            super(ColorSortActivity.this);

            paint.setStyle(
                    Paint.Style.FILL
            );

            setFocusable(true);

            resetLevel();
        }

        void resetLevel() {

            selectedTube = -1;

            tubes.clear();

            int colorCount =
                    Math.min(
                            2 + level / 2,
                            6
                    );

            for (int i = 0;
                 i < colorCount;
                 i++) {

                List<Integer> tube =
                        new ArrayList<>();

                for (int j = 0;
                     j < 4;
                     j++) {

                    tube.add(i);
                }

                tubes.add(tube);
            }

            /*
             * برای اینکه بازی واقعاً نیاز به
             * مرتب کردن داشته باشد، رنگ‌ها
             * را به صورت چرخشی پخش می‌کنیم.
             */
            List<Integer> mixed =
                    new ArrayList<>();

            for (List<Integer> tube : tubes) {

                mixed.addAll(tube);
            }

            for (int i = 0;
                 i < mixed.size();
                 i++) {

                int swap =
                        (i * 3 + level) %
                                mixed.size();

                int temp =
                        mixed.get(i);

                mixed.set(
                        i,
                        mixed.get(swap)
                );

                mixed.set(
                        swap,
                        temp
                );
            }

            int index = 0;

            for (List<Integer> tube : tubes) {

                tube.clear();

                for (int j = 0;
                     j < 4;
                     j++) {

                    tube.add(
                            mixed.get(index++)
                    );
                }
            }

            /*
             * دو شیشه خالی
             */
            tubes.add(
                    new ArrayList<>()
            );

            tubes.add(
                    new ArrayList<>()
            );

            updateLevelText();

            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {

            super.onDraw(canvas);

            canvas.drawColor(
                    Color.rgb(235, 248, 250)
            );

            int tubeCount =
                    tubes.size();

            float width =
                    getWidth();

            float height =
                    getHeight();

            int columns;

            if (tubeCount <= 4) {

                columns = 4;

            } else {

                columns = 4;
            }

            int rows =
                    (int) Math.ceil(
                            tubeCount /
                                    (float) columns
                    );

            float horizontalSpace =
                    width /
                            (columns + 1);

            float tubeWidth =
                    Math.min(
                            72,
                            horizontalSpace * 0.62f
                    );

            float tubeHeight =
                    Math.min(
                            250,
                            height /
                                    (rows + 0.9f)
                    );

            float gap =
                    12;

            for (int i = 0;
                 i < tubeCount;
                 i++) {

                int row =
                        i / columns;

                int column =
                        i % columns;

                float x =
                        horizontalSpace *
                                (column + 1);

                float centerY =
                        tubeHeight *
                                (row + 0.62f);

                float left =
                        x -
                                tubeWidth / 2;

                float top =
                        centerY -
                                tubeHeight / 2;

                float right =
                        x +
                                tubeWidth / 2;

                float bottom =
                        centerY +
                                tubeHeight / 2;

                /*
                 * شیشه
                 */
                paint.setStyle(
                        Paint.Style.STROKE
                );

                paint.setStrokeWidth(5);

                if (i == selectedTube) {

                    paint.setColor(
                            Color.rgb(
                                    12,
                                    91,
                                    120
                            )
                    );

                } else {

                    paint.setColor(
                            Color.rgb(
                                    145,
                                    175,
                                    185
                            )
                    );
                }

                RectF glass =
                        new RectF(
                                left,
                                top,
                                right,
                                bottom
                        );

                canvas.drawRoundRect(
                        glass,
                        18,
                        18,
                        paint
                );

                /*
                 * رنگ‌های داخل شیشه
                 */
                paint.setStyle(
                        Paint.Style.FILL
                );

                List<Integer> tube =
                        tubes.get(i);

                int count =
                        tube.size();

                float liquidHeight =
                        (bottom - top - 12) /
                                4f;

                for (int j = 0;
                     j < count;
                     j++) {

                    int colorIndex =
                            tube.get(j);

                    paint.setColor(
                            colors[colorIndex]
                    );

                    float liquidTop =
                            bottom -
                                    6 -
                                    (j + 1) *
                                            liquidHeight;

                    float liquidBottom =
                            bottom -
                                    6 -
                                    j *
                                            liquidHeight;

                    RectF liquid =
                            new RectF(
                                    left + 5,
                                    liquidTop,
                                    right - 5,
                                    liquidBottom
                            );

                    canvas.drawRoundRect(
                            liquid,
                            8,
                            8,
                            paint
                    );
                }

                /*
                 * شماره شیشه
                 */
                paint.setColor(
                        Color.DKGRAY
                );

                paint.setTextSize(17);

                paint.setTextAlign(
                        Paint.Align.CENTER
                );

                canvas.drawText(
                        String.valueOf(i + 1),
                        x,
                        bottom + 27,
                        paint
                );
            }

            /*
             * راهنمای بازی
             */
            paint.setColor(
                    Color.rgb(
                            12,
                            91,
                            120
                    )
            );

            paint.setTextSize(17);

            paint.setTextAlign(
                    Paint.Align.CENTER
            );

            canvas.drawText(
                    "یک شیشه را لمس کن، سپس شیشه مقصد را لمس کن",
                    width / 2,
                    height - 18,
                    paint
            );
        }

        @Override
        public boolean onTouchEvent(
                MotionEvent event
        ) {

            if (event.getAction() !=
                    MotionEvent.ACTION_DOWN) {

                return true;
            }

            float touchX =
                    event.getX();

            float touchY =
                    event.getY();

            int tube =
                    findTube(
                            touchX,
                            touchY
                    );

            if (tube == -1) {

                return true;
            }

            handleTubeClick(tube);

            return true;
        }

        private int findTube(
                float x,
                float y
        ) {

            int tubeCount =
                    tubes.size();

            float width =
                    getWidth();

            float height =
                    getHeight();

            int columns = 4;

            int rows =
                    (int) Math.ceil(
                            tubeCount /
                                    (float) columns
                    );

            float horizontalSpace =
                    width /
                            (columns + 1);

            float tubeWidth =
                    Math.min(
                            72,
                            horizontalSpace * 0.62f
                    );

            float tubeHeight =
                    Math.min(
                            250,
                            height /
                                    (rows + 0.9f)
                    );

            for (int i = 0;
                 i < tubeCount;
                 i++) {

                int row =
                        i / columns;

                int column =
                        i % columns;

                float centerX =
                        horizontalSpace *
                                (column + 1);

                float centerY =
                        tubeHeight *
                                (row + 0.62f);

                float left =
                        centerX -
                                tubeWidth / 2 -
                                15;

                float right =
                        centerX +
                                tubeWidth / 2 +
                                15;

                float top =
                        centerY -
                                tubeHeight / 2 -
                                15;

                float bottom =
                        centerY +
                                tubeHeight / 2 +
                                15;

                if (x >= left &&
                        x <= right &&
                        y >= top &&
                        y <= bottom) {

                    return i;
                }
            }

            return -1;
        }

        private void handleTubeClick(
                int tubeIndex
        ) {

            if (selectedTube == -1) {

                if (tubes.get(tubeIndex).isEmpty()) {

                    return;
                }

                selectedTube =
                        tubeIndex;

                invalidate();

                return;
            }

            if (selectedTube ==
                    tubeIndex) {

                selectedTube = -1;

                invalidate();

                return;
            }

            if (canMove(
                    selectedTube,
                    tubeIndex
            )) {

                moveColor(
                        selectedTube,
                        tubeIndex
                );

                selectedTube = -1;

                invalidate();

                if (isLevelComplete()) {

                    completeLevel();
                }

            } else {

                Toast.makeText(
                        ColorSortActivity.this,
                        "❌ این حرکت امکان‌پذیر نیست",
                        Toast.LENGTH_SHORT
                ).show();

                selectedTube = -1;

                invalidate();
            }
        }

        private boolean canMove(
                int from,
                int to
        ) {

            List<Integer> source =
                    tubes.get(from);

            List<Integer> target =
                    tubes.get(to);

            if (source.isEmpty()) {

                return false;
            }

            if (target.size() >= 4) {

                return false;
            }

            if (target.isEmpty()) {

                return true;
            }

            int sourceColor =
                    source.get(
                            source.size() - 1
                    );

            int targetColor =
                    target.get(
                            target.size() - 1
                    );

            return sourceColor ==
                    targetColor;
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

            /*
             * همه رنگ‌های یکسان پشت سر هم
             * را تا ظرفیت مقصد منتقل می‌کنیم.
             */
            while (!source.isEmpty()
                    && target.size() < 4
                    && source.get(
                    source.size() - 1
            ) == color) {

                target.add(
                        source.remove(
                                source.size() - 1
                        )
                );
            }
        }

        private boolean isLevelComplete() {

            for (List<Integer> tube : tubes) {

                if (tube.isEmpty()) {

                    continue;
                }

                if (tube.size() != 4) {

                    return false;
                }

                int first =
                        tube.get(0);

                for (int i = 1;
                     i < tube.size();
                     i++) {

                    if (tube.get(i) != first) {

                        return false;
                    }
                }
            }

            return true;
        }

        private void completeLevel() {

            if (level < MAX_LEVEL) {

                Toast.makeText(
                        ColorSortActivity.this,
                        "🎉 آفرین! مرحله " +
                                level +
                                " تمام شد",
                        Toast.LENGTH_SHORT
                ).show();

                level++;

                resetLevel();

            } else {

                Toast.makeText(
                        ColorSortActivity.this,
                        "🏆 عالی! همه مراحل تمام شد",
                        Toast.LENGTH_LONG
                ).show();

                level = 1;

                resetLevel();
            }
        }
    }
}
