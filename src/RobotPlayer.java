import java.util.*;
import java.io.*;
import bc.*;
import bc.UnitType.*;

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
                    case Factory:
                    	Factory.run(gc, curUnit);
                    	break;
                    //case UnitType.Healer:
                    //	Healer.run(gc, curUnit);
                    //  break;
                    case Knight:
                    	Knight.run(gc, curUnit);
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