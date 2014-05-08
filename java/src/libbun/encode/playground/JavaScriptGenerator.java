// ***************************************************************************
// Copyright (c) 2013, JST/CREST DEOS project authors. All rights reserved.
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

package libbun.encode.playground;

import libbun.ast.binary.AssignNode;
import libbun.ast.error.ErrorNode;
import libbun.ast.error.TypeErrorNode;
import libbun.ast.expression.GetIndexNode;
import libbun.ast.statement.BunTryNode;
import libbun.parser.common.BunLogger;
import libbun.type.BType;
import libbun.util.LibBunSystem;
import libbun.util.Var;

public class JavaScriptGenerator extends libbun.encode.release.JavaScriptGenerator {
	public JavaScriptGenerator() {
		this.SetNativeType(BType.BooleanType, "Boolean");
		this.SetNativeType(BType.IntType, "Number");
		this.SetNativeType(BType.FloatType, "Number");
		this.SetNativeType(BType.StringType, "String");
		this.SetNativeType(BType.VarType, "Object");
		this.LoadInlineLibrary("inline.js", "//");
		this.SetReservedName("this", "self");
	}

	@Override public void VisitTryNode(BunTryNode Node) {
		this.Source.Append("try");
		this.GenerateExpression(Node.TryBlockNode());
		if(Node.HasCatchBlockNode()){
			@Var String VarName = Node.ExceptionName();
			this.Source.Append("catch(", VarName, ") ");
			this.GenerateExpression(Node.CatchBlockNode());
		}
		if (Node.HasFinallyBlockNode()) {
			this.Source.Append("finally");
			this.GenerateExpression(Node.FinallyBlockNode());
		}
	}

	@Override public void VisitErrorNode(ErrorNode Node) {
		if(Node instanceof TypeErrorNode) {
			@Var TypeErrorNode ErrorNode = (TypeErrorNode)Node;
			this.GenerateExpression(ErrorNode.ErrorNode);
		}
		else {
			@Var String Message = BunLogger._LogError(Node.SourceToken, Node.ErrorMessage);
			this.Source.AppendWhiteSpace();
			this.Source.Append("(function(){ throw new Error(");
			this.Source.Append(LibBunSystem._QuoteString(Message));
			this.Source.Append("); })()");
		}
	}

	@Override public void VisitAssignNode(AssignNode Node) {
		this.GenerateExpression(Node.LeftNode());
		this.Source.Append(" = ");
		this.GenerateExpression(Node.RightNode());
	}

	@Override public void VisitGetIndexNode(GetIndexNode Node) {
		this.GenerateExpression(Node.RecvNode());
		this.GenerateExpression("[", Node.IndexNode(), "]");
	}
}
