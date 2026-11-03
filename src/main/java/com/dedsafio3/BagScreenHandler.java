package com.dedsafio3;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;

/** Menú de una bolsa (cofre de 3 o 6 filas). No deja mover la propia bolsa mientras está abierta. */
public class BagScreenHandler extends GenericContainerScreenHandler {
	private final ItemStack bag;

	public BagScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, int rows, ItemStack bag) {
		super(rows == 3 ? ScreenHandlerType.GENERIC_9X3 : ScreenHandlerType.GENERIC_9X6, syncId, playerInventory, inventory, rows);
		this.bag = bag;
	}

	@Override
	public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
		if (slotIndex >= 0 && slotIndex < slots.size() && slots.get(slotIndex).getStack() == bag) {
			return;
		}
		if (actionType == SlotActionType.SWAP && button >= 0 && button < player.getInventory().size()
				&& player.getInventory().getStack(button) == bag) {
			return;
		}
		super.onSlotClick(slotIndex, button, actionType, player);
	}
}
