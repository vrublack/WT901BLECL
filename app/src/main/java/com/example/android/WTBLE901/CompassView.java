package com.example.android.WTBLE901;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

import wtzn.wtbtble901.R;


public class CompassView extends View {
    private Context context;
    private int bigCircle; // 外圈半径
    private int rudeRadius; // 可移动小球的半径
    private int centerColor; // 可移动小球的颜色
    private Bitmap centerBg;
    private Bitmap bitmapBack; // 背景图片
    private Paint mPaint; // 背景画笔
    private Paint mCenterPaint; // 可移动小球画笔
    private Point centerPoint;// 中心位置
    private Point mRockPosition;// 小球当前位置
    private OnColorChangedListener listener; // 小球移动的监听
    private int length; // 小球到中心位置的距离
    private int screenWidth, screenHeight;
    private Bitmap cpBg;//指南针图片
    private Paint cpPaint;//指南针画笔
    private int cpCircle;
    private float currentAngle = 0f; //当前旋转角度

    public CompassView(Context context) {
        super(context);
    }

    public CompassView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        init(attrs);
    }

    public CompassView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init(attrs);
    }

    /**
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @return
     * @describe 计算两点之间的位置
     */
    public static int getLength(float x1, float y1, float x2, float y2) {
        return (int) Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    /**
     * @param a
     * @param b
     * @param cutRadius
     * @return
     * @describe 当触摸点超出圆的范围的时候，设置小球边缘位置
     */
    public static Point getBorderPoint(Point a, Point b, int cutRadius) {
        float radian = getRadian(a, b);
        return new Point(a.x + (int) (cutRadius * Math.cos(radian)), a.x
                + (int) (cutRadius * Math.sin(radian)));
    }

    /**
     * @param a
     * @param b
     * @return
     * @describe 触摸点与中心点之间直线与水平方向的夹角角度
     */
    public static float getRadian(Point a, Point b) {
        float lenA = b.x - a.x;
        float lenB = b.y - a.y;
        float lenC = (float) Math.sqrt(lenA * lenA + lenB * lenB);
        float ang = (float) Math.acos(lenA / lenC);
        ang = ang * (b.y < a.y ? -1 : 1);
        return ang;
    }

    public void setOnColorChangedListener(OnColorChangedListener listener) {
        this.listener = listener;
    }

    /**
     * @param attrs
     * @describe 初始化操作
     */
    private void init(AttributeSet attrs) {

        DisplayMetrics dm = new DisplayMetrics();
        dm = getResources().getDisplayMetrics();
        float density = dm.density; // 屏幕密度（像素比例：0.75/1.0/1.5/2.0）
        int densityDPI = dm.densityDpi; // 屏幕密度（每寸像素：120/160/240/320）
        screenWidth = dm.widthPixels; // 屏幕宽（像素，如：3200px）
        screenHeight = dm.heightPixels; // 屏幕高（像素，如：1280px）
        // 获取自定义组件的属性
        cpCircle = screenWidth / 2 - 150;
        TypedArray types = context.obtainStyledAttributes(attrs,
                R.styleable.color_picker);
        try {
            bigCircle = types.getDimensionPixelOffset(
                    R.styleable.color_picker_circle_radius, screenWidth / 2 - 150);

            rudeRadius = types.getDimensionPixelOffset(
                    R.styleable.color_picker_center_radius, 80);
//            centerColor = types.getColor(
//                    R.styleable.color_picker_center_color, Color.WHITE);
        } finally {
            types.recycle(); // TypeArray用完需要recycle
        }
        // 内圈图
        bitmapBack = BitmapFactory.decodeResource(getResources(), R.drawable.circle2);
        bitmapBack = Bitmap.createScaledBitmap(bitmapBack, bigCircle * 2, bigCircle * 2,
                false);
        // 水泡图
        centerBg = BitmapFactory.decodeResource(getResources(), R.drawable.circle1);
        centerBg = Bitmap.createScaledBitmap(centerBg, rudeRadius * 2, rudeRadius * 2, false);

        //指南针
        cpBg = BitmapFactory.decodeResource(getResources(), R.drawable.circle3);
        cpBg = Bitmap.createScaledBitmap(cpBg, cpCircle * 2, cpCircle * 2, false);

        // 中心位置坐标
        centerPoint = new Point(bigCircle - 80, bigCircle - 80);
        mRockPosition = new Point(centerPoint);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mCenterPaint = new Paint();
        mCenterPaint.setAntiAlias(true);
//        cpPaint = new Paint();
//        cpPaint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 画背景图片
        canvas.drawBitmap(bitmapBack, 0, 0, mPaint);
        canvas.drawBitmap(centerBg, mRockPosition.x, mRockPosition.y, mCenterPaint);

//        canvas.drawBitmap(cpBg, 0, 0, cpPaint);
//        canvas.rotate(-currentAngle, 0, 0);
//        if (currentAngle > 360f) {
//            currentAngle = currentAngle - 360f;
//        } else {
//            currentAngle = currentAngle + 2f;
//        }

        // 画中心小球
//        canvas.drawCircle(mRockPosition.x, mRockPosition.y, rudeRadius,
//                mCenterPaint);
    }

    public Thread thread = new Thread() {

        @Override
        public void run() {
            super.run();
            while (true) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                postInvalidate();
            }
        }
    };


//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        switch (event.getAction()) {
//            case MotionEvent.ACTION_DOWN: // 按下
//                length = getLength(event.getX(), event.getY(), centerPoint.x,
//                        centerPoint.y);
//                if (length <= bigCircle - rudeRadius * 2) {
//                    mRockPosition.set((int) event.getX(), (int) event.getY());
//                } else {
//                    mRockPosition = getBorderPoint(centerPoint, new Point(
//                            (int) event.getX(), (int) event.getY()), bigCircle
//                            - rudeRadius * 2);
//                }
//                listener.onColorChangDown(bitmapBack.getPixel(mRockPosition.x,
//                        mRockPosition.y));
//                break;
//            case MotionEvent.ACTION_MOVE: // 移动
//                length = getLength(event.getX(), event.getY(), centerPoint.x,
//                        centerPoint.y);
//                if (length <= bigCircle - rudeRadius * 2) {
//                    mRockPosition.set((int) event.getX(), (int) event.getY());
//                } else {
//                    mRockPosition = getBorderPoint(centerPoint, new Point(
//                            (int) event.getX(), (int) event.getY()), bigCircle
//                            - rudeRadius * 2);
//                }
//                listener.onColorChange(bitmapBack.getPixel(mRockPosition.x,
//                        mRockPosition.y));
////                Log.e("---", "中心坐标：" + centerPoint.x + "||" + centerPoint.y);
////                Log.e("--", "x轴=" + mRockPosition.x + "y轴=" + mRockPosition.y);
//                break;
//            case MotionEvent.ACTION_UP:// 抬起
//                listener.onColorChangeUp(bitmapBack.getPixel(mRockPosition.x,
//                        mRockPosition.y));
//                break;
//
//            default:
//                break;
//        }
//        invalidate(); // 更新画布
//        return true;
//    }

    public void moveCenter(int x, int y) {
        length = getLength(x, y, centerPoint.x,
                centerPoint.y);
        if (length <= bigCircle - rudeRadius * 2) {
            mRockPosition.set((int) x, (int) y);
        } else {
            mRockPosition = getBorderPoint(centerPoint, new Point(
                    (int) x, (int) y), bigCircle
                    - rudeRadius * 2);
        }
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // 视图大小设置为直径
        setMeasuredDimension(bigCircle * 2, bigCircle * 2);
    }

    // 颜色发生变化的回调接口
    public interface OnColorChangedListener {
        void onColorChange(int color);

        void onColorChangeUp(int color);

        void onColorChangDown(int color);
    }
}
