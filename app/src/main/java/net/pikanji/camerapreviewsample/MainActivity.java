
package net.pikanji.camerapreviewsample;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

/**
 * Sample driver class to demonstrate the use of CameraPreview class.
 */
public class MainActivity extends Activity implements View.OnClickListener {

    /**
     * request code for image capture to check on handling intent result
     */
    private static final int REQUEST_IMAGE_CAPTURE = 1;

    /**
     * the only bitmap so we can keep track of it and recycle if needed
     */
    private Bitmap mBitmap =null;

    /**
     * an imageview to show the taken picture
     */
    private ImageView mImageView = null;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.main);
        
        findViewById(R.id.button_sample).setOnClickListener(this);
        findViewById(R.id.button_test).setOnClickListener(this);
        findViewById(R.id.button_intent).setOnClickListener(this);

        mImageView = (ImageView) findViewById(R.id.imageView);
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()) {
            case R.id.button_sample:
                intent = new Intent(this, CameraPreviewSampleActivity.class);
                startActivity(intent);
                break;
            case R.id.button_test:
                intent = new Intent(this, CameraPreviewTestActivity.class);
                startActivity(intent);
                break;
            case R.id.button_intent:
                dispatchTakePictureIntent();
                break;
        }
    }

    /**
     * helper to launch the capture intent
     */
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // here we get back the thumbnail of the taken picture
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            // Since we used an intent the default camera app (user preferences) is used and
            // the image is stored in gallery
            mBitmap = (Bitmap) extras.get("data");
            mImageView.setImageBitmap(mBitmap);
        }

        // TODO add code to show how to retrieve the image and/or store it
    }

    @Override
    protected void onDestroy() {
        // TODO check if this is sufficient
        if( mBitmap != null && !mBitmap.isRecycled())
            mBitmap.recycle();
        mBitmap = null;
    }
}
