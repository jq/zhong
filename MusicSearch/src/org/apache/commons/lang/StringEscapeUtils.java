package org.apache.commons.lang;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

public class StringEscapeUtils {
	public static String unescapeHtml(String str) {
        if (str == null) {
            return null;
        }
        try {
            StringWriter writer = new StringWriter ((int)(str.length() * 1.5));
            unescapeHtml(writer, str);
            return writer.toString();
        } catch (IOException e) {
            //assert false;
            //should be impossible
            e.printStackTrace();
            return null;
        }
    }
	public static void unescapeHtml(Writer writer, String string) throws IOException {
        if (writer == null ) {
            throw new IllegalArgumentException ("The Writer must not be null.");
        }
        if (string == null) {
            return;
        }
        Entities.HTML40.unescape(writer, string);
    }
}
