package com.example.ttoki.whichway;

import java.util.UUID;

public class UUIDS {
    // UUIDs for UAT service and associated characteristics.
    private UUID UART_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    private UUID TX_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
    private UUID RX_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");
    // UUID for the BTLE client characteristic which is necessary for notifications.
    private UUID CLIENT_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public UUID getClientUuid() {
        return CLIENT_UUID;
    }

    public UUID getUartUuid() {
        return UART_UUID;
    }

    public UUID getTxUuid() {
        return TX_UUID;
    }

    public UUID getRxUuid() {
        return RX_UUID;
    }
}