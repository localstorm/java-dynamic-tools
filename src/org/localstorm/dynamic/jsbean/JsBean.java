package org.localstorm.dynamic.jsbean;

import sun.org.mozilla.javascript.internal.NativeArray;
import sun.org.mozilla.javascript.internal.NativeObject;

import javax.script.*;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;

/**
 * @author localstorm
 *         Date: 11.12.13
 */
public class JsBean {

    private final static String INTERNAL_NATIVE_DATE = "sun.org.mozilla.javascript.internal.NativeDate";
    private final static ThreadLocal<Object> PARAMS_TL = new ThreadLocal<Object>();

    private List<String> jsLibPaths = new ArrayList<String>();
    private List<String> jsLibScripts = new ArrayList<String>();
    private Map<String, Object> jsBindings = new HashMap<String, Object>();
    private ScriptEngine js;

    public void init() throws ScriptException {
        ScriptEngineManager mgr = new ScriptEngineManager();
        this.js = mgr.getEngineByName("javascript");

        Bindings bindings = js.getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.put("$value", PARAMS_TL);
        for (Map.Entry<String, Object> e : jsBindings.entrySet()) {
            bindings.put(e.getKey(), e.getValue());
        }

        for (String lib : jsLibPaths) {
            try {
                js.eval(new FileReader(lib));
            } catch (IOException e) {
                throw new ScriptException(e);
            }
        }
        for (String lib : jsLibScripts) {
            js.eval(lib);
        }
    }

    /**
     * jsCall represent a JS code being executed.
     * Data is bound to $value.get() variable.
     */
    public <T> T processJava(String jsCall, Object data, Class<T> resultClass) throws ScriptException {
        if (js == null) {
            throw new IllegalStateException("Init method init() has never been called");
        }
        PARAMS_TL.set(data);
        return handleObjectResult(js.eval(jsCall), resultClass);
    }

    public <T> T processJson(String jsCall, String json, Class<T> resultClass) throws ScriptException {
        if (js == null) {
            throw new IllegalStateException("Init method init() has never been called");
        }
        PARAMS_TL.set(json);
        PARAMS_TL.set(js.eval("JSON.parse($value.get());"));
        Object o = js.eval(jsCall);
        return handleObjectResult(o, resultClass);
    }

    public void setJsLibPaths(List<String> jsLibPaths) {
        this.jsLibPaths = jsLibPaths;
    }

    public void addJsLibPaths(String... jsLibPaths) {
        Collections.addAll(this.jsLibPaths, jsLibPaths);
    }

    public void setJsLibScripts(List<String> jsLibScripts) {
        this.jsLibScripts = jsLibScripts;
    }

    public void addLibScripts(String... libScripts) {
        Collections.addAll(this.jsLibScripts, libScripts);
    }

    public void addBinding(String name, Object obj) {
        this.jsBindings.put(name, obj);
    }

    public void setJsBindings(Map<String, Object> jsBindings) {
        this.jsBindings = jsBindings;
    }

    @SuppressWarnings("unchecked")
    private <T> T handleObjectResult(Object o, Class<T> resultClass) {
        if (o == null) {
            return null;
        }

        if (resultClass.isAssignableFrom(Date.class)) {
            if (o instanceof Long) {
                return (T) new Date((Long) o);
            }

            if (o.getClass().getName().equals(INTERNAL_NATIVE_DATE)) {
                try {
                    Bindings bnd = js.createBindings();
                    bnd.put("$tmp", o);
                    Long time = ((Number) js.eval("$tmp.getTime()", bnd)).longValue();
                    return (T) new Date(time);
                }catch(ScriptException e) {
                    throw new RuntimeException("Unexpected case", e);
                }
            }
        }

        if (resultClass.isAssignableFrom(List.class)) {
            ArrayList<Object> objs = new ArrayList<Object>();
            handleCollectionResult(o, objs);
            return (T) objs;
        }

        if (resultClass.isAssignableFrom(Set.class)) {
            Set<Object> objs = new HashSet<Object>();
            handleCollectionResult(o, objs);
            return (T) objs;
        }

        if (resultClass.isAssignableFrom(Map.class)) {
            Map<String, Object> objs = new HashMap<String, Object>();
            handleMapResult(o, objs);
            return (T) objs;
        }

        if (resultClass.isArray()) {
            Class<?> comType = resultClass.getComponentType();
            if (o instanceof NativeArray) {
                return handleArrayResult((NativeArray) o, resultClass, comType);
            }
            if (comType.isAssignableFrom(o.getClass())) {
                Object value = comType.cast(o);
                Object array = Array.newInstance(comType, 1);
                Array.set(array, 0, value);
                return resultClass.cast(array);
            }
            throw new ClassCastException("Unable to interpret result: [" + o + "] and cast it to " + resultClass);
        }

        return resultClass.cast(o);
    }

    private <T> void handleCollectionResult(Object o, Collection<Object> objects) {
        if (o instanceof NativeArray) {
            for (Object aNa : (NativeArray) o) {
                objects.add(aNa);
            }
        } else {
            objects.add(o);
        }
    }

    private <T> void handleMapResult(Object o, Map<String, Object> objects) {
        if (o instanceof NativeArray) {
            int i = 0;
            for (Object aNa : (NativeArray) o) {
                objects.put(Integer.toString(i++), aNa);
            }
        }
        if (o instanceof NativeObject) {
            Set<Map.Entry<Object, Object>> entries = ((NativeObject) o).entrySet();
            for (Map.Entry<Object, Object> e : entries) {
                String key = (e.getKey() != null) ? e.getKey().toString() : null;
                Object value = e.getValue();
                if (value instanceof NativeArray) {
                    ArrayList<Object> list = new ArrayList<Object>();
                    handleCollectionResult(value, list);
                    objects.put(key, list);
                } else {
                    objects.put(key, value);
                }
            }
        } else {
            objects.put(null, o);
        }
    }

    private <T> T handleArrayResult(NativeArray na, Class<T> resultClass, Class<?> comType) {
        Object array = Array.newInstance(comType, na.size());
        int i = 0;
        for (Object aNa : na) {
            Array.set(array, i, aNa);
            i++;
        }
        return resultClass.cast(array);
    }

}
