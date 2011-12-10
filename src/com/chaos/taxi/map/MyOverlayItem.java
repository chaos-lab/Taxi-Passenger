package com.chaos.taxi.map;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

public abstract class MyOverlayItem extends OverlayItem {

	public int mType = 0;
	public static final int USER_OVERLAY_ITEM = 0;
	public static final int DRIVER_OVERLAY_ITEM = 1;
	public static final int AROUND_OVERLAY_ITEM = 2;

	public MyOverlayItem(GeoPoint point, String title, String snippet, int type) {
		super(point, title, snippet);
		mType = type;
	}

	public static class MyOverlayItemParam {
		public MyOverlayItemParam() {

		}
	}

	abstract public void onTap();

	abstract public MyOverlayItemParam getParam();
}
