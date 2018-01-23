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
	static boolean[] karbonitesLeft;
	static int[] numKarbsCounter;
	static int[] karbAmount;
	static int numWorkers = -1;
	static MapLocation[] karbonites;
	static int factsQueued = 0;
	static int replicationLimit;
	static boolean wentToMine = false;
	static HashMap<Integer, MapLocation> karboniteTargets = new HashMap<Integer, MapLocation>();
	//set of factories or rockets a worker is going to build to prevent ppl queueing the saem location
	static HashSet<Integer> structuresToBuild = new HashSet<Integer>();
	static HashMap<Integer, UnitType> structureType = new HashMap<Integer, UnitType>();
	static HashMap<Integer, Integer> prevHealth = new HashMap<Integer, Integer>();

	public static void run(Unit curUnit) {

		Worker.curUnit = curUnit;
		curPlanet = gc.planet();

		if (curUnit.location().isInGarrison()) {
			return;
		}
		curLoc = curUnit.location().mapLocation();
		if (!Player.parentWorker.containsKey(curUnit.id())) {
			for (int i = 0; i < Player.gotoable.length; i++) {
				if (Player.useful.contains(i) && Player.gotoable[i][curLoc.getX()][curLoc.getY()]) {
					Player.parentWorker.put(curUnit.id(), i);
					break;
				}
			}
		}
		//System.out.println("-------------------");
        //System.out.println(Player.timesReachedTarget);
        //System.out.println(Player.enemyLocation[Player.parentWorker.get(curUnit.id())]);
		

		if (Player.priorityTarget.containsKey(curUnit.id())) {
            MapLocation rocket = Player.priorityTarget.get(curUnit.id());
            if (gc.hasUnitAtLocation(rocket) && gc.senseUnitAtLocation(rocket).unitType() == UnitType.Rocket) {
                //System.out.println("Going to rocket!");
                move(Player.priorityTarget.get(curUnit.id()));
            } else {
                Player.priorityTarget.remove(curUnit.id());
            }
            return;
        }
        
        //System.out.println("------------------------------");
        //System.out.println(curUnit.id());
        //System.out.println(Player.parentWorker.get(curUnit.id()));
        //System.out.println(numKarbsCounter[Player.parentWorker.get(curUnit.id())]);
        //System.out.println(karbonitesLeft[Player.parentWorker.get(curUnit.id())]);
		//replicationLimit = (int) Math.max(Math.round(Math.sqrt(Math.sqrt((Player.gridX) * (Player.gridY)) * Math.sqrt(numKarbsCounter[Player.parentWorker.get(curUnit.id())]*3)) / Math.sqrt(gc.round())),Math.round(Math.sqrt((Player.gridY) * (Player.gridX)) / 1.5 / Math.sqrt(gc.round())));
        replicationLimit = Math.max((int)Math.round(Math.sqrt((Player.gridY) * (Player.gridX)) / 1.5 / Math.sqrt(gc.round())), (int) karbAmount[Player.parentWorker.get(curUnit.id())] / 2 / 40);

		if (!prevHealth.containsKey(curUnit.id())) {
		    prevHealth.put(curUnit.id(), (int) curUnit.health());
        } else {
		    if (prevHealth.get(curUnit.id()) > curUnit.health()) {
		        //run away!!!
                move(Player.startingLocation[Player.parentWorker.get(curUnit.id())]);
            }
            prevHealth.put(curUnit.id(), (int) curUnit.health());
        }
		//TODO: if on mars temporary code
		if (gc.planet() == Planet.Mars) {
			if (Worker.karbonitesLeft[Player.parentWorker.get(curUnit.id())]) {
			    goMine();
            } else {
            	move(Player.enemyLocation[Player.parentWorker.get(curUnit.id())]);
            }
            if (gc.round() > 750 || numWorkers < 6) {
            	for (int i = 0; i < directions.length; i++) {
	                if (gc.canReplicate(curUnit.id(), directions[i])) {
	                    gc.replicate(curUnit.id(), directions[i]);
	                    Player.parentWorker.put(gc.senseUnitAtLocation(curUnit.location().mapLocation().add(directions[i])).id(), Player.parentWorker.get(curUnit.id()));
	                    break;
	                }
	            }
            }
            
			return;
		}
		//System.out.println("Repl limit: " + Integer.toString(replicationLimit));
		//not enough workers - replicate
		if ((numWorkers < 6 || numWorkers < replicationLimit)) {
			replicateAnywhere();
		}

		//if i do have a target blueprint
		if (target.containsKey(curUnit.id())) {
			workOnBlueprint();
			//removeBuildStructureTarget();
			//removeKarboniteTarget();
			return;
		}

		if (gc.round() < 10 && karbonitesLeft[Player.parentWorker.get(curUnit.id())] && distance(curLoc, selectKarbonite()) <= 4) {
			goMine();
			//removeBuildStructureTarget();
			replicateAnywhere();
			return;
		}


		if (buildBlueprintLocation.containsKey(curUnit.id())) {
			buildStructure(structureType.get(curUnit.id()));
			//removeKarboniteTarget();
			return;
		}

		//System.out.println("Facts: " + Integer.toString(numFacts));
		if (numFacts < (Player.split[Player.parentWorker.get(curUnit.id())] ? 2 : 5) && gc.karbonite() >= 120 && Player.timesReachedTarget < 3) {
			buildStructure(UnitType.Factory);
			//removeKarboniteTarget();
		} else if (gc.karbonite() >= 75 && gc.researchInfo().getLevel(UnitType.Rocket) > 0) {
			buildStructure(UnitType.Rocket);
			//removeKarboniteTarget();
		} else if (karbonitesLeft[Player.parentWorker.get(curUnit.id())]) {
			goMine();
			//removeBuildStructureTarget();
		} else {
			//System.out.println("nothing to do");
			//nothing to do
			move(Player.startingLocation[Player.parentWorker.get(curUnit.id())]);
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
					curLoc = curUnit.location().mapLocation();
					return;
				}
			}
		}
	}

	public static void workOnBlueprint() {
		int targetBlueprint = target.get(curUnit.id());
		Unit toWorkOn = null;
		MapLocation blueprintLoc = null;
		try {
			toWorkOn = gc.unit(targetBlueprint);
			blueprintLoc = toWorkOn.location().mapLocation();
		} catch (Exception e) {
			//blueprint probably died
			target.remove(curUnit.id());
			Worker.run(curUnit);
		}

		UnitType blueType = toWorkOn.unitType();
		if (blueType != UnitType.Factory && blueType != UnitType.Rocket) {
			target.remove(curUnit.id());
			Worker.run(curUnit);
			return;
		}

		//already done working on
		if (toWorkOn.structureIsBuilt() != 0) {
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
				if (curUnit.abilityHeat() < 10 && numWorkers < replicationLimit) {
					//try replicating to where there's this blueprint while making others move if they can
					MapLocation temp = null;
					for (int i = 0; i < directions.length; i++) {
						temp = curLoc.add(directions[i]);
						if (distance(temp, blueprintLoc) <= 2) {
							HashSet<Integer> cantGo = new HashSet<Integer>();
							cantGo.add(hash(curLoc));
							if (makeWay(temp, cantGo, blueprintLoc) && gc.canReplicate(curUnit.id(), directions[i])) {
								gc.replicate(curUnit.id(), directions[i]);
								Unit newUnit = gc.senseUnitAtLocation(curUnit.location().mapLocation().add(directions[i]));
								int newId = newUnit.id();
                                target.put(newId, targetBlueprint);
                                Player.parentWorker.put(newId, Player.parentWorker.get(curUnit.id()));
                                int tempid = curUnit.id();
                                Worker.run(newUnit);
                                curUnit = gc.unit(tempid);
                                curLoc = curUnit.location().mapLocation();
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
									curLoc = curUnit.location().mapLocation();
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
				if (!cantGo.contains(hash(toGo)) && onMap(temp) && Player.gotoable[Player.parentWorker.get(curUnit.id())][temp.getX()][temp.getY()] && distance(temp, blueprintLoc) <= 2) {
					HashSet<Integer> tempCantGo = new HashSet<Integer>();
					tempCantGo.addAll(cantGo);
					tempCantGo.add(hash(toGo));
					if (makeWay(temp, tempCantGo, blueprintLoc)) {
						gc.moveRobot(unit.id(), directions[i]);
                        curLoc = curUnit.location().mapLocation();
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
				if (!cantGo.contains(hash(toGo)) && onMap(temp) && Player.gotoable[Player.parentWorker.get(curUnit.id())][temp.getX()][temp.getY()]) {
					HashSet<Integer> tempCantGo = new HashSet<Integer>();
					tempCantGo.addAll(cantGo);
					tempCantGo.add(hash(toGo));
					if (moveAway(temp, tempCantGo)) {
						gc.moveRobot(unit.id(), directions[i]);
                        curLoc = curUnit.location().mapLocation();
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
                int tempid = curUnit.id();
                Unit newUnit = gc.senseUnitAtLocation(curUnit.location().mapLocation().add(directions[i]));
                Player.parentWorker.put(newUnit.id(), Player.parentWorker.get(curUnit.id()));
                Worker.run(newUnit);
                curUnit = gc.unit(tempid);
                curLoc = curUnit.location().mapLocation();
				return;
			}
		}
	}

	public static void goMine() {
		wentToMine = true;
		if (!karboniteTargets.containsKey(curUnit.id())) {
			MapLocation karb = selectKarbonite();
			if (karb != null) {
				karboniteTargets.put(curUnit.id(), karb);
			} else {
				//there should be no more karbonite
				return;
			}
			
		}
		MapLocation theKarb = karboniteTargets.get(curUnit.id());
		//if im next to karbonite
		if (distance(curLoc, karboniteTargets.get(curUnit.id())) <= 2) {
			Direction directionToKarb = curLoc.directionTo(theKarb);
			if (gc.canHarvest(curUnit.id(), directionToKarb)) {
				//harvest karbonite
				gc.harvest(curUnit.id(), directionToKarb);
				karbAmount[Player.parentWorker.get(curUnit.id())] -= curUnit.workerHarvestAmount();
				Player.currentIncome += curUnit.workerHarvestAmount();
				return;
			} else if (gc.karboniteAt(theKarb) == 0) {
				//karbonite is rip
				Player.hasKarbonite[theKarb.getX()][theKarb.getY()] = false;
				karboniteTargets.remove(curUnit.id());
				numKarbsCounter[Player.parentWorker.get(curUnit.id())]--;
				Worker.run(curUnit);
			}
		} else {
			move(theKarb);
		}
	}

	public static MapLocation selectKarbonite() {
		//TODO: bfs for nearest karbonite
		LinkedList<MapLocation> queue = new LinkedList<MapLocation>();
		HashSet<Integer> visited = new HashSet<Integer>();
		queue.add(curLoc);
		visited.add(hash(curLoc));
		while (!queue.isEmpty()) {
			MapLocation current = queue.poll();
			if (Player.hasKarbonite[current.getX()][current.getY()]) {
				return current;
			}
			for (int i = 0; i < directions.length; i++) {
				MapLocation test = current.add(directions[i]);
				if (!visited.contains(hash(test)) && onMap(test) && Player.gotoable[Player.parentWorker.get(curUnit.id())][test.getX()][test.getY()]) {
					queue.add(test);
					visited.add(hash(test));
				}
			}
		}
		karbonitesLeft[Player.parentWorker.get(curUnit.id())] = false;
		return null;
	}

	//bfs to find square such that there are no other factories or rockets around range n of it
	public static MapLocation findBlueprintLocation() {
		LinkedList<MapLocation> queue = new LinkedList<MapLocation>();
		HashSet<Integer> visited = new HashSet<Integer>();
		queue.add(curLoc);
		MapLocation last = curLoc;
		
		visited.add(hash(curLoc));
		int mostSquaresFree = -1;
		MapLocation best = null;
		
		while (!queue.isEmpty()) {
			MapLocation current = queue.poll();
			if (manDistance(curLoc, current) >= 30) {
				return best;
			}
			
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
				boolean res = onMap(test);
				if (!res || res && !Player.gotoable[Player.parentWorker.get(curUnit.id())][test.getX()][test.getY()]) {
					around++;
				}
			}

			
			VecUnit nearby = gc.senseNearbyUnitsByTeam(current, 2, Player.myTeam);
			for (int i = 0; i < nearby.size(); i++) {
				UnitType temp = nearby.get(i).unitType();
				//TODO maybe add unittype.rocket too
				if (temp == UnitType.Factory || temp == UnitType.Rocket) {
					around++;
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
			if (8 - around > mostSquaresFree && !structuresToBuild.contains(hash(current))) {
				mostSquaresFree = 8 - around;
				if (mostSquaresFree == 8) {
					return current;
				}
				best = current;
			}
		}
		//System.out.println("no good position found :(");
		return best;
	}

	public static boolean goAble(int x, int y) {

		return (x >= 0 && y >= 0 && x < Player.gridX && y < Player.gridY) && Player.gotoable[Player.parentWorker.get(curUnit.id())][x][y];
	}

	public static void buildStructure(UnitType type) {
		//System.out.println("build structure");
		structureType.put(curUnit.id(), type);
		if (type == UnitType.Factory && numFacts > (Player.split[Player.parentWorker.get(curUnit.id())] ? 2 : 6)) {
			structuresToBuild.remove(hash(buildBlueprintLocation.get(curUnit.id())));
			buildBlueprintLocation.remove(curUnit.id());
			structureType.remove(curUnit.id());
			Worker.run(curUnit);
			return;
		}
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
				HashSet<Integer> temp = new HashSet<Integer>();
				temp.add(hash(curLoc));
				moveAway(blueprintLocation, temp);
			}
			if (gc.canBlueprint(curUnit.id(), type, dirToBlueprint)) {
				gc.blueprint(curUnit.id(), type, dirToBlueprint);
				Unit blueprint = gc.senseUnitAtLocation(blueprintLocation);
				Player.parentWorker.put(blueprint.id(), Player.parentWorker.get(curUnit.id()));
				UnitType temp = blueprint.unitType();
				int targetBlueprint = blueprint.id();
				buildBlueprintLocation.remove(curUnit.id());
				structureType.remove(curUnit.id());
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
			if (x >= 0 && y >= 0 && x < Player.gridX && y < Player.gridY && Player.gotoable[Player.parentWorker.get(curUnit.id())][x][y]) {
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
				if (x >= 0 && x < Player.gridX && y >= 0 && y < Player.gridY && Player.gotoable[Player.parentWorker.get(curUnit.id())][toCheck.getX()][toCheck.getY()] && !visited.contains(hash(toCheck))) {
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
		int workersNeeded = 1;
		while (!queue.isEmpty()) {
			MapLocation current = queue.poll();
			//System.out.println("HI");
			for (int i = 0; i < directions.length; i++) {
				MapLocation toCheck = current.add(directions[i]);
				//if (workersNeeded == 0 && combatUnitsNeeded == 0) {
					//return;
				//}
				int x = toCheck.getX();
				int y = toCheck.getY();
				if (x >= 0 && x < Player.gridX && y >= 0 && y < Player.gridY && Player.gotoable[Player.parentWorker.get(curUnit.id())][toCheck.getX()][toCheck.getY()] && !visited.contains(hash(toCheck))) {
					queue.add(toCheck);
					visited.add(hash(toCheck));
				}
				if (rangersNeeded == 0 && healersNeeded == 0 && workersNeeded == 0) {
					return;
				}
				if (gc.hasUnitAtLocation(toCheck)) {
					Unit there = gc.senseUnitAtLocation(toCheck);
					UnitType temp = there.unitType();
					if (!Player.priorityTarget.containsKey(there.id())) {
						if (temp == UnitType.Ranger && rangersNeeded > 0) {
							rangersNeeded--;
							Player.priorityTarget.put(there.id(), rocketLoc);
						} else if (temp == UnitType.Healer && healersNeeded > 0) {
							healersNeeded--;
							Player.priorityTarget.put(there.id(), rocketLoc);
						} else if (temp == UnitType.Worker && workersNeeded > 0) {
							workersNeeded--;
							Player.priorityTarget.put(there.id(), rocketLoc);
						}
					}
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
            //bfs hasnt been run yet
            //Player.bfsMin(target, curLoc);
            if (Player.bfsMin(target, curLoc)) {
            	move(target);
            } else {
            	System.out.println("cant get there worker");
            }
        }
        if (gc.isMoveReady(curUnit.id())) {
        	Player.blockedCount++;
        	moveCloser(target);
        }
        curLoc = curUnit.location().mapLocation();
    }

    public static boolean moveCloser(MapLocation enemy) {
        int best = 99999999;
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
        return Player.gotoable[Player.parentWorker.get(curUnit.id())][x][y];// && !allyThere;
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