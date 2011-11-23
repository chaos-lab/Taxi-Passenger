package com.chaos.taxi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.chaos.taxi.map.TaxiMapView;
import com.chaos.taxi.map.TaxiOverlayItem.TaxiOverlayItemParam;
import com.chaos.taxi.map.UserOverlayItem.UserOverlayItemParam;
import com.google.android.maps.GeoPoint;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.location.Location;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

public class RequestProcessor {
	private static final String TAG = "RequestProcessor";
	static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
	static final String REGISTER_SUCCESS = "REGISTER_SUCESS";
	static final String HTTPSERVER = "http://taxi.no.de/passenger";

	static final int CALLSERVER_INTERVAL = 5000;
	static final float LOCATION_UPDATE_DISTANCE = (float) 5.0; // 5 meters
	static final int REQUEST_TIMEOUT_THRESHOLD = 30000;

	static final String CANCEL_CALL_TAXI_REQUEST = "cancel-call-taxi";
	static final String CALL_TAXI_REQUEST = "call-taxi";
	static final String CALL_TAXI_RESPONSE = "call-taxi-reply";
	static final String CALL_TAXI_COMPLETE = "call-taxi-complete";
	static final String FIND_TAXI_REQUEST = "FindTaxi";
	static final String LOCATION_UPDATE_REQUEST = "location-update";
	static final String REFRESH_REQUEST = "RefreshRequest";

	static final Integer CALL_TAXI_STATUS_CALLING = 0;
	static final Integer CALL_TAXI_STATUS_REJECTTED = 2;
	static final Integer CALL_TAXI_STATUS_SUCCEED = 3;

	static boolean mStopSendRequestThread = false;

	static ArrayList<Request> mRequests = new ArrayList<Request>();
	static Context mContext = null;

	static Object mMapViewLock = new Object();
	static TaxiMapView mMapView = null;
	static HashMap<Long, Integer> mCallTaxiRequestStatusMap = new HashMap<Long, Integer>();

	static Object mUserGeoPointLock = new Object();
	static GeoPoint mUserGeoPoint = null;
	static Object mCallTaxiLock = new Object();
	static TaxiOverlayItemParam mMyTaxiParam = null;
	static long mCallTaxiRequestId = System.currentTimeMillis();

	static DefaultHttpClient mHttpClient = new DefaultHttpClient();
	static Thread mSendRequestThread = null;

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

	public static void initRequestProcessor(Context context, TaxiMapView mapView) {
		mContext = context;
		mMapView = mapView;
	}

	public static void setUserGeoPoint(GeoPoint point) {
		if (point == null) {
			Log.d(TAG, "setUserGeoPoint: point is null!");
			return;
		}
		GeoPoint lastPoint = null;
		synchronized (mUserGeoPointLock) {
			lastPoint = mUserGeoPoint;
			mUserGeoPoint = point;
		}
		if (lastPoint != null) {
			Location last = TaxiUtil.geoPointToLocation(lastPoint);
			Location current = TaxiUtil.geoPointToLocation(point);
			if (last.distanceTo(current) >= LOCATION_UPDATE_DISTANCE) {
				addRequest(generateUserLocationUpdateRequest(point));
			}
		} else {
			addRequest(generateUserLocationUpdateRequest(point));
		}
	}

	public static GeoPoint getUserGeoPoint() {
		synchronized (mUserGeoPointLock) {
			return mUserGeoPoint;
		}
	}

	public static TaxiOverlayItemParam getMyTaxiParam() {
		synchronized (mCallTaxiLock) {
			return mMyTaxiParam;
		}
	}

	private static void animateTo(GeoPoint point) {
		synchronized (mMapViewLock) {
			mMapView.getController().animateTo(point);
		}
	}

	private static void addRequest(Request request, Object data) {
		if (request == null) {
			Log.d(TAG, "request is null!");
			return;
		}
		try {
			request.mRequestJson.put("request_type", request.mRequestType);
		} catch (JSONException e) {
			Log.e(TAG, "cannot put request_type into request! "
					+ request.mRequestType);
			e.printStackTrace();
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

	private static void addRequest(Request request) {
		addRequest(request, null);
	}

	private static boolean removeRequest(String requestType) {
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

	private static Request getRequest() {
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

	public static void sendLocateUserRequest() {
		GeoPoint point = getUserGeoPoint();
		if (point != null) {
			animateTo(point);
			synchronized (mMapViewLock) {
				mMapView.showUserOverlay(new UserOverlayItemParam(point));
			}
		} else {
			Toast.makeText(mContext, "Waiting for locate", 4000).show();
		}
	}

	public static void sendLocateTaxiRequest() {
		Log.d(TAG, "sendLocateTaxiRequest");
		TaxiOverlayItemParam param = getMyTaxiParam();
		if (param != null && param.mPoint != null) {
			animateTo(param.mPoint);
			synchronized (mMapViewLock) {
				mMapView.removeAroundOverlay();
				mMapView.showMyTaxiOverlay(param);
			}
		} else {
			Toast.makeText(mContext, "Waiting for taxi locate", 4000).show();
		}
	}

	public static void sendFindTaxiRequest() {
		Pair<Integer, JSONObject> httpRet = sendRequestToServer(generateFindTaxiRequest());
		if (httpRet == null) {
			return;
		}

		// handle the find taxi result
		if (httpRet.first == 200) {
			JSONObject jsonRet = httpRet.second;
			if (jsonRet == null) {
				Log.e(TAG,
						"sendFindTaxiRequest: SUCCEED! server response is error!");
				return;
			}

			int status = -1;
			try {
				status = jsonRet.getInt("status");
			} catch (JSONException e1) {
				e1.printStackTrace();
			}
			if (status != 0) {
				// TODO: add support for other status codes
				Log.e(TAG,
						"sendFindTaxiRequest: server return status is not 0! "
								+ status);
				return;
			}
			try {
				JSONArray taxis = jsonRet.getJSONArray("taxis");
				if (taxis != null) {
					for (int i = 0; i < taxis.length(); ++i) {
						JSONObject taxiInfo = taxis.getJSONObject(i);
						GeoPoint point = new GeoPoint(
								taxiInfo.getInt("latitude"),
								taxiInfo.getInt("longitude"));
						String carNumber = taxiInfo.getString("car_number");
						String phoneNumber = taxiInfo.getString("phone_number");
						String nickName = taxiInfo.getString("nickname");
						TaxiOverlayItemParam param = new TaxiOverlayItemParam(
								point, carNumber, phoneNumber, nickName);
						synchronized (mMapViewLock) {
							mMapView.addAroundTaxiOverlay(param);
						}
					}
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}

	public static void cancelCallTaxiRequest() {
		return;
		/*
		 * Request request = null; synchronized (mCallTaxiLock) { mMyTaxiParam =
		 * null; if (!removeRequest(CALL_TAXI_REQUEST)) request =
		 * generateCancelCallTaxiRequest(mCallTaxiRequestId);
		 * mCallTaxiRequestStatusMap.remove(mCallTaxiRequestId);
		 * ++mCallTaxiRequestId; } if (request != null) { final Request req =
		 * request; new Thread(new Runnable() { public void run() { while (true)
		 * { Pair<Integer, JSONObject> ret = sendRequestToServer(req); if (ret
		 * != null) { if (ret.first == 200) { if (ret.second == null) {
		 * Log.e(TAG,
		 * "cancelCallTaxiRequest: SUCCEED! server response is error! ");
		 * return; } int status = -1; try { status =
		 * ret.second.getInt("status"); } catch (JSONException e1) {
		 * e1.printStackTrace(); } if (status == 0) { Log.d(TAG,
		 * "cancel taxi request succeed!"); return; } else { // TODO: add
		 * support for other status codes Log.e(TAG,
		 * "cancelCallTaxiRequest: server return status is not 0! " + status);
		 * return; }
		 * 
		 * } else { String message = null; try { message =
		 * ret.second.getString("message") + " status " + ret.first; } catch
		 * (JSONException e) { e.printStackTrace(); message =
		 * "CancelCallTaxi IS FAILED! status " + ret.first; } Log.d(TAG,
		 * ret.first + " " + message); } } } } }).start(); }
		 */
	}

	public static long sendCallTaxiRequest(String taxiPhoneNumber) {
		synchronized (mCallTaxiLock) {
			if (mMyTaxiParam != null) {
				return -1;
			} else {
				++mCallTaxiRequestId;
				mCallTaxiRequestStatusMap.put(mCallTaxiRequestId,
						CALL_TAXI_STATUS_CALLING);
				addRequest(generateCallTaxiRequest(mCallTaxiRequestId,
						taxiPhoneNumber));
				return mCallTaxiRequestId;
			}
		}
	}

	public static long sendCallTaxiRequest() {
		return sendCallTaxiRequest(null);
	}

	public static void showCallTaxiSucceedDialog() {
		synchronized (mCallTaxiLock) {
			if (mMyTaxiParam != null) {
				AlertDialog dialog = new AlertDialog.Builder(mContext)
						.setIcon(android.R.drawable.ic_dialog_info)
						.setTitle("CallTaxiSucceed: ")
						.setMessage(
								"CarNumber is " + mMyTaxiParam.mCarNumber
										+ "\nPhoneNumber is "
										+ mMyTaxiParam.mPhoneNumber
										+ "\nNickName is "
										+ mMyTaxiParam.mNickName)
						.setPositiveButton("Locate", new OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								sendLocateTaxiRequest();
							}
						}).setNegativeButton("OK", null).create();
				dialog.show();
			} else {
				Log.wtf(TAG, "taxi should not be null!");
			}
		}
	}

	public static int getCallTaxiStatus(long requestId) {
		synchronized (mCallTaxiLock) {
			return mCallTaxiRequestStatusMap.get(requestId);
		}
	}

	public static void logout() {
		mStopSendRequestThread = true;
		try {
			if (mSendRequestThread != null) {
				mSendRequestThread.join();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static String login(String phoneNumber, String password) {
		TaxiActivity.sPhoneNumber = phoneNumber;

		HttpPost httpPost = new HttpPost(HTTPSERVER + "/signin");
		JSONObject signinJson = new JSONObject();
		try {
			signinJson.put("phone_number", phoneNumber);
			signinJson.put("password", password);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		Log.d(TAG, "signin json str is " + signinJson.toString());
		TaxiUtil.setHttpEntity(httpPost, signinJson.toString());

		Pair<Integer, JSONObject> executeRet = executeHttpRequest(httpPost,
				"Login");
		String message = null;
		if (executeRet.first != 200) {
			Log.e(TAG, "login fail, status code is " + executeRet.first);
			if (message == null || message.equals("")) {
				message = "LOGIN FAILED! " + executeRet.first;
			}
		} else {
			if (executeRet.second == null) {
				Log.e(TAG, "LOGIN IS FAILED! response format also error!");
			}
			int status = -1;
			try {
				status = executeRet.second.getInt("status");
			} catch (JSONException e1) {
				e1.printStackTrace();
			}
			if (status != 0) {
				// TODO: add detail support for other status codes
				Log.e(TAG, "login: server return status is not 0! " + status);
				try {
					message = executeRet.second.getString("message") + " "
							+ status;
				} catch (JSONException e) {
					e.printStackTrace();
					message = "LOGIN IS FAILED! response format also error! "
							+ status;
				}
				return message;
			}
			message = LOGIN_SUCCESS;
			Log.d(TAG, "start mSendRequestThread!");
			mSendRequestThread = new Thread(mTask);
			mStopSendRequestThread = false;
			mSendRequestThread.start();
		}
		// return LOGIN_SUCCESS;
		return message;
	}

	public static String register(String nickName, String phoneNumber,
			String password) {
		TaxiActivity.sNickName = nickName;
		TaxiActivity.sPhoneNumber = phoneNumber;

		HttpPost httpPost = new HttpPost(HTTPSERVER + "/signup");
		JSONObject registerJson = new JSONObject();
		try {
			registerJson.put("nickname", nickName);
			registerJson.put("phone_number", phoneNumber);
			registerJson.put("password", password);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		Log.d(TAG, "register json str is " + registerJson.toString());
		TaxiUtil.setHttpEntity(httpPost, registerJson.toString());

		Pair<Integer, JSONObject> executeRet = executeHttpRequest(httpPost,
				"Register");
		String message = null;
		if (executeRet.first != 200) {
			Log.e(TAG, "register fail, status code is " + executeRet.first);
			if (message == null || message.equals("")) {
				message = "REGISTER FAILED! " + executeRet.first;
			}
		} else {
			if (executeRet.second == null) {
				Log.e(TAG, "REGISTER IS FAILED! response format is also error!");
			}
			int status = -1;
			try {
				status = executeRet.second.getInt("status");
			} catch (JSONException e1) {
				e1.printStackTrace();
			}
			if (status == 0) {
				return REGISTER_SUCCESS;
			}
			try {
				message = executeRet.second.getString("message") + " " + status;
			} catch (JSONException e) {
				e.printStackTrace();
				message = "REGISTER IS FAILED! JSONException. " + status;
			}
		}
		return message;
	}

	private static void resendRequest(Request request) {
		// sleep 1 second before resend
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if (request.mRequestType.equals(CALL_TAXI_REQUEST)) {
			synchronized (mCallTaxiLock) {
				if (request.getLongData() == mCallTaxiRequestId) {
					addRequest(request);
				}
			}
		} else if (request.mRequestType.equals(LOCATION_UPDATE_REQUEST)) {
			addRequest(generateUserLocationUpdateRequest(getUserGeoPoint()));
		}
	}

	private static Request generateFindTaxiRequest() {
		GeoPoint userPoint = getUserGeoPoint();
		if (userPoint == null) {
			Log.w(TAG,
					"user geoPoint not updated! cannot send FindTaxi request!");
			return null;
		}

		JSONObject jsonObj = new JSONObject();
		try {
			jsonObj.put("latitude", userPoint.getLatitudeE6());
			jsonObj.put("Longitude", userPoint.getLongitudeE6());
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
		return new Request(FIND_TAXI_REQUEST, jsonObj);
	}

	private static Request generateCancelCallTaxiRequest(long callTaxiRequestId) {
		JSONObject jsonObj = new JSONObject();
		try {
			jsonObj.put("type", CANCEL_CALL_TAXI_REQUEST);
			jsonObj.put("from", TaxiActivity.sPhoneNumber);
			jsonObj.put("request_id", callTaxiRequestId);
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
		Request request = new Request(CANCEL_CALL_TAXI_REQUEST, jsonObj);
		request.mData = (Long) callTaxiRequestId;
		return request;
	}

	private static Request generateCallTaxiRequest(long callTaxiRequestId,
			String taxiPhoneNumber) {
		Log.d(TAG, "generateCallTaxiRequest, callTaxiNumber: "
				+ callTaxiRequestId + " taxiPhoneNumber: " + taxiPhoneNumber);
		JSONObject jsonObj = new JSONObject();
		GeoPoint userPoint = getUserGeoPoint();
		taxiPhoneNumber = "13851403984";
		try {
			jsonObj.put("type", CALL_TAXI_REQUEST);
			jsonObj.put("from", TaxiActivity.sPhoneNumber);
			jsonObj.put("number", callTaxiRequestId);
			if (taxiPhoneNumber != null) {
				jsonObj.put("to", taxiPhoneNumber);
			}
			JSONObject data = new JSONObject();
			JSONObject temp = new JSONObject();
			if (userPoint != null) {
				temp.put("latitude", userPoint.getLatitudeE6());
				temp.put("Longitude", userPoint.getLongitudeE6());
				temp.put("phone_number", "13913391280");
				temp.put("nickname", "souriki");
			}
			data.put("passenger", temp);
			jsonObj.put("data", data);
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
		Request request = new Request(CALL_TAXI_REQUEST, jsonObj);
		request.mData = (Long) callTaxiRequestId;
		return request;
	}

	private static Request generateUserLocationUpdateRequest(GeoPoint point) {
		if (point == null) {
			Log.i(TAG, "null point do not need update!");
			return null;
		}
		JSONObject jsonObj = new JSONObject();
		try {
			jsonObj.put("latitude", point.getLatitudeE6());
			jsonObj.put("Longitude", point.getLongitudeE6());
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
		return new Request(LOCATION_UPDATE_REQUEST, jsonObj);
	}

	private static Pair<Integer, JSONObject> executeHttpRequest(
			HttpUriRequest httpUriRequest, String requestType) {
		Integer statusCode = 0;
		String exceptionMsg = null;

		try {
			HttpResponse httpResponse = null;
			httpUriRequest.setHeader("Content-Type",
					"application/x-www-form-urlencoded");
			synchronized (mHttpClient) {
				httpResponse = mHttpClient.execute(httpUriRequest);
			}
			statusCode = httpResponse.getStatusLine().getStatusCode();
			if (statusCode == 200) {
				BufferedReader bufferedReader = new BufferedReader(
						new InputStreamReader(httpResponse.getEntity()
								.getContent()));
				StringBuffer stringBuffer = new StringBuffer();
				for (String line = bufferedReader.readLine(); line != null; line = bufferedReader
						.readLine()) {
					stringBuffer.append(line);
				}

				String str = stringBuffer.toString();
				Log.d(TAG, requestType + " response is " + str);
				if (str == null) {
					return new Pair<Integer, JSONObject>(statusCode, null);
				} else {
					return new Pair<Integer, JSONObject>(statusCode,
							new JSONObject(str));
				}
			} else {
				Log.w(TAG, "HttpFail. StatusCode is " + statusCode);
				return new Pair<Integer, JSONObject>(statusCode, null);
			}
		} catch (ClientProtocolException e) {
			exceptionMsg = "ClientProtocolException: " + e.getMessage();
			e.printStackTrace();
		} catch (IOException e) {
			exceptionMsg = "IOException: " + e.getMessage();
			e.printStackTrace();
		} catch (JSONException e) {
			exceptionMsg = "JSONException: " + e.getMessage();
			e.printStackTrace();
		}

		JSONObject jsonRet = new JSONObject();
		try {
			jsonRet.put("message", "Cannot conenct to server!" + exceptionMsg);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return new Pair<Integer, JSONObject>(-1, jsonRet);
	}

	private static Pair<Integer, JSONObject> sendRequestToServer(Request request) {
		HttpUriRequest httpUriRequest = TaxiUtil
				.generateHttpUriRequest(request);
		if (httpUriRequest == null) {
			Log.wtf(TAG, "Cannot generate HttpUriRequest for request: "
					+ request.mRequestJson.toString());
			return null;
		}

		return executeHttpRequest(httpUriRequest, request.mRequestType);
	}

	static Runnable mTask = new Runnable() {
		public void run() {
			int count = 0;
			while (true) {
				if (mStopSendRequestThread) {
					Log.d(TAG, "stop send request thread!");
					return;
				} else {
					++count;
					Log.d(TAG, "send request count: " + count);
				}

				Request request = getRequest();
				if (request == null) {
					sendRefreshRequestToServer();
					try {
						Thread.sleep(CALLSERVER_INTERVAL);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					continue;
				}

				Pair<Integer, JSONObject> httpRet = sendRequestToServer(request);
				if (httpRet == null) {
					continue;
				}
				if (httpRet.first != 200) {
					Log.d(TAG, "sendRequest: " + request.mRequestType
							+ ". status is " + httpRet.first);
					resendRequest(request);
				} else if (httpRet.first == 200) {
					if (httpRet.second == null) {
						Log.e(TAG, "sendRequest: " + request.mRequestType
								+ " fail! response format also error!");
						continue;
					}
					int status = -1;
					try {
						status = httpRet.second.getInt("status");
					} catch (JSONException e) {
						e.printStackTrace();
					}
					if (status != 0) {
						// TODO: add support for other status codes
						Log.e(TAG, "sendRequest: " + request.mRequestType
								+ " fail! status is " + status);
						resendRequest(request);
					}
				}
			}
		}
	};

	private static void sendRefreshRequestToServer() {
		Request request = new Request(REFRESH_REQUEST, null);

		Pair<Integer, JSONObject> httpRet = sendRequestToServer(request);
		if (httpRet == null) {
			return;
		}
		Log.d(TAG, "refresh request status is " + httpRet.first);
		if (httpRet.first != 200) {
			return;
		}
		// handle the result
		handleRefreshResponseJson(httpRet.second);
	}

	private static void handleRefreshResponseJson(JSONObject jsonRet) {
		if (jsonRet == null) {
			return;
		}
		int status = -1;
		try {
			status = jsonRet.getInt("status");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		if (status != 0) {
			// TODO: add support for other status codes
			Log.e(TAG, "handleRefreshResponseJson: status is " + status);
			return;
		}

		JSONArray messageArrayJson = jsonRet.optJSONArray("messages");
		if (messageArrayJson == null) {
			Log.d(TAG, "no message in refresh response!");
			return;
		}

		handleJsonMessageArray(messageArrayJson);
	}

	private static void handleJsonMessageArray(JSONArray messageArrayJson) {
		for (int i = 0; i < messageArrayJson.length(); ++i) {
			JSONObject jsonObject = messageArrayJson.optJSONObject(0);
			String type = jsonObject.optString("type");
			if (type != null && type.equals(CALL_TAXI_RESPONSE)) {
				handleCallTaxiReplyJson(jsonObject);
			}
		}
	}

	private static void handleCallTaxiReplyJson(JSONObject callTaxiReplyJson) {
		Log.d(TAG, "handle call taxi reply!");
		String taxiPhoneNumber = callTaxiReplyJson.optString("from");
		if (taxiPhoneNumber == null) {
			Log.wtf(TAG, "taxiPhoneNumber is null in call taxi reply!!!");
			return;
		}
		Log.i(TAG, "taxiPhoneNumber is " + taxiPhoneNumber);
		JSONObject data = callTaxiReplyJson.optJSONObject("data");
		if (data == null) {
			Log.e(TAG, "data in call taxi reply should not be null!");
			return;
		}
		boolean accept = data.optBoolean("accept");
		if (!accept) {
			Log.d(TAG, "call taxi request is rejected!");
			synchronized (mMapViewLock) {
				mCallTaxiRequestStatusMap.put(mCallTaxiRequestId,
						CALL_TAXI_STATUS_REJECTTED);
			}
			return;
		}

		int callTaxiRequestId = callTaxiReplyJson.optInt("request_id", -1);
		synchronized (mCallTaxiLock) {
			if (callTaxiRequestId < mCallTaxiRequestId) {
				Log.w(TAG, "current no ignore call taxi response: "
						+ callTaxiRequestId + " currentNumber is "
						+ mCallTaxiRequestId);
			}
			synchronized (mMapViewLock) {
				mMyTaxiParam = mMapView.findInAroundTaxi(taxiPhoneNumber);
				if (mMyTaxiParam == null) {
					Log.e(TAG, "mMyTaxiParam should not be null!");
					mMyTaxiParam = new TaxiOverlayItemParam(new GeoPoint(
							32042962, 118784149), "123", "13851403984", "test");
				}
				mCallTaxiRequestStatusMap.put(mCallTaxiRequestId,
						CALL_TAXI_STATUS_SUCCEED);
			}

		}
	}
}
