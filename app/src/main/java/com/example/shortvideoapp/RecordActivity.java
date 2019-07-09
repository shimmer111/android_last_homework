package com.example.shortvideoapp;

import androidx.appcompat.app.AppCompatActivity;

import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import java.io.IOException;

import static com.example.shortvideoapp.utils.Utils.MEDIA_TYPE_IMAGE;
import static com.example.shortvideoapp.utils.Utils.MEDIA_TYPE_VIDEO;
import static com.example.shortvideoapp.utils.Utils.getOutputMediaFile;

public class RecordActivity extends AppCompatActivity {
    private static final String TAG = "RecordActivity";
    public Button mBtnRecordSwitch;
    public SurfaceView mSurfaceView;
    private Camera mCamera;
    private int CAMERA_TYPE = android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK;
    private boolean isRecording = false;
    private int rotationDegree = 0;
    private SurfaceHolder surfaceHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_record);
        mBtnRecordSwitch = findViewById(R.id.btnRecordSwitch);
        mSurfaceView = findViewById(R.id.img);
        surfaceHolder = mSurfaceView.getHolder();

        mCamera = getCamera(CAMERA_TYPE);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceHolder.addCallback(new PlayerCallBack());
//        mBtnRecordSwitchClick();
        mBtnRecordSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //todo 录制，第一次点击是start，第二次点击是stop
                if (isRecording) {
                    //todo 停止录制
                    Log.d(TAG, "ButtonOnClick: finish record");
                    releaseMediaRecorder();
                    isRecording = false;
                    mBtnRecordSwitch.setText(R.string.start_record);
                    Log.d(TAG, "ButtonOnClick: finish record end");

                } else {
                    //todo 录制
                    Log.d(TAG, "ButtonOnClick: start record");
                    prepareVideoRecorder();
                    isRecording = true;
                    mBtnRecordSwitch.setText(R.string.recording);
                    Log.d(TAG, "ButtonOnClick: start record end");
                }
            }
        });

    }

    private Camera getCamera(int type){
        CAMERA_TYPE = type;
        if (mCamera != null) {
            releaseCameraAndPreview();
        }
        Camera cam = Camera.open(type);
        rotationDegree = getCameraDisplayOrientation(type);
        cam.setDisplayOrientation(rotationDegree);
        return cam;
    }
    private static final int DEGREE_90 = 90;
    private static final int DEGREE_180 = 180;
    private static final int DEGREE_270 = 270;
    private static final int DEGREE_360 = 360;
    private int getCameraDisplayOrientation(int cameraId) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = DEGREE_90;
                break;
            case Surface.ROTATION_180:
                degrees = DEGREE_180;
                break;
            case Surface.ROTATION_270:
                degrees = DEGREE_270;
                break;
            default:
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % DEGREE_360;
            result = (DEGREE_360 - result) % DEGREE_360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + DEGREE_360) % DEGREE_360;
        }
        return result;
    }
    private void releaseCameraAndPreview() {
        //todo 释放camera资源
        if(mCamera != null){
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }
    private void mBtnRecordSwitchClick(){
        mBtnRecordSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isRecording) {
                    releaseMediaRecorder();
                    isRecording = false;
                    mBtnRecordSwitch.setText(R.string.start_record);
                } else {
                    prepareVideoRecorder();
                    isRecording = true;
                    mBtnRecordSwitch.setText(R.string.recording);
                }
            }
        });
    }
    private MediaRecorder mMediaRecorder;
    private void releaseMediaRecorder(){
        mMediaRecorder.stop();
        mMediaRecorder.reset();
        mMediaRecorder.release();
        mMediaRecorder = null;
        mCamera.lock();
    }
    private boolean prepareVideoRecorder(){
        mMediaRecorder = new MediaRecorder();
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);

        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

        mMediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());

        mMediaRecorder.setPreviewDisplay(surfaceHolder.getSurface());
        mMediaRecorder.setOrientationHint(rotationDegree);

        try {
            mMediaRecorder.prepare();
            mMediaRecorder.start();
        } catch (Exception e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }



    private class PlayerCallBack implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        }
        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            releaseCameraAndPreview();
        }
    }
}
