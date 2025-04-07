package org.starficz.UIFramework

import org.starficz.UIFramework.ReflectionUtils.invoke
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

internal object ReflectionUtils {
    private val fieldClass =
        Class.forName("java.lang.reflect.Field", false, Class::class.java.classLoader)

    private val setFieldHandle =
        fieldClass.getHandle("set", Void.TYPE, Any::class.java, Any::class.java)
    private val getFieldHandle =
        fieldClass.getHandle("get", Any::class.java, Any::class.java)
    private val getFieldTypeHandle =
        fieldClass.getHandle("getType", Class::class.java)
    private val getFieldNameHandle =
        fieldClass.getHandle("getName", String::class.java)
    private val setFieldAccessibleHandle =
        fieldClass.getHandle("setAccessible", Void.TYPE, Boolean::class.javaPrimitiveType)

    private val methodClass =
        Class.forName("java.lang.reflect.Method", false, Class::class.java.classLoader)
    private val getMethodNameHandle =
        methodClass.getHandle("getName", String::class.java)
    private val invokeMethodHandle =
        methodClass.getHandle("invoke", Any::class.java, Any::class.java, Array<Any>::class.java)
    private val setMethodAccessibleHandle =
        methodClass.getHandle("setAccessible", Void.TYPE, Boolean::class.javaPrimitiveType)
    private val getMethodReturnHandle =
        methodClass.getHandle("getReturnType", Class::class.java)
    private val getMethodParametersHandle =
        methodClass.getHandle("getParameterTypes", arrayOf<Class<*>>().javaClass)

    private val constructorClass =
        Class.forName("java.lang.reflect.Constructor", false, Class::class.java.classLoader)
    private val getConstructorParametersHandle =
        constructorClass.getHandle("getParameterTypes", arrayOf<Class<*>>().javaClass)

    internal fun Class<*>.getHandle(name: String, returnType: Class<*>, vararg paramTypes: Class<*>?): MethodHandle {
        return MethodHandles.lookup().findVirtual(this, name, MethodType.methodType(returnType, paramTypes))
    }

    internal fun Any.get(reflectedField: ReflectedField): Any? {
        return reflectedField.get(this)
    }

    internal fun Any.get(name: String? = null, type: Class<*>? = null): Any?{
        val target = if(this is BoxedUIElement) this.boxedElement else this
        val reflectedFields = target.getFieldsMatching(name, assignableFromType=type)
        if (reflectedFields.isEmpty())
            throw IllegalArgumentException("Field: $name of type: $type not exist for class: $target")
        else if (reflectedFields.size > 1)
            throw IllegalArgumentException("Field: $name of type: $type is ambiguous for class: $target")
        else return reflectedFields[0].get(target)
    }

    internal fun Any.set(reflectedField: ReflectedField, value: Any?) {
        reflectedField.set(this, value)
    }

    internal fun Any.set(name: String? = null, value: Any?) {
        val target = if(this is BoxedUIElement) this.boxedElement else this
        val valueType = value!!::class.javaPrimitiveType ?: value::class.java
        val reflectedFields = target.getFieldsMatching(name, assignableToType=valueType)
        if (reflectedFields.isEmpty())
            throw IllegalArgumentException("Field: $name of type: $valueType not exist for class: $target")
        else if (reflectedFields.size > 1)
            throw IllegalArgumentException("Field: $name of type: $valueType is ambiguous for class: $target")
        else return reflectedFields[0].set(target, value)
    }

    internal fun Any.getFieldsMatching(
        name: String? = null,
        type: Class<*>? = null,
        assignableFromType: Class<*>? = null,
        assignableToType: Class<*>? = null
    ): List<ReflectedField> {
        val target = if(this is BoxedUIElement) this.boxedElement else this
        return target::class.java.getFieldsMatching(name, type, assignableFromType, assignableToType)
    }

    internal fun Class<*>.getFieldsMatching(
        name: String? = null,
        type: Class<*>? = null,
        assignableFromType: Class<*>? = null,
        assignableToType: Class<*>? = null
    ): List<ReflectedField> {
        val fieldInstances: Set<Any> = (this.declaredFields + this.fields).toSet()
        return fieldInstances.filter { field ->
            val fieldType = if (type != null || assignableToType != null || assignableFromType != null){
                getFieldTypeHandle.invoke(field) as Class<*>
            } else null

            val nameMatches = name?.let { getFieldNameHandle.invoke(field) == it } != false
            val typeMatches = type?.let { fieldType!! == it } != false
            val assignableToTypeMatches = assignableToType?.let { fieldType!!.isAssignableFrom(it) } != false
            val assignableFromTypeMatches = assignableFromType?.isAssignableFrom(fieldType!!) != false

            // filter out object fields that arnt specifically matched for
            val objectFilter = fieldType?.let { it != Object::class.java || name != null || type != null } != false

            nameMatches && typeMatches && assignableToTypeMatches && assignableFromTypeMatches && objectFilter
        }.map { ReflectedField(it) }
    }

    internal fun Any.getFieldsWithMethodsMatching(
        methodName: String? = null,
        methodReturnType: Class<*>? = null,
        numOfMethodParams: Int? = null,
        methodParameterTypes: Array<Class<*>>? = null
    ): List<ReflectedField> {
        val target = if(this is BoxedUIElement) this.boxedElement else this
        return target::class.java.getFieldsWithMethodsMatching(methodName, methodReturnType, numOfMethodParams, methodParameterTypes)
    }

    internal fun Class<*>.getFieldsWithMethodsMatching(
        methodName: String? = null,
        methodReturnType: Class<*>? = null,
        numOfMethodParams: Int? = null,
        methodParameterTypes: Array<Class<*>>? = null
    ): List<ReflectedField> {
        val fieldInstances: Set<Any> = (this.declaredFields + this.fields).toSet()
        return fieldInstances.filter { fieldInstance ->
            val fieldType = getFieldTypeHandle.invoke(fieldInstance) as Class<*>?
            fieldType?.getMethodsMatching(methodName, methodReturnType, numOfMethodParams, methodParameterTypes)?.isNotEmpty() == true
        }.map { ReflectedField(it) }
    }

    internal fun Any.invoke(reflectedMethod: ReflectedMethod, vararg args: Any?): Any?{
        return reflectedMethod.invoke(this, *args)
    }

    internal fun Any.invoke(name: String, vararg args: Any?): Any?{
        val target = if(this is BoxedUIElement) this.boxedElement else this
        val paramTypes = args.map { arg -> arg!!::class.javaPrimitiveType ?: arg::class.java }.toTypedArray()
        val reflectedMethods = target.getMethodsMatching(name, parameterTypes=paramTypes)
        if (reflectedMethods.isEmpty())
            throw IllegalArgumentException("Parameters: $paramTypes for method: $name does not exist for class: $target")
        else if (reflectedMethods.size > 1)
            throw IllegalArgumentException("Parameters: $paramTypes are ambiguous for method: $name for class: $target")
        else return reflectedMethods[0].invoke(target, *args)
    }

    @Deprecated("Static methods should be directly called with the reflectedMethod",
        ReplaceWith("ReflectedMethod.invoke(null, args)"))
    internal fun Class<*>.invoke(reflectedMethod: ReflectedMethod, vararg args: Any?): Any?{
        return reflectedMethod.invoke(null, *args)
    }

    internal fun Class<*>.invoke(name: String, vararg args: Any?): Any?{
        val paramTypes = args.map { arg -> arg!!::class.javaPrimitiveType ?: arg::class.java }.toTypedArray()
        val reflectedMethods = this.getMethodsMatching(name, parameterTypes=paramTypes)
        if (reflectedMethods.isEmpty())
            throw IllegalArgumentException("Parameters: $paramTypes for method: $name does not exist for class: $this")
        else if (reflectedMethods.size > 1)
            throw IllegalArgumentException("Parameters: $paramTypes are ambiguous for method: $name on class: $this")
        else return reflectedMethods[0].invoke(null, *args)
    }

    internal fun Any.getMethodsMatching(
        name: String? = null,
        returnType: Class<*>? = null,
        numOfParams: Int? = null,
        parameterTypes: Array<Class<*>>? = null
    ): List<ReflectedMethod> {
        val target = if(this is BoxedUIElement) this.boxedElement else this
        return target::class.java.getMethodsMatching(name, returnType, numOfParams, parameterTypes)
    }

    internal fun Class<*>.getMethodsMatching(
        name: String? = null,
        returnType: Class<*>? = null,
        numOfParams: Int? = null,
        parameterTypes: Array<Class<*>>? = null
    ): List<ReflectedMethod> {
        val instancesOfMethods: Set<Any> = (this.declaredMethods + this.methods).toSet()
        return instancesOfMethods.filter { method ->
            val methodParamTypes = getMethodParametersHandle.invoke(method) as Array<Class<*>>
            // check that the name of the method is correct (if provided)
            (name?.let { getMethodNameHandle.invoke(method) == it } != false &&
            // check that the return of the method can be assigned to what we want (if provided)
            returnType?.isAssignableFrom(getMethodReturnHandle.invoke(method) as Class<*>) != false &&
            // check that the number of parameters matches (if provided)
            numOfParams?.let { methodParamTypes.size == it } != false &&
            // finally check if the input params can be assigned can be
            // assigned to all the types the method is expecting.
            parameterTypes?.let { inputParams ->
                inputParams.size == methodParamTypes.size &&
                inputParams.zip(methodParamTypes).all { (inputParam, methodParam) ->
                    methodParam.isAssignableFrom(inputParam)
                }
            } != false)
        }.map{ ReflectedMethod(it) }
    }

    internal fun Any.hasFieldOfName(name: String, contains: Boolean = false): Boolean {
        val target = if(this is BoxedUIElement) this.boxedElement else this
        val fieldInstances: Array<out Any> = target.javaClass.declaredFields
        return if (!contains) {
            fieldInstances.any { getFieldNameHandle.invoke(it) == name }
        } else {
            fieldInstances.any { (getFieldNameHandle.invoke(it) as String).contains(name) }
        }

    }

    internal fun Any.hasMethodOfName(name: String, contains: Boolean = false): Boolean {
        val target = if(this is BoxedUIElement) this.boxedElement else this
        val instancesOfMethods: Array<out Any> = target.javaClass.declaredMethods

        return if (!contains) {
            instancesOfMethods.any { getMethodNameHandle.invoke(it) == name }
        } else {
            instancesOfMethods.any { (getMethodNameHandle.invoke(it) as String).contains(name) }
        }
    }

    internal fun Any.hasConstructorOfParameters(vararg parameterTypes: Class<*>): Boolean {
        val target = if(this is BoxedUIElement) this.boxedElement else this
        val instancesOfConstructors: Array<out Any> = target.javaClass.declaredConstructors

        return instancesOfConstructors.any {
            (getMethodNameHandle.invoke(it) as Array<*>).contentEquals(parameterTypes)
        }
    }

    internal fun getConstructorHandle(clazz: Class<*>, vararg arguments: Class<*>): MethodHandle {
        return MethodHandles.lookup().findConstructor(clazz, MethodType.methodType(Void.TYPE, arguments))
    }

    internal fun Class<*>.instantiate(vararg args: Any?): Any? {
        val paramTypes = args.map { arg -> arg!!::class.javaPrimitiveType ?: arg::class.java }.toTypedArray()
        val constructorHandle = getConstructorHandle(clazz=this, *paramTypes)
        return constructorHandle.invokeWithArguments(args.toList())
    }


    class ReflectedField(val field: Any) {
        fun get(instance: Any?): Any? {
            val target = if(instance is BoxedUIElement) instance.boxedElement else instance
            setFieldAccessibleHandle.invoke(field, true)
            return getFieldHandle.invoke(field, target)
        }

        fun set(instance: Any?, value: Any?) {
            val target = if(instance is BoxedUIElement) instance.boxedElement else instance
            setFieldAccessibleHandle.invoke(field, true)
            setFieldHandle.invoke(field, target, value)
        }

        val type: Class<*> = getFieldTypeHandle.invoke(field) as Class<*>
        val name: String = getFieldNameHandle.invoke(field) as String
    }

    class ReflectedMethod(val method: Any) {
        fun invoke(instance: Any?, vararg arguments: Any?): Any? {

            val target = if(instance is BoxedUIElement) instance.boxedElement else instance
            setMethodAccessibleHandle.invoke(method, true)
            return invokeMethodHandle.invoke(method, target, arguments)
        }

        val returnType: Class<*>? = getMethodReturnHandle.invoke(method) as Class<*>?
        val name: String = getMethodNameHandle.invoke(method) as String
        val parameterTypes: Array<Class<*>> = getMethodParametersHandle.invoke(method) as Array<Class<*>>
    }
}