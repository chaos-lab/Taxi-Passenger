package com.chaos.taxi.map;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;

import com.google.android.maps.GeoPoint;

public class UserOverlayItem extends MyOverlayItem {
	private static final String TAG = "UserOverlayItem";
	private Context mContext = null;
	private UserOverlayItemParam mParam = null;

	public static class UserOverlayItemParam extends MyOverlayItemParam {
		public GeoPoint mPoint = null;

		public UserOverlayItemParam(GeoPoint point) {
			mPoint = point;
		}
	}

	public UserOverlayItem(Context context, UserOverlayItemParam param) {
		super(param.mPoint, "Me", "I am here!");
		mContext = context;
		mParam = param;
	}

	@Override
	public UserOverlayItemParam getParam() {
		return mParam;
	}

	@Override
	public void onTap() {
		Log.d(TAG, "onTap");
		AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
		dialog.setTitle("Me");
		dialog.setMessage("I'm here!");
		dialog.setNegativeButton("OK", null);
		dialog.show();
	}
}
