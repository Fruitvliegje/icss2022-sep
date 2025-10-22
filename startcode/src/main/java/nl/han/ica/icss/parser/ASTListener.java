package nl.han.ica.icss.parser;

import java.awt.*;
import java.util.Stack;


import nl.han.ica.datastructures.IHANStack;
import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.operations.AddOperation;
import nl.han.ica.icss.ast.operations.MultiplyOperation;
import nl.han.ica.icss.ast.operations.SubtractOperation;
import nl.han.ica.icss.ast.selectors.ClassSelector;
import nl.han.ica.icss.ast.selectors.IdSelector;
import nl.han.ica.icss.ast.selectors.TagSelector;

/**
 * This class extracts the ICSS Abstract Syntax Tree from the Antlr Parse tree.
 */
public class ASTListener extends ICSSBaseListener {

	//Accumulator attributes:
	private AST ast;

	//Use this to keep track of the parent nodes when recursively traversing the ast
	private Stack<ASTNode> currentContainer;

	public ASTListener() {
		ast = new AST();
		currentContainer = new Stack<>(); // nog aanpassen naar HAN STACK
	}

	public AST getAST() {
		return ast;
	}


	// als root neerzetten
	@Override
	public void enterStylesheet(ICSSParser.StylesheetContext ctx) {
		Stylesheet stylesheet = new Stylesheet();
		currentContainer.push(stylesheet);
	}


	@Override
	public void exitStylesheet(ICSSParser.StylesheetContext ctx) {
		Stylesheet stylesheet = (Stylesheet) currentContainer.pop();
		ast.setRoot(stylesheet);

	}

	@Override
	public void enterRuleset(ICSSParser.RulesetContext ctx) {
		Stylerule stylerule = new Stylerule();
		currentContainer.push(stylerule);
	}

	@Override
	public void exitRuleset(ICSSParser.RulesetContext ctx) {
		Stylerule stylerule = (Stylerule) currentContainer.pop();
		currentContainer.peek().addChild(stylerule);
	}

	@Override
	public void enterSelector(ICSSParser.SelectorContext ctx) {
		IdSelector selector = new IdSelector(ctx.getText());
		currentContainer.push(selector);
	}


	@Override
	public void exitSelector(ICSSParser.SelectorContext ctx) {
		IdSelector selector = (IdSelector) currentContainer.pop();
		currentContainer.peek().addChild(selector);
	}

	@Override
	public void enterDeclaration(ICSSParser.DeclarationContext ctx) {
		Declaration declaration = new Declaration();
		currentContainer.push(declaration);
	}


	@Override
	public void exitDeclaration(ICSSParser.DeclarationContext ctx) {
		Declaration declaration = (Declaration) currentContainer.pop();
		currentContainer.peek().addChild(declaration);
	}

	@Override
	public void enterProperty(ICSSParser.PropertyContext ctx) {
      PropertyName propertyName = new PropertyName(ctx.getText());
	  currentContainer.push(propertyName);
	}

	@Override
	public void exitProperty(ICSSParser.PropertyContext ctx) {
		PropertyName propertyName = (PropertyName) currentContainer.pop();
		currentContainer.peek().addChild(propertyName);
	}

	@Override
	public void enterAddOperation(ICSSParser.AddOperationContext ctx) {
		AddOperation addOperation = new AddOperation();
        currentContainer.add(addOperation);
	}


	@Override
	public void exitAddOperation(ICSSParser.AddOperationContext ctx) {
		AddOperation addOperation = (AddOperation) currentContainer.pop();
		currentContainer.peek().addChild(addOperation);
	}

	@Override
	public void enterMultiplyOperation(ICSSParser.MultiplyOperationContext ctx) {
		MultiplyOperation multiplyOperation = new MultiplyOperation();
		currentContainer.add(multiplyOperation);
	}

	@Override
	public void exitMultiplyOperation(ICSSParser.MultiplyOperationContext ctx) {
		MultiplyOperation multiplyOperation = (MultiplyOperation) currentContainer.pop();
		currentContainer.peek().addChild(multiplyOperation);
	}

	@Override
	public void enterSubtractOperation(ICSSParser.SubtractOperationContext ctx) {
		SubtractOperation subtractOperation = new SubtractOperation();
		currentContainer.add(subtractOperation);
	}

	@Override
	public void exitSubtractOperation(ICSSParser.SubtractOperationContext ctx) {
		SubtractOperation subtractOperation = (SubtractOperation) currentContainer.pop();
		currentContainer.peek().addChild(subtractOperation);
	}

	@Override
	public void enterPixelLiteral(ICSSParser.PixelLiteralContext ctx) {
		PixelLiteral pixelLiteral = new PixelLiteral(ctx.getText());
		currentContainer.push(pixelLiteral);
	}

	@Override
	public void exitPixelLiteral(ICSSParser.PixelLiteralContext ctx) {
		PixelLiteral pixelLiteral = (PixelLiteral) currentContainer.pop();
		currentContainer.peek().addChild(pixelLiteral);
	}


	@Override
	public void enterScalarLiteral(ICSSParser.ScalarLiteralContext ctx) {
	  ScalarLiteral scalarLiteral = new ScalarLiteral(ctx.getText());
	  currentContainer.push(scalarLiteral);
	}


	@Override
	public void exitScalarLiteral(ICSSParser.ScalarLiteralContext ctx) {
		ScalarLiteral scalarLiteral = (ScalarLiteral) currentContainer.pop();
		currentContainer.peek().addChild(scalarLiteral);
	}


	@Override
	public void enterPercentageLiteral(ICSSParser.PercentageLiteralContext ctx) {
		PercentageLiteral percentageLiteral = new PercentageLiteral(ctx.getText());
		currentContainer.push(percentageLiteral);
	}

	@Override
	public void exitPercentageLiteral(ICSSParser.PercentageLiteralContext ctx) {
		PercentageLiteral percentageLiteral = (PercentageLiteral) currentContainer.pop();
		currentContainer.peek().addChild(percentageLiteral);
	}


	@Override
	public void enterColorLiteral(ICSSParser.ColorLiteralContext ctx) {
		ColorLiteral colorLiteral = new ColorLiteral(ctx.getText());
		currentContainer.push(colorLiteral);
	}

	@Override
	public void exitColorLiteral(ICSSParser.ColorLiteralContext ctx) {
		ColorLiteral colorLiteral = (ColorLiteral) currentContainer.pop();
		currentContainer.peek().addChild(colorLiteral);
	}

	@Override
	public void enterBoolLiteral(ICSSParser.BoolLiteralContext ctx) {
		BoolLiteral boolLiteral = new BoolLiteral(ctx.getText());
		currentContainer.push(boolLiteral);
	}

	@Override
	public void exitBoolLiteral(ICSSParser.BoolLiteralContext ctx) {
		BoolLiteral boolLiteral = (BoolLiteral) currentContainer.pop();
		currentContainer.peek().addChild(boolLiteral);
	}

	@Override
	public void enterVariableReference(ICSSParser.VariableReferenceContext ctx) {
		VariableReference variableReference = new VariableReference(ctx.getText());
		currentContainer.push(variableReference);
	}

	@Override
	public void exitVariableReference(ICSSParser.VariableReferenceContext ctx) {
		VariableReference variableReference = (VariableReference) currentContainer.pop();
		currentContainer.peek().addChild(variableReference);
	}


	@Override
	public void enterVariableAssignment(ICSSParser.VariableAssignmentContext ctx) {
		VariableAssignment variableAssignment = new VariableAssignment();
		currentContainer.push(variableAssignment);
	}


}



