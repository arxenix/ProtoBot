package me.ankur.protobot;

import com.google.code.chatterbotapi.ChatterBot;
import com.google.code.chatterbotapi.ChatterBotFactory;
import com.google.code.chatterbotapi.ChatterBotSession;
import com.google.code.chatterbotapi.ChatterBotType;

/**
 * Created by Sundara on 2/13/15.
 */
public class ChatBotClient extends ProtobowlClient {

    ChatterBotSession botSession;

    /**
     * Instantiate a new protobowl client
     *
     * @param name name
     * @param room room
     */
    public ChatBotClient(String name, String room) {
        super(name, room);

        try {
            ChatterBotFactory factory = new ChatterBotFactory();
            ChatterBot bot = factory.create(ChatterBotType.CLEVERBOT);
            botSession = bot.createSession();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onUpdateQuestion() {
        //next();
    }

    @Override
    public void onSync() {
        //next();
    }

    @Override
    public void onPing() {
        //next();
    }

    @Override
    public void finishQuestion() {
        //next();
    }

    @Override
    public void onReceiveChat(String userid, long time, String message) {
        System.out.println("Receive Chat << " + message);
        if (!message.contains("@!@") && !message.contains("@*@")) {
            if (this.getClientUser() != null) {
                User user = this.getUserFromId(userid);
                if (user != null) {
                    if (!user.equals(this.getClientUser())) {
                        try {
                            if (message.startsWith("!")) {
                                message = message.substring(1);
                                if (message.length() > 0) {
                                    String[] args = message.split(" ");
                                    if (args[0].equalsIgnoreCase("answer")) {
                                        this.chat("The answer is: " + getCurrentQuestion().getAnswer());
                                    } else if (args[0].equalsIgnoreCase("speed")) {
                                        if (args.length == 2) {
                                            double speed = Double.parseDouble(args[1]);
                                            this.setQuestionSpeed(speed);
                                            System.out.println("Setting speed to " + speed);
                                        }
                                    } else if (args[0].equalsIgnoreCase("category")) {
                                        if (args.length == 2) {
                                            this.setCategory(args[1]);
                                            System.out.println("Setting category to " + args[1]);
                                        }
                                    }
                                } else {
                                    this.chat("Invalid!");
                                }
                            } else if (message.startsWith(".") && message.length() > 1) {
                                String chat = botSession.think(message.substring(1));
                                System.out.println("Send Chat >> " + chat);
                                this.chat(chat);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    System.out.println("Error! User is null?");
                }
            }
        }
    }

    @Override
    public void onUserJoin(User user, String ip) {
        //@*@individuals hi
        if (this.getClientUser() != null) {
            if (user != null) {
                if (!user.equals(this.getClientUser())) {
                    try {
                        this.chat("Hello, @!@" + user.getId() + ", I am a bot! Put a '.' in front of your messages to talk to me!");
                        //System.out.println("Send Chat >> "+"Hello, @!@"+user.getId()+", I am a bot!");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void onUserLoad(User user) {
        //@*@individuals hi
        if (this.getClientUser() != null) {
            if (user != null) {
                if (!user.equals(this.getClientUser())) {
                    try {
                        this.chat("Hello, @!@" + user.getId() + ", I am a bot! Put a '.' in front of your messages to talk to me!");
                        //System.out.println("Send Chat >> "+"Hello, @!@"+user.getId()+", I am a bot!");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public void onConnectToRoom() {
        this.setPreference(Preference.DISTRACTION_FREE, false);
        this.setPreference(Preference.SHOW_TYPING, false);
    }
}
