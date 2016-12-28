package com.jayfella.sky;

import com.jayfella.sky.atmosphere.AtmosphereState;
import com.jme3.app.FlyCamAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.system.AppSettings;

/**
 * Created by James on 28/12/2016.
 */
public class Main extends SimpleApplication {

    private static Main main;

    public static void main(String... args) {

        main = new Main();

        AppSettings appSettings = new AppSettings(true);
        appSettings.setTitle("TestDayNightCycle - jMonkeyEngine");

        main.start();

    }

    private Main() {
        super(new FlyCamAppState(), new AtmosphereState());
    }

    @Override
    public void simpleInitApp() {

        main.getStateManager().getState(AtmosphereState.class)
                .getPositionProvider()
                .getCalendar().reset(2016, 7, 4, 12, 15, 4800);

    }
}
