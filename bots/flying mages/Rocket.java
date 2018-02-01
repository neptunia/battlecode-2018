import bc.*;
import java.util.*;

public class Rocket {

    static Unit curUnit;
    static MapLocation curLoc;
    static GameController gc;
    static Direction[] directions = Direction.values();
    static int myId;
    static HashSet<Integer> workersPerRocket = new HashSet<Integer>();
    //static HashMap<Integer, HashSet<Integer>> assignedUnits = new HashMap<Integer, HashSet<Integer>>();
    static HashSet<Integer> assignedUnits = new HashSet<Integer>();
    static int landSpotNumber = 0;
    static boolean initialized = false;

    public static void run(Unit curUnit) {

        Rocket.curUnit = curUnit;


        if (curUnit.location().isInSpace()) {
            //in space
            return;
        }

        Rocket.curLoc = curUnit.location().mapLocation();

        if (curUnit.location().isOnPlanet(Planet.Earth)) {
            if (curUnit.health() == curUnit.maxHealth()) {
                if (gc.round() == 749) {
                    launch();
                }
            } else if (curUnit.structureGarrison().size() != 0) {
                launch();
            }
            //on earth, load units
            VecUnit adjacentUnits = gc.senseNearbyUnitsByTeam(curUnit.location().mapLocation(), 2, Player.myTeam);
            for (int i = 0; i < adjacentUnits.size(); i++) {
                Unit unit = adjacentUnits.get(i);
                int id = unit.id();
                if (gc.canLoad(curUnit.id(), id) && Player.priorityTarget.containsKey(id)) {
                    Player.priorityTarget.remove(id);
                    gc.load(curUnit.id(), id);
                    if (unit.unitType() == UnitType.Ranger) {
                        Worker.numRangerGoingToRocket--;
                    } else if (unit.unitType() == UnitType.Healer) {
                        Worker.numHealerGoingToRocket--;
                    }
                }
            }

            if (curUnit.structureMaxCapacity() == curUnit.structureGarrison().size()) {
                launch();
            }
        } else {
            //initialize
            if (!initialized) {
                Player.marsInitialize();
                initialized = true;
            }
            //assign a mars id
            if (!Worker.id.containsKey(curUnit.id())) {
                for (int i = 0; i < Player.gotoable.length; i++) {
                    if (Player.gotoable[i][curLoc.getX()][curLoc.getY()]) {
                        Worker.id.put(curUnit.id(), i);
                        break;
                    }
                }
            }
            
            myId = Worker.id.get(curUnit.id());
            //on mars, unload units
            MapLocation curLoc = curUnit.location().mapLocation();
            for (int i = 0; i < directions.length; i++) {
                if (gc.canUnload(curUnit.id(), directions[i])) {
                    gc.unload(curUnit.id(), directions[i]);
                    int newId = gc.senseUnitAtLocation(curUnit.location().mapLocation().add(directions[i])).id();
                    Worker.id.put(newId, myId);
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
            HashSet<Integer> cantGo = new HashSet<Integer>();
            MapLocation current = curUnit.location().mapLocation();
            for (int i = 0; i < directions.length; i++) {
                cantGo.add(hash(current.add(directions[i])));
            }
            for (int i = 0; i < directions.length && directions[i] != Direction.Center; i++) {
                if (gc.hasUnitAtLocation(current.add(directions[i]))) {
                    rocketMoveAway(current.add(directions[i]), cantGo);
                }
            }
            gc.launchRocket(curUnit.id(), marsStart);
            landSpotNumber++;
        } else {
            //System.out.println("Rocket precomputation borked");
        }
    }

    public static boolean rocketMoveAway(MapLocation toGo, HashSet<Integer> cantGo) {
        if (gc.hasUnitAtLocation(toGo)) {
            Unit unit = gc.senseUnitAtLocation(toGo);
            if (!gc.isMoveReady(unit.id()) || unit.unitType() == UnitType.Factory || unit.unitType() == UnitType.Rocket) {
                return false;
            }
            for (int i = 0; i < directions.length; i++) {
                MapLocation temp = toGo.add(directions[i]);
                if (!cantGo.contains(hash(temp)) && onMap(temp) && Player.gotoable[myId][temp.getX()][temp.getY()]) {
                    HashSet<Integer> tempCantGo = new HashSet<Integer>();
                    tempCantGo.addAll(cantGo);
                    tempCantGo.add(hash(temp));
                    if (rocketMoveAway(temp, tempCantGo)) {
                        gc.moveRobot(unit.id(), directions[i]);
                        return true;
                    }
                }
            }
            return false;
        } else {
            return true;
        }
    }

    public static boolean onMap(MapLocation loc) {
        int x = loc.getX();
        int y = loc.getY();
        return x >= 0 && y >= 0 && x < Player.gridX && y < Player.gridY;
    }
}