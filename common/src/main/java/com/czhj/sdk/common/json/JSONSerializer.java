package com.czhj.sdk.common.json;

import android.annotation.SuppressLint;
import android.text.TextUtils;

import com.czhj.sdk.logger.SigmobLog;

import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JSONSerializer {

    @SuppressLint("UseSparseArrays")
    public static String Serialize(Object obj) throws CyclicObjectException {
        return Serialize(obj, new HashMap<Integer, Object>(), null, false, false); // could be SparseArray
    }

    public static String Serialize(Object obj, String serializeName) throws CyclicObjectException {
        return Serialize(obj, new HashMap<Integer, Object>(), serializeName, false, false); // could be SparseArray
    }

    public static String Serialize(Object obj, String serializeName, boolean isHump) throws CyclicObjectException {
        return Serialize(obj, new HashMap<Integer, Object>(), serializeName, isHump, false); // could be SparseArray
    }

    private static final String[] REPLACEMENT_CHARS;
    private static final String[] HTML_SAFE_REPLACEMENT_CHARS;

    static {
        REPLACEMENT_CHARS = new String[128];
        for (int i = 0; i <= 0x1f; i++) {
            REPLACEMENT_CHARS[i] = String.format("\\u%04x", i);
        }
        REPLACEMENT_CHARS['"'] = "\\\"";
        REPLACEMENT_CHARS['\\'] = "\\\\";
        REPLACEMENT_CHARS['\t'] = "\\t";
        REPLACEMENT_CHARS['\b'] = "\\b";
        REPLACEMENT_CHARS['\n'] = "\\n";
        REPLACEMENT_CHARS['\r'] = "\\r";
        REPLACEMENT_CHARS['\f'] = "\\f";
        HTML_SAFE_REPLACEMENT_CHARS = REPLACEMENT_CHARS.clone();
        HTML_SAFE_REPLACEMENT_CHARS['<'] = "\\u003c";
        HTML_SAFE_REPLACEMENT_CHARS['>'] = "\\u003e";
        HTML_SAFE_REPLACEMENT_CHARS['&'] = "\\u0026";
        HTML_SAFE_REPLACEMENT_CHARS['='] = "\\u003d";
        HTML_SAFE_REPLACEMENT_CHARS['\''] = "\\u0027";
    }

    public static String Serialize(Object obj, String serializeName, boolean isHump, boolean isReplace) throws CyclicObjectException {
        return Serialize(obj, new HashMap<Integer, Object>(), serializeName, isHump, isReplace); // could be SparseArray
    }

    private static Pattern linePattern = Pattern.compile("_(\\w)");

    private static String lineToHump(String str) {
        Matcher matcher = linePattern.matcher(str);

        if (matcher.find()) {
            str = str.toLowerCase();
            matcher = linePattern.matcher(str);

            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                matcher.appendReplacement(sb, matcher.group(1).toUpperCase());
            }
            matcher.appendTail(sb);
            return sb.toString();
        }
        return str;

    }

    private static String Serialize(Object obj, HashMap<Integer, Object> visited, String serializeName, boolean isHump, boolean isReplace) throws CyclicObjectException {

        if (null == obj) {
            return "null";
        }
        String content = null;

        Class c = obj.getClass();
        if (obj instanceof String) {
            String str = (String) obj;
            content = SerializeString(str);
        } else if (c.isPrimitive() ||
                c == Boolean.class ||
                c == Short.class ||
                c == Integer.class ||
                c == Long.class ||
                c == Float.class ||
                c == Double.class ||
                c == Byte.class ||
                c == Character.class) {
            content = SerializePrimitive(obj);
        } else if (!isReplace && visited.get(obj.hashCode()) != null) {
            return "null";
        }

        visited.put(obj.hashCode(), obj);

        if (TextUtils.isEmpty(content)) {

            if (obj instanceof List) {
                List l = (List) obj;
                content = SerializeArray(l.toArray(new Object[l.size()]), visited, isHump, isReplace);
            } else if (obj instanceof Map) {
                content = SerializeMap((Map) obj, visited, isHump, isReplace);
            } else if (c.isArray()) {
                content = SerializeArray(obj, visited, isHump, isReplace);
            } else {
                content = SerializeObject(obj, visited, isHump, isReplace);
            }
        }

        if (TextUtils.isEmpty(serializeName)) {

            return content;
        } else {

            StringBuilder stringBuilder = new StringBuilder("{");


            if (isHump) {
                serializeName = lineToHump(serializeName);
            }

            stringBuilder.append(SerializeString(serializeName));
            stringBuilder.append(':');
            stringBuilder.append(content);
            stringBuilder.append('}');
            return stringBuilder.toString();
        }

    }


    private static String SerializeString(String value) {
        String[] replacements = HTML_SAFE_REPLACEMENT_CHARS;
        Writer out = new StringWriter();
        try {
            out.write('\"');
            int last = 0;
            int length = value.length();
            for (int i = 0; i < length; i++) {
                char c = value.charAt(i);
                String replacement;
                if (c < 128) {
                    replacement = replacements[c];
                    if (replacement == null) {
                        continue;
                    }
                } else if (c == '\u2028') {
                    replacement = "\\u2028";
                } else if (c == '\u2029') {
                    replacement = "\\u2029";
                } else {
                    continue;
                }
                if (last < i) {
                    out.write(value, last, i - last);
                }
                out.write(replacement);
                last = i + 1;
            }
            if (last < length) {
                out.write(value, last, length - last);
            }
            out.write('\"');
        } catch (Throwable e) {
            SigmobLog.e("SerializeString", e);
//            throw new RuntimeException(e);
        }

        return out.toString();

    }

    private static String SerializePrimitive(Object obj) {
        return obj.toString();
    }


    private static String SerializeMap(Map obj, HashMap<Integer, Object> visited, boolean isHump, boolean isReplace) {
        StringBuilder sb = new StringBuilder();
        boolean comma = false;

        sb.append("{");
        for (Object oentry : obj.entrySet()) {
            Map.Entry e = (Map.Entry) oentry;

            String name = e.getKey().toString();
            Object value = e.getValue();


            if (comma) {
                sb.append(",");
            }


            String serializeName = SerializeString(name);

            if (isHump) {
                serializeName = lineToHump(serializeName);
            }

            sb.append(serializeName);
            sb.append(":");
            sb.append(Serialize(value, visited, null, isHump, isReplace));
            comma = true;

        }
        sb.append("}");

        return sb.toString();
    }

    private static String SerializeObject(Object obj, HashMap<Integer, Object> visited, boolean isHump, boolean isReplace) {

        StringBuilder sb = new StringBuilder();

        sb.append("{");
        Field[] fields = obj.getClass().getDeclaredFields();
        boolean comma = false;
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object value = field.get(obj);
                String name = field.getName();
                int fieldValue = field.getModifiers();//如：private、static、final等

                boolean isStatic = Modifier.isStatic(fieldValue);
                boolean isTransient = field.isAnnotationPresent(Transient.class);

                if (!name.startsWith("this$") && !isStatic && (null == value || !isTransient)) {

                    if (comma) {
                        sb.append(",");
                    }

                    String serializeName = SerializeString(name);
                    if (isHump) {
                        serializeName = lineToHump(serializeName);
                    }

                    sb.append(serializeName);
                    sb.append(":");
                    sb.append(Serialize(value, visited, null, isHump, isReplace));
                    comma = true;
                }
            } catch (Throwable e) {
                SigmobLog.e("json seriallize error", e);
                // field value exception.
            }
        }
        sb.append("}");

        return sb.toString();
    }

    private static String SerializeArray(Object arr, HashMap<Integer, Object> visited, boolean isHump, boolean isReplace) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");

        int l = Array.getLength(arr);
        for (int i = 0; i < l; i++) {
            sb.append(Serialize(Array.get(arr, i), visited, null, isHump, isReplace));
            if (i < l - 1) {
                sb.append(",");
            }
        }

        sb.append("]");
        return sb.toString();
    }

}
