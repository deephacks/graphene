package org.deephacks.graphene.cdi;


import org.deephacks.graphene.Graphene;

import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.io.Serializable;
import java.lang.reflect.Method;

@Transaction
@Interceptor
class TransactionInterceptor implements Serializable {
  @Inject
  private Graphene graphene;


  @AroundInvoke
  public Object aroundInvoke(final InvocationContext ic) throws Exception {
    TransactionAttribute attr = getTransactionAttribute(ic.getMethod());
    switch (attr) {
      case REQUIRES_NEW_READ:
        return graphene.withTxReadReturn(tx -> {
          try {
            return ic.proceed();
          } catch (Exception e) {
            throw new IllegalStateException(e);
          }
        });
      case REQUIRED_WRITE:
        return graphene.joinTxWriteReturn(tx -> {
          try {
            return ic.proceed();
          } catch (Exception e) {
            throw new IllegalStateException(e);
          }
        });
      default:
        return graphene.joinTxReadReturn(tx -> {
          try {
            return ic.proceed();
          } catch (Exception e) {
            throw new IllegalStateException(e);
          }
        });
    }
  }

  private TransactionAttribute getTransactionAttribute(Method method) {
    Transaction tx = method.getAnnotation(Transaction.class);
    if (tx != null) {
      return tx.value();
    } else {
      tx = method.getDeclaringClass().getAnnotation(Transaction.class);
      if (tx == null) {
        throw new IllegalStateException("No Transaction annotation found");
      }
      return tx.value();
    }
  }
}
