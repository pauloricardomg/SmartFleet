package pt.utl.ist.mobcomp.SmartFleet.vehicle;

public class BatteryManager {

	private static BatteryManager instance;
		
	public static double MAX_CAPACITY = 10.0;
	
	public static BatteryManager createInstance(String serverIP, String gpsPort, String myId){
		instance = new BatteryManager(serverIP, gpsPort, myId);
		return instance;
	}
	
	public static BatteryManager getInstance(){
		return instance;
	}
	
	private Integer alt;
	private double batteryLevel;

	private String serverIP;

	private String gpsPort;

	private String myId;
	
	public BatteryManager(String serverIP, String gpsPort, String myId){
		this.serverIP = serverIP;
		this.gpsPort = gpsPort;
		this.myId = myId;
		this.alt = 0;
		this.batteryLevel = MAX_CAPACITY;
	}
	
	
	public Integer getAlt() {
		return alt;
	}

	public synchronized double getBatteryLevel(){
		return this.batteryLevel;
	}
	
	public synchronized boolean drainBattery(double capacity){
		this.batteryLevel = Math.max(0, this.batteryLevel-capacity);
		return batteryLevel == 0;
	}
	
	public synchronized boolean rechargeBattery(double capacity){
		this.batteryLevel = Math.min(MAX_CAPACITY, this.batteryLevel+capacity);
		return batteryLevel == MAX_CAPACITY;
	}
	
	public synchronized void rechargeAll(){
		this.batteryLevel = MAX_CAPACITY;
	}
	
	public synchronized void drainAll(){
		this.batteryLevel = 0;
		while(alt != 0){
			lowerAltitude();
		}
	}
	
	
	
	//ALTITUDE
	
	public synchronized void raiseAltitude(){
		alt += 100;
		try{
			String url = String.format("http://" + this.serverIP + ":" + this.gpsPort + "/ChangeAltitude?vehicleID=" + 
					this.myId + ";alt=" +  this.alt);
			VehicleActivity.contact_server(url);
		}catch(Exception e){
			System.out.println("Exception here: "+ e.getMessage());
		}
		
		//CONSUME BATTERY
		if(drainBattery(0.2)){ //0.2KW per 100 meters
			//Means battery is over, vehicle needs to stop :(
			this.stopVehicle();
		}
	}
	
	public synchronized void lowerAltitude(){
		alt = Math.max(0, alt-100);
		try{
			String url = String.format("http://" + this + ":" + this.gpsPort + "/ChangeAltitude?vehicleID=" + 
					this.myId + ";alt=" +  this.alt);
			VehicleActivity.contact_server(url);
		}catch(Exception e){
			System.out.println("Exception here: "+ e.getMessage());
		}
	}
	
	public void stopVehicle(){
		try{
			String url = String.format("http://" + this.serverIP + ":" + this.gpsPort + "/StopVehicle?vehicleID=" + 
					this.myId);
			VehicleActivity.contact_server(url);
		}catch(Exception e){
			System.out.println("Exception here: "+ e.getMessage());
		}
	}
	
}
