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
        int nearestEnemy = findNearestEnemy();
        if (nearestEnemy == -1) {
            if (gc.isMoveReady(curUnit.id())) {
                move(Player.enemyLocation);
            }
            if (canHeal()) {
                healNearbyAllies();
            }
        } else {
            if (gc.isMoveReady(curUnit.id())){
                moveAway(gc.unit(nearestEnemy).location().mapLocation());
            }
            if (canHeal()) {
                healNearbyAllies();
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
                    return Integer.compare(fScore.get(nodeA), fScore.get(nodeB));
                }
            });



            gScore.put(startHash, 0);
            fScore.put(startHash, manDistance(curLoc, target));
            openList.offer(startHash);
            while (!openList.isEmpty()) {
                int current = openList.poll();

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
                }

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


        if (!Player.paths.containsKey(movingTo)) {
            //System.out.println("wot borked");
            //System.out.println("Enemy Location: " + Integer.toString(Player.enemyLocation.getX()) + " " + Integer.toString(Player.enemyLocation.getY()));
            //System.out.println("Cur location: " + Integer.toString(curLoc.getX()) + " " + Integer.toString(curLoc.getY()));
            //System.out.println("Target Location: " + Integer.toString(target.getX()) + " " + Integer.toString(target.getY()));
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
            MapLocation tryToGoTo = curUnit.location().mapLocation().add(temp);
            Unit blockedBy = gc.senseUnitAtLocation(tryToGoTo);
            if (blockedBy.unitType() == UnitType.Factory || blockedBy.unitType() == UnitType.Rocket || blockedBy.unitType() == UnitType.Worker) {
                //if im not blocked by an attacking unit, then move aside
                moveAttack(target);
            }
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
