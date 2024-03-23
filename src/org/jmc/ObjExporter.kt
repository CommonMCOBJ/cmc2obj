// For Interopt
@file:JvmName("ObjExporter")

package org.jmc;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import kotlin.collections.MutableMap;

import javax.annotation.CheckForNull;

import org.jmc.Options.OffsetType;
import org.jmc.models.Banner;
import org.jmc.registry.Registries;
import org.jmc.threading.ReaderRunnable;
import org.jmc.threading.ThreadInputQueue;
import org.jmc.threading.ThreadOutputQueue;
import org.jmc.threading.WriterRunnable;
import org.jmc.util.Filesystem;
import org.jmc.util.Hilbert.HilbertComparator;
import org.jmc.util.Log;
import org.jmc.util.Messages;

/**
 * Do the export. Export settings are taken from the global Options.
 * <p>
 * The overall logic is as follows:
 * <ul>
 * <li>Add the geometry to the OBJ, one chunk at a time.
 * <li>The ChunkDataBuffer holds a collection of chunks in the range of 
 * x-1..x+1 and z-1..z+1 around the chunk that is being processed. This
 * is so that neighbouring block information exists for blocks that are at
 * the edge of the chunk.
 * <li>By holding only 9 chunks at a time, we can export arbitrarily large
 * maps in constant memory.
 * </ul>
 * 
 * @param progress
 *            If not null, the exporter will invoke this callback to inform
 *            on the operation's progress.
 * @param writeTex
 *            Whether to write the textures to the output folder.
 */
fun export(progress: ProgressCallback?, writeTex: Boolean) {
	Log.debug("Exporting world "+Options.worldDir);
	
	val objfile: File = File(Options.outputDir, Options.objFileName);
	val mtlfile: File = File(Options.outputDir, Options.mtlFileName);
	val tmpdir: File  = File(Options.outputDir, "temp");

	if (tmpdir.exists()) {
		Log.error("Cannot create directory: " + tmpdir.getAbsolutePath() + "\nSomething is in the way.", null);
		return;
	}

	try {
		objfile.createNewFile();
		mtlfile.createNewFile();
	} catch (e: IOException) {
		Log.error("Cannot write to the chosen location!", e);
		return;
	}
	
	val threads: ArrayList<Thread> = ArrayList<Thread>(Options.exportThreads);
	var writeThread: Thread? = null;
	
	val exportTimer: Long = System.nanoTime();

	try {
		Registries.objTextures.clear();
		resetErrors();
		
		if (Options.maxX - Options.minX == 0 || Options.maxY - Options.minY == 0
				|| Options.maxZ - Options.minZ == 0) {
			Log.error(Messages.getString("MainPanel.SEL_ERR"), null, true);
			return;
		}

		val obj_writer: PrintWriter = PrintWriter(objfile, StandardCharsets.UTF_8.name())
		
		if (progress != null) {
			progress.setMessage(Messages.getString("Progress.OBJ"));
		}

		// Calculate the boundaries of the chunks selected by the user
		val cs: Point = Chunk.getChunkPos(Options.minX, Options.minZ);
		val ce: Point = Chunk.getChunkPos(Options.maxX + 15, Options.maxZ + 15);
		var oxs: Int; 
		var oys: Int;
		var ozs: Int;

		if (Options.offsetType == OffsetType.CENTER) {
			oxs = -(Options.minX + (Options.maxX - Options.minX) / 2);
			oys = -Options.minY;
			ozs = -(Options.minZ + (Options.maxZ - Options.minZ) / 2);
			Log.info("Center offset: " + oxs + "/" + oys + "/" + ozs);
		} else if (Options.offsetType == OffsetType.CUSTOM) {
			oxs = Options.offsetX;
			oys = 0;
			ozs = Options.offsetZ;
			Log.info("Custom offset: " + oxs + "/" + oys + "/" + ozs);
		} else {
			oxs = 0;
			oys = 0;
			ozs = 0;
		}

		val chunksToDo: Int = (ce.x - cs.x + 1) * (ce.y - cs.y + 1);

		val chunk_buffer: ChunkDataBuffer = ChunkDataBuffer(Options.minX, Options.maxX, Options.minY,
				Options.maxY, Options.minZ, Options.maxZ);
		
		val inputQueue: ThreadInputQueue = ThreadInputQueue();
		val outputQueue: ThreadOutputQueue  = ThreadOutputQueue(Options.exportThreads);

		val writeRunner: WriterRunnable = WriterRunnable(outputQueue, obj_writer, progress, chunksToDo);
		writeRunner.setOffset(oxs.toDouble(), oys.toDouble(), ozs.toDouble());
		writeRunner.setScale(Options.scale);
		
		writeCommonMcObjHeader(obj_writer);

		obj_writer.println("mtllib " + mtlfile.getName());
		obj_writer.println();
		if (!Options.objectPerMaterial && !Options.objectPerBlock && !Options.objectPerChunk) {
			obj_writer.println(Options.getObjObject() + " minecraft");
			obj_writer.println();
		}

		Log.info("Processing chunks...");
		
		for (i in 0..Options.exportThreads) {
			val thread: Thread = Thread(ReaderRunnable(chunk_buffer, cs, ce, inputQueue, outputQueue));
			thread.setName("ReadThread-" + i);
			thread.setPriority(Thread.NORM_PRIORITY - 1);
			threads.add(thread);
			thread.start();
		}

		writeThread = Thread(writeRunner);
		writeThread.setName("WriteThread");
		writeThread.start();
		
		val objTimer: Long = System.nanoTime();
		
		val chunkList: ArrayList<Point> = ArrayList<Point>();
		
		// loop through the chunks selected by the user
		for (cx in cs.x..ce.x) {
			for (cz in cs.y..ce.y) {
				chunkList.add(Point(cx, cz));
			}
		}
		
		chunkList.sortWith(HilbertComparator(Math.max(ce.x - cs.x, ce.y - cs.y)));
		
		for (chunk in chunkList) {
			inputQueue.add(chunk);
		}
		
		inputQueue.finish();
		
		var objTimer2: Long = System.nanoTime();
		
		for (thread in threads){
			thread.join();
		}
		Log.debug("Reading Chunks:" + (System.nanoTime() - objTimer2)/1000000000);
		objTimer2 = System.nanoTime();
		
		outputQueue.waitUntilEmpty();
		writeThread.interrupt();
		writeThread.join();
		
		Log.debug("Writing File:" + (System.nanoTime() - objTimer2)/1000000000);
		Log.info("OBJ Export Time:" + (System.nanoTime() - objTimer)/1000000000);
		
		chunk_buffer.removeAllChunks();

		obj_writer.close();
		
		if (Thread.interrupted())
			return;

		if (progress != null) {
			progress.setProgress(1.toFloat());
		}
		Log.info("Saved model to " + objfile.getAbsolutePath());

		if (!Options.objectPerBlock && (!Options.objectPerChunk || Options.objectPerMaterial)) {
			//mmdanggg2: in maya the obj importer does not recognise the same obj group appearing twice
			//		so if we want to export per chunk, the current sorting will not work in maya.
			Log.info("Sorting OBJ file...");
			if (progress != null)
				progress.setMessage(Messages.getString("Progress.OBJ_SORT"));

			if (!tmpdir.mkdir()) {
				Log.error("Cannot temp create directory: " + tmpdir.getAbsolutePath(), null);
				return;
			}

			val mainfile: File	= File(tmpdir, "main");
			val main: PrintWriter	= PrintWriter(mainfile, StandardCharsets.UTF_8.name());
			val vertexfile: File	= File(tmpdir, "vertex");
			val vertex: PrintWriter = PrintWriter(vertexfile, StandardCharsets.UTF_8.name());
			val normalfile: File	= File(tmpdir, "normal");
			val normal: PrintWriter = PrintWriter(normalfile, StandardCharsets.UTF_8.name());
			val uvfile: File	= File(tmpdir, "uv");
			val uv: PrintWriter	= PrintWriter(uvfile, StandardCharsets.UTF_8.name());

			val objin: BufferedReader = Files.newBufferedReader(objfile.toPath(), StandardCharsets.UTF_8);

			val faces: MutableMap<String, FaceFile> = HashMap<String, FaceFile>();
			var facefilecount: Int = 1;

			var current_ff: FaceFile? = null;
			var current_o: String = Options.getObjObject() + " default";

			var maxcount: Int = objfile.length().toInt();
			if (maxcount == 0) {
				maxcount = 1;
			}
			var count: Int = 0;

			var line: String?;
			while (objin.readLine().also { line = it } != null) {
				// Shadowing variable to 
				// make our lives easier
				var line = line ?: ""
				if (line.length == 0) {
					continue;
				}

				count += line.length + 1;
				if (count > maxcount) {
					count = maxcount;
				}

				if (progress != null) {
					progress.setProgress(0.5f * count.toFloat() / maxcount.toFloat());
				}

				if (line.startsWith("usemtl ")) {
					line = line.substring(7).trim();

					if (!faces.containsKey(line)) {
						current_ff = FaceFile();
						current_ff.name = line;
						current_ff.file = File(tmpdir, "" + facefilecount);
						facefilecount++;
						current_ff.writer = PrintWriter(current_ff.file, StandardCharsets.UTF_8.name());
						faces.put(line, current_ff);
					} else
						current_ff = faces.get(line);

					if (Options.objectPerChunk) {
						current_ff?.writer?.println();
						current_ff?.writer?.println(current_o);
						current_ff?.writer?.println();
					}
				} else if (line.startsWith("f ")) {
					if (current_ff != null) {
						current_ff.writer.println(line);
					}
				} else if (line.startsWith("v ")) {
					vertex.println(line);
				} else if (line.startsWith("vn ")) {
					normal.println(line);
				} else if (line.startsWith("vt ")) {
					uv.println(line);
				} else if (line.startsWith(Options.getObjObject() + " ")) {
					current_o = line;
				} else {
					main.println(line);
					if (line.startsWith("mtllib"))
						main.println();
				}
			}

			objin.close();

			vertex.close();
			normal.close();
			uv.close();

			val norm_reader: BufferedReader = Files.newBufferedReader(normalfile.toPath(), StandardCharsets.UTF_8);
			while (norm_reader.readLine().also { line = it } != null) {
				main.println(line);
			}
			norm_reader.close();
			normalfile.delete();

			val uv_reader: BufferedReader = Files.newBufferedReader(uvfile.toPath(), StandardCharsets.UTF_8);
			while (uv_reader.readLine().also { line = it } != null) {
				main.println(line);
			}
			uv_reader.close();
			uvfile.delete();

			val vertex_reader: BufferedReader = Files.newBufferedReader(vertexfile.toPath(), StandardCharsets.UTF_8);
			while (objin.readLine().also { line = it } != null) {
				main.println(line);
			}
			vertex_reader.close();
			vertexfile.delete();

			count = 0;
			maxcount = faces.size;

			for (ff in faces.values) {
				val current_mat: String = ff.name;

				ff.writer.close();

				count++;
				if (progress != null) {
					progress.setProgress(0.5f + 0.5f * count.toFloat() / maxcount.toFloat());
				}

				vertex.println();
				if (Options.objectPerMaterial && !Options.objectPerChunk) {
					main.println(Options.getObjObject() + " " + ff.name);
				}
				main.println();

				val reader: BufferedReader = Files.newBufferedReader(ff.file.toPath(), StandardCharsets.UTF_8);
				while (reader.readLine().also { line = it } != null) {
					if (Options.objectPerChunk && line?.startsWith(Options.getObjObject() + " ") as Boolean) {
						if (Options.objectPerMaterial)
							main.println(line + "_" + current_mat);
						else
							main.println(line);
					} else
						main.println(line);
				}
				reader.close();

				ff.file.delete();
			}

			main.close();

			Filesystem.moveFile(mainfile, objfile);

			if (progress != null) {
				progress.setProgress(1.toFloat());
			}

			if (!tmpdir.delete()) {
				Log.error("Failed to erase temp dir: " + tmpdir.getAbsolutePath()
						+ "\nPlease remove it yourself!", null);
			}
		}
		
		Log.info(String.format("Writing materials to %s...", mtlfile.getAbsolutePath()));
		Materials.writeMTLFile(mtlfile, progress);
		
		if (writeTex) {
			Log.info("Exporting textures...");
			synchronized (Registries.objTextures) {
				/*if (Options.textureMerge) {
					Log.error("Texture merging is not supported!", null);
					TextureExporter.mergeTextures(Registries.objTextures, progress);
				} else {
					TextureExporter.exportTextures(Registries.objTextures, progress);
				//}*/// TODO fix single tex export
				TextureExporter.exportTextures(Registries.objTextures, progress);
			}
		}
		Log.info("Export Time:" + (System.nanoTime() - exportTimer)/1000000000);
		Log.info("Done!");
	} catch (e: InterruptedException) {
		Log.debug("Export interrupted!");
	} catch (e: Exception) {
		Log.error("Error while exporting OBJ:", e);
	} finally {
		for (t in threads) {
			t.interrupt();
		}
		if (writeThread != null) {
			writeThread.interrupt();
		}
		System.gc();
	}
}

/**
 * Add CommonMCOBJ Header.
 * <p>
 * CommonMCOBJ is a common standard for exporting metadata
 * in OBJs.
 * @param objWriter
 *      The writer that's writing the OBJ file
 */
private fun writeCommonMcObjHeader(objWriter: PrintWriter) {
	objWriter.println("# COMMON_MC_OBJ_START");
	objWriter.println("# version: 1");
	objWriter.println("# exporter: cmc2obj");  // Name of the exporter, all lowercase, with spaces substituted by underscores
	objWriter.println("# world_name: ${Options.worldDir?.getName()}");  // Name of the source world
	objWriter.println("# world_path: ${Options.worldDir?.toString()}");  // Path of the source world
	objWriter.println("# exported_bounds_min: (${Options.minX}, ${Options.minY}, ${Options.minZ})");  // The lowest block coordinate exported in the obj file
	objWriter.println("# exported_bounds_max: (${Options.maxX-1}, ${Options.maxY-1}, ${Options.maxZ-1})");  // The highest block coordinate exported in the obj file
	objWriter.println("# block_scale: ${Options.scale}"); // Scale of each block
	objWriter.println("# is_centered: " + (if (Options.offsetType == OffsetType.CENTER) "true" else "false"));  // true if centered, false if not
	objWriter.println("# z_up: false");  // true if the Z axis is up instead of Y, false is not
	objWriter.println("# texture_type: INDIVIDUAL_TILES");  // ATLAS or INDIVIDUAL_TILES
	objWriter.println("# has_split_blocks: " + (if (Options.objectPerBlock) "true" else "false"));  // true if blocks have been split, false if not
	objWriter.println("# COMMON_MC_OBJ_END");
	objWriter.println();
}

private fun resetErrors() {
	Banner.resetReadError();
	Log.resetSingles();
}

/**
 * Little helper class for the map used in sorting.
 * 
 * @author danijel
 * 
 */
private class FaceFile {
	lateinit var name: String;
	lateinit var file: File;
	lateinit var writer: PrintWriter;
}
