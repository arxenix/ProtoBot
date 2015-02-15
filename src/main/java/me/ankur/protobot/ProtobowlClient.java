package me.ankur.protobot;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@WebSocket(maxTextMessageSize = 64 * 1024)
public abstract class ProtobowlClient implements WebSocketListener {

    private static final String USER_AGENT = "Mozilla/5.0";
    private static HttpClient httpClient = HttpClientBuilder.create().build();
    private final CountDownLatch closeLatch;
    private String name;
    private String room;
    private String ip;
    private Question currentQuestion;
    private HashMap<String, User> userMap = new HashMap<>();
    private User clientUser;
    @SuppressWarnings("unused")
    private Session session;
    private String alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private int i = 2;
    private int n = 1;

    /**
     * Instantiate a new protobowl client
     *
     * @param name name
     * @param room room
     */
    public ProtobowlClient(String name, String room) {
        this.room = room;
        this.name = name;
        this.closeLatch = new CountDownLatch(1);
    }

    private static String getExternalIP() {
        try {
            URL whatismyip = new URL("http://checkip.amazonaws.com");
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    whatismyip.openStream()));
            return in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String sendGet(String url) throws Exception {
        HttpGet request = new HttpGet(url);

        // add request header
        request.addHeader("User-Agent", USER_AGENT);

        HttpResponse response = httpClient.execute(request);

        System.out.println("\nSending 'GET' request to URL : " + url);
        System.out.println("Response Code : " +
                response.getStatusLine().getStatusCode());
        if (response.getStatusLine().getStatusCode() == 502) {
            return null;
        }

        BufferedReader rd = new BufferedReader(
                new InputStreamReader(response.getEntity().getContent()));

        StringBuilder result = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }
        return result.toString();
    }

    public void connect() {
        System.out.println("Attempting to connect to Protobowl...");
        String server = "protobowl.nodejitsu.com/socket.io/1/";
        try {
            ip = getExternalIP();
            System.out.println("Your ip: " + ip);
            String get = sendGet("http://" + server);
            if (get == null) {
                System.out.println("Could not connect to default protobowl server! Trying alternative...");
                server = "cab.antimatter15.com:443/socket.io/1/";
                get = sendGet("http://" + server);
            }
            //String get = sendGet("http://cab.antimatter15.com:443/socket.io/1/");  ?? when normal server is down i think?
            final String socketString = get.split(":")[0];
            System.out.println("Socket = " + socketString);

            WebSocketClient client = new WebSocketClient();
            client.getPolicy().setMaxTextMessageSize(131072);
            client.getPolicy().setMaxTextMessageBufferSize(131072);
            client.setMaxTextMessageBufferSize(131072);
            client.start();
            URI echoUri = new URI("ws://" + server + "websocket/" + socketString);
            ClientUpgradeRequest request = new ClientUpgradeRequest();
            client.connect(this, echoUri, request);
            System.out.printf("Connecting to : %s%n", echoUri);
            this.awaitClose(7, TimeUnit.DAYS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void close() {
        session.close();
    }

    public boolean awaitClose(int duration, TimeUnit unit) throws InterruptedException {
        return this.closeLatch.await(duration, unit);
    }

    public abstract void onUpdateQuestion();

    public abstract void onSync();

    public abstract void onPing();

    public abstract void finishQuestion();

    public abstract void onReceiveChat(String userid, long time, String message);

    public abstract void onUserJoin(User user, String ip);

    public abstract void onUserLoad(User user);

    public abstract void onConnectToRoom();

    public User getClientUser() {
        return this.clientUser;
    }

    public User getUserFromId(String id) {
        return this.userMap.get(id);
    }

    public void answer(String ans) {
        try {
            JSONObject answer = new JSONObject()
                    .put("name", "guess")
                    .put("args", new JSONArray().put(0, new JSONObject().put("text", ans).put("done", true)).put(1, JSONObject.NULL));
            session.getRemote().sendString("5:::" + answer.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void chat(String text, String chatSession) {
        //5:::{"name":"chat","args":[{"text":"Test Message","session":"7mvjyywmz9i19k9","user":"52f32a4d6dd84bc09584b30648b51d229d926428","first":false,"done":true,"time":1423879967259}]}
        try {
            JSONObject answer = new JSONObject()
                    .put("name", "chat")
                    .put("args", new JSONArray().put(0,
                            new JSONObject().put("text", text)
                                    .put("session", chatSession)
                                    .put("user", this.clientUser.getId())
                                    .put("first", false)
                                    .put("done", true)
                                    .put("time", System.currentTimeMillis())
                    ));
            session.getRemote().sendString("5:::" + answer.toString());
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }

    public String chat(String text) {
        String chatSession = generateString(15);
        chat(text, chatSession);
        return chatSession;
    }

    /**
     * Sets the question speed
     *
     * @param speed A speed from 200 (slow) to 0.1 (fast)
     */
    public void setQuestionSpeed(double speed) {
        //5:::{"name":"set_speed","args":[22.388059701492537,null]}
        try {
            JSONObject answer = new JSONObject()
                    .put("name", "set_speed")
                    .put("args", new JSONArray().put(0, speed).put(1, JSONObject.NULL));
            session.getRemote().sendString("5:::" + answer.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Set the difficulty
     *
     * @param difficulty (Empty string for All)
     */
    public void setDifficulty(String difficulty) {
        //5:::{"name":"set_difficulty","args":["Open",null]}
        try {
            JSONObject answer = new JSONObject()
                    .put("name", "set_difficulty")
                    .put("args", new JSONArray().put(0, difficulty).put(1, JSONObject.NULL));
            session.getRemote().sendString("5:::" + answer.toString());
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Set Max Buzz
     *
     * @param maxBuzz max buzzes
     */
    public void setMaxBuzz(int maxBuzz) {
        //5:::{"name":"set_max_buzz","args":[null,null]}
        try {
            JSONObject answer = new JSONObject()
                    .put("name", "set_max_buzz");

            JSONArray args = new JSONArray();
            if (maxBuzz == Integer.MAX_VALUE) {
                args = args.put(0, JSONObject.NULL);
            } else {
                args = args.put(0, maxBuzz);
            }
            args = args.put(1, JSONObject.NULL);
            answer = answer.put("args", args);
            session.getRemote().sendString("5:::" + answer.toString());
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }

    public void setSkip(boolean skip) {
        //5:::{"name":"set_skip","args":[true,null]}
        try {
            JSONObject answer = new JSONObject()
                    .put("name", "set_skip")
                    .put("args", new JSONArray().put(0, skip).put(1, JSONObject.NULL));
            session.getRemote().sendString("5:::" + answer.toString());
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }

    public void setPause(boolean pause) {
        //5:::{"name":"set_pause","args":[true,null]}
        try {
            JSONObject answer = new JSONObject()
                    .put("name", "set_pause")
                    .put("args", new JSONArray().put(0, pause).put(1, JSONObject.NULL));
            session.getRemote().sendString("5:::" + answer.toString());
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }

    public void setBonus(boolean bonus) {
        //5:::{"name":"set_bonus","args":[true,null]}
        try {
            JSONObject answer = new JSONObject()
                    .put("name", "set_bonus")
                    .put("args", new JSONArray().put(0, bonus).put(1, JSONObject.NULL));
            session.getRemote().sendString("5:::" + answer.toString());
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }

    public void resetScore() {
        //5:::{"name":"reset_score","args":[null,null]}
        try {
            JSONObject answer = new JSONObject()
                    .put("name", "reset_score")
                    .put("args", new JSONArray().put(0, JSONObject.NULL).put(1, JSONObject.NULL));
            session.getRemote().sendString("5:::" + answer.toString());
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }

    public void pause() {
        //5:::{"name":"pause","args":[null,null]}
        try {
            JSONObject answer = new JSONObject()
                    .put("name", "pause")
                    .put("args", new JSONArray().put(0, JSONObject.NULL).put(1, JSONObject.NULL));
            session.getRemote().sendString("5:::" + answer.toString());
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }

    public void unpause() {
        //5:::{"name":"unpause","args":[null,null]}
        try {
            JSONObject answer = new JSONObject()
                    .put("name", "unpause")
                    .put("args", new JSONArray().put(0, JSONObject.NULL).put(1, JSONObject.NULL));
            session.getRemote().sendString("5:::" + answer.toString());
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Set the Category
     *
     * @param category (empty string for All)
     */
    public void setCategory(String category) {
        //5:::{"name":"set_category","args":["Mythology",null]}
        try {
            JSONObject answer = new JSONObject()
                    .put("name", "set_category")
                    .put("args", new JSONArray().put(0, category).put(1, JSONObject.NULL));
            session.getRemote().sendString("5:::" + answer.toString());
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }

    public void setPreference(Preference pref, boolean enabled) {
        //5:::{"name":"pref","args":["typing",false]}
        //5:::{"name":"pref","args":["distraction",false]}
        try {
            JSONObject answer = new JSONObject()
                    .put("name", "pref")
                    .put("args", new JSONArray().put(0, pref.toString()).put(1, enabled));
            session.getRemote().sendString("5:::" + answer.toString());
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }

    public void buzz(String qid) {
        //5:4+::{"name":"buzz","args":["54769911ea23cca905508fb6"]}
        try {
            JSONObject buzz = new JSONObject()
                    .put("name", "buzz")
                    .put("args", new JSONArray().put(0, qid));
            session.getRemote().sendString("5:" + i + "+::" + buzz.toString());
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }

    public void buzz() {
        buzz(getCurrentQuestion().getQID());
    }

    public Question getCurrentQuestion() {
        return this.currentQuestion;
    }

    public void next() {
        try {
            JSONObject next = new JSONObject()
                    .put("name", "next")
                    .put("args", new JSONArray().put(0, JSONObject.NULL).put(1, JSONObject.NULL));
            session.getRemote().sendString("5:::" + next.toString());
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }

    public void setName(String name) {
        //5:::{"name":"set_name","args":["Name change lol",null]}
        try {
            JSONObject setName = new JSONObject()
                    .put("name", "set_name")
                    .put("args", new JSONArray().put(0, name).put(1, JSONObject.NULL));
            session.getRemote().sendString("5:::" + setName.toString());
            //System.out.println(">>> Setname "+name);
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }

    private String generateString(String seed, int len) {
        int n = alphabet.length(); //10

        StringBuilder result = new StringBuilder();
        Random r = new Random(seed.hashCode()); //11

        for (int i = 0; i < len; i++) //12
            result.append(alphabet.charAt(r.nextInt(n))); //13

        return result.toString();
    }

    private String generateString(int len) {
        int n = alphabet.length(); //10

        StringBuilder result = new StringBuilder();
        Random r = new Random(); //11

        for (int i = 0; i < len; i++) //12
            result.append(alphabet.charAt(r.nextInt(n))); //13

        return result.toString();
    }

    @Override
    public void onWebSocketBinary(byte[] bytes, int i, int i1) {

    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        System.out.printf("Connection closed: %d - %s%n", statusCode, reason);
        this.session = null;
        this.closeLatch.countDown();
    }

    @Override
    public void onWebSocketConnect(Session session) {
//System.out.printf("Got connect: %s%n", session);
        System.out.println("Got connect!");
        this.session = session;


        try {
            JSONObject join = new JSONObject()
                    .put("name", "join")
                    .put("args", new JSONArray().put(0,
                            new JSONObject()
                                    .put("cookie", generateString(name, 41))
                                    .put("auth", "")
                                    .put("question_type", "qb")
                                    .put("room_name", room)
                                    .put("muwave", false)
                                    .put("agent", "M4/Web")
                                    .put("agent_version", "Sun Dec 28 2014 20:44:30 GMT-0500 (EST)")
                                    .put("referrers", new JSONArray().put(0, "http://protobowl.com/"))
                                    .put("version", 8)
                    ));
            session.getRemote().sendString("5:::" + join.toString());

            onConnectToRoom();
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onWebSocketError(Throwable throwable) {
        throwable.printStackTrace();
    }

    @Override
    public void onWebSocketText(String msg) {
        //System.out.println("<<< " + msg);

        try {
            if (msg.startsWith("2::")) {
                //System.out.println(">>> 2::");
                //System.out.println(">>> pong");
                session.getRemote().sendString("2::");
                onPing();
            } else if (msg.startsWith("6:::2+")) {
                JSONObject echoReply = new JSONObject()
                        .put("name", "echo")
                        .put("args", new JSONArray().put(0, new JSONObject()
                                .put("avg", 31)
                                .put("std", 0)
                                .put("n", n)));
                n += 3;
                ///System.out.println(">>> 5:"+i+"+::"+echoReply.toString());
                //System.out.println(">>> echo");
                session.getRemote().sendString("5:" + i + "+::" + echoReply.toString());
                i++;

                setName(name);
            } else if (msg.startsWith("6:::1+")) {
                JSONObject echoReply = new JSONObject()
                        .put("name", "echo")
                        .put("args", new JSONArray().put(0, new JSONObject()));
                //System.out.println(">>> echo1");
                //System.out.println(">>> 5:"+i+"+::"+echoReply.toString());
                session.getRemote().sendString("5:" + i + "+::" + echoReply.toString());
                i++;
            } else {
                int idx = msg.indexOf("{");
                if (idx > 0) {
                    JSONObject obj = new JSONObject(msg.substring(idx));
                    //System.out.println("json object: "+obj.toString());
                    if (obj.has("name")) {
                        String name = obj.getString("name");
                        //System.out.println("Packet name: "+name);
                        if (name.equals("sync")) {
                            JSONObject sync = obj.getJSONArray("args").getJSONObject(0);
                            if (sync.has("answer")) {
                                String qid = obj.getJSONArray("args").getJSONObject(0).getString("qid");
                                String ans = obj.getJSONArray("args").getJSONObject(0).getString("answer");
                                currentQuestion = new Question(qid, ans);

                                onUpdateQuestion();
                            }

                            if (sync.has("users")) {
                                JSONArray users = sync.getJSONArray("users");
                                for (int i = 0; i < users.length(); i++) {
                                    JSONObject user = users.getJSONObject(i);
                                    if (user.getBoolean("online_state")) {
                                        String id = user.getString("id");
                                        String username = user.getString("name");

                                        if (!userMap.containsKey(id)) {
                                            User u = new User(id, username);
                                            //System.out.println("User "+username+" is in the room!");
                                            onUserLoad(u);
                                            userMap.put(id, u);
                                        }
                                    }
                                }
                            }

                            onSync();
                            next();

                            JSONObject syncReply = new JSONObject()
                                    .put("name", "check_public")
                                    .put("args", new JSONArray().put(0, ""));
                            //System.out.println(">>> 5:1+::"+syncReply.toString());
                            session.getRemote().sendString("5:1+::" + syncReply.toString());
                        } else if (name.equals("finish_question")) {
                            finishQuestion();
                        } else if (name.equals("chat")) {
                            //5:::{"name":"chat","args":[{"text":"hi","session":"vh80h52nle4s4i","user":"52f32a4d6dd84bc09584b30648b51d229d926428","first":false,"done":true,"time":1423882902231}]}
                            JSONObject chat = obj.getJSONArray("args").getJSONObject(0);
                            if (chat.getBoolean("done")) {
                                String text = chat.getString("text");
                                long time = chat.getLong("time");
                                String id = chat.getString("user");
                                onReceiveChat(id, time, text);
                            }
                        } else if (name.equals("joined")) {
                            //5:::{"name":"joined","args":[{"auth":null,"id":"52f32a4d6dd84bc09584b30648b51d229d926428","name":"Parsex","existing":true,"ip":"71.168.64.177"}]}
                            JSONObject join = obj.getJSONArray("args").getJSONObject(0);
                            String id = join.getString("id");
                            String username = join.getString("name");
                            String ip = join.getString("ip");

                            User user = new User(id, username);

                            //System.out.println("User "+username+" joined the room!");

                            if (ip.equals(this.ip) && user.getUsername().equals(this.name)) {
                                this.clientUser = user;
                                System.out.println("Loaded ClientUser... " + clientUser);
                            }

                            userMap.put(id, user);
                            onUserJoin(user, ip);
                        } else if (name.equals("log")) {
                            //5:::{"name":"log","args":[{"user":"b7de82dbddc7cec484dca53c156635aad85d2871","verb":"left the room (logged on 5 minutes ago)","time":1423888959307}]}
                            JSONObject log = obj.getJSONArray("args").getJSONObject(0);
                            if (log.getString("verb").startsWith("left the room")) {
                                //System.out.println("User "+this.getUserFromId(log.getString("user")).getUsername()+" left the room!");
                                this.userMap.remove(log.getString("user"));
                            }
                        }
                    }
                }
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    public enum Preference {
        SOUND_ON_BUZZ("sounds"),
        ENABLE_CHAT("webrtc"),
        SHOW_TYPING("typing"),
        DISTRACTION_FREE("distraction"),
        LAST_50("movingwindow");

        private String str;

        private Preference(String str) {
            this.str = str;
        }

        public String toString() {
            return str;
        }
    }

    public class User {
        private String id;
        private String username;

        public User(String id, String username) {
            this.id = id;
            this.username = username;
        }

        public String getId() {
            return this.id;
        }

        public String getUsername() {
            return this.username;
        }

        public String toString() {
            return "User-  name:" + username + ", id:" + id;
        }
    }

    public class Question {
        private String qid;
        private String ans;

        public Question(String qid, String ans) {
            this.qid = qid;
            this.ans = ans;
        }

        public String getQID() {
            return this.qid;
        }

        public String getAnswer() {
            return this.ans;
        }

        public String getFixedAnswer() {
            return ans.replaceAll("\\{", "").replaceAll("\\}", "").replaceAll("\\(", "").replaceAll("\\)", "").replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\"", "").replaceAll("accept", "").replaceAll("before mentioned", "").replaceAll("also", "").replaceAll(" or ", " ").replaceAll("do not", "").replaceAll("prompt", "").replaceAll("either", "");
        }
    }
}