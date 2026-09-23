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
import java.util.Random;

public class ColorSortActivity extends Activity {

    private SortGameView gameView;
    private TextView levelText;
    private TextView moveText;
    private TextView scoreText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(10, 8, 10, 10);
        root.setBackgroundColor(Color.rgb(8, 24, 34));

        TextView title = new TextView(this);
        title.setText("🎨  سورت رنگ‌ها");
        title.setTextSize(27);
        title.setTextColor(Color.WHITE);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 4, 0, 4);

        root.addView(
                title,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        LinearLayout stats = new LinearLayout(this);
        stats.setGravity(Gravity.CENTER);
        stats.setOrientation(LinearLayout.HORIZONTAL);

        levelText = makeStatText();
        moveText = makeStatText();
        scoreText = makeStatText();

        stats.addView(
                levelText,
                new LinearLayout.LayoutParams(
                        0,
                        55,
                        1
                )
        );

        stats.addView(
                moveText,
                new LinearLayout.LayoutParams(
                        0,
                        55,
                        1
                )
        );

        stats.addView(
                scoreText,
                new LinearLayout.LayoutParams(
                        0,
                        55,
                        1
                )
        );

        root.addView(stats);

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
        bottom.setPadding(4, 4, 4, 2);

        Button undo = new Button(this);
        undo.setText("↩ برگشت");
        undo.setTextSize(14);
        undo.setAllCaps(false);

        Button restart = new Button(this);
        restart.setText("🔄 دوباره");
        restart.setTextSize(14);
        restart.setAllCaps(false);

        Button hint = new Button(this);
        hint.setText("💡 راهنما");
        hint.setTextSize(14);
        hint.setAllCaps(false);

        bottom.addView(
                undo,
                new LinearLayout.LayoutParams(
                        0,
                        58,
                        1
                )
        );

        bottom.addView(
                restart,
                new LinearLayout.LayoutParams(
                        0,
                        58,
                        1
                )
        );

        bottom.addView(
                hint,
                new LinearLayout.LayoutParams(
                        0,
                        58,
                        1
                )
        );

        root.addView(bottom);

        setContentView(root);

        undo.setOnClickListener(
                v -> gameView.undoMove()
        );

        restart.setOnClickListener(
                v -> gameView.resetLevel()
        );

        hint.setOnClickListener(
                v -> gameView.showHint()
        );

        updateTexts();
    }

    private TextView makeStatText() {

        TextView text = new TextView(this);

        text.setTextSize(14);
        text.setTextColor(Color.rgb(205, 230, 236));
        text.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );
        text.setGravity(Gravity.CENTER);

        return text;
    }

    private void updateTexts() {

        if (gameView == null) {
            return;
        }

        if (levelText != null) {
            levelText.setText(
                    "مرحله\n" +
                            gameView.level +
                            " / " +
                            gameView.MAX_LEVEL
            );
        }

        if (moveText != null) {
            moveText.setText(
                    "حرکت\n" +
                            gameView.moves
            );
        }

        if (scoreText != null) {
            scoreText.setText(
                    "امتیاز\n" +
                            gameView.score
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

        private final Paint highlightPaint =
                new Paint(Paint.ANTI_ALIAS_FLAG);

        private final Paint textPaint =
                new Paint(Paint.ANTI_ALIAS_FLAG);

        private final Handler handler =
                new Handler();

        private final Random random =
                new Random();

        private final List<List<Integer>> tubes =
                new ArrayList<>();

        private final List<Move> history =
                new ArrayList<>();

        private int selectedTube = -1;

        private int level = 1;

        private int moves = 0;

        private int score = 0;

        private boolean levelFinished = false;

        private boolean showingHint = false;

        private int hintFrom = -1;

        private int hintTo = -1;

        private long hintUntil = 0;

        private final int MAX_LEVEL = 30;

        private final int CAPACITY = 4;

        /*
         * رنگ‌های عمیق و پررنگ
         */
        private final int[] colors = {

                Color.rgb(218, 38, 58),    // ruby
                Color.rgb(35, 93, 218),    // sapphire
                Color.rgb(24, 166, 91),    // emerald
                Color.rgb(231, 139, 18),   // amber
                Color.rgb(126, 54, 190),   // purple
                Color.rgb(10, 157, 165),   // turquoise
                Color.rgb(220, 45, 126),   // magenta
                Color.rgb(128, 75, 39),    // brown
                Color.rgb(54, 112, 126),   // blue gray
                Color.rgb(193, 56, 155)    // pink purple
        };

        SortGameView() {

            super(ColorSortActivity.this);

            setLayerType(
                    View.LAYER_TYPE_SOFTWARE,
                    null
            );

            paint.setAntiAlias(true);
            glassPaint.setAntiAlias(true);
            shadowPaint.setAntiAlias(true);
            highlightPaint.setAntiAlias(true);
            textPaint.setAntiAlias(true);

            setFocusable(true);

            resetLevel();
        }

        /*
         * ساخت مرحله
         */
        void resetLevel() {

            selectedTube = -1;
            moves = 0;
            levelFinished = false;
            showingHint = false;
            hintFrom = -1;
            hintTo = -1;

            tubes.clear();
            history.clear();

            int colorCount =
                    getColorCount();

            /*
             * هر رنگ یک شیشه کامل
             */
            for (int c = 0;
                 c < colorCount;
                 c++) {

                List<Integer> tube =
                        new ArrayList<>();

                for (int i = 0;
                     i < CAPACITY;
                     i++) {

                    tube.add(c);
                }

                tubes.add(tube);
            }

            /*
             * شیشه‌های خالی
             *
             * در مراحل بالاتر یک شیشه
             * اضافه می‌شود.
             */
            int emptyCount =
                    level < 10 ? 2 :
                    level < 20 ? 2 : 3;

            for (int i = 0;
                 i < emptyCount;
                 i++) {

                tubes.add(
                        new ArrayList<>()
                );
            }

            /*
             * پازل را با حرکات معکوس
             * به‌هم می‌ریزیم.
             *
             * این روش باعث می‌شود
             * مرحله قابل حل باقی بماند.
             */
            shufflePuzzle(
                    getShuffleCount()
            );

            updateTexts();
            invalidate();
        }

        private int getColorCount() {

            if (level <= 3) {
                return 3;
            }

            if (level <= 6) {
                return 4;
            }

            if (level <= 10) {
                return 5;
            }

            if (level <= 15) {
                return 6;
            }

            if (level <= 20) {
                return 7;
            }

            if (level <= 25) {
                return 8;
            }

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

        /*
         * پازل را با حرکت‌های قانونی
         * به‌هم می‌ریزیم.
         */
        private void shufflePuzzle(
                int count
        ) {

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
                        possibleFrom,
                        random
                );

                boolean moved = false;

                for (int from :
                        possibleFrom) {

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

                    int sameCount =
                            getTopSameCount(
                                    source
                            );

                    List<Integer> targets =
                            new ArrayList<>();

                    for (int to = 0;
                         to < tubes.size();
                         to++) {

                        if (to == from) {
                            continue;
                        }

                        if (to == previousFrom) {
                            continue;
                        }

                        List<Integer> target =
                                tubes.get(to);

                        if (target.size() >= CAPACITY) {
                            continue;
                        }

                        /*
                         * در ساخت اولیه،
                         * ترجیح می‌دهیم رنگ‌ها
                         * روی رنگ متفاوت قرار بگیرند.
                         */
                        if (target.isEmpty()) {

                            targets.add(to);

                        } else {

                            int targetColor =
                                    target.get(
                                            target.size() - 1
                                    );

                            if (targetColor !=
                                    sourceColor) {

                                targets.add(to);
                            }
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

                    /*
                     * معمولاً یک یا دو رنگ
                     * با هم جابه‌جا می‌شوند.
                     */
                    int amount =
                            Math.min(
                                    sameCount,
                                    Math.min(
                                            free,
                                            1 +
                                                    random.nextInt(
                                                            2
                                                    )
                                    )
                            );

                    if (amount <= 0) {
                        continue;
                    }

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

            /*
             * اگر اتفاقاً مرحله خیلی مرتب
             * باقی ماند، دوباره تلاش می‌کنیم.
             */
            if (isLevelComplete()) {

                if (count < 120) {
                    shufflePuzzle(count + 15);
                }
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

            for (int i =
                         tube.size() - 1;
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

        @Override
        protected void onDraw(
                Canvas canvas
        ) {

            super.onDraw(canvas);

            float width = getWidth();
            float height = getHeight();

            drawBackground(
                    canvas,
                    width,
                    height
            );

            drawTopDecoration(
                    canvas,
                    width
            );

            int tubeCount =
                    tubes.size();

            int columns;

            if (tubeCount <= 6) {
                columns = 3;
            } else if (tubeCount <= 8) {
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
                            (columns + 1f);

            float availableHeight =
                    height - 62;

            float rowSpace =
                    availableHeight /
                            (rows + 0.15f);

            float tubeWidth =
                    Math.min(
                            82f,
                            horizontalSpace * 0.72f
                    );

            float tubeHeight =
                    Math.min(
                            226f,
                            rowSpace * 0.72f
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
                                (row + 0.55f);

                drawTube(
                        canvas,
                        i,
                        centerX,
                        centerY,
                        tubeWidth,
                        tubeHeight
                );
            }

            /*
             * راهنمای پایین صفحه
             */
            textPaint.setTypeface(
                    Typeface.DEFAULT
            );

            textPaint.setTextAlign(
                    Paint.Align.CENTER
            );

            textPaint.setTextSize(13);

            textPaint.setColor(
                    Color.rgb(
                            164,
                            196,
                            205
                    )
            );

            canvas.drawText(
                    "رنگ‌های یکسان را در یک شیشه جمع کن",
                    width / 2,
                    height - 10,
                    textPaint
            );

            if (showingHint &&
                    System.currentTimeMillis()
                            < hintUntil) {

                drawHint(
                        canvas,
                        width,
                        height
                );

                postInvalidateDelayed(
                        100
                );

            } else {

                showingHint = false;
            }

            if (levelFinished) {

                drawVictory(
                        canvas,
                        width,
                        height
                );
            }
        }

        private void drawBackground(
                Canvas canvas,
                float width,
                float height
        ) {

            LinearGradient gradient =
                    new LinearGradient(
                            0,
                            0,
                            0,
                            height,
                            Color.rgb(
                                    7,
                                    22,
                                    31
                            ),
                            Color.rgb(
                                    15,
                                    46,
                                    58
                            ),
                            Shader.TileMode.CLAMP
                    );

            paint.setShader(gradient);

            canvas.drawRect(
                    0,
                    0,
                    width,
                    height,
                    paint
            );

            paint.setShader(null);

            RadialGradient glow =
                    new RadialGradient(
                            width * 0.5f,
                            height * 0.35f,
                            width * 0.75f,
                            Color.argb(
                                    45,
                                    20,
                                    151,
                                    178
                            ),
                            Color.argb(
                                    0,
                                    20,
                                    151,
                                    178
                            ),
                            Shader.TileMode.CLAMP
                    );

            paint.setShader(glow);

            canvas.drawCircle(
                    width * 0.5f,
                    height * 0.35f,
                    width * 0.75f,
                    paint
            );

            paint.setShader(null);

            /*
             * نقاط نور پس‌زمینه
             */
            paint.setStyle(
                    Paint.Style.FILL
            );

            paint.setColor(
                    Color.argb(
                            28,
                            255,
                            255,
                            255
                    )
            );

            canvas.drawCircle(
                    width * 0.08f,
                    height * 0.18f,
                    3,
                    paint
            );

            canvas.drawCircle(
                    width * 0.91f,
                    height * 0.22f,
                    4,
                    paint
            );

            canvas.drawCircle(
                    width * 0.18f,
                    height * 0.82f,
                    3,
                    paint
            );

            canvas.drawCircle(
                    width * 0.82f,
                    height * 0.76f,
                    3,
                    paint
            );
        }

        private void drawTopDecoration(
                Canvas canvas,
                float width
        ) {

            paint.setStyle(
                    Paint.Style.FILL
            );

            LinearGradient line =
                    new LinearGradient(
                            0,
                            0,
                            width,
                            0,
                            Color.TRANSPARENT,
                            Color.argb(
                                    150,
                                    65,
                                    211,
                                    224
                            ),
                            Shader.TileMode.MIRROR
                    );

            paint.setShader(line);

            canvas.drawRect(
                    0,
                    1,
                    width,
                    3,
                    paint
            );

            paint.setShader(null);
        }

        private void drawTube(
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

            boolean selected =
                    index == selectedTube;

            boolean hinted =
                    showingHint &&
                            (index == hintFrom ||
                                    index == hintTo);

            /*
             * سایه
             */
            shadowPaint.setStyle(
                    Paint.Style.FILL
            );

            shadowPaint.setColor(
                    Color.argb(
                            selected ? 90 : 65,
                            0,
                            0,
                            0
                    )
            );

            shadowPaint.setShadowLayer(
                    selected ? 18 : 12,
                    0,
                    8,
                    Color.argb(
                            120,
                            0,
                            0,
                            0
                    )
            );

            RectF shadow =
                    new RectF(
                            left - 2,
                            top + 5,
                            right + 2,
                            bottom + 6
                    );

            canvas.drawRoundRect(
                    shadow,
                    24,
                    24,
                    shadowPaint
            );

            shadowPaint.clearShadowLayer();

            /*
             * هاله انتخاب
             */
            if (selected || hinted) {

                RadialGradient glow =
                        new RadialGradient(
                                centerX,
                                centerY,
                                tubeWidth * 1.05f,
                                Color.argb(
                                        selected ? 105 : 70,
                                        39,
                                        204,
                                        218
                                ),
                                Color.argb(
                                        0,
                                        39,
                                        204,
                                        218
                                ),
                                Shader.TileMode.CLAMP
                        );

                highlightPaint.setShader(glow);

                canvas.drawCircle(
                        centerX,
                        centerY,
                        tubeWidth * 1.05f,
                        highlightPaint
                );

                highlightPaint.setShader(null);
            }

            /*
             * بدنه شیشه
             */
            LinearGradient glassGradient =
                    new LinearGradient(
                            left,
                            top,
                            right,
                            bottom,
                            Color.argb(
                                    55,
                                    255,
                                    255,
                                    255
                            ),
                            Color.argb(
                                    15,
                                    255,
                                    255,
                                    255
                            ),
                            Shader.TileMode.CLAMP
                    );

            glassPaint.setShader(
                    glassGradient
            );

            glassPaint.setStyle(
                    Paint.Style.FILL
            );

            RectF glass =
                    new RectF(
                            left,
                            top,
                            right,
                            bottom
                    );

            canvas.drawRoundRect(
                    glass,
                    24,
                    24,
                    glassPaint
            );

            glassPaint.setShader(null);

            /*
             * رنگ‌ها داخل شیشه
             */
            List<Integer> tube =
                    tubes.get(index);

            float innerLeft =
                    left + 7;

            float innerRight =
                    right - 7;

            float innerBottom =
                    bottom - 7;

            float layerHeight =
                    (tubeHeight - 14) /
                            CAPACITY;

            for (int j = 0;
                 j < tube.size();
                 j++) {

                int colorIndex =
                        tube.get(j);

                float liquidTop =
                        innerBottom -
                                (j + 1) *
                                        layerHeight;

                float liquidBottom =
                        innerBottom -
                                j *
                                        layerHeight;

                drawLiquid(
                        canvas,
                        innerLeft,
                        liquidTop,
                        innerRight,
                        liquidBottom,
                        colorIndex,
                        j ==
                                tube.size() - 1
                );
            }

            /*
             * حاشیه شیشه
             */
            paint.setStyle(
                    Paint.Style.STROKE
            );

            paint.setStrokeWidth(
                    selected ? 4.5f : 2.8f
            );

            paint.setColor(
                    selected
                            ? Color.rgb(
                            62,
                            211,
                            225
                    )
                            : Color.argb(
                            130,
                            190,
                            229,
                            235
                    )
            );

            canvas.drawRoundRect(
                    glass,
                    24,
                    24,
                    paint
            );

            /*
             * دهانه شیشه
             */
            RectF rim =
                    new RectF(
                            left + 5,
                            top + 5,
                            right - 5,
                            top + 22
                    );

            paint.setStrokeWidth(2);

            paint.setColor(
                    Color.argb(
                            180,
                            225,
                            249,
                            252
                    )
            );

            canvas.drawRoundRect(
                    rim,
                    9,
                    9,
                    paint
            );

            /*
             * انعکاس اصلی شیشه
             */
            paint.setStrokeCap(
                    Paint.Cap.ROUND
            );

            paint.setStrokeWidth(4);

            paint.setColor(
                    Color.argb(
                            105,
                            255,
                            255,
                            255
                    )
            );

            canvas.drawLine(
                    left + 12,
                    top + 29,
                    left + 12,
                    bottom - 31,
                    paint
            );

            paint.setStrokeCap(
                    Paint.Cap.BUTT
            );

            /*
             * شماره شیشه
             */
            textPaint.setTypeface(
                    Typeface.DEFAULT_BOLD
            );

            textPaint.setTextAlign(
                    Paint.Align.CENTER
            );

            textPaint.setTextSize(12);

            textPaint.setColor(
                    Color.rgb(
                            165,
                            198,
                            206
                    )
            );

            canvas.drawText(
                    String.valueOf(index + 1),
                    centerX,
                    bottom + 20,
                    textPaint
            );

            /*
             * علامت انتخاب
             */
            if (selected) {

                textPaint.setTextSize(17);

                textPaint.setColor(
                        Color.rgb(
                                64,
                                220,
                                232
                        )
                );

                canvas.drawText(
                        "▲",
                        centerX,
                        top - 8,
                        textPaint
                );
            }
        }

        private void drawLiquid(
                Canvas canvas,
                float left,
                float top,
                float right,
                float bottom,
                int colorIndex,
                boolean topLayer
        ) {

            int base =
                    colors[colorIndex];

            int light =
                    lighten(
                            base,
                            1.25f
                    );

            int dark =
                    darken(
                            base,
                            0.55f
                    );

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

            canvas.drawRect(
                    left,
                    top,
                    right,
                    bottom,
                    paint
            );

            paint.setShader(null);

            /*
             * درخشش سمت چپ
             */
            LinearGradient shine =
                    new LinearGradient(
                            left,
                            0,
                            right,
                            0,
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

            paint.setShader(shine);

            canvas.drawRect(
                    left + 2,
                    top,
                    right - 2,
                    bottom,
                    paint
            );

            paint.setShader(null);

            if (topLayer) {

                /*
                 * سطح براق مایع
                 */
                paint.setColor(
                        Color.argb(
                                105,
                                255,
                                255,
                                255
                        )
                );

                canvas.drawRoundRect(
                        new RectF(
                                left + 3,
                                top + 2,
                                right - 3,
                                top + 9
                        ),
                        6,
                        6,
                        paint
                );

                /*
                 * حباب‌های ریز
                 */
                paint.setColor(
                        Color.argb(
                                95,
                                255,
                                255,
                                255
                        )
                );

                canvas.drawCircle(
                        left + 14,
                        top + 19,
                        2.5f,
                        paint
                );

                canvas.drawCircle(
                        right - 15,
                        top + 27,
                        2,
                        paint
                );

                canvas.drawCircle(
                        (left + right) / 2f,
                        top + 14,
                        1.8f,
                        paint
                );
            }
        }

        private int lighten(
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

        private int darken(
                int color,
                float factor
        ) {

            return Color.rgb(
                    Math.max(
                            0,
                            (int)
                                    (Color.red(color) *
                                            factor)
                    ),
                    Math.max(
                            0,
                            (int)
                                    (Color.green(color) *
                                            factor)
                    ),
                    Math.max(
                            0,
                            (int)
                                    (Color.blue(color) *
                                            factor)
                    )
            );
        }

        private void drawHint(
                Canvas canvas,
                float width,
                float height
        ) {

            float alpha =
                    0.5f +
                            0.5f *
                                    (float)
                                            Math.sin(
                                                    System.currentTimeMillis()
                                                            / 150.0
                                            );

            paint.setStyle(
                    Paint.Style.STROKE
            );

            paint.setStrokeWidth(
                    5
            );

            paint.setColor(
                    Color.argb(
                            (int)
                                    (150 * alpha),
                            255,
                            213,
                            64
                    )
            );

            if (hintFrom >= 0 &&
                    hintTo >= 0) {

                float[] from =
                        getTubeCenter(hintFrom);

                float[] to =
                        getTubeCenter(hintTo);

                if (from != null &&
                        to != null) {

                    canvas.drawLine(
                            from[0],
                            from[1],
                            to[0],
                            to[1],
                            paint
                    );

                    paint.setStyle(
                            Paint.Style.FILL
                    );

                    canvas.drawCircle(
                            to[0],
                            to[1],
                            8,
                            paint
                    );
                }
            }

            paint.setStyle(
                    Paint.Style.FILL
            );
        }

        private float[] getTubeCenter(
                int index
        ) {

            int tubeCount =
                    tubes.size();

            int columns =
                    tubeCount <= 6 ? 3 : 4;

            int rows =
                    (int) Math.ceil(
                            tubeCount /
                                    (float) columns
                    );

            float width =
                    getWidth();

            float height =
                    getHeight();

            float horizontalSpace =
                    width /
                            (columns + 1f);

            float availableHeight =
                    height - 62;

            float rowSpace =
                    availableHeight /
                            (rows + 0.15f);

            int row =
                    index / columns;

            int column =
                    index % columns;

            float x =
                    horizontalSpace *
                            (column + 1);

            float y =
                    rowSpace *
                            (row + 0.55f);

            return new float[]{
                    x,
                    y
            };
        }

        private void drawVictory(
                Canvas canvas,
                float width,
                float height
        ) {

            /*
             * پس‌زمینه تاریک روی بازی
             */
            paint.setStyle(
                    Paint.Style.FILL
            );

            paint.setColor(
                    Color.argb(
                            190,
                            3,
                            12,
                            18
                    )
            );

            canvas.drawRect(
                    0,
                    0,
                    width,
                    height,
                    paint
            );

            /*
             * هاله
             */
            RadialGradient glow =
                    new RadialGradient(
                            width / 2f,
                            height / 2f,
                            210,
                            Color.argb(
                                    100,
                                    30,
                                    204,
                                    184
                            ),
                            Color.argb(
                                    0,
                                    30,
                                    204,
                                    184
                            ),
                            Shader.TileMode.CLAMP
                    );

            paint.setShader(glow);

            canvas.drawCircle(
                    width / 2f,
                    height / 2f,
                    210,
                    paint
            );

            paint.setShader(null);

            textPaint.setTextAlign(
                    Paint.Align.CENTER
            );

            textPaint.setTypeface(
                    Typeface.DEFAULT_BOLD
            );

            textPaint.setTextSize(34);

            textPaint.setColor(
                    Color.WHITE
            );

            canvas.drawText(
                    "🎉 عالی!",
                    width / 2f,
                    height / 2f - 55,
                    textPaint
            );

            textPaint.setTextSize(19);

            textPaint.setColor(
                    Color.rgb(
                            190,
                            232,
                            236
                    )
            );

            canvas.drawText(
                    "مرحله " +
                            level +
                            " کامل شد",
                    width / 2f,
                    height / 2f - 15,
                    textPaint
            );

            int stars =
                    calculateStars();

            String starText = "";

            for (int i = 0;
                 i < 3;
                 i++) {

                starText +=
                        i < stars
                                ? "★"
                                : "☆";
            }

            textPaint.setTextSize(34);

            textPaint.setColor(
                    Color.rgb(
                            255,
                            205,
                            55
                    )
            );

            canvas.drawText(
                    starText,
                    width / 2f,
                    height / 2f + 38,
                    textPaint
            );

            textPaint.setTextSize(15);

            textPaint.setColor(
                    Color.rgb(
                            180,
                            207,
                            214
                    )
            );

            canvas.drawText(
                    "حرکت‌ها: " +
                            moves +
                            "   •   امتیاز: " +
                            calculateLevelScore(),
                    width / 2f,
                    height / 2f + 72,
                    textPaint
            );
        }

        private int calculateStars() {

            int colorsCount =
                    getColorCount();

            int perfect =
                    colorsCount * 3;

            if (moves <= perfect) {
                return 3;
            }

            if (moves <= perfect + 7) {
                return 2;
            }

            return 1;
        }

        private int calculateLevelScore() {

            int base =
                    100 +
                            level * 25;

            int penalty =
                    moves * 3;

            return Math.max(
                    25,
                    base - penalty
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

            if (tube < 0) {
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

            int columns =
                    tubeCount <= 6 ? 3 : 4;

            int rows =
                    (int) Math.ceil(
                            tubeCount /
                                    (float) columns
                    );

            float width =
                    getWidth();

            float height =
                    getHeight();

            float horizontalSpace =
                    width /
                            (columns + 1f);

            float availableHeight =
                    height - 62;

            float rowSpace =
                    availableHeight /
                            (rows + 0.15f);

            float tubeWidth =
                    Math.min(
                            82f,
                            horizontalSpace * 0.72f
                    );

            float tubeHeight =
                    Math.min(
                            226f,
                            rowSpace * 0.72f
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
                                (row + 0.55f);

                float left =
                        centerX -
                                tubeWidth / 2f -
                                20;

                float right =
                        centerX +
                                tubeWidth / 2f +
                                20;

                float top =
                        centerY -
                                tubeHeight / 2f -
                                20;

                float bottom =
                        centerY +
                                tubeHeight / 2f +
                                30;

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

                saveMove(
                        selectedTube,
                        tubeIndex
                );

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
                        "این حرکت ممکن نیست",
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

            if (from < 0 ||
                    to < 0 ||
                    from >= tubes.size() ||
                    to >= tubes.size()) {

                return false;
            }

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

        private void saveMove(
                int from,
                int to
        ) {

            List<Integer> source =
                    tubes.get(from);

            int color =
                    source.get(
                            source.size() - 1
                    );

            int amount =
                    getTopSameCount(
                            source
                    );

            int free =
                    CAPACITY -
                            tubes.get(to).size();

            amount =
                    Math.min(
                            amount,
                            free
                    );

            history.add(
                    new Move(
                            from,
                            to,
                            color,
                            amount
                    )
            );

            /*
             * جلوگیری از رشد بیش از حد تاریخچه
             */
            if (history.size() > 100) {

                history.remove(0);
            }
        }

        void undoMove() {

            if (levelFinished) {
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

            for (int i = 0;
                 i < move.amount;
                 i++) {

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

            selectedTube = -1;

            updateTexts();
            invalidate();
        }

        void showHint() {

            if (levelFinished) {
                return;
            }

            for (int from = 0;
                 from < tubes.size();
                 from++) {

                if (tubes.get(from).isEmpty()) {
                    continue;
                }

                for (int to = 0;
                     to < tubes.size();
                     to++) {

                    if (from == to) {
                        continue;
                    }

                    if (canMove(from, to)) {

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
                    "💡 فعلاً حرکت مناسبی پیدا نشد",
                    Toast.LENGTH_SHORT
            ).show();
        }

        private boolean isLevelComplete() {

            for (List<Integer> tube :
                    tubes) {

                /*
                 * شیشه خالی مجاز است.
                 */
                if (tube.isEmpty()) {
                    continue;
                }

                /*
                 * هر شیشه پر باید
                 * کاملاً یک رنگ باشد.
                 */
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

            int earned =
                    calculateLevelScore();

            score += earned;

            updateTexts();
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

                        if (level <
                                MAX_LEVEL) {

                            level++;

                            resetLevel();

                        } else {

                            Toast.makeText(
                                    ColorSortActivity.this,
                                    "🏆 تمام ۳۰ مرحله را کامل کردی!",
                                    Toast.LENGTH_LONG
                            ).show();

                            level = 1;
                            score = 0;

                            resetLevel();
                        }

                    },
                    2200
            );
        }
    }

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
