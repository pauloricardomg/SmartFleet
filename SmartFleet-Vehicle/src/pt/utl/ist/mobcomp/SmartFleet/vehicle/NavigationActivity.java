package pt.utl.ist.mobcomp.SmartFleet.vehicle;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import pt.utl.ist.mobcomp.SmartFleet.bean.StationInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

public class NavigationActivity extends MapActivity implements LocationListener {

	private static Double VEHICLE_SPEED = 10.0;
	
	private MapView map=null;
	
	List<StationInfo> activeStations;
	SmartFleetOverlay vehiclesOverlay;
	SmartFleetOverlay stationsOverlay;
	SmartFleetOverlay crashedOverlay;
	
	String vehicleID;
	String destination;
	Location currDest;
	String pList;
	String allPassengers;
	String passengersNotForNextDest;

	private LocationManager locationManager;

	private VehicleManager manager;

	private TextView textAlt;

	private TextView textBatt;

	private TextView textPart;

	private TextView textTime;

	private BatteryManager battMan;

	private TextView textDest;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.nav);
		this.manager = VehicleManager.getInstance();
		this.battMan = BatteryManager.getInstance();
		
		textDest = (TextView) this.findViewById(R.id.dest);
		textAlt = (TextView) this.findViewById(R.id.alt);
		textBatt = (TextView) this.findViewById(R.id.batt);
		textPart = (TextView) this.findViewById(R.id.parties);
		textTime = (TextView) this.findViewById(R.id.time);
		
		// Get the location manager
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		
        //get the Bundle out of the Intent...
        Bundle extras = getIntent().getExtras();
        activeStations = (List<StationInfo>)extras.get("stations");
        Location position = (Location)extras.get("position");	
        vehicleID = extras.getString("vehicleID");
        destination = extras.getString("destination");
        double lat = extras.getDouble("lat");
        double lon = extras.getDouble("lon");
        pList = extras.getString("pList"); 
        allPassengers = extras.getString("allPassengers");
        passengersNotForNextDest = extras.getString("passengersNotForNextDest");
        
		textPart.setText("Parties: " + this.allPassengers == null? "None" : this.allPassengers);
        textDest.setText("Destination: " + destination);
        
        currDest = new Location(LocationManager.GPS_PROVIDER);
        currDest.setLatitude(lat);
        currDest.setLongitude(lon);
        	
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
		vehiclesOverlay.update(getVehicleItems(position));
		
		//Crashed overlay
		marker = getResources().getDrawable(R.drawable.crashed);
		marker.setBounds(0, 0, marker.getIntrinsicWidth(), marker.getIntrinsicHeight());
		crashedOverlay = new SmartFleetOverlay(marker);
		crashedOverlay.update(getCrashedItems(position));		
		
		map.getOverlays().add(stationsOverlay);
		map.getOverlays().add(vehiclesOverlay);
		map.getOverlays().add(crashedOverlay);
	}
	
	@Override
	public void onLocationChanged(Location location) {
		if(VehicleActivity.getStation(location, activeStations) != null){
			
			if(pList!= null)
			{
				Intent intent = new Intent(this, Arrival.class);
				intent.putExtra("destination",destination);
				intent.putExtra("passenger", pList );
				intent.putExtra("station", true);
				startActivityForResult(intent, 1);
			}
			
			else
			{
				//Go back to station screen
	            Intent intent1 = new Intent();
		    	setResult(RESULT_OK, intent1);
		        finish();
			}
			return;	
		} 
		Boolean reachedDestination = getPartiesOfDestination(location);
	    if(reachedDestination) {
			
			if(pList!= null)
			{
			//MOVE TO ARRIVED AT DESTINATION ACTIVITY
			Log.d("ScreenChange", "ARRIVAL SCREEN");
			Intent intent = new Intent(this, Arrival.class);
			intent.putExtra("destination",destination);
			intent.putExtra("passenger", pList );
			intent.putExtra("station", false);
			startActivityForResult(intent, 1);
			}
			
			else
			{
				//Go back to station screen
	            Intent intent1 = new Intent();
		    	setResult(RESULT_OK, intent1);
		        finish();
			}
			
			//  Notify travelers to leave when arriving at destination
			// drop parties, etc
			//if(hasEnoughBattery()){
			//	nextDest = selectNextDest()
			//	gpsEmulator.moveTo(vehicleId, nextDest)
			//} else {
			//	returnToClosestStation()
			//}
		} else {
			vehiclesOverlay.update(getVehicleItems(location));
			crashedOverlay.update(getCrashedItems(location));
			map.getController().setCenter(getPoint(location));
			
			float distanceTo = location.distanceTo(currDest);
			//Toast.makeText(NavigationActivity.this, "Distance to location: " + distanceTo + "Km",Toast.LENGTH_LONG);
			
			int timeToReach = (int)(distanceTo/VEHICLE_SPEED);
			
			textBatt.setText("Battery: " + (int)(battMan.getBatteryLevel() * 1000) + "W");
			textAlt.setText("Altitude: " + battMan.getAlt());
			textTime.setText("Time to dest: " + timeToReach + "s (" + (int)distanceTo + "m)");
			
			//show.setText("Lat: " + String.valueOf(location.getLatitude()) + "Lon: " + String.valueOf(location.getLongitude()));
			//still moving
			// Display current position on map and expected time of arrival during flight	
			//drainBattery()
		}
	}
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("CheckStartActivity","onActivityResult and resultCode = "+resultCode);
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==1){
            Toast.makeText(this, "Destination is a Station", Toast.LENGTH_LONG).show();
            
            //Go back to station screen
            Intent intent1 = new Intent();
	    	setResult(RESULT_OK, intent1);
	        finish();
	        
        }
        else{
        	//stay on navigation screen! but we just go back to the main screen and call a different method
            Toast.makeText(this, "Destination not a station", Toast.LENGTH_LONG).show();
            Intent intent1 = new Intent();
	    	setResult(RESULT_OK, intent1);
	        finish();
        }
    }

	
	private Boolean getPartiesOfDestination(Location location) {
		// TODO Auto-generated method stub
		if(location.distanceTo(currDest) < VEHICLE_SPEED) //TODO: Change it to 10m.
		{
			return true;
		/*if (pList!= null && pList.length() > 0)
			return true;
		else
			return false;*/
		}
		else
			return false;
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
						info.getName() + " - Station"));
			}
		}
	
		return list;
	}
	
	private List<OverlayItem> getVehicleItems(Location location){
		List<OverlayItem> list = new LinkedList<OverlayItem>();
		
		list.add(new OverlayItem(getPoint(location), vehicleID,
				"Vehicle: " + vehicleID));
		
		if(manager.getInRange().size() > 0){
			double lat, lon;
			for (String otherVID : new ArrayList<String>(manager.getInRange())) {
				VehicleInfo info = manager.getLearnedVehicles().get(otherVID);
				if(info.getLat() != null && info.getLon() != null && !info.getBat().equals(0.0)){
					lat = info.getLat();
					lon = info.getLon();
					list.add(new OverlayItem(getPoint(lat, lon), otherVID,
							"Vehicle: " + otherVID + "" +
							"Battery: " + info.getBat() + "\n"));
				}
			}
		}
	
		return list;
	}
	
	private List<OverlayItem> getCrashedItems(Location location){
		List<OverlayItem> list = new LinkedList<OverlayItem>();
		
		if(manager.getInRange().size() > 0){
			double lat, lon;
			for (String otherVID : new ArrayList<String>(manager.getInRange())) {
				VehicleInfo info = manager.getLearnedVehicles().get(otherVID);
				if(info.getLat() != null && info.getLon() != null && info.getBat().equals(0.0)){
					lat = info.getLat();
					lon = info.getLon();
					list.add(new OverlayItem(getPoint(lat, lon), otherVID,
							"Vehicle: " + otherVID + "" +
							"Battery: " + info.getBat() + "\n"));
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
	
	class BatteryOffReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			
		}
		
	}
    
}
