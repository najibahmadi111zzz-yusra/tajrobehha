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
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ColorSortActivity extends Activity {

    private SortGameView gameView;
    private TextView levelText;
    private TextView moveText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Color.rgb(232, 247, 250));
        root.setPadding(8, 0, 8, 0);

        TextView title = new TextView(this);
        title.setText("🎨  سورت رنگ‌ها");
        title.setTextSize(29);
        title.setTextColor(Color.rgb(8, 82, 112));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 16, 0, 2);

        root.addView(
                title,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        levelText = new TextView(this);
        levelText.setTextSize(18);
        levelText.setTextColor(Color.rgb(48, 69, 76));
        levelText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        levelText.setGravity(Gravity.CENTER);
        levelText.setPadding(0, 1, 0, 0);

        root.addView(
                levelText,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        moveText = new TextView(this);
        moveText.setTextSize(14);
        moveText.setTextColor(Color.rgb(92, 112, 118));
        moveText.setGravity(Gravity.CENTER);
        moveText.setPadding(0, 1, 0, 6);

        root.addView(
                moveText,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        gameView = new SortGameView();

        root.addView(
                gameView,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        LinearLayout bottom = new LinearLayout(this);
        bottom.setGravity(Gravity.CENTER);
        bottom.setPadding(14, 4, 14, 15);

        Button restart = new Button(this);
        restart.setText("🔄  شروع دوباره");
        restart.setTextSize(16);
        restart.setAllCaps(false);

        bottom.addView(
                restart,
                new LinearLayout.LayoutParams(
                        0,
                        64,
                        1
                )
        );

        root.addView(bottom);

        setContentView(root);

        restart.setOnClickListener(
                v -> gameView.resetLevel()
        );

        updateTexts();
    }

    private void updateTexts() {

        if (levelText != null && gameView != null) {
            levelText.setText(
                    "مرحله " +
                            gameView.level +
                            " از " +
                            gameView.MAX_LEVEL
            );
        }

        if (moveText != null && gameView != null) {
            moveText.setText(
                    "حرکت‌ها: " +
                            gameView.moves +
                            "   •   رنگ‌ها را جدا کن"
            );
        }
    }

    class SortGameView extends View {

        private final Paint paint =
                new Paint(Paint.ANTI_ALIAS_FLAG);

        private final Paint glassPaint =
                new Paint(Paint.ANTI_ALIAS_FLAG);

        private final Paint shadowPaint =
                new Paint(Paint.ANTI_ALIAS_FLAG);

        private final Paint glowPaint =
                new Paint(Paint.ANTI_ALIAS_FLAG);

        private final Paint bubblePaint =
                new Paint(Paint.ANTI_ALIAS_FLAG);

        private final List<List<Integer>> tubes =
                new ArrayList<>();

        private final Handler handler =
                new Handler();

        private int selectedTube = -1;

        private int level = 1;

        private int moves = 0;

        private boolean levelFinished = false;

        private final int MAX_LEVEL = 10;

        private final int CAPACITY = 4;

        private final int[] colors = {
                Color.rgb(244, 65, 82),
                Color.rgb(53, 123, 238),
                Color.rgb(55, 190, 94),
                Color.rgb(250, 172, 39),
                Color.rgb(161, 76, 215),
                Color.rgb(23, 178, 178),
                Color.rgb(246, 83, 158),
                Color.rgb(137, 91, 54)
        };

        SortGameView() {

            super(ColorSortActivity.this);

            paint.setAntiAlias(true);
            glassPaint.setAntiAlias(true);
            shadowPaint.setAntiAlias(true);
            glowPaint.setAntiAlias(true);
            bubblePaint.setAntiAlias(true);

            setFocusable(true);

            resetLevel();
        }

        void resetLevel() {

            selectedTube = -1;
            moves = 0;
            levelFinished = false;

            tubes.clear();

            int colorCount =
                    Math.min(
                            2 + level / 2,
                            6
                    );

            for (int color = 0;
                 color < colorCount;
                 color++) {

                List<Integer> tube =
                        new ArrayList<>();

                for (int i = 0;
                     i < CAPACITY;
                     i++) {

                    tube.add(color);
                }

                tubes.add(tube);
            }

            tubes.add(new ArrayList<>());
            tubes.add(new ArrayList<>());

            shufflePuzzle(
                    18 + level * 7
            );

            updateTexts();
            invalidate();
        }

        private void shufflePuzzle(int count) {

            int previousFrom = -1;
            int previousTo = -1;

            for (int step = 0;
                 step < count;
                 step++) {

                List<Integer> possibleFrom =
                        new ArrayList<>();

                for (int i = 0;
                     i < tubes.size();
                     i++) {

                    if (!tubes.get(i).isEmpty()) {
                        possibleFrom.add(i);
                    }
                }

                Collections.shuffle(
                        possibleFrom
                );

                boolean moved = false;

                for (int from : possibleFrom) {

                    if (tubes.get(from).isEmpty()) {
                        continue;
                    }

                    int color =
                            tubes.get(from).get(
                                    tubes.get(from).size() - 1
                            );

                    List<Integer> possibleTo =
                            new ArrayList<>();

                    for (int to = 0;
                         to < tubes.size();
                         to++) {

                        if (to == from) {
                            continue;
                        }

                        if (to == previousFrom &&
                                from == previousTo) {
                            continue;
                        }

                        List<Integer> target =
                                tubes.get(to);

                        if (target.size() >= CAPACITY) {
                            continue;
                        }

                        if (target.isEmpty()) {

                            possibleTo.add(to);

                        } else {

                            int targetColor =
                                    target.get(
                                            target.size() - 1
                                    );

                            if (targetColor != color) {
                                possibleTo.add(to);
                            }
                        }
                    }

                    if (!possibleTo.isEmpty()) {

                        Collections.shuffle(
                                possibleTo
                        );

                        int to =
                                possibleTo.get(0);

                        moveForShuffle(
                                from,
                                to
                        );

                        previousFrom = from;
                        previousTo = to;

                        moved = true;
                        break;
                    }
                }

                if (!moved) {
                    break;
                }
            }
        }

        private void moveForShuffle(
                int from,
                int to
        ) {

            List<Integer> source =
                    tubes.get(from);

            List<Integer> target =
                    tubes.get(to);

            if (source.isEmpty() ||
                    target.size() >= CAPACITY) {
                return;
            }

            int color =
                    source.get(
                            source.size() - 1
                    );

            int sameCount = 0;

            for (int i = source.size() - 1;
                 i >= 0;
                 i--) {

                if (source.get(i) == color) {
                    sameCount++;
                } else {
                    break;
                }
            }

            int free =
                    CAPACITY - target.size();

            int amount =
                    Math.min(
                            sameCount,
                            Math.min(
                                    free,
                                    1 + (int)
                                            (Math.random() * 2)
                            )
                    );

            for (int i = 0;
                 i < amount;
                 i++) {

                target.add(
                        source.remove(
                                source.size() - 1
                        )
                );
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {

            super.onDraw(canvas);

            float width = getWidth();
            float height = getHeight();

            drawBeautifulBackground(
                    canvas,
                    width,
                    height
            );

            int tubeCount =
                    tubes.size();

            int columns =
                    tubeCount <= 6 ? 3 : 4;

            int rows =
                    (int) Math.ceil(
                            tubeCount /
                                    (float) columns
                    );

            float horizontalSpace =
                    width /
                            (columns + 1f);

            float availableHeight =
                    height - 42;

            float rowSpace =
                    availableHeight /
                            (rows + 0.28f);

            float tubeWidth =
                    Math.min(
                            88f,
                            horizontalSpace * 0.68f
                    );

            float tubeHeight =
                    Math.min(
                            238f,
                            rowSpace * 0.75f
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
                        rowSpace *
                                (row + 0.52f);

                drawProfessionalTube(
                        canvas,
                        i,
                        centerX,
                        centerY,
                        tubeWidth,
                        tubeHeight
                );
            }

            paint.setStyle(
                    Paint.Style.FILL
            );

            paint.setTypeface(
                    Typeface.DEFAULT
            );

            paint.setTextAlign(
                    Paint.Align.CENTER
            );

            paint.setTextSize(14);

            paint.setColor(
                    Color.rgb(69, 93, 101)
            );

            canvas.drawText(
                    "یک شیشه را انتخاب کن، سپس شیشه مقصد را لمس کن",
                    width / 2,
                    height - 11,
                    paint
            );

            if (levelFinished) {
                drawWinOverlay(
                        canvas,
                        width,
                        height
                );
            }
        }

        private void drawBeautifulBackground(
                Canvas canvas,
                float width,
                float height
        ) {

            LinearGradient bg =
                    new LinearGradient(
                            0,
                            0,
                            0,
                            height,
                            Color.rgb(246, 253, 254),
                            Color.rgb(211, 239, 244),
                            Shader.TileMode.CLAMP
                    );

            paint.setShader(bg);

            canvas.drawRect(
                    0,
                    0,
                    width,
                    height,
                    paint
            );

            paint.setShader(null);

            /*
             * نورهای بزرگ و نرم
             */
            RadialGradient glow1 =
                    new RadialGradient(
                            width * 0.18f,
                            height * 0.18f,
                            145,
                            Color.argb(
                                    42,
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
                    );

            paint.setShader(glow1);

            canvas.drawCircle(
                    width * 0.18f,
                    height * 0.18f,
                    145,
                    paint
            );

            paint.setShader(null);

            RadialGradient glow2 =
                    new RadialGradient(
                            width * 0.88f,
                            height * 0.72f,
                            180,
                            Color.argb(
                                    32,
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
                    );

            paint.setShader(glow2);

            canvas.drawCircle(
                    width * 0.88f,
                    height * 0.72f,
                    180,
                    paint
            );

            paint.setShader(null);

            /*
             * حباب‌های پس‌زمینه
             */
            paint.setStyle(
                    Paint.Style.STROKE
            );

            paint.setStrokeWidth(2);

            paint.setColor(
                    Color.argb(
                            45,
                            255,
                            255,
                            255
                    )
            );

            canvas.drawCircle(
                    width * 0.08f,
                    height * 0.52f,
                    18,
                    paint
            );

            canvas.drawCircle(
                    width * 0.91f,
                    height * 0.25f,
                    25,
                    paint
            );

            canvas.drawCircle(
                    width * 0.76f,
                    height * 0.88f,
                    12,
                    paint
            );

            paint.setStyle(
                    Paint.Style.FILL
            );
        }

        private void drawProfessionalTube(
                Canvas canvas,
                int index,
                float centerX,
                float centerY,
                float tubeWidth,
                float tubeHeight
        ) {

            float left =
                    centerX -
                            tubeWidth / 2f;

            float right =
                    centerX +
                            tubeWidth / 2f;

            float top =
                    centerY -
                            tubeHeight / 2f;

            float bottom =
                    centerY +
                            tubeHeight / 2f;

            /*
             * هاله انتخاب
             */
            if (index == selectedTube) {

                RadialGradient selectedGlow =
                        new RadialGradient(
                                centerX,
                                centerY,
                                tubeWidth * 0.95f,
                                Color.argb(
                                        100,
                                        12,
                                        91,
                                        120
                                ),
                                Color.argb(
                                        0,
                                        12,
                                        91,
                                        120
                                ),
                                Shader.TileMode.CLAMP
                        );

                glowPaint.setShader(
                        selectedGlow
                );

                canvas.drawCircle(
                        centerX,
                        centerY,
                        tubeWidth * 0.95f,
                        glowPaint
                );

                glowPaint.setShader(null);
            }

            /*
             * سایه نرم
             */
            shadowPaint.setStyle(
                    Paint.Style.FILL
            );

            shadowPaint.setColor(
                    Color.argb(
                            42,
                            25,
                            70,
                            80
                    )
            );

            RectF shadow =
                    new RectF(
                            left - 2,
                            top + 8,
                            right + 5,
                            bottom + 10
                    );

            canvas.drawRoundRect(
                    shadow,
                    27,
                    27,
                    shadowPaint
            );

            /*
             * داخل شیشه:
             * یک زمینه شفاف بسیار روشن
             */
            RectF inside =
                    new RectF(
                            left + 4,
                            top + 4,
                            right - 4,
                            bottom - 4
                    );

            paint.setStyle(
                    Paint.Style.FILL
            );

            paint.setColor(
                    Color.argb(
                            38,
                            255,
                            255,
                            255
                    )
            );

            canvas.drawRoundRect(
                    inside,
                    25,
                    25,
                    paint
            );

            /*
             * مایع‌ها
             */
            List<Integer> tube =
                    tubes.get(index);

            float innerLeft =
                    left + 8;

            float innerRight =
                    right - 8;

            float liquidBottom =
                    bottom - 8;

            float liquidHeight =
                    (tubeHeight - 16) /
                            CAPACITY;

            for (int j = 0;
                 j < tube.size();
                 j++) {

                int colorIndex =
                        tube.get(j);

                float liquidTop =
                        liquidBottom -
                                (j + 1) *
                                        liquidHeight;

                float liquidBottomHere =
                        liquidBottom -
                                j *
                                        liquidHeight;

                drawLuxuryLiquid(
                        canvas,
                        innerLeft,
                        liquidTop,
                        innerRight,
                        liquidBottomHere,
                        colorIndex,
                        j == tube.size() - 1,
                        centerX
                );
            }

            /*
             * قاب شیشه
             */
            paint.setStyle(
                    Paint.Style.STROKE
            );

            paint.setStrokeWidth(
                    index == selectedTube
                            ? 6
                            : 4
            );

            if (index == selectedTube) {

                paint.setColor(
                        Color.rgb(
                                12,
                                91,
                                120
                        )
                );

            } else {

                paint.setColor(
                        Color.argb(
                                185,
                                111,
                                154,
                                165
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
                    26,
                    26,
                    paint
            );

            /*
             * لبه بالایی شیشه
             */
            paint.setStrokeWidth(3);

            paint.setColor(
                    Color.argb(
                            145,
                            255,
                            255,
                            255
                    )
            );

            RectF rim =
                    new RectF(
                            left + 5,
                            top + 5,
                            right - 5,
                            top + 22
                    );

            canvas.drawRoundRect(
                    rim,
                    12,
                    12,
                    paint
            );

            /*
             * انعکاس نور طولی روی شیشه
             */
            paint.setStrokeWidth(5);

            paint.setStrokeCap(
                    Paint.Cap.ROUND
            );

            paint.setColor(
                    Color.argb(
                            115,
                            255,
                            255,
                            255
                    )
            );

            canvas.drawLine(
                    left + 12,
                    top + 28,
                    left + 12,
                    bottom - 28,
                    paint
            );

            paint.setStrokeCap(
                    Paint.Cap.BUTT
            );

            paint.setStyle(
                    Paint.Style.FILL
            );

            /*
             * شماره شیشه
             */
            paint.setTypeface(
                    Typeface.DEFAULT_BOLD
            );

            paint.setTextAlign(
                    Paint.Align.CENTER
            );

            paint.setTextSize(14);

            paint.setColor(
                    Color.rgb(
                            66,
                            87,
                            94
                    )
            );

            canvas.drawText(
                    String.valueOf(index + 1),
                    centerX,
                    bottom + 23,
                    paint
            );

            /*
             * فلش بالای شیشه انتخاب‌شده
             */
            if (index == selectedTube) {

                paint.setColor(
                        Color.rgb(
                                12,
                                91,
                                120
                        )
                );

                paint.setTextSize(19);

                canvas.drawText(
                        "▼",
                        centerX,
                        top - 9,
                        paint
                );
            }
        }

        private void drawLuxuryLiquid(
                Canvas canvas,
                float left,
                float top,
                float right,
                float bottom,
                int colorIndex,
                boolean topLayer,
                float centerX
        ) {

            int base =
                    colors[colorIndex];

            int light =
                    lightenColor(
                            base,
                            1.22f
                    );

            int dark =
                    darkenColor(
                            base,
                            0.68f
                    );

            /*
             * گرادیان سه‌بعدی مایع
             */
            LinearGradient liquidGradient =
                    new LinearGradient(
                            0,
                            top,
                            0,
                            bottom,
                            light,
                            dark,
                            Shader.TileMode.CLAMP
                    );

            paint.setStyle(
                    Paint.Style.FILL
            );

            paint.setShader(
                    liquidGradient
            );

            RectF liquid =
                    new RectF(
                            left,
                            top,
                            right,
                            bottom
                    );

            canvas.drawRect(
                    liquid,
                    paint
            );

            paint.setShader(null);

            /*
             * درخشش مرکزی
             */
            LinearGradient shineGradient =
                    new LinearGradient(
                            left,
                            0,
                            right,
                            0,
                            Color.argb(
                                    0,
                                    255,
                                    255,
                                    255
                            ),
                            Color.argb(
                                    72,
                                    255,
                                    255,
                                    255
                            ),
                            Shader.TileMode.CLAMP
                    );

            paint.setShader(
                    shineGradient
            );

            canvas.drawRect(
                    left + 5,
                    top,
                    right - 5,
                    bottom,
                    paint
            );

            paint.setShader(null);

            /*
             * سطح براق مایع
             */
            if (topLayer) {

                paint.setColor(
                        Color.argb(
                                105,
                                255,
                                255,
                                255
                        )
                );

                RectF liquidTop =
                        new RectF(
                                left + 3,
                                top + 3,
                                right - 3,
                                top + 11
                        );

                canvas.drawRoundRect(
                        liquidTop,
                        7,
                        7,
                        paint
                );

                /*
                 * چند حباب ظریف
                 */
                drawBubble(
                        canvas,
                        left + 15,
                        top + 20,
                        3,
                        Color.argb(
                                110,
                                255,
                                255,
                                255
                        )
                );

                drawBubble(
                        canvas,
                        right - 17,
                        top + 32,
                        2,
                        Color.argb(
                                90,
                                255,
                                255,
                                255
                        )
                );

                drawBubble(
                        canvas,
                        centerX,
                        top + 14,
                        2,
                        Color.argb(
                                75,
                                255,
                                255,
                                255
                        )
                );
            }
        }

        private void drawBubble(
                Canvas canvas,
                float x,
                float y,
                float radius,
                int color
        ) {

            bubblePaint.setStyle(
                    Paint.Style.FILL
            );

            bubblePaint.setColor(color);

            canvas.drawCircle(
                    x,
                    y,
                    radius,
                    bubblePaint
            );
        }

        private int lightenColor(
                int color,
                float factor
        ) {

            int r =
                    Math.min(
                            255,
                            (int)
                                    (Color.red(color) *
                                            factor)
                    );

            int g =
                    Math.min(
                            255,
                            (int)
                                    (Color.green(color) *
                                            factor)
                    );

            int b =
                    Math.min(
                            255,
                            (int)
                                    (Color.blue(color) *
                                            factor)
                    );

            return Color.rgb(
                    r,
                    g,
                    b
            );
        }

        private int darkenColor(
                int color,
                float factor
        ) {

            int r =
                    Math.max(
                            0,
                            (int)
                                    (Color.red(color) *
                                            factor)
                    );

            int g =
                    Math.max(
                            0,
                            (int)
                                    (Color.green(color) *
                                            factor)
                    );

            int b =
                    Math.max(
                            0,
                            (int)
                                    (Color.blue(color) *
                                            factor)
                    );

            return Color.rgb(
                    r,
                    g,
                    b
            );
        }

        private void drawWinOverlay(
                Canvas canvas,
                float width,
                float height
        ) {

            paint.setStyle(
                    Paint.Style.FILL
            );

            paint.setColor(
                    Color.argb(
                            145,
                            235,
                            248,
                            250
                    )
            );

            canvas.drawRect(
                    0,
                    0,
                    width,
                    height,
                    paint
            );

            RadialGradient winGlow =
                    new RadialGradient(
                            width / 2,
                            height / 2,
                            180,
                            Color.argb(
                                    80,
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
                    );

            paint.setShader(
                    winGlow
            );

            canvas.drawCircle(
                    width / 2,
                    height / 2,
                    180,
                    paint
            );

            paint.setShader(null);

            paint.setTypeface(
                    Typeface.DEFAULT_BOLD
            );

            paint.setTextAlign(
                    Paint.Align.CENTER
            );

            paint.setColor(
                    Color.rgb(
                            8,
                            82,
                            112
                    )
            );

            paint.setTextSize(34);

            canvas.drawText(
                    "🎉 آفرین!",
                    width / 2,
                    height / 2 - 18,
                    paint
            );

            paint.setTextSize(19);

            paint.setColor(
                    Color.rgb(
                            60,
                            78,
                            84
                    )
            );

            canvas.drawText(
                    "مرحله با موفقیت کامل شد",
                    width / 2,
                    height / 2 + 25,
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

            if (levelFinished) {
                return true;
            }

            int tube =
                    findTube(
                            event.getX(),
                            event.getY()
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

            int columns =
                    tubeCount <= 6 ? 3 : 4;

            int rows =
                    (int) Math.ceil(
                            tubeCount /
                                    (float) columns
                    );

            float horizontalSpace =
                    width /
                            (columns + 1f);

            float availableHeight =
                    height - 42;

            float rowSpace =
                    availableHeight /
                            (rows + 0.28f);

            float tubeWidth =
                    Math.min(
                            88f,
                            horizontalSpace * 0.68f
                    );

            float tubeHeight =
                    Math.min(
                            238f,
                            rowSpace * 0.75f
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
                        rowSpace *
                                (row + 0.52f);

                float left =
                        centerX -
                                tubeWidth / 2f -
                                22;

                float right =
                        centerX +
                                tubeWidth / 2f +
                                22;

                float top =
                        centerY -
                                tubeHeight / 2f -
                                22;

                float bottom =
                        centerY +
                                tubeHeight / 2f +
                                32;

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

                    Toast.makeText(
                            ColorSortActivity.this,
                            "این شیشه خالی است",
                            Toast.LENGTH_SHORT
                    ).show();

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

                moves++;

                selectedTube = -1;

                updateTexts();
                invalidate();

                if (isLevelComplete()) {
                    completeLevel();
                }

            } else {

                Toast.makeText(
                        ColorSortActivity.this,
                        "❌ این رنگ را نمی‌توان اینجا ریخت",
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

            if (target.size() >= CAPACITY) {
                return false;
            }

            int sourceColor =
                    source.get(
                            source.size() - 1
                    );

            if (target.isEmpty()) {
                return true;
            }

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

            while (!source.isEmpty()
                    && target.size() < CAPACITY
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

                if (tube.size() != CAPACITY) {
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

            levelFinished = true;

            invalidate();

            Toast.makeText(
                    ColorSortActivity.this,
                    "🎉 مرحله " +
                            level +
                            " کامل شد!",
                    Toast.LENGTH_SHORT
            ).show();

            handler.postDelayed(
                    () -> {

                        if (!levelFinished) {
                            return;
                        }

                        if (level < MAX_LEVEL) {

                            level++;

                            resetLevel();

                        } else {

                            Toast.makeText(
                                    ColorSortActivity.this,
                                    "🏆 همه ۱۰ مرحله تمام شد!",
                                    Toast.LENGTH_LONG
                            ).show();

                            level = 1;

                            resetLevel();
                        }

                    },
                    1800
            );
        }
    }
}
