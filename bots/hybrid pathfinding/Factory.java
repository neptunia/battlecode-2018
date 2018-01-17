import bc.*;
import java.util.*;

public class Factory {

	static Unit curUnit;
	static GameController gc;
	static int c = 0;
	//factory id, target
	static HashMap<Integer, MapLocation> presetTargets = new HashMap<Integer, MapLocation>();
	static HashMap<Integer, Integer> presetCounter = new HashMap<Integer, Integer>();

	public static void run(GameController gc, Unit curUnit) {

		Factory.curUnit = curUnit;

		/*if (curUnit.isFactoryProducing()) {
			return;
		}*/

		if (Player.timesReachedTarget >= 3) {
            return;
        }

		Direction[] directions = Direction.values();

		VecUnitID garrison = curUnit.structureGarrison();
		for (int i = 0; i < garrison.size(); i++) {
			//unload units
			for (int a = 0; a < directions.length; a++) {
				if (gc.canUnload(curUnit.id(), directions[a])) {
					gc.unload(curUnit.id(), directions[a]);
					//set the unit's target to preset target
					if (presetTargets.containsKey(curUnit.id())) {
						MapLocation target = presetTargets.get(curUnit.id());
						Ranger.priorityTarget.put(garrison.get(i), target);
						presetCounter.put(curUnit.id(), presetCounter.get(curUnit.id()) + 1);
						if (presetCounter.get(curUnit.id()) >= 6) {
							presetCounter.remove(curUnit.id());
							presetTargets.remove(curUnit.id());
						}
					}
					break;
				}
			}
		}

		Player.currentIncome -= 4;
		//produce unit if no rockets have been started AND rockets can be built
		if (!(Worker.rocketsBuilt < 2 && Worker.rocketBlueprintId == -1 && gc.researchInfo().getLevel(UnitType.Rocket) > 0) && gc.canProduceRobot(curUnit.id(), UnitType.Ranger)) {
			/*if (c % 8 == 7) {
				gc.produceRobot(curUnit.id(), UnitType.Healer);
			} else {
				gc.produceRobot(curUnit.id(), UnitType.Ranger);
			}*/
			gc.produceRobot(curUnit.id(), UnitType.Ranger);
			c += 1;
		}

		return;
	}
}