import bc.*;
import java.util.*;

public class Worker {

	static Unit curUnit;
	static GameController gc;
	static Direction[] directions = Direction.values();
	static HashMap<Integer, HashSet<Integer>> visited = new HashMap<Integer, HashSet<Integer>>();
	//target blueprint to work on for each worker
	static HashMap<Integer, Integer> target = new HashMap<Integer, Integer>();
	static HashMap<Integer, MapLocation> buildBlueprintLocation = new HashMap<Integer, MapLocation>();
	static int rocketsBuilt = 0;
	static int rocketBlueprintId = -1;
	static int numFacts = 0;
	static boolean karbonitesLeft = true;
	static int numWorkers = -1;
	static MapLocation[] karbonites;
	static MapLocation curLoc;
	static int numKarbsCounter = 0;
	static HashMap<Integer, MapLocation> karboniteTargets = new HashMap<Integer, MapLocation>();
	static HashMap<Integer, MapLocation> lastStructure = new HashMap<Integer, MapLocation>();

	public static void run(GameController gc, Unit curUnit) {

		//Note: with bugmove, whenever a unit changes target, then prevLocation should remove their unit id from its keys

		Worker.curUnit = curUnit;

		if (curUnit.location().isInGarrison()) {
			return;
		}

		curLoc = curUnit.location().mapLocation();

		//TODO: if on mars temporary code
		if (gc.planet() == Planet.Mars) {
			move(Player.enemyLocation);
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
			//rush first rocket
			if (gc.karbonite() >= 75 && Worker.rocketsBuilt == 0 && Worker.rocketBlueprintId == -1 && gc.researchInfo().getLevel(UnitType.Rocket) > 0) {
				buildStructure(UnitType.Rocket);
			}
			// count number of factories and number of workers
			VecUnit units = gc.myUnits();
			if (numWorkers == -1) {
				numWorkers = 0;
				for (int i = 0; i < units.size(); i++) {
					if (units.get(i).unitType() == UnitType.Worker) {
						numWorkers++;
					}
				}
			}

			// if we need more factories:
			if (numWorkers >= numFacts && gc.karbonite() >= 100) {
				buildStructure(UnitType.Factory);
			}
			// if we need more workers:
			else if (numWorkers < numFacts && gc.karbonite() >= 15 && curUnit.abilityHeat() < 10) {
				for (int i = 0; i < directions.length; i++) {
					if (gc.canReplicate(curUnit.id(), directions[i])) {
						gc.replicate(curUnit.id(), directions[i]);
						// since i replicated, increase number of workers
						numWorkers++;
						return;
					}
				}
			}

			else if (Player.prevIncome < 10 && karbonitesLeft) {
				// go mine
				goMine();
			}

			else if (numWorkers < numFacts && gc.karbonite() >= 75) {
				// build a rocket (but only if we don't need another factory)
				buildStructure(UnitType.Rocket);
			} else if (karbonitesLeft) {
				goMine();
			} else {
				//go back near base
				//blocking other people
				//int tempX = curLoc.getX();
				//int tempY = curLoc.getY();
				//if ((goAble(tempX - 1, tempY) || goAble(tempX + 1, tempY)) && (goAble(tempX, tempY - 1) || goAble(tempX, tempY + 1))) {
					move(Player.startingLocation);
				//}
				
			}
			
		}
		return;
	}

	public static void goMine() {
		//if already have a karbonite target
		if (karboniteTargets.get(curUnit.id()) != null && karboniteTargets.containsKey(curUnit.id())) {
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
		MapLocation newTarget = selectKarbonite();
		if (newTarget == null) {
			return;
		}
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
			karbonitesLeft = false;
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

	//bfs to find square such that there are no other factories or rockets around range n of it
	public static void findBlueprintLocation(UnitType type) {
		LinkedList<MapLocation> queue = new LinkedList<MapLocation>();
		HashSet<Integer> visited = new HashSet<Integer>();
		int curHash = hash(curLoc);
		if (lastStructure.containsKey(curUnit.id())) {
			queue.add(lastStructure.get(curUnit.id()));
		} else {
			queue.add(curLoc);
		}
		
		visited.add(curHash);
		
		while (!queue.isEmpty()) {
			MapLocation current = queue.poll();
			
			for (int i = 0; i < directions.length; i++) {
				MapLocation test = current.add(directions[i]);
				int testHash = hash(test);
				if (!visited.contains(testHash) && checkPassable2(test)) {
					visited.add(testHash);
					queue.add(test);
				}
				
			}
			long around = gc.senseNearbyUnitsByTeam(current, 2, Player.myTeam).size() - 1;
			int tempX = current.getX();
			int tempY = current.getY();
			//on the map and gotoable
			if (around <= 0 && curHash != hash(current) && (goAble(tempX - 1, tempY) || goAble(tempX + 1, tempY)) && (goAble(tempX, tempY - 1) || goAble(tempX, tempY + 1))) {
				buildBlueprintLocation.put(curUnit.id(), current);
				return;
			}
		}
		System.out.println("no good position found :(");
	}

	public static boolean goAble(int x, int y) {

		return (x >= 0 && y >= 0 && x < Player.gridX && y < Player.gridY) && Player.gotoable[x][y];
	}

	//make sure workers dont get stuck between a factory and rocket
	public static boolean goAble2(int x, int y) {
		try {
			Unit asdf = gc.senseUnitAtLocation(new MapLocation(gc.planet(), x, y));
			if (asdf.unitType() == UnitType.Factory || asdf.unitType() == UnitType.Rocket || asdf.unitType() == UnitType.Worker) {
				return false;
			}
		} catch (Exception e) {};
		return (x >= 0 && y >= 0 && x < Player.gridX && y < Player.gridY) && Player.gotoable[x][y];
	}

	public static void buildStructure(UnitType type) {
		System.out.println("build structure");
		if (!buildBlueprintLocation.containsKey(curUnit.id())) {
			findBlueprintLocation(type);
		}
		MapLocation blueprintLocation = buildBlueprintLocation.get(curUnit.id());
		System.out.println("Blueprint coords: " + Integer.toString(blueprintLocation.getX()) + ", " + Integer.toString(blueprintLocation.getY()));
		Direction dirToBlueprint = curLoc.directionTo(blueprintLocation);
		//if i can build it
		if (distance(curLoc, blueprintLocation) <= 2 && gc.canBlueprint(curUnit.id(), type, dirToBlueprint)) {
			gc.blueprint(curUnit.id(), type, dirToBlueprint);
			lastStructure.put(curUnit.id(), blueprintLocation);
			buildBlueprintLocation.remove(curUnit.id());
			int targetBlueprint = gc.senseUnitAtLocation(curUnit.location().mapLocation().add(dirToBlueprint)).id();
			if (type == UnitType.Rocket) {
				rocketBlueprintId = targetBlueprint;
				rocketsBuilt++;
			}
            if (type == UnitType.Factory) {
                numFacts++;
            }
			//tell all nearby workers to go work on it
			VecUnit nearby = gc.senseNearbyUnitsByTeam(curUnit.location().mapLocation(), 4, Player.myTeam);
			for (int a = 0; a < nearby.size(); a++) {
				Unit temp = nearby.get(a);
				if (temp.unitType() == UnitType.Worker) {
					target.put(temp.id(), targetBlueprint);
				}
			}
		} else {
			//move towards it
			move(blueprintLocation);
		}
	}

    //pathing
    //move towards target location
    public static void move(MapLocation target) {
        int startHash = hash(curLoc);
        int goal = hash(target);
        if (!gc.isMoveReady(curUnit.id()) || startHash == goal) {
            return;
        }
        //a*
        int movingTo = doubleHash(curLoc, target);
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



            gScore.put(startHash, 0);
            fScore.put(startHash, manDistance(curLoc, target));
            openList.offer(startHash);
            while (!openList.isEmpty()) {
                int current = openList.poll();

                int remainingPath = doubleHash(current, goal);
                if (Player.paths.containsKey(remainingPath)) {
                    //TODO: optimization once the killed has been fixed
                    //completes the path
                    int tempCur = current;
                    while (tempCur != goal) {
                        int tempHash = doubleHash(tempCur, goal);
                        int nextNode = Player.paths.get(tempHash);
                        fromMap.put(nextNode, tempCur);
                        tempCur = nextNode;
                    }
                    current = goal;
                }

                if (current == goal) {
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

                int tempY = current % 69;
                int tempX = (current - tempY) / 69;
                curLoc = new MapLocation(gc.planet(), tempX, tempY);

                //System.out.println("Node im on " + print(current));

                closedList.add(current);

                //iterate through neighbors
                for (int i = 0; i < directions.length; i++) {
                    int neighbor = hash(curLoc.add(directions[i]));
                    //if a path is already computed for this node to the goal then dont needa compute more

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

        if (!Player.paths.containsKey(movingTo)) {
            System.out.println("wot borked work move");
            System.out.println("Enemy Location: " + Integer.toString(Player.enemyLocation.getX()) + " " + Integer.toString(Player.enemyLocation.getY()));
            System.out.println("Cur location: " + Integer.toString(curLoc.getX()) + " " + Integer.toString(curLoc.getY()));
            System.out.println("Target Location: " + Integer.toString(target.getX()) + " " + Integer.toString(target.getY()));
            return;
        }

        int toMove = Player.paths.get(movingTo);

        int y = toMove % 69;
        int x = (toMove - y) / 69;

        MapLocation next = new MapLocation(gc.planet(), x, y);
        Direction temp = curUnit.location().mapLocation().directionTo(next);
        if (gc.canMove(curUnit.id(), temp)) {
            gc.moveRobot(curUnit.id(), temp);
        } else {
            //blocked by something
            MapLocation tryToGoTo = curUnit.location().mapLocation().add(temp);
            Unit blockedBy = gc.senseUnitAtLocation(tryToGoTo);
            if (blockedBy.unitType() == UnitType.Factory || blockedBy.unitType() == UnitType.Rocket || blockedBy.unitType() == UnitType.Worker) {
                //if im not blocked by an attacking unit, then move aside
                moveAttack(next);
            }
        }
    }

	public static boolean moveAttack(MapLocation target) {
        //greedy pathfinding
        int smallest = 999999;
        Direction d = null;
        int curDist = distance(curUnit.location().mapLocation(), target);
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
            if (!visited.get(curUnit.id()).contains(hash(newSquare.getX(), newSquare.getY())) && gc.canMove(curUnit.id(), directions[i]) && dist < smallest && dist < curDist) {
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
        if (test.getX() >= Player.gridX || test.getY() >= Player.gridY || test.getX() < 0 || test.getY() < 0) {
            return false;
        }
        boolean allyThere = true;
        try {
            Unit temp = gc.senseUnitAtLocation(test);
            if (temp.unitType() != UnitType.Worker) {
                allyThere = false;
            }
        } catch (Exception e) {
            allyThere = false;
        }
        return Player.planetMap.isPassableTerrainAt(test) == 1 && !allyThere;
    }

    //workers are not obstacles
    public static boolean checkPassable2(MapLocation test) {
        if (test.getX() >= Player.gridX || test.getY() >= Player.gridY || test.getX() < 0 || test.getY() < 0) {
            return false;
        }
        boolean allyThere = true;
        try {
            Unit temp = gc.senseUnitAtLocation(test);
            if (temp.unitType() == UnitType.Worker) {
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