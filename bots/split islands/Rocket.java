import bc.*;
import java.util.*;

public class Rocket {

    static Unit curUnit;
    static GameController gc;
    static Direction[] directions = Direction.values();
    static HashSet<Integer> workersPerRocket = new HashSet<Integer>();
    static HashMap<Integer, Integer> turnsSinceBuilt = new HashMap<Integer, Integer>();
    //static HashMap<Integer, HashSet<Integer>> assignedUnits = new HashMap<Integer, HashSet<Integer>>();
    static HashSet<Integer> assignedUnits = new HashSet<Integer>();
    static int landSpotNumber = 0;

    public static void run(GameController gc, Unit curUnit) {

        Rocket.curUnit = curUnit;

        if (curUnit.location().isInSpace()) {
            //in space
            return;
        } else if (curUnit.location().isOnPlanet(Planet.Earth)) {
            if (curUnit.health() == curUnit.maxHealth()) {
                if (gc.round() == 749) {
                    launch();
                }
                if (!turnsSinceBuilt.containsKey(curUnit.id())) {
                    turnsSinceBuilt.put(curUnit.id(), 1);
                } else if (turnsSinceBuilt.get(curUnit.id()) > 100 && curUnit.structureGarrison().size() != 0) {
                    launch();
                } else {
                    turnsSinceBuilt.put(curUnit.id(), turnsSinceBuilt.get(curUnit.id())+1);
                }
            } else if (curUnit.structureGarrison().size() != 0) {
                launch();
            }
            //on earth, load units
            VecUnit adjacentUnits = gc.senseNearbyUnitsByTeam(curUnit.location().mapLocation(), 2, Player.myTeam);
            //TODO prioritize certain units over others
            for (int i = 0; i < adjacentUnits.size(); i++) {
                int id = adjacentUnits.get(i).id();
                if (gc.canLoad(curUnit.id(), id) && Player.priorityTarget.containsKey(id)) {
                    if (!(workersPerRocket.contains(curUnit.id()) && adjacentUnits.get(i).unitType() == UnitType.Worker)) {
                        Player.priorityTarget.remove(id);
                        gc.load(curUnit.id(), adjacentUnits.get(i).id());
                        if (adjacentUnits.get(i).unitType() == UnitType.Worker) {
                            workersPerRocket.add(curUnit.id());
                        }
                    }
                }
            }

            if (curUnit.structureMaxCapacity() == curUnit.structureGarrison().size()) {
                launch();
            }
        } else {
            //on mars, unload units
            if (Player.gotoableEmpty) {
                Player.initialize();
                Player.chooseTarget();
            }
            MapLocation curLoc = curUnit.location().mapLocation();
            if (!Player.parentWorker.containsKey(curUnit.id())) {
            for (int i = 0; i < Player.gotoable.length; i++) {
                if (Player.gotoable[i][curLoc.getX()][curLoc.getY()]) {
                    Player.parentWorker.put(curUnit.id(), i);
                    break;
                }
            }
        }
            for (int i = 0; i < directions.length; i++) {
                if (gc.canUnload(curUnit.id(), directions[i])) {
                    gc.unload(curUnit.id(), directions[i]);
                    Player.parentWorker.put(gc.senseUnitAtLocation(curUnit.location().mapLocation().add(directions[i])).id(), Player.parentWorker.get(curUnit.id()));
                }
            }
        }

    }

    public static int hash(MapLocation loc) {
        return 69 * loc.getX() + loc.getY();
    }

    public static void launch() {
        //Player.loadingRocket = false;
        //Player.enemyLocation = Player.pastLoc;

        int hashLoc = gc.getTeamArray(Planet.Mars).get(landSpotNumber);
        int y = hashLoc % 69;
        int x = (hashLoc - y) / 69;
        //System.out.println("X coordinate: " + Integer.toString(x));
        //System.out.println("Y coordinate: " + Integer.toString(y));

        //launch
        MapLocation marsStart = new MapLocation(Planet.Mars, x, y);
        if (gc.canLaunchRocket(curUnit.id(), marsStart)) {
            gc.launchRocket(curUnit.id(), marsStart);
            landSpotNumber++;
        } else {
            //System.out.println("Rocket precomputation borked");
        }
    }
}
