package org.linphone.ui;

import org.linphone.CallManager;
import org.linphone.LinphoneManager;
import org.linphone.LinphoneUtils;
import org.linphone.R;
import org.linphone.compatibility.Compatibility;
import org.linphone.compatibility.CompatibilityScaleGestureDetector;
import org.linphone.compatibility.CompatibilityScaleGestureListener;
import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCore;
import org.linphone.mediastream.Log;
import org.linphone.mediastream.video.AndroidVideoWindowImpl;
import org.linphone.mediastream.video.capture.hwconf.AndroidCameraConfiguration;
import org.linphone.mediastream.video.display.GL2JNIView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.RelativeLayout;

public class LinphoneVideoView extends RelativeLayout implements OnGestureListener, OnDoubleTapListener, CompatibilityScaleGestureListener {
	private GL2JNIView mVideoView;
	private SurfaceView mVideoPreview;
	private AndroidVideoWindowImpl androidVideoWindowImpl;
	
	private GestureDetector mGestureDetector;
	private float mZoomFactor = 1.f;
	private float mZoomCenterX, mZoomCenterY;
	private CompatibilityScaleGestureDetector mScaleDetector;
	
	@SuppressLint("ClickableViewAccessibility")
	private OnTouchListener mTouchListener = new OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (mScaleDetector != null) {
				mScaleDetector.onTouchEvent(event);
			}
			
			mGestureDetector.onTouchEvent(event);
			if (mTouchListener != null) {
				mTouchListener.onTouch(v, event);
			}
			
			performClick();
			return true;
		}
	};

	public LinphoneVideoView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initViews(context);
	}

	public LinphoneVideoView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initViews(context);
	}

	public LinphoneVideoView(Context context) {
		super(context);
		initViews(context);
	}
	
	public void switchCamera() {
		try {
			int videoDeviceId = LinphoneManager.getLc().getVideoDevice();
			videoDeviceId = (videoDeviceId + 1) % AndroidCameraConfiguration.retrieveCameras().length;
			LinphoneManager.getLc().setVideoDevice(videoDeviceId);
			CallManager.getInstance().updateCall();
			
			// previous call will cause graph reconstruction -> regive preview window
			if (mVideoPreview != null) {
				LinphoneManager.getLc().setPreviewWindow(mVideoPreview);
			} else {
				Log.w("No video preview window...");
			}
		} catch (ArithmeticException ae) {
			Log.e("Cannot swtich camera : no camera");
		}
	}
	
	public void setOnTouchListener(OnTouchListener listener) {
		mTouchListener = listener;
	}
	
	public void release() {
		if (androidVideoWindowImpl != null) {
			synchronized (androidVideoWindowImpl) {
				LinphoneManager.getLc().setVideoWindow(null);
				LinphoneManager.getLc().setPreviewWindow(null);
				androidVideoWindowImpl.release();
				androidVideoWindowImpl = null;
			}
		}
	}
	
	public void setPreviewVisibility(boolean visibility) {
		if (mVideoPreview != null) {
			mVideoPreview.setVisibility(visibility ? View.VISIBLE : View.GONE);
		}
	}
	
	@SuppressLint("ClickableViewAccessibility")
	private void initViews(Context context) {
		mVideoView = new GL2JNIView(context);
		LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		addView(mVideoView, params);
		mVideoView.setOnTouchListener(mTouchListener);
		
		mVideoPreview = new SurfaceView(context);
		int width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, getResources().getInteger(R.integer.video_preview_w), getResources().getDisplayMetrics());
		int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, getResources().getInteger(R.integer.video_preview_h), getResources().getDisplayMetrics());
		LayoutParams params2 = new RelativeLayout.LayoutParams(width, height);
		params2.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
		params2.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
		addView(mVideoPreview, 1, params2);
		
		fixZOrder(mVideoView, mVideoPreview);
	}
	
	private void fixZOrder(SurfaceView video, SurfaceView preview) {
		video.setZOrderOnTop(false);
		preview.setZOrderOnTop(true);
		preview.setZOrderMediaOverlay(true); // Needed to be able to display control layout over
	}
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		
		if (androidVideoWindowImpl == null) {
			androidVideoWindowImpl = new AndroidVideoWindowImpl(mVideoView, mVideoPreview, new AndroidVideoWindowImpl.VideoWindowListener() {
				@Override
				public void onVideoRenderingSurfaceReady(AndroidVideoWindowImpl vw, SurfaceView surface) {
					LinphoneManager.getLc().setVideoWindow(vw);
				}
	
				@Override
				public void onVideoRenderingSurfaceDestroyed(AndroidVideoWindowImpl vw) {
					LinphoneCore lc = LinphoneManager.getLc(); 
					if (lc != null) {
						lc.setVideoWindow(null);
					}
				}
	
				@Override
				public void onVideoPreviewSurfaceReady(AndroidVideoWindowImpl vw, SurfaceView surface) {
					LinphoneManager.getLc().setPreviewWindow(surface);
				}
	
				@Override
				public void onVideoPreviewSurfaceDestroyed(AndroidVideoWindowImpl vw) {
					LinphoneCore lc = LinphoneManager.getLc(); 
					if (lc != null) {
						lc.setPreviewWindow(null);
					}
				}
			});
		}
		
		mGestureDetector = new GestureDetector(this); 
		mScaleDetector = Compatibility.getScaleGestureDetector(getContext(), this);
	}
	
	@Override
	protected void onDetachedFromWindow() {
		if (mGestureDetector != null) {
			mGestureDetector.setOnDoubleTapListener(null);
			mGestureDetector = null;
		}
		if (mScaleDetector != null) {
			mScaleDetector.destroy();
			mScaleDetector = null;
		}
		
		super.onDetachedFromWindow();
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
	}
	
    public boolean onScale(CompatibilityScaleGestureDetector detector) {
    	mZoomFactor *= detector.getScaleFactor();
        // Don't let the object get too small or too large.
		// Zoom to make the video fill the screen vertically
		float portraitZoomFactor = ((float) mVideoView.getHeight()) / (float) ((3 * mVideoView.getWidth()) / 4);
		// Zoom to make the video fill the screen horizontally
		float landscapeZoomFactor = ((float) mVideoView.getWidth()) / (float) ((3 * mVideoView.getHeight()) / 4);
    	mZoomFactor = Math.max(0.1f, Math.min(mZoomFactor, Math.max(portraitZoomFactor, landscapeZoomFactor)));

    	LinphoneCall currentCall = LinphoneManager.getLc().getCurrentCall();
    	if (currentCall != null) {
    		currentCall.zoomVideo(mZoomFactor, mZoomCenterX, mZoomCenterY);
            return true;
    	}
        return false;
    }

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		if (LinphoneUtils.isCallEstablished(LinphoneManager.getLc().getCurrentCall())) {
			if (mZoomFactor > 1) {
				// Video is zoomed, slide is used to change center of zoom
				if (distanceX > 0 && mZoomCenterX < 1) {
					mZoomCenterX += 0.01;
				} else if(distanceX < 0 && mZoomCenterX > 0) {
					mZoomCenterX -= 0.01;
				}
				if (distanceY < 0 && mZoomCenterY < 1) {
					mZoomCenterY += 0.01;
				} else if(distanceY > 0 && mZoomCenterY > 0) {
					mZoomCenterY -= 0.01;
				}
				
				if (mZoomCenterX > 1)
					mZoomCenterX = 1;
				if (mZoomCenterX < 0)
					mZoomCenterX = 0;
				if (mZoomCenterY > 1)
					mZoomCenterY = 1;
				if (mZoomCenterY < 0)
					mZoomCenterY = 0;
				
				LinphoneManager.getLc().getCurrentCall().zoomVideo(mZoomFactor, mZoomCenterX, mZoomCenterY);
				return true;
			}
		}
		
		return false;
	}

	@Override
	public boolean onDoubleTap(MotionEvent e) {
		if (LinphoneUtils.isCallEstablished(LinphoneManager.getLc().getCurrentCall())) {
			if (mZoomFactor == 1.f) {
				// Zoom to make the video fill the screen vertically
				float portraitZoomFactor = ((float) mVideoView.getHeight()) / (float) ((3 * mVideoView.getWidth()) / 4);
				// Zoom to make the video fill the screen horizontally
				float landscapeZoomFactor = ((float) mVideoView.getWidth()) / (float) ((3 * mVideoView.getHeight()) / 4);
				
				mZoomFactor = Math.max(portraitZoomFactor, landscapeZoomFactor);
			}
			else {
				resetZoom();
			}
			
			LinphoneManager.getLc().getCurrentCall().zoomVideo(mZoomFactor, mZoomCenterX, mZoomCenterY);
			return true;
		}
		
		return false;
	}

	private void resetZoom() {
		mZoomFactor = 1.f;
		mZoomCenterX = mZoomCenterY = 0.5f;
	}
	
	@Override
	public boolean onDown(MotionEvent e) {
		return true; // Needed to make the GestureDetector working
	}

	@Override
	public boolean onDoubleTapEvent(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onSingleTapConfirmed(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
		
	}

	@Override
	public void onShowPress(MotionEvent e) {
		
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		return false;
	}
}
