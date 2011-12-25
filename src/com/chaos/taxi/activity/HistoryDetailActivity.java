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
	static final int SHOW_PROGRESS_DIALOG = 1;
	static final int DISMISS_PROGRESS_DIALOG = 2;
	static final int SHOW_TOAST_TEXT = 3;
	static final int PASSENGER_RATING_ON = 4;
	static final int EDIT_COMMENT_ET_ON = 5;
	static final int SUBMIT_COMMENT_ON = 6;
	static final int SUBMIT_COMMENT_BAR_ON = 7;
	static final int SUBMIT_COMMENT_BTN_OFF = 8;
	static final int PASSENGER_RATING_OFF = 9;
	static final int EDIT_COMMENT_ET_OFF = 10;
	static final int SUBMIT_COMMENT_BTN_ON = 11;
	static final int SUBMIT_COMMENT_BAR_OFF = 12;
	static final int COMMENT_TEXTVIEW_ON = 13;

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
		mSubmitCommentButton = (Button) findViewById(R.id.submitCommentButton);
		mSubmitCommentProgressBar = (LinearLayout) findViewById(R.id.submitCommentProgressBar);

		mOkButton = (Button) findViewById(R.id.okButton);

		mAddCommentEditText.setVisibility(View.GONE);
		mSubmitCommentButton.setVisibility(View.GONE);
		mSubmitCommentProgressBar.setVisibility(View.GONE);

		mPassengerRatingBar
				.setOnRatingBarChangeListener(new OnRatingBarChangeListener() {
					public void onRatingChanged(RatingBar ratingBar,
							float rating, boolean fromUser) {
						ratingBar.setRating(Math.round(rating));
					}
				});
		mSubmitCommentButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				new Thread(new Runnable() {
					public void run() {
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
						mHandler.sendEmptyMessage(PASSENGER_RATING_ON);
						mHandler.sendEmptyMessage(EDIT_COMMENT_ET_ON);
						mHandler.sendEmptyMessage(SUBMIT_COMMENT_BTN_OFF);
						mHandler.sendEmptyMessage(SUBMIT_COMMENT_BAR_ON);

						String comment = mAddCommentEditText.getText() == null ? null
								: mAddCommentEditText.getText().toString();
						if (RequestProcessor.sendSubmitCommentRequest(
								mHistoryItem.mId, comment, rating)) {
							mHandler.sendEmptyMessage(PASSENGER_RATING_OFF);
							mHandler.sendEmptyMessage(EDIT_COMMENT_ET_OFF);
							mHandler.sendEmptyMessage(SUBMIT_COMMENT_BTN_OFF);
							mHandler.sendEmptyMessage(SUBMIT_COMMENT_BAR_OFF);

							if (comment != null)
								mPassengerCommentTV.setText(comment);
							else
								mPassengerCommentTV
										.setText("Passenger left no comment.");

							mHandler.sendEmptyMessage(COMMENT_TEXTVIEW_ON);

							mHistoryItem.mPassengerEvaluation = rating;
							mHistoryItem.mPassengerComment = comment;
							mTaxiHistorySqlHelper.replaceHistory(mHistoryItem);
						} else {
							mHandler.sendEmptyMessage(PASSENGER_RATING_ON);
							mHandler.sendEmptyMessage(SUBMIT_COMMENT_BTN_ON);
							mHandler.sendEmptyMessage(SUBMIT_COMMENT_BAR_OFF);

							mHandler.sendMessage(mHandler.obtainMessage(
									SHOW_TOAST_TEXT,
									"Submit comment fail. Please retry later."));
						}
					}
				}).start();
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
				|| (mHistoryItem.mDestinationLatitude != null
						&& mHistoryItem.mDestinationLongitude != null
						&& mHistoryItem.mDestinationLatitude != 0 && mHistoryItem.mDestinationLongitude != 0)) {
			locationStr += " To: "
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

		if (mHistoryItem.mPassengerEvaluation != null
				&& mHistoryItem.mPassengerEvaluation != 0) {
			mPassengerRatingBar.setIsIndicator(true);
			mPassengerRatingBar.setRating(Math
					.round(mHistoryItem.mPassengerEvaluation));
			if (mHistoryItem.mPassengerComment != null
					&& mHistoryItem.mPassengerComment.length() != 0) {
				mPassengerCommentTV.setText(mHistoryItem.mPassengerComment);
			} else {
				mPassengerCommentTV.setText("Passenger left no comment.");
			}
		} else {
			mPassengerCommentTV.setVisibility(View.GONE);
			mPassengerRatingBar.setIsIndicator(false);
			mAddCommentEditText.setVisibility(View.VISIBLE);
			mSubmitCommentButton.setVisibility(View.VISIBLE);
			mSubmitCommentProgressBar.setVisibility(View.GONE);
		}

		mOkButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				HistoryDetailActivity.this.finish();
			}
		});
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
				break;
			case PASSENGER_RATING_ON:
				mPassengerRatingBar.setIsIndicator(true);
				break;
			case PASSENGER_RATING_OFF:
				mPassengerRatingBar.setIsIndicator(false);
				break;
			case EDIT_COMMENT_ET_ON:
				mAddCommentEditText.setVisibility(View.VISIBLE);
				break;
			case EDIT_COMMENT_ET_OFF:
				mAddCommentEditText.setVisibility(View.GONE);
				break;
			case SUBMIT_COMMENT_BTN_ON:
				mSubmitCommentButton.setVisibility(View.VISIBLE);
				break;
			case SUBMIT_COMMENT_BTN_OFF:
				mSubmitCommentButton.setVisibility(View.GONE);
				break;
			case SUBMIT_COMMENT_BAR_ON:
				mSubmitCommentProgressBar.setVisibility(View.VISIBLE);
				break;
			case SUBMIT_COMMENT_BAR_OFF:
				mSubmitCommentProgressBar.setVisibility(View.GONE);
				break;
			case COMMENT_TEXTVIEW_ON:
				mPassengerCommentTV.setVisibility(View.VISIBLE);
				break;
			}
		}
	};
}
