// Generated from /home/stoffe/dev/graphene/core/src/main/java/Gql.g4 by ANTLR 4.x

    package org.deephacks.graphene.internal.gql;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link GqlParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface GqlVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link GqlParser#ordered}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrdered(@NotNull GqlParser.OrderedContext ctx);

	/**
	 * Visit a parse tree produced by {@link GqlParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression(@NotNull GqlParser.ExpressionContext ctx);

	/**
	 * Visit a parse tree produced by {@link GqlParser#or}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOr(@NotNull GqlParser.OrContext ctx);

	/**
	 * Visit a parse tree produced by {@link GqlParser#ordering}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrdering(@NotNull GqlParser.OrderingContext ctx);

	/**
	 * Visit a parse tree produced by {@link GqlParser#skip}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSkip(@NotNull GqlParser.SkipContext ctx);

	/**
	 * Visit a parse tree produced by {@link GqlParser#parse}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParse(@NotNull GqlParser.ParseContext ctx);

	/**
	 * Visit a parse tree produced by {@link GqlParser#operator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOperator(@NotNull GqlParser.OperatorContext ctx);

	/**
	 * Visit a parse tree produced by {@link GqlParser#filter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFilter(@NotNull GqlParser.FilterContext ctx);

	/**
	 * Visit a parse tree produced by {@link GqlParser#not}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNot(@NotNull GqlParser.NotContext ctx);

	/**
	 * Visit a parse tree produced by {@link GqlParser#and}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnd(@NotNull GqlParser.AndContext ctx);

	/**
	 * Visit a parse tree produced by {@link GqlParser#limit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLimit(@NotNull GqlParser.LimitContext ctx);

	/**
	 * Visit a parse tree produced by {@link GqlParser#value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValue(@NotNull GqlParser.ValueContext ctx);

	/**
	 * Visit a parse tree produced by {@link GqlParser#reversed}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReversed(@NotNull GqlParser.ReversedContext ctx);
}