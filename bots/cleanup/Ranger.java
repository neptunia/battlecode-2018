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
            System.out.println(Player.enemyLocation.toString());
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

    //pathing
    //move towards target location
    public static void move(MapLocation target) {
        MapLocation curLoc = curUnit.location().mapLocation();
        int startHash = hash(curLoc);
        int goal = hash(target);
        if (!gc.isMoveReady(curUnit.id()) || startHash == goal) {
            return;
        }
        //a*
        int movingTo = doubleHash(curLoc, target);
        if (!Player.paths.containsKey(movingTo)) {
            HashSet<Integer> closedList = new HashSet<Integer>();
            HashMap<Integer, Integer> gScore = new HashMap<Integer, Integer>();
            HashMap<Integer, Integer> fScore = new HashMap<Integer, Integer>();
            HashMap<Integer, Integer> fromMap = new HashMap<Integer, Integer>();
            PriorityQueue<Integer> openList = new PriorityQueue<Integer>(11, new Comparator<Integer>() {
                public int compare(Integer nodeA, Integer nodeB) {
                    return Integer.compare(fScore.containsKey(nodeA) ? fScore.get(nodeA) : 9999999, fScore.containsKey(nodeB) ? fScore.get(nodeB) : 9999999);
                }
            });



            gScore.put(startHash, 0);
            fScore.put(startHash, manDistance(curLoc, target));
            openList.offer(startHash);
            while (!openList.isEmpty()) {
                int current = openList.poll();
                /*
                int remainingPath = doubleHash(current, goal);
                if (Player.paths.containsKey(remainingPath)) {
                    //TODO: optimization once the killed has been fixed
                    //completes the path
                    int tempCur = current;
                    while (tempCur != goal) {
                        int tempHash = doubleHash(tempCur, goal);
                        int nextNode = Player.paths.get(tempHash);
                        fromMap.put(nextNode, tempCur);
                        tempCur = nextNode;
                    }
                    current = goal;
                }*/

                if (current == goal) {
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

                int tempY = current % 69;
                int tempX = (current - tempY) / 69;
                curLoc = new MapLocation(curPlanet, tempX, tempY);

                //System.out.println("Node im on " + print(current));

                closedList.add(current);

                //iterate through neighbors
                for (int i = 0; i < directions.length; i++) {
                    int neighbor = hash(curLoc.add(directions[i]));
                    //if a path is already computed for this node to the goal then dont needa compute more

                    if (checkPassable(curLoc.add(directions[i]))) {
                        if (closedList.contains(neighbor)) {
                            continue;
                        }

                        if (!openList.contains(neighbor)) {
                            openList.add(neighbor);
                        }

                        int tentG = gScore.get(current) + 1;

                        if (gScore.containsKey(neighbor) && tentG >= gScore.get(neighbor)) {
                            continue;
                        }
                        fScore.put(neighbor, tentG + manDistance(neighbor, hash(target.getX(), target.getY())));
                        openList.offer(neighbor);
                        gScore.put(neighbor, tentG);
                        fromMap.put(neighbor, current);
                    }
                }
            }
        }
        //System.out.println(hash(curUnit.location().mapLocation()));
        //System.out.println(Arrays.asList(Player.paths.get(movingTo)));
        //System.out.println(Player.paths.get(movingTo).containsKey(hash(curUnit.location().mapLocation())));
        

        if (!Player.paths.containsKey(movingTo)) {
            System.out.println("wot borked");
            //System.out.println("Enemy Location: " + Integer.toString(Player.enemyLocation.getX()) + " " + Integer.toString(Player.enemyLocation.getY()));
            //System.out.println("Cur location: " + Integer.toString(curLoc.getX()) + " " + Integer.toString(curLoc.getY()));
            //System.out.println("Target Location: " + Integer.toString(target.getX()) + " " + Integer.toString(target.getY()));
            //moveAttack(target);
            return;
        }

        int toMove = Player.paths.get(movingTo);

        int y = toMove % 69;
        int x = (toMove - y) / 69;

        MapLocation next = new MapLocation(curPlanet, x, y);
        Direction temp = curUnit.location().mapLocation().directionTo(next);
        if (gc.canMove(curUnit.id(), temp)) {
            gc.moveRobot(curUnit.id(), temp);
        } else {
            //blocked by something
            Player.blockedCount++;
            //moveAttack(target);
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
