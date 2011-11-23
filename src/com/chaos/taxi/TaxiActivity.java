package com.chaos.taxi;

import com.chaos.taxi.map.TaxiMapView;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class TaxiActivity extends MapActivity {
	private final static String TAG = "TaxiActivity";
	static final int CALL_TAXI_REQUEST_CODE = 9188868;

	static String sNickName = null;
	static String sPhoneNumber = null;

	LocationManager mLocationManager = null;
	TaxiMapView mMapView = null;
	Button mCallTaxiBtn = null;
	Button mAutoLocateBtn = null;
	Button mLocateTaxiBtn = null;
	Button mFindTaxiBtn = null;
	boolean mHasTaxi = false;
	GeoPoint mTaxiPoint = null;

	LocationListener locationListener = new LocationListener() {
		public void onLocationChanged(Location location) {
			if (location != null) {
				Log.i(TAG, "Location changed : Lat: " + location.getLatitude()
						+ " Lng: " + location.getLongitude());
				GeoPoint point = getUserLastKnownGeoPoint();
				RequestProcessor.setUserGeoPoint(point);
			}
		}

		public void onProviderDisabled(String provider) {
			Log.d(TAG, "provider: " + provider + " disabled!");
		}

		public void onProviderEnabled(String provider) {
			Log.d(TAG, "provider: " + provider + " enabled!");
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
			Log.d(TAG, "provider: " + provider + " status: " + status);
		}
	};

	private GeoPoint getUserLastKnownGeoPoint() {
		Location gpsLocation = mLocationManager
				.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if (gpsLocation != null) {
			Log.d(TAG, "gpsLocation is " + gpsLocation.getLatitude() + " "
					+ gpsLocation.getLongitude());
		}
		Location networkLocation = mLocationManager
				.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		if (networkLocation != null) {
			Log.d(TAG, "networkLocation is " + networkLocation.getLatitude()
					+ " " + networkLocation.getLongitude());
		}

		Location location = TaxiUtil.chooseBetterLocation(gpsLocation,
				networkLocation);

		if (location != null) {
			return TaxiUtil.locationToGeoPoint(location);
		} else {
			return null;
		}
	}

	private void setButtonListener() {
		mCallTaxiBtn = (Button) findViewById(R.id.call_taxi_btn);
		mAutoLocateBtn = (Button) findViewById(R.id.auto_locate_btn);
		mLocateTaxiBtn = (Button) findViewById(R.id.locate_taxi_btn);
		mFindTaxiBtn = (Button) findViewById(R.id.find_taxi_btn);

		mCallTaxiBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				long requestId = RequestProcessor.sendCallTaxiRequest();
				if (requestId != -1) {
					Intent intent = new Intent(TaxiActivity.this,
							WaitTaxiActivity.class);
					intent.putExtra("WaitTaxiTime",
							RequestProcessor.REQUEST_TIMEOUT_THRESHOLD / 1000);
					intent.putExtra("RequestId", requestId);
					startActivityForResult(intent, CALL_TAXI_REQUEST_CODE);
				} else {
					AlertDialog dialog = new AlertDialog.Builder(
							TaxiActivity.this)
							.setIcon(android.R.drawable.ic_dialog_info)
							.setTitle("CallTaxiFail: ")
							.setMessage("Already have a taxi")
							.setPositiveButton("Locate",
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog,
												int which) {
											RequestProcessor
													.sendLocateTaxiRequest();
										}
									}).setNegativeButton("OK", null).create();
					dialog.show();
				}
			}
		});

		mLocateTaxiBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				RequestProcessor.sendLocateTaxiRequest();
			}
		});

		mFindTaxiBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				RequestProcessor.sendFindTaxiRequest();
			}
		});

		mAutoLocateBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Log.d(TAG, "currentZoomLevel: " + mMapView.getZoomLevel());
				RequestProcessor.setUserGeoPoint(getUserLastKnownGeoPoint());
				RequestProcessor.sendLocateUserRequest();
			}
		});
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == CALL_TAXI_REQUEST_CODE) {
			if (resultCode == WaitTaxiActivity.CANCEL_WAIT) {
				RequestProcessor.cancelCallTaxiRequest();
			} else if (resultCode == WaitTaxiActivity.SUCCEED_WAIT) {
				RequestProcessor.showCallTaxiSucceedDialog();
			} else if (resultCode == WaitTaxiActivity.REJECT_WAIT) {
				AlertDialog dialog = new AlertDialog.Builder(TaxiActivity.this)
						.setIcon(android.R.drawable.ic_dialog_info)
						.setTitle("CallTaxiFail: ")
						.setMessage("Driver Reject!")
						.setNegativeButton("OK", null).create();
				dialog.show();
			}
		}
	}

	@Override
	public void onStart() {
		mMapView = (TaxiMapView) findViewById(R.id.google_map);
		mMapView.initTaxiMapView(this);

		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				1000, 0, locationListener);
		mLocationManager.requestLocationUpdates(
				LocationManager.NETWORK_PROVIDER, 1000, 0, locationListener);

		RequestProcessor.initRequestProcessor(TaxiActivity.this, mMapView);

		setButtonListener();
		super.onStart();
	}

	@Override
	public void onDestroy() {
		RequestProcessor.logout();
		super.onDestroy();
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
}