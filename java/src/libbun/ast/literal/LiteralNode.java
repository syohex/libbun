package libbun.ast.literal;

import libbun.ast.BNode;
import libbun.parser.classic.BToken;

public abstract class LiteralNode extends ConstNode {
	protected LiteralNode(BNode ParentNode, BToken SourceToken) {
		super(ParentNode, SourceToken);
	}
}
