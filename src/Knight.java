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
		//TODO change team number!!
		VecUnit nearbyUnits = gc.senseNearbyUnits(location, curUnit.attackRange());
		for (int i = 0; i < nearbyUnits.size(); i++) {
			Unit unit = nearbyUnits.get(i);
			//if can attack this enemy unit
			if (unit.team != gc.team() && gc.isAttackReady(curUnit.id()) && gc.canAttack(curUnit.id(), unit.id())) {
				gc.attack(unit.id(), curUnit.id());
			}
		}
	}

}