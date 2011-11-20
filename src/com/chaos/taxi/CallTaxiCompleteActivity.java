package com.chaos.taxi;

import com.chaos.taxi.map.TaxiOverlayItem.TaxiOverlayItemParam;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class CallTaxiCompleteActivity extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		TaxiOverlayItemParam taxiParam = intent.getParcelableExtra("TaxiParam");

		setContentView(R.layout.call_taxi_complete);
		TextView call_taxi_complete_tv = (TextView) findViewById(R.id.call_taxi_complete_tv);
		Button call_taxi_complete_ok_btn = (Button) findViewById(R.id.call_taxi_complete_ok_btn);

		call_taxi_complete_tv.setText("CallTaxiComplete: \n" + "CarNumber is "
				+ taxiParam.mCarNumber + "\nPhoneNumber is "
				+ taxiParam.mPhoneNumber + "\nDriverName is "
				+ taxiParam.mNickName);
		call_taxi_complete_ok_btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				CallTaxiCompleteActivity.this.finish();
			}
		});
	}
}
