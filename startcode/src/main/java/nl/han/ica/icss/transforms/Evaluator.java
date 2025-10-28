package nl.han.ica.icss.transforms;

import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.loops.ForLoop;
import nl.han.ica.icss.ast.loops.LoopIdentifier;
import nl.han.ica.icss.ast.operations.*;
import nl.han.ica.icss.ast.selectors.ClassSelector;
import nl.han.ica.icss.ast.selectors.IdSelector;
import nl.han.ica.icss.ast.selectors.TagSelector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class Evaluator implements Transform {

    private LinkedList<HashMap<String, Literal>> variableValues;

    @Override
    public void apply(AST ast) {
        variableValues = new LinkedList<>();
        applyStylesheet(ast.root);
    }

    private void applyStylesheet(Stylesheet node) {
        variableValues.push(new HashMap<>());

        LinkedList<ASTNode> evaluatedChildren = new LinkedList<>();

        for (ASTNode child : node.getChildren()) {
            if (child instanceof VariableAssignment) {
                applyVariableAssignment((VariableAssignment) child);
            } else if (child instanceof Stylerule) {
                applyStylerule((Stylerule) child);
                evaluatedChildren.add(child);
            } else if (child instanceof ForLoop) {
                applyForLoop((ForLoop) child, evaluatedChildren);
            }
        }

        node.body = new ArrayList<>(evaluatedChildren);
        variableValues.pop();
    }

    private void applyVariableAssignment(VariableAssignment node) {
        Expression evaluated = evaluateExpression(node.expression);
        if (!(evaluated instanceof Literal)) return;
        node.expression = evaluated;
        variableValues.peek().put(node.name.name, (Literal) evaluated);
    }

    private void applyStylerule(Stylerule node) {
        variableValues.push(new HashMap<>());

        LinkedList<ASTNode> evaluatedBody = new LinkedList<>();

        applyBody(node.body, evaluatedBody);

        node.body = new ArrayList<>(evaluatedBody);
        variableValues.pop();

    }

    private void applyIfClause(IfClause ifClause, List<ASTNode> parentBody) {
        Expression conditionExpr = evaluateExpression(ifClause.conditionalExpression);

        boolean conditionTrue = false;
        if (conditionExpr instanceof BoolLiteral) {
            conditionTrue = ((BoolLiteral) conditionExpr).value;
        }

        if (conditionTrue) {
            variableValues.push(new HashMap<>());
            applyBody(ifClause.body, parentBody);
            variableValues.pop();
        } else if (ifClause.elseClause != null) {
            applyElseClause(ifClause.elseClause, parentBody);
        }
    }

    private void applyElseClause(ElseClause elseClause, List<ASTNode> parentBody) {
        variableValues.push(new HashMap<>());
        applyBody(elseClause.body, parentBody);
        variableValues.pop();
    }

    private void applyBody(ArrayList<ASTNode> body, List<ASTNode> parentBody) {
        for (ASTNode child : body) {
            if (child instanceof VariableAssignment) {
                applyVariableAssignment((VariableAssignment) child);
            }
        }

        // Dan de rest
        for (ASTNode child : body) {
            if (child instanceof Declaration) {
                applyDeclaration((Declaration) child);
                Declaration newDecl = (Declaration) child;

                boolean replaced = false;
                for (int i = 0; i < parentBody.size(); i++) {
                    ASTNode existing = parentBody.get(i);
                    if (existing instanceof Declaration) {
                        Declaration existingDecl = (Declaration) existing;
                        if (existingDecl.property.name.equals(newDecl.property.name)) {
                            parentBody.set(i, newDecl);
                            replaced = true;
                            break;
                        }
                    }
                }

                if (!replaced) {
                    parentBody.add(newDecl);
                }

            } else if (child instanceof IfClause) {
                applyIfClause((IfClause) child, parentBody);
            } else if (child instanceof Stylerule) {
                applyStylerule((Stylerule) child);
                parentBody.add(child);
            }
        }
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

        if (expression instanceof Operation) {
            Operation op = (Operation) expression;
            op.lhs = evaluateExpression(op.lhs);
            op.rhs = evaluateExpression(op.rhs);

            if (!(op.lhs instanceof Literal) || !(op.rhs instanceof Literal)) return op;

            if (op instanceof AddOperation) {
                return evalAddOperation((AddOperation) op);
            } else if (op instanceof SubtractOperation) {
                return evalSubtractOperation((SubtractOperation) op);
            } else if (op instanceof MultiplyOperation) {
                return evalMultiplyOperation((MultiplyOperation) op);
            }
        }

        return expression;
    }

    private Expression evalAddOperation(AddOperation expr) {
        Literal lhs = (Literal) expr.lhs;
        Literal rhs = (Literal) expr.rhs;

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
        Literal lhs = (Literal) expr.lhs;
        Literal rhs = (Literal) expr.rhs;

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
        Literal left = (Literal) expr.lhs;
        Literal right = (Literal) expr.rhs;

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


    private void applyForLoop(ForLoop forLoop, List<ASTNode> parentBody) {
        Expression startExpr = evaluateExpression(forLoop.rangeStart);
        Expression endExpr = evaluateExpression(forLoop.rangeEnd);

        if (!(startExpr instanceof ScalarLiteral) || !(endExpr instanceof ScalarLiteral)) return;

        int start = ((ScalarLiteral) startExpr).value;
        int end = ((ScalarLiteral) endExpr).value;

        for (int i = start; i <= end; i++) {
            for (ASTNode bodyNode : forLoop.body) {
                if (!(bodyNode instanceof Stylerule)) continue;

                Stylerule original = (Stylerule) bodyNode;
                Stylerule newStylerule = new Stylerule();
                newStylerule.selectors = new ArrayList<>();
                newStylerule.body = new ArrayList<>();

                for (Selector sel : original.selectors) {
                    if (sel instanceof IdSelector)
                        newStylerule.selectors.add(new IdSelector(((IdSelector) sel).id + i));
                    else if (sel instanceof ClassSelector)
                        newStylerule.selectors.add(new ClassSelector(((ClassSelector) sel).cls + i));
                    else if (sel instanceof TagSelector)
                        newStylerule.selectors.add(new TagSelector(((TagSelector) sel).tag + i));
                }

                for (ASTNode innerNode : original.body) {
                    if (innerNode instanceof Declaration) {
                        Declaration originalDecl = (Declaration) innerNode;
                        Declaration newDecl = new Declaration();
                        newDecl.property = originalDecl.property;

                        Expression replaced = replaceLoopIdentifier(originalDecl.expression, i);
                        newDecl.expression = evaluateExpression(replaced);

                        newStylerule.body.add(newDecl);
                    }
                }

                parentBody.add(newStylerule);
            }
        }
    }

    private Expression replaceLoopIdentifier(Expression expr, int value) {
        if (expr instanceof LoopIdentifier) {
            return new ScalarLiteral(value);
        }

        if (expr instanceof PixelLiteral) {
            return new PixelLiteral(((PixelLiteral) expr).value);
        }

        if (expr instanceof PercentageLiteral) {
            return new PercentageLiteral(((PercentageLiteral) expr).value);
        }

        if (expr instanceof ScalarLiteral) {
            return new ScalarLiteral(((ScalarLiteral) expr).value);
        }

        if (expr instanceof VariableReference) {
            return expr;
        }

        if (expr instanceof Operation) {
            Operation op = (Operation) expr;
            Operation newOp;

            if (op instanceof MultiplyOperation) newOp = new MultiplyOperation();
            else if (op instanceof AddOperation) newOp = new AddOperation();
            else if (op instanceof SubtractOperation) newOp = new SubtractOperation();
            else return expr;

            newOp.lhs = replaceLoopIdentifier(op.lhs, value);
            newOp.rhs = replaceLoopIdentifier(op.rhs, value);
            return newOp;
        }

        return expr;
    }
}