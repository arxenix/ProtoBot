package me.ankur.protobot;

/**
 * Created by Sundara on 2/14/15.
 */
public class SilentAnswerClient extends ProtobowlClient {
    /**
     * Instantiate a new protobowl client
     *
     * @param name
     * @param room
     */
    public SilentAnswerClient(String name, String room) {
        super(name, room);
    }

    @Override
    public void onUpdateQuestion() {
        this.setName("Answer: "+this.getCurrentQuestion().getFixedAnswer());
    }

    @Override
    public void onSync() {

    }

    @Override
    public void onPing() {

    }

    @Override
    public void finishQuestion() {

    }

    @Override
    public void onReceiveChat(String userid, long time, String message) {

    }

    @Override
    public void onUserJoin(User user, String ip) {

    }

    @Override
    public void onUserLoad(User user) {

    }

    @Override
    public void onConnectToRoom() {

    }
}
