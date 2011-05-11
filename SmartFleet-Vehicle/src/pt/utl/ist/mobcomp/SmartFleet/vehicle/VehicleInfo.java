package pt.utl.ist.mobcomp.SmartFleet.vehicle;

public class VehicleInfo {
	
	String ipAddress;
	Integer port;
	
	public VehicleInfo(String ipAddress, Integer port) {
		this.ipAddress = ipAddress;
		this.port = port;
	}

	/**
	 * @return the ipAddress
	 */
	public String getIpAddress() {
		return ipAddress;
	}

	/**
	 * @return the port
	 */
	public Integer getPort() {
		return port;
	}
	
	

}
