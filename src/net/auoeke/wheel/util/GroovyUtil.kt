package net.auoeke.wheel.util

import net.auoeke.reflect.Invoker
import org.codehaus.groovy.runtime.callsite.CallSite
import java.util.*

@Suppress("NAME_SHADOWING")
object GroovyUtil {
    fun sites(type: Class<*>?): List<CallSite> {
        return listOf(*Invoker.invoke(Invoker.findStatic(type, "\$getCallSiteArray", Array<CallSite>::class.java)))
    }

    fun site(type: Class<*>?, index: Int): CallSite {
        return sites(type)[index]
    }

    fun property(sites: List<CallSite>, o: Any?, vararg indexes: Int): Any? {
        var o = o

        for (index in indexes) {
            o = sites[index].callGetProperty(o)
        }

        return o
    }

    fun call(sites: List<CallSite>, o: Any?, vararg indexes: Int): Any? {
        var o = o

        for (index in indexes) {
            o = sites[index].call(o)
        }

        return o
    }
}
