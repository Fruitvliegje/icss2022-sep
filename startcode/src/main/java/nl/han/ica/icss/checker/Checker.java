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
            }
        }

        for (ASTNode child : stylesheet.getChildren()) {
            if (child instanceof Stylerule) {
                checkStylerule((Stylerule) child);
            }
        }
        variableTypes.pop();
    }

    private void checkVariableAssignment(VariableAssignment assignment) {
        ExpressionType assignmentType = getExpressionType(assignment.expression);

        if (assignmentType == ExpressionType.UNDEFINED) {
            assignment.setError("Ongeldige uitdrukking voor variabele: " + assignment.name.name);
            return;
        }

        String varName = assignment.name.name;
        variableTypes.peek().put(varName, assignmentType);
    }

    private void checkStylerule(Stylerule stylerule) {
        for (ASTNode child : stylerule.getChildren()) {
            if (child instanceof Declaration) {
                checkDeclaration((Declaration) child);
            } else if (child instanceof IfClause) {
                checkIfClause((IfClause) child);
            }
        }
    }

    private void checkIfClause(IfClause ifClause) {
        if (!(ifClause.conditionalExpression instanceof VariableReference
                || ifClause.conditionalExpression instanceof BoolLiteral)) {
            ifClause.setError("If-conditie moet een variabele of boolean literal bevatten.");
        }

        ExpressionType conditionType;
        if (ifClause.conditionalExpression instanceof VariableReference) {
            String varName = ((VariableReference) ifClause.conditionalExpression).name;
            conditionType = resolveVariableType(varName);

            if (conditionType == ExpressionType.UNDEFINED) {
                ifClause.setError("Variabele '" + varName + "' is niet gedeclareerd.");
            } else if (conditionType != ExpressionType.BOOL) {
                ifClause.setError("If-conditie vereist een BOOLEAN type, maar de variabele '"
                        + varName + "' heeft type " + conditionType.name() + ".");
            }
        }

        checkBody(ifClause.body);

        if (ifClause.elseClause != null) {
            checkElseClause(ifClause.elseClause);
        }
    }

    private void checkElseClause(ElseClause elseClause) {
        for (ASTNode child : elseClause.body) {
            if (child instanceof Declaration) {
                checkDeclaration((Declaration) child);
            } else if (child instanceof IfClause) {
                elseClause.setError("Geneste IF-statements zijn niet toegestaan binnen een ELSE-blok.");
            }
        }
    }

    private void checkBody(ArrayList<ASTNode> body) {
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
        ExpressionType lhsType = getExpressionType(operation.lhs);
        ExpressionType rhsType = getExpressionType(operation.rhs);

        if (lhsType == ExpressionType.UNDEFINED || rhsType == ExpressionType.UNDEFINED) return ExpressionType.UNDEFINED;

        if (lhsType.equals(rhsType) &&
                (lhsType == ExpressionType.PIXEL || lhsType == ExpressionType.PERCENTAGE || lhsType == ExpressionType.SCALAR)) {
            return lhsType;
        }

        operation.setError("Type mismatch in Add/Subtract: kan geen " + lhsType.name() + " met " + rhsType.name() + " combineren.");
        return ExpressionType.UNDEFINED;
    }

    private ExpressionType checkSubtractOperation(Operation operation) {
        ExpressionType lhsType = getExpressionType(operation.lhs);
        ExpressionType rhsType = getExpressionType(operation.rhs);

        if (lhsType == ExpressionType.UNDEFINED || rhsType == ExpressionType.UNDEFINED) {
            return ExpressionType.UNDEFINED;
        }

        if (lhsType.equals(rhsType) &&
                (lhsType == ExpressionType.PIXEL || lhsType == ExpressionType.PERCENTAGE || lhsType == ExpressionType.SCALAR)) {
            return lhsType;
        }

        operation.setError("Type mismatch in Subtract: kan geen " + lhsType.name() + " van " + rhsType.name() + " aftrekken.");
        return ExpressionType.UNDEFINED;
    }

    private ExpressionType checkMultiplyOperation(Operation operation) {
        ExpressionType lhsType = getExpressionType(operation.lhs);
        ExpressionType rhsType = getExpressionType(operation.rhs);

        if (lhsType == ExpressionType.UNDEFINED || rhsType == ExpressionType.UNDEFINED) return ExpressionType.UNDEFINED;

        if (lhsType == ExpressionType.SCALAR && rhsType == ExpressionType.SCALAR) {
            return ExpressionType.SCALAR;
        }

        if (lhsType == ExpressionType.SCALAR && rhsType != ExpressionType.COLOR) {
            return rhsType;
        }
        if (rhsType == ExpressionType.SCALAR && lhsType != ExpressionType.COLOR) {
            return lhsType;
        }

        operation.setError("Type mismatch in multiply: ten minste een operand moet SCALAR zijn en geen van beide mag COLOR zijn.");
        return ExpressionType.UNDEFINED;
    }

    private void checkPixelOrPercentage(Declaration declaration) {
        ExpressionType resultType = getExpressionType(declaration.expression);

        if (resultType == ExpressionType.PIXEL || resultType == ExpressionType.PERCENTAGE) {
            return;
        }

        if (declaration.expression instanceof VariableReference && resultType == ExpressionType.UNDEFINED) {
            declaration.setError("Variabele '" + ((VariableReference) declaration.expression).name + "' is niet gedeclareerd of heeft een onbekend type.");
            return;
        }

        declaration.setError("Property '" + declaration.property.name + "' vereist PIXEL of PERCENTAGE, maar resulteert in " + resultType.name() + ".");
    }



    private void checkColor(Declaration declaration) {
        ExpressionType resultType = getExpressionType(declaration.expression);

        if (resultType == ExpressionType.COLOR) {
            return;
        }

        if (declaration.expression instanceof VariableReference && resultType == ExpressionType.UNDEFINED) {
            declaration.setError("Variabele '" + ((VariableReference) declaration.expression).name + "' is niet gedeclareerd of heeft een onbekend type.");
            return;
        }

        declaration.setError("Property '" + declaration.property.name + "' vereist COLOR, maar resulteert in " + resultType.name());
    }

}