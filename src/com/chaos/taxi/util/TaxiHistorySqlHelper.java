package com.chaos.taxi.util;

import java.util.ArrayList;

import com.chaos.taxi.util.TaxiHistorySqlHelper.HistoryItem;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class TaxiHistorySqlHelper extends SQLiteOpenHelper {
	private static final String TAG = "TaxiHistorySqlHelper";
	static final String TABLE_NAME = "taxi_history";

	public static class HistoryItem {
		public static final String ID = "id";
		public static final String CAR_NUMBER = "car_number";
		public static final String NICKNAME = "nickname";
		public static final String PHONE_NUMBER = "phoneNumber";
		public static final String ORIGIN = "origin";
		public static final String ORIGIN_LATITUDE = "origin_latitude";
		public static final String ORIGIN_LONGITUDE = "origin_longitude";
		public static final String DESTINATION = "destination";
		public static final String DESTINATION_LATITUDE = "destination_latitude";
		public static final String DESTINATION_LONGITUDE = "destination_longitude";
		public static final String DRIVER_EVALUATION = "driver_eval";
		public static final String PASSENGER_EVALUATION = "passenger_eval";
		public static final String DRIVER_COMMENT = "driver_comment";
		public static final String PASSENGER_COMMENT = "passenger_comment";
		public static final String DRIVER_COMMENT_TIMESTAMP = "driver_comment_timestamp";
		public static final String PASSENGER_COMMENT_TIMESTAMP = "passenger_comment_timestamp";
		public static final String START_TIMESTAMP = "start_timestamp";
		public static final String END_TIMESTAMP = "end_timestamp";
		public static final String HISTORY_STATE = "history_state";
		public static final String NEXT_ID = "next_id";

		public long mId;
		public String mCarNumber;
		public String mNickName;
		public String mPhoneNumber;
		public String mOrigin;
		public Double mOriginLatitude;
		public Double mOriginLongitude;
		public String mDestination;
		public Double mDestinationLatitude;
		public Double mDestinationLongitude;
		public Double mDriverEvaluation;
		public Double mPassengerEvaluation;
		public String mDriverComment;
		public String mPassengerComment;
		public Long mDriverCommentTimeStamp;
		public Long mPassengerCommentTimeStamp;
		public Long mStartTimeStamp;
		public Long mEndTimeStamp;
		public int mHistoryState;

		public long mNextId;

		public HistoryItem(long id, String carNumber, String nickName,
				String phoneNumber, String origin, Double originLatitude,
				Double originLongitude, String destination,
				Double destinationLatitude, Double destinationLongitude,
				Double driverEvaluation, Double passengerEvaluation,
				String driverComment, String passengerComment,
				Long driverCommentTimeStamp, Long passengerCommentTimeStamp,
				Long startTimeStamp, Long endTimeStamp, long nextId,
				int historyState) {
			mId = id;
			mCarNumber = carNumber;
			mNickName = nickName;
			mPhoneNumber = phoneNumber;
			mOrigin = origin;
			mOriginLatitude = originLatitude;
			mOriginLongitude = originLongitude;
			mDestination = destination;
			mDestinationLatitude = destinationLatitude;
			mDestinationLongitude = destinationLongitude;
			mDriverEvaluation = driverEvaluation;
			mPassengerEvaluation = passengerEvaluation;
			mDriverComment = driverComment;
			mPassengerComment = passengerComment;
			mDriverCommentTimeStamp = driverCommentTimeStamp;
			mPassengerCommentTimeStamp = passengerCommentTimeStamp;
			mStartTimeStamp = startTimeStamp;
			mEndTimeStamp = endTimeStamp;
			mHistoryState = historyState;

			mNextId = nextId;

			if (mOrigin == null) {
				mOrigin = queryGpscoderInNanjing(mOriginLatitude,
						mOriginLongitude);
			}
			if (mDestination == null) {
				mDestination = queryGpscoderInNanjing(mDestinationLatitude,
						mDestinationLongitude);
			}
		}

		public HistoryItem(Cursor cursor) {
			this(cursor.getLong(cursor.getColumnIndex(ID)), cursor
					.getString(cursor.getColumnIndex(CAR_NUMBER)), cursor
					.getString(cursor.getColumnIndex(NICKNAME)), cursor
					.getString(cursor.getColumnIndex(PHONE_NUMBER)), cursor
					.getString(cursor.getColumnIndex(ORIGIN)), cursor
					.getDouble(cursor.getColumnIndex(ORIGIN_LATITUDE)), cursor
					.getDouble(cursor.getColumnIndex(ORIGIN_LONGITUDE)), cursor
					.getString(cursor.getColumnIndex(DESTINATION)), cursor
					.getDouble(cursor.getColumnIndex(DESTINATION_LATITUDE)),
					cursor.getDouble(cursor
							.getColumnIndex(DESTINATION_LONGITUDE)),
					cursor.getDouble(cursor.getColumnIndex(DRIVER_EVALUATION)),
					cursor.getDouble(cursor
							.getColumnIndex(PASSENGER_EVALUATION)), cursor
							.getString(cursor.getColumnIndex(DRIVER_COMMENT)),
					cursor.getString(cursor.getColumnIndex(PASSENGER_COMMENT)),
					cursor.getLong(cursor
							.getColumnIndex(DRIVER_COMMENT_TIMESTAMP)),
					cursor.getLong(cursor
							.getColumnIndex(PASSENGER_COMMENT_TIMESTAMP)),
					cursor.getLong(cursor.getColumnIndex(START_TIMESTAMP)),
					cursor.getLong(cursor.getColumnIndex(END_TIMESTAMP)),
					cursor.getInt(cursor.getColumnIndex(NEXT_ID)), cursor
							.getInt(cursor.getColumnIndex(HISTORY_STATE)));
		}

		private String queryGpscoderInNanjing(Double latitude, Double longitude) {
			if (isGpsLocationInNanjing(latitude, longitude)) {
				return RequestProcessor.sendQueryGpscoderRequest(latitude,
						longitude);
			}
			return null;
		}

		private boolean isGpsLocationInNanjing(Double latitude, Double longitude) {
			if (latitude == null || longitude == null) {
				return false;
			}
			// TODO Auto-generated method stub
			return true;
		}

		public ContentValues getInsertContentValues() {
			ContentValues cv = new ContentValues();
			cv.put(ID, mId);
			cv.put(CAR_NUMBER, mCarNumber);
			cv.put(NICKNAME, mNickName);
			cv.put(PHONE_NUMBER, mPhoneNumber);
			cv.put(ORIGIN, mOrigin);
			cv.put(ORIGIN_LATITUDE, mOriginLatitude);
			cv.put(ORIGIN_LONGITUDE, mOriginLongitude);
			cv.put(DESTINATION, mDestination);
			cv.put(DESTINATION_LATITUDE, mDestinationLatitude);
			cv.put(DESTINATION_LONGITUDE, mDestinationLongitude);
			cv.put(DRIVER_EVALUATION, mDriverEvaluation);
			cv.put(PASSENGER_EVALUATION, mPassengerEvaluation);
			cv.put(DRIVER_COMMENT, mDriverComment);
			cv.put(PASSENGER_COMMENT, mPassengerComment);
			cv.put(DRIVER_COMMENT_TIMESTAMP, mDriverCommentTimeStamp);
			cv.put(PASSENGER_COMMENT_TIMESTAMP, mPassengerCommentTimeStamp);
			cv.put(START_TIMESTAMP, mStartTimeStamp);
			cv.put(END_TIMESTAMP, mEndTimeStamp);
			cv.put(NEXT_ID, mNextId);
			cv.put(HISTORY_STATE, mHistoryState);
			return cv;
		}

		public String getUpdateCommand() {
			String str = "UPDATE " + TABLE_NAME + " SET " + CAR_NUMBER + "="
					+ mCarNumber + " " + NICKNAME + "=" + mNickName + " "
					+ PHONE_NUMBER + "=" + mPhoneNumber + " " + ORIGIN + "="
					+ mOrigin + " " + ORIGIN_LATITUDE + "=" + mOriginLatitude
					+ " " + ORIGIN_LONGITUDE + "=" + mOriginLongitude + " "
					+ DESTINATION + "=" + mDestination + " "
					+ DESTINATION_LATITUDE + "=" + mDestinationLatitude + " "
					+ DESTINATION_LONGITUDE + "=" + mDestinationLongitude + " "
					+ DRIVER_EVALUATION + "=" + mDriverEvaluation + " "
					+ PASSENGER_EVALUATION + "=" + mPassengerEvaluation + " "
					+ DRIVER_COMMENT + "=" + mDriverComment + " "
					+ PASSENGER_COMMENT + "=" + mPassengerComment + " "
					+ DRIVER_COMMENT_TIMESTAMP + "=" + mDriverCommentTimeStamp
					+ " " + PASSENGER_COMMENT_TIMESTAMP + "="
					+ mPassengerCommentTimeStamp + " " + START_TIMESTAMP + "="
					+ mStartTimeStamp + " " + END_TIMESTAMP + "="
					+ mEndTimeStamp + " " + NEXT_ID + "=" + mNextId + " "
					+ HISTORY_STATE + "=" + mHistoryState + " WHERE " + ID
					+ "=" + mId;
			Log.d(TAG, "update command is " + str);
			return str;
		}

		public static HistoryItem parseItemFromCursor(Cursor cursor) {
			if (cursor == null || cursor.getCount() == 0) {
				Log.e(TAG, "Not enough content in cursor: " + cursor);
				return null;
			}

			if (!cursor.moveToFirst()) {
				return null;
			}
			HistoryItem item = new HistoryItem(cursor);
			return item;
		}

		public static ArrayList<HistoryItem> parseItemFromCursor(Cursor cursor,
				int count) {
			if (cursor == null || cursor.getCount() == 0) {
				Log.e(TAG, "Not enough content in cursor: " + cursor);
				return null;
			}

			Log.d(TAG, "ori count is " + count);
			count = cursor.getCount() > count ? count : cursor.getCount();
			Log.d(TAG, "now count is " + count);
			ArrayList<HistoryItem> items = new ArrayList<HistoryItem>();

			if (!cursor.moveToFirst())
				return null;

			cursor.move(-1);
			while (cursor.moveToNext()) {
				items.add(new HistoryItem(cursor));
			}
			return items;
		}

		public static void sortSequentialItemsDesc(ArrayList<HistoryItem> items) {
			if (items != null) {
				sortItemsDesc(items, 0);
			}
			return;
		}

		private static void sortItemsDesc(ArrayList<HistoryItem> items,
				int startIndex) {
			if (startIndex >= items.size()) {
				return;
			}
			for (int i = startIndex; i < items.size() - 1; ++i) {
				if (items.get(i).mId < items.get(i + 1).mId) {
					HistoryItem item = items.get(i);
					items.set(i, items.get(i + 1));
					items.set(i + 1, item);
				}
			}
			sortItemsDesc(items, startIndex + 1);
		}
	}

	public TaxiHistorySqlHelper(Context context) {
		super(context, "history_db", null, 3);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("create table if not exists " + TABLE_NAME + "("
				+ HistoryItem.ID + " integer primary key DESC, "
				+ HistoryItem.CAR_NUMBER + " varchar not null, "
				+ HistoryItem.NICKNAME + " varchar not null, "
				+ HistoryItem.PHONE_NUMBER + " varchar not null, "
				+ HistoryItem.ORIGIN + " text, " + HistoryItem.ORIGIN_LATITUDE
				+ " REAL, " + HistoryItem.ORIGIN_LONGITUDE + " REAL, "
				+ HistoryItem.DESTINATION + " text, "
				+ HistoryItem.DESTINATION_LATITUDE + " REAL, "
				+ HistoryItem.DESTINATION_LONGITUDE + " REAL, "
				+ HistoryItem.DRIVER_EVALUATION + " REAL, "
				+ HistoryItem.PASSENGER_EVALUATION + " REAL, "
				+ HistoryItem.DRIVER_COMMENT + " text, "
				+ HistoryItem.PASSENGER_COMMENT + " text "
				+ HistoryItem.DRIVER_COMMENT_TIMESTAMP + " integer, "
				+ HistoryItem.PASSENGER_COMMENT_TIMESTAMP + " integer "
				+ HistoryItem.START_TIMESTAMP + " integer, "
				+ HistoryItem.END_TIMESTAMP + " integer, "
				+ HistoryItem.HISTORY_STATE + " integer " + HistoryItem.NEXT_ID
				+ " integer not null)");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion != newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
			onCreate(db);
		}
	}

	public void open() {
	}

	public void close() {
		super.close();
	}

	public long replaceHistory(HistoryItem item) {
		Log.d(TAG, "insertHistory: " + item.mId);
		ContentValues cv = item.getInsertContentValues();
		SQLiteDatabase db = getWritableDatabase();
		return db.replace(TABLE_NAME, null, cv);
	}

	public long insertHistory(HistoryItem item) {
		Log.d(TAG, "insertHistory: " + item.mId);
		ContentValues cv = item.getInsertContentValues();
		SQLiteDatabase db = getWritableDatabase();
		return db.insert(TABLE_NAME, null, cv);
	}

	public void updateHistory(HistoryItem item) {
		Log.d(TAG, "updateHistory: " + item.mId);
		String str = item.getUpdateCommand();
		SQLiteDatabase db = getWritableDatabase();
		db.execSQL(str);
	}

	public void deleteHistory(long historyId) {
		Log.d(TAG, "deleteHistory: " + historyId);
		SQLiteDatabase db = getWritableDatabase();
		db.delete(TABLE_NAME, HistoryItem.ID + "=" + historyId, null);
	}

	public HistoryItem queryMaxIdHistory() {
		Log.d(TAG, "queryMaxIdHistory");
		SQLiteDatabase db = getReadableDatabase();
		Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null,
				HistoryItem.ID + " DESC");
		HistoryItem item = HistoryItem.parseItemFromCursor(cursor);
		if (cursor != null) {
			cursor.close();
		}
		return item;
	}

	public HistoryItem queryHistory(long historyId) {
		Log.d(TAG, "queryHistory: " + historyId);
		String sql = "SELECT * FROM " + TABLE_NAME + " where " + HistoryItem.ID
				+ "=" + historyId;
		SQLiteDatabase db = getReadableDatabase();
		Cursor cursor = db.rawQuery(sql, null);
		HistoryItem item = HistoryItem.parseItemFromCursor(cursor);
		if (cursor != null) {
			cursor.close();
		}
		return item;
	}

	public ArrayList<HistoryItem> batchQueryHistory(long historyId, int count) {
		Log.d(TAG, "batchQueryHistory: " + historyId);
		if (count <= 0) {
			return null;
		}
		String sql = "SELECT * FROM " + TABLE_NAME + " where " + HistoryItem.ID
				+ "<=" + historyId + " ORDER BY " + HistoryItem.ID + " DESC";
		SQLiteDatabase db = getReadableDatabase();
		Cursor cursor = db.rawQuery(sql, null);
		ArrayList<HistoryItem> items = HistoryItem.parseItemFromCursor(cursor,
				count);
		if (cursor != null) {
			cursor.close();
		}
		return items;
	}
}
