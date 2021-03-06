package com.onaple.itemizer.service;

import com.onaple.itemizer.data.access.ItemDAO;
import com.onaple.itemizer.data.beans.ItemBean;
import com.onaple.itemizer.data.beans.ItemLoreWriter;
import com.onaple.itemizer.utils.ItemBuilder;
import com.onaple.itemizer.utils.PoolFetcher;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.item.inventory.ItemStack;

import javax.inject.Singleton;
import java.util.*;

@Singleton
public class ItemService implements IItemService {

    public ItemService() {
    }


    private Map<Key, ItemLoreWriter> customLoreAppenders = new HashMap<>();

    public Set<ItemLoreWriter> getItemLoreAppenders(Set<Key> keys) {
        Set<ItemLoreWriter> writers = new TreeSet<>();
        for (Map.Entry<Key, ItemLoreWriter> entry : customLoreAppenders.entrySet()) {
            if (keys.contains(entry.getKey())) {
                writers.add(entry.getValue());
            }
        }
        return writers;
    }

    @Override
    public void addItemLoreAppender(ItemLoreWriter writer) {
        writer.getKeys().stream().forEach(a -> customLoreAppenders.put(a, writer));
    }

    @Override
    public Optional<ItemStack> fetch(String id) {
        Optional<ItemBean> optionalItem = PoolFetcher.fetchItemFromPool(id);
        if (optionalItem.isPresent()) {
            return new ItemBuilder().buildItemStack(optionalItem.get());
        }
        return Optional.empty();
    }

    @Override
    public Optional<ItemStack> retrieve(String id) {
        Optional<ItemBean> optionalItem = ItemDAO.getItem(id);
        if (optionalItem.isPresent()) {
            return new ItemBuilder().buildItemStack(optionalItem.get());
        }
        return Optional.empty();

    }
}
