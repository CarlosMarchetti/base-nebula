import dev.nebula.client.Main;
import dev.nebula.client.mappings.Mappings;
import dev.nebula.client.mappings.wrapper.client.Minecraft;
import dev.nebula.client.gui.settings.SettingsManager;
import dev.nebula.client.modules.combat.KillAura;
import dev.nebula.client.injection.transformers.PlayerMotionTransformer;
import dev.nebula.client.utils.AuraTargetSelector;
import org.objectweb.asm.*;
import static org.objectweb.asm.Opcodes.*;
import java.util.*;
import java.lang.reflect.*;
public class CombatTickCheck {
 public static final List<String> events=new ArrayList<>();
 public static class Box {public double minX,minY,minZ,maxX,maxY,maxZ;Box(Living e){minX=e.posX-.3;minY=e.posY;minZ=e.posZ-.3;maxX=e.posX+.3;maxY=e.posY+1.8;maxZ=e.posZ+.3;}}
 public static class Living {
  public double posX,posY,posZ; public float rotationYaw,rotationPitch,height=1.8f,width=.6f,health=20; public double motionX=1,motionZ=1; public boolean sprinting=true; public MovementInput movementInput=new MovementInput(); public float sprintGateForward;
  public boolean onGround=true,isDead; public int id;
  public Box getEntityBoundingBox(){return new Box(this);} public float getHealth(){return health;}
  public boolean isInvisible(){return false;}public boolean isSneaking(){return false;}public boolean isSprinting(){return sprinting;}
  public boolean canEntityBeSeen(Living e){return true;} public void swingItem(){events.add("swing");} public void setSprinting(boolean b){sprinting=b;} public void setSneaking(boolean b){}
  public void sprintGate(){sprintGateForward=movementInput.moveForward;}
  public float getEyeHeight(){return 1.62f;}
  public String getName(){return "target"+id;}
 }
 public static class MovementInput {public float moveForward,moveStrafe; public void updatePlayerMoveState(){moveForward=1;moveStrafe=0;}}
 public static class Controller {public void attackEntity(Living player,Living target){Game.instance.handler.send(new Attack(target,Action.ATTACK));if(player.sprinting){player.motionX*=.6;player.motionZ*=.6;player.setSprinting(false);}}}
 public enum Action {ATTACK}
 public static class Attack {public Living target;public Attack(Living e,Action action){target=e;}}
 public static class Handler {public List<Attack> attacks=new ArrayList<>();public void send(Object o){attacks.add((Attack)o);events.add("attack");}}
 public static class World {public List<Living> loadedEntityList=new ArrayList<>();}
 public static class Settings {public float mouseSensitivity=.5f;}
 public static class Game {public static Game instance; public Living player;public World world=new World();public Settings settings=new Settings();public Handler handler=new Handler();public Controller controller=new Controller();public Handler getNetHandler(){return handler;}}
 static class Loader extends ClassLoader {Class<?> load(byte[] b){return defineClass(null,b,0,b.length);}}
 static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
 static Field field(Class<?> c,String n)throws Exception{return c.getField(n);}
 static void mainField(String name,Object value)throws Exception {
  ModuleMetadataCheck.seedProfile();
  dev.nebula.client.runtime.ClientContext context=new dev.nebula.client.runtime.ClientContext((SettingsManager)value,java.nio.file.Paths.get("build/test-data/combat/config.txt"),null);
  Field f=Main.class.getDeclaredField("context");f.setAccessible(true);f.set(null,context);
 }

 public static void movement(float yaw,float pitch){events.add("move:"+yaw+":"+pitch);}
 static byte[] playerClass(){
  String own="net/minecraft/client/entity/EntityPlayerSP", base="CombatTickCheck$Living";
  ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_FRAMES);w.visit(V1_8,ACC_PUBLIC,own,null,base,null);
  MethodVisitor m=w.visitMethod(ACC_PUBLIC,"<init>","()V",null,null);m.visitCode();m.visitVarInsn(ALOAD,0);m.visitMethodInsn(INVOKESPECIAL,base,"<init>","()V",false);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();
  m=w.visitMethod(ACC_PUBLIC,"onUpdateWalkingPlayer","()V",null,null);m.visitCode();Label alive=new Label();m.visitVarInsn(ALOAD,0);m.visitFieldInsn(GETFIELD,own,"isDead","Z");m.visitJumpInsn(IFEQ,alive);m.visitInsn(RETURN);m.visitLabel(alive);
  m.visitVarInsn(ALOAD,0);m.visitFieldInsn(GETFIELD,own,"rotationYaw","F");m.visitVarInsn(ALOAD,0);m.visitFieldInsn(GETFIELD,own,"rotationPitch","F");
  m.visitMethodInsn(INVOKESTATIC,"CombatTickCheck","movement","(FF)V",false);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();
  m=w.visitMethod(ACC_PUBLIC,"onLivingUpdate","()V",null,null);m.visitCode();m.visitVarInsn(ALOAD,0);m.visitFieldInsn(GETFIELD,own,"movementInput","LCombatTickCheck$MovementInput;");m.visitMethodInsn(INVOKEVIRTUAL,"CombatTickCheck$MovementInput","updatePlayerMoveState","()V",false);
  m.visitVarInsn(ALOAD,0);m.visitMethodInsn(INVOKEVIRTUAL,own,"sprintGate","()V",false);m.visitInsn(RETURN);m.visitMaxs(0,0);m.visitEnd();
  w.visitEnd();return w.toByteArray();
 }
 public static void main(String[] args)throws Exception{
  AuraTargetSelector selector=new AuraTargetSelector();
  check(selector.select(Arrays.asList(1,2),"Single",0,50)==1,"single initial");
  check(selector.select(Arrays.asList(2,1),"Single",100,50)==1,"single sticks despite priority change");
  selector.reset();check(selector.select(Arrays.asList(1,2),"Switch",0,50)==1,"switch initial");
  check(selector.select(Arrays.asList(1,2),"Switch",49,50)==1,"switch delay");
  check(selector.select(Arrays.asList(1,2),"Switch",50,50)==2,"switch rotates");
  check(selector.select(Arrays.asList(1),"Switch",51,50)==1,"despawn recovery");
  check(selector.select(Collections.emptyList(),"Single",52,50)==null,"empty reset");
  mainField("settingsManager",new SettingsManager());
  Game.instance=new Game();Game game=Game.instance;
  Mappings.MINECRAFT_INSTANCE=field(Game.class,"instance");Mappings.MINECRAFT_THE_PLAYER=field(Game.class,"player");Mappings.MINECRAFT_THE_WORLD=field(Game.class,"world");
  Mappings.MINECRAFT_GET_PLAYER_CONTROLLER=field(Game.class,"controller");Mappings.PLAYER_CONTROLLER_ATTACK_ENTITY=Controller.class.getMethod("attackEntity",Living.class,Living.class);
  Mappings.ENTITY_PLAYER_MOVEMENT_INPUT=field(Living.class,"movementInput");Mappings.MOVEMENT_INPUT_FORWARD=field(MovementInput.class,"moveForward");Mappings.MOVEMENT_INPUT_STRAFE=field(MovementInput.class,"moveStrafe");
  Mappings.MINECRAFT_GAME_SETTINGS=field(Game.class,"settings");Mappings.MINECRAFT_NET_HANDLER=Game.class.getMethod("getNetHandler");
  Mappings.WORLD_LOADED_ENTITY_LIST=field(World.class,"loadedEntityList");
  Mappings.ENTITY_IS_SPRINTING=Living.class.getMethod("isSprinting");Mappings.ENTITY_GET_EYE_HEIGHT=Living.class.getMethod("getEyeHeight");
  Mappings.ENTITY_CLASS=Living.class;Mappings.ENTITY_PLAYER_CLASS=Living.class;Mappings.ENTITY_LIVING_BASE_CLASS=Living.class;
  Mappings.ENTITY_POS_X=field(Living.class,"posX");Mappings.ENTITY_POS_Y=field(Living.class,"posY");Mappings.ENTITY_POS_Z=field(Living.class,"posZ");
  Mappings.ENTITY_ROTATION_YAW=field(Living.class,"rotationYaw");Mappings.ENTITY_ROTATION_PITCH=field(Living.class,"rotationPitch");
  Mappings.ENTITY_ON_GROUND=field(Living.class,"onGround");Mappings.ENTITY_IS_DEAD=field(Living.class,"isDead");Mappings.ENTITY_ID_FIELD=field(Living.class,"id");Mappings.ENTITY_HEIGHT_FIELD=field(Living.class,"height");
  Mappings.ENTITY_GET_HEALTH=Living.class.getMethod("getHealth");Mappings.ENTITY_GET_BOUNDING_BOX=Living.class.getMethod("getEntityBoundingBox");Mappings.ENTITY_IS_INVISIBLE=Living.class.getMethod("isInvisible");Mappings.ENTITY_PLAYER_GET_NAME=Living.class.getMethod("getName");Mappings.ENTITY_PLAYER_SWING_ITEM=Living.class.getMethod("swingItem");
  Mappings.AXIS_ALIGNED_BB_MIN_X=field(Box.class,"minX");Mappings.AXIS_ALIGNED_BB_MIN_Y=field(Box.class,"minY");Mappings.AXIS_ALIGNED_BB_MIN_Z=field(Box.class,"minZ");Mappings.AXIS_ALIGNED_BB_MAX_X=field(Box.class,"maxX");Mappings.AXIS_ALIGNED_BB_MAX_Y=field(Box.class,"maxY");Mappings.AXIS_ALIGNED_BB_MAX_Z=field(Box.class,"maxZ");
  Mappings.C02_PACKET_USE_ENTITY_CONSTRUCTOR_ENTITY_ACTION=Attack.class.getConstructor(Living.class,Action.class);Mappings.C02_PACKET_USE_ENTITY_ACTION_ATTACK=field(Action.class,"ATTACK");Mappings.NET_HANDLER_ADD_TO_SEND_QUEUE=Handler.class.getMethod("send",Object.class);
  byte[] transformed=new PlayerMotionTransformer().transform(CombatTickCheck.class.getClassLoader(),"net/minecraft/client/entity/EntityPlayerSP",null,null,playerClass());check(transformed!=null,"walking transform");
  transformed=new dev.nebula.client.injection.transformers.EntityPlayerSPTransformer().transform(CombatTickCheck.class.getClassLoader(),"net/minecraft/client/entity/EntityPlayerSP",null,null,transformed);
  check(transformed!=null,"input correction transformer");
  Class<?> type=new Loader().load(transformed);Living player=(Living)type.newInstance();player.id=99;player.rotationYaw=123;player.rotationPitch=17;game.player=player;Minecraft.invalidate();
  Living enemy=new Living();enemy.id=1;enemy.posZ=2;game.world.loadedEntityList.add(player);game.world.loadedEntityList.add(enemy);
  KillAura aura=new KillAura();aura.initializeSettings();check(Main.getSettingsManager().getSettingByName(aura,"Silent")==null,"no legacy visible mode");aura.enable();check(aura.isEnabled(),"aura enabled");
  Method walking=type.getMethod("onUpdateWalkingPlayer");
  aura.onTick();dev.nebula.client.utils.RotationUtils.Rotation frozen=dev.nebula.client.utils.RotationUtils.getTargetRotation();enemy.posX=1;walking.invoke(player);
  check(dev.nebula.client.utils.RotationUtils.getTargetRotation()==frozen,"one rotation for physics and walking packet");enemy.posX=0;
  for(int i=0;i<24;i++){aura.onTick();walking.invoke(player);}
  check(!game.handler.attacks.isEmpty(),"end-to-end attack emitted");
  check(player.rotationYaw==123 && player.rotationPitch==17,"camera untouched across attacks");
  int attackIndex=events.indexOf("attack");check(attackIndex>0 && events.get(attackIndex-1).equals("swing") && events.get(attackIndex+1).startsWith("move:"),"swing and vanilla attack precede movement");
  check(player.motionX==.6 && player.motionZ==.6 && !player.sprinting,"vanilla controller side effects applied");
  Main.getSettingsManager().getSettingByName(aura,"Mode").setValue("Multi");
  Living second=new Living();second.id=2;second.posZ=2.5;game.world.loadedEntityList.add(second);
  Living outsideRay=new Living();outsideRay.id=3;outsideRay.posX=2.5;game.world.loadedEntityList.add(outsideRay);
  Field next=KillAura.class.getDeclaredField("nextAttackAt");next.setAccessible(true);next.setLong(aura,0);
  int beforeMulti=game.handler.attacks.size();aura.onTick();walking.invoke(player);
  check(game.handler.attacks.size()==beforeMulti+2,"Multi only attacks eligible targets on reported ray");
  Mappings.ENTITY_GET_BOUNDING_BOX=null;next.setLong(aura,0);int beforeFallback=game.handler.attacks.size();aura.onTick();walking.invoke(player);
  check(game.handler.attacks.size()>beforeFallback,"missing bounding-box mapping fallback");
  check(player.rotationYaw==123 && player.rotationPitch==17,"camera unchanged with Multi and fallback");
  Object inventoryOwner=new Object();Main.getContext().inventoryLease().acquire(inventoryOwner);next.setLong(aura,0);int beforeInventory=game.handler.attacks.size();aura.onTick();walking.invoke(player);check(game.handler.attacks.size()==beforeInventory,"inventory action suspends attacks");Main.getContext().inventoryLease().release(inventoryOwner);
  aura.disable();float cameraYaw=player.rotationYaw,cameraPitch=player.rotationPitch;
  player.rotationYaw=0;dev.nebula.client.utils.RotationUtils.setTargetRotation(90,0);type.getMethod("onLivingUpdate").invoke(player);
  check(player.movementInput.moveStrafe==1 && player.movementInput.moveForward==0,"corrected input");
  check(player.sprintGateForward==0,"input correction precedes vanilla sprint gate");
  dev.nebula.client.utils.RotationUtils.clearTargetRotation();type.getMethod("onLivingUpdate").invoke(player);
  check(player.movementInput.moveForward==1 && player.movementInput.moveStrafe==0,"vanilla input restored without target");
  player.rotationYaw=cameraYaw;player.rotationPitch=cameraPitch;
  int count=game.handler.attacks.size();walking.invoke(player);check(game.handler.attacks.size()==count,"disabled stops attacks");
  System.out.println("PASS target modes, swing/vanilla attack BEFORE movement, local sprint/velocity effects, frozen rotation, input BEFORE sprint gate, camera unchanged");
 }
}
