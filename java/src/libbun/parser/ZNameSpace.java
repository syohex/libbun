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


package libbun.parser;
import libbun.parser.ast.ZBlockNode;
import libbun.parser.ast.ZLetVarNode;
import libbun.parser.ast.ZNode;
import libbun.parser.ast.ZTypeNode;
import libbun.type.ZClassType;
import libbun.type.ZType;
import libbun.util.Field;
import libbun.util.LibZen;
import libbun.util.Nullable;
import libbun.util.Var;
import libbun.util.ZMap;
import libbun.util.ZMatchFunction;
import libbun.util.ZTokenFunction;

public final class ZNameSpace {
	@Field public final ZGenerator   Generator;
	@Field public final ZBlockNode   BlockNode;
	@Field ZTokenFunc[]       TokenMatrix = null;
	@Field ZMap<ZSyntax>      SyntaxTable = null;
	@Field ZMap<ZNode> SymbolTable2 = null;

	public ZNameSpace(ZGenerator Generator, ZBlockNode BlockNode) {
		this.BlockNode = BlockNode;   // rootname is null
		this.Generator = Generator;
		assert(this.Generator != null);
	}

	public final ZNameSpace GetParentNameSpace() {
		if(this.BlockNode != null) {
			@Var ZNode Node = this.BlockNode.ParentNode;
			while(Node != null) {
				if(Node instanceof ZBlockNode) {
					@Var ZBlockNode blockNode = (ZBlockNode)Node;
					if(blockNode.NullableNameSpace != null) {
						return blockNode.NullableNameSpace;
					}
				}
				Node = Node.ParentNode;
			}
		}
		return null;
	}

	@Override public String toString() {
		return "NS[" + this.BlockNode + "]";
	}

	public final ZNameSpace GetRootNameSpace() {
		return this.Generator.RootNameSpace;
	}

	// TokenMatrix
	public final ZTokenFunc GetTokenFunc(int ZenChar) {
		if(this.TokenMatrix == null) {
			return this.GetParentNameSpace().GetTokenFunc(ZenChar);
		}
		return this.TokenMatrix[ZenChar];
	}

	private final ZTokenFunc JoinParentFunc(ZTokenFunction Func, ZTokenFunc Parent) {
		if(Parent != null && Parent.Func == Func) {
			return Parent;
		}
		return new ZTokenFunc(Func, Parent);
	}

	public final void AppendTokenFunc(String keys, ZTokenFunction TokenFunc) {
		if(this.TokenMatrix == null) {
			this.TokenMatrix = LibZen._NewTokenMatrix();
			if(this.GetParentNameSpace() != null) {
				@Var int i = 0;
				while(i < this.TokenMatrix.length) {
					this.TokenMatrix[i] = this.GetParentNameSpace().GetTokenFunc(i);
					i = i + 1;
				}
			}
		}
		@Var int i = 0;
		while(i < keys.length()) {
			@Var int kchar = LibZen._GetTokenMatrixIndex(LibZen._GetChar(keys, i));
			this.TokenMatrix[kchar] = this.JoinParentFunc(TokenFunc, this.TokenMatrix[kchar]);
			i = i + 1;
		}
	}

	// Pattern
	public final ZSyntax GetSyntaxPattern(String PatternName) {
		@Var ZNameSpace NameSpace = this;
		while(NameSpace != null) {
			if(NameSpace.SyntaxTable != null) {
				return NameSpace.SyntaxTable.GetOrNull(PatternName);
			}
			NameSpace = NameSpace.GetParentNameSpace();
		}
		return null;
	}

	public final void SetSyntaxPattern(String PatternName, ZSyntax Syntax) {
		if(this.SyntaxTable == null) {
			this.SyntaxTable = new ZMap<ZSyntax>(null);
		}
		this.SyntaxTable.put(PatternName, Syntax);
	}

	public final static String _RightPatternSymbol(String PatternName) {
		return "\t" + PatternName;
	}

	public final ZSyntax GetRightSyntaxPattern(String PatternName) {
		return this.GetSyntaxPattern(ZNameSpace._RightPatternSymbol(PatternName));
	}

	private void AppendSyntaxPattern(String PatternName, ZSyntax NewPattern) {
		LibZen._Assert(NewPattern.ParentPattern == null);
		@Var ZSyntax ParentPattern = this.GetSyntaxPattern(PatternName);
		NewPattern.ParentPattern = ParentPattern;
		this.SetSyntaxPattern(PatternName, NewPattern);
	}

	public final void DefineStatement(String PatternName, ZMatchFunction MatchFunc) {
		@Var int Alias = PatternName.indexOf(" ");
		@Var String Name = PatternName;
		if(Alias != -1) {
			Name = PatternName.substring(0, Alias);
		}
		@Var ZSyntax Pattern = new ZSyntax(this, Name, MatchFunc);
		Pattern.IsStatement = true;
		this.AppendSyntaxPattern(Name, Pattern);
		if(Alias != -1) {
			this.DefineStatement(PatternName.substring(Alias+1), MatchFunc);
		}
	}

	public final void DefineExpression(String PatternName, ZMatchFunction MatchFunc) {
		@Var int Alias = PatternName.indexOf(" ");
		@Var String Name = PatternName;
		if(Alias != -1) {
			Name = PatternName.substring(0, Alias);
		}
		@Var ZSyntax Pattern = new ZSyntax(this, Name, MatchFunc);
		this.AppendSyntaxPattern(Name, Pattern);
		if(Alias != -1) {
			this.DefineExpression(PatternName.substring(Alias+1), MatchFunc);
		}
	}

	public final void DefineRightExpression(String PatternName, int SyntaxFlag, ZMatchFunction MatchFunc) {
		@Var int Alias = PatternName.indexOf(" ");
		@Var String Name = PatternName;
		if(Alias != -1) {
			Name = PatternName.substring(0, Alias);
		}
		@Var ZSyntax Pattern = new ZSyntax(this, Name, MatchFunc);
		Pattern.SyntaxFlag = SyntaxFlag;
		this.AppendSyntaxPattern(ZNameSpace._RightPatternSymbol(Name), Pattern);
		if(Alias != -1) {
			this.DefineRightExpression(PatternName.substring(Alias+1), SyntaxFlag, MatchFunc);
		}
	}

	public final void SetSymbol(String Symbol, ZNode EntryNode) {
		if(this.SymbolTable2 == null) {
			this.SymbolTable2 = new ZMap<ZNode>(null);
		}
		this.SymbolTable2.put(Symbol, EntryNode);
	}

	public final ZNode GetSymbol(String Symbol) {
		@Var ZNameSpace NameSpace = this;
		while(NameSpace != null) {
			if(NameSpace.SymbolTable2 != null) {
				@Var ZNode EntryNode = NameSpace.SymbolTable2.GetOrNull(Symbol);
				if(EntryNode != null) {
					return EntryNode;
				}
			}
			NameSpace = NameSpace.GetParentNameSpace();
		}
		return null;
	}

	public final void SetDebugSymbol(String Symbol, ZNode EntryNode) {
		if(this.SymbolTable2 == null) {
			this.SymbolTable2 = new ZMap<ZNode>(null);
		}
		System.out.println("SetSymbol: " + Symbol + " @" + this);
		this.SymbolTable2.put(Symbol, EntryNode);
	}

	public final ZNode GetDebugSymbol(String Symbol) {
		@Var ZNameSpace NameSpace = this;
		while(NameSpace != null) {
			System.out.println("GetSymbol: " + Symbol + " @" + NameSpace);
			if(NameSpace.SymbolTable2 != null) {
				@Var ZNode EntryNode = NameSpace.SymbolTable2.GetOrNull(Symbol);
				if(EntryNode != null) {
					return EntryNode;
				}
			}
			NameSpace = NameSpace.GetParentNameSpace();
		}
		return null;
	}

	public final int GetNameIndex(String Name) {
		@Var int NameIndex = -1;
		@Var ZNameSpace NameSpace = this;
		while(NameSpace != null) {
			if(NameSpace.SymbolTable2 != null) {
				@Var ZNode EntryNode = NameSpace.SymbolTable2.GetOrNull(Name);
				if(EntryNode != null) {
					NameIndex = NameIndex + 1;
				}
			}
			NameSpace = NameSpace.GetParentNameSpace();
		}
		return NameIndex;
	}

	public final void SetRootSymbol(String Symbol, ZNode EntryNode) {
		this.GetRootNameSpace().SetSymbol(Symbol, EntryNode);
	}

	public final ZLetVarNode GetLocalVariable(String Name) {
		@Var ZNode EntryNode = this.GetSymbol(Name);
		//System.out.println("var " + VarName + ", entry=" + Entry + ", NameSpace=" + this);
		if(EntryNode instanceof ZLetVarNode) {
			return (ZLetVarNode)EntryNode;
		}
		return null;
	}



	//	public final ZSymbolEntry GetSymbol(String Symbol) {
	//		@Var ZNameSpace NameSpace = this;
	//		while(NameSpace != null) {
	//			if(NameSpace.SymbolTable != null) {
	//				@Var ZSymbolEntry Entry = NameSpace.SymbolTable.GetOrNull(Symbol);
	//				if(Entry != null) {
	//					if(Entry.IsDisabled) {
	//						return null;
	//					}
	//					return Entry;
	//				}
	//			}
	//			NameSpace = NameSpace.GetParentNameSpace();
	//		}
	//		return null;
	//	}
	//
	//	public final ZNode GetSymbolNode(String Symbol) {
	//		@Var ZSymbolEntry Entry = this.GetSymbol(Symbol);
	//		if(Entry != null) {
	//			return Entry.Node;
	//		}
	//		return null;
	//	}
	//
	//	private void SetLocalSymbolEntry(String Symbol, ZSymbolEntry Entry) {
	//		if(this.SymbolTable == null) {
	//			this.SymbolTable = new ZMap<ZSymbolEntry>(null);
	//		}
	//		this.SymbolTable.put(Symbol, Entry);
	//	}
	//
	//	public final ZSymbolEntry SetLocalSymbol(String Symbol, ZNode Node) {
	//		@Var ZSymbolEntry Parent = this.GetSymbol(Symbol);
	//		Node.ParentNode = null; // kill links
	//		this.SetLocalSymbolEntry(Symbol, new ZSymbolEntry(Parent, Node));
	//		return Parent;
	//	}
	//
	//	public final ZSymbolEntry SetGlobalSymbol(String Symbol, ZNode Node) {
	//		return this.GetRootNameSpace().SetLocalSymbol(Symbol, Node);
	//	}
	//
	//	public final ZVariable GetLocalVariable(String VarName) {
	//		@Var ZSymbolEntry Entry = this.GetSymbol(VarName);
	//		//System.out.println("var " + VarName + ", entry=" + Entry + ", NameSpace=" + this);
	//		if(Entry instanceof ZVariable) {
	//			return (ZVariable)Entry;
	//		}
	//		return null;
	//	}
	//
	//	public final int SetLocalVariable(ZFunctionNode FunctionNode, ZType VarType, String VarName, ZToken SourceToken) {
	//		@Var ZSymbolEntry Parent = this.GetSymbol(VarName);
	//		@Var ZVariable VarInfo = new ZVariable(Parent, FunctionNode, 0, VarType, VarName, SourceToken);
	//		//System.out.println("set var " + VarName + ", entry=" + VarInfo + ", NameSpace=" + this);
	//		this.SetLocalSymbolEntry(VarName, VarInfo);
	//		return VarInfo.VarUniqueIndex;
	//	}

	// Type

	public final void SetTypeName(String Name, ZType Type, @Nullable ZToken SourceToken) {
		@Var ZTypeNode Node = new ZTypeNode(null, SourceToken, Type);
		this.SetSymbol(Name, Node);
	}

	public final void SetTypeName(ZType Type, @Nullable ZToken SourceToken) {
		this.SetTypeName(Type.ShortName, Type, SourceToken);
	}

	public final ZType GetType(String TypeName, ZToken SourceToken, boolean IsCreation) {
		@Var ZNode Node = this.GetSymbol(TypeName);
		if(Node instanceof ZTypeNode) {
			return Node.Type;
		}
		if(Node == null && IsCreation && LibZen._IsSymbol(LibZen._GetChar(TypeName, 0))) {
			@Var ZType Type = new ZClassType(TypeName, ZType.VarType);
			this.GetRootNameSpace().SetTypeName(TypeName, Type, SourceToken);
			return Type;
		}
		return null;
	}


}
