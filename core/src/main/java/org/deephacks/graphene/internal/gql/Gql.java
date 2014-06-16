package org.deephacks.graphene.internal.gql;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.deephacks.graphene.internal.gql.GqlParser.AndContext;
import org.deephacks.graphene.internal.gql.GqlParser.ExpressionContext;
import org.deephacks.graphene.internal.gql.GqlParser.FilterContext;
import org.deephacks.graphene.internal.gql.GqlParser.LimitContext;
import org.deephacks.graphene.internal.gql.GqlParser.NotContext;
import org.deephacks.graphene.internal.gql.GqlParser.OrContext;
import org.deephacks.graphene.internal.gql.GqlParser.OrderingContext;
import org.deephacks.graphene.internal.gql.GqlParser.SkipContext;
import org.deephacks.graphene.internal.gql.GqlParser.ValueContext;
import org.deephacks.graphene.internal.gql.Predicates.Contains;
import org.deephacks.graphene.internal.gql.Predicates.EndsWith;
import org.deephacks.graphene.internal.gql.Predicates.Eq;
import org.deephacks.graphene.internal.gql.Predicates.Gt;
import org.deephacks.graphene.internal.gql.Predicates.GtEq;
import org.deephacks.graphene.internal.gql.Predicates.Lt;
import org.deephacks.graphene.internal.gql.Predicates.LtEq;
import org.deephacks.graphene.internal.gql.Predicates.NotEq;
import org.deephacks.graphene.internal.gql.Predicates.RegExp;
import org.deephacks.graphene.internal.gql.Predicates.StartsWith;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.deephacks.graphene.internal.gql.Query.QueryBuilder;

class Gql<T> extends GqlBaseVisitor<QueryBuilder<T>> {
    private QueryBuilder<T> queryBuilder;

    public Gql(QueryBuilder<T> queryBuilder) {
      this.queryBuilder = queryBuilder;
    }

    @Override
    public QueryBuilder<T> visitNot(@NotNull NotContext ctx) {
      if (ctx.NOT() != null) {
        return super.visitNot(ctx).negate();
      } else if (ctx.filter().or() != null) {
        return visit(ctx.filter().or());
      }
      return super.visitNot(ctx);
    }

    @Override
    public QueryBuilder<T> visitExpression(@NotNull ExpressionContext ctx) {
      if (ctx.ID() != null) {
        queryBuilder.setType(ctx.ID().getText());
      }
      super.visitExpression(ctx);
      return queryBuilder;
    }

    @Override
    public QueryBuilder<T> visitLimit(@NotNull LimitContext ctx) {
      return queryBuilder.setLimit(Long.parseLong(ctx.NUMBER(0).getText()));
    }

    @Override
    public QueryBuilder<T> visitSkip(@NotNull SkipContext ctx) {
      return queryBuilder.setSkip(Long.parseLong(ctx.NUMBER().getText()));
    }

    @Override
    public QueryBuilder<T> visitOrdering(@NotNull OrderingContext ctx) {
      if (ctx.ordered() != null) {
        if (ctx.ordered().ID() == null || ctx.ordered().ID().isEmpty()) {
          return queryBuilder.setOrdered();
        }
        List<String> ids = ctx.ordered().ID().stream().map(TerminalNode::getText).collect(Collectors.toList());
        return queryBuilder.setOrdered(ids);
      } else if (ctx.reversed() != null) {
        if (ctx.reversed().ID() == null || ctx.reversed().ID().isEmpty()) {
          return queryBuilder.setReversed();
        }
        List<String> ids = ctx.reversed().ID().stream().map(TerminalNode::getText).collect(Collectors.toList());
        return queryBuilder.setReversed(ids);
      }
      return queryBuilder;
    }

    @Override
    public QueryBuilder<T> visitOr(@NotNull OrContext ctx) {
      ctx.and().forEach(this::visit);
      if (ctx.and().size() > 1) {
        queryBuilder = queryBuilder.or(ctx.and().size());
      }
      return queryBuilder;
    }

    @Override
    public QueryBuilder<T> visitAnd(@NotNull AndContext ctx) {
      ctx.not().forEach(this::visit);
      if (ctx.not().size() > 1) {
        queryBuilder = queryBuilder.and(ctx.not().size());
      }
      return queryBuilder;
    }

    @Override
    public QueryBuilder<T> visitFilter(@NotNull FilterContext ctx) {
      Optional<String> id = ctx.ID() == null ? Optional.empty() : Optional.of(ctx.ID().getText());
      ValueAccessor valueAccessor = new ValueAccessor(id, queryBuilder.getType());
      GrammarValue value = getValue(ctx.value(), valueAccessor);
      if (ctx.operator().EQ() != null) {
        return queryBuilder.addPredicate(new Eq<>(valueAccessor, value));
      } else if (ctx.operator().NT_EQ() != null) {
        return queryBuilder.addPredicate(new NotEq<>(valueAccessor, value));
      } else if (ctx.operator().GT() != null) {
        return queryBuilder.addPredicate(new Gt<>(valueAccessor, value));
      } else if (ctx.operator().GT_EQ() != null) {
        return queryBuilder.addPredicate(new GtEq<>(valueAccessor, value));
      } else if (ctx.operator().LT() != null) {
        return queryBuilder.addPredicate(new Lt<>(valueAccessor, value));
      } else if (ctx.operator().LT_EQ() != null) {
        return queryBuilder.addPredicate(new LtEq<>(valueAccessor, value));
      } else if (ctx.operator().CONTAINS() != null) {
        return queryBuilder.addPredicate(new Contains<>(valueAccessor, value));
      } else if (ctx.operator().STARTS_WITH() != null) {
        return queryBuilder.addPredicate(new StartsWith<>(valueAccessor, value));
      } else if (ctx.operator().ENDS_WITH() != null) {
        return queryBuilder.addPredicate(new EndsWith<>(valueAccessor, value));
      } else if (ctx.operator().REGEXP() != null) {
        return queryBuilder.addPredicate(new RegExp<>(valueAccessor, value));
      }
      throw new IllegalStateException("Did not recognize operator ["+ctx.operator()+"]");
    }

    private GrammarValue getValue(ValueContext context, ValueAccessor valueAccessor) {
      String value = context.getText();
      if (context.DECIMAL() != null) {
        return new GrammarValue(value, valueAccessor);
      } else if (context.NUMBER() != null) {
        return new GrammarValue(value, valueAccessor);
      } else if (context.TRUE() != null) {
        return new GrammarValue(true, valueAccessor);
      } else if (context.FALSE() != null) {
        return new GrammarValue(false, valueAccessor);
      } else if (context.NULL() != null) {
        return new GrammarValue(null, valueAccessor);
      }
      return new GrammarValue(value.substring(1, value.length() - 1), valueAccessor);
    }
  }
