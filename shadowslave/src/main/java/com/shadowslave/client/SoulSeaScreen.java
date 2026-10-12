package com.shadowslave.client;

import com.shadowslave.content.Content;
import com.shadowslave.core.Rules;
import com.shadowslave.network.SpellNetwork;
import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;

public final class SoulSeaScreen extends Screen {
  private int page, tab, left, top, w, h;
  private String selected = "", signature = "", query = "";
  private int filter;
  private static final String[] FILTERS = {
    "all", "armor", "weapon", "head", "chest", "legs", "feet", "charm", "utility"
  };

  private java.util.List<String> visibleMemories() {
    return list("owned").stream()
        .filter(
            id -> {
              var m = Content.memories.get(id);
              return m != null
                  && (filter == 0 || m.type().equals(FILTERS[filter]))
                  && (m.name()
                          .toLowerCase(java.util.Locale.ROOT)
                          .contains(query.toLowerCase(java.util.Locale.ROOT))
                      || m.biome().contains(query.toLowerCase(java.util.Locale.ROOT)));
            })
        .toList();
  }

  public SoulSeaScreen() {
    super(Component.literal("Soul Sea"));
  }

  private CompoundTag data() {
    return SpellNetwork.clientState;
  }

  private List<String> list(String key) {
    return data().getList(key, Tag.TAG_STRING).stream().map(Tag::getAsString).toList();
  }

  private void action(String op, String arg) {
    SpellNetwork.send(op, arg);
  }

  @Override
  protected void init() {
    layout();
  }

  private void layout() {
    clearWidgets();
    w = Math.min(580, width - 16);
    h = Math.min(330, height - 16);
    left = (width - w) / 2;
    top = (height - h) / 2;
    String[] tabs = {"Status", "Memories", "Soul Core", "Traits", "Bestiary", "Guide"};
    int tw = (w - 44) / 6;
    for (int i = 0; i < 6; i++) {
      int selectedTab = i;
      button(
          tabs[i],
          left + 12 + i * (tw + 4),
          top + 35,
          tw,
          () -> {
            tab = selectedTab;
            page = 0;
            layout();
          });
    }
    button("×", left + w - 33, top + 9, 22, this::onClose);
    if (tab == 0) {
      String stage = data().getString("stage");
      int y = top + h - 30;
      int actionWidth = Math.min(155,(w-30)/2);
      if (stage.equals("retry"))
        button("Retry Nightmare", left + 12, y, actionWidth, () -> action("retry", ""));
      else if (stage.equals("nightmare") || stage.equals("preparing"))
        button(
            "Abandon trial",
            left + 12,
            y,
            actionWidth,
            () ->
                minecraft.setScreen(
                    new net.minecraft.client.gui.screens.ConfirmScreen(
                        ok -> {
                          minecraft.setScreen(this);
                          if (ok) action("abandon", "");
                        },
                        Component.literal("Abandon this attempt?"),
                        Component.literal(
                            "Your items return. Appraisal, seals and boss victory reset."))));
      else if (data().getInt("soulRank") > 1 && !data().getBoolean("inDream"))
        button(
            "Enter Dream Realm",
            left + 12,
            y,
            actionWidth,
            () -> {
              action("dream", "");
              onClose();
            });
      if (data().getInt("soulRank") > 0)
        button(
            data().getBoolean("active") ? "Dismiss Memories" : "Summon · 10 Essence",
            left + w - actionWidth - 12,
            y,
            actionWidth,
            () -> action("toggle", ""));
    }
    if (tab == 1) {
      var search =
          new net.minecraft.client.gui.components.EditBox(
              font, left + 13, top + 66, w - 135, 18, Component.literal("Search Memories"));
      search.setMaxLength(48);
      search.setValue(query);
      search.setHint(Component.literal("Search name or biome…"));
      search.setResponder(
          value -> {
            query = value;
            page = 0;
            int caret = search.getCursorPosition();
            layout();
            for (var child : children()) if (child instanceof net.minecraft.client.gui.components.EditBox next) next.setCursorPosition(caret);
          });
      addRenderableWidget(search);
      setFocused(search);
      button(
          FILTERS[filter],
          left + w - 115,
          top + 65,
          102,
          () -> {
            filter = (filter + 1) % FILTERS.length;
            page = 0;
            layout();
          });
      var owned = visibleMemories();
      int rows = Math.max(1, (h - 139) / 23);
      int pages = Math.max(1, (owned.size() + rows - 1) / rows);
      page = Math.clamp(page, 0, pages - 1);
      for (int i = 0; i < rows; i++) {
        int index = page * rows + i;
        if (index >= owned.size()) break;
        String id = owned.get(index);
        var m = Content.memories.get(id);
        if (m == null) continue;
        var roll = data().getCompound("memoryRolls").getCompound(id);
        int rank = roll.contains("rank") ? Math.clamp(roll.getInt("rank"), 0, 6) : m.rank();
        String label = (list("equipped").contains(id) ? "◆ " : "◇ ") + m.name() + " · " + Rules.RANKS[rank] + " / " + m.type();
        button(
            label,
            left + 34,
            top + 88 + i * 23,
            w - 46,
            () -> {
              selected = id;
              action("equip", id);
            });
      }
      int y = top + h - 43;
      button(
          "‹",
          left + 12,
          y,
          23,
          () -> {
            page--;
            layout();
          });
      button(
          "›",
          left + 40,
          y,
          23,
          () -> {
            page++;
            layout();
          });
      var def = Content.memories.get(selected);
      if (def != null && def.type().equals("weapon") && list("equipped").contains(selected))
        button(
            "Summon weapon",
            left + 70,
            y,
            142,
            () -> {
              action("manifest", selected);
              onClose();
            });
      if (def != null && !list("equipped").contains(selected) && list("owned").contains(selected))
        button("Unbind as trade token", left + 70, y, 167, () -> action("crystallize", selected));
    }
    if (tab == 3 || tab == 4) {
      int rows = Math.max(1, (h - 116) / 32),
          pages = Math.max(1, ((tab == 3 ? com.shadowslave.player.Traits.ALL.size() : com.shadowslave.entity.CreatureEcology.ALL.size()) + rows - 1) / rows);
      page = Math.clamp(page, 0, pages - 1);
      button(
          "‹",
          left + 12,
          top + h - 29,
          23,
          () -> {
            page--;
            layout();
          });
      if(tab==4)button("View animated models",left+w-172,top+h-29,160,
          ()->minecraft.setScreen(new CreatureGalleryScreen(this,0,page*rows)));
      button(
          "›",
          left + 40,
          top + h - 29,
          23,
          () -> {
            page++;
            layout();
          });
    }
  }

  private void button(String label, int x, int y, int width, Runnable action) {
    addRenderableWidget(SpellTheme.button(label, x, y, width, action,
        label.equals(new String[]{"Status", "Memories", "Soul Core", "Traits", "Bestiary", "Guide"}[tab])));
  }

  @Override
  public void tick() {
    String s =
        data().getString("stage")
            + data().getList("owned", Tag.TAG_STRING)
            + data().getList("equipped", Tag.TAG_STRING)
            + data().getBoolean("active") + data().getInt("soulRank") + data().getInt("family") + data().getBoolean("inDream");
    if (!s.equals(signature)) {
      signature = s;
      layout();
    }
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }

  @Override
  public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
    // Screen.render invokes this before widgets. Blur only the world behind the panel.
    super.renderBackground(g, mouseX, mouseY, delta);
    g.fillGradient(0, 0, width, height, 0xC808101B, 0xE2090E19);
    int accent = SpellTheme.accent(data().getInt("family"));
    SpellTheme.panel(g, left, top, w, h, accent);
    g.fill(left + 1, top + 61, left + w - 1, top + 62, SpellTheme.EDGE);
    if (data().getInt("soulRank") > 0) {
      int count = Math.clamp(data().getInt("cores"), 1, 7);
      if (w>=420) for (int i=0;i<count;i++) SpellTheme.core(g,left+w-55-i*13,top+15,9,
          i<count-1?1:data().getDouble("coreSaturation"),accent);
      else g.drawString(font,count+" cores",left+w-88,top+15,accent,false);
    }
    g.drawString(font, font.plainSubstrByWidth("THE NIGHTMARE SPELL",w-110), left + 13, top + 13, 0xDDC49B, false);
    g.drawString(font, "SOUL SEA", left + 13, top + 24, 0x9B9A93, false);
    if (tab == 0) renderStatus(g);
    else if (tab == 1) renderMemories(g, mouseX, mouseY);
    else if (tab == 2) renderGrowth(g);
    else if (tab == 3) renderTraits(g);
    else if (tab == 4) renderBestiary(g);
    else renderGuide(g);
  }

  @Override
  public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
    super.render(g, mouseX, mouseY, delta);
    if (tab == 1) {
      int rows = Math.max(1, (h - 139) / 23);
      var owned = visibleMemories();
      for (int i = 0; i < rows && page * rows + i < owned.size(); i++)
        g.renderItem(
            com.shadowslave.memory.MemoryTokenItem.create(owned.get(page * rows + i)),
            left + 13,
            top + 90 + i * 23);
      g.drawString(
          font,
          (page + 1) + " / " + Math.max(1, (owned.size() + rows - 1) / rows),
          left + w - 72,
          top + h - 38,
          0xC8C5BA,
          false);
      renderMemoryTooltip(g, mouseX, mouseY);
    }
  }

  private void renderStatus(GuiGraphics g) {
    var d = data();
    String stage = d.getString("stage");
    int x = left + 14, y = top + 73;
    g.drawString(font, stage.toUpperCase(Locale.ROOT), x, y, 0xDDC49B, false);
    y += 20;
    if (stage.equals("aspirant")) {
      long total = Math.max(1, d.getLong("countdownTicks"));
      text(
          g,
          "The First Nightmare approaches. Online time and nights skipped through sleep both"
              + " count.",
          x,
          y,
          w - 28);
      y += 35;
      SpellClient.bar(g, x, y, w - 28, 7, d.getLong("elapsed") / (double) total, 0xFFD0AD72);
      y += 17;
      text(
          g,
          "Day "
              + Math.min(total / 24000, 1 + d.getLong("elapsed") / 24000)
              + " / "
              + total / 24000
              + ". Your ordinary inventory will be kept safe during the trial.",
          x,
          y,
          w - 28);
      return;
    }
    if (stage.equals("preparing")) {
      text(
          g,
          "The Spell is building your private trial. You will enter automatically when it is"
              + " ready.",
          x,
          y,
          w - 28);
      return;
    }
    if (stage.equals("nightmare") || stage.equals("retry")) {
      g.drawString(font, d.getString("trialName"), x, y, 0xE2CFAE, false);
      y += 17;
      int rank = Rules.aspectRank(d.getInt("points"), d.getBoolean("divine"));
      g.drawString(
          font,
          d.getInt("targetRank") > 1
              ? "Ascension target: " + (d.getInt("targetRank") == 3 ? "Master" : "Saint")
              : "Projected Aspect: " + Rules.RANKS[rank] + " (" + d.getInt("points") + " points)",
          x,
          y,
          0xE9E5ED,
          false);
      y += 16;
      g.drawString(
          font,
          "Restored seals: "
              + d.getInt("altars")
              + "/"
              + d.getInt("sealCount")
              + "   Eligible kills: "
              + d.getInt("kills"),
          x,
          y,
          0xAFB4C6,
          false);
      y += 23;
      text(
          g,
          d.getInt("targetRank") > 1
              ? "Defeat the ruler and restore every linked seal to complete this Nightmare."
              : d.getBoolean("divine")
                  ? "Divine boss defeated. Complete the seals and use the exit to claim your"
                        + " Aspect."
                  : "Seek the optional boss in one of five arenas. Kills can reach Sacred; only"
                        + " defeating this boss unlocks Divine.",
          x,
          y,
          w - 28);
      return;
    }
    g.drawString(
        font,
        d.getString("aspectName") + " · " + Rules.RANKS[Math.clamp(d.getInt("aspectRank"), 0, 6)],
        x,
        y,
        0xDFC499,
        false);
    y += 17;
    g.drawString(
        font,
        "Soul rank: "
            + Rules.RANKS[Math.clamp(d.getInt("soulRank") - 1, 0, 6)]
            + "    Cores: "
            + d.getInt("cores"),
        x,
        y,
        0xDEDCD3,
        false);
    y += 18;
    SpellClient.bar(
        g,
        x,
        y,
        w - 28,
        6,
        d.getInt("essence") / (double) Math.max(1, d.getInt("maxEssence")),
        0xFFC7AA77);
    y += 12;
    g.drawString(
        font,
        "Essence "
            + d.getInt("essence")
            + " / "
            + d.getInt("maxEssence")
            + "    Soul fragments "
            + d.getInt("fragments"),
        x,
        y,
        0xB3ADC9,
        false);
    y += 21;
    int form = d.getInt("beastForm");
    String keys =
        "Abilities: [R]"
            + (d.getInt("soulRank") >= 2 ? " [G]" : "")
            + (d.getInt("soulRank") >= 3 ? " [H]" : "")
            + (d.getInt("soulRank") >= 4 ? " [J]" : "");
    if (form >= 0 && form < com.shadowslave.combat.BeastForms.ALL.size()) keys = com.shadowslave.combat.BeastForms.ALL.get(form).name() + " · R attack";
    g.drawString(font, keys, x, y, 0xDDC49B, false);
    y += 18;
    g.drawString(font, "FLAW", x, y, 0xD29B9B, false);
    y += 12;
    text(g, d.getString("flawDescription"), x, y, w - 28);
  }

  private void renderMemories(GuiGraphics g, int mouseX, int mouseY) {
    var equipped = list("equipped");
    g.drawString(
        font,
        font.plainSubstrByWidth("Attuned "
            + equipped.size()
            + " / 7 · "
            + list("owned").size()
            + " / "
            + Content.memories.size(),w-125),
        left + 110,
        top + 24,
        0xC8C5BA,
        false);
    g.drawString(
        font,
        font.plainSubstrByWidth("1 full armor · 1 charm · 2 weapons · 1 per body part",w-26),
        left + 13,
        top + h - 15,
        0x8F899C,
        false);
    if (list("owned").isEmpty())
      text(
          g,
          "Complete a First Nightmare to earn your first Memories. Hunt regional creatures for"
              + " ranked Memory drops. Bind tokens with right-click.",
          left + 14,
          top + 97,
          w - 28);
  }

  private void renderMemoryTooltip(GuiGraphics g, int mouseX, int mouseY) {
    int rows = Math.max(1, (h - 139) / 23);
    var owned = visibleMemories();
    for (int i = 0; i < rows; i++) {
      int index = page * rows + i;
      if (index >= owned.size()) break;
      var m = Content.memories.get(owned.get(index));
      if (m == null) continue;
      if (mouseX >= left + 12
          && mouseX <= left + w - 12
          && mouseY >= top + 88 + i * 23
          && mouseY < top + 108 + i * 23) {
        var lines = new ArrayList<Component>();
        lines.add(
            Component.literal(
                Rules.RANKS[Math.clamp(
                        data().getCompound("memoryRolls").contains(m.id())
                            ? data().getCompound("memoryRolls").getCompound(m.id()).getInt("rank")
                            : m.rank(), 0, 6)]
                    + " · Tier "
                    + Math.max(
                        1, data().getCompound("memoryRolls").getCompound(m.id()).getInt("tier"))
                    + " · "
                    + m.type()));
        lines.add(Component.literal(m.description()));
        lines.add(Component.literal("Found in: " + m.biome().replace('_', ' ')));
        lines.add(
            Component.literal("Armor uses real armor points; rank and tier improve its strength."));
        g.renderComponentTooltip(font, lines, mouseX, mouseY);
      }
    }
  }

  private void renderGuide(GuiGraphics g) {
    String[] lines = {
      "O Soul Sea · R Dormant · G Awakened · H Ascended · J Beast form",
      "In a beast form R uses its special attack. Dragon, griffin and wyvern: double Space to fly."
          + " J returns to human.",
      "Fragments strengthen every ability, Essence, health and recovery. Traits are earned"
          + " permanent bonuses. See Growth and Traits.",
      "First Nightmare: restore six linked seals; exit. Optional boss grants Divine. Sleep, then"
          + " return through a Gateway to Awaken.",
      "Hunt creatures for ranked Memories, rare Echoes and Nightmare Seeds. Seeds open the Second"
          + " and Third Nightmares."
    };
    int y = top + 73;
    for (String line : lines) {
      var wrapped = font.split(Component.literal(line), w - 28);
      for (var part : wrapped) {
        if (y > top + h - 16) return;
        g.drawString(font, part, left + 14, y, 0xBFC1BC, false);
        y += 11;
      }
      y += 7;
    }
  }

  private void renderGrowth(GuiGraphics g) {
    var d = data();
    int x = left + 14, y = top + 73;
    if (d.getInt("soulRank") == 0) {
      text(
          g,
          "Complete your First Nightmare to awaken your Soul. Fragment saturation will strengthen"
              + " all abilities.",
          x,
          y,
          w - 28);
      return;
    }
    int cores = Math.max(1, d.getInt("cores"));
    g.drawString(
        font,
        "Soul Cores " + cores + " / " + (d.getInt("aspectRank") == 6 ? 7 : 1),
        x,
        y,
        0xDDC49B,
        false);
    y += 22;
    int accent = SpellTheme.accent(d.getInt("family"));
    for (int i=0;i<cores;i++) SpellTheme.core(g,x+i*26,y,17,i<cores-1?1:d.getDouble("coreSaturation"),accent);
    y += 29;
    int filled =
        Math.clamp(d.getInt("fragments") - Rules.coreFloor(cores), 0, Rules.coreCapacity(cores));
    g.drawString(
        font, "Current core " + filled + " / " + Rules.coreCapacity(cores), x, y, 0xBDC7C8, false);
    y += 14;
    SpellClient.bar(g, x, y, w - 28, 6, d.getDouble("coreSaturation"), 0xFFB1C49A);
    y += 18;
    g.drawString(
        font,
        String.format(Locale.ROOT, "Ability strength ×%.2f", d.getDouble("abilityMultiplier")),
        x,
        y,
        0xE3D0AD,
        false);
    y += 15;
    g.drawString(
        font,
        String.format(
            Locale.ROOT,
            "Maximum health %.1f · Essence %d",
            d.getFloat("maxHealth"),
            d.getInt("maxEssence")),
        x,
        y,
        0xBDC7C8,
        false);
    y += 15;
    g.drawString(
        font,
        String.format(
            Locale.ROOT,
            "Cooldown R %.1fs · G %.1fs · H %.1fs",
            d.getInt("dormantCooldownTicks") / 20.0,
            d.getInt("awakenedCooldownTicks") / 20.0,
            d.getInt("ascendedCooldownTicks") / 20.0),
        x,
        y,
        0xB5B8BE,
        false);
    y += 19;
    text(
        g,
        "Each fragment improves your Soul. A new core preserves your growth; Divine Aspects can"
            + " form seven cores. Traits persist after death.",
        x,
        y,
        w - 28);
  }

  private void renderTraits(GuiGraphics g) {
    int rows = Math.max(1, (h - 116) / 32);
    var all = com.shadowslave.player.Traits.ALL;
    var earned = list("traits");
    g.drawString(
        font,
        "Permanent traits: " + earned.size() + " / " + all.size(),
        left + 14,
        top + 70,
        0xDDC49B,
        false);
    for (int i = 0; i < rows && page * rows + i < all.size(); i++) {
      var t = all.get(page * rows + i);
      int y = top + 88 + i * 32;
      boolean has = earned.contains(t.id());
      g.drawString(
          font, (has ? "◆ " : "◇ ") + t.name(), left + 14, y, has ? 0xD8D7AE : 0x7E8585, false);
      int yy = y + 11;
      for (var line : font.split(Component.literal(t.description()), w - 28)) {
        if (yy >= y + 31) break;
        g.drawString(font, line, left + 14, yy, has ? 0xAEBFC4 : 0x737C83, false);
        yy += 10;
      }
    }
    g.drawString(
        font,
        (page + 1) + " / " + ((all.size() + rows - 1) / rows),
        left + 80,
        top + h - 23,
        0xAEBFC4,
        false);
  }

  private void renderBestiary(GuiGraphics g) {
    var all = com.shadowslave.entity.CreatureEcology.ALL;
    int rows = Math.max(1, (h - 116) / 32);
    g.drawString(font, "NIGHTMARE CREATURES · " + all.size() + " species", left+14, top+70, SpellTheme.GOLD, false);
    for (int i=0;i<rows && page*rows+i<all.size();i++) {
      var s=all.get(page*rows+i); int y=top+88+i*32;
      g.fill(left+12,y-2,left+w-12,y+28, i%2==0?0x661F2B3B:0x331F2B3B);
      SpellTheme.core(g,left+18,y+4,11,1,SpellTheme.accent(s.family()));
      String name=s.name()+" · "+com.shadowslave.entity.CreatureEcology.RANKS[s.rank()]+" "+com.shadowslave.entity.CreatureEcology.CLASSES[s.cores()];
      g.drawString(font,font.plainSubstrByWidth(name,w-54),left+38,y,SpellTheme.TEXT,false);
      String info=s.regions().replace('_',' ').replace(","," / ")+" · "+s.behavior().replace('_',' ');
      g.drawString(font,font.plainSubstrByWidth(info,w-54),left+38,y+12,0x99AFBF,false);
    }
    g.drawString(font,(page+1)+" / "+((all.size()+rows-1)/rows),left+80,top+h-23,0xAEBFC4,false);
  }

  private void text(GuiGraphics g, String text, int x, int y, int maxWidth) {
    for (var line : font.split(Component.literal(text), maxWidth)) {
      if (y > top + h - 40) break;
      g.drawString(font, line, x, y, 0xC2C3BD, false);
      y += 11;
    }
  }
}
