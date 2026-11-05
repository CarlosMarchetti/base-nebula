import dev.nebula.client.runtime.ClientContext;
import dev.nebula.client.modules.Module;
import dev.nebula.client.modules.Category;
import dev.nebula.client.gui.settings.impl.*;
import dev.nebula.client.config.SettingCodec;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
public class SettingsPersistenceCheck {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static void invalid(dev.nebula.client.gui.settings.Setting setting,String raw){try{SettingCodec.apply(setting,raw);throw new AssertionError("Accepted invalid "+raw);}catch(IllegalArgumentException|ArithmeticException expected){}}
    public static void main(String[] args)throws Exception{
        Path dir=Paths.get(args[0]);Files.createDirectories(dir);ClientContext c=ModuleMetadataCheck.context(dir);c.dispatcher().bindCurrentThread();c.modules().initializeModules();
        Module module=c.modules().getModule("KillAura");List<dev.nebula.client.gui.settings.Setting> settings=c.settings().getSettingsByModule(module);check(!settings.isEmpty(),"Missing actual settings");
        Map<String,String> defaults=new LinkedHashMap<>();for(dev.nebula.client.gui.settings.Setting s:settings)defaults.put(s.getName(),SettingCodec.encode(s));
        module.initializeSettings();check(settings.size()==c.settings().getSettingsByModule(module).size(),"Setup duplicated settings");
        IntSetting integer=new IntSetting("checkInt",module,3,0,20);SettingCodec.apply(integer,"6.00");check(integer.getValue()==6,"Decimal integer handling");invalid(integer,"6.3");
        BooleanSetting bool=new BooleanSetting("checkBool",module,true);invalid(bool,"junk");
        DoubleSetting decimal=new DoubleSetting("checkDouble",module,1,0,20);invalid(decimal,"NaN");invalid(decimal,"Infinity");
        ListSetting list=new ListSetting("checkList",module,Arrays.asList("alpha,colon:","quoted\""));String encoded=SettingCodec.encode(list);SettingCodec.apply(list,encoded);check(encoded.equals(SettingCodec.encode(list)),"List roundtrip");
        RangeSetting range=new RangeSetting("checkRange",module,1,2,0,10,true);SettingCodec.apply(range,"[2.5,8.5]");check(SettingCodec.encode(range).equals("[2.5,8.5]"),"Range roundtrip");invalid(range,"[8,2]");
        for(dev.nebula.client.gui.settings.Setting s:settings)if(s instanceof BooleanSetting)s.setValue(!((Boolean)s.getValue()));
        module.restoreDefaultSettings();for(dev.nebula.client.gui.settings.Setting s:settings)check(defaults.get(s.getName()).equals(SettingCodec.encode(s)),"Default reset: "+s.getName());
        c.clans().replaceAll(Arrays.asList("ally:a","ally,b"),Arrays.asList("enemy"),Arrays.asList("player"));c.persistence().save();Path config=dir.resolve("config.txt");byte[] saved=Files.readAllBytes(config);
        StringBuilder large=new StringBuilder(new String(saved,StandardCharsets.UTF_8));for(int i=0;i<1500;i++)large.append("UNKNOWN:padding:entry-").append(i).append('\n');
        Files.write(config,large.toString().getBytes(StandardCharsets.UTF_8));byte[] before=Files.readAllBytes(config);c.persistence().load();c.persistence().load();c.persistence().flushIfDirty();
        check(Arrays.equals(before,Files.readAllBytes(config)),"Load rewrote/truncated large config");check(c.clans().getAlliedClans().equals(Arrays.asList("ally:a","ally,b")),"Clan reload duplicated/lost entries");
        c.persistence().save();check(Files.size(config)<before.length,"Explicit save did not replace config");
        Path legacy=dir.resolve("legacy.txt");Files.write(legacy,saved);Files.delete(config);c.persistence().load();check(Files.exists(config),"Legacy not imported");check(Arrays.equals(saved,Files.readAllBytes(legacy)),"Legacy modified");
        System.out.println("PASS defaults, codecs, >8KB reload, clans, atomic save and legacy import");
    }
}
