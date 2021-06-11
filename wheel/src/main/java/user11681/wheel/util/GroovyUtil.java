package user11681.wheel.util;

import java.util.Arrays;
import java.util.List;
import org.codehaus.groovy.runtime.callsite.CallSite;
import user11681.reflect.Invoker;
import user11681.uncheck.Uncheck;

public class GroovyUtil {
    public static List<CallSite> sites(Class<?> type) {
        return Arrays.asList(Invoker.invoke(Invoker.findStatic(type, "$getCallSiteArray", CallSite[].class)));
    }

    public static CallSite site(Class<?> type, int index) {
        return sites(type).get(index);
    }

    public static Object property(List<CallSite> sites, Object object, int... indexes) {
        for (int index : indexes) {
            Object javaBad = object;
            object = Uncheck.handle(() -> sites.get(index).callGetProperty(javaBad));
        }

        return object;
    }

    public static Object call(List<CallSite> sites, Object object, int... indexes) {
        for (int index : indexes) {
            Object javaBad = object;
            object = Uncheck.handle(() -> sites.get(index).call(javaBad));
        }

        return object;
    }
}
