/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.codeInsight.generation.surroundWith;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;

class JavaWithTryFinallySurrounder extends JavaStatementsSurrounder{
  public String getTemplateDescription() {
    return CodeInsightBundle.message("surround.with.try.finally.template");
  }

  public TextRange surroundStatements(Project project, Editor editor, PsiElement container, PsiElement[] statements) throws IncorrectOperationException{
    PsiManager manager = PsiManager.getInstance(project);
    PsiElementFactory factory = JavaPsiFacade.getInstance(manager.getProject()).getElementFactory();
    CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(project);

    statements = SurroundWithUtil.moveDeclarationsOut(container, statements, false);
    if (statements.length == 0){
      return null;
    }

    @NonNls String text = "try{\n}finally{\n\n}";
    PsiTryStatement tryStatement = (PsiTryStatement)factory.createStatementFromText(text, null);
    tryStatement = (PsiTryStatement)codeStyleManager.reformat(tryStatement);

    tryStatement = (PsiTryStatement)container.addAfter(tryStatement, statements[statements.length - 1]);

    PsiCodeBlock tryBlock = tryStatement.getTryBlock();
    if (tryBlock == null) {
      return null;
    }
    SurroundWithUtil.indentCommentIfNecessary(tryBlock, statements);
    tryBlock.addRange(statements[0], statements[statements.length - 1]);
    container.deleteChildRange(statements[0], statements[statements.length - 1]);

    PsiCodeBlock finallyBlock = tryStatement.getFinallyBlock();
    if (finallyBlock == null) {
      return null;
    }
    int offset = finallyBlock.getTextRange().getStartOffset() + 2;
    editor.getCaretModel().moveToOffset(offset);
    final Document document = editor.getDocument();
    PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document);
    editor.getSelectionModel().removeSelection();
    final PsiStatement firstTryStmt = tryBlock.getStatements()[0];
    final int indent = firstTryStmt.getTextOffset() - document.getLineStartOffset(document.getLineNumber(firstTryStmt.getTextOffset()));
    EditorModificationUtil.insertStringAtCaret(editor, StringUtil.repeat(" ", indent), false, true);
    return new TextRange(editor.getCaretModel().getOffset(), editor.getCaretModel().getOffset());
  }
}