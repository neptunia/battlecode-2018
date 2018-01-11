import bc.*;

public class Knight {

	static Unit curUnit;
	static GameController gc;
	static Direction[] directions = Direction.values();
	static HashMap<Integer, String> visited = new HashMap<Integer, String>();

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
	}

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
		double smallest = 999999;
		Direction d = null;
		MapLocation curLoc = curUnit.location().mapLocation();
		int hash = hash(curLoc.getX(), curLoc.getY());
		if (!visited.containsKey(curUnit.id())) {
			visited.put(curUnit.id(), Integer.toString(hash) + ",");
		} else {
			visited.put(curUnit.id(), visited.get(curUnit.id()) + Integer.toString(hash) + ",");
		}
		for (int i = 0; i < directions.length; i++) {
			MapLocation newSquare = curLoc.add(directions[i]);
			if (!visited.get(curUnit.id()).contains(Integer.toString(hash(newSquare.getX(), newSquare.getY()))) && gc.canMove(curUnit.id(), directions[i]) && distance(newSquare, target) < smallest) {
				smallest = distance(newSquare, target);
				d = directions[i];
			}
		}
		if (d == null) {
			//can't move
			return false;
		}
		gc.moveRobot(curUnit.id(), d);
		return true;
	}

	//get target unit should be pathing towards
	public static MapLocation getTarget() {
		return Player.enemyLocation;
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