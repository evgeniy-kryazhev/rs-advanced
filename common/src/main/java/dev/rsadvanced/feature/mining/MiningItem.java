package dev.rsadvanced.feature.mining;

import com.refinedmods.refinedstorage.api.network.energy.EnergyStorage;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.support.energy.AbstractEnergyItem;
import com.refinedmods.refinedstorage.common.api.support.slotreference.SlotReference;
import com.refinedmods.refinedstorage.common.api.support.slotreference.SlotReferenceHandlerItem;
import dev.rsadvanced.feature.mining.module.HarvestToolModule;
import dev.rsadvanced.feature.mining.module.ToolModule;
import dev.rsadvanced.feature.mining.module.ToolModuleContainer;
import dev.rsadvanced.feature.mining.module.ToolModules;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Minecraft and RS entry point. Installed modules provide behavior and own their stack settings. */
public class MiningItem extends AbstractEnergyItem implements SlotReferenceHandlerItem {
    public static final long CAPACITY = 400_000;
    public static final long BLOCK_COST = 80;
    public static final TagKey<Block> INCORRECT = HarvestToolModule.INCORRECT;
    private final List<ToolModule> installedModules;

    public MiningItem() {
        this(ToolModules.BUILT_IN);
    }

    protected MiningItem(List<ToolModule> installedModules) {
        // Use vanilla diamond-pickaxe attributes; energy powers mining only.
        super(new Properties().stacksTo(1)
                        .component(DataComponents.TOOL, ToolModules.HARVEST.properties())
                        .attributes(PickaxeItem.createAttributes(Tiers.DIAMOND, 1.0f, -2.8f)),
                RefinedStorageApi.INSTANCE.getEnergyItemHelper());
        this.installedModules = List.copyOf(installedModules);
    }

    public static ToolModuleContainer modules(ItemStack stack) {
        if (!(stack.getItem() instanceof MiningItem item)) {
            throw new IllegalArgumentException("Stack is not a network mining tool");
        }
        return new ToolModuleContainer(stack, item.installedModules);
    }

    // Compatibility accessors keep existing integrations and saved items using module-owned settings.
    public static MiningMode mode(ItemStack stack) {
        return modules(stack).require(ToolModules.SPEED).mode(stack);
    }

    public static void setMode(ItemStack stack, MiningMode mode) {
        modules(stack).require(ToolModules.SPEED).set(stack, mode);
    }

    public static boolean veinMining(ItemStack stack) {
        return modules(stack).require(ToolModules.VEIN).selected(stack);
    }

    public static void setVeinMining(ItemStack stack, boolean enabled) {
        modules(stack).require(ToolModules.VEIN).set(stack, enabled);
    }

    public static int radius(ItemStack stack) {
        return modules(stack).require(ToolModules.AREA).radius(stack);
    }

    public static void setRadius(ItemStack stack, int radius) {
        modules(stack).require(ToolModules.AREA).set(stack, radius);
    }

    public static void prepare(ItemStack stack) {
        modules(stack).prepare();
    }

    public static long energy(ItemStack stack) {
        return modules(stack).require(ToolModules.ENERGY).stored(stack);
    }

    public EnergyStorage createEnergyStorage(ItemStack stack) {
        return modules(stack).require(ToolModules.ENERGY).storage(stack);
    }

    @Override
    public ItemStack createAtEnergyCapacity() {
        ItemStack stack = new ItemStack(this);
        modules(stack).require(ToolModules.ENERGY).fill(stack);
        return stack;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return modules(stack).correctForDrops(state);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return modules(stack).miningSpeed(state);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public int getEnchantmentValue() {
        return 0;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            use(serverPlayer, stack, RefinedStorageApi.INSTANCE.createInventorySlotReference(player, hand));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() == null) {
            return InteractionResult.PASS;
        }
        return use(context.getLevel(), context.getPlayer(), context.getHand()).getResult();
    }

    @Override
    public void use(ServerPlayer player, ItemStack stack, SlotReference reference) {
        ToolModuleContainer modules = modules(stack);
        modules.prepare();
        modules.use(player, reference);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        modules(stack).appendTooltip(context, lines, flag);
    }
}
