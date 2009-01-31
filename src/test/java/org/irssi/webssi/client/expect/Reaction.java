/**
 * 
 */
package org.irssi.webssi.client.expect;

public abstract class Reaction<T> {
	public abstract void run(T param);
	
	public static <T> Reaction<T> saveParam(final Ref<T> ref) {
		return new Reaction<T>() {
			public void run(T param) {
				assert ref.value == null;
				ref.value = param;
			};
		};
	}
	
//	public static <T> Reaction<T> assertParam(final Ref<T> ref) {
//		return new Reaction<T>() {
//			public void run(T param) {
//				if (ref.value != param)
//					throw new AssertionError("expected " + ref.value + " got " + param);
//			};
//		};
//	}
}