package org.irssi.webssi.client.model;

/**
 * Level of activity in a window or window item.
 */
public enum DataLevel {
	NONE("dlNone"),
	TEXT("dlText"),
	MSG("dlMsg"),
	HILIGHT("dlHilight");
	
	private final String styleName;
	
	private DataLevel(String styleName) {
		this.styleName = styleName;
	}
	
	public static DataLevel fromInt(int dataLevel) {
		return DataLevel.values()[dataLevel];
	}

	public String getStyleName() {
		return styleName;
	}
}
