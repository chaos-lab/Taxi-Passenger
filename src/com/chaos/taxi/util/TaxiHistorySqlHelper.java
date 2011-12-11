package com.chaos.taxi.util;

import java.io.IOException;
import java.util.List;

import com.google.android.maps.GeoPoint;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

public class TaxiHistorySqlHelper extends SQLiteOpenHelper {
	private static final String TAG = "TaxiHistorySqlHelper";
	SQLiteDatabase mSQLiteDatabase = null;
	static final String TABLE_NAME = "taxi_history";
	static final String ID = "id";
	static final String CAR_NUMBER = "car_number";
	static final String NICKNAME = "nickname";
	static final String PHONE_NUMBER = "phoneNumber";
	static final String LOCATION = "location";
	static final String DESTINATION = "destination";
	static final String DRIVER_EVALUATION = "driver_eval";
	static final String PASSENGER_EVALUATION = "passenger_eval";
	static final String DRIVER_COMMENT = "driver_comment";
	static final String PASSENGER_COMMENT = "passenger_comment";
	static final String START_TIMESTAMP = "start_timestamp";
	static final String END_TIMESTAMP = "end_timestamp";

	static class HistoryItem {
		int mId;
		String mCarNumber;
		String mNickName;
		String mPhoneNumber;
		String mLocation;
		String mDestination;
		int mDriverEvaluation;
		int mPassengerEvaluation;
		String mDriverComment;
		String mPassengerComment;
		int mStartTimeStamp;
		int mEndTimeStamp;

		static Geocoder mGeocoder = null;

		public HistoryItem(Context context, int id, String carNumber,
				String nickName, String phoneNumber, int locationLatitude,
				GeoPoint locationPoint, GeoPoint destinationPoint,
				int driverEvaluation, int passengerEvaluation,
				String driverComment, String passengerComment,
				int startTimeStamp, int endTimeStamp) {
			if (mGeocoder != null) {
				mGeocoder = new Geocoder(context);
			}
			mId = id;
			mCarNumber = carNumber;
			mNickName = nickName;
			mPhoneNumber = phoneNumber;
			mLocation = queryLocationName(locationPoint);
			mDestination = queryLocationName(destinationPoint);
			mDriverEvaluation = driverEvaluation;
			mPassengerEvaluation = passengerEvaluation;
			mDriverComment = driverComment;
			mPassengerComment = passengerComment;
			mStartTimeStamp = startTimeStamp;
			mEndTimeStamp = endTimeStamp;
		}

		public HistoryItem(int id, String carNumber, String nickName,
				String phoneNumber, String location, String destination,
				int driverEvaluation, int passengerEvaluation,
				String driverComment, String passengerComment,
				int startTimeStamp, int endTimeStamp) {
			mId = id;
			mCarNumber = carNumber;
			mNickName = nickName;
			mPhoneNumber = phoneNumber;
			mLocation = location;
			mDestination = destination;
			mDriverEvaluation = driverEvaluation;
			mPassengerEvaluation = passengerEvaluation;
			mDriverComment = driverComment;
			mPassengerComment = passengerComment;
			mStartTimeStamp = startTimeStamp;
			mEndTimeStamp = endTimeStamp;
		}

		public HistoryItem(Cursor cursor) {
			this(cursor.getInt(cursor.getColumnIndex(ID)), cursor
					.getString(cursor.getColumnIndex(CAR_NUMBER)), cursor
					.getString(cursor.getColumnIndex(NICKNAME)), cursor
					.getString(cursor.getColumnIndex(PHONE_NUMBER)), cursor
					.getString(cursor.getColumnIndex(LOCATION)), cursor
					.getString(cursor.getColumnIndex(DESTINATION)), cursor
					.getInt(cursor.getColumnIndex(DRIVER_EVALUATION)), cursor
					.getInt(cursor.getColumnIndex(PASSENGER_EVALUATION)),
					cursor.getString(cursor.getColumnIndex(DRIVER_COMMENT)),
					cursor.getString(cursor.getColumnIndex(PASSENGER_COMMENT)),
					cursor.getInt(cursor.getColumnIndex(START_TIMESTAMP)),
					cursor.getInt(cursor.getColumnIndex(END_TIMESTAMP)));
		}

		private String queryLocationName(GeoPoint point) {
			if (mGeocoder == null) {
				Log.e(TAG, "mGeocoder is still null!");
				return null;
			}

			List<Address> addrList = null;
			try {
				addrList = mGeocoder.getFromLocation(
						point.getLatitudeE6() / 1000000.0,
						point.getLongitudeE6() / 1000000.0, 1);
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (addrList != null && addrList.size() > 0) {
				StringBuilder sb = new StringBuilder();
				Address addr = addrList.get(0);
				if (addr.getAddressLine(1) != null)
					sb.append(addr.getAddressLine(1)).append(" ");
				if (addr.getAddressLine(2) != null)
					sb.append(addr.getAddressLine(2)).append(" ");
				Log.d(TAG, "got location name: " + sb.toString() + " for "
						+ point.getLatitudeE6() + ", " + point.getLongitudeE6());
				return sb.toString();
			}
			return null;
		}

		public ContentValues getInsertContentValues() {
			ContentValues cv = new ContentValues();
			cv.put(ID, mId);
			cv.put(CAR_NUMBER, mCarNumber);
			cv.put(NICKNAME, mNickName);
			cv.put(PHONE_NUMBER, mPhoneNumber);
			cv.put(LOCATION, mLocation);
			cv.put(DESTINATION, mDestination);
			cv.put(DRIVER_EVALUATION, mDriverEvaluation);
			cv.put(PASSENGER_EVALUATION, mPassengerEvaluation);
			cv.put(DRIVER_COMMENT, mDriverComment);
			cv.put(PASSENGER_COMMENT, mPassengerComment);
			cv.put(START_TIMESTAMP, mStartTimeStamp);
			cv.put(END_TIMESTAMP, mEndTimeStamp);
			return cv;
		}

		public String getUpdateCommand() {
			String str = "UPDATE " + TABLE_NAME + " SET " + CAR_NUMBER + "="
					+ mCarNumber + " " + NICKNAME + "=" + mNickName + " "
					+ PHONE_NUMBER + "=" + mPhoneNumber + " " + LOCATION + "="
					+ mLocation + " " + DESTINATION + "=" + mDestination + " "
					+ DRIVER_EVALUATION + "=" + mDriverEvaluation + " "
					+ PASSENGER_EVALUATION + "=" + mPassengerEvaluation + " "
					+ DRIVER_COMMENT + "=" + mDriverComment + " "
					+ PASSENGER_COMMENT + "=" + mPassengerComment + " "
					+ START_TIMESTAMP + "=" + mStartTimeStamp + " "
					+ END_TIMESTAMP + "=" + mEndTimeStamp + " WHERE " + ID
					+ "=" + mId;
			Log.d(TAG, "update command is " + str);
			return str;
		}

		public static HistoryItem parseItemFromCursor(Cursor cursor) {
			if (cursor == null || cursor.getCount() == 0) {
				Log.e(TAG, "Not enough content in cursor: " + cursor);
				return null;
			}

			cursor.moveToFirst();
			HistoryItem item = new HistoryItem(cursor);
			return item;
		}

		public static HistoryItem[] parseItemFromCursor(Cursor cursor, int count) {
			if (cursor == null || cursor.getCount() == 0) {
				Log.e(TAG, "Not enough content in cursor: " + cursor);
				return null;
			}

			Log.d(TAG, "ori count is " + count);
			count = cursor.getCount() > count ? count : cursor.getCount();
			Log.d(TAG, "now count is " + count);
			HistoryItem[] items = new HistoryItem[count];

			cursor.moveToFirst();
			for (int i = 0; i < count; ++i) {
				items[i] = new HistoryItem(cursor);
				cursor.moveToNext();
			}
			return items;
		}
	}

	public TaxiHistorySqlHelper(Context context, String name,
			CursorFactory factory, int version) {
		super(context, name, factory, version);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("create table if not exists " + TABLE_NAME + "(" + ID
				+ " integer primary key DESC, " + CAR_NUMBER
				+ " varchar not null, " + NICKNAME + " varchar not null, "
				+ PHONE_NUMBER + " varchar not null, " + LOCATION
				+ " text not null, " + DESTINATION + " text, "
				+ DRIVER_EVALUATION + " integer, " + PASSENGER_EVALUATION
				+ " integer, " + DRIVER_COMMENT + " text, " + START_TIMESTAMP
				+ " integer, " + END_TIMESTAMP + " integer, "
				+ PASSENGER_COMMENT + " text)");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// do nothing
	}

	public void open() {
		mSQLiteDatabase = getWritableDatabase();
	}

	public void close() {
		super.close();
	}

	public void insertHistory(HistoryItem item) {
		Log.d(TAG, "insertHistory: " + item.mId);
		ContentValues cv = item.getInsertContentValues();
		mSQLiteDatabase.insert(TABLE_NAME, null, cv);
	}

	public void updateHistory(HistoryItem item) {
		Log.d(TAG, "updateHistory: " + item.mId);
		String str = item.getUpdateCommand();
		mSQLiteDatabase.execSQL(str);
	}

	public void deleteHistory(int historyId) {
		Log.d(TAG, "deleteHistory: " + historyId);
		mSQLiteDatabase.delete(TABLE_NAME, ID + "=" + historyId, null);
	}

	public HistoryItem queryHistory(int historyId) {
		Log.d(TAG, "queryHistory: " + historyId);
		String sql = "SELECT * FROM " + TABLE_NAME + " where " + ID + "="
				+ historyId;
		Cursor cursor = mSQLiteDatabase.rawQuery(sql, null);
		return HistoryItem.parseItemFromCursor(cursor);
	}

	public HistoryItem[] batchQueryHistory(int historyId, int count) {
		Log.d(TAG, "batchQueryHistory: " + historyId);
		String sql = "SELECT * FROM " + TABLE_NAME + " where " + ID + "="
				+ historyId;
		Cursor cursor = mSQLiteDatabase.rawQuery(sql, null);
		return HistoryItem.parseItemFromCursor(cursor, count);
	}
}
