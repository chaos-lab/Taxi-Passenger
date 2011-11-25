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

import com.chaos.taxi.RequestManager.Request;
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

	static final Integer CALL_TAXI_STATUS_CALLING = 0;
	static final Integer CALL_TAXI_STATUS_REJECTED = 2;
	static final Integer CALL_TAXI_STATUS_SUCCEED = 3;
	static final Integer CALL_TAXI_STATUS_SERVER_ERROR = 3;

	static boolean mStopSendRequestThread = true;
	static Thread mSendRequestThread = new SendRequestThread();

	static Context mContext = null;

	static Object mMapViewLock = new Object();
	static TaxiMapView mMapView = null;

	static Object mUserGeoPointLock = new Object();
	static GeoPoint mUserGeoPoint = null;

	static Object mCallTaxiLock = new Object();
	static TaxiOverlayItemParam mMyTaxiParam = null;
	static long mCallTaxiRequestKey = System.currentTimeMillis();
	static HashMap<Long, Integer> mCallTaxiRequestStatusMap = new HashMap<Long, Integer>();

	static DefaultHttpClient mHttpClient = new DefaultHttpClient();

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

		// handle the find taxi result
		if (httpRet.first == 200) {
			JSONObject jsonRet = httpRet.second;
			if (jsonRet == null) {
				Log.e(TAG,
						"sendFindTaxiRequest: SUCCEED! server response is error!");
				Toast.makeText(mContext,
						"sendFindTaxiRequest: server response is error!", 4000)
						.show();
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
				Toast.makeText(mContext,
						"sendFindTaxiRequest: status is not 0! " + status, 4000)
						.show();
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
		} else {
			Log.e(TAG, "sendFindTaxiRequest: HttpFail: " + httpRet.first);
			Toast.makeText(mContext,
					"sendFindTaxiRequest: HttpFail: " + httpRet.first, 4000)
					.show();
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
							if (ret.first == 200) {
								if (ret.second == null) {
									Log.e(TAG,
											"cancelCallTaxiRequest: SUCCEED! server response is error! ");
									Toast.makeText(
											mContext,
											"CallTaxiRequest: Server gives null response!",
											4000).show();
									return;
								}
								int status = -1;
								try {
									status = ret.second.getInt("status");
								} catch (JSONException e1) {
									e1.printStackTrace();
								}
								if (status == 0) {
									Log.d(TAG, "cancel taxi request succeed!");
									return;
								} else {
									// TODO: add support for other status codes
									Log.e(TAG,
											"cancelCallTaxiRequest: server return status is not 0! "
													+ status);
									Toast.makeText(
											mContext,
											"CallTaxiRequest: Server return "
													+ status, 4000).show();
									return;
								}

							} else {
								Log.e(TAG,
										"cancelCallTaxiRequest failed: HttpStatus "
												+ ret.first);
								Toast.makeText(
										mContext,
										"CallTaxiRequest: HttpStatus "
												+ ret.first, 4000).show();
							}
						}
					}
				}
			}).start();
		}
	}

	public static long callTaxi(String taxiPhoneNumber) {
		synchronized (mCallTaxiLock) {
			if (mMyTaxiParam != null) {
				return -1;
			} else {
				++mCallTaxiRequestKey;
				mCallTaxiRequestStatusMap.put(mCallTaxiRequestKey,
						CALL_TAXI_STATUS_CALLING);
				RequestManager.addCallTaxiRequest(getUserGeoPoint(),
						mCallTaxiRequestKey, taxiPhoneNumber);
				return mCallTaxiRequestKey;
			}
		}
	}

	public static long callTaxi() {
		return callTaxi(null);
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

		HttpPost httpPost = new HttpPost(HTTPSERVER + "/signout");
		Pair<Integer, JSONObject> executeRet = executeHttpRequest(httpPost,
				"Signout");
		if (executeRet.first != 200) {
			Log.e(TAG, "SIGNOUT fail, HttpStatus: " + executeRet.first);
			// Toast.makeText(mContext,
			// "SIGNOUT fail, HttpStatus: " + executeRet.first, 5000)
			// .show();
			return;
		} else {
			if (executeRet.second == null) {
				Log.e(TAG, "SIGNOUT IS FAILED! response format also error!");
				// Toast.makeText(mContext,
				// "SIGNOUT fail, Server return null response", 5000)
				// .show();
				return;
			}
			int status = -1;
			try {
				status = executeRet.second.getInt("status");
			} catch (JSONException e1) {
				e1.printStackTrace();
			}
			if (status != 0) {
				// TODO: add detail support for other status codes
				Log.e(TAG, "SIGNOUT: server return status is not 0! " + status);
				// Toast.makeText(mContext,
				// "SIGNOUT fail, Server return " + status, 5000).show();
			} else {
				Log.d(TAG, "SIGNOUT success.");
			}
		}
	}

	public static String login(String phoneNumber, String password) {
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
		if (executeRet.first != 200) {
			Log.e(TAG, "login fail, status code is " + executeRet.first);
			return "LOGIN FAILED! HttpStatus: " + executeRet.first;
		} else {
			if (executeRet.second == null) {
				return "LOGIN IS FAILED! server return null response!";
			} else {
				int status = -1;
				try {
					status = executeRet.second.getInt("status");
				} catch (JSONException e1) {
					Log.e(TAG, "Cannot parse login response: "
							+ executeRet.second.toString());
					e1.printStackTrace();
				}
				if (status != 0) {
					// TODO: add detail support for other status codes
					Log.e(TAG, "login: server return status is not 0! "
							+ status);
					return "login fail: server return status: " + status;
				}
				startSendRequestThread();
				return LOGIN_SUCCESS;
			}
		}
	}

	private static void startSendRequestThread() {
		Log.d(TAG, "startSendRequestThread!");
		if (mStopSendRequestThread) {
			mStopSendRequestThread = false;
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
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public static String register(String nickName, String phoneNumber,
			String password) {
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
		if (executeRet.first != 200) {
			Log.e(TAG, "register fail, status code is " + executeRet.first);
			return "REGISTER FAILED! HttpStatus: " + executeRet.first;
		} else {
			if (executeRet.second == null) {
				return "REGISTER IS FAILED! response format is also error!";
			}
			int status = -1;
			try {
				status = executeRet.second.getInt("status");
			} catch (JSONException e1) {
				e1.printStackTrace();
			}
			if (status != 0) {
				return "REGISTER IS FAILED! status. " + status;
			} else {
				startSendRequestThread();
				return REGISTER_SUCCESS;
			}
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

				Pair<Integer, JSONObject> httpRet = sendRequestToServer(request);
				if (httpRet == null) {
					continue;
				}
				if (httpRet.first != 200) {
					Log.e(TAG, "sendRequest: " + request.mRequestType
							+ ". status is " + httpRet.first);
				} else if (httpRet.first == 200) {
					if (httpRet.second == null) {
						Log.e(TAG, "sendRequest: " + request.mRequestType
								+ " fail! server return null response!");
						continue;
					}
					int status = -1;
					try {
						status = httpRet.second.getInt("status");
					} catch (JSONException e) {
						e.printStackTrace();
					}
					if (status != 0) {
						Log.e(TAG, "sendRequest: " + request.mRequestType
								+ " fail! status is " + status);
					}
				}
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
					int latitude = taxiLocationJson.getInt("latitude");
					int longitude = taxiLocationJson.getInt("longitude");
					mMyTaxiParam.mPoint = new GeoPoint(latitude, longitude);
				} catch (JSONException e) {
					e.printStackTrace();
				}
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
