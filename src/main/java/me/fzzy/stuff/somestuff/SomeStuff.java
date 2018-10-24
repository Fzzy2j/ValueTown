package me.fzzy.stuff.somestuff;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.awt.event.ItemEvent;
import java.util.*;

public final class SomeStuff extends JavaPlugin implements Listener {

    HashMap<Material, Float> xpValues = new HashMap<Material, Float>() {{
        put(Material.DIAMOND_ORE, 10f);
        put(Material.EMERALD_ORE, 15f);
        put(Material.IRON_ORE, 3f);
        put(Material.LAPIS_ORE, 2f);
        put(Material.REDSTONE_ORE, 1f);
        put(Material.COAL_ORE, 1f);
        put(Material.SALMON, 6f);
        put(Material.COD, 6f);
        put(Material.NETHER_QUARTZ_ORE, 0.5f);

        put(Material.OAK_LOG, 0.6f);
        put(Material.ACACIA_LOG, 0.6f);
        put(Material.DARK_OAK_LOG, 0.6f);
        put(Material.BIRCH_LOG, 0.6f);
        put(Material.JUNGLE_LOG, 0.6f);
        put(Material.SPRUCE_LOG, 0.6f);

        put(Material.STRIPPED_OAK_WOOD, 0.6f);
        put(Material.STRIPPED_ACACIA_WOOD, 0.6f);
        put(Material.STRIPPED_DARK_OAK_WOOD, 0.6f);
        put(Material.STRIPPED_BIRCH_WOOD, 0.6f);
        put(Material.STRIPPED_JUNGLE_WOOD, 0.6f);
        put(Material.STRIPPED_SPRUCE_WOOD, 0.6f);

        put(Material.OAK_PLANKS, 0.6f / 4);
        put(Material.ACACIA_PLANKS, 0.6f / 4);
        put(Material.DARK_OAK_PLANKS, 0.6f / 4);
        put(Material.BIRCH_PLANKS, 0.6f / 4);
        put(Material.JUNGLE_PLANKS, 0.6f / 4);
        put(Material.SPRUCE_PLANKS, 0.6f / 4);

        put(Material.COBBLESTONE, 0.01f);
        put(Material.NETHERRACK, 0.01f);
        put(Material.DIRT, 0.02f);
        put(Material.SAND, 0.03f);
        put(Material.SOUL_SAND, 0.03f);
        put(Material.RED_SAND, 0.03f);
        put(Material.GRAVEL, 0.03f);

        put(Material.SUGAR_CANE, 0.01f);
        put(Material.WHEAT, 0.03f);
    }};

    @Override
    public void onEnable() {
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onCommandPreProcess(PlayerCommandPreprocessEvent event) {
        if (event.getMessage().equalsIgnoreCase("/value")) {
            List<Material> calculated = new ArrayList<>();
            ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
            if (item != null)
                event.getPlayer().sendMessage(item.getType() + "x" + item.getAmount() + " is worth " + (float) getTotalValue(item, calculated) + "xp");
            else
                event.getPlayer().sendMessage("Hold the item you with to know the value of.");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        event.setExpToDrop(0);
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        event.setExpToDrop(0);
    }

    @EventHandler
    public void onSmelt(FurnaceExtractEvent event) {
        event.setExpToDrop(0);
    }

    public void convertToXp(ItemStack item, Location loc) {
        List<Material> calculated = new ArrayList<>();
        double xp = getTotalValue(item, calculated);
        int random = new Random().nextInt(10000);
        ExperienceOrb orb = loc.getWorld().spawn(loc, ExperienceOrb.class);
        if (xp < 1 && xp * 10000 > random) {
            orb.setExperience((int) Math.ceil(xp));
        } else if (xp >= 1) {
            orb.setExperience((int) Math.ceil(xp));
        }
    }

    public double getTotalValue(ItemStack item, List<Material> calculated) {
        double xp = 0;
        if (item != null) {
            ItemStack check = new ItemStack(item.getType(), item.getAmount());
            if (!calculated.contains(check.getType())) {
                if (xpValues.containsKey(check.getType())) {
                    xp += check.getAmount() * xpValues.get(check.getType());
                    return xp;
                }
                List<Recipe> recpiesList = Bukkit.getRecipesFor(check);
                for (Recipe recipe : recpiesList) {
                    if (recipe instanceof ShapelessRecipe) {
                        calculated.add(check.getType());
                        for (ItemStack ingredient : ((ShapelessRecipe) recipe).getIngredientList()) {
                            if (ingredient != null)
                                ingredient.setAmount(check.getAmount());
                            xp += getTotalValue(ingredient, calculated) / (double) recipe.getResult().getAmount();
                        }
                    }
                    if (recipe instanceof ShapedRecipe) {
                        calculated.add(check.getType());
                        for (ItemStack ingredient : ((ShapedRecipe) recipe).getIngredientMap().values()) {
                            if (ingredient != null)
                                ingredient.setAmount(check.getAmount());
                            if (ingredient != null)
                                xp += getTotalValue(ingredient, calculated) / (double) recipe.getResult().getAmount();
                        }
                    }
                    if (recipe instanceof FurnaceRecipe) {
                        ItemStack ingredient = ((FurnaceRecipe) recipe).getInput();
                        if (ingredient != null)
                            ingredient.setAmount(check.getAmount());
                        if (ingredient != null)
                            xp += getTotalValue(ingredient, calculated) / (double) recipe.getResult().getAmount();
                    }
                    if (xp > 0)
                        break;
                }
            }
        }
        return xp;
    }

    public List<UUID> burned = new ArrayList<>();

    @EventHandler
    public void onCombust(EntityCombustEvent event) {
        if (event.getEntityType() == EntityType.DROPPED_ITEM) {
            if (!burned.contains(event.getEntity().getUniqueId())) {
                burned.add(event.getEntity().getUniqueId());

                Item item = (Item) event.getEntity();
                convertToXp(item.getItemStack(), item.getLocation());

                event.getEntity().remove();
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        burned.remove(event.getEntity().getUniqueId());
                    }
                }.runTaskLater(this, 10L);
            }
        }
    }

    @EventHandler
    public void onItemDestroy(EntityDamageEvent event) {
        if (event.getEntityType() == EntityType.DROPPED_ITEM) {
            if (!burned.contains(event.getEntity().getUniqueId())) {
                burned.add(event.getEntity().getUniqueId());

                Item item = (Item) event.getEntity();
                convertToXp(item.getItemStack(), item.getLocation());

                event.getEntity().remove();
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        burned.remove(event.getEntity().getUniqueId());
                    }
                }.runTaskLater(this, 10L);
            }
        }
    }

    @EventHandler
    public void onItemDespawn(ItemDespawnEvent event) {
        if (!burned.contains(event.getEntity().getUniqueId())) {
            burned.add(event.getEntity().getUniqueId());

            Item item = event.getEntity();
            convertToXp(item.getItemStack(), item.getLocation());

            event.getEntity().remove();
            new BukkitRunnable() {
                @Override
                public void run() {
                    burned.remove(event.getEntity().getUniqueId());
                }
            }.runTaskLater(this, 10L);
        }
    }

    @EventHandler
    public void onMobSpawn(EntitySpawnEvent event) {
        if (event.getEntityType() == EntityType.BAT)
            event.setCancelled(true);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.isGliding()) {
            String speed = ((int) (event.getFrom().distance(event.getTo()) * 20 * 60)) + "";
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(speed + " blocks/minute"));
            if (player.getVelocity().length() < 0.8)
                player.setVelocity(player.getVelocity().multiply(new Vector(1.05, 1, 1.05)));

            Location prevLocation = prevLocations.getOrDefault(player.getUniqueId(), event.getFrom());
            Location lookAt = Distance.lookAt(prevLocation, event.getFrom());
            double yaw = -lookAt.getYaw();
            double pitch = lookAt.getPitch() + 90;
            double powerFactor = 0.07;
            for (int i = 0; i < 360; i += 10) {
                double x = 1;
                double y = 0;
                double z = 0;

                double[] rotate = rotateAroundY(x, y, z, i * (Math.PI / 180));
                rotate = rotateAroundZ(rotate[0], rotate[1], rotate[2], pitch * (Math.PI / 180));
                rotate = rotateAroundY(rotate[0], rotate[1], rotate[2], (yaw + 90) * (Math.PI / 180));

                player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, event.getFrom(), 0, rotate[0] * powerFactor, rotate[1] * powerFactor, rotate[2] * powerFactor);
            }
            prevLocations.put(player.getUniqueId(), event.getFrom());
        } else {
            prevLocations.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onExplode(ExplosionPrimeEvent event) {
        if (event.getEntityType() == EntityType.CREEPER) {
            event.setRadius(0);
        }
    }

    private HashMap<UUID, Location> prevLocations = new HashMap<>();

    public double[] rotateAroundZ(double x, double y, double z, double t) {
        double x2 = x * Math.cos(t) - y * Math.sin(t);
        double y2 = x * Math.sin(t) + y * Math.cos(t);
        double z2 = z;
        double[] all = {x2, y2, z2};
        return all;
    }

    public double[] rotateAroundY(double x, double y, double z, double t) {
        double x2 = x * Math.cos(t) + z * Math.sin(t);
        double y2 = y;
        double z2 = -x * Math.sin(t) + z * Math.cos(t);
        double[] all = {x2, y2, z2};
        return all;
    }

    public double[] rotateAroundX(double x, double y, double z, double t) {
        double x2 = x;
        double y2 = y * Math.cos(t) - z * Math.sin(t);
        double z2 = y * Math.sin(t) + z * Math.cos(t);
        double[] all = {x2, y2, z2};
        return all;
    }

}
