/*********************************************************************************************
 * 
 *
 * 'Application.java', in plugin 'msi.gama.headless', is part of the source code of the 
 * GAMA modeling and simulation platform.
 * (c) 2007-2014 UMI 209 UMMISCO IRD/UPMC & Partners
 * 
 * Visit https://code.google.com/p/gama-platform/ for license information and developers contact.
 * 
 * 
 **********************************************************************************************/
package msi.gama.headless.runtime;

import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


import msi.gama.headless.common.Globals;
import msi.gama.headless.common.HeadLessErrors;
import msi.gama.headless.core.HeadlessSimulationLoader;
import msi.gama.headless.job.ExperimentJob;
import msi.gama.headless.job.IExperimentJob;
import msi.gama.headless.xml.ConsoleReader;
import msi.gama.headless.xml.Reader;
import msi.gama.headless.xml.ScriptFactory;
import msi.gama.headless.xml.XMLWriter;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;




public class Application implements IApplication {

	final public static String CONSOLE_PARAMETER = "-c";
	final public static String TUNNELING_PARAMETER = "-p";
	final public static String THREAD_PARAMERTER = "-hpc";
	final public static String VERBOSE_PARAMERTER = "-v";
	
	
	public static boolean headLessSimulation = false;
	public int numberOfThread = -1;
	public boolean consoleMode = false;
	public boolean tunnelingMode = false;
	public boolean verbose = false;
	public SimulationRuntime processorQueue;

	private static boolean containParameter(final String[] args, String param)
	{
		for(String p:args)
			{
				if(p.equals(param))
					return true;
			}
		return false;
	}

	private static boolean containConsoleParameter(final String[] args)
	{
		return containParameter(args, CONSOLE_PARAMETER);
	}

	private static boolean containTunnellingParameter(final String[] args)
	{
		return containParameter(args, TUNNELING_PARAMETER);
	}

	private static boolean containVerboseParameter(final String[] args)
	{
		return containParameter(args, VERBOSE_PARAMERTER);
	}
	
	private static int getNumberOfThread(final String[] args)
	{
		for(int n = 0; n<args.length; n++)
		{
			if(args[n].equals(THREAD_PARAMERTER))
				return Integer.valueOf(args[n+1]).intValue();
			
		}
		return SimulationRuntime.UNDEFINED_QUEUE_SIZE;
	}
	private  boolean checkParameters(final String[] args) {
		if ( args == null && !this.tunnelingMode  ) { return showError(HeadLessErrors.LAUNCHING_ERROR, null); }
		if ( args.length < 2 && !this.tunnelingMode  ) { return showError(HeadLessErrors.PARAMETER_ERROR, null); }
	
		int outIndex = args.length -1;
		int inIndex = args.length -2;
		
		Globals.OUTPUT_PATH = args[outIndex];
		Globals.IMAGES_PATH = args[outIndex] + "/snapshot";
		File output = new File(Globals.OUTPUT_PATH);
		if(!output.exists())
			output.mkdir();
		
		File images = new File(Globals.IMAGES_PATH);
		if(!images.exists())
			images.mkdir();
		
		if(this.consoleMode == false)
		{
			File input = new File(args[inIndex]);
			if (!input.exists()) {
				return showError(HeadLessErrors.NOT_EXIST_FILE_ERROR, args[inIndex]);
			}

		}
		return true;
	}

	private static boolean showError(final int errorCode, final String path) {
		System.out.println(HeadLessErrors.getError(errorCode, path));
		return false;
	}
	
	@Override
	public Object start(final IApplicationContext context) throws Exception {
		SystemLogger.removeDisplay();
		Map<String, String[]> mm = context.getArguments();
		String[] args = mm.get("application.args");
		verbose = containVerboseParameter(args);
		if(verbose)
		{
			  SystemLogger.activeDisplay();  
		}
		HeadlessSimulationLoader.preloadGAMA();
		
/*		List<IExperimentJob> jb = ScriptFactory.loadAndBuildJobs(args[args.length-2]);
		Document dd =ScriptFactory.buildXmlDocument(jb);
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(dd);
		StreamResult result = new StreamResult(new File("/tmp/file.xml"));
		transformer.transform(source, result);

		System.out.println("File saved!");*/
		this.tunnelingMode = Application.containTunnellingParameter(args);
		this.consoleMode = tunnelingMode || Application.containConsoleParameter(args);
		
		
		if ( tunnelingMode == false && !checkParameters(args)  ) {
			System.exit(-1);
		}
		this.numberOfThread = Application.getNumberOfThread(args);
		processorQueue = new LocalSimulationRuntime(this.numberOfThread);
		
		Reader in = null;
		
		if(this.verbose ||!this.tunnelingMode)
		{
			SystemLogger.activeDisplay();
		}
		
		if(this.consoleMode)
		{
			in =new Reader(ConsoleReader.readOnConsole());
		}
		else
		{
			 in = new Reader(args[args.length-2]);
			 
		}
		in.parseXmlFile();
		 this.buildAndRunSimulation(in.getSimulation());
		 in.dispose();
		while (processorQueue.isPerformingSimulation()) {
			Thread.sleep(1000);
		}
		return null;
		
	}
	public void buildAndRunSimulation(Collection<ExperimentJob> sims)
	{
		Iterator<ExperimentJob> it = sims.iterator();
		while (it.hasNext()) {
			ExperimentJob sim = it.next();
			try {
				XMLWriter ou = null;
				if(tunnelingMode == true)
				{
					ou = new XMLWriter(new BufferedWriter(new OutputStreamWriter(System.out)));
				}
				else
				{
					ou = new XMLWriter(Globals.OUTPUT_PATH + "/" + Globals.OUTPUT_FILENAME + sim.getExperimentID() + ".xml");
				
				}
				sim.setBufferedWriter(ou);	
				
				processorQueue.pushSimulation(sim);
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}

	@Override
	public void stop() {}

}
