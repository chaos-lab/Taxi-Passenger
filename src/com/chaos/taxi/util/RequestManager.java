package com.chaos.taxi.util;

import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.chaos.taxi.util.RequestManager.Request;
import com.google.android.maps.GeoPoint;

public class RequestManager {
	public static final String TAG = "RequestManager";

	static final String CANCEL_CALL_TAXI_REQUEST = "call-taxi-cancel";
	static final String CALL_TAXI_REQUEST = "call-taxi";
	static final String CALL_TAXI_RESPONSE = "call-taxi-reply";
	static final String CALL_TAXI_COMPLETE = "call-taxi-complete";
	static final String FIND_TAXI_REQUEST = "find-taxi";
	static final String LOCATION_UPDATE_REQUEST = "location-update";
	static final String REFRESH_REQUEST = "refresh-request";
	final static String SIGNIN_REQUEST = "signin-request";
	final static String REGISTER_REQUEST = "register-request";
	final static String SIGNOUT_REQUEST = "signout-request";

	static final String GET_CALLTAXI_HISTORY_REQUEST = "get-calltaxi-history";
	static final String GET_CALLTAXI_EVALUATION_REQUEST = "get-calltaxi-evaluation";
	static final String GET_USER_EVALUATION_REQUEST = "get-user-evaluation";
	static final String PUSH_HISTORY_EVALUATION_REQUEST = "push-history-evaluation";
	final static String PUSH_GPSCODER_REQUEST = "push-gps-coder-request";

	static ArrayList<Request> mRequests = new ArrayList<Request>();

	public static class Request {
		long mRequestTime;
		String mRequestType;
		JSONObject mRequestJson;
		Object mData;

		public Request(String requestType, JSONObject requestJson) {
			mRequestTime = System.currentTimeMillis();
			mRequestType = requestType;
			mRequestJson = requestJson;
			mData = null;
		}

		public long getLongData() {
			return (Long) mData;
		}
	}

	public static void addLocationUpdateRequest(GeoPoint point) {
		if (point == null) {
			Log.i(TAG, "null point do not need update!");
			return;
		}
		JSONObject jsonObj = new JSONObject();
		try {
			jsonObj.put("latitude", point.getLatitudeE6() / 1000000.0);
			jsonObj.put("longitude", point.getLongitudeE6() / 1000000.0);
		} catch (JSONException e) {
			e.printStackTrace();
			return;
		}
		addRequest(new Request(LOCATION_UPDATE_REQUEST, jsonObj));
	}

	public static Request generateFindTaxiRequest(GeoPoint userPoint) {
		if (userPoint == null) {
			Log.w(TAG,
					"user geoPoint not updated! cannot send FindTaxi request!");
			return null;
		}

		JSONObject jsonObj = new JSONObject();
		try {
			jsonObj.put("latitude", userPoint.getLatitudeE6() / 1000000.0);
			jsonObj.put("longitude", userPoint.getLongitudeE6() / 1000000.0);
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}

		return new Request(FIND_TAXI_REQUEST, jsonObj);
	}

	public static void addCallTaxiRequest(GeoPoint userPoint,
			long callTaxiRequestKey, String taxiPhoneNumber) {
		Log.d(TAG, "generateCallTaxiRequest, callTaxiRequestKey: "
				+ callTaxiRequestKey + " taxiPhoneNumber: " + taxiPhoneNumber);
		JSONObject jsonObj = new JSONObject();
		try {
			// jsonObj.put("type", CALL_TAXI_REQUEST);
			jsonObj.put("key", callTaxiRequestKey);
			if (taxiPhoneNumber != null) {
				jsonObj.put("driver", taxiPhoneNumber);
			}
			JSONObject locationJson = new JSONObject();
			if (userPoint != null) {
				locationJson.put("latitude",
						userPoint.getLatitudeE6() / 1000000.0);
				locationJson.put("longitude",
						userPoint.getLongitudeE6() / 1000000.0);
			} else {
				Log.w(TAG, "CallTaxi: userPoint is still null!");
			}
			jsonObj.put("origin", locationJson);
		} catch (JSONException e) {
			e.printStackTrace();
			return;
		}
		Request request = new Request(CALL_TAXI_REQUEST, jsonObj);
		request.mData = (Long) callTaxiRequestKey;
		addRequest(request);
		return;
	}

	public static Request generateCancelCallTaxiRequest(long callTaxiRequestKey) {
		JSONObject jsonObj = new JSONObject();
		try {
			// jsonObj.put("type", CANCEL_CALL_TAXI_REQUEST);
			jsonObj.put("key", callTaxiRequestKey);
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
		Request request = new Request(CANCEL_CALL_TAXI_REQUEST, jsonObj);
		request.mData = (Long) callTaxiRequestKey;
		return request;
	}

	public static void addRequest(Request request, Object data) {
		if (request == null) {
			Log.d(TAG, "request is null!");
			return;
		}

		request.mData = data;
		synchronized (mRequests) {
			for (int i = 0; i < mRequests.size(); ++i) {
				if (mRequests.get(i).mRequestType.equals(request.mRequestType)) {
					mRequests.set(i, request);
				}
			}
			mRequests.add(request);
		}
	}

	public static void addRequest(Request request) {
		addRequest(request, null);
	}

	public static boolean removeRequest(String requestType) {
		Log.i(TAG, "removeRequst: " + requestType);
		if (requestType == null) {
			return false;
		}
		synchronized (mRequests) {
			Iterator<Request> iter = mRequests.iterator();
			while (iter.hasNext()) {
				if (requestType.equals(iter.next().mRequestType)) {
					mRequests.remove(iter);
					return true;
				}
			}
			return false;
		}
	}

	public static Request popRequest() {
		synchronized (mRequests) {
			if (mRequests.size() > 0) {
				Request request = mRequests.get(0);
				mRequests.remove(0);
				return request;
			} else {
				return null;
			}
		}
	}

	public static void addPushGpscoderRequest(String addrStr, double latitude,
			double longitude) {
		JSONObject jsonObj = new JSONObject();
		try {
			jsonObj.put("latitude", latitude);
			jsonObj.put("longitude", longitude);
			jsonObj.put("name", addrStr);
		} catch (JSONException e) {
			e.printStackTrace();
			return;
		}
		Request request = new Request(PUSH_GPSCODER_REQUEST, jsonObj);
		addRequest(request);
	}

	public static Request generateQueryHistoryRequest(long startTimeStamp,
			long endTimeStamp, int countNeedToQuery) {
		JSONObject jsonObj = new JSONObject();
		try {
			jsonObj.put("start_time", startTimeStamp);
			jsonObj.put("end_time", endTimeStamp);
			jsonObj.put("count", countNeedToQuery);
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
		Request request = new Request(GET_CALLTAXI_HISTORY_REQUEST, jsonObj);
		return request;
	}

	public static Request generateSubmitCommentRequest(long id, String comment,
			double rating) {
		JSONObject jsonObj = new JSONObject();
		try {
			jsonObj.put("id", id);
			jsonObj.put("score", rating);
			jsonObj.put("comment", comment);
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
		Request request = new Request(PUSH_HISTORY_EVALUATION_REQUEST, jsonObj);
		return request;
	}

	public static Request generateUserEvaluationRequest(String phoneNumber,
			long endTime, int count) {
		JSONObject jsonObj = new JSONObject();
		try {
			jsonObj.put("phone_number", phoneNumber);
			jsonObj.put("end_time", endTime);
			jsonObj.put("count", count);
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
		Request request = new Request(GET_USER_EVALUATION_REQUEST, jsonObj);
		return request;
	}
}
