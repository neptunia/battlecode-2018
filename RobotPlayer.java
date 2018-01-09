import java.util.*;
import java.io.*;

public class RobotPlayer {

	static GameController gc;
	
	@SuppressWarnings("unused")
    public static void run(GameController gc) throws GameActionException {

        this.gc = gc;

        VecUnit myUnits = gc.myUnits();
        long numberOfUnits = myUnits.size();

        long currentRound = gc.round();

        //iterate through units
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
        gc.queueResearch(Unittype.Ranger);
        gc.queueResearch(Unittype.Rocket);

        //end turn
        gc.nextTurn();
	}

}