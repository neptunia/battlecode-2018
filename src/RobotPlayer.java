import java.util.*;
import java.io.*;

public class RobotPlayer {

	static GameController gc;
	
	public static void main(String args[]) {

        GameController gc = new GameController();

                
        while (true) {
            long currentRound = gc.round();
            VecUnit myUnits = gc.myUnits();
            long numberOfUnits = myUnits.size();

                    

            //iterate through units
            // might need to fix this later; what happens if I create a new unit in the middle of this loop?
            for (int i = 0; i < numberOfUnits; i++) {
                Unit curUnit = myUnits.get(i);
                //perform unit task based on unit type
                switch (curUnit.unitType()) {
                    case UnitType.Factory:
                    	Factory.run(gc, curUnit);
                    case UnitType.Healer:
                    	Healer.run(gc, curUnit);
                    case UnitType.Knight:
                    	Knight.run(gc, curUnit);
                    case UnitType.Mage:
                    	Mage.run(gc, curUnit);
                    case UnitType.Ranger:
                    	Ranger.run(gc, curUnit);
                    case UnitType.Rocket:
                    	Rocket.run(gc, curUnit);
                    case UnitType.Worker:
                    	Worker.run(gc, curUnit);
                }
            }

            //do research
            gc.queueResearch(UnitType.Ranger);
            gc.queueResearch(UnitType.Rocket);

            //end turn
            gc.nextTurn();
        }
	}
}