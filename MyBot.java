import hlt.*;
import java.util.Map;
import java.util.TreeMap;
import java.util.HashMap;

import java.util.ArrayList;

public class MyBot {
    private static GameMap gameMap = null;
    public static void main(final String[] args) {
        final Networking networking = new Networking();
        gameMap = networking.initialize("Defender 2");

        final ArrayList<Move> moveList = new ArrayList<>();
        for (;;) {
            moveList.clear();
            networking.updateMap(gameMap);
            Map<Integer, Integer> targetedPlanets = new HashMap<Integer, Integer>();
            Map<Integer, Integer> targetedShips = new HashMap<Integer, Integer>();
            for(Planet p: gameMap.getAllPlanets().values())
                targetedPlanets.put(p.getId(), 0);
            for(Ship s: gameMap.getAllShips())
                if(belongsToEnemy(s))
                    targetedShips.put(s.getId(), 0);

            for(final Ship ship: gameMap.getMyPlayer().getShips().values()) {
                Move command = null;
                Ship nearestAvailFriendly = null;
                Ship nearestDockedFriendly = null;
                Map<Double, Entity> entities = gameMap.nearbyEntitiesByDistance(ship);
                /*
                if(ship.getDockingStatus() != Ship.DockingStatus.Undocked) {
                    Ship nearestEnemy = null;
                    double distToEnemy = 0;
                    for (double distance : entities.keySet()){
                        Entity e = entities.get(distance);
                        if (e instanceof Ship && belongsToEnemy(e)) {
                            nearestEnemy = (Ship) e;
                            distToEnemy = distance;
                            break;
                        }
                    }
                    if(nearestEnemy != null && distToEnemy < 42 && nearestEnemy.getDockingStatus() == Ship.DockingStatus.Undocked && targetedShips.get(nearestEnemy.getId()) == 0){
                        command = new UndockMove(ship);
                        moveList.add(command);
                        targetedShips.put(nearestEnemy.getId(), targetedShips.get(nearestEnemy.getId()) + 1);
                    }
                    else
                        targetedPlanets.put(ship.getDockedPlanet(), targetedPlanets.get(ship.getDockedPlanet())+1);
                    continue;
                }*/
                if(ship.getDockingStatus() != Ship.DockingStatus.Undocked){
                    targetedPlanets.put(ship.getDockedPlanet(), targetedPlanets.get(ship.getDockedPlanet())+1);
                    continue;
                }

                for(double distance: entities.keySet()){
                    Entity entity = entities.get(distance);
                    if(entity instanceof Planet && !belongsToEnemy(entity)){
                        if(targetedPlanets.get(entity.getId()) >= ((Planet)entity).getDockingSpots())
                            continue;
                        if(ship.canDock((Planet)entity))
                            command = new DockMove(ship, (Planet)entity);
                        else if( ((Planet)entity).getDockedShips().size() < ((Planet)entity).getDockingSpots() )
                            command = Navigation.navigateShipToDock(gameMap, ship, entity, Constants.MAX_SPEED);
                        targetedPlanets.put(entity.getId(), targetedPlanets.get(entity.getId()) + 1);
                        break;
                    }
                    else if(entity instanceof Ship){
                        if(isFriendly(entity)) {
                            if ( ((Ship) entity).getDockingStatus() == Ship.DockingStatus.Undocked && nearestAvailFriendly == null)
                                nearestAvailFriendly = (Ship) entity;
                            else if( ((Ship) entity).getDockingStatus() != Ship.DockingStatus.Undocked && nearestDockedFriendly == null)
                                nearestDockedFriendly = (Ship) entity;
                        }
                        else if(belongsToEnemy(entity) && targetedShips.get(entity.getId()) < 4){
                            command = moveToEntity(gameMap, ship, entity);
                            targetedShips.put(entity.getId(), targetedShips.get(entity.getId())+1);
                            break;
                        }
                    }
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
