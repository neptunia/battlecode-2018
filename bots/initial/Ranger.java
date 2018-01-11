import bc.*;

import java.util.*;

public class Ranger {

    static Unit curUnit;
    static GameController gc;
    static Direction[] directions = Direction.values();
    static HashMap<Integer, HashSet<Integer>> visited = new HashMap<Integer, HashSet<Integer>>();

    public static void run(GameController gc, Unit curUnit) {

        Ranger.curUnit = curUnit;

        if (curUnit.location().isInGarrison()) {
            return;
        }

    }
}
