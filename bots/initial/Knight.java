import bc.*;
import java.util.*;

public class Knight {

	static Unit curUnit;
	static GameController gc;
	static Direction[] directions = Direction.values();
	static HashMap<Integer, HashSet<Integer>> visited = new HashMap<Integer, HashSet<Integer>>();
	static HashMap<Integer, Integer> targets = new HashMap<Integer, Integer>();

	public static void run(GameController gc, Unit curUnit) {

		Knight.curUnit = curUnit;
		Knight.gc = gc;

		if (curUnit.location().isInGarrison()) {
			return;
		}

		Pair nearbyInfo = null;

		if (!targets.containsKey(curUnit.id())) {
			//try to find an enemy target in range
			nearbyInfo = findTarget();
		} else if (gc.unit(targets.get(curUnit.id())).health() == 0) {
			//already killed this unit, remove
			targets.remove(curUnit.id());
			nearbyInfo = findTarget();
		}

		//didnt sense number of allies and enemies yet
		if (nearbyInfo == null) {
			nearbyInfo = countNearby();
		}

		//if this knight has a target
		if (canMove()) {
			if (targets.containsKey(curUnit.id())) {
				//move towards them
				if (nearbyInfo.friendly > nearbyInfo.enemy) {
					move(gc.unit(targets.get(curUnit.id())).location().mapLocation());
				} else {
					//enemy too strank
					move(Player.startingLocation);
				}
			} else {
				//explore
				move(Player.enemyLocation);
			}
		}

		//attack first enemy
		//TODO should change later
		if (canAttack()) {
			attackNearbyEnemies();
		}

	}

	public static Pair findTarget() {
		Pair ret = new Pair();
		VecUnit nearby = gc.senseNearbyUnits(curUnit.location().mapLocation(), curUnit.visionRange());
		int tempTarget = null;
		int smallest = 9999999;
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
		if (tempTarget != null) {
			for (int i = 0; i < nearby.size(); i++) {
				Unit temp3 = nearby.get(i);
				if (temp3.team() == gc.team()) {
					ret.friendly++;
					targets.put(temp3.id(), tempTarget);
				}
			}
		}
		return ret;
	}

	public static Pair countNearby() {
		Pair ret = new Pair();
		VecUnit nearby = gc.senseNearbyUnits(curUnit.location().mapLocation(), curUnit.visionRange());
		for (int i = 0; i < nearby.size(); i++) {
			Unit temp3 = nearby.get(i);
			if (temp3.team() != gc.team()) {
				ret.enemy++;
			}
		}
		for (int i = 0; i < nearby.size(); i++) {
			Unit temp3 = nearby.get(i);
			if (temp3.team() == gc.team()) {
				ret.friendly++;
			}
		}
		return ret;
	}

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

	public static void attackNearbyEnemies() {
		VecUnit nearbyUnits = getNearby(curUnit.location().mapLocation(), (int) curUnit.attackRange());
		for (int i = 0; i < nearbyUnits.size(); i++) {
			Unit unit = nearbyUnits.get(i);
			//if can attack this enemy unit
			if (unit.team() != gc.team() && gc.canAttack(curUnit.id(), unit.id())) {
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
			visited.remove(curUnit.id());
			return false;
		}
		gc.moveRobot(curUnit.id(), d);
		return true;
	}

	public static int hash(int x, int y) {
		return 69 * x + y;
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