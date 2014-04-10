
package net.pikanji.camerapreviewsample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/**
 * Sample driver class to demonstrate the use of CameraPreview class.
 */
public class MainActivity extends Activity implements View.OnClickListener {
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.main);
        
        findViewById(R.id.button_sample).setOnClickListener(this);
        findViewById(R.id.button_test).setOnClickListener(this);
        findViewById(R.id.button_intent).setOnClickListener(this);
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
            default:
                break;
        }
    }
}
