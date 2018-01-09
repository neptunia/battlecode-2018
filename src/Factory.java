import java.util.*;
import java.io.*;

public class Factory {

	static Unit curUnit;
	static GameController gc;

	public static void run(GameController gc, Unit curUnit) {
		this.curUnit = curUnit;
		this.gc = gc;

		Direction[] directions = Direction.values();

		VecUnitID garrison = curUnit.structureGarrison();
		if (garrison.size() > 0) {
			for (int i = 0; i < garrison.size(); i++) {
				//unload units
				for (int a = 0; a < directions.length; a++) {
					if (gc.canUnload(garrison.get(i).id(), directions[a])) {
						gc.unload(garrison.get(i).id(), directions[a]);
						break;
					}
				}
			}
		} else {
			//produce unit
			if (gc.canProduceRobot(curUnit.id(), bc.UnitType.Knight)) {
				gc.produceRobot(curUnit.id(), bc.UnitType.Knight);
			}
		}

		return;
	}
}