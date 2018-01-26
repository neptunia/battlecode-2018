import bc.*;
import java.util.*;

public class Player {

    static Direction[] directions = Direction.values();
    static Team myTeam, enemyTeam;
    static GameController gc;
    static boolean[][] hasKarbonite;
    static int[][][] pathDistances;
    static int gridX, gridY;
    static boolean[][] passable;
    static boolean[][][] gotoable;
    static ArrayList<Unit> newUnits = new ArrayList<Unit>();
    static HashMap<Integer, MapLocation> initialWorkerStartingLocation = new HashMap<Integer, MapLocation>();
    static MapLocation[] enemyLocation;
    static long effectiveKarbonite;
    static long karboniteGonnaUse = 0;
    static int numFactory, numRanger, numHealer, numWorker;
    static int knightsProduced = 0;
    static int timesReachedTarget = 0;
    static int blockedCount, prevBlocked = 0;
    static boolean marsBfsDone = false;
    static MapLocation[] unitLocations = new MapLocation[2500];
    static int unitLocationCounter;
    static HashMap<Integer, MapLocation> priorityTarget = new HashMap<Integer, MapLocation>();


	public static void main(String args[]) {
        GameController gc = new GameController();
        Player.gc = gc;
        Worker.gc = gc;
        Factory.gc = gc;
        Ranger.gc = gc;
        Healer.gc = gc;
        Rocket.gc = gc;
        Knight.gc = gc;

        myTeam = gc.team();
        enemyTeam = (myTeam == Team.Red ? Team.Blue : Team.Red);

        initialize();

        gc.queueResearch(UnitType.Worker);
        gc.queueResearch(UnitType.Knight);
        gc.queueResearch(UnitType.Healer);
        gc.queueResearch(UnitType.Healer);
        gc.queueResearch(UnitType.Rocket);
        gc.queueResearch(UnitType.Healer);
        gc.queueResearch(UnitType.Ranger);
        gc.queueResearch(UnitType.Ranger);
        gc.queueResearch(UnitType.Rocket);

        while (true) {

            if (gc.planet() == Planet.Mars) {
                if (gc.asteroidPattern().hasAsteroid(gc.round())) {
                    AsteroidStrike aster = gc.asteroidPattern().asteroid(gc.round());
                    MapLocation asteroid = aster.getLocation();
                    Worker.karbonitePatches[asteroid.getX()][asteroid.getY()] = true;
                    Worker.noMoreKarbonite = false;
                }
            }

            VecUnit myUnits = gc.myUnits();

            //count units
            numFactory = 0;
            numRanger = 0;
            numHealer = 0;
            numWorker = 0;
            blockedCount = 0;
            unitLocationCounter = 0;
            for (int i = 0; i < myUnits.size(); i++) {
                Unit curUnit = myUnits.get(i);
                UnitType type = curUnit.unitType();
                if (type == UnitType.Factory) {
                    numFactory++;
                } else if (type == UnitType.Ranger) {
                    numRanger++;
                } else if (type == UnitType.Healer) {
                    numHealer++;
                } else if (type == UnitType.Worker) {
                    numWorker++;
                }
                if (!curUnit.location().isInGarrison()) {
                    unitLocations[unitLocationCounter] = curUnit.location().mapLocation();
                    unitLocationCounter++;
                }
            }
            
            for (int i = 0; i < myUnits.size(); i++) {
                Unit curUnit = myUnits.get(i);
                
                //perform unit task based on unit type
                try {
                    switch (curUnit.unitType()) {
                        case Factory:
                            Factory.run(curUnit);
                            break;
                            /*
                        
                        
                        case Mage:
                            Mage.run(gc, curUnit);
                            break;
                        */
                        case Knight:
                            Knight.run(curUnit);
                            break;
                        case Rocket:
                            Rocket.run(curUnit);
                            break;
                        case Healer:
                            Healer.run(curUnit);
                            break;
                        case Ranger:
                            Ranger.run(curUnit);
                            break;
                        case Worker:
                            Worker.run(curUnit);
                            break;
                    }
                } catch (Exception e) {
                    System.out.println("unit died");
                    e.printStackTrace();
                }
            }
            prevBlocked = blockedCount;
            for (int i = 0; i < newUnits.size(); i++) {
                Unit curUnit = newUnits.get(i);
                try {
                    switch (curUnit.unitType()) {
                        case Factory:
                            Factory.run(curUnit);
                            break;
                            /*
                        
                        
                        case Mage:
                            Mage.run(gc, curUnit);
                            break;
                        */
                        case Knight:
                            Knight.run(curUnit);
                            break;
                        case Rocket:
                            Rocket.run(curUnit);
                            break;
                        case Healer:
                            Healer.run(curUnit);
                            break;
                        case Ranger:
                            Ranger.run(curUnit);
                            break;
                        case Worker:
                            Worker.run(curUnit);
                            break;
                    }
                } catch (Exception e) {
                    System.out.println("unit died");
                    e.printStackTrace();
                }
            }
            newUnits.clear();
            chooseTarget();

            if (gc.round() % 10 == 0) {
                System.gc();
            }
            System.out.println(gc.getTimeLeftMs());
            gc.nextTurn();
        }
    }

    public static void initialize() {
        PlanetMap planetMap = gc.startingMap(gc.planet());
        int width = (int) planetMap.getWidth();
        int height = (int) planetMap.getHeight();
        pathDistances = new int[69 * width + height][width][height];
        for (int i = 0; i < 69 * width + height; i++) {
            for (int a = 0; a < width; a++) {
                for (int j = 0; j < height; j++) {
                    pathDistances[i][a][j] = 696969;
                }
            }
        }
        gridX = width;
        gridY = height;

        passable = new boolean[width][height];
        for (int i = 0; i < width; i++) {
            for (int a = 0; a < height; a++) {
                if (planetMap.isPassableTerrainAt(new MapLocation(gc.planet(), i, a)) != 0) {
                    passable[i][a] = true;
                }
            }
        }

        int[][] karbonite = new int[width][height];

        for (int i = 0; i < width; i++) {
            for (int a = 0; a < height; a++) {
                MapLocation test = new MapLocation(gc.planet(), i, a);
                int amnt = (int) planetMap.initialKarboniteAt(test);
                karbonite[i][a] = amnt;
            }
        }
        
        boolean[][] good = new boolean[width][height];
        int[][] sums = new int[width][height];

        
        for (int i = 0; i < width; i++) {
            for (int a = 0; a < height; a++) {
                MapLocation test = new MapLocation(gc.planet(), i, a);
                int sum = 0;
                for (int j = 0; j < directions.length; j++) {
                    MapLocation test2 = test.add(directions[j]);
                    if (onMap(test2)) {
                        sum += karbonite[test2.getX()][test2.getY()];
                    }
                }
                sums[i][a] = sum;
                if (sum >= 25) {
                    good[i][a] = true;
                }
            }
        }

        boolean[][] spots = new boolean[width][height];
        int count = 0;
        for (int i = 0; i < width; i++) {
            for (int a = 0; a < height; a++) {
                MapLocation test = new MapLocation(gc.planet(), i, a);
                if (good[i][a] && passable[test.getX()][test.getY()]) {
                    spots[i][a] = true;
                    count++;
                    for (int j = 0; j < directions.length; j++) {
                        MapLocation test2 = test.add(directions[j]);
                        if (onMap(test2)) {
                            good[test2.getX()][test2.getY()] = false;
                        }
                    }
                }
            }
        }

        VecUnit startingUnits = gc.startingMap(gc.planet()).getInitial_units();

        MapLocation myStartLocation, enemyStartLocation;

        myStartLocation = chooseClosestPoint(gc.team(), width, height);

        Team enemyTeam = gc.team() == Team.Red ? Team.Blue : Team.Red;

        enemyStartLocation = chooseClosestPoint(enemyTeam, width, height);

        gotoable = new boolean[(int) startingUnits.size() / 2][width][height];
        enemyLocation = new MapLocation[(int) startingUnits.size() / 2];
        //MapLocation[][] ret = new MapLocation[(int) startingUnits.size() / 2][count];
        Worker.replicationLimit = new int[(int) startingUnits.size() / 2];
        Worker.counter = new int[(int) startingUnits.size() / 2];
        int c = 0;
        for (int i = 0; i < startingUnits.size(); i++) {
            Unit unit = startingUnits.get(i);
            if (unit.team() == gc.team()) {
                if (unit.location().isInGarrison()) {
                    continue;
                }
                MapLocation startLoc = unit.location().mapLocation();
                boolean alreadySeen = false;
                for (int j = 0; j < c; j++) {
                    if (gotoable[j][startLoc.getX()][startLoc.getY()]) {
                        Worker.id.put(unit.id(), j);
                        alreadySeen = true;
                    }
                }
                if (alreadySeen) {
                    continue;
                }
                int workerLimit = 0;
                //save worker starting location
                initialWorkerStartingLocation.put(c, unit.location().mapLocation());
                //create array of karbonite spots
                Worker.id.put(unit.id(), c);
                
                //perform bfs on this unit's location to find spots in order of distance
                LinkedList<MapLocation> queue = new LinkedList<MapLocation>();
                HashSet<Integer> visited = new HashSet<Integer>();
                queue.add(startLoc);
                visited.add(hash(startLoc));
                int c2 = 0;
                while (!queue.isEmpty()) {
                    MapLocation current = queue.poll();
                    gotoable[c][current.getX()][current.getY()] = true;
                    if (spots[current.getX()][current.getY()]) {// && manDistance(current, myStartLocation) >= manDistance(current, enemyStartLocation)) {
                        //ret[c][c2] = current;
                        if (manDistance(current, myStartLocation) >= manDistance(current, enemyStartLocation) && sums[current.getX()][current.getY()] >= 50) {
                            workerLimit++;
                        }
                        c2++;
                    }
                    for (int a = 0; a < directions.length; a++) {
                        MapLocation test = current.add(directions[a]);
                        if (!visited.contains(hash(test)) && onMap(test) && passable[test.getX()][test.getY()]) {
                            visited.add(hash(test));
                            queue.add(test);
                        }
                    }
                }
                enemyLocation[c] = chooseFarthestPoint(c);
                Worker.replicationLimit[c] = Math.min(Math.max(workerLimit, 6), 30);
                c++;
            }
        }
        //Worker.spots = ret;
        Worker.karbonitePatches = spots;

        //precompute landing spots for Mars.
        if (gc.planet() == Planet.Mars && !marsBfsDone) {
            c = 1;
            //first, do bfs and set values of map
            PlanetMap startingmap = gc.startingMap(Planet.Mars);
            int[][] map = new int[width][height];
            HashSet<Integer> visitedmars = new HashSet<Integer>();
            for (int i = 0; i < startingmap.getWidth(); i++) {
                for (int j = 0; j < startingmap.getHeight(); j++) {
                    //first, mark all unpassable tiles as visited
                    if (!visitedmars.contains(hash(i,j))) {
                        visitedmars.add(hash(i,j));
                        if (!passable[i][j]) {
                            //unpassable
                            map[i][j] = -1;
                        } else {
                            //passable, run bfs on it!
                            Queue<MapLocation> marsbfs = new LinkedList<MapLocation>();
                            MapLocation startingpoint = new MapLocation(Planet.Mars,i,j);
                            marsbfs.add(startingpoint);
                            visitedmars.add(hash(startingpoint));
                            while (!marsbfs.isEmpty()) {
                                //System.out.println(visitedmars.size());
                                MapLocation nextpoint = marsbfs.poll();
                                map[nextpoint.getX()][nextpoint.getY()] = c;
                                for (int k = 0; k < directions.length-1; k++) {
                                    MapLocation toAdd = nextpoint.add(directions[k]);
                                    if (!visitedmars.contains(hash(toAdd.getX(), toAdd.getY())) && onMap(toAdd) && passable[toAdd.getX()][toAdd.getY()]) {
                                        visitedmars.add(hash(toAdd));
                                        marsbfs.add(toAdd);
                                    }
                                }
                            }
                            c++;
                        }
                    }
                }
            }
            //System.out.println("bfs done");
            //now count how large each section is
            HashMap<Integer, Integer> sizeOfSection = new HashMap<Integer, Integer>();

            for (int i = 0; i < startingmap.getWidth(); i++) {
                for (int j = 0; j < startingmap.getHeight(); j++) {
                    if (map[i][j] != -1) {
                        if (!sizeOfSection.containsKey(map[i][j])) {
                            sizeOfSection.put(map[i][j],1);
                        } else {
                            sizeOfSection.put(map[i][j], sizeOfSection.get(map[i][j])+1);
                        }
                    }
                }
            }
            if (sizeOfSection.containsKey(0)) {
                //bfs is broken (not all nodes are being visited)
                //System.out.println("fix mars initialization");
            }

            Integer[] bestSwaths = sizeOfSection.keySet().toArray(new Integer[sizeOfSection.keySet().size()]);
            Arrays.sort(bestSwaths, new Comparator<Integer>() {
                @Override
                public int compare(Integer o1, Integer o2) {
                    return sizeOfSection.get(o1).compareTo(sizeOfSection.get(o2));
                }
            });

            c = 0;
            int swathIndex = bestSwaths.length - 1;
            while (c < 100 && swathIndex >= 0) {
                for (int i = 0; i < startingmap.getWidth(); i += 3) {
                    for (int j = 0; j < startingmap.getHeight(); j += 3) {
                        //adding by two ensures that landing spots will never be adjacent
                        //inb4 they cuck us and give us only tiny pockets
                        if (map[i][j] == bestSwaths[swathIndex] && c < 100) {
                            gc.writeTeamArray(c, hash(i,j));
                            c++;
                        }
                    }
                }
                swathIndex--;
            }
            marsBfsDone = true;
        }
    }

    public static void marsInitialize() {
        gotoable = new boolean[1][gridX][gridY];
        VecUnit myUnits = gc.myUnits();
        for (int i = 0; i < myUnits.size(); i++) {
            Unit curUnit = myUnits.get(i);
            if (curUnit.location().isInGarrison()) {
                continue;
            }
            LinkedList<MapLocation> queue = new LinkedList<MapLocation>();
            HashSet<Integer> visited = new HashSet<Integer>();
            MapLocation start = curUnit.location().mapLocation();
            queue.add(start);
            visited.add(hash(start));
            while (!queue.isEmpty()) {
                MapLocation current = queue.poll();
                gotoable[0][current.getX()][current.getY()] = true;
                for (int a = 0; a < directions.length; a++) {
                    MapLocation test = current.add(directions[a]);
                    if (!visited.contains(hash(test)) && onMap(test) && passable[test.getX()][test.getY()]) {
                        queue.add(test);
                        visited.add(hash(test));
                    }
                }
            }
        }
        enemyLocation = new MapLocation[1];
        enemyLocation[0] = chooseFarthestPoint(0);
    }

    public static void chooseTarget() {
        for (int i = 0; i < enemyLocation.length; i++) {
            if (enemyLocation[i] != null) {
                if (gc.senseNearbyUnitsByTeam(enemyLocation[i], 0, myTeam).size() > 0) {
                    timesReachedTarget++;
                    enemyLocation[i] = chooseFarthestPoint(i);
                }
            }
            
        }
    }

    public static MapLocation chooseFarthestPoint(int parent) {
        double greatest = -1;
        int smallX = -1;
        int smallY = -1;
        for (int i = 0; i < gridX; i++) {
            for (int a = 0; a < gridY; a++) {
                if (!gotoable[parent][i][a]) {
                    continue;
                }
                double tempDist = 0;
                for (int j = 0; j < unitLocationCounter; j++) {
                    tempDist += manDistance(i, a, unitLocations[j].getX(), unitLocations[j].getY());
                }
                if (tempDist > greatest) {
                    greatest = tempDist;
                    smallX = i;
                    smallY = a;
                }
            }
        }
        MapLocation point = new MapLocation(gc.planet(), smallX, smallY);
        //BFS the whole map
        bfs(point);
        return point;
    }

    public static MapLocation chooseClosestPoint(Team team, int width, int height) {
        VecUnit startingUnits = gc.startingMap(gc.planet()).getInitial_units();
        double greatest = 9999999;
        int smallX = -1;
        int smallY = -1;
        for (int i = 0; i < width; i++) {
            for (int a = 0; a < height; a++) {
                double tempDist = 0;
                for (int j = 0; j < startingUnits.size(); j++) {
                    Unit tempUnit = startingUnits.get(j);
                    if (tempUnit.team() == team) {
                        MapLocation temp = tempUnit.location().mapLocation();
                        tempDist += distanceSq(i, a, temp.getX(), temp.getY());
                    }
                }
                if (tempDist < greatest) {
                    greatest = tempDist;
                    smallX = i;
                    smallY = a;
                }
            }
        }
        MapLocation point = new MapLocation(gc.planet(), smallX, smallY);
        return point;
    }



    public static void bfs(MapLocation point) {
        LinkedList<MapLocation> queue = new LinkedList<MapLocation>();
        HashMap<Integer, Integer> visitedDistances = new HashMap<Integer, Integer>();
        queue.add(point);
        int pointHash = hash(point);
        visitedDistances.put(hash(point), 0);
        while (!queue.isEmpty()) {
            MapLocation current = queue.poll();
            int curHash = hash(current);
            pathDistances[pointHash][current.getX()][current.getY()] = visitedDistances.get(curHash);
            for (int i = 0; i < directions.length; i++) {
                MapLocation neighbor = current.add(directions[i]);
                int neighborHash = hash(neighbor);
                if (!visitedDistances.containsKey(neighborHash) && onMap(neighbor) && passable[neighbor.getX()][neighbor.getY()]) {
                    visitedDistances.put(neighborHash, visitedDistances.get(curHash) + 1);
                    queue.add(neighbor);
                }
            }
        }
    }

    public static boolean bfsMin(MapLocation point, MapLocation stop) {
        LinkedList<MapLocation> queue = new LinkedList<MapLocation>();
        HashMap<Integer, Integer> visitedDistances = new HashMap<Integer, Integer>();
        queue.add(point);
        int pointHash = hash(point);
        int stopHash = hash(stop);
        visitedDistances.put(hash(point), 0);
        int stopDistance = 99999999;
        int check = 99999999;
        while (!queue.isEmpty()) {
            MapLocation current = queue.poll();
            int curHash = hash(current);
            int distance = visitedDistances.get(curHash);
            pathDistances[pointHash][current.getX()][current.getY()] = distance;
            if (stopHash == curHash) {
                check = distance;
                return true;
            }
            for (int i = 0; i < directions.length; i++) {
                MapLocation neighbor = current.add(directions[i]);
                int neighborHash = hash(neighbor);
                if (!visitedDistances.containsKey(neighborHash) && onMap(neighbor) && passable[neighbor.getX()][neighbor.getY()]) {
                    visitedDistances.put(neighborHash, visitedDistances.get(curHash) + 1);
                    queue.add(neighbor);
                }
            }
        }
        return false;
    }

    public static boolean onMap(MapLocation loc) {
        int x = loc.getX();
        int y = loc.getY();
        return x >= 0 && y >= 0 && x < gridX && y < gridY;
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

    public static int manDistance(int x1, int y1, int x2, int y2) {
        return Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2));
    }

    public static int hash(MapLocation loc) {
        return 69 * loc.getX() + loc.getY();
    }

    public static int hash(int x, int y) {
        return 69 * x + y;
    }

    public static double distanceSq(int x1, int y1, int x2, int y2) {
        return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
    }

}