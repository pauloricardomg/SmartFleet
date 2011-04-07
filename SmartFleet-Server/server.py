import web
from collections import namedtuple

''' ####################  '''
''' Web Service Handling  '''
''' ####################  '''

render = web.template.render('templates/', cache=False)

# Registered server calls
urls = (
    # http://serverIP:8080/RegisterParty?stationID=stationID;partyName=partyName;numPassengers=2;dest=abc METHOD: GET
    '/RegisterParty', 'RegisterParty',
    '/RegisterStation', 'RegisterStation'
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

web.webapi.internalerror = web.debugerror
if __name__ == '__main__': app.run()


''' ################### '''
''' Smart Fleet Server  '''
''' ################### '''

# Server class definitions

class Vehicle:

	def __init__(self, id, capacity):
		    self.id = id
		    self.capacity = capacity
		    self.used = 0
		    self.battLevel = 0
		    self.lastStation = ''

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

	def __init__(self, id, name, ip, port):
		    self.id = id
		    self.name = name
		    self.ip = ip
		    self.port = port

	def __eq__(self, other):
		return self.id == other.id

# Server data structures

vehicles = { 'vehicle1' : Vehicle('vehicle1',5),
	     'vehicle2' : Vehicle('vehicle1',5) }

activeStations = {}

stationQueues = {}

# Server interface

def registerStation(input):
	stationID = input.id
	stationName = input.name
	stationIP = input.ip
	stationPort = input.port

	# Verify if station is already registered
	if(activeStations.get(stationID) != None):
		print "Station " + thisParty.name + " already registered. Rejecting request."
		return 0

	# Add station to active stations
	print "Registering station " + stationName + ". IP: " + stationIP + ". Port: " + stationPort 
	activeStations[stationID] = Station(stationID, stationName, stationIP, stationPort)
	print "Active stations " + str(activeStations)

	return 1

def registerParty(input):
	# Retrieve station party queue
	stationQueue = stationQueues.get(input.stationID)
	if(stationQueue == None):
		print "Station " + input.stationID + " not found. Adding to list of stations."
		stationQueue = stationQueues[input.stationID] = []

	# Verify is party is already in station queue
	thisParty = Party(input.partyName, input.numPassengers, input.dest)
	if(thisParty in stationQueue):
		print "Party " + thisParty.name + " already registered. Rejecting request."
		return 0

	# Add party to station queue
	print "Registering party " + thisParty.name + ". Passengers: " + thisParty.numPassengers + ". Destination: " + thisParty.dest 
	stationQueue.append(thisParty)
	print "Station " + input.stationID + " queue: " + str(stationQueue)

	return 1
