package dev.fallingcloud.betterinventory.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import dev.fallingcloud.betterinventory.BetterInventoryConfig;
import dev.fallingcloud.betterinventory.BetterInventoryLayout;
import dev.fallingcloud.betterinventory.client.screen.BetterInventoryScreen;
import dev.fallingcloud.betterinventory.inv.FurnaceState;
import dev.fallingcloud.betterinventory.inv.PlayerLoadout;
import dev.fallingcloud.betterinventory.logic.StackUpgradeService;
import dev.fallingcloud.betterinventory.menu.BetterInventoryMenu;
import dev.fallingcloud.betterinventory.shim.ItemStackHandler;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/**
 * 1.4.1 client self-test: a scripted walk through every client-visible feature
 * (screen swap, stack caps, GUI scales, Better Combat bridge, creative
 * interop, furnace UI, rules tabs, death-drop recovery), enabled with
 * {@code -Dbetterinventory.selftest=true} and logged as PASS/FAIL lines.
 *
 * <p>26.3 adaptations (all javap-verified): screens live on
 * {@code minecraft.gui} (screen()/setScreen), clicks go through
 * {@code AbstractContainerMenu.clicked(int, int, ContainerInput, Player)} with
 * the {@link ContainerInput} enum replacing ClickType, {@code gameMode}
 * replaces interactionManager, {@code KeyMapping.set/click(InputConstants.Key)}
 * replace the KeyBinding press helpers, {@code options.guiScale()} returns an
 * OptionInstance ({@code get()/set()}), pitch is {@code setXRot},
 * {@code resizeGui()} replaces onResolutionChanged, screenshots use
 * {@code Screenshot.grab(gameDirectory, name, gameRenderer.mainRenderTarget(),
 * 1, callback)}, the effect of the creative-tab click is driven through
 * {@link CreativeInventory#tabClicked} (the same handler the F-4
 * CreativeScreenMixin invokes; 26.3's CreativeModeTab dropped
 * getRow/getColumn/isSpecial so the 1.4.1 coordinate math cannot be
 * reproduced), and {@code Level.getEntities()} needs an AABB so the player's
 * box inflated by 96 blocks is used.
 */
public final class ClientSelfTest {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int VANILLA_CONTAINER_MAX = 99;
    private static Step step = Step.WAIT_WORLD;
    private static int wait;
    private static int pass;
    private static int fail;
    private static int toolsBeforeDeath;
    private static int polls;
    private static Vec3 deathPos = Vec3.ZERO;
    private static String lastScreen = "none";
    private static final int[] SCALES = new int[]{2, 3, 4};
    private static int scaleIndex;
    private static int savedScale = -1;
    private static final boolean COMBAT_ONLY = "combat".equals(System.getProperty("betterinventory.selftest.only"));
    private static final boolean RULES_ONLY = "rules".equals(System.getProperty("betterinventory.selftest.only"));
    private static final String[] BC_SERIES = new String[]{"rack", "held", "hit"};
    private static final CameraType[] BC_VIEWS = new CameraType[]{CameraType.FIRST_PERSON, CameraType.THIRD_PERSON_FRONT};
    private static final int BC_SWINGS = 3;
    private static int bcSeries;
    private static int bcView;
    private static int bcSwing;
    private static int bcTick;
    private static int bcBlockSlot = -1;
    private static float bcTargetHealth;
    private static int furnaceClicks;
    private static int carriedBeforeDeath;
    private static int hubBeforeDeath;
    private static final List<String> bcRackAnimations = new ArrayList<>();
    private static final List<String> bcHeldAnimations = new ArrayList<>();
    private static volatile int attributeEvents;
    private static boolean probing;

    private ClientSelfTest() {}

    public static boolean enabled() {
        return Boolean.getBoolean("betterinventory.selftest");
    }

    public static void attributeEvent() {
        attributeEvents++;
        Minecraft minecraft = Minecraft.getInstance();
        if (probing && minecraft.isRunning() && minecraft.player != null) {
            ClientCombatBridge.substituteForBetterCombat(minecraft.player.getInventory().getSelectedItem());
        }
    }

    private static void check(String name, boolean ok, String detail) {
        if (ok) {
            pass++;
            LOGGER.info("[BI selftest] PASS {} ({})", name, detail);
        } else {
            fail++;
            LOGGER.error("[BI selftest] FAIL {} ({})", name, detail);
        }
    }

    private static void go(Step next, int ticks) {
        step = next;
        wait = ticks;
    }

    private static int filled(ItemStackHandler handler) {
        int n = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            if (!handler.getStackInSlot(i).isEmpty()) {
                n++;
            }
        }
        return n;
    }

    private static void clickTab(Minecraft minecraft, int tab) {
        if (minecraft.gui.screen() instanceof BetterInventoryScreen inventory) {
            clickScreen(inventory, BetterInventoryLayout.leftPos(inventory.width) + 134 + tab * 24 + 12, (inventory.height - 216) / 2 + 10);
        }
    }

    /** 26.3 adaptation: Screen.mouseClicked now takes a MouseButtonEvent plus a
     * handled flag; a left click at (x, y) is synthesized for the scripted
     * presses that the 1.4.1 selftest performed with the old int overload.
     * 26.3 mouse buttons use SDL codes: 1 = left, 3 = right. */
    private static void clickScreen(Screen screen, double x, double y) {
        screen.mouseClicked(new MouseButtonEvent(x, y, new MouseButtonInfo(1, 0)), false);
    }

    private static String screenName(Minecraft minecraft) {
        return minecraft.gui.screen() == null ? "none" : minecraft.gui.screen().getClass().getSimpleName();
    }

    public static void tick(Minecraft minecraft) {
        if (step != Step.DONE && minecraft.player != null && minecraft.level != null) {
            String screen = screenName(minecraft);
            if (!screen.equals(lastScreen)) {
                LOGGER.info("[BI selftest] screen {} -> {} (at {})", lastScreen, screen, step);
                lastScreen = screen;
            }

            if (step.name().startsWith("BC_")
                    && step != Step.BC_WATCH
                    && step != Step.BC_BEGIN
                    && betterCombat()
                    && Integer.valueOf(1).compareTo(asInt(bcClient(minecraft, "getUpswingTicks"))) <= 0) {
                LOGGER.warn(
                        "[BI selftest] unrequested swing at {} wait={} attackDown={} windowActive={} combo={}",
                        step, wait, minecraft.options.keyAttack.isDown(), minecraft.getWindow().isFocused(), bcClient(minecraft, "getComboCount"));
            }

            if (wait-- <= 0) {
                try {
                    advance(minecraft);
                } catch (RuntimeException e) {
                    fail++;
                    LOGGER.error("[BI selftest] FAIL client threw at {}", step, e);
                    finish(minecraft);
                }
            }
        }
    }

    private static void advance(Minecraft minecraft) {
        PlayerLoadout loadout = ClientHudState.loadout(minecraft);
        switch (step) {
            case WAIT_WORLD -> {
                if (minecraft.player.isDeadOrDying() || minecraft.gui.screen() instanceof DeathScreen) {
                    minecraft.player.respawn();
                    go(Step.WAIT_WORLD, 60);
                } else if (!minecraft.player.onGround()) {
                    go(Step.WAIT_WORLD, 20);
                } else {
                    go(Step.FILL, 100);
                }
            }
            case FILL -> {
                LOGGER.info("[BI selftest] vanilla-inventory key is bound to: {}", BetterInventoryClient.OPEN_VANILLA_INVENTORY.getTranslatedKeyMessage().getString());
                minecraft.getConnection().sendCommand("difficulty peaceful");
                if (RULES_ONLY) {
                    minecraft.getConnection().sendCommand("betterinventory rules");
                    go(Step.RULES_OPEN, 40);
                    return;
                }
                minecraft.getConnection().sendCommand("gamerule keepInventory false");
                minecraft.getConnection().sendCommand("betterinventory fill 3");
                go(COMBAT_ONLY ? Step.BC_BEGIN : Step.OPEN, 40);
            }
            case OPEN -> {
                if (minecraft.gui.screen() != null) {
                    minecraft.gui.setScreen(null);
                    go(Step.OPEN, 5);
                    return;
                }
                minecraft.gui.setScreen(new InventoryScreen(minecraft.player));
                go(Step.AWAIT_SCREEN, 40);
            }
            case AWAIT_SCREEN -> {
                check("inventory key opens the mod screen", minecraft.gui.screen() instanceof BetterInventoryScreen, "screen=" + screenName(minecraft));
                toolsBeforeDeath = filled(loadout.tools);
                check(
                        "loadout synced to the client",
                        toolsBeforeDeath > 0,
                        "tools=" + toolsBeforeDeath + " offhand=" + filled(loadout.offhandStore) + " gather=" + filled(loadout.gather) + " backpack=" + !loadout.backpack().isEmpty());
                int cap = minecraft.player.getInventory().getMaxStackSize();
                check(
                        "player inventory takes the stack upgrade",
                        cap == StackUpgradeService.capFor(minecraft.player) && cap != VANILLA_CONTAINER_MAX,
                        "inventoryMax=" + cap + " upgradeCap=" + StackUpgradeService.capFor(minecraft.player));
                go(Step.SHOT, 20);
            }
            case SHOT -> {
                saveScreenshot(minecraft.gameDirectory, "bi-selftest-inventory.png",
                        message -> LOGGER.info("[BI selftest] {}", message.getString()));
                savedScale = minecraft.options.guiScale().get();
                go(Step.SCALE, 10);
            }
            case SCALE -> {
                if (scaleIndex >= SCALES.length) {
                    minecraft.options.guiScale().set(savedScale);
                    minecraft.resizeGui();
                    go(Step.LOOKUP, 10);
                    return;
                }
                minecraft.options.guiScale().set(SCALES[scaleIndex]);
                minecraft.resizeGui();
                go(Step.SCALE_CHECK, 10);
            }
            case SCALE_CHECK -> {
                int scale = SCALES[scaleIndex];
                if (minecraft.gui.screen() instanceof BetterInventoryScreen inventory) {
                    int width = inventory.width;
                    int[] view = playerViewArea(inventory);
                    int[] zoom = zoomArea(inventory);
                    int right = zoom != null ? zoom[0] + zoom[2] : view[0] + 350;
                    int strip = view[0] + 216;
                    check(
                            "layout fits the screen at GUI scale " + scale,
                            width < 350 || view[0] >= 0 && right <= width,
                            "screen=" + width + "x" + inventory.height + " left=" + view[0] + " right=" + right
                                    + " stripCentre=" + strip + " screenCentre=" + width / 2 + " zoom=" + (zoom != null)
                                    + " viewer=" + RecipeViewers.installed());
                    pointAtFirstItem(minecraft, inventory);
                } else {
                    check("layout fits the screen at GUI scale " + scale, false, "screen=" + screenName(minecraft));
                }
                go(Step.SCALE_SHOT, 4);
            }
            case SCALE_SHOT -> {
                saveScreenshot(minecraft.gameDirectory, "bi-selftest-scale" + SCALES[scaleIndex++] + ".png",
                        message -> LOGGER.info("[BI selftest] {}", message.getString()));
                go(Step.SCALE, 5);
            }
            case LOOKUP -> {
                minecraft.player.closeContainer();
                go(Step.LOOKUP_CHECK, 5);
            }
            case LOOKUP_CHECK -> {
                Inventory inventory = minecraft.player.getInventory();
                for (int i = 0; i < Inventory.getSelectionSize(); i++) {
                    if (inventory.getItem(i).getItem() instanceof BlockItem) {
                        inventory.setSelectedSlot(i);
                        break;
                    }
                }
                ItemStack held = inventory.getSelectedItem();
                int before = attributeEvents;
                ClientHudState.weaponFlashTicks = 15;
                probing = true;
                ItemStack answer;
                try {
                    answer = ClientCombatBridge.substituteForBetterCombat(held);
                } finally {
                    probing = false;
                }
                check(
                        "Better Combat's weapon lookup answers the rack weapon without asking other mods",
                        answer != held && !answer.isEmpty() && attributeEvents == before,
                        "held=" + held + " answer=" + answer + " attributeEvents=" + (attributeEvents - before) + " (seen before: " + before + ")");
                go(Step.BC_BEGIN, 5);
            }
            case BC_BEGIN -> {
                if (!betterCombat()) {
                    LOGGER.info("[BI selftest] Better Combat is not installed - its scenario is skipped");
                    go(Step.VANILLA_KEY, 5);
                    return;
                }
                if (minecraft.gui.screen() != null) {
                    minecraft.gui.setScreen(null);
                }
                Inventory inventory = minecraft.player.getInventory();
                for (int i = 1; i < Inventory.getSelectionSize(); i++) {
                    if (inventory.getItem(i).getItem() instanceof BlockItem) {
                        bcBlockSlot = i;
                        break;
                    }
                }
                minecraft.getConnection().sendCommand("item replace entity @s hotbar.0 with diamond_sword");
                minecraft.getConnection().sendCommand("time set noon");
                minecraft.player.setXRot(-35.0F);
                bcSeries = 0;
                bcView = 0;
                bcSwing = 0;
                go(Step.BC_NEXT, 20);
            }
            case BC_CLICK -> {
                if (!BC_SERIES[bcSeries].equals("hit")
                        && minecraft.crosshairPickEntity == null
                        && minecraft.player.getXRot() > -85.0F) {
                    minecraft.player.setXRot(minecraft.player.getXRot() - 10.0F);
                    go(Step.BC_CLICK, 3);
                    return;
                }
                InputConstants.Key attack = attackKey(minecraft);
                KeyMapping.set(attack, true);
                KeyMapping.click(attack);
                bcTick = 0;
                go(Step.BC_WATCH, 0);
            }
            case BC_WATCH -> {
                bcTick++;
                String series = BC_SERIES[bcSeries];
                String view = bcView == 0 ? "first" : "third";
                Object hand = Boolean.TRUE.equals(bcClient(minecraft, "isWeaponSwingInProgress")) ? bcCurrentAttack(minecraft) : null;
                String animation = hand == null ? "none" : String.valueOf(bcGet(bcGet(hand, "attack"), "animation"));
                if (bcTick == 1 && !series.equals("hit")) {
                    (series.equals("rack") ? bcRackAnimations : bcHeldAnimations).add(animation);
                }
                if (bcTick <= 14) {
                    LivingEntity target = bcTarget(minecraft);
                    LOGGER.info(
                            "[BI selftest] bc {} {} swing={} t={} combo={} upswing={} animation={} swingStack={} hud={} flash={} delay={} crosshair={} inReach={} target={}",
                            series, view, bcSwing, bcTick,
                            bcClient(minecraft, "getComboCount"),
                            bcClient(minecraft, "getUpswingTicks") + (minecraft.options.keyAttack.isDown() ? "+keydown" : ""),
                            animation,
                            hand == null ? "none" : bcGet(hand, "itemStack"),
                            ClientHudState.activeTool,
                            ClientHudState.weaponFlashTicks,
                            minecraft.player.getAttackStrengthScale(1.0F),
                            minecraft.crosshairPickEntity == null ? "null" : minecraft.crosshairPickEntity.getType().getDescription().getString(),
                            bcClient(minecraft, "hasTargetsInReach"),
                            target == null ? "none" : target.getHealth() + "hp at " + String.format("%.2f", target.distanceTo(minecraft.player)));
                }
                if (bcTick == 2) {
                    KeyMapping.set(attackKey(minecraft), false);
                }
                if (bcTick == 3 || bcTick == 5 || bcTick == 7 || bcTick == 9) {
                    saveScreenshot(minecraft.gameDirectory, "bi-bc-" + series + "-" + view + "-" + bcSwing + "-t" + bcTick + ".png", message -> {});
                }
                if (bcTick < 18) {
                    go(Step.BC_WATCH, 0);
                } else if (series.equals("hit")) {
                    go(Step.BC_HIT_CHECK, 10);
                } else if (++bcSwing < BC_SWINGS) {
                    go(Step.BC_CLICK, 0);
                } else if (++bcView < BC_VIEWS.length) {
                    go(Step.BC_NEXT, 0);
                } else if (++bcSeries < BC_SERIES.length - 1) {
                    bcView = 0;
                    go(Step.BC_NEXT, 0);
                } else {
                    minecraft.options.setCameraType(CameraType.FIRST_PERSON);
                    check(
                            "with Better Combat the rack weapon swings its whole combo, like the same weapon held",
                            bcRackAnimations.equals(bcHeldAnimations) && new HashSet<>(bcHeldAnimations).size() == BC_SWINGS,
                            "rack=" + bcRackAnimations + " held=" + bcHeldAnimations);
                    go(Step.BC_TARGET, 5);
                }
            }
            case BC_NEXT -> {
                boolean held = BC_SERIES[bcSeries].equals("held");
                minecraft.player.getInventory().setSelectedSlot(held ? 0 : bcBlockSlot);
                minecraft.options.setCameraType(BC_VIEWS[bcView]);
                bcSwing = 0;
                go(Step.BC_CLICK, 45);
            }
            case BC_TARGET -> {
                if (bcTargetHealth == 0.0F) {
                    minecraft.player.getInventory().setSelectedSlot(bcBlockSlot);
                    minecraft.player.setXRot(0.0F);
                    bcTargetHealth = -1.0F;
                    go(Step.BC_TARGET, 10);
                    return;
                }
                minecraft.getConnection().sendCommand("summon minecraft:villager ^ ^ ^2 {NoAI:1b,Silent:1b,PersistenceRequired:1b}");
                bcSeries = BC_SERIES.length - 1;
                bcView = 0;
                bcSwing = 0;
                go(Step.BC_CLICK, 50);
            }
            case BC_HIT_CHECK -> {
                LivingEntity target = bcTarget(minecraft);
                float health = target == null ? -1.0F : target.getHealth();
                check(
                        "with Better Combat a block in the hand hits for the rack sword's damage",
                        target != null && target.getMaxHealth() - health >= 4.0F,
                        "target=" + (target == null ? "none" : target.getType().getDescription().getString())
                                + " health " + (target == null ? -1.0F : target.getMaxHealth()) + " -> " + health);
                minecraft.getConnection().sendCommand("kill @e[type=minecraft:villager,distance=..8]");
                go(Step.BC_END, 10);
            }
            case BC_END -> {
                if (COMBAT_ONLY) {
                    finish(minecraft);
                } else {
                    go(Step.VANILLA_KEY, 5);
                }
            }
            case VANILLA_KEY -> {
                minecraft.player.closeContainer();
                InventorySwap.openVanillaOnce(minecraft);
                go(Step.AWAIT_VANILLA, 10);
            }
            case AWAIT_VANILLA -> {
                saveScreenshot(minecraft.gameDirectory, "bi-selftest-vanilla.png",
                        message -> LOGGER.info("[BI selftest] {}", message.getString()));
                check(
                        "the vanilla-inventory key opens Minecraft's own screen",
                        minecraft.gui.screen() != null && minecraft.gui.screen().getClass() == InventoryScreen.class && !InventorySwap.vanillaPreferred(),
                        "screen=" + screenName(minecraft) + " vanillaPreferred=" + InventorySwap.vanillaPreferred());
                minecraft.gui.setScreen(null);
                go(Step.CREATIVE, 5);
            }
            case CREATIVE -> {
                minecraft.getConnection().sendCommand("gamemode creative");
                minecraft.getConnection().sendCommand("item replace entity @s hotbar.8 with air");
                go(Step.CREATIVE_OPEN, 20);
            }
            case CREATIVE_OPEN -> {
                minecraft.gui.setScreen(new InventoryScreen(minecraft.player));
                go(Step.CREATIVE_TAB, 10);
            }
            case CREATIVE_TAB -> {
                check(
                        "in creative the inventory key opens the creative screen's item tabs",
                        minecraft.gui.screen() instanceof CreativeModeInventoryScreen && !CreativeInventory.vanillaInventoryShowing(minecraft.gui.screen()),
                        "screen=" + screenName(minecraft));
                clickInventoryTab(minecraft);
                go(Step.CREATIVE_OURS, 40);
            }
            case CREATIVE_OURS -> {
                check("...and its inventory tab opens the mod screen", minecraft.gui.screen() instanceof BetterInventoryScreen, "screen=" + screenName(minecraft));
                saveScreenshot(minecraft.gameDirectory, "bi-selftest-creative.png",
                        message -> LOGGER.info("[BI selftest] {}", message.getString()));
                if (minecraft.gui.screen() instanceof BetterInventoryScreen inventory) {
                    clickScreen(inventory, BetterInventoryLayout.leftPos(inventory.width) + 91 + 12, (inventory.height - 216) / 2 + 10);
                }
                go(Step.CREATIVE_ITEMS, 10);
            }
            case CREATIVE_ITEMS -> {
                check(
                        "the mod screen's creative tab leads back to the item tabs",
                        minecraft.gui.screen() instanceof CreativeModeInventoryScreen && !CreativeInventory.vanillaInventoryShowing(minecraft.gui.screen()),
                        "screen=" + screenName(minecraft));
                minecraft.player.inventoryMenu.setCarried(new ItemStack(Items.DIAMOND, 3));
                clickInventoryTab(minecraft);
                go(Step.CREATIVE_CARRIED, 40);
            }
            case CREATIVE_CARRIED -> {
                ItemStack carried = minecraft.gui.screen() instanceof BetterInventoryScreen inventory
                        ? ((BetterInventoryMenu) inventory.getMenu()).getCarried()
                        : ItemStack.EMPTY;
                check(
                        "a stack on the cursor comes along to the mod screen",
                        carried.is(Items.DIAMOND) && carried.getCount() == 3,
                        "screen=" + screenName(minecraft) + " cursor=" + carried);
                minecraft.player.closeContainer();
                go(Step.CREATIVE_KEY, 5);
            }
            case CREATIVE_KEY -> {
                minecraft.gui.setScreen(new InventoryScreen(minecraft.player));
                go(Step.CREATIVE_REOPENED, 40);
            }
            case CREATIVE_REOPENED -> {
                check(
                        "the inventory key reopens the mod screen while it was the last one open",
                        minecraft.gui.screen() instanceof BetterInventoryScreen,
                        "screen=" + screenName(minecraft));
                InventorySwap.toVanilla(minecraft);
                go(Step.CREATIVE_VANILLA, 10);
            }
            case CREATIVE_VANILLA -> {
                saveScreenshot(minecraft.gameDirectory, "bi-selftest-creative-vanilla.png",
                        message -> LOGGER.info("[BI selftest] {}", message.getString()));
                check(
                        "in creative the swap button goes to the creative screen's own inventory tab",
                        minecraft.gui.screen() instanceof CreativeModeInventoryScreen
                                && CreativeInventory.vanillaInventoryShowing(minecraft.gui.screen())
                                && InventorySwap.vanillaPreferred(),
                        "screen=" + screenName(minecraft) + " vanillaPreferred=" + InventorySwap.vanillaPreferred());
                InventorySwap.toBetter(minecraft);
                go(Step.CREATIVE_BACK, 40);
            }
            case CREATIVE_BACK -> {
                check(
                        "...and back to the mod screen",
                        minecraft.gui.screen() instanceof BetterInventoryScreen && !InventorySwap.vanillaPreferred(),
                        "screen=" + screenName(minecraft) + " vanillaPreferred=" + InventorySwap.vanillaPreferred());
                minecraft.player.closeContainer();
                minecraft.getConnection().sendCommand("gamemode survival");
                go(Step.REOPEN, 20);
            }
            case REOPEN -> {
                minecraft.gui.setScreen(new InventoryScreen(minecraft.player));
                go(Step.AWAIT_REOPEN, 40);
            }
            case AWAIT_REOPEN -> {
                check(
                        "...and the inventory key still opens the mod screen afterwards",
                        minecraft.gui.screen() instanceof BetterInventoryScreen,
                        "screen=" + screenName(minecraft));
                go(Step.FURNACE_GIVE, 5);
            }
            case FURNACE_GIVE -> {
                minecraft.getConnection().sendCommand("item replace entity @s inventory.0 with betterinventory:furnace_crafting_upgrade");
                minecraft.getConnection().sendCommand("item replace entity @s inventory.1 with minecraft:raw_iron 8");
                minecraft.getConnection().sendCommand("item replace entity @s inventory.2 with minecraft:coal 4");
                go(Step.FURNACE_EQUIP, 20);
            }
            case FURNACE_EQUIP -> {
                if (!(minecraft.gui.screen() instanceof BetterInventoryScreen inventory)) {
                    check("the crafting tab's second click shows the furnace", false, "screen=" + screenName(minecraft));
                    go(Step.KILL, 5);
                    return;
                }
                BetterInventoryMenu menu = (BetterInventoryMenu) inventory.getMenu();
                menu.clicked(9, 0, ContainerInput.PICKUP, minecraft.player);
                menu.clicked(100, 0, ContainerInput.PICKUP, minecraft.player);
                menu.clicked(9, 0, ContainerInput.PICKUP, minecraft.player);
                furnaceClicks = 0;
                go(Step.FURNACE_OPEN, 10);
            }
            case FURNACE_OPEN -> {
                if (minecraft.gui.screen() instanceof BetterInventoryScreen inventory
                        && !((BetterInventoryMenu) inventory.getMenu()).furnaceOpen()
                        && furnaceClicks++ < 3) {
                    clickScreen(inventory, BetterInventoryLayout.leftPos(inventory.width) + 254 + 12, (inventory.height - 216) / 2 + 10);
                    go(Step.FURNACE_OPEN, 10);
                    return;
                }
                go(Step.FURNACE_FEED, 2);
            }
            case FURNACE_FEED -> {
                boolean shown = minecraft.gui.screen() instanceof BetterInventoryScreen inventory
                        && ((BetterInventoryMenu) inventory.getMenu()).furnaceOpen()
                        && ((BetterInventoryMenu) inventory.getMenu()).getSlot(117).isActive();
                check(
                        "the crafting tab's second click shows the furnace",
                        shown,
                        "screen=" + screenName(minecraft) + " hasFurnace=" + loadout.hasFurnace() + " clicks=" + furnaceClicks);
                if (minecraft.gui.screen() instanceof BetterInventoryScreen inventory) {
                    BetterInventoryMenu menu = (BetterInventoryMenu) inventory.getMenu();
                    menu.clicked(10, 0, ContainerInput.QUICK_MOVE, minecraft.player);
                    menu.clicked(11, 0, ContainerInput.QUICK_MOVE, minecraft.player);
                }
                go(Step.FURNACE_LIT, 60);
            }
            case FURNACE_LIT -> {
                FurnaceState furnace = loadout.furnace;
                check(
                        "shift-click feeds it and it lights",
                        furnace.items.getStackInSlot(0).is(Items.RAW_IRON) && furnace.lit > 0 && furnace.cook > 0,
                        "input=" + furnace.items.getStackInSlot(0) + " fuel=" + furnace.items.getStackInSlot(1)
                                + " lit=" + furnace.lit + "/" + furnace.litDuration + " cook=" + furnace.cook + "/" + furnace.cookTotal);
                saveScreenshot(minecraft.gameDirectory, "bi-selftest-furnace.png",
                        message -> LOGGER.info("[BI selftest] {}", message.getString()));
                go(Step.FURNACE_DONE, 170);
            }
            case FURNACE_DONE -> {
                ItemStack out = loadout.furnace.items.getStackInSlot(2);
                check("...and smelts while the screen is open", out.is(Items.IRON_INGOT), "output=" + out);
                go(Step.KILL, 5);
            }
            case RULES_OPEN -> {
                if (minecraft.gui.screen() != null) {
                    minecraft.gui.setScreen(null);
                    go(Step.RULES_OPEN, 5);
                    return;
                }
                minecraft.gui.setScreen(new InventoryScreen(minecraft.player));
                go(Step.RULES_CHECK, 40);
            }
            case RULES_CHECK -> {
                boolean tabs = true;
                for (int tab = 0; tab < 5; tab++) {
                    tabs &= loadout.tabAvailable(tab);
                }
                check(
                        "with requireBackpack off in the config every tab is open with no backpack",
                        minecraft.gui.screen() instanceof BetterInventoryScreen
                                && loadout.packFree
                                && loadout.backpack().isEmpty()
                                && tabs
                                && loadout.gatherUnlocked()
                                && loadout.upgradesUnlocked(),
                        "screen=" + screenName(minecraft) + " packFree=" + loadout.packFree + " backpack=" + loadout.backpack()
                                + " tabs=" + tabs + " extraArea=" + loadout.gatherUnlocked() + " upgradeSlots=" + loadout.upgradesUnlocked());
                saveScreenshot(minecraft.gameDirectory, "bi-selftest-rules.png",
                        message -> LOGGER.info("[BI selftest] {}", message.getString()));
                clickTab(minecraft, 2);
                go(Step.RULES_TAB, 20);
            }
            case RULES_TAB -> {
                if (!(minecraft.gui.screen() instanceof BetterInventoryScreen inventory)) {
                    check("with swapMainInventory off a tab pages the extra area and leaves the main inventory", false, "screen=" + screenName(minecraft));
                } else {
                    ItemStack extra = ((BetterInventoryMenu) inventory.getMenu()).getSlot(36).getItem();
                    ItemStack main = ((BetterInventoryMenu) inventory.getMenu()).getSlot(9).getItem();
                    check(
                            "with swapMainInventory off a tab pages the extra area and leaves the main inventory",
                            loadout.extraOnly
                                    && ((BetterInventoryMenu) inventory.getMenu()).activeTab() == 2
                                    && extra.is(Items.DIAMOND)
                                    && extra.getCount() >= 5
                                    && main.is(Items.GOLD_INGOT),
                            "extraOnly=" + loadout.extraOnly + " tab=" + ((BetterInventoryMenu) inventory.getMenu()).activeTab()
                                    + " extra=" + extra + " main=" + main);
                    saveScreenshot(minecraft.gameDirectory, "bi-selftest-rules-tab2.png",
                            message -> LOGGER.info("[BI selftest] {}", message.getString()));
                }
                clickTab(minecraft, 0);
                go(Step.RULES_BACK, 20);
            }
            case RULES_BACK -> {
                if (!(minecraft.gui.screen() instanceof BetterInventoryScreen inventory)) {
                    check("...and the default tab shows its own extra area again", false, "screen=" + screenName(minecraft));
                } else {
                    ItemStack extra = ((BetterInventoryMenu) inventory.getMenu()).getSlot(36).getItem();
                    ItemStack main = ((BetterInventoryMenu) inventory.getMenu()).getSlot(9).getItem();
                    check(
                            "...and the default tab shows its own extra area again",
                            ((BetterInventoryMenu) inventory.getMenu()).activeTab() == 0 && !extra.is(Items.DIAMOND) && main.is(Items.GOLD_INGOT),
                            "tab=" + ((BetterInventoryMenu) inventory.getMenu()).activeTab() + " extra=" + extra + " main=" + main);
                }
                finish(minecraft);
            }
            case KILL -> {
                if (corpseMod()
                        && minecraft.gui.screen() instanceof BetterInventoryScreen inventory
                        && ((BetterInventoryMenu) inventory.getMenu()).activeTab() != 0) {
                    clickTab(minecraft, 0);
                    go(Step.KILL, 10);
                    return;
                }
                minecraft.player.closeContainer();
                deathPos = minecraft.player.position();
                if (corpseMod()) {
                    carriedBeforeDeath = carried(minecraft);
                    hubBeforeDeath = total(loadout.gather);
                } else {
                    minecraft.getConnection().sendCommand("clear @s");
                }
                minecraft.getConnection().sendCommand("kill @s");
                go(Step.AWAIT_DEATH, 30);
            }
            case AWAIT_DEATH -> {
                check("death screen", minecraft.gui.screen() instanceof DeathScreen, "screen=" + screenName(minecraft));
                minecraft.player.respawn();
                go(Step.AWAIT_RESPAWN, 60);
            }
            case AWAIT_RESPAWN -> {
                if (minecraft.gui.screen() != null) {
                    minecraft.gui.setScreen(null);
                }
                check(
                        "death drops the loadout",
                        filled(loadout.tools) == 0 && loadout.backpack().isEmpty(),
                        "tools=" + filled(loadout.tools) + " backpack=" + !loadout.backpack().isEmpty());
                minecraft.getConnection().sendCommand("tp @s " + deathPos.x + " " + deathPos.y + " " + deathPos.z);
                go(Step.AWAIT_PICKUP, 60);
            }
            case AWAIT_PICKUP -> {
                if (corpseMod()) {
                    if (polls++ == 0) {
                        minecraft.getConnection().sendCommand("betterinventory corpse");
                        go(Step.AWAIT_PICKUP, 40);
                        return;
                    }
                    check(
                            "with Corpse the loadout goes straight home from the corpse",
                            filled(loadout.tools) == toolsBeforeDeath && !loadout.backpack().isEmpty() && total(loadout.gather) == hubBeforeDeath,
                            "tools=" + filled(loadout.tools) + "/" + toolsBeforeDeath + " backpack=" + !loadout.backpack().isEmpty()
                                    + " hub=" + total(loadout.gather) + "/" + hubBeforeDeath);
                    check(
                            "...and every item the player carried comes back, oversized stacks included",
                            carried(minecraft) == carriedBeforeDeath,
                            "carried=" + carried(minecraft) + "/" + carriedBeforeDeath);
                    finish(minecraft);
                    return;
                }
                int onGround = 0;
                for (Entity entity : minecraft.level.getEntities((Entity) null, minecraft.player.getBoundingBox().inflate(96.0), e -> true)) {
                    if (entity instanceof ItemEntity && entity.distanceTo(minecraft.player) < 8.0F) {
                        onGround++;
                    }
                }
                boolean back = filled(loadout.tools) > 0 && !loadout.backpack().isEmpty();
                boolean all = filled(loadout.tools) == toolsBeforeDeath && !loadout.backpack().isEmpty();
                if (!all && (!back || onGround != 0) && polls++ < 45) {
                    if (!back && polls % 5 == 0) {
                        minecraft.getConnection().sendCommand("clear @s");
                    }
                    minecraft.getConnection().sendCommand("tp @e[type=item,distance=..12] @s");
                    go(Step.AWAIT_PICKUP, 20);
                    return;
                }
                check(
                        "picked-up drops return to the loadout",
                        back,
                        "tools=" + filled(loadout.tools) + "/" + toolsBeforeDeath + " backpack=" + !loadout.backpack().isEmpty()
                                + " stillOnGround=" + onGround + " polls=" + polls);
                finish(minecraft);
            }
            default -> finish(minecraft);
        }
    }

    /** 26.3 adaptation of the 1.4.1 screen's playerViewArea(): the 26.3 screen
     * provides it itself (F-2c); here the same rectangle is derived from the
     * layout constants so the selftest is independent of the screen port. */
    private static int[] playerViewArea(BetterInventoryScreen inventory) {
        int x = BetterInventoryLayout.leftPos(inventory.width);
        int y = (inventory.height - 216) / 2;
        return new int[]{x + BetterInventoryLayout.CHAR_X, y + BetterInventoryLayout.CHAR_Y,
                BetterInventoryLayout.CHAR_W, BetterInventoryLayout.CHAR_H};
    }

    /** See {@link #playerViewArea}; the zoom panel is only shown when it fits. */
    private static int[] zoomArea(BetterInventoryScreen inventory) {
        boolean fits = BetterInventoryConfig.ZOOM_ENABLED.get() && BetterInventoryLayout.zoomFits(inventory.width, RecipeViewers.installed());
        int x = BetterInventoryLayout.leftPos(inventory.width);
        int y = (inventory.height - 216) / 2;
        return fits ? new int[]{x + BetterInventoryLayout.ZOOM_X, y + BetterInventoryLayout.ZOOM_Y,
                BetterInventoryLayout.ZOOM_SIZE, BetterInventoryLayout.ZOOM_SIZE} : null;
    }

    private static void saveScreenshot(java.io.File gameDirectory, String name, java.util.function.Consumer<net.minecraft.network.chat.Component> callback) {
        Minecraft minecraft = Minecraft.getInstance();
        Screenshot.grab(gameDirectory, name, minecraft.gameRenderer.mainRenderTarget(), 1, callback);
    }

    private static Integer asInt(Object value) {
        return value instanceof Integer number ? number : 0;
    }

    private static boolean corpseMod() {
        try {
            Class.forName("de.maxhenkel.corpse.gui.Guis");
            return true;
        } catch (ClassNotFoundException | LinkageError absent) {
            return false;
        }
    }

    private static int total(ItemStackHandler handler) {
        int n = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            n += handler.getStackInSlot(i).getCount();
        }
        return n;
    }

    private static int carried(Minecraft minecraft) {
        Inventory inventory = minecraft.player.getInventory();
        int n = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            n += inventory.getItem(i).getCount();
        }
        return n;
    }

    private static boolean betterCombat() {
        try {
            Class.forName("net.bettercombat.api.MinecraftClient_BetterCombat");
            return true;
        } catch (ClassNotFoundException | LinkageError absent) {
            return false;
        }
    }

    private static Object bcClient(Minecraft minecraft, String method) {
        try {
            return Class.forName("net.bettercombat.api.MinecraftClient_BetterCombat").getMethod(method).invoke(minecraft);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return "?" + e.getClass().getSimpleName();
        }
    }

    private static Object bcCurrentAttack(Minecraft minecraft) {
        try {
            Object combo = bcClient(minecraft, "getComboCount");
            return Class.forName("net.bettercombat.logic.PlayerAttackHelper")
                    .getMethod("getCurrentAttack", Player.class, int.class)
                    .invoke(null, minecraft.player, combo);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    private static Object bcGet(Object record, String component) {
        try {
            Method method = record.getClass().getMethod(component);
            method.setAccessible(true);
            return method.invoke(record);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return "?" + e.getClass().getSimpleName();
        }
    }

    private static InputConstants.Key attackKey(Minecraft minecraft) {
        return ClientPlatform.boundKey(minecraft.options.keyAttack);
    }

    private static LivingEntity bcTarget(Minecraft minecraft) {
        for (Entity entity : minecraft.level.getEntities((Entity) null, minecraft.player.getBoundingBox().inflate(96.0), e -> true)) {
            if (entity instanceof Villager villager && entity.distanceTo(minecraft.player) < 6.0F) {
                return villager;
            }
        }
        return null;
    }

    /** 26.3 adaptation: CreativeModeTab lost getRow/getColumn/isSpecial, so the
     * 1.4.1 coordinate math cannot be reproduced; this drives the same
     * {@link CreativeInventory#tabClicked} handler the F-4 CreativeScreenMixin
     * invokes when the real inventory tab is clicked. */
    private static void clickInventoryTab(Minecraft minecraft) {
        for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
            if (tab.getType() == CreativeModeTab.Type.INVENTORY) {
                CreativeInventory.tabClicked(tab);
                return;
            }
        }
    }

    private static void pointAtFirstItem(Minecraft minecraft, BetterInventoryScreen inventory) {
        try {
            int[] view = playerViewArea(inventory);
            for (Slot slot : ((BetterInventoryMenu) inventory.getMenu()).slots) {
                if (slot.hasItem() && slot.isActive()) {
                    double scale = minecraft.getWindow().getGuiScale();
                    int top = (inventory.height - 216) / 2;
                    Field x = minecraft.mouseHandler.getClass().getDeclaredField("xpos");
                    Field y = minecraft.mouseHandler.getClass().getDeclaredField("ypos");
                    x.setAccessible(true);
                    y.setAccessible(true);
                    x.setDouble(minecraft.mouseHandler, (view[0] + slot.x + 8) * scale);
                    y.setDouble(minecraft.mouseHandler, (top + slot.y + 8) * scale);
                    return;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Best effort pointer nudge; the checks still report their own failures.
        }
    }

    private static void finish(Minecraft minecraft) {
        step = Step.DONE;
        LOGGER.info("[BI selftest] CLIENT DONE pass={} fail={}", pass, fail);
        if (Boolean.getBoolean("betterinventory.selftest.exit")) {
            minecraft.stop();
        }
    }

    private enum Step {
        WAIT_WORLD,
        FILL,
        OPEN,
        AWAIT_SCREEN,
        SHOT,
        SCALE,
        SCALE_CHECK,
        SCALE_SHOT,
        LOOKUP,
        LOOKUP_CHECK,
        BC_BEGIN,
        BC_CLICK,
        BC_WATCH,
        BC_NEXT,
        BC_TARGET,
        BC_HIT_CHECK,
        BC_END,
        VANILLA_KEY,
        AWAIT_VANILLA,
        CREATIVE,
        CREATIVE_OPEN,
        CREATIVE_TAB,
        CREATIVE_OURS,
        CREATIVE_ITEMS,
        CREATIVE_CARRIED,
        CREATIVE_KEY,
        CREATIVE_REOPENED,
        CREATIVE_VANILLA,
        CREATIVE_BACK,
        REOPEN,
        AWAIT_REOPEN,
        FURNACE_GIVE,
        FURNACE_EQUIP,
        FURNACE_OPEN,
        FURNACE_FEED,
        FURNACE_LIT,
        FURNACE_DONE,
        RULES_OPEN,
        RULES_CHECK,
        RULES_TAB,
        RULES_BACK,
        KILL,
        AWAIT_DEATH,
        AWAIT_RESPAWN,
        AWAIT_PICKUP,
        DONE
    }
}
