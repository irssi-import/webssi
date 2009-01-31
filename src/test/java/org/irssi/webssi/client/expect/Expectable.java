/**
 * 
 */
package org.irssi.webssi.client.expect;

public class Expectable<T> {
	public static Expectable<Object> NOTHING = new Expectable<Object>("nothing") {
		@Override
		void activated(ExpectSession session) {
			session.called(this, null);			
		}
	};
	
	public static <T> Expectable<T> declareExpectable(String name) {
		return new Expectable<T>(name);
	}
	
	private final String name;
	
	private Expectable(String name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
		return name;
	}

	void activated(ExpectSession session) {
	}
}