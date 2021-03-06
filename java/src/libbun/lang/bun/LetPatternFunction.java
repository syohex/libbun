package libbun.lang.bun;

import libbun.parser.ZTokenContext;
import libbun.parser.ast.ZLetVarNode;
import libbun.parser.ast.ZNode;
import libbun.util.Var;
import libbun.util.ZMatchFunction;

public class LetPatternFunction extends ZMatchFunction {

	@Override public ZNode Invoke(ZNode ParentNode, ZTokenContext TokenContext, ZNode LeftNode) {
		@Var ZNode LetNode = new ZLetVarNode(ParentNode, ZLetVarNode._IsReadOnly);
		LetNode = TokenContext.MatchToken(LetNode, "let", ZTokenContext._Required);
		LetNode = TokenContext.MatchPattern(LetNode, ZLetVarNode._NameInfo, "$Name$", ZTokenContext._Required);
		//		if(TokenContext.MatchToken(".")) {
		//			LetNode = TokenContext.MatchPattern(LetNode, "$Name$", ZTokenContext.Required);
		//		}
		LetNode = TokenContext.MatchPattern(LetNode, ZLetVarNode._TypeInfo, "$TypeAnnotation$", ZTokenContext._Optional);
		LetNode = TokenContext.MatchToken(LetNode, "=", ZTokenContext._Required);
		LetNode = TokenContext.MatchPattern(LetNode, ZLetVarNode._InitValue, "$Expression$", ZTokenContext._Required);
		return LetNode;
	}

}
