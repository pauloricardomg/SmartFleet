package pt.ist.utl.mobcomp.SmartFleet.monitoring;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.xmlpull.v1.XmlPullParserException;

import pt.utl.ist.mobcomp.SmartFleet.bean.StationInfo;
import pt.utl.ist.mobcomp.SmartFleet.util.HTTPClient;
import pt.utl.ist.mobcomp.SmartFleet.util.XmlUtils;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

public class MonitoringActivity extends MapActivity {

	private MapView map=null;
	//private MyLocationOverlay me=null;
	
	SmartFleetOverlay stationsOverlay;;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		map=(MapView)findViewById(R.id.mapView);

		//Marques de Pombal
		map.getController().setCenter(getPoint(38.725137,-9.149873));
		map.getController().setZoom(17);
		map.setBuiltInZoomControls(true);
		
		List<StationInfo> activeStations = lookupStations("http://192.168.0.129:8080/GetAllStations");
		
		Drawable marker = getResources().getDrawable(R.drawable.train);
		marker.setBounds(0, 0, marker.getIntrinsicWidth(), marker.getIntrinsicHeight());
		stationsOverlay = new SmartFleetOverlay(marker, getOverlayItems(activeStations));
		
		map.getOverlays().add(stationsOverlay);

	}

	@Override
	public void onResume() {
		super.onResume();
	}		

	@Override
	public void onPause() {
		super.onPause();
	}		

 	@Override
	protected boolean isRouteDisplayed() {
		return(false);
	}

 	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_S) {
			map.setSatellite(!map.isSatellite());
			return(true);
		}
		else if (keyCode == KeyEvent.KEYCODE_Z) {
			map.displayZoomControls(true);
			return(true);
		}

		return(super.onKeyDown(keyCode, event));
	}

 	private GeoPoint getPoint(String lat, String lon){
 		double latDouble = new Double(lat);
 		double lonDouble = new Double(lon);
 		return getPoint(latDouble, lonDouble);
 	}
 	
	private GeoPoint getPoint(double lat, double lon) {
		return(new GeoPoint((int)(lat*1000000.0),
													(int)(lon*1000000.0)));
	}

	private class SmartFleetOverlay extends ItemizedOverlay<OverlayItem> {
		private List<OverlayItem> items;

		public SmartFleetOverlay(Drawable marker, List<OverlayItem> items) {
			super(marker);
			this.items = items;

			boundCenterBottom(marker);
			populate();
		}
		
		@Override
		protected OverlayItem createItem(int i) {
			return(items.get(i));
		}

		@Override
		protected boolean onTap(int i) {
			
			Toast makeText = Toast.makeText(MonitoringActivity.this,
											items.get(i).getSnippet(),
											Toast.LENGTH_LONG);
			
			
			
			makeText.show();

			return(true);
		}

		@Override
		public int size() {
			return(items.size());
		}
	}
	
	private List<OverlayItem> getOverlayItems(List<StationInfo> infos){
		List<OverlayItem> list = new LinkedList<OverlayItem>();
		
		if(infos != null){
			for (StationInfo info : infos) {
				list.add(new OverlayItem(getPoint(info.getLat(), info.getLon()),
						info.getName(),
						info.getName() + " - Station\n" +
						"Number of passengers waiting: " + info.getQueueSize() + "\n" +
						"Average wait time: " + info.getWaitTime() + "\n" + 
						"Vehicles present: " + info.getVehicles() + "\n"));
			}
		}
	
		return list;
	}
	
	private List<StationInfo> lookupStations(String url)
	{

		//Make a get Request to the server
		String response = null;
		try {
			response = HTTPClient.executeHttpGet(url);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		//Parse the received xml to see if the registration was successful or not.	
		List<StationInfo> result = null;
		try {
			result = XmlUtils.parsexml(response);
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return result;
	}
    
}
