// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.opendata.core.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.AbstractReader;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.plugins.opendata.core.datasets.AbstractDataSetHandler;
import org.openstreetmap.josm.plugins.opendata.core.datasets.fr.FrenchConstants;
import org.openstreetmap.josm.tools.Logging;
import org.xml.sax.SAXException;

import neptune.ChouettePTNetworkType;
import neptune.ChouettePTNetworkType.ChouetteArea.AreaCentroid;
import neptune.ChouettePTNetworkType.ChouetteArea.StopArea;
import neptune.ChouettePTNetworkType.ChouetteLineDescription.ChouetteRoute;
import neptune.ChouettePTNetworkType.ChouetteLineDescription.StopPoint;
import neptune.LineType;
import neptune.LongLatTypeType;
import neptune.PTLinkType;
import neptune.PTNetworkType;
import neptune.PointType;
import neptune.StopAreaType;
import neptune.StopPointType;
import neptune.TridentObjectType;

/**
 * NEPTUNE -&gt; OSM converter
 * See http://www.chouette.mobi/IMG/pdf/NF__F_-Neptune-maj.pdf
 */
public class NeptuneReader extends AbstractReader implements FrenchConstants {

    public static final String OSM_PUBLIC_TRANSPORT = "public_transport";
    public static final String OSM_STOP = "stop";
    public static final String OSM_STOP_AREA = "stop_area";
    public static final String OSM_STOP_POSITION = "stop_position";
    public static final String OSM_PLATFORM = "platform";
    public static final String OSM_STATION = "station";
    public static final String OSM_NETWORK = "network";
    public static final String OSM_ROUTE = "route";
    public static final String OSM_ROUTE_MASTER = "route_master";

    public static final String OSM_TRAIN = "train";
    public static final String OSM_SUBWAY = "subway";
    public static final String OSM_MONORAIL = "monorail";
    public static final String OSM_TRAM = "tram";
    public static final String OSM_BUS = "bus";
    public static final String OSM_TROLLEYBUS = "trolleybus";
    public static final String OSM_AERIALWAY = "aerialway";
    public static final String OSM_FERRY = "ferry";

    private static final List<URL> schemas = new ArrayList<>();
    static {
        schemas.add(NeptuneReader.class.getResource(NEPTUNE_XSD));
    }

    private ChouettePTNetworkType root;

    private final Map<String, OsmPrimitive> tridentObjects = new HashMap<>();

    public static final boolean acceptsXmlNeptuneFile(File file) {
        return acceptsXmlNeptuneFile(file, null);
    }

    public static final boolean acceptsXmlNeptuneFile(File file, URL schemaURL) {

        if (schemaURL == null) {
            schemaURL = schemas.get(0);
        }

        try (FileInputStream in = new FileInputStream(file)) {
            Source xmlFile = new StreamSource(in);
            try {
                SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                Schema schema = schemaFactory.newSchema(schemaURL);
                Validator validator = schema.newValidator();
                validator.validate(xmlFile);
                Logging.info(xmlFile.getSystemId() + " is valid");
                return true;
            } catch (SAXException e) {
                Logging.error(xmlFile.getSystemId() + " is NOT valid");
                Logging.error("Reason: " + e.getLocalizedMessage());
            } catch (IOException e) {
                Logging.error(xmlFile.getSystemId() + " is NOT valid");
                Logging.error("Reason: " + e.getLocalizedMessage());
            }
        } catch (IOException e) {
            Logging.error(e);
        }

        return false;
    }

    public static DataSet parseDataSet(InputStream in, AbstractDataSetHandler handler, ProgressMonitor instance) throws IllegalDataException {
        return new NeptuneReader().doParseDataSet(in, instance);
    }

    protected static final <T> T unmarshal(Class<T> docClass, InputStream inputStream) throws JAXBException {
        String packageName = docClass.getPackage().getName();
        JAXBContext jc = JAXBContext.newInstance(packageName, NeptuneReader.class.getClassLoader());
        Unmarshaller u = jc.createUnmarshaller();
        @SuppressWarnings("unchecked")
        JAXBElement<T> doc = (JAXBElement<T>) u.unmarshal(inputStream);
        return doc.getValue();
    }

    private void linkTridentObjectToOsmPrimitive(TridentObjectType object, OsmPrimitive p) {
        p.put("ref:neptune", object.getObjectId());
        if (tridentObjects.put(object.getObjectId(), p) != null) {
            Logging.error("Trident object duplicated !!! : "+object.getObjectId());
        }
    }

    protected Node createNode(LatLon latlon) {
        Node n = new Node(latlon);
        ds.addPrimitive(n);
        return n;
    }

    private Node createPlatform(StopPointType stop) {
        Node n = createNode(createLatLon(stop));
        n.put(OSM_PUBLIC_TRANSPORT, OSM_PLATFORM);
        linkTridentObjectToOsmPrimitive(stop, n);
        n.put("name", stop.getName());
        return n;
    }

    protected Relation createRelation(String type) {
        Relation r = new Relation();
        r.put("type", type);
        ds.addPrimitive(r);
        return r;
    }

    protected Relation createPtRelation(String pt, TridentObjectType object) {
        Relation r = createRelation(OSM_PUBLIC_TRANSPORT);
        r.put(OSM_PUBLIC_TRANSPORT, pt);
        linkTridentObjectToOsmPrimitive(object, r);
        return r;
    }

    protected Relation createNetwork(PTNetworkType network) {
        Relation r = createRelation(OSM_NETWORK);
        linkTridentObjectToOsmPrimitive(network, r);
        r.put("name", network.getName());
        return r;
    }

    protected Relation createRouteMaster(LineType line) {
        Relation r = createPtRelation(OSM_ROUTE_MASTER, line);
        switch (line.getTransportModeName()) {
        case BUS:
            r.put(OSM_ROUTE_MASTER, OSM_BUS); break;
        case AIR:
            r.put(OSM_ROUTE_MASTER, OSM_AERIALWAY); break;
        case FERRY:
            r.put(OSM_ROUTE_MASTER, OSM_FERRY); break;
        case METRO:
            r.put(OSM_ROUTE_MASTER, OSM_SUBWAY); break;
        case TRAIN:
            r.put(OSM_ROUTE_MASTER, OSM_TRAIN); break;
        case TRAMWAY:
            r.put(OSM_ROUTE_MASTER, OSM_TRAM); break;
        case TROLLEYBUS:
            r.put(OSM_ROUTE_MASTER, OSM_TROLLEYBUS); break;
        default:
            Logging.warn("Unsupported transport mode: "+line.getTransportModeName());
        }
        r.put("ref", line.getNumber());
        r.put("name", line.getTransportModeName().value()+" "+line.getNumber()+": "+line.getName());
        return r;
    }

    private Relation createRoute(ChouetteRoute route) {
        Relation r = createPtRelation(OSM_ROUTE, route);
        r.put("name", route.getName());
        return r;
    }

    protected Relation createStopArea(StopAreaType sa) {
        Relation r = createPtRelation(OSM_STOP_AREA, sa);
        r.put("name", sa.getName());
        return r;
    }

    protected LatLon createLatLon(PointType point) {
        return new LatLon(point.getLatitude().doubleValue(), point.getLongitude().doubleValue());
    }

    protected final <T extends TridentObjectType> T findTridentObject(List<T> list, String id) {
        for (T object : list) {
            if (object.getObjectId().equals(id)) {
                return object;
            }
        }
        return null;
    }

    protected StopPoint findStopPoint(String id) {
        return findTridentObject(root.getChouetteLineDescription().getStopPoint(), id);
    }

    protected StopArea findStopArea(String id) {
        return findTridentObject(root.getChouetteArea().getStopArea(), id);
    }

    protected AreaCentroid findAreaCentroid(String id) {
        return findTridentObject(root.getChouetteArea().getAreaCentroid(), id);
    }

    protected PTLinkType findPtLink(String id) {
        return findTridentObject(root.getChouetteLineDescription().getPtLink(), id);
    }

    protected static boolean isNullLatLon(ILatLon ll) {
        return ll.lat() == 0.0 && ll.lon() == 0.0;
    }

    @Override
    protected DataSet doParseDataSet(InputStream in, ProgressMonitor instance) throws IllegalDataException {
        try {
            root = unmarshal(ChouettePTNetworkType.class, in);
        } catch (JAXBException e) {
            throw new IllegalDataException(e);
        }

        Relation network = createNetwork(root.getPTNetwork());

        // Parsing Stop areas
        for (StopArea sa : root.getChouetteArea().getStopArea()) {
            switch (sa.getStopAreaExtension().getAreaType()) {
                case COMMERCIAL_STOP_POINT:
                case QUAY:
                    parseStopArea(sa);
                    break;
                case BOARDING_POSITION:
                    //Main.info("skipping StopArea with type "+sa.getStopAreaExtension().getAreaType()+": "+sa.getObjectId());
                    break;
                default:
                    Logging.warn("Unsupported StopArea type: "+sa.getStopAreaExtension().getAreaType());
            }
        }

        Relation routeMaster = createRouteMaster(root.getChouetteLineDescription().getLine());
        network.addMember(new RelationMember(null, routeMaster));

        for (ChouetteRoute cr : root.getChouetteLineDescription().getChouetteRoute()) {
            Relation route = createRoute(cr);
            routeMaster.addMember(new RelationMember(null, route));
            for (String id : cr.getPtLinkId()) {
                PTLinkType ptlink = findPtLink(id);
                if (ptlink == null) {
                    Logging.warn("Cannot find PTLinkType: "+id);
                } else {
                    /*StopPoint start = findStopPoint(ptlink.getStartOfLink());
                    StopPoint end = findStopPoint(ptlink.getEndOfLink());*/
                    OsmPrimitive start = tridentObjects.get(ptlink.getStartOfLink());
                    OsmPrimitive end = tridentObjects.get(ptlink.getEndOfLink());
                    if (start == null) {
                        Logging.warn("Cannot find start StopPoint: "+ptlink.getStartOfLink());
                    } else if (start.get(OSM_PUBLIC_TRANSPORT).equals(OSM_STOP) || start.get(OSM_PUBLIC_TRANSPORT).equals(OSM_PLATFORM)) {
                        addStopToRoute(route, start);
                    }

                    if (end == null) {
                        Logging.warn("Cannot find end StopPoint: "+ptlink.getEndOfLink());
                    } else if (end.get(OSM_PUBLIC_TRANSPORT).equals(OSM_STOP) || end.get(OSM_PUBLIC_TRANSPORT).equals(OSM_PLATFORM)) {
                        addStopToRoute(route, end);
                    }
                }
            }
        }

        return ds;
    }

    private void parseStopArea(StopArea sa) {
        Relation stopArea = createStopArea(sa);
        stopArea.put("name", sa.getName());
        for (String childId : sa.getContains()) {
            if (childId.contains("StopArea")) {
                StopArea child = findStopArea(childId);
                if (child == null) {
                    Logging.warn("Cannot find StopArea: "+childId);
                } else {
                    switch (child.getStopAreaExtension().getAreaType()) {
                        case BOARDING_POSITION:
                        case QUAY:
                            parseStopAreaChild(stopArea, child);
                            break;
                        default:
                            Logging.warn("Unsupported child type: "+child.getStopAreaExtension().getAreaType());
                    }
                }

            } else if (childId.contains("StopPoint")) {
                StopPoint child = findStopPoint(childId);
                if (child == null) {
                    Logging.warn("Cannot find StopPoint: "+childId);
                } else {
                    // TODO
                    Logging.info("TODO: handle StopPoint "+childId);
                }

            } else {
                Logging.warn("Unsupported child: "+childId);
            }
        }
    }

    private void parseStopAreaChild(Relation stopArea, StopArea child) {
        for (String grandchildId : child.getContains()) {
            if (grandchildId.contains("StopPoint")) {
                StopPoint grandchild = findStopPoint(grandchildId);
                if (grandchild == null) {
                    Logging.warn("Cannot find StopPoint: "+grandchildId);
                } else {
                    if (grandchild.getLongLatType().equals(LongLatTypeType.WGS_84)) {
                        Node platform = createPlatform(grandchild);
                        stopArea.addMember(new RelationMember(OSM_PLATFORM, platform));
                    } else {
                        Logging.warn("Unsupported long/lat type: "+grandchild.getLongLatType());
                    }
                }
            } else {
                Logging.warn("Unsupported grandchild: "+grandchildId);
            }
        }
        String centroidId = child.getCentroidOfArea();
        AreaCentroid areaCentroid = findAreaCentroid(centroidId);
        if (areaCentroid == null) {
            Logging.warn("Cannot find AreaCentroid: "+centroidId);
        } else if (!areaCentroid.getLongLatType().equals(LongLatTypeType.WGS_84)) {
            Logging.warn("Unsupported long/lat type: "+areaCentroid.getLongLatType());
        } else {
            for (RelationMember member : stopArea.getMembers()) {
                // Fix stop coordinates if needed
                if (member.getRole().equals(OSM_PLATFORM) && isNullLatLon(member.getNode())) {
                    member.getNode().setCoor(createLatLon(areaCentroid));
                }
            }
        }
    }

    private static boolean addStopToRoute(Relation route, OsmPrimitive stop) {
        if (route.getMembersCount() == 0 || !route.getMember(route.getMembersCount()-1).getMember().equals(stop)) {
            route.addMember(new RelationMember(stop.get(OSM_PUBLIC_TRANSPORT), stop));
            return true;
        } else {
            return false;
        }
    }

    public static List<URL> getSchemas() {
        return schemas;
    }

    public static void registerSchema(URL resource) {
        if (resource != null && !schemas.contains(resource)) {
            schemas.add(resource);
        }
    }
}
