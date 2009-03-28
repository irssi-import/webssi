package org.irssi.webssi.client.view;

import java.util.HashMap;
import java.util.Map;

import org.irssi.webssi.client.model.Activity;
import org.irssi.webssi.client.model.DataLevel;

class ColorUtil {
	private static Map<Character, Character> colorAliasMap = new HashMap<Character, Character>();
	static {
		colorAliasMap.put('0', 'k');
		colorAliasMap.put('1', 'r');
		colorAliasMap.put('2', 'g');
		colorAliasMap.put('3', 'y');
		colorAliasMap.put('4', 'b');
		colorAliasMap.put('5', 'm');
		colorAliasMap.put('6', 'c');
		colorAliasMap.put('7', 'w');
		colorAliasMap.put('9', '_');
	}
	
	private static Map<Character, String> styleMap = new HashMap<Character, String>();
	static {
		styleMap.put('_', "b");
		styleMap.put('U', "u");
		styleMap.put('#', "m");
		styleMap.put('F', null); // not supporting flashing
	}
	
	private static String colorToStyle(String color) {
		if (color == null || color.length() < 2 || color.charAt(0) != '%')
			return null;
		char c = color.charAt(1);
		if (colorAliasMap.containsKey(c))
			c = colorAliasMap.get(c);
		
		if (styleMap.containsKey(c)) {
			return styleMap.get(c);
		} else if (Character.isLowerCase(c)) {
			return "f" + c;
		} else if (Character.isUpperCase(c)) {
			return "l" + c;
		} else {
			return null;
		}
	}
	
	static String styleForActivity(Activity activity) {
		String styleName = null;
		if (activity.getDataLevel() == DataLevel.HILIGHT) {
			styleName = ColorUtil.colorToStyle(activity.getHilightColor());
		}
		if (styleName == null)
			styleName = activity.getDataLevel().getStyleName();
		return styleName;
	}
}
