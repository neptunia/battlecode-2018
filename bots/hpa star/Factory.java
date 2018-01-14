import bc.*;

public class Factory {

	static Unit curUnit;
	static GameController gc;
	static int c = 0;

	public static void run(GameController gc, Unit curUnit) {

		System.out.println("factory");

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

		System.out.println("factory produce");
		
		//produce unit
		if (gc.canProduceRobot(curUnit.id(), UnitType.Knight)) {
			System.out.println("wew a knight");
			gc.produceRobot(curUnit.id(), UnitType.Knight);
			c += 1;
		}

		System.out.println("factory done");

		return;
	}
}