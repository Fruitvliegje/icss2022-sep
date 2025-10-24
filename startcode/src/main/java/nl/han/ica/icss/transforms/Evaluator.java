package nl.han.ica.icss.transforms;

import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.operations.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class Evaluator implements Transform {

    private LinkedList<HashMap<String, Literal>> variableValues;

    @Override
    public void apply(AST ast) {
        variableValues = new LinkedList<>();
        applyStylesheet(ast.root);
    }

    private void applyStylesheet(Stylesheet node) {
        variableValues.push(new HashMap<>());

        for (ASTNode child : node.getChildren()) {
            if (child instanceof VariableAssignment) {
                applyVariableAssignment((VariableAssignment) child);
            } else if (child instanceof Stylerule) {
                applyStylerule((Stylerule) child);
            }
        }

        variableValues.pop();
    }

    private void applyVariableAssignment(VariableAssignment node) {
        Literal value = (Literal) evaluateExpression(node.expression);
        if (value == null) return;
        variableValues.peek().put(node.name.name, value);
    }

    private void applyStylerule(Stylerule node) {
        variableValues.push(new HashMap<>());

        LinkedList<ASTNode> evaluatedBody = new LinkedList<>();

        for (ASTNode child : node.getChildren()) {
            if (child instanceof Declaration) {
                applyDeclaration((Declaration) child);
                evaluatedBody.add(child);
            } else if (child instanceof IfClause) {
                applyIfClause((IfClause) child, evaluatedBody);
            }
        }

        node.body = new ArrayList<>(evaluatedBody);
        variableValues.pop();
    }

    private void applyIfClause(IfClause ifClause, LinkedList<ASTNode> parentBody) {
        Expression conditionExpr = evaluateExpression(ifClause.conditionalExpression);

        boolean conditionTrue = false;
        if (conditionExpr instanceof BoolLiteral) {
            conditionTrue = ((BoolLiteral) conditionExpr).value;
        }

        if (conditionTrue) {
            applyBody(ifClause.body, parentBody);
        } else if (ifClause.elseClause != null) {
            applyElseClause(ifClause.elseClause, parentBody);
        }
    }

    private void applyElseClause(ElseClause elseClause, LinkedList<ASTNode> parentBody) {
        applyBody(elseClause.body, parentBody);
    }

    private void applyBody(Iterable<ASTNode> body, LinkedList<ASTNode> parentBody) {
        variableValues.push(new HashMap<>());

        for (ASTNode child : body) {
            if (child instanceof Declaration) {
                applyDeclaration((Declaration) child);
                parentBody.add(child);
            } else if (child instanceof VariableAssignment) {
                applyVariableAssignment((VariableAssignment) child);
            } else if (child instanceof IfClause) {
                applyIfClause((IfClause) child, parentBody);
            }
        }

        variableValues.pop();
    }

    private void applyDeclaration(Declaration node) {
        Expression evaluated = evaluateExpression(node.expression);
        if (evaluated instanceof Literal) {
            node.expression = evaluated;
        }
    }

    private Expression evaluateExpression(Expression expression) {
        if (expression instanceof Literal) {
            return expression;
        }

        if (expression instanceof VariableReference) {
            Literal value = resolveVariable((VariableReference) expression);
            return value != null ? value : expression;
        }

        if (expression instanceof AddOperation) {
            return evalAddOperation((AddOperation) expression);
        } else if (expression instanceof SubtractOperation) {
            return evalSubtractOperation((SubtractOperation) expression);
        } else if (expression instanceof MultiplyOperation) {
            return evalMultiplyOperation((MultiplyOperation) expression);
        }

        return expression;
    }

    private Expression evalAddOperation(AddOperation expr) {
        Expression lhs = evaluateExpression(expr.lhs);
        Expression rhs = evaluateExpression(expr.rhs);
        if (!(lhs instanceof Literal) || !(rhs instanceof Literal)) return expr;

        if (lhs instanceof PixelLiteral && rhs instanceof PixelLiteral) {
            return new PixelLiteral(((PixelLiteral) lhs).value + ((PixelLiteral) rhs).value);
        }
        if (lhs instanceof PercentageLiteral && rhs instanceof PercentageLiteral) {
            return new PercentageLiteral(((PercentageLiteral) lhs).value + ((PercentageLiteral) rhs).value);
        }
        if (lhs instanceof ScalarLiteral && rhs instanceof ScalarLiteral) {
            return new ScalarLiteral(((ScalarLiteral) lhs).value + ((ScalarLiteral) rhs).value);
        }
        return expr;
    }

    private Expression evalSubtractOperation(SubtractOperation expr) {
        Expression lhs = evaluateExpression(expr.lhs);
        Expression rhs = evaluateExpression(expr.rhs);
        if (!(lhs instanceof Literal) || !(rhs instanceof Literal)) return expr;

        if (lhs instanceof PixelLiteral && rhs instanceof PixelLiteral) {
            return new PixelLiteral(((PixelLiteral) lhs).value - ((PixelLiteral) rhs).value);
        }
        if (lhs instanceof PercentageLiteral && rhs instanceof PercentageLiteral) {
            return new PercentageLiteral(((PercentageLiteral) lhs).value - ((PercentageLiteral) rhs).value);
        }
        if (lhs instanceof ScalarLiteral && rhs instanceof ScalarLiteral) {
            return new ScalarLiteral(((ScalarLiteral) lhs).value - ((ScalarLiteral) rhs).value);
        }
        return expr;
    }

    private Expression evalMultiplyOperation(MultiplyOperation expr) {
        Expression lhs = evaluateExpression(expr.lhs);
        Expression rhs = evaluateExpression(expr.rhs);
        if (!(lhs instanceof Literal) || !(rhs instanceof Literal)) return expr;

        Literal left = (Literal) lhs;
        Literal right = (Literal) rhs;

        if (left instanceof ScalarLiteral && right instanceof PixelLiteral)
            return new PixelLiteral(((ScalarLiteral) left).value * ((PixelLiteral) right).value);
        if (right instanceof ScalarLiteral && left instanceof PixelLiteral)
            return new PixelLiteral(((PixelLiteral) left).value * ((ScalarLiteral) right).value);
        if (left instanceof ScalarLiteral && right instanceof PercentageLiteral)
            return new PercentageLiteral(((ScalarLiteral) left).value * ((PercentageLiteral) right).value);
        if (right instanceof ScalarLiteral && left instanceof PercentageLiteral)
            return new PercentageLiteral(((PercentageLiteral) left).value * ((ScalarLiteral) right).value);
        if (left instanceof ScalarLiteral && right instanceof ScalarLiteral)
            return new ScalarLiteral(((ScalarLiteral) left).value * ((ScalarLiteral) right).value);

        return expr;
    }

    private Literal resolveVariable(VariableReference ref) {
        for (HashMap<String, Literal> scope : variableValues) {
            if (scope.containsKey(ref.name)) {
                return scope.get(ref.name);
            }
        }
        return null;
    }
}