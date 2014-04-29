package org.deephacks.graphene.cdi;


import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.io.Serializable;
import java.lang.reflect.Method;

import static org.deephacks.graphene.TransactionManager.joinTxReturn;
import static org.deephacks.graphene.TransactionManager.withTxReturn;

@Transaction
@Interceptor
class TransactionInterceptor implements Serializable {

  @AroundInvoke
  public Object aroundInvoke(final InvocationContext ic) throws Exception {
    TransactionAttribute attr = getTransactionAttribute(ic.getMethod());
    if (attr == TransactionAttribute.REQUIRES_NEW) {
      return withTxReturn(tx -> {
        try {
          return ic.proceed();
        } catch (Exception e) {
          throw new IllegalStateException(e);
        }
      });
    } else {
      return joinTxReturn(tx -> {
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
