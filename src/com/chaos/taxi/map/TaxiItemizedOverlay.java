package com.chaos.taxi.map;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.chaos.taxi.R;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;

public class TaxiItemizedOverlay extends ItemizedOverlay<OverlayItem> {
	private static final String TAG = "TaxiItemizedOverlay";
	private Context mContext = null;
	private ArrayList<MyOverlayItem> mOverlayItems = new ArrayList<MyOverlayItem>();

	public TaxiItemizedOverlay(Drawable defaultMarker) {
		super(boundCenterBottom(defaultMarker));
	}

	public TaxiItemizedOverlay(Drawable defaultMarker, Context context) {
		this(defaultMarker);
		mContext = context;
	}

	@Override
	protected OverlayItem createItem(int i) {
		return mOverlayItems.get(i);
	}

	@Override
	public int size() {
		return mOverlayItems.size();
	}

	public void addOverlayItem(MyOverlayItem item) {
		switch (item.mType) {
		case MyOverlayItem.USER_OVERLAY_ITEM:
			item.setMarker(boundCenterBottom(mContext.getResources()
					.getDrawable(R.drawable.my_location)));
			break;
		case MyOverlayItem.DRIVER_OVERLAY_ITEM:
			item.setMarker(boundCenterBottom(mContext.getResources()
					.getDrawable(R.drawable.my_car)));
			break;
		case MyOverlayItem.AROUND_OVERLAY_ITEM:
			item.setMarker(boundCenterBottom(mContext.getResources()
					.getDrawable(R.drawable.car)));
			break;
		}
		mOverlayItems.add(item);
		populate();
	}

	public void removeOverlayItem(MyOverlayItem item) {
		mOverlayItems.remove(item);
		setLastFocusedIndex(-1);
		populate();
	}

	@Override
	protected boolean onTap(int index) {
		Log.d(TAG, "onTap: " + index);
		MyOverlayItem item = mOverlayItems.get(index);
		if (item == null) {
			return true;
		}
		item.onTap();
		return true;
	}
}
