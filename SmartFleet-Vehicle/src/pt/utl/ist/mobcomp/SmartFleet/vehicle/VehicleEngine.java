package pt.utl.ist.mobcomp.SmartFleet.vehicle;

import java.util.ArrayList;

import android.location.Location;
import android.location.LocationManager;


public class VehicleEngine implements Runnable {

	public static Double BATTERY_DRAIN_RATE = BatteryManager.MAX_CAPACITY/3600;
	
	private VehicleActivity vehicleActivity;
	private LocationManager lm;
	final private String myvID;
	private boolean started;
	private VehicleManager vehicMan;
	private BatteryManager battMan;
	private boolean moving;


	public VehicleEngine(VehicleActivity vActivity, LocationManager lm){
		this.vehicleActivity = vActivity;
		this.lm = lm;
		this.myvID = vehicleActivity.getId();
		this.started = false;
		this.vehicMan = VehicleManager.getInstance();
		this.battMan = BatteryManager.getInstance();
		this.moving = false;
	}
	
	public void stop(){
		started = false;
	}
	
	public void startMoving(){
		this.moving = true;
	}
	
	public void stopMoving(){
		this.moving = false;
	}
	
	@Override
	public void run() {
		
		this.started = true;
		while(this.started){
			
			try {
				//Send gossip to nearby vehicles
				if(vehicMan.getInRange().size() > 0){
					Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
					double lon = location.getLongitude();
					double lat = location.getLatitude();

					Integer alt = vehicleActivity.getAlt();
					String dest = vehicleActivity.getDestination();
					String pList = vehicleActivity.getPassengerList();
					Double bat = battMan.getBatteryLevel();
					for (String inRange : new ArrayList<String>(vehicMan.getInRange())) {
						VehicleInfo info = vehicMan.getLearnedVehicles().get(inRange);
						Runnable gossip = new WebserviceClient("gossip", info.getIpAddress(), info.getPort(), myvID, lat, lon, alt, dest, pList, bat, System.currentTimeMillis());
						gossip.run();
					}
				}
				
				if(moving && battMan.getBatteryLevel() > 0 && battMan.drainBattery(BATTERY_DRAIN_RATE)){
					//Means battery is over, vehicle needs to stop :(
					vehicleActivity.stopVehicle();
				}
				
				Thread.sleep(1000L);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}

}
