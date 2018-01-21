import bc.*;
import java.util.*;

import java.text.NumberFormat;

public class Player {

    static boolean[][] passable;
    static Unit[][] map;
    static int gridX, gridY;
    static MapLocation enemyLocation = null;
    static MapLocation startingLocation = null;
    static Direction[] directions = Direction.values();
    static GameController gc;
    static PlanetMap planetMap;
    static Team myTeam, enemyTeam;
    static long prevIncome;
    static long currentIncome;
    static boolean[][] gotoable;
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
                if (gc.planet() == Planet.Mars) {
                    if (gc.asteroidPattern().hasAsteroid(gc.round())) {
                        AsteroidStrike aster = gc.asteroidPattern().asteroid(gc.round());
                        if (gotoable[aster.getLocation().getX()][aster.getLocation().getY()]) {
                            Worker.karbonites[Worker.numKarbsCounter] = aster.getLocation();
                            if (!Worker.karbonitesLeft) {
                                Worker.karbonitesLeft = true;
                            }
                            Worker.karboniteIndex.put(hash(aster.getLocation()), Worker.numKarbsCounter);
                            Worker.numKarbsCounter++;
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
                        System.out.println("unit died");
                        e.printStackTrace();
                    }
                }
                try {
                    chooseTarget();
                } catch (Exception e) {
                    System.out.println("choose target borked");
                    e.printStackTrace();
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
                    System.out.println("time borked");
                    e.printStackTrace();
                }
                gc.nextTurn();
            }
        } catch (Exception e) {
            e.printStackTrace();
            //System.out.println("even worse");
        }
    }

    public static void initialize() {
        int width = (int) planetMap.getWidth();
        int height = (int) planetMap.getHeight();
        pathDistances = new int[width * 69 + height + 1][width][height];
        for (int i = 0; i < width * 69 + height + 1; i++) {
            for (int a = 0; a < width; a++) {
                for (int j = 0; j < height; j++) {
                    pathDistances[i][a][j] = -1;
                }
            }
        }
        gridX = width;
        gridY = height;
        map = new Unit[width][height];
        passable = new boolean[width][height];
        Worker.karbonites = new MapLocation[2500];

        gotoable = new boolean[width][height];

        VecUnit myUnits = gc.myUnits();

        //
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
        LinkedList<MapLocation> queue = new LinkedList<MapLocation>();
        HashSet<Integer> visited = new HashSet<Integer>();

        for (int i = 0; i < myUnits.size(); i++) {
            Unit tempUnit = myUnits.get(i);
            if ((tempUnit.unitType() == UnitType.Worker && !tempUnit.location().isInGarrison()) || tempUnit.unitType() == UnitType.Rocket) {
                MapLocation curLoc = tempUnit.location().mapLocation();
                queue.add(curLoc);
                gotoable[curLoc.getX()][curLoc.getY()] = true;
                visited.add(hash(curLoc));
            }
        }

        if (queue.isEmpty()) {
            gotoableEmpty = true;
        } else {
            gotoableEmpty = false;
        }

        int count = 0;
        //find gotoable squares
        //TODO: workers on different islands
        while (!queue.isEmpty()) {
            MapLocation current = queue.poll();
            for (int i = 0; i < directions.length; i++) {
                MapLocation toCheck = current.add(directions[i]);
                int tempHash = hash(toCheck);
                if (!visited.contains(tempHash) && checkPassable(toCheck)) {
                    count++;
                    visited.add(tempHash);
                    gotoable[toCheck.getX()][toCheck.getY()] = true;
                    queue.add(toCheck);
                }
            }
        }

        //boolean[][] karbs = new boolean[width][height];

        //find karbonites on map
        for (int i = 0; i < width; i++) {
            for (int a = 0; a < height; a++) {
                MapLocation test = new MapLocation(gc.planet(), i, a);
                if (gotoable[i][a] && planetMap.initialKarboniteAt(test) > 0) {
                    //karbs[i][a] = true;
                    Worker.karboniteIndex.put(hash(test), Worker.numKarbsCounter);
                    Worker.karbonites[Worker.numKarbsCounter] = test;
                    Worker.numKarbsCounter++;
                }
            }
        }
        if (Worker.numKarbsCounter > 0) {
            Worker.karbonitesLeft = true;
        } else {
            Worker.karbonitesLeft = false;
        }

        startingLocation = chooseClosestPoint();

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
    }

    public static int hash(int x, int y) {
        return 69 * x + y;
    }

    public static void chooseTarget() {
        if (enemyLocation == null) {
            enemyLocation = chooseFarthestPoint();
        } else {
            if (gc.senseNearbyUnitsByTeam(enemyLocation, 0, myTeam).size() > 0) {
                timesReachedTarget++;
                enemyLocation = chooseFarthestPoint();
            }
        }
    }

    public static MapLocation chooseFarthestPoint() {
        double greatest = -1;
        int smallX = 0;
        int smallY = 0;
        for (int i = 0; i < gridX; i++) {
            for (int a = 0; a < gridY; a++) {
                if (!gotoable[i][a]) {
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

    public static MapLocation chooseClosestPoint() {
        double greatest = 9999999;
        int smallX = 0;
        int smallY = 0;
        for (int i = 0; i < gridX; i++) {
            for (int a = 0; a < gridY; a++) {
                if (!gotoable[i][a]) {
                    continue;
                }
                double tempDist = 0;
                for (int j = 0; j < numUnitsThisRound; j++) {
                    tempDist += distanceSq(i, a, unitLocations[j].getX(), unitLocations[j].getY());
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
            }
            if (distance > check + 1) {
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