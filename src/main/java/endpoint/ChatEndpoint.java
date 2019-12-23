package endpoint;

import entity.Message;
import entity.MessageDecoder;
import entity.MessageEncoder;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@ServerEndpoint(value = "/chat/{username}", decoders = MessageDecoder.class, encoders = MessageEncoder.class)
public class ChatEndpoint {
    private Session session;
    private static Set<ChatEndpoint> chatEndpoints = new CopyOnWriteArraySet<>();
    private static HashMap<String, String> users = new HashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("username")String username) throws IOException {
        this.session = session;
        chatEndpoints.add(this);
        users.put(session.getId(),username);

        Message message = new Message();
        message.setFrom(username);
        message.setContent(username + " connected!");
        broadcast(message);
    }

    @OnMessage
    public void onMessage(Session session, Message message) throws IOException{
        message.setFrom(users.get(session.getId()));
        broadcast(message);
    }

    @OnClose
    public void onClose(Session session) throws IOException{
        chatEndpoints.remove(this);
        Message message = new Message();
        message.setFrom(users.get(session.getId()));
        message.setContent(users.get(session.getId()) + " disconnected.");
        broadcast(message);
        System.out.println(users.get(session.getId()) + " close.");
    }

    @OnError
    public void onError(Session session, Throwable throwable){}

    private static void broadcast(Message message) throws IOException{
        chatEndpoints.forEach(chatEndpoint -> {
            synchronized (chatEndpoint) {
                try {
                    chatEndpoint.session.getBasicRemote().sendObject(message);
                } catch (IOException | EncodeException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
