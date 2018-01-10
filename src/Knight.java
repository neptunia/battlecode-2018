import java.util.*;
import java.io.*;
import bc.*;

public class Knight {

	static Unit curUnit;
	static GameController gc;
	static Direction[] directions = Direction.values();

	public static void run(GameController gc, Unit curUnit) {

		MapLocation location = curUnit.location().mapLocation();

		//attack enemies that are near you
		if (canAttack()) {
			attackNearbyEnemies();
		}

		if (canMove()) {
			move();
		}
	}

	public static boolean canAttack() {
		return curUnit.attackHeat() < 10;
	}

	public static void attackNearbyEnemies() {
		VecUnit nearbyUnits = gc.senseNearbyUnits(curUnit.location().mapLocation(), curUnit.attackRange());
		for (int i = 0; i < nearbyUnits.size(); i++) {
			Unit unit = nearbyUnits.get(i);
			//if can attack this enemy unit
			if (unit.team() != gc.team() && gc.isAttackReady(curUnit.id()) && gc.canAttack(curUnit.id(), unit.id())) {
				gc.attack(unit.id(), curUnit.id());
				return;
			}
		}
	}

	public static boolean canMove() {
		return curUnit.movementHeat() < 10;
	}

	public static void move() {
		for (int i = 0; i < directions.length; i++) {
			if (gc.isMoveReady(curUnit.id()) && gc.canMove(curUnit.id(), directions[i])) {
				gc.moveRobot(curUnit.id(), directions[i]);
				return;
			}
		}
	}

}