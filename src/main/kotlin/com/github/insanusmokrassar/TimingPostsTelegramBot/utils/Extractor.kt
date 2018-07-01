package com.github.insanusmokrassar.TimingPostsTelegramBot.utils

import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import java.lang.reflect.InvocationTargetException

fun <T> initObject(className: String, params: IObject<Any>?): T {
    return params ?.let {
        extract<T>(className, it)
    } ?: {
        extract<T>(className)
    }()
}

/**
 * Return new instance of target class
 * @param path Path to package as path.to.package.java
 * *
 * @param <T> Target class (or interface)
 * *
 * @return New instance of target class
 * *
 * @throws IllegalArgumentException
</T> */
@Throws(IllegalArgumentException::class)
fun <T> extract(path: String, vararg constructorArgs: Any?): T {
    val targetClass = getClass<T>(path)
    targetClass.constructors.forEach {
        if (it.parameterTypes.size != constructorArgs.size) {
            return@forEach
        }
        try {
            return it.newInstance(*constructorArgs) as T
        } catch (e: InstantiationException) {
            throw IllegalArgumentException("Can't instantiate the instance of class: it may be interface or abstract class", e)
        } catch (e: IllegalAccessException) {
            throw IllegalArgumentException("Can't instantiate the instance of class: can't get access for instantiating it", e)
        } catch (e: InvocationTargetException) {
            return@forEach
        } catch (e: IllegalArgumentException) {
            return@forEach
        }

    }
    throw IllegalArgumentException("Can't find constructor for this args")
}

@Throws(IllegalArgumentException::class)
fun <T> getClass(path: String): Class<T> {
    try {
        val targetClass = Class.forName(path)
        return targetClass as Class<T>
    } catch (e: ClassNotFoundException) {
        throw IllegalArgumentException("Can't find class with this classPath: $path", e)
    }

}