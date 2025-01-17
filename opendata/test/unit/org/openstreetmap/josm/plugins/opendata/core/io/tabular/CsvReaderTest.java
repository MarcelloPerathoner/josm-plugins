// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.opendata.core.io.tabular;

import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.plugins.opendata.core.datasets.AbstractDataSetHandler;
import org.openstreetmap.josm.plugins.opendata.core.io.NonRegFunctionalTests;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests of {@link CsvReader} class.
 */
@BasicPreferences
class CsvReaderTest {

    /**
     * Setup test.
     */
    @RegisterExtension
    JOSMTestRules rules = new JOSMTestRules().projection();

    private static AbstractDataSetHandler newHandler(final String epsgCode) {
        AbstractDataSetHandler handler = new AbstractDataSetHandler() {
            @Override
            public boolean acceptsFilename(String filename) {
                return true;
            }

            @Override
            public void updateDataSet(DataSet ds) {
            }
        };
        handler.setSpreadSheetHandler(new DefaultCsvHandler() {
            @Override
            public boolean handlesProjection() {
                return true;
            }

            @Override
            public LatLon getCoor(EastNorth en, String[] fields) {
                return Projections.getProjectionByCode(epsgCode).eastNorth2latlon(en);
            }
        });
        return handler;
    }

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/18029">#18029</a>
     * @throws Exception if an error occurs during reading
     */
    @Test
    void testTicket18029() throws Exception {
        try (InputStream is = TestUtils.getRegressionDataStream(18029, "gtfs_stops.broken.csv")) {
            NonRegFunctionalTests.testGeneric("#18029", CsvReader.parseDataSet(is, newHandler("EPSG:4326"), null));
        }
    }

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/13508">#13508</a>
     * @throws Exception if an error occurs during reading
     */
    @Test
    void testTicket13508() throws Exception {
        try (InputStream is = TestUtils.getRegressionDataStream(13508, "arrets-de-bus0.csv")) {
            NonRegFunctionalTests.testGeneric("#13508", CsvReader.parseDataSet(is, newHandler("EPSG:4326"), null));
        }
    }

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/10214">#10214</a>
     * @throws Exception if an error occurs during reading
     */
    @Test
    void testTicket10214() throws Exception {
        try (InputStream is = TestUtils.getRegressionDataStream(10214, "utf8_test.csv")) {
            NonRegFunctionalTests.testTicket10214(CsvReader.parseDataSet(is, newHandler("EPSG:4326"), null));
        }
    }

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/8805">#8805</a>
     * @throws Exception if an error occurs during reading
     */
    @Test
    void testTicket8805() throws Exception {
        try (InputStream is = TestUtils.getRegressionDataStream(8805, "XXX.csv")) {
            NonRegFunctionalTests.testGeneric("#8805", CsvReader.parseDataSet(is, newHandler("EPSG:4326"), null));
        }
    }
}
