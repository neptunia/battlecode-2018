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
    static UnitType[][] units;
    static boolean gotoableEmpty;
    static MapLocation[] unitLocations;
    static int numUnitsThisRound;
    static int timesReachedTarget = 0;
    static boolean sawEnemy = false;
    //static boolean splitMap = false;
    static HashMap<Integer, Integer> paths = new HashMap<Integer, Integer>();

	
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
            //iterate through units
            // might need to fix this later; what happens if I create a new unit in the middle of this loop?
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
            gc.queueResearch(UnitType.Ranger);
            gc.queueResearch(UnitType.Rocket);
            gc.queueResearch(UnitType.Ranger);
            gc.queueResearch(UnitType.Ranger);

            long startTime = 10000;
            long endTime = 10000;

            unitLocations = new MapLocation[5000];
            units = new UnitType[gridX][gridY];

            while (true) {
                if (gc.planet() == Planet.Earth && gc.round() >= 750) {
                    gc.nextTurn();
                }
                sawEnemy = false;

                currentIncome = 10 - Math.max(gc.karbonite() / 40, 0);
                long currentRound = gc.round();
                VecUnit myUnits = gc.myUnits();
                long numberOfUnits = myUnits.size();
                //record locations of all my units
                numUnitsThisRound = 0;
                //iterate through units
                // might need to fix this later; what happens if I create a new unit in the middle of this loop?
                for (int i = 0; i < numberOfUnits; i++) {
                    Unit curUnit = myUnits.get(i);
                    try {
                        MapLocation curLoc = curUnit.location().mapLocation();
                        units[curLoc.getX()][curLoc.getY()] = curUnit.unitType();
                        unitLocations[numUnitsThisRound] = curLoc;
                        numUnitsThisRound++;
                    } catch (Exception e) {};
                }
                for (int i = 0; i < numberOfUnits; i++) {
                    Unit curUnit = myUnits.get(i);
                    
                    //perform unit task based on unit type
                    try {
                        switch (curUnit.unitType()) {
                            case Factory:
                                Factory.run(gc, curUnit);
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
                                Worker.run(gc, curUnit);
                        }
                    } catch (Exception e) {System.out.println("unit died");}
                }

                chooseTarget();



                endTime = gc.getTimeLeftMs();
                total += startTime - endTime;
                System.out.println("Time: " + Long.toString(startTime - endTime));
                System.out.println("Average: " + Float.toString(total / gc.round()));
                System.out.println("Time Left: " + Long.toString(gc.getTimeLeftMs()));
                prevIncome = currentIncome;
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
                System.out.println("Round: " + Long.toString(gc.round()));
                startTime = endTime;
                if (sawEnemy) {
                    timesReachedTarget = 0;
                }
                gc.nextTurn();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("even worse");
        }
    }

    public static void initialize() {
        int width = (int) planetMap.getWidth();
        int height = (int) planetMap.getHeight();
        gridX = width;
        gridY = height;
        map = new Unit[width][height];
        passable = new boolean[width][height];
        Worker.karbonites = new MapLocation[2500];

        gotoable = new boolean[width][height];

        VecUnit myUnits = gc.myUnits();

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

        startingLocation = chooseClosestPoint();
        //System.out.println("Start loc: " + Integer.toString(startingLocation.getX()) + " " + Integer.toString(startingLocation.getY()));

        //TODO make karbonite more efficient
        int counter = 0;
        for (int i = 0; i < width; i++) {
            for (int a = 0; a < height; a++) {
                MapLocation temp = new MapLocation(gc.planet(), i, a);
                if (planetMap.isPassableTerrainAt(temp) == 1) {
                    passable[i][a] = true;
                } else {
                    passable[i][a] = false;
                }
                if (planetMap.initialKarboniteAt(temp) > 0 && gotoable[i][a]) {
                    Worker.karbonites[Worker.numKarbsCounter] = temp;
                    Worker.numKarbsCounter++;
                }
            }
        }
    }

    public static void chooseTarget() {
        if (enemyLocation == null) {
            enemyLocation = chooseFarthestPoint();
        } else {
            if (gc.senseNearbyUnitsByTeam(enemyLocation, 2, myTeam).size() > 0) {
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
        return new MapLocation(gc.planet(), smallX, smallY);
    }

    public static MapLocation chooseClosestPoint() {
        double smallest = 99999999.0;
        int smallX = 0;
        int smallY = 0;
        for (int i = 0; i < gridX; i++) {
            for (int a = 0; a < gridY; a++) {
                if (!gotoable[i][a]) {
                    continue;
                }
                double tempDist = 0;
                for (int j = 0; j < numUnitsThisRound; j++) {
                    System.out.println(unitLocations[j].getX());
                    System.out.println(unitLocations[j].getY());
                    tempDist += distanceSq(i, a, unitLocations[j].getX(), unitLocations[j].getY());
                }
                if (tempDist < smallest) {
                    smallest = tempDist;
                    smallX = i;
                    smallY = a;
                }
            }
        }
        return new MapLocation(gc.planet(), smallX, smallY);
    }

    public static double distanceSq(int x1, int y1, int x2, int y2) {
        return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
    }

    public static int hash(MapLocation loc) {
        return 69 * loc.getX() + loc.getY();
    }

    public static boolean checkPassable(MapLocation test) {
        if (test.getX() >= gridX || test.getY() >= gridY || test.getX() < 0 || test.getY() < 0) {
            return false;
        }
        return planetMap.isPassableTerrainAt(test) == 1;
    }

}