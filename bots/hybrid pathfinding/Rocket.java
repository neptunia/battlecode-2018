import bc.*;

public class Rocket {

    static Unit curUnit;
    static GameController gc;
    static Direction[] directions = Direction.values();

    public static void run(GameController gc, Unit curUnit) {

        Rocket.curUnit = curUnit;

        if (Player.firstTime) {
            Player.firstTime = false;
            //guesstimate enemy location
            if (Player.enemyLocation == null) {
                MapLocation temp = curUnit.location().mapLocation();
                Player.startingLocation = temp;
                Player.enemyLocation = new MapLocation(gc.planet(), Player.gridX - temp.getX(), Player.gridY - temp.getY());
            }
        }

        if (curUnit.location().isInSpace()) {
            //in space
            return;
        } else if (curUnit.location().isOnPlanet(Planet.Earth)) {
            //on earth, load units
            VecUnit adjacentUnits = gc.senseNearbyUnitsByTeam(curUnit.location().mapLocation(), 2, Player.myTeam);
            //TODO prioritize certain units over others
            for (int i = 0; i < adjacentUnits.size(); i++) {
                if (gc.canLoad(curUnit.id(), adjacentUnits.get(i).id())) {
                    gc.load(curUnit.id(), adjacentUnits.get(i).id());
                }
            }

            if (curUnit.structureMaxCapacity() == curUnit.structureGarrison().size()) {
                PlanetMap marsmap = gc.startingMap(Planet.Mars);
                marsmap.setPlanet(Planet.Mars);
                //garrison is full, launch
                //first, try starting coords on mars
                MapLocation marsStart = new MapLocation(Planet.Mars, Player.startingLocation.getX(), Player.startingLocation.getY());
                if (gc.canLaunchRocket(curUnit.id(), marsStart)) {
                    gc.launchRocket(curUnit.id(), marsStart);
                } else {
                    for (int i = 0; i < marsmap.getWidth(); i++) {
                        boolean stop = false;
                        for (int j = 0; j < marsmap.getHeight(); j++) {
                            MapLocation temp = new MapLocation(Planet.Mars, (int) ((Player.startingLocation.getX() + i) % marsmap.getWidth()), (int) ((Player.startingLocation.getY() + j) % marsmap.getWidth()));
                            if (gc.canLaunchRocket(curUnit.id(), temp)) {
                                gc.launchRocket(curUnit.id(), temp);
                                stop = true;
                                break;
                            }
                        }
                        if (stop) {
                            break;
                        }
                    }
                }
            }
        } else {
            //on mars, unload units
            for (int i = 0; i < directions.length; i++) {
                if (gc.canUnload(curUnit.id(), directions[i])) {
                    gc.unload(curUnit.id(), directions[i]);
                }
            }
        }

    }
}
