package utils;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class CashHandler {
    // функция округления после запятой
    public static String roundTo(Float value){
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(Locale.ENGLISH);
        DecimalFormat df = new DecimalFormat("#.##", dfs);
        df.setRoundingMode(RoundingMode.HALF_DOWN);
        return df.format(value);
    }
    // функция округления после запятой
    public static String roundTo(Double value){
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(Locale.ENGLISH);
        DecimalFormat df = new DecimalFormat("#.##", dfs);
        df.setRoundingMode(RoundingMode.HALF_DOWN);
        return df.format(value);
    }

    public static String addRuble(String value){
        return String.format(Locale.ENGLISH, "%s\u00A0\u20BD", value);
    }
    public static String addRuble(int value){
        return String.format(Locale.ENGLISH, "%d\u00A0\u20BD", value);
    }
    public static String addRuble(float value){
        return String.format(Locale.ENGLISH, "%.2f\u00A0\u20BD", value);
    }
    public static String addRuble(double value){
        return String.format(Locale.ENGLISH, "%.2f\u00A0\u20BD", value);
    }

    public static float countPercent(Float full, String percent) {
        return full / 100 * Float.valueOf(percent);
    }
    public static double countPercent(Float full, Double percent) {
        return full / 100 * percent;
    }


    public static double countPercentForCc(int hours, float gainTotal, Double percent) {
        return (double) hours / 164.17 * gainTotal * percent;
    }

    public static int timeToInt(String time){
        String[] arr = time.split(":");
        return Integer.parseInt(arr[0]);
    }
}
