package org.jmc;

import org.jmc.models.BlockModel;
import org.jmc.models.None;
import org.jmc.registry.NamespaceID;


/**
 * A special-purpose implementation of BlockInfo used to represent unknown block types.
 * The behavior of this class varies according to the "renderUnknown" global option.
 */
public class NullBlockInfo : BlockInfo(NamespaceID.NULL, "unknown", null, Occlusion.NONE, null, false, null)
{
	private val noneModel: BlockModel;

	init {
		noneModel = None();
	}

	override fun getOcclusion(): Occlusion {
		return Occlusion.NONE; 
	}

	override fun getModel(): BlockModel {
		return noneModel; 
	}

}
