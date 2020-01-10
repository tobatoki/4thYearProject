#include <Adafruit_GPS.h>
#include <Arduino.h>
#include <Adafruit_NeoPixel.h>
#ifdef __AVR__
  #include <avr/power.h>
#endif

//Pin used for neopixel data
#define PIN            12
//Number of pixels being used
#define NUMPIXELS      2
#define GPSECHO false

#include "Adafruit_BLE.h"
#include "Adafruit_BluefruitLE_UART.h"
#include "BluefruitConfig.h"

    #define FACTORYRESET_ENABLE         1
    #define MINIMUM_FIRMWARE_VERSION    "0.6.6"
    #define MODE_LED_BEHAVIOUR          "MODE"
    #define BLUEFRUIT_HWSERIAL_NAME          Serial1
    #define BLUEFRUIT_UART_MODE_PIN         -1   // Not used with FLORA
    #define BLUEFRUIT_UART_CTS_PIN          -1   // Not used with FLORA
    #define BLUEFRUIT_UART_RTS_PIN          -1   // Not used with FLORA
/*=========================================================================*/
//Initialize Bluefruit object
Adafruit_BluefruitLE_UART ble(BLUEFRUIT_HWSERIAL_NAME, BLUEFRUIT_UART_MODE_PIN);

boolean deliveryStarted = false;
boolean latLngArrayFull = false;
boolean waitingForJob = false;
uint8_t tetheredFlag = 2;
float latLngArray[30];
char turns[15];
uint8_t left = 0;
uint8_t right = 1;
uint8_t turnsArrayPointer = 0;
uint8_t addToLatLngIndex = 0;
uint8_t addToTurnsIndex = 0;
uint8_t latLngArrayPointer = 0;
uint8_t gpsSignalCount = 0;
unsigned long previousMillisLights = 0;
unsigned long flashInterval = 0;
boolean firstFix = false;
boolean lightOn = false;
uint8_t red = 0;
uint8_t green = 0;
uint8_t blue = 0;
uint8_t flashes = 0;
uint8_t flashNum = 0;
float oldTarget = 0.0;
float oldGpsLat = 0.0;
float oldGpsLng = 0.0;
const float time0 = 0.1;
float targetDistance;
boolean journeyComplete = false;

// A small helper
void error(const __FlashStringHelper*err) {
  Serial.println(err);
  while (1);
}

//---------------------------------------------------------------------------------------
//---------------------------------------------------------------------------------------

void setup(void){
  Serial.begin(115200);

  if ( !ble.begin(VERBOSE_MODE) )
    error(F("Couldn't find Bluefruit make sure it's in CoMmanD mode & check wiring?"));
  if( FACTORYRESET_ENABLE ){
    /* Perform a factory reset to make sure everything is in a known state */
    if(!ble.factoryReset())
      error(F("Couldn't factory reset"));
  }

  /* Disable command echo from Bluefruit */
  ble.echo(false);
  ble.info();
  ble.verbose(false);

  /* Wait for connection */
  while(!ble.isConnected()) {
    Serial.println(F("bluetooth not connected"));
    delay(500);
  }

  if(ble.isVersionAtLeast(MINIMUM_FIRMWARE_VERSION))
    ble.sendCommandCheckOK("AT+HWModeLED=" MODE_LED_BEHAVIOUR);

  //This is the first main loop. This loop will continue
  //as long as the app is tethered to the flora
  while(ble.isConnected()){
    //If the journey has not been completed, call tetheredFunc
    if(!journeyComplete) tetheredFunc();
  }

}

//---------------------------------------------------------------------------------------
//---------------------------------------------------------------------------------------

void loop(void){
}

//--------------------------------------------------------------------------------------
//--------------------------------------------------------------------------------------

void tetheredFunc(){
  //Check for incoming characters from Bluefruit
  ble.println("AT+BLEUARTRX");
  ble.readline();
  ble.waitForOK();

  char bufferData[21];
  readFromBuffer(bufferData);

  if(bufferData[0] != 'O'){
    Serial.print(F("BufferData: "));
    Serial.println(bufferData);
    processData(bufferData);
  }
}

void processData(char data[]){
  if(condition(data, 'e', 'n', 'd')) endOfLatLngArray();
  else if(condition(data, 'l', 'a', 't') || condition(data, 'l', 'n', 'g'))
    processLatLng(data);
  else if(data[0] == 'L' || data[0] == 'R' || data[0] == 'N'){
    turns[addToTurnsIndex++] = data[0];
    sendConfirmation();
  }
}

void endOfLatLngArray(){
  latLngArrayFull = true;
  latLngArray[addToLatLngIndex] = 0.0;
  turns[addToTurnsIndex] = 'E';
  sendConfirmation();
  printRouteDetails();
}

void processLatLng(char data[]){
  if(!deliveryStarted){
    deliveryStarted = true;
  }

  char latLng[10];
  removeLatLngTag(latLng, data);
  latLngArray[addToLatLngIndex++] = atof(latLng);
  sendConfirmation();
}

void removeLatLngTag(char latLng[], char data[]){
  int j = 0;
  int k = 3;
  while(data[k] != '\0')
    latLng[j++] = data[k++];
  latLng[j] = '\0';
}

//Reads data from bluetooth buffer as cstring
void readFromBuffer(char bufferData[]){
  int i = 0;
  char bufferValue;
  while((bufferValue = ble.buffer[i]) != NULL)
    bufferData[i++] = bufferValue;
  bufferData[i] = '\0';
}

void printRouteDetails(){
  Serial.println(F("Route Details"));
    int i = 0;
    int lat = 0;
    int lng = 0;
    int turn = 0;
    while(latLngArray[i] != NULL){
      Serial.print(F("Lat No."));
      Serial.println(lat++);
      Serial.println(latLngArray[i++], 5);
      Serial.print(F("Lng No."));
      Serial.println(lng++);
      Serial.println(latLngArray[i++], 5);
      Serial.print(F("Turn No."));
      Serial.println(turn);
      Serial.println(turns[turn++]);
      Serial.println(F("==============================================================="));
    }
}

void sendConfirmation(){
  // Send characters to Bluefruit
  ble.print("AT+BLEUARTTX=");
  ble.println('0');
  // check response status
  ble.waitForOK();
}

boolean condition(char bufferData[], char zero, char one, char two){
  return bufferData[0] == zero &&
          bufferData[1] == one &&
          bufferData[2] == two;
}
