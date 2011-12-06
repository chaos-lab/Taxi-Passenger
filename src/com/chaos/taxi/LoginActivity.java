package com.chaos.taxi;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.inputmethodservice.InputMethodService;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends Activity {
	private static final String TAG = "LoginActivity";
	static final int REGISTER_REQUEST_CODE = 111111;
	static final int SHOW_PROGRESS_DIALOG = 100;
	static final int DISMISS_PROGRESS_DIALOG = 200;
	static final int SHOW_TOAST_TEXT = 300;

	ProgressDialog mProgressDialog = null;

	Button login_btn = null;
	Button register_btn = null;
	EditText phoneNumber_et = null;
	EditText password_et = null;

	Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Log.d(TAG, "get message: " + msg.what);
			switch (msg.what) {
			case SHOW_PROGRESS_DIALOG:
				mProgressDialog = ProgressDialog.show(LoginActivity.this,
						"Login", "Waiting for login...");
				break;
			case DISMISS_PROGRESS_DIALOG:
				if (mProgressDialog != null)
					mProgressDialog.dismiss();
				break;
			case SHOW_TOAST_TEXT:
				Toast.makeText(LoginActivity.this, (CharSequence) msg.obj, 4000)
						.show();
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		RequestProcessor.initRequestProcessor(LoginActivity.this, null);
		setContentView(R.layout.login);

		login_btn = (Button) findViewById(R.id.login_btn);
		register_btn = (Button) findViewById(R.id.register_btn);
		phoneNumber_et = (EditText) findViewById(R.id.login_phone_number_et);
		password_et = (EditText) findViewById(R.id.password_et);

		login_btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Log.d(TAG, "login_btn onClick!");

				new Thread(new Runnable() {
					public void run() {
						mHandler.sendEmptyMessage(SHOW_PROGRESS_DIALOG);

						String phoneNumber = phoneNumber_et.getText()
								.toString();
						String password = password_et.getText().toString();
						Log.d(TAG, "phoneNumber is " + phoneNumber
								+ " password is " + password);
						if (phoneNumber == null || phoneNumber.equals("")) {
							Toast.makeText(LoginActivity.this,
									"Should input the PhoneNumber", 4000)
									.show();
						} else if (password == null || password.equals("")) {
							Toast.makeText(LoginActivity.this,
									"Should input the Password", 4000).show();
						} else {
							String msg = RequestProcessor.login(phoneNumber,
									password);
							if (msg.equals(RequestProcessor.LOGIN_SUCCESS)) {
								if (!TaxiActivity.sStarted) {
									LoginActivity.this
											.startActivity(new Intent(
													LoginActivity.this,
													TaxiActivity.class));
								}
								mHandler.sendEmptyMessage(DISMISS_PROGRESS_DIALOG);
								LoginActivity.this.finish();
							} else {
								mHandler.sendMessage(mHandler.obtainMessage(
										SHOW_TOAST_TEXT, "Login Fail: " + msg));
								mHandler.sendEmptyMessage(DISMISS_PROGRESS_DIALOG);
							}
						}
					}
				}).start();
			}
		});

		register_btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Log.d(TAG, "register_btn onClick!");

				LoginActivity.this.startActivityForResult(new Intent(
						LoginActivity.this, RegisterActivity.class),
						REGISTER_REQUEST_CODE);
			}
		});
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REGISTER_REQUEST_CODE
				&& resultCode == RegisterActivity.REGISTER_SUCCESS_CODE) {
			// LoginActivity.this.startActivity(new Intent(LoginActivity.this,
			// TaxiActivity.class));
			// LoginActivity.this.finish();
		}
	}
}
