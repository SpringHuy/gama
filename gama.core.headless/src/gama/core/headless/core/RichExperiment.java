/*********************************************************************************************
 *
 *
 * 'MoleExperiment.java', in plugin 'gama.core.headless', is part of the source code of the GAMA modeling and simulation
 * platform. (c) 2007-2014 UMI 209 UMMISCO IRD/UPMC & Partners
 *
 * Visit https://code.google.com/p/gama-platform/ for license information and developers contact.
 *
 *
 **********************************************************************************************/
package gama.core.headless.core;

import gama.core.headless.common.DataType;
import gama.core.headless.job.ExperimentJob.ListenedVariable;
import gama.core.headless.job.ExperimentJob.OutputType;
import gama.GAMA;
import gama.common.interfaces.IModel;
import gama.common.interfaces.outputs.IDisplayOutput;
import gama.common.interfaces.outputs.IOutput;
import gama.runtime.exceptions.GamaRuntimeException;

public class RichExperiment extends Experiment implements IRichExperiment {
	public RichExperiment(final IModel mdl) {
		super(mdl);
	}

	@Override
	public OutputType getTypeOf(final String name) {
		if (currentExperiment == null)
			return OutputType.OUTPUT;
		if (currentExperiment.hasVar(name))
			return OutputType.EXPERIMENT_ATTRIBUTE;
		if (currentExperiment.getModel().getSpecies().hasVar(name))
			return OutputType.SIMULATION_ATTRIBUTE;
		return OutputType.OUTPUT;
	}

	@Override
	public RichOutput getRichOutput(final ListenedVariable v) {
		final String parameterName = v.getName();
		if (currentSimulation.dead())
			return null;
		final IOutput output = currentSimulation.getOutputManager().getOutputWithOriginalName(parameterName);
		if (output == null)
			throw GamaRuntimeException.error("Output unresolved", currentExperiment.getExperimentScope());
		output.update();

		Object val = null;
		DataType tpe = null;

		if (output instanceof IOutput.Monitor) {
			// ((SimulationAgent)
			// this.currentExperiment.getAgent().getSimulation()).getOutputManager().getOutputWithName(parameterName)
			val = ((IOutput.Monitor) output).getLastValue();
			if (val instanceof Integer) {
				tpe = DataType.INT;
			} else if (val instanceof Double) {
				tpe = DataType.INT;
			} else if (val instanceof String) {
				tpe = DataType.STRING;
			} else {
				tpe = DataType.UNDEFINED;
			}

		} else if (output instanceof IDisplayOutput.Layered) {
			val = ((IDisplayOutput.Layered) output).getImage(v.width, v.height);
			tpe = DataType.DISPLAY2D;
		} else if (output instanceof IOutput.FileBased) {
			val = ((IOutput.FileBased) output).getFile();
			tpe = DataType.DISPLAY2D;
		}
		return new RichOutput(parameterName, this.currentStep, val, tpe);
	}

	@Override
	public void dispose() {
		GAMA.closeExperiment(currentExperiment);
	}
}
