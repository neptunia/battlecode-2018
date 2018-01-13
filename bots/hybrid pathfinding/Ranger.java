import bc.*;

import java.util.*;

public class Ranger {

    static Unit curUnit;
    static GameController gc;
    static Direction[] directions = Direction.values();
    static HashMap<Integer, HashSet<Integer>> visited = new HashMap<Integer, HashSet<Integer>>();
    static HashMap<Integer, Integer> prevLocation = new HashMap<Integer, Integer>();

    public static void run(GameController gc, Unit curUnit) {

        Ranger.curUnit = curUnit;

        if (curUnit.location().isInGarrison()) {
            return;
        }

        if (Player.firstTime) {
            Player.firstTime = false;
            //guesstimate enemy location
            if (Player.enemyLocation == null) {
                MapLocation temp = curUnit.location().mapLocation();
                Player.startingLocation = temp;
                Player.enemyLocation = new MapLocation(gc.planet(), Player.gridX - temp.getX(), Player.gridY - temp.getY());
            }
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
                    moveAttack(loc);
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

    //get target unit should be pathing towards
    public static MapLocation getTarget() {
        return Player.enemyLocation;
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

    public static boolean moveAttack(MapLocation target) {
        //greedy pathfinding
        int smallest = 999999;
        Direction d = null;
        int curDist = distance(curUnit.location().mapLocation(), target);
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
            if (!visited.get(curUnit.id()).contains(hash(newSquare.getX(), newSquare.getY())) && gc.canMove(curUnit.id(), directions[i]) && dist < smallest && dist < curDist) {
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

    //pathing
    //move towards target location
    public static void move(MapLocation target) {
        //a*
        int movingTo = doubleHash(curUnit.location().mapLocation(), target);
        if (!Player.paths.containsKey(movingTo)) {
            HashSet<Integer> closedList = new HashSet<Integer>();
            HashMap<Integer, Integer> gScore = new HashMap<Integer, Integer>();
            HashMap<Integer, Integer> fScore = new HashMap<Integer, Integer>();
            HashMap<Integer, Integer> fromMap = new HashMap<Integer, Integer>();
            PriorityQueue<Integer> openList = new PriorityQueue<Integer>(11, new Comparator<Integer>() {
            public int compare(Integer nodeA, Integer nodeB) {
                return Integer.compare(fScore.get(nodeA), fScore.get(nodeB));
            }
        });

            MapLocation curLoc = curUnit.location().mapLocation();

            int startHash = hash(curLoc);

            gScore.put(startHash, 0);
            fScore.put(startHash, manDistance(curLoc, target));
            openList.offer(startHash);

            int goal = hash(target);

            while (!openList.isEmpty()) {
                int current = openList.poll();

                int tempY = current % 69;
                int tempX = (current - tempY) / 69;
                curLoc = new MapLocation(gc.planet(), tempX, tempY);
                
                //System.out.println("Node im on " + print(current));

                closedList.add(current);

                //iterate through neighbors
                for (int i = 0; i < directions.length; i++) {
                    int neighbor = hash(curLoc.add(directions[i]));
                    if (neighbor == goal) {
                        fromMap.put(neighbor, current);
                        HashMap<Integer, Integer> path = new HashMap<Integer, Integer>();
                        HashMap<Integer, Integer> path2 = new HashMap<Integer, Integer>();
                        int next = goal;

                        int prev = -1;
                        ArrayList<Integer> before = new ArrayList<Integer>();
                        before.add(next);
                        while (fromMap.containsKey(next)) {
                            //System.out.println(print(next));
                            //path.put(next, prev);
                            prev = next;
                            next = fromMap.get(prev);
                            before.add(next);
                            //Player.paths.put(doubleHash(prev, next), path);
                            //Player.paths.put(doubleHash(next, prev), path);
                            //TODO put in between Player.paths... a b c d e needs bc, bd, cd 
                            path.put(next, prev);
                            path2.put(prev, next);
                        }
                        int temp = before.size();
                        for (int j = 0; j < temp; j++) {
                            for (int a = 0; a < j; a++) {
                                Player.paths.put(doubleHash(before.get(j), before.get(a)), path.get(before.get(j)));
                                Player.paths.put(doubleHash(before.get(a), before.get(j)), path2.get(before.get(a)));
                            }
                        }
                        
                        break;
                    }
                    if (checkPassable(curLoc.add(directions[i]))) {
                        if (closedList.contains(neighbor)) {
                            continue;
                        }

                        int tentG = gScore.get(current) + 1;

                        boolean contains = openList.contains(neighbor);
                        if (!contains || tentG < gScore.get(neighbor)) {
                            gScore.put(neighbor, tentG);
                            fScore.put(neighbor, tentG + manDistance(neighbor, hash(target.getX(), target.getY())));

                            if (contains) {
                                openList.remove(neighbor);
                            }

                            openList.offer(neighbor);
                            //System.out.println("Add: " + print(neighbor));
                            fromMap.put(neighbor, current);
                        }
                    }
                }
            }
        }
        //System.out.println(hash(curUnit.location().mapLocation()));
        //System.out.println(Arrays.asList(Player.paths.get(movingTo)));
        //System.out.println(Player.paths.get(movingTo).containsKey(hash(curUnit.location().mapLocation())));

        int toMove = Player.paths.get(movingTo);

        int y = toMove % 69;
        int x = (toMove - y) / 69;
        
        MapLocation next = new MapLocation(gc.planet(), x, y);
        Direction temp = curUnit.location().mapLocation().directionTo(next);
        if (gc.canMove(curUnit.id(), temp) && canMove()) {
            gc.moveRobot(curUnit.id(), temp);
        } else {
            //System.out.println("Darn");
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
        if (test.getX() >= Player.gridX || test.getY() >= Player.gridY || test.getX() < 0 || test.getY() < 0) {
            return false;
        }
        return Player.planetMap.isPassableTerrainAt(test) == 1;
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
        return (x2 - x1) + (y2 - y1);
    }

    public static int manDistance(int hash1, int hash2) {
        int y1 = hash1 % 69;
        int x1 = (hash1 - y1) / 69;
        int y2 = hash2 % 69;
        int x2 = (hash2 - y2) / 69;
        return (x2 - x1) + (y2 - y1);
    }

    public static int distance(int hash1, int hash2) {
        int y1 = hash1 % 69;
        int x1 = (hash1 - y1) / 69;
        int y2 = hash2 % 69;
        int x2 = (hash2 - y2) / 69;
        return (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2);
    }
}
