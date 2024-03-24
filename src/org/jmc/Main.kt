package org.jmc;

import java.util.Locale;
import java.util.prefs.Preferences;

import javax.swing.UIManager;

import org.jmc.Options.UIMode;
import org.jmc.gui.MainWindow;
import org.jmc.gui.Settings;
import org.jmc.registry.Registries;
import org.jmc.util.IDConvert;
import org.jmc.util.Log;
import org.jmc.export;

fun main(args: Array<String>)
{
	Thread.setDefaultUncaughtExceptionHandler(Thread.UncaughtExceptionHandler { t: Thread, e: Throwable ->
		Log.error("Uncaught exception in thread: ${t.getName()}", e);
	});

	try
	{
		val prefs: Preferences = Preferences.userNodeForPackage(Settings::class.java);
		val loc_num: Int = prefs.getInt("LANGUAGE", 0);
		Locale.setDefault(Options.availableLocales[loc_num]);
	} catch(e: Exception)
	{
		Log.info("WARNING: cannot change language on this system!");
		Locale.setDefault(Locale.ENGLISH);
	}
	
	System.out.println("cmc2obj "+Version.VERSION());

	if (args.size == 0) {
		Options.uiMode = UIMode.GUI;
		runGUI();
	}
	else {
		Options.uiMode = UIMode.CONSOLE;
		runConsole(args);
	}
}

	
private fun runGUI()
{
	try {
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
	}
	catch (e: Exception) {
		e.printStackTrace();
	}
	
	MainWindow();

	try {
		Configuration.initialize();
		Registries.initialize();
		BlockTypes.initialize();
		IDConvert.initialize();
		EntityTypes.initialize();
		
		MainWindow.main.loadingFinished();
		
		Log.info("Initialization done");
	}
	catch (e: Exception) {
		Log.error("Error reading configuration file:", e);
	}
}


private fun runConsole(args: Array<String>)
{
	try {
		CmdLineParser.parse(args);
	}
	catch (e: CmdLineParser.CmdLineException) {
		Log.error("Error: " + e.message, null);
		System.exit(-1);
	}
	
	try {
		Configuration.initialize();
		Registries.initialize();
		BlockTypes.initialize();
		IDConvert.initialize();
		EntityTypes.initialize();
	}
	catch (e: Exception) {
		Log.error("Error reading configuration file:", e);
		System.exit(-2);
	}
	
	if (Options.exportWorld) {
		export(ConsoleProgress(), Options.exportTex);
	}
}


class ConsoleProgress : ProgressCallback {
    companion object {
        private const val SIZE = 78
    }

    private var last = Int.MAX_VALUE

    override fun setProgress(value: Float) {
        val curr = minOf((SIZE * value).toInt(), SIZE)
        if (curr < last) {
            print("[")
            last = 0
        }
        while (curr > last) {
            print(".")
            last++
            if (last == SIZE) println("]")
        }
    }

    override fun setMessage(message: String) {
        // TODO: Implement setMessage if needed
    }
}


