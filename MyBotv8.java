import hlt.*;
import java.util.Map;
import java.util.TreeMap;
import java.util.HashMap;

import java.util.ArrayList;

public class MyBotv8 {
    private static GameMap gameMap = null;
    public static void main(final String[] args) {
        final Networking networking = new Networking();
        gameMap = networking.initialize("Settler 8");

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
            Map<Integer, Integer> targetedPlanets = new HashMap<Integer, Integer>();
            for(Planet p: gameMap.getAllPlanets().values())
                targetedPlanets.put(p.getId(), 0);

            for(final Ship ship: gameMap.getMyPlayer().getShips().values()) {
                Ship.DockingStatus ds = ship.getDockingStatus();

                if(ds == Ship.DockingStatus.Docked)
                    continue;

                if(ds == Ship.DockingStatus.Docking || ds == Ship.DockingStatus.Undocking){
                    targetedPlanets.put(ship.getDockedPlanet(), targetedPlanets.get(ship.getDockedPlanet())+1);
                    continue;
                }

                Move command = null;
                Map<Double, Entity> entities = gameMap.nearbyEntitiesByDistance(ship);
                Map<Double, Planet> nearbyPlanets = new TreeMap<Double, Planet>();
                TreeMap<Double, Ship> enemyShips = new TreeMap<Double, Ship>();
                Map<Double, Ship> friendlyShips = new TreeMap<Double, Ship>();
                for(double distance: entities.keySet()){
                    Entity entity = entities.get(distance);
                    if(entity instanceof Planet && !belongsToEnemy(entity))
                        nearbyPlanets.put(distance, (Planet)entity);
                    else if(entity instanceof Ship){
                        if(isFriendly(entity))
                            friendlyShips.put(distance, (Ship)entity);
                        else
                            enemyShips.put(distance, (Ship)entity);
                    }
                }
                int em = numEmptyPlanets();

                for(Planet p: nearbyPlanets.values()){
                    if(targetedPlanets.get(p.getId()) >= p.getDockingSpots())
                        continue;
                    if(ship.canDock(p) && !p.isFull())
                        command = new DockMove(ship, p);
                    else{
                        if(p.getDockedShips().size() >= p.getDockingSpots()*0.75)
                            continue;
                        command = moveToEntity(gameMap, ship, p);
                    }
                    targetedPlanets.put(p.getId(), targetedPlanets.get(p.getId()) + 1);
                    break;
                }
                if(command == null) {
                    Ship t = enemyShips.firstEntry().getValue();
                    command = moveToEntity(gameMap, ship, t);
                }
                if(command != null)
                    moveList.add(command);
            }
            Networking.sendMoves(moveList);
        }
    }
    private static boolean belongsToEnemy(Entity e){
        return !(e.getOwner() == gameMap.getMyPlayerId() || e.getOwner() == -1);
    }

    private static boolean isFriendly(Entity e){
        return e.getOwner() == gameMap.getMyPlayerId();
    }

    private static ThrustMove moveToEntity(GameMap gm, Ship source, Entity target){
        final int speed = Constants.MAX_SPEED;
        final int maxCorrections = Constants.MAX_NAVIGATION_CORRECTIONS;
        final double angularStepRad = Math.PI/180.0;
        return Navigation.navigateShipTowardsTarget(gm, source, source.getClosestPoint(target), speed, true, maxCorrections, angularStepRad);
    }
    private static int numEmptyPlanets(){
        int num = 0;
        for(Planet p : gameMap.getAllPlanets().values())
            if(p.getOwner() == -1)
                num++;
        return num;
    }
}
