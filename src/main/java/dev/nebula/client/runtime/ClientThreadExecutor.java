package dev.nebula.client.runtime;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import dev.nebula.client.utils.Log;
public final class ClientThreadExecutor {
    private final ConcurrentLinkedQueue<Runnable> tasks = new ConcurrentLinkedQueue<>();
    private volatile Thread owner;
    public void execute(Runnable task) {
        Objects.requireNonNull(task,"task");
        if (Thread.currentThread()==owner) task.run(); else tasks.add(task);
    }
    public synchronized void bindCurrentThread() {
        if (owner==null) owner=Thread.currentThread();
        else if(owner!=Thread.currentThread()) throw new IllegalStateException("Client task drain on a foreign thread");
    }
    public void drain() {
        bindCurrentThread();
        Runnable task;
        while((task=tasks.poll())!=null) {
            try {task.run();} catch(RuntimeException e){Log.error("Client task failed: "+e);}
        }
    }
    public boolean isClientThread(){return owner==Thread.currentThread();}
}
