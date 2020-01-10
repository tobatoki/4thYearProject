// Output of this test program to the serial monitor should be "This is a receiving", but what is sent is
// "This is a receiving test". Testing out the 20 chars too
// sent from the bluefruit app on my android phone.

#include <Arduino.h>
#ifdef __AVR__
  #include <avr/power.h>
#endif

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

Adafruit_BluefruitLE_UART ble(BLUEFRUIT_HWSERIAL_NAME, BLUEFRUIT_UART_MODE_PIN);

// A small helper
void error(const __FlashStringHelper*err) {
  Serial.println(err);
  while (1);
}

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
  while(!ble.isConnected())
    delay(500);

  if(ble.isVersionAtLeast(MINIMUM_FIRMWARE_VERSION))
    ble.sendCommandCheckOK("AT+HWModeLED=" MODE_LED_BEHAVIOUR);
}

void loop(void){
  //Check for incoming characters from Bluefruit
  ble.println("AT+BLEUARTRX");
  ble.readline();
  ble.waitForOK();

  char bufferData[21];
  readFromBuffer(bufferData);

  if(bufferData[0] != 'O'){
    //Output should be "This is a receiving... beacuse of the 20 char"
    Serial.print(F("Received: "));
    Serial.println(bufferData);
  }
}

void readFromBuffer(char bufferData[]){
  int i = 0;
  char bufferValue;
  while((bufferValue = ble.buffer[i]) != NULL)
    bufferData[i++] = bufferValue;
  bufferData[i] = '\0';
}
