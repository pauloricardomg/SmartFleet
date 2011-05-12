package pt.utl.ist.mobcomp.SmartFleet.vehicle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VehicleManager {

	private static VehicleManager instance;
	
	public static VehicleManager getInstance(){
		if(instance == null){
			instance = new VehicleManager();
		}
		return instance;
	}
	
	private HashMap<String, VehicleInfo> learnedVehicles;
	private List<String> inRange;
	
	public VehicleManager(){
		learnedVehicles = new HashMap<String, VehicleInfo>();
		inRange = new ArrayList<String>();
	}

	public HashMap<String, VehicleInfo> getLearnedVehicles() {
		return learnedVehicles;
	}

	public List<String> getInRange() {
		return inRange;
	}
	
}
