import web
import socket
import time
from threading import Thread
from math import *


''' ####################  '''
''' Web Service Handling  '''
''' ####################  '''

render = web.template.render('templates/', cache=False)

# Registered server calls
urls = (
	# http://serverIP:9090/RegisterVehicle?id=vehicle001;lat=42.438917;lon=42.438917;ip=192.168.0.1;port=1001;emulatorPort=6000 METHOD: GET (Response: SimpleResponse.xml)
	'/RegisterVehicle', 'RegisterVehicle',
	# http://serverIP:9090/MoveTo?vehicleID=vehicle001;lat=52.123453;lon=33.221234 METHOD: GET (Response: SimpleResponse.xml)
	'/MoveTo', 'MoveTo',
)

app = web.application(urls, globals())

class RegisterVehicle:
    def GET(self): # it is GET just for testing from the browser, change it later to POST
	result = registerVehicle(web.input())
        return render.SimpleResponse(result)

class MoveTo:
    def GET(self): # it is GET just for testing from the browser, change it later to POST
	result = moveTo(web.input())
        return render.SimpleResponse(result)

web.webapi.internalerror = web.debugerror
if __name__ == '__main__': app.run()


''' ################### '''
''' Smart Fleet GPS Server  '''
''' ################### '''

# Server class definitions

class Vehicle:

	def __init__(self, id, lat, lon, ip, port, emulatorPort):
		    self.id = id
		    self.lat = lat
		    self.lon = lon
		    self.ip = ip
		    self.port = port
		    self.emulatorPort = emulatorPort

	def __eq__(self, other):
		if(other == None):
			return 0

		return self.id == other.id

# Server data structures

vehicles = {}

# Server interface

def registerVehicle(input):
	vehicleID = input.id
	lat = float(input.lat)
	lon = float(input.lon)
	ip = input.ip
	port = int(input.port)
	emulatorPort = int(input.emulatorPort)

	# Add vehicle to active vehicles
	print "Registering vehicle " + vehicleID + ". ip: " + ip + ":" + input.port + ":" + input.emulatorPort + ". Lat: " + input.lat + ", Lon: " + input.lon 
	vehicle = Vehicle(vehicleID, lat, lon, ip, port, emulatorPort)
	vehicles[vehicleID] = vehicle
	print "Active vehicles " + str(vehicles)

	s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
	s.connect(("localhost", vehicle.emulatorPort))
	print "Sending geo fix " + str(vehicle.lon) + " " + str(vehicle.lat) + " of vehicle " + vehicle.id + " to port " + str(vehicle.emulatorPort)
	s.send("geo fix " + str(vehicle.lon) + " " + str(vehicle.lat) + "\n")
	s.close()	

	return 1

def moveTo(input):
	vehicleID = input.vehicleID
	lat = float(input.lat)
	lon = float(input.lon)

	print "Vehicle " + vehicleID + " will now move to  lat:"  + input.lat + ", lon:" + input.lon + " ."

	vehicle = vehicles.get(vehicleID)
	if(vehicle == None):
		print "Vehicle " + input.stationID + " not found. Rejecting request."
		return 0

	t = Thread( target=move, args = ( vehicle, lat, lon ) )
	t.start()

	return 1

def move(vehicle, lat, lon):
	s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
	s.connect(("localhost", vehicle.emulatorPort))
	
	print "Successfully connected to emulator of vehicle " + vehicle.id + " on port: " + str(vehicle.emulatorPort)

	oldLat = None
	oldLon = None
	while(vehicle.lat != lat and vehicle.lon != lon):
		brng = rhumbBearingTo(vehicle.lat, vehicle.lon, lat, lon)
		newPos = rhumbDestinationPoint(vehicle.lat, vehicle.lon, brng, 0.01) # 10 meters per second
		
		if(newPos[0] == oldLat or oldLat == lat):
			oldLat = vehicle.lat
			vehicle.lat = lat
		else:
			oldLat = vehicle.lat
			vehicle.lat = newPos[0]
		
		if(newPos[1] == oldLon or oldLon == lon):
			oldLon = vehicle.lon
			vehicle.lon = lon
		else:
			oldLon = vehicle.lon
			vehicle.lon = newPos[1]

		print "Sending geo fix " + str(vehicle.lon) + " " + str(vehicle.lat) + " of vehicle " + vehicle.id + " to port " + str(vehicle.emulatorPort)
		s.send("geo fix " + str(vehicle.lon) + " " + str(vehicle.lat) + "\n")
		time.sleep(1)

	
	print "Vehicle " + vehicle.id + " finished moving to  lat:"  + str(lat) + ", lon:" + str(lon) + " . Closing connection."

	s.close()

earthRadius = 6367.0 # km



# Returns the distance from this point to the supplied point, in km, travelling along a rhumb line
#   see http://williams.best.vwh.net/avform.htm#Rhumb
# @param   {LatLon} point: Latitude/longitude of destination point
# @returns {Number} Distance in km between this point and destination point
def rhumbDistanceTo(lat, lon, olat, olon):
    R = earthRadius
    lat1 = radians(lat)
    lat2 = radians(olat)
    dLat = radians(olat-lat)
    dLon = radians(olon-lon)
  
    dPhi = log(tan(lat2/2+pi/4)/tan(lat1/2+pi/4))
    if(not isnan(dLat/dPhi)):
	    q = dLat/dPhi 
    else: 
	    q = cos(lat1)  # E-W line gives dPhi=0

    # if dLon over 180dg take shorter rhumb across 180dg meridian:
    if (dLon > pi):
        dLon = 2*pi - dLon;
  
    dist = sqrt(dLat*dLat + q*q*dLon*dLon) * R; 
 
    print "dist: " + str(dist)
 
    return dist;  #4 sig figs reflects typical 0.3% accuracy of spherical model


# Returns the bearing from this point to the supplied point along a rhumb line, in degrees
# @param   {LatLon} point: Latitude/longitude of destination point
# @returns {Number} Bearing in degrees from North
def rhumbBearingTo(lat, lon, olat, olon):
    lat1 = radians(lat)
    lat2 = radians(olat)
    dLon = radians(olon-lon)
  
    dPhi = log(tan(lat2/2+pi/4)/tan(lat1/2+pi/4))
    if (abs(dLon) > pi):
	    if(dLon>0):
		    dLon=-(2*pi-dLon)
	    else:
		    dLon=(2*pi+dLon);
    
    brng = atan2(dLon, dPhi);
  
    return (degrees(brng)+360) % 360;




# Returns the destination point from this point having travelled the given distance (in km) on the 
# given bearing along a rhumb line

# @param   {Number} brng: Bearing in degrees from North
# @param   {Number} dist: Distance in km
# @returns {LatLon} Destination point
def rhumbDestinationPoint(lat, lon, brng, dist):
    R = earthRadius
    d = dist/R;  # d = angular distance covered on earth's surface
    lat1 = radians(lat) 
    lon1 = radians(lon)
    brng = radians(brng);

    lat2 = lat1 + d*cos(brng)
    dLat = lat2-lat1;
    dPhi = log(tan(lat2/2+pi/4)/tan(lat1/2+pi/4))
    if(not isnan(dLat/dPhi)):
	    q = dLat/dPhi 
    else: 
	    q = cos(lat1)  # E-W line gives dPhi=0
    dLon = d*sin(brng)/q;
    # check for some daft bugger going past the pole
    if (abs(lat2) > pi/2):
	    if(lat2>0):
		    lat2 = pi-lat2 
	    else:
		    lat2 = -(pi-lat2);
    lon2 = (lon1+dLon+3*pi)%(2*pi) - pi;
 
    return (degrees(lat2), degrees(lon2))


