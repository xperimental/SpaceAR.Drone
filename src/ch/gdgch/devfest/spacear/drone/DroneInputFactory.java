package ch.gdgch.devfest.spacear.drone;

import android.content.Context;

public class DroneInputFactory {

    public static DroneInput newInstance(Context context) {
        return new GameControllerInput(context);
    }

}
