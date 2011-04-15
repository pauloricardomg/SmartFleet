package pt.utl.ist.mobcomp.SmartFleet.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.xmlpull.v1.XmlPullParserException;

import android.content.res.AssetManager;

import pt.utl.ist.mobcomp.SmartFleet.bean.StationInfo;
import pt.utl.ist.mobcomp.SmartFleet.bean.VehicleInfo;

public class LookupUtils {

	public static List<StationInfo> lookupStations(String url)
	{

		String response = null;
		try {
			response = HTTPClient.executeHttpGet(url);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		List<StationInfo> result = null;
		try {
			result = XmlUtils.parseStationInfos(response);
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return result;
	}
	
	public static List<VehicleInfo> lookupVehicles(String url)
	{

		String response = null;
		try {
			response = HTTPClient.executeHttpGet(url);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		List<VehicleInfo> result = null;
		try {
			result = XmlUtils.parseVehicleInfos(response);
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return result;
	}
	
	
	public static Properties readPropertiesFile(AssetManager assetManager, String fileName) {
		Properties properties = null;
		
		// Read from the /assets directory
		try {
		    InputStream inputStream = assetManager.open(fileName);
		    properties = new Properties();
		    properties.load(inputStream);
		} catch (IOException e) {
		    System.err.println("Failed to open microlog property file");
		    e.printStackTrace();
		}
		return properties;
	}
}
