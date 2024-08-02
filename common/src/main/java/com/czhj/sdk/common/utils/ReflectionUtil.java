package com.czhj.sdk.common.utils;


import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReflectionUtil {

    public static Method getDeclaredMethodWithTraversal(final Class<?> clazz, final String methodName, final Class<?>... parameterTypes) throws NoSuchMethodException {
        Preconditions.NoThrow.checkNotNull(methodName);
        Preconditions.NoThrow.checkNotNull(parameterTypes);

        Class<?> currentClass = clazz;

        while (currentClass != null) {
            try {
                return currentClass.getDeclaredMethod(methodName, parameterTypes);
            } catch (Throwable e) {
                currentClass = currentClass.getSuperclass();
            }
        }

        throw new NoSuchMethodException();
    }


    public static List<Method> getMethodWithTraversal(final Class<?> clazz) {
        Preconditions.NoThrow.checkNotNull(clazz);

        try {
            return Arrays.asList(clazz.getMethods());
        } catch (Throwable e) {

        }

        return null;
    }


    public static Map<String, String> getPrivateFields(final Class classType) {
        Field[] declaredFields = classType.getDeclaredFields();

        Map<String, String> declaredFieldMap = new HashMap<>(declaredFields.length);
        for (Field field : declaredFields) {
            declaredFieldMap.put(field.getName(), field.getType().getName());
        }

        return declaredFieldMap;
    }


    public static class MethodBuilder {

        private final Object mInstance;

        private final String mMethodName;

        private Class<?> mClass;


        private final List<Class<?>> mParameterClasses;

        private final List<Object> mParameters;
        private boolean mIsAccessible;
        private boolean mIsStatic;

        public MethodBuilder(final Object instance, final String methodName) {
            Preconditions.NoThrow.checkNotNull(methodName);

            mInstance = instance;
            mMethodName = methodName;

            mParameterClasses = new ArrayList<>();
            mParameters = new ArrayList<>();

            mClass = (instance != null) ? instance.getClass() : null;
        }


        public MethodBuilder(final Class cls, final String methodName) {
            Preconditions.NoThrow.checkNotNull(methodName);

            mIsStatic = true;
            mInstance = null;
            mMethodName = methodName;

            mParameterClasses = new ArrayList<>();
            mParameters = new ArrayList<>();

            mClass = cls;
        }


        public <T> MethodBuilder addParam(final Class<T> clazz, final T parameter) {
            Preconditions.NoThrow.checkNotNull(clazz);

            mParameterClasses.add(clazz);
            mParameters.add(parameter);

            return this;
        }


        public Object execute() throws Exception {
            final Class<?>[] classArray = new Class<?>[mParameterClasses.size()];
            final Class<?>[] parameterTypes = mParameterClasses.toArray(classArray);

            final Method method = getDeclaredMethodWithTraversal(mClass, mMethodName, parameterTypes);

            if (mIsAccessible) {
                method.setAccessible(true);
            }

            final Object[] parameters = mParameters.toArray();

            if (mIsStatic) {
                return method.invoke(null, parameters);
            } else {
                return method.invoke(mInstance, parameters);
            }
        }
    }
}
