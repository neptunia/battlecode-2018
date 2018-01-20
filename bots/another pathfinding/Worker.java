import bc.*;
import java.util.*;

public class Worker {

	static Unit curUnit;
	static GameController gc;
	static MapLocation curLoc;
	static Direction[] directions = Direction.values();
	static HashMap<Integer, HashSet<Integer>> visited = new HashMap<Integer, HashSet<Integer>>();
	static HashMap<Integer, Integer> target = new HashMap<Integer, Integer>();
	static HashMap<Integer, MapLocation> buildBlueprintLocation = new HashMap<Integer, MapLocation>();
	static int rocketsBuilt = 0;
	static int numFacts = 0;
	static Planet curPlanet;
	static boolean karbonitesLeft = true;
	static int numWorkers = -1;
	static MapLocation[] karbonites;
	static HashMap<Integer, Integer> karboniteIndex = new HashMap<Integer, Integer>();
	static int numKarbsCounter = 0;
	static int factsQueued = 0;
	static boolean wentToMine = false;
	static HashMap<Integer, MapLocation> karboniteTargets = new HashMap<Integer, MapLocation>();
	//set of factories or rockets a worker is going to build to prevent ppl queueing the saem location
	static HashSet<Integer> structuresToBuild = new HashSet<Integer>();

	public static void run(Unit curUnit) {

		Worker.curUnit = curUnit;
		curPlanet = gc.planet();

		if (curUnit.location().isInGarrison()) {
			return;
		}
		curLoc = curUnit.location().mapLocation();

		//TODO: if on mars temporary code
		if (gc.planet() == Planet.Mars) {
			move(Player.enemyLocation);
			if (numWorkers < (int)(Math.sqrt(Player.gridX * Player.gridY))) {
				for (int i = 0; i < directions.length; i++) {
					if (gc.canReplicate(curUnit.id(), directions[i])) {
						gc.replicate(curUnit.id(), directions[i]);
						Player.workerCount++;
						//Worker.run(gc.senseUnitAtLocation(curUnit.location().mapLocation().add(directions[i])));
						break;
					}
				}
			}
			return;
		}

		//if i do have a target blueprint
		if (target.containsKey(curUnit.id())) {
			workOnBlueprint();
			removeBuildStructureTarget();
			removeKarboniteTarget();
			return;
		}

		if (gc.round() < 10 && distance(curLoc, selectKarbonite()) <= 4) {
			goMine();
			removeBuildStructureTarget();
			replicateAnywhere();
			return;
		}


		if (buildBlueprintLocation.containsKey(curUnit.id())) {
			buildStructure(UnitType.Factory);
			removeKarboniteTarget();
			return;
		}

		//not enough workers - replicate
		if ((numWorkers < 6 || numWorkers < (int) Math.round(Math.sqrt((Player.planetMap.getHeight()) * (Player.planetMap.getWidth())) / 2 / Math.sqrt(gc.round())))) {
			replicateAnywhere();
		}


		if (Player.prevBlocked < 10 && numFacts < 5 && gc.karbonite() >= 120 && Player.timesReachedTarget < 3) {
			buildStructure(UnitType.Factory);
			removeKarboniteTarget();
		} else if (karbonitesLeft && gc.karbonite() < 200) {
			goMine();
			removeBuildStructureTarget();
		} else if (gc.karbonite() >= 75 && gc.researchInfo().getLevel(UnitType.Rocket) > 0) {
			buildStructure(UnitType.Rocket);
			removeKarboniteTarget();
		} else if (karbonitesLeft) {
			goMine();
			removeBuildStructureTarget();
		} else {
			System.out.println("nothing to do");
			//nothing to do
			moveAnywhere();
		}
	}

	public static void removeKarboniteTarget() {
		if (karboniteTargets.containsKey(curUnit.id())) {
			karboniteTargets.remove(curUnit.id());
		}
	}

	public static void removeBuildStructureTarget() {
		if (buildBlueprintLocation.containsKey(curUnit.id())) {
			MapLocation temp = buildBlueprintLocation.get(curUnit.id());
			structuresToBuild.remove(hash(temp));
			buildBlueprintLocation.remove(curUnit.id());
		}
	}

	public static void moveAnywhere() {
		if (gc.isMoveReady(curUnit.id())) {
			for (int i = 0; i < directions.length; i++) {
				if (gc.canMove(curUnit.id(), directions[i])) {
					gc.moveRobot(curUnit.id(), directions[i]);
					return;
				}
			}
		}
	}

	public static void workOnBlueprint() {
		int targetBlueprint = target.get(curUnit.id());
		Unit toWorkOn = null;
		try {
			toWorkOn = gc.unit(targetBlueprint);
		} catch (Exception e) {
			//blueprint probably died
			target.remove(curUnit.id());
			Worker.run(curUnit);
		}


		MapLocation blueprintLoc = toWorkOn.location().mapLocation();
		//already done working on
		if (toWorkOn.health() == toWorkOn.maxHealth()) {
			target.remove(curUnit.id());
			if (toWorkOn.unitType() == UnitType.Rocket && !Rocket.assignedUnits.contains(targetBlueprint)) {
				assignUnits(blueprintLoc);
				Rocket.assignedUnits.add(targetBlueprint);
			}
			Worker.run(curUnit);
		} else {
			//goto it and build it

			if (distance(blueprintLoc, curLoc) <= 2) {
				//next to it, i can work on it
				if (gc.canBuild(curUnit.id(), targetBlueprint)) {
					gc.build(curUnit.id(), targetBlueprint);
				} else {
					//couldnt work on it
					//System.out.println("Blueprint location: " + blueprintLoc.toString());
					//System.out.println("couldnt work on blueprint");
				}
				/*
				if (gc.isMoveReady(curUnit.id())) {
					makeWay(curLoc, new HashSet<Integer>(), blueprintLoc));
				}
				*/
				if (curUnit.abilityHeat() < 10 && numWorkers < (int) Math.round(Math.sqrt((Player.planetMap.getHeight()) * (Player.planetMap.getWidth())) / 2 / Math.sqrt(gc.round()))) {
					//try replicating to where there's this blueprint while making others move if they can
					MapLocation temp = null;
					for (int i = 0; i < directions.length; i++) {
						temp = curLoc.add(directions[i]);
						if (distance(temp, blueprintLoc) <= 2) {
							HashSet<Integer> cantGo = new HashSet<Integer>();
							cantGo.add(hash(curLoc));
							if (makeWay(temp, cantGo, blueprintLoc) && gc.canReplicate(curUnit.id(), directions[i])) {
								gc.replicate(curUnit.id(), directions[i]);
								target.put(gc.senseUnitAtLocation(temp).id(), targetBlueprint);
								//Worker.run(gc.senseUnitAtLocation(curUnit.location().mapLocation().add(directions[i])));
								break;
							}
						}
					}
				}
				
			} else {
				//move towards it while optimizing workers around factory
				if (gc.isMoveReady(curUnit.id())) {
					if (manDistance(curLoc, blueprintLoc) == 2) {
						MapLocation temp = null;
						for (int i = 0; i < directions.length; i++) {
							temp = curLoc.add(directions[i]);
							if (manDistance(temp, blueprintLoc) == 1) {
								HashSet<Integer> cantGo = new HashSet<Integer>();
								cantGo.add(hash(curLoc));
								if (makeWay(temp, cantGo, blueprintLoc) && gc.canMove(curUnit.id(), directions[i])) {
									gc.moveRobot(curUnit.id(), directions[i]);
									break;
								}
							}
						}
					} else {
						move(blueprintLoc);
					}
				}
			}
		}
	}

	public static boolean makeWay(MapLocation toGo, HashSet<Integer> cantGo, MapLocation blueprintLoc) {
		try {
			Unit unit = gc.senseUnitAtLocation(toGo);
			if (!gc.isMoveReady(unit.id())) {
				return false;
			}
			for (int i = 0; i < directions.length; i++) {
				MapLocation temp = toGo.add(directions[i]);
				if (!cantGo.contains(hash(toGo)) && onMap(temp) && Player.gotoable[temp.getX()][temp.getY()] && distance(temp, blueprintLoc) <= 2) {
					HashSet<Integer> tempCantGo = new HashSet<Integer>();
					tempCantGo.addAll(cantGo);
					tempCantGo.add(hash(toGo));
					if (makeWay(temp, tempCantGo, blueprintLoc)) {
						gc.moveRobot(unit.id(), directions[i]);
						return true;
					}
				}
			}
		} catch (Exception e) {
			//e.printStackTrace();
			return true;
		}
		return false;
	}

	public static boolean moveAway(MapLocation toGo, HashSet<Integer> cantGo) {
		try {
			Unit unit = gc.senseUnitAtLocation(toGo);
			if (!gc.isMoveReady(unit.id()) || unit.unitType() == UnitType.Factory) {
				return false;
			}
			for (int i = 0; i < directions.length; i++) {
				MapLocation temp = toGo.add(directions[i]);
				if (!cantGo.contains(hash(toGo)) && onMap(temp) && Player.gotoable[temp.getX()][temp.getY()]) {
					HashSet<Integer> tempCantGo = new HashSet<Integer>();
					tempCantGo.addAll(cantGo);
					tempCantGo.add(hash(toGo));
					if (moveAway(temp, tempCantGo)) {
						gc.moveRobot(unit.id(), directions[i]);
						return true;
					}
				}
			}
		} catch (Exception e) {
			//e.printStackTrace();
			return true;
		}
		return false;
	}

	public static boolean onMap(MapLocation test) {
		int x = test.getX();
		int y = test.getY();
		return x >= 0 && y >= 0 && x < Player.gridX && y < Player.gridY;
	}

	//shuffles workers around factories effectively
	//first parameter is parent, second is 
	//public static boolean factoryMakeWay()

	public static void replicateAnywhere() {
		for (int i = 0; i < directions.length; i++) {
			if (gc.canReplicate(curUnit.id(), directions[i])) {
				gc.replicate(curUnit.id(), directions[i]);
				//karboniteList.put(gc.senseUnitAtLocation(curLoc.add(directions[i])).id(), karboniteList.get(curUnit.id()));
				numWorkers++;
				//Worker.run(gc.senseUnitAtLocation(curUnit.location().mapLocation().add(directions[i])));
				return;
			}
		}
	}

	public static void goMine() {
		wentToMine = true;
		if (!karboniteTargets.containsKey(curUnit.id())) {
			karboniteTargets.put(curUnit.id(), selectKarbonite());
		}
		MapLocation theKarb = karboniteTargets.get(curUnit.id());
		//if im next to karbonite
		if (distance(curLoc, karboniteTargets.get(curUnit.id())) <= 2) {
			Direction directionToKarb = curLoc.directionTo(theKarb);
			if (gc.canHarvest(curUnit.id(), directionToKarb)) {
				//harvest karbonite
				gc.harvest(curUnit.id(), directionToKarb);
				Player.currentIncome += curUnit.workerHarvestAmount();
				return;
			} else if (gc.karboniteAt(theKarb) == 0) {
				//karbonite is rip
				karbonites[karboniteIndex.get(hash(theKarb))] = null;
				karboniteTargets.remove(curUnit.id());
				
				Worker.run(curUnit);
			}
		} else {
			move(theKarb);
		}
	}

	public static MapLocation selectKarbonite() {
		int smallest = 9999999;
		MapLocation karb = null;
		for (int i = 0; i < numKarbsCounter; i++) {
			if (karbonites[i] != null) {
				int dist = distance(curLoc, karbonites[i]);
				if (karbonites[i] != null && dist < smallest) {
					smallest = dist;
					karb = karbonites[i];
				}
			}
		}
		if (karb == null) {
			karbonitesLeft = false;
		}
		return karb;
	}

	//bfs to find square such that there are no other factories or rockets around range n of it
	public static MapLocation findBlueprintLocation() {
		LinkedList<MapLocation> queue = new LinkedList<MapLocation>();
		HashSet<Integer> visited = new HashSet<Integer>();
		queue.add(curLoc);
		MapLocation last = curLoc;
		
		visited.add(hash(curLoc));
		
		while (!queue.isEmpty()) {
			MapLocation current = queue.poll();
			
			for (int i = 0; i < directions.length; i++) {
				MapLocation test = current.add(directions[i]);
				int testHash = hash(test);
				if (!visited.contains(testHash) && checkPassable(test)) {
					visited.add(testHash);
					queue.add(test);
				}
			}
			int around = 0;
			for (int i = 0; i < directions.length; i++) {
				MapLocation test = current.add(directions[i]);
				if (onMap(test) && !Player.gotoable[test.getX()][test.getY()]) {
					around++;
					break;
				}
			}

			
			VecUnit nearby = gc.senseNearbyUnitsByTeam(current, 2, Player.myTeam);
			for (int i = 0; i < nearby.size(); i++) {
				UnitType temp = nearby.get(i).unitType();
				//TODO maybe add unittype.rocket too
				if (temp == UnitType.Factory || temp == UnitType.Rocket) {
					around++;
					break;
				}
			}
			for (int i = 0; i < directions.length && around == 0; i++) {
				MapLocation toTest = current.add(directions[i]);
				if (structuresToBuild.contains(hash(toTest))) {
					around++;
				}
			}
			int tempX = current.getX();
			int tempY = current.getY();
			//on the map and gotoable and doesnt block off any spots
			if (around == 0 && !structuresToBuild.contains(hash(current)) && (goAble(tempX - 1, tempY) || goAble(tempX + 1, tempY)) && (goAble(tempX, tempY - 1) || goAble(tempX, tempY + 1))) {
				return current;
			}
		}
		//System.out.println("no good position found :(");
		return null;
	}

	public static boolean goAble(int x, int y) {

		return (x >= 0 && y >= 0 && x < Player.gridX && y < Player.gridY) && Player.gotoable[x][y];
	}

	public static void buildStructure(UnitType type) {
		//System.out.println("build structure");
		if (!buildBlueprintLocation.containsKey(curUnit.id())) {
			MapLocation open = findBlueprintLocation();
			if (open == null) {
				System.out.println("no good locatin found");
				return;
			}
			buildBlueprintLocation.put(curUnit.id(), open);
			structuresToBuild.add(hash(open));
		}
		MapLocation blueprintLocation = buildBlueprintLocation.get(curUnit.id());
		int blueprintHash = hash(blueprintLocation);
		//System.out.println("Blueprint coords: " + Integer.toString(blueprintLocation.getX()) + ", " + Integer.toString(blueprintLocation.getY()));
		Direction dirToBlueprint = curLoc.directionTo(blueprintLocation);
		//if i can build it
		if (distance(curLoc, blueprintLocation) <= 2) {
			//someone's probably blocking it
			if (!gc.canBlueprint(curUnit.id(), type, dirToBlueprint)) {
				moveAway(blueprintLocation, new HashSet<Integer>());
			}
			if (gc.canBlueprint(curUnit.id(), type, dirToBlueprint)) {
				gc.blueprint(curUnit.id(), type, dirToBlueprint);
				Unit blueprint = gc.senseUnitAtLocation(blueprintLocation);
				UnitType temp = blueprint.unitType();
				int targetBlueprint = blueprint.id();
				buildBlueprintLocation.remove(curUnit.id());
				structuresToBuild.remove(blueprintHash);
				
				if (temp == UnitType.Rocket) {
					rocketsBuilt++;
				}
	            if (temp == UnitType.Factory) {
	                numFacts++;
	            }
	            assignWorkers(blueprintLocation, targetBlueprint);
			}
			
		} else {
			//move towards it
			move(blueprintLocation);
		}
	}

	public static void assignWorkers(MapLocation facLoc, int id) {
		LinkedList<MapLocation> queue = new LinkedList<MapLocation>();
		HashSet<Integer> visited = new HashSet<Integer>();
		queue.add(facLoc);
		visited.add(hash(facLoc));
		int workersNeeded = 0;
		for (int i = 0; i < directions.length; i++) {
			MapLocation temp = facLoc.add(directions[i]);
			int x = temp.getX();
			int y = temp.getY();
			if (x >= 0 && y >= 0 && x < Player.gridX && y < Player.gridY && Player.gotoable[x][y]) {
				workersNeeded++;
			}
		}
		while (!queue.isEmpty()) {
			MapLocation current = queue.poll();
			//System.out.println("HI");
			if (manDistance(facLoc, current) > 3) {
				//System.out.println(workersNeeded);
				return;
			}
			for (int i = 0; i < directions.length; i++) {
				MapLocation toCheck = current.add(directions[i]);
				if (workersNeeded <= 0) {
					return;
				}
				try {
					Unit there = gc.senseUnitAtLocation(toCheck);
					if (there.unitType() == UnitType.Worker && !target.containsKey(there.id())) {
						//if worker can replicate
						if (there.abilityHeat() < 10) {
							workersNeeded -= 4;
						} else {
							workersNeeded--;
						}
						target.put(there.id(), id);
						//System.out.println("WEW FOUND A WORKER");
					}
				} catch (Exception e) {
					//System.out.println("error");
					//e.printStackTrace();
				}
				int x = toCheck.getX();
				int y = toCheck.getY();
				if (x >= 0 && x < Player.gridX && y >= 0 && y < Player.gridY && Player.gotoable[toCheck.getX()][toCheck.getY()] && !visited.contains(hash(toCheck))) {
					queue.add(toCheck);
					visited.add(hash(toCheck));
				}
			}
		}
	}

	public static void assignUnits(MapLocation rocketLoc) {
		LinkedList<MapLocation> queue = new LinkedList<MapLocation>();
		HashSet<Integer> visited = new HashSet<Integer>();
		queue.add(rocketLoc);
		visited.add(hash(rocketLoc));
		//int workersNeeded = 1;
		int rangersNeeded = 5;
		int healersNeeded = 2;
		while (!queue.isEmpty()) {
			MapLocation current = queue.poll();
			//System.out.println("HI");
			for (int i = 0; i < directions.length; i++) {
				MapLocation toCheck = current.add(directions[i]);
				//if (workersNeeded == 0 && combatUnitsNeeded == 0) {
					//return;
				//}
				if (rangersNeeded == 0 && healersNeeded == 0) {
					return;
				}
				try {
					Unit there = gc.senseUnitAtLocation(toCheck);
					UnitType temp = there.unitType();
					if (temp != UnitType.Factory && temp != UnitType.Rocket && temp != UnitType.Worker && !Player.priorityTarget.containsKey(there.id())) {
						if (temp == UnitType.Ranger) {
							rangersNeeded--;
						} else if (temp == UnitType.Healer) {
							healersNeeded--;
						}
						//set combat unit's target to this rocket
						Player.priorityTarget.put(there.id(), rocketLoc);
					}
					/*
					if (there.unitType() == UnitType.Worker) {
						workersNeeded--;
						//set worker target to this rocket
						Player.priorityTarget.put(there.id(), rocketLoc);
					} else if (there.unitType() != UnitType.Factory && there.unitType() != UnitType.Rocket) {
						combatUnitsNeeded--;
						//set combat unit's target to this rocket
						Player.priorityTarget.put(there.id(), rocketLoc);
					}*/
				} catch (Exception e) {
					//no unit there
					//e.printStackTrace();
				}
				int x = toCheck.getX();
				int y = toCheck.getY();
				if (x >= 0 && x < Player.gridX && y >= 0 && y < Player.gridY && Player.gotoable[toCheck.getX()][toCheck.getY()] && !visited.contains(hash(toCheck))) {
					queue.add(toCheck);
					visited.add(hash(toCheck));
				}
			}
		}
		System.out.println("Rip not enough units to put into rocket");
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
        if (currentDist != -1) {
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
            //bfs hasnt been run yet
            //Player.bfsMin(target, curLoc);
            if (Player.bfsMin(target, curLoc)) {
            	move(target);
            } else {
            	System.out.println("cant get there worker");
            }
        }
        if (gc.isMoveReady(curUnit.id())) {
        	//moveAnywhere();
        }
    }

	public static boolean canMove() {
		return curUnit.movementHeat() < 10;
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

	//workers, terrain, and off map are obstacles
	public static boolean checkPassable(MapLocation test) {
		int x = test.getX();
		int y = test.getY();
        if (x >= Player.gridX || y >= Player.gridY || x < 0 || y < 0) {
            return false;
        }
       	/*
        boolean allyThere = true;
        try {
            Unit temp = gc.senseUnitAtLocation(test);
            if (temp.unitType() != UnitType.Worker) {
                allyThere = false;
            }
        } catch (Exception e) {
            allyThere = false;
        }*/
        return Player.gotoable[x][y];// && !allyThere;
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

	//TODO: max(abs(x1 - x2), abs(y1 - y2))

	public static int manDistance(MapLocation first, MapLocation second) {
		int x1 = first.getX(), y1 = first.getY(), x2 = second.getX(), y2 = second.getY();
		return Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2));
	}

	public static int manDistance(int hash1, int hash2) {
		int y1 = hash1 % 69;
		int x1 = (hash1 - y1) / 69;
		int y2 = hash2 % 69;
		int x2 = (hash2 - y2) / 69;
		return Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2));
	}

	public static int distance(int hash1, int hash2) {
		int y1 = hash1 % 69;
		int x1 = (hash1 - y1) / 69;
		int y2 = hash2 % 69;
		int x2 = (hash2 - y2) / 69;
		return (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2);
	}

}