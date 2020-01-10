// This program tests the gps module's accuracy in retrieving a latitude and longitude fix.
// The result will be output to the Serial Monitor and then compared with the known location
// on google maps

#include <Adafruit_GPS.h>
#include <SoftwareSerial.h>
#define GPSECHO false
SoftwareSerial mySerial(A9,A10);
Adafruit_GPS GPS(&mySerial);
//Adafruit_GPS GPS(&Serial1);
boolean firstFixFlag = true;

void setup(){
  while (!Serial);
    delay(500);

  Serial.begin(115200);
  GPS.begin(9600);
  GPS.sendCommand(PMTK_SET_NMEA_OUTPUT_RMCGGA);
  GPS.sendCommand(PMTK_SET_NMEA_UPDATE_1HZ);

  delay(1000);
  // Ask for firmware version
  Serial.println(PMTK_Q_RELEASE);
}

void loop()
{
  // read data from the GPS in the 'main loop'
  char c = GPS.read();
  // if you want to debug, this is a good time to do it!
  if (GPSECHO)
      if (c) Serial.print(c);

  // if a sentence is received, we can check the checksum, parse it...
  if (GPS.newNMEAreceived()) {
    if (!GPS.parse(GPS.lastNMEA()))
      return;
  }

  if (GPS.fix) {
    float lat = latLngToDecimal(GPS.latitude, GPS.lat);
    float lng = latLngToDecimal(GPS.longitude, GPS.lon);
    if(firstFixFlag){
      Serial.print("The latitude is: ");
      Serial.println(lat, 6);
      Serial.print("The longitude is: ");
      Serial.println(lng, 6);
      //firstFixFlag = false;
    }
  }
}

// Convert NMEA coordinate to decimal degrees
float latLngToDecimal(float nmeaCoord, char dir) {
  uint16_t wholeDegrees = 0.01 * nmeaCoord;
  int modifier = 1;

  if (dir == 'W' || dir == 'S') modifier = -1;

  return (wholeDegrees + (nmeaCoord - 100.0 * wholeDegrees) / 60.0) * modifier;
}
