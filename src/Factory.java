import java.util.*;
import java.io.*;
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
		if (garrison.size() > 0) {
			for (int i = 0; i < garrison.size(); i++) {
				//unload units
				for (int a = 0; a < directions.length; a++) {
					if (gc.canUnload(garrison.get(i), directions[a])) {
						gc.unload(garrison.get(i), directions[a]);
						break;
					}
				}
			}
		}
		
		//produce unit
		if (gc.canProduceRobot(curUnit.id(), bc.UnitType.Knight)) {
			gc.produceRobot(curUnit.id(), bc.UnitType.Knight);
		}

		return;
	}
}