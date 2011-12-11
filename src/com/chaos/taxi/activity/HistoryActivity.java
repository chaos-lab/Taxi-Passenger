package com.chaos.taxi.activity;

import java.util.ArrayList;

import com.chaos.taxi.R;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class HistoryActivity extends ListActivity {
	private static final String TAG = "HistoryActivity";
	private ArrayList<String> mHistoryItems = new ArrayList<String>();
	private ArrayList<Long> mHistoryItemIds = new ArrayList<Long>();
	ProgressDialog mProgressDialog = null;

	static final int SHOW_PROGRESS_DIALOG = 100;
	static final int DISMISS_PROGRESS_DIALOG = 200;
	static final int SHOW_TOAST_TEXT = 300;

	Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Log.d(TAG, "get message: " + msg.what);
			switch (msg.what) {
			case SHOW_PROGRESS_DIALOG:
				mProgressDialog = ProgressDialog.show(HistoryActivity.this,
						"Login", "Waiting for login...");
				break;
			case DISMISS_PROGRESS_DIALOG:
				if (mProgressDialog != null) {
					mProgressDialog.dismiss();
					mProgressDialog = null;
				}
				break;
			case SHOW_TOAST_TEXT:
				Toast.makeText(HistoryActivity.this, (CharSequence) msg.obj,
						4000).show();
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mProgressDialog = new ProgressDialog(this);

		initListView();
	}

	protected void initListView() {
		new Thread(new Runnable() {
			public void run() {
				mHandler.sendEmptyMessage(SHOW_PROGRESS_DIALOG);

				// TODO: get history from local
				// TODO: get history from server
				// TODO: save history to local

				mHistoryItems.add("history 1");
				ArrayAdapter<String> adapter = new ArrayAdapter<String>(
						HistoryActivity.this, R.layout.history, mHistoryItems);

				mHandler.sendEmptyMessage(DISMISS_PROGRESS_DIALOG);
			}
		}).start();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// TODO Auto-generated method stub
		Log.d(TAG, "position is " + position);
	}
}
