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
import java.net.URLEncoder;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;

import com.chaos.taxi.RequestManager.Request;
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
		loc.setLatitude((float) (point.getLatitudeE6()) / (float) 1e6);
		loc.setLongitude((float) (point.getLongitudeE6()) / (float) 1e6);
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

	public static String getServerAddressByRequestType(String requestType) {
		if (requestType.equals(RequestManager.FIND_TAXI_REQUEST)) {
			return RequestProcessor.HTTPSERVER + "/taxi/near";
		} else if (requestType.equals(RequestManager.CALL_TAXI_REQUEST)) {
			return RequestProcessor.HTTPSERVER + "/service/create";
		} else if (requestType.equals(RequestManager.LOCATION_UPDATE_REQUEST)) {
			return RequestProcessor.HTTPSERVER + "/passenger/location/update";
		} else if (requestType.equals(RequestManager.REFRESH_REQUEST)) {
			return RequestProcessor.HTTPSERVER + "/passenger/refresh";
		} else if (requestType.equals(RequestManager.CANCEL_CALL_TAXI_REQUEST)) {
			return RequestProcessor.HTTPSERVER + "/service/cancel";
		} else if (requestType.equals(RequestManager.CALL_TAXI_COMPLETE)) {
			return RequestProcessor.HTTPSERVER + "/service/complete";
		} else if (requestType.equals(RequestManager.LOCATION_UPDATE_REQUEST)) {
			return RequestProcessor.HTTPSERVER + "/passenger/location/update";
		} else if (requestType.equals(RequestManager.SIGNIN_REQUEST)) {
			return RequestProcessor.HTTPSERVER + "/passenger/signin";
		} else if (requestType.equals(RequestManager.REGISTER_REQUEST)) {
			return RequestProcessor.HTTPSERVER + "/passenger/signup";
		} else if (requestType.equals(RequestManager.SIGNOUT_REQUEST)) {
			return RequestProcessor.HTTPSERVER + "/passenger/signout";
		}
		return null;
	}

	public static boolean isGetRequest(String requestType) {
		if (requestType.equals(RequestManager.REFRESH_REQUEST)
				|| requestType.equals(RequestManager.FIND_TAXI_REQUEST)) {
			return true;
		}
		return false;
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
		if (!isGetRequest(request.mRequestType)) {
			Log.d(TAG, "generate HttpPost for " + request.mRequestType);
			httpUriRequest = new HttpPost(serverAddress);
			if (request.mRequestJson != null) {
				Log.d(TAG,
						"request json str is "
								+ request.mRequestJson.toString());
				if (!TaxiUtil.setHttpEntity((HttpPost) httpUriRequest,
						request.mRequestJson.toString())) {
					return null;
				}
			}
		} else {
			Log.d(TAG, "generate HttpGet for " + request.mRequestType);
			if (request.mRequestJson != null) {
				Log.d(TAG,
						"request json str is "
								+ request.mRequestJson.toString());
				serverAddress += "?json_data="
						+ URLEncoder.encode(request.mRequestJson.toString()); 
				Log.d(TAG, "serverAddress now is " + serverAddress);
			} /*
			 * else { serverAddress += "?json_data={}"; }
			 */
			httpUriRequest = new HttpGet(serverAddress);
		}
		return httpUriRequest;
	}

	public static boolean setHttpEntity(HttpEntityEnclosingRequestBase request,
			String entityStr) {
		entityStr = "json_data=" + entityStr;
		Log.d(TAG, "setHttpEntity: " + entityStr);
		try {
			request.setEntity(new StringEntity(entityStr));
			return true;
		} catch (UnsupportedEncodingException e) {
			Log.e(TAG, "cannot encode the string to StringEntity. " + request);
			e.printStackTrace();
			return false;
		}
	}
}
