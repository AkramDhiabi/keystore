package keystore.handler;

import com.google.gson.Gson;
import keystore.services.ChangeEvent;
import keystore.services.ChangeListener;
import keystore.services.KeyValueStoreService;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@WebSocket
public class WatchHandler implements ChangeListener {
    private List<Session> listeners;
    private AtomicInteger watchGauge;

    public WatchHandler(KeyValueStoreService keyValueStoreService, AtomicInteger watchGauge) {
        this.watchGauge = watchGauge;
        listeners = Collections.synchronizedList(new ArrayList<>());
        keyValueStoreService.addChangeListener(this);
    }

    @OnWebSocketConnect
    public void onConnect(Session user) {
        listeners.add(user);
        watchGauge.incrementAndGet();
    }

    @OnWebSocketClose
    public void onClose(Session user, int statusCode, String reason) {
        listeners.remove(user);
        watchGauge.decrementAndGet();
    }

    @OnWebSocketMessage
    public void onMessage(Session user, String message) {
        // ignored
    }

    @Override
    public void handle(ChangeEvent ce) {
        for (Session session : listeners) {
            try {
                session.getRemote().sendString(new Gson().toJson(ce));
            } catch (IOException e) {
                // ignored
            }
        }
    }
}
