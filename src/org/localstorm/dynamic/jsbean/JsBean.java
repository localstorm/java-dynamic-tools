package org.localstorm.dynamic.jsbean;

import sun.org.mozilla.javascript.internal.NativeArray;

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

    private <T> T handleObjectResult(Object o, Class<T> resultClass) {
        if (o == null) {
            return null;
        }
        if (!resultClass.isArray()) {
            return resultClass.cast(o);
        } else {
            Class<?> comType = resultClass.getComponentType();
            if (o instanceof NativeArray) {
                NativeArray na = (NativeArray) o;
                Object array = Array.newInstance(comType, na.size());
                int i = 0;
                for (Object aNa : na) {
                    Array.set(array, i, aNa);
                    i++;
                }
                return resultClass.cast(array);
            }
            if (comType.isAssignableFrom(o.getClass())) {
                Object value = comType.cast(o);
                Object array = Array.newInstance(comType, 1);
                Array.set(array, 0, value);
                return resultClass.cast(array);
            }
            throw new ClassCastException("Unable to interpret result: ["+o+"] and cast it to "+resultClass);
        }
    }

}
