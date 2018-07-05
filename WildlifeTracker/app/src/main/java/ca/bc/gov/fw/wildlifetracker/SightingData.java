package ca.bc.gov.fw.wildlifetracker;

import java.util.Date;

/**
 * Data holder class to encapsulate a sighting record
 *
 * Created by griffith on 2016-04-05.
 */
public class SightingData {

    public int numBulls;
    public int numCows;
    public int numCalves;
    public int numUnknown;
    public int numHours;
    public String managementUnit;
    public Date date;

    public long databaseId;
}
