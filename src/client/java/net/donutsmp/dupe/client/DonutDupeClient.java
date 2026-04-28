package net.donutsmp.dupe.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.donutsmp.dupe.DonutDupeMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class DonutDupeClient implements ClientModInitializer {
	private static final String DUPE_PREFIX = ".dupe";

	@Override
	public void onInitializeClient() {
        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
			if (!message.startsWith(DUPE_PREFIX)) {
				return true;
			}

			handleDupeMessage(message);
			return false;
		});

		DonutDupeMod.LOGGER.info("Donut Dupe client initialized.");
	}

	private static void handleDupeMessage(String message) {
		LocalPlayer player = Minecraft.getInstance().player;

		if (player == null) {
			return;
		}

		String[] parts = message.trim().split("\\s+");
		if (parts.length != 2) {
			sendError("Usage: .dupe <amount>");
			return;
		}

		int amount;
		try {
			amount = Integer.parseInt(parts[1]);
		} catch (NumberFormatException exception) {
			sendError("Amount must be a whole number.");
			return;
		}

		if (amount < 1) {
			sendError("Amount must be at least 1.");
			return;
		}

		Inventory inventory = player.getInventory();
		ItemStack selectedStack = inventory.getSelectedItem();

		if (selectedStack.isEmpty()) {
			sendError("Hold an item before using .dupe.");
			return;
		}

		int duplicatedAmount = dropCopies(player, selectedStack, amount);

		if (duplicatedAmount <= 0) {
			sendError("Couldn't drop any copies on the ground.");
			return;
		}

		sendFeedback(
			"Cloned " + duplicatedAmount + " " + itemLabel(duplicatedAmount) + " of "
				+ selectedStack.getHoverName().getString() + " and dropped them on the ground."
		);
	}

	private static int dropCopies(LocalPlayer player, ItemStack template, int amount) {
		ClientLevel level = (ClientLevel) player.level();
		int remaining = amount;
		int duplicated = 0;

		while (remaining > 0) {
			ItemStack copyStack = template.copyWithCount(Math.min(remaining, template.getMaxStackSize()));
			int stackCount = copyStack.getCount();
			ItemEntity droppedItem = new ItemEntity(level, player.getX(), player.getEyeY() - 0.3D, player.getZ(), copyStack);
			Vec3 throwVelocity = player.getLookAngle().scale(0.35D).add(0.0D, 0.15D, 0.0D);
			droppedItem.setDeltaMovement(throwVelocity);
			droppedItem.setPickUpDelay(10);
			droppedItem.makeFakeItem();
			level.addEntity(droppedItem);

			duplicated += stackCount;
			remaining -= stackCount;
		}

		return duplicated;
	}

	private static String itemLabel(int amount) {
		return amount == 1 ? "item" : "items";
	}

	private static void sendFeedback(String message) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player != null) {
			player.sendSystemMessage(Component.literal(message));
		}
	}

	private static void sendError(String message) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player != null) {
			player.sendSystemMessage(Component.literal(message));
		}
	}

}
