package dev.nebula.client.managers;
import java.util.concurrent.atomic.AtomicInteger;
public final class AttackStatsManager {
    private final AtomicInteger enemyAttacks=new AtomicInteger(),clanAttacks=new AtomicInteger();
    public void incrementEnemyAttacks(){enemyAttacks.incrementAndGet();}
    public void incrementClanAttacks(){clanAttacks.incrementAndGet();}
    public int getEnemyAttacks(){return enemyAttacks.get();}
    public int getClanAttacks(){return clanAttacks.get();}
    public void resetStats(){enemyAttacks.set(0);clanAttacks.set(0);}
}
