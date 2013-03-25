package com.Qunar;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;

public class IdReader {

    private static final String[] CONTAINER_LAYOUT_NAMES = //
    { "FrameLayout", "LinearLayout", "RelativeLayout", "TableLayout" };
    private static boolean replace_to_camel = false;
    private static boolean field_with_shortname = false;
    private static boolean viewgroup_parent = false;
    private static String sFileName;

    public static void main(String[] args) throws ParseException {
        Options options = new Options();
        // add t option
        options.addOption("src", true, "gen file src");
        options.addOption("r", false, "replace to camel");
        options.addOption("s", false, "field with short name");
        options.addOption("vp", false, "declare viewgroup as parent");
        CommandLineParser parser = new PosixParser();
        CommandLine cmd = parser.parse(options, args);
        if (cmd.hasOption("src")) {
            sFileName = cmd.getOptionValue("src");
        } else {
            System.out.println("at least one param of xml file name");
            return;
        }

        if (cmd.hasOption("r")) {
            replace_to_camel = true;
        }
        if (cmd.hasOption("s")) {
            field_with_shortname = true;
        }
        if (cmd.hasOption("vp")) {
            viewgroup_parent = true;
        }
        SAXBuilder builder = new SAXBuilder();
        try {
            Document doc = builder.build(sFileName);
            Element root = doc.getRootElement();
            readXml(root);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void readXml(Element element) {
        readIdAttr(element);
        List<Element> children = element.getChildren();
        for (Element child : children) {
            readXml(child);
        }
    }

    private static void readIdAttr(Element element) {
        Attribute idAttr = element.getAttribute("id",
                Namespace.getNamespace("android", "http://schemas.android.com/apk/res/android"));
        String name = element.getName();
        if (name.equals("include")) {
            Attribute layoutAttr = element.getAttribute("layout");
            // 暂时不支持android包下的inclue
            if (layoutAttr.getValue().startsWith("@android:layout/")) {
                return;
            }
            String includeFileName = layoutAttr.getValue().replace("@layout/", "").concat(".xml");
            File f = new File(sFileName);
            includeFileName = f.getParent() + File.separator + includeFileName;
            SAXBuilder builder = new SAXBuilder();
            try {
                Document doc = builder.build(includeFileName);
                Element root = doc.getRootElement();
                readXml(root);
            } catch (JDOMException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (idAttr != null) {
            StringBuilder sbBuilder = new StringBuilder();
            sbBuilder.append("@From(");
            String idStr = idAttr.getValue().substring(1).replace("id/", "R.id.").replace(':', '.').replace("+", "");
            sbBuilder.append(idStr);
            sbBuilder.append(")\n");
            sbBuilder.append("private ");
            if (field_with_shortname) {
                String[] names = name.split("\\.");
                if (names.length > 0) {
                    name = names[names.length - 1];
                }
            }
            if (viewgroup_parent) {
                for (String cname : CONTAINER_LAYOUT_NAMES) {
                    if (cname.equals(name)) {
                        name = "ViewGroup";
                    }
                }
            }
            sbBuilder.append(name + " ");
            String fieldName = idStr.substring(idStr.lastIndexOf(".") + 1);
            if (replace_to_camel && fieldName.contains("_")) {
                String[] names = fieldName.split("_");
                fieldName = "";
                for (String string : names) {
                    char[] chars = string.toCharArray();
                    chars[0] = Character.toUpperCase(chars[0]);
                    fieldName = fieldName + new String(chars);
                }
            }
            char[] chars = fieldName.toCharArray();
            chars[0] = Character.toLowerCase(chars[0]);
            sbBuilder.append(new String(chars));
            sbBuilder.append(";");
            System.out.println(sbBuilder.toString());
        }
    }
}