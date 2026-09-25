package com.bookofthedeadnotifier;

import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.game.ItemManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Set;

/**
 * Reads what the player is carrying: worn equipment, inventory, and the runes stored in a rune
 * pouch. Every read must happen on the client thread.
 */
@Singleton
public class PlayerLoadout
{
	private static final int[] RUNE_POUCH_TYPE_VARBITS = {
		VarbitID.RUNE_POUCH_TYPE_1,
		VarbitID.RUNE_POUCH_TYPE_2,
		VarbitID.RUNE_POUCH_TYPE_3,
		VarbitID.RUNE_POUCH_TYPE_4,
		VarbitID.RUNE_POUCH_TYPE_5,
		VarbitID.RUNE_POUCH_TYPE_6
	};
	private static final int[] RUNE_POUCH_QUANTITY_VARBITS = {
		VarbitID.RUNE_POUCH_QUANTITY_1,
		VarbitID.RUNE_POUCH_QUANTITY_2,
		VarbitID.RUNE_POUCH_QUANTITY_3,
		VarbitID.RUNE_POUCH_QUANTITY_4,
		VarbitID.RUNE_POUCH_QUANTITY_5,
		VarbitID.RUNE_POUCH_QUANTITY_6
	};
	private static final Set<Integer> RUNE_POUCH_ITEM_IDS = Set.of(
		ItemID.BH_RUNE_POUCH,
		ItemID.BH_RUNE_POUCH_TROUVER,
		ItemID.DIVINE_RUNE_POUCH,
		ItemID.DIVINE_RUNE_POUCH_TROUVER);
	private static final EquipmentInventorySlot[] INFINITE_SOURCE_SLOTS = {
		EquipmentInventorySlot.WEAPON,
		EquipmentInventorySlot.SHIELD
	};
	private static final int UNLIMITED = Integer.MAX_VALUE;

	@Inject
	private Client client;

	@Inject
	private ItemManager itemManager;

	public static boolean isRunePouchVarbit(int varbitId)
	{
		return contains(RUNE_POUCH_TYPE_VARBITS, varbitId) || contains(RUNE_POUCH_QUANTITY_VARBITS, varbitId);
	}

	/**
	 * How many thralls of the given tier the player can currently cast.
	 */
	public int castsAvailable(ThrallTier tier)
	{
		// Resolved once per check rather than per rune: finding the pouch means scanning both
		// containers for each of its four item ids.
		boolean carriesRunePouch = carriesRunePouch();
		return tier.castsAvailable(rune -> availableRunes(rune, carriesRunePouch));
	}

	public boolean carries(int itemId)
	{
		return contains(InventoryID.EQUIPMENT, itemId) || contains(InventoryID.INVENTORY, itemId);
	}

	private int availableRunes(ThrallRune rune, boolean carriesRunePouch)
	{
		if (hasInfiniteSourceEquipped(rune))
		{
			return UNLIMITED;
		}

		int carried = countInInventory(rune);
		return carriesRunePouch ? carried + countInRunePouch(rune) : carried;
	}

	private boolean hasInfiniteSourceEquipped(ThrallRune rune)
	{
		if (!rune.hasInfiniteSources())
		{
			return false;
		}

		ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
		if (equipment == null)
		{
			return false;
		}

		for (EquipmentInventorySlot slot : INFINITE_SOURCE_SLOTS)
		{
			Item item = equipment.getItem(slot.getSlotIdx());
			if (item != null && rune.isInfiniteSource(itemManager.canonicalize(item.getId())))
			{
				return true;
			}
		}

		return false;
	}

	private int countInInventory(ThrallRune rune)
	{
		ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
		if (inventory == null)
		{
			return 0;
		}

		int count = 0;
		for (Item item : inventory.getItems())
		{
			if (rune.isSatisfiedByRune(item.getId()))
			{
				count += item.getQuantity();
			}
		}
		return count;
	}

	private int countInRunePouch(ThrallRune rune)
	{
		int count = 0;
		for (int slot = 0; slot < RUNE_POUCH_TYPE_VARBITS.length; slot++)
		{
			count += countInRunePouchSlot(slot, rune);
		}
		return count;
	}

	private int countInRunePouchSlot(int slot, ThrallRune rune)
	{
		int runeEnumId = client.getVarbitValue(RUNE_POUCH_TYPE_VARBITS[slot]);
		int quantity = client.getVarbitValue(RUNE_POUCH_QUANTITY_VARBITS[slot]);

		if (runeEnumId == 0 || quantity <= 0)
		{
			return 0;
		}

		return rune.isSatisfiedByRune(runePouchItemId(runeEnumId)) ? quantity : 0;
	}

	private int runePouchItemId(int runeEnumId)
	{
		EnumComposition runePouchRunes = client.getEnum(EnumID.RUNEPOUCH_RUNE);
		return runePouchRunes == null ? 0 : runePouchRunes.getIntValue(runeEnumId);
	}

	public boolean carriesRunePouch()
	{
		return RUNE_POUCH_ITEM_IDS.stream().anyMatch(this::carries);
	}

	private boolean contains(InventoryID containerId, int itemId)
	{
		ItemContainer container = client.getItemContainer(containerId);
		if (container == null)
		{
			return false;
		}

		for (Item item : container.getItems())
		{
			if (itemManager.canonicalize(item.getId()) == itemId)
			{
				return true;
			}
		}
		return false;
	}

	private static boolean contains(int[] values, int value)
	{
		for (int candidate : values)
		{
			if (candidate == value)
			{
				return true;
			}
		}
		return false;
	}
}
