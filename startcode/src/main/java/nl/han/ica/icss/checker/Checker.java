package nl.han.ica.icss.checker;

import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.operations.AddOperation;
import nl.han.ica.icss.ast.operations.MultiplyOperation;
import nl.han.ica.icss.ast.operations.SubtractOperation;
import nl.han.ica.icss.ast.types.ExpressionType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class Checker {

    private LinkedList<HashMap<String, ExpressionType>> variableTypes;

    public void check(AST ast) {
        variableTypes = new LinkedList<>();
        checkStylesheet(ast.root);
    }

    private void checkStylesheet(Stylesheet stylesheet) {
        variableTypes.push(new HashMap<>());
        for (ASTNode child : stylesheet.getChildren()) {
            if (child instanceof VariableAssignment) {
                checkVariableAssignment((VariableAssignment) child);
            } else if (child instanceof Stylerule) {
                checkStylerule((Stylerule) child);
            }
        }
        variableTypes.pop();
    }

    private void checkVariableAssignment(VariableAssignment assignment) {
        ExpressionType assignmentType = getExpressionType(assignment.expression);

        if (assignmentType == ExpressionType.UNDEFINED) {
            if (assignment.expression instanceof VariableReference) {
                assignment.setError("Variabele '" + ((VariableReference) assignment.expression).name + "' is niet gedeclareerd.");
            } else {
                assignment.setError("Ongeldige uitdrukking voor variabele: " + assignment.name.name);
            }
            return;
        }

        String varName = assignment.name.name;
        variableTypes.peek().put(varName, assignmentType);
    }

    private void checkStylerule(Stylerule stylerule) {
        variableTypes.push(new HashMap<>());
        checkBody(stylerule.body);
        variableTypes.pop();
    }

    private void checkIfClause(IfClause ifClause) {
        checkConditionType(ifClause);

        variableTypes.push(new HashMap<>());
        checkBody(ifClause.body);
        variableTypes.pop();

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
        variableTypes.push(new HashMap<>());
        checkBody(elseClause.body);
        variableTypes.pop();
    }

    private void checkBody(ArrayList<ASTNode> body) {
        for (ASTNode child : body) {
            if (child instanceof VariableAssignment) {
                checkVariableAssignment((VariableAssignment) child);
            }
        }

        for(ASTNode child : body){
            if(child instanceof Declaration){
                checkDeclaration((Declaration)child);
            } else if (child instanceof IfClause){
                checkIfClause((IfClause)child);
            }
        }
    }

    private void checkDeclaration(Declaration declaration) {
        String propertyName = declaration.property.name;

        if (propertyName.equals("width") || propertyName.equals("height")) {
            checkPixelOrPercentage(declaration);
        } else if (propertyName.equals("color") || propertyName.equals("background-color")) {
            checkColor(declaration);
        }
    }

    private ExpressionType resolveVariableType(String name) {
        for (HashMap<String, ExpressionType> scope : variableTypes) {
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        return ExpressionType.UNDEFINED;
    }

    private ExpressionType getExpressionType(Expression expression) {
        if (expression instanceof PixelLiteral) return ExpressionType.PIXEL;
        if (expression instanceof PercentageLiteral) return ExpressionType.PERCENTAGE;
        if (expression instanceof ColorLiteral) return ExpressionType.COLOR;
        if (expression instanceof ScalarLiteral) return ExpressionType.SCALAR;
        if (expression instanceof BoolLiteral) return ExpressionType.BOOL;

        if (expression instanceof VariableReference) {
            return resolveVariableType(((VariableReference) expression).name);
        }

        if (expression instanceof AddOperation) return checkAddOperation((Operation) expression);
        if (expression instanceof SubtractOperation) return checkSubtractOperation((Operation) expression);
        if (expression instanceof MultiplyOperation) return checkMultiplyOperation((Operation) expression);

        return ExpressionType.UNDEFINED;
    }


    private ExpressionType checkAddOperation(Operation operation) {
        return checkAddOrSubtractOperation(operation, "Add");
    }

    private ExpressionType checkSubtractOperation(Operation operation) {
        return checkAddOrSubtractOperation(operation, "Subtract");
    }

    private ExpressionType checkAddOrSubtractOperation(Operation operation, String opName) {
        ExpressionType lhsType = getExpressionType(operation.lhs);
        ExpressionType rhsType = getExpressionType(operation.rhs);

        if (lhsType == ExpressionType.UNDEFINED || rhsType == ExpressionType.UNDEFINED) return ExpressionType.UNDEFINED;

        if (lhsType == ExpressionType.COLOR || rhsType == ExpressionType.COLOR) {
            operation.setError("Operatie " + opName + " mag geen COLOR gebruiken.");
            return ExpressionType.UNDEFINED;
        }

        if (lhsType.equals(rhsType) &&
                (lhsType == ExpressionType.PIXEL || lhsType == ExpressionType.PERCENTAGE || lhsType == ExpressionType.SCALAR)) {
            return lhsType;
        }

        operation.setError("Type mismatch in " + opName + ": kan geen " + lhsType.name() + " met " + rhsType.name() + " combineren.");
        return ExpressionType.UNDEFINED;
    }

    private ExpressionType checkMultiplyOperation(Operation operation) {
        ExpressionType lhsType = getExpressionType(operation.lhs);
        ExpressionType rhsType = getExpressionType(operation.rhs);

        if (lhsType == ExpressionType.UNDEFINED || rhsType == ExpressionType.UNDEFINED) return ExpressionType.UNDEFINED;

        if (lhsType == ExpressionType.COLOR || rhsType == ExpressionType.COLOR) {
            operation.setError("Vermenigvuldiging mag geen COLOR gebruiken.");
            return ExpressionType.UNDEFINED;
        }

        if (lhsType == ExpressionType.SCALAR || rhsType == ExpressionType.SCALAR) {
            if (lhsType == ExpressionType.SCALAR && rhsType == ExpressionType.SCALAR) return ExpressionType.SCALAR;
            return (lhsType == ExpressionType.SCALAR) ? rhsType : lhsType;
        }

        operation.setError("Type mismatch in multiply: ten minste één operand moet SCALAR zijn.");
        return ExpressionType.UNDEFINED;
    }

    private void checkPixelOrPercentage(Declaration declaration) {
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

    private void checkColor(Declaration declaration) {
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

}