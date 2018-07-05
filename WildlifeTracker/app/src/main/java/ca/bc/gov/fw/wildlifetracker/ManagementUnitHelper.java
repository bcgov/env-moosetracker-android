package ca.bc.gov.fw.wildlifetracker;

import android.util.Log;

import java.util.ArrayList;

/**
 * Utility class for dealing with region and MU names.
 */
public class ManagementUnitHelper {

    public static String[] regions__ = { "1", "2", "3", "4", "5", "6", "7A", "7B", "8" };

    public static String[][] musByRegion__;

    static {
        musByRegion__ = new String[9][];
        musByRegion__[0] = createMUs("1-", 1, 15);
        musByRegion__[1] = createMUs("2-", 1, 19);
        musByRegion__[2] = createMUs("3-", 12, 20, 26, 46);
        musByRegion__[3] = createMUs("4-", 1, 9, 14, 40);
        musByRegion__[4] = createMUs("5-", 1, 16);
        musByRegion__[5] = createMUs("6-", 1, 30);
        musByRegion__[6] = createMUs("7-", 1, 18, 23, 30, 37, 41);  // 7A
        musByRegion__[7] = createMUs("7-", 19, 22, 31, 36, 42, 58);  // 7B
        musByRegion__[8] = createMUs("8-", 1, 15, 21, 26);
    }

    public static String findRegionForMU(String mu) {
        if ((mu == null) || (mu.length() < 1)) {
            Log.println(Log.ERROR, "ManagementUnitHelper", "Invalid mu: " + mu);
            return null;
        }
        for (int i = 0; i < musByRegion__.length; i++) {
            if (mu.charAt(0) != regions__[i].charAt(0)) {
                continue;
            }
            for (int j = 0; j < musByRegion__[i].length; j++) {
                if (mu.equals(musByRegion__[i][j])) {
                    return regions__[i];
                }
            }
        }
        return null;
    }

    private static String[] createMUs(String base, int... args) {
        ArrayList<String> list = new ArrayList<>();
        for (int i = 0; i < args.length; i += 2) {
            for (int j = args[i]; j <= args[i + 1]; j++)
                list.add(base + String.valueOf(j));
        }
        String[] result = new String[list.size()];
        return list.toArray(result);
    }

}
