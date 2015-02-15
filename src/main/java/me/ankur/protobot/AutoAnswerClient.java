package me.ankur.protobot;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Sundara on 2/13/15.
 */
public class AutoAnswerClient extends ProtobowlClient {
    /**
     * Instantiate a new protobowl client
     *
     * @param name name
     * @param room room
     */
    public AutoAnswerClient(String name, String room) {
        super(name, room);
    }

    @Override
    public void onUpdateQuestion() {
        /*new Timer().schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        System.out.println(">>> BUZZ!");
                        buzz();
                    }
                },
                100
        );*/
        buzz();

        new Timer().schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        System.out.println(">>> Answer: " + getCurrentQuestion().getAnswer());
                        answer(getCurrentQuestion().getFixedAnswer());
                    }
                },
                500
        );
    }

    @Override
    public void onSync() {
        //next();
    }

    @Override
    public void onPing() {
        next();
    }

    @Override
    public void finishQuestion() {
        next();
    }

    @Override
    public void onReceiveChat(String userid, long time, String message) {

    }

    @Override
    public void onUserJoin(User user, String ip) {
        /*if(this.getClientUser()!=null) {
            chat("Hello, "+user.getUsername()+"!");
        }*/
    }

    @Override
    public void onUserLoad(User user) {

    }

    @Override
    public void onConnectToRoom() {
        this.setPreference(Preference.DISTRACTION_FREE, true);
        this.setPreference(Preference.SHOW_TYPING, false);
    }
}
