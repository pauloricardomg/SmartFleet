package pt.utl.ist.mobcomp.SmartFleet.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import pt.utl.ist.mobcomp.SmartFleet.bean.StationInfo;
import pt.utl.ist.mobcomp.SmartFleet.bean.VehicleInfo;


public class XmlUtils {

	public static List<VehicleInfo> parseVehicleInfos(String xmlTest) throws XmlPullParserException, IOException {

		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
		XmlPullParser xpp = factory.newPullParser();
		byte[] bytes = xmlTest.getBytes();
		InputStream xml = new ByteArrayInputStream(bytes);
		xpp.setInput(xml, null);

		List<VehicleInfo> infos = null;
		
		VehicleInfo currentInfo = null;
		String attr = null;
		String name;
		
		while (true) {
			int event = xpp.next();
			switch(event){
	
			case XmlPullParser.START_TAG:
				name = xpp.getName();
				
				if(name.equals("Vehicle")){
					currentInfo = new VehicleInfo();
					infos.add(currentInfo);
				} else if(name.equals("VehicleInfo")){
					infos = new LinkedList<VehicleInfo>();
				} else {
					attr = name;
				} 
				break;
				
			case XmlPullParser.END_TAG:
				name = xpp.getName();
				
				if(name.equals("Vehicle")){
					currentInfo = null;
				} else if(!name.equals("VehicleInfo")){
					attr = null;
				}
				break;
				
			case XmlPullParser.TEXT:
				if(currentInfo != null && attr != null){
					String text = xpp.getText();
					if(attr.equals("id")){
						currentInfo.setId(text);
					} else if(attr.equals("lat")){
						currentInfo.setLat(text);
					} else if(attr.equals("lon")){
						currentInfo.setLon(text);
					} else if(attr.equals("battLevel")){
						int battLevel = new Integer(text);
						currentInfo.setBattLevel(battLevel);
					}
				}
				break;
				
			case XmlPullParser.END_DOCUMENT:
				return infos;
			
			}
		}

	}
	
	public static List<StationInfo> parseStationInfos(String xmlTest) throws XmlPullParserException, IOException {

		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
		XmlPullParser xpp = factory.newPullParser();
		byte[] bytes = xmlTest.getBytes();
		InputStream xml = new ByteArrayInputStream(bytes);
		xpp.setInput(xml, null);

		List<StationInfo> infos = null;
		
		StationInfo currentInfo = null;
		String attr = null;
		String name;
		
		while (true) {
			int event = xpp.next();
			switch(event){
	
			case XmlPullParser.START_TAG:
				name = xpp.getName();
				
				if(name.equals("Station")){
					currentInfo = new StationInfo();
					infos.add(currentInfo);
				} else if(name.equals("StationInfo")){
					infos = new LinkedList<StationInfo>();
				} else {
					attr = name;
				} 
				break;
				
			case XmlPullParser.END_TAG:
				name = xpp.getName();
				
				if(name.equals("Station")){
					currentInfo = null;
				} else if(!name.equals("StationInfo")){
					attr = null;
				}
				break;
				
			case XmlPullParser.TEXT:
				if(currentInfo != null && attr != null){
					String text = xpp.getText();
					if(attr.equals("id")){
						currentInfo.setId(text);
					} else if(attr.equals("name")){
						currentInfo.setName(text);
					} else if(attr.equals("lat")){
						currentInfo.setLat(text);
					} else if(attr.equals("lon")){
						currentInfo.setLon(text);
					} else if(attr.equals("ip")){
						currentInfo.setIp(text);
					} else if(attr.equals("port")){
						int port = new Integer(text);
						currentInfo.setPort(port);
					} else if(attr.equals("queueSize")){
						int queueSize = new Integer(text);
						currentInfo.setQueueSize(queueSize);
					} else if(attr.equals("waitTime")){
						int waitTime = new Integer(text);
						currentInfo.setWaitTime(waitTime);
					} else if(attr.equals("vehicles")){
						int vehicles = new Integer(text);
						currentInfo.setVehicles(vehicles);						
					}
				}
				break;
				
			case XmlPullParser.END_DOCUMENT:
				return infos;
			
			}
		}

	}
	
	enum StationTags {
		ID, NAME, LAT, LON, IP, PORT, QUEUESIZE, WAITTIME, VEHICLES
	}
	
	
	public static void main(String[] args) {
		
		String nav = "Hello;World";
		
		String[] split = nav.split(";");
		System.out.println(split[0]);
		System.out.println(nav.split(";")[1]);
		
	}
}
