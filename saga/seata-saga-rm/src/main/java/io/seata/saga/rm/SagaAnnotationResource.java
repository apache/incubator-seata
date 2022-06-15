package io.seata.saga.rm;

import java.lang.reflect.Method;

/**
 * Saga annotation mode resource (Only register application as a saga annotation mode resource)
 *
 * @author ruishansun
 */
public class SagaAnnotationResource extends SagaResource {

    private Object targetBean;

    private String actionName;

    private Method commitMethod;

    private Method compensationMethod;

    private String compensationMethodName;

    private Class<?>[] compensationArgsClasses;

    private String[] phaseTwoCompensationKeys;

    public Object getTargetBean() {
        return targetBean;
    }

    public void setTargetBean(Object targetBean) {
        this.targetBean = targetBean;
    }

    public String getActionName() {
        return actionName;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

    public Method getCommitMethod() {
        return commitMethod;
    }

    public void setCommitMethod(Method commitMethod) {
        this.commitMethod = commitMethod;
    }

    public Method getCompensationMethod() {
        return compensationMethod;
    }

    public void setCompensationMethod(Method compensationMethod) {
        this.compensationMethod = compensationMethod;
    }

    public String getCompensationMethodName() {
        return compensationMethodName;
    }

    public void setCompensationMethodName(String compensationMethodName) {
        this.compensationMethodName = compensationMethodName;
    }

    public Class<?>[] getCompensationArgsClasses() {
        return compensationArgsClasses;
    }

    public void setCompensationArgsClasses(Class<?>[] compensationArgsClasses) {
        this.compensationArgsClasses = compensationArgsClasses;
    }

    public String[] getPhaseTwoCompensationKeys() {
        return phaseTwoCompensationKeys;
    }

    public void setPhaseTwoCompensationKeys(String[] phaseTwoCompensationKeys) {
        this.phaseTwoCompensationKeys = phaseTwoCompensationKeys;
    }
}
