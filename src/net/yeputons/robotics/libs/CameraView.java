package net.yeputons.robotics.libs;

import android.content.Context;
import android.graphics.*;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

/**
 * Created with IntelliJ IDEA.
 * User: Egor Suvorov
 * Date: 01.08.13
 * Time: 10:18
 * To change this template use File | Settings | File Templates.
 */
public class CameraView extends ViewGroup {
    protected class CameraDrawer extends View {
        protected Bitmap toDraw;
        protected Matrix matrix;

        public CameraDrawer(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (toDraw != null && matrix != null)
                canvas.drawBitmap(toDraw, matrix, null);
        }

        public void setBitmapAndMatrix(Bitmap toDraw, Matrix matrix) {
            this.toDraw = toDraw;
            this.matrix = matrix;
        }
    }

    protected CameraSurface surface;
    protected CameraDrawer drawer;
    protected CameraListener listener;
    protected Bitmap toDraw;
    protected Canvas canvas;
    protected Matrix drawMatrix;
    protected int preferredWidth, preferredHeight;
    private long lastTime = Long.MIN_VALUE, lastProcessingTime = Long.MAX_VALUE;

    public CameraView(Context context) {
        super(context);
        surface = new CameraSurface(context);
        addView(surface);

        drawer = new CameraDrawer(context);
        addView(drawer);

        surface.setCameraListener(new CameraListener() {
            @Override
            public void onCameraFrame(byte[] data, int width, int height, int cameraDisplayOrientation, Canvas _canvas) {
                lastProcessingTime = System.currentTimeMillis() - lastTime;
                lastTime = System.currentTimeMillis();
                if (listener != null) {
                    listener.onCameraFrame(data, width, height, cameraDisplayOrientation, canvas);
                }
                drawer.setBitmapAndMatrix(toDraw, drawMatrix);
                drawer.invalidate();
            }

            @Override
            public void onSizeChange(int width, int height, int cameraDisplayOrientation) {
                if (toDraw != null) toDraw.recycle();
                toDraw = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                canvas = new Canvas(toDraw);

                Display display = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
                drawMatrix = new Matrix();
                drawMatrix.postRotate(cameraDisplayOrientation);
                switch (cameraDisplayOrientation) {
                    case 0: break;
                    case 90: drawMatrix.postTranslate(height, 0); break;
                    case 180: drawMatrix.postTranslate(width, height); break;
                    case 270: drawMatrix.postTranslate(0, width); break;
                }
                if (cameraDisplayOrientation % 180 == 0) {
                    preferredWidth = width;
                    preferredHeight = height;
                } else {
                    preferredWidth = height;
                    preferredHeight = width;
                }
                drawMatrix.postScale(
                        (float) getWidth() / preferredWidth,
                        (float) getHeight() / preferredHeight
                );

                if (listener != null)
                    listener.onSizeChange(width, height, cameraDisplayOrientation);
            }
        });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (toDraw == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        int modeW = MeasureSpec.getMode(widthMeasureSpec);
        int modeH = MeasureSpec.getMode(heightMeasureSpec);
        int parW = MeasureSpec.getSize(widthMeasureSpec);
        int parH = MeasureSpec.getSize(heightMeasureSpec);

        if (modeW == MeasureSpec.UNSPECIFIED && modeH == MeasureSpec.UNSPECIFIED) {
            setMeasuredDimension(preferredWidth, preferredHeight);
            return;
        }

        if (modeW == MeasureSpec.UNSPECIFIED) parW = Integer.MAX_VALUE;
        if (modeH == MeasureSpec.UNSPECIFIED) parH = Integer.MAX_VALUE;
        int newW = Math.min(parW, parH * preferredWidth / preferredHeight);
        int newH = Math.min(parH, parW * preferredHeight / preferredWidth);
        if (modeW == MeasureSpec.EXACTLY) newW = parW;
        if (modeH == MeasureSpec.EXACTLY) newH = parH;
        setMeasuredDimension(newW, newH);
    }

    @Override
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        for (int i = 0; i < getChildCount(); i++)
            getChildAt(i).layout(l, t, r, b);
    }

    public long getLastProcessingTime() {
        return lastProcessingTime;
    }

    public void setCameraListener(CameraListener listener) {
        this.listener = listener;
    }
}
