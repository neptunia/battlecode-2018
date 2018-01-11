import bc.*;

public class Rocket {

    static Unit curUnit;
    static GameController gc;
    static Direction[] directions = Direction.values();

    public static void run(GameController gc, Unit curUnit) {

        Rocket.curUnit = curUnit;

    }
}
