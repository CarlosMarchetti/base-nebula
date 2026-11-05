import dev.nebula.client.runtime.ClientThreadExecutor;
import dev.nebula.client.event.*;
import dev.nebula.client.managers.AttackStatsManager;
import java.util.concurrent.atomic.*;
public class ConcurrencyCheck {
    public static class Ping extends Event {}
    public static class Listener {AtomicInteger count=new AtomicInteger();@EventTarget public void ping(Ping event){count.incrementAndGet();}}
    public static void main(String[] args)throws Exception{
        ClientThreadExecutor dispatch=new ClientThreadExecutor();dispatch.bindCurrentThread();AtomicInteger count=new AtomicInteger();AtomicBoolean wrong=new AtomicBoolean();
        Thread worker=new Thread(()->{for(int i=0;i<1000;i++)dispatch.execute(()->{if(!dispatch.isClientThread())wrong.set(true);count.incrementAndGet();});try{dispatch.drain();wrong.set(true);}catch(IllegalStateException expected){}});worker.start();worker.join();if(count.get()!=0)throw new AssertionError("Foreign thread ran queued commands");dispatch.drain();if(count.get()!=1000||wrong.get())throw new AssertionError("Thread ownership/queue failure");
        EventManager events=EventManager.getInstance();Listener listener=new Listener();events.register(listener);events.register(listener);events.call(new Ping());if(listener.count.get()!=1)throw new AssertionError("Duplicate listener registration");
        AttackStatsManager stats=new AttackStatsManager();Thread[] workers=new Thread[4];for(int i=0;i<workers.length;i++){workers[i]=new Thread(()->{for(int j=0;j<5000;j++){stats.incrementEnemyAttacks();events.call(new Ping());}});workers[i].start();}for(Thread t:workers)t.join();if(stats.getEnemyAttacks()!=20000||listener.count.get()!=20001)throw new AssertionError("Concurrent counter/event loss");events.unregister(listener);
        System.out.println("PASS client thread ownership, queued commands, idempotent events and concurrent counters");
    }
}
