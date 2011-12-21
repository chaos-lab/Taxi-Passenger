package com.chaos.taxi.activity;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import com.chaos.taxi.R;
import com.chaos.taxi.util.RequestProcessor;
import com.chaos.taxi.util.TaxiHistorySqlHelper;
import com.chaos.taxi.util.TaxiHistorySqlHelper.HistoryItem;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class HistoryActivity extends ListActivity {
	private static final String TAG = "HistoryActivity";
	private ArrayList<HashMap<String, Object>> mHistoryItems = new ArrayList<HashMap<String, Object>>();
	private ArrayList<Long> mHistoryItemIds = new ArrayList<Long>();
	SimpleAdapter mSimpleAdapter = null;

	View mFooterView = null;
	LinearLayout mFooterProgressBar = null;
	Button mFooterButton = null;
	ProgressDialog mProgressDialog = null;
	TaxiHistorySqlHelper mTaxiHistorySqlHelper = null;
	long mTailHistoryId = 0;

	static final int SHOW_HISTORY_COUNT = 10;

	static final int SHOW_PROGRESS_DIALOG = 100;
	static final int DISMISS_PROGRESS_DIALOG = 200;
	static final int SHOW_TOAST_TEXT = 300;
	static final int SET_LIST_ADAPTER = 400;

	static final String ITEM_TITLE = "ITEM_TITLE";
	static final String ITEM_STAR1 = "ITEM_STAR1";
	static final String ITEM_STAR2 = "ITEM_STAR2";
	static final String ITEM_STAR3 = "ITEM_STAR3";
	static final String ITEM_STAR4 = "ITEM_STAR4";
	static final String ITEM_STAR5 = "ITEM_STAR5";
	static final String ITEM_TEXT = "ITEM_TEXT";

	static final String LIST_TAIL_HISTORY_ID = "LIST_TAIL_HISTORY_ID";

	Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Log.d(TAG, "get message: " + msg.what);
			switch (msg.what) {
			case SHOW_PROGRESS_DIALOG:
				if (mProgressDialog != null) {
					mProgressDialog.dismiss();
				}
				mProgressDialog = ProgressDialog.show(HistoryActivity.this,
						"Loading", (String) msg.obj);
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
				break;
			case SET_LIST_ADAPTER:
				HistoryActivity.this.setListAdapter(mSimpleAdapter);
				break;
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initFooterView();
		mProgressDialog = new ProgressDialog(this);
		mTaxiHistorySqlHelper = new TaxiHistorySqlHelper(this);
		mTaxiHistorySqlHelper.open();

		initListView();
	}

	@Override
	public void onResume() {
		super.onResume();
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

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putLong(LIST_TAIL_HISTORY_ID, mTailHistoryId);
	}

	@Override
	public void onRestoreInstanceState(Bundle state) {
		super.onRestoreInstanceState(state);
		mTailHistoryId = state.getInt(LIST_TAIL_HISTORY_ID);
	}

	protected void initListView() {
		setContentView(R.layout.history_refreshing);
		new Thread(new Runnable() {
			public void run() {
				// query server for the new history items
				ArrayList<HistoryItem> items = RequestProcessor
						.sendQueryHistoryRequest(0, System.currentTimeMillis(),
								SHOW_HISTORY_COUNT);
				saveHistoryItems(items);

				// in case we cannot connect to server
				if (items == null) {
					// get the latest local history item
					HistoryItem tailHistoryItem = mTaxiHistorySqlHelper
							.queryMaxIdHistory();
					if (tailHistoryItem != null) {
						items = mTaxiHistorySqlHelper.batchQueryHistory(
								tailHistoryItem.mId, SHOW_HISTORY_COUNT);
					}
				}

				if (items != null) {
					for (int i = 0; i < items.size(); ++i) {
						HashMap<String, Object> map = new HashMap<String, Object>();
						map.put(ITEM_TITLE, items.get(i).mCarNumber);
						map.put(ITEM_TEXT, items.get(i).mOrigin);
						mHistoryItems.add(map);
						mHistoryItemIds.add(items.get(i).mId);
					}

					if (items.size() >= SHOW_HISTORY_COUNT) {
						addFooterButton();
					}
					if (items.size() > 0) {
						mTailHistoryId = items.get(items.size() - 1).mId;
					}
				}

				mSimpleAdapter = new SimpleAdapter(HistoryActivity.this,
						mHistoryItems, R.layout.history, new String[] {
								ITEM_TITLE, ITEM_TEXT }, new int[] {
								R.id.ItemTitle, R.id.ItemText });
				mHandler.sendEmptyMessage(SET_LIST_ADAPTER);
			}
		}).start();
	}

	private void initFooterView() {
		mFooterView = LayoutInflater.from(this).inflate(
				R.layout.history_footer, null);
		mFooterButton = (Button) mFooterView
				.findViewById(R.id.moreHistoryButton);
		mFooterProgressBar = (LinearLayout) mFooterView
				.findViewById(R.id.moreHistoryProgressBar);
		mFooterButton.setVisibility(View.VISIBLE);
		mFooterProgressBar.setVisibility(View.GONE);

		mFooterButton.setText("More...");
		mFooterButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				new Thread(new Runnable() {
					public void run() {
						mFooterButton.setVisibility(View.GONE);
						mFooterProgressBar.setVisibility(View.VISIBLE);

						// first check the local database
						ArrayList<HistoryItem> items = mTaxiHistorySqlHelper
								.batchQueryHistory(mTailHistoryId - 1,
										SHOW_HISTORY_COUNT);
						int countNeedToQuery = SHOW_HISTORY_COUNT;
						if (items != null && items.size() > 0) {
							for (int i = 0; i < items.size() - 1; ++i) {
								if (items.get(i).mNextId == items.get(i + 1).mId) {
									--countNeedToQuery;
								} else {
									break;
								}

							}
						}

						// if history not enough, query the server
						if (countNeedToQuery > 0) {
							// query from server
							HistoryItem tailHistoryItem = mTaxiHistorySqlHelper
									.queryHistory(mTailHistoryId);
							if (tailHistoryItem == null) {
								Log.wtf(TAG,
										"Should have a HistoryItem with mTailHistoryId: "
												+ mTailHistoryId);
								return;
							}
							ArrayList<HistoryItem> itemsFromServer = RequestProcessor
									.sendQueryHistoryRequest(0,
											tailHistoryItem.mStartTimeStamp,
											countNeedToQuery);
							saveHistoryItems(itemsFromServer);

							if (items == null) {
								items = itemsFromServer;
							} else {
								if (itemsFromServer != null) {
									items.addAll(itemsFromServer);
								}
							}
						}

						// add the history to view
						if (items == null || items.size() == 0) {
							mHandler.sendMessage(mHandler.obtainMessage(
									SHOW_TOAST_TEXT, "No More History."));
						} else {
							for (int i = 0; i < items.size(); ++i) {
								HashMap<String, Object> map = new HashMap<String, Object>();
								map.put(ITEM_TITLE, items.get(i).mCarNumber);
								map.put(ITEM_TEXT, items.get(i).mOrigin);
								mHistoryItems.add(map);
								mHistoryItemIds.add(items.get(i).mId);
							}
							mSimpleAdapter.notifyDataSetChanged();
							mTailHistoryId = items.get(items.size() - 1).mId;
						}

						mFooterButton.setVisibility(View.VISIBLE);
						mFooterProgressBar.setVisibility(View.GONE);
					}
				}).start();
			}
		});
	}

	protected void saveHistoryItems(ArrayList<HistoryItem> items) {
		if (items == null || items.size() == 0) {
			Log.d(TAG, "no need to saveHistoryItems.");
			return;
		}
		// get the latest local history item
		HistoryItem tailHistoryItem = mTaxiHistorySqlHelper.queryMaxIdHistory();
		long tailHistoryId = 0;
		if (tailHistoryItem != null) {
			tailHistoryId = tailHistoryItem.mId;
		}

		boolean needSetNextId = true;
		for (int i = 0; i < items.size(); ++i) {
			if (items.get(i).mId == tailHistoryId) {
				needSetNextId = false;
			}
			if (needSetNextId) {
				if (i == items.size() - 1) {
					items.get(i).mNextId = 0;
				} else {
					items.get(i).mNextId = items.get(i + 1).mId;
				}
			}
			mTaxiHistorySqlHelper.replaceHistory(items.get(i));
		}
	}

	protected void addFooterButton() {
		this.getListView().addFooterView(mFooterButton);
	}

	protected void removeFooterButton() {
		this.getListView().removeFooterView(mFooterButton);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Log.d(TAG, "onListItemClick: position is " + position);
		long historyId = mHistoryItemIds.get(position);
		Log.d(TAG, "onListItemClick: historyId is " + historyId);
		Intent intent = new Intent(this, HistoryDetailActivity.class);
		intent.putExtra("HistoryId", historyId);
		startActivity(intent);
	}
}
