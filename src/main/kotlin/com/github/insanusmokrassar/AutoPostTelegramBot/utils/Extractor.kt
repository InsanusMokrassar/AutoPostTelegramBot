package com.github.insanusmokrassar.AutoPostTelegramBot.utils

import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import java.lang.reflect.InvocationTargetException

fun <T> initObject(
    className: String,
    params: IObject<Any>?,
    vararg classLoaders: ClassLoader = arrayOf(ClassLoader.getSystemClassLoader())
): T {
    val exceptions = mutableListOf<Exception>()
    classLoaders.forEach {
        classLoader ->
        try {
            return params ?.let {
                classLoader.extract<T>(className, it)
            } ?: {
                classLoader.extract<T>(className)
            }()
        } catch (e: Exception) {
            exceptions.add(e)
        }
    }
    throw IllegalArgumentException(exceptions.joinToString("\n") { it.message ?: "" })
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
fun <T> ClassLoader.extract(path: String, vararg constructorArgs: Any?): T {
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
fun <T> ClassLoader.getClass(path: String): Class<T> {
    try {
        val targetClass = loadClass(path)
        return targetClass as Class<T>
    } catch (e: ClassNotFoundException) {
        throw IllegalArgumentException("Can't find class with this classPath: $path", e)
    }

}