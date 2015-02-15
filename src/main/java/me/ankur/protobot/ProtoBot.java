package me.ankur.protobot;

/**
 * Created by Sundara on 1/13/15.
 */
public class ProtoBot {

    public static void main(String[] args) throws Exception {
        try {
            new Thread() {
                @Override
                public void run() {
                    new ChatBotClient("alliterate apple", "hsquizbowl").connect();
                }
            }.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
