import dev.nebula.client.connection.Connection;
import dev.nebula.client.managers.EventsHandler;
import dev.nebula.client.modules.Category;
import dev.nebula.client.modules.Module;
import dev.nebula.client.runtime.ClientContext;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicInteger;

public class PacketIsolationCheck {
    static class Broken extends Module {
        AtomicInteger calls = new AtomicInteger();

        Broken() {
            super("BrokenFixture", "test", Category.CONFIG, 0);
        }

        public void onEnable() {}

        public void onDisable() {}

        public void onTick() {}

        public boolean onPacket(Object packet, Connection.Side side) {
            calls.incrementAndGet();
            throw new IllegalStateException("fixture failure");
        }
    }

    static class Observer extends Module {
        AtomicInteger calls = new AtomicInteger();
        boolean cancel;

        Observer() {
            super("ObserverFixture", "test", Category.CONFIG, 0);
        }

        public void onEnable() {}

        public void onDisable() {}

        public void onTick() {}

        public boolean onPacket(Object packet, Connection.Side side) {
            calls.incrementAndGet();
            return !cancel;
        }
    }

    public static void main(String[] args) throws Exception {
        GameFeaturesCheck.FakeGame game = new GameFeaturesCheck.FakeGame();
        ClientContext context = GameFeaturesCheck.install(game, Paths.get(args[0]));
        Broken broken = new Broken();
        Observer observer = new Observer();
        context.modules().registerModuleInstance(broken);
        context.modules().registerModuleInstance(observer);
        broken.enable();
        observer.enable();
        EventsHandler handler = new EventsHandler();
        AtomicInteger lost = new AtomicInteger();
        Thread netty =
                new Thread(
                        () -> {
                            for (int i = 0; i < 1000; i++)
                                if (!handler.onPacket(new Object(), Connection.Side.OUT))
                                    lost.incrementAndGet();
                        });
        netty.start();
        netty.join();
        if (lost.get() != 0 || broken.calls.get() != 1 || observer.calls.get() != 1000)
            throw new AssertionError(
                    "Broken callback cancelled traffic, starved observers or flooded retries");
        context.dispatcher().drain();
        if (broken.isEnabled())
            throw new AssertionError("Broken module not disabled on client thread");
        observer.cancel = true;
        if (handler.onPacket(new Object(), Connection.Side.OUT))
            throw new AssertionError("Legitimate cancellation lost");
        System.out.println(
                "PASS broken-module isolation, 1000 packets preserved, one failure queued and normal cancellation retained");
    }
}
