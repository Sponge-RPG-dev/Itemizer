package com.onaple.itemizer.utils;

import com.onaple.itemizer.data.beans.MinerBean;
import org.spongepowered.api.block.BlockType;

import java.util.*;

public class MinerUtil {
    /** Map of miners **/
    private Map<String, MinerBean> miners = new HashMap<>();
    /** List of miners keys already processed **/
    private List<String> keysProcessed = new ArrayList<>();

    /**
     * Utility to resolve miners inheritance
     * @param minersList Miners with inherit relations
     */
    public MinerUtil(List<MinerBean> minersList) {
        for (MinerBean miner : minersList) {
            miners.put(miner.getId(), miner);
        }
    }

    /**
     * Resolve miners inheritance
     * @return List of exhaustive miners
     */
    public List<MinerBean> getExpandedMiners() {
        for (Map.Entry<String, MinerBean> miner : miners.entrySet()) {
            resolveDependencies(miner.getKey());
        }
        return new ArrayList<>(miners.values());
    }

    /**
     * Resolve dependencies of a miner, recursively
     * @param minerKey Key of the miner to expand
     */
    private void resolveDependencies(String minerKey) {
        keysProcessed.add(minerKey);
        MinerBean miner = miners.get(minerKey);
        if (miner.getInheritances() != null) {
            for (String inheritKey : miner.getInheritances()) {
                if (!keysProcessed.contains(inheritKey)) {
                    resolveDependencies(inheritKey);
                }
                Map<String,BlockType> inheritValues = new HashMap<>();
                inheritValues.putAll(miners.get(inheritKey).getMineTypes());
                miner.setMineTypes(inheritValues);
            }
        }
        miners.put(minerKey, miner);
    }
}
