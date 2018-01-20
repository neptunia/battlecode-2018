import bc.*;

import java.util.*;

public class Ranger {

    static Unit curUnit;
    static GameController gc;
    static Direction[] directions = Direction.values();
    static Planet curPlanet;
    static MapLocation curLoc;
    static HashMap<Integer, HashSet<Integer>> visited = new HashMap<Integer, HashSet<Integer>>();
    static HashMap<Integer, Integer> prevLocation = new HashMap<Integer, Integer>();

    public static void run(GameController gc, Unit curUnit) {

        Ranger.curUnit = curUnit;
        curPlanet = gc.planet();

        if (curUnit.location().isInGarrison() || curUnit.location().isInSpace()) {
            return;
        }

        curLoc = curUnit.location().mapLocation();

        if (Player.priorityTarget.containsKey(curUnit.id())) {
            System.out.println("Going to rocket!");
            move(Player.priorityTarget.get(curUnit.id()));
            return;
        }

        Pair target = findNearestEnemy();
        if (Player.timesReachedTarget >= 3 && target.enemyClosest == -1) {
            return;
        }
        if (target.enemyClosest != -1) {
            Player.timesReachedTarget = 0;
            rangerMicro(target.enemyClosest);
        } else if (target.enemyClosest == -1) {
            //explore if no units detected
            move(Player.enemyLocation);
        }

    }

    // ranger micro for ranger v ranger action
    // only called when nearest enemy is a ranger, and our vision is upgraded
    // takes in enemy ID
    public static void rangerMicro(int enemyid) {
        MapLocation enemyLoc = gc.unit(enemyid).location().mapLocation();
        MapLocation myLoc = curUnit.location().mapLocation();
        int dist = distance(myLoc, enemyLoc);
        if (dist <= 10) {
            // too close!
            if (gc.isMoveReady(curUnit.id())) {
                moveAway(enemyLoc);
            }
            attackNearbyEnemies();
            return;
        }
        if (dist < 50) {
            // within attack range
            if (canAttack()) {
                gc.attack(curUnit.id(), enemyid);
            }
            if (gc.isMoveReady(curUnit.id())) {
                moveAway(enemyLoc);
            }
            return;
        }
        if (dist <= 70) {
            // within moveattack range
            if (gc.isMoveReady(curUnit.id()) && canAttack() && moveCloser(enemyLoc) && gc.canAttack(curUnit.id(), enemyid)) {
                // move was successful
                gc.attack(curUnit.id(), enemyid);
            }
            return;
        }
        // if distance is between 70 and 100, dont do anything
        // we can move closer if the enemy is not a ranger, though
        if (gc.isMoveReady(curUnit.id())) {
            if ((dist == 100) || (dist < 100 && gc.unit(enemyid).unitType() != UnitType.Ranger)) {
                moveCloser(enemyLoc);
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
            return true;
        }
        return false;
    }

    // calc which direction minimizes distance between enemy and ranger
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

    public static class Pair { //DIFFERENT FROM PAIR IN KNIGHT CLASS
        int enemyAttack; //closest enemy that is attackable
        int enemyClosest; //closest enemy
        Pair() {
            enemyAttack = -1;
            enemyClosest = -1;
        }
    }
    //returns id of nearest enemy unit and nearest attackable enemy unit
    public static Pair findNearestEnemy() {
        Pair p = new Pair();
        VecUnit nearby = gc.senseNearbyUnitsByTeam(curUnit.location().mapLocation(), curUnit.visionRange(), Player.enemyTeam);
        int smallest1 = 9999999;
        int smallest2 = (int)curUnit.attackRange();
        for (int i = 0; i < nearby.size(); i++) {
            Unit temp3 = nearby.get(i);
            MapLocation temp2 = temp3.location().mapLocation();
            int temp = distance(curUnit.location().mapLocation(), temp2);
            if (temp < smallest1) {
                smallest1 = temp;
                p.enemyClosest = temp3.id();
            }
            if (temp >= 10 && temp <= smallest2) {
                smallest2 = temp;
                p.enemyAttack = temp3.id();
            }
        }
        return p;
    }

    public static boolean canAttack() {
        return curUnit.attackHeat() < 10;
    }

    public static void attackNearbyEnemies() {
        VecUnit nearbyUnits = gc.senseNearbyUnits(curUnit.location().mapLocation(), (int) curUnit.attackRange());
        for (int i = 0; i < nearbyUnits.size(); i++) {
            Unit unit = nearbyUnits.get(i);
            //if can attack this enemy unit
            if (unit.team() != gc.team() && gc.isAttackReady(curUnit.id()) && gc.canAttack(curUnit.id(), unit.id())) {
                gc.attack(curUnit.id(), unit.id());
                return;
            }
        }
    }

    public static boolean canMove() {
        return gc.isMoveReady(curUnit.id());
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
            System.out.println("cant get there");
        }
        if (gc.isMoveReady(curUnit.id())) {
            Player.blockedCount++;
        }
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

    public static int doubleHash(MapLocation first, MapLocation second) {
        int x1 = first.getX(), y1 = first.getY(), x2 = second.getX(), y2 = second.getY();
        return (69 * x1) + y1 + ((69 * x2) + y2) * 10000;
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

    public static int hash(int x, int y) {
        return 69 * x + y;
    }

    public static int hash(MapLocation loc) {
        return 69 * loc.getX() + loc.getY();
    }

    public static int distance(MapLocation first, MapLocation second) {
        int x1 = first.getX(), y1 = first.getY(), x2 = second.getX(), y2 = second.getY();
        return (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2);
    }

    public static int manDistance(MapLocation first, MapLocation second) {
        int x1 = first.getX(), y1 = first.getY(), x2 = second.getX(), y2 = second.getY();
        return Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2));
    }

    public static int manDistance(int hash1, int hash2) {
        int y1 = hash1 % 69;
        int x1 = (hash1 - y1) / 69;
        int y2 = hash2 % 69;
        int x2 = (hash2 - y2) / 69;
        return Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2));
    }

    public static int distance(int hash1, int hash2) {
        int y1 = hash1 % 69;
        int x1 = (hash1 - y1) / 69;
        int y2 = hash2 % 69;
        int x2 = (hash2 - y2) / 69;
        return (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2);
    }
}
