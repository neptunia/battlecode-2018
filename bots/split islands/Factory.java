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
					int newId = gc.senseUnitAtLocation(curLoc.add(directions[a])).id();
					Player.parentWorker.put(newId, Player.parentWorker.get(curUnit.id()));
					break;
				}
			}
		}

		Player.currentIncome -= 4;
		//produce unit if no rockets have been started AND rockets can be built
		if (Player.prevBlocked < 10 && gc.canProduceRobot(curUnit.id(), UnitType.Ranger)) {
			if (Worker.numWorkers == 0 && gc.karbonite() >= 25) {
                gc.produceRobot(curUnit.id(), UnitType.Worker);
            } else if (gc.researchInfo().getLevel(UnitType.Healer) >= 1 && Player.numRangers > 4*Player.numHealers) {
				gc.produceRobot(curUnit.id(), UnitType.Healer);
				Player.numHealers++;
			} else {
				gc.produceRobot(curUnit.id(), UnitType.Ranger);
				Player.numRangers++;
			}
		}

		return;
	}

	public static int hash(MapLocation loc) {
		return 69 * loc.getX() + loc.getY();
	}
}