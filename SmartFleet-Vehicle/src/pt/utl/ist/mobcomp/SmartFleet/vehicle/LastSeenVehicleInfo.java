package pt.utl.ist.mobcomp.SmartFleet.vehicle;

public class LastSeenVehicleInfo {
	
	String ipAddress;
	Integer port;
	String lat;
	String lon;
	String alt;
	String timeStamp;
	
	public LastSeenVehicleInfo(String ipAddress, Integer port, String lat,
			String lon, String alt, String timeStamp) {
		super();
		this.ipAddress = ipAddress;
		this.port = port;
		this.lat = lat;
		this.lon = lon;
		this.alt = alt;
		this.timeStamp = timeStamp;
	}
	

}
