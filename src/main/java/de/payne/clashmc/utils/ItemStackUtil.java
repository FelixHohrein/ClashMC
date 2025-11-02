package de.payne.clashmc.utils;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;

import net.kyori.adventure.text.Component;


public class ItemStackUtil {

	public ItemStackUtil(){
		
	}

	private static ItemStack getSkull(String textures) {
	    final ItemStack head = new ItemStack(Material.PLAYER_HEAD);
	
	    head.editMeta(SkullMeta.class, skullMeta -> {
	        final UUID uuid = UUID.randomUUID();
	        final PlayerProfile playerProfile = Bukkit.createProfile(uuid, uuid.toString().substring(0, 16));
	        playerProfile.setProperty(new ProfileProperty("textures", textures));
	
	        skullMeta.setPlayerProfile(playerProfile);
	    });
	    return head;
	}
	
    public static ItemStack createItem(Material material, String displayName, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(displayName));

        if (lore != null) {
            meta.lore(lore.stream().map(Component::text).collect(Collectors.toList()));
        }

        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createItemSkull(String base64, String displayName, List<String> lore) {
        ItemStack item = getSkull(base64);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(displayName));

        if (lore != null) {
            meta.lore(lore.stream().map(Component::text).collect(Collectors.toList()));
        }

        item.setItemMeta(meta);
        return item;
    }
    
    public static ItemStack createPickaxeWithUpgradeLevel(int level) {
        ItemStack pickaxe = new ItemStack(Material.DIAMOND_PICKAXE);
        ItemMeta meta = pickaxe.getItemMeta();

        int efficiency = level / 2;
        int fortune = level - efficiency;

        meta.addEnchant(Enchantment.EFFICIENCY, efficiency, true);
        meta.addEnchant(Enchantment.FORTUNE, fortune, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        
        meta.setDisplayName("§bBergbau-Spitzhacke §7(Level " + level + ")");
        meta.setLore(List.of("§7Effizienz: §a" + efficiency, "§7Glück: §a" + fortune));
        pickaxe.setItemMeta(meta);

        return pickaxe;
    }
}