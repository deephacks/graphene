package org.deephacks.graphene.cdi;


import org.deephacks.graphene.EntityRepository;
import org.deephacks.graphene.TransactionManager;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.io.Serializable;

@Transaction
@Interceptor
class TransactionInterceptor implements Serializable {
    private static final EntityRepository repository = new EntityRepository();
    private static final TransactionManager tm = repository.getTransactionManager();

    @AroundInvoke
    public Object aroundInvoke(final InvocationContext ic) throws Exception {
        try {

            boolean initalTx = tm.peek() == null;
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
}


