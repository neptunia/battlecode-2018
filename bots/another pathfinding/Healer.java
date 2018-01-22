import bc.*;

import java.util.*;

public class Healer {

    static Unit curUnit;
    static GameController gc;
    static Direction[] directions = Direction.values();
    static HashMap<Integer, HashSet<Integer>> visited = new HashMap<Integer, HashSet<Integer>>();
    static Planet curPlanet;
    static MapLocation curLoc;

    public static void run(GameController gc, Unit curUnit) {

        Healer.curUnit = curUnit;
        curPlanet = gc.planet();

        if (curUnit.location().isInGarrison()) {
            return;
        }

        curLoc = curUnit.location().mapLocation();

        if (Player.priorityTarget.containsKey(curUnit.id())) {
            MapLocation rocket = Player.priorityTarget.get(curUnit.id());
            if (gc.hasUnitAtLocation(rocket) && gc.senseUnitAtLocation(rocket).unitType() == UnitType.Rocket) {
                //System.out.println("Going to rocket!");
                move(Player.priorityTarget.get(curUnit.id()));
            } else {
                Player.priorityTarget.remove(curUnit.id());
            }
                
            
            return;
        }

        healerMicro();

    }

    public static void healerMicro() {
        int closestEnemy = enemyTooClose();
        if (closestEnemy != -1) {
            // there is an enemy within 70 units of me. This is bad, move away.
            if (gc.isMoveReady(curUnit.id())) {
                moveAway(gc.unit(closestEnemy).location().mapLocation());
            }

            // then heal whoever's available
            if (canHeal()) {
                healNearbyAllies();
            }

            findOverchargeTarget();

            return;
        }

        // no enemies nearby;
        if (!moveHeal()) {
            // lol no friendlies nearby either
            if (gc.isMoveReady(curUnit.id())) {
                curLoc = curUnit.location().mapLocation();
                move(Player.enemyLocation);
            }
        }
        findOverchargeTarget();
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

    public static boolean moveCloser(MapLocation enemy) {
        int best = distance(curUnit.location().mapLocation(), enemy);
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
            return true;
        }
        return false;
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
            return true;
        }
        return false;
    }

    //returns id of nearest enemy unit and nearest attackable enemy unit
    public static int findNearestEnemy() {
        int p = -1;
        VecUnit nearby = gc.senseNearbyUnitsByTeam(curUnit.location().mapLocation(), curUnit.visionRange(), Player.enemyTeam);
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
    public static boolean canHeal() {
        return curUnit.attackHeat() < 10;
    }

    public static boolean moveHeal() {
        VecUnit nearbyUnits = getNearby(curUnit.location().mapLocation(), 48);
        long maxHp = -1;
        int id = -1;
        for (int i = 0; i < nearbyUnits.size(); i++) {
            Unit unit = nearbyUnits.get(i);
            //if can attack this enemy unit
            if (unit.team() == gc.team() && gc.isHealReady(curUnit.id())) {
                if (unit.health() > maxHp && unit.maxHealth() - unit.health() >= Math.abs(curUnit.damage())) {
                    maxHp = unit.health();
                    id = unit.id();
                }
            }
        }
        if (id == -1) {
            // no friendlies in vision range
            return false;
        }
        if (distance(curUnit.location().mapLocation(), gc.unit(id).location().mapLocation()) > 30) {
            // out of my range, attempt to move closer
            if (gc.isMoveReady(curUnit.id()) && moveCloser(gc.unit(id).location().mapLocation()) && gc.canHeal(curUnit.id(), id)) {
                gc.heal(curUnit.id(), id);
                return true;
            }
        }
        healNearbyAllies();
        return true;
    }

    public static void findOverchargeTarget() {
        if (!gc.isOverchargeReady(curUnit.id())) {
            return;
        }
        VecUnit nearbyUnits = gc.senseNearbyUnitsByTeam(curUnit.location().mapLocation(), curUnit.abilityRange(), Player.myTeam);
        //long maxHp = -1;
        int id = -1;
        boolean rangerFound = false;
        for (int i = 0; i < nearbyUnits.size(); i++) {
            Unit unit = nearbyUnits.get(i);
            //just pick something
            if (unit.team() == gc.team() && id == -1) {
                id = unit.id();
            }
            // want a ranger
            if (rangerFound == false && unit.unitType() == UnitType.Ranger) {
                id = unit.id();
                rangerFound = true;
            }
            //now look for better candidates
            if (unit.unitType() == UnitType.Ranger && Ranger.inCombat.get(unit.id())) {
                //maxHp = unit.health();
                id = unit.id();
            }
        }
        if (id != -1 && gc.canOvercharge(curUnit.id(), id)) {
            gc.overcharge(curUnit.id(), id);
            Ranger.run(gc, gc.unit(id));
        }
    }

    public static void healNearbyAllies() {
        VecUnit nearbyUnits = getNearby(curUnit.location().mapLocation(), (int) curUnit.attackRange());
        long maxHp = -1;
        int id = -1;
        for (int i = 0; i < nearbyUnits.size(); i++) {
            Unit unit = nearbyUnits.get(i);
            //if can attack this enemy unit
            if (unit.team() == gc.team() && gc.isHealReady(curUnit.id()) && gc.canHeal(curUnit.id(), unit.id())) {
                if (unit.health() > maxHp && unit.maxHealth() - unit.health() >= Math.abs(curUnit.damage())) {
                    maxHp = unit.health();
                    id = unit.id();
                }
            }
        }
        if (id != -1) {
            gc.heal(curUnit.id(), id);
        }
    }

    public static void healWeakestAllies() {
        VecUnit nearbyUnits = getNearby(curUnit.location().mapLocation(), (int) curUnit.attackRange());
        long minHp = 99999999;
        int id = -1;
        for (int i = 0; i < nearbyUnits.size(); i++) {
            Unit unit = nearbyUnits.get(i);
            //if can attack this enemy unit
            if (unit.team() == gc.team() && gc.isHealReady(curUnit.id()) && gc.canHeal(curUnit.id(), unit.id())) {
                if (unit.health() < minHp) {
                    minHp = unit.health();
                    id = unit.id();
                }
            }
        }
        gc.heal(curUnit.id(), id);
    }

    public static boolean canMove() {
        return curUnit.movementHeat() < 10;
    }

    public static int hash(MapLocation loc) {
        return 69 * loc.getX() + loc.getY();
    }

    public static int doubleHash(MapLocation first, MapLocation second) {
        int x1 = first.getX(), y1 = first.getY(), x2 = second.getX(), y2 = second.getY();
        return (69 * x1) + y1 + ((69 * x2) + y2) * 10000;
    }

    public static int manDistance(int hash1, int hash2) {
        int y1 = hash1 % 69;
        int x1 = (hash1 - y1) / 69;
        int y2 = hash2 % 69;
        int x2 = (hash2 - y2) / 69;
        return Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2));
    }

    //pathing
    //move towards target location
    public static void move(MapLocation target) {
        int targetHash = hash(target);
        if (hash(curLoc) == targetHash || !gc.isMoveReady(curUnit.id())) {
            return;
        }
        int x = curLoc.getX();
        int y = curLoc.getY();
        int currentDist = Player.pathDistances[targetHash][x][y];
        if (currentDist != -1) {
            if (x < Player.gridX - 1 && Player.pathDistances[targetHash][x + 1][y] - currentDist < 0 && gc.canMove(curUnit.id(), Direction.East)) {
                gc.moveRobot(curUnit.id(), Direction.East);
            } else if (x > 0 && Player.pathDistances[targetHash][x - 1][y] - currentDist < 0 && gc.canMove(curUnit.id(), Direction.West)) {
                gc.moveRobot(curUnit.id(), Direction.West);
            } else if (y < Player.gridY - 1 && Player.pathDistances[targetHash][x][y + 1] - currentDist < 0 && gc.canMove(curUnit.id(), Direction.North)) {
                gc.moveRobot(curUnit.id(), Direction.North);
            } else if (y > 0 && Player.pathDistances[targetHash][x][y - 1] - currentDist < 0 && gc.canMove(curUnit.id(), Direction.South)) {
                gc.moveRobot(curUnit.id(), Direction.South);
            } else if (y < Player.gridY - 1 && x < Player.gridX - 1 && Player.pathDistances[targetHash][x + 1][y + 1] - currentDist < 0 && gc.canMove(curUnit.id(), Direction.Northeast)) {
                gc.moveRobot(curUnit.id(), Direction.Northeast);
            } else if (y > 0 && x < Player.gridX - 1 && Player.pathDistances[targetHash][x + 1][y - 1] - currentDist < 0 && gc.canMove(curUnit.id(), Direction.Southeast)) {
                gc.moveRobot(curUnit.id(), Direction.Southeast);
            } else if (x > 0 && y < Player.gridY - 1 && Player.pathDistances[targetHash][x - 1][y + 1] - currentDist < 0 && gc.canMove(curUnit.id(), Direction.Northwest)) {
                gc.moveRobot(curUnit.id(), Direction.Northwest);
            } else if (x > 0 && y > 0 && Player.pathDistances[targetHash][x - 1][y - 1] - currentDist < 0 && gc.canMove(curUnit.id(), Direction.Southwest)) {
                gc.moveRobot(curUnit.id(), Direction.Southwest);
            }
        } else {
            //cant get there
            if (Player.bfsMin(target, curLoc)) {
                move(target);
            } else {
                //System.out.println("cant get there healer");
            }
        }
        if (gc.isMoveReady(curUnit.id())) {
            Player.blockedCount++;
            moveCloser(target);
        }
    }


    public static int manDistance(MapLocation first, MapLocation second) {
        int x1 = first.getX(), y1 = first.getY(), x2 = second.getX(), y2 = second.getY();
        return Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2));
    }

    public static boolean checkPassable(MapLocation test) {
        int x = test.getX();
        int y = test.getY();
        if (x >= Player.gridX || y >= Player.gridY || x < 0 || y < 0) {
            return false;
        }
        /*
        boolean allyThere = true;
        try {
            Unit temp = gc.senseUnitAtLocation(test);
            if (temp.unitType() != UnitType.Factory && temp.unitType() != UnitType.Rocket) {
                allyThere = false;
            }
        } catch (Exception e) {
            allyThere = false;
        }*/
        return Player.gotoable[x][y];// && !allyThere;
    }

    public static boolean moveAttack(MapLocation target) {
        //greedy pathfinding
        int smallest = 999999;
        Direction d = null;
        MapLocation curLoc = curUnit.location().mapLocation();
        int hash = hash(curLoc.getX(), curLoc.getY());
        if (!visited.containsKey(curUnit.id())) {
            HashSet<Integer> temp = new HashSet<Integer>();
            temp.add(hash);
            visited.put(curUnit.id(), temp);
        } else {
            visited.get(curUnit.id()).add(hash);
        }
        for (int i = 0; i < directions.length; i++) {
            MapLocation newSquare = curLoc.add(directions[i]);
            int dist = distance(newSquare, target);
            if (!visited.get(curUnit.id()).contains(hash(newSquare.getX(), newSquare.getY())) && gc.canMove(curUnit.id(), directions[i]) && dist < smallest) {
                smallest = distance(newSquare, target);
                d = directions[i];
            }
        }
        if (d == null) {
            //can't move
            //TODO change
            visited.remove(curUnit.id());
            return false;
        }
        gc.moveRobot(curUnit.id(), d);
        return true;
    }

    public static int doubleHash(int x1, int y1, int x2, int y2) {
        return (69 * x1) + y1 + ((69 * x2) + y2) * 10000;
    }

    public static int doubleHash(int hash1, int hash2) {
        int y1 = hash1 % 69;
        int x1 = (hash1 - y1) / 69;
        int y2 = hash2 % 69;
        int x2 = (hash2 - y2) / 69;
        return (69 * x1) + y1 + ((69 * x2) + y2) * 10000;
    }

    //get target unit should be pathing towards
    public static MapLocation getTarget() {
        return Player.enemyLocation;
    }

    public static int hash(int x, int y) {
        return 69 * x + y;
    }

    public static int distance(MapLocation first, MapLocation second) {
        int x1 = first.getX(), y1 = first.getY(), x2 = second.getX(), y2 = second.getY();
        return (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2);
    }

    //senses nearby units and updates RobotPlayer.map with detected units
    public static VecUnit getNearby(MapLocation maploc, int radius) {
        VecUnit nearby = gc.senseNearbyUnits(maploc, radius);
        for (int i = 0; i < nearby.size(); i++) {
            Unit unit = nearby.get(i);
            MapLocation temp = unit.location().mapLocation();
            Player.map[temp.getX()][temp.getY()] = unit;
        }
        return nearby;
    }
}