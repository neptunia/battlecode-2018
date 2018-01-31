import bc.*;

import java.util.*;

public class Mage {

    static Unit curUnit;
    static GameController gc;
    static MapLocation curLoc;
    static Direction[] directions = Direction.values();
    static HashMap<Integer, HashSet<Integer>> visited = new HashMap<Integer, HashSet<Integer>>();
    static HashMap<Integer, Integer> prevLocation = new HashMap<Integer, Integer>();
    static Planet curPlanet;
    static int myId;

    public static void run(GameController gc, Unit curUnit) {
        Mage.curUnit = curUnit;
        curPlanet = gc.planet();

        if (curUnit.location().isInGarrison()) {
            return;
        }

        myId = Worker.id.get(curUnit.id());

        curLoc = curUnit.location().mapLocation();

        if (gc.researchInfo().getLevel(UnitType.Mage) == 4 && gc.researchInfo().getLevel(UnitType.Healer) == 3 && gc.round() % 10 == 0) {
            mageNuke();
            return;
        }

        Pair bestunit = findBestUnit();
        if (bestunit.unit2 == -1) {
            //no visible units
            if (canMove()) {
                move(Player.enemyLocation[myId]);
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

    // guaranteed killability.
    public static boolean isKillable(int enemyid) {
        int myDamage = curUnit.damage();
        if (gc.unit(enemyid).unitType() == UnitType.Knight) {
            return (myDamage - gc.unit(enemyid).knightDefense()) > gc.unit(enemyid).health();
        } else {
            return myDamage > gc.unit(enemyid).health();
        }
    }

    // guaranteed killability.
    public static boolean isKillable(int enemyid, int numOvercharges) {
        int myDamage = curUnit.damage();
        if (gc.unit(enemyid).unitType() == UnitType.Knight) {
            return (myDamage - gc.unit(enemyid).knightDefense())*(1+numOvercharges) > gc.unit(enemyid).health();
        } else {
            return myDamage*(1+numOvercharges) > gc.unit(enemyid).health();
        }
    }

    public static void geyMage() {
        VecUnit nearby = gc.senseNearbyUnits(curLoc, 30);
        ArrayList<Integer> nearbyHealers = new ArrayList<Integer> ();

        int overchargesAvailable = 0;
        for (int i = 0; i < nearby.size(); i++) {
            Unit temp = nearby.get(i);
            if (temp.team() == gc.team() && temp.unitType() == UnitType.Healer && temp.abilityHeat() < 10) {
                overchargesAvailable++;
                nearbyHealers.add(temp.id());
            }
        }

        int target = bestAttack(overchargesAvailable);

        if (target != -1) {
            // just stand there and attack
            while (overchargesAvailable >= 0) {
                if (canAttack() && gc.canAttack(curUnit.id(), target)) {
                    gc.attack(curUnit.id(), target);
                }
                
                int target = bestAttack(overchargesAvailable);
                if (target == -1) {
                    break;
                }

                gc.overcharge(healerid, curUnit.id);
                overchargesAvailable--;

            }
        }

        


    }

    public static void mageNuke() {
        // should only ever be called if overcharge and blink are researched!
        Pair bestunit = findUnits(70);
        if (bestunit.unit2 == -1) {
            //no visible units
            if (canMove()) {
                move(Player.enemyLocation[myId]);
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
            MapLocation enemyLoc = gc.unit(bestunit.unit1).location().mapLocation();
            int dst = distance(curLoc, enemyLoc);
            if (dst <= 30) {
                // attack
                gc.attack(curUnit.id(), bestunit.unit1);
                // move backwards
                if (canMove()) {
                    moveAway(enemyLoc);
                }
                blinkAway(enemyLoc);
                return;
            }

            // moveattack
            if (gc.isMoveReady(curUnit.id())) {
                moveCloser(enemyLoc);
            }
            if (distance(curLoc, enemyLoc) > 30) {
                // rip move failed
                blinkIntoRange(enemyLoc);
            }
            if (distance(curLoc, enemyLoc) > 30 && gc.isMoveReady(curUnit.id())) {
                // rip move failed
                moveCloser(enemyLoc);
            }

            // now attempt attack
            int toAttack = findUnits(30).unit1;
            if (toAttack != -1) {
                gc.attack(curUnit.id(), toAttack);
            }

            // now attempt retreat
            if (canMove() && gc.isMoveReady(curUnit.id())) {
                moveAway(enemyLoc);
            }
            blinkAway(enemyLoc);
            return;
            


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

    // flash towards a location.
    public static void blinkCloser(MapLocation location) {
        MapLocation myLoc = curUnit.location().mapLocation();
        int myX = myLoc.getX();
        int myY = myLoc.getY();
        Planet myPlanet = myLoc.getPlanet();
        MapLocation test;
        MapLocation best = null;
        int closestDist = distance(myLoc, location);
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if (i == 0 && j == 0) {
                    continue;
                }
                test = new MapLocation(myPlanet, myX + i, myY + j);
                if (checkPassable(test)) {
                    int dist = distance(location, test);
                    if (dist < closestDist) {
                        closestDist = dist;
                        best = test;
                    }
                }

            }
        }
        // now blink there
        if (best != null) {
            if (curUnit.abilityHeat() < 10 && gc.canBlink(curUnit.id(), best)) {
                gc.blink(curUnit.id(), best);
                curLoc = curUnit.location().mapLocation();
            }
        }


    }

    public static void blinkAway(MapLocation location) {
        MapLocation myLoc = curUnit.location().mapLocation();
        int myX = myLoc.getX();
        int myY = myLoc.getY();
        Planet myPlanet = myLoc.getPlanet();
        MapLocation test;
        MapLocation best = null;
        int closestDist = distance(myLoc, location);
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if (i == 0 && j == 0) {
                    continue;
                }
                test = new MapLocation(myPlanet, myX + i, myY + j);
                if (checkPassable(test)) {
                    int dist = distance(location, test);
                    if (dist > closestDist) {
                        closestDist = dist;
                        best = test;
                    }
                }

            }
        }
        // now blink there
        if (best != null) {
            if (curUnit.abilityHeat() < 10 && gc.canBlink(curUnit.id(), best)) {
                gc.blink(curUnit.id(), best);
                curLoc = curUnit.location().mapLocation();
            }
        }


    }

    // get as close as possible until within range (try for exactly 30 range. don't want to overextend)
    public static void blinkIntoRange(MapLocation location) {
        MapLocation myLoc = curUnit.location().mapLocation();
        int myX = myLoc.getX();
        int myY = myLoc.getY();
        Planet myPlanet = myLoc.getPlanet();
        MapLocation test;
        MapLocation best = null;
        int closestDist = distance(myLoc, location);
        if (closestDist == 30) {
            return;
        }
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if (i == 0 && j == 0) {
                    continue;
                }
                test = new MapLocation(myPlanet, myX + i, myY + j);
                if (checkPassable(test)) {
                    int dist = distance(location, test);
                    if (closestDist <= 30 && dist <= 30 && dist < closestDist) {
                        // if i can get within range, pick the best spot
                        closestDist = dist;
                        best = test;
                    } else if (closestDist > 30 && dist < closestDist) {
                        // if im currently outside range and i just want to get as close as possible
                        // yes i get that these two conditions can be merged but its too long then and harder to follow
                        closestDist = dist;
                        best = test;
                    }
                }

            }
        }
        // now blink there
        if (best != null) {
            if (curUnit.abilityHeat() < 10 && gc.canBlink(curUnit.id(), best)) {
                gc.blink(curUnit.id(), best);
                curLoc = curUnit.location().mapLocation();
            }
        }

    }

    public static void mageMicro(int enemyid, int closestenemy) {
        MapLocation enemyLoc = gc.unit(enemyid).location().mapLocation();
        MapLocation closestLoc = gc.unit(closestenemy).location().mapLocation();
        MapLocation myLoc = curUnit.location().mapLocation();
        
        int dist = distance(myLoc, enemyLoc);
        int closestdist = distance(myLoc, closestLoc);

        if (closestdist <= 30) {
            // too close!
            // only attack it if I can kill it. If not, it's a lost cause anyways.
            // TODO: if an enemy worker is next to me i don't really care
            if (isKillable(closestenemy)) {
                if (gc.isMoveReady(curUnit.id()) && closestdist <= 2) {
                    moveAway(closestLoc);
                }
                // note: putting attack here means that the mage commits suicide if it was unable to move.
                // however, it also kills the enemy. Maybe change later.
                if (canAttack() && gc.canAttack(curUnit.id(), closestenemy)) {
                    gc.attack(curUnit.id(), closestenemy);
                }

                if (gc.isMoveReady(curUnit.id())) {
                    moveAway(closestLoc);
                }
            } else {
                if (canAttack() && gc.canAttack(curUnit.id(), enemyid)) {
                    gc.attack(curUnit.id(), enemyid);
                }
                if (gc.isMoveReady(curUnit.id())) {
                    moveAway(closestLoc);
                }
                if (gc.isAttackReady(curUnit.id()) && gc.canAttack(curUnit.id(), closestenemy)) {
                    gc.attack(curUnit.id(), closestenemy);
                }
            }

            return;
        }
        // if they're in attack range, attack them and run
        if (dist <= 30) {
            if (canAttack()) {
                gc.attack(curUnit.id(), enemyid);
            }
            if (gc.isMoveReady(curUnit.id())) {
                moveAway(enemyLoc);
            }
            return;
        }
        if (dist <= 48) {
            // try moving closer and attack
            if (gc.isMoveReady(curUnit.id()) && canAttack() && moveCloser(enemyLoc) && gc.canAttack(curUnit.id(), enemyid)) {
                // move was successful
                gc.attack(curUnit.id(), enemyid);
            } else {
                if (gc.canAttack(curUnit.id(), closestenemy) && gc.isAttackReady(curUnit.id())) {
                    gc.attack(curUnit.id(), closestenemy);
                }
            }
            return;
        }
    }

    public static void moveIfNeeded(MapLocation enemy) {
        if (distance(enemy, curUnit.location().mapLocation()) <= 2) {
            moveAway(enemy);
        }
    }



    //calc which direction maximizes distance between enemy and mage
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
            curLoc = curUnit.location().mapLocation();
            return true;
        }
        return false;
    }

    // calc which direction minimizes distance between enemy and mage
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
            curLoc = curUnit.location().mapLocation();
            return true;
        }
        return false;
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
        // 48 is attackmove range
        VecUnit nearby = gc.senseNearbyUnits(curLoc, 48);
        if (nearby.size() == 0) {
            return ret;
        }
        int net = 1;
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

    public static int bestAttack(int numOvercharges) {
        int ret = -1;
        net = 0;

        VecUnit nearby = gc.senseNearbyUnitsByTeam(curLoc, searchRange, Player.enemyTeam);
        if (nearby.size() == 0) {
            return ret;
        }
        for (int i = 0; i < nearby.size(); i++) {
            int tempnet = 0;
            int temptotal = 0;
            MapLocation point = nearby.get(i).location().mapLocation();
            if (nearby.get(i).team() == gc.team()) {
                tempnet -= curUnit.damage();
            } else {
                tempnet += curUnit.damage();
                temptotal += curUnit.damage();
                // weight kills more i guess
                if isKillable(nearby.get(i).id(),numOvercharges) {
                    tempnet += 100;
                    temptotal += 100;
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
                        // weight kills more i guess
                        if isKillable(temp.id(),numOvercharges) {
                            tempnet += 100;
                            temptotal += 100;
                        }
                    }
                } catch (Exception e) {
                    continue;
                }
            }
            if (tempnet > net || (tempnet == net && temptotal > total)) {
                net = tempnet;
                total = temptotal;
                ret = i.id();
            }
        }
        return ret;

    }

    public static Pair findUnits(int searchRange) {
        Pair ret = new Pair();
        // 48 is attackmove range
        VecUnit nearby = gc.senseNearbyUnitsByTeam(curLoc, searchRange, Player.enemyTeam);
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

    public static boolean moveAttack(MapLocation target) {
        //greedy pathfinding
        int smallest = 999999;
        Direction d = null;
        int curDist = distance(curUnit.location().mapLocation(), target);
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
        curLoc = curUnit.location().mapLocation();
        return true;
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
                curLoc = curUnit.location().mapLocation();
            } else if (x > 0 && Player.pathDistances[targetHash][x - 1][y] - currentDist < 0 && gc.canMove(curUnit.id(), Direction.West)) {
                gc.moveRobot(curUnit.id(), Direction.West);
                curLoc = curUnit.location().mapLocation();
            } else if (y < Player.gridY - 1 && Player.pathDistances[targetHash][x][y + 1] - currentDist < 0 && gc.canMove(curUnit.id(), Direction.North)) {
                gc.moveRobot(curUnit.id(), Direction.North);
                curLoc = curUnit.location().mapLocation();
            } else if (y > 0 && Player.pathDistances[targetHash][x][y - 1] - currentDist < 0 && gc.canMove(curUnit.id(), Direction.South)) {
                gc.moveRobot(curUnit.id(), Direction.South);
                curLoc = curUnit.location().mapLocation();
            } else if (y < Player.gridY - 1 && x < Player.gridX - 1 && Player.pathDistances[targetHash][x + 1][y + 1] - currentDist < 0 && gc.canMove(curUnit.id(), Direction.Northeast)) {
                gc.moveRobot(curUnit.id(), Direction.Northeast);
                curLoc = curUnit.location().mapLocation();
            } else if (y > 0 && x < Player.gridX - 1 && Player.pathDistances[targetHash][x + 1][y - 1] - currentDist < 0 && gc.canMove(curUnit.id(), Direction.Southeast)) {
                gc.moveRobot(curUnit.id(), Direction.Southeast);
                curLoc = curUnit.location().mapLocation();
            } else if (x > 0 && y < Player.gridY - 1 && Player.pathDistances[targetHash][x - 1][y + 1] - currentDist < 0 && gc.canMove(curUnit.id(), Direction.Northwest)) {
                gc.moveRobot(curUnit.id(), Direction.Northwest);
                curLoc = curUnit.location().mapLocation();
            } else if (x > 0 && y > 0 && Player.pathDistances[targetHash][x - 1][y - 1] - currentDist < 0 && gc.canMove(curUnit.id(), Direction.Southwest)) {
                gc.moveRobot(curUnit.id(), Direction.Southwest);
                curLoc = curUnit.location().mapLocation();
            }
        } else {
            //bfs hasnt been run yet
            Player.bfs(target);
            move(target);
        }
        //i didn't move :(
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
        return Player.gotoable[myId][x][y];// && !allyThere;
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
