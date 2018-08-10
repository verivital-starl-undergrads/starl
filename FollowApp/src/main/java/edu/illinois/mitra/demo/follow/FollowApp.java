package edu.illinois.mitra.demo.follow;

/**
 * Created by VerivitalLab on 2/26/2016.
 * This app was created to test the drones. Robots will each go to a different waypoint and broadcast an arrived message.
 * Once all robots have arrived at their respective waypoints and received a message from the others, they will go to the next waypoints.
 *
 * wpt files must have keys starting at 0-A and increasing numerically #-A
 */


import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;


import edu.illinois.mitra.starl.comms.RobotMessage;
import edu.illinois.mitra.starl.gvh.GlobalVarHolder;
import edu.illinois.mitra.starl.interfaces.LogicThread;
import edu.illinois.mitra.starl.motion.MotionParameters;
import edu.illinois.mitra.starl.motion.MotionParameters.COLAVOID_MODE_TYPE;
import edu.illinois.mitra.starl.objects.ItemPosition;


public class FollowApp extends LogicThread {
    private static final String TAG = "Follow App";
    public static final int ARRIVED_MSG = 22;
    private int destIndex;
    private int messageCount = 0;
    private int numBots;
    private int numWaypoints;
    private boolean arrived = false;
    private boolean goForever = true;
    private int msgNum = 0;
    private boolean loaded = false;
    private HashSet<RobotMessage> receivedMsgs = new HashSet<RobotMessage>();

    final Map<String, ItemPosition> destinations = new HashMap<String, ItemPosition>();
    ItemPosition currentDestination;

    private enum Stage {
        INIT, PICK, GO, DONE, WAIT
    };

    private Stage stage = Stage.INIT;

    public FollowApp(GlobalVarHolder gvh) {
        super(gvh);
        MotionParameters.Builder settings = new MotionParameters.Builder();
//		settings.ROBOT_RADIUS(400);
        settings.COLAVOID_MODE(COLAVOID_MODE_TYPE.USE_COLAVOID);
        MotionParameters param = settings.build();
        gvh.plat.moat.setParameters(param);

        for(ItemPosition i : gvh.gps.getWaypointPositions())
            destinations.put(i.getName(), i);

        gvh.comms.addMsgListener(this, ARRIVED_MSG);
        destIndex = gvh.id.getIdNumber();
        numBots = gvh.id.getParticipants().size();
        Log.d(TAG,"Constructed");
    }

    @Override
    public List<Object> callStarL() {
        Log.d(TAG,"Running");
        while(true) {
            switch(stage) {
                case INIT:
                    Log.d(TAG,"Init");
                    numWaypoints = destinations.size();
                    stage = Stage.PICK;
                case PICK:
                    Log.d(TAG,"Pick");
                    arrived = false;
                    if(destinations.isEmpty() && !loaded) {
                        for(ItemPosition i : gvh.gps.getWaypointPositions())
                            destinations.put(i.getName(), i);
                        Log.d(TAG,"Waypoints Loaded " + destinations);
                        stage = Stage.PICK;
                        loaded=true;
                    } else if(destinations.isEmpty() && loaded){
                        stage = Stage.DONE;
                    } else {
//                        currentDestination = getDestination(destinations, destIndex);
//                        //Log.d(TAG, currentDestination.toString());
//                        destIndex++;
                        if(destIndex >= numWaypoints) {
                            destIndex = 0;
                        }
                        currentDestination = getDestination(destinations, destIndex);
                        //Log.d(TAG, currentDestination.toString());
                        destIndex++;
                        gvh.plat.moat.goTo(currentDestination);
                        Log.d(TAG,"Motion Automaton go to called");

                        //gvh.plat.moat.takePicture();
                        //gvh.plat.moat.takePicture();
                        stage = Stage.GO;
                    }
                    break;
                case GO:
                    Log.d(TAG,"Go");
                    if(!gvh.plat.moat.inMotion) {
                        if(!goForever) {
                            if (currentDestination != null)
                                destinations.remove(currentDestination.getName());
                        }
                        RobotMessage inform = new RobotMessage("ALL", name, ARRIVED_MSG, Integer.toString(msgNum));
                        msgNum++;
                        gvh.log.d(TAG, "At Goal, sent message");
                        gvh.comms.addOutgoingMessage(inform);
                        arrived = true;
                        //gvh.plat.moat.rotateGimbal(15);
                        //gvh.plat.moat.takePicture();
                        stage = Stage.WAIT;
                    }
                    break;
                case WAIT:
                    Log.d(TAG,"Wait");
                    if((messageCount >= numBots - 1) && arrived) {
                        messageCount = 0;
                        stage = Stage.PICK;
                    }
                    break;
                case DONE:
                    Log.d(TAG,"Done");
                    return null;
            }
            sleep(100);
        }
    }

    @Override
    protected void receive(RobotMessage m) {
        boolean alreadyReceived = false;
        for(RobotMessage msg : receivedMsgs) {

            if(msg.getFrom().equals(m.getFrom()) && msg.getContents().equals(m.getContents())) {
                alreadyReceived = true;
                break;
            }
        }
        if(m.getMID() == ARRIVED_MSG && !m.getFrom().equals(name) && !alreadyReceived) {
            gvh.log.d(TAG, "Adding to message count from " + m.getFrom());
            receivedMsgs.add(m);
            messageCount++;
        }
       /* if((messageCount == numBots) && arrived) {
            messageCount = 0;
            stage = Stage.PICK;
        }*/
    }


    @SuppressWarnings("unchecked")
    private <X, T> T getDestination(Map<X, T> map, int index) {
        // Keys must be 0-A format for this to work
        String key = Integer.toString(index) + "-A";
        System.out.println(key);
        // this is for key that is just an int, no -A
        //String key = Integer.toString(index);
        return map.get(key);
    }
}