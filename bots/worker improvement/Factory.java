import bc.*;
import java.util.*;

public class Factory {

	static Unit curUnit;
	static MapLocation curLoc;
	static GameController gc;
	static int myId;
	static HashSet<Integer> workers = new HashSet<Integer>();
	static Direction[] directions = Direction.values();
	static HashSet<Integer> sentHealSignal = new HashSet<Integer>();
	static HashMap<Integer, Integer> thingsProduced = new HashMap<Integer, Integer>();

	public static void run(Unit curUnit) {

		Factory.curUnit = curUnit;
		Factory.curLoc = curUnit.location().mapLocation();
		if (!thingsProduced.containsKey(curUnit.id())) {
			thingsProduced.put(curUnit.id(), 0);
		}

		if (curUnit.structureIsBuilt() == 0) {
			return;
		}
		myId = Worker.id.get(curUnit.id());

		if (curUnit.health() != curUnit.maxHealth()) {
			findWorkersToHealMe();
			sentHealSignal.add(curUnit.id());
		} else if (sentHealSignal.contains(curUnit.id())) {
			sentHealSignal.remove(curUnit.id());
		}

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

		if (Player.prevBlocked < 10 && ((Player.timesReachedTarget < 1 && gc.round() < 600) || (Player.numRanger < 10 && Player.numHealer < 4)) && (gc.karbonite() > 140 || gc.researchInfo().getLevel(UnitType.Rocket) == 0)) {
			if (Player.numRanger + Player.numKnight > 3 * Player.numHealer && gc.researchInfo().getLevel(UnitType.Healer) >= 1 && gc.canProduceRobot(curUnit.id(), UnitType.Healer)) {
				gc.produceRobot(curUnit.id(), UnitType.Healer);
				Player.numHealer++;
			} else if (gc.canProduceRobot(curUnit.id(), UnitType.Healer) && gc.senseNearbyUnitsByTeam(curLoc, 50, Player.enemyTeam).size() != 0) {
				if (thingsProduced.get(curUnit.id()) % 2 == 0) {
					gc.produceRobot(curUnit.id(), UnitType.Knight);
					Player.numKnight++;
				} else {
					gc.produceRobot(curUnit.id(), UnitType.Mage);
				}
				thingsProduced.put(curUnit.id(), thingsProduced.get(curUnit.id())+1);
			} else if (gc.canProduceRobot(curUnit.id(), UnitType.Ranger)) {
				gc.produceRobot(curUnit.id(), UnitType.Ranger);
				Player.numRanger++;
			}
		}

	}

	public static void findWorkersToHealMe() {
		LinkedList<MapLocation> queue = new LinkedList<MapLocation>();
		HashSet<Integer> visited = new HashSet<Integer>();
		queue.add(curLoc);
		visited.add(hash(curLoc));
		int workerFound = 0;
		while (!queue.isEmpty()) {
			MapLocation current = queue.poll();
			if (manDistance(current, curLoc) > 2 && workerFound >= 2) {
				return;
			}
			if (gc.hasUnitAtLocation(current)) {
				Unit theUnit = gc.senseUnitAtLocation(current);
				//if its' a worker on my team and he's not building/healing anything
				if (theUnit.team() == Player.myTeam && theUnit.unitType() == UnitType.Worker && !Worker.structures.containsKey(theUnit.id()) && !Worker.healFactory.containsKey(theUnit.id())) {
					workerFound++;
					Worker.healFactory.put(theUnit.id(), curLoc);
				}
			}
			for (int i = 0; i < directions.length; i++) {
				MapLocation test = current.add(directions[i]);
				if (!visited.contains(hash(test)) && checkPassable(test)) {
					queue.add(test);
					visited.add(hash(test));
				}
			}
		}
	}

	public static int manDistance(MapLocation first, MapLocation second) {
        int x1 = first.getX(), y1 = first.getY(), x2 = second.getX(), y2 = second.getY();
        return Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2));
    }

	public static boolean checkPassable(MapLocation test) {
        int x = test.getX();
        int y = test.getY();
        if (x >= Player.gridX || y >= Player.gridY || x < 0 || y < 0) {
            return false;
        }
        return Player.gotoable[myId][x][y];// && !allyThere;
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