package com.chaos.taxi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class WaitTaxiActivity extends Activity {
	private static final String TAG = "WaitTaxiActivity";
	static final int SUCCEED_WAIT = 1;
	static final int CANCEL_WAIT = 2;
	static final int REJECT_WAIT = 3;
	static final int DRIVER_UNAVAILABLE = 4;
	static final int SERVER_ERROR = 5;

	static final int SET_RESEND_VIEW = 100;
	static final int SET_WAITTIME_TEXT = 200;

	static final String RET_CODE = "RET_CODE";

	Button waitTaxiBtn = null;
	TextView waitTaxiTimeTv = null;
	long mRequestKey = -1;

	Button keepWaitBtn = null;
	Button cancelWaitBtn = null;

	int mWaitTaxiTime = 0;
	Integer mLeftWaitTaxiTime = 0;
	String mTaxiPhoneNumber = null;

	// Thread mSetWaitTimeThread = null;

	private void setLeftWaitTaxiTime(int val) {
		synchronized (mLeftWaitTaxiTime) {
			mLeftWaitTaxiTime = val;
		}
	}

	private void decreaseSetLeftWaitTaxiTime() {
		synchronized (mLeftWaitTaxiTime) {
			--mLeftWaitTaxiTime;
		}
	}

	private int getLeftWaitTaxiTime() {
		synchronized (mLeftWaitTaxiTime) {
			return mLeftWaitTaxiTime;
		}
	}

	private void setWaitTaxiView() {
		setContentView(R.layout.wait_taxi);
		waitTaxiBtn = (Button) findViewById(R.id.wait_taxi_btn);
		waitTaxiTimeTv = (TextView) findViewById(R.id.wait_taxi_time_tv);

		setLeftWaitTaxiTime(mWaitTaxiTime);
		Log.d(TAG, "mWaitTaxiTime is " + mWaitTaxiTime);

		waitTaxiTimeTv.setText("" + mWaitTaxiTime);
		waitTaxiBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent retIntent = new Intent();
				retIntent.putExtra(RET_CODE, CANCEL_WAIT);
				setResult(0, retIntent);
				WaitTaxiActivity.this.finish();
			}
		});

		new Thread(mTask).start();
	}

	class MyHandler extends Handler {
		public MyHandler() {
			super(Looper.getMainLooper());
		}

		public void handleMessage(Message msg) {
			switch (msg.what) {
			case SET_RESEND_VIEW:
				setResendCallTaxiView();
				break;
			case SET_WAITTIME_TEXT:
				if (waitTaxiTimeTv != null) {
					waitTaxiTimeTv.setText("" + getLeftWaitTaxiTime());
				}
			}
		};
	}

	private Handler mHandler = new MyHandler();

	private void setResendCallTaxiView() {
		WaitTaxiActivity.this.setContentView(R.layout.resend_call_taxi);
		keepWaitBtn = (Button) findViewById(R.id.keep_wait_btn);
		cancelWaitBtn = (Button) findViewById(R.id.cancel_call_taxi);

		keepWaitBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				setLeftWaitTaxiTime(mWaitTaxiTime);
				setWaitTaxiView();
			}
		});

		cancelWaitBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent retIntent = new Intent();
				retIntent.putExtra(RET_CODE, CANCEL_WAIT);
				setResult(0, retIntent);
				WaitTaxiActivity.this.finish();
			}
		});
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");

		Intent intent = getIntent();
		mWaitTaxiTime = intent.getIntExtra("WaitTaxiTIme",
				RequestProcessor.REQUEST_TIMEOUT_THRESHOLD / 1000);
		mRequestKey = intent.getLongExtra("RequestKey", -1);
		Log.d(TAG, "mRequestKey is " + mRequestKey);
		mTaxiPhoneNumber = intent.getStringExtra("TaxiPhoneNumber");

		setWaitTaxiView();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	Runnable mTask = new Runnable() {
		public void run() {
			Log.d(TAG, "enter wait taxi thread");
			while (true) {
				Message msg = mHandler.obtainMessage();
				msg.what = SET_WAITTIME_TEXT;
				mHandler.sendMessage(msg);

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				int status = RequestProcessor.getCallTaxiStatus(mRequestKey);
				if (status == RequestProcessor.CALL_TAXI_STATUS_SUCCEED) {
					Log.d(TAG, "status is CALL_TAXI_STATUS_SUCCEED!");
					Intent retIntent = new Intent();
					retIntent.putExtra(RET_CODE, SUCCEED_WAIT);
					setResult(0, retIntent);
					WaitTaxiActivity.this.finish();
				} else if (status == RequestProcessor.CALL_TAXI_STATUS_REJECTED) {
					Log.d(TAG, "status is CALL_TAXI_STATUS_REJECTED!");
					Intent retIntent = new Intent();
					retIntent.putExtra(RET_CODE, REJECT_WAIT);
					setResult(0, retIntent);
					WaitTaxiActivity.this.finish();
				} else if (status == RequestProcessor.CALL_TAXI_DRIVER_UNAVAILABLE) {
					Log.d(TAG, "status is CALL_TAXI_DRIVER_UNAVAILABLE!");
					Intent retIntent = new Intent();
					retIntent.putExtra(RET_CODE, DRIVER_UNAVAILABLE);
					setResult(0, retIntent);
					WaitTaxiActivity.this.finish();
				} else if (status == RequestProcessor.CALL_TAXI_STATUS_SERVER_ERROR) {
					Log.d(TAG, "status is CALL_TAXI_STATUS_SERVER_ERROR!");
					Intent retIntent = new Intent();
					retIntent.putExtra(RET_CODE, SERVER_ERROR);
					retIntent.putExtra("TaxiPhoneNumber", mTaxiPhoneNumber);
					setResult(0, retIntent);
					WaitTaxiActivity.this.finish();
				} else if (status == RequestProcessor.CALL_TAXI_STATUS_CANCELED) {
					Log.d(TAG, "status is CALL_TAXI_STATUS_CANCELED!");
					Intent retIntent = new Intent();
					retIntent.putExtra(RET_CODE, CANCEL_WAIT);
					setResult(0, retIntent);
					WaitTaxiActivity.this.finish();
				}

				decreaseSetLeftWaitTaxiTime();
				if (0 == getLeftWaitTaxiTime()) {
					msg = mHandler.obtainMessage();
					msg.what = SET_RESEND_VIEW;
					mHandler.sendMessage(msg);

					break;
				}
			}
			Log.d(TAG, "leave wait taxi thread");
		}
	};

	@Override
	public void onBackPressed() {
		Log.d(TAG, "onBackPressed is ignored!");
		return;
	}
}
