package org.linphone;
/*
VideoCallFragment.java
Copyright (C) 2012  Belledonne Communications, Grenoble, France

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/
import org.linphone.ui.LinphoneVideoView;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

/**
 * @author Sylvain Berfini
 */
public class VideoCallFragment extends Fragment {
	private RelativeLayout mRoot;
	private LinphoneVideoView mVideo;
	private boolean isVideoSet = false;
	private InCallActivity inCallActivity;
	
	@SuppressLint("ClickableViewAccessibility")
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {		
        View view = inflater.inflate(R.layout.video, container, false);
        mRoot = (RelativeLayout) view.findViewById(R.id.video_frame);
        
        mVideo = (LinphoneVideoView) view.findViewById(R.id.videoSurface);
        mVideo.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (inCallActivity != null) {
					inCallActivity.displayVideoCallControlsIfHidden();
				}
				return true;
			}
		});
        LinphoneManager.getInstance().setLinphoneVideo(mVideo);
		
        isVideoSet = true;
		return view;
    }
	
	public void switchCamera() {
		mVideo.switchCamera();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		LinphoneVideoView video = LinphoneManager.getInstance().getLinphoneVideoIfAvailable();
		if (video != null && !isVideoSet) {
			LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
			video.setPreviewVisibility(true);
			mRoot.addView(video, 0, params);
			isVideoSet = true;
			mVideo = video;
		}
	}
	
	@Override
	public void onPause() {
		if (isVideoSet) {
			mRoot.removeView(mVideo);
			isVideoSet = false;
		}
		super.onPause();
	}
	
	@Override
	public void onDestroy() {
		inCallActivity = null;
		super.onDestroy();
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		inCallActivity = (InCallActivity) activity;
		if (inCallActivity != null) {
			inCallActivity.bindVideoFragment(this);
		}
	}
}
