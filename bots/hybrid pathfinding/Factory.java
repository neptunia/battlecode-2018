import bc.*;

public class Factory {

	static Unit curUnit;
	static GameController gc;
	static int c = 0;

	public static void run(GameController gc, Unit curUnit) {

		Factory.curUnit = curUnit;

		/*if (curUnit.isFactoryProducing()) {
			return;
		}*/

		Direction[] directions = Direction.values();

		VecUnitID garrison = curUnit.structureGarrison();
		for (int i = 0; i < garrison.size(); i++) {
			//unload units
			for (int a = 0; a < directions.length; a++) {
				if (gc.canUnload(curUnit.id(), directions[a])) {
					gc.unload(curUnit.id(), directions[a]);
					//TODO update RobotPlayer.map with location of placed down robot
					break;
				}
			}
		}
		
		//produce unit if no rockets have been started AND rockets can be built
		if (!(Worker.rocketsBuilt == 0 && Worker.rocketBlueprintId == -1 && gc.researchInfo().getLevel(UnitType.Rocket) > 0) && gc.canProduceRobot(curUnit.id(), UnitType.Ranger)) {
			gc.produceRobot(curUnit.id(), UnitType.Ranger);
			c += 1;
		}

		return;
	}
}