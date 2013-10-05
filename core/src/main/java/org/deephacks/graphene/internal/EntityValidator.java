package org.deephacks.graphene.internal;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

public class EntityValidator {
    private Validator validator;

    /** API class for JSR 349 1.1 bean validation */
    private static final String JSR303_1_0_CLASSNAME = "javax.validation.Validation";
    private boolean valdiationPresent = false;

    public EntityValidator() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            cl.loadClass(JSR303_1_0_CLASSNAME);
        } catch (Exception e) {
            valdiationPresent = false;
        }
        valdiationPresent = true;
    }

    public void validate(Object entity) {
        validate(Arrays.asList(entity));
    }

    public void validate(Collection<Object> beans) {
        if (validator == null) {
            validator = Validation.buildDefaultValidatorFactory().getValidator();
        }

        for (Object bean : beans) {
            Set<ConstraintViolation<Object>> violations = validator.validate(bean);
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(getErrorMessage(violations), violations);
            }
        }
    }

    private String getErrorMessage(Set<ConstraintViolation<Object>> violations) {
        StringBuilder constraintFailureMessage = new StringBuilder();
        for (ConstraintViolation violation : violations) {
            if (constraintFailureMessage.length() != 0) {
                constraintFailureMessage.append(" and ");
            }
            constraintFailureMessage.append(violation.getRootBeanClass().getSimpleName()).append(" ");
            constraintFailureMessage.append(violation.getPropertyPath()).append(" ");
            constraintFailureMessage.append(violation.getMessage());
        }
        return constraintFailureMessage.toString();
    }

}
