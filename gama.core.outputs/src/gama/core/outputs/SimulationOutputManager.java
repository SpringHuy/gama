/*******************************************************************************************************
 *
 * gama.core.outputs.SimulationOutputManager.java, in plugin msi.gama.core, is part of the source code of the GAMA
 * modeling and simulation platform (v. 1.8)
 *
 * (c) 2007-2018 UMI 209 UMMISCO IRD/SU & Partners
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package gama.core.outputs;

import gama.processor.annotations.ISymbolKind;
import gama.processor.annotations.GamlAnnotations.doc;
import gama.processor.annotations.GamlAnnotations.example;
import gama.processor.annotations.GamlAnnotations.inside;
import gama.processor.annotations.GamlAnnotations.symbol;
import gama.processor.annotations.GamlAnnotations.usage;
import gama.GAMA;
import gama.common.interfaces.IKeyword;
import gama.common.interfaces.outputs.IDisplayOutput;
import gama.common.interfaces.outputs.IOutput;
import gama.common.interfaces.outputs.IOutputManager;
import gama.kernel.simulation.SimulationAgent;
import gama.runtime.scope.IScope;
import gaml.compilation.factories.DescriptionFactory;
import gaml.descriptions.IDescription;

/**
 * The Class OutputManager.
 *
 * @author Alexis Drogoul modified by Romain Lavaud 05.07.2010
 */
@symbol (
		name = IKeyword.OUTPUT,
		kind = ISymbolKind.OUTPUT,
		with_sequence = true,
		concept = {})
@inside (
		kinds = { ISymbolKind.MODEL, ISymbolKind.EXPERIMENT })
@doc (
		value = "`output` blocks define how to visualize a simulation (with one or more display blocks that define separate windows). It will include a set of displays, monitors and files statements. It will be taken into account only if the experiment type is `gui`.",
		usages = { @usage (
				value = "Its basic syntax is: ",
				examples = { @example (
						value = "experiment exp_name type: gui {",
						isExecutable = false),
						@example (
								value = "   // [inputs]",
								isExecutable = false),
						@example (
								value = "   output {",
								isExecutable = false),
						@example (
								value = "      // [display, file, inspect, layout or monitor statements]",
								isExecutable = false),
						@example (
								value = "   }",
								isExecutable = false),
						@example (
								value = "}",
								isExecutable = false) }) },
		see = { IKeyword.DISPLAY, IKeyword.MONITOR, IKeyword.INSPECT, IKeyword.OUTPUT_FILE, IKeyword.LAYOUT })
public class SimulationOutputManager extends AbstractOutputManager implements IOutputManager.Simulation {

	public static SimulationOutputManager createEmpty() {
		return new SimulationOutputManager(DescriptionFactory.create(IKeyword.OUTPUT, (String[]) null));
	}

	public SimulationOutputManager(final IDescription desc) {
		super(desc);
	}

	@Override
	public boolean init(final IScope scope) {
		scope.getGui().getStatus(scope).waitStatus(" Building outputs ");
		final boolean result = super.init(scope);
		updateDisplayOutputsName(scope.getSimulation());
		scope.getGui().getStatus(scope).informStatus(" " + scope.getRoot().getName() + " ready");
		return result;
	}

	@Override
	public void updateDisplayOutputsName(final SimulationAgent agent) {
		for (final IOutput out : this) {
			if (out instanceof IDisplayOutput) {
				final IDisplayOutput display = (IDisplayOutput) out;
				GAMA.getGui().updateViewTitle(display, agent);
			}
		}

	}

}
