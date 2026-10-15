package dev.rsadvanced.feature.mining;

import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.energy.EnergyStorage;
import com.refinedmods.refinedstorage.api.network.impl.energy.EnergyStorageImpl;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.support.energy.AbstractEnergyItem;
import com.refinedmods.refinedstorage.common.api.support.slotreference.SlotReference;
import com.refinedmods.refinedstorage.common.api.support.slotreference.SlotReferenceFactory;
import com.refinedmods.refinedstorage.common.api.support.slotreference.SlotReferenceHandlerItem;
import dev.architectury.registry.menu.MenuRegistry;
import dev.rsadvanced.RSAdvanced;
import dev.rsadvanced.content.AdvancedComponents;
import dev.rsadvanced.feature.connection.NetworkCardSlot;
import dev.rsadvanced.mixin.MiningBlockStateAccessor;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** RS owns the energy component and charging adapters; vanilla owns mining and enchantments. */
public class MiningItem extends AbstractEnergyItem implements SlotReferenceHandlerItem {
    public static final long CAPACITY = 400_000;
    public static final long BLOCK_COST = 80;
    private static final String MODE_KEY = "rsadvanced_mining_mode";

    public MiningItem() {
        super(new Properties().stacksTo(1).component(DataComponents.TOOL, createMultitoolProperties()),
                RefinedStorageApi.INSTANCE.getEnergyItemHelper());
    }

    public static final TagKey<Block> INCORRECT =
            TagKey.create(Registries.BLOCK,
                    RSAdvanced.id("incorrect_for_mining_tool"));

    private static Tool createMultitoolProperties() {
        return new Tool(List.of(Tool.Rule.deniesDrops(INCORRECT)), 1, 0);
    }

    public static MiningMode mode(ItemStack stack) {
        MiningMode stored = stack.getOrDefault(AdvancedComponents.MINING_MODE.get(), MiningMode.NORMAL);
        return stored == MiningMode.VEIN ? MiningMode.NORMAL : stored;
    }

    public static void setMode(ItemStack stack, MiningMode mode) {
        boolean vein = veinMining(stack) || mode == MiningMode.VEIN;
        stack.set(AdvancedComponents.MINING_MODE.get(), mode == MiningMode.VEIN ? MiningMode.NORMAL : mode);
        stack.set(AdvancedComponents.MINING_VEIN.get(), vein);
    }

    public static boolean veinMining(ItemStack stack) {
        boolean legacyVein = stack.getOrDefault(AdvancedComponents.MINING_MODE.get(), MiningMode.NORMAL) == MiningMode.VEIN;
        return stack.getOrDefault(AdvancedComponents.MINING_VEIN.get(), legacyVein);
    }

    public static void setVeinMining(ItemStack stack, boolean enabled) {
        MiningMode speed = mode(stack);
        stack.set(AdvancedComponents.MINING_MODE.get(), speed);
        stack.set(AdvancedComponents.MINING_VEIN.get(), enabled);
    }

    public static void prepare(ItemStack stack) {
        // Migrate the former exclusive Vein mode without altering the area, energy or card.
        setMode(stack, mode(stack));
        // Existing enchantments are explicitly removed on first server use, without changing energy or the card.
        stack.remove(DataComponents.ENCHANTMENTS);
        stack.remove(DataComponents.STORED_ENCHANTMENTS);
    }

    public static long energy(ItemStack stack) {
        return stack.getOrDefault(com.refinedmods.refinedstorage.common.content.DataComponents.INSTANCE.getEnergy(), 0L);
    }

    public EnergyStorage createEnergyStorage(ItemStack stack) {
        EnergyStorage delegate = RefinedStorageApi.INSTANCE.asItemEnergyStorage(
                new EnergyStorageImpl(MiningBalance.settings().capacity()), stack);
        return new EnergyStorage() {
            @Override
            public long getStored() {
                return delegate.getStored();
            }

            @Override
            public long getCapacity() {
                return delegate.getCapacity();
            }

            @Override
            public long receive(long amount, Action action) {
                return delegate.receive(Math.min(amount, MiningBalance.settings().chargeRate()), action);
            }

            @Override
            public long extract(long amount, Action action) {
                return delegate.extract(amount, action);
            }
        };
    }

    @Override
    public ItemStack createAtEnergyCapacity() {
        ItemStack stack = new ItemStack(this);
        stack.set(com.refinedmods.refinedstorage.common.content.DataComponents.INSTANCE.getEnergy(),
                MiningBalance.settings().capacity());
        return stack;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return !state.is(INCORRECT);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        MiningMode mode = mode(stack);
        long required = MiningBalance.cost(mode,
                ((MiningBlockStateAccessor) state).rsadvanced$rawDestroySpeed());
        if (!RefinedStorageApi.INSTANCE.isEnergyRequired()) {
            return mode.speed();
        }
        return MiningBalance.speed(mode, energy(stack), required);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, net.minecraft.world.entity.LivingEntity target,
                             net.minecraft.world.entity.LivingEntity attacker) {
        if (!attacker.level().isClientSide()) {
            prepare(stack);
            if (RefinedStorageApi.INSTANCE.isEnergyRequired()) {
                createEnergyStorage(stack).extract(MiningBalance.settings().weaponCost(),
                        Action.EXECUTE);
            }
        }
        return true;
    }

    public static int radius(ItemStack stack) {
        return Math.clamp(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getInt(MODE_KEY), 0, 2);
    }

    public static void setRadius(ItemStack stack, int radius) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(MODE_KEY, Math.clamp(radius, 0, 2)));
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
        prepare(stack);
        MenuRegistry.openExtendedMenu(player, new SimpleMenuProvider(
                (id, inventory, owner) -> new MiningCardMenu(id, inventory, reference),
                Component.translatable("gui.rsadvanced.mining_card")),
                buffer -> SlotReferenceFactory.STREAM_CODEC.encode(
                        new RegistryFriendlyByteBuf(buffer, player.registryAccess()), reference));
    }

    private static Component modeLabel(ItemStack stack) {
        int width = radius(stack) * 2 + 1;
        return Component.translatable("item.rsadvanced.network_mining_tool.mode", width, width);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        lines.add(Component.translatable("item.rsadvanced.network_mining_tool.speed_mode",
                Component.translatable("gui.rsadvanced.mining_mode." + mode(stack).getSerializedName()), mode(stack).speed()));
        lines.add(modeLabel(stack));
        lines.add(Component.translatable(veinMining(stack)
                ? "gui.rsadvanced.mining_vein_on" : "gui.rsadvanced.mining_vein_off"));
        lines.add(Component.translatable("item.rsadvanced.network_mining_tool.help"));
        ItemStack card = NetworkCardSlot.card(stack);
        if (!card.isEmpty()) {
            card.getItem().appendHoverText(card, context, lines, flag);
        }
    }
}
