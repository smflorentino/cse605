package javax.realtime;


/**
 * This is a visitor for children scoped allocation contexts.  It defines some
 * work to be performed on each child.  It is used by
 * {@link AllocationContext#visitScopedChildren(ChildScopeVisitor)}.
 */
public class ChildScopeVisitor {


	  /**
	   * The method to be called when using this visitor.  
	   *
	   * @param scope is a child scoped allocation context.
	   *
	   * @return some instance of an Object
	   */
	  Object visit(ScopedAllocationContext scope) {
		return null;
	}
	
}
