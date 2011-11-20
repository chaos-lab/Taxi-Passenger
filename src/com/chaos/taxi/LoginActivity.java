package com.chaos.taxi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends Activity {
	private static final String TAG = "LoginActivity";
	static final int REGISTER_REQUEST_CODE = 111111;

	Button login_btn = null;
	Button register_btn = null;
	EditText nickname_et = null;
	EditText password_et = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login);

		login_btn = (Button) findViewById(R.id.login_btn);
		register_btn = (Button) findViewById(R.id.register_btn);
		nickname_et = (EditText) findViewById(R.id.nick_name_et);
		password_et = (EditText) findViewById(R.id.password_et);

		login_btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Log.d(TAG, "login_btn onClick!");

				String nickName = nickname_et.getText().toString();
				String password = password_et.getText().toString();
				Log.d(TAG, "nickName is " + nickName + " password is "
						+ password);
				if (nickName == null || nickName.equals("")) {
					Toast.makeText(LoginActivity.this,
							"Should input the NickName", 4000).show();
				} else if (password == null || password.equals("")) {
					Toast.makeText(LoginActivity.this,
							"Should input the Password", 4000).show();
				} else {
					String msg = RequestProcessor.login(nickName, password);
					if (msg.equals(RequestProcessor.LOGIN_SUCCESS)) {
						LoginActivity.this.startActivity(new Intent(
								LoginActivity.this, TaxiActivity.class));
						LoginActivity.this.finish();
					} else {				
						Toast.makeText(LoginActivity.this,
								"Login Fail: " + msg, 5000).show();
					}
				}
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
			this.finish();
		}
	}
}
