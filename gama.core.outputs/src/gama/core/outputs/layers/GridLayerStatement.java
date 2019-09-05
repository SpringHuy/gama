/*******************************************************************************************************
 *
 * gama.core.outputs.layers.GridLayerStatement.java, in plugin msi.gama.core, is part of the source code of the GAMA
 * modeling and simulation platform (v. 1.8)
 *
 * (c) 2007-2018 UMI 209 UMMISCO IRD/SU & Partners
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package gama.core.outputs.layers;

import static gaml.expressions.IExpressionFactory.TRUE_EXPR;

import gama.core.outputs.layers.GridLayerStatement.GridLayerSerializer;
import gama.core.outputs.layers.GridLayerStatement.GridLayerValidator;
import gama.processor.annotations.IConcept;
import gama.processor.annotations.ISymbolKind;
import gama.processor.annotations.GamlAnnotations.doc;
import gama.processor.annotations.GamlAnnotations.example;
import gama.processor.annotations.GamlAnnotations.facet;
import gama.processor.annotations.GamlAnnotations.facets;
import gama.processor.annotations.GamlAnnotations.inside;
import gama.processor.annotations.GamlAnnotations.symbol;
import gama.processor.annotations.GamlAnnotations.usage;
import gama.common.interfaces.IGamlIssue;
import gama.common.interfaces.IKeyword;
import gama.runtime.exceptions.GamaRuntimeException;
import gama.runtime.scope.IScope;
import gaml.GAML;
import gaml.compilation.annotations.serializer;
import gaml.compilation.annotations.validator;
import gaml.compilation.interfaces.IDescriptionValidator;
import gaml.descriptions.IDescription;
import gaml.descriptions.SpeciesDescription;
import gaml.descriptions.StatementDescription;
import gaml.descriptions.SymbolDescription;
import gaml.descriptions.SymbolSerializer;
import gaml.expressions.IExpression;
import gaml.types.IType;
import gaml.types.Types;

/**
 * Written by drogoul Modified on 9 nov. 2009
 *
 * @todo Description
 *
 */
@symbol (
		name = IKeyword.GRID_POPULATION,
		kind = ISymbolKind.LAYER,
		with_sequence = false,
		concept = { IConcept.GRID, IConcept.DISPLAY, IConcept.INSPECTOR })
@inside (
		symbols = IKeyword.DISPLAY)
@facets (
		value = { @facet (
				name = IKeyword.POSITION,
				type = IType.POINT,
				optional = true,
				doc = @doc ("position of the upper-left corner of the layer. Note that if coordinates are in [0,1[, the position is relative to the size of the environment (e.g. {0.5,0.5} refers to the middle of the display) whereas it is absolute when coordinates are greater than 1 for x and y. The z-ordinate can only be defined between 0 and 1. The position can only be a 3D point {0.5, 0.5, 0.5}, the last coordinate specifying the elevation of the layer.")),
				@facet (
						name = IKeyword.SELECTABLE,
						type = { IType.BOOL },
						optional = true,
						doc = @doc ("Indicates whether the agents present on this layer are selectable by the user. Default is true")),
				@facet (
						name = IKeyword.SIZE,
						type = IType.POINT,
						optional = true,
						doc = @doc ("extent of the layer in the screen from its position. Coordinates in [0,1[ are treated as percentages of the total surface, while coordinates > 1 are treated as absolute sizes in model units (i.e. considering the model occupies the entire view). Like in 'position', an elevation can be provided with the z coordinate, allowing to scale the layer in the 3 directions ")),
				@facet (
						name = IKeyword.TRANSPARENCY,
						type = IType.FLOAT,
						optional = true,
						doc = @doc ("the transparency rate of the agents (between 0 and 1, 1 means no transparency)")),
				@facet (
						name = IKeyword.SPECIES,
						type = IType.SPECIES,
						optional = false,
						doc = @doc ("the species of the agents in the grid")),
				@facet (
						name = IKeyword.LINES,
						type = IType.COLOR,
						optional = true,
						doc = @doc ("the color to draw lines (borders of cells)")),
				@facet (
						name = IKeyword.ELEVATION,
						type = { IType.MATRIX, IType.FLOAT, IType.INT, IType.BOOL },
						optional = true,
						doc = @doc ("Allows to specify the elevation of each cell, if any. Can be a matrix of float (provided it has the same size than the grid), an int or float variable of the grid species, or simply true (in which case, the variable called 'grid_value' is used to compute the elevation of each cell)")),
				@facet (
						name = IKeyword.TEXTURE,
						type = { IType.FILE },
						optional = true,
						doc = @doc ("Either file  containing the texture image to be applied on the grid or, if not specified, the use of the image composed by the colors of the cells")),
				@facet (
						name = IKeyword.GRAYSCALE,
						type = IType.BOOL,
						optional = true,
						doc = @doc ("if true, givse a grey value to each polygon depending on its elevation (false by default)")),
				@facet (
						name = IKeyword.TRIANGULATION,
						type = IType.BOOL,
						optional = true,
						doc = @doc ("specifies whther the cells will be triangulated: if it is false, they will be displayed as horizontal squares at a given elevation, whereas if it is true, cells will be triangulated and linked to neighbors in order to have a continuous surface (false by default)")),
				@facet (
						name = "hexagonal",
						type = IType.BOOL,
						optional = true,
						internal = true,
						doc = @doc ("")),
				@facet (
						name = IKeyword.TEXT,
						type = IType.BOOL,
						optional = true,
						doc = @doc ("specify whether the attribute used to compute the elevation is displayed on each cells (false by default)")),
				@facet (
						name = "draw_as_dem",
						type = IType.BOOL,
						optional = true,
						doc = @doc (
								deprecated = "use 'elevation' instead. This facet is not functional anymore")),
				@facet (
						name = "dem",
						type = IType.MATRIX,
						optional = true,
						doc = @doc (
								deprecated = "use 'elevation' instead. This facet is not functional anymore")),
				@facet (
						name = IKeyword.REFRESH,
						type = IType.BOOL,
						optional = true,
						doc = @doc ("(openGL only) specify whether the display of the species is refreshed. (true by default, usefull in case of agents that do not move)")) },
		omissible = IKeyword.SPECIES)
@doc (
		value = "`" + IKeyword.GRID_POPULATION + "` is used using the `" + IKeyword.GRID
				+ "` keyword. It allows the modeler to display in an optimized way all cell agents of a grid (i.e. all agents of a species having a grid topology).",
		usages = { @usage (
				value = "The general syntax is:",
				examples = { @example (
						value = "display my_display {",
						isExecutable = false),
						@example (
								value = "   grid ant_grid lines: #black position: { 0.5, 0 } size: {0.5,0.5};",
								isExecutable = false),
						@example (
								value = "}",
								isExecutable = false) }),
				@usage (
						value = "To display a grid as a DEM:",
						examples = { @example (
								value = "display my_display {",
								isExecutable = false),
								@example (
										value = "    grid cell texture: texture_file text: false triangulation: true elevation: true;",
										isExecutable = false),
								@example (
										value = "}",
										isExecutable = false) }) },
		see = { IKeyword.DISPLAY, IKeyword.AGENTS, IKeyword.CHART, IKeyword.EVENT, "graphics", IKeyword.IMAGE,
				IKeyword.OVERLAY, IKeyword.POPULATION })
@serializer (GridLayerSerializer.class)
@validator (GridLayerValidator.class)
public class GridLayerStatement extends AbstractLayerStatement {

	public static class GridLayerSerializer extends SymbolSerializer<SymbolDescription> {

		@Override
		protected void serializeKeyword(final SymbolDescription desc, final StringBuilder sb,
				final boolean includingBuiltIn) {
			sb.append("grid ");
		}

	}

	public static class GridLayerValidator implements IDescriptionValidator<StatementDescription> {

		@Override
		public void validate(final StatementDescription d) {
			final String name = d.getFacet(SPECIES).serialize(true);
			final SpeciesDescription sd = d.getModelDescription().getSpeciesDescription(name);
			if (sd == null || !sd.isGrid()) {
				d.error(name + " is not a grid species", IGamlIssue.WRONG_TYPE, SPECIES);
				return;
			}
			final IExpression exp = sd.getFacetExpr(NEIGHBORS);
			if (exp != null && exp.isConst()) {
				final Integer n = (Integer) exp.getConstValue();
				if (n == 6) {
					d.setFacet("hexagonal", TRUE_EXPR);
				}
			}
			final IExpression tx = d.getFacetExpr(TEXTURE);
			final IExpression el = d.getFacetExpr(ELEVATION);
			if (el == null || FALSE.equals(el.serialize(true))) {
				if (tx == null) {
					d.setFacet("flat", TRUE_EXPR);
				} else {
					// if texture is defined and elevation no, we need to set a fake elevation otherwise texture will
					// not be drawn
					d.setFacet(ELEVATION, GAML.getExpressionFactory().createConst(0.0, Types.FLOAT));
				}
			}
		}

	}

	final boolean isHexagonal, isFlatGrid;

	public GridLayerStatement(final IDescription desc) throws GamaRuntimeException {
		super(desc);
		setName(getFacet(IKeyword.SPECIES).literalValue());
		isHexagonal = desc.hasFacet("hexagonal");
		isFlatGrid = desc.hasFacet("flat");
	}

	@Override
	public boolean _init(final IScope scope) throws GamaRuntimeException {
		return true;
	}

	@Override
	public LayerType getType(final boolean isOpenGL) {
		return isHexagonal || isOpenGL && isFlatGrid ? LayerType.GRID_AGENTS : LayerType.GRID;
	}

	@Override
	public boolean _step(final IScope sim) throws GamaRuntimeException {
		return true;
	}

}
