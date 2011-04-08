import web
from collections import namedtuple

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
	'/GetAllStations', 'GetAllStations'
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
		    self.lat = lat
		    self.lon = lon

	def __eq__(self, other):
		return self.id == other.id

class Party:

	def __init__(self, name, numPassengers, dest):
		    self.name = name
		    self.numPassengers = numPassengers
		    self.dest = dest

	def __eq__(self, other):
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

	def __eq__(self, other):
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
		print "Station " + thisParty.name + " already registered. Rejecting request."
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
		print "Station " + input.stationID + " not found. Rejecting request."
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
