package libbun.lang.bun;

import libbun.parser.ZTokenContext;
import libbun.parser.ast.ZNode;
import libbun.parser.ast.ZWhileNode;
import libbun.util.Var;
import libbun.util.ZMatchFunction;

public class WhilePatternFunction extends ZMatchFunction {

	@Override public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		@Var ZNode WhileNode = new ZWhileNode(ParentNode);
		WhileNode = TokenContext.MatchToken(WhileNode, "while", ZTokenContext._Required);
		WhileNode = TokenContext.MatchToken(WhileNode, "(", ZTokenContext._Required);
		WhileNode = TokenContext.MatchPattern(WhileNode, ZWhileNode._Cond, "$Expression$", ZTokenContext._Required, ZTokenContext._AllowSkipIndent);
		if(TokenContext.MatchNewLineToken("whatever")) {
			WhileNode = TokenContext.MatchPattern(WhileNode, ZWhileNode._Next, "$InStatement$", ZTokenContext._Required, ZTokenContext._AllowSkipIndent);
		}
		WhileNode = TokenContext.MatchToken(WhileNode, ")", ZTokenContext._Required);
		WhileNode = TokenContext.MatchPattern(WhileNode, ZWhileNode._Block, "$Block$", ZTokenContext._Required);
		return WhileNode;
	}

}
