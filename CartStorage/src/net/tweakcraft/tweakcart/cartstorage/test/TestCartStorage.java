package net.tweakcraft.tweakcart.cartstorage.test;

import java.util.ArrayList;

import net.minecraft.server.EntityMinecart;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldServer;
import net.tweakcraft.tweakcart.api.event.TweakVehiclePassesSignEvent;
import net.tweakcraft.tweakcart.cartstorage.CartStorage;
import net.tweakcraft.tweakcart.model.Direction;
import net.tweakcraft.tweakcart.test.TweakCartInventoryTest;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.block.CraftChest;
import org.bukkit.craftbukkit.block.CraftSign;
import org.bukkit.craftbukkit.entity.CraftStorageMinecart;
import org.bukkit.entity.Minecart;
import org.bukkit.inventory.ItemStack;

public class TestCartStorage {
    private CraftSign sign;
    private CraftChest chest;
    private CraftStorageMinecart storageCart;
    private CartStorage.CartStorageEventListener eventListener;

    public class MalformedInvContentException extends Exception {
        private String error;

        public MalformedInvContentException(String error) {
            this.error = error;
        }

        public String getError() {
            return this.error;
        }
    }

    public TestCartStorage(CraftServer craftServer, CartStorage.CartStorageEventListener eventListener) {
        MinecraftServer server = craftServer.getServer();
        WorldServer world = server.getWorldServer(0);
        CraftWorld craftWorld = world.getWorld();

        EntityMinecart cart = new EntityMinecart(world, 1d, 1d, 1d, 0);
        storageCart = new CraftStorageMinecart(craftServer, cart);

        /*
           * init block
           */
        craftWorld.getBlockAt(1, 1, 1).setType(Material.SIGN_POST);
        craftWorld.getBlockAt(1, 2, 1).setType(Material.CHEST);

        /*
           * sign
           */
        Block signBlock = craftWorld.getBlockAt(1, 1, 1);
        sign = new CraftSign(signBlock);

        /*
           * chest
           */
        Block chestBlock = craftWorld.getBlockAt(1, 2, 1);
        chest = new CraftChest(chestBlock);

        this.eventListener = eventListener;
    }

    public void testAll() {
        try {
            System.out.println(this.testCase(
                "collect items|all items",
                "7:64|5:64||7:64;5:64"
            ));
        } catch (MalformedInvContentException MIE) {
            System.out.println("Malformed case:" + MIE.getError());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean testCase(String caseSign, String... content) throws Exception {
        String[] invs;
        ItemStack[][] invStacks = new ItemStack[4][];
        if (content.length == 1) {
            invs = content[0].split("\\|");
        } else {
            invs = content;
        }
        if (invs.length != 4) {
            throw new MalformedInvContentException("need 4 invs");
        }
        for (int s = 0; s < 4; s++) {
            ArrayList<ItemStack> list = new ArrayList<ItemStack>();
            if (!invs[s].isEmpty()) {
                String[] stacks = invs[s].split("\\;");
                for (int x = 0; x < stacks.length; x++) {
                    String[] stackVal = stacks[x].split("\\:");
                    if (stackVal.length != 2) {
                        throw new MalformedInvContentException("some stacks hasn't got an amount");
                    }
                    int amount = Integer.parseInt(stackVal[1]);

                    while (amount > 0) {
                        if (amount > 64) {
                            list.add(new ItemStack(Integer.parseInt(stackVal[0]), 64));
                        } else {
                            list.add(new ItemStack(Integer.parseInt(stackVal[0]), amount));

                        }
                        amount -= 64;
                    }
                }
            }
            invStacks[s] = list.toArray(new ItemStack[list.size()]);
            if (invStacks[s] == null) {
                invStacks[s] = new ItemStack[0];
            }
        }
        return this.testCase(
            "collect items|all items",
            invStacks[0],
            invStacks[1],
            invStacks[2],
            invStacks[3]
        );
    }

    public boolean testCase(
        String signContent,
        ItemStack[] cartInv,
        ItemStack[] chestInv,
        ItemStack[] resCartInv,
        ItemStack[] resChestInv) {
        String[] signLines = signContent.split("\\|");
        for (int l = 0; l < 4; l++) {
            sign.setLine(l, (l < signLines.length) ? signLines[l] : "");
        }
        storageCart.getInventory().clear();
        storageCart.getInventory().addItem(cartInv);
        chest.getInventory().clear();
        chest.getInventory().addItem(chestInv);

        TweakVehiclePassesSignEvent event = new TweakVehiclePassesSignEvent(
            (Minecart) storageCart,
            Direction.NORTH,
            sign,
            "collect items");

        eventListener.onSignPass(event);

        ItemStack[] cartStacks = storageCart.getInventory().getContents();
        ItemStack[] chestStacks = chest.getInventory().getContents();

        TweakCartInventoryTest invTest = new TweakCartInventoryTest();
        return (invTest.compareInventories(cartStacks, resCartInv) && invTest.compareInventories(chestStacks, resChestInv));
    }
}