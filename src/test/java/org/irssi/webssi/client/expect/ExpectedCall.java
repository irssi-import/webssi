package org.irssi.webssi.client.expect;

import java.util.ArrayList;

/**
 * Registration of a call that is expected in the future.
 * 
 * @param <T> The type of the parameter of the call.
 */
public final class ExpectedCall<T> {
	/**
	 * The session this ExpectedCall belongs to.
	 */
	private final ExpectSession session;
	
	/**
	 * The previous call, that has to be called before this one is activated (really expected).
	 * this ExpectedCall is always be in previous.followers.
	 * For a root, previous is null.
	 */
	private ExpectedCall<?> previous;
	
	/**
	 * Call we are expecting
	 */
	final Expectable<T> expectable;
	
	/**
	 * True iff should be checked if expectedParam equals the actual parameter.
	 * (needed to make a difference between null and unspecified expectedParam) 
	 */
	private boolean matchParam;
	
	/**
	 * Parameter we expect. The actual parameter should be equal (as in Object.equals) to this.
	 */
	private T expectedParam;
	
	/**
	 * Alternative for expectedParam: a Ref pointing to the expected parameter.
	 */
	private Ref<T> paramRef;
	
	/**
	 * List of calls that should be activated after this call
	 */
	private ArrayList<ExpectedCall<?>> followers = new ArrayList<ExpectedCall<?>>();
	
	/**
	 * List of reactions to execute when this call is matched
	 */
	private ArrayList<Reaction<? super T>> reactions = new ArrayList<Reaction<? super T>>();
	
	/**
	 * Optional calls may be called, but if another unexpected call is encountered,
	 * they are considered done.
	 */
	private boolean optional = false;
	
	/**
	 * Number of times we expect this call to be matched
	 */
	private int expectCount = 1;
	
	/**
	 * Actual times this call has been matched already
	 */
	private int calledCount = 0;
	
	/**
	 * Actual parameter used when this was matched (the last time).
	 */
	private T calledParam;
	
	ExpectedCall(ExpectSession session, Expectable<T> expectable, ExpectedCall<?> previous) {
		this.session = session;
		this.expectable = expectable;
		this.matchParam = false;
		this.expectedParam = null;
		this.previous = previous;
	}
	
	/**
	 * Only match if the given value is equal to the parameter of the actual call.
	 * @param param Expected parameter
	 * @return this
	 */
	public ExpectedCall<T> withParam(T param) {
		this.matchParam = true;
		this.expectedParam = param;
		return this;
	}
	
	/**
	 * Only match if the value of the given Ref (at the moment of the matching) is equal to the parameter of the actual call.
	 * @return this
	 */
	public ExpectedCall<T> withParamRef(Ref<T> paramRef) {
		this.paramRef = paramRef;
		return this;
	}
	
	/**
	 * Checks if the give parameter matches the expected parameter.
	 */
	boolean matches(T param) {
		return (matchParam == false || eq(param, this.expectedParam))
			&& (paramRef == null || eq(param, paramRef.value));
	}
	
	private static boolean eq(Object o1, Object o2) {
		return o1 == null ? o2 == null : o1.equals(o2);
	}
	
	/**
	 * Called when this ExpectedCall has really been called.
	 * Will only be called if matches(param) returned true.
	 * @param param
	 * @return true iff this ExpectedCall should now be removed.
	 * 	If false, it should not be removed, because the call could be repeated.
	 */
	boolean called(T param) {
		calledCount++;
		assert calledCount <= expectCount || expectCount == -1;
		calledParam = param;
		boolean done = (calledCount == expectCount);
		if (done)
			activateFollowers();
		
		for (Reaction<? super T> reaction : reactions) {
			reaction.run(calledParam);
		}
		
		return done;
	}
	
	boolean removeWhenMatched() {
		return calledCount == expectCount - 1;
	}
	
	/**
	 * Activate the followers.
	 * Done when this call is matched as many times as expected,
	 * or when this call is optional and is dropped.
	 */
	void activateFollowers() {
		for (ExpectedCall<?> follower : followers) {
			session.addToExpecting(follower);
			follower.expectable.activated(session);
		}
	}

	/**
	 * Adds a follower to the list.
	 * A follower is activated after this call is matched.
	 * @param expectable Call to expect after this
	 * @return A newly created ExpectedCall, of the given expectable
	 */
	public <U> ExpectedCall<U> followedBy(Expectable<U> expectable) {
		ExpectedCall<U> follower = new ExpectedCall<U>(session, expectable, this);
		followers.add(follower);
		return follower;
	}
	
	/**
	 * Adds the root of the given call as follower, to be activated after this call is matched.
	 * The call should have a root created with {@link ExpectSession#createRoot()}
	 * @param call Call in the chain that follows this call.
	 * @return The given call
	 */
	public <U> ExpectedCall<U> followedBy(ExpectedCall<U> call) {
		ExpectedCall<?> follower = call.getRoot();
		assert follower.session == this.session;
		assert follower != session.beginning();
		follower.previous = this;
		followers.add(follower);
		return call;
	}
	
	/**
	 * Adds a reaction. Reactions are activated when this ExpectedCall is matched.
	 * @param reaction Reaction to execute
	 * @return this
	 */
	public ExpectedCall<T> react(Reaction<? super T> reaction) {
		reactions.add(reaction);
		return this;
	}

	/**
	 * Makes this an optional call that may be called any number of times.
	 * This call will be dropped, and followers activated once an unexpected call is encountered
	 * @return this
	 */
	public ExpectedCall<T> zeroOrMoreTimes() {
		optional = true;
		expectCount = -1;
		return this;
	}
	
	public ExpectedCall<T> optional() {
		optional = true;
		return this;
	}
	
	/**
	 * Returns the first call of this chain of ExpectedCalls.
	 */
	private ExpectedCall<?> getRoot() {
		return previous == null ? this : previous.getRoot();
	}
	
	@Override
	public String toString() {
		return expectable + (matchParam ? "(" + expectedParam + ")" : "") + (optional ? "[optional]" : "");		
	}
	
	/**
	 * Returns if this call is optional.
	 * @see #zeroOrMoreTimes()
	 */
	public boolean isOptional() {
		return optional;
	}
}