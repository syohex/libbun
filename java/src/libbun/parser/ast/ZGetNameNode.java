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

package libbun.parser.ast;

import libbun.parser.ZToken;
import libbun.parser.ZVisitor;
import libbun.util.Field;
import libbun.util.Nullable;
import libbun.util.Var;

public class ZGetNameNode extends ZNode {
	@Field public boolean IsCaptured = false;
	@Field public String  GivenName;
	@Field public int   VarIndex = 0;
	@Field @Nullable public ZNode ResolvedNode = null;

	public ZGetNameNode(ZNode ParentNode, ZToken SourceToken, String GivenName) {
		super(ParentNode, SourceToken, 0);
		this.GivenName = GivenName;
	}

	//	public ZGetNameNode(ZNode ParentNode, ZFunc ResolvedFunc) {
	//		super(ParentNode, null, 0);
	//		this.GivenName = ResolvedFunc.FuncName;
	//		this.Type = ResolvedFunc.GetFuncType();
	//	}

	public final boolean IsGlobalName() {
		if(this.ResolvedNode != null) {
			return this.ResolvedNode.GetDefiningFunctionNode() == null;
		}
		return false;
	}

	public final String GetName() {
		@Var ZNode ResolvedNode = this.ResolvedNode;
		if(ResolvedNode != null) {
			@Var ZFunctionNode DefNode = this.ResolvedNode.GetDefiningFunctionNode();
			if(DefNode == null) {
				if(ResolvedNode instanceof ZLetVarNode) {
					return ((ZLetVarNode)ResolvedNode).GlobalName;
				}
			}
		}
		return this.GivenName;
	}

	@Override public void Accept(ZVisitor Visitor) {
		Visitor.VisitGetNameNode(this);
	}

}