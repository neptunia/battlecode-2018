import bc.*;

public class Player {

    static boolean[][] passable;
    static Unit[][] map;
    static int gridX, gridY;
    static MapLocation enemyLocation = null;
    static MapLocation startingLocation = null;
    static GameController gc;
    static PlanetMap planetMap;
	
	public static void main(String args[]) {
        try {
            GameController gc = new GameController();

            Mage.gc = gc;
            Rocket.gc = gc;
            Healer.gc = gc;
            Factory.gc = gc;
            Ranger.gc = gc;
            Knight.gc = gc;
            Worker.gc = gc;
            Player.gc = gc;
            Player.planetMap = gc.startingMap(Planet.Earth);

            initialize();

            while (true) {
                long startTime = System.currentTimeMillis();
                try {

                    long currentRound = gc.round();
                    VecUnit myUnits = gc.myUnits();
                    long numberOfUnits = myUnits.size();

                    //iterate through units
                    // might need to fix this later; what happens if I create a new unit in the middle of this loop?
                    for (int i = 0; i < numberOfUnits; i++) {
                        Unit curUnit = myUnits.get(i);
                        //perform unit task based on unit type
                        switch (curUnit.unitType()) {
                            case Factory:
                                try {
                                    Factory.run(gc, curUnit);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    System.out.println("Factory ded");
                                }
                                break;
                            case Healer:
                                try {
                                    Healer.run(gc, curUnit);
                                } catch (Exception e) {
                                    e.printStackTrace();;
                                    System.out.println("Healer ded");
                                }
                                break;
                            case Knight:
                                try {
                                    Knight.run(gc, curUnit);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    System.out.println("knight ded");
                                }
                                break;
                            case Mage:
                                try {
                                    Mage.run(gc, curUnit);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    System.out.println("mage ded");
                                }
                                break;
                            case Ranger:
                            	try {
                            	    Ranger.run(gc, curUnit);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    System.out.println("mage ded");
                                }
                                break;
                            case Rocket:
                                try {
                                    Rocket.run(gc, curUnit);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    System.out.println("mage ded");
                                }
                                break;
                            case Worker:
                                try {
                                    Worker.run(gc, curUnit);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    System.out.println("worker ded");
                                }
                        }
                    }

                    //do research
                    try {
                        gc.queueResearch(UnitType.Ranger);
                        gc.queueResearch(UnitType.Rocket);
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println("research ded");
                    }

                    //end turn
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("fuck me");
                }
                long endTime = System.currentTimeMillis();
                System.out.println("Time");
                System.out.println(endTime - startTime);
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
        for (int i = 0; i < width; i++) {
            for (int a = 0; a < height; a++) {
                MapLocation temp = new MapLocation(Planet.Earth, i, a);
                if (planetMap.isPassableTerrainAt(temp) == 1) {
                    passable[i][a] = true;
                } else {
                    passable[i][a] = false;
                }
            }
        }
    }

}