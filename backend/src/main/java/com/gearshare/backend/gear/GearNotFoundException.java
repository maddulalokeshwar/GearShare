
package com.gearshare.backend.gear;

public class GearNotFoundException extends RuntimeException {

    public GearNotFoundException(String message) {
        super(message);
    }
}