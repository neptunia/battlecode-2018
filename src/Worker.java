import bc.*;

public class Worker {

	static Unit curUnit;
	static GameController gc;
	static Direction[] directions = Direction.values();
	static int targetBlueprint = -1;

	public static void run(GameController gc, Unit curUnit) {

		Worker.gc = gc;
		Worker.curUnit = curUnit;

		if (gc.round() == 1) {
			//Initial replication
			for (int i = 0; i < directions.length; i++) {
				if (gc.canReplicate(curUnit.id(), directions[i])) {
					gc.replicate(curUnit.id(), directions[i]);
					break;
				}
			}
			//guesstimate enemy location
			if (Player.enemyLocation == null) {
				MapLocation temp = curUnit.location().mapLocation();
				Player.enemyLocation = new MapLocation(Planet.Earth, Player.gridX - temp.getX(), Player.gridY - temp.getY());
			}
		}

		//go to current blueprint working on and do it
		if (targetBlueprint != -1) {
			Unit toWorkOn = gc.unit(targetBlueprint);
			MapLocation blueprintLoc = toWorkOn.location().mapLocation();
			MapLocation curLoc = curUnit.location().mapLocation();
			if (distance(blueprintLoc.getX(), blueprintLoc.getY(), curLoc.getX(), curLoc.getY()) <= 2) {
				if (!buildBlueprint(targetBlueprint)) {
					System.out.println("SUM TING WONG :(");
				}
				if (toWorkOn.health() == toWorkOn.maxHealth()) {
					targetBlueprint = -1;
				}
			} else {
				move(blueprintLoc);
			}
		} else {
			buildFactory();
		}
		

		return;
	}

	public static double distance(int x1, int y1, int x2, int y2) {
		return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
	}

	/* try to work on any blueprints nearby
	public static void workOnBlueprint() {
		MapLocation curLoc = curUnit.location().mapLocation();
		VecUnit nearby = getNearby(curLoc, 2);
		for (int i = 0; i < nearby.size(); i++) {
			Unit toBuild = nearby.get(i);
			if (gc.canBuild(curUnit.id(), toBuild.id())) {
				gc.build(curUnit.id(), toBuild.id());
				return;
			}
		}
	}*/

	//build blueprint given structure unit id
	public static boolean buildBlueprint(int id) {
		if (gc.canBuild(curUnit.id(), id)) {
			gc.build(curUnit.id(), id);
			return true;
		}
		return false;
	}

	//senses nearby units and updates RobotPlayer.map
	public static VecUnit getNearby(MapLocation maploc, int radius) {
		VecUnit nearby = gc.senseNearbyUnits(maploc, radius);
		for (int i = 0; i < nearby.size(); i++) {
			Unit unit = nearby.get(i);
			MapLocation temp = unit.location().mapLocation();
			Player.map[temp.getX()][temp.getY()] = unit;
		}
		return nearby;
	}

	//builds a factory in an open space around worker
	public static void buildFactory() {
		for (int i = 0; i < directions.length - 2; i++) {
			if (gc.canBlueprint(curUnit.id(), UnitType.Factory, directions[i])) {
				gc.blueprint(curUnit.id(), UnitType.Factory, directions[i]);
				targetBlueprint = gc.senseUnitAtLocation(curUnit.location().mapLocation().add(directions[i])).id();
				break;
			}
		}
	}

	//move towards target location
	public static void move(MapLocation target) {
		//TODO implement pathfinding
		for (int i = 0; i < directions.length; i++) {
			if (gc.isMoveReady(curUnit.id()) && gc.canMove(curUnit.id(), directions[i])) {
				gc.moveRobot(curUnit.id(), directions[i]);
				return;
			}
		}
	}

}