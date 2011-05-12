package pt.utl.ist.mobcomp.SmartFleet.vehicle;

public class BatteryManager {

	private static BatteryManager instance;
		
	public static double MAX_CAPACITY = 10.0;
	
	public static void createInstance(VehicleActivity vehicleActivity){
		
	}
	
	public static BatteryManager getInstance(){
		if(instance == null){
			instance = new BatteryManager();
		}
		return instance;
	}
	
	private double batteryLevel;
	
	public BatteryManager(){
		this.batteryLevel = MAX_CAPACITY;
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
	}
	
}
