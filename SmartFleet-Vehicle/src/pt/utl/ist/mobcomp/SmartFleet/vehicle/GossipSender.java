package pt.utl.ist.mobcomp.SmartFleet.vehicle;

import java.util.ArrayList;

import android.location.Location;
import android.location.LocationManager;


public class GossipSender implements Runnable {

	private VehicleActivity vehicleActivity;
	private LocationManager lm;
	final private String myvID;
	private boolean started;

	public GossipSender(VehicleActivity vActivity, LocationManager lm){
		this.vehicleActivity = vActivity;
		this.lm = lm;
		this.myvID = vehicleActivity.getId();
		this.started = false;
	}
	
	public void stop(){
		started = false;
	}
	
	@Override
	public void run() {
		
		this.started = true;
		while(this.started){
			
			try {
				if(vehicleActivity.getInRange().size() > 0){
					Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
					double lon = location.getLongitude();
					double lat = location.getLatitude();

					Integer alt = vehicleActivity.getAlt();
					String dest = vehicleActivity.getDestination();
					String pList = vehicleActivity.getPassengerList();
					Double bat = vehicleActivity.getBattery();
					for (String inRange : new ArrayList<String>(vehicleActivity.getInRange())) {
						VehicleInfo info = vehicleActivity.getLearnedVehicles().get(inRange);
						Runnable gossip = new WebserviceClient("gossip", info.getIpAddress(), info.getPort(), myvID, lat, lon, alt, dest, pList, bat, System.currentTimeMillis());
						gossip.run();
					}
				}
				Thread.sleep(1000L);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}

}
