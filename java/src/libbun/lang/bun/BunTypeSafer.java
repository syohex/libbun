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

package libbun.lang.bun;

import libbun.parser.BGenerator;
import libbun.parser.BLogger;
import libbun.parser.BNameSpace;
import libbun.parser.BNodeUtils;
import libbun.parser.BToken;
import libbun.parser.BTypeChecker;
import libbun.parser.ast.BBooleanNode;
import libbun.parser.ast.BFloatNode;
import libbun.parser.ast.BGetNameNode;
import libbun.parser.ast.BIntNode;
import libbun.parser.ast.BLetVarNode;
import libbun.parser.ast.BNode;
import libbun.parser.ast.BNullNode;
import libbun.parser.ast.BSetNameNode;
import libbun.parser.ast.BStringNode;
import libbun.parser.ast.ZAndNode;
import libbun.parser.ast.ZArrayLiteralNode;
import libbun.parser.ast.ZBinaryNode;
import libbun.parser.ast.ZBlockNode;
import libbun.parser.ast.ZBreakNode;
import libbun.parser.ast.ZCastNode;
import libbun.parser.ast.ZClassNode;
import libbun.parser.ast.ZComparatorNode;
import libbun.parser.ast.ZDefaultValueNode;
import libbun.parser.ast.ZErrorNode;
import libbun.parser.ast.ZFuncCallNode;
import libbun.parser.ast.ZFuncNameNode;
import libbun.parser.ast.ZFunctionNode;
import libbun.parser.ast.ZGetIndexNode;
import libbun.parser.ast.ZGetterNode;
import libbun.parser.ast.ZGroupNode;
import libbun.parser.ast.ZIfNode;
import libbun.parser.ast.ZInstanceOfNode;
import libbun.parser.ast.ZListNode;
import libbun.parser.ast.ZLocalDefinedNode;
import libbun.parser.ast.ZMacroNode;
import libbun.parser.ast.ZMapEntryNode;
import libbun.parser.ast.ZMapLiteralNode;
import libbun.parser.ast.ZMethodCallNode;
import libbun.parser.ast.ZNewObjectNode;
import libbun.parser.ast.ZNotNode;
import libbun.parser.ast.ZOrNode;
import libbun.parser.ast.ZReturnNode;
import libbun.parser.ast.ZSetIndexNode;
import libbun.parser.ast.ZSetterNode;
import libbun.parser.ast.ZThrowNode;
import libbun.parser.ast.ZTopLevelNode;
import libbun.parser.ast.ZTryNode;
import libbun.parser.ast.ZTypeNode;
import libbun.parser.ast.ZUnaryNode;
import libbun.parser.ast.ZVarBlockNode;
import libbun.parser.ast.ZWhileNode;
import libbun.type.BClassType;
import libbun.type.BFunc;
import libbun.type.BFuncType;
import libbun.type.BGenericType;
import libbun.type.BMacroFunc;
import libbun.type.BPrototype;
import libbun.type.BType;
import libbun.type.BTypePool;
import libbun.type.BVarScope;
import libbun.type.BVarType;
import libbun.util.BField;
import libbun.util.BLib;
import libbun.util.Nullable;
import libbun.util.Var;

public class BunTypeSafer extends BTypeChecker {

	@BField protected ZFunctionNode CurrentFunctionNode = null;

	public BunTypeSafer(BGenerator Generator) {
		super(Generator);
	}

	public final boolean IsTopLevel() {
		return (this.CurrentFunctionNode == null);
	}

	@Override public void VisitDefaultValueNode(ZDefaultValueNode Node) {
		@Var BType Type = this.GetContextType();
		if(Type.IsIntType()) {
			this.ReturnTypeNode(new BIntNode(Node.ParentNode, null, 0), Type);
		}
		else if(Type.IsBooleanType()) {
			this.ReturnTypeNode(new BBooleanNode(Node.ParentNode, null, false), Type);
		}
		else if(Type.IsFloatType()) {
			this.ReturnTypeNode(new BFloatNode(Node.ParentNode, null, 0.0), Type);
		}
		else if(!Type.IsVarType()) {
			this.ReturnTypeNode(new BNullNode(Node.ParentNode, null), Type);
		}
		else {
			this.ReturnTypeNode(Node, Type);
		}
	}

	@Override public void VisitNullNode(BNullNode Node) {
		@Var BType Type = this.GetContextType();
		this.ReturnTypeNode(Node, Type);
	}

	@Override public void VisitBooleanNode(BBooleanNode Node) {
		this.ReturnTypeNode(Node, BType.BooleanType);
	}

	@Override public void VisitIntNode(BIntNode Node) {
		this.ReturnTypeNode(Node, BType.IntType);
	}

	@Override public void VisitFloatNode(BFloatNode Node) {
		this.ReturnTypeNode(Node, BType.FloatType);
	}

	@Override public void VisitStringNode(BStringNode Node) {
		this.ReturnTypeNode(Node, BType.StringType);
	}

	@Override public void VisitArrayLiteralNode(ZArrayLiteralNode Node) {
		@Var BType ArrayType = this.GetContextType();
		if(ArrayType.IsMapType() && Node.GetListSize() == 0) {
			/* this is exceptional treatment for map literal */
			this.ReturnTypeNode(new ZMapLiteralNode(Node.ParentNode), ArrayType);
			return;
		}
		@Var BType ElementType = BType.VarType;
		if(ArrayType.IsArrayType()) {
			ElementType = ArrayType.GetParamType(0);
		}
		@Var int i = 0;
		while(i < Node.GetListSize()) {
			@Var BNode SubNode = Node.GetListAt(i);
			SubNode = this.CheckType(SubNode, ElementType);
			Node.SetListAt(i, SubNode);
			if(ElementType.IsVarType()) {
				ElementType = SubNode.Type;
			}
			i = i + 1;
		}
		if(!ElementType.IsVarType()) {
			this.ReturnTypeNode(Node,BTypePool._GetGenericType1(BGenericType._ArrayType, ElementType));
		}
		else {
			this.ReturnTypeNode(Node, BType.VarType);
		}
	}

	@Override public void VisitMapLiteralNode(ZMapLiteralNode Node) {
		@Var BType ContextType = this.GetContextType();
		@Var BType EntryType = BType.VarType;
		if(ContextType.IsMapType()) {
			EntryType = ContextType.GetParamType(0);
		}
		@Var int i = 0;
		while(i < Node.GetListSize()) {
			@Var ZMapEntryNode EntryNode = Node.GetMapEntryNode(i);
			if(EntryNode.Name == null) {
				EntryNode.Name = EntryNode.KeyNode().SourceToken.GetText();
			}
			if(EntryNode.IsUntyped()) {
				this.CheckTypeAt(EntryNode, ZMapEntryNode._Value, EntryType);
				if(EntryType.IsVarType()) {
					EntryType = EntryNode.GetAstType(ZMapEntryNode._Value);
				}
			}
			i = i + 1;
		}
		if(!EntryType.IsVarType()) {
			this.ReturnTypeNode(Node, BTypePool._GetGenericType1(BGenericType._MapType, EntryType));
			return;
		}
		else {
			this.ReturnTypeNode(Node, BType.VarType);
		}
	}

	@Override public void VisitGetNameNode(BGetNameNode Node) {
		@Var BNameSpace NameSpace = Node.GetNameSpace();
		@Var BLetVarNode VarNode = NameSpace.GetSymbol(Node.GivenName);
		Node.ResolvedNode = VarNode;
		if(VarNode != null) {
			VarNode.Used();
			this.ReturnTypeNode(Node, VarNode.DeclType());
		}
		else {
			this.ReturnTypeNode(Node, BType.VarType);
		}
	}

	@Override public void VisitSetNameNode(BSetNameNode Node) {
		this.CheckTypeAt(Node, BSetNameNode._NameInfo, BType.VarType);
		@Var BGetNameNode NameNode = Node.NameNode();
		if(!NameNode.IsUntyped()) {
			this.CheckTypeAt(Node, BSetNameNode._Expr, Node.NameNode().Type);
			this.ReturnTypeNode(Node, BType.VoidType);
			return;
		}
		//this.ReturnErrorNode(Node, Node.SourceToken, "readonly variable"); // FIXME
	}

	private BType GetIndexType(BNameSpace NameSpace, BType RecvType) {
		if(RecvType.IsArrayType() || RecvType.IsStringType()) {
			return BType.IntType;
		}
		if(RecvType.IsMapType()) {
			return BType.StringType;
		}
		return BType.VarType;
	}

	private BType GetElementType(BNameSpace NameSpace, BType RecvType) {
		if(RecvType.IsArrayType() || RecvType.IsMapType()) {
			return RecvType.GetParamType(0);
		}
		if(RecvType.IsStringType()) {
			return BType.StringType;
		}
		return BType.VarType;
	}

	@Override public void VisitGetIndexNode(ZGetIndexNode Node) {
		@Var BNameSpace NameSpace = Node.GetNameSpace();
		this.CheckTypeAt(Node, ZGetIndexNode._Recv, BType.VarType);
		this.CheckTypeAt(Node, ZGetIndexNode._Index, this.GetIndexType(NameSpace, Node.RecvNode().Type));
		this.ReturnTypeNode(Node, this.GetElementType(NameSpace, Node.RecvNode().Type));
	}

	@Override public void VisitSetIndexNode(ZSetIndexNode Node) {
		@Var BNameSpace NameSpace = Node.GetNameSpace();
		this.CheckTypeAt(Node, ZSetIndexNode._Recv, BType.VarType);
		this.CheckTypeAt(Node, ZSetIndexNode._Index, this.GetIndexType(NameSpace, Node.RecvNode().Type));
		this.CheckTypeAt(Node, ZSetIndexNode._Expr, this.GetElementType(NameSpace, Node.RecvNode().Type));
		this.ReturnTypeNode(Node, BType.VoidType);
	}

	@Override public void VisitGroupNode(ZGroupNode Node) {
		@Var BType ContextType = this.GetContextType();
		this.CheckTypeAt(Node, ZGroupNode._Expr, ContextType);
		this.ReturnTypeNode(Node, Node.GetAstType(ZGroupNode._Expr));
	}

	@Override public void VisitMacroNode(ZMacroNode FuncNode) {
		this.ReturnNode(this.TypeListNodeAsFuncCall(FuncNode, FuncNode.GetFuncType()));
	}

	@Override public void VisitFuncCallNode(ZFuncCallNode Node) {
		@Var BNameSpace NameSpace = Node.GetNameSpace();
		this.TypeCheckNodeList(Node);
		this.CheckTypeAt(Node, ZFuncCallNode._Functor, BType.VarType);
		@Var BNode FuncNode = Node.FunctorNode();
		@Var BType FuncNodeType = Node.GetAstType(ZFuncCallNode._Functor);
		if(FuncNodeType instanceof BFuncType) {
			this.ReturnNode(this.TypeListNodeAsFuncCall(Node, (BFuncType)FuncNodeType));
			return;
		}
		if(FuncNode instanceof ZTypeNode) {   // TypeName()..;
			@Var String FuncName = FuncNode.Type.GetName();
			FuncNode = new ZFuncNameNode(Node, FuncNode.SourceToken, FuncName, FuncNode.Type, Node.GetListSize());
			Node.SetNode(ZFuncCallNode._Functor, FuncNode);
		}
		if(FuncNode instanceof BGetNameNode) {
			@Var String FuncName = ((BGetNameNode)FuncNode).GivenName;
			FuncNode = new ZFuncNameNode(Node, FuncNode.SourceToken, FuncName, Node.GetRecvType(), Node.GetListSize());
			Node.SetNode(ZFuncCallNode._Functor, FuncNode);
		}
		if(FuncNode instanceof ZFuncNameNode) {
			ZFuncNameNode FuncNameNode = (ZFuncNameNode)FuncNode;
			@Var BFunc Func = this.LookupFunc(NameSpace, FuncNameNode.FuncName, FuncNameNode.RecvType, FuncNameNode.FuncParamSize);
			if(Func instanceof BMacroFunc) {
				@Var ZMacroNode MacroNode = Node.ToMacroNode((BMacroFunc)Func);
				this.ReturnNode(this.TypeListNodeAsFuncCall(MacroNode, Func.GetFuncType()));
				return;
			}
			if(Func != null) {
				this.ReturnNode(this.TypeListNodeAsFuncCall(Node, Func.GetFuncType()));
				return;
			}
			this.ReturnTypeNode(Node, BType.VarType);
		}
		else {
			this.ReturnNode(new ZErrorNode(Node, "not function: " + FuncNodeType + " of node " + Node.FunctorNode()));
		}
	}

	private BType LookupFieldType(BNameSpace NameSpace, BType ClassType, String FieldName) {
		ClassType = ClassType.GetRealType();
		if(ClassType instanceof BClassType) {
			return ((BClassType)ClassType).GetFieldType(FieldName, BType.VoidType);
		}
		return NameSpace.Generator.GetFieldType(ClassType, FieldName);
	}

	private BType LookupSetterType(BNameSpace NameSpace, BType ClassType, String FieldName) {
		ClassType = ClassType.GetRealType();
		if(ClassType instanceof BClassType) {
			return ((BClassType)ClassType).GetFieldType(FieldName, BType.VoidType);
		}
		return NameSpace.Generator.GetSetterType(ClassType, FieldName);
	}

	private BNode UndefinedFieldNode(BNode Node, String Name) {
		return new ZErrorNode(Node, "undefined field: " + Name + " of " + Node.GetAstType(ZGetterNode._Recv));
	}

	@Override public void VisitGetterNode(ZGetterNode Node) {
		this.CheckTypeAt(Node, ZGetterNode._Recv, BType.VarType);
		@Var BNode RecvNode = Node.RecvNode();
		if(!RecvNode.IsUntyped()) {
			@Var BType FieldType = this.LookupFieldType(Node.GetNameSpace(), Node.GetAstType(ZGetterNode._Recv), Node.GetName());
			if(FieldType.IsVoidType()) {
				this.ReturnNode(this.UndefinedFieldNode(Node, Node.GetName()));
				return;
			}
			this.ReturnTypeNode(Node, FieldType);
			return;
		}
		//		if(RecvNode instanceof BGetNameNode) {
		//			@Var String Symbol = ((BGetNameNode)RecvNode).GetName() + "." + Node.GetName();
		//			@Var BNode VarNode = Node.GetNameSpace().GetSymbol(Symbol);
		//			if(VarNode instanceof BAsmNode) {
		//				((BGetNameNode) RecvNode).GivenName = Symbol;
		//				((BGetNameNode) RecvNode).ResolvedNode = VarNode;
		//				this.ReturnTypeNode(RecvNode, VarNode.Type);
		//				return;
		//			}
		//		}
		this.ReturnTypeNode(Node, BType.VarType);
	}

	@Override public void VisitSetterNode(ZSetterNode Node) {
		this.CheckTypeAt(Node, ZSetterNode._Recv, BType.VarType);
		if(!Node.RecvNode().IsUntyped()) {
			@Var BNameSpace NameSpace = Node.GetNameSpace();
			@Var BType FieldType = this.LookupSetterType(NameSpace, Node.GetAstType(ZSetterNode._Recv), Node.GetName());
			if(FieldType.IsVoidType()) {
				this.ReturnNode(this.UndefinedFieldNode(Node, Node.GetName()));
				return;
			}
			this.CheckTypeAt(Node, ZSetterNode._Expr, FieldType);
			this.ReturnTypeNode(Node, BType.VoidType);
		}
		else {
			/* if Recv is Var, type should not be decided */
			this.ReturnTypeNode(Node, BType.VarType);
		}
	}

	private void VisitListAsNativeMethod(BNode Node, BType RecvType, String MethodName, ZListNode List) {
		@Var BFuncType FuncType = this.Generator.GetMethodFuncType(RecvType, MethodName, List);
		if(FuncType != null) {
			if(!FuncType.IsVarType()) {
				@Var int i = 0;
				//@Var int StaticShift = FuncType.GetParamSize() - List.GetListSize();
				@Var int StaticShift = FuncType.GetFuncParamSize() - List.GetListSize();
				while(i < List.GetListSize()) {
					@Var BNode SubNode = List.GetListAt(i);
					SubNode = this.CheckType(SubNode, FuncType.GetFuncParamType(i+StaticShift));
					List.SetListAt(i, SubNode);
					i = i + 1;
				}
			}
			this.ReturnTypeNode(Node, FuncType.GetReturnType());
			return;
		}
		@Var String Message = null;
		if(MethodName == null) {
			Message = "undefined constructor: " + RecvType;
		}
		else {
			Message = "undefined method: " + MethodName + " of " + RecvType;
		}
		this.ReturnErrorNode(Node, null, Message);
	}

	@Override public void VisitMethodCallNode(ZMethodCallNode Node) {
		this.CheckTypeAt(Node, ZMethodCallNode._Recv, BType.VarType);
		@Var BNameSpace NameSpace = Node.GetNameSpace();
		@Var BNode RecvNode = Node.RecvNode();
		if(!RecvNode.IsUntyped()) {
			@Var BType FieldType = this.LookupFieldType(NameSpace, Node.GetAstType(ZMethodCallNode._Recv), Node.MethodName());
			if(FieldType instanceof BFuncType) {
				@Var BFuncType FieldFuncType = (BFuncType)FieldType;
				@Var ZFuncCallNode FuncCall = Node.ToGetterFuncCall(FieldFuncType);
				this.ReturnNode(this.TypeListNodeAsFuncCall(FuncCall, FieldFuncType));
				return;
			}
			@Var int FuncParamSize = Node.GetListSize() + 1;
			@Var BFunc Func = this.LookupFunc(NameSpace, Node.MethodName(), Node.GetAstType(ZMethodCallNode._Recv), FuncParamSize);
			if(Func != null) {
				@Var ZListNode FuncCallNode = Node.ToFuncCallNode(this, Func, RecvNode);
				this.ReturnNode(this.TypeListNodeAsFuncCall(FuncCallNode, Func.GetFuncType()));
				return;
			}
			this.VisitListAsNativeMethod(Node, Node.GetAstType(ZMethodCallNode._Recv), Node.MethodName(), Node);
			return;
		}
		this.TypeCheckNodeList(Node);
		//		if(RecvNode instanceof BGetNameNode) {
		//			@Var String Symbol = ((BGetNameNode)RecvNode).GetName();
		//			@Var String FuncName = Symbol + "." + Node.MethodName();
		//			@Var int FuncParamSize = Node.GetListSize();
		//			@Var ZFunc Func = this.LookupFunc(NameSpace, FuncName, Node.GetAstType(ZMethodCallNode._NameInfo+1), FuncParamSize);
		//			if(Func != null) {
		//				@Var ZListNode FuncCallNode = Node.ToFuncCallNode(this, Func, null);
		//				this.ReturnNode(this.TypeListNodeAsFuncCall(FuncCallNode, Func.GetFuncType()));
		//				return;
		//			}
		//			@Var BNode VarNode = NameSpace.GetSymbol(Symbol);
		//			if(VarNode instanceof BAsmNode) {
		//				this.ReturnTypeNode(Node, ZType.VarType);
		//				return;
		//			}
		//			//			LibZen._PrintLine("FIXME: undefined function call:" + FuncName);
		//			//			//TODO: undefined function
		//		}
		this.ReturnTypeNode(Node, BType.VarType);
	}

	@Override public void VisitNewObjectNode(ZNewObjectNode Node) {
		@Var BNameSpace NameSpace = Node.GetNameSpace();
		@Var BType ContextType = this.GetContextType();
		this.TypeCheckNodeList(Node);
		if(Node.ClassType().IsVarType()) {
			if(ContextType.IsVarType()) {
				this.ReturnTypeNode(Node, BType.VarType);
				return;
			}
			Node.GivenType = ContextType;
		}
		@Var int FuncParamSize = Node.GetListSize() + 1;
		@Var BFunc Func = this.LookupFunc(NameSpace, Node.ClassType().GetName(), Node.ClassType(), FuncParamSize);
		if(Func != null) {
			@Var ZListNode FuncCall = Node.ToFuncCallNode(NameSpace.Generator.TypeChecker, Func);
			this.ReturnNode(this.TypeListNodeAsFuncCall(FuncCall, Func.GetFuncType()));
			return;
		}
		if(FuncParamSize == 1) { /* no argument */
			this.ReturnTypeNode(Node, Node.ClassType());
		}
		else {
			this.VisitListAsNativeMethod(Node, Node.ClassType(), null, Node);
		}
	}

	@Override public void VisitUnaryNode(ZUnaryNode Node) {
		this.CheckTypeAt(Node, ZUnaryNode._Recv, BType.VarType);
		this.ReturnTypeNode(Node, Node.RecvNode().Type);
	}

	@Override public void VisitNotNode(ZNotNode Node) {
		this.CheckTypeAt(Node, ZNotNode._Recv, BType.BooleanType);
		this.ReturnTypeNode(Node, BType.BooleanType);
	}

	@Override public void VisitCastNode(ZCastNode Node) {
		@Var BType ContextType = this.GetContextType();
		if(Node.CastType().IsVarType()) {
			Node.Type = ContextType;
		}
		this.TryTypeAt(Node, ZCastNode._Expr, Node.CastType());
		@Var BType ExprType = Node.ExprNode().Type;
		if(Node.Type.IsVarType() || ExprType.IsVarType()) {
			this.ReturnNode(Node);
			return;
		}
		if(ExprType.Equals(Node.Type) || Node.Type.Accept(ExprType)) {
			this.ReturnNode(Node.ExprNode());
			return;
		}
		if(ExprType.Accept(Node.Type)) {
			this.ReturnNode(this.CreateStupidCastNode(Node.Type, Node.ExprNode(), Node.GetAstToken(ZCastNode._TypeInfo), "unsafe downcast"));
			return;
		}
		else {
			@Var BFunc Func = this.Generator.LookupConverterFunc(ExprType, Node.Type);
			if(Func != null) {
				this.ReturnTypeNode(Node.ToFuncCallNode(this, Func), Node.Type);
				return;
			}
		}
		this.ReturnNode(this.CreateStupidCastNode(Node.Type, Node.ExprNode(), Node.GetAstToken(ZCastNode._TypeInfo), "undefined converter"));
	}

	@Override public void VisitInstanceOfNode(ZInstanceOfNode Node) {
		this.CheckTypeAt(Node, ZBinaryNode._Left, BType.VarType);
		if(!(Node.TargetType() instanceof BClassType)) {
			BLogger._LogWarning(Node.GetAstToken(ZInstanceOfNode._TypeInfo), "instanceof takes a class type; the result is implementation-dependant.");
		}
		this.ReturnTypeNode(Node, BType.BooleanType);
	}

	private BType GuessBinaryLeftType(BToken Op, BType ContextType) {
		if(Op.EqualsText('|') || Op.EqualsText('&') || Op.EqualsText("<<") || Op.EqualsText(">>") || Op.EqualsText('^')) {
			return BType.IntType;
		}
		if(Op.EqualsText('+') || Op.EqualsText('-') || Op.EqualsText('*') || Op.EqualsText('/') || Op.EqualsText('%')) {
			if(ContextType.IsNumberType()) {
				return ContextType;
			}
		}
		return BType.VarType;
	}

	private void UnifyBinaryNodeType(ZBinaryNode Node, BType Type) {
		if(Node.GetAstType(ZBinaryNode._Left).Equals(Type)) {
			this.CheckTypeAt(Node, ZBinaryNode._Right, Type);
			return;
		}
		if(Node.GetAstType(ZBinaryNode._Right).Equals(Type)) {
			this.CheckTypeAt(Node, ZBinaryNode._Left, Type);
		}
	}

	private void UnifyBinaryEnforcedType(ZBinaryNode Node, BType Type) {
		if(Node.GetAstType(ZBinaryNode._Left).Equals(Type)) {
			Node.SetNode(ZBinaryNode._Right, this.EnforceNodeType(Node.RightNode(), Type));
			return;
		}
		if(Node.GetAstType(ZBinaryNode._Right).Equals(Type)) {
			Node.SetNode(ZBinaryNode._Left, this.EnforceNodeType(Node.LeftNode(), Type));
		}
	}

	@Override public void VisitBinaryNode(ZBinaryNode Node) {
		@Var BType ContextType = this.GetContextType();
		@Var BType LeftType = this.GuessBinaryLeftType(Node.SourceToken, ContextType);
		@Var BType RightType = this.GuessBinaryLeftType(Node.SourceToken, ContextType);
		this.CheckTypeAt(Node, ZBinaryNode._Left, LeftType);
		this.CheckTypeAt(Node, ZBinaryNode._Right, RightType);
		if(!Node.GetAstType(ZBinaryNode._Left).Equals(Node.GetAstType(ZBinaryNode._Right))) {
			if(Node.SourceToken.EqualsText('+')) {
				this.UnifyBinaryEnforcedType(Node, BType.StringType);
			}
			this.UnifyBinaryNodeType(Node, BType.FloatType);
			this.CheckTypeAt(Node, ZBinaryNode._Left, Node.GetAstType(ZBinaryNode._Right));
		}
		this.ReturnTypeNode(Node.TryMacroNode(this.Generator), Node.GetAstType(ZBinaryNode._Left));
	}

	@Override public void VisitComparatorNode(ZComparatorNode Node) {
		this.CheckTypeAt(Node, ZBinaryNode._Left, BType.VarType);
		this.TryTypeAt(Node, ZBinaryNode._Right, Node.GetAstType(ZBinaryNode._Left));
		this.UnifyBinaryNodeType(Node, BType.FloatType);
		//this.CheckTypeAt(Node, ZBinaryNode._Right, Node.GetAstType(ZBinaryNode._Left));
		this.ReturnTypeNode(Node, BType.BooleanType);
	}

	@Override public void VisitAndNode(ZAndNode Node) {
		this.CheckTypeAt(Node, ZBinaryNode._Left, BType.BooleanType);
		this.CheckTypeAt(Node, ZBinaryNode._Right, BType.BooleanType);
		this.ReturnTypeNode(Node, BType.BooleanType);
	}

	@Override public void VisitOrNode(ZOrNode Node) {
		this.CheckTypeAt(Node, ZBinaryNode._Left, BType.BooleanType);
		this.CheckTypeAt(Node, ZBinaryNode._Right, BType.BooleanType);
		this.ReturnTypeNode(Node, BType.BooleanType);
	}

	protected void VisitVarDeclNode(BNameSpace NameSpace, BLetVarNode Node1) {
		@Var @Nullable BLetVarNode CurNode = Node1;
		while(CurNode != null) {
			CurNode.InitValueNode();
			this.CheckTypeAt(CurNode, BLetVarNode._InitValue, CurNode.DeclType());
			if(CurNode.DeclType().IsVarType()) {
				CurNode.SetDeclType(CurNode.GetAstType(BLetVarNode._InitValue));
			}
			CurNode.SetDeclType(this.VarScope.NewVarType(CurNode.DeclType(), CurNode.GetGivenName(), CurNode.SourceToken));
			NameSpace.SetSymbol(CurNode.GetGivenName(), CurNode);
			CurNode = CurNode.NextVarNode();
		}
	}

	@Override public void VisitBlockNode(ZBlockNode Node) {
		@Var int i = 0;
		while(i < Node.GetListSize()) {
			@Var BNode SubNode = Node.GetListAt(i);
			@Var BNode TypedNode = this.CheckType(SubNode, BType.VoidType);
			@Var BNode CheckNode = Node.GetListAt(i);
			while(SubNode != CheckNode) {  // detecting replacement
				SubNode = CheckNode;
				TypedNode = this.CheckType(SubNode, BType.VoidType);
				CheckNode = Node.GetListAt(i);
			}
			Node.SetListAt(i, TypedNode);
			if(BNodeUtils._IsBlockBreak(SubNode)) {
				Node.ClearListToSize(i+1);
				break;
			}
			i = i + 1;
		}
		this.ReturnTypeNode(Node, BType.VoidType);
	}

	@Override public void VisitVarBlockNode(ZVarBlockNode Node) {
		if(this.IsTopLevel()) {
			this.ReturnErrorNode(Node, Node.SourceToken, "only available inside function");
			return;
		}
		this.VisitVarDeclNode(Node.GetBlockNameSpace(), Node.VarDeclNode());
		this.VisitBlockNode(Node);
		if(Node.GetListSize() == 0) {
			BLogger._LogWarning(Node.SourceToken, "unused variable: " + Node.VarDeclNode().GetGivenName());
		}
	}

	@Override public void VisitIfNode(ZIfNode Node) {
		this.CheckTypeAt(Node, ZIfNode._Cond, BType.BooleanType);
		this.CheckTypeAt(Node, ZIfNode._Then, BType.VoidType);
		if(Node.HasElseNode()) {
			this.CheckTypeAt(Node, ZIfNode._Else, BType.VoidType);
		}
		this.ReturnTypeNode(Node, BType.VoidType);
	}

	@Override public void VisitReturnNode(ZReturnNode Node) {
		if(this.IsTopLevel()) {
			this.ReturnErrorNode(Node, Node.SourceToken, "only available inside function");
			return;
		}
		@Var BType ReturnType = this.CurrentFunctionNode.ReturnType();
		if(Node.HasReturnExpr() && ReturnType.IsVoidType()) {
			Node.AST[ZReturnNode._Expr] = null;
		}
		else if(!Node.HasReturnExpr() && !ReturnType.IsVarType() && !ReturnType.IsVoidType()) {
			BLogger._LogWarning(Node.SourceToken, "returning default value of " + ReturnType);
			Node.SetNode(ZReturnNode._Expr, new ZDefaultValueNode());
		}
		if(Node.HasReturnExpr()) {
			this.CheckTypeAt(Node, ZReturnNode._Expr, ReturnType);
		}
		else {
			if(ReturnType instanceof BVarType) {
				((BVarType)ReturnType).Infer(BType.VoidType, Node.SourceToken);
			}
		}
		this.ReturnTypeNode(Node, BType.VoidType);
	}

	@Override public void VisitWhileNode(ZWhileNode Node) {
		this.CheckTypeAt(Node, ZWhileNode._Cond, BType.BooleanType);
		this.CheckTypeAt(Node, ZWhileNode._Block, BType.VoidType);
		if(Node.HasNextNode()) {
			this.CheckTypeAt(Node, ZWhileNode._Next, BType.VoidType);
			Node.BlockNode().Append(Node.NextNode());
		}
		this.ReturnTypeNode(Node, BType.VoidType);
	}

	@Override public void VisitBreakNode(ZBreakNode Node) {
		this.ReturnTypeNode(Node, BType.VoidType);
	}

	@Override public void VisitThrowNode(ZThrowNode Node) {
		this.CheckTypeAt(Node, ZThrowNode._Expr, BType.VarType);
		this.ReturnTypeNode(Node, BType.VoidType);
	}

	@Override public void VisitTryNode(ZTryNode Node) {
		this.CheckTypeAt(Node, ZTryNode._Try, BType.VoidType);
		if(Node.HasCatchBlockNode()) {
			@Var BNameSpace NameSpace = Node.CatchBlockNode().GetBlockNameSpace();
			@Var BLetVarNode VarNode = new BLetVarNode(Node, BLetVarNode._IsReadOnly, null, null);
			VarNode.GivenName = Node.ExceptionName();
			VarNode.GivenType = BClassType._ObjectType;
			NameSpace.SetSymbol(VarNode.GetGivenName(), VarNode);
			this.CheckTypeAt(Node, ZTryNode._Catch, BType.VoidType);
		}
		if(Node.HasFinallyBlockNode()) {
			this.CheckTypeAt(Node, ZTryNode._Finally, BType.VoidType);
		}
		this.ReturnTypeNode(Node, BType.VoidType);
	}

	@Override public void VisitLetNode(BLetVarNode Node) {
		if(Node.IsTopLevel()) {
			@Var BType DeclType = Node.DeclType();
			this.CheckTypeAt(Node, BLetVarNode._InitValue, DeclType);
			@Var BType ConstType = Node.InitValueNode().Type;
			Node.SetAstType(BLetVarNode._NameInfo, ConstType);
			Node.NameIndex = this.Generator.GetUniqueNumber();
			Node.GetNameSpace().SetSymbol(Node.GetGivenName(), Node);
			this.ReturnTypeNode(Node, BType.VoidType);
		}
		else {
			this.ReturnNode(new ZVarBlockNode(null, Node, Node.GetScopeBlockNode()));
		}
	}

	@Override public void DefineFunction(ZFunctionNode FunctionNode, boolean Enforced) {
		if(FunctionNode.FuncName() != null && FunctionNode.ResolvedFuncType == null) {
			@Var BFuncType FuncType = FunctionNode.GetFuncType();
			if(Enforced || !FuncType.IsVarType()) {
				@Var BNameSpace NameSpace = FunctionNode.GetNameSpace();
				@Var BPrototype Func = NameSpace.Generator.SetPrototype(FunctionNode, FunctionNode.FuncName(), FuncType);
				if(Func != null) {
					Func.Defined();
					if(Func.DefinedCount > 1) {
						BLogger._LogError(FunctionNode.SourceToken, "redefinition of function: " + Func);
					}
				}
			}
		}
	}

	private void PushFunctionNode(BNameSpace NameSpace, ZFunctionNode FunctionNode, BType ContextType) {
		@Var BFuncType FuncType = null;
		if(ContextType instanceof BFuncType) {
			FuncType = (BFuncType)ContextType;
		}
		this.CurrentFunctionNode = FunctionNode.Push(this.CurrentFunctionNode);
		this.VarScope = new BVarScope(this.VarScope, this.Logger, null);
		@Var int i = 0;
		while(i < FunctionNode.GetListSize()) {
			@Var BLetVarNode ParamNode = FunctionNode.GetParamNode(i);
			ParamNode.SetDeclType(this.VarScope.NewVarType(ParamNode.DeclType(), ParamNode.GetGivenName(), ParamNode.GetAstToken(BLetVarNode._NameInfo)));
			if(FuncType != null) {
				this.VarScope.InferType(FuncType.GetFuncParamType(i), ParamNode);
			}
			NameSpace.SetSymbol(ParamNode.GetGivenName(), ParamNode);
			i = i + 1;
		}
		FunctionNode.SetReturnType(this.VarScope.NewVarType(FunctionNode.ReturnType(), "return", FunctionNode.SourceToken));
		if(FuncType != null) {
			FunctionNode.Type.Maybe(FuncType.GetReturnType(), null);
		}
	}

	private void PopFunctionNode(BNameSpace NameSpace) {
		this.CurrentFunctionNode = this.CurrentFunctionNode.Pop();
		this.VarScope = this.VarScope.Parent;
	}

	@Override public void VisitFunctionNode(ZFunctionNode Node) {
		//LibZen._PrintDebug("name="+Node.FuncName+ ", Type=" + Node.Type + ", IsTopLevel=" + this.IsTopLevel());
		@Var BType ContextType = this.GetContextType();
		if(Node.IsUntyped()) {
			Node.Type = ContextType;  // funcdecl is requested with VoidType
		}
		if(Node.Type.IsVoidType()) {
			if(Node.FuncName() == null) {   // function() object
				Node.Type = BType.VarType;
			}
			//			if(!this.IsTopLevel()) {
			//				/* function f() {} ==> var f = function() {} */
			//				@Var ZVarNode VarNode = new ZVarNode(Node.ParentNode);
			//				VarNode.SetNode(ZLetVarNode._NameInfo, Node.AST[ZFunctionNode._NameInfo]);
			//				VarNode.SetNode(ZLetVarNode._InitValue, Node);
			//				@Var ZBlockNode Block = Node.GetScopeBlockNode();
			//				@Var int Index = Block.IndexOf(Node);
			//				Block.CopyTo(Index+1, VarNode);
			//				Block.ClearListAfter(Index+1);   // Block[Index] is set to VarNode
			//				this.VisitVarNode(VarNode);
			//				return;
			//			}
		}
		if(!BNodeUtils._HasFunctionBreak(Node.BlockNode())) {
			//System.out.println("adding return.. ");
			Node.BlockNode().SetNode(BNode._AppendIndex, new ZReturnNode(Node));
		}
		@Var BNameSpace NameSpace = Node.BlockNode().GetBlockNameSpace();
		this.PushFunctionNode(NameSpace, Node, ContextType);
		this.VarScope.TypeCheckFuncBlock(this, Node);
		this.PopFunctionNode(NameSpace);
		if(!Node.Type.IsVoidType()) {
			Node.Type = Node.GetFuncType();
		}
		this.ReturnNode(Node);
	}

	@Override public void VisitClassNode(ZClassNode Node) {
		@Var BNameSpace NameSpace = Node.GetNameSpace();
		@Var BType ClassType = NameSpace.GetType(Node.ClassName(), Node.SourceToken, true/*IsCreation*/);
		if(ClassType instanceof BClassType) {
			if(!ClassType.IsOpenType()) {
				this.ReturnNode(new ZErrorNode(Node, Node.ClassName() + " has been defined."));
				return;
			}
			Node.ClassType = (BClassType)ClassType;
		}
		else {
			this.ReturnNode(new ZErrorNode(Node, Node.ClassName() + " is not a Zen class."));
			return;
		}
		//System.out.println(" B NodeClass.ToOpen="+Node.ClassType+", IsOpenType="+Node.ClassType.IsOpenType());
		if(Node.SuperType() != null) {
			if(Node.SuperType() instanceof BClassType && !Node.SuperType().IsOpenType()) {
				Node.ClassType.EnforceSuperClass((BClassType)Node.SuperType());
			}
			else {
				this.ReturnNode(new ZErrorNode(Node.ParentNode, Node.GetAstToken(ZClassNode._TypeInfo), "" + Node.SuperType() + " cannot be extended."));
				return;
			}
		}
		@Var int i = 0;
		while(i < Node.GetListSize()) {
			@Var BLetVarNode FieldNode = Node.GetFieldNode(i);
			if(!Node.ClassType.HasField(FieldNode.GetGivenName())) {
				FieldNode.InitValueNode();// creation of default value if not given;
				this.CheckTypeAt(FieldNode, BLetVarNode._InitValue, FieldNode.DeclType());
				if(FieldNode.DeclType().IsVarType()) {
					FieldNode.SetDeclType(FieldNode.InitValueNode().Type);
				}
				if(FieldNode.DeclType().IsVarType()) {
					BLogger._LogError(FieldNode.SourceToken, "type of " + FieldNode.GetGivenName() + " is unspecific");
				}
				else {
					Node.ClassType.AppendField(FieldNode.DeclType(), FieldNode.GetGivenName(), FieldNode.SourceToken);
				}
			}
			else {
				BLogger._LogError(FieldNode.SourceToken, "duplicated field: " + FieldNode.GetGivenName());
			}
			FieldNode.Type = BType.VoidType;
			i = i + 1;
		}
		Node.ClassType.TypeFlag = BLib._UnsetFlag(Node.ClassType.TypeFlag, BType.OpenTypeFlag);
		//System.out.println(" E NodeClass.ToOpen="+Node.ClassType+", IsOpenType="+Node.ClassType.IsOpenType());
		this.ReturnTypeNode(Node, BType.VoidType);
	}

	@Override public void VisitTopLevelNode(ZTopLevelNode Node) {
		// TODO Auto-generated method stub
		System.out.println("FIXME: " + Node);
	}

	@Override public void VisitLocalDefinedNode(ZLocalDefinedNode Node) {
		// TODO Auto-generated method stub
		System.out.println("FIXME: " + Node);
	}


	// utils

	private BFunc LookupFunc(BNameSpace NameSpace, String FuncName, BType RecvType, int FuncParamSize) {
		@Var String Signature = BFunc._StringfySignature(FuncName, FuncParamSize, RecvType);
		@Var BFunc Func = this.Generator.GetDefinedFunc(Signature);
		//System.out.println("LookupFunc: " + Func + " by " + Signature);
		if(Func != null) {
			return Func;
		}
		RecvType = RecvType.GetSuperType();
		while(RecvType != null) {
			Signature = BFunc._StringfySignature(FuncName, FuncParamSize, RecvType);
			Func = this.Generator.GetDefinedFunc(Signature);
			if(Func != null) {
				return Func;
			}
			if(RecvType.IsVarType()) {
				break;
			}
			RecvType = RecvType.GetSuperType();
		}
		//		if(Func == null) {
		//			System.err.println("Unfound: " + FuncName + ", " + RecvType + ", " + FuncParamSize);
		//		}
		return null;
	}

	//	private ZFunc LookupFunc2(ZNameSpace NameSpace, String FuncName, ZType RecvType, int FuncParamSize) {
	//		@Var ZFunc Func = this.Generator.LookupFunc(FuncName, RecvType, FuncParamSize);
	//		if(Func == null && RecvType.IsIntType()) {
	//			Func = this.Generator.GetDefinedFunc(FuncName, ZType.FloatType, FuncParamSize);
	//		}
	//		if(Func == null && RecvType.IsFloatType()) {
	//			Func = this.Generator.GetDefinedFunc(FuncName, ZType.IntType, FuncParamSize);
	//		}
	//		if(Func == null) {
	//			System.err.println("Unfound: " + FuncName + ", " + RecvType + ", " + FuncParamSize);
	//		}
	//		return null;
	//	}

}

