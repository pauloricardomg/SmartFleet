package pt.utl.ist.mobcomp.SmartFleet.vehicle;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import pt.utl.ist.mobcomp.SmartFleet.bean.StationInfo;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

public class NavigationActivity extends MapActivity implements LocationListener {

	private MapView map=null;
	
	List<StationInfo> activeStations;
	SmartFleetOverlay vehiclesOverlay;
	SmartFleetOverlay stationsOverlay;
	String vehicleID;
	String destination;

	private LocationManager locationManager;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.nav);
		
		// Get the location manager
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		
        //get the Bundle out of the Intent...
        Bundle extras = getIntent().getExtras();
        activeStations = (List<StationInfo>)extras.get("stations");
        Location position = (Location)extras.get("position");	
        vehicleID = extras.getString("vehicleID");
        destination = extras.getString("destination");
		
		map=(MapView)findViewById(R.id.mapView);

		map.getController().setCenter(getPoint(position));
		map.getController().setZoom(17);
		//map.setBuiltInZoomControls(false);
		
		//Stations overlay
		Drawable marker = getResources().getDrawable(R.drawable.train);
		marker.setBounds(0, 0, marker.getIntrinsicWidth(), marker.getIntrinsicHeight());
		stationsOverlay = new SmartFleetOverlay(marker);
		stationsOverlay.update(getStationItems(activeStations));
		
		//Vehicles overlay
		marker = getResources().getDrawable(R.drawable.ship);
		marker.setBounds(0, 0, marker.getIntrinsicWidth(), marker.getIntrinsicHeight());
		vehiclesOverlay = new SmartFleetOverlay(marker);
		vehiclesOverlay.update(getVehicleItem(position));
		
		map.getOverlays().add(stationsOverlay);
		map.getOverlays().add(vehiclesOverlay);

	}
	
	@Override
	public void onLocationChanged(Location location) {
		List<String> parties;
		if(VehicleActivity.getStation(location, activeStations) != null){
			//RESUME VEHICLE ACTIVITY
			Intent intent = new Intent();
	    	setResult(RESULT_OK, intent);
	        finish();
	        
		} if((parties = getPartiesOfDestination(location)) != null) {
			//MOVE TO ARRIVED AT DESTINATION ACTIVITY
			Intent intent = new Intent(this, NavigationActivity.class);
			intent.putExtra("destination",destination);
			startActivityForResult(intent, 0);
			
			
			//  Notify travelers to leave when arriving at destination
			// drop parties, etc
			//if(hasEnoughBattery()){
			//	nextDest = selectNextDest()
			//	gpsEmulator.moveTo(vehicleId, nextDest)
			//} else {
			//	returnToClosestStation()
			//}
		} else {
			vehiclesOverlay.update(getVehicleItem(location));
			map.getController().setCenter(getPoint(location));
			//show.setText("Lat: " + String.valueOf(location.getLatitude()) + "Lon: " + String.valueOf(location.getLongitude()));
			//still moving
			// Display current position on map and expected time of arrival during flight	
			//drainBattery()
		}
	}
	
	
	//TODO: Implement this method
	private List<String> getPartiesOfDestination(Location location) {
		// TODO Auto-generated method stub
		return null;
	}

	/* Request updates at startup */
	@Override
	protected void onResume() {
		super.onResume();
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 400, 1, this);
	}

	/* Remove the locationlistener updates when Activity is paused */
	@Override
	protected void onPause() {
		super.onPause();
		locationManager.removeUpdates(this);
	}

 	@Override
	protected boolean isRouteDisplayed() {
		return(false);
	}
 	
	private GeoPoint getPoint(Location location) {
		return(new GeoPoint((int)(location.getLatitude()*1000000.0),
													(int)(location.getLongitude()*1000000.0)));
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
			
			Toast makeText = Toast.makeText(NavigationActivity.this,
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
	
	private List<OverlayItem> getVehicleItem(Location location){
		List<OverlayItem> list = new LinkedList<OverlayItem>();
		
		list.add(new OverlayItem(getPoint(location), vehicleID,
				"Vehicle: " + vehicleID + "\n" +
		"Battery Level: 0"));
		
		if(VehicleActivity.inRange.size() > 0){
			double lat, lon;
			for (String otherVID : new ArrayList<String>(VehicleActivity.inRange)) {
				VehicleInfo info = VehicleActivity.learnedVehicles.get(otherVID);
				if(info.getLat() != null && info.getLon() != null){
					lat = info.getLat();
					lon = info.getLon();
					list.add(new OverlayItem(getPoint(lat, lon), otherVID,
							"Vehicle: " + otherVID + "\n"));
				}
			}
		}
	
		return list;
	}


	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}
    
}
