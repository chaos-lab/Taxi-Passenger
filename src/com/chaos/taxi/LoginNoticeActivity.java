package com.chaos.taxi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class LoginNoticeActivity extends Activity {
	private static final String TAG = "LoginNoticeActivity";

	Button relogin_btn = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.login_notice);
		relogin_btn = (Button) findViewById(R.id.relogin_ok_btn);

		relogin_btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				LoginNoticeActivity.this.startActivity(new Intent(
						LoginNoticeActivity.this, LoginActivity.class));
				LoginNoticeActivity.this.finish();
			}
		});
	}
}
