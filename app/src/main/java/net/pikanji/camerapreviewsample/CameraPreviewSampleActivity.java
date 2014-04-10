package net.pikanji.camerapreviewsample;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

public class CameraPreviewSampleActivity extends ActionBarActivity {

    private CameraPreview mPreview;
    private RelativeLayout mLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide status-bar
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Hide title-bar, must be before setContentView
        //requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Requires RelativeLayout.
        // FIXME this will still work since we wrap our own relativeLayout around it
        mLayout = new RelativeLayout(this);
        // set a dark background here to hide the fact that this view might not wrap parent
        // completely in order to respect aspect ratio of the camera
        mLayout.setBackgroundColor(getResources().getColor(R.color.background));
        setContentView(mLayout);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Set the second argument by your choice.
        // Usually, 0 for back-facing camera, 1 for front-facing camera.
        // If the OS is pre-gingerbreak, this does not have any effect.
        mPreview = new CameraPreview(this, 0);
        LayoutParams previewLayoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        // Un-comment below lines to specify the size.
        //previewLayoutParams.height = 500;
        //previewLayoutParams.width = 500;

        // Un-comment below line to specify the position.
        //mPreview.setCenterPosition(270, 130);

        mLayout.addView(mPreview, 0, previewLayoutParams);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPreview.stop();
        mLayout.removeView(mPreview); // This is necessary.
        mPreview = null;
    }
}
