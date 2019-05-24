package net.velor.rdc_utils.handlers;

class TimeHandler {
    static Integer[] getTime(String timeString) {
        String[] values = timeString.split(":");
        return new Integer[]{Integer.valueOf(values[0]), Integer.valueOf(values[1])};
    }
}
