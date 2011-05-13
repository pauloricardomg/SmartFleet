There are 3 modules to be run:

Station
Vehicles
Monitoring Station

For all the 3 modules, set the conf files in the assets folder as described below.

1) Station.conf

id=123456                  //ID to be assigned for the destination       
name=MarquesPombal         // Name of the Station 
lat=38.725137              //  Latitude value for the Station   
lon=-9.149873              //  Longitude value for the Station 
station_ip=127.0.0.1       //IP address of the station, server will use this IP to contact station                                    
port=4003                  // port to which the station is listening 
server_ip=194.210.226.21   // IP address of the server   
server_port=8080           // port of the server. Always 8080

2)Vehicle.conf

id=1                       //ID to be assigned for the destination   
lat=38.725137              //  Latitude value for the vehicle    
lon=-9.149873              //  Longitude value for the vehicle   
server_ip=194.210.226.21   // IP address of central Server  
server_port=8080           // port of the server. Always 8080    
gps_server_port=9090       // port of the gps emulator server. Always 9090   
emulator_port=5554         // port of the emulator on which the vehicle is launched 
vehicle_ip=10.0.2.2        // Always 10.0.2.2. Launch all vehicles on same machine  
vehicle_port=4000          // port to which vehicle should listen to
capacity=5                 // Capacity of the Vehicle

3) monitoring.properties

server_ip=192.168.0.129         // IP address of central Server
server_port=8080                   // port of the server. Always 8080       
station_update_period=60000  // Perform station updates after how many milliseconds
vehicle_update_period=1000  // Perform vehicle updates after how many milliseconds


Before launching any of the emulator start two servers
1) central Server   SmartFleet-Server/server.py (start by running: python server.py 8080)
2) GPS server       SmartFleet-GPSServer/server.py (start by running: python server.py 9090)   
