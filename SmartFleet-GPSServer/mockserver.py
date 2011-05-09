import web
import socket
import time
from collections import namedtuple
from threading import Thread
from math import *

''' ####################  '''
''' Web Service Handling  '''
''' ####################  '''

render = web.template.render('templates/', cache=False)

# Registered server calls
urls = (
	# http://serverIP:8080/RegisterVehicle?id=vehicle001;capacity=5;lat=42.438917;lon=42.438917 METHOD: GET (Response: SimpleResponse.xml)
	'/RegisterVehicle', 'RegisterVehicle',
	# http://serverIP:8080/RegisterParty?stationID=12345;partyName=EMDC;numPassengers=4;dest=4567 METHOD: GET (Response: SimpleResponse.xml)
	'/RegisterParty', 'RegisterParty',
	# http://serverIP:8080/RegisterStation?id=12345;name=Alameda;lat=53.123456;lon=22.1234567;ip=127.0.0.1;port=4001 METHOD: GET (Response: SimpleResponse.xml)
	'/RegisterStation', 'RegisterStation',
	# http://serverIP:8080/GetAllStations METHOD: GET (Response: StationInfo.xml)
	'/GetAllStations', 'GetAllStations',
	# http://serverIP:8080/GetAllVehicles METHOD: GET (Response: StationInfo.xml)
	'/GetAllVehicles', 'GetAllVehicles',
	# http://serverIP:8080/ArrivedAtStation?vehicleID=vehicle001;stationID=12345;freeSeats=3 METHOD: GET (Response: PartyInfo.xml)
	'/ArrivedAtStation', 'ArrivedAtStation',
	# http://serverIP:9090/MoveTo?vehicleID=vehicle001;lat=52.123453;lon=33.221234 METHOD: GET (Response: SimpleResponse.xml)
	'/MoveTo', 'MoveTo',
)

class MoveTo:
    def GET(self): # it is GET just for testing from the browser, change it later to POST
	result = moveTo(web.input())
        return render.SimpleResponse(result)

app = web.application(urls, globals())

class RegisterParty:
    def GET(self): # it is GET just for testing from the browser, change it later to POST
	result = registerParty(web.input())
        return render.SimpleResponse(result)

class RegisterStation:
    def GET(self): # it is GET just for testing from the browser, change it later to POST
	result = registerStation(web.input())
        return render.SimpleResponse(result)

class RegisterVehicle:
    def GET(self): # it is GET just for testing from the browser, change it later to POST
	result = registerVehicle(web.input())
        return render.SimpleResponse(result)

class GetAllStations:
    def GET(self): # it is GET just for testing from the browser, change it later to POST
        return render.StationInfo(getAllStations())

class GetAllVehicles:
    def GET(self): # it is GET just for testing from the browser, change it later to POST
        return render.VehicleInfo(getAllVehicles())


class ArrivedAtStation:
    def GET(self): # it is GET just for testing from the browser, change it later to POST
	result = arrivedAtStation(web.input())
        return render.PartyInfo(result)




web.webapi.internalerror = web.debugerror
if __name__ == '__main__': app.run()


''' ################### '''
''' Smart Fleet Server  '''
''' ################### '''

# Server class definitions

class Vehicle:

	def __init__(self, id, capacity, lat, lon):
		    self.id = id
		    self.capacity = capacity
		    self.current = 0
		    self.battLevel = 0
		    self.lat = float(lat)
		    self.lon = float(lon)
		    self.emulatorPort = 6669

	def __eq__(self, other):
		if(other == None):
			return 0

		return self.id == other.id

class Party:

	def __init__(self, name, numPassengers, dest):
		    self.name = name
		    self.numPassengers = numPassengers
		    self.dest = dest
		    self.arrival = int(time.time())

	def __eq__(self, other):
		if(other == None):
			return 0

		return self.name == other.name

class Station:

	def __init__(self, id, name, lat, lon, ip, port):
		    self.id = id
		    self.name = name
		    self.ip = ip
		    self.port = port
		    self.lat = lat
		    self.lon = lon
		    self.queue = []
		    self.vehicles = []
		    self.parties = 0
		    self.avgWait = 0

	def peopleWaiting(self):
		result = 0
		for party in self.queue:
			result += int(party.numPassengers)

		return result

	def removeParties(self, partiesToRemove):
		now = int(time.time())
		# Remove selected parties from queue
		for party in partiesToRemove:
			waitTime = now - party.arrival
			self.parties = self.parties+1
			self.avgWait = ((self.avgWait * (self.parties-1)) + waitTime)/self.parties
			self.queue.remove(party)

	def __eq__(self, other):
		if(other == None):
			return 0

		return self.id == other.id

# Server data structures

activeStations = {}

activeVehicles = {}

# Server interface

def getAllStations():
	result = []
	if(len(activeStations) > 0):
		result = activeStations.values()
	print "Retrieving all stations."
	return result

def getAllVehicles():
	result = []
	if(len(activeVehicles) > 0):
		result = activeVehicles.values()
	print "Retrieving all vehicles: " + str(activeVehicles)
	return result

def registerVehicle(input):
	vehicleID = input.id
	capacity = input.capacity
	lat = input.lat
	lon = input.lon

	# Add vehicle to active vehicles
	print "Registering vehicle " + vehicleID + ". Capacity: " + capacity + ". Lat: " + lat + ", Lon: " + lon 
	activeVehicles[vehicleID] = Vehicle(vehicleID, capacity, lat, lon)
	print "Active vehicles " + str(activeVehicles)

	return 1


def registerStation(input):
	stationID = input.id
	stationName = input.name
	stationIP = input.ip
	stationPort = input.port
	stationLat = input.lat
	stationLon = input.lon

	# Verify if station is already registered
	if(activeStations.get(stationID) != None):
		print "Station " + stationName + " already registered. Rejecting request."
		return 0

	# Add station to active stations
	print "Registering station " + stationName + ". IP: " + stationIP + ". Port: " + stationPort + ". Lat: " + stationLat + ", Lon: " + stationLon 
	activeStations[stationID] = Station(stationID, stationName, stationLat, stationLon, stationIP, stationPort)
	print "Active stations " + str(activeStations)

	return 1

def registerParty(input):
	stationID = input.stationID
	partyName = input.partyName
	numPassengers = input.numPassengers
	destination = input.dest
	
	# Retrieve station
	station = activeStations.get(stationID)
	if(station == None):
		print "Station " + stationID + " not found. Rejecting request."
		return 0

	# Verify is party is already in station queue
	thisParty = Party(partyName, numPassengers, destination)
	if(thisParty in station.queue):
		print "Party " + partyName + " already registered. Rejecting request."
		return 0

	# Add party to station queue
	print "Registering party " + partyName + ". Passengers: " + numPassengers + ". Destination: " + destination 
	station.queue.append(thisParty)
	print "Station " + stationID + " queue: " + str(station.queue)

	return 1

def arrivedAtStation(input):
	vehicleID = input.vehicleID
	stationID = input.stationID
	freeSeats = int(input.freeSeats)

	print "Vehicle " + vehicleID + " arrived at station " + stationID + " with " + str(freeSeats) + " free seats."

	station = activeStations.get(stationID)
	if(station == None):
		print "Station " + input.stationID + " not found. Rejecting request."
		return []

	vehicle = activeVehicles.get(vehicleID)
	if(vehicle == None):
		print "Vehicle " + input.stationID + " not found. Rejecting request."
		return []

	station.vehicles.append(vehicleID)
	vehicle.lat = station.lat
	vehicle.lon = station.lon

	boardingParties = []

	for party in station.queue:
		passengers = int(party.numPassengers)
		if(freeSeats >= passengers):
			freeSeats -= passengers
			print "Boarding party " + party.name + " on vehicle " + vehicleID + ". Left seats: " + str(freeSeats)
			boardingParties.append(party)
			if(freeSeats == 0):
				break

	if(len(boardingParties) > 0):
		msg = vehicleID + ";"

		for party in boardingParties:
			msg += party.name + ","

		station.removeParties(boardingParties)

		host = station.ip
		port = int(station.port)

		print "Sending message to " + host + ":" + str(port) + ": " + msg
		s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
		s.connect((host, port))
		s.send(msg)
	else:
		print "No parties on this station fit in this car."

	return boardingParties


def moveTo(input):
	vehicleID = input.vehicleID
	lat = float(input.lat)
	lon = float(input.lon)

	print "Vehicle " + vehicleID + " will now move to  lat:"  + input.lat + ", lon:" + input.lon + " ."

	vehicle = activeVehicles.get(vehicleID)
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
