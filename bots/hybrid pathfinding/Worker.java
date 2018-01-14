import bc.*;
import java.util.*;

public class Worker {

	static Unit curUnit;
	static GameController gc;
	static Direction[] directions = Direction.values();
	static HashMap<Integer, HashSet<Integer>> visited = new HashMap<Integer, HashSet<Integer>>();
	//target blueprint to work on for each worker
	static HashMap<Integer, Integer> target = new HashMap<Integer, Integer>();
	//bugpathing storing previous square (unit id, hash of location)
	static HashMap<Integer, Integer> prevLocation = new HashMap<Integer, Integer>();
	static int rocketsBuilt = 0;
	static int rocketBlueprintId = -1;
	static int numFacts = -1;
	static int numWorkers = -1;
	static MapLocation[] karbonites;
	static int numKarbsCounter = 0;
	static HashMap<Integer, MapLocation> karboniteTargets = new HashMap<Integer, MapLocation>();

	public static void run(GameController gc, Unit curUnit) {

		//Note: with bugmove, whenever a unit changes target, then prevLocation should remove their unit id from its keys

		Worker.curUnit = curUnit;

		if (curUnit.location().isInGarrison()) {
			return;
		}

		if (Player.firstTime) {
			Player.firstTime = false;
			//guesstimate enemy location
			if (Player.enemyLocation == null) {
				MapLocation temp = curUnit.location().mapLocation();
				Player.startingLocation = temp;
				Player.enemyLocation = new MapLocation(gc.planet(), Player.gridX - temp.getX(), Player.gridY - temp.getY());
			}
		}

		if (gc.round() == 1) {
			//Initial replication
			for (int i = 0; i < directions.length; i++) {
				if (gc.canReplicate(curUnit.id(), directions[i])) {
					gc.replicate(curUnit.id(), directions[i]);
					break;
				}
			}
		}

		//TODO worker AI: implement bfs, run away if enemies are too strong, and maybe improve when to replicate
		//remove target if factory already died
		try {
			if (target.containsKey(curUnit.id())) {
				gc.unit(target.get(curUnit.id()));
			}
		} catch (Exception e) {
			target.remove(curUnit.id());
		}

		boolean doingAThing = false;
		//if i do have a target blueprint
		if (target.containsKey(curUnit.id())) {
			int targetBlueprint = target.get(curUnit.id());
			Unit toWorkOn = gc.unit(targetBlueprint);
			//already done working on
			if (toWorkOn.health() == toWorkOn.maxHealth()) {
				target.remove(curUnit.id());
			} else {
				//goto it and build it
				MapLocation blueprintLoc = toWorkOn.location().mapLocation();
				MapLocation curLoc = curUnit.location().mapLocation();
				if (distance(blueprintLoc, curLoc) <= 2) {
					//next to it, i can work on it
					if (!buildBlueprint(targetBlueprint)) {
						System.out.println("SUM TING WONG :(");
					} else {
						doingAThing = true;
					}
				} else {
					//move towards it
					if (canMove()) {
						move(blueprintLoc);
						doingAThing = true;
					}
				}
			}
		}

		//if worker is idle
		if (!doingAThing) {
			// count number of factories and number of workers
			VecUnit units = gc.myUnits();
			if (numFacts == -1) {
				numFacts = 0;
				numWorkers = 0;
				for (int i = 0; i < units.size(); i++) {
					if (units.get(i).unitType() == UnitType.Factory) {
						numFacts++;
					} else if (units.get(i).unitType() == UnitType.Worker) {
						numWorkers++;
					}
				}
			}

			// if we need more factories:
			if (numWorkers >= 2 * numFacts && gc.karbonite() >= 100) {
				buildStructure(UnitType.Factory);
			}
			// if we need more workers:
			else if (numWorkers < 2 * numFacts && gc.karbonite() >= 15 && gc.karbonite() <75 && curUnit.abilityHeat() < 10) {
				for (int i = 0; i < directions.length; i++) {
					if (gc.canReplicate(curUnit.id(), directions[i])) {
						gc.replicate(curUnit.id(), directions[i]);
						// since i replicated, increase number of workers
						numWorkers++;
						return;
					}
				}
			}

			else if (Player.prevIncome < 0) {
				// go mine
				goMine();
			}

			else if (numWorkers < 2 * numFacts && gc.karbonite() >= 75) {
				// build a rocket (but only if we don't need another factory)
				buildStructure(UnitType.Rocket);
			} else {
				goMine();
			}
			
		}
		return;
	}

	public static void goMine() {
		MapLocation curLoc = curUnit.location().mapLocation();
		//if already have a karbonite target
		if (karboniteTargets.containsKey(curUnit.id()) && karboniteTargets.get(curUnit.id()) != null) {
			MapLocation theKarb = karboniteTargets.get(curUnit.id());
			if (distance(curLoc, karboniteTargets.get(curUnit.id())) <= 2) {
				//im next to it
				Direction directionToKarb = curLoc.directionTo(theKarb);
				System.out.println("first canharvest: " + Boolean.toString(gc.canHarvest(curUnit.id(), directionToKarb)));
				if (gc.canHarvest(curUnit.id(), directionToKarb)) {
					System.out.println("harvest");
					gc.harvest(curUnit.id(), directionToKarb);
					Player.currentIncome += curUnit.workerHarvestAmount();
					return;
				} else {
					MapLocation rem = karboniteTargets.get(curUnit.id());
					karboniteTargets.remove(curUnit.id());
					for (int i = 0; i < karbonites.length; i++) {
						if (karbonites[i] != null && hash(rem) == hash(karbonites[i])) {
							karbonites[i] = null;
							break;
						}
					}
				}
			} else {
				//dont have a target :(
				//select it, then move to it
				takeCareOfKarbonite();
			}
		} else {
			takeCareOfKarbonite();
		}
	}

	public static void takeCareOfKarbonite() {
		MapLocation curLoc = curUnit.location().mapLocation();
		MapLocation newTarget = selectKarbonite();
		karboniteTargets.put(curUnit.id(), newTarget);
		if (distance(curLoc, karboniteTargets.get(curUnit.id())) <= 2) {
			Direction directionToKarb = curLoc.directionTo(newTarget);
			System.out.println(gc.canHarvest(curUnit.id(), directionToKarb));
			if (gc.canHarvest(curUnit.id(), directionToKarb)) {
				System.out.println("harvest!!");
				gc.harvest(curUnit.id(), directionToKarb);
				Player.currentIncome += curUnit.workerHarvestAmount();
			} else {
				MapLocation rem = karboniteTargets.get(curUnit.id());
				for (int i = 0; i < karbonites.length; i++) {
					if (karbonites[i] != null && hash(rem) == hash(karbonites[i])) {
						karbonites[i] = null;
						break;
					}
				}
			}
		} else {
			move(karboniteTargets.get(curUnit.id()));
		}
	}

	public static MapLocation selectKarbonite() {
		int smallest = 9999999;
		MapLocation karb = null;
		MapLocation curLoc = curUnit.location().mapLocation();
		for (int i = 0; i < numKarbsCounter; i++) {
			if (karbonites[i] != null) {
				int dist = distance(curLoc, karbonites[i]);
				if (karbonites[i] != null && dist < smallest && Player.planetMap.initialKarboniteAt(karbonites[i]) > 0) {
					smallest = dist;
					karb = karbonites[i];
				}
			}
		}
		if (karb == null) {
			System.out.println("rip no karbonites");
		}
		return karb;
	}

	//build blueprint given structure unit id
	public static boolean buildBlueprint(int id) {
		if (gc.canBuild(curUnit.id(), id)) {
			gc.build(curUnit.id(), id);
			return true;
		}
		return false;
	}

	//builds a factory in an open space around worker
	//TODO improve factory building scheme
	public static void buildStructure(UnitType type) {
		int numDirections = 0;
		for (int i = 0; i < directions.length - 1; i++) {
			if (gc.canMove(curUnit.id(), directions[i])) {
				numDirections++;
			}
		}
		for (int i = 0; numDirections >= 2 && i < directions.length - 2; i++) {
			if (gc.canBlueprint(curUnit.id(), type, directions[i])) {
				gc.blueprint(curUnit.id(), type, directions[i]);
				int targetBlueprint = gc.senseUnitAtLocation(curUnit.location().mapLocation().add(directions[i])).id();
				if (type == UnitType.Rocket) {
					rocketBlueprintId = targetBlueprint;
					rocketsBuilt++;
				}
				//tell all nearby workers to go work on it
				//TODO maybe bfs within n range for workers to work on the factory
				VecUnit nearby = gc.senseNearbyUnits(curUnit.location().mapLocation(), 4);
				for (int a = 0; a < nearby.size(); a++) {
					Unit temp = nearby.get(a);
					if (temp.team() == gc.team() && temp.unitType() == UnitType.Worker) {
						//if changing target of a unit
						if (prevLocation.containsKey(temp.id()) && prevLocation.get(temp.id()) != targetBlueprint) {
							prevLocation.remove(temp.id());
						}
						target.put(temp.id(), targetBlueprint);
					}
				}
				break;
			}
		}
	}

	//move towards target location
	public static void move(MapLocation target) {
		//a*
		int movingTo = doubleHash(curUnit.location().mapLocation(), target);
		if (!Player.paths.containsKey(movingTo)) {
			HashSet<Integer> closedList = new HashSet<Integer>();
			HashMap<Integer, Integer> gScore = new HashMap<Integer, Integer>();
			HashMap<Integer, Integer> fScore = new HashMap<Integer, Integer>();
			HashMap<Integer, Integer> fromMap = new HashMap<Integer, Integer>();
			PriorityQueue<Integer> openList = new PriorityQueue<Integer>(11, new Comparator<Integer>() {
			public int compare(Integer nodeA, Integer nodeB) {
				return Integer.compare(fScore.get(nodeA), fScore.get(nodeB));
			}
		});

			MapLocation curLoc = curUnit.location().mapLocation();

			int startHash = hash(curLoc);

			gScore.put(startHash, 0);
			fScore.put(startHash, manDistance(curLoc, target));
			openList.offer(startHash);

			int goal = hash(target);

			while (!openList.isEmpty()) {
				int current = openList.poll();

				int tempY = current % 69;
				int tempX = (current - tempY) / 69;
				curLoc = new MapLocation(gc.planet(), tempX, tempY);
				
				//System.out.println("Node im on " + print(current));

				closedList.add(current);

				//iterate through neighbors
				for (int i = 0; i < directions.length; i++) {
					int neighbor = hash(curLoc.add(directions[i]));
					//if a path is already computed for this node to the goal then dont needa compute more
					int neighborPath = doubleHash(neighbor, hash(target));
					if (Player.paths.containsKey(neighborPath)) {
						//TODO: optimization once the killed has been fixed
						//curloc to neighbor matched within themselves
						//curloc to neighbor matched with paths[neighborPath] to goal

					}
					if (neighbor == goal) {
						fromMap.put(neighbor, current);
						HashMap<Integer, Integer> path = new HashMap<Integer, Integer>();
						HashMap<Integer, Integer> path2 = new HashMap<Integer, Integer>();
						int next = goal;

						int prev = -1;
						ArrayList<Integer> before = new ArrayList<Integer>();
						before.add(next);
						while (fromMap.containsKey(next)) {
							//System.out.println(print(next));
							//path.put(next, prev);
							prev = next;
							next = fromMap.get(prev);
							before.add(next);
							//Player.paths.put(doubleHash(prev, next), path);
							//Player.paths.put(doubleHash(next, prev), path);
							//TODO put in between Player.paths... a b c d e needs bc, bd, cd 
							path.put(next, prev);
							path2.put(prev, next);
						}
						int temp = before.size();
						for (int j = 0; j < temp; j++) {
							for (int a = 0; a < j; a++) {
								Player.paths.put(doubleHash(before.get(j), before.get(a)), path.get(before.get(j)));
								Player.paths.put(doubleHash(before.get(a), before.get(j)), path2.get(before.get(a)));
							}
						}
						
						break;
					}
					if (checkPassable(curLoc.add(directions[i]))) {
						if (closedList.contains(neighbor)) {
							continue;
						}

						int tentG = gScore.get(current) + 1;

						boolean contains = openList.contains(neighbor);
						if (!contains || tentG < gScore.get(neighbor)) {
							gScore.put(neighbor, tentG);
							fScore.put(neighbor, tentG + manDistance(neighbor, hash(target.getX(), target.getY())));

							if (contains) {
								openList.remove(neighbor);
							}

							openList.offer(neighbor);
							//System.out.println("Add: " + print(neighbor));
							fromMap.put(neighbor, current);
						}
					}
				}
			}
		}
		//System.out.println(hash(curUnit.location().mapLocation()));
		//System.out.println(Arrays.asList(Player.paths.get(movingTo)));
		//System.out.println(Player.paths.get(movingTo).containsKey(hash(curUnit.location().mapLocation())));

		int toMove = Player.paths.get(movingTo);

		int y = toMove % 69;
		int x = (toMove - y) / 69;
		
		MapLocation next = new MapLocation(gc.planet(), x, y);
		Direction temp = curUnit.location().mapLocation().directionTo(next);
		if (gc.canMove(curUnit.id(), temp) && gc.isMoveReady(curUnit.id())) {
			gc.moveRobot(curUnit.id(), temp);
		} else {
			//System.out.println("Darn");
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

	public static boolean checkPassable(MapLocation test) {
		if (test.getX() >= Player.gridX || test.getY() >= Player.gridY || test.getX() < 0 || test.getY() < 0) {
			return false;
		}
		return Player.planetMap.isPassableTerrainAt(test) == 1;
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

}