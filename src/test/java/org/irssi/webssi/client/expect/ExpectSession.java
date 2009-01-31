package org.irssi.webssi.client.expect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class ExpectSession {
	// TODO change expecting to a List<ExpectedCall<?>> to simplify 
	private final HashMap<Expectable<?>, List<ExpectedCall<?>>> expecting = new HashMap<Expectable<?>, List<ExpectedCall<?>>>();
	private final ExpectListener listener;
	private final ExpectedCall<Object> beginning = new ExpectedCall<Object>(this, Expectable.NOTHING, null);
	
	public ExpectSession(ExpectListener listener) {
		this.listener = listener;
	}
	
	@Deprecated
	public <T> ExpectedCall<T> expect(Expectable<T> expectable) {
		return beginning().followedBy(expectable);
	}
	
	public ExpectedCall<Object> beginning() {
		return beginning;
	}
	
	public void start() {
		beginning.called(null);
	}
	
	void addToExpecting(ExpectedCall<?> call) {
		Expectable<?> expectable = call.expectable;
		List<ExpectedCall<?>> list = expecting.get(expectable);
		if (list == null) {
			list = new ArrayList<ExpectedCall<?>>();
			expecting.put(expectable, list);
		}
		
		list.add(call);
	}
	
	@SuppressWarnings("unchecked") // assuming expecting's values T matches the keys, which addToExpecting assures
	public <T> void called(Expectable<T> expectable, T param) {
		List<ExpectedCall<?>> list = expecting.get(expectable);
		if (list != null) {
			for (ExpectedCall<?> expectedCall : list) {
				ExpectedCall<T> call = (ExpectedCall<T>) expectedCall;
				if (call.matches(param)) {
					listener.called(expectable, param);
					
					boolean remove = call.called(param);
					
					if (remove) {
						remove(call);
//						list.remove(call);
//						if (list.isEmpty()) {
//							expecting.remove(expectable);
//						}
					}
					
					listener.log("expecting: " + expecting);
					
					if (expecting.size() == 0) {
						listener.done();
					}
					
					return;
				}
			}
		}
		
		if (dropOptional()) {
			// we've dropped optional calls, try again
			called(expectable, param);
		} else {
			throw new AssertionError("Unexpected call " + expectable);
		}
	}
	
	private void remove(ExpectedCall<?> call) {
		List<ExpectedCall<?>> list = expecting.get(call.expectable);
		assert list.contains(call);
		list.remove(call);
		if (list.isEmpty()) {
			expecting.remove(call.expectable);
		}
	}
	
	private boolean dropOptional() {
		boolean removedOne = false;
		// iterate over copies to avoid ConcurrentModificationException
		// should not be needed anymore when changing expecting to a simple List
		for (List<ExpectedCall<?>> list : new ArrayList<List<ExpectedCall<?>>>(expecting.values())) {
			for (ExpectedCall<?> expectedCall : new ArrayList<ExpectedCall<?>>(list)) {
				if (expectedCall.isOptional()) {
					listener.log("dropping optional " + expectedCall);
					expectedCall.activateFollowers();
					remove(expectedCall);
					removedOne = true;
				}
			}
		}
		return removedOne;
	}
	
	public ExpectedCall<Object> createRoot() {
		return new ExpectedCall<Object>(this, Expectable.NOTHING, null);
	}
}
