package agzam4proc.api.utils;

import java.util.Comparator;
import java.util.Objects;

import arc.func.Boolf;
import arc.func.Func2;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Nullable;

public class DagNode<T extends Equality<T>> {
	
	public T value;

	public final Seq<DagNode<T>> children = new Seq<>();

	public DagNode(T value) {
		Objects.requireNonNull(value);
		this.value = value;
	}

	@Nullable
	private DagNode<T> findSharedSubtree(Seq<DagNode<T>> scope, DagNode<T> target) {
		for (int i = 0; i < scope.size; i++) {
			DagNode<T> current = scope.get(i);
			Log.info("> @", current);
			if (current.eql(target)) {
				Log.info("FOUND");
				target.value = current.value;
				return current;
			}
			if (current.children != null) {
				DagNode<T> found = findSharedSubtree(current.children, target);
				if (found != null) {
					Log.info("FOUND");
					target.value = found.value;
					return found;
				}
			}
		}
		return null;
	}


	@SafeVarargs
	public static <T extends Equality<T>> DagNode<T> of(T payload, DagNode<T> ...childNodes) {
		return of(Seq.with(childNodes), payload);
	}

	/**
	 * Links child nodes to the current<br>
	 * performs full graph deduplication, and returns the interned node
	 */
	public static <T extends Equality<T>> DagNode<T> of(Seq<DagNode<T>> childNodes, T payload) {
		var node = new DagNode<T>(payload);
		node.add(childNodes);
		return node;
	}
	
	public void add(Seq<DagNode<T>> childNodes) {
		children.addAll(childNodes);
		for (int i = 0; i < children.size; i++) {
			for (int ii = 0; ii < children.size; ii++) {
				var found = children.get(i).find(children.get(ii));
				if(found != null) {
					children.set(i, found);
				}
			}
		}
	}
	

	@Nullable
	private DagNode<T> find(DagNode<T> node) {
		if(eql(node)) return this;
		if(node.children.size != 0) {
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
		if(this.children.size != 0) {
			for (int i = 0; i < this.children.size; i++) {
				var found = this.children.get(i).find(node);
				if (found != null) return found;
			}
		}
		return null;
	}

	@Nullable
	public DagNode<T> find(Boolf<DagNode<T>> boolf) {
		if(boolf.get(this)) return this;
		for (int i = 0; i < children.size; i++) {
			var n = children.get(i).find(boolf);
			if(n != null) return n;
		}
		return null;
	}

	@Nullable
	public <R> R reduce(R r, Func2<R, DagNode<T>, R> func) {
		r = func.get(r, this);
		for (int i = 0; i < children.size; i++) {
			r = children.get(i).reduce(r, func);
		}
		return r;
	}

//	@Override
//	public boolean equals(Object o) {
//		if (this == o) return true;
//		if (o == null || getClass() != o.getClass()) return false;
//		DagNode<?, ?> node = (DagNode<?, ?>) o;
//		if (!Objects.equals(constant, node.constant)) return false;
//		if (children == null && node.children == null) return true;
//		if (children == null || node.children == null) return false;
//		if (children.size != node.children.size) return false;
//		for (int i = 0; i < children.size; i++) {
//			if (!children.get(i).equals(node.children.get(i))) return false;
//		}
//		return true;
//	}


	public boolean eql(DagNode<T> node) {
		if (this == node) return true;
		if (node == null) return false;
		if (!node.value.eql(value)) return false;
		if (children.size != node.children.size) return false;
		for (int i = 0; i < children.size; i++) {
			if (!children.get(i).eql(node.children.get(i))) return false;
		}
		return true;
	}

	@Override
	public String toString() {
//		if (isConstant()) return "CONST-" + "[" + constant + "]";
		StringBuilder sb = new StringBuilder(value.toString()).append("(");
		for (int i = 0; i < children.size; i++) {
			sb.append(children.get(i));
			if (i < children.size - 1) sb.append(", ");
		}
		return sb.append(")").toString();
	}

	public Seq<DagNode<T>> linearize() {
		Seq<DagNode<T>> output = new Seq<>();
		linearize(output);
		return output;
	}

	public Seq<DagNode<T>> linearize(Comparator<? super DagNode<T>> comp) {
		Seq<DagNode<T>> output = new Seq<>();
		linearize(output, comp);
		return output;
	}
	
	public Seq<DagNode<T>> linearizeMinChildren() {
		Seq<DagNode<T>> output = new Seq<>();
		linearize(output, (n1,n2) -> Integer.compare(n1.children.size, n2.children.size));
		return output;
	}

	private void linearize(Seq<DagNode<T>> output) {
//		if (children.size == 0) return;
		if(output.contains(this)) return;
		for (int i = 0; i < children.size; i++) {
			children.get(i).linearize(output);
		}
		output.add(this);
	}
	
	private void linearize(Seq<DagNode<T>> output, Comparator<? super DagNode<T>> comp) {
		if(output.contains(this)) return;
		var sorted = Seq.with(children).sort(comp);
//		Log.info("Nodes on level: @", sorted.toString(", ", n -> n.value.toString()));
		for (int i = 0; i < sorted.size; i++) {
			sorted.get(i).linearize(output, comp);
		}
		output.add(this);
	}
	

}
