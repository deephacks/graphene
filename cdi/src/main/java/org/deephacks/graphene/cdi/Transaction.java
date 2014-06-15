package org.deephacks.graphene.cdi;

import javax.enterprise.util.Nonbinding;
import javax.interceptor.InterceptorBinding;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target({METHOD, TYPE})
@Documented
@InterceptorBinding
public @interface Transaction {
  @Nonbinding TransactionAttribute value() default TransactionAttribute.REQUIRED_READ;
}
