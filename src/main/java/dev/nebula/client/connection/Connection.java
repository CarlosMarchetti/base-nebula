package dev.nebula.client.connection;
import dev.nebula.client.managers.EventsHandler;
import dev.nebula.client.mappings.wrapper.client.Minecraft;
import io.netty.channel.*;
import io.netty.util.ReferenceCountUtil;
import java.nio.channels.ClosedChannelException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CancellationException;
public final class Connection extends ChannelDuplexHandler {
    public enum Side { IN, OUT }
    private static final String NAME="Nebula_PacketHandler";
    private final EventsHandler handler;
    private final Channel channel;
    private final Queue<Pending> pending=new ConcurrentLinkedQueue<>();
    public Connection(EventsHandler handler,Channel channel){this.handler=handler;this.channel=channel;}
    public static Connection connect(EventsHandler handler){
        Channel channel=(Channel)Minecraft.getMinecraft().getNetworkManager().getChannel();
        ChannelHandler existing=channel.pipeline().get(NAME);
        if(existing instanceof Connection)return (Connection)existing;
        Connection connection=new Connection(handler,channel);channel.pipeline().addBefore("packet_handler",NAME,connection);return connection;
    }
    public boolean isAttached(){return channel.isActive()&&channel.pipeline().context(this)!=null;}
    @Override public void channelRead(ChannelHandlerContext ctx,Object message)throws Exception{
        if(handler.onPacket(message,Side.IN))super.channelRead(ctx,message);else ReferenceCountUtil.release(message);
    }
    @Override public void write(ChannelHandlerContext ctx,Object message,ChannelPromise promise)throws Exception{
        if(handler.onPacket(message,Side.OUT))super.write(ctx,message,promise);else pending.add(new Pending(ctx,message,promise));
    }
    public void sendQueuedPacket(){Pending message=pending.poll();if(message==null)return;
        message.context.executor().execute(()->{
            if(!message.context.channel().isActive()){discard(message,new ClosedChannelException());return;}
            try{super.write(message.context,message.message,message.promise);message.context.flush();}
            catch(Exception e){discard(message,e);}
        });
    }
    public boolean hasQueuedPackets(){return !pending.isEmpty();}
    public void clearQueue(){failAll(new CancellationException("Queued packet discarded"));}
    @Override public void channelInactive(ChannelHandlerContext context)throws Exception{failAll(new ClosedChannelException());super.channelInactive(context);}
    @Override public void handlerRemoved(ChannelHandlerContext context)throws Exception{failAll(new ClosedChannelException());super.handlerRemoved(context);}
    private void failAll(Throwable failure){Pending value;while((value=pending.poll())!=null)discard(value,failure);}
    private static void discard(Pending value,Throwable failure){ReferenceCountUtil.release(value.message);value.promise.tryFailure(failure);}
    private static final class Pending{
        final ChannelHandlerContext context;final Object message;final ChannelPromise promise;
        Pending(ChannelHandlerContext context,Object message,ChannelPromise promise){this.context=context;this.message=message;this.promise=promise;}
    }
}
