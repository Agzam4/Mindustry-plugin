package agzam4.utils;

import java.util.Arrays;

import arc.util.Nullable;

public class Strs {

	public static String format(String text, Object... args) {
        if(args.length > 0){
            StringBuilder out = new StringBuilder(text.length() + args.length*2);
            int argi = 0;
            for(int i = 0; i < text.length(); i++){
                char c = text.charAt(i);
                if(c == '@' && argi < args.length){
                    out.append(stringify(args[argi++]));
                }else if(c == '$' && argi < args.length){
                	// TODO: Escaped
                    out.append(stringify(args[argi++]));
                }else{
                    out.append(c);
                }
            }

            return out.toString();
        }
        return text;
    }

	public static String stringify(Object o){
        if(o instanceof Object[]) return Arrays.deepToString((Object[])o);
        else if(o instanceof int[]) return Arrays.toString((int[])o);
        else if(o instanceof long[]) return Arrays.toString((long[])o);
        else if(o instanceof float[]) return Arrays.toString((float[])o);
        else if(o instanceof double[]) return Arrays.toString((double[])o);
        else if(o instanceof char[]) return Arrays.toString((char[])o);
        else if(o instanceof short[]) return Arrays.toString((short[])o);
        else if(o instanceof byte[]) return Arrays.toString((byte[])o);
        else if(o instanceof boolean[]) return Arrays.toString((boolean[])o);
        return String.valueOf(o);
    }

	public static String escape(@Nullable String src){
        if(src == null) return null;

        for(int i = 0; i < src.length(); i++){
            if(getEscapedChar(src.charAt(i)) != null){
                StringBuilder sb = new StringBuilder();
                if(i > 0) sb.append(src, 0, i);
                return doEscapeString(sb, src, i);
            }
        }
        return src;
    }
    
    private static String doEscapeString(StringBuilder sb, String src, int cur){
        int start = cur;
        for(int i = cur; i < src.length(); i++){
            String escaped = getEscapedChar(src.charAt(i));
            if(escaped != null){
                sb.append(src, start, i);
                sb.append(escaped);
                start = i + 1;
            }
        }
        sb.append(src, start, src.length());
        return sb.toString();
    }

    public static String getEscapedChar(char c){
        switch(c){
            case '\"': return "\\\"";
            case '\t': return "\\t";
            case '\n': return "\\n";
            case '\r': return "\\r";
            case '\f': return "\\f";
            case '\b': return "\\b";
            case '\\': return "\\\\";
            default: return null;
        }
    }
    
}
