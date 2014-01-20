package net.yeputons.robotics.libs;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.view.*;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: Egor Suvorov
 * Date: 29.07.13
 * Time: 20:54
 * To change this template use File | Settings | File Templates.
 */
public class CameraSurface extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
    protected Camera camera;
    protected CameraListener listener = null;

    public CameraSurface(Context context) {
        super(context);

        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void setCameraListener(CameraListener listener) {
        this.listener = listener;
    }

    protected int estimateBufferSize(Camera.Parameters parameters) {
        int format = parameters.getPreviewFormat();
        Camera.Size size = parameters.getPreviewSize();

        assert (format != ImageFormat.YUY2);
        int bits = ImageFormat.getBitsPerPixel(format);
        return (size.width * size.height * bits + 7) / 8;
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        camera = Camera.open();

        Camera.Parameters parameters = camera.getParameters();

        try {
            camera.setPreviewDisplay(surfaceHolder);
        } catch (IOException e) {
            throw new RuntimeException("Cannot setPreviewDisplay", e);
        }
    }

    int[] argbBuffer;
    Camera.Size size;
    boolean isPortrait;

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
        camera.stopPreview();

        Camera.Parameters parameters = camera.getParameters();
        size = parameters.getPreviewSize();
        argbBuffer = new int[size.width * size.height];

        Display display = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        isPortrait = false;
        switch (display.getRotation()) {
            case Surface.ROTATION_0:
                camera.setDisplayOrientation(90);
                isPortrait = true;
                break;
            case Surface.ROTATION_90:
                camera.setDisplayOrientation(0);
                break;
            case Surface.ROTATION_180:
                camera.setDisplayOrientation(270);
                isPortrait = true;
                break;
            case Surface.ROTATION_270:
                camera.setDisplayOrientation(180);
                break;
        }
        requestLayout();

        if (listener != null)
            listener.onSizeChange(size.width, size.height, isPortrait);

        camera.setPreviewCallbackWithBuffer(this);
        camera.addCallbackBuffer(new byte[estimateBufferSize(parameters)]);

        camera.startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        if (listener != null)
            listener.onCameraFrame(bytes, size.width, size.height, isPortrait, null);
        camera.addCallbackBuffer(bytes);
    }

    public static int convertYuvToRgb(int y, int u, int v) {
        u -= 128;
        v -= 128;

        int r, g, b;

        r = y + (int) 1.772f * u;
        g = y - (int) (0.344f * u + 0.714f * v);
        b = y + (int) 1.402f * v;
        r = r > 255 ? 255 : r < 0 ? 0 : r;
        g = g > 255 ? 255 : g < 0 ? 0 : g;
        b = b > 255 ? 255 : b < 0 ? 0 : b;





        return 0xff000000 | (r << 16) | (g << 8) | b;
    }

    public static int getColor(byte[] bytes, int x, int y, int width, int height) {
        int Y = bytes[y * width + x] & 0xFF;
        int offUv = height * width + (y >> 1) * width + (x - (x & 1));
        int U = bytes[offUv] & 0xFF;
        int V = bytes[offUv + 1] & 0xFF;
        return convertYuvToRgb(Y, U, V);
    }
}
