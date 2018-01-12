import bc.*;
import java.util.*;

public class Worker {

	static Unit curUnit;
	static GameController gc;
	static Direction[] directions = Direction.values();
	static HashMap<Integer, HashSet<Integer>> visited = new HashMap<Integer, HashSet<Integer>>();
	//target blueprint to work on for each worker
	static HashMap<Integer, Integer> target = new HashMap<Integer, Integer>();
	//bugpathing storing previous square (unit id, hash of location)
	static HashMap<Integer, Integer> prevLocation = new HashMap<Integer, Integer>();

	public static void run(GameController gc, Unit curUnit) {

		//Note: with bugmove, whenever a unit changes target, then prevLocation should remove their unit id from its keys

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
		//remove target if factory already died
		try {
			gc.unit(target.get(curUnit.id()));
		} catch (Exception e) {
			target.remove(curUnit.id());
		}

		//if i do have a target blueprint
		if (target.containsKey(curUnit.id())) {
			int targetBlueprint = target.get(curUnit.id());
			Unit toWorkOn = gc.unit(targetBlueprint);
			//already done working on
			if (toWorkOn.health() == toWorkOn.maxHealth()) {
				target.remove(curUnit.id());
				buildFactory();
			} else {
				//goto it and build it
				MapLocation blueprintLoc = toWorkOn.location().mapLocation();
				MapLocation curLoc = curUnit.location().mapLocation();
				if (distance(blueprintLoc, curLoc) <= 2) {
					//next to it, i can work on it
					if (!buildBlueprint(targetBlueprint)) {
						System.out.println("SUM TING WONG :(");
					}
				} else {
					//move towards it
					if (canMove()) {
						move(blueprintLoc);
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
	//TODO improve factory building scheme
	public static void buildFactory() {
		for (int i = 0; i < directions.length - 2; i++) {
			if (gc.canBlueprint(curUnit.id(), UnitType.Factory, directions[i])) {
				gc.blueprint(curUnit.id(), UnitType.Factory, directions[i]);
				int targetBlueprint = gc.senseUnitAtLocation(curUnit.location().mapLocation().add(directions[i])).id();
				//tell all nearby workers to go work on it
				//TODO maybe bfs within n range for workers to work on the factory
				VecUnit nearby = gc.senseNearbyUnits(curUnit.location().mapLocation(), 4);
				for (int a = 0; a < nearby.size(); a++) {
					Unit temp = nearby.get(a);
					if (temp.team() == gc.team() && temp.unitType() == UnitType.Worker) {
						//if changing target of a unit
						if (prevLocation.containsKey(temp.id()) && prevLocation.get(temp.id()) != targetBlueprint) {
							prevLocation.remove(temp.id());
						}
						target.put(temp.id(), targetBlueprint);
					}
				}
				break;
			}
		}
	}

	public static int hash(int x, int y) {
		return 69 * x + y;
	}

	public static int hash(MapLocation loc) {
		return 69 * loc.getX() + loc.getY();
	}

	public static void move(MapLocation target) {
		//finding square directly going towards path
		//TODO (there's probably some math thing that's better)
		int smallest = 9999999;
		Direction direct = null;
		for (int i = 0; i < directions.length; i++) {
			MapLocation newSquare = curUnit.location().mapLocation().add(directions[i]);
			int temp = distance(target, newSquare);
			if (temp < smallest) {
				smallest = temp;
				direct = directions[i];
			}
		}
		//if i can move directly
		if (direct != null) {
			if (gc.canMove(curUnit.id(), direct)) {
				prevLocation.remove(curUnit.id());
				gc.moveRobot(curUnit.id(), direct);
				return;
			} else {
				System.out.println("Blocked by ally :(");
			}
		}

		//follow obstacle
		if (!prevLocation.containsKey(curUnit.id())) {
			//choose a direction of obstacle to go in
			prevLocation.put(curUnit.id(), hash(curUnit.location().mapLocation()));
			//find obstacle border closest to target
			smallest = 99999999;
			MapLocation wall = null;
			Direction toMove = null;
			for (int i = 0; i < directions.length; i++) {
				MapLocation test = curUnit.location().mapLocation().add(directions[i]);
				//TODO check if isPassable returns true or false for allies
				if (checkAdjacentToObstacle(test) && distance(test, target) < smallest) {
					smallest = distance(test, target);
					toMove = directions[i];
					wall = test;
				}
			}
			//try to move there
			if (gc.canMove(curUnit.id(), toMove)) {
				gc.moveRobot(curUnit.id(), toMove);
			} else {
				System.out.println("Blocked by ally 2 :(");
			}
		} else {
			//already following obstacle
			//find wall that's not equal to prevLocation
			MapLocation wall = null;
			int previousHash = prevLocation.get(curUnit.id());
			Direction toMove = null;
			for (int i = 0; i < directions.length; i++) {
				MapLocation test = curUnit.location().mapLocation().add(directions[i]);
				//TODO check if isPassable returns true or false for allies
				if (checkAdjacentToObstacle(test) && hash(test) != previousHash) {
					wall = test;
					toMove = directions[i];
				}
			}
			if (wall == null) {
				System.out.println("Bug move is borked");
			} else {
				//try moving there
				if (gc.canMove(curUnit.id(), toMove)) {
					gc.moveRobot(curUnit.id(), toMove);
				} else {
					System.out.println("Blocked by ally 3 :(");
				}
			}
		}
	}

	//check if a square is the border of an obstacle (aka if an obstacle is on left right up or down of it)
	public static boolean checkAdjacentToObstacle(MapLocation test) {
		Direction[] temp = {Direction.North, Direction.South, Direction.East, Direction.South};
		for (int i = 0; i < temp.length; i++) {
			if (Player.planetMap.isPassableTerrainAt(test) == 0 || gc.senseUnitAtLocation(test).team() == gc.team()) {
				return true;
			}
		}
		return false;
	}

	//move towards target location
	/*public static boolean move(MapLocation target) {
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
	}*/

	public static boolean canMove() {
		return curUnit.movementHeat() < 10;
	}

}