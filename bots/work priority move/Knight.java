import bc.*;
import java.util.*;

public class Knight {

	static Unit curUnit;
	static GameController gc;
	static Direction[] directions = Direction.values();
	static HashMap<Integer, HashSet<Integer>> visited = new HashMap<Integer, HashSet<Integer>>();
	static HashMap<Integer, Integer> targets = new HashMap<Integer, Integer>();
	static MapLocation curLoc;

	public static void run(GameController gc, Unit curUnit) {

		Knight.curUnit = curUnit;
		Knight.gc = gc;

		if (curUnit.location().isInGarrison()) {
			return;
		}

		Knight.curLoc = curUnit.location().mapLocation();

		Pair nearbyInfo = null;

		//if i dont have a target right now
		if (!targets.containsKey(curUnit.id())) {
			//try to find an enemy target in range
			nearbyInfo = findTarget();
		} else {
			//remove unit if it is already killed
			try {
				gc.unit(targets.get(curUnit.id()));
			} catch (Exception e) {
				//already killed this unit, or it ran away, remove
				targets.remove(curUnit.id());
				nearbyInfo = findTarget();
			}
			
		}

		//didnt sense number of allies and enemies yet
		if (nearbyInfo == null) {
			nearbyInfo = countNearby();
		}

		//if this knight has a target
		if (targets.containsKey(curUnit.id())) {
			Player.sawEnemy = true;
			MapLocation targetLoc = gc.unit(targets.get(curUnit.id())).location().mapLocation();
			//move towards them if my army is stronger
			if (canMove()) {
				move(targetLoc);
				if (canMove()) {
					moveAttack(targetLoc);
				}
			}
			if (canAttack()) {
				tryAttack(targets.get(curUnit.id()));
			}
		} else {
			//otherwise explore
			if (canMove()) {
				move(Player.enemyLocation[Player.parentWorker.get(curUnit.id())]);
			}
		}

		//if i couldnt attack my target, attack anyone around me i can attack
		if (canAttack()) {
			attackNearbyEnemies();
		}

	}

	//maximizes distance between enemy
	public static void moveAway(MapLocation enemy) {
        int best = distance(curUnit.location().mapLocation(), enemy);
        Direction bestd = null;
        for (int i = 0; i < directions.length; i++) {
            MapLocation temp = curUnit.location().mapLocation().add(directions[i]);
            if (gc.canMove(curUnit.id(), directions[i]) && distance(temp, enemy) > best) {
                best = distance(temp, enemy);
                bestd = directions[i];
            }
        }
        if (bestd != null) {
            gc.moveRobot(curUnit.id(), bestd);
        }
    }

	//try to attack a target unit
	public static void tryAttack(int id) {
		if (gc.canAttack(curUnit.id(), id)) {
			gc.attack(curUnit.id(), id);
		}
	}

	public static Pair findTarget() {
		Pair ret = new Pair();
		VecUnit nearby = gc.senseNearbyUnitsByTeam(curUnit.location().mapLocation(), curUnit.visionRange(), Player.enemyTeam);
		int tempTarget = -1;
		int smallest = 9999999;
		//find nearest target
		for (int i = 0; i < nearby.size(); i++) {
			Unit temp3 = nearby.get(i);
			ret.enemy++;
			MapLocation temp2 = temp3.location().mapLocation();
			int temp = distance(curUnit.location().mapLocation(), temp2);
			if (temp < smallest) {
				smallest = temp;
				tempTarget = temp3.id();
			}
		}
		//if found a target, set that as target for units around me
		if (smallest != 9999999) {
			for (int i = 0; i < nearby.size(); i++) {
				Unit temp3 = nearby.get(i);
				if (temp3.team() == gc.team()) {
					ret.friendly++;
					targets.put(temp3.id(), tempTarget);
				}
			}
		}
		return ret;
	}

	//count number of nearby enemies and allies
	public static Pair countNearby() {
		Pair ret = new Pair();
		VecUnit nearby = gc.senseNearbyUnits(curUnit.location().mapLocation(), curUnit.visionRange());
		for (int i = 0; i < nearby.size(); i++) {
			Unit temp3 = nearby.get(i);
			if (temp3.unitType() != UnitType.Worker && temp3.unitType() != UnitType.Rocket && temp3.unitType() != UnitType.Factory) {
				if (temp3.team() != gc.team()) {
					ret.enemy++;
				} else {
					ret.friendly++;
				}
			}
		}
		return ret;
	}

	//struct to store two ints lol
	public static class Pair {
		int friendly;
		int enemy;
		Pair() {
			friendly = 0;
			enemy = 0;
		}
	}

	public static boolean canAttack() {
		return curUnit.attackHeat() < 10;
	}

	//attack nearest enemy found
	public static void attackNearbyEnemies() {
		VecUnit nearbyUnits = getNearby(curUnit.location().mapLocation(), (int) curUnit.attackRange());
		for (int i = 0; i < nearbyUnits.size(); i++) {
			Unit unit = nearbyUnits.get(i);
			//if can attack this enemy unit
			if (unit.team() != gc.team() && gc.isAttackReady(curUnit.id()) && gc.canAttack(curUnit.id(), unit.id())) {
				gc.attack(curUnit.id(), unit.id());
				return;
			}
		}
	}

	public static boolean canMove() {
		return curUnit.movementHeat() < 10;
	}

	public static boolean moveAttack(MapLocation target) {
        //greedy pathfinding
        int smallest = 999999;
        Direction d = null;
        MapLocation curLoc = curUnit.location().mapLocation();
        int hash = hash(curLoc.getX(), curLoc.getY());
        if (!visited.containsKey(curUnit.id())) {
            HashSet<Integer> temp = new HashSet<Integer>();
            temp.add(hash);
            visited.put(curUnit.id(), temp);
        } else {
            visited.get(curUnit.id()).add(hash);
        }
        for (int i = 0; i < directions.length; i++) {
            MapLocation newSquare = curLoc.add(directions[i]);
            int dist = distance(newSquare, target);
            if (!visited.get(curUnit.id()).contains(hash(newSquare.getX(), newSquare.getY())) && gc.canMove(curUnit.id(), directions[i]) && dist < smallest) {
                smallest = distance(newSquare, target);
                d = directions[i];
            }
        }
        if (d == null) {
            //can't move
            //TODO change
            visited.remove(curUnit.id());
            return false;
        }
        gc.moveRobot(curUnit.id(), d);
        return true;
    }

	//pathing
	//move towards target location
	public static void move(MapLocation target) {
		int targetHash = hash(target);
		if (hash(curLoc) == targetHash || !gc.isMoveReady(curUnit.id())) {
			return;
		}
		int x = curLoc.getX();
		int y = curLoc.getY();
		int currentDist = Player.pathDistances[targetHash][x][y];
		if (currentDist != 696969) {
			if (x < Player.gridX - 1 && Player.pathDistances[targetHash][x + 1][y] - currentDist < 0 && gc.canMove(curUnit.id(), Direction.East)) {
				gc.moveRobot(curUnit.id(), Direction.East);
			} else if (x > 0 && Player.pathDistances[targetHash][x - 1][y] - currentDist < 0 && gc.canMove(curUnit.id(), Direction.West)) {
				gc.moveRobot(curUnit.id(), Direction.West);
			} else if (y < Player.gridY - 1 && Player.pathDistances[targetHash][x][y + 1] - currentDist < 0 && gc.canMove(curUnit.id(), Direction.North)) {
				gc.moveRobot(curUnit.id(), Direction.North);
			} else if (y > 0 && Player.pathDistances[targetHash][x][y - 1] - currentDist < 0 && gc.canMove(curUnit.id(), Direction.South)) {
				gc.moveRobot(curUnit.id(), Direction.South);
			} else if (y < Player.gridY - 1 && x < Player.gridX - 1 && Player.pathDistances[targetHash][x + 1][y + 1] - currentDist < 0 && gc.canMove(curUnit.id(), Direction.Northeast)) {
				gc.moveRobot(curUnit.id(), Direction.Northeast);
			} else if (y > 0 && x < Player.gridX - 1 && Player.pathDistances[targetHash][x + 1][y - 1] - currentDist < 0 && gc.canMove(curUnit.id(), Direction.Southeast)) {
				gc.moveRobot(curUnit.id(), Direction.Southeast);
			} else if (x > 0 && y < Player.gridY - 1 && Player.pathDistances[targetHash][x - 1][y + 1] - currentDist < 0 && gc.canMove(curUnit.id(), Direction.Northwest)) {
				gc.moveRobot(curUnit.id(), Direction.Northwest);
			} else if (x > 0 && y > 0 && Player.pathDistances[targetHash][x - 1][y - 1] - currentDist < 0 && gc.canMove(curUnit.id(), Direction.Southwest)) {
				gc.moveRobot(curUnit.id(), Direction.Southwest);
			}
		} else {
			//cant get there
			if (Player.bfsMin(target, curLoc)) {
				move(target);
			} else {
				System.out.println("cant get there ranger");
			}
		}
		if (gc.isMoveReady(curUnit.id())) {
			Player.blockedCount++;
			moveGreed(target);
		}
	}

	public static boolean moveGreed(MapLocation enemy) {
		int best = 999999999;
		Direction bestd = null;
		for (int i = 0; i < directions.length; i++) {
			MapLocation temp = curUnit.location().mapLocation().add(directions[i]);
			if (gc.canMove(curUnit.id(), directions[i]) && distance(temp, enemy) < best) {
				best = distance(temp, enemy);
				bestd = directions[i];
			}
		}
		if (bestd != null) {
			gc.moveRobot(curUnit.id(), bestd);
			return true;
		}
		return false;
	}

	public static String print(int hash) {
		int asdf = hash % 69;
		int asdfg = (hash - asdf) / 69;
		return Integer.toString(asdf) + " " + Integer.toString(asdfg);
	}

	public static int doubleHash(int x1, int y1, int x2, int y2) {
		return (69 * x1) + y1 + ((69 * x2) + y2) * 10000;
	}

	public static int doubleHash(int hash1, int hash2) {
		int y1 = hash1 % 69;
		int x1 = (hash1 - y1) / 69;
		int y2 = hash2 % 69;
		int x2 = (hash2 - y2) / 69;
		return (69 * x1) + y1 + ((69 * x2) + y2) * 10000;
	}

	public static int doubleHash(MapLocation first, MapLocation second) {
		int x1 = first.getX(), y1 = first.getY(), x2 = second.getX(), y2 = second.getY();
		return (69 * x1) + y1 + ((69 * x2) + y2) * 10000;
	}

	public static boolean checkPassable(MapLocation test) {
        if (test.getX() >= Player.gridX || test.getY() >= Player.gridY || test.getX() < 0 || test.getY() < 0) {
            return false;
        }
        boolean allyThere = true;
        //factories and rockets and workers count as obstacles
        try {
            Unit temp = gc.senseUnitAtLocation(test);
            if (temp.unitType() != UnitType.Factory && temp.unitType() != UnitType.Rocket && temp.unitType() != UnitType.Worker) {
                allyThere = false;
            }
        } catch (Exception e) {
            allyThere = false;
        }
        return Player.planetMap.isPassableTerrainAt(test) == 1 && !allyThere;
    }

	public static int hash(int x, int y) {
		return 69 * x + y;
	}

	public static int hash(MapLocation loc) {
		return 69 * loc.getX() + loc.getY();
	}

	public static int distance(MapLocation first, MapLocation second) {
		int x1 = first.getX(), y1 = first.getY(), x2 = second.getX(), y2 = second.getY();
		return (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2);
	}

	public static int manDistance(MapLocation first, MapLocation second) {
		int x1 = first.getX(), y1 = first.getY(), x2 = second.getX(), y2 = second.getY();
		return (x2 - x1) + (y2 - y1);
	}

	public static int manDistance(int hash1, int hash2) {
		int y1 = hash1 % 69;
		int x1 = (hash1 - y1) / 69;
		int y2 = hash2 % 69;
		int x2 = (hash2 - y2) / 69;
		return (x2 - x1) + (y2 - y1);
	}

	public static int distance(int hash1, int hash2) {
		int y1 = hash1 % 69;
		int x1 = (hash1 - y1) / 69;
		int y2 = hash2 % 69;
		int x2 = (hash2 - y2) / 69;
		return (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2);
	}

	//senses nearby units and updates RobotPlayer.map with detected units
	public static VecUnit getNearby(MapLocation maploc, int radius) {
		VecUnit nearby = gc.senseNearbyUnits(maploc, radius);
		for (int i = 0; i < nearby.size(); i++) {
			Unit unit = nearby.get(i);
			MapLocation temp = unit.location().mapLocation();
			Player.map[temp.getX()][temp.getY()] = unit;
		}
		return nearby;
	}

}