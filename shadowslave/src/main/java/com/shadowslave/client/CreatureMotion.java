package com.shadowslave.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Pose-only animation for the native cuboid rigs. The caller restores the rest
 * pose first; all joint rotations here are deltas, so anatomical rest angles
 * survive animation. Gaits use the entity's travelled-distance phase rather
 * than a timer, preventing feet from cycling while a creature stands still.
 */
public final class CreatureMotion {
  private CreatureMotion() {}

  public static void animate(ModelPart rig, String id, float swing, float stride,
      float age, float yaw, float pitch, float strike, boolean charging, boolean flying) {
    float movement = Mth.clamp(stride, 0, 1.25f);
    float progress = Mth.clamp(strike, 0, 1);
    // The server holds the windup before the strike starts. Begin an active
    // strike in that same held pose, release by .25, then recover to rest.
    // Zero is the idle sentinel; the caller supplies epsilon on frame one.
    float anticipation = progress > 0 ? 1 - smooth(.02f, .25f, progress) : 0;
    float release = smooth(.025f, .25f, progress) * (1 - smooth(.25f, 1, progress));
    float recovery = smooth(.28f, .52f, progress) * (1 - smooth(.52f, 1, progress));
    float prepare = charging ? 1 : anticipation;
    float breath = Mth.sin(age * .065f);
    ModelPart head = child(rig, "head");
    ModelPart body = child(rig, "body");

    hide(child(rig, "runes"));
    if (head != null) hide(child(head, "eyes"));
    // Move the complete skeleton, never pull its torso away from its neck/legs.
    rig.y += breath * .055f;

    switch (id) {
      case "carapace_scavenger", "carapace_centurion", "carapace_demon" ->
          crab(rig, head, movement, swing, age, prepare, release, recovery);
      case "iron_spider", "iron_spider_matriarch", "glass_huntsman" ->
          spider(rig, head, movement, swing, age, prepare, release);
      case "black_ravager", "dread_wolf", "rime_stalker", "moss_hart", "cinder_hound", "wolf", "stag" ->
          quadruped(rig, head, id, movement, swing, age, prepare, release, flying);
      case "chain_worm", "ivory_burrower", "azure_serpent", "leviathan" ->
          serpent(rig, body, head, id, movement, swing, age, prepare, release);
      case "spire_messenger", "aurora_moth", "griffin", "dragon", "wyvern" ->
          flyer(rig, head, id, movement, swing, age, prepare, release, flying);
      case "blood_flower", "briar_maw" -> plant(rig, head, age, prepare, release);
      case "soul_devouring_tree" -> tree(rig, head, age, prepare, release);
      case "mordant_mimic" -> mimic(rig, head, movement, swing, age, prepare, release);
      case "rolling_stone" -> stone(rig, body, movement, swing, age, prepare, release);
      case "gilded_mantis", "flesh_reaver" ->
          reaver(rig, head, id, movement, swing, age, prepare, release);
      case "moonveil_wraith", "chalice_maiden", "hollow_watcher" ->
          apparition(rig, body, head, id, age, prepare, release);
      case "lord_of_dead" -> ossuary(rig, body, age, prepare, release);
      default -> humanoid(rig, body, head, id, movement, swing, age, prepare, release, recovery);
    }

    if (head != null && !id.equals("mordant_mimic")) {
      float limit = isArthropod(id) ? 23 : id.equals("soul_devouring_tree") || id.equals("lord_of_dead") ? 12 : 38;
      head.yRot += Mth.clamp(yaw, -limit, limit) * Mth.DEG_TO_RAD;
      head.xRot += Mth.clamp(pitch, -23, 25) * Mth.DEG_TO_RAD;
    }
    if (head != null && !id.equals("mordant_mimic")) {
      ModelPart jaw = child(head, "jaw");
      if (jaw != null) jaw.xRot += .012f + .014f * breath + prepare * .12f + release * .38f;
    }
  }

  private static void crab(ModelPart rig, ModelPart head, float move, float swing,
      float age, float prepare, float hit, float recover) {
    float phase = swing * .88f;
    // Opposing leg sets form a lateral scuttle; the shoulder does not pedal.
    radialSteps(rig, move, phase, .24f, .14f, .3f);
    rig.y += Mth.sin(phase * 2) * move * .1f + prepare * .28f - hit * .18f;
    rig.zRot += Mth.sin(phase) * move * .018f;
    if (head != null) head.xRot += prepare * .06f - hit * .13f;
    for (int side : new int[]{-1, 1}) {
      ModelPart arm = child(rig, "arm_" + side);
      if (arm == null) continue;
      float offset = side == -1 ? 1 : .7f;
      arm.xRot += -.12f + prepare * .32f - hit * .65f * offset + recover * .1f;
      arm.yRot += side * (.035f * Mth.sin(age * .045f) + prepare * .16f - hit * .17f);
      ModelPart elbow = child(arm, "elbow");
      if (elbow == null) continue;
      elbow.xRot += -prepare * .42f + hit * .22f;
      // Hinge the two fingers against each other, then clamp on release.
      ModelPart pincer = child(elbow, "pincer");
      ModelPart thumb = child(elbow, "thumb");
      float gape = .07f + prepare * .5f - hit * .055f;
      if (pincer != null) pincer.yRot += -side * gape;
      if (thumb != null) thumb.yRot += side * gape * 1.25f;
      ModelPart hand = child(elbow, "hand");
      if (hand != null) {
        ModelPart finger = child(hand, "pincer");
        ModelPart opposing = child(hand, "thumb");
        if (finger != null) finger.yRot += -side * gape;
        if (opposing != null) opposing.yRot += side * gape * 1.25f;
      }
    }
  }

  private static void spider(ModelPart rig, ModelPart head, float move, float swing,
      float age, float prepare, float hit) {
    radialSteps(rig, move, swing * .96f, .34f, .075f, .37f);
    rig.y += prepare * .32f - hit * .2f;
    rig.xRot += -prepare * .045f + hit * .055f;
    if (head != null) head.xRot += -hit * .09f;
    ModelPart body = child(rig, "body");
    if (body != null) {
      ModelPart abdomen = child(body, "abdomen");
      if (abdomen != null) abdomen.xRot += Mth.sin(age * .055f) * .012f + prepare * .045f;
    }
  }

  private static void radialSteps(ModelPart rig, float move, float phase,
      float sweep, float foreAft, float lift) {
    for (int i = 0; i < 8; i++) {
      ModelPart leg = child(rig, "leg_" + i);
      if (leg == null) continue;
      int side = i % 2 == 0 ? -1 : 1;
      // Alternating left/right legs and alternating front/rear pairs keep four
      // support points planted while the other four swing forward.
      float cycle = phase + ((i / 2 + i % 2) % 2) * Mth.PI;
      float arc = Mth.sin(cycle);
      float lifted = Math.max(0, arc) * move;
      leg.yRot += side * Mth.cos(cycle) * move * sweep;
      leg.xRot += arc * move * foreAft;
      leg.zRot += side * lifted * lift;
      ModelPart knee = child(leg, "knee");
      if (knee != null) {
        knee.zRot -= side * lifted * lift * .75f;
        knee.xRot += lifted * .08f;
        ModelPart foot = child(knee, "foot");
        if (foot != null) foot.zRot -= side * lifted * lift * .25f;
      }
    }
  }

  private static void quadruped(ModelPart rig, ModelPart head, String id, float move,
      float swing, float age, float prepare, float hit, boolean airborne) {
    boolean heavy = id.equals("black_ravager");
    float gait = swing * (heavy ? .57f : .76f);
    for (int i = 0; i < 4; i++) {
      ModelPart leg = child(rig, "leg_" + i);
      if (leg == null) continue;
      // Front-left/rear-right and front-right/rear-left form a diagonal trot.
      float phase = gait + (i == 0 || i == 3 ? 0 : Mth.PI);
      float lift = Math.max(0, Mth.sin(phase));
      leg.xRot += Mth.cos(phase) * move * (heavy ? .36f : .52f);
      leg.xRot += i < 2 ? prepare * .18f - hit * .35f : -prepare * .2f + hit * .18f;
      ModelPart knee = child(leg, "knee");
      if (knee != null) {
        knee.xRot += -lift * move * .42f - prepare * .16f;
        if (airborne) knee.xRot -= .35f;
        ModelPart foot = child(knee, "foot");
        if (foot != null) foot.xRot += lift * move * .13f;
      }
    }
    rig.y += Mth.cos(gait * 2) * move * (heavy ? .08f : .15f) + prepare * .35f;
    rig.xRot += prepare * .045f - hit * .1f;
    if (head != null) {
      head.xRot += -prepare * .09f + hit * .13f - Mth.cos(gait * 2) * move * .015f;
      for (int side : new int[]{-1, 1}) {
        ModelPart ear = child(head, "ear_" + side);
        if (ear != null) ear.xRot += .02f * Mth.sin(age * .035f + side) + prepare * .12f;
      }
    }
    ModelPart tail = child(rig, "tail");
    if (tail != null) chainWave(tail, age * .07f + gait * .18f, .045f + move * .08f, .62f, 0);
  }

  private static void serpent(ModelPart rig, ModelPart body, ModelPart head, String id,
      float move, float swing, float age, float prepare, float hit) {
    boolean worm = id.equals("chain_worm") || id.equals("ivory_burrower");
    float phase = swing * .65f + age * .026f;
    if (body != null) {
      if (worm) {
        rig.xRot += Mth.sin(phase) * (.012f + move * .035f);
        peristalsis(child(body, "segment"), phase - .65f, .025f + move * .105f, 0);
      } else {
        rig.yRot += Mth.sin(phase) * (.012f + move * .035f);
        chainWave(child(body, "segment"), phase - .7f, .03f + move * .14f, .68f, 0);
      }
    }
    if (head != null) {
      head.xRot += prepare * .12f - hit * .3f;
      for (int side : new int[]{-1, 1}) {
        ModelPart mandible = child(head, "mandible_" + side);
        if (mandible != null) mandible.yRot += side * (.02f + prepare * .16f - hit * .02f);
      }
    }
    rig.y += prepare * .2f;
    ModelPart tail = child(rig, "tail");
    if (tail != null) chainWave(tail, phase - 2, .035f + move * .12f, .7f, 0);
    if (id.equals("leviathan")) wings(rig, age, false, false, prepare, hit);
    if (id.equals("chain_worm")) {
      wings(rig, age, false, false, prepare, hit);
      for (int side : new int[]{-1, 1}) {
        ModelPart arm = child(rig, "arm_" + side);
        if (arm == null) continue;
        arm.xRot += -.1f - prepare * .48f - hit * .68f;
        arm.yRot += side * (prepare * .08f - hit * .17f);
        ModelPart elbow = child(arm, "elbow");
        if (elbow != null) elbow.xRot += -.08f - prepare * .24f + hit * .12f;
      }
    }
  }

  private static void flyer(ModelPart rig, ModelPart head, String id, float move,
      float swing, float age, float prepare, float hit, boolean flying) {
    boolean moth = id.equals("aurora_moth");
    boolean hover = flying || moth || id.equals("spire_messenger");
    wings(rig, age, hover, moth, prepare, hit);
    if (id.equals("griffin")) {
      quadruped(rig, head, id, move, swing, age, prepare, hit, hover);
    } else {
      for (int i = 0; i < 6; i++) {
        ModelPart leg = child(rig, "leg_" + i);
        if (leg == null) continue;
        leg.xRot += hover ? .24f : Mth.cos(swing * .68f + i * Mth.PI) * move * .4f;
        ModelPart knee = child(leg, "knee");
        if (knee != null) knee.xRot += hover ? -.58f : -Math.max(0, Mth.sin(swing * .68f + i * Mth.PI)) * move * .28f;
      }
      ModelPart tail = child(rig, "tail");
      if (tail != null) chainWave(tail, age * .065f, hover ? .08f : .035f, .65f, 0);
    }
    if (hover) {
      rig.y += Mth.sin(age * .095f) * (moth ? .32f : .16f);
      rig.xRot += move * .07f - prepare * .035f;
    }
    if (head != null) {
      head.xRot += -prepare * .1f - hit * .16f;
      for (int side : new int[]{-1, 1}) {
        ModelPart antenna = child(head, "antenna_" + side);
        if (antenna != null) antenna.xRot += Mth.sin(age * .055f + side) * .04f;
      }
    }
    for (int i = -1; i < 6; i++) {
      if (i == 0) continue;
      ModelPart arm = child(rig, "arm_" + i);
      if (arm == null) continue;
      if (id.equals("wyvern")) arm.visible = false;
      else {
        float row = i < 2 ? 0 : i / 2;
        arm.xRot += -.12f - prepare * .22f - hit * .3f;
        if (id.equals("spire_messenger")) {
          // Six taloned arms contract around the victim together; slight row
          // offsets retain a readable silhouette instead of intersecting.
          arm.xRot += -.11f + row * .035f - hit * .18f;
          arm.zRot += (i == -1 || i % 2 == 0 ? -1 : 1) * (.025f + prepare * .07f);
          ModelPart elbow = child(arm, "elbow");
          if (elbow != null) elbow.xRot += -.22f - prepare * .12f + hit * .08f;
        }
      }
    }
  }

  private static void wings(ModelPart rig, float age, boolean flying, boolean moth,
      float prepare, float hit) {
    float frequency = moth ? .63f : flying ? .33f : .065f;
    float phase = age * frequency;
    for (int side : new int[]{-1, 1}) {
      ModelPart wing = child(rig, "wing_" + side);
      if (wing == null) continue;
      float amplitude = moth ? .48f : flying ? .42f : .025f;
      wing.zRot += side * (Mth.sin(phase) * amplitude + prepare * .09f - hit * .12f);
      wing.xRot += Mth.cos(phase) * (flying ? .08f : .015f);
      // The wrist follows the shoulder after it reverses, softening the flap.
      ModelPart elbow = child(wing, "elbow");
      if (elbow != null) {
        elbow.zRot += side * Mth.sin(phase - .55f) * amplitude * .35f;
        elbow.yRot += side * ((flying ? .035f : .018f) + prepare * .06f);
        ModelPart tip = child(elbow, "tip");
        if (tip != null) tip.zRot += side * Mth.sin(phase - .95f) * amplitude * .25f;
      }
      ModelPart forewing = child(wing, "forewing");
      ModelPart hindwing = child(wing, "hindwing");
      if (forewing != null) forewing.zRot += side * Mth.sin(phase - .15f) * .12f;
      if (hindwing != null) hindwing.zRot += side * Mth.sin(phase - .65f) * .19f;
      for (int i = 0; i < 7; i++) {
        ModelPart feather = child(wing, "feather_" + i);
        if (feather != null) feather.xRot += Mth.sin(phase - .4f - i * .09f) * (flying ? .07f : .012f);
      }
    }
  }

  private static void plant(ModelPart rig, ModelPart head, float age, float prepare, float hit) {
    rig.zRot += Mth.sin(age * .035f) * .016f;
    rig.xRot += Mth.sin(age * .029f) * .012f - prepare * .028f + hit * .05f;
    if (head == null) return;
    for (int i = 0; i < 8; i++) {
      ModelPart petal = child(head, "petal_" + i);
      if (petal != null) petal.xRot += Mth.sin(age * .04f + i * .6f) * .025f + prepare * .52f - hit * .22f;
    }
    ModelPart jaw = child(head, "jaw");
    if (jaw != null) jaw.xRot += prepare * .2f - hit * .1f;
  }

  private static void tree(ModelPart rig, ModelPart head, float age, float prepare, float hit) {
    // The trunk stays rooted. Branches and hanging fruit carry the motion.
    rig.zRot += Mth.sin(age * .017f) * .004f;
    for (int i = 0; i < 6; i++) {
      ModelPart branch = child(rig, "arm_" + i);
      if (branch == null) continue;
      float phase = age * .026f + i * .72f;
      branch.zRot += Mth.sin(phase) * .021f;
      branch.xRot += Mth.cos(phase * .87f) * .016f - prepare * .07f + hit * .1f;
      ModelPart twig = child(branch, "twig");
      if (twig == null) twig = child(child(branch, "elbow"), "twig");
      if (twig == null) continue;
      twig.zRot += Mth.sin(phase - .6f) * .032f;
      ModelPart fruit = child(twig, "fruit");
      if (fruit != null) {
        fruit.zRot += Mth.sin(phase - 1.1f) * .045f;
        fruit.xRot += Mth.cos(phase - .8f) * .025f;
      }
    }
    if (head != null) head.xRot += prepare * .025f - hit * .045f;
  }

  private static void mimic(ModelPart rig, ModelPart head, float move, float swing,
      float age, float prepare, float hit) {
    for (int i = 0; i < 4; i++) {
      ModelPart leg = child(rig, "leg_" + i);
      if (leg != null) leg.xRot += Mth.cos(swing * 1.05f + (i == 0 || i == 3 ? 0 : Mth.PI)) * move * .43f;
    }
    rig.zRot += Mth.sin(swing * 1.05f) * move * .028f;
    // Rotate the lid about its rear hinge, rather than treating it as a neck.
    if (head != null) head.xRot += -.025f - prepare * .72f - hit * .12f - .012f * Mth.sin(age * .047f);
    ModelPart tongue = child(rig, "tail");
    if (tongue != null) {
      tongue.xRot += prepare * .15f - hit * .16f;
      chainWave(tongue, age * .105f, .035f + hit * .1f, .8f, 0);
    }
  }

  private static void stone(ModelPart rig, ModelPart body, float move, float swing,
      float age, float prepare, float hit) {
    if (body != null) body.xRot += swing * .32f;
    rig.zRot += Mth.sin(swing * .5f) * move * .055f;
    rig.y += Mth.sin(swing * 1.1f) * move * .15f + prepare * .18f - hit * .1f;
  }

  private static void reaver(ModelPart rig, ModelPart head, String id, float move,
      float swing, float age, float prepare, float hit) {
    radialSteps(rig, move, swing * .8f, .13f, .12f, .18f);
    rig.xRot += prepare * .05f - hit * .08f;
    if (id.equals("flesh_reaver")) {
      // The two large rear jumping legs coil at the knee during the warning,
      // then extend as the locust launches. Front/middle legs remain anchors.
      for (int i = 0; i < 2; i++) {
        ModelPart rear = child(rig, "leg_" + i);
        if (rear == null) continue;
        rear.xRot += -prepare * .36f + hit * .24f;
        ModelPart knee = child(rear, "knee");
        if (knee != null) knee.xRot += prepare * .6f - hit * .5f;
      }
      rig.y += prepare * .25f - hit * .28f;
      wings(rig, age, false, false, prepare, hit);
      for (int side : new int[]{-1, 1}) {
        ModelPart wing = child(rig, "wing_" + side);
        if (wing != null) {
          wing.yRot -= side * hit * .45f;
          wing.zRot += side * hit * .14f;
        }
      }
    }
    for (int side : new int[]{-1, 1}) {
      ModelPart arm = child(rig, "arm_" + side);
      if (arm == null) continue;
      arm.xRot += -.18f - prepare * .63f + hit * .4f;
      arm.yRot += side * (prepare * .14f - hit * .26f);
      ModelPart elbow = child(arm, "elbow");
      if (elbow != null) elbow.xRot += -.17f - prepare * .45f + hit * .58f;
    }
    if (head != null) head.zRot += Mth.sin(age * .035f) * (id.equals("gilded_mantis") ? .055f : .025f);
  }

  private static void ossuary(ModelPart rig, ModelPart body, float age, float prepare, float hit) {
    // A mound of human remains has no stride, shoulders, or royal posture.
    // Small asynchronous bone shifts suggest life below the stationary mass.
    rig.zRot += Mth.sin(age * .018f) * .003f;
    if (body == null) return;
    for (int i = 0; i < 24; i++) {
      ModelPart skull = child(body, "skull_" + i);
      if (skull == null) continue;
      float phase = age * .026f + i * .73f;
      skull.xRot += Mth.sin(phase) * .017f + prepare * .025f - hit * .035f;
      skull.zRot += Mth.cos(phase * .81f) * .013f;
      skull.y -= (prepare * .025f + hit * .06f) * (.65f + .35f * Mth.sin(i));
    }
  }

  private static void apparition(ModelPart rig, ModelPart body, ModelPart head,
      String id, float age, float prepare, float hit) {
    rig.y += Mth.sin(age * .047f) * .5f;
    rig.zRot += Mth.sin(age * .026f) * .012f;
    if (body != null) {
      for (int i = 0; i < 8; i++) {
        ModelPart veil = child(body, "veil_" + i);
        if (veil == null) continue;
        veil.xRot += Mth.sin(age * .045f + i * .56f) * .055f + prepare * .04f;
        veil.zRot += Mth.cos(age * .038f + i * .46f) * .025f;
        chainWave(child(veil, "segment"), age * .045f + i * .56f - .6f, .035f, .65f, 0);
      }
    }
    for (int side : new int[]{-1, 1}) {
      ModelPart arm = child(rig, "arm_" + side);
      if (arm == null) continue;
      boolean vessel = id.equals("chalice_maiden") && side == 1;
      arm.xRot += vessel ? -.35f - prepare * .18f : -.08f - prepare * .34f - hit * .3f;
      arm.zRot += side * (Mth.sin(age * .031f + side) * .035f + prepare * .045f);
      ModelPart elbow = child(arm, "elbow");
      if (elbow != null) elbow.xRot += vessel ? -.38f : -.12f - prepare * .15f;
    }
    if (head != null) head.zRot += Mth.sin(age * .023f) * .018f;
  }

  private static void humanoid(ModelPart rig, ModelPart body, ModelPart head, String id,
      float move, float swing, float age, float prepare, float hit, float recover) {
    boolean knight = id.equals("forsaken_knight") || id.equals("stone_saint");
    boolean giant = id.contains("colossus") || id.equals("lord_of_dead");
    boolean doll = id.equals("sailor_doll");
    float phase = swing * (giant ? .49f : doll ? .82f : .67f);
    float step = giant ? .32f : doll ? .34f : .48f;
    for (int i = 0; i < 2; i++) {
      ModelPart leg = child(rig, "leg_" + i);
      if (leg == null) continue;
      float cycle = phase + i * Mth.PI;
      leg.xRot += Mth.cos(cycle) * move * step + (i == 0 ? -prepare * .08f : prepare * .05f);
      ModelPart knee = child(leg, "knee");
      if (knee != null) {
        float lift = Math.max(0, Mth.sin(cycle)) * move;
        knee.xRot += -lift * (doll ? .12f : .3f);
        ModelPart foot = child(knee, "foot");
        if (foot != null) foot.xRot += lift * .11f;
      }
    }
    rig.y += (1 - Mth.cos(phase * 2)) * move * (giant ? -.06f : -.1f);
    rig.xRot += prepare * .026f - hit * .045f;
    for (int side : new int[]{-1, 1}) {
      ModelPart arm = child(rig, "arm_" + side);
      if (arm == null) continue;
      boolean shield = knight && side == 1;
      if (shield) {
        arm.xRot += -.18f - prepare * .34f - hit * .14f;
        arm.yRot += -.06f - prepare * .16f;
      } else {
        arm.xRot += Mth.cos(phase + (side < 0 ? Mth.PI : 0)) * move * (giant ? .11f : .2f);
        arm.xRot += -prepare * .78f - hit * 1.05f + recover * .15f;
        arm.yRot += side * (prepare * .08f - hit * .14f);
      }
      arm.zRot += Mth.sin(age * .038f + side) * .014f;
      ModelPart elbow = child(arm, "elbow");
      if (elbow != null) elbow.xRot += shield ? -.3f - prepare * .22f : -.06f - prepare * .26f + hit * .12f;
    }
    if (doll && head != null) {
      // A slowly leaning head and almost rigid knees distinguish the doll.
      head.zRot += .065f + Mth.sin(age * .019f) * .035f;
      head.xRot += Mth.sin(age * .037f) * .016f;
    }
    if (body != null) {
      for (int side : new int[]{-1, 1}) {
        ModelPart mantle = child(body, "mantle_" + side);
        if (mantle != null) mantle.xRot += Mth.sin(age * .045f + side * .7f) * .035f + move * .04f;
      }
    }
  }

  private static void chainWave(ModelPart part, float phase, float strength, float lag, int depth) {
    if (part == null || depth > 16) return;
    part.yRot += Mth.sin(phase) * strength;
    part.xRot += Mth.cos(phase * .7f) * strength * .15f;
    chainWave(child(part, "segment"), phase - lag, strength * .96f, lag, depth + 1);
  }

  private static void peristalsis(ModelPart part, float phase, float strength, int depth) {
    if (part == null || depth > 16) return;
    part.xRot += Mth.sin(phase) * strength;
    part.yRot += Mth.sin(phase * .6f) * strength * .25f;
    peristalsis(child(part, "segment"), phase - .7f, strength * .96f, depth + 1);
  }

  private static boolean isArthropod(String id) {
    return id.startsWith("carapace_") || id.contains("spider") || id.equals("glass_huntsman") || id.equals("gilded_mantis");
  }

  private static float smooth(float start, float end, float value) {
    float t = Mth.clamp((value - start) / (end - start), 0, 1);
    return t * t * (3 - 2 * t);
  }

  private static ModelPart child(ModelPart part, String name) {
    return part != null && part.hasChild(name) ? part.getChild(name) : null;
  }

  private static void hide(ModelPart part) {
    if (part != null) part.visible = false;
  }
}
