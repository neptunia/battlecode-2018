import bc.*;
import java.util.*;

public class Worker {

	static Unit curUnit;
	static MapLocation curLoc;
	static GameController gc;
	static MapLocation[][] spots;
	static Direction[] directions = Direction.values();
	static int[] counter;
	static int myId;
    static boolean noMoreKarbonite = false;
	static HashMap<Integer, Integer> id = new HashMap<Integer, Integer>();
	static HashMap<Integer, MapLocation> target = new HashMap<Integer, MapLocation>();
	static HashSet<Integer> spotsTaken = new HashSet<Integer>();
    static HashSet<Integer> structuresToBuild = new HashSet<Integer>();
    static HashMap<Integer, Blueprint> structures = new HashMap<Integer, Blueprint>();
    static HashMap<Integer, Integer> numberWorkersAssigned = new HashMap<Integer, Integer>();
    static boolean[][] karbonitePatches;
    static HashSet<Integer> patchesOccupied = new HashSet<Integer>();
    //static HashSet<Integer> placedAlready = new HashSet<Integer>();
    static int[] replicationLimit;

	public static void run(Unit curUnit) {

		Worker.curUnit = curUnit;

        if (curUnit.location().isInGarrison()) {
            return;
        }

        Worker.myId = id.get(curUnit.id());
		curLoc = curUnit.location().mapLocation();

        if (gc.planet() == Planet.Mars) {
            return;
        }

        if (structures.containsKey(curUnit.id())) {
            buildStructure();
            return;
        }

        if (Player.prevBlocked < 15 && gc.karbonite() + Player.karboniteGonnaUse >= 200 && Player.numFactory + structuresToBuild.size() < 5 && gc.round() != 1) {
            startStructure(UnitType.Factory);
            Worker.run(curUnit);
        } else if (gc.karbonite() + Player.karboniteGonnaUse >= 130 && gc.researchInfo().getLevel(UnitType.Rocket) > 0) {
            startStructure(UnitType.Rocket);
            Worker.run(curUnit);
        } else {
            goMine();
        }
		
	}

    public static void buildStructure() {
        Blueprint toBuild = structures.get(curUnit.id());
        if (gc.hasUnitAtLocation(toBuild.loc) && gc.senseUnitAtLocation(toBuild.loc).unitType() == toBuild.type) {
            Unit theBlueprint = gc.senseUnitAtLocation(toBuild.loc);
            if (theBlueprint.structureIsBuilt() != 0) {
                //finished building it
                numberWorkersAssigned.remove(hash(toBuild.loc));
                structures.remove(curUnit.id());
                structuresToBuild.remove(hash(toBuild.loc));
                if (toBuild.type == UnitType.Rocket && !Rocket.assignedUnits.contains(theBlueprint.id())) {
                    assignUnits(toBuild.loc);
                    Rocket.assignedUnits.add(theBlueprint.id());
                }
                Player.newUnits.add(curUnit);
                return;
            }
            if (manDistance(curLoc, toBuild.loc) == 2 && gc.isMoveReady(curUnit.id())) {
                //one step away from it
                MapLocation temp = null;
                for (int i = 0; i < directions.length; i++) {
                    temp = curLoc.add(directions[i]);
                    if (manDistance(temp, toBuild.loc) == 1) {
                        HashSet<Integer> cantGo = new HashSet<Integer>();
                        cantGo.add(hash(curLoc));
                        if (makeWay(temp, cantGo, toBuild.loc) && gc.canMove(curUnit.id(), directions[i])) {
                            gc.moveRobot(curUnit.id(), directions[i]);
                            curLoc = curLoc.add(directions[i]);
                            break;
                        }
                    }
                }
            } else if (manDistance(curLoc, toBuild.loc) == 1) {
                //TODO: replicate around it if factory doesnt already have 8 workers
                if (numberWorkersAssigned.get(hash(toBuild.loc)) < 8 && Player.numWorker < 6 && curUnit.abilityHeat() < 10) {
                    MapLocation temp = null;
                    for (int i = 0; i < directions.length; i++) {
                        temp = curLoc.add(directions[i]);
                        if (manDistance(temp, toBuild.loc) == 1) {
                            HashSet<Integer> cantGo = new HashSet<Integer>();
                            cantGo.add(hash(curLoc));
                            if (makeWay(temp, cantGo, toBuild.loc) && gc.canReplicate(curUnit.id(), directions[i])) {
                                gc.replicate(curUnit.id(), directions[i]);
                                Unit newUnit = gc.senseUnitAtLocation(curLoc.add(directions[i]));
                                Player.numWorker++;
                                id.put(newUnit.id(), myId);
                                structures.put(newUnit.id(), toBuild);
                                Player.newUnits.add(newUnit);
                                //numberWorkersAssigned.put(hash(toBuild.loc), numberWorkersAssigned.get(hash(toBuild.loc)) + 1);
                                return;
                            }
                        }
                    }
                } else {
                    int blueprintId = gc.senseUnitAtLocation(toBuild.loc).id();
                    if (gc.canBuild(curUnit.id(), blueprintId)) {
                        gc.build(curUnit.id(), blueprintId);
                    } else {
                        //System.out.println("Couldn't work on blueprint :(");
                    }
                }
                
                
            } else {
                move(toBuild.loc);
                harvestAroundMe();
            }
        } else {
            //no one put it down yet
            /*if (placedAlready.contains(hash(toBuild.loc))) {
                //oh no, it got killed
                structures.remove(curUnit.id());
                Player.newUnits.add(curUnit);

            }*/
            int distanceToBlueprint = manDistance(curLoc, toBuild.loc);
            if (distanceToBlueprint == 1) {
                if (gc.hasUnitAtLocation(toBuild.loc)) {
                    HashSet<Integer> temp = new HashSet<Integer>();
                    temp.add(hash(curLoc));
                    moveAway(toBuild.loc, temp);
                }
                //im next to it, place it down
                if (gc.canBlueprint(curUnit.id(), toBuild.type, curLoc.directionTo(toBuild.loc))) {
                    gc.blueprint(curUnit.id(), toBuild.type, curLoc.directionTo(toBuild.loc));
                    int newId = gc.senseUnitAtLocation(toBuild.loc).id(); 
                    id.put(newId, myId);
                    if (toBuild.type == UnitType.Factory) {
                        Player.karboniteGonnaUse += 200;
                    } else {
                        Player.karboniteGonnaUse += 130;
                    }
                    //placedAlready.add(hash(toBuild.loc));
                } else {
                    System.out.println("Couldn't place down blueprint??");
                }
                
            } else if (distanceToBlueprint == 0) {
                moveAnywhere();
            } else {
                move(toBuild.loc);
                harvestAroundMe();
            }
        }
    }

    public static void startStructure(UnitType type) {
        MapLocation blueprintLocation = findBlueprintLocation();
        if (type == UnitType.Factory) {
            Player.karboniteGonnaUse -= 200;
        } else {
            Player.karboniteGonnaUse -= 130;
        }
        
        structuresToBuild.add(hash(blueprintLocation));
        Blueprint theBlueprint = new Blueprint(blueprintLocation, type);
        //assign workers to it
        LinkedList<MapLocation> queue = new LinkedList<MapLocation>();
        HashSet<Integer> visited = new HashSet<Integer>();
        queue.add(blueprintLocation);
        visited.add(hash(blueprintLocation));
        int workerCount = 0;
        while (!queue.isEmpty()) {
            MapLocation current = queue.poll();
            if ((manDistance(blueprintLocation, current) > 6 || workerCount == 8) && workerCount > 2) {
                numberWorkersAssigned.put(hash(blueprintLocation), workerCount);
                return;
            }
            if (gc.hasUnitAtLocation(current)) {
                Unit temp = gc.senseUnitAtLocation(current);
                if (temp.unitType() == UnitType.Worker && !structures.containsKey(temp.id())) {
                    structures.put(temp.id(), theBlueprint);
                    workerCount++;
                }
            }
            for (int i = 0; i < directions.length; i++) {
                MapLocation test = current.add(directions[i]);
                if (!visited.contains(hash(test)) && checkPassable(test)) {
                    visited.add(hash(test));
                    queue.add(test);
                }
            }
        }
        numberWorkersAssigned.put(hash(blueprintLocation), workerCount);
    }

	public static void goMine() {
		if (!target.containsKey(curUnit.id()) && !noMoreKarbonite) {
			MapLocation temp = findKarboniteSpot();
            if (temp != null) {
                target.put(curUnit.id(), temp);
                patchesOccupied.add(hash(temp));
            } else {
                noMoreKarbonite = true;
            }
		}

        if (target.containsKey(curUnit.id())) {
            move(target.get(curUnit.id()));
        } else {
            move(Player.initialWorkerStartingLocation.get(myId));
        }
		
        harvestAroundMe();
        if (Player.numWorker < replicationLimit[myId] && patchesOccupied.size() < replicationLimit[myId]) {
            //replicate
            //TODO: optimize
            MapLocation spot = findKarboniteSpot();
            if (spot != null) {
                replicateNearestTo(spot);
            }
        }
	}

    public static MapLocation findKarboniteSpot() {
        LinkedList<MapLocation> queue = new LinkedList<MapLocation>();
        HashSet<Integer> visited = new HashSet<Integer>();
        queue.add(curLoc);
        visited.add(hash(curLoc));
        while (!queue.isEmpty()) {
            MapLocation current = queue.poll();
            if (karbonitePatches[current.getX()][current.getY()] && !patchesOccupied.contains(hash(current))) {
                //found the nearest karbonite patch
                return current;
            }
            for (int i = 0; i < directions.length; i++) {
                MapLocation test = current.add(directions[i]);
                if (!visited.contains(hash(test)) && checkPassable(test)) {
                    queue.add(test);
                    visited.add(hash(test));
                }
            }
        }
        return null;
    }

    public static void harvestAroundMe() {
        long most = -1;
        Direction mostDir = null;
        for (int i = 0; i < directions.length; i++) {
            MapLocation temp = curLoc.add(directions[i]);
            if (!onMap(temp)) {
                continue;
            }
            long amnt = gc.karboniteAt(temp);
            if (onMap(temp) && amnt > 2 && gc.canHarvest(curUnit.id(), directions[i])) {
                gc.harvest(curUnit.id(), directions[i]);
                return;
            }
            if (amnt > most) {
                most = amnt;
                mostDir = directions[i];
            }
        }
        if (most == 0 && target.containsKey(curUnit.id()) && hash(target.get(curUnit.id())) == hash(curLoc)) {
            //no more karbonite at this spot
            target.remove(curUnit.id());
            //TODO: maybe another actions
        } else if (gc.canHarvest(curUnit.id(), mostDir)) {
            gc.harvest(curUnit.id(), mostDir);
        }
    }

    public static void replicateNearestTo(MapLocation loc) {
        //TODO: optimize bigly
        HashSet<Direction> done = new HashSet<Direction>();
        for (int i = 0; i < directions.length; i++) {
            Direction best = null;
            int minDistance = 99999999;
            for (int a = 0; a < directions.length; a++) {
                if (!done.contains(directions[a]) && distance(curLoc.add(directions[a]), loc) < minDistance) {
                    best = directions[a];
                    minDistance = distance(curLoc.add(directions[a]), loc);
                }
            }
            if (gc.canReplicate(curUnit.id(), best)) {
                gc.replicate(curUnit.id(), best);
                Player.numWorker++;
                Unit newUnit = gc.senseUnitAtLocation(curLoc.add(best));
                id.put(newUnit.id(), myId);
                Player.newUnits.add(newUnit);
                return;
            } else {
                done.add(best);
            }
            
        }
    }

    public static void assignUnits(MapLocation rocketLoc) {
        LinkedList<MapLocation> queue = new LinkedList<MapLocation>();
        HashSet<Integer> visited = new HashSet<Integer>();
        queue.add(rocketLoc);
        visited.add(hash(rocketLoc));
        //int workersNeeded = 1;
        int rangersNeeded = 5;
        int healersNeeded = 2;
        int workersNeeded = 1;
        while (!queue.isEmpty()) {
            MapLocation current = queue.poll();
            for (int i = 0; i < directions.length; i++) {
                MapLocation toCheck = current.add(directions[i]);
                if (checkPassable(toCheck) && !visited.contains(hash(toCheck))) {
                    queue.add(toCheck);
                    visited.add(hash(toCheck));
                }
                if (rangersNeeded == 0 && healersNeeded == 0 && workersNeeded == 0) {
                    return;
                }
                if (gc.hasUnitAtLocation(toCheck)) {
                    Unit there = gc.senseUnitAtLocation(toCheck);
                    UnitType temp = there.unitType();
                    if (!Player.priorityTarget.containsKey(there.id())) {
                        if (temp == UnitType.Ranger && rangersNeeded > 0) {
                            rangersNeeded--;
                            Player.priorityTarget.put(there.id(), rocketLoc);
                        } else if (temp == UnitType.Healer && healersNeeded > 0) {
                            healersNeeded--;
                            Player.priorityTarget.put(there.id(), rocketLoc);
                        } else if (temp == UnitType.Worker && workersNeeded > 0) {
                            workersNeeded--;
                            Player.priorityTarget.put(there.id(), rocketLoc);
                        }
                    }
                }
                
            }
        }
        System.out.println("Rip not enough units to put into rocket");
    }

    public static MapLocation findBlueprintLocation() {
        LinkedList<MapLocation> queue = new LinkedList<MapLocation>();
        HashSet<Integer> visited = new HashSet<Integer>();

        //find a blueprint location near this
        /*
        queue.add(Player.initialWorkerStartingLocation.get(myId));
        visited.add(hash(Player.initialWorkerStartingLocation.get(myId)));
        */

        queue.add(curLoc);
        visited.add(hash(curLoc));

        int mostSquaresFree = -1;
        MapLocation best = null;
        
        while (!queue.isEmpty()) {
            MapLocation current = queue.poll();
            if (manDistance(curLoc, current) >= 30) {
                return best;
            }
            
            for (int i = 0; i < directions.length; i++) {
                MapLocation test = current.add(directions[i]);
                int testHash = hash(test);
                if (!visited.contains(testHash) && checkPassable(test)) {
                    visited.add(testHash);
                    queue.add(test);
                }
            }
            int around = 0;
            for (int i = 0; i < directions.length; i++) {
                MapLocation test = current.add(directions[i]);
                boolean res = onMap(test);
                if (!res || res && !Player.gotoable[myId][test.getX()][test.getY()]) {
                    around++;
                }
            }

            
            VecUnit nearby = gc.senseNearbyUnitsByTeam(current, 2, Player.myTeam);
            for (int i = 0; i < nearby.size(); i++) {
                UnitType temp = nearby.get(i).unitType();
                //TODO maybe add unittype.rocket too
                if (temp == UnitType.Factory || temp == UnitType.Rocket) {
                    around++;
                }
            }
            for (int i = 0; i < directions.length && around == 0; i++) {
                MapLocation toTest = current.add(directions[i]);
                if (structuresToBuild.contains(hash(toTest))) {
                    around++;
                }
            }
            int tempX = current.getX();
            int tempY = current.getY();
            //on the map and gotoable and doesnt block off any spots
            if (8 - around > mostSquaresFree && !structuresToBuild.contains(hash(current))) {
                mostSquaresFree = 8 - around;
                if (mostSquaresFree == 8) {
                    return current;
                }
                best = current;
            }
        }
        //System.out.println("no good position found :(");
        return best;
    }

    static class Blueprint {
        MapLocation loc;
        UnitType type;
        Blueprint(MapLocation loc, UnitType type) {
            this.loc = loc;
            this.type = type;
        }
    }

    public static boolean makeWay(MapLocation toGo, HashSet<Integer> cantGo, MapLocation blueprintLoc) {
        if (gc.hasUnitAtLocation(toGo)) {
            Unit unit = gc.senseUnitAtLocation(toGo);
            if (!gc.isMoveReady(unit.id())) {
                return false;
            }
            for (int i = 0; i < directions.length; i++) {
                MapLocation temp = toGo.add(directions[i]);
                if (!cantGo.contains(hash(toGo)) && checkPassable(temp) && manDistance(temp, blueprintLoc) <= 1 && unit.unitType() != UnitType.Factory && unit.unitType() != UnitType.Rocket) {
                    HashSet<Integer> tempCantGo = new HashSet<Integer>();
                    tempCantGo.addAll(cantGo);
                    tempCantGo.add(hash(toGo));
                    if (makeWay(temp, tempCantGo, blueprintLoc)) {
                        gc.moveRobot(unit.id(), directions[i]);
                        return true;
                    }
                }
            }
        } else {
            //e.printStackTrace();
            return true;
        }
        return false;
    }

    public static boolean moveAway(MapLocation toGo, HashSet<Integer> cantGo) {
        try {
            Unit unit = gc.senseUnitAtLocation(toGo);
            if (!gc.isMoveReady(unit.id()) || unit.unitType() == UnitType.Factory) {
                return false;
            }
            for (int i = 0; i < directions.length; i++) {
                MapLocation temp = toGo.add(directions[i]);
                if (!cantGo.contains(hash(toGo)) && onMap(temp) && Player.gotoable[myId][temp.getX()][temp.getY()]) {
                    HashSet<Integer> tempCantGo = new HashSet<Integer>();
                    tempCantGo.addAll(cantGo);
                    tempCantGo.add(hash(toGo));
                    if (moveAway(temp, tempCantGo)) {
                        gc.moveRobot(unit.id(), directions[i]);
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            //e.printStackTrace();
            return true;
        }
        return false;
    }

    public static void move(MapLocation target) {
        int targetHash = hash(target);
        if (hash(curLoc) == targetHash || !gc.isMoveReady(curUnit.id())) {
            return;
        }
        int x = curLoc.getX();
        int y = curLoc.getY();
        int currentDist = Player.pathDistances[targetHash][x][y];
        Direction best = null;
        if (currentDist != 696969) {
            if (x < Player.gridX - 1 && Player.pathDistances[targetHash][x + 1][y] - currentDist < 0) {
                if (gc.canMove(curUnit.id(), Direction.East)) {
                	gc.moveRobot(curUnit.id(), Direction.East);
                    curLoc = curLoc.add(Direction.East);
                }
                best = Direction.East;
            } else if (x > 0 && Player.pathDistances[targetHash][x - 1][y] - currentDist < 0) {
                if (gc.canMove(curUnit.id(), Direction.West)) {
                	gc.moveRobot(curUnit.id(), Direction.West);
                    curLoc = curLoc.add(Direction.West);
                }
                best = Direction.West;
            } else if (y < Player.gridY - 1 && Player.pathDistances[targetHash][x][y + 1] - currentDist < 0) {
                if (gc.canMove(curUnit.id(), Direction.North)) {
                	gc.moveRobot(curUnit.id(), Direction.North);
                    curLoc = curLoc.add(Direction.North);
                }
                best = Direction.North;
            } else if (y > 0 && Player.pathDistances[targetHash][x][y - 1] - currentDist < 0) {
                if (gc.canMove(curUnit.id(), Direction.South)) {
                	gc.moveRobot(curUnit.id(), Direction.South);
                    curLoc = curLoc.add(Direction.South);
                }
                best = Direction.South;
            } else if (y < Player.gridY - 1 && x < Player.gridX - 1 && Player.pathDistances[targetHash][x + 1][y + 1] - currentDist < 0) {
                if (gc.canMove(curUnit.id(), Direction.Northeast)) {
                	gc.moveRobot(curUnit.id(), Direction.Northeast);
                    curLoc = curLoc.add(Direction.Northeast);
                }
                best = Direction.Northeast;
            } else if (y > 0 && x < Player.gridX - 1 && Player.pathDistances[targetHash][x + 1][y - 1] - currentDist < 0) {
                if (gc.canMove(curUnit.id(), Direction.Southeast)) {
                	gc.moveRobot(curUnit.id(), Direction.Southeast);
                    curLoc = curLoc.add(Direction.Southeast);

                }
                best = Direction.Southeast;
            } else if (x > 0 && y < Player.gridY - 1 && Player.pathDistances[targetHash][x - 1][y + 1] - currentDist < 0) {
                if (gc.canMove(curUnit.id(), Direction.Northwest)) {
                	gc.moveRobot(curUnit.id(), Direction.Northwest);
                    curLoc = curLoc.add(Direction.Northwest);
                }
                best = Direction.Northwest;
            } else if (x > 0 && y > 0 && Player.pathDistances[targetHash][x - 1][y - 1] - currentDist < 0) {
                if (gc.canMove(curUnit.id(), Direction.Southwest)) {
                	gc.moveRobot(curUnit.id(), Direction.Southwest);
                    curLoc = curLoc.add(Direction.Southwest);
                }
                best = Direction.Southwest;
            }
        } else {
            //bfs hasnt been run yet
            //Player.bfsMin(target, curLoc);
            if (Player.bfsMin(target, curLoc)) {
            	move(target);
            } else {
            	//System.out.println("cant get there worker");
            }
        }
        if (gc.isMoveReady(curUnit.id())) {
            moveCloser(target);
        }

    }

    public static int hash(MapLocation loc) {
        return 69 * loc.getX() + loc.getY();
    }

    public static boolean moveCloser(MapLocation enemy) {
        int best = 99999999;
        Direction bestd = null;
        for (int i = 0; i < directions.length; i++) {
            MapLocation temp = curUnit.location().mapLocation().add(directions[i]);
            if (gc.canMove(curUnit.id(), directions[i]) && distance(temp, enemy) < best) {
                best = distance(temp, enemy);
                bestd = directions[i];
            }
        }
        if (bestd != null) {
            gc.moveRobot(curUnit.id(), bestd);
            curLoc = curLoc.add(bestd);
            return true;
        }
        return false;
    }

    public static int distance(MapLocation first, MapLocation second) {
        int x1 = first.getX(), y1 = first.getY(), x2 = second.getX(), y2 = second.getY();
        return (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2);
    }

    public static boolean onMap(MapLocation loc) {
        int x = loc.getX();
        int y = loc.getY();
        return x >= 0 && y >= 0 && x < Player.gridX && y < Player.gridY;
    }

    public static boolean checkPassable(MapLocation test) {
        int x = test.getX();
        int y = test.getY();
        if (x >= Player.gridX || y >= Player.gridY || x < 0 || y < 0) {
            return false;
        }
        return Player.gotoable[myId][x][y];// && !allyThere;
    }

    public static int manDistance(MapLocation first, MapLocation second) {
        int x1 = first.getX(), y1 = first.getY(), x2 = second.getX(), y2 = second.getY();
        return Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2));
    }
    public static void moveAnywhere() {
        if (!gc.isMoveReady(curUnit.id())) {
            return;
        }
        for (int i = 0; i < directions.length; i++) {
            if (gc.canMove(curUnit.id(), directions[i])) {
                gc.moveRobot(curUnit.id(), directions[i]);
                curLoc = curLoc.add(directions[i]);
                return;
            }
        }
    }
}