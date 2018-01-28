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
    static int numRangerGoingToRocket = 0, numHealerGoingToRocket = 0;
    static boolean noMoreKarbonite = false;
    static boolean fullyReplicated = false;
	static HashMap<Integer, Integer> id = new HashMap<Integer, Integer>();
	static HashMap<Integer, MapLocation> target = new HashMap<Integer, MapLocation>();
	static HashSet<Integer> spotsTaken = new HashSet<Integer>();
    static HashSet<Integer> structuresToBuild = new HashSet<Integer>();
    static HashMap<Integer, Blueprint> structures = new HashMap<Integer, Blueprint>();
    static HashMap<Integer, Integer> numberWorkersAssigned = new HashMap<Integer, Integer>();
    static HashMap<Integer, Boolean> split = new HashMap<Integer, Boolean>();
    static HashMap<Integer, MapLocation> healFactory = new HashMap<Integer, MapLocation>();
    static boolean[][] karbonitePatches;
    static HashSet<Integer> patchesOccupied = new HashSet<Integer>();
    //static HashSet<Integer> placedAlready = new HashSet<Integer>();
    static int[] replicationLimit;

	public static void run(Unit curUnit) {

		Worker.curUnit = curUnit;

        if (curUnit.location().isInGarrison()) {
            return;
        }

        curLoc = curUnit.location().mapLocation();

        if (!id.containsKey(curUnit.id())) {
            for (int i = 0; i < Player.gotoable.length; i++) {
                if (Player.gotoable[i][curLoc.getX()][curLoc.getY()]) {
                    id.put(curUnit.id(), i);
                    break;
                }
            }
        }

        Worker.myId = id.get(curUnit.id());

        if (gc.planet() == Planet.Mars) {
            marsMine();
            if ((gc.round() > 750 || Player.numWorker < 6 || gc.getTeamArray(Planet.Earth).get(0) == 69) && curUnit.abilityHeat() < 10) {
                //System.out.println("wew replicate");
                for (int i = 0; i < directions.length; i++) {
                    if (gc.canReplicate(curUnit.id(), directions[i])) {
                        gc.replicate(curUnit.id(), directions[i]);
                        Player.numWorker++;
                        //Unit newUnit = gc.senseUnitAtLocation(curLoc.add(directions[i]));
                        //id.put(newUnit.id(), myId);
                        //Player.newUnits.add(newUnit);
                        return;
                    }
                }
            }
            return;
        }

        if (structures.containsKey(curUnit.id())) {
            Player.someoneTriedBuilding = true;
            buildStructure();
            return;
        }
        if (healFactory.containsKey(curUnit.id())) {
            healFactory();
            return;
        }
        ///&& Player.numFactory + structuresToBuild.size() < 4 
        //remove dead units from structures
        /*
        Set<Integer> keySet = structures.keySet();
        for (Integer id : keySet) {
            if (!Player.workerIds.contains(id)) {
                structures.remove(id);
            }
        }*/
        /*
        System.out.println("---------------------");
        System.out.println(structuresToBuild.size());
        System.out.println(structures.size());*/
        if (Player.prevBlocked < 10 && gc.karbonite() + Player.karboniteGonnaUse >= 200 && gc.round() != 1 && gc.round() < 550 && (Player.numFactory + structuresToBuild.size() < (split.get(myId) ? 3 : 6)) || (split.get(myId) ? false : gc.karbonite() + Player.karboniteGonnaUse >= 1000)) {
            startStructure(UnitType.Factory);
            Worker.run(curUnit);
        } else if (gc.karbonite() + Player.karboniteGonnaUse >= 150 && Player.numFactory >= 1 && (gc.round() < 550 ? (Player.prevBlocked >= 8 || Player.timesReachedTarget >= 2 || split.get(myId)) : true) && gc.researchInfo().getLevel(UnitType.Rocket) > 0 && Player.numRanger - numRangerGoingToRocket >= 5 && Player.numHealer - numHealerGoingToRocket >= 2) {
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
            }
            if (manDistance(curLoc, toBuild.loc) == 1) {
                //TODO: replicate around it if factory doesnt already have 8 workers
                if (numberWorkersAssigned.get(hash(toBuild.loc)) < 8 && Player.numWorker < 6 && curUnit.abilityHeat() < 10 && gc.karbonite() >= 60) {
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
                            }
                        }
                    }
                }
                if (curUnit.workerHasActed() == 0) {
                    int blueprintId = gc.senseUnitAtLocation(toBuild.loc).id();
                    if (gc.canBuild(curUnit.id(), blueprintId)) {
                        gc.build(curUnit.id(), blueprintId);
                    } else {
                        //System.out.println("Couldn't work on blueprint :(");
                    }
                }
                
                
            }
            if (manDistance(curLoc, toBuild.loc) > 2) {
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
                        Player.karboniteGonnaUse += 150;
                    }
                    //placedAlready.add(hash(toBuild.loc));
                } else {
                    //System.out.println("Couldn't place down blueprint??");
                }
                
            } else if (distanceToBlueprint == 0) {
                moveAnywhere();
                harvestAroundMe();
            } else {
                move(toBuild.loc);
                harvestAroundMe();
            }
        }
    }

    public static void healFactory() {
        MapLocation factory = healFactory.get(curUnit.id());
        //factory ded
        if (!gc.hasUnitAtLocation(factory)) {
            healFactory.remove(curUnit.id());
            Worker.run(curUnit);
            return;
        }
        Unit theFact = gc.senseUnitAtLocation(factory);
        if (theFact.unitType() != UnitType.Factory || theFact.health() == theFact.maxHealth()) {
            healFactory.remove(curUnit.id());
            Worker.run(curUnit);
            return;
        }
        int distance = manDistance(curLoc, factory);
        if (distance == 2 && gc.isMoveReady(curUnit.id())) {
            //one step away from it
            MapLocation temp = null;
            for (int i = 0; i < directions.length; i++) {
                temp = curLoc.add(directions[i]);
                if (manDistance(temp, factory) == 1) {
                    HashSet<Integer> cantGo = new HashSet<Integer>();
                    cantGo.add(hash(curLoc));
                    if (makeWay(temp, cantGo, factory) && gc.canMove(curUnit.id(), directions[i])) {
                        gc.moveRobot(curUnit.id(), directions[i]);
                        curLoc = curLoc.add(directions[i]);
                        break;
                    }
                }
            }
        }
        if (distance == 1) {
            if (gc.canRepair(curUnit.id(), theFact.id())) {
                gc.repair(curUnit.id(), theFact.id());
            } else {
                harvestAroundMe();
            }
        }
        if (distance > 2) {
            move(factory);
            harvestAroundMe();
        }
    }

    public static void startStructure(UnitType type) {
        MapLocation blueprintLocation = findBlueprintLocation();
        if (type == UnitType.Factory) {
            Player.karboniteGonnaUse -= 200;
        } else {
            Player.karboniteGonnaUse -= 150;
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
                if (temp.unitType() == UnitType.Worker && !structures.containsKey(temp.id()) && !healFactory.containsKey(curUnit.id())) {
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

    public static int enemyTooClose() {
        int p = -1;
        VecUnit nearby = gc.senseNearbyUnitsByTeam(curUnit.location().mapLocation(), 70, Player.enemyTeam);
        int smallest1 = 9999999;
        for (int i = 0; i < nearby.size(); i++) {
            Unit temp3 = nearby.get(i);
            MapLocation temp2 = temp3.location().mapLocation();
            int temp = distance(curUnit.location().mapLocation(), temp2);
            if (temp < smallest1) {
                smallest1 = temp;
                p = temp3.id();
            }
        }
        return p;
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
		    if (gc.isMoveReady(curUnit.id())) {
                int closestEnemy = enemyTooClose();
                if (closestEnemy == -1) {
                    move(Player.initialWorkerStartingLocation.get(myId));
                } else {
                    moveAway(gc.unit(closestEnemy).location().mapLocation());
                }
            }
        }
		
        harvestAroundMe();
        if (fullyReplicated && Player.numWorker <= (int) replicationLimit[myId] / 4.0) {
            //too many workers dying, stop replication
            noMoreKarbonite = true;
            target.clear();
            System.out.println("RUN AWAY!!!");
            fullyReplicated = false;
            return;
        }
        if (Player.numWorker < replicationLimit[myId] && patchesOccupied.size() < replicationLimit[myId] && curUnit.abilityHeat() < 10) {
            //replicate
            //TODO: optimize
            MapLocation spot = findKarboniteSpot();
            if (spot != null) {
                replicateNearestTo(spot);
            }
            if (Player.numWorker > (int) replicationLimit[myId] / 4.0) {
                fullyReplicated = true;
            }
        }
	}

    //calc which direction maximizes distance between enemy and ranger
    public static boolean moveAway(MapLocation enemy) {
        int best = distance(curUnit.location().mapLocation(), enemy);
        Direction bestd = null;
        for (int i = 0; i < directions.length; i++) {
            MapLocation temp = curUnit.location().mapLocation().add(directions[i]);
            if (gc.canMove(curUnit.id(), directions[i]) && distance(temp, enemy) > best) {
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
        if (curUnit.workerHasActed() != 0) {
            return;
        }
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
            karbonitePatches[target.get(curUnit.id()).getX()][target.get(curUnit.id()).getY()] = false;
            target.remove(curUnit.id());
            //TODO: maybe another actions
        } else if (gc.canHarvest(curUnit.id(), mostDir)) {
            gc.harvest(curUnit.id(), mostDir);
        }
    }

    public static void replicateNearestTo(MapLocation loc) {
        //TODO: optimize bigly
        if (curUnit.workerHasActed() != 0 || gc.karbonite() < 60) {
            return;
        }
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
        int rangersNeeded = 4;
        int healersNeeded = 3;
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
                            numRangerGoingToRocket++;
                            Player.priorityTarget.put(there.id(), rocketLoc);
                        } else if (temp == UnitType.Healer && healersNeeded > 0) {
                            healersNeeded--;
                            numHealerGoingToRocket++;
                            Player.priorityTarget.put(there.id(), rocketLoc);
                        } else if (temp == UnitType.Worker && workersNeeded > 0) {
                            workersNeeded--;
                            Player.priorityTarget.put(there.id(), rocketLoc);
                        }
                    }
                }
                
            }
        }
        //System.out.println("Rip not enough units to put into rocket");
    }

    public static void marsMine() {
        //System.out.println("wew mars mine");
        //TODO: mining karbonite on mars
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
            move(Player.enemyLocation[myId]);
        }
        
        harvestAroundMe();
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
                return;
            } else {
            	//System.out.println("cant get there worker");
            }
        }
        if (gc.isMoveReady(curUnit.id())) {
            moveCloser(target);
        }
        if (gc.planet() == Planet.Earth) {
            if (gc.isMoveReady(curUnit.id())) {
                MapLocation toMove = curLoc.add(best);
                HashSet<Integer> temp = new HashSet<Integer>();
                temp.add(hash(curLoc));
                moveAway(toMove, temp);
                if (gc.canMove(curUnit.id(), best)) {
                    gc.moveRobot(curUnit.id(), best);
                    curLoc = curLoc.add(best);
                } else {
                    //System.out.println("priority move didn't work");
                }
            }
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