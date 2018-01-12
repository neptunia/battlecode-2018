import bc.*;
import java.util.*;

public class Knight {

	static Unit curUnit;
	static GameController gc;
	static Direction[] directions = Direction.values();
	static HashMap<Integer, HashSet<Integer>> visited = new HashMap<Integer, HashSet<Integer>>();
	static HashMap<Integer, MapLocation> targets = new HashMap<Integer, MapLocation>();

	public static void run(GameController gc, Unit curUnit) {

		Knight.curUnit = curUnit;
		Knight.gc = gc;

		if (curUnit.location().isInGarrison()) {
			return;
		}
		MapLocation tgt = getTarget();
		if (tgt != null) {
			if (CheckWithinRange()) {
				// if i win, go in
				if (canMove())
					move(tgt);
			} else {
				// retreat
				setTarget(curUnit, Player.startingLocation);

			}
		} else {
			if (CheckWithinRange()) {
				// target???
				//setTarget(curunit, Player.enemyLocation());
				targetEnemy();
				if (canMove())
					move(tgt);
			} else {
				// explore
				setTarget(curUnit, Player.enemyLocation);

			}
		}


		//attack enemies that are near you
		if (canAttack()) {
			attackNearbyEnemies();
		}

		if (getTarget() == curUnit.location().mapLocation()) {
			setTarget(curUnit, null);
		}
	}

	public static void setTarget(Unit unit, MapLocation x) {
		targets.put(unit.id(), x);
	}

	public static MapLocation getTarget() {
		if (!targets.containsKey(curUnit.id())) {
			targets.put(curUnit.id(), null);
			return null;
		} else {
			return targets.get(curUnit.id());
		}
	}

	public static void targetEnemy() {
		VecUnit nearbyUnits = getNearby(curUnit.location().mapLocation(), (int) curUnit.visionRange());
		MapLocation tl = Player.startingLocation;
		for (int i = 0; i < nearbyUnits.size(); i++) {
			Unit unit = nearbyUnits.get(i);
			if (unit.team() != gc.team()) {
				// enemy
				tl = unit.location().mapLocation();
				break;
				
			}
		}
		setTarget(curUnit, tl);
		for (int i = 0; i < nearbyUnits.size(); i++) {
			Unit unit = nearbyUnits.get(i);
			if (unit.team() == gc.team()) {
				// enemy
				setTarget(unit, tl);
			}
		}
	}


	public static boolean CheckWithinRange() {
		VecUnit nearbyUnits = getNearby(curUnit.location().mapLocation(), (int) curUnit.visionRange());
		
		int friendlyArmyCount = 1;
		int enemyArmyCount = 0;
		int enemyCount = 0;


		for (int i = 0; i < nearbyUnits.size(); i++) {
			Unit unit = nearbyUnits.get(i);
			if (unit.team() != gc.team()) {
				// enemy
				if (unit.unitType() == UnitType.Knight || unit.unitType() == UnitType.Mage || unit.unitType() == UnitType.Healer || unit.unitType() == UnitType.Ranger) {
					enemyArmyCount += 1;
				}
				enemyCount += 1;
			} else {
				if (unit.unitType() == UnitType.Knight || unit.unitType() == UnitType.Mage || unit.unitType() == UnitType.Healer || unit.unitType() == UnitType.Ranger) {
					friendlyArmyCount += 1;
				}
			}
		}
		return (friendlyArmyCount >= enemyArmyCount && enemyCount > 0);
	}

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