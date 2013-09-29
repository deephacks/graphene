package org.deephacks.graphene.cdi;


import org.deephacks.graphene.EntityRepository;
import org.deephacks.graphene.TransactionManager;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.io.Serializable;
import java.lang.reflect.Method;

@Transaction
@Interceptor
class TransactionInterceptor implements Serializable {
    private static final EntityRepository repository = new EntityRepository();
    private static final TransactionManager tm = repository.getTransactionManager();

    @AroundInvoke
    public Object aroundInvoke(final InvocationContext ic) throws Exception {
        try {
            TransactionAttribute attr = getTransactionAttribute(ic.getMethod());
            boolean initalTx = false;
            if (attr == TransactionAttribute.REQUIRES_NEW || tm.peek() == null) {
                initalTx = true;
            }
            if (initalTx) {
                tm.beginTransaction();
            }
            Object result = ic.proceed();
            if (initalTx) {
                tm.commit();
            }
            return result;
        } catch (Throwable e) {
            try {
                tm.rollback();
            } catch (Exception e1) {
                throw new IllegalStateException(e1);
            }
            throw e;
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
