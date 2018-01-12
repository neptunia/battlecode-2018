import bc.*;
import java.util.*;

public class Knight {

	static Unit curUnit;
	static GameController gc;
	static Direction[] directions = Direction.values();
	static HashMap<Integer, HashSet<Integer>> visited = new HashMap<Integer, HashSet<Integer>>();
	static HashMap<Integer, Integer> targets = new HashMap<Integer, Integer>();
	static HashMap<Integer, Integer> prevLocation = new HashMap<Integer, Integer>();

	public static void run(GameController gc, Unit curUnit) {

		Knight.curUnit = curUnit;
		Knight.gc = gc;

		if (curUnit.location().isInGarrison()) {
			return;
		}

		Pair nearbyInfo = null;

		//if i dont have a target right now
		if (!targets.containsKey(curUnit.id())) {
			//try to find an enemy target in range
			nearbyInfo = findTarget();
		} else {
			//remove unit if it is already killed
			try {
				gc.unit(targets.get(curUnit.id()));
			} catch (Exception e) {
				//already killed this unit, or it ran away, remove
				targets.remove(curUnit.id());
				nearbyInfo = findTarget();
			}
			
		}

		//didnt sense number of allies and enemies yet
		if (nearbyInfo == null) {
			nearbyInfo = countNearby();
		}

		//if this knight has a target
		if (targets.containsKey(curUnit.id())) {
			//move towards them if my army is stronger
			if (nearbyInfo.friendly >= nearbyInfo.enemy) {
				if (canMove()) {
					move(gc.unit(targets.get(curUnit.id())).location().mapLocation());
				}
				if (canAttack()) {
					tryAttack(targets.get(curUnit.id()));
				}
			} else {
				//otherwise run away!!!
				if (canMove()) {
					move(Player.startingLocation);
				}
			}
		} else {
			//otherwise explore
			if (canMove()) {
				move(Player.enemyLocation);
			}
		}

		//if i couldnt attack my target, attack anyone around me i can attack
		if (canAttack()) {
			attackNearbyEnemies();
		}

	}

	//try to attack a target unit
	public static void tryAttack(int id) {
		if (gc.canAttack(curUnit.id(), id)) {
			gc.attack(curUnit.id(), id);
		}
	}

	public static Pair findTarget() {
		Pair ret = new Pair();
		VecUnit nearby = gc.senseNearbyUnits(curUnit.location().mapLocation(), curUnit.visionRange());
		int tempTarget = -1;
		int smallest = 9999999;
		//find nearest target
		for (int i = 0; i < nearby.size(); i++) {
			Unit temp3 = nearby.get(i);
			if (temp3.team() != gc.team()) {
				ret.enemy++;
				MapLocation temp2 = temp3.location().mapLocation();
				int temp = distance(curUnit.location().mapLocation(), temp2);
				if (temp < smallest) {
					smallest = temp;
					tempTarget = temp3.id();
				}
			}
		}
		//if found a target, set that as target for units around me
		if (smallest != 9999999) {
			for (int i = 0; i < nearby.size(); i++) {
				Unit temp3 = nearby.get(i);
				if (temp3.team() == gc.team()) {
					ret.friendly++;
					if (prevLocation.containsKey(temp3.id()) && prevLocation.get(temp3.id()) != tempTarget) {
						prevLocation.remove(temp3.id());
					}
					targets.put(temp3.id(), tempTarget);
				}
			}
		}
		return ret;
	}

	//count number of nearby enemies and allies
	public static Pair countNearby() {
		Pair ret = new Pair();
		VecUnit nearby = gc.senseNearbyUnits(curUnit.location().mapLocation(), curUnit.visionRange());
		for (int i = 0; i < nearby.size(); i++) {
			Unit temp3 = nearby.get(i);
			if (temp3.team() != gc.team()) {
				ret.enemy++;
			} else {
				ret.friendly++;
			}
		}
		return ret;
	}

	//struct to store two ints lol
	public static class Pair {
		int friendly;
		int enemy;
		Pair() {
			friendly = 0;
			enemy = 0;
		}
	}

	//TODO optimization if i want to do it
	//public static class mutableTarget

	/*
	public static void run(GameController gc, Unit curUnit) {

		Knight.curUnit = curUnit;
		Knight.gc = gc;

		if (curUnit.location().isInGarrison()) {
			return;
		}

		//attack enemies that are near you
		if (canAttack()) {
			attackNearbyEnemies();
		}

		if (canMove()) {
			move(getTarget());
		}
	}*/

	public static boolean canAttack() {
		return curUnit.attackHeat() < 10;
	}

	//attack nearest enemy found
	public static void attackNearbyEnemies() {
		VecUnit nearbyUnits = getNearby(curUnit.location().mapLocation(), (int) curUnit.attackRange());
		for (int i = 0; i < nearbyUnits.size(); i++) {
			Unit unit = nearbyUnits.get(i);
			//if can attack this enemy unit
			if (unit.team() != gc.team() && gc.isAttackReady(curUnit.id()) && gc.canAttack(curUnit.id(), unit.id())) {
				gc.attack(curUnit.id(), unit.id());
				return;
			}
		}
	}

	public static boolean canMove() {
		return curUnit.movementHeat() < 10;
	}

	//pathing
	//move towards target location
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
				if (checkPassable(test) && checkAdjacentToObstacle(test) && distance(test, target) < smallest) {
					smallest = distance(test, target);
					toMove = directions[i];
					wall = test;
				}
			}
			if (toMove == null) {
				//can't move
				return;
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
				if (checkPassable(test) && checkAdjacentToObstacle(test) && hash(test) != previousHash) {
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
			if (!checkPassable(test.add(temp[i]))) {
				return true;
			}
		}
		return false;
	}

	public static boolean checkPassable(MapLocation test) {
		if (test.getX() >= Player.gridX || test.getY() >= Player.gridY || test.getX() < 0 || test.getY() < 0) {
			return false;
		}
		boolean allyThere = true;
		try {
			gc.senseUnitAtLocation(test);
		} catch (Exception e) {
			allyThere = false;
		}
		return Player.planetMap.isPassableTerrainAt(test) == 1 && !allyThere;
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

	//senses nearby units and updates RobotPlayer.map with detected units
	public static VecUnit getNearby(MapLocation maploc, int radius) {
		VecUnit nearby = gc.senseNearbyUnits(maploc, radius);
		for (int i = 0; i < nearby.size(); i++) {
			Unit unit = nearby.get(i);
			MapLocation temp = unit.location().mapLocation();
			Player.map[temp.getX()][temp.getY()] = unit;
		}
		return nearby;
	}

}