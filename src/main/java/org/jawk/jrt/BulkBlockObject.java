package org.jawk.jrt;

import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;

/**
 * A convenience class that blocks
 * until any Blockable in the handles set is ready
 * with data (i.e., will not block).
 * It's like BlockObject in that this is returned
 * to the AVM/AwkScript for execution by
 * the BlockManager.  Unlike the BlockObject, however,
 * it implements the block() and getNotifierTag()
 * to manage blocking for a collection of Blockables.
 *
 * @see BlockManager
 * @see BlockObject
 */
public final class BulkBlockObject extends BlockObject {

  /**
   * When set true, all empty (AWK-null) handles are treated
   * as blocked blockables, allowing other blockers to block.
   */
  private static final boolean BYPASS_ALL_BLANK_HANDLES = true;

  private final String prefix;
  private final Set<String> handles;
  private final Map<String, ? extends Blockable> blockables;
  private final VariableManager vm;

  /**
   * Construct a block object which waits for any Blockable
   * within the set of Blockables to "unblock".
   *
   * @param prefix First part of the return string for the block operation.
   * @param blockables All universe of handles and their associated blockables.
   * @param vm Required to obtain OFS (used in the construction of the
   * 	return string / notifier tag).
   */
  public BulkBlockObject(String prefix, Map<String, ? extends Blockable> blockables, VariableManager vm) {
	if (prefix == null) throw new IllegalArgumentException("prefix argument cannot be null");
	if (blockables == null) throw new IllegalArgumentException("blockables argument cannot be null");
	if (vm == null) throw new IllegalArgumentException("vm argument cannot be null");
	this.prefix = prefix;
	this.handles = new LinkedHashSet<String>();
	this.blockables = blockables;
	this.vm = vm;
  }
  public final boolean containsHandle(String handle) {
	return handles.contains(handle);
  }
  private String block_result = null;
  /**
   * What to return to the client code when a handle is non-blocking.
   * <p>
   * The format is as follows :
   * <blockquote>
   * <pre>
   * prefix OFS handle
   * </pre>
   *
   * @return The client string containing the handle of the
   * non-blocking object.
   */
  public final String getNotifierTag() {
	assert prefix != null;
	assert vm != null;
	return prefix
	+JRT.toAwkString(vm.getOFS(), vm.getCONVFMT().toString())
	+block_result;
  }

  private static final String ALL_HANDLES_ARE_BLANK = "ALL_HANDLES_ARE_BLANK";
  private static final String ALL_HANDLES_ARE_BLOCKED = "ALL_HANDLES_ARE_BLOCKED";

  public final void block()
  throws InterruptedException {
	synchronized(this) {

		String handle = checkForNonblockHandle();

		if (handle == ALL_HANDLES_ARE_BLANK) {
			this.wait();
			throw new Error("Should never be notified.");
		}

		if (handle == ALL_HANDLES_ARE_BLOCKED) {
			this.wait();
			handle = checkForNonblockHandle();
		}
		assert handle != null;
		assert handle != ALL_HANDLES_ARE_BLOCKED : "handle == ALL_HANDLES_ARE_BLOCKED is an invalid return value ... willBlock() could be of issue";
		block_result = handle;
	}
  }

  private final String checkForNonblockHandle() {
	boolean all_handles_are_blank = true;
	// cycle through all block_handles
	// check if any of them has accepted sockets
	for(String handle : handles) {
		if (BYPASS_ALL_BLANK_HANDLES)
			if (handle.equals(""))
				continue;
		all_handles_are_blank = false;
		Blockable blockable = blockables.get(handle);
		if (blockable == null)
			/*
			throw new AwkRuntimeException("handle '"+handle+"' not a valid blockable -- "+
					"if these are dialog handles, perhaps you didn't clear out the dialog handle after it has been destroyed via DialogDestroy()?");
			*/
			throw new AwkRuntimeException("handle '"+handle+"' doesn't map to a valid blockable");
		if (! blockable.willBlock(this))
			return handle;
	}
	//return null;
	if (all_handles_are_blank)
		return ALL_HANDLES_ARE_BLANK;
	else
		return ALL_HANDLES_ARE_BLOCKED;
  }

  private static final BlockHandleValidator no_block_handle_validation = new BlockHandleValidator() {
	public final String isBlockHandleValid(String handle) {
		// always valid
		return null;
	}
  };

  public final BlockObject populateHandleSet(Object[] args, VariableManager vm) {
	return populateHandleSet(args, vm, no_block_handle_validation);
  }

  public final BlockObject populateHandleSet(Object[] args, VariableManager vm, BlockHandleValidator validator) {
	BlockObject blocker = this;

	if (args.length == 0)
		throw new IllegalArgumentException(prefix+" blocker requires at least one argument.");
	int num_args = args.length;
	Object last_arg = args[num_args-1];
	if (last_arg instanceof BlockObject) {
		BlockObject bo = (BlockObject) last_arg;
		blocker.setNextBlockObject(bo);
		if (args.length == 1)
			throw new IllegalArgumentException(prefix+" blocker requires at least one item to close-block on.");
		--num_args;
	} else
		blocker.clearNextBlockObject();

	// what we know
	// 0 .. num_args-1 = items to close-block
	// num_args >= 1

	Set<String> handle_set = handles;

	handle_set.clear();

	for(int i=0;i<num_args;++i) {
		Object o = args[i];
		if (o instanceof AssocArray) {
			AssocArray aa = (AssocArray) o;
			for(Object oo : aa.keySet()) {
				String handle = JRT.toAwkString(oo, vm.getCONVFMT().toString());
				String reason = validator.isBlockHandleValid(handle);
				if (reason != null)
					throw new AwkRuntimeException(handle+": invalid handle: "+reason);
				// otherwise...
				handle_set.add(handle);
			}
		} else {
			String handle = JRT.toAwkString(o, vm.getCONVFMT().toString());
			String reason = validator.isBlockHandleValid(handle);
			if (reason != null)
				throw new AwkRuntimeException(handle+": invalid handle: "+reason);
			// otherwise...
			handle_set.add(handle);
		}
	}

	return blocker;
  }
} // public final class BulkBlockObject {BlockObject}

