package nl.han.ica.icss.checker;

import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.loops.ForLoop;
import nl.han.ica.icss.ast.loops.LoopIdentifier;
import nl.han.ica.icss.ast.operations.AddOperation;
import nl.han.ica.icss.ast.operations.MultiplyOperation;
import nl.han.ica.icss.ast.operations.SubtractOperation;
import nl.han.ica.icss.ast.types.ExpressionType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class Checker {

    private LinkedList<HashMap<String, ExpressionType>> variableScopes;

    public void check(AST ast) {
        variableScopes = new LinkedList<>();
        checkStylesheet(ast.root);
    }

    private void checkStylesheet(Stylesheet stylesheet) {
        pushScope();

        for (ASTNode child : stylesheet.getChildren()) {
            if (child instanceof VariableAssignment) {
                checkVariableAssignment((VariableAssignment) child);
            } else if (child instanceof Stylerule) {
                checkStylerule((Stylerule) child);
            } else if (child instanceof ForLoop) {
                checkForLoop((ForLoop) child);
            }
        }

        popScope();
    }

    private void checkVariableAssignment(VariableAssignment assignment) {
        ExpressionType assignmentType = getExpressionType(assignment.expression);

        if (assignmentType == ExpressionType.UNDEFINED) {
            setAssignmentError(assignment);
            return;
        }

        String variableName = assignment.name.name;
        variableScopes.peek().put(variableName, assignmentType);
    }

    private void setAssignmentError(VariableAssignment assignment) {
        if (assignment.expression instanceof VariableReference) {
            String variableName = ((VariableReference) assignment.expression).name;
            assignment.setError("Variabele '" + variableName + "' is niet gedeclareerd.");
        } else {
            assignment.setError("Ongeldige uitdrukking voor variabele: " + assignment.name.name);
        }
    }

    private void checkStylerule(Stylerule stylerule) {
        pushScope();
        checkBody(stylerule.body);
        popScope();
    }

    private void checkIfClause(IfClause ifClause) {
        checkConditionType(ifClause);

        pushScope();
        checkBody(ifClause.body);
        popScope();

        if (ifClause.elseClause != null) {
            checkElseClause(ifClause.elseClause);
        }
    }

    private void checkConditionType(IfClause ifClause) {
        ExpressionType conditionType = getExpressionType(ifClause.conditionalExpression);

        if (conditionType == ExpressionType.UNDEFINED) {
            ifClause.setError("Conditie variabele is niet gedeclareerd of heeft een onbekend type.");
        } else if (conditionType != ExpressionType.BOOL) {
            ifClause.setError("If-conditie vereist een BOOLEAN type, maar heeft type " + conditionType.name() + ".");
        }
    }

    private void checkElseClause(ElseClause elseClause) {
        pushScope();
        checkBody(elseClause.body);
        popScope();
    }

    private void checkBody(ArrayList<ASTNode> body) {
        for (ASTNode child : body) {
            if (child instanceof VariableAssignment) {
                checkVariableAssignment((VariableAssignment) child);
            }
        }

        for (ASTNode child : body) {
            if (child instanceof Declaration) {
                checkDeclaration((Declaration) child);
            } else if (child instanceof IfClause) {
                checkIfClause((IfClause) child);
            }
        }
    }

    private void checkDeclaration(Declaration declaration) {
        String propertyName = declaration.property.name;

        if (propertyName.equals("width") || propertyName.equals("height")) {
            checkPixelOrPercentageProperty(declaration);
        } else if (propertyName.equals("color") || propertyName.equals("background-color")) {
            checkColorProperty(declaration);
        }
    }

    private ExpressionType resolveVariableType(String variableName) {
        for (HashMap<String, ExpressionType> scope : variableScopes) {
            if (scope.containsKey(variableName)) {
                return scope.get(variableName);
            }
        }
        return ExpressionType.UNDEFINED;
    }

    private ExpressionType getExpressionType(Expression expression) {
        if (expression instanceof PixelLiteral) {
            return ExpressionType.PIXEL;
        }
        if (expression instanceof PercentageLiteral) {
            return ExpressionType.PERCENTAGE;
        }
        if (expression instanceof ColorLiteral) {
            return ExpressionType.COLOR;
        }
        if (expression instanceof ScalarLiteral) {
            return ExpressionType.SCALAR;
        }
        if (expression instanceof BoolLiteral) {
            return ExpressionType.BOOL;
        }

        if (expression instanceof LoopIdentifier) {
            return ExpressionType.LOOP_IDENTIFIER;
        }

        if (expression instanceof VariableReference) {
            return resolveVariableType(((VariableReference) expression).name);
        }

        if (expression instanceof AddOperation) {
            return checkAddOperation((Operation) expression);
        }
        if (expression instanceof SubtractOperation) {
            return checkSubtractOperation((Operation) expression);
        }
        if (expression instanceof MultiplyOperation) {
            return checkMultiplyOperation((Operation) expression);
        }

        return ExpressionType.UNDEFINED;
    }

    private ExpressionType checkAddOperation(Operation operation) {
        return checkAddOrSubtractOperation(operation, "Add");
    }

    private ExpressionType checkSubtractOperation(Operation operation) {
        return checkAddOrSubtractOperation(operation, "Subtract");
    }

    private ExpressionType checkAddOrSubtractOperation(Operation operation, String operationName) {
        ExpressionType leftType = getExpressionType(operation.lhs);
        ExpressionType rightType = getExpressionType(operation.rhs);

        if (leftType == ExpressionType.UNDEFINED || rightType == ExpressionType.UNDEFINED) {
            return ExpressionType.UNDEFINED;
        }

        if (leftType == ExpressionType.COLOR || rightType == ExpressionType.COLOR) {
            operation.setError("Operatie " + operationName + " mag geen COLOR gebruiken.");
            return ExpressionType.UNDEFINED;
        }

        if (isValidArithmeticType(leftType) && leftType.equals(rightType)) {
            return leftType;
        }

        operation.setError("Type mismatch in " + operationName + ": kan geen " + leftType.name() + " met " + rightType.name() + " combineren.");
        return ExpressionType.UNDEFINED;
    }

    private boolean isValidArithmeticType(ExpressionType type) {
        return type == ExpressionType.PIXEL
                || type == ExpressionType.PERCENTAGE
                || type == ExpressionType.SCALAR;
    }

    private ExpressionType checkMultiplyOperation(Operation operation) {
        ExpressionType leftType = getExpressionType(operation.lhs);
        ExpressionType rightType = getExpressionType(operation.rhs);

        if (leftType == ExpressionType.UNDEFINED || rightType == ExpressionType.UNDEFINED) {
            return ExpressionType.UNDEFINED;
        }

        if (leftType == ExpressionType.COLOR || rightType == ExpressionType.COLOR) {
            operation.setError("Vermenigvuldiging mag geen COLOR gebruiken.");
            return ExpressionType.UNDEFINED;
        }

        if (leftType == ExpressionType.SCALAR && rightType == ExpressionType.SCALAR) {
            return ExpressionType.SCALAR;
        }

        if (leftType == ExpressionType.SCALAR || rightType == ExpressionType.SCALAR) {
            return (leftType == ExpressionType.SCALAR) ? rightType : leftType;
        }

        operation.setError("Type mismatch in multiply: ten minste één operand moet SCALAR zijn.");
        return ExpressionType.UNDEFINED;
    }

    private void checkPixelOrPercentageProperty(Declaration declaration) {
        ExpressionType resultType = getExpressionType(declaration.expression);



        if (resultType == ExpressionType.PIXEL || resultType == ExpressionType.PERCENTAGE) {
            return;
        }

        if (resultType == ExpressionType.UNDEFINED) {
            declaration.setError("Variabele is niet gedeclareerd of heeft een onbekend type.");
            return;
        }

        declaration.setError("Property '" + declaration.property.name + "' vereist PIXEL of PERCENTAGE, maar resulteert in " + resultType.name() + ".");
    }

    private void checkColorProperty(Declaration declaration) {
        ExpressionType resultType = getExpressionType(declaration.expression);

        if (resultType == ExpressionType.COLOR) {
            return;
        }

        if (resultType == ExpressionType.UNDEFINED) {
            declaration.setError("Variabele is niet gedeclareerd of heeft een onbekend type.");
            return;
        }

        declaration.setError("Property '" + declaration.property.name + "' vereist COLOR, maar resulteert in " + resultType.name());
    }

    private void checkForLoop(ForLoop forLoop) {
        if (forLoop.loopVariable == null) {
            forLoop.setError("For-loop mist een loopvariabele.");
            return;
        }

        String loopVariableName = forLoop.loopVariable.name;

        checkForLoopRangeValues(forLoop);

        pushScope();
        variableScopes.peek().put(loopVariableName, ExpressionType.LOOP_IDENTIFIER);

        if (forLoop.body == null || forLoop.body.isEmpty()) {
            forLoop.setError("For-loop heeft een lege body.");
        } else {
            checkBody(forLoop.body);
        }

        popScope();
    }

    private void checkForLoopRangeValues(ForLoop forLoop) {
        ExpressionType startType = getExpressionType(forLoop.rangeStart);
        ExpressionType endType = getExpressionType(forLoop.rangeEnd);

        if (!areValidLoopRangeTypes(startType, endType)) {
            setForLoopRangeError(forLoop, startType, endType, false);
            return;
        }

        if (forLoop.rangeStart instanceof ScalarLiteral && forLoop.rangeEnd instanceof ScalarLiteral) {
            ScalarLiteral startLit = (ScalarLiteral) forLoop.rangeStart;
            ScalarLiteral endLit = (ScalarLiteral) forLoop.rangeEnd;

            if (startLit.value > endLit.value) {
                setForLoopRangeError(forLoop, startType, endType, true);
            }
        }

    }

    private void setForLoopRangeError(ForLoop forLoop, ExpressionType startType, ExpressionType endType, boolean isRangeError) {
        if (isRangeError) {
            forLoop.setError("For-loop 'from' waarde mag niet groter zijn dan 'to' waarde.");
        } else if (startType == ExpressionType.UNDEFINED || endType == ExpressionType.UNDEFINED) {
            forLoop.setError("For-loop gebruikt ongedefinieerde waarden voor 'from' en/of 'to'.");
        } else {
            forLoop.setError("For-loop vereist SCALAR waarden voor 'from' en 'to', maar kreeg " + startType + " en " + endType + ".");
        }
    }


    private boolean areValidLoopRangeTypes(ExpressionType startType, ExpressionType endType) {
        return startType == ExpressionType.SCALAR && endType == ExpressionType.SCALAR;
    }

    private void pushScope() {
        variableScopes.push(new HashMap<>());
    }

    private void popScope() {
        variableScopes.pop();
    }
}