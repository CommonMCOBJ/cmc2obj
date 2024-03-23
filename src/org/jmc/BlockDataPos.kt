package org.jmc;

import org.jmc.geom.BlockPos;
import org.jmc.registry.NamespaceID;

class BlockDataPos(@JvmField val pos: BlockPos, @JvmField val data: BlockData, @JvmField val biome: NamespaceID){
	override fun toString(): String {
		return String.format("%s = %s; biome = %s", pos.toString(), data.toString(), biome.toString());
	}
}
