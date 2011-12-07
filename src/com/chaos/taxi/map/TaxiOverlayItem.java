package com.chaos.taxi.map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.chaos.taxi.RequestProcessor;
import com.chaos.taxi.TaxiActivity;
import com.chaos.taxi.WaitTaxiActivity;
import com.google.android.maps.GeoPoint;

public class TaxiOverlayItem extends MyOverlayItem {
	private static final String TAG = "TaxiOverlayItem";
	private Context mContext = null;

	public TaxiOverlayItemParam mParam;

	public static class TaxiOverlayItemParam extends MyOverlayItemParam
			implements Parcelable {
		public GeoPoint mPoint = null;
		public String mCarNumber = null;
		public String mPhoneNumber = null;
		public String mNickName = null;

		public TaxiOverlayItemParam(GeoPoint point, String carNumber,
				String phoneNumber, String nickName) {
			mPoint = point;
			mCarNumber = carNumber;
			mPhoneNumber = phoneNumber;
			mNickName = nickName;
		}

		public int describeContents() {
			return 0;
		}

		public void writeToParcel(Parcel dest, int flags) {
			dest.writeInt(mPoint.getLatitudeE6());
			dest.writeInt(mPoint.getLongitudeE6());
			dest.writeString(mCarNumber);
			dest.writeString(mPhoneNumber);
			dest.writeString(mNickName);
		}

		public static final Parcelable.Creator<TaxiOverlayItemParam> CREATOR = new Creator<TaxiOverlayItemParam>() {
			public TaxiOverlayItemParam createFromParcel(Parcel source) {
				int latitude = source.readInt();
				int longitude = source.readInt();
				String carNumber = source.readString();
				String phoneNumber = source.readString();
				String nickName = source.readString();
				TaxiOverlayItemParam param = new TaxiOverlayItemParam(
						new GeoPoint(latitude, longitude), carNumber,
						phoneNumber, nickName);
				return param;
			}

			public TaxiOverlayItemParam[] newArray(int size) {
				return null;
			}
		};
	}

	public TaxiOverlayItem(Context context, TaxiOverlayItemParam param,
			boolean isMyCar) {
		super(param.mPoint, "Taxi", "CarNumber is " + param.mCarNumber
				+ "\nPhoneNumber is " + param.mPhoneNumber + "\nDriverName is "
				+ param.mNickName);
		Log.d(TAG, "CarNumber is " + param.mCarNumber + "PhoneNumber is "
				+ param.mPhoneNumber + "NickName is " + param.mNickName);
		mContext = context;
		mParam = param;
		if (isMyCar) {
			// this.setMarker(mContext.getResources().getDrawable(R.drawable.my_car));
		} else {
			// this.setMarker(mContext.getResources().getDrawable(R.drawable.car));
		}
	}

	@Override
	public TaxiOverlayItemParam getParam() {
		return mParam;
	}

	@Override
	public void onTap() {
		Log.d(TAG, "onTap");
		AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
		dialog.setTitle("Taxi");
		dialog.setMessage("CarNumber is " + mParam.mCarNumber
				+ "\nPhoneNumber is " + mParam.mPhoneNumber + "\nNickName is "
				+ mParam.mNickName);
		if (!RequestProcessor.hasTaxi()) {
			dialog.setPositiveButton("CallTaxi", new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					RequestProcessor.setMyTaxiParam(mParam.mCarNumber,
							mParam.mNickName, mParam.mPhoneNumber,
							mParam.mPoint);
					RequestProcessor.callTaxi(mParam.mPhoneNumber);
				}
			});
		}
		dialog.setNegativeButton("Return", null);
		dialog.show();
	}
}
