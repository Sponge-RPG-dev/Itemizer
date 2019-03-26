package com.onaple.itemizer.service;

import com.onaple.itemizer.data.beans.ItemNbtFactory;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.spongepowered.api.item.inventory.ItemStack;

@ConfigSerializable
public interface ItemNBTModule {

    String getKeyId();

    ItemNbtFactory build(ConfigurationNode node);

    void apply(ItemStack itemStack);
}
