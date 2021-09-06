package net.auoeke.wheel.util

import net.auoeke.extensions.type
import net.auoeke.reflect.Invoker
import org.codehaus.groovy.runtime.callsite.CallSite

@Suppress("NAME_SHADOWING")
object GroovyUtil {
    private fun sites(type: Class<*>?): List<CallSite> {
        return listOf(*Invoker.invoke(Invoker.findStatic(type, "\$getCallSiteArray", type<Array<CallSite>>())))
    }

    fun site(type: Class<*>?, index: Int): CallSite {
        return this.sites(type)[index]
    }

    fun property(sites: List<CallSite>, o: Any?, vararg indexes: Int): Any? {
        var o = o

        indexes.forEach {
            o = sites[it].callGetProperty(o)
        }

        return o
    }

    fun call(sites: List<CallSite>, o: Any?, vararg indexes: Int): Any? {
        var o = o

        indexes.forEach {
            o = sites[it].call(o)
        }

        return o
    }
}
