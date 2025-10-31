package nl.han.ica.icss.generator;

import nl.han.ica.icss.ast.*;
import nl.han.ica.icss.ast.literals.*;
import nl.han.ica.icss.ast.selectors.ClassSelector;
import nl.han.ica.icss.ast.selectors.IdSelector;
import nl.han.ica.icss.ast.selectors.TagSelector;

public class Generator {

	private StringBuilder output;

	public String generate(AST ast) {
		output = new StringBuilder();
		output.append("/* Nouri zijn CSS Output */ \n\n");
		generateStylesheet(ast.root);
		return output.toString();
	}

	private void generateStylesheet(Stylesheet node) {
		for (ASTNode child : node.getChildren()) {
			if (child instanceof Stylerule) {
				generateStylerule((Stylerule) child);
			}
		}
	}

	private void generateStylerule(Stylerule node) {
		for (int i = 0; i < node.selectors.size(); i++) {
			generateSelector(node.selectors.get(i));
			if (i < node.selectors.size() - 1) {
				output.append(", ");
			}
		}
		output.append(" {\n");
		for (ASTNode child : node.body) {
			if (child instanceof Declaration) {
				generateDeclaration((Declaration) child);
			}
		}

		output.append("}\n\n");
	}

	private void generateSelector(Selector selector) {
		if (selector instanceof TagSelector) {
			output.append(((TagSelector) selector).tag);
		} else if (selector instanceof ClassSelector) {
			output.append(((ClassSelector) selector).cls);
		} else if (selector instanceof IdSelector) {
			output.append(((IdSelector) selector).id);
		}
	}

	private void generateDeclaration(Declaration declaration) {
		output.append(declaration.property.name);
		output.append(": ");

		if (declaration.expression instanceof Literal) {
			generateLiteral((Literal) declaration.expression);
		}
		output.append(";\n");
	}

	private void generateLiteral(Literal literal) {
		if (literal instanceof PixelLiteral) {
			output.append(((PixelLiteral) literal).value);
			output.append("px");
		} else if (literal instanceof PercentageLiteral) {
			output.append(((PercentageLiteral) literal).value);
			output.append("%");
		} else if (literal instanceof ScalarLiteral) {
			output.append(((ScalarLiteral) literal).value);
		} else if (literal instanceof ColorLiteral) {
			output.append(((ColorLiteral) literal).value);
		}
	}
}