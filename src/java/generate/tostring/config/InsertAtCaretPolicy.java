/*
 * Copyright 2001-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package generate.tostring.config;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.util.IncorrectOperationException;
import generate.tostring.GenerateToStringContext;
import generate.tostring.psi.PsiAdapter;

/**
 * Inserts the method at the caret position.
 */
public class InsertAtCaretPolicy implements InsertNewMethodPolicy {

    private static final InsertAtCaretPolicy instance = new InsertAtCaretPolicy();

    private InsertAtCaretPolicy() {
    }

    public static InsertAtCaretPolicy getInstance() {
        return instance;
    }

    public boolean insertNewMethod(PsiClass clazz, PsiMethod newMethod) throws IncorrectOperationException {
        Project project = GenerateToStringContext.getProject();
        PsiAdapter psi = GenerateToStringContext.getPsi();
        PsiJavaFile javaFile = psi.getSelectedJavaFile(project, psi.getPsiManager(project));
        Editor editor = psi.getSelectedEditor(project);

        // find the element the cursor is postion on
        PsiElement cur = psi.findElementAtCursorPosition(javaFile, editor);

        // find better spot to insert, since cur can be anywhere
        PsiElement spot = findBestSpotToInsert(cur);
        if (spot != null) {
            clazz.addAfter(newMethod, spot);
        } else {
            // could not find a good spot so the cursor is in a strage position
            // ID 10 and ID 12: insert inside clazz even if cursor is outside the right and left brace of the class
            if (beforeRightBrace(cur, clazz)) {
                clazz.addAfter(newMethod, clazz.getLBrace());
            } else {
                clazz.addBefore(newMethod, clazz.getRBrace());
            }
        }

        return true;
    }

    private static PsiElement findBestSpotToInsert(PsiElement elem) {
        // we can insert after whitespace, method or a member
        if (elem instanceof PsiWhiteSpace) {
            // parent must not be a method, then we are at whitespace within a method and therefore want to insert after the method
            PsiMethod method = PsiAdapter.findParentMethod(elem);
            return method == null ? elem : method;
        } else if (elem instanceof PsiMethod) {
            // a method is fine
            return elem;
        } else if (elem instanceof PsiMember) {
            // okay only problem is that we can't insert at class position and PsiClass is a subclass for PsiMember
            if (!(elem instanceof PsiClass)) {
                return elem;
            }
        }

        // we reached to far up in the top and can't find a good spot to insert
        if (elem instanceof PsiJavaFile) {
            return null;
        }

        // search up for a good spot
        PsiElement parent = elem.getParent();
        if (parent != null) {
            return findBestSpotToInsert(parent);
        } else {
            return null;
        }
    }

    private static boolean beforeRightBrace(PsiElement elem, PsiClass clazz) {
        if (clazz == null || clazz.getRBrace() == null) {
            return true; // if no brace assume yes
        }

        return elem.getTextOffset() < clazz.getRBrace().getTextOffset();
    }

    public String toString() {
        return "At caret";
    }

}
