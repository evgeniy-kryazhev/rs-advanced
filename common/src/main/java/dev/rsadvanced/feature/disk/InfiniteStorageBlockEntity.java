package dev.rsadvanced.feature.disk;

import com.refinedmods.refinedstorage.api.network.impl.node.AbstractStorageContainerNetworkNode;
import com.refinedmods.refinedstorage.api.network.impl.node.storage.StorageNetworkNode;
import com.refinedmods.refinedstorage.api.storage.Storage;
import com.refinedmods.refinedstorage.common.Platform;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.storage.StorageBlockData;
import com.refinedmods.refinedstorage.common.content.Menus;
import com.refinedmods.refinedstorage.common.storage.StorageConfigurationContainerImpl;
import com.refinedmods.refinedstorage.common.support.FilterWithFuzzyMode;
import com.refinedmods.refinedstorage.common.support.containermenu.NetworkNodeExtendedMenuProvider;
import com.refinedmods.refinedstorage.common.support.network.AbstractBaseNetworkNodeContainerBlockEntity;
import com.refinedmods.refinedstorage.common.support.resource.ResourceContainerData;
import com.refinedmods.refinedstorage.common.support.resource.ResourceContainerImpl;
import dev.rsadvanced.content.AdvancedComponents;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamEncoder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

public final class InfiniteStorageBlockEntity extends AbstractBaseNetworkNodeContainerBlockEntity<StorageNetworkNode>
        implements NetworkNodeExtendedMenuProvider<StorageBlockData>, AbstractStorageContainerNetworkNode.Provider {
    private static final String DEFINITION_TAG = "cell_definition";

    private final DiskResourceKind kind;
    private final FilterWithFuzzyMode filter;
    private final StorageConfigurationContainerImpl configuration;
    private ResourceLocation definitionId;
    private InfiniteResourceStorage storage;

    public InfiniteStorageBlockEntity(BlockPos position, BlockState state) {
        super(InfiniteStorageBlockContent.ENTITY.get(), position, state,
                new StorageNetworkNode(energyUsage(state), 0, 1));
        this.kind = ((InfiniteStorageBlock) state.getBlock()).kind();
        var resourceFactory = kind == DiskResourceKind.ITEM
                ? RefinedStorageApi.INSTANCE.getItemResourceFactory()
                : RefinedStorageApi.INSTANCE.getFluidResourceFactory();
        this.filter = FilterWithFuzzyMode.createAndListenForUniqueFilters(
                ResourceContainerImpl.createForFilter(resourceFactory), this::setChanged,
                resources -> mainNetworkNode.getStorageConfiguration().setFilters(resources));
        this.configuration = new StorageConfigurationContainerImpl(mainNetworkNode.getStorageConfiguration(), filter,
                this::setChanged, this::getRedstoneMode, this::setRedstoneMode);
        mainNetworkNode.getStorageConfiguration().setNormalizer(filter.createNormalizer());
        mainNetworkNode.setProvider(this);
    }

    private static long energyUsage(BlockState state) {
        DiskResourceKind kind = ((InfiniteStorageBlock) state.getBlock()).kind();
        return kind == DiskResourceKind.ITEM
                ? Platform.INSTANCE.getConfig().getStorageBlock().getCreativeEnergyUsage()
                : Platform.INSTANCE.getConfig().getFluidStorageBlock().getCreativeEnergyUsage();
    }

    public void setDefinition(ResourceLocation definitionId) {
        this.definitionId = definitionId;
        // Keep only the catalog identity. Infinite sources never allocate a repository storage UUID.
        this.storage = CellDefinitions.serverDefinition(definitionId)
                .filter(definition -> definition.kind() == kind)
                .map(definition -> InfiniteStorageType.forKind(kind).create(definitionId, definition))
                .orElse(null);
        mainNetworkNode.onStorageChanged(0);
        setChanged();
    }

    public ResourceLocation definitionId() {
        return definitionId;
    }

    public StorageNetworkNode node() {
        return mainNetworkNode;
    }

    @Override
    public Optional<Storage> resolve(int index) {
        if (index != 0) {
            return Optional.empty();
        }
        return Optional.ofNullable(storage);
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (definitionId != null) {
            tag.putString(DEFINITION_TAG, definitionId.toString());
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        setDefinition(ResourceLocation.tryParse(tag.getString(DEFINITION_TAG)));
    }

    @Override
    public void writeConfiguration(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeConfiguration(tag, registries);
        configuration.save(tag);
        filter.save(tag, registries);
    }

    @Override
    public void readConfiguration(CompoundTag tag, HolderLookup.Provider registries) {
        super.readConfiguration(tag, registries);
        configuration.load(tag);
        filter.load(tag, registries);
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput components) {
        super.applyImplicitComponents(components);
        setDefinition(components.get(AdvancedComponents.CELL_DEFINITION.get()));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (definitionId != null) {
            components.set(AdvancedComponents.CELL_DEFINITION.get(), definitionId);
        }
    }

    @Override
    public Component getName() {
        return overrideName(CellDefinitions.displayDefinition(definitionId)
                .filter(definition -> definition.kind() == kind)
                .map(definition -> Component.translatable("block.rsadvanced.infinite_storage_block",
                        CellResourceNames.name(definition.resourceKey())))
                .orElseGet(() -> Component.translatable("block.rsadvanced.unknown_infinite_storage_block")));
    }

    @Override
    public StorageBlockData getMenuData() {
        return new StorageBlockData(0, 0, ResourceContainerData.of(filter.getFilterContainer()).resources());
    }

    @Override
    public StreamEncoder<RegistryFriendlyByteBuf, StorageBlockData> getMenuCodec() {
        return RefinedStorageApi.INSTANCE.getStorageBlockDataStreamCodec();
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inventory, Player player) {
        var menuType = kind == DiskResourceKind.ITEM ? Menus.INSTANCE.getItemStorage() : Menus.INSTANCE.getFluidStorage();
        var menu = new InfiniteStorageBlockMenu(menuType, syncId, player, filter.getFilterContainer(), configuration,
                candidate -> Container.stillValidBlockEntity(this, candidate)
                        && candidate instanceof ServerPlayer serverPlayer && canOpen(serverPlayer),
                () -> player instanceof ServerPlayer serverPlayer
                        && canOpen(serverPlayer) && containers.canBuild(serverPlayer));
        menu.rsadvanced$setInfiniteResources(storage == null ? java.util.Set.of()
                : storage.rsadvanced$getInfiniteResources());
        return menu;
    }
}
