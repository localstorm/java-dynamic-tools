import org.localstorm.dynamic.jsbean.JsBean;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author localstorm
 *         Date: 11.12.13
 */
public class JsBeanExample {

    public static void main(String[] args) throws Exception {
        JsBean jsb = new JsBean();
        jsb.addJsLibPaths("pretty-date.js");

        //jsb.addBindi`ng("stdout", System.out);
        //jsb.addBinding("stderr", System.err);
        jsb.init();

        Object value = jsb.processJava("prettyDate($value.get());", new Date(), String.class);
        System.out.println("Java string: " + value + " / "+value.getClass());

        value = jsb.processJava("prettyDate($value.get());", new Date(System.currentTimeMillis()-1000*3700), String.class);
        System.out.println("Java string: " + value + " / "+value.getClass());

        Integer[] values = jsb.processJson("$value.get().values", "{\"text\": \"someShit\", \"values\": [1, 2]}", Integer[].class);
        System.out.println("Java array: " + Arrays.toString(values) + " / " + values.getClass());

        Object num = jsb.processJson("$value.get().num", "{\"num\": 10, \"text\": \"someShit\", \"values\": [1, 2]}", Integer.class);
        System.out.println("Java number: " + num + " / " + num.getClass());

        List list = jsb.processJson("$value.get().values", "{\"num\": 10, \"text\": \"someShit\", \"values\": [1, 2]}", List.class);
        System.out.println("Java list: " + list + " / " + list.getClass());

        Map map = jsb.processJson("$value.get()", "{\"num\": 10, \"text\": \"someShit\", \"values\": [1, 2]}", Map.class);
        System.out.println("Java map: " + map + " / " + map.getClass());

        Date date = jsb.processJson("new Date()", null, Date.class);
        System.out.println("Java date: " + date + " / " + date.getClass());
    }
}
