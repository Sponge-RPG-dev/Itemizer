package com.onaple.itemizer.utils;

import com.onaple.itemizer.GlobalConfig;
import com.onaple.itemizer.Itemizer;
import com.onaple.itemizer.data.beans.*;

import com.onaple.itemizer.service.IItemService;
import com.onaple.itemizer.service.ItemService;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.trait.BlockTrait;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.item.BreakableData;
import org.spongepowered.api.data.manipulator.mutable.item.EnchantmentData;
import org.spongepowered.api.data.type.SkullTypes;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.enchantment.Enchantment;
import org.spongepowered.api.item.enchantment.EnchantmentType;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.*;

import javax.inject.Singleton;
import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class ItemBuilder {

    private GlobalConfig config = Itemizer.getItemizer().getGlobalConfig();
    private ItemStack item;
    private List<Text> lore;
    private Set<Key> usedKeys = new HashSet<>();

    public ItemBuilder() {
        lore = new ArrayList<>();
    }


    /**
     * Build an itemstack from an ItemBean
     * @param itemBean Data of the item to build
     * @return Optional of the itemstack
     */
    public Optional<ItemStack> buildItemStack(ItemBean itemBean) {
        Optional<ItemType> optionalType = Sponge.getRegistry().getType(ItemType.class, itemBean.getType());
        if (optionalType.isPresent()) {
            Optional<BlockType> potentialBlock = optionalType.get().getBlock();
            if (potentialBlock.isPresent()) {
               BlockState blockState = addTraits(potentialBlock.get(),itemBean.getBlockTrait());
             this.item = ItemStack.builder().fromBlockState(blockState).build();
            } else {
                this.item = ItemStack.builder().itemType(optionalType.get()).build();
            }
               defineItemStack(itemBean,config.getHiddenFlags().get("Unbreakable"));
               enchantItemStack(itemBean,config.getHiddenFlags().get("Enchantments"));
               grantMining(itemBean,config.getHiddenFlags().get("CanDestroy"));
               setAttribute(itemBean,config.getHiddenFlags().get("Attributes_modifiers"));
               setNbt(itemBean);
               setCustomDatamanipulators(itemBean);
            Itemizer.getLogger().info("Hide flag value : "+config.getHiddenFlagsValue());
                this.item = ItemStack.builder()
                        .fromContainer(item.toContainer().set(DataQuery.of("UnsafeData","HideFlags"),config.getHiddenFlagsValue()))
                        .build();
                addLore();
           /* } else{
                if (itemBean.getLore() != null) {
                    List<Text> loreData = new ArrayList<>();
                    for (String loreLine : itemBean.getLore().split("\n")) {
                        loreData.add(Text.builder(loreLine).color(TextColors.GRAY).build());
                    }

                    Set<ItemLoreWriter> itemLoreAppenders = ItemService.INSTANCE.getItemLoreAppenders(usedKeys);
                    for (ItemLoreWriter itemLoreAppender : itemLoreAppenders) {
                        itemLoreAppender.apply(item, loreData);
                    }
                    item.offer(Keys.ITEM_LORE, loreData);
                }

            }*/

            return Optional.ofNullable(this.item);
        } else {
            Itemizer.getLogger().warn("Unknown item type : " + itemBean.getType());
        }
        return Optional.empty();
    }

    private void setCustomDatamanipulators(ItemBean itemBean) {
        List<IItemBeanConfiguration> thirdpartyConfigs = itemBean.getThirdpartyConfigs();
        for (IItemBeanConfiguration cfg : thirdpartyConfigs) {
            cfg.apply(item);
            usedKeys.add(cfg.getKey());
        }
    }


    /**
     * Build an itemstack from this name
     * @param name Data of the item to build
     * @return Optional of the itemstack
     */
    public Optional<ItemStack> buildItemStack(String name) {
        Optional<ItemType> optionalType = Sponge.getRegistry().getType(ItemType.class,name);
        if (optionalType.isPresent()) {
            ItemStack itemStack = ItemStack.builder().itemType(optionalType.get()).build();
            return Optional.of(itemStack);
        } else {
            Itemizer.getLogger().warn("Unknown item type : " + name);
        }
        return Optional.empty();
    }

    /**
     * Define the characteristics of an ItemStack from an ItemBean
     * @param itemBean Data of the item to define
     * @return ItemStack edited
     */
    private void defineItemStack(ItemBean itemBean,boolean rewrite) {
        //item Id
        if (itemBean.getId() != null && !itemBean.getId().isEmpty()) {
            setCustomData("id",itemBean.getId());
        }

        // Item name
        if (itemBean.getName() != null && !itemBean.getName().isEmpty()) {
            item.offer(Keys.DISPLAY_NAME, Text.builder(itemBean.getName()).style(TextStyles.BOLD).build());
        }
        // Item lore
        if (itemBean.getLore() != null) {

            for (String loreLine : itemBean.getLore().split("\n")) {
                lore.add(Text.builder(loreLine).color(TextColors.GRAY).build());
            }

        }

        // Item attributes
        item.offer(Keys.UNBREAKABLE, itemBean.isUnbreakable());
        if(itemBean.isUnbreakable()) {
            if(rewrite && config.getUnbreakableRewrite() != null) {

                lore.add(Text.builder(config.getUnbreakableRewrite()).color(TextColors.DARK_GRAY).style(TextStyles.ITALIC).build());
            }
        }
        if(itemBean.getDurability() > 0){
            item.offer(Keys.ITEM_DURABILITY, itemBean.getDurability());
        }


        if(itemBean.getToolLevel() !=0) {
            DataContainer container = this.item.toContainer();
            container.set(DataQuery.of("UnsafeData", "ToolLevel"), itemBean.getToolLevel());
            this.item = ItemStack.builder().fromContainer(container).build();
        }

    }

    /**
     * Enchant an ItemStack with an ItemBean data
     * @param itemBean Data of the item to enchant
     * @return Enchanted (or not) ItemStack
     */
    private void enchantItemStack(ItemBean itemBean, boolean rewrite) {
        Map<String, Integer> enchants = itemBean.getEnchants();
        if (!enchants.isEmpty()) {
            EnchantmentData enchantmentData = item.getOrCreate(EnchantmentData.class).get();
            for (Map.Entry<String, Integer> enchant : enchants.entrySet()) {
                Optional<EnchantmentType> optionalEnchant = Sponge.getRegistry().getType(EnchantmentType.class, enchant.getKey());
                if (optionalEnchant.isPresent()) {
                    enchantmentData.set(enchantmentData.enchantments().add(Enchantment.builder().
                            type(optionalEnchant.get()).
                            level(enchant.getValue()).build()));
                    if(rewrite) {
                        if(config.getEnchantRewrite().size()>0) {
                            Itemizer.getLogger().info(config.getEnchantRewrite().get( optionalEnchant.get()));
                            lore.add(Text
                                    .builder(config.getEnchantRewrite().get( optionalEnchant.get())+ " " + enchant.getValue())
                                    .style(TextStyles.ITALIC)
                                    .color(TextColors.DARK_PURPLE)
                                    .build());
                        }
                    }
                } else {
                    Itemizer.getLogger().warn("Unknown enchant : " + enchant.getKey());
                }
            }

                item.offer(enchantmentData);

        }
    }

    /**
     * Grant mining capabilities
     * @param itemBean Data of the item
     * @return Item with mining powers
     */
    private void grantMining(ItemBean itemBean,boolean rewrite) {
        BreakableData breakableData = item.getOrCreate(BreakableData.class).get();
        List<MinerBean> minerList = Itemizer.getConfigurationHandler().getMinerList();
        List<String> minerNames = new ArrayList<>();
        if(!itemBean.getMiners().isEmpty()) {
            Text.Builder miningText = Text.builder("Can mine :").color(TextColors.BLUE).style(TextStyles.UNDERLINE);
            for (String minerId : itemBean.getMiners()) {
                for (MinerBean minerBean : minerList) {
                    if (minerBean.getId().equals(minerId)) {
                        minerBean.getMineTypes().forEach((blockName, blockType) -> {
                            miningText.append(Text.builder(" " + blockName + " ").color(TextColors.BLUE).style(TextStyles.RESET).build());
                            Optional<BlockType> optionalBlockType = Sponge.getRegistry().getType(BlockType.class, blockType);
                            optionalBlockType.ifPresent(blockType1 -> breakableData.set(breakableData.breakable().add(blockType1)));
                        });

                    }
                }
            }
            if(rewrite) {
                lore.add(miningText.build());
            }
        }
        item.offer(breakableData);
    }

    /**
     * Set attributes to an item
     * @param itemBean Data of the item
     * @return Item with attributes set
     */
    private void setAttribute(ItemBean itemBean,Boolean rewrite){
        List<DataContainer> containers = new ArrayList();
        Text.Builder attributeTextbuilder = Text.builder();

        for(AttributeBean att : itemBean.getAttributeList()){
            DataContainer dc = createAttributeModifier(att);
            containers.add(dc);
            Text.Builder attributText;
            if(att.getOperation()==0){
                attributText = Text.builder(String.format("%.1f", att.getAmount())+" ");

            } else if(att.getOperation()==1) {

                attributText =Text.builder( String.format("%.1f", att.getAmount()*100)+ "% ");
            } else {
                attributText =Text.builder(String.format("%.1f", att.getAmount()*100)+ "% ");
            }
            String name = config.getModifierRewrite().get(att.getName());
            if(name == null){
                name = att.getName();
            }
            attributText.append(Text.builder(name).build());
            if(att.getAmount()>0){
                attributText.color(TextColors.GREEN);
            } else {
                attributText.color(TextColors.RED);
            }
            if(rewrite) {
                lore.add(attributText.build());
            }
        }

        DataContainer container = this.item.toContainer();
        container.set(DataQuery.of("UnsafeData","AttributeModifiers"),containers);
    }


    /**
     * Create the datacontainer for an attribute's data
     * @param attribute Data of the attribute
     * @return DataContainer from which the item will be recreated
     */
    private DataContainer createAttributeModifier(AttributeBean attribute){
        UUID uuid = UUID.randomUUID();
        DataContainer dataContainer = DataContainer.createNew();
        dataContainer.set(DataQuery.of("AttributeName"),attribute.getName());
        dataContainer.set(DataQuery.of("Name"),attribute.getName());
        dataContainer.set(DataQuery.of("Amount"),attribute.getAmount());
        dataContainer.set(DataQuery.of("Operation"),attribute.getOperation());
        dataContainer.set(DataQuery.of("Slot"),attribute.getSlot());
        dataContainer.set(DataQuery.of("UUIDMost"),uuid.getMostSignificantBits());
        dataContainer.set(DataQuery.of("UUIDLeast"),uuid.getLeastSignificantBits());
        return dataContainer;
    }

    private void addLore() {
        item.offer(Keys.ITEM_LORE,lore);
    }

    private void setNbt(ItemBean itemBean){
        for (Map.Entry<String,Object> nbt: itemBean.getNbtList().entrySet()
             ) {
            setCustomData(nbt.getKey(),nbt.getValue());
        }
    }
    private void setCustomData(String queryPath,Object value){
        List<String> queryList ;
        if(queryPath.contains(".")) {
            String[] queries = queryPath.split(".");
            Itemizer.getLogger().info(("length" + queries.length));
            queryList = Arrays.stream(queries).collect(Collectors.toList());
        }
        else {
            queryList = new ArrayList<>();
            queryList.add(queryPath);
        }
        queryList.add(0,"UnsafeData");
        DataQuery dt = DataQuery.of(queryList);
        this.item = ItemStack.builder()
                .fromContainer(item.toContainer().set(dt,value))
                .build();
    }
    /**
     * Add block traits to a future block
     * @param blockType Type of the block
     * @param traits Map containing all the traits
     * @return BlockState of the future block
     */
    private static BlockState addTraits(BlockType blockType, Map<String, String> traits) {
        BlockState blockState = blockType.getDefaultState();
        for (Map.Entry<String, String> trait : traits.entrySet()) {
            Optional<BlockTrait<?>> optTrait = blockState.getTrait(trait.getKey());
            if (optTrait.isPresent()) {
                Optional<BlockState> newBlockState = blockState.withTrait(optTrait.get(), trait.getValue());
                if (newBlockState.isPresent()) {
                    blockState = newBlockState.get();
                }
            }
        }
        return blockState;
    }
}
