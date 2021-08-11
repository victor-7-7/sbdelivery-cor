package ru.skillbranch.sbdelivery.aop

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.reflect.MethodSignature
import java.lang.reflect.Method
import java.util.regex.Pattern

@Aspect
class LogAspect {
    // https://www.lenar.io/logging-method-invocations-in-java-with-aspectj/
    // https://howtodoinjava.com/spring-aop/aspectj-after-returning-annotation-example/

    companion object {
        // https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html
        // Backslashes within string literals in Java source code are interpreted
        // as required by The Java™ Language Specification as either Unicode escapes
        // (section 3.3) or other character escapes (section 3.10.6) It is therefore
        // necessary to double backslashes in string literals that represent regular
        // expressions to protect them from interpretation by the Java bytecode compiler.
        // The string literal "\(hello\)" is illegal and leads to a compile-time error;
        // in order to match the string (hello) the string literal "\\(hello\\)" must be used
        val regex = Pattern.compile("\\(dishes=\\[.*").toRegex()
        const val replacement = "(dishes=[%%%список из блюд%%%])"
        const val tag = "AspectJ"
    }

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    @Before("execution(* *(..)) && @within(LogClassMethods)")
    fun doBeforeClassMethod(jp: JoinPoint) = doAction(jp)

    @Before("execution(* *(..)) && @within(LogThatFunc)")
    fun doBeforeFunction(jp: JoinPoint) = doAction(jp, false)

    // Не билдится:
    // org.aspectj.weaver.BCException: Unable to find Asm for stackmap generation
    // (Looking for 'aj.org.objectweb.asm.ClassReader'). Stackmap generation for
    // woven code is required to avoid verify errors on a Java 1.7 or higher runtime
/*
    @AfterReturning(pointcut="execution(* *(..)) && @within(LogClassMethods)", returning="retVal")
    fun doAfterClassMethod(retVal: Any) = doAfter(retVal)

    @AfterReturning(pointcut="execution(* *(..)) && @within(LogThatFunc)", returning="retVal")
    fun doAfterFunction(retVal: Any) = doAfter(retVal)
*/

    private fun doAction(jp: JoinPoint, fromClass: Boolean = true) {
        if (jp.target != null) {
            val typeName = jp.target.javaClass.simpleName
            val preamble = if (fromClass) "Class: $typeName"
                else "Extension for: $typeName"
            val signature = jp.signature as MethodSignature
            val method: Method = signature.method
            val paramsNames = signature.parameterNames
            val args = jp.args
            var paramsStr = ""
            args.forEachIndexed { idx, v ->
                // Ищем в строке-значении параметра v вхождение подстроки "(dishes=[" и если находим,
                // то меняем оставшуюся часть (от вхождения и до конца v-строки) на заглушку.
                // Дело в том, что dishes-список движок не показывает полностью, а обрезает
                val pv = "$v".replace(regex, replacement)
                    .replace("ru.skillbranch.sbdelivery.screens.", "")
                    .replace("ru.skillbranch.sbdelivery.repository.", "")
                paramsStr += "[${paramsNames[idx]} = $pv]"
            }
            val retN = signature.returnType.simpleName
            Log.i(tag,  "$preamble | Method: ${method.name} | Params: $paramsStr | Return Type: $retN")
            Log.d(tag,  "=========================\n")
        }
    }

    /*private fun doAfter(retVal: Any) {
        if (retVal is Unit) {
            Log.d(TAG,  "=============0============\n")
        } else {
            Log.d(TAG,  "--------\n")
            Log.i(TAG,  "Return value: ${gson.toJson(retVal)}")
            Log.d(TAG,  "=============1===========\n")
        }
    }*/
}

fun String.doMoreClean() = replace(LogAspect.regex, LogAspect.replacement)
    .replace("ru.skillbranch.sbdelivery.screens.", "")
    .replace("ru.skillbranch.sbdelivery.repository.", "")

// @Retention(AnnotationRetention.RUNTIME) // <- default
@Target(AnnotationTarget.CLASS)
annotation class LogClassMethods

// Хотел аннотировать котлиновские функции-расширения. Но
// аспект расширений не видит и игнорит эту аннотацию.
// Поэтому пришлось ручками раскидывать логи по местам
@Target(AnnotationTarget.FUNCTION)
annotation class LogThatFunc

