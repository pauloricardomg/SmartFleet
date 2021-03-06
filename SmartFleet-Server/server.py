import web
import socket
import time
from collections import namedtuple
import shelve

''' ####################  '''
''' Web Service Handling  '''
''' ####################  '''

render = web.template.render('templates/', cache=False)

# Registered server calls
urls = (
	# http://serverIP:8080/RegisterVehicle?id=vehicle001;capacity=5;lat=42.438917;lon=42.438917 METHOD: GET (Response: SimpleResponse.xml)
	'/RegisterVehicle', 'RegisterVehicle',
	# http://serverIP:8080/RegisterParty?stationID=12345;partyName=EMDC;numPassengers=4;dest=4567;destLat=32.321234;destLon=33.123456 METHOD: GET (Response: SimpleResponse.xml)
	'/RegisterParty', 'RegisterParty',
	# http://serverIP:8080/RegisterStation?id=12345;name=Alameda;lat=53.123456;lon=22.1234567;ip=127.0.0.1;port=4001 METHOD: GET (Response: SimpleResponse.xml)
	'/RegisterStation', 'RegisterStation',
	# http://serverIP:8080/GetAllStations METHOD: GET (Response: StationInfo.xml)
	'/GetAllStations', 'GetAllStations',
	# http://serverIP:8080/GetAllVehicles METHOD: GET (Response: StationInfo.xml)
	'/GetAllVehicles', 'GetAllVehicles',
	# http://serverIP:8080/ArrivedAtStation?vehicleID=vehicle001;stationID=12345;freeSeats=3;(parties=p1,p2,p3);ts=1305149006317 METHOD: GET (Response: PartyInfo.xml)
	'/ArrivedAtStation', 'ArrivedAtStation',
	# http://serverIP:8080/LeaveStation?vehicleID=vehicle001;stationID=12345;dest=dest;ts=1305149006317 METHOD: GET (Response: SimpleResponse.xml)
	'/LeaveStation', 'LeaveStation',
	# http://serverIP:8080/Update?vid=1;lat=33.222;lon=444.55;alt=100;dest=blabla;plist=bla,bla;bat=123.456;ts=1305149006317 METHOD: GET (Response: SimpleResponse.xml)
	'/Update', 'Update',
)

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

class LeaveStation:
    def GET(self): # it is GET just for testing from the browser, change it later to POST
	result = leaveStation(web.input())
        return render.SimpleResponse(result)

class Update:
    def GET(self): # it is GET just for testing from the browser, change it later to POST
	result = update(web.input())
        return render.SimpleResponse(result)


# Server data structures

activeStations = shelve.open('stations.db',writeback=True)
activeVehicles = shelve.open('vehicles.db',writeback=True)

altitudes = {}



web.webapi.internalerror = web.debugerror
if __name__ == '__main__':
	#activeStations = shelve.open('stations.db',writeback=True)
	#activeVehicles = shelve.open('vehicles.db',writeback=True)
	app.run()
	activeStations.close()
	activeVehicles.close()


''' ################### '''
''' Smart Fleet Server  '''
''' ################### '''

# Server class definitions

class Vehicle:

	def __init__(self, id, capacity, lat, lon):
		    self.id = id
		    self.capacity = capacity
		    self.current = 0
		    self.battLevel = 10.0
		    self.lat = lat
		    self.lon = lon
		    self.alt = 0
		    self.dest = None 
		    self.parties = []
		    self.ts = 0L
		    

	def __eq__(self, other):
		if(other == None):
			return 0

		return self.id == other.id

class Party:

	def __init__(self, name, numPassengers, dest, lat, lon):
		    self.name = name
		    self.numPassengers = numPassengers
		    self.dest = dest
		    self.arrival = int(time.time())
		    self.destLat = lat
		    self.destLon = lon

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
	vehicleID = str(input.id)
	capacity = input.capacity
	lat = input.lat
	lon = input.lon

	# Add vehicle to active vehicles
	print "Registering vehicle " + vehicleID + ". Capacity: " + capacity + ". Lat: " + lat + ", Lon: " + lon 
	activeVehicles[vehicleID] = Vehicle(vehicleID, capacity, lat, lon)
	print "Active vehicles " + str(activeVehicles)

	return 1


def registerStation(input):
	stationID = str(input.id)
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
	stationID = str(input.stationID)
	partyName = input.partyName
	numPassengers = input.numPassengers
	destination = input.dest
	destLat = input.destLat
	destLon = input.destLon
	
	# Retrieve station
	station = activeStations.get(stationID)
	if(station == None):
		print "Station " + stationID + " not found. Rejecting request."
		return 0

	# Verify is party is already in station queue
	thisParty = Party(partyName, numPassengers, destination, destLat, destLon)
	if(thisParty in station.queue):
		print "Party " + partyName + " already registered. Rejecting request."
		return 0

	# Add party to station queue
	print "Registering party " + partyName + ". Passengers: " + numPassengers + ". Destination: " + destination + ". Lat: " + destLat + ". Lon: " + destLon 
	station.queue.append(thisParty)
	print "Station " + stationID + " queue: " + str(station.queue)

	return 1

def arrivedAtStation(input):
	vehicleID = str(input.vehicleID)
	stationID = str(input.stationID)
	freeSeats = int(input.freeSeats)
	ts = long(input.ts)
	print "Vehicle " + vehicleID + " arrived at station " + stationID + " with " + str(freeSeats) + " free seats. TS=" + input.ts

	if(vehicleID in altitudes):
		print "Removing vehicle from altitudes list"
		del altitudes[vehicleID]

	station = activeStations.get(stationID)
	if(station == None):
		print "Station " + input.stationID + " not found. Rejecting request."
		return []

	vehicle = activeVehicles.get(vehicleID)
	if(vehicle == None):
		print "Vehicle " + input.vehicleID + " not found. Rejecting request."
		return []

	if(vehicle.id not in station.vehicles):
		station.vehicles.append(vehicleID)

	if(hasattr(input,"parties")):
			stillInVehicle = input.parties.split(",")
			print "Parties " + str(stillInVehicle) + " still on vehicle."
			for party in vehicle.parties[:]:
				if(not party in stillInVehicle):
					print "Party " + party + " is no longer in vehicle."
					vehicle.parties.remove(party)
	else:
		print "No parties currently in the car, cleaning party list"
		vehicle.parties = []

	vehicle.lat = station.lat
	vehicle.lon = station.lon
	vehicle.alt = 0
	vehicle.ts = ts

	boardingParties = []

	for party in station.queue:
		passengers = int(party.numPassengers)
		if(freeSeats >= passengers):
			freeSeats -= passengers
			print "Boarding party " + party.name + " on vehicle " + vehicleID + ". Left seats: " + str(freeSeats)
			vehicle.parties.append(party.name)
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
		s.close()
	else:
		print "No parties on this station fit in this car."

	return boardingParties


def leaveStation(input):
	vehicleID = str(input.vehicleID)
	stationID = str(input.stationID)
	dest = input.dest
	ts = long(input.ts)
	print "Vehicle " + vehicleID + " left  station " + stationID + " to destination " + dest + ". TS=" + input.ts

	station = activeStations.get(stationID)
	if(station == None):
		print "Station " + input.stationID + " not found. Rejecting request."
		return -1

	vehicle = activeVehicles.get(vehicleID)
	if(vehicle == None):
		print "Vehicle " + input.vehicleID + " not found. Rejecting request."
		return -1

	station.vehicles.remove(vehicleID)

	vehicle.ts = ts
	vehicle.dest = dest
	vehicle.battLevel = 10.0
		
	host = station.ip
	port = int(station.port)

	selectedAlt = 100
	#while(selectedAlt in altitudes.values() and selectedAlt < 500): #max altitude
	#	selectedAlt += 100
	
	print "Selected altitude for vehicle " + vehicleID + " is " + str(selectedAlt)
	vehicle.alt = selectedAlt
	altitudes[vehicleID] = selectedAlt

	msg = "left"
	print "Sending message to " + host + ":" + str(port) + ": " + msg
	s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
	s.connect((host, port))
	s.send(msg)
	s.close()

	return selectedAlt



	# http://serverIP:8080/Update?vid=1;lat=33.222;lon=444.55;alt=100;dest=blabla;plist=bla,bla;bat=123.456;ts=1305149006317 METHOD: GET (Response: SimpleResponse.xml)

def update(input):
	vid = str(input.vid)
	lat = input.lat
	lon = input.lon
	alt = input.alt
	dest = input.dest
	plist = input.plist
	bat = input.bat
	ts = long(input.ts)
	
	vehicle = activeVehicles.get(vid)
	if(vehicle == None):
		print "Vehicle " + input.vid + " not found. Rejecting request."
		return -1

	if(ts > vehicle.ts):
		print "Received update for vehicle " + input.vid + ": lat: " + input.lat + " lon: " + input.lon + " alt: " + input.alt + " dest: " + input.dest + " plist: " + input.plist + " bat: " + input.bat + " ts: " + input.ts
		vehicle.ts = ts
		vehicle.dest = dest
		vehicle.lat = lat
		vehicle.lon = lon
		vehicle.alt = alt
		vehicle.bat = bat

		if(float(bat) == 0.0):
			print "Vehicle " + input.vid + " crashed!!"

		if(plist != ""):
			stillInVehicle = plist.split(",")
			print "Parties " + str(stillInVehicle) + " still on vehicle."
			for party in vehicle.parties[:]:
				if(not party in stillInVehicle):
					print "Party " + party + " is no longer in vehicle."
					vehicle.parties.remove(party)
		return 1
	else:
		print "Received old update for vehicle " + input.vid + ". Current ts: " + str(vehicle.ts) + " Update ts: " + input.ts
		return 0
