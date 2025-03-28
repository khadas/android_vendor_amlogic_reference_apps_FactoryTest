package cn.com.factorytest;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.widget.Button;
import java.util.List;

public class MipiCameraTestActivity extends Activity implements SurfaceHolder.Callback {
    private static final String TAG = "MIPI_CAM";
    private static final int CAMERA_ID = 0;
    private SurfaceView mSurfaceView;
    private Camera mCamera;
    private SurfaceHolder mHolder;
    private boolean mPreviewRunning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.camera_mipi);

        initUI();
        setupCameraSurface();
    }

    private void initUI() {
        Button success = findViewById(R.id.btn_success);
        Button fail = findViewById(R.id.btn_fail);
        success.setOnClickListener(v -> saveTestResult(1));
        fail.setOnClickListener(v -> saveTestResult(0));
    }

    private void setupCameraSurface() {
        mSurfaceView = findViewById(R.id.mSurfaceView_mipi);
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    private void saveTestResult(int result) {
        Settings.System.putInt(getContentResolver(),
            "Khadas_mipi_camera_test", result);
        finish();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "Surface created");
        try {
            openCamera();
            setupCameraParameters();
            startPreview(holder);
        } catch (Exception e) {
            Log.e(TAG, "Camera init failed", e);
            releaseCamera();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "Surface changed");
        if (mPreviewRunning) {
            stopPreview();
        }
        startPreview(holder);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "Surface destroyed");
        releaseCamera();
    }

    private void openCamera() throws RuntimeException {
        if (mCamera == null) {
            mCamera = Camera.open(CAMERA_ID);
        }
    }

    @SuppressLint("NewApi")
    private void setupCameraParameters() {
        Camera.Parameters params = mCamera.getParameters();
        params.setPreviewFormat(ImageFormat.NV21);

        List<String> focusModes = params.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }

        Camera.Size optimalSize = getOptimalPreviewSize();
        if (optimalSize != null) {
            params.setPreviewSize(optimalSize.width, optimalSize.height);
        }

        mCamera.setParameters(params);
        setCameraDisplayOrientation();
    }

    private Camera.Size getOptimalPreviewSize() {
        Display display = getWindowManager().getDefaultDisplay();
        Point displaySize = new Point();
        display.getRealSize(displaySize);

        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();

        double targetRatio = (double) displaySize.x / displaySize.y;
        double minDiff = Double.MAX_VALUE;
        Camera.Size optimalSize = null;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(ratio - targetRatio);
            }
        }
        return optimalSize != null ? optimalSize : sizes.get(0);
    }

    private void setCameraDisplayOrientation() {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(CAMERA_ID, info);

        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;
        } else {
            result = (info.orientation - degrees + 360) % 360;
        }
        mCamera.setDisplayOrientation(result);
    }

    private void startPreview(SurfaceHolder holder) {
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
            mPreviewRunning = true;
        } catch (Exception e) {
            Log.e(TAG, "Start preview failed", e);
        }
    }

    private void stopPreview() {
        try {
            mCamera.stopPreview();
            mPreviewRunning = false;
        } catch (Exception e) {
            Log.d(TAG, "Stop preview failed", e);
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!isChangingConfigurations()) {
            releaseCamera();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseCamera();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return event.getKeyCode() == KeyEvent.KEYCODE_BACK ||
            super.dispatchKeyEvent(event);
    }
}