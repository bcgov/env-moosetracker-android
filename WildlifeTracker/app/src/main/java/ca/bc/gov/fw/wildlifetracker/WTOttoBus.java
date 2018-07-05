package ca.bc.gov.fw.wildlifetracker;

import com.squareup.otto.Bus;

/**
 * Created by griffith on 2015-09-15.
 */
public class WTOttoBus {
    private static final Bus BUS = new Bus();

    public static Bus getInstance() {
        return BUS;
    }
}
