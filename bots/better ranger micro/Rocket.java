import bc.*;
import java.util.*;

public class Rocket {

    static Unit curUnit;
    static GameController gc;
    static Direction[] directions = Direction.values();

    public static void run(GameController gc, Unit curUnit) {

        Rocket.curUnit = curUnit;

        if (curUnit.location().isInSpace()) {

            //in space
            return;
        } else if (curUnit.location().isOnPlanet(Planet.Earth)) {
            //on earth, load units
            VecUnit adjacentUnits = gc.senseNearbyUnitsByTeam(curUnit.location().mapLocation(), 2, Player.myTeam);
            //TODO prioritize certain units over others
            for (int i = 0; i < adjacentUnits.size(); i++) {
                if (gc.canLoad(curUnit.id(), adjacentUnits.get(i).id())) {
                    gc.load(curUnit.id(), adjacentUnits.get(i).id());
                }
            }

            if (curUnit.structureMaxCapacity() == curUnit.structureGarrison().size()) {
                PlanetMap marsmap = gc.startingMap(Planet.Mars);
                marsmap.setPlanet(Planet.Mars);
                //garrison is full, launch
                //first, try starting coords on mars
                MapLocation marsStart = new MapLocation(Planet.Mars, curUnit.location().mapLocation().getX(), curUnit.location().mapLocation().getY());
                if (gc.canLaunchRocket(curUnit.id(), marsStart)) {
                    gc.launchRocket(curUnit.id(), marsStart);
                    int myHash = hash(curUnit.location().mapLocation());
                    //TODO make more efficient
                    for (Integer key : Factory.presetTargets.keySet()) {
                        try {
                            if (myHash == hash(Factory.presetTargets.get(key))) {
                                Factory.presetTargets.remove(key);
                                System.out.println("Removed");
                                break;
                            }
                        } catch (Exception e) {};
                    }
                    //Ranger.priorityTarget.values().removeAll(Collections.singleton(myHash));
                } else {
                    for (int i = 0; i < marsmap.getWidth(); i++) {
                        boolean stop = false;
                        for (int j = 0; j < marsmap.getHeight(); j++) {
                            MapLocation temp = new MapLocation(Planet.Mars, (int) ((curUnit.location().mapLocation().getX() + i) % marsmap.getWidth()), (int) ((curUnit.location().mapLocation().getY() + j) % marsmap.getWidth()));
                            if (gc.canLaunchRocket(curUnit.id(), temp)) {
                                gc.launchRocket(curUnit.id(), temp);
                                int myHash = hash(curUnit.location().mapLocation());
                                //TODO make more efficient
                                for (Integer key : Factory.presetTargets.keySet()) {
                                    try {
                                        if (myHash == hash(Factory.presetTargets.get(key))) {
                                            Factory.presetTargets.remove(key);
                                            System.out.println("Removed");
                                            break;
                                        }
                                    } catch (Exception e) {};
                                }
                                //Ranger.priorityTarget.values().removeAll(Collections.singleton(myHash));
                                stop = true;
                                break;
                            }
                        }
                        if (stop) {
                            break;
                        }
                    }
                }
            }
        } else {
            //on mars, unload units
            if (Player.gotoableEmpty) {
                Player.initialize();
                Player.chooseTarget();
            }
            for (int i = 0; i < directions.length; i++) {
                if (gc.canUnload(curUnit.id(), directions[i])) {
                    gc.unload(curUnit.id(), directions[i]);
                }
            }
        }

    }

    public static int hash(MapLocation loc) {
        return 69 * loc.getX() + loc.getY();
    }
}
