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


public class Main
{
	/**
	 * Start of program.
	 * 
	 * @param args program arguments
	 */
	public static void main(String[] args)
	{
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) { 
				Log.error("Uncaught exception in thread: "+t.getName(), e);
			}
		});

		try
		{
			Preferences prefs = Preferences.userNodeForPackage(Settings.class);
			int loc_num=prefs.getInt("LANGUAGE", 0);
			Locale.setDefault(Options.availableLocales[loc_num]);
		} catch(Exception e)
		{
			Log.info("WARNING: cannot change language on this system!");
			Locale.setDefault(Locale.ENGLISH);
		}
		
		System.out.println("cmc2obj "+Version.VERSION());

		if (args.length == 0) {
			Options.uiMode = UIMode.GUI;
			runGUI();
		}
		else {
			Options.uiMode = UIMode.CONSOLE;
			runConsole(args);
		}
	}

	
	private static void runGUI()
	{
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		new MainWindow();

		try {
			Configuration.initialize();
			Registries.initialize();
			BlockTypes.initialize();
			IDConvert.initialize();
			EntityTypes.initialize();
			
			MainWindow.main.loadingFinished();
			
			Log.info("Initialization done");
		}
		catch (Exception e) {
			Log.error("Error reading configuration file:", e);
		}
	}
	

	private static void runConsole(String[] args)
	{
		try {
			CmdLineParser.parse(args);
		}
		catch (CmdLineParser.CmdLineException e) {
			Log.error("Error: " + e.getMessage(), null);
			System.exit(-1);
		}
		
		try {
			Configuration.initialize();
			Registries.initialize();
			BlockTypes.initialize();
			IDConvert.initialize();
			EntityTypes.initialize();
		}
		catch (Exception e) {
			Log.error("Error reading configuration file:", e);
			System.exit(-2);
		}
		
		if (Options.exportWorld) {
			ObjExporter.export(new ConsoleProgress(), Options.exportTex);
		}
	}

}

/** Simple console progress bar */
class ConsoleProgress implements ProgressCallback
{
	private static final int SIZE = 78;
	private int last = Integer.MAX_VALUE;
	
	@Override
	public void setProgress(float value) {
		int curr = Math.min((int)(SIZE * value), SIZE);
		if (curr < last) {
			System.out.print("[");
			last = 0;
		}
		while (curr > last) {
			System.out.print(".");
			last++;
			if (last == SIZE) System.out.println("]");
		}
	}

	@Override
	public void setMessage(String message) {
		// TODO Auto-generated method stub
	}
}
