package nl.han.ica.icss.ast.loops;

import nl.han.ica.icss.ast.Expression;

import java.util.Objects;

public class LoopIdentifier extends Expression {

    public String name;

    public LoopIdentifier(String name) {
        super();
        this.name = name;
    }

    @Override
    public String getNodeLabel() {
        return "loop Identifier (" + name+ ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        LoopIdentifier that = (LoopIdentifier) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {

        return Objects.hash(name);
    }
}

