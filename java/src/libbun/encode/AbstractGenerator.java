// ***************************************************************************
// Copyright (c) 2013-2014, Libbun project authors. All rights reserved.
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// *  Redistributions of source code must retain the above copyright notice,
//    this list of conditions and the following disclaimer.
// *  Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
// TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
// PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
// CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
// EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
// PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
// OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
// WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
// OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
// ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// **************************************************************************


package libbun.encode;

import libbun.ast.AbstractListNode;
import libbun.ast.BNode;
import libbun.ast.BunBlockNode;
import libbun.ast.DesugarNode;
import libbun.ast.SyntaxSugarNode;
import libbun.ast.decl.BunClassNode;
import libbun.ast.decl.BunFunctionNode;
import libbun.ast.decl.BunLetVarNode;
import libbun.ast.decl.TopLevelNode;
import libbun.ast.error.ErrorNode;
import libbun.ast.literal.BunNullNode;
import libbun.ast.literal.DefaultValueNode;
import libbun.parser.BLangInfo;
import libbun.parser.BLogger;
import libbun.parser.BNameSpace;
import libbun.parser.BOperatorVisitor;
import libbun.parser.BToken;
import libbun.parser.BTokenContext;
import libbun.parser.BTypeChecker;
import libbun.type.BClassType;
import libbun.type.BFormFunc;
import libbun.type.BFunc;
import libbun.type.BFuncType;
import libbun.type.BPrototype;
import libbun.type.BType;
import libbun.util.BField;
import libbun.util.BIgnored;
import libbun.util.BLib;
import libbun.util.BMap;
import libbun.util.Nullable;
import libbun.util.Var;
import libbun.util.ZenMethod;

public abstract class AbstractGenerator extends BOperatorVisitor {
	@BField public BMap<String>        ImportedLibraryMap = new BMap<String>(null);
	@BField public BMap<String>        SymbolMap = new BMap<String>(null);
	@BField private final BMap<BFunc>  DefinedFuncMap = new BMap<BFunc>(null);

	@BField public final BNameSpace      RootNameSpace;
	@BField public BLogger               Logger;
	@BField public BTypeChecker          TypeChecker;
	@BField public BLangInfo             LangInfo;
	@BField protected String             TopLevelSymbol = null;
	@BField private int                  UniqueNumber = 0;

	protected AbstractGenerator(BLangInfo LangInfo) {
		this.RootNameSpace = new BNameSpace(this, null);
		this.Logger        = new BLogger();
		this.LangInfo      = LangInfo;
		this.TypeChecker   = null;
	}

	public final void SetTypeChecker(BTypeChecker TypeChecker) {
		this.TypeChecker = TypeChecker;
	}

	// symbol map

	protected void SetReservedName(String Keyword, @Nullable String AnotherName) {
		if(AnotherName == null) {
			AnotherName = "_" + Keyword;
		}
		this.SymbolMap.put(Keyword, AnotherName);
	}

	public final String GetNonKeyword(String Text) {
		return this.SymbolMap.GetValue(Text, Text);
	}

	public final int GetUniqueNumber() {
		@Var int UniqueNumber = this.UniqueNumber;
		this.UniqueNumber = this.UniqueNumber + 1;
		return UniqueNumber;
	}

	public String NameUniqueSymbol(String Symbol, int NameIndex) {
		return Symbol + "__B" + NameIndex;
	}

	public final String NameUniqueSymbol(String Symbol) {
		return this.NameUniqueSymbol(Symbol, this.GetUniqueNumber());
	}


	protected void SetNativeType(BType Type, String TypeName) {
		@Var String Key = "" + Type.TypeId;
		this.SymbolMap.put(Key, TypeName);
	}

	protected String GetNativeTypeName(BType Type) {
		@Var String Key = "" + Type.TypeId;
		@Var String TypeName = this.SymbolMap.GetOrNull(Key);
		if (TypeName == null) {
			return Type.GetName();
		}
		return TypeName;
	}

	public String NameGlobalNameClass(String Name) {
		return "G__" + Name;
	}

	public final String NameClass(BType ClassType) {
		return ClassType.GetName() /*+ "" + ClassType.TypeId*/;
	}

	public final String NameFunctionClass(String FuncName, BFuncType FuncType) {
		return "F__" + FuncType.StringfySignature(FuncName);
	}

	public final String NameFunctionClass(String FuncName, BType RecvType, int FuncParamSize) {
		return "F__" + BFunc._StringfySignature(FuncName, FuncParamSize, RecvType);
	}

	public final String NameType(BType Type) {
		if(Type.IsArrayType()) {
			return "ArrayOf" + this.NameType(Type.GetParamType(0)) + "_";
		}
		if(Type.IsMapType()) {
			return "MapOf" + this.NameType(Type.GetParamType(0)) + "_";
		}
		if(Type instanceof BFuncType) {
			@Var String s = "FuncOf";
			@Var int i = 0;
			while(i < Type.GetParamSize()) {
				s = s +  this.NameType(Type.GetParamType(i));
				i = i + 1;
			}
			return s + "_";
		}
		if(Type instanceof BClassType) {
			return this.NameClass(Type);
		}
		return Type.GetName();
	}

	protected final String NameMethod(BType ClassType, String MethodName) {
		return "_" + this.NameClass(ClassType) + "_" + MethodName;
	}

	protected final boolean IsMethod(String FuncName, BFuncType FuncType) {
		@Var BType RecvType = FuncType.GetRecvType();
		if(RecvType instanceof BClassType && FuncName != null) {
			@Var BClassType ClassType = (BClassType)RecvType;
			@Var BType FieldType = ClassType.GetFieldType(FuncName, null);
			if(FieldType == null || !FieldType.IsFuncType()) {
				FuncName = BLib._AnotherName(FuncName);
				FieldType = ClassType.GetFieldType(FuncName, null);
				if(FieldType == null || !FieldType.IsFuncType()) {
					return false;
				}
			}
			if(FieldType instanceof BFuncType) {
				if(((BFuncType)FieldType).AcceptAsFieldFunc(FuncType)) {
					return true;
				}
			}
		}
		return false;
	}

	// function map

	public final void SetDefinedFunc(BFunc Func) {
		this.DefinedFuncMap.put(Func.GetSignature(), Func);
	}

	private String NameConverterFunc(BType FromType, BType ToType) {
		return FromType.GetUniqueName() + "T" + ToType.GetUniqueName();
	}

	public final void SetConverterFunc(BType FromType, BType ToType, BFunc Func) {
		this.DefinedFuncMap.put(this.NameConverterFunc(FromType, ToType), Func);
	}

	public final BFunc LookupConverterFunc(BType FromType, BType ToType) {
		while(FromType != null) {
			@Var BFunc Func = this.DefinedFuncMap.GetOrNull(this.NameConverterFunc(FromType, ToType));
			//System.out.println("get " + this.NameConverterFunc(FromType, ToType) + ", func="+ Func);
			if(Func != null) {
				return Func;
			}
			FromType = FromType.GetSuperType();
		}
		return null;
	}

	//

	public final BPrototype SetPrototype(BNode Node, String FuncName, BFuncType FuncType) {
		@Var BFunc Func = this.GetDefinedFunc(FuncName, FuncType);
		if(Func != null) {
			if(!FuncType.Equals(Func.GetFuncType())) {
				BLogger._LogError(Node.SourceToken, "function has been defined diffrently: " + Func.GetFuncType());
				return null;
			}
			if(Func instanceof BPrototype) {
				return (BPrototype)Func;
			}
			BLogger._LogError(Node.SourceToken, "function has been defined as macro" + Func);
			return null;
		}
		@Var BPrototype	Proto= new BPrototype(0, FuncName, FuncType, Node.SourceToken);
		this.DefinedFuncMap.put(Proto.GetSignature(), Proto);
		return Proto;
	}

	public final BFunc GetDefinedFunc(String GlobalName) {
		@Var BFunc Func = this.DefinedFuncMap.GetOrNull(GlobalName);
		if(Func == null && BLib._IsLetter(BLib._GetChar(GlobalName, 0))) {
			//			System.out.println("AnotherName = " + GlobalName + ", " + LibZen._AnotherName(GlobalName));
			Func = this.DefinedFuncMap.GetOrNull(BLib._AnotherName(GlobalName));
		}
		//System.out.println("sinature="+GlobalName+", func="+Func);
		return Func;
	}

	public final BFunc GetDefinedFunc(String FuncName, BFuncType FuncType) {
		return this.GetDefinedFunc(FuncType.StringfySignature(FuncName));
	}

	public final BFunc GetDefinedFunc(String FuncName, BType RecvType, int FuncParamSize) {
		return this.GetDefinedFunc(BFunc._StringfySignature(FuncName, FuncParamSize, RecvType));
	}

	public final BFunc LookupFunc(String FuncName, BType RecvType, int FuncParamSize) {
		@Var BFunc Func = this.GetDefinedFunc(BFunc._StringfySignature(FuncName, FuncParamSize, RecvType));
		while(Func == null) {
			RecvType = RecvType.GetSuperType();
			if(RecvType == null) {
				break;
			}
			Func = this.GetDefinedFunc(BFunc._StringfySignature(FuncName, FuncParamSize, RecvType));
			//			if(RecvType.IsVarType()) {
			//				break;
			//			}
		}
		return Func;
	}

	public final BFormFunc GetFormFunc(String FuncName, BType RecvType, int FuncParamSize) {
		@Var BFunc Func = this.GetDefinedFunc(BFunc._StringfySignature(FuncName, FuncParamSize, RecvType));
		if(Func instanceof BFormFunc) {
			return ((BFormFunc)Func);
		}
		return null;
	}

	@ZenMethod public void WriteTo(@Nullable String FileName) {
		// TODO Stub
	}

	@ZenMethod public String GetSourceText() {
		return null;
	}

	@ZenMethod public BType GetFieldType(BType BaseType, String Name) {
		return BType.VarType;     // undefined
	}

	@ZenMethod public BType GetSetterType(BType BaseType, String Name) {
		return BType.VarType;     // undefined
	}

	@ZenMethod public BFuncType GetConstructorFuncType(BType ClassType, AbstractListNode List) {
		//return null;              // undefined and undefined error
		return BFuncType._FuncType;    // undefined and no error
	}

	@ZenMethod public BFuncType GetMethodFuncType(BType RecvType, String MethodName, AbstractListNode List) {
		//return null;              // undefined and undefined error
		return BFuncType._FuncType;     // undefined and no error
	}

	// Naming


	public final void VisitUndefinedNode(BNode Node) {
		@Var ErrorNode ErrorNode = new ErrorNode(Node.ParentNode, Node.SourceToken, "undefined node:" + Node.toString());
		this.VisitErrorNode(ErrorNode);
	}

	@Override public final void VisitDefaultValueNode(DefaultValueNode Node) {
		this.VisitNullNode(new BunNullNode(Node.ParentNode, null));
	}

	@Override public void VisitSyntaxSugarNode(SyntaxSugarNode Node) {
		@Var DesugarNode DeNode = Node.DeSugar(this, this.TypeChecker);
		@Var int i = 0;
		while(i < DeNode.GetAstSize()) {
			DeNode.AST[i].Accept(this);  // FIXME
			i = i + 1;
		}
	}

	@ZenMethod protected void GenerateExpression(BNode Node) {
		Node.Accept(this);
	}

	@ZenMethod protected void GenerateStatement(BNode Node) {
		Node.Accept(this);
	}

	protected boolean ExecStatement(BNode Node) {
		this.EnableVisitor();
		this.TopLevelSymbol = null;
		if(Node instanceof TopLevelNode) {
			((TopLevelNode)Node).Perform(this.RootNameSpace);
		}
		else {
			if(this.TypeChecker != null) {
				Node = this.TypeChecker.CheckType(Node, BType.VarType);
			}
			if(this.IsVisitable()) {
				if(Node instanceof BunFunctionNode || Node instanceof BunClassNode || Node instanceof BunLetVarNode) {
					Node.Type = BType.VoidType;
					this.GenerateStatement(Node);
				}
				else {
					if(!this.LangInfo.AllowTopLevelScript) {
						@Var String FuncName = this.NameUniqueSymbol("Main");
						Node = this.TypeChecker.CreateFunctionNode(Node.ParentNode, FuncName, Node);
						this.TopLevelSymbol = FuncName;
					}
					this.GenerateStatement(Node);
				}
			}
		}
		return this.IsVisitable();
	}

	public final boolean LoadScript(String ScriptText, String FileName, int LineNumber) {
		@Var boolean Result = true;
		@Var BunBlockNode TopBlockNode = new BunBlockNode(null, this.RootNameSpace);
		@Var BTokenContext TokenContext = new BTokenContext(this, this.RootNameSpace, FileName, LineNumber, ScriptText);
		TokenContext.SkipEmptyStatement();
		@Var BToken SkipToken = TokenContext.GetToken();
		while(TokenContext.HasNext()) {
			TokenContext.SetParseFlag(BTokenContext._NotAllowSkipIndent);
			TopBlockNode.ClearListToSize(0);
			SkipToken = TokenContext.GetToken();
			@Var BNode StmtNode = TokenContext.ParsePattern(TopBlockNode, "$Statement$", BTokenContext._Required);
			if(StmtNode.IsErrorNode()) {
				TokenContext.SkipError(SkipToken);
			}
			if(!this.ExecStatement(StmtNode)) {
				Result = false;
				break;
			}
			TokenContext.SkipEmptyStatement();
			TokenContext.Vacume();
		}
		this.Logger.OutputErrorsToStdErr();
		return Result;
	}


	public final boolean LoadFile(String FileName, @Nullable BToken SourceToken) {
		@Var String ScriptText = BLib._LoadTextFile(FileName);
		if(ScriptText == null) {
			BLogger._LogErrorExit(SourceToken, "file not found: " + FileName);
			return false;
		}
		return this.LoadScript(ScriptText, FileName, 1);
	}

	public final boolean RequireLibrary(String LibName, @Nullable BToken SourceToken) {
		@Var String Key = "_Z" + LibName.toLowerCase();
		@Var String Value = this.ImportedLibraryMap.GetOrNull(Key);
		if(Value == null) {
			@Var String Path = this.LangInfo.GetLibPath(LibName);
			@Var String Script = BLib._LoadTextFile(Path);
			if(Script == null) {
				BLogger._LogErrorExit(SourceToken, "library not found: " + LibName + " as " + Path);
				return false;
			}
			@Var boolean Result = this.LoadScript(Script, Path, 1);
			this.ImportedLibraryMap.put(Key, Path);
			return Result;
		}
		return true;
	}

	@BIgnored public void Perform() {
		if(this.TopLevelSymbol != null) {
			System.out.println("TODO: " + this.TopLevelSymbol);
		}
	}

	@ZenMethod public void ExecMain() {
		this.Logger.OutputErrorsToStdErr();
	}

	//	public final String Translate(String ScriptText, String FileName, int LineNumber) {
	//		@Var ZBlockNode TopBlockNode = new ZBlockNode(this.Generator.RootNameSpace);
	//		@Var ZTokenContext TokenContext = new ZTokenContext(this.Generator, this.Generator.RootNameSpace, FileName, LineNumber, ScriptText);
	//		TokenContext.SkipEmptyStatement();
	//		@Var ZToken SkipToken = TokenContext.GetToken();
	//		while(TokenContext.HasNext()) {
	//			TokenContext.SetParseFlag(ZTokenContext._NotAllowSkipIndent);
	//			TopBlockNode.ClearListAfter(0);
	//			SkipToken = TokenContext.GetToken();
	//			@Var ZNode ParsedNode = TokenContext.ParsePattern(TopBlockNode, "$Statement$", ZTokenContext._Required);
	//			if(ParsedNode.IsErrorNode()) {
	//				TokenContext.SkipError(SkipToken);
	//			}
	//			this.Exec2(ParsedNode, false);
	//			TokenContext.SkipEmptyStatement();
	//			TokenContext.Vacume();
	//		}
	//		return this.Generator.GetSourceText();
	//	}



}
