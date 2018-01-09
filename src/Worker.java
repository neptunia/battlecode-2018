import java.util.*;
import java.io.*;

public class Worker {

	static Unit curUnit;
	static GameController gc;
	static Direction[] directions = Direction.values();

	public static void run(GameController gc, Unit curUnit) {
		this.curUnit = curUnit;
		this.gc = gc;

		if (gc.round() == 1) {
			//Initial replication
			for (int i = 0; i < directions.length; i++) {
				if (gc.canReplicate(directions[i])) {
					gc.replicate(curUnit.id(), directions[i]);
					break;
				}
			}
		}

		if (curUnit.workerHasActed()) {
			return;
		}

		//try to work on a blueprint nearby
		workOnBluePrint();

		if (curUnit.workerHasActed()) {
			return;
		}
		
		//try to build a factory in all dirs
		buildFactory();

		if (curUnit.workerHasActed()) {
			return;
		}

		//can't build factory, try moving in some dir
		move();

		return;
	}

	public static void workOnBlueprint() {
		Location curLoc = unit.location();
		VecUnit nearby = gc.senseNearbyUnits(curLoc.mapLocation(), 2);
		for (int i = 0; i < nearby.size(); i++) {
			Unit toBuild = nearby.get(i);
			if (gc.canBuild(curUnit.id(), toBuild.id())) {
				gc.build(curUnit.id(), toBuild.id());
				return;
			}
		}
	}

	public static void buildFactory() {
		for (int i = 0; i < directions.length; i++) {
			if (gc.karbonite() > bc.UnitType.Factory.blueprint_cost() && gc.can_blueprint(curUnit.id(), bc.UnitType.Factory, directions[i])) {
				gc.blueprint(curUnit.id(), bc.UnitTypeFactory, directions[i]);
				return;
			}
		}
	}

	public static void move() {
		for (int i = 0; i < directions.length; i++) {
			if (gc.isMoveReady(curUnit.id()) && gc.canMove(curUnit.id(), directions[i])) {
				gc.moveRobot(curUnit.id(), directions[i]);
				return;
			}
		}
	}

}