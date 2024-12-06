package com.xcu.context;

public class BaseContext {

    private static final ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    public static void saveUser(Long userId){
        threadLocal.set(userId);
    }

    public static Long getUserId(){
        return threadLocal.get();
    }

    public static void removeUser(){
        threadLocal.remove();
    }

}
