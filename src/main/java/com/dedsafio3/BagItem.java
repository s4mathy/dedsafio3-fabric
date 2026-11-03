package com.dedsafio3;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/** Bolsa con inventario propio: Primitiva (3 filas) o Glebanoide (6 filas). Clic derecho para abrirla. */
public class BagItem extends TooltipItem {
	private final int rows;

	public BagItem(Settings settings, int rows) {
		super(settings);
		this.rows = rows;
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
		ItemStack stack = player.getStackInHand(hand);
		if (world.isClient) {
			return TypedActionResult.success(stack);
		}
		int size = rows * 9;
		SimpleInventory inventory = new SimpleInventory(size);
		DefaultedList<ItemStack> stored = DefaultedList.ofSize(size, ItemStack.EMPTY);
		stack.getOrDefault(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT).copyTo(stored);
		for (int i = 0; i < size; i++) {
			inventory.setStack(i, stored.get(i));
		}
		// Se guarda en la bolsa cada vez que cambia el contenido.
		inventory.addListener(sender -> {
			List<ItemStack> out = new ArrayList<>(size);
			for (int i = 0; i < size; i++) {
				out.add(inventory.getStack(i).copy());
			}
			stack.set(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(out));
		});
		player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
				(syncId, playerInventory, p) -> new BagScreenHandler(syncId, playerInventory, inventory, rows, stack), stack.getName()));
		return TypedActionResult.success(stack);
	}
}
