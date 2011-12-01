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
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.chaos.taxi.RequestManager.Request;
import com.chaos.taxi.map.TaxiMapView;
import com.chaos.taxi.map.TaxiOverlayItem.TaxiOverlayItemParam;
import com.chaos.taxi.map.UserOverlayItem.UserOverlayItemParam;
import com.google.android.maps.GeoPoint;

import android.app.Activity;
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
	static final String HTTPSERVER = "http://taxi.no.de";

	static final int CALLSERVER_INTERVAL = 5000;
	static final float LOCATION_UPDATE_DISTANCE = (float) 5.0; // 5 meters
	public static final int REQUEST_TIMEOUT_THRESHOLD = 30000;

	static final Integer CALL_TAXI_STATUS_CALLING = 0;
	static final Integer CALL_TAXI_STATUS_REJECTED = 2;
	static final Integer CALL_TAXI_STATUS_SUCCEED = 3;
	static final Integer CALL_TAXI_STATUS_SERVER_ERROR = 3;

	static boolean mStopSendRequestThread = true;
	static Thread mSendRequestThread = null;

	static Context mContext = null;

	static Object mMapViewLock = new Object();
	static TaxiMapView mMapView = null;

	static Object mUserGeoPointLock = new Object();
	static GeoPoint mUserGeoPoint = null;

	static Object mCallTaxiLock = new Object();
	static TaxiOverlayItemParam mMyTaxiParam = null;
	static long mCallTaxiRequestKey = System.currentTimeMillis();
	static HashMap<Long, Integer> mCallTaxiRequestStatusMap = new HashMap<Long, Integer>();
	static HashMap<Long, String> mCallTaxiPhoneNumberMap = new HashMap<Long, String>();

	static final int MAX_TOTAL_CONNECTIONS = 100;
	static final int MAX_ROUTE_CONNECTIONS = 100;
	static final int WAIT_TIMEOUT = 10 * 1000; // 10 seconds
	static final int READ_TIMEOUT = 20 * 1000; // 20 seconds
	static final int CONNECT_TIMEOUT = 60 * 60 * 1000; // 1 hour
	static DefaultHttpClient mHttpClient = null;

	static {
		BasicHttpParams httpParams = new BasicHttpParams();
		ConnManagerParams.setMaxTotalConnections(httpParams,
				MAX_TOTAL_CONNECTIONS);
		ConnManagerParams.setTimeout(httpParams, WAIT_TIMEOUT);
		ConnPerRouteBean connPerRoute = new ConnPerRouteBean(
				MAX_ROUTE_CONNECTIONS);
		ConnManagerParams.setMaxConnectionsPerRoute(httpParams, connPerRoute);
		HttpConnectionParams.setConnectionTimeout(httpParams, CONNECT_TIMEOUT);
		HttpConnectionParams.setSoTimeout(httpParams, READ_TIMEOUT);

		SchemeRegistry registry = new SchemeRegistry();
		registry.register(new Scheme("http", PlainSocketFactory
				.getSocketFactory(), 80));
		registry.register(new Scheme("https", SSLSocketFactory
				.getSocketFactory(), 443));

		mHttpClient = new DefaultHttpClient(new ThreadSafeClientConnManager(
				httpParams, registry), httpParams);
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
				RequestManager.addLocationUpdateRequest(point);
			}
		} else {
			RequestManager.addLocationUpdateRequest(point);
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
		GeoPoint userPoint = getUserGeoPoint();
		if (userPoint == null) {
			Log.d(TAG, "sendFindTaxiRequest: no user location!");
			Toast.makeText(mContext, "Still waiting for locate...", 4000)
					.show();
			return;
		}
		Pair<Integer, JSONObject> httpRet = sendRequestToServer(RequestManager
				.generateFindTaxiRequest(userPoint));
		if (httpRet == null) {
			return;
		}
		try {
			JSONArray taxis = httpRet.second.getJSONArray("taxis");
			if (taxis != null) {
				Log.d(TAG, "has taxis! " + taxis.length());
				mMapView.removeAroundOverlay();
				for (int i = 0; i < taxis.length(); ++i) {
					JSONObject taxiInfo = taxis.getJSONObject(i);
					GeoPoint point = new GeoPoint(
							(int) taxiInfo.getDouble("latitude") * 1000000,
							(int) taxiInfo.getDouble("longitude") * 1000000);
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

	public static void cancelCallTaxiRequest() {
		Request request = null;
		synchronized (mCallTaxiLock) {
			mMyTaxiParam = null;
			if (!RequestManager.removeRequest(RequestManager.CALL_TAXI_REQUEST))
				request = RequestManager
						.generateCancelCallTaxiRequest(mCallTaxiRequestKey);
			++mCallTaxiRequestKey;
		}
		if (request != null) {
			final Request req = request;
			new Thread(new Runnable() {
				public void run() {
					while (true) {
						Pair<Integer, JSONObject> ret = sendRequestToServer(req);
						if (ret != null) {
							return;
						} else {
							Log.d(TAG, "cancel request fail!");
							return;
						}
					}
				}
			}).start();
		}
	}

	public static void callTaxi(String taxiPhoneNumber) {
		long requestKey = 1;
		synchronized (mCallTaxiLock) {
			if (mMyTaxiParam != null) {
				requestKey = -1;
			} else {
				++mCallTaxiRequestKey;
				mCallTaxiRequestStatusMap.put(mCallTaxiRequestKey,
						CALL_TAXI_STATUS_CALLING);
				RequestManager.addCallTaxiRequest(getUserGeoPoint(),
						mCallTaxiRequestKey, taxiPhoneNumber);
				mCallTaxiPhoneNumberMap.put(mCallTaxiRequestKey,
						taxiPhoneNumber);
				requestKey = mCallTaxiRequestKey;
			}
		}
		if (requestKey != -1) {
			Intent intent = new Intent(mContext, WaitTaxiActivity.class);
			intent.putExtra("WaitTaxiTime",
					RequestProcessor.REQUEST_TIMEOUT_THRESHOLD / 1000);
			intent.putExtra("RequestKey", requestKey);
			((Activity) mContext).startActivityForResult(intent,
					TaxiActivity.CALL_TAXI_REQUEST_CODE);
		} else {
			AlertDialog dialog = new AlertDialog.Builder(mContext)
					.setIcon(android.R.drawable.ic_dialog_info)
					.setTitle("CallTaxiFail: ")
					.setMessage("Already have a taxi")
					.setPositiveButton("Locate",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									RequestProcessor.sendLocateTaxiRequest();
								}
							}).setNegativeButton("OK", null).create();
			dialog.show();
		}
	}

	public static void callTaxi() {
		callTaxi(null);
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

	public static int popCallTaxiStatus(long requestKey) {
		int status = CALL_TAXI_STATUS_CALLING;
		synchronized (mCallTaxiLock) {
			if (mCallTaxiRequestStatusMap.containsKey(requestKey)) {
				status = mCallTaxiRequestStatusMap.get(requestKey);
				mCallTaxiRequestStatusMap.remove(requestKey);
			}
		}
		return status;
	}

	public static void signout() {
		stopSendRequestThread();

		HttpPost httpPost = new HttpPost(
				TaxiUtil.getServerAddressByRequestType(RequestManager.SIGNOUT_REQUEST));
		executeHttpRequest(httpPost, "Signout");
	}

	public static String login(String phoneNumber, String password) {
		HttpPost httpPost = new HttpPost(
				TaxiUtil.getServerAddressByRequestType(RequestManager.SIGNIN_REQUEST));
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
		if (executeRet != null) {
			startSendRequestThread();
			return LOGIN_SUCCESS;
		} else {
			return "LOGIN_FAIL";
		}
	}

	private static void startSendRequestThread() {
		Log.d(TAG, "startSendRequestThread!");
		if (mStopSendRequestThread) {
			mStopSendRequestThread = false;
			mSendRequestThread = new SendRequestThread();
			mSendRequestThread.start();
		}
	}

	private static void stopSendRequestThread() {
		Log.d(TAG, "stopSendRequestThread!");
		if (!mStopSendRequestThread) {
			mStopSendRequestThread = true;
			try {
				if (mSendRequestThread != null) {
					mSendRequestThread.join();
					mSendRequestThread = null;
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public static String register(String nickName, String phoneNumber,
			String password) {
		HttpPost httpPost = new HttpPost(
				TaxiUtil.getServerAddressByRequestType(RequestManager.REGISTER_REQUEST));
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
		if (executeRet != null) {
			return REGISTER_SUCCESS;
		} else {
			return "REGISTER_FAIL";
		}
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
					JSONObject retJson = new JSONObject(str);
					int status = retJson.optInt("status", -1);
					if (status == 1) {
						// need relogin
						// TODO: start relogin activity
						return null;
					} else if (status != 0) {
						// TODO: other status codes
						return null;
					}
					return new Pair<Integer, JSONObject>(statusCode,
							new JSONObject(str));
				}
			} else {
				Log.w(TAG, "HttpFail. StatusCode is " + statusCode);
				Toast.makeText(mContext, "HTTP Fail! StatusCode: ", statusCode)
						.show();
				return null;
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
		if (request == null) {
			Log.e(TAG, "sendRequestToServer: request is null!");
			return null;
		}
		HttpUriRequest httpUriRequest = TaxiUtil
				.generateHttpUriRequest(request);
		if (httpUriRequest == null) {
			Log.wtf(TAG, "no HttpUriRequest for request: "
					+ request.mRequestJson.toString());
			return null;
		}

		return executeHttpRequest(httpUriRequest, request.mRequestType);
	}

	static class SendRequestThread extends Thread {
		public SendRequestThread() {
			super();
		}

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

				Request request = RequestManager.popRequest();
				if (request == null) {
					sendRefreshRequestToServer();
					try {
						Thread.sleep(CALLSERVER_INTERVAL);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					continue;
				}

				sendRequestToServer(request);
			}
		}
	};

	private static void sendRefreshRequestToServer() {
		Request request = new Request(RequestManager.REFRESH_REQUEST, null);

		Pair<Integer, JSONObject> httpRet = sendRequestToServer(request);
		if (httpRet == null) {
			return;
		}
		Log.d(TAG, "refresh request status is " + httpRet.first);
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

		JSONArray messageJsonArray = jsonRet.optJSONArray("messages");
		if (messageJsonArray == null) {
			Log.d(TAG, "no message in refresh response!");
			return;
		}

		for (int i = 0; i < messageJsonArray.length(); ++i) {
			JSONObject messageJson = messageJsonArray.optJSONObject(i);
			if (messageJson == null) {
				Log.e(TAG, "cannot optJSONObject at " + i);
				continue;
			}
			String type = messageJson.optString("type");
			if (type != null) {
				if (type.equals(RequestManager.CALL_TAXI_RESPONSE)) {
					handleCallTaxiReplyJson(messageJson);
				} else if (type.equals(RequestManager.LOCATION_UPDATE_REQUEST)) {
					handleTaxiLocationUpdate(messageJson);
				} else {
					Log.e(TAG, "type is not recognized: " + type);
				}
			} else {
				Log.e(TAG, "type is null in message: " + messageJson.toString());
			}
		}
	}

	private static void handleCallTaxiComplete(JSONObject callTaxiCompleteJson) {
		synchronized (mCallTaxiLock) {
			if (mMyTaxiParam == null) {
				Log.w(TAG, "handleCallTaxiComplete: do not have a taxi!");
				return;
			} else {
				Intent intent = new Intent(mContext,
						CallTaxiCompleteActivity.class);
				intent.putExtra("TaxiParam", mMyTaxiParam);
				mMyTaxiParam = null;
				mContext.startActivity(intent);
			}
		}
	}

	private static void handleTaxiLocationUpdate(JSONObject taxiLocationJson) {
		synchronized (mCallTaxiLock) {
			if (mMyTaxiParam == null) {
				Log.w(TAG, "handleTaxiLocationUpdate: do not have a taxi!");
				return;
			} else {
				try {
					int latitude = (int) (taxiLocationJson
							.getDouble("latitude") * 1000000);
					int longitude = (int) (taxiLocationJson
							.getDouble("longitude") * 1000000);
					mMyTaxiParam.mPoint = new GeoPoint(latitude, longitude);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static void handleCallTaxiReplyJson(JSONObject callTaxiReplyJson) {
		Log.d(TAG, "handle call taxi reply!");

		boolean accept = callTaxiReplyJson.optBoolean("accept");
		if (!accept) {
			Log.d(TAG, "call taxi request is rejected!");
			synchronized (mMapViewLock) {
				mCallTaxiRequestStatusMap.put(mCallTaxiRequestKey,
						CALL_TAXI_STATUS_REJECTED);
			}
			return;
		}

		int callTaxiRequestKey = callTaxiReplyJson.optInt("key", -1);
		synchronized (mCallTaxiLock) {
			if (callTaxiRequestKey < mCallTaxiRequestKey) {
				Log.w(TAG, "ignore call taxi response: " + callTaxiRequestKey
						+ " currentNumber is " + mCallTaxiRequestKey);
			} else {
				String taxiPhoneNumber = mCallTaxiPhoneNumberMap
						.get(callTaxiRequestKey);
				if (taxiPhoneNumber == null) {
					taxiPhoneNumber = callTaxiReplyJson.optString("driver");
				}
				if (taxiPhoneNumber == null) {
					Log.e(TAG, "no driver number in reply!");
					return;
				}
				synchronized (mMapViewLock) {
					mMyTaxiParam = mMapView.findInAroundTaxi(taxiPhoneNumber);
					if (mMyTaxiParam == null) {
						Log.wtf(TAG, "mMyTaxiParam should not be null!");
						return;
					}
					mCallTaxiRequestStatusMap.put(mCallTaxiRequestKey,
							CALL_TAXI_STATUS_SUCCEED);
				}
			}
		}
	}
}
