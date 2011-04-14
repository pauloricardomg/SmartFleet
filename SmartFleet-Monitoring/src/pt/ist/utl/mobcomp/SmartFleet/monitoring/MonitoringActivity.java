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

	private static final long STATIONS_QUERY_TIME = 10000L;
	private MapView map=null;
	
	List<StationInfo> currentStations;
	SmartFleetOverlay stationsOverlay;;
	Thread thrd;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		map=(MapView)findViewById(R.id.mapView);

		//Marques de Pombal
		map.getController().setCenter(getPoint(38.725137,-9.149873));
		map.getController().setZoom(17);
		map.setBuiltInZoomControls(true);
		
		Drawable marker = getResources().getDrawable(R.drawable.train);
		marker.setBounds(0, 0, marker.getIntrinsicWidth(), marker.getIntrinsicHeight());
		stationsOverlay = new SmartFleetOverlay(marker);
		//stationsOverlay.update(new LinkedList<OverlayItem>());
		
		currentStations = null;
		
		map.getOverlays().add(stationsOverlay);

	}

	@Override
	public void onResume() {
		super.onResume();
		
		if(thrd == null){
			thrd = new Thread(new Runnable() {
				public void run() {
					while (!Thread.interrupted()) {
						List<StationInfo> activeStations = lookupStations("http://194.210.225.30:8080/GetAllStations");
						
						if(activeStations != null && !activeStations.equals(currentStations)){
							currentStations = activeStations;
							List<OverlayItem> overlayItems = getOverlayItems(activeStations);
							stationsOverlay.update(overlayItems);
						}
						
						try {
							Thread.sleep(STATIONS_QUERY_TIME);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			});
			thrd.start();
		}
	}		

	@Override
	public void onPause() {
		super.onPause();
		if (thrd != null)
			thrd.interrupt();
		thrd = null;
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		if (thrd != null)
			thrd.interrupt();
		thrd = null;
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
		private Drawable marker;
		
		public SmartFleetOverlay(Drawable marker) {
			super(marker);
			this.marker = marker;
			boundCenterBottom(this.marker);
		}
		
		
		public void update(List<OverlayItem> items){
			this.items = items;
			
		    //setLastFocusedIndex(-1);
			populate();
			map.postInvalidate();
		}
		
		@Override
		protected OverlayItem createItem(int i) {
			return(items.get(i));
		}

		@Override
		protected boolean onTap(int i) {
			
			Toast makeText = Toast.makeText(MonitoringActivity.this,
											items.get(i).getSnippet(),Toast.LENGTH_LONG);
			
			
			
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
