package net.yeputons.robotics.libs;

import android.content.Context;
import android.content.res.Resources;
import android.hardware.usb.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Created by Egor Suvorov on 25.07.2014.
 */
public class NxtUsbController extends NxtAbstractController {
    public static int DEFAULT_TIMEOUT = 100; // msec

    public static UsbDevice getAnyNxtDevice(Context context) {
        UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        for (Map.Entry<String, UsbDevice> item : deviceList.entrySet()) {
            UsbDevice dev = item.getValue();
            if (dev.getVendorId() == 0x0694 && dev.getProductId() == 0x0002) {
                return dev;
            }
        }
        throw new NoSuchElementException();
    }

    protected Context mContext;
    protected UsbDevice mUsbDevice;
    protected UsbInterface mUsbInterface;
    protected UsbEndpoint mIn, mOut;
    protected UsbDeviceConnection mConnection;

    public NxtUsbController(Context context, UsbDevice usbDevice) {
        super();
        mContext = context;
        mUsbDevice = usbDevice;
        if (mUsbDevice.getInterfaceCount() != 1)
            throw new IllegalArgumentException(String.format("Invalid NXT: %d interfaces instead of one", mUsbDevice.getInterfaceCount()));

        mUsbInterface = mUsbDevice.getInterface(0);
        if (mUsbInterface.getEndpointCount() != 2)
            throw new IllegalArgumentException(String.format("Invalid NXT: %d endpoints instead of two", mUsbInterface.getEndpointCount()));

        for (int i = 0; i < mUsbInterface.getEndpointCount(); i++) {
            UsbEndpoint endPoint = mUsbInterface.getEndpoint(i);
            if (endPoint.getType() != UsbConstants.USB_ENDPOINT_XFER_BULK) {
                throw new IllegalArgumentException(String.format("Invalid NXT: non-bulk endpoint #%d", endPoint.getEndpointNumber()));
            }
            if (endPoint.getDirection() == UsbConstants.USB_DIR_IN) {
                mIn = endPoint;
            } else {
                mOut = endPoint;
            }
        }
        if (mIn == null) throw new IllegalArgumentException("Invalid NXT: no input endpoint");
        if (mOut == null) throw new IllegalArgumentException("Invalid NXT: no output endpoint");
    }

    @Override
    public void connect() throws IOException {
        if (isConnected())
            throw new IllegalStateException("Already connected");
        UsbManager manager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        mConnection = manager.openDevice(mUsbDevice);
        if (mConnection == null) throw new IOException("Unable to open NXT device");
        if (!mConnection.claimInterface(mUsbInterface, true)) {
            mConnection.close();
            throw new IOException("Unable to claim NXT's interface");
        }
        mIsConnected = true;
    }

    @Override
    public void disconnect() {
        if (!isConnected())
            throw new IllegalStateException("Not connected");
        mConnection.close();
        mIsConnected = false;
    }

    @Override
    synchronized public byte[] sendDirectCommand(byte[] command) throws IOException, InterruptedException {
        if (!isConnected())
            throw new IllegalStateException("Not connected");

        if (command.length < 2 || command.length > MAX_DIRECT_COMMAND_LENGTH)
            throw new IllegalArgumentException("command.length should be >= 2 && <= MAX_DIRECT_COMMAND_LENGTH");
        if (command[0] != (byte) 0x00 && command[0] != (byte) 0x80)
            throw new IllegalArgumentException("command[0] should be either 0x00 or 0x80");

        int res = mConnection.bulkTransfer(mOut, command, command.length, DEFAULT_TIMEOUT);
        if (res < 0) throw new IOException("Transfer failed, bulkTransfer returned " + res);

        if (command[0] == (byte) 0x80) return null; // Response is not required

        byte[] answer = new byte[MAX_DIRECT_COMMAND_LENGTH];
        int ansLen = mConnection.bulkTransfer(mIn, answer, answer.length, DEFAULT_TIMEOUT);
        if (ansLen < 0) throw new IOException("Answer receipt failed, bulkTransfer returned " + res);

        byte[] result = new byte[ansLen];
        System.arraycopy(answer, 0, result, 0, ansLen);
        return result;
    }

    @Override
    public void setMessageWriteListener(MessageWriteListener listener) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
