import bc.*;
import java.util.*;

public class Factory {

	static Unit curUnit;
	static MapLocation curLoc;
	static GameController gc;
	static int myId;
	static HashSet<Integer> workers = new HashSet<Integer>();
	static Direction[] directions = Direction.values();

	public static void run(Unit curUnit) {

		Factory.curUnit = curUnit;
		Factory.curLoc = curUnit.location().mapLocation();

		if (curUnit.structureIsBuilt() == 0) {
			return;
		}
		myId = Worker.id.get(curUnit.id());

		VecUnitID garrison = curUnit.structureGarrison();
		for (int i = 0; i < garrison.size(); i++) {
			//unload units
			for (int a = 0; a < directions.length; a++) {
				if (gc.canUnload(curUnit.id(), directions[a])) {
					gc.unload(curUnit.id(), directions[a]);
					Unit newUnit = gc.senseUnitAtLocation(curLoc.add(directions[a]));
					int newId = newUnit.id();
					Worker.id.put(newId, myId);
					Player.newUnits.add(newUnit);
					break;
				}
			}
		}

		if (Player.numWorker < 1 && gc.canProduceRobot(curUnit.id(), UnitType.Worker)) {
			gc.produceRobot(curUnit.id(), UnitType.Worker);
			Player.numWorker++;
		}

		if (Player.knightsProduced <= 5 && gc.round() <= 100 && gc.senseNearbyUnitsByTeam(curLoc, 50, Player.enemyTeam).size() != 0) {
			gc.produceRobot(curUnit.id(), UnitType.Knight);
			Player.knightsProduced++;
		} else if (Player.prevBlocked < 15 && Player.timesReachedTarget < 3) {
			if (Player.numRanger > 3 * Player.numHealer && gc.researchInfo().getLevel(UnitType.Healer) >= 1 && gc.canProduceRobot(curUnit.id(), UnitType.Healer)) {
				gc.produceRobot(curUnit.id(), UnitType.Healer);
				Player.numHealer++;
			} else if (gc.canProduceRobot(curUnit.id(), UnitType.Ranger)) {
				gc.produceRobot(curUnit.id(), UnitType.Ranger);
				Player.numRanger++;
			}
		}
	}

	public static int hash(MapLocation loc) {
		return 69 * loc.getX() + loc.getY();
	}

	public static boolean onMap(MapLocation test) {
        int x = test.getX();
        int y = test.getY();
        return x >= 0 && y >= 0 && x < Player.gridX && y < Player.gridY;
    }
}