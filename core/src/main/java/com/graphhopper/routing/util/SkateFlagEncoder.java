package com.graphhopper.routing.util;

import com.graphhopper.reader.ReaderRelation;
import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.profiles.DecimalEncodedValue;
import com.graphhopper.routing.profiles.EncodedValue;
import com.graphhopper.routing.profiles.UnsignedDecimalEncodedValue;
import com.graphhopper.routing.weighting.PriorityWeighting;
import com.graphhopper.storage.IntsRef;
import com.graphhopper.util.PMap;

import java.util.*;

import static com.graphhopper.routing.util.EncodingManager.getKey;
import static com.graphhopper.routing.util.PriorityCode.*;
import static com.graphhopper.routing.util.PriorityCode.AVOID_IF_POSSIBLE;
import static com.graphhopper.routing.util.PriorityCode.VERY_NICE;
import static com.graphhopper.routing.util.PriorityCode.PREFER;

/** Dieser FlagEncoder definiert das Bit-Layout für das Skateboardfahren (Geschwindigkeit, Zugang, Oberfläche, Beschaffenheit ...).
 * auf Grundlage des AbstractFlagEncoder */
public class SkateFlagEncoder extends AbstractFlagEncoder {

    private final Set<String> excludeSurfaces = new HashSet<>(); /** Oberflächen ausschließen  */

    private final Set<String> excludeSmoothness = new HashSet<>(); /** Beschaffenheit von Oberflächen ausschließen  */

    private final Set<String> excludeHighwayTags = new HashSet<>(); /** Straßenwerte ausschließen  */

    private final Map<String, Integer> highwaySpeeds = new HashMap<>(); /** Geschwindigkeit auf der Straße  */

    protected boolean speedTwoDirections;


    /** Geschwindigkeiten */
    static final int SLOW_SPEED = 2;
    static final int MEAN_SPEED = 5;
    static final int FERRY_SPEED = 15;


    /** Straßenwerte berücksichtigen (safe, allowed, avoid)  */
    final Set<String> safeHighwayTags = new HashSet<>();
    final Set<String> allowedHighwayTags = new HashSet<>();
    final Set<String> avoidHighwayTags = new HashSet<>();
    // convert network tag of hiking routes into a way route code
    final Map<String, Integer> hikingNetworkToCode = new HashMap<>();
    protected HashSet<String> sidewalkValues = new HashSet<>(5);
    protected HashSet<String> sidewalksNoValues = new HashSet<>(5);

    /** Wird verwendet, um einen Prioritätswert in den Art-Flags einer Kante zu speichernt */
    private DecimalEncodedValue priorityWayEncoder;
    private EncodedValueOld relationCodeEncoder;


    protected void setHighwaySpeed(String highway, int speed) {
        highwaySpeeds.put(highway, speed);
    }


    public SkateFlagEncoder() {
        this(4, 1);
    }

    /** (Super-)Konstruktor wird aufgerufen. Z.B. speedBits gibt an,
     * wie viele Bits für die Geschwindigkeitsinformationen reserviert werden sollen, speedFactor gibt an,
     * durch welchen Faktor die Geschwindigkeit vor dem Speichern geteilt werden soll */
    public SkateFlagEncoder(PMap properties) {
        this((int) properties.getLong("speedBits", 4),
                properties.getDouble("speedFactor", 1));
        this.properties = properties;
        this.setBlockFords(properties.getBool("block_fords", true));
    }

    public SkateFlagEncoder(String propertiesStr) {
        this(new PMap(propertiesStr));
    }

    public SkateFlagEncoder(int speedBits, double speedFactor) {
        super(speedBits, speedFactor, 0);

        /** eingeschränkte Werte*/
        restrictions.addAll(Arrays.asList("foot", "access"));
        restrictedValues.add("private");
        restrictedValues.add("no");
        restrictedValues.add("restricted");
        restrictedValues.add("military");
        restrictedValues.add("emergency");

        /** vorgesehene Werte*/
        intendedValues.add("yes");
        intendedValues.add("designated");
        intendedValues.add("official");
        intendedValues.add("permissive");

        /** Gehweg Werte (vorhanden, nicht vorhanden, beide Seiten, nur auf der rechten Seite der Straße, ...)*/
        sidewalksNoValues.add("no");
        sidewalksNoValues.add("none");
        sidewalksNoValues.add("separate");

        sidewalkValues.add("yes");
        sidewalkValues.add("both");
        sidewalkValues.add("left");
        sidewalkValues.add("right");

        /** absolute und potenzielle Hindernisse fürs skaten */
        setBlockByDefault(false);
        absoluteBarriers.add("fence");
        absoluteBarriers.add("handrail");
        absoluteBarriers.add("wall");
        absoluteBarriers.add("turnstile");
        potentialBarriers.add("kerb");
        potentialBarriers.add("cattle_grid");
        potentialBarriers.add("motorcycle_barrier");

        /** sichere Straßen auf den skaten erlaubt ist */
        safeHighwayTags.add("footway");
        safeHighwayTags.add("path");
        safeHighwayTags.add("pedestrian");
        safeHighwayTags.add("living_street");
        safeHighwayTags.add("residential");
        safeHighwayTags.add("service");
        safeHighwayTags.add("platform");

        /** Straßen auf den skaten nicht erlaubt bzw. gefährlich ist */
        avoidHighwayTags.add("trunk");
        avoidHighwayTags.add("trunk_link");
        avoidHighwayTags.add("primary");
        avoidHighwayTags.add("primary_link");
        avoidHighwayTags.add("secondary");
        avoidHighwayTags.add("secondary_link");
        avoidHighwayTags.add("tertiary");
        avoidHighwayTags.add("tertiary_link");
        avoidHighwayTags.add("steps");
        avoidHighwayTags.add("track");
        avoidHighwayTags.add("cycleway");

        /** erlaubte Straßenwerte */
        allowedHighwayTags.addAll(safeHighwayTags);
        allowedHighwayTags.add("unclassified");
        allowedHighwayTags.add("road");

        /** ausgeschlossene Oberflächen Werte*/
        excludeSurfaces.add("cobblestone");
        excludeSurfaces.add("gravel");
        excludeSurfaces.add("sand");

        /** ausgeschlossene Beschaffenheit Werte */
        excludeSmoothness.add("bad");
        excludeSmoothness.add("very_bad");
        excludeSmoothness.add("horrible");
        excludeSmoothness.add("very_horrible");
        excludeSmoothness.add("impassable");

        /** Geschwindigkeit anpassen je nach Straßenart */
        setHighwaySpeed("living_street", MEAN_SPEED);
        setHighwaySpeed("path", FERRY_SPEED);
        setHighwaySpeed("footway", MEAN_SPEED);
        setHighwaySpeed("platform", MEAN_SPEED);
        setHighwaySpeed("pedestrian", MEAN_SPEED);
        setHighwaySpeed("service", MEAN_SPEED);
        setHighwaySpeed("residential", MEAN_SPEED);
        // no other highway applies:
        setHighwaySpeed("unclassified", FERRY_SPEED);

        hikingNetworkToCode.put("iwn", UNCHANGED.getValue());
        hikingNetworkToCode.put("nwn", UNCHANGED.getValue());
        hikingNetworkToCode.put("rwn", UNCHANGED.getValue());
        hikingNetworkToCode.put("lwn", UNCHANGED.getValue());


        /** Geschwindigkeiten anpassen */
        maxPossibleSpeed = FERRY_SPEED;
        speedDefault = MEAN_SPEED; //MEAN_SPEED ist 5 (Fußgänger geschwindigkeit)
        speedTwoDirections = true;
        init();
    }

    @Override
    public int getVersion() {
        return 5;
    }

    /** Definiert Bits, die für Kantenflags verwendet werden, die für access, Geschwindigkeit usw. verwendet werden */
    @Override
    public void createEncodedValues(List<EncodedValue> registerNewEncodedValue, String prefix, int index) {
        // Die ersten beiden Bits sind für die Routenverarbeitung in der Oberklasse reserviert
        super.createEncodedValues(registerNewEncodedValue, prefix, index);
        registerNewEncodedValue.add(speedEncoder = new UnsignedDecimalEncodedValue(getKey(prefix, "average_speed"), speedBits, speedFactor, true));
        registerNewEncodedValue.add(priorityWayEncoder = new UnsignedDecimalEncodedValue(getKey(prefix, "priority"), 3, PriorityCode.getFactor(1), false));
    }

    /** Definiert die Bits, die für Beziehungsflags verwendet werden. */
    @Override
    public int defineRelationBits(int index, int shift) {
        relationCodeEncoder = new EncodedValueOld("RelationCode", shift, 3, 1, 0, 7);
        return shift + relationCodeEncoder.getBits();
    }


    /** unveränderte Methoden aus AbstractFlagEncoder */
    @Override
    public int defineTurnBits(int index, int shift) {
        return shift;
    }

    @Override
    public boolean isTurnRestricted(long flags) {
        return false;
    }

    @Override
    public double getTurnCost(long flag) {
        return 0;
    }

    @Override
    public long getTurnFlags(boolean restricted, double costs) {
        return 0;
    }


    /** Manager-Klasse zum Registrieren von Encodern, Zuweisen der Flag-Werte und
     * Überprüfen von Objekten */
    @Override
    public EncodingManager.Access getAccess(ReaderWay way) {
        String highwayValue = way.getTag("highway");
        if (highwayValue == null) {
            EncodingManager.Access acceptPotentially = EncodingManager.Access.CAN_SKIP;

            if (way.hasTag("route", ferries)) {
                String footTag = way.getTag("foot");
                if (footTag == null || intendedValues.contains(footTag))
                    acceptPotentially = EncodingManager.Access.FERRY;
            }

            // Sonderfälle
            if (way.hasTag("railway", "platform"))
                acceptPotentially = EncodingManager.Access.CAN_SKIP;

            if (way.hasTag("man_made", "pier"))
                acceptPotentially = EncodingManager.Access.WAY;

            if (!acceptPotentially.canSkip()) { //  Encoder solle die oben eingeschränkten Werte skippen
                if (way.hasTag(restrictions, restrictedValues) && !getConditionalTagInspector().isRestrictedWayConditionallyPermitted(way))
                    return EncodingManager.Access.CAN_SKIP;
                return acceptPotentially;
            }

            return EncodingManager.Access.CAN_SKIP;
        }


        //  Encoder solle hiking (Wanderwege) Werte skippen
        if (way.hasTag("sac_scale"))
            return EncodingManager.Access.CAN_SKIP;


        if (way.hasTag("foot", intendedValues))
            return EncodingManager.Access.WAY;

        //  Zugangsbeschränkungen überprüfen
        if (way.hasTag(restrictions, restrictedValues) && !getConditionalTagInspector().isRestrictedWayConditionallyPermitted(way))
            return EncodingManager.Access.CAN_SKIP;

        // Gehwege mit Werten sollen mit berücksichtigt werden
        if (way.hasTag("sidewalk", sidewalkValues))
            return EncodingManager.Access.WAY;

        //  nicht erlaubte Straßen werden übersprungen
        if (!allowedHighwayTags.contains(highwayValue))
            return EncodingManager.Access.CAN_SKIP;

        if (way.hasTag("motorroad", "yes"))
            return EncodingManager.Access.CAN_SKIP;

        //  keine nassen Wege
        if (isBlockFords() && (way.hasTag("highway", "ford") || way.hasTag("ford")))
            return EncodingManager.Access.CAN_SKIP;

        if (getConditionalTagInspector().isPermittedWayConditionallyRestricted(way))
            return EncodingManager.Access.CAN_SKIP;

        //  Straßen die vermieden werden sollen und keinen Bürgersteig haben sollen übersprungen werden
        if (way.hasTag("highway", avoidHighwayTags) && !way.hasTag("sidewalk", sidewalkValues)) {
            return EncodingManager.Access.CAN_SKIP;
        }

        //  ausgeschlossene Oberflächen Werte sollen übersprungen werden
        if (way.hasTag("surface", excludeSurfaces))
            return EncodingManager.Access.CAN_SKIP;

        //  ausgeschlossene Oberflächenbeschaffenheiten Werte sollen übersprungen werden
        if (way.hasTag("smoothness", excludeSmoothness))
            return EncodingManager.Access.CAN_SKIP;


        //  Bordsteine werden überprüft, ob skaten möglich ist.
        if (way.hasTag("kerb", "lowered")){ //  abgesenkte Bordsteine sind möglich
            return EncodingManager.Access.WAY;
        }
        if (way.hasTag("kerb", "flush")){   //  Bordsteine auf Straßenebene sind möglich
            return EncodingManager.Access.WAY;
        }
        if (way.hasTag("kerb", "no")){  //  keine Bordsteine sind möglich
            return EncodingManager.Access.WAY;
        }
        if (way.hasTag("kerb", "rolled")){  //  gerollte Bordsteine sind möglich
            return EncodingManager.Access.WAY;
        }
        if (way.hasTag("kerb", "raised")) { //  erhöhte Bordsteine sind nicht möglich
            return EncodingManager.Access.CAN_SKIP;
        }

        return EncodingManager.Access.WAY;
    }


    /** Analysiert die Eigenschaften einer Relation und erstellt die Routing-Flags.
     * Diese Methode wird aufgerufen, um die nützlichen Beziehungs-Tags zu ermitteln. */
    @Override
    public long handleRelationTags(long oldRelationFlags, ReaderRelation relation) {
        int code = 0;
        if (relation.hasTag("route", "hiking") || relation.hasTag("route", "foot")) {
            Integer val = hikingNetworkToCode.get(relation.getTag("network"));
            if (val != null)
                code = val;
            else
                code = hikingNetworkToCode.get("lwn");
        } else if (relation.hasTag("route", "ferry")) {
            code = PriorityCode.AVOID_IF_POSSIBLE.getValue();
        }

        int oldCode = (int) relationCodeEncoder.getValue(oldRelationFlags);
        if (oldCode < code)
            return relationCodeEncoder.setValue(0, code);
        return oldRelationFlags;
    }

    /** Analysiert Sie die Eigenschaften eines Weges und erstellt Sie die Randmarkierungen. */
    @Override
    public IntsRef handleWayTags(IntsRef edgeFlags, ReaderWay way, EncodingManager.Access access, long relationFlags) {
        if (access.canSkip())
            return edgeFlags;

        accessEnc.setBool(false, edgeFlags, true);
        accessEnc.setBool(true, edgeFlags, true);
        if (!access.isFerry()) {
            setSpeed(true,edgeFlags, MEAN_SPEED);

        } else {
            double ferrySpeed = getFerrySpeed(way);
            setSpeed(true, edgeFlags, ferrySpeed);

        }
        int priorityFromRelation = 0;
        if (relationFlags != 0)
            priorityFromRelation = (int) relationCodeEncoder.getValue(relationFlags);

        priorityWayEncoder.setDecimal(false, edgeFlags, PriorityCode.getFactor(handlePriority(way, priorityFromRelation)));
        return edgeFlags;
    }

    /** Hier wird jeder Priorität eine Gewichtung hinzugefügt. Diese sortierte Karte ermöglicht
     * Unterklassen zum „Einfügen“ wichtigerer Prioritäten sowie zum Überschreiben festgelegter Prioritäten */
    protected int handlePriority(ReaderWay way, int priorityFromRelation) {
        TreeMap<Double, Integer> weightToPrioMap = new TreeMap<>();
        if (priorityFromRelation == 0)
            weightToPrioMap.put(0d, UNCHANGED.getValue());
        else
            weightToPrioMap.put(110d, priorityFromRelation);

        collect(way, weightToPrioMap);

        // wählt die Priorität mit dem größten Wert
        return weightToPrioMap.lastEntry().getValue();
    }

    /** PriorityCode {
     WORST(0),
     AVOID_AT_ALL_COSTS(1),
     REACH_DEST(2),
     AVOID_IF_POSSIBLE(3),
     UNCHANGED(4),
     PREFER(5),
     VERY_NICE(6),
     BEST(7); */

    /** mehr Prioritäten mit collect */

    void collect(ReaderWay way, TreeMap<Double, Integer> weightToPrioMap) {
        String highway = way.getTag("highway");
        if (way.hasTag("smoothness", "excellent"))  //  Routen mit exzellenter Oberflächenbeschaffenheit werden am stärksten bevorzugt
            weightToPrioMap.put(100d, VERY_NICE.getValue());

        if (way.hasTag("foot", "designated"))  //  Routen die spezielle für Fußgänger vorgesehen werden auch bevorzugt. (denn skaten ist nur auf Fußgängerwege erlaubt)
            weightToPrioMap.put(100d, PREFER.getValue());

        if (way.hasTag("smoothness", "good"))  //  Routen mit guter Oberflächenbeschaffenheit werden auch bevorzugt
            weightToPrioMap.put(100d, PREFER.getValue());


        //  Straßen die erlaubt sind oder eine Geschwindigkeit von max. 20 haben werden priorisiert.
        double maxSpeed = getMaxSpeed(way);
        if (safeHighwayTags.contains(highway) || maxSpeed > 0 && maxSpeed <= 20) {
            weightToPrioMap.put(40d, PREFER.getValue());
            if (way.hasTag("tunnel", intendedValues)) {
                if (way.hasTag("sidewalk", sidewalksNoValues))
                    weightToPrioMap.put(40d, AVOID_IF_POSSIBLE.getValue());
                else
                    weightToPrioMap.put(40d, UNCHANGED.getValue());
            }
            //  Straßen die vermieden werden oder eine hohe maximale Geschwindigkeit haben werden nicht priorisiert und ausgewichen.
        } else if (maxSpeed > 50 || avoidHighwayTags.contains(highway)) {
            if (!way.hasTag("sidewalk", sidewalkValues))
                weightToPrioMap.put(45d, AVOID_IF_POSSIBLE.getValue());
        }

        //  Fahrradwege sind fürs skaten nicht erlaubt.
        if (way.hasTag("bicycle", "official") || way.hasTag("bicycle", "designated"))
            weightToPrioMap.put(44d, AVOID_IF_POSSIBLE.getValue());
    }

    public boolean supports(Class<?> feature) {
        if (super.supports(feature))
            return true;

        return PriorityWeighting.class.isAssignableFrom(feature);
    }

    double getSpeed(boolean reverse, IntsRef edgeFlags) {
        double speed = super.getSpeed(reverse, edgeFlags);
        if (speed == getMaxSpeed()) {
            // We cannot be sure if it was a long or a short trip
            return SHORT_TRIP_FERRY_SPEED;
        }
        return speed;
    }

    public String toString() {
        return "skate";
    }


}
