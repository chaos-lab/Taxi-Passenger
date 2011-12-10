package com.chaos.taxi.map;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import com.chaos.taxi.R;
import com.chaos.taxi.map.TaxiOverlayItem.TaxiOverlayItemParam;
import com.chaos.taxi.map.UserOverlayItem.UserOverlayItemParam;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;

public class TaxiMapView extends MapView {
	private static final String TAG = "TaxiMapView";
	// Time in ms
	static final int LONGPRESS_THRESHOLD = 500;
	static final int DOUBLECLICK_THRESHOLD = 500;
	static final int ZOOM_LEVEL = 15;
	static final GeoPoint NANJING_DEFAULT = new GeoPoint(32041800, 118784064);

	private Context mContext = null;
	private GeoPoint mLastMapCenter;
	private Timer mLongpressTimer = new Timer();
	private long mLastClickTime = 0;
	private Point mLastClickPixPoint = null;

	// private MyLocationOverlay mMyLocationOverlay = null;
	private TaxiItemizedOverlay mTaxiItemizedOverlay = null;

	// record the overlay items for remove
	TaxiOverlayItem mTaxiOverlayItem = null;
	UserOverlayItem mUserOverlayItem = null;
	ArrayList<TaxiOverlayItem> mAroundTaxiOverlayItem = new ArrayList<TaxiOverlayItem>();

	public TaxiMapView(Context context, String apiKey) {
		super(context, apiKey);
	}

	public TaxiMapView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public TaxiMapView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public void initTaxiMapView(Context context) {
		mContext = context;
		setBuiltInZoomControls(true);
		getController().setZoom(ZOOM_LEVEL);
		getController().animateTo(NANJING_DEFAULT);
		mTaxiItemizedOverlay = new TaxiItemizedOverlay(context.getResources()
				.getDrawable(R.drawable.ic_launcher), context);
		getOverlays().add(mTaxiItemizedOverlay);
		invalidate();
	}

	public TaxiOverlayItemParam findInAroundTaxi(String phoneNumber) {
		Log.d(TAG, "findInAroundTaxi: " + phoneNumber);
		Iterator<TaxiOverlayItem> iter = mAroundTaxiOverlayItem.iterator();
		while (iter.hasNext()) {
			TaxiOverlayItem item = iter.next();
			Log.d(TAG, "item.mParam.mPhoneNumber is "
					+ item.mParam.mPhoneNumber);
			if (item.mParam.mPhoneNumber.equals(phoneNumber)) {
				return item.getParam();
			}
		}
		Log.w(TAG, "findInAroundTaxi find nothing!!!");
		return null;
	}

	public void showMyTaxiOverlay(TaxiOverlayItemParam param) {
		if (param.mPoint == null) {
			Log.wtf(TAG, "showTaxiOverlay with empty point. "
					+ param.mCarNumber);
			return;
		}
		Log.d(TAG, "showMyTaxiOverlay: " + param.mPoint.getLatitudeE6() + " "
				+ param.mPoint.getLongitudeE6());
		getController().animateTo(param.mPoint);
		mTaxiOverlayItem = new TaxiOverlayItem(mContext, param, true);
		mTaxiItemizedOverlay.addOverlayItem(mTaxiOverlayItem);
		invalidate();
	}

	public void removeMyTaxiOverlay() {
		if (mTaxiOverlayItem != null) {
			mTaxiItemizedOverlay.removeOverlayItem(mTaxiOverlayItem);
			mTaxiOverlayItem = null;
			invalidate();
		}
	}

	public void showUserOverlay(UserOverlayItemParam param) {
		if (param == null || param.mPoint == null) {
			Log.wtf(TAG, "no user point specified in showUserOverlay!");
			return;
		}

		Log.d(TAG, "showUserOverlay: " + param.mPoint.getLatitudeE6() + " "
				+ param.mPoint.getLongitudeE6());
		if (mUserOverlayItem != null) {
			mTaxiItemizedOverlay.removeOverlayItem(mUserOverlayItem);
		}
		mUserOverlayItem = new UserOverlayItem(mContext, param);
		mUserOverlayItem.setMarker(mContext.getResources().getDrawable(
				R.drawable.my_location));
		mTaxiItemizedOverlay.addOverlayItem(mUserOverlayItem);
		getController().animateTo(param.mPoint);
		invalidate();
	}

	public void removeUserOverlay() {
		if (mUserOverlayItem != null) {
			mTaxiItemizedOverlay.removeOverlayItem(mUserOverlayItem);
			mUserOverlayItem = null;
			invalidate();
		}
	}

	public void addAroundTaxiOverlay(TaxiOverlayItemParam param) {
		Log.d(TAG, "addAroundTaxiOverlay: " + param.mCarNumber);
		if (param.mPoint == null) {
			Log.wtf(TAG, "addAroundTaxiOverlay with empty point. ");
			return;
		}
		Log.d(TAG, "addAroundTaxiOverlay: " + param.mPoint.getLatitudeE6()
				+ " " + param.mPoint.getLongitudeE6());
		TaxiOverlayItem item = new TaxiOverlayItem(mContext, param, false);
		if (mTaxiOverlayItem != null
				&& item.mParam.mCarNumber
						.equals(mTaxiOverlayItem.mParam.mCarNumber)) {
			Log.d(TAG, item.mParam.mCarNumber + " is myCar.");
			return;
		}
		mAroundTaxiOverlayItem.add(item);
		mTaxiItemizedOverlay.addOverlayItem(item);
		invalidate();
	}

	public void showAroundTaxiOverlay(TaxiOverlayItemParam[] params) {
		for (int index = 0; index < params.length; ++index) {
			if (params[index].mPoint == null) {
				Log.wtf(TAG, "showAroundOverlay with empty point. "
						+ params[index].mCarNumber);
				continue;
			}
			TaxiOverlayItem item = new TaxiOverlayItem(mContext, params[index],
					false);
			if (mTaxiOverlayItem != null
					&& item.mParam.mCarNumber
							.equals(mTaxiOverlayItem.mParam.mCarNumber)) {
				Log.d(TAG, item.mParam.mCarNumber + " is myCar.");
				continue;
			}
			mAroundTaxiOverlayItem.add(item);
			mTaxiItemizedOverlay.addOverlayItem(item);
		}
		invalidate();
	}

	public void removeAroundOverlay() {
		Iterator<TaxiOverlayItem> iter = mAroundTaxiOverlayItem.iterator();
		while (iter.hasNext()) {
			mTaxiItemizedOverlay.removeOverlayItem(iter.next());
		}
		mAroundTaxiOverlayItem.clear();
		invalidate();
	}

	private void handleLongPress(GeoPoint lastPixPoint) {
		// Log.d(TAG, "handle long press here!");
	}

	private void handleDoubleClick(Point lastPixPoint) {
		if (lastPixPoint == null) {
			Log.w(TAG, "lastPixPoint is null!");
			return;
		}

		if (this.getZoomLevel() >= 5) {
			GeoPoint mapCenter = getMapCenter();
			Point mapCenterPixPoint = getProjection().toPixels(mapCenter, null);
			GeoPoint animatePoint = getProjection().fromPixels(
					(mapCenterPixPoint.x + lastPixPoint.x) / 2,
					(mapCenterPixPoint.y + lastPixPoint.y) / 2);

			getController().animateTo(animatePoint);
		}
		getController().zoomIn();
	}

	private void handleMapPress(final MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			long currentTime = System.currentTimeMillis();
			if (currentTime - mLastClickTime <= DOUBLECLICK_THRESHOLD) {
				handleDoubleClick(mLastClickPixPoint);
				mLastClickTime = 0;
				mLastClickPixPoint = null;
			} else {
				mLastClickTime = currentTime;
				mLastClickPixPoint = new Point((int) event.getX(),
						(int) event.getY());
			}

			mLongpressTimer = new Timer();
			mLongpressTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					GeoPoint longpressLocation = getProjection().fromPixels(
							(int) event.getX(), (int) event.getY());
					handleLongPress(longpressLocation);
				}
			}, LONGPRESS_THRESHOLD);

			mLastMapCenter = getMapCenter();
		} else if (event.getAction() == MotionEvent.ACTION_MOVE) {
			if (!getMapCenter().equals(mLastMapCenter)) {
				mLongpressTimer.cancel();
			}
			mLastMapCenter = getMapCenter();
		} else if (event.getAction() == MotionEvent.ACTION_UP) {
			mLongpressTimer.cancel();
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// GeoPoint point = getProjection().fromPixels((int) event.getX(),
		// (int) event.getY());
		// Log.d(TAG, point.getLatitudeE6() + " " + point.getLongitudeE6());
		handleMapPress(event);
		if (mTaxiItemizedOverlay.size() > 0) {
			return super.onTouchEvent(event);
		}
		return true;
	}
}
