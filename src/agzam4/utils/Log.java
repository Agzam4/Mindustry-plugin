package agzam4.utils;

import java.util.Stack;

import arc.struct.ObjectMap;
import arc.struct.OrderedMap;
import arc.util.ColorCodes;

public class Log {

	private static ObjectMap<String, Colors> colors = new ObjectMap<>();
	
	static {
		for (var c : Colors.values()) {
			colors.put(c.name, c);
		}
		reset();
		
	    ColorCodes.flush = "\033[H\033[2J";
	    ColorCodes.reset = "\u001B[0m";
	    ColorCodes.bold = "\u001B[1m";
	    ColorCodes.italic = "\u001B[3m";
	    ColorCodes.underline = "\u001B[4m";
	    ColorCodes.black = "\u001B[30m";
	    ColorCodes.red = "\u001B[31m";
	    ColorCodes.green = "\u001B[32m";
	    ColorCodes.yellow = "\u001B[33m";
	    ColorCodes.blue = "\u001B[34m";
	    ColorCodes.purple = "\u001B[35m";
	    ColorCodes.cyan = "\u001B[36m";
	    ColorCodes.lightBlack = "\u001b[90m";
	    ColorCodes.lightRed = "\u001B[91m";
	    ColorCodes.lightGreen = "\u001B[92m";
	    ColorCodes.lightYellow = "\u001B[93m";
	    ColorCodes.lightBlue = "\u001B[94m";
	    ColorCodes.lightMagenta = "\u001B[95m";
	    ColorCodes.lightCyan = "\u001B[96m";
	    ColorCodes.lightWhite = "\u001b[97m";
	    ColorCodes.white = "\u001B[37m";

	    ColorCodes.backDefault = "\u001B[49m";
	    ColorCodes.backRed = "\u001B[41m";
	    ColorCodes.backGreen = "\u001B[42m";
	    ColorCodes.backYellow = "\u001B[43m";
	    ColorCodes.backBlue = "\u001B[44m";

        OrderedMap<String, String> map = OrderedMap.of(
        "bd", ColorCodes.backDefault,
        "br", ColorCodes.backRed,
        "bg", ColorCodes.backGreen,
        "by", ColorCodes.backYellow,
        "bb", ColorCodes.backBlue,

        "ff", ColorCodes.flush,
        "fr", ColorCodes.reset,
        "fb", ColorCodes.bold,
        "fi", ColorCodes.italic,
        "fu", ColorCodes.underline,
        "k", ColorCodes.black,
        "lk", ColorCodes.lightBlack,
        "lw", ColorCodes.lightWhite,
        "r", ColorCodes.red,
        "g", ColorCodes.green,
        "y", ColorCodes.yellow,
        "b", ColorCodes.blue,
        "p", ColorCodes.purple,
        "c", ColorCodes.cyan,
        "lr", ColorCodes.lightRed,
        "lg", ColorCodes.lightGreen,
        "ly", ColorCodes.lightYellow,
        "lm", ColorCodes.lightMagenta,
        "lb", ColorCodes.lightBlue,
        "lc", ColorCodes.lightCyan,
        "w", ColorCodes.white
        );
        String[] values = map.values().toSeq().toArray(String.class);
        for (int i = 0; i < ColorCodes.values.length; i++) {
        	ColorCodes.values[i] = values[i];
		}
	}
	
	public static void init() {
		
	}
	
	public static void reset() {
		arc.util.Log.useColors = true;
	}
	
	public static enum Colors {

		red(31),
		green(32),
		royal(34),
		yellow(33),
		magenta(35),
		gray(90),
		lime(92),
		lightYellow(93),
		blue(94),
		cyan(96),
		reset(0);

		String color, name = name();

		private Colors(int code) {
			color = code == 0 ? "\033[0m" : "\033[1;" + code + "m";
			StringBuilder name = new StringBuilder();
			for (int i = 0; i < this.name.length(); i++) {
				char c = this.name.charAt(i);
				if(Character.isUpperCase(c)) {
					name.append('-');
					c = Character.toLowerCase(c);
				}
				name.append(c);
			}
			this.name = name.toString();
		}

		private Colors(String str) {
			color = "&" + str;
			StringBuilder name = new StringBuilder();
			for (int i = 0; i < this.name.length(); i++) {
				char c = this.name.charAt(i);
				if(Character.isUpperCase(c)) {
					name.append('-');
					c = Character.toLowerCase(c);
				}
				name.append(c);
			}
			this.name = name.toString();
		}
		
		@Override
		public String toString() {
			return color;
		}
	}

//    "bd", backDefault,
//    "br", backRed,
//    "bg", backGreen,
//    "by", backYellow,
//    "bb", backBlue,
//
//    "ff", flush,
//    "fr", reset,
//    "fb", bold,
//    "fi", italic,
//    "fu", underline,
//    "k", black,
//    "lk", lightBlack,
//    "lw", lightWhite,
//    "r", red,
//    "g", green,
//    "y", yellow,
//    "b", blue,
//    "p", purple,
//    "c", cyan,
//    "lr", lightRed,
//    "lg", lightGreen,
//    "ly", lightYellow,
//    "lm", lightMagenta,
//    "lb", lightBlue,
//    "lc", lightCyan,
//    "w", white
	

	public static void info(Object... args) {
//		(args);
		arc.util.Log.info(getCaller() + paint(format("", args)));
	}

	public static void info(String text, Object... args) {
//		arc.util.Log.info(text, args);
		arc.util.Log.info(getCaller() + paint(format(text, args)));
	}

	public static void warn(Object... args) {
		arc.util.Log.warn(getCaller() + paint(format("[yellow]", args) + "[]"));
	}
	
	public static void warn(String text, Object... args) {
		arc.util.Log.warn(getCaller() + paint("[yellow]" + format(text, args) + "[]"));
	}
	
	private static String getCaller() {
		try {
			throw new Exception("Meow");
		} catch (Exception e) {
			StackTraceElement method = e.getStackTrace()[2];
	        StringBuilder sb = new StringBuilder();
	        sb.append('(');
	        if (method.isNativeMethod()) {
	            sb.append("Native Method");
	        } else if (method.getFileName() == null) {
	            sb.append("Unknown Source");
	        } else {
	            sb.append(method.getFileName());
	            if (method.getLineNumber() >= 0) {
	                sb.append(':').append(method.getLineNumber());
	            }
	        }
	        sb.append(')');
	        sb.append(' ');
	        return sb.toString();
		}
	}

	public static void err(String text, Object... args) {
		arc.util.Log.err(getCaller() + format(text, args));
	}
	
	private static String format(String str, Object... args) {
		StringBuilder formated = new StringBuilder(str.length());
		int arg = 0;
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			if(c == '@' && arg < args.length) {
				formated.append(toString(args[arg++]));
				continue;
			}
			formated.append(c);
		}
		for (int i = arg; i < args.length; i++) {
			if(formated.length() != 0) formated.append(' ');
			formated.append(toString(args[i]));
		}
		return formated.toString();
	}
	
	private static String paint(String str) {
		Stack<Colors> stack = new Stack<Log.Colors>();
		stack.push(Colors.reset);
		StringBuilder colored = new StringBuilder();
		
		int begin = -1;
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			
			if(c == '[') {
				begin = i+1;
				continue;
			}
			if(c == ']' && begin != -1) {
				String tag = str.substring(begin, i);
				begin = -1;
				if(tag.length() == 0) {
					if(stack.size() > 0) stack.pop();
					if(stack.size() > 0) {
						colored.append(stack.peek());
					} else {
						colored.append("[]");
					}
					continue;
				}
				Colors color = colors.get(tag);
				if(color == null) {
					colored.append('[');
					colored.append(tag);
					colored.append(']');
				} else {
					colored.append(color);
					stack.push(color);
				}
				continue;
			}
			if(begin != -1) continue;
			colored.append(c);
		}
		if(begin != -1) {
			colored.append(str.substring(begin-1, str.length()));
		}
		if(stack.size() > 1) colored.append(Colors.reset);
		return colored.toString();
	}
	
	private static String toString(Object object) {
		if(object == null) return "[magenta]null[]";
		if(object instanceof Boolean) {
			Boolean b = (Boolean) object;
			return b ? "[green]true[]" : "[red]false[]";
		}
		return object.toString();
	}

	public static void err(Throwable e) {
		arc.util.Log.err(e);
	}


}