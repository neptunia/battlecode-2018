import bc.*;

public class Factory {

	static Unit curUnit;
	static GameController gc;

	public static void run(GameController gc, Unit curUnit) {

		Factory.curUnit = curUnit;
		Factory.gc = gc;

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
		
		//produce unit
		if (gc.canProduceRobot(curUnit.id(), UnitType.Knight)) {
			gc.produceRobot(curUnit.id(), UnitType.Knight);
		}

		return;
	}
}