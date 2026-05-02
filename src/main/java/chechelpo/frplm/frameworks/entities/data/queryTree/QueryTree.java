package chechelpo.frplm.frameworks.entities.data.queryTree;

import org.jetbrains.annotations.Nullable;
import org.jooq.TableField;
import org.jooq.TableRecord;

/**
 * Logic tree for doing arbitrary queries over a SINGLE table's attributes.
 * @param <R> table record of the table to do queries over.
 */
public class QueryTree<R extends TableRecord<R>> {
    private final Node root;
    private QueryTree(Node root){
        this.root = root;
    }

    public static class QueryBuilder<R extends TableRecord<R>>{
        private OperatorNode root;
        private Node current;
        public QueryBuilder(Operator root){
            this.root = new OperatorNode(root);
        }

        public <T> QueryBuilder<R> finalOperation(TableField<R,T> field, Operator operator, T value){
            if (current == null) {
                OperatorNode op = new OperatorNode(operator);
                op.setLeft(new FieldNode(field, op));
                op.setRight(new ValueNode(value, op));

            } else if (!(current instanceof OperatorNode))
                throw new IllegalStateException("Somehow current isn't operator node and also not null");
            OperatorNode op = (OperatorNode) current;
            assert op.rightSon() == null;
            return null;
        }

    }
    public sealed interface Node permits FieldNode, OperatorNode, ValueNode {
        @Nullable Node leftSon();
        @Nullable Node rightSon();
        @Nullable Node parent();
    }

    public static final class ValueNode<T> implements Node {
        public final T value;
        private final Node parent;
        private ValueNode(T value, Node parent){
            this.value = value;
            this.parent=parent;
        }

        @Override
        public @Nullable Node leftSon() {
            throw new IllegalArgumentException("This is a value node");
        }

        @Override
        public @Nullable Node rightSon() {
            throw new IllegalArgumentException("This is a value node");
        }

        @Override
        public @Nullable Node parent() {
            return this.parent;
        }
    }
    public static final class OperatorNode implements Node{
        public final Operator operator;
        private Node left;
        private Node right;
        private Node parent;
        private OperatorNode(Operator op){
            this.operator = op;
        }
        private void setParent(Node parent){
            this.parent = parent;
        }
        private void setRight(Node node){
            this.right = node;
        }
        private void setLeft(Node node){
            this.left = node;
        }

        @Override
        public @Nullable Node leftSon() {
            return this.left;
        }

        @Override
        public @Nullable Node rightSon() {
            return this.right;
        }

        @Override
        public @Nullable Node parent() {
            return parent;
        }
    }

    public static final class FieldNode<R extends TableRecord<R>,T> implements Node{
        public final TableField<R,T> field;
        private final Node parent;
        private FieldNode(TableField<R,T> field, Node parent){
            this.field=field;
            this.parent = parent;
        }

        @Override
        public @Nullable Node leftSon() {
            throw new IllegalArgumentException("This is a field node");
        }

        @Override
        public @Nullable Node rightSon() {
            throw new IllegalArgumentException("This is a field node");
        }

        @Override
        public @Nullable Node parent() {
            return this.parent;
        }
    }

    public enum Operator {
        EQUALS,
        DIFFERENT,
        GREATER_THAN,
        LESS_THAN,
    }
}
