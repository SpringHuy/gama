/*******************************************************************************************************
 *
 * gaml.statements.FocusStatement.java, in plugin gama.core,
 * is part of the source code of the GAMA modeling and simulation platform (v. 1.8)
 * 
 * (c) 2007-2018 UMI 209 UMMISCO IRD/SU & Partners
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 * 
 ********************************************************************************************************/
package gaml.statements;

import gama.GAMA;
import gama.common.interfaces.IAgent;
import gama.common.interfaces.IKeyword;
import gama.metamodel.shape.IShape;
import gama.processor.annotations.IConcept;
import gama.processor.annotations.ISymbolKind;
import gama.processor.annotations.GamlAnnotations.doc;
import gama.processor.annotations.GamlAnnotations.example;
import gama.processor.annotations.GamlAnnotations.facet;
import gama.processor.annotations.GamlAnnotations.facets;
import gama.processor.annotations.GamlAnnotations.inside;
import gama.processor.annotations.GamlAnnotations.symbol;
import gama.processor.annotations.GamlAnnotations.usage;
import gama.runtime.exceptions.GamaRuntimeException;
import gama.runtime.scope.IScope;
import gaml.descriptions.IDescription;
import gaml.expressions.IExpression;
import gaml.operators.Cast;
import gaml.types.IType;

/**
 * Written by drogoul Modified on 6 févr. 2010
 * 
 * @todo Description
 * 
 */

@symbol(name = IKeyword.FOCUS_ON, kind = ISymbolKind.SINGLE_STATEMENT, with_sequence = false, concept = {
		IConcept.DISPLAY, IConcept.GEOMETRY })
@inside(kinds = { ISymbolKind.BEHAVIOR, ISymbolKind.SEQUENCE_STATEMENT, ISymbolKind.LAYER })
@facets(value = {
		@facet(name = IKeyword.VALUE, type = IType.NONE, optional = false, doc = @doc("The agent, list of agents, geometry to focus on")) }, omissible = IKeyword.VALUE)
@doc(value = "Allows to focus on the passed parameter in all available displays. Passing 'nil' for the parameter will make all screens return to their normal zoom", usages = {
		@usage(value = "Focuses on an agent, a geometry, a set of agents, etc...)", examples = {
				@example("focus_on my_species(0);") }) })
public class FocusStatement extends AbstractStatement {

	@Override
	public String getTrace(final IScope scope) {
		// We dont trace focus statements
		return "";
	}

	final IExpression value;

	public FocusStatement(final IDescription desc) {
		super(desc);
		value = getFacet(IKeyword.VALUE);
	}

	@Override
	public Object privateExecuteIn(final IScope scope) throws GamaRuntimeException {
		final IAgent agent = scope.getAgent();
		if (agent != null && !agent.dead()) {
			final IShape o = Cast.asGeometry(scope, value.value(scope));
			GAMA.getGui().setFocusOn(o);
		}
		return value.value(scope);
	}
}
