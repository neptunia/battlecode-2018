import java.util.*;
import java.io.*;
import bc.*;

public class RobotPlayer {
	
	public static void main(String args[]) {
        try {
            GameController gc = new GameController();


            while (true) {
                try {
                    long currentRound = gc.round();
                    VecUnit myUnits = gc.myUnits();
                    long numberOfUnits = myUnits.size();

                    //iterate through units
                    // might need to fix this later; what happens if I create a new unit in the middle of this loop?
                    for (int i = 0; i < numberOfUnits; i++) {
                        Unit curUnit = myUnits.get(i);
                        //perform unit task based on unit type
                        switch (curUnit.unitType()) {
                            case Factory:
                                try {
                                    Factory.run(gc, curUnit);
                                } catch (Exception e) {
                                    System.out.println("Factory ded");
                                }
                                break;
                            //case UnitType.Healer:
                            //	Healer.run(gc, curUnit);
                            //  break;
                            case Knight:
                                try {
                                    Knight.run(gc, curUnit);
                                } catch (Exception e) {
                                    System.out.println("knight ded");
                                }
                                break;
                            //case UnitType.Mage:
                            //	Mage.run(gc, curUnit);
                            //  break;
                            //case UnitType.Ranger:
                            //	Ranger.run(gc, curUnit);
                            //  break;
                            //case UnitType.Rocket:
                            //	Rocket.run(gc, curUnit);
                            //  break;
                            case Worker:
                                try {
                                    Worker.run(gc, curUnit);
                                } catch (Exception e) {
                                    System.out.println("worker ded");
                                }
                        }
                    }

                    //do research
                    try {
                        gc.queueResearch(UnitType.Ranger);
                        gc.queueResearch(UnitType.Rocket);
                    } catch (Exception e) {
                        System.out.println("research ded");
                    }

                    //end turn
                } catch (Exception e) {
                    System.out.println("fuck me");
                }
                gc.nextTurn();
            }
        } catch (Exception e) {
            System.out.println("even worse");
        }
    }

}