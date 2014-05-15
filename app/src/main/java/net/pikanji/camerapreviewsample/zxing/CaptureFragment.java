package net.pikanji.camerapreviewsample.zxing;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.ResultPointCallback;

import net.pikanji.camerapreviewsample.R;
import net.pikanji.camerapreviewsample.zxing.camera.CameraManager;

/**
 * This activity opens the camera and does the actual scanning on a background
 * thread. It draws a viewfinder to help the user place the barcode correctly,
 * shows feedback as the image processing is happening, and then overlays the
 * results when a scan is successful.
 * 
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 */
public class CaptureFragment extends Fragment implements
		SurfaceHolder.Callback {

	private static final String TAG = CaptureFragment.class.getSimpleName();

	// moved to mediator
	// private CameraManager cameraManager;

	private CaptureActivityHandler handler;
	private Result savedResultToShow;
	private ViewfinderView viewfinderView;
	protected Result lastResult;
	private boolean hasSurface;

	private Collection<BarcodeFormat> decodeFormats;
	private Map<DecodeHintType, ?> decodeHints;
	private String characterSet;
	private InactivityTimer inactivityTimer;

	private SurfaceView mSurfaceView;

	public static final String KEY_SCAN_RESULT = "scan_result";
	public static final String KEY_INFO_TEXT = "info_text";

	/**
	 * the result handler used as a callback to get the result from the qr code
	 * scanning back
	 */
	private FragmentResultHandler mResultHandler;

	Handler getHandler() {
		return this.handler;
	}

	/**
	 * default ctor needed for android, use
	 * {@link #createInstance(String, FragmentResultHandler)} instead
	 */
	public CaptureFragment() {
		// set this instance to mediator also
		Mediator.getInstance().setCaptureActivity(this);
	}

	/**
	 * use this to create an instance of this fragment
	 * 
	 * @param resultHandler
	 * @return
	 */
	public static CaptureFragment createInstance(final String infoText,
			FragmentResultHandler resultHandler) {
		CaptureFragment cf = new CaptureFragment();
		Bundle b = new Bundle();
		b.putString(KEY_INFO_TEXT, infoText);
		cf.setArguments(b);
		cf.setResultHandler(resultHandler);
		return cf;
	}

	public void setResultHandler(FragmentResultHandler resultHandler) {
		mResultHandler = resultHandler;
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		// force portrait 
		getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		Window window = getActivity().getWindow();
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		// setContentView(R.layout.fragment_capture);

		hasSurface = false;
		inactivityTimer = new InactivityTimer(getActivity());

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_capture, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		mSurfaceView = (SurfaceView) view.findViewById(R.id.preview_view);
		viewfinderView = (ViewfinderView) view
				.findViewById(R.id.viewfinder_view);

		// set text if available
		Bundle args = getArguments();
		if (args != null && args.containsKey(KEY_INFO_TEXT)) {
			((TextView) view.findViewById(R.id.text_info)).setText(args
					.getString(KEY_INFO_TEXT));
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onResume() {
		super.onResume();

		// CameraManager must be initialized here, not in onCreate(). This is
		// necessary because we don't want to open the camera driver and measure
		// the screen size if we're going to show another fragment first

		// update mediator with this object
		Mediator.getInstance().setCameraManager(
				new CameraManager(getActivity().getApplication()));

		// removed viewfinderView completely

		// viewfinderView.setCameraManager(cameraManager); // this shouldn't be

		handler = null;
		lastResult = null;

		resetStatusView();

		SurfaceHolder surfaceHolder = mSurfaceView.getHolder();

		// for < api level 11 this property has to be set manually
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB)
			surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		if (hasSurface) {
			// The activity was paused but not stopped, so the surface still
			// exists. Therefore
			// surfaceCreated() won't be called, so init the camera here.
			initCamera(surfaceHolder);
		} else {
			// Install the callback and wait for surfaceCreated() to init the
			// camera.
			surfaceHolder.addCallback(this);
		}

		inactivityTimer.onResume();

		decodeFormats = null;
		characterSet = null;

	}

	@Override
	public void onPause() {
		if (handler != null) {
			handler.quitSynchronously();
			handler = null;
		}
		inactivityTimer.onPause();
		Mediator.getInstance().getCameraManager().closeDriver();
		if (!hasSurface) {
			SurfaceHolder surfaceHolder = mSurfaceView.getHolder();
			surfaceHolder.removeCallback(this);
		}
		super.onPause();
	}

	@Override
	public void onDestroy() {
		inactivityTimer.shutdown();
		super.onDestroy();
	}

	// these settings (light, focus, ... were removed, check revisions)

	private void decodeOrStoreSavedBitmap(Bitmap bitmap, Result result) {
		// Bitmap isn't used yet -- will be used soon
		if (handler == null) {
			savedResultToShow = result;
		} else {
			if (result != null) {
				savedResultToShow = result;
			}
			if (savedResultToShow != null) {
				Message message = Message.obtain(handler,
						R.id.decode_succeeded, savedResultToShow);
				handler.sendMessage(message);
			}
			savedResultToShow = null;
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (holder == null) {
			Log.e(TAG,
					"*** WARNING *** surfaceCreated() gave us a null surface!");
		}
		if (!hasSurface) {
			hasSurface = true;
			initCamera(holder);
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// just mark we have a surface here
		hasSurface = false;
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// nothing to do 
	}

	/**
	 * A valid barcode has been found, so give an indication of success and show
	 * the results.
	 * 
	 * @param rawResult
	 *            The contents of the barcode.
	 * @param scaleFactor
	 *            amount by which thumbnail was scaled
	 * @param barcode
	 *            A greyscale bitmap of the camera data which was decoded.
	 */
	public void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor) {
		inactivityTimer.onActivity();
		lastResult = rawResult;

		CharSequence displayContents = rawResult.getText();

		// handle the result here, extremely simplified
		Bundle data = new Bundle();
		data.putString(KEY_SCAN_RESULT, displayContents.toString());
		mResultHandler.handleFragmentResult(data);

	}

	private void initCamera(SurfaceHolder surfaceHolder) {
		if (surfaceHolder == null) {
			throw new IllegalStateException("No SurfaceHolder provided");
		}
		if (Mediator.getInstance().getCameraManager().isOpen()) {
			Log.w(TAG,
					"initCamera() while already open -- late SurfaceView callback?");
			return;
		}
		try {
			Mediator.getInstance().getCameraManager().openDriver(surfaceHolder);
			// Creating the handler starts the preview, which can also throw a
			// RuntimeException.
			if (handler == null) {
				handler = new CaptureActivityHandler(decodeFormats,
						decodeHints, characterSet,
						// new ViewfinderResultPointCallback(viewfinderView));//
						// no longer needed, cameraManager);
						new ResultPointCallback() {

							@Override
							public void foundPossibleResultPoint(
									ResultPoint point) {
								// viewfinderView.addPossibleResultPoint(point);
								// ignore these now
							}
						});

			}
			decodeOrStoreSavedBitmap(null, null);
		} catch (IOException ioe) {
			Log.e(TAG, ioe.getMessage());
		} catch (RuntimeException e) {
			// Barcode Scanner has seen crashes in the wild of this variety:
			// java.?lang.?RuntimeException: Fail to connect to camera service
			Log.e(TAG, "Unexpected error initializing camera ",
                    e);
		}
	}

	public void restartPreviewAfterDelay(long delayMS) {
		if (handler != null) {
			handler.sendEmptyMessageDelayed(R.id.restart_preview, delayMS);
		}
		resetStatusView();
	}

	private void resetStatusView() {
		viewfinderView.setVisibility(View.VISIBLE);
		lastResult = null;
	}

	public void drawViewfinder() {
		viewfinderView.drawViewfinder();
	}

}
