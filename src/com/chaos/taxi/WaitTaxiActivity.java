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
	static final int SUCCEED_WAIT = 0;
	static final int CANCEL_WAIT = 1;
	static final int REJECT_WAIT = 2;

	static final int SET_RESEND_VIEW = 100;
	static final int SET_WAITTIME_TEXT = 200;

	Button waitTaxiBtn = null;
	TextView waitTaxiTimeTv = null;
	long mRequestKey = -1;

	Button keepWaitBtn = null;
	Button cancelWaitBtn = null;

	int mWaitTaxiTime = 0;
	Integer mLeftWaitTaxiTime = 0;

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
				setResult(CANCEL_WAIT);
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
				setResult(CANCEL_WAIT);
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
		mRequestKey = getIntent().getLongExtra("RequestKey", -1);
		Log.d(TAG, "mRequestKey is " + mRequestKey);

		setWaitTaxiView();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	Runnable mTask = new Runnable() {
		public void run() {
			while (true) {
				Message msg = new Message();
				msg.what = SET_WAITTIME_TEXT;
				mHandler.sendMessage(msg);

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				int status = RequestProcessor.popCallTaxiStatus(mRequestKey);
				if (status == RequestProcessor.CALL_TAXI_STATUS_SUCCEED) {
					WaitTaxiActivity.this.setResult(SUCCEED_WAIT);
					WaitTaxiActivity.this.finish();
				} else if (status == RequestProcessor.CALL_TAXI_STATUS_REJECTED) {
					setResult(REJECT_WAIT);
					WaitTaxiActivity.this.finish();
				}

				decreaseSetLeftWaitTaxiTime();
				if (0 == getLeftWaitTaxiTime()) {
					msg = new Message();
					msg.what = SET_RESEND_VIEW;
					mHandler.sendMessage(msg);

					break;
				}
			}
		}
	};

	@Override
	public void onBackPressed() {
		Log.d(TAG, "onBackPressed is ignored!");
		return;
	}
}
