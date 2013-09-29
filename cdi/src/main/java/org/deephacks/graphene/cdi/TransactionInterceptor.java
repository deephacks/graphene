package org.deephacks.graphene.cdi;


import org.deephacks.graphene.EntityRepository;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.io.Serializable;

@Transaction
@Interceptor
class TransactionInterceptor implements Serializable {
    private static final EntityRepository repository = new EntityRepository();

    @AroundInvoke
    public Object aroundInvoke(final InvocationContext ic) throws Exception {
        try {
            boolean initalTx = ThreadLocalManager.peek(com.sleepycat.je.Transaction.class) == null;
            if (initalTx) {
                ThreadLocalManager.push(com.sleepycat.je.Transaction.class, repository.getTx());
            }
            Object result = ic.proceed();
            if (initalTx) {
                ThreadLocalManager.clear(com.sleepycat.je.Transaction.class);
                repository.commit();
            }
            return result;
        } catch (Exception e) {
            try {
                repository.rollback();
            } catch (Exception e1) {
                throw new IllegalStateException(e1);
            }
            throw e;
        }
    }
}


