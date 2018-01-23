import bc.*;
import java.util.*;

public class Player {

    static boolean[][] passable;
    static Unit[][] map;
    static int gridX, gridY;
    static MapLocation[] enemyLocation;
    static MapLocation[] startingLocation;
    static Direction[] directions = Direction.values();
    static GameController gc;
    static PlanetMap planetMap;
    static Team myTeam, enemyTeam;
    static long prevIncome;
    static long currentIncome;
    static boolean[][][] gotoable;
    static boolean[][] hasKarbonite;
    static boolean[] split;
    static int blockedCount;
    static int prevBlocked;
    static int numRangers = 0, numHealers = 0;
    //static UnitType[][] units;
    static boolean gotoableEmpty;
    static MapLocation[] unitLocations;
    static MapLocation pastLoc = null;
    static int numUnitsThisRound;
    static int workerCount = 0;
    static int timesReachedTarget = 0;
    static boolean sawEnemy = false;
    static float averageTime = 0;
    //static boolean splitMap = false;
    static HashMap<Integer, Integer> paths = new HashMap<Integer, Integer>();
    static boolean marsBfsDone = false;
    static HashMap<Integer, MapLocation> priorityTarget = new HashMap<Integer, MapLocation>();
    static int[][][] pathDistances;
    static HashMap<Integer, Integer> parentWorker = new HashMap<Integer, Integer>();
    static MapLocation[] asteroidsBeforeLand = new MapLocation[2500];
    static int asteroidCounter = 0;
    static HashSet<Integer> useful = new HashSet<Integer>();


	public static void main(String args[]) {
        try {
            GameController gc = new GameController();

            prevIncome = 10 - Math.max(gc.karbonite() / 40, 0);
            Mage.gc = gc;
            Rocket.gc = gc;
            Healer.gc = gc;
            Factory.gc = gc;
            Ranger.gc = gc;
            Knight.gc = gc;
            Worker.gc = gc;
            Player.gc = gc;
            Player.planetMap = gc.startingMap(gc.planet());

            myTeam = gc.team();
            if (myTeam == Team.Red) {
                enemyTeam = Team.Blue;
            } else {
                enemyTeam = Team.Red;
            }

            long total = 0;

            VecUnit temp = gc.myUnits();
            numUnitsThisRound = 0;
            for (int i = 0; i < temp.size(); i++) {
                Unit curUnit = temp.get(i);
                try {
                    MapLocation curLoc = curUnit.location().mapLocation();
                    unitLocations[numUnitsThisRound] = curLoc;
                    numUnitsThisRound++;
                } catch (Exception e) {};
            }

            initialize();


            //do research
            gc.queueResearch(UnitType.Worker);
            gc.queueResearch(UnitType.Healer);
            gc.queueResearch(UnitType.Healer);
            gc.queueResearch(UnitType.Healer);
            gc.queueResearch(UnitType.Rocket);
            gc.queueResearch(UnitType.Ranger);
            gc.queueResearch(UnitType.Ranger);
            gc.queueResearch(UnitType.Worker);
            gc.queueResearch(UnitType.Worker);
            gc.queueResearch(UnitType.Rocket);


            long startTime = 10000;
            long endTime = 10000;

            unitLocations = new MapLocation[2500];
            //units = new UnitType[gridX][gridY];
            while (true) {
                if (gc.planet() == Planet.Earth && gc.round() >= 750) {
                    gc.nextTurn();
                }
                //TODO: change
                if (gc.planet() == Planet.Mars) {
                    if (gc.asteroidPattern().hasAsteroid(gc.round())) {
                        AsteroidStrike aster = gc.asteroidPattern().asteroid(gc.round());
                        if (gotoable.length == 0) {
                            asteroidsBeforeLand[asteroidCounter] = aster.getLocation();
                            asteroidCounter++;
                        } else {
                            for (int i = 0; i < gotoable.length; i++) {
                                if (gotoable[i][aster.getLocation().getX()][aster.getLocation().getY()]) {
                                    MapLocation asteroid = aster.getLocation();
                                    if (!Worker.karbonitesLeft[i]) {
                                        Worker.karbonitesLeft[i] = true;
                                    }
                                    hasKarbonite[asteroid.getX()][asteroid.getY()] = true;
                                }
                            }
                        }
                        
                    }
                }

                
                currentIncome = 10 - Math.max(gc.karbonite() / 40, 0);
                long currentRound = gc.round();
                VecUnit myUnits = gc.myUnits();
                numUnitsThisRound = 0;
                Worker.numWorkers = 0;
                Worker.numFacts = 0;
                blockedCount = 0;
                numRangers = 0;
                numHealers = 0;
                for (int i = 0; i < myUnits.size(); i++) {
                    Unit curUnit = myUnits.get(i);
                    try {
                        MapLocation curLoc = curUnit.location().mapLocation();
                        UnitType tempType = curUnit.unitType();
                        //units[curLoc.getX()][curLoc.getY()] = tempType;
                        unitLocations[numUnitsThisRound] = curLoc;
                        numUnitsThisRound++;
                        if (tempType == UnitType.Worker) {
                            Worker.numWorkers++;
                        } else if (tempType == UnitType.Factory) {
                            Worker.numFacts++;
                        } else if (tempType == UnitType.Ranger) {
                            numRangers++;
                        } else if (tempType == UnitType.Healer) {
                            numHealers++;
                        }
                    } catch (Exception e) {};
                }
                for (int i = 0; i < myUnits.size(); i++) {
                    Unit curUnit = myUnits.get(i);
                    
                    //perform unit task based on unit type
                    try {
                        switch (curUnit.unitType()) {
                            case Factory:
                                Factory.run(curUnit);
                                break;
                            case Healer:
                                Healer.run(gc, curUnit);
                                break;
                            case Knight:
                                Knight.run(gc, curUnit);
                                break;
                            case Mage:
                                Mage.run(gc, curUnit);
                                break;
                            case Ranger:
                            	Ranger.run(gc, curUnit);
                                break;
                            case Rocket:
                                Rocket.run(gc, curUnit);
                                break;
                            case Worker:
                                Worker.run(curUnit);
                        }
                    } catch (Exception e) {
                        //System.out.println("unit died");
                        //e.printStackTrace();
                    }
                }
                try {
                    chooseTarget();
                } catch (Exception e) {
                    //System.out.println("choose target borked");
                    //e.printStackTrace();
                }
                //System.out.println(blockedCount);
                prevBlocked = blockedCount;


                try {
                    endTime = gc.getTimeLeftMs();
                    total += startTime - endTime;
                    averageTime = total / gc.round();
                    //System.out.println("Time: " + Long.toString(startTime - endTime));
                    //System.out.println("Average: " + Float.toString(averageTime));
                    //System.out.println("Time Left: " + Long.toString(gc.getTimeLeftMs()));
                    prevIncome = currentIncome;
                    if (gc.round() % 25 == 0) {
                        System.gc();
                    }
                    //Runtime runtime = Runtime.getRuntime();

                    //NumberFormat format = NumberFormat.getInstance();

                    //StringBuilder sb = new StringBuilder();
                    //long maxMemory = runtime.maxMemory();
                    //long allocatedMemory = runtime.totalMemory();
                    //long freeMemory = runtime.freeMemory();

                    //sb.append("free memory: " + format.format(freeMemory / 1024) + "<br/>");
                    //sb.append("allocated memory: " + format.format(allocatedMemory / 1024) + "<br/>");
                    //sb.append("max memory: " + format.format(maxMemory / 1024) + "<br/>");
                    //sb.append("total free memory: " + format.format((freeMemory + (maxMemory - allocatedMemory)) / 1024) + "<br/>");
                    //System.out.println(sb);
                    //System.out.println("Enemy location: " + Integer.toString(enemyLocation.getX()) + ", " + Integer.toString(enemyLocation.getY()));
                    //System.out.println("Round: " + Long.toString(gc.round()));
                    startTime = endTime;
                } catch (Exception e) {
                    //System.out.println("time borked");
                    //e.printStackTrace();
                }
                gc.nextTurn();
            }
        } catch (Exception e) {
            //e.printStackTrace();
            //System.out.println("even worse");
        }
    }

    public static void initialize() {
        int width = (int) planetMap.getWidth();
        int height = (int) planetMap.getHeight();

        VecUnit myUnits = gc.myUnits();
        int numWorkers = (int) myUnits.size();
        gotoable = new boolean[numWorkers][width][height];
        enemyLocation = new MapLocation[numWorkers];
        Worker.karbonitesLeft = new boolean[numWorkers];
        Worker.numKarbsCounter = new int[numWorkers];
        Worker.karbAmount = new int[numWorkers];
        startingLocation = new MapLocation[numWorkers];
        hasKarbonite = new boolean[width][height];
        pathDistances = new int[width * 69 + height + 1][width][height];
        map = new Unit[width][height];
        passable = new boolean[width][height];
        split = new boolean[numWorkers];
        
        
        for (int i = 0; i < width * 69 + height + 1; i++) {
            for (int a = 0; a < width; a++) {
                for (int j = 0; j < height; j++) {
                    pathDistances[i][a][j] = 696969;
                }
            }
        }
        gridX = width;
        gridY = height;
        
        Worker.karbonites = new MapLocation[2500];

        

        for (int i = 0; i < width; i++) {
            for (int a = 0; a < height; a++) {
                MapLocation temp = new MapLocation(gc.planet(), i, a);
                if (planetMap.isPassableTerrainAt(temp) == 1) {
                    passable[i][a] = true;
                } else {
                    passable[i][a] = false;
                }
            }
        }

        

        //bfs to find which spaces can be traveled to
        
        
        for (int i = 0; i < numWorkers; i++) {
            Unit tempUnit = myUnits.get(i);
            if (tempUnit.location().isInGarrison()) {
                continue;
            }
            MapLocation curLoc = tempUnit.location().mapLocation();
            LinkedList<MapLocation> queue = new LinkedList<MapLocation>();
            HashSet<Integer> visited = new HashSet<Integer>();
            if (tempUnit.unitType() == UnitType.Worker && gc.planet() == Planet.Earth) {
                boolean visitedBefore = false;
                for (int a = 0; a < i; a++) {
                    if (gotoable[a][curLoc.getX()][curLoc.getY()] && useful.contains(a)) {
                        visitedBefore = true;
                        parentWorker.put(tempUnit.id(), a);
                        break;
                    }
                }
                if (!visitedBefore) {
                    queue.add(curLoc);
                    parentWorker.put(tempUnit.id(), i);
                    useful.add(i);
                    visited.add(hash(curLoc));
                }
            } else if (tempUnit.unitType() == UnitType.Rocket) {
                queue.add(curLoc);
                parentWorker.put(tempUnit.id(), i);
                useful.add(i);
                visited.add(hash(curLoc));
            }
            while (!queue.isEmpty()) {
                MapLocation current = queue.poll();
                gotoable[i][current.getX()][current.getY()] = true;
                //System.out.println(current);
                for (int a = 0; a < directions.length; a++) {
                    MapLocation toCheck = current.add(directions[a]);
                    int tempHash = hash(toCheck);
                    if (!visited.contains(tempHash) && checkPassable(toCheck)) {
                        visited.add(tempHash);
                        queue.add(toCheck);
                    }
                }
            }
            if (useful.contains(i)) {
                startingLocation[i] = chooseClosestPoint(i);
            }
        }


        for (int i = 0; i < width; i++) {
            for (int a = 0; a < height; a++) {
                MapLocation test = new MapLocation(gc.planet(), i, a);
                int amnt = (int) planetMap.initialKarboniteAt(test);
                if (amnt > 0) {
                    //karbs[i][a] = true;
                    hasKarbonite[i][a] = true;
                    for (int j = 0; j < numWorkers; j++) {
                        if (gotoable[j][i][a]) {
                            Worker.numKarbsCounter[j]++;
                            Worker.karbAmount[j] += amnt / 2;
                            if (!Worker.karbonitesLeft[j]) {
                                Worker.karbonitesLeft[j] = true;
                            }
                        }
                    }
                }
            }
        }

        

        

        //precompute landing spots for Mars.
        if (gc.planet() == Planet.Mars && !marsBfsDone) {
            int c = 1;
            //first, do bfs and set values of map
            PlanetMap startingmap = gc.startingMap(Planet.Mars);
            int[][] map = new int[(int)startingmap.getWidth()][(int)startingmap.getHeight()];
            HashSet<Integer> visitedmars = new HashSet<Integer>();
            for (int i = 0; i < startingmap.getWidth(); i++) {
                for (int j = 0; j < startingmap.getHeight(); j++) {
                    //first, mark all unpassable tiles as visited
                    if (!visitedmars.contains(hash(i,j))) {
                        visitedmars.add(hash(i,j));
                        if (startingmap.isPassableTerrainAt(new MapLocation(Planet.Mars, i, j)) == 0) {
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
                                    if (!visitedmars.contains(hash(toAdd.getX(), toAdd.getY())) && startingmap.onMap(toAdd) && startingmap.isPassableTerrainAt(toAdd) != 0) {
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

            int max = -1;
            int best = 0;
            //decide landing points
            //land all rockets in biggest section
            for (int i : sizeOfSection.keySet()) {
                if (sizeOfSection.get(i) > max) {
                    max = sizeOfSection.get(i);
                    best = i;
                }
            }
            c = 0;
            for (int i = 0; i < startingmap.getWidth(); i += 3) {
                for (int j = 0; j < startingmap.getHeight(); j += 3) {
                    //adding by two ensures that landing spots will never be adjacent
                    //inb4 they cuck us and give us only tiny pockets
                    if (map[i][j] == best && c < 100) {
                        gc.writeTeamArray(c, hash(i,j));
                        c++;
                    }
                }
            }
            marsBfsDone = true;
        }

        if (gotoableEmpty) {
            for (int i = 0; i < asteroidCounter; i++) {
                MapLocation aster = asteroidsBeforeLand[i];
                for (int a = 0; a < gotoable.length; a++) {
                    if (!Worker.karbonitesLeft[a] && gotoable[a][aster.getX()][aster.getY()]) {
                        Worker.karbonitesLeft[a] = true;
                    }
                    hasKarbonite[aster.getX()][aster.getY()] = true;
                }
            }
        }
        if (numWorkers == 0) {
            gotoableEmpty = true;
        } else {
            gotoableEmpty = false;
        }
        for (int i = 0; i < enemyLocation.length; i++) {
            enemyLocation[i] = null;
        }

        VecUnit startingUnits = gc.startingMap(gc.planet()).getInitial_units();
        //find out if map is split for each worker on same island
        for (int i = 0; i < numWorkers; i++) {
            if (useful.contains(i)) {
                boolean spl = true;
                for (int a = 0; a < startingUnits.size(); a++) {
                    Unit unit = startingUnits.get(a);
                    if (unit.team() != myTeam) {
                        //enemy worker
                        MapLocation enemyLoc = unit.location().mapLocation();
                        if (gotoable[i][enemyLoc.getX()][enemyLoc.getY()]) {
                            spl = false;
                            break;
                        }
                    }
                }
                split[i] = spl;
                //System.out.println("-----------------------");
                //System.out.println(spl);
            }
        }
    }

    public static int hash(int x, int y) {
        return 69 * x + y;
    }

    public static void chooseTarget() {
        for (int i = 0; i < enemyLocation.length; i++) {
            if (useful.contains(i)) {
                if (enemyLocation[i] == null) {
                    enemyLocation[i] = chooseFarthestPoint(i);
                } else {
                    if (gc.senseNearbyUnitsByTeam(enemyLocation[i], 0, myTeam).size() > 0) {
                        timesReachedTarget++;
                        enemyLocation[i] = chooseFarthestPoint(i);
                    }
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
                for (int j = 0; j < numUnitsThisRound; j++) {
                    tempDist += distanceSq(i, a, unitLocations[j].getX(), unitLocations[j].getY());
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

    public static MapLocation chooseClosestPoint(int parent) {
        VecUnit startingUnits = gc.startingMap(gc.planet()).getInitial_units();
        double greatest = 9999999;
        int smallX = -1;
        int smallY = -1;
        for (int i = 0; i < gridX; i++) {
            for (int a = 0; a < gridY; a++) {
                if (!gotoable[parent][i][a]) {
                    continue;
                }
                double tempDist = 0;
                for (int j = 0; j < startingUnits.size(); j++) {
                    Unit tempUnit = startingUnits.get(j);
                    if (tempUnit.team() == myTeam) {
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
        //BFS the whole map
        bfs(point);
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

    public static boolean onMap(MapLocation test) {
        int x = test.getX();
        int y = test.getY();
        return x >= 0 && y >= 0 && x < Player.gridX && y < Player.gridY;
    }

    public static double distanceSq(int x1, int y1, int x2, int y2) {
        return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
    }

    public static int hash(MapLocation loc) {
        return 69 * loc.getX() + loc.getY();
    }

    public static boolean checkPassable(MapLocation test) {
        int x = test.getX();
        int y = test.getY();
        if (x >= gridX || y >= gridY || x < 0 || y < 0) {
            return false;
        }
        return passable[x][y];
    }

}