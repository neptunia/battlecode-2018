import bc.*;

import java.util.*;
import java.io.*;

public class Worker {

	static Unit curUnit;
	static GameController gc;
	static Direction[] directions = Direction.values();

	public static void run(GameController gc, Unit curUnit) {

		Worker.gc = gc;
		Worker.curUnit = curUnit;

		if (gc.round() == 1) {
			//Initial replication
			for (int i = 0; i < directions.length; i++) {
				if (gc.canReplicate(curUnit.id(), directions[i])) {
					gc.replicate(curUnit.id(), directions[i]);
					break;
				}
			}
			//guesstimate enemy location
			MapLocation temp = curUnit.location().mapLocation();
			RobotPlayer.enemyLocation = new MapLocation(Planet.Earth, RobotPlayer.gridX - temp.getX(), RobotPlayer.gridY - temp.getY());
		}

		//try to work on a blueprint nearby
		workOnBlueprint();
		
		//try to build a factory in all dirs
		buildFactory();

		//can't build factory, try moving in some dir
		move();

		return;
	}

	public static void workOnBlueprint() {
		Location curLoc = curUnit.location();
		VecUnit nearby = getNearby(curLoc, 2);
		for (int i = 0; i < nearby.size(); i++) {
			Unit toBuild = nearby.get(i);
			if (gc.canBuild(curUnit.id(), toBuild.id())) {
				gc.build(curUnit.id(), toBuild.id());
				return;
			}
		}
	}

	//senses nearby units and updates RobotPlayer.map
	public static VecUnit getNearby(MapLocation maploc, int radius) {
		VecUnit nearby = gc.senseNearbyUnits(maploc, radius);
		for (int i = 0; i < nearby.size(); i++) {
			Unit unit = nearby.get(i);
			MapLocation temp = unit.location().mapLocation();
			RobotPlayer.map[temp.getX()][temp.getY()] = unit;
		}
		return nearby;
	}

	public static void buildFactory() {
		for (int i = 0; i < directions.length; i++) {
			if (gc.canBlueprint(curUnit.id(), UnitType.Factory, directions[i])) {
				gc.blueprint(curUnit.id(), UnitType.Factory, directions[i]);
				break;
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