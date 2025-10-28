package nl.han.ica.icss.ast.loops;

import nl.han.ica.icss.ast.ASTNode;
import nl.han.ica.icss.ast.Expression;

import java.util.ArrayList;
import java.util.Objects;

public class ForLoop extends ASTNode {

    public LoopIdentifier loopVariable;
    public Expression rangeStart;
    public Expression rangeEnd;
    public ArrayList<ASTNode> body = new ArrayList<>();

    public ForLoop() {}

    public ForLoop(LoopIdentifier loopVariable, Expression rangeStart, Expression rangeEnd, ArrayList<ASTNode> body) {
        this.loopVariable = loopVariable;
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
        this.body = body;
    }

    @Override
    public String getNodeLabel() {
        return "ForLoop";
    }

    @Override
    public ArrayList<ASTNode> getChildren() {
        ArrayList<ASTNode> children = new ArrayList<>();
        if (loopVariable != null) children.add(loopVariable);
        if (rangeStart != null) children.add(rangeStart);
        if (rangeEnd != null) children.add(rangeEnd);
        if (body != null) children.addAll(body);
        return children;
    }

    @Override
    public ASTNode addChild(ASTNode child) {
        if (child instanceof LoopIdentifier && loopVariable == null) {
            loopVariable = (LoopIdentifier) child;
        } else if (child instanceof Expression) {
            if (rangeStart == null) {
                rangeStart = (Expression) child;
            } else if (rangeEnd == null) {
                rangeEnd = (Expression) child;
            } else {
                body.add(child);
            }
        } else {
            body.add(child);
        }
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ForLoop)) return false;
        if (!super.equals(o)) return false;
        ForLoop forLoop = (ForLoop) o;
        return Objects.equals(loopVariable, forLoop.loopVariable)
                && Objects.equals(rangeStart, forLoop.rangeStart)
                && Objects.equals(rangeEnd, forLoop.rangeEnd)
                && Objects.equals(body, forLoop.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(loopVariable, rangeStart, rangeEnd, body);
    }
}