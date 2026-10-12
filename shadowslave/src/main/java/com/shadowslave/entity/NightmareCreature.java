package com.shadowslave.entity;

import com.shadowslave.ShadowSlaveMod;
import com.shadowslave.content.Content;
import com.shadowslave.nightmare.*;
import com.shadowslave.world.Sanctuary;
import java.util.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class NightmareCreature extends Monster {
  private static final EntityDataAccessor<Boolean> BOSS =
      SynchedEntityData.defineId(NightmareCreature.class, EntityDataSerializers.BOOLEAN);
  private static final EntityDataAccessor<Integer> FAMILY =
      SynchedEntityData.defineId(NightmareCreature.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Boolean> CHARGING =
      SynchedEntityData.defineId(NightmareCreature.class, EntityDataSerializers.BOOLEAN);
  public static final int ATTACK_IDLE = 0, ATTACK_WINDUP = 1, ATTACK_STRIKE = 2,
      ATTACK_RECOVERY = 3;
  private static final EntityDataAccessor<Integer> ATTACK_PHASE =
      SynchedEntityData.defineId(NightmareCreature.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Long> ATTACK_STARTED =
      SynchedEntityData.defineId(NightmareCreature.class, EntityDataSerializers.LONG);
  private static final EntityDataAccessor<Integer> ATTACK_DURATION =
      SynchedEntityData.defineId(NightmareCreature.class, EntityDataSerializers.INT);
  public static final ResourceKey<DamageType> NIGHTMARE_DAMAGE =
      ResourceKey.create(Registries.DAMAGE_TYPE, ShadowSlaveMod.id("nightmare"));
  private static final EntityDataAccessor<String> SPECIES =
      SynchedEntityData.defineId(NightmareCreature.class, EntityDataSerializers.STRING);
  private static final EntityDataAccessor<Boolean> ECHO =
      SynchedEntityData.defineId(NightmareCreature.class, EntityDataSerializers.BOOLEAN);
  public UUID trialId, owner;
  public int encounter = -1, rank = 1, creatureClass = 1;
  private int windup, pulseClock;
  public int specialWindup, specialCooldown = 60, specialFollowThrough;
  public Vec3 specialTarget = Vec3.ZERO;
  public final Set<UUID> specialHits = new HashSet<>();
  private final ServerBossEvent bossBar =
      new ServerBossEvent(
          Component.literal("Nightmare"),
          BossEvent.BossBarColor.YELLOW,
          BossEvent.BossBarOverlay.PROGRESS);

  public NightmareCreature(EntityType<? extends Monster> type, Level level) {
    super(type, level);
    xpReward = 5;
  }

  public static AttributeSupplier.Builder attributes() {
    return Monster.createMonsterAttributes()
        .add(Attributes.MAX_HEALTH, 18)
        .add(Attributes.MOVEMENT_SPEED, .25)
        .add(Attributes.ATTACK_DAMAGE, 3)
        .add(Attributes.FOLLOW_RANGE, 30)
        .add(Attributes.FLYING_SPEED, .35);
  }

  @Override
  protected void defineSynchedData(SynchedEntityData.Builder builder) {
    super.defineSynchedData(builder);
    builder.define(ECHO, false);
    builder.define(SPECIES, "");
    builder.define(BOSS, false);
    builder.define(FAMILY, 0);
    builder.define(CHARGING, false);
    builder.define(ATTACK_PHASE, ATTACK_IDLE);
    builder.define(ATTACK_STARTED, 0L);
    builder.define(ATTACK_DURATION, 1);
  }

  public boolean isEcho() {
    return entityData.get(ECHO);
  }

  public void makeEcho(UUID owner) {
    this.owner = owner;
    entityData.set(ECHO, true);
    setPersistenceRequired();
    setCustomName(Component.literal("Echo · " + species().name()));
  }

  @Override
  public boolean canAttack(LivingEntity target) {
    return isEcho()
        ? (target instanceof NightmareCreature n && !n.isEcho())
        : super.canAttack(target);
  }

  public String speciesId() {
    return entityData.get(SPECIES);
  }

  public CreatureEcology.Species species() {
    return CreatureEcology.get(speciesId());
  }

  public void configureSpecies(CreatureEcology.Species s) {
    configure(s.family(), false, s.rank(), null, null, -1);
    entityData.set(SPECIES, s.id());
    configureMovement();
    creatureClass = s.cores();
    refreshDimensions();
    double base = new double[] {24, 48, 135, 360, 1100, 3200, 10000}[s.rank()];
    getAttribute(Attributes.MAX_HEALTH).setBaseValue(base * (1 + (s.cores() - 1) * .38));
    getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(3 + s.rank() * 3 + s.cores() * 1.2);
    getAttribute(Attributes.ARMOR)
        .setBaseValue(
            List.of("armor", "brood", "dark_regenerate").contains(s.behavior())
                ? 6 + s.rank() * 2
                : 2);
    getAttribute(Attributes.MOVEMENT_SPEED)
        .setBaseValue(s.body() == 11 ? .01 : s.body() == 9 ? .08 : s.body() == 3 ? .30 : .24);
    getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(Math.min(.85, .1 * s.cores()));
    setHealth(getMaxHealth());
    xpReward = 8 + s.rank() * 8 + s.cores() * 3;
    setCustomName(
        Component.literal(
            s.name()
                + " · "
                + CreatureEcology.RANKS[s.rank()]
                + " "
                + CreatureEcology.CLASSES[s.cores()]));
    setCustomNameVisible(false);
  }

  private void configureMovement() {
    var s = species();
    boolean airborne = s != null && (s.body() == 5 || s.body() == 6 || s.id().equals("chain_worm"));
    setNoGravity(airborne);
    if (airborne) {
      navigation = new net.minecraft.world.entity.ai.navigation.FlyingPathNavigation(this, level());
      moveControl = new net.minecraft.world.entity.ai.control.FlyingMoveControl(this, 20, true);
    } else if (navigation instanceof net.minecraft.world.entity.ai.navigation.FlyingPathNavigation) {
      navigation = new net.minecraft.world.entity.ai.navigation.GroundPathNavigation(this, level());
      moveControl = new net.minecraft.world.entity.ai.control.MoveControl(this);
    }
  }

  @Override
  public boolean onClimbable() {
    return species() != null && species().body() == 2 && horizontalCollision || super.onClimbable();
  }

  public boolean isCharging() {
    return entityData.get(CHARGING);
  }

  public void setCharging(boolean value) {
    boolean changed = value != isCharging();
    entityData.set(CHARGING, value);
    if (!changed || level().isClientSide) return;
    if (value) {
      startAttackPhase(ATTACK_WINDUP, Math.max(1, Math.max(windup, specialWindup)));
    } else if (getTarget() != null && getTarget().isAlive()) {
      startAttackPhase(ATTACK_STRIKE, 10);
    } else {
      startAttackPhase(ATTACK_IDLE, 1);
    }
  }

  /** Authoritative animation timing also reaches clients that start tracking mid-attack. */
  public int attackPhase() {
    return entityData.get(ATTACK_PHASE);
  }

  public float attackPhaseProgress(float partialTick) {
    if (attackPhase() == ATTACK_IDLE) return 0;
    float partial = Float.isFinite(partialTick) ? Math.clamp(partialTick, 0, 1) : 0;
    double elapsed = level().getGameTime() - entityData.get(ATTACK_STARTED) + partial;
    return (float) Math.clamp(elapsed / entityData.get(ATTACK_DURATION), 0, 1);
  }

  /** Matches the 0..1 strike input used by the species rig; windup is separately posed. */
  public float attackAnimationProgress(float partialTick) {
    return attackPhase() == ATTACK_STRIKE ? attackPhaseProgress(partialTick) : 0;
  }

  private void startAttackPhase(int phase, int ticks) {
    entityData.set(ATTACK_PHASE, phase);
    entityData.set(ATTACK_STARTED, level().getGameTime());
    entityData.set(ATTACK_DURATION, Math.max(1, ticks));
  }

  private void tickAttackAnimation() {
    if (attackPhaseProgress(0) < 1) return;
    if (attackPhase() == ATTACK_STRIKE) startAttackPhase(ATTACK_RECOVERY, 8);
    else if (attackPhase() == ATTACK_RECOVERY) startAttackPhase(ATTACK_IDLE, 1);
  }

  public boolean isBoss() {
    return entityData.get(BOSS);
  }

  public int family() {
    return entityData.get(FAMILY);
  }

  private static final float[] WIDTHS = {2.10f, 1.60f, 1.90f, 2.40f, 2.00f, 1.70f, 2.30f};
  private static final float[] HEIGHTS = {4.20f, 4.70f, 4.40f, 4.10f, 4.60f, 4.50f, 4.90f};

  /** One source of truth for rendering, navigation, targeting and physical collision. */
  public static EntityDimensions bodyDimensions(int family, boolean boss) {
    int index = Math.clamp(family, 0, 6);
    float scale = boss ? 1.5f : 1;
    return EntityDimensions.scalable(WIDTHS[index] * scale, HEIGHTS[index] * scale)
        .withEyeHeight(HEIGHTS[index] * scale * .79f);
  }

  @Override
  protected EntityDimensions getDefaultDimensions(Pose pose) {
    var s = species();
    return s == null
        ? bodyDimensions(family(), isBoss())
        : EntityDimensions.scalable(s.width(), s.height()).withEyeHeight(s.height() * .78f);
  }

  @Override
  public void onSyncedDataUpdated(EntityDataAccessor<?> data) {
    super.onSyncedDataUpdated(data);
    if (BOSS.equals(data) || FAMILY.equals(data) || SPECIES.equals(data)) refreshDimensions();
  }

  public boolean canStandAt(net.minecraft.core.BlockPos pos) {
    var body =
        getDimensions(getPose()).makeBoundingBox(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
    if (!level().noCollision(this, body)
        || level().containsAnyLiquid(body)
        || !level().getBlockState(pos.below()).isSolid()) return false;
    // Large creatures need support under their feet, not a lone block under their centre.
    for (double x : new double[] {body.minX + .05, body.maxX - .05})
      for (double z : new double[] {body.minZ + .05, body.maxZ - .05})
        if (!level().getBlockState(net.minecraft.core.BlockPos.containing(x, pos.getY() - 1, z)).isSolid())
          return false;
    return true;
  }

  public void configure(int family, boolean boss, int rank, UUID trial, UUID owner, int encounter) {
    entityData.set(BOSS, boss);
    entityData.set(FAMILY, Math.clamp(family, 0, 6));
    refreshDimensions();
    this.rank = rank;
    this.trialId = trial;
    this.owner = owner;
    this.encounter = encounter;
    creatureClass = boss ? 4 : 1;
    getAttribute(Attributes.MAX_HEALTH)
        .setBaseValue(boss ? 240 + rank * 100 : 26 * Math.pow(1.85, rank));
    getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(boss ? 6 + rank : 3 + rank);
    getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(boss ? .245 : .235);
    setHealth(getMaxHealth());
    if (trial != null || boss) setPersistenceRequired();
    setCustomName(
        Component.literal(
            boss ? Content.trial(family).boss() : Content.trial(family).family() + " Nightmare"));
    setCustomNameVisible(boss);
    bossBar.setName(getDisplayName());
  }

  @Override
  protected void registerGoals() {
    goalSelector.addGoal(0, new FloatGoal(this));
    goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
    goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, .7));
    goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8));
    targetSelector.addGoal(1, new HurtByTargetGoal(this));
    targetSelector.addGoal(
        2,
        new NearestAttackableTargetGoal<>(
            this,
            Player.class,
            10,
            true,
            false,
            p ->
                !isEcho()
                    && (owner == null || p.getUUID().equals(owner))
                    && !Sanctuary.isSafe(p.level(), p.blockPosition())));
  }

  @Override
  public boolean doHurtTarget(Entity target) {
    if (specialWindup > 0) return false;
    if (Sanctuary.isSafe(target.level(), target.blockPosition())) return false;
    swing(InteractionHand.MAIN_HAND);
    if (!level().isClientSide) startAttackPhase(ATTACK_STRIKE, 6);
    boolean hit =
        target.hurt(
            damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
    if (hit && target instanceof LivingEntity living && species() != null) {
      switch (species().behavior()) {
        case "poison" -> living.addEffect(new MobEffectInstance(MobEffects.POISON, 100));
        case "fire" -> living.igniteForSeconds(4);
        case "frost", "web" ->
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
        case "drain" -> heal(3);
      }
    }
    return hit;
  }

  @Override
  public boolean removeWhenFarAway(double distance) {
    return trialId == null && !isBoss() && !isEcho();
  }

  @Override
  public void tick() {
    super.tick();
    if (!(level() instanceof ServerLevel level)) return;
    tickAttackAnimation();
    if (isEcho()) {
      var master = owner == null ? null : level.getServer().getPlayerList().getPlayer(owner);
      if (master == null || master.level() != level) {
        discard();
        return;
      }
      if (tickCount % 10 == 0) {
        var enemies =
            level.getEntitiesOfClass(
                NightmareCreature.class,
                getBoundingBox().inflate(18),
                n ->
                    n != this
                        && !n.isEcho()
                        && n.isAlive()
                        && n.trialId == null
                        && !Sanctuary.isSafe(level, n.blockPosition()));
        var target =
            enemies.stream()
                .min(java.util.Comparator.comparingDouble(this::distanceToSqr))
                .orElse(null);
        setTarget(target);
        if (target == null && distanceToSqr(master) > 16) getNavigation().moveTo(master, 1.3);
        if (distanceToSqr(master) > 1600) {
          var pos = master.blockPosition().offset(4, 0, 0);
          if (canStandAt(pos)) teleportTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        }
      }
      return;
    }
    if (trialId == null) {
      if (getTarget() != null && Sanctuary.isSafe(level, getTarget().blockPosition())) {
        setTarget(null);
        getNavigation().stop();
      }
      if (tickCount % 10 == 0 && Sanctuary.isSafe(level, blockPosition())) {
        discard();
        return;
      }
    }
    if (trialId != null && tickCount % 20 == 0) {
      var t = SpellSavedData.get(level.getServer()).instances.get(trialId);
      if (t == null || !t.state.equals("active")) {
        discard();
        return;
      }
      var player = level.getServer().getPlayerList().getPlayer(t.owner);
      if (player == null || !player.level().dimension().equals(level.dimension())) {
        setTarget(null);
        setNoAi(true);
        return;
      }
      setNoAi(false);
      if (!t.contains(blockPosition()))
        teleportTo(t.origin().getX() + .5, 81, t.origin().getZ() + .5);
    }
    AncientCombat.tick(this, level);
    tickSpecies(level);
    if (!isBoss()) return;
    setCharging(windup > 0);
    bossBar.setProgress(getHealth() / getMaxHealth());
    if (getTarget() == null || isNoAi()) return;
    if (windup > 0) {
      getNavigation().stop();
      windup--;
      double angle = tickCount * .5;
      level.sendParticles(
          ParticleTypes.SOUL_FIRE_FLAME,
          getX() + Math.cos(angle) * 6,
          getY() + .2,
          getZ() + Math.sin(angle) * 6,
          3,
          .15,
          .1,
          .15,
          0);
      if (windup == 0) pulse(level);
    } else if (++pulseClock >= (getHealth() < getMaxHealth() * .5 ? 85 : 140)) {
      pulseClock = 0;
      windup = 30;
      playSound(SoundEvents.WARDEN_SONIC_CHARGE, .65f, 1.7f);
      if (getTarget() instanceof ServerPlayer p)
        p.displayClientMessage(
            Component.literal("The boss gathers power — move beyond the ring!"), true);
    }
  }

  private void tickSpecies(ServerLevel level) {
    var s = species();
    if (s == null || isNoAi()) return;
    if (tickCount % 60 == 0
        && (s.behavior().equals("regenerate")
            || s.behavior().equals("dark_regenerate")
                && level.getMaxLocalRawBrightness(blockPosition()) < 8)) heal(2);
    var target = getTarget();
    if (target == null
        || specialWindup > 0
        || Sanctuary.isSafe(level, target.blockPosition())
        || !hasLineOfSight(target)) return;
    // Offensive skills now use AncientCombat's windup and recovery. Brood spawning is separate.
    if (tickCount % 100 == 0
        && distanceToSqr(target) < 196
        && s.behavior().equals("brood")
        && level.getEntitiesOfClass(NightmareCreature.class, getBoundingBox().inflate(24)).size()
            < 10) {
      var child = ShadowSlaveMod.CREATURE.get().create(level);
      if (child != null) {
        child.configureSpecies(
            CreatureEcology.get(
                s.id().equals("iron_spider_matriarch") ? "iron_spider" : "stone_saint"));
        var pos = blockPosition().offset(5, 0, 0);
        if (child.canStandAt(pos)) {
          child.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, 0, 0);
          level.addFreshEntity(child);
        }
      }
    }
  }

  private void pulse(ServerLevel level) {
    level.sendParticles(
        ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 1, getZ(), 70, 4, .5, 4, .06);
    for (var p :
        level.getEntitiesOfClass(
            ServerPlayer.class,
            getBoundingBox().inflate(6),
            p -> owner == null || p.getUUID().equals(owner))) {
      if (Sanctuary.isSafe(level, p.blockPosition())) continue;
      if (p.distanceToSqr(this) > 36) continue;
      p.hurt(
          new DamageSource(
              level
                  .registryAccess()
                  .registryOrThrow(Registries.DAMAGE_TYPE)
                  .getHolderOrThrow(NIGHTMARE_DAMAGE),
              this),
          5 + rank);
      switch (family()) {
        case 0 -> p.igniteForSeconds(3);
        case 1 -> p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 70));
        case 2 -> p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
        case 3 -> p.knockback(1.6, getX() - p.getX(), getZ() - p.getZ());
        case 4 -> p.addEffect(new MobEffectInstance(MobEffects.POISON, 60));
        case 5 -> p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 50));
        case 6 -> p.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 30));
      }
    }
  }

  @Override
  public void startSeenByPlayer(ServerPlayer p) {
    super.startSeenByPlayer(p);
    if (isBoss()) bossBar.addPlayer(p);
  }

  @Override
  public void stopSeenByPlayer(ServerPlayer p) {
    super.stopSeenByPlayer(p);
    bossBar.removePlayer(p);
  }

  @Override
  public void addAdditionalSaveData(CompoundTag n) {
    super.addAdditionalSaveData(n);
    n.putBoolean("SpellEcho", isEcho());
    n.putString("SpellSpecies", speciesId());
    n.putBoolean("SpellBoss", isBoss());
    n.putInt("SpellFamily", family());
    n.putInt("SpellRank", rank);
    n.putInt("SpellClass", creatureClass);
    n.putInt("Encounter", encounter);
    if (trialId != null) n.putUUID("Trial", trialId);
    if (owner != null) n.putUUID("Owner", owner);
  }

  @Override
  public void readAdditionalSaveData(CompoundTag n) {
    super.readAdditionalSaveData(n);
    entityData.set(ECHO, n.getBoolean("SpellEcho"));
    entityData.set(SPECIES, n.getString("SpellSpecies"));
    entityData.set(BOSS, n.getBoolean("SpellBoss"));
    entityData.set(FAMILY, n.getInt("SpellFamily"));
    rank = n.getInt("SpellRank");
    creatureClass = n.getInt("SpellClass");
    encounter = n.getInt("Encounter");
    trialId = n.hasUUID("Trial") ? n.getUUID("Trial") : null;
    owner = n.hasUUID("Owner") ? n.getUUID("Owner") : null;
    bossBar.setName(getDisplayName());
    configureMovement();
    refreshDimensions();
  }
}
