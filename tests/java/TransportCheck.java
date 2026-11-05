import dev.nebula.client.connection.Connection;
import dev.nebula.client.managers.EventsHandler;
import io.netty.channel.*;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.buffer.*;
public class TransportCheck {
    static void check(boolean b,String msg){if(!b)throw new AssertionError(msg);}
    public static void main(String[] args){
        EventsHandler deny=new EventsHandler(){@Override public boolean onPacket(Object p,Connection.Side side){return false;}};
        EmbeddedChannel channel=new EmbeddedChannel(new ChannelDuplexHandler());Connection connection=new Connection(deny,channel);channel.pipeline().addFirst(connection);
        ByteBuf a=Unpooled.buffer().writeByte(1);ChannelPromise p=channel.newPromise();channel.pipeline().write(a,p);check(!p.isDone()&&connection.hasQueuedPackets(),"Cancelled packet not owned by queue");connection.clearQueue();check(p.isDone()&&!p.isSuccess()&&a.refCnt()==0,"Clear did not fail promise/release");
        ByteBuf b=Unpooled.buffer().writeByte(2);ChannelPromise p2=channel.newPromise();channel.pipeline().write(b,p2);connection.sendQueuedPacket();channel.runPendingTasks();check(p2.isSuccess(),"Queued write never completed");ByteBuf sent=(ByteBuf)channel.readOutbound();check(sent==b,"Wrong queued message");sent.release();
        ByteBuf incoming=Unpooled.buffer().writeByte(3);channel.writeInbound(incoming);check(incoming.refCnt()==0,"Denied inbound leak");
        ByteBuf c=Unpooled.buffer().writeByte(4);ChannelPromise p3=channel.newPromise();channel.pipeline().write(c,p3);channel.close().syncUninterruptibly();channel.runPendingTasks();check(p3.isDone()&&!p3.isSuccess()&&c.refCnt()==0,"Disconnect leak/pending promise");channel.finish();
        System.out.println("PASS queue flush, cancellation, inbound release and disconnect cleanup");
    }
}
