import bc.*;
import java.util.*;

public class Factory {

	static Unit curUnit;
	static MapLocation curLoc;
	static GameController gc;
	static HashSet<Integer> workers = new HashSet<Integer>();
	static Direction[] directions = Direction.values();

	public static void run(Unit curUnit) {

		Factory.curUnit = curUnit;
		Factory.curLoc = curUnit.location().mapLocation();

		if (Player.timesReachedTarget >= 3) {
            return;
        }

		VecUnitID garrison = curUnit.structureGarrison();
		for (int i = 0; i < garrison.size(); i++) {
			//unload units
			for (int a = 0; a < directions.length; a++) {
				if (gc.canUnload(curUnit.id(), directions[a])) {
					gc.unload(curUnit.id(), directions[a]);
					break;
				}
			}
		}

		Player.currentIncome -= 4;
		//produce unit if no rockets have been started AND rockets can be built
		if (gc.canProduceRobot(curUnit.id(), UnitType.Ranger)) {
			gc.produceRobot(curUnit.id(), UnitType.Ranger);
		}

		return;
	}

	public static int hash(MapLocation loc) {
		return 69 * loc.getX() + loc.getY();
	}
}