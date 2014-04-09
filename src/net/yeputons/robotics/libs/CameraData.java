package net.yeputons.robotics.libs;

public class CameraData {
    private byte[] data;
    private int width, height;

    public CameraData(int width, int height) {
        this.data = null;
        this.width = width;
        this.height = height;
    }

    void setData(byte[] data) {
        this.data = data;
    }

    public int getWidth() {
        return width;
    }
    public int getHeight() {
        return height;
    }
    public int getColor(int x, int y) {
        if (data == null) throw new IllegalStateException("No data array is available");
        return CameraSurface.getColor(data, x, y, width, height);
    }
}

