package libbun.lang.bun.shell;

import libbun.util.LibZen;
import libbun.util.Var;
import libbun.util.ZTokenFunction;
import libbun.parser.ZSourceContext;

public class CommandSymbolTokenFunction extends ZTokenFunction {
	@Override public boolean Invoke(ZSourceContext SourceContext) {
		@Var int startIndex = SourceContext.GetPosition();
		@Var StringBuilder symbolBuilder = new StringBuilder();
		while(SourceContext.HasChar()) {
			@Var char ch = SourceContext.GetCurrentChar();
			if(!LibZen._IsDigitOrLetter(ch) && ch != '-' && ch != '+' && ch != '_') {
				break;
			}
			symbolBuilder.append(ch);
			SourceContext.MoveNext();
		}
		if(SourceContext.TokenContext.NameSpace.GetSymbol(ShellUtils._ToCommandSymbol(symbolBuilder.toString())) != null) {
			SourceContext.Tokenize(CommandSymbolPatternFunction._PatternName, startIndex, SourceContext.GetPosition());
			return true;
		}
		return false;
	}
}
