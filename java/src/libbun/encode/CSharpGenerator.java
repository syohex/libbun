package libbun.encode;

import libbun.parser.ZLogger;
import libbun.parser.ast.ZArrayLiteralNode;
import libbun.parser.ast.ZClassNode;
import libbun.parser.ast.ZErrorNode;
import libbun.parser.ast.ZFuncNameNode;
import libbun.parser.ast.ZFunctionNode;
import libbun.parser.ast.ZGetIndexNode;
import libbun.parser.ast.ZInstanceOfNode;
import libbun.parser.ast.ZLetVarNode;
import libbun.parser.ast.ZMapEntryNode;
import libbun.parser.ast.ZMapLiteralNode;
import libbun.parser.ast.ZMethodCallNode;
import libbun.parser.ast.ZNewObjectNode;
import libbun.parser.ast.ZNode;
import libbun.parser.ast.ZNullNode;
import libbun.parser.ast.ZReturnNode;
import libbun.parser.ast.ZSetIndexNode;
import libbun.parser.ast.ZThrowNode;
import libbun.parser.ast.ZTryNode;
import libbun.type.ZClassType;
import libbun.type.ZFuncType;
import libbun.type.ZType;
import libbun.util.Field;
import libbun.util.LibZen;
import libbun.util.Var;
import libbun.util.ZArray;
import libbun.util.ZMap;
import libbun.util.ZenMethod;

public class CSharpGenerator extends ZSourceGenerator {

	@Field private ZFunctionNode MainFuncNode = null;
	@Field private final ZArray<ZFunctionNode> ExportFunctionList = new ZArray<ZFunctionNode>(new ZFunctionNode[4]);

	public CSharpGenerator() {
		super("cs", "5.0");
		this.IntLiteralSuffix="";
		this.TopType = "object";
		this.SetNativeType(ZType.BooleanType, "bool");
		this.SetNativeType(ZType.IntType, "long");  // for beautiful code
		this.SetNativeType(ZType.FloatType, "double");
		this.SetNativeType(ZType.StringType, "string");
		this.SetNativeType(ZType.VarType, "object");  // for safety

		this.SetReservedName("this", "@this");
		this.CurrentBuilder.AppendNewLine("/* end of header */", this.LineFeed);
		this.CurrentBuilder.OpenIndent("namespace ZenGenerated {");
	}

	@Override protected void GenerateImportLibrary(String LibName) {
		this.HeaderBuilder.AppendNewLine("using ", LibName, this.SemiColon);
	}

	@Override @ZenMethod protected void Finish(String FileName) {
		//if(FileName == null) {
		FileName = "ZenMain";
		/*}
		else {
			@Var int loc = FileName.lastIndexOf('/');
			if(loc != -1) {
				FileName = FileName.substring(loc+1);
			}
		}*/

		this.GenerateClass("public static", FileName, ZClassType._ObjectType);
		this.CurrentBuilder.OpenIndent(" {");
		if(this.MainFuncNode != null) {
			this.CurrentBuilder.AppendNewLine("public static void Main(String[] a)");
			this.CurrentBuilder.OpenIndent(" {");
			//this.CurrentBuilder.AppendNewLine(this.NameFunctionClass(this.MainFuncNode.FuncName(), ZType.VoidType, 0), ".f();");
			this.CurrentBuilder.AppendNewLine(this.MainFuncNode.FuncName() + "();");
			this.NameFunctionClass(this.MainFuncNode.FuncName(), ZType.VoidType, 0);
			this.CurrentBuilder.CloseIndent("}");
		}
		this.CurrentBuilder.CloseIndent("}");
		this.CurrentBuilder.CloseIndent("}"); // end of namespace
		this.CurrentBuilder.AppendLineFeed();
	}


	@Override protected void GenerateCode(ZType ContextType, ZNode Node) {
		if(Node.IsUntyped() && !Node.IsErrorNode() && !(Node instanceof ZFuncNameNode)) {
			ZLogger._LogError(Node.SourceToken, "untyped error: " + Node);
			Node.Accept(this);
			this.CurrentBuilder.Append("/*untyped*/");
		}
		else {
			if(ContextType != null && Node.Type != ContextType && !ContextType.IsGreekType()) {
				this.CurrentBuilder.Append("(");
				this.GenerateTypeName(ContextType);
				this.CurrentBuilder.Append(")");
			}
			Node.Accept(this);
		}
	}

	@Override public void VisitArrayLiteralNode(ZArrayLiteralNode Node) {
		if(Node.GetListSize() == 0) {
			this.CurrentBuilder.Append("new ", this.GetCSharpTypeName(Node.Type, false), "()");
		}
		else {
			this.ImportLibrary("System.Collections.Generic");
			this.CurrentBuilder.Append("new ", this.GetCSharpTypeName(Node.Type, false));
			this.VisitListNode("{", Node, "}");
		}
	}

	@Override public void VisitMapLiteralNode(ZMapLiteralNode Node) {
		this.CurrentBuilder.Append("new ", this.GetCSharpTypeName(Node.Type, false));
		if(Node.GetListSize() > 0) {
			@Var int i = 0;
			this.CurrentBuilder.OpenIndent(" {");
			while(i < Node.GetListSize()) {
				@Var ZMapEntryNode Entry = Node.GetMapEntryNode(i);
				this.CurrentBuilder.AppendNewLine("{");
				this.GenerateCode2("", Entry.KeyNode(), this.Camma, Entry.ValueNode(), "},");
				i = i + 1;
			}
			this.CurrentBuilder.CloseIndent("}");
		}else{
			this.CurrentBuilder.Append("()");
		}
	}

	@Override public void VisitNewObjectNode(ZNewObjectNode Node) {
		this.CurrentBuilder.Append("new " + this.NameClass(Node.Type));
		this.VisitListNode("(", Node, ")");
	}

	@Override public void VisitGetIndexNode(ZGetIndexNode Node) {
		this.GenerateCode(null, Node.RecvNode());
		if(Node.RecvNode().Type == ZType.StringType){
			this.GenerateCode2(".Substring(", null, Node.IndexNode(), ", 1)");
		}else{
			this.GenerateCode2("[", null, Node.IndexNode(), "]");
		}
	}

	@Override public void VisitSetIndexNode(ZSetIndexNode Node) {
		this.GenerateCode(null, Node.RecvNode());
		this.GenerateCode2("[", null, Node.IndexNode(), "] = ");
		this.GenerateCode(null, Node.ExprNode());
	}

	@Override public void VisitMethodCallNode(ZMethodCallNode Node) {
		this.GenerateSurroundCode(Node.RecvNode());
		this.CurrentBuilder.Append(".");
		this.CurrentBuilder.Append(Node.MethodName());
		this.VisitListNode("(", Node, ")");
	}

	//	@Override public void VisitFuncCallNode(ZFuncCallNode Node) {
	//
	//		if(Node.IsStaticFuncCall()) {
	//			this.GenerateCode(null, Node.FunctionNode());
	//			this.VisitListNode("(", Node, ")");
	//		}
	//		else {
	//			this.GenerateCode(null, Node.FunctionNode());
	//			this.VisitListNode("(", Node, ")");
	//		}
	//	}

	//	@Override public void VisitCastNode(ZCastNode Node) {
	//		this.CurrentBuilder.Append("(");
	//		this.VisitType(Node.Type);
	//		this.CurrentBuilder.Append(")");
	//		this.GenerateSurroundCode(Node.ExprNode());
	//	}

	@Override public void VisitThrowNode(ZThrowNode Node) {
		this.CurrentBuilder.Append("throw ");
		this.GenerateCode2("new Exception((", null, Node.ExprNode(),").ToString())");
	}

	@Override public void VisitTryNode(ZTryNode Node) {
		this.CurrentBuilder.Append("try ");
		this.GenerateCode(null, Node.TryBlockNode());
		if(Node.HasCatchBlockNode()) {
			@Var String VarName = this.NameUniqueSymbol("e");
			this.CurrentBuilder.AppendNewLine("catch (Exception ", VarName, ")");
			this.CurrentBuilder.OpenIndent(" {");
			this.CurrentBuilder.AppendNewLine("Object ", Node.ExceptionName(), " = ");
			this.CurrentBuilder.Append("/*FIXME*/", VarName, this.SemiColon);
			this.VisitStmtList(Node.CatchBlockNode());
			this.CurrentBuilder.Append(this.SemiColon);
			this.CurrentBuilder.CloseIndent("}");
		}
		if(Node.HasFinallyBlockNode()) {
			this.CurrentBuilder.AppendNewLine("finally ");
			this.GenerateCode(null, Node.FinallyBlockNode());
		}
	}

	private String GetCSharpTypeName(ZType Type, boolean Boxing) {
		this.ImportLibrary("System");
		this.ImportLibrary("System.Diagnostics");
		this.ImportLibrary("System.Collections.Generic");
		this.ImportLibrary("System.Linq");
		this.ImportLibrary("System.Text");
		if(Type.IsArrayType()) {
			this.ImportLibrary("System.Collections.Generic");
			return "List<" + this.GetCSharpTypeName(Type.GetParamType(0), true) + ">";
		}
		if(Type.IsMapType()) {
			this.ImportLibrary("System.Collections.Generic");
			return "Dictionary<string," + this.GetCSharpTypeName(Type.GetParamType(0), true) + ">";
		}
		if(Type instanceof ZFuncType) {
			return this.GetFuncTypeClass((ZFuncType)Type);
		}
		if(Type instanceof ZClassType) {
			return this.NameClass(Type);
		}
		if(Boxing) {
			if(Type.IsIntType()) {
				return "Int64";
			}
			if(Type.IsFloatType()) {
				return "Double";
			}
			if(Type.IsBooleanType()) {
				return "Bool";
			}
		}
		return this.GetNativeTypeName(Type);
	}


	@Field private final ZMap<String> FuncNameMap = new ZMap<String>(null);

	String GetFuncTypeClass(ZFuncType FuncType) {
		@Var String ClassName = this.FuncNameMap.GetOrNull(FuncType.GetUniqueName());
		if(ClassName == null){
			@Var ZSourceBuilder MainBuilder = this.CurrentBuilder;
			this.CurrentBuilder = new ZSourceBuilder(this, null);
			@Var boolean HasReturnValue = !FuncType.GetReturnType().equals(ZType.VoidType);
			if(HasReturnValue){
				this.CurrentBuilder.Append("Func");
			}else{
				this.CurrentBuilder.Append("Action");
			}
			this.CurrentBuilder.Append("<");
			@Var int i = 0;
			while(i < FuncType.GetFuncParamSize()) {
				if(i > 0) {
					this.CurrentBuilder.Append(this.Camma);
				}
				this.GenerateTypeName(FuncType.GetFuncParamType(i));
				i = i + 1;
			}
			if(HasReturnValue){
				this.CurrentBuilder.Append(this.Camma);
				this.GenerateTypeName(FuncType.GetReturnType());
			}
			this.CurrentBuilder.Append(">");
			ClassName = this.CurrentBuilder.toString();
			this.CurrentBuilder = MainBuilder;
			this.FuncNameMap.put(FuncType.GetUniqueName(), ClassName);
		}
		return ClassName;
	}

	@Override protected void GenerateTypeName(ZType Type) {
		if(Type instanceof ZFuncType) {
			this.CurrentBuilder.Append(this.GetFuncTypeClass((ZFuncType)Type));
		}
		else {
			this.CurrentBuilder.Append(this.GetCSharpTypeName(Type.GetRealType(), false));
		}
	}

	@Override
	protected void VisitVarDeclNode(ZLetVarNode Node) {
		if(Node.InitValueNode() instanceof ZNullNode){
			this.GenerateTypeName(Node.DeclType());
			this.CurrentBuilder.Append(" ");
		}else if(Node.InitValueNode() instanceof ZFunctionNode){
			this.GenerateTypeName(Node.DeclType());
			this.CurrentBuilder.Append(" ");
		}else{
			this.CurrentBuilder.Append("var ");
		}
		this.CurrentBuilder.Append(this.NameLocalVariable(Node.GetNameSpace(), Node.GetName()));
		this.CurrentBuilder.Append(" = ");
		this.GenerateCode(null, Node.InitValueNode());
		this.CurrentBuilder.Append(this.SemiColon);
		if(Node.HasNextVarNode()) { this.VisitVarDeclNode(Node.NextVarNode()); }
	}

	@Override public void VisitLetNode(ZLetVarNode Node) {
		this.CurrentBuilder.AppendNewLine("public static partial class ZenMain");
		this.CurrentBuilder.OpenIndent(" {");
		this.GenerateClassField("static readonly ", Node.GetAstType(ZLetVarNode._InitValue), Node.GlobalName, null);
		this.GenerateCode2(" = ", null, Node.InitValueNode(), this.SemiColon);
		this.CurrentBuilder.CloseIndent("}");
	}

	@Override protected void VisitParamNode(ZLetVarNode Node) {
		this.GenerateTypeName(Node.Type);
		this.CurrentBuilder.Append(" ");
		this.CurrentBuilder.Append(this.NameLocalVariable(Node.GetNameSpace(), Node.GetName()));
	}

	@Override public void VisitFunctionNode(ZFunctionNode Node) {
		@Var boolean IsLambda = (Node.FuncName() == null);
		if(IsLambda){
			this.GenerateLambdaFunction(Node);
			return;
		}
		if(!Node.Type.IsVoidType()) {
			@Var String FuncName = Node.GetUniqueName(this);
			this.CurrentBuilder = this.InsertNewSourceBuilder();
			FuncName = this.GenerateFunctionAsClass(FuncName, Node);
			this.CurrentBuilder.AppendLineFeed();
			this.CurrentBuilder = this.CurrentBuilder.Pop();
			this.CurrentBuilder.Append(FuncName);
		}
		else {
			this.GenerateFunctionAsClass(Node.FuncName(), Node);
			if(Node.IsExport) {
				if(Node.FuncName().equals("main")) {
					this.MainFuncNode = Node;
				}
				else {
					this.ExportFunctionList.add(Node);
				}
			}
		}
	}

	private void GenerateLambdaFunction(ZFunctionNode Node){
		this.VisitListNode("(", Node, ") => ");
		if(Node.BlockNode().GetListSize() == 1){
			@Var ZNode FirstNode = Node.BlockNode().GetListAt(0);
			if(FirstNode instanceof ZReturnNode){
				this.GenerateCode(null, ((ZReturnNode)FirstNode).ExprNode());
				return;
			}
		}
		this.GenerateCode(null, Node.BlockNode());
	}

	private void VisitInstanceMethodParameters(ZFunctionNode VargNode){
		this.CurrentBuilder.Append("(");
		@Var int i = 1;
		while(i < VargNode.GetListSize()) {
			@Var ZNode ParamNode = VargNode.GetListAt(i);
			if (i > 0) {
				this.CurrentBuilder.Append(this.Camma);
			}
			this.GenerateCode(null, ParamNode);
			i = i + 1;
		}
		this.CurrentBuilder.Append(")");
	}

	private String GenerateFunctionAsClass(String FuncName, ZFunctionNode Node) {
		@Var ZLetVarNode FirstParam = Node.GetListSize() == 0 ? null : (ZLetVarNode)Node.GetListAt(0);
		@Var boolean IsInstanceMethod = FirstParam != null && FirstParam.GetName().equals("this");

		this.GenerateClass("public static", "ZenMain", Node.GetFuncType());
		this.CurrentBuilder.OpenIndent(" { ");
		this.CurrentBuilder.AppendNewLine("public static ");
		this.GenerateTypeName(Node.ReturnType());
		this.CurrentBuilder.Append(" ");
		this.CurrentBuilder.Append(FuncName);
		if(IsInstanceMethod){
			this.VisitFuncParamNode("(this ", Node, ")");
		}else{
			this.VisitFuncParamNode("(", Node, ")");
		}

		this.GenerateCode(null, Node.BlockNode());

		this.CurrentBuilder.CloseIndent("}");
		this.CurrentBuilder.AppendNewLine();
		return FuncName;
	}

	@Override public void VisitInstanceOfNode(ZInstanceOfNode Node) {
		this.GenerateCode(null, Node.AST[ZInstanceOfNode._Left]);
		this.CurrentBuilder.Append(" is ");
		this.GenerateTypeName(Node.TargetType());
	}


	private void GenerateClass(String Qualifier, String ClassName, ZType SuperType) {
		if(Qualifier != null && Qualifier.length() > 0) {
			this.CurrentBuilder.AppendNewLine(Qualifier);
			this.CurrentBuilder.AppendWhiteSpace("partial class ", ClassName);
		}
		else {
			this.CurrentBuilder.AppendNewLine("partial class ", ClassName);
		}
		if(!SuperType.Equals(ZClassType._ObjectType) && !SuperType.IsFuncType()) {
			this.CurrentBuilder.Append(" : ");
			this.GenerateTypeName(SuperType);
		}
	}

	private void GenerateClassField(String Qualifier, ZType FieldType, String FieldName, String Value) {
		if(Qualifier.length() > 1){
			this.CurrentBuilder.AppendNewLine(Qualifier);
			this.CurrentBuilder.Append("public ");
		}else{
			this.CurrentBuilder.AppendNewLine("public ");
		}
		this.GenerateTypeName(FieldType);
		this.CurrentBuilder.Append(" ", FieldName);
		if(Value != null) {
			this.CurrentBuilder.Append(" = ", Value);
			this.CurrentBuilder.Append(this.SemiColon);
		}
	}

	@Override public void VisitClassNode(ZClassNode Node) {
		@Var ZType SuperType = Node.ClassType.GetSuperType();
		@Var String ClassName = this.NameClass(Node.ClassType);
		this.GenerateClass("public", ClassName, SuperType);
		this.CurrentBuilder.OpenIndent(" {");
		@Var int i = 0;
		while (i < Node.GetListSize()) {
			@Var ZLetVarNode FieldNode = Node.GetFieldNode(i);
			this.GenerateClassField("", FieldNode.DeclType(), FieldNode.GetName(), null);
			this.CurrentBuilder.Append(this.SemiColon);
			i = i + 1;
		}
		this.CurrentBuilder.AppendNewLine();

		i = 0;
		//while(i < Node.ClassType.GetFieldSize()) {
		//@Var ZClassField Field = Node.ClassType.GetFieldAt(i);
		//			if(Field.FieldType.IsFuncType()) {
		//				this.GenerateClassField("static", Field.FieldType, this.NameMethod(Node.ClassType, Field.FieldName), "null");
		//				this.CurrentBuilder.Append(this.SemiColon);
		//			}
		//i = i + 1;
		//}

		this.CurrentBuilder.AppendNewLine("public ", this.NameClass(Node.ClassType), "()");
		this.CurrentBuilder.OpenIndent(" {");
		//this.CurrentBuilder.AppendNewLine("super();");
		while (i < Node.GetListSize()) {
			@Var ZLetVarNode FieldNode = Node.GetFieldNode(i);
			this.CurrentBuilder.AppendNewLine("this.", FieldNode.GetName(), "=");
			this.GenerateCode(null, FieldNode.InitValueNode());
			this.CurrentBuilder.Append(this.SemiColon);
			i = i + 1;
		}
		i = 0;
		while(i < Node.ClassType.GetFieldSize()) {
			//@Var ZClassField Field = Node.ClassType.GetFieldAt(i);
			//			if(Field.FieldType.IsFuncType()) {
			//				this.CurrentBuilder.AppendNewLine("if(", this.NameMethod(Node.ClassType, Field.FieldName), " != null) ");
			//				this.CurrentBuilder.OpenIndent("{");
			//				this.CurrentBuilder.AppendNewLine("this.", Field.FieldName, "=");
			//				this.CurrentBuilder.Append(this.NameMethod(Node.ClassType, Field.FieldName), ";", this.LineFeed);
			//				this.CurrentBuilder.CloseIndent("}");
			//			}
			i = i + 1;
		}

		this.CurrentBuilder.CloseIndent("}"); /* end of constructor*/
		this.CurrentBuilder.CloseIndent("}"); /* end of class */
		this.CurrentBuilder.AppendLineFeed();
	}

	@Override public void VisitErrorNode(ZErrorNode Node) {
		ZLogger._LogError(Node.SourceToken, Node.ErrorMessage);
		this.CurrentBuilder.Append("ThrowError(");
		this.CurrentBuilder.Append(LibZen._QuoteString(Node.ErrorMessage));
		this.CurrentBuilder.Append(")");
	}

	//	@Override public void VisitExtendedNode(ZNode Node) {
	//
	//	}


}
