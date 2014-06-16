// Generated from /home/stoffe/dev/graphene/core/src/main/java/Gql.g4 by ANTLR 4.x

    package org.deephacks.graphene.internal.gql;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link GqlParser}.
 */
public interface GqlListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link GqlParser#ordered}.
	 * @param ctx the parse tree
	 */
	void enterOrdered(@NotNull GqlParser.OrderedContext ctx);
	/**
	 * Exit a parse tree produced by {@link GqlParser#ordered}.
	 * @param ctx the parse tree
	 */
	void exitOrdered(@NotNull GqlParser.OrderedContext ctx);

	/**
	 * Enter a parse tree produced by {@link GqlParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(@NotNull GqlParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link GqlParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(@NotNull GqlParser.ExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link GqlParser#or}.
	 * @param ctx the parse tree
	 */
	void enterOr(@NotNull GqlParser.OrContext ctx);
	/**
	 * Exit a parse tree produced by {@link GqlParser#or}.
	 * @param ctx the parse tree
	 */
	void exitOr(@NotNull GqlParser.OrContext ctx);

	/**
	 * Enter a parse tree produced by {@link GqlParser#ordering}.
	 * @param ctx the parse tree
	 */
	void enterOrdering(@NotNull GqlParser.OrderingContext ctx);
	/**
	 * Exit a parse tree produced by {@link GqlParser#ordering}.
	 * @param ctx the parse tree
	 */
	void exitOrdering(@NotNull GqlParser.OrderingContext ctx);

	/**
	 * Enter a parse tree produced by {@link GqlParser#skip}.
	 * @param ctx the parse tree
	 */
	void enterSkip(@NotNull GqlParser.SkipContext ctx);
	/**
	 * Exit a parse tree produced by {@link GqlParser#skip}.
	 * @param ctx the parse tree
	 */
	void exitSkip(@NotNull GqlParser.SkipContext ctx);

	/**
	 * Enter a parse tree produced by {@link GqlParser#parse}.
	 * @param ctx the parse tree
	 */
	void enterParse(@NotNull GqlParser.ParseContext ctx);
	/**
	 * Exit a parse tree produced by {@link GqlParser#parse}.
	 * @param ctx the parse tree
	 */
	void exitParse(@NotNull GqlParser.ParseContext ctx);

	/**
	 * Enter a parse tree produced by {@link GqlParser#operator}.
	 * @param ctx the parse tree
	 */
	void enterOperator(@NotNull GqlParser.OperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link GqlParser#operator}.
	 * @param ctx the parse tree
	 */
	void exitOperator(@NotNull GqlParser.OperatorContext ctx);

	/**
	 * Enter a parse tree produced by {@link GqlParser#filter}.
	 * @param ctx the parse tree
	 */
	void enterFilter(@NotNull GqlParser.FilterContext ctx);
	/**
	 * Exit a parse tree produced by {@link GqlParser#filter}.
	 * @param ctx the parse tree
	 */
	void exitFilter(@NotNull GqlParser.FilterContext ctx);

	/**
	 * Enter a parse tree produced by {@link GqlParser#not}.
	 * @param ctx the parse tree
	 */
	void enterNot(@NotNull GqlParser.NotContext ctx);
	/**
	 * Exit a parse tree produced by {@link GqlParser#not}.
	 * @param ctx the parse tree
	 */
	void exitNot(@NotNull GqlParser.NotContext ctx);

	/**
	 * Enter a parse tree produced by {@link GqlParser#and}.
	 * @param ctx the parse tree
	 */
	void enterAnd(@NotNull GqlParser.AndContext ctx);
	/**
	 * Exit a parse tree produced by {@link GqlParser#and}.
	 * @param ctx the parse tree
	 */
	void exitAnd(@NotNull GqlParser.AndContext ctx);

	/**
	 * Enter a parse tree produced by {@link GqlParser#limit}.
	 * @param ctx the parse tree
	 */
	void enterLimit(@NotNull GqlParser.LimitContext ctx);
	/**
	 * Exit a parse tree produced by {@link GqlParser#limit}.
	 * @param ctx the parse tree
	 */
	void exitLimit(@NotNull GqlParser.LimitContext ctx);

	/**
	 * Enter a parse tree produced by {@link GqlParser#value}.
	 * @param ctx the parse tree
	 */
	void enterValue(@NotNull GqlParser.ValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link GqlParser#value}.
	 * @param ctx the parse tree
	 */
	void exitValue(@NotNull GqlParser.ValueContext ctx);

	/**
	 * Enter a parse tree produced by {@link GqlParser#reversed}.
	 * @param ctx the parse tree
	 */
	void enterReversed(@NotNull GqlParser.ReversedContext ctx);
	/**
	 * Exit a parse tree produced by {@link GqlParser#reversed}.
	 * @param ctx the parse tree
	 */
	void exitReversed(@NotNull GqlParser.ReversedContext ctx);
}