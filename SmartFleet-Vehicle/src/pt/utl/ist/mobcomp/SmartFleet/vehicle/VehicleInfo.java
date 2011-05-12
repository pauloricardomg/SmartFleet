package pt.utl.ist.mobcomp.SmartFleet.vehicle;

public class VehicleInfo {

	private Integer port;
    private String vID;
    private Double lat;
    private Double lon;
    private Integer alt;
    private String type;
    private String dest;
    private String pList;
    private Double bat;
    private Long time;
	
	public VehicleInfo(String vID, String ipAddress, Integer port) {
		this.vID = vID;
		this.ipAddress = ipAddress;
		this.port = port;
	}

	public String getvID() {
		return vID;
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
	
	private String ipAddress;
	public Double getLat() {
		return lat;
	}

	public void setLat(Double lat) {
		this.lat = lat;
	}

	public Double getLon() {
		return lon;
	}

	public void setLon(Double lon) {
		this.lon = lon;
	}

	public Integer getAlt() {
		return alt;
	}

	public void setAlt(Integer alt) {
		this.alt = alt;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getDest() {
		return dest;
	}

	public void setDest(String dest) {
		this.dest = dest;
	}

	public String getpList() {
		return pList;
	}

	public void setpList(String pList) {
		this.pList = pList;
	}

	public Double getBat() {
		return bat;
	}

	public void setBat(Double bat) {
		this.bat = bat;
	}

	public Long getTime() {
		return time;
	}

	public void setTime(Long time) {
		this.time = time;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

}
