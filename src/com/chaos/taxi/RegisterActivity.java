package com.chaos.taxi;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class RegisterActivity extends Activity {
	static final int REGISTER_SUCCESS_CODE = 222222;

	Button mRegisterButton = null;
	EditText mNickNameET = null;
	EditText mPhoneNumberET = null;
	EditText mPasswordET = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.register);

		mRegisterButton = (Button) findViewById(R.id.register_btn);
		mNickNameET = (EditText) findViewById(R.id.reg_nick_name_et);
		mPhoneNumberET = (EditText) findViewById(R.id.reg_phone_number_et);
		mPasswordET = (EditText) findViewById(R.id.reg_passwd_et);

		mRegisterButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				String nickName = mNickNameET.getText().toString();
				String phoneNumber = mPhoneNumberET.getText().toString();
				String password = mPasswordET.getText().toString();

				if (nickName == null || nickName.equals("")) {
					Toast.makeText(RegisterActivity.this,
							"NickName should not be null!", 4000).show();
				} else if (phoneNumber == null || phoneNumber.equals("")) {
					Toast.makeText(RegisterActivity.this,
							"PhoneNumber should not be null!", 4000).show();
				} else if (password == null || password.equals("")) {
					Toast.makeText(RegisterActivity.this,
							"Password should not be null!", 4000).show();
				} else {
					String message = RequestProcessor.register(nickName,
							phoneNumber, password);
					if (message.equals(RequestProcessor.REGISTER_SUCCESS)) {
						setResult(REGISTER_SUCCESS_CODE);
						RegisterActivity.this.finish();
					} else {
						Toast.makeText(RegisterActivity.this, message, 5000);
					}
				}
			}
		});
	}
}
