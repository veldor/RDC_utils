package net.velor.rdc_utils.handlers;

import android.graphics.Color;

import java.util.Random;

class ColorsHandler {
    static String getRandomColor(){
        Random rnd = new Random();
        int intColor = Color.rgb(rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
        return String.format("#%06X", (0xFFFFFF & intColor));
    }
}
