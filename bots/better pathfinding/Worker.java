import bc.*;
import java.util.*;

public class Worker {

	static Unit curUnit;
	static GameController gc;
	static Direction[] directions = Direction.values();
	static int targetBlueprint = -1;
	static HashMap<Integer, HashSet<Integer>> visited = new HashMap<Integer, HashSet<Integer>>();

	public static void run(GameController gc, Unit curUnit) {

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
				Player.startingLocation = temp;
				Player.enemyLocation = new MapLocation(Planet.Earth, Player.gridX - temp.getX(), Player.gridY - temp.getY());
			}
		}

		//go to current blueprint working on and do it
		//TODO iff fairly close to it already
		if (targetBlueprint != -1) {
			Unit toWorkOn = gc.unit(targetBlueprint);
			if (toWorkOn.health() == toWorkOn.maxHealth()) {
				targetBlueprint = -1;
				buildFactory();
			} else {
				MapLocation blueprintLoc = toWorkOn.location().mapLocation();
				MapLocation curLoc = curUnit.location().mapLocation();
				if (distance(blueprintLoc, curLoc) <= 2) {
					if (!buildBlueprint(targetBlueprint)) {
						System.out.println("SUM TING WONG :(");
					}
				} else {
					if (gc.isMoveReady(curUnit.id())) {
						if (!move(blueprintLoc)) {
							System.out.println("CANT MOVE");
						}
					}
				}
			}
		} else {
			buildFactory();
		}
		

		return;
	}

	public static int distance(MapLocation first, MapLocation second) {
		int x1 = first.getX(), y1 = first.getY(), x2 = second.getX(), y2 = second.getY();
		return (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2);
	}

	//build blueprint given structure unit id
	public static boolean buildBlueprint(int id) {
		if (gc.canBuild(curUnit.id(), id)) {
			gc.build(curUnit.id(), id);
			return true;
		}
		return false;
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

	public static int hash(int x, int y) {
		return 69 * x + y;
	}

	//move towards target location
	public static boolean move(MapLocation target) {
		//TODO implement pathfinding
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
			if (!visited.get(curUnit.id()).contains(hash(newSquare.getX(), newSquare.getY())) && gc.canMove(curUnit.id(), directions[i]) && distance(newSquare, target) < smallest) {
				smallest = distance(newSquare, target);
				d = directions[i];
			}
		}
		if (d == null) {
			//can't move
			//check around unit for friendly units and look at their target to decide whether to find another path or wait for them
			visited.remove(curUnit.id());
			return false;
		}
		gc.moveRobot(curUnit.id(), d);
		return true;
	}

}