//This program tests that when the LatLng array is sent to the flora, the neopixels flash blue
//to inform the user that the route is being sent.
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
//Initialize Neopixel object
Adafruit_NeoPixel pixels = Adafruit_NeoPixel(NUMPIXELS, PIN, NEO_GRB + NEO_KHZ800);

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
  pixels.begin();

  //Make sure pixels are set to of from start
  changePixel(0, 0, 0, 0);
  changePixel(1, 0, 0, 0);

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
    tetheredFunc();
  }

  //Sets the variables that the interrupt uses to flash the lights.
  //If the bluetooth becomes disconnected the lights will flash purple
  setFlashVariables(128, 0 ,128, 10, 0, 300, false, 0, 1);

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

  flashingSignal();

  if(bufferData[0] != 'O'){
    Serial.print(F("BufferData: "));
    Serial.println(bufferData);
    processData(bufferData);
  }
}

void processData(char data[]){
  if(condition(data, 'l', 'a', 't') || condition(data, 'l', 'n', 'g'))
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
  // flash green to indicate all waypoints have been delivered
  setFlashVariables(0, 255, 0, 10, 0, 100, false, 0, 1);
}

void processLatLng(char data[]){
  if(!deliveryStarted){
    deliveryStarted = true;
    //flash blue to indicate waypoints are being delivered to arduino
    setFlashVariables(0, 0, 255, 0, 0, 600, true, 0, 1);
  }

  char latLng[10];
  removeLatLngTag(latLng, data);
  latLngArray[addToLatLngIndex++] = atof(latLng);
  sendConfirmation();
}

void printTurnDetails(float targetLat, float targetLng, float targetDistance){
  Serial.print(F("latLng = "));
  Serial.print(targetLat);
  Serial.print(F(", "));
  Serial.println(targetLng);
  Serial.print(F("Distance = "));
  Serial.println(targetDistance);
  Serial.print(F("THE DIRECTION TO TURN (LEFT) is: "));
  Serial.println(turns[turnsArrayPointer]);
  Serial.println(F("==================================================================="));
}

//Timer interrupt to flash neopixels
void flashingSignal(){
  //Get the amount of milliseconds since the program started
  unsigned long currentMillis = millis();

  //Every n milliseconds (dictated by flashinterval)
  //the interrupt will fire
  if((currentMillis - previousMillisLights > flashInterval)){

    //While there are flashes left of still waiting for
    //latlng array to fill up
    if(flashNum < flashes || waitingForJob){
      flashNum++;
      previousMillisLights = currentMillis;
      if(!lightOn){
        //If lights are off, turn them on
        changePixel(left, red, green, blue);
        changePixel(right, red, green, blue);
        lightOn = true;
      }
      else{
        //If lights are on, turn them off
        changePixel(0, 0, 0, 0);
        changePixel(1, 0, 0, 0);
        lightOn = false;
      }
    }
    //When flashNum reaches the specified flashes, set
    //the flashVariables and pixels back to zero
    else if(flashNum > 0 && flashNum == flashes){
      setFlashVariables(0, 0, 0, 0, 0, 0, false, 0, 0);
      changePixel(0, 0, 0, 0);
      changePixel(1, 0, 0, 0);
    }
  }
}

void setFlashVariables(uint8_t r, uint8_t g, uint8_t b,
            uint8_t f, uint8_t fNum, unsigned long fI, boolean wFJ,
            uint8_t l, uint8_t ri){
   red = r;
   green = g;
   blue = b;
   flashes = f;
   flashNum = fNum;
   flashInterval = fI;
   waitingForJob = wFJ;
   left = l;
   right = ri;
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

//Set chosen pixel color/brightness
void changePixel(int pixel, int r, int g, int b){
    pixels.setPixelColor(pixel, pixels.Color(r, g, b));
    pixels.show(); //This sends the updated pixel color to the hardware.
}
