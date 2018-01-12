import bc.*;

import java.util.*;

public class Ranger {

    static Unit curUnit;
    static GameController gc;
    static Direction[] directions = Direction.values();
    static HashMap<Integer, HashSet<Integer>> visited = new HashMap<Integer, HashSet<Integer>>();

    public static void run(GameController gc, Unit curUnit) {

        Ranger.curUnit = curUnit;

        if (curUnit.location().isInGarrison()) {
            return;
        }

        Pair target = findNearestEnemy();
        if (target.enemyClosest == -1) {
            //explore if no units detected
            if (canMove()) {
                move(Player.enemyLocation);
            }
        } else {
            MapLocation loc = gc.unit(target.enemyClosest).location().mapLocation();
            int distance = distance(loc, curUnit.location().mapLocation());
            if (distance < 10) {
                //move away, they're not even attackable! attack someone else instead.
                if (canAttack() && gc.canAttack(curUnit.id(), target.enemyAttack)) {
                    gc.attack(curUnit.id(), target.enemyAttack);
                }
                if (canMove()) {
                    moveAway(loc);
                }
            } else if (distance <= 30) {
                //move away, they're too close!
                if (canAttack() && gc.canAttack(curUnit.id(), target.enemyClosest)) {
                    gc.attack(curUnit.id(), target.enemyClosest);
                }
                if (canMove()) {
                    moveAway(loc);
                }
            } else if (distance <= 50) {
                //attack them
                if (canAttack() && gc.canAttack(curUnit.id(), target.enemyClosest)) {
                    gc.attack(curUnit.id(), target.enemyClosest);
                }
            } else {
                //they're too far away, move towards them
                if (canMove()) {
                    move(loc);
                }

                if (canAttack() && gc.canAttack(curUnit.id(), target.enemyClosest)) {
                    gc.attack(curUnit.id(), target.enemyClosest);
                }
            }
        }

    }

    //calc which direction maximizes distance between enemy and ranger
    public static void moveAway(MapLocation enemy) {
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
        }
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
        VecUnit nearby = gc.senseNearbyUnits(curUnit.location().mapLocation(), curUnit.visionRange());
        int smallest1 = 9999999;
        int smallest2 = 9999999;
        for (int i = 0; i < nearby.size(); i++) {
            Unit temp3 = nearby.get(i);
            if (temp3.team() != gc.team()) {
                MapLocation temp2 = temp3.location().mapLocation();
                int temp = distance(curUnit.location().mapLocation(), temp2);
                if (temp < smallest1) {
                    smallest1 = temp;
                    p.enemyClosest = temp3.id();
                }
                if (temp > 10 && temp < smallest2) {
                    smallest2 = temp;
                    p.enemyAttack = temp3.id();
                }
            }
        }
        return p;
    }

    public static boolean canAttack() {
        return curUnit.attackHeat() < 10;
    }

    public static void attackNearbyEnemies() {
        VecUnit nearbyUnits = getNearby(curUnit.location().mapLocation(), (int) curUnit.attackRange());
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
        return curUnit.movementHeat() < 10;
    }

    //pathing
    //move towards target location
    public static boolean move(MapLocation target) {
        //TODO implement pathfinding
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
            if (!visited.get(curUnit.id()).contains(hash(newSquare.getX(), newSquare.getY())) && gc.canMove(curUnit.id(), directions[i]) && distance(newSquare, target) < smallest) {
                smallest = distance(newSquare, target);
                d = directions[i];
            }
        }
        if (d == null) {
            //can't move
            visited.remove(curUnit.id());
            return false;
        }
        gc.moveRobot(curUnit.id(), d);
        return true;
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
