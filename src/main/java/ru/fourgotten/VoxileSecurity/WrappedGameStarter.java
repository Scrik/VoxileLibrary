package ru.fourgotten.VoxileSecurity;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import ru.fourgotten.VoxileSecurity.Data.MessagesWrapper.MessageWrappedGame;
import ru.fourgotten.VoxileSecurity.Hacks.BinaryClassLoader;
import ru.fourgotten.VoxileSecurity.Hacks.HacksApplicator;
import ru.methuselah.authlib.GlobalReplacementList;

public class WrappedGameStarter
{
	public static WrappedGameStarter instance;
	public static WrappedGameStarter getInstance()
	{
		return instance;
	}
	private MessageWrappedGame message;
	public MessageWrappedGame getMessage()
	{
		return message;
	}
	protected static final URL myOwnURL = WrappedGameStarter.class.getProtectionDomain().getCodeSource().getLocation();
	public int startGameInCurrentProcess(MessageWrappedGame message)
	{
		this.message = message;
		if(message.replacements == null)
			message.replacements = new GlobalReplacementList();
		// Load natives and libraries, fix static fields
		System.out.println("Loading libraries:");
		final ArrayList<URL> urls = new ArrayList<URL>();
		for(String library : message.libraries)
			try
			{
				final URL toURL = new File(library).toURI().toURL();
				urls.add(toURL);
				System.out.println(toURL.toString());
			} catch(MalformedURLException ex) {
				System.err.println("Error loading library file (" + library + "):\n" + ex);
			}
		System.out.println("Starting minecraft client...");
		final ClassLoader loader = new BinaryClassLoader(urls.toArray(new URL[urls.size()]), null);
		Thread.currentThread().setContextClassLoader(loader);
		HacksApplicator.process(message, loader);
		try
		{
			// Load main class and start the game
			final Class mainClass = Class.forName(message.mainClass, true, loader);
			final Method method = mainClass.getMethod("main", new Class[] { String[].class });
			method.invoke(null, new Object[]
			{
				// String[] args
				createRealGameArgumentList(message, loader),
			});
			System.out.println("Wrapper has finished it's work.");
			System.out.println("if the game is not started, it is not my problem!");
			return 0;
		} catch(ClassNotFoundException ex) {
			System.err.println(ex);
		} catch(NoSuchMethodException ex) {
			System.err.println(ex);
		} catch(SecurityException ex) {
			System.err.println(ex);
		} catch(IllegalAccessException ex) {
			System.err.println(ex);
		} catch(IllegalArgumentException ex) {
			System.err.println(ex);
		} catch(InvocationTargetException ex) {
			System.err.println(ex);
			return 6;
		}
		return 5;
	}
	public static String[] createRealGameArgumentList(MessageWrappedGame msg, ClassLoader loader)
	{
		final ArrayList<String> result = new ArrayList<String>();
		addToNewArgs(result, "--gameDir",     msg.gameDir);
		addToNewArgs(result, "--version",     msg.version);
		addToNewArgs(result, "--assetsDir",   msg.assetsDir);
		addToNewArgs(result, "--assetIndex",  msg.assetIndex);
		addToNewArgs(result, "--username",    msg.username);
		addToNewArgs(result, "--uuid",        msg.uuid);
		addToNewArgs(result, "--accessToken", msg.accessToken);
		result.add("--userType");
		result.add("mojang");
		result.add("--userProperties");
		result.add("{}");
		if(msg.arguments != null)
			result.addAll(Arrays.asList(msg.arguments));
		try
		{
			// Add tweaker class
			if(msg.tweakerClass != null && !"".equals(msg.tweakerClass))
			{
				final Class tweaker = Class.forName(msg.tweakerClass, true, loader);
				addToNewArgs(result, "--tweakClass", tweaker.getCanonicalName());
			}
		} catch(ClassNotFoundException ex) {
			System.err.println(ex);
		}
		return result.toArray(new String[result.size()]);
	}
	private static void addToNewArgs(ArrayList<String> args, String arg, String value)
	{
		if(args != null && arg != null && value != null && !"".equals(value))
		{
			args.add(arg);
			args.add(value);
		}
	}
}
