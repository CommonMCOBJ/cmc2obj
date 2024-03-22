package org.jmc;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import kotlin.collections.List;
import java.util.Locale;
import kotlin.collections.Set;

import org.jmc.registry.NamespaceID;


/**
 * Holds the global options for the program.
 * 
 * Some options are only used in GUI mode or command line mode, but most apply to both.
 */
class Options
{
	enum class UIMode
	{
		GUI,
		CONSOLE
	}

	enum class OffsetType
	{
		NONE,
		CENTER,
		CUSTOM
	}
	
	enum class OverwriteAction
	{
		ASK,
		ALWAYS,
		NEVER
	}
	companion object {
		/**
		 * A list of available locales in the program.
		 */
		@JvmField	
		var availableLocales: Array<Locale> = arrayOf(Locale.ENGLISH, Locale.GERMAN, Locale.CHINESE, Locale("pl"))

		/**
		 * User interface mode.
		 */
		@JvmField
		var uiMode: UIMode = UIMode.GUI
		

		/**
		 * Output directory.
		 */
		@JvmField	
		var outputDir: File = File(".")

		/**
		 * Path to the Minecraft world save directory.
		 */
		@JvmField
		var worldDir: File? = null

		/**
		 * Path to the resource packs to extract.
		 */
		@JvmField
		var resourcePacks: MutableList<File> = Collections.synchronizedList(ArrayList<File>())

		/**
		 * Whether to export the textures.
		 */
		@JvmField
		var exportTex: Boolean = true;

		/**
		 * Scaling to apply to textures.
		 */
		@JvmField
		var textureScale: Double = 1.0
		
		/**
		 * Whether to overwrite existing textures.
		 */
		@JvmField
		var textureOverwrite: Boolean = true
		
		/**
		 * Whether to export the base textures.
		 * Only used in console mode.
		 */
		@JvmField
		var textureDiffuse: Boolean = true

		/**
		 * Export alpha channel as separate file(s).
		 */
		@JvmField
		var textureAlpha: Boolean = false
		
		/**
		 * Whether to attempt exporting normal maps.
		 */
		@JvmField
		var textureNormal: Boolean = false
		
		/**
		 * Whether to attempt exporting specular maps.
		 */
		@JvmField
		var textureSpecular: Boolean = false

		/**
		 * Merge textures into one file.
		 */
		@JvmField
		var textureMerge: Boolean = false

		/**
		 * Export a separate pass for blocks that should emit light. Useful for
		 * renders using GI.
		 */
		@JvmField
		var textureLight: Boolean = false

		/**
		 * Export cloud texture to an OBJ file.
		 */
		@JvmField
		var exportClouds: Boolean = false
		
		/**
		 * Export cloud texture to an OBJ file.
		 */
		@JvmField
		var exportCloudsFile: String = "clouds.obj"
		
		/**
		 * Id of the world dimension to export.
		 */
		@JvmField
		var dimension: Int = 0

		/**
		 * Lower bound of the volume to export.
		 */
		@JvmField var minX: Int = -32
		@JvmField var minY: Int = 0
		@JvmField var minZ: Int = -32;

		/**
		 * Upper bound of the volume to export.
		 */
		@JvmField var maxX: Int = 32
		@JvmField var maxY: Int = 256
		@JvmField var maxZ: Int = 32

		/**
		 * How to scale the exported geometry.
		 */
		@JvmField
		var scale: Float = 1.0f

		/**
		 * How to offset the coordinates of the exported geometry.
		 */
		@JvmField
		var offsetType: OffsetType = OffsetType.NONE

		/**
		 * Custom offset.
		 */
		@JvmField var offsetX: Int = 0
		@JvmField var offsetZ: Int = 0

		/**
		 * If true, will export a separate object for each material.
		 */
		@JvmField
		var objectPerMaterial: Boolean = false

		/**
		 * If false and objectPerMaterial is true, will ignore occlusion rules
		 * and always create faces for adjacent blocks that aren't the same.
		 */
		@JvmField
		var objectPerMaterialOcclusion: Boolean = false

		/**
		 * If true, will export a separate object for each chunk.
		 */
		@JvmField
		var objectPerChunk: Boolean = false

		/**
		 * If true, will export a separate object for each block.
		 */
		@JvmField
		var objectPerBlock: Boolean = false

		/**
		 * If false and objectPerBlock is true, will ignore occlusion rules
		 * and always create faces for adjacent blocks.
		 */
		@JvmField
		var objectPerBlockOcclusion: Boolean = false

		/**
		 * If true, will add extra faces to backside of standalone faces.
		 */
		@JvmField
		var doubleSidedFaces: Boolean = false

		/**
		 * If true, will randomly pick from blockstate models rather than just the first.
		 */
		@JvmField
		var randBlockVariations: Boolean = false
		
		/**
		 * If true, will convert ore blocks to stone.	
		 */
		@JvmField
		var convertOres: Boolean = false
		
		/**
		 * If true, will export with a single material.
		 */
		@JvmField
		var singleMaterial: Boolean = false

		
		/**
		 * If true, will try harder to merge vertices that have the same coordinates.
		 */
		@JvmField
		var removeDuplicates: Boolean = false
		
		/**
		 * If true, will try to merge planar faces and create optimized geometry.
		 */
		@JvmField
		var optimiseGeometry: Boolean = false

		/**
		 * If true, sides and bottom of the model are rendered as well.
		 */
		@JvmField
		var renderSides: Boolean = false

		/**
		 * If true, entities are rendered in the model. 
		 */
		@JvmField
		var renderEntities: Boolean = false

		/**
		 * If true, biomes are taken into account during export.
		 */
		@JvmField
		var renderBiomes: Boolean = true

		/**
		 * If true, includes blocks with unknown block ids in the output. 
		 */
		@JvmField
		var renderUnknown: Boolean = false

		/**
		 * List of block ids to exclude.
		 */
		@JvmField
		var excludeBlocks: Set<NamespaceID> = HashSet<NamespaceID>()
		
		/**
		 * If true, use the excludeBlock set as a whitelist of the only blocks to include
		 */
		@JvmField
		var excludeBlocksIsWhitelist: Boolean = false
		
		/**
		 * Returns true if the block is in the {@link #excludeBlocks excluded blocks} list or if {@link #excludeBlocksIsWhitelist whitelisting} is on,
		 * if the block is not in the {@link #excludeBlocks excluded blocks} list
		 */
		@JvmStatic
		fun isBlockExcluded(block: NamespaceID): Boolean {
			val contains: Boolean = excludeBlocks.contains(block)
			return !excludeBlocksIsWhitelist == contains
		}

		/**
		 * Whether to export the world.
		 * Only used in console mode.
		 */
		@JvmField
		var exportWorld: Boolean = false
		
		/**
		 * If true, will export to the last location and name that was used.
		 */
		@JvmField
		var useLastSaveLoc: Boolean = true

		/**
		 * Whether to overwrite .OBJ files on export.
		 * Only used in GUI mode.
		 */
		@JvmField
		var objOverwriteAction: OverwriteAction = OverwriteAction.ASK

		/**
		 * Whether to overwrite .MTL files on export.
		 * Only used in GUI mode.
		 */
		@JvmField
		var mtlOverwriteAction: OverwriteAction = OverwriteAction.ASK

		/**
		 * Name of .OBJ file to export.
		 */
		@JvmField
		var objFileName: String = "minecraft.obj"

		/**
		 * Name of .MTL file to export.
		 */
		@JvmField
		var mtlFileName: String = "minecraft.mtl"
		
		/**
		 * How many threads to use when exporting.
		 */
		@JvmField
		var exportThreads: Int = 8
		
		/**
		 * Export objects as obj groups instead of objects (Maya compatible)
		 */
		@JvmField
		var objUseGroup: Boolean = false
		

		@JvmStatic
		fun getObjObject(): Char {
			return if (objUseGroup) 'g' else 'o'
		}
	}
}
