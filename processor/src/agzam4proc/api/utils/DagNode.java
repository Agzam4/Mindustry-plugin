package agzam4proc.api.utils;

import java.util.Objects;

import arc.struct.Seq;
import arc.util.Log;
import arc.util.Nullable;

public class DagNode<T, C> {

	/** An additional value that not used in comparisons */
	public T payload;

	@Nullable
	public C value;

	@Nullable 
	public final Seq<DagNode<T, C>> children;

	/** branch constructor **/
	public DagNode(T payload) {
		this.payload = payload;
		this.children = new Seq<>();
		this.value = null;
	}

	/** Constant constructor **/
	protected DagNode(T payload, Seq<DagNode<T, C>> children, C value) {
		this.payload = payload;
		this.children = children;
		this.value = value;
	}

	public static <T, C> DagNode<T, C> constant(T payload, C value) {
		return new DagNode<>(payload, (Seq<DagNode<T, C>>) null, value);
	}

	public static <T, C> DagNode<T, C> constant(C value) {
		return new DagNode<T, C>(null, null, value);
	}

	private DagNode(T payload, @Nullable C value, @Nullable Seq<DagNode<T, C>> children) {
		this.payload = payload;
		this.value = value;
		this.children = children;
	}

	public static <T, C> DagNode<T, C> branch(T payload) {
		return new DagNode<T, C>(payload, null, new Seq<DagNode<T, C>>());
	}

	public static <T, C> DagNode<T, C> createConstant(T payload, C value) {
		return new DagNode<>(payload, value, null);
	}

	public boolean isConstant() {
		return children == null;
	}

	@Nullable
	private DagNode<T, C> findSharedSubtree(Seq<DagNode<T, C>> scope, DagNode<T, C> target) {
		for (int i = 0; i < scope.size; i++) {
			DagNode<T, C> current = scope.get(i);
			Log.info("> @", current);
			if (current.eql(target)) {
				Log.info("FOUND");
				target.payload = current.payload;
				return current;
			}
			if (current.children != null) {
				DagNode<T, C> found = findSharedSubtree(current.children, target);
				if (found != null) {
					Log.info("FOUND");
					target.payload = found.payload;
					return found;
				}
			}
		}
		return null;
	}


	/**
	 * Links child nodes to the current<br>
	 * performs full graph deduplication, and returns the interned node
	 */
	public static <T, C> DagNode<T, C> of(Seq<DagNode<T, C>> childNodes, T payload) {
		for (int i = 0; i < childNodes.size; i++) {
			for (int ii = 0; ii < childNodes.size; ii++) {
				var found = childNodes.get(i).find(childNodes.get(ii));
				if(found != null) {
					childNodes.set(i, found);
				}
			}
		}
		return new DagNode<T, C>(payload, null, childNodes);
	}

	@Nullable
	private DagNode<T, C> find(DagNode<T, C> node) {
		if(eql(node)) return this;
		if(!node.isConstant()) {
			for (int i = 0; i < node.children.size; i++) {
				var childDuplicate = this.find(node.children.get(i));
				if (childDuplicate != null) {
					node.children.set(i, childDuplicate);
				}
			}
			if (this.eql(node)) {
				return this;
			}
		}
		if(!isConstant()) {
			for (int i = 0; i < this.children.size; i++) {
				var found = this.children.get(i).find(node);
				if (found != null) return found;
			}
		}
		return null;
	}


	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		DagNode<?, ?> node = (DagNode<?, ?>) o;
		if (!Objects.equals(value, node.value)) return false;
		if (children == null && node.children == null) return true;
		if (children == null || node.children == null) return false;
		if (children.size != node.children.size) return false;
		for (int i = 0; i < children.size; i++) {
			if (!children.get(i).equals(node.children.get(i))) return false;
		}
		return true;
	}


	public boolean eql(DagNode<T, C> node) {
		if (this == node) return true;
		if (node == null) return false;
		if (!Objects.equals(value, node.value)) return false;
		if (children == null && node.children == null) return true;
		if (children == null || node.children == null) return false;
		if (children.size != node.children.size) return false;
		for (int i = 0; i < children.size; i++) {
			if (!children.get(i).eql(node.children.get(i))) return false;
		}
		return true;
	}

	@Override
	public String toString() {
		if (isConstant()) return "CONST-" + "[" + value + "]";
		StringBuilder sb = new StringBuilder().append("(");
		for (int i = 0; i < children.size; i++) {
			sb.append(children.get(i));
			if (i < children.size - 1) sb.append(", ");
		}
		return sb.append(")").toString();
	}

	public Seq<DagNode<T, C>> linearize() {
		Seq<DagNode<T, C>> output = new Seq<>();
		linearize(output);
		return output;
	}

	private void linearize(Seq<DagNode<T, C>> output) {
		if (output.contains(this)) return;
		if (children != null) {
			for (int i = 0; i < children.size; i++) {
				children.get(i).linearize(output);
			}
		}
		output.add(this);
	}

}
