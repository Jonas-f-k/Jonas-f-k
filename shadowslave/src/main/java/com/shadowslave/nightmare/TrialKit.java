package com.shadowslave.nightmare;

import com.shadowslave.ShadowSlaveMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;

/**
 * Copies earned equipment into the trial; the escrow remains unchanged and is restored on either
 * outcome.
 */
public final class TrialKit {
  public static void supply(ServerPlayer p) {
    var backup = new Inventory(p);
    backup.load(ShadowSlaveMod.soul(p).inventoryBackup);
    ItemStack weapon = ItemStack.EMPTY, ranged = ItemStack.EMPTY;
    double score = -1;
    for (int i = 0; i < backup.getContainerSize(); i++) {
      var item = backup.getItem(i);
      if (item.getItem() instanceof SwordItem || item.getItem() instanceof AxeItem) {
        double value = ((TieredItem) item.getItem()).getTier().getAttackDamageBonus();
        if (value > score) {
          score = value;
          weapon = item.copy();
        }
      }
      if (item.getItem() instanceof BowItem || item.getItem() instanceof CrossbowItem)
        ranged = item.copy();
    }
    if (weapon.isEmpty()) weapon = new ItemStack(Items.STONE_SWORD);
    p.getInventory().add(weapon);
    p.getInventory().add(ranged.isEmpty() ? new ItemStack(Items.BOW) : ranged);
    for (var slot :
        new EquipmentSlot[] {
          EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        }) {
      ItemStack best = ItemStack.EMPTY;
      int defense = -1;
      for (int i = 0; i < backup.getContainerSize(); i++) {
        var item = backup.getItem(i);
        if (item.getItem() instanceof ArmorItem armor
            && armor.getEquipmentSlot() == slot
            && armor.getDefense() > defense) {
          best = item.copy();
          defense = armor.getDefense();
        }
      }
      if (!best.isEmpty()) p.setItemSlot(slot, best);
    }
    p.getInventory().add(new ItemStack(Items.ARROW, 64));
    p.getInventory().add(new ItemStack(Items.COOKED_BEEF, 48));
    p.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
  }
}
