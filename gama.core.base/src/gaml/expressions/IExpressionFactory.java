/*******************************************************************************************************
 *
 * gaml.expressions.IExpressionFactory.java, in plugin gama.core, is part of the source code of the GAMA
 * modeling and simulation platform (v. 1.8)
 *
 * (c) 2007-2018 UMI 209 UMMISCO IRD/SU & Partners
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package gaml.expressions;

import org.eclipse.emf.ecore.EObject;

import gama.common.interfaces.IAgent;
import gama.runtime.exceptions.GamaRuntimeException;
import gama.runtime.scope.IExecutionContext;
import gaml.constants.ConstantExpression;
import gaml.constants.SpeciesConstantExpression;
import gaml.constants.UnitConstantExpression;
import gaml.descriptions.ActionDescription;
import gaml.descriptions.ConstantExpressionDescription;
import gaml.descriptions.IDescription;
import gaml.descriptions.IExpressionDescription;
import gaml.statements.Arguments;
import gaml.types.IType;

/**
 * Written by drogoul Modified on 27 d�c. 2010
 *
 * @todo Description
 *
 */
@SuppressWarnings ({ "rawtypes" })
public interface IExpressionFactory {

	ConstantExpression TRUE_EXPR = ConstantExpressionDescription.TRUE_EXPR_DESCRIPTION;
	ConstantExpression FALSE_EXPR = ConstantExpressionDescription.FALSE_EXPR_DESCRIPTION;
	ConstantExpression NIL_EXPR = ConstantExpressionDescription.NULL_EXPR_DESCRIPTION;
	String TEMPORARY_ACTION_NAME = "__synthetic__action__";

	// public void registerParserProvider(IExpressionCompilerProvider parser);

	ConstantExpression createConst(final Object val, final IType type) throws GamaRuntimeException;

	ConstantExpression createConst(final Object val, final IType type, String name) throws GamaRuntimeException;

	SpeciesConstantExpression createSpeciesConstant(final IType type);

	IExpression createExpr(final IExpressionDescription s, final IDescription context);

	IExpression createExpr(final String s, IDescription context);

	IExpression createExpr(final String s, final IDescription context, final IExecutionContext additionalContext);

	UnitConstantExpression getUnitExpr(final String unit);

	Arguments createArgumentMap(ActionDescription action, IExpressionDescription args, IDescription context);

	IExpressionCompiler getParser();

	IExpression createVar(String name, IType type, boolean isConst, int scope, IDescription definitionDescription);

	IExpression createList(final Iterable<? extends IExpression> elements);

	IExpression createMap(final Iterable<? extends IExpression> elements);

	/**
	 * @param op
	 * @param context
	 * @param currentEObject
	 * @param args
	 * @return
	 */
	IExpression createOperator(String op, IDescription context, EObject currentEObject, IExpression... args);

	/**
	 * @param type
	 * @param keyType
	 * @param contentsType
	 * @return
	 */
	IExpression createTypeExpression(IType type);

	/**
	 *
	 */
	void resetParser();

	/**
	 * Creates a new unit expression
	 *
	 * @param value
	 * @param t
	 * @param doc
	 * @return
	 */
	UnitConstantExpression createUnit(Object value, IType t, String name, String doc, String deprecated, boolean isTime,
			String[] names);

	/**
	 * @param op
	 * @param callerContext
	 * @param action
	 * @param call
	 * @param arguments
	 * @return
	 */
	IExpression createAction(String op, IDescription callerContext, ActionDescription action, IExpression call,
			Arguments arguments);

	IExpression createTemporaryActionForAgent(IAgent agent, String expression, IExecutionContext tempContext);

	boolean hasOperator(String op, IDescription context, EObject object, IExpression... compiledArgs);

	IExpression createAs(IDescription context, IExpression toCast, IExpression createTypeExpression);

}