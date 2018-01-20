import bc.*;

import java.util.*;

public class Mage {

    static Unit curUnit;
    static GameController gc;
    static Direction[] directions = Direction.values();
    static HashMap<Integer, HashSet<Integer>> visited = new HashMap<Integer, HashSet<Integer>>();
    static HashMap<Integer, Integer> prevLocation = new HashMap<Integer, Integer>();

    public static void run(GameController gc, Unit curUnit) {

        Mage.curUnit = curUnit;

        if (curUnit.location().isInGarrison()) {
            return;
        }

        Pair bestunit = findBestUnit();
        if (bestunit.unit2 == -1) {
            //no visible units
            if (canMove()) {
                move(Player.enemyLocation);
            }
        } else if (bestunit.unit1 == -1) {
            //no good attacks
            //moves away if there is an adjacent unit, else do nothing
            if (canMove()) {
                moveIfNeeded(gc.unit(bestunit.unit2).location().mapLocation());
            }
        } else {
            //good attack opportunity
            //moves away if there is an adjacent unit, else do nothing
            mageMicro(bestunit.unit1, bestunit.unit2);

            /*
            if (canAttack() && gc.canAttack(curUnit.id(), bestunit.unit1)) {
                gc.attack(curUnit.id(), bestunit.unit1);
            }
            try {
                if (canMove()) {
                    moveIfNeeded(gc.unit(bestunit.unit2).location().mapLocation());
                }
            } catch (Exception e) {
                //do nothing
            }
            */
        }
    }

    // guaranteed killability. Assumes rushing knight research.
    public static void isKillable(int enemyid) {
        int myDamage = curUnit.damage()
        if (gc.unit(enemyid).unitType() == UnitType.Knight) {
            if (gc.round < 25) {
                // 5 armor
                return (myDamage - 5) > gc.unit(enemyid).health();
            } else if (gc.round < 100) {
                // 10 armor
                return (myDamage - 10) > gc.unit(enemyid).health();
            } else {
                // 15 armor
                return (myDamage - 15) > gc.unit(enemyid).health();
            }
        } else {
            return myDamage > gc.unit(enemyid).health();
        }
    }

    public static void mageMicro(int enemyid, int closestenemy) {
        MapLocation enemyLoc = gc.unit(enemyid).location().mapLocation();
        MapLocation closestLoc = gc.unit(closestenemy).location().mapLocation();
        MapLocation myLoc = curUnit.location().mapLocation();
        
        int dist = distance(myLoc, enemyLoc);
        int closestdist = distance(myLoc, enemyLoc);

        if (closestdist <= 2) {
            // too close!
            // only attack it if I can kill it. If not, it's a lost cause anyways.
            // TODO: if an enemy worker is next to me i don't really care
            if isKillable(gc.unit(closestenemy)) {
                if (gc.isMoveReady(curUnit.id())) {
                    moveAway(enemyLoc);    
                }
                // note: putting attack here means that the mage commits suicide if it was unable to move.
                // however, it also kills the enemy. Maybe change later.
                if (canAttack()) {
                    gc.attack(curUnit.id(), closestenemy);
                }
            } else {
                if (canAttack()) {
                    gc.attack(curUnit.id(), enemyid);
                }
                moveAway(enemyLoc)
            }

            return;
        }
        if (canAttack()) {
            gc.attack(curUnit.id(), enemyid);
        }
        if (gc.isMoveReady(curUnit.id())) {
            moveAway(enemyLoc);
        }
        return;
        
    }

    public static void moveIfNeeded(MapLocation enemy) {
        if (distance(enemy, curUnit.location().mapLocation()) <= 2) {
            moveAway(enemy);
        }
    }



    //calc which direction maximizes distance between enemy and mage
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

    //different from both knight and ranger pair
    public static class Pair {
        int unit1;
        int unit2;
        public Pair() {
            unit1 = -1;
            unit2 = -1;
        }
    }

    //finds best unit to attack
    public static Pair findBestUnit() {
        Pair ret = new Pair();
        VecUnit nearby = gc.senseNearbyUnitsByTeam(curUnit.location().mapLocation(), curUnit.visionRange(), Player.enemyTeam);
        if (nearby.size() == 0) {
            return ret;
        }
        int net = 0;
        int total = 0;
        int smallest = 9999999;
        for (int i = 0; i < nearby.size(); i++) {
            int tempnet = 0;
            int temptotal = 0;
            MapLocation point = nearby.get(i).location().mapLocation();
            if (nearby.get(i).team() == gc.team()) {
                tempnet -= curUnit.damage();
            } else {
                tempnet += curUnit.damage();
                temptotal += curUnit.damage();
                int tempdist = distance(point, curUnit.location().mapLocation());
                if (tempdist < smallest) {
                    smallest = tempdist;
                    ret.unit2 = nearby.get(i).id();
                }
            }
            for (int j = 0; j < directions.length; j++) {
                try {
                    Unit temp = gc.senseUnitAtLocation(point.add(directions[j]));
                    if (temp.team() == gc.team()) {
                        tempnet -= curUnit.damage();
                    } else {
                        tempnet += curUnit.damage();
                        temptotal += curUnit.damage();
                    }
                } catch (Exception e) {
                    continue;
                }
            }
            if (tempnet > net || (tempnet == net && temptotal > total)) {
                net = tempnet;
                total = temptotal;
                ret.unit1 = nearby.get(i).id();
            }
        }
        return ret;
    }

    public static boolean canAttack() {
        return curUnit.attackHeat() < 10;
    }

    public static boolean canMove() {
        return curUnit.movementHeat() < 10;
    }

    //get target unit should be pathing towards
    public static MapLocation getTarget() {
        return Player.enemyLocation;
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
        if (!gc.isMoveReady(curUnit.id())) {
            return;
        }
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

                    int neighborPath = doubleHash(neighbor, goal);
                    if (Player.paths.containsKey(neighborPath)) {
                        //TODO: optimization once the killed has been fixed
                        //completes the path
                        int tempCur = neighbor;
                        while (tempCur != goal) {
                            int tempHash = doubleHash(neighbor, goal);
                            int nextNode = Player.paths.get(tempHash);
                            fromMap.put(nextNode, tempCur);
                            tempCur = nextNode;
                        }
                        neighbor = goal;
                    }
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
            MapLocation tryToGoTo = curUnit.location().mapLocation().add(temp);
            Unit blockedBy = gc.senseUnitAtLocation(tryToGoTo);
            if (blockedBy.unitType() == UnitType.Factory || blockedBy.unitType() == UnitType.Rocket || blockedBy.unitType() == UnitType.Worker) {
                moveAttack(target);
            }
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
