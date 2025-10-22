package nl.han.ica.icss.checker;

import nl.han.ica.datastructures.IHANLinkedList;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.ColorLiteral;
import nl.han.ica.icss.ast.literals.PixelLiteral;
import nl.han.ica.icss.ast.operations.AddOperation;
import nl.han.ica.icss.ast.operations.MultiplyOperation;
import nl.han.ica.icss.ast.operations.SubtractOperation;
import nl.han.ica.icss.ast.types.ExpressionType;

import java.util.HashMap;
import java.util.LinkedList;


public class Checker {

    private LinkedList<HashMap<String, ExpressionType>> variableTypes;

    public void check(AST ast) {

        checkStylesheet(ast.root);
        // variableTypes = new HANLinkedList<>();


    }



    private void checkStylesheet(Stylesheet stylesheet){
          checkStylerule((Stylerule)stylesheet.getChildren().get(0));

    }

    private void checkStylerule(Stylerule stylerule) {
        for(ASTNode child : stylerule.getChildren()){
            if(child instanceof Declaration){
                checkDeclaration((Declaration)child);
            }
        }
    }

    private void checkDeclaration(Declaration declaration) {
        if (declaration.property.name.equals("width")) {
            checkWidth(declaration);
        }
        if (declaration.property.name.equals("color")) {
            checkColor(declaration);
        }



    }

    private void checkWidth(Declaration declaration) {
        if (!(declaration.expression instanceof PixelLiteral ||
                declaration.expression instanceof AddOperation ||
                declaration.expression instanceof MultiplyOperation ||
                declaration.expression instanceof SubtractOperation ||
                declaration.expression instanceof VariableReference)){
            declaration.setError("Wesh je moet pixels gebruiken eh neef");
        }
    }

    private void checkColor(Declaration declaration) {
        if (!(declaration.expression instanceof ColorLiteral ||
                declaration.expression instanceof VariableReference)){
            declaration.setError("Wesh je moet #HEX gebruiken eh neef");
        }
    }


}
