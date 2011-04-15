package pt.ist.utl.mobcomp.SmartFleet.monitoring;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import pt.utl.ist.mobcomp.SmartFleet.bean.StationInfo;
import pt.utl.ist.mobcomp.SmartFleet.bean.VehicleInfo;
import pt.utl.ist.mobcomp.SmartFleet.util.LookupUtils;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

public class MonitoringActivity extends MapActivity {

	private MapView map=null;
	
	List<StationInfo> currentStations;
	List<VehicleInfo> currentVehicles;
	SmartFleetOverlay stationsOverlay;
	SmartFleetOverlay vehiclesOverlay;
	Thread stationsThread;
	Thread vehiclesThread;
	
	String ip;
	String port;
	long stationPeriod;
	long vehiclePeriod;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		Properties properties = LookupUtils.readPropertiesFile(this.getResources().getAssets(), "monitoring.properties");
	    
	    ip = properties.getProperty("server_ip");
	    port = properties.getProperty("server_port");
	    stationPeriod = new Long(properties.getProperty("station_update_period"));
	    vehiclePeriod = new Long(properties.getProperty("vehicle_update_period"));  
		
		map=(MapView)findViewById(R.id.mapView);

		//Marques de Pombal
		map.getController().setCenter(getPoint(38.725137,-9.149873));
		map.getController().setZoom(17);
		map.setBuiltInZoomControls(true);
		
		//Stations overlay
		Drawable marker = getResources().getDrawable(R.drawable.train);
		marker.setBounds(0, 0, marker.getIntrinsicWidth(), marker.getIntrinsicHeight());
		stationsOverlay = new SmartFleetOverlay(marker);
		//stationsOverlay.update(new LinkedList<OverlayItem>());
		
		//Vehicles overlay
		marker = getResources().getDrawable(R.drawable.ship);
		marker.setBounds(0, 0, marker.getIntrinsicWidth(), marker.getIntrinsicHeight());
		vehiclesOverlay = new SmartFleetOverlay(marker);
		
		currentStations = null;
		currentVehicles = null;
		
		map.getOverlays().add(stationsOverlay);
		map.getOverlays().add(vehiclesOverlay);

	}
	


	@Override
	public void onResume() {

		if(stationsThread == null){
			stationsThread = new Thread(getStationMonitor());
			stationsThread.start();
		}
		
		if(vehiclesThread == null){
			vehiclesThread = new Thread(getVehicleMonitor());
			vehiclesThread.start();
		}
		
		super.onResume();
	}

	private Runnable getStationMonitor() {
		return new Runnable() {
			public void run() {
				while (!Thread.interrupted()) {
					List<StationInfo> activeStations = LookupUtils.lookupStations("http://" + ip + ":" + port + "/GetAllStations");
					
					if(activeStations != null && !activeStations.equals(currentStations)){
						currentStations = activeStations;
						List<OverlayItem> overlayItems = getStationItems(activeStations);
						stationsOverlay.update(overlayItems);
					}
					
					try {
						Thread.sleep(stationPeriod);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		};
	}		
	
	private Runnable getVehicleMonitor() {
		return new Runnable() {
			public void run() {
				while (!Thread.interrupted()) {
					List<VehicleInfo> activeVehicles = LookupUtils.lookupVehicles("http://" + ip + ":" + port + "/GetAllVehicles");
					
					if(activeVehicles != null && !activeVehicles.equals(currentVehicles)){
						currentVehicles = activeVehicles;
						List<OverlayItem> overlayItems = getVehicleItems(activeVehicles);
						vehiclesOverlay.update(overlayItems);
					}
					
					try {
						Thread.sleep(vehiclePeriod);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		};
	}

	@Override
	public void onPause() {
		if (stationsThread != null)
			stationsThread.interrupt();
		if (vehiclesThread != null)
			vehiclesThread.interrupt();
		vehiclesThread = null;		
		stationsThread = null;
		super.onPause();
	}
	
	@Override
	public void onDestroy(){
		if (stationsThread != null)
			stationsThread.interrupt();
		stationsThread = null;
		if (vehiclesThread != null)
			vehiclesThread.interrupt();
		vehiclesThread = null;
		super.onDestroy();
	}

 	@Override
	protected boolean isRouteDisplayed() {
		return(false);
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
			this.items = new LinkedList<OverlayItem>();
			boundCenterBottom(this.marker);
			populate();
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
	
	private List<OverlayItem> getStationItems(List<StationInfo> infos){
		List<OverlayItem> list = new LinkedList<OverlayItem>();
		
		if(infos != null){
			for (StationInfo info : infos) {
				list.add(new OverlayItem(getPoint(info.getLat(), info.getLon()),
						info.getName(),
						info.getName() + " - Station\n" +
		
						"Number of passengers waiting: " + info.getQueueSize() + "\n" +
						"Average wait time: " + info.getWaitTime() + "s \n" + 
						"Vehicles present: " + info.getVehicles() + "\n"));
			}
		}
	
		return list;
	}
	
	private List<OverlayItem> getVehicleItems(List<VehicleInfo> infos){
		List<OverlayItem> list = new LinkedList<OverlayItem>();
		
		if(infos != null){
			for (VehicleInfo info : infos) {
				list.add(new OverlayItem(getPoint(info.getLat(), info.getLon()), info.getId(),
						"Vehicle: " + info.getId() + "\n" +
						"Battery Level: " + info.getBattLevel()));
			}
		}
	
		return list;
	}
    
}
