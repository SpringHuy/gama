/*********************************************************************************************
 *
 * 'BlockExpressionDescription.java, in plugin gama.core.lang, is part of the source code of the GAMA modeling and
 * simulation platform. (c) 2007-2016 UMI 209 UMMISCO IRD/UPMC & Partners
 *
 * Visit https://github.com/gama-platform/gama for license information and developers contact.
 * 
 *
 **********************************************************************************************/
package gama.core.lang.gaml.expression;

import gaml.compilation.factories.DescriptionFactory;
import gaml.compilation.interfaces.ISyntacticElement;
import gaml.descriptions.IDescription;
import gaml.descriptions.IExpressionDescription;
import gaml.descriptions.SpeciesDescription;
import gaml.descriptions.StatementDescription;
import gaml.expressions.DenotedActionExpression;
import gaml.expressions.IExpression;

public class BlockExpressionDescription extends EcoreBasedExpressionDescription {

	final ISyntacticElement element;

	public BlockExpressionDescription(final ISyntacticElement element) {
		super(element.getElement());
		this.element = element;
	}

	@Override
	public IExpression compile(final IDescription context) {
		final SpeciesDescription sd = context.getSpeciesContext();
		// if (sd.isExperiment())
		// sd = sd.getModelDescription();
		final StatementDescription action = (StatementDescription) DescriptionFactory.create(element, sd, null);
		if (action != null) {
			sd.addChild(action);
			action.validate();
			//			final String name = action.getName();
			expression = new DenotedActionExpression(action);
		}
		return expression;
	}

	@Override
	public IExpressionDescription cleanCopy() {
		return new BlockExpressionDescription(element);
	}

}
