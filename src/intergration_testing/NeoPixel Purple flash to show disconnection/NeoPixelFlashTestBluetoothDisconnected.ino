//This program tests when the bluetooth is disconnected the neopixels flash purple to inform
//The user that the board is untethered from the phone.
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

boolean waitingForJob = false;
unsigned long previousMillisLights = 0;
unsigned long flashInterval = 0;
boolean firstFix = false;
boolean lightOn = false;
uint8_t red = 0;
uint8_t green = 0;
uint8_t blue = 0;
uint8_t flashes = 0;
uint8_t flashNum = 0;

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

  /* Wait for connection */
  while(!ble.isConnected()) {
    Serial.println(F("bluetooth not connected"));
    delay(500);
  }

  if(ble.isVersionAtLeast(MINIMUM_FIRMWARE_VERSION))
    ble.sendCommandCheckOK("AT+HWModeLED=" MODE_LED_BEHAVIOUR);

  //This is the first main loop. This loop will continue
  //as long as the app is tethered to the flora
  while(ble.isConnected()){}

  //Sets the variables that the interrupt uses to flash the lights.
  //If the bluetooth becomes disconnected the lights will flash purple
  setFlashVariables(128, 0 ,128, 100, 0, 300, false, 0, 1);

}

//---------------------------------------------------------------------------------------
//---------------------------------------------------------------------------------------

void loop(void){
  flashingSignal();
}

//--------------------------------------------------------------------------------------
//--------------------------------------------------------------------------------------

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


//Set chosen pixel color/brightness
void changePixel(int pixel, int r, int g, int b){
    pixels.setPixelColor(pixel, pixels.Color(r, g, b));
    pixels.show(); //This sends the updated pixel color to the hardware.
}
