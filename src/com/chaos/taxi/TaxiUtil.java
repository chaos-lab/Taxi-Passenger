package com.chaos.taxi;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;

import com.chaos.taxi.RequestProcessor.Request;
import com.google.android.maps.GeoPoint;

import android.content.Context;
import android.location.Location;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

public class TaxiUtil {
	private static final String TAG = "TaxiUtil";
	static final int LOCATIONTIME_THRESHOLD = 10000;
	static final String TAXICOOKIE_FILE = "taxi_cookie";

	private static Location chooseTimeOrderedLocation(Location location1,
			Location location2) {
		if ((location1.getTime() - location2.getTime()) > LOCATIONTIME_THRESHOLD) {
			return location1;
		} else {
			return (location1.getAccuracy() > location2.getAccuracy()) ? location1
					: location2;
		}
	}

	public static Location chooseBetterLocation(Location location1,
			Location location2) {
		if (location1 != null) {
			Log.i(TAG, "Location1 : Lat: " + location1.getLatitude() + " Lng: "
					+ location1.getLongitude());
		} else {
			Log.d(TAG, "Location1 is null!");
			return location2;
		}
		if (location2 != null) {
			Log.i(TAG, "Location2 : Lat: " + location2.getLatitude() + " Lng: "
					+ location2.getLongitude());
		} else {
			Log.d(TAG, "Location2 is null!");
			return location1;
		}

		if (location1.getTime() > location2.getTime()) {
			return chooseTimeOrderedLocation(location1, location2);
		} else {
			return chooseTimeOrderedLocation(location2, location1);
		}
	}

	public static GeoPoint locationToGeoPoint(Location location) {
		if (location == null) {
			return null;
		}
		return new GeoPoint((int) (location.getLatitude() * 1e6),
				(int) (location.getLongitude() * 1e6));
	}
	
	public static Location geoPointToLocation(GeoPoint point) {
		if (point == null) {
			return null;
		}
		Location loc = new Location("FromGeoPoint");
		loc.setLatitude((float)(point.getLatitudeE6()) / (float)1e6);
		loc.setLongitude((float)(point.getLongitudeE6()) / (float)1e6);
		return loc;
	}

	public static void saveCookie(Context context, String cookieValue) {
		FileOutputStream out = null;
		try {
			out = context.openFileOutput(TAXICOOKIE_FILE, 0);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		if (out == null) {
			Log.w(TAG, "saveCookie: Cannot open file: " + TAXICOOKIE_FILE);
		}

		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
		try {
			writer.write(cookieValue);
			writer.newLine();
			writer.close();
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void setCookie(Context context, String url) {
		FileInputStream in = null;
		try {
			in = context.openFileInput(TAXICOOKIE_FILE);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		if (in == null) {
			Log.w(TAG, "saveCookie: Cannot open file: " + TAXICOOKIE_FILE);
		}

		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		String cookieStr = null;
		try {
			cookieStr = reader.readLine();
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		Log.d(TAG, "cookieStr: " + cookieStr);
		if (cookieStr == null) {
			return;
		}

		CookieSyncManager.createInstance(context);
		CookieManager cookieManager = CookieManager.getInstance();
		cookieManager.removeSessionCookie();
		cookieManager.setCookie(url, cookieStr);
		CookieSyncManager.getInstance().sync();
	}

	private static String getServerAddressByRequestType(String requestType) {
		if (requestType.equals(RequestProcessor.FIND_TAXI_REQUEST)) {
			return RequestProcessor.HTTPSERVER + "/passenger/taxi/near";
		} else if (requestType.equals(RequestProcessor.CALL_TAXI_REQUEST)) {
			return RequestProcessor.HTTPSERVER + "/passenger/message";
		} else if (requestType.equals(RequestProcessor.LOCATION_UPDATE_REQUEST)) {
			return RequestProcessor.HTTPSERVER + "/passenger/location/update";
		} else if (requestType.equals(RequestProcessor.REFRESH_REQUEST)) {
			return RequestProcessor.HTTPSERVER + "/passenger/refresh";
		}
		return null;
	}
	
	public static HttpUriRequest generateHttpUriRequest(Request request) {
		if (request == null) {
			return null;
		}
		String serverAddress = getServerAddressByRequestType(request.mRequestType);
		if (serverAddress == null) {
			Log.wtf(TAG, "serverAddress is null for requestType: "
					+ request.mRequestType);
			return null;
		}

		HttpUriRequest httpUriRequest = null;
		if (request.mRequestJson != null) {
			httpUriRequest = new HttpPost(serverAddress);
			try {
				((HttpPost) httpUriRequest).setEntity(new StringEntity(
						request.mRequestJson.toString()));
			} catch (UnsupportedEncodingException e1) {
				Log.e(TAG, "cannot encode the string to StringEntity. "
						+ request);
				e1.printStackTrace();
				return null;
			}
		} else {
			// it is the refresh request
			httpUriRequest = new HttpGet(serverAddress);
		}
		return httpUriRequest;
	}
}