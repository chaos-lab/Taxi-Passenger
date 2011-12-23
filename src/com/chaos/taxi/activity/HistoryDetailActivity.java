package com.chaos.taxi.activity;

import com.chaos.taxi.R;
import com.chaos.taxi.util.RequestProcessor;
import com.chaos.taxi.util.TaxiHistorySqlHelper;
import com.chaos.taxi.util.TaxiHistorySqlHelper.HistoryItem;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.Toast;
import android.widget.RatingBar.OnRatingBarChangeListener;
import android.widget.TextView;

public class HistoryDetailActivity extends Activity {
	private static final String TAG = "HistoryDetailActivity";
	static final int SHOW_PROGRESS_DIALOG = 100;
	static final int DISMISS_PROGRESS_DIALOG = 200;
	static final int SHOW_TOAST_TEXT = 300;

	TaxiHistorySqlHelper mTaxiHistorySqlHelper = null;
	ProgressDialog mProgressDialog = null;
	HistoryItem mHistoryItem = null;

	TextView mDriverInfoTV = null;
	TextView mLocationInfoTV = null;
	RatingBar mDriverRatingBar = null;
	TextView mDriverCommentTV = null;

	RatingBar mPassengerRatingBar = null;
	TextView mPassengerCommentTV = null;
	EditText mAddCommentEditText = null;
	Button mAddCommentButton = null;
	Button mSubmitCommentButton = null;
	LinearLayout mSubmitCommentProgressBar = null;

	Button mOkButton = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		RequestProcessor.initRequestProcessor(this);
		
		Intent intent = getIntent();
		long historyId = intent.getLongExtra("HistoryId", -1);
		if (historyId == -1) {
			Log.d(TAG, "historyId should exist in intent!");
			finish();
			return;
		}

		mTaxiHistorySqlHelper = new TaxiHistorySqlHelper(this);
		mTaxiHistorySqlHelper.open();

		mHistoryItem = mTaxiHistorySqlHelper.queryHistory(historyId);
		if (mHistoryItem == null) {
			Log.e(TAG, "No HistoryItem for " + historyId + "!");
			HistoryDetailActivity.this.finish();
			return;
		}
		showHistoryDetail();
	}

	private void showHistoryDetail() {
		setContentView(R.layout.history_detail);
		
		mDriverInfoTV = (TextView) findViewById(R.id.driverInfoTextView);
		mLocationInfoTV = (TextView) findViewById(R.id.locationInfoTextView);
		mDriverRatingBar = (RatingBar) findViewById(R.id.driverRatingBar);
		mDriverCommentTV = (TextView) findViewById(R.id.driverCommentTextView);

		mPassengerRatingBar = (RatingBar) findViewById(R.id.passengerRatingBar);
		mPassengerCommentTV = (TextView) findViewById(R.id.passengerCommentTextView);
		mAddCommentEditText = (EditText) findViewById(R.id.addCommentEditText);
		mAddCommentButton = (Button) findViewById(R.id.addCommentButton);
		mSubmitCommentButton = (Button) findViewById(R.id.submitCommentButton);
		mSubmitCommentProgressBar = (LinearLayout) findViewById(R.id.submitCommentProgressBar);

		mOkButton = (Button) findViewById(R.id.okButton);

		mAddCommentEditText.setVisibility(View.GONE);
		mAddCommentButton.setVisibility(View.GONE);
		mSubmitCommentButton.setVisibility(View.GONE);
		mSubmitCommentProgressBar.setVisibility(View.GONE);

		mPassengerRatingBar
				.setOnRatingBarChangeListener(new OnRatingBarChangeListener() {
					public void onRatingChanged(RatingBar ratingBar,
							float rating, boolean fromUser) {
						ratingBar.setRating(Math.round(rating));
					}
				});
		mAddCommentButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mAddCommentButton.setVisibility(View.GONE);
				mPassengerRatingBar.setIsIndicator(false);
				mAddCommentEditText.setVisibility(View.VISIBLE);
				mSubmitCommentButton.setVisibility(View.VISIBLE);
				mSubmitCommentProgressBar.setVisibility(View.GONE);
			}
		});
		mSubmitCommentButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				double rating = (int) Math.floor(mPassengerRatingBar
						.getRating());
				if (mPassengerRatingBar.getRating() - rating > 0.5) {
					rating += 0.5;
				}
				if (rating == 0) {
					mHandler.sendMessage(mHandler.obtainMessage(
							SHOW_TOAST_TEXT, "Must set the rating."));
					return;
				}
				mPassengerRatingBar.setIsIndicator(true);
				mAddCommentButton.setVisibility(View.GONE);
				mAddCommentEditText.setVisibility(View.VISIBLE);
				mSubmitCommentButton.setVisibility(View.GONE);
				mSubmitCommentProgressBar.setVisibility(View.VISIBLE);

				String comment = mAddCommentEditText.getText() == null ? null
						: mAddCommentEditText.getText().toString();
				if (RequestProcessor.sendSubmitCommentRequest(mHistoryItem.mId,
						comment, rating)) {
					mAddCommentButton.setVisibility(View.GONE);
					mAddCommentEditText.setVisibility(View.GONE);
					mSubmitCommentButton.setVisibility(View.GONE);
					mSubmitCommentProgressBar.setVisibility(View.GONE);

					if (comment != null)
						mPassengerCommentTV.setText(comment);
					else
						mPassengerCommentTV
								.setText("Passenger left no comment.");
					mPassengerCommentTV.setVisibility(View.VISIBLE);

					mHistoryItem.mPassengerEvaluation = rating;
					mHistoryItem.mPassengerComment = comment;
					mTaxiHistorySqlHelper.replaceHistory(mHistoryItem);
				} else {
					mPassengerRatingBar.setIsIndicator(false);
					mSubmitCommentButton.setVisibility(View.VISIBLE);
					mSubmitCommentProgressBar.setVisibility(View.GONE);
					mHandler.sendMessage(mHandler.obtainMessage(
							SHOW_TOAST_TEXT,
							"Submit comment fail. Please retry later."));
				}
			}
		});

		mDriverInfoTV.setText("DriverInfo: CarNumber is "
				+ mHistoryItem.mCarNumber + ". PhoneNumber is "
				+ mHistoryItem.mPhoneNumber + ". DriverName is "
				+ mHistoryItem.mNickName);

		String locationStr = "From: "
				+ (mHistoryItem.mOrigin == null ? "Latitude: "
						+ mHistoryItem.mOriginLatitude + ", Longitude"
						+ mHistoryItem.mOriginLongitude : mHistoryItem.mOrigin);
		if (mHistoryItem.mDestination != null
				|| (mHistoryItem.mDestinationLatitude != null && mHistoryItem.mDestinationLongitude != null)) {
			locationStr += "To: "
					+ (mHistoryItem.mDestination == null ? "Latitude: "
							+ mHistoryItem.mDestinationLatitude + ", Longitude"
							+ mHistoryItem.mDestinationLongitude
							: mHistoryItem.mDestination);
		}
		mLocationInfoTV.setText(locationStr);

		if (mHistoryItem.mDriverEvaluation != null) {
			mDriverRatingBar.setRating((float) Math
					.ceil(mHistoryItem.mDriverEvaluation));
		}
		if (mHistoryItem.mDriverComment != null) {
			mDriverCommentTV.setText(mHistoryItem.mDriverComment);
		} else {
			mDriverCommentTV.setText("Driver left no comment!");
		}

		if (mHistoryItem.mPassengerEvaluation != null) {
			mPassengerRatingBar.setIsIndicator(true);
			mPassengerRatingBar.setRating(Math
					.round(mHistoryItem.mPassengerEvaluation));
			if (mHistoryItem.mPassengerComment != null) {
				mPassengerCommentTV.setText(mHistoryItem.mPassengerComment);
			} else {
				mPassengerCommentTV.setText("Passenger left no comment.");
			}
		} else {
			mPassengerCommentTV.setVisibility(View.GONE);
			mPassengerRatingBar.setIsIndicator(false);
			mAddCommentButton.setVisibility(View.VISIBLE);
			mAddCommentEditText.setVisibility(View.GONE);
			mSubmitCommentButton.setVisibility(View.GONE);
			mSubmitCommentProgressBar.setVisibility(View.GONE);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		RequestProcessor.initRequestProcessor(this);
		if (mTaxiHistorySqlHelper == null) {
			mTaxiHistorySqlHelper = new TaxiHistorySqlHelper(this);
		}
		mTaxiHistorySqlHelper.open();
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mTaxiHistorySqlHelper != null) {
			mTaxiHistorySqlHelper.close();
		}
	}

	Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Log.d(TAG, "get message: " + msg.what);
			switch (msg.what) {
			case SHOW_PROGRESS_DIALOG:
				mProgressDialog = ProgressDialog
						.show(HistoryDetailActivity.this, "Loading",
								(String) msg.obj);
				break;
			case DISMISS_PROGRESS_DIALOG:
				if (mProgressDialog != null) {
					mProgressDialog.dismiss();
					mProgressDialog = null;
				}
				break;
			case SHOW_TOAST_TEXT:
				Toast.makeText(HistoryDetailActivity.this,
						(CharSequence) msg.obj, 4000).show();
			}
		}
	};
}
