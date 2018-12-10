import hlt.*;
import java.util.Map;
import java.util.TreeMap;
import java.util.HashMap;

import java.util.ArrayList;

public class Attacker {
	// tests immediate attacks
    public static void main(final String[] args) {
        final Networking networking = new Networking();
        final GameMap gameMap = networking.initialize("Attacker");

        // We now have 1 full minute to analyse the initial map.
        final String initialMapIntelligence =
                "width: " + gameMap.getWidth() +
                "; height: " + gameMap.getHeight() +
                "; players: " + gameMap.getAllPlayers().size() +
                "; planets: " + gameMap.getAllPlanets().size();
        Log.log(initialMapIntelligence);

        final ArrayList<Move> moveList = new ArrayList<>();
        for (;;) {
            moveList.clear();
            networking.updateMap(gameMap);

            Map<Integer, Integer> targetedShips = new HashMap<Integer, Integer>();

            for(final Ship ship: gameMap.getMyPlayer().getShips().values()) {
                Map<Double, Entity> entities = gameMap.nearbyEntitiesByDistance(ship);
                Map<Double, Ship> enemyShips = new TreeMap<Double, Ship>();
                for(double distance: entities.keySet()){
                    Entity entity = entities.get(distance);
                    if(entity instanceof Ship && !isFriendly(gameMap, entity)) {
                        enemyShips.put(distance, (Ship) entity);
                        targetedShips.put(entity.getId(), 0);
                    }
                }
                for(final double distance: enemyShips.keySet()){
                    Ship enemy = enemyShips.get(distance);
                    if(enemy.getDockingStatus() != Ship.DockingStatus.Undocked && targetedShips.get(enemy.getId()) == 0) {
                        final ThrustMove nav = moveToShip(gameMap, ship, enemy);
                        if (nav != null) {
                            moveList.add(nav);
                            targetedShips.put(enemy.getId(), targetedShips.get(enemy.getId())+1);
                            break;
                        }
                    }
                }
            }
            Networking.sendMoves(moveList);
        }
    }
    private static boolean isFriendly(GameMap gm, Entity e){
        return e.getOwner() == gm.getMyPlayerId();
    }

    private static ThrustMove moveToShip(GameMap gm, Ship source, Entity target){
        final int speed = Constants.MAX_SPEED;
        final int maxCorrections = Constants.MAX_NAVIGATION_CORRECTIONS;
        final double angularStepRad = Math.PI/180.0;
        return Navigation.navigateShipTowardsTarget(gm, source, source.getClosestPoint(target), speed, true, maxCorrections, angularStepRad);
    }
}
