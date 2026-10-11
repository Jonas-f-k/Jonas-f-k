package com.shadowslave.network;

import com.shadowslave.ShadowSlaveMod;
import com.shadowslave.combat.Aspects;
import com.shadowslave.core.Settings;
import com.shadowslave.memory.Memories;
import com.shadowslave.nightmare.Nightmares;
import com.shadowslave.world.DreamRealm;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class SpellNetwork {
  /** Client-only data holder has no references to Minecraft client classes. */
  public static volatile CompoundTag clientState = new CompoundTag();

  public static final java.util.Map<java.util.UUID, java.util.List<String>> appearances =
      new java.util.concurrent.ConcurrentHashMap<>();
  public static final java.util.Map<java.util.UUID, Integer> forms =
      new java.util.concurrent.ConcurrentHashMap<>();
  public static final java.util.Map<java.util.UUID, Long> beastAttacks =
      new java.util.concurrent.ConcurrentHashMap<>();

  public static final java.util.concurrent.ConcurrentLinkedQueue<Visual> visuals = new java.util.concurrent.ConcurrentLinkedQueue<>();
  public record Visual(net.minecraft.resources.ResourceLocation dimension, int family, int kind,
      double x,double y,double z,float radius,int duration) implements CustomPacketPayload {
    public Visual {
      if (dimension == null || family<0 || family>6 || kind<0 || kind>6 || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
          || Math.abs(x)>30000000 || Math.abs(z)>30000000 || Math.abs(y)>10000 || !Float.isFinite(radius) || radius<.25f || radius>32 || duration<1 || duration>80)
        throw new IllegalArgumentException("Invalid Spell visual");
    }
    public static final Type<Visual> TYPE = new Type<>(ShadowSlaveMod.id("visual"));
    public static final StreamCodec<RegistryFriendlyByteBuf,Visual> CODEC = new StreamCodec<>() {
      public Visual decode(RegistryFriendlyByteBuf b){return new Visual(b.readResourceLocation(),b.readVarInt(),b.readVarInt(),b.readDouble(),b.readDouble(),b.readDouble(),b.readFloat(),b.readVarInt());}
      public void encode(RegistryFriendlyByteBuf b,Visual v){b.writeResourceLocation(v.dimension);b.writeVarInt(v.family);b.writeVarInt(v.kind);b.writeDouble(v.x);b.writeDouble(v.y);b.writeDouble(v.z);b.writeFloat(v.radius);b.writeVarInt(v.duration);}
    };
    public Type<? extends CustomPacketPayload> type(){return TYPE;}
  }
  public static void visual(net.minecraft.server.level.ServerLevel level,int family,int type,net.minecraft.world.phys.Vec3 at,double radius,int duration){
    PacketDistributor.sendToPlayersNear(level,null,at.x,at.y,at.z,96,new Visual(level.dimension().location(),Math.clamp(family,0,6),type,at.x,at.y,at.z,(float)Math.clamp(radius,.25,32),Math.clamp(duration,1,80)));
  }

  public record Appearance(
      java.util.UUID player, java.util.List<String> memories, int form, long attackUntil)
      implements CustomPacketPayload {
    public static final Type<Appearance> TYPE = new Type<>(ShadowSlaveMod.id("appearance"));
    public static final StreamCodec<RegistryFriendlyByteBuf, Appearance> CODEC =
        new StreamCodec<>() {
          public Appearance decode(RegistryFriendlyByteBuf b) {
            var id = b.readUUID();
            int n = b.readVarInt();
            if (n < 0 || n > 7) throw new IllegalArgumentException("Appearance size");
            var a = new java.util.ArrayList<String>();
            for (int i = 0; i < n; i++) a.add(b.readUtf(80));
            int form = b.readVarInt();
            if (form < -1 || form > 6) throw new IllegalArgumentException("Beast form");
            return new Appearance(id, java.util.List.copyOf(a), form, b.readLong());
          }

          public void encode(RegistryFriendlyByteBuf b, Appearance a) {
            b.writeUUID(a.player);
            b.writeVarInt(a.memories.size());
            for (String id : a.memories) b.writeUtf(id, 80);
            b.writeVarInt(a.form);
            b.writeLong(a.attackUntil);
          }
        };

    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public record State(CompoundTag data) implements CustomPacketPayload {
    public static final Type<State> TYPE = new Type<>(ShadowSlaveMod.id("state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC =
        new StreamCodec<>() {
          public State decode(RegistryFriendlyByteBuf b) {
            var n = b.readNbt();
            return new State(n == null ? new CompoundTag() : n);
          }

          public void encode(RegistryFriendlyByteBuf b, State s) {
            b.writeNbt(s.data);
          }
        };

    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public record Action(String operation, String argument) implements CustomPacketPayload {
    public static final Type<Action> TYPE = new Type<>(ShadowSlaveMod.id("action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, Action> CODEC =
        new StreamCodec<>() {
          public Action decode(RegistryFriendlyByteBuf b) {
            return new Action(b.readUtf(24), b.readUtf(80));
          }

          public void encode(RegistryFriendlyByteBuf b, Action a) {
            b.writeUtf(a.operation, 24);
            b.writeUtf(a.argument, 80);
          }
        };

    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public static void register(RegisterPayloadHandlersEvent event) {
    var r = event.registrar("5");
    r.playToClient(Visual.TYPE,Visual.CODEC,(payload,context)->{while(visuals.size()>=64)visuals.poll();visuals.add(payload);});
    r.playToClient(
        Appearance.TYPE,
        Appearance.CODEC,
        (payload, context) -> {
          appearances.put(payload.player(), payload.memories());
          int before = forms.getOrDefault(payload.player(), -1);
          if (payload.form() < 0) {
            forms.remove(payload.player());
            beastAttacks.remove(payload.player());
          } else {
            forms.put(payload.player(), payload.form());
            beastAttacks.put(payload.player(), payload.attackUntil());
          }
          if (before != payload.form()) {
            var player = context.player().level().getPlayerByUUID(payload.player());
            if (player != null) player.refreshDimensions();
          }
        });
    r.playToClient(
        State.TYPE,
        State.CODEC,
        (payload, context) -> {
          clientState = payload.data();
          var id = context.player().getUUID();
          int form = payload.data().getInt("beastForm"), before = forms.getOrDefault(id, -1);
          if (form < 0) {
            forms.remove(id);
            beastAttacks.remove(id);
          } else {
            forms.put(id, form);
            beastAttacks.put(id, payload.data().getLong("beastAttackUntil"));
          }
          if (form != before) context.player().refreshDimensions();
        });
    r.playToServer(
        Action.TYPE,
        Action.CODEC,
        (payload, context) -> {
          if (context.player() instanceof ServerPlayer p)
            handle(p, payload.operation(), payload.argument());
        });
  }

  public static void handle(ServerPlayer p, String operation, String argument) {
    var d = ShadowSlaveMod.soul(p);
    long now = p.server.overworld().getGameTime();
    if (now - d.lastActionTick < 3) return;
    d.lastActionTick = now;
    switch (operation) {
      case "refresh" -> {}
      case "equip" -> {
        if (!Memories.equip(d, argument))
          p.displayClientMessage(
              Component.literal(
                  "Cannot equip: check ownership, seven-slot capacity and item type limits."),
              true);
        Memories.removeManifested(p);
      }
      case "cast" -> Aspects.cast(p);
      case "awakened" -> Aspects.castAwakened(p);
      case "ascended" -> com.shadowslave.combat.HigherAspects.cast(p);
      case "transform" -> com.shadowslave.combat.HigherAspects.transform(p);
      case "manifest" -> Memories.manifest(p, argument);
      case "crystallize" -> Memories.crystallize(p, argument);
      case "toggle" -> {
        if (d.initiated()) {
          if (d.activeMemories) {
            d.activeMemories = false;
            Memories.removeManifested(p);
          } else if (d.spend(10)) d.activeMemories = true;
        }
      }
      case "retry" -> {
        if (d.stage.equals("retry")
            || d.stage.equals("aspirant") && d.elapsed >= Settings.COUNTDOWN_DAYS.get() * 24000L)
          Nightmares.start(p, -1);
      }
      case "abandon" -> Nightmares.fail(p);
      case "dream" -> {
        if (d.soulRank >= 2) DreamRealm.enter(p);
        else
          p.displayClientMessage(
              Component.literal(
                  "Sleep in an Overworld bed to enter your first Dream Realm journey."),
              true);
      }
      default -> {
        return;
      }
    }
    sync(p);
  }

  public static void sync(ServerPlayer p) {
    com.shadowslave.player.SoulGrowth.apply(p);
    Memories.updateArmor(p);
    var d = ShadowSlaveMod.soul(p);
    var n = d.serializeNBT(p.registryAccess());
    n.remove("inventory");
    n.remove("return");
    n.putInt("maxEssence", d.maxEssence());
    n.putInt("cores", d.cores());
    n.putLong(
        "cooldownRemaining", Math.max(0, d.cooldownUntil - p.server.overworld().getGameTime()));
    n.putLong(
        "awakenedCooldownRemaining",
        Math.max(0, d.awakenedCooldownUntil - p.server.overworld().getGameTime()));
    n.putLong(
        "ascendedCooldownRemaining",
        Math.max(0, d.ascendedCooldownUntil - p.server.overworld().getGameTime()));
    n.putLong(
        "transformCooldownRemaining",
        Math.max(0, d.transformCooldownUntil - p.server.overworld().getGameTime()));
    n.putLong(
        "beastCooldownRemaining",
        Math.max(0, d.beastCooldownUntil - p.server.overworld().getGameTime()));
    n.putLong("formRemaining", Math.max(0, d.transformUntil - p.server.overworld().getGameTime()));
    n.putLong("beastAttackUntil", d.beastAttackUntil);
    n.putInt("beastForm", com.shadowslave.combat.BeastForms.active(p));
    n.putDouble("coreSaturation", com.shadowslave.core.Rules.saturation(d.cores(), d.fragments));
    n.putDouble("abilityMultiplier", com.shadowslave.player.SoulGrowth.ability(d));
    n.putFloat("maxHealth", p.getMaxHealth());
    n.putInt(
        "dormantCooldownTicks",
        com.shadowslave.player.SoulGrowth.cooldown(d, d.soulRank >= 2 ? 140 : 280));
    n.putInt("awakenedCooldownTicks", com.shadowslave.player.SoulGrowth.cooldown(d, 240));
    n.putInt("ascendedCooldownTicks", com.shadowslave.player.SoulGrowth.cooldown(d, 360));
    n.putLong("countdownTicks", Settings.COUNTDOWN_DAYS.get() * 24000L);
    n.putBoolean("inDream", DreamRealm.isDream(p.level()));
    var t = Nightmares.current(p);
    if (t != null) {
      n.putInt("targetRank", t.targetRank);
      n.putInt("sealCount", t.seals());
      n.putInt("points", t.points);
      n.putBoolean("divine", t.bossDefeated);
      n.putInt("altars", Integer.bitCount(t.altars));
      n.putInt("kills", t.killed.size());
    }
    if (d.family >= 0) {
      var def = com.shadowslave.content.Content.trial(d.family);
      n.putString("aspectName", def.aspect());
      n.putString("flawDescription", def.flaw());
      n.putString("trialName", def.name());
    }
    PacketDistributor.sendToPlayer(p, new State(n));
    var look =
        new java.util.ArrayList<>(
            Memories.active(d).stream()
                .map(com.shadowslave.content.Content.Memory::id)
                .limit(7)
                .toList());
    PacketDistributor.sendToPlayersTrackingEntityAndSelf(
        p,
        new Appearance(
            p.getUUID(),
            look.stream().limit(7).toList(),
            com.shadowslave.combat.BeastForms.active(p),
            d.beastAttackUntil));
  }

  public static void send(String operation, String argument) {
    PacketDistributor.sendToServer(new Action(operation, argument));
  }
}
