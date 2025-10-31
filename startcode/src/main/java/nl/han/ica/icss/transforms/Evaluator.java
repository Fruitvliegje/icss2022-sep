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

    private LinkedList<HashMap<String, Literal>> variableScopes;

    @Override
    public void apply(AST ast) {
        variableScopes = new LinkedList<>();
        applyStylesheet(ast.root);
    }

    private void applyStylesheet(Stylesheet stylesheet) {
        pushScope();

        LinkedList<ASTNode> evaluatedChildren = new LinkedList<>();

        for (ASTNode child : stylesheet.getChildren()) {
            if (child instanceof VariableAssignment) {
                applyVariableAssignment((VariableAssignment) child);
            } else if (child instanceof Stylerule) {
                applyStylerule((Stylerule) child);
                evaluatedChildren.add(child);
            } else if (child instanceof ForLoop) {
                applyForLoop((ForLoop) child, evaluatedChildren);
            }
        }

        stylesheet.body = new ArrayList<>(evaluatedChildren);
        popScope();
    }

    private void applyVariableAssignment(VariableAssignment assignment) {
        Expression evaluatedExpr = evaluateExpression(assignment.expression);
        if (!(evaluatedExpr instanceof Literal)) {
            return;
        }

        assignment.expression = evaluatedExpr;
        variableScopes.peek().put(assignment.name.name, (Literal) evaluatedExpr);
    }

    private void applyStylerule(Stylerule stylerule) {
        pushScope();

        LinkedList<ASTNode> evaluatedBody = new LinkedList<>();
        applyBody(stylerule.body, evaluatedBody);

        stylerule.body = new ArrayList<>(evaluatedBody);
        popScope();
    }

    private void applyIfClause(IfClause ifClause, List<ASTNode> parentBody) {
        Expression conditionExpr = evaluateExpression(ifClause.conditionalExpression);

        boolean conditionTrue = (conditionExpr instanceof BoolLiteral)
                && ((BoolLiteral) conditionExpr).value;

        if (conditionTrue) {
            pushScope();
            applyBody(ifClause.body, parentBody);
            popScope();
        } else if (ifClause.elseClause != null) {
            applyElseClause(ifClause.elseClause, parentBody);
        }
    }

    private void applyElseClause(ElseClause elseClause, List<ASTNode> parentBody) {
        pushScope();
        applyBody(elseClause.body, parentBody);
        popScope();
    }

    private void applyBody(ArrayList<ASTNode> body, List<ASTNode> parentBody) {
        for (ASTNode child : body) {
            if (child instanceof VariableAssignment) {
                applyVariableAssignment((VariableAssignment) child);
            }
        }

        for (ASTNode child : body) {
            if (child instanceof Declaration) {
                applyDeclaration((Declaration) child);
                addOrReplaceDeclaration((Declaration) child, parentBody);
            } else if (child instanceof IfClause) {
                applyIfClause((IfClause) child, parentBody);
            } else if (child instanceof Stylerule) {
                applyStylerule((Stylerule) child);
                parentBody.add(child);
            }
        }
    }

    private void addOrReplaceDeclaration(Declaration declaration, List<ASTNode> parentBody) {
        for (int i = 0; i < parentBody.size(); i++) {
            ASTNode existingNode = parentBody.get(i);
            if (existingNode instanceof Declaration) {
                Declaration existingDecl = (Declaration) existingNode;
                if (existingDecl.property.name.equals(declaration.property.name)) {
                    parentBody.set(i, declaration);
                    return;
                }
            }
        }
        parentBody.add(declaration);
    }

    private void applyDeclaration(Declaration declaration) {
        Expression evaluatedExpr = evaluateExpression(declaration.expression);
        if (evaluatedExpr instanceof Literal) {
            declaration.expression = evaluatedExpr;
        }
    }

    private Expression evaluateExpression(Expression expression) {
        if (expression instanceof Literal) {
            return expression;
        }

        if (expression instanceof VariableReference) {
            Literal resolvedValue = resolveVariable((VariableReference) expression);
            return resolvedValue != null ? resolvedValue : expression;
        }

        if (expression instanceof Operation) {
            return evaluateOperation((Operation) expression);
        }

        return expression;
    }

    private Expression evaluateOperation(Operation operation) {
        operation.lhs = evaluateExpression(operation.lhs);
        operation.rhs = evaluateExpression(operation.rhs);

        if (!(operation.lhs instanceof Literal) || !(operation.rhs instanceof Literal)) {
            return operation;
        }

        if (operation instanceof AddOperation) {
            return evaluateAddOperation((Literal) operation.lhs, (Literal) operation.rhs, operation);
        } else if (operation instanceof SubtractOperation) {
            return evaluateSubtractOperation((Literal) operation.lhs, (Literal) operation.rhs, operation);
        } else if (operation instanceof MultiplyOperation) {
            return evaluateMultiplyOperation((Literal) operation.lhs, (Literal) operation.rhs, operation);
        }

        return operation;
    }

    private Expression evaluateAddOperation(Literal lhs, Literal rhs, Operation fallback) {
        if (lhs instanceof PixelLiteral && rhs instanceof PixelLiteral) {
            return new PixelLiteral(
                    ((PixelLiteral) lhs).value + ((PixelLiteral) rhs).value
            );
        }
        if (lhs instanceof PercentageLiteral && rhs instanceof PercentageLiteral) {
            return new PercentageLiteral(
                    ((PercentageLiteral) lhs).value + ((PercentageLiteral) rhs).value
            );
        }
        if (lhs instanceof ScalarLiteral && rhs instanceof ScalarLiteral) {
            return new ScalarLiteral(
                    ((ScalarLiteral) lhs).value + ((ScalarLiteral) rhs).value
            );
        }
        return fallback;
    }

    private Expression evaluateSubtractOperation(Literal lhs, Literal rhs, Operation fallback) {
        if (lhs instanceof PixelLiteral && rhs instanceof PixelLiteral) {
            return new PixelLiteral(
                    ((PixelLiteral) lhs).value - ((PixelLiteral) rhs).value
            );
        }
        if (lhs instanceof PercentageLiteral && rhs instanceof PercentageLiteral) {
            return new PercentageLiteral(
                    ((PercentageLiteral) lhs).value - ((PercentageLiteral) rhs).value
            );
        }
        if (lhs instanceof ScalarLiteral && rhs instanceof ScalarLiteral) {
            return new ScalarLiteral(
                    ((ScalarLiteral) lhs).value - ((ScalarLiteral) rhs).value
            );
        }
        return fallback;
    }

    private Expression evaluateMultiplyOperation(Literal lhs, Literal rhs, Operation fallback) {
        if (lhs instanceof ScalarLiteral && rhs instanceof PixelLiteral) {
            return new PixelLiteral(
                    ((ScalarLiteral) lhs).value * ((PixelLiteral) rhs).value
            );
        }
        if (lhs instanceof PixelLiteral && rhs instanceof ScalarLiteral) {
            return new PixelLiteral(
                    ((PixelLiteral) lhs).value * ((ScalarLiteral) rhs).value
            );
        }

        if (lhs instanceof ScalarLiteral && rhs instanceof PercentageLiteral) {
            return new PercentageLiteral(
                    ((ScalarLiteral) lhs).value * ((PercentageLiteral) rhs).value
            );
        }
        if (lhs instanceof PercentageLiteral && rhs instanceof ScalarLiteral) {
            return new PercentageLiteral(
                    ((PercentageLiteral) lhs).value * ((ScalarLiteral) rhs).value
            );
        }

        if (lhs instanceof ScalarLiteral && rhs instanceof ScalarLiteral) {
            return new ScalarLiteral(
                    ((ScalarLiteral) lhs).value * ((ScalarLiteral) rhs).value
            );
        }

        return fallback;
    }

    private Literal resolveVariable(VariableReference reference) {
        for (HashMap<String, Literal> scope : variableScopes) {
            if (scope.containsKey(reference.name)) {
                return scope.get(reference.name);
            }
        }
        return null;
    }

    private void applyForLoop(ForLoop forLoop, List<ASTNode> parentBody) {
        Expression startExpr = evaluateExpression(forLoop.rangeStart);
        Expression endExpr = evaluateExpression(forLoop.rangeEnd);

        if (!(startExpr instanceof ScalarLiteral) || !(endExpr instanceof ScalarLiteral)) {
            return;
        }

        int start = ((ScalarLiteral) startExpr).value;
        int end = ((ScalarLiteral) endExpr).value;

        for (int i = start; i <= end; i++) {
            for (ASTNode bodyNode : forLoop.body) {
                if (bodyNode instanceof Stylerule) {
                    Stylerule expandedStylerule = expandStyleruleForIteration(
                            (Stylerule) bodyNode, i
                    );
                    parentBody.add(expandedStylerule);
                }
            }
        }
    }

    private Stylerule expandStyleruleForIteration(Stylerule originalStylerule, int iteration) {
        Stylerule expandedStylerule = new Stylerule();
        expandedStylerule.selectors = new ArrayList<>();
        expandedStylerule.body = new ArrayList<>();

        for (Selector selector : originalStylerule.selectors) {
            expandedStylerule.selectors.add(expandSelector(selector, iteration));
        }

        for (ASTNode node : originalStylerule.body) {
            if (node instanceof Declaration) {
                Declaration originalDecl = (Declaration) node;
                Declaration expandedDecl = new Declaration();
                expandedDecl.property = originalDecl.property;

                Expression replacedExpr = replaceLoopIdentifier(originalDecl.expression, iteration);
                expandedDecl.expression = evaluateExpression(replacedExpr);

                expandedStylerule.body.add(expandedDecl);
            }
        }

        return expandedStylerule;
    }

    private Selector expandSelector(Selector selector, int iteration) {
        if (selector instanceof IdSelector) {
            return new IdSelector(((IdSelector) selector).id + iteration);
        } else if (selector instanceof ClassSelector) {
            return new ClassSelector(((ClassSelector) selector).cls + iteration);
        } else if (selector instanceof TagSelector) {
            return new TagSelector(((TagSelector) selector).tag + iteration);
        }
        return selector;
    }

    private Expression replaceLoopIdentifier(Expression expression, int value) {
        if (expression instanceof LoopIdentifier) {
            return new ScalarLiteral(value);
        }

        if (expression instanceof PixelLiteral) {
            return new PixelLiteral(((PixelLiteral) expression).value);
        }

        if (expression instanceof PercentageLiteral) {
            return new PercentageLiteral(((PercentageLiteral) expression).value);
        }

        if (expression instanceof ScalarLiteral) {
            return new ScalarLiteral(((ScalarLiteral) expression).value);
        }

        if (expression instanceof VariableReference) {
            return expression;
        }

        if (expression instanceof Operation) {
            return replaceLoopIdentifierInOperation((Operation) expression, value);
        }

        return expression;
    }

    private Operation replaceLoopIdentifierInOperation(Operation operation, int value) {
        Operation newOperation = createOperationInstance(operation);
        if (newOperation == null) {
            return operation;
        }

        newOperation.lhs = replaceLoopIdentifier(operation.lhs, value);
        newOperation.rhs = replaceLoopIdentifier(operation.rhs, value);
        return newOperation;
    }

    private Operation createOperationInstance(Operation operation) {
        if (operation instanceof MultiplyOperation) {
            return new MultiplyOperation();
        } else if (operation instanceof AddOperation) {
            return new AddOperation();
        } else if (operation instanceof SubtractOperation) {
            return new SubtractOperation();
        }
        return null;
    }

    private void pushScope() {
        variableScopes.push(new HashMap<>());
    }

    private void popScope() {
        variableScopes.pop();
    }

}