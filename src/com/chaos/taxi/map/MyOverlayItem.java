package com.chaos.taxi.map;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;


public abstract class MyOverlayItem extends OverlayItem {

	public MyOverlayItem(GeoPoint point, String title, String snippet) {
		super(point, title, snippet);
	}
	
	public static class MyOverlayItemParam {
		public MyOverlayItemParam() {
			
		}
	}

	abstract public void onTap();
	
	abstract public MyOverlayItemParam getParam();
}
