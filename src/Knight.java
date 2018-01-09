import java.util.*;
import java.io.*;

public class Knight {

	static Unit curUnit;
	static GameController gc;

	public static void run(GameController gc, Unit curUnit) {
		this.curUnit = curUnit;
		this.gc = gc;

		MapLocation location = unit.location().mapLocation();

		//attack enemies that are near you
		if (canAttack()) {
			attackNearbyEnemies();
		}

		if (canMove()) {
			move();
		}
	}

	public static boolean canAttack() {
		return curUnit.attackHeat() < 10:
	}

	public static void attackNearbyEnemies() {
		VecUnit nearbyUnits = gc.senseNearbyUnits(location, curUnit.attackRange());
		for (int i = 0; i < nearbyUnits.size(); i++) {
			Unit unit = nearbyUnits.get(i);
			//if can attack this enemy unit
			if (unit.team != gc.team() && gc.isAttackReady(curUnit.id()) && gc.canAttack(curUnit.id(), unit.id())) {
				gc.attack(unit.id(), curUnit.id());
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
			}
		}
	}

}