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
package generate.tostring.psi;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.IncorrectOperationException;
import generate.tostring.util.StringUtil;

/**
 * Basic PSI Adapter with common function that works in all supported versions of IDEA.
 */
public abstract class PsiAdapter {

    /**
     * Constructor - use {@link PsiAdapterFactory}.
     */
    protected PsiAdapter() {
    }

    /**
     * Get's the PSIManager.
     *
     * @param project IDEA project.
     * @return the PSIManager.
     */
    public PsiManager getPsiManager(Project project) {
        return PsiManager.getInstance(project);
    }

    /**
     * Get's the fields for the class.
     *
     * @param clazz class.
     * @return the fields for the class. If the class doesn't have any fields the array's size is 0.
     */
    public PsiField[] getFields(PsiClass clazz) {
        return clazz.getFields();
    }

    /**
     * Get's the PSIElementFactory.
     * <p/>The factory can be used to create methods.
     *
     * @param manager PSIManager.
     * @return the factory.
     */
    public PsiElementFactory getPsiElemetFactory(PsiManager manager) {
        return manager.getElementFactory();
    }

    /**
     * Get's the current class for the javafile and relative to the editor's caret.
     * It's possible to get an inner class if the caret is positioned inside the innerclass.
     *
     * @param javaFile javafile for the class to find.
     * @param editor   the editor.
     * @return the class, null if not found.
     */
    public PsiClass getCurrentClass(PsiJavaFile javaFile, Editor editor) {
        if (javaFile == null) {
            return null;
        }
        PsiElement element = javaFile.findElementAt(editor.getCaretModel().getOffset());
        return element != null ? findClass(element) : null;
    }

    /**
     * Finds the class for the given element.
     * <p/>
     * Will look in the element's parent hieracy.
     *
     * @param element element to find it's class
     * @return the class, null if not found.
     */
    public PsiClass findClass(PsiElement element) {
        if (element instanceof PsiClass) {
            return (PsiClass) element;
        }

        if (element.getParent() != null) {
            return findClass(element.getParent());
        }

        return null;
    }

    /**
     * Returns true if a field is constant.
     * <p/>
     * This is identifed as the name of the field is only in uppercase and it has
     * a <code>static</code> modifier.
     *
     * @param field field to check if it's a constant
     * @return true if constant.
     */
    public boolean isConstantField(PsiField field) {
        PsiModifierList list = field.getModifierList();
        if (list == null) {
            return false;
        }

        // modifier must be static
        if (!list.hasModifierProperty(PsiModifier.STATIC)) {
            return false;
        }

        // name must NOT have any lowercase character
        return !StringUtil.hasLowerCaseChar(field.getName());
    }

    /**
     * Find's an existing method with the given name.
     * If there isn't a method with the name, null is returned.
     *
     * @param clazz the class
     * @param name  name of method to find
     * @return the found method, null if none exist
     */
    public PsiMethod findMethodByName(PsiClass clazz, String name) {
        PsiMethod[] methods = clazz.getMethods();

        // use reverse to find from botton as the duplicate conflict resolution policy requires this
        for (int i = methods.length - 1; i >= 0; i--) {
            PsiMethod method = methods[i];
            if (name.equals(method.getName()))
                return method;
        }
        return null;
    }

    /**
     * Returns true if the given field a primtive array type (e.g., int[], long[], float[]).
     *
     * @param type type.
     * @return true if field is a primitve array type.
     */
    public boolean isPrimitiveArrayType(PsiType type) {
        return isPrimitiveType(type) && isArray(type);
    }

    /**
     * Is the type an Object array type (etc. String[], Object[])?
     *
     * @param type type.
     * @return true if it's an Object array type.
     */
    public boolean isObjectArrayType(PsiType type) {
        if (isPrimitiveType(type))
            return false;

        return type.getCanonicalText().indexOf("[]") > 0;
    }

    /**
     * Is the type a String array type (etc. String[])?
     *
     * @param type type.
     * @return true if it's a String array type.
     */
    public boolean isStringArrayType(PsiType type) {
        if (isPrimitiveType(type))
            return false;

        return type.getCanonicalText().indexOf("String[]") > 0;
    }

    /**
     * Is the given field a {@link java.util.Collection} type?
     *
     * @param factory element factory.
     * @param type    type.
     * @return true if it's a Collection type.
     */
    public boolean isCollectionType(PsiElementFactory factory, PsiType type) {
        return isTypeOf(factory, type, "java.util.Collection");
    }

    /**
     * Is the given field a {@link java.util.Map} type?
     *
     * @param factory element factory.
     * @param type    type.
     * @return true if it's a Map type.
     */
    public boolean isMapType(PsiElementFactory factory, PsiType type) {
        return isTypeOf(factory, type, "java.util.Map");
    }

    /**
     * Is the given field a {@link java.util.Set} type?
     *
     * @param factory element factory.
     * @param type    type.
     * @return true if it's a Map type.
     */
    public boolean isSetType(PsiElementFactory factory, PsiType type) {
        return isTypeOf(factory, type, "java.util.Set");
    }

    /**
     * Is the given field a {@link java.util.List} type?
     *
     * @param factory element factory.
     * @param type    type.
     * @return true if it's a Map type.
     */
    public boolean isListType(PsiElementFactory factory, PsiType type) {
        return isTypeOf(factory, type, "java.util.List");
    }

    /**
     * Is the given field a {@link java.lang.String} type?
     *
     * @param factory element factory.
     * @param type    type.
     * @return true if it's a String type.
     */
    public boolean isStringType(PsiElementFactory factory, PsiType type) {
        return isTypeOf(factory, type, "java.lang.String");
    }

    /**
     * Is the given field assignable from {@link java.lang.Object}?
     *
     * @param factory element factory.
     * @param type    type.
     * @return true if it's an Object type.
     */
    public boolean isObjectType(PsiElementFactory factory, PsiType type) {
        return isTypeOf(factory, type, "java.lang.Object");
    }

    /**
     * Is the given field a {@link java.util.Date} type?
     *
     * @param factory element factory.
     * @param type    type.
     * @return true if it's a Date type.
     */
    public boolean isDateType(PsiElementFactory factory, PsiType type) {
        return isTypeOf(factory, type, "java.util.Date");
    }

    /**
     * Is the given field a {@link java.util.Calendar} type?
     *
     * @param factory element factory.
     * @param type    type.
     * @return true if it's a Calendar type.
     */
    public boolean isCalendarType(PsiElementFactory factory, PsiType type) {
        return isTypeOf(factory, type, "java.util.Calendar");
    }

    /**
     * Is the given field a {@link java.lang.Boolean} type or a primitive boolean type?
     *
     * @param factory element factory.
     * @param type    type.
     * @return true if it's a Boolean or boolean type.
     */
    public boolean isBooleanType(PsiElementFactory factory, PsiType type) {
        if (isPrimitiveType(type)) {
            // test for simple type of boolean
            String s = type.getCanonicalText();
            return "boolean".equals(s);
        } else {
            // test for Object type of Boolean
            return isTypeOf(factory, type, "java.lang.Boolean");
        }
    }

    /**
     * Is the given field a numeric type (assignable from java.lang.Numeric or a primitive type of byte, short, int, long, float, double type)?
     *
     * @param factory element factory.
     * @param type    type.
     * @return true if it's a numeric type.
     */
    public boolean isNumericType(PsiElementFactory factory, PsiType type) {
        if (isPrimitiveType(type)) {
            // test for simple type of numeric
            String s = type.getCanonicalText();
            return "byte".equals(s) || "double".equals(s) || "float".equals(s) || "int".equals(s) || "long".equals(s) || "short".equals(s);
        } else {
            // test for Object type of numeric
            return isTypeOf(factory, type, "java.lang.Number");
        }
    }

    /**
     * Is the given type an array? (using [] in its name)
     *
     * @param type the type.
     * @return true if it is an array, false if not.
     */
    private boolean isArray(PsiType type) {
        return type.getCanonicalText().indexOf("[]") > 0;
    }

    /**
     * Is there a <code>transient</code> modifier?
     * <p/>eg: <code>private transient String myField;</code>.
     *
     * @param modifiers the modifiers
     */
    public boolean isModifierTransient(PsiModifierList modifiers) {
        return modifiers.hasModifierProperty(PsiModifier.TRANSIENT);
    }

    /**
     * Is there a <code>volatile</code> modifier?
     * <p/>eg: <code>private volatile Image screen;</code>.
     *
     * @param modifiers the modifiers
     */
    public boolean isModifierVolatile(PsiModifierList modifiers) {
        return modifiers.hasModifierProperty(PsiModifier.VOLATILE);
    }

    /**
     * Is there a <code>public</code> modifier?
     * <p/>eg: <code>public String myField;</code>.
     *
     * @param modifiers the modifiers
     */
    public boolean isModifierPublic(PsiModifierList modifiers) {
        return modifiers.hasModifierProperty(PsiModifier.PUBLIC);
    }

    /**
     * Is there a <code>protected</code> modifier?
     * <p/>eg: <code>public String myField;</code>.
     *
     * @param modifiers the modifiers
     */
    public boolean isModifierProtected(PsiModifierList modifiers) {
        return modifiers.hasModifierProperty(PsiModifier.PROTECTED);
    }

    /**
     * Is there a <code>package-local</code> modifier?
     * <p/>eg: <code>String myField;</code>.
     *
     * @param modifiers the modifiers
     */
    public boolean isModifierPackageLocal(PsiModifierList modifiers) {
        return modifiers.hasModifierProperty(PsiModifier.PACKAGE_LOCAL);
    }

    /**
     * Is there a <code>private</code> modifier?
     * <p/>eg: <code>private static String myField;</code>.
     *
     * @param modifiers the modifiers
     */
    public boolean isModifierPrivate(PsiModifierList modifiers) {
        return modifiers.hasModifierProperty(PsiModifier.PRIVATE);
    }

    /**
     * Is there a <code>abstract</code> modifier?
     * <p/>eg: <code>public abstract String getConfiguration()</code>.
     *
     * @param modifiers the modifiers
     */
    public boolean isModifierAbstract(PsiModifierList modifiers) {
        return modifiers.hasModifierProperty(PsiModifier.ABSTRACT);
    }

    /**
     * Is there a <code>final</code> modifier?
     * <p/>eg: <code>final static boolean DEBUG = false;</code>.
     *
     * @param modifiers the modifiers
     */
    public boolean isModifierFinal(PsiModifierList modifiers) {
        return modifiers.hasModifierProperty(PsiModifier.FINAL);
    }

    /**
     * Is there a <code>static</code> modifier?
     * <p/>eg: <code>private static String getMyField()</code>.
     *
     * @param modifiers the modifiers
     */
    public boolean isModifierStatic(PsiModifierList modifiers) {
        return modifiers.hasModifierProperty(PsiModifier.STATIC);
    }

    /**
     * Is there a <code>synchronized</code> modifier?
     * <p/>eg: <code>public synchronized void putInCache()</code>.
     *
     * @param modifiers the modifiers
     */
    public boolean isModifierSynchronized(PsiModifierList modifiers) {
        return modifiers.hasModifierProperty(PsiModifier.SYNCHRONIZED);
    }

    /**
     * Get's the CodeStyleManager for the project.
     *
     * @param project project.
     * @return the CodeStyleManager.
     */
    public CodeStyleManager getCodeStyleManager(Project project) {
        return CodeStyleManager.getInstance(project);
    }

    /**
     * Does the class have any fields?
     *
     * @param clazz the PsiClass.
     * @return true if the class has fields.
     */
    public boolean hasFields(PsiClass clazz) {
        return clazz.getFields().length > 0;
    }

    /**
     * Does the javafile have the import statement?
     *
     * @param javaFile        javafile.
     * @param importStatement import statement to test existing for.
     * @return true if the javafile has the import statement.
     */
    public boolean hasImportStatement(PsiJavaFile javaFile, String importStatement) {
        PsiImportList importList = javaFile.getImportList();
        if (importList == null) {
            return false;
        }

        if (importStatement.endsWith(".*")) {
            return (importList.findOnDemandImportStatement(fixImportStatement(importStatement)) != null);
        } else {
            return (importList.findSingleClassImportStatement(importStatement) != null);
        }
    }

    /**
     * Add's an importstatement to the javafile and optimizes the imports afterwards.
     *
     * @param javaFile                javafile.
     * @param importStatementOnDemand name of importstatement, must be with a wildcard (etc. java.util.*).
     * @param factory                 PSI element factory.
     * @param codeStyleManager        PSI codestyle to optimize the imports.
     * @throws com.intellij.util.IncorrectOperationException
     *          is thrown if there is an error creating the importstatement.
     */
    public void addImportStatement(PsiJavaFile javaFile, String importStatementOnDemand, PsiElementFactory factory, CodeStyleManager codeStyleManager) throws IncorrectOperationException {
        PsiImportStatement is = factory.createImportStatementOnDemand(fixImportStatement(importStatementOnDemand));

        // add the import to the file, and optimize the imports
        PsiImportList importList = javaFile.getImportList();
        if (importList != null) {
            importList.add(is);
        }

        codeStyleManager.optimizeImports(javaFile);
    }

    /**
     * Fixes the import statement to be returned as packagename only (without .* or any Classname).
     * <p/>
     * <br/>Example: java.util will be returned as java.util
     * <br/>Example: java.util.* will be returned as java.util
     * <br/>Example: java.text.SimpleDateFormat will be returned as java.text
     *
     * @param importStatementOnDemand import statement
     * @return import statement only with packagename
     */
    private String fixImportStatement(String importStatementOnDemand) {
        if (importStatementOnDemand.endsWith(".*")) {
            return importStatementOnDemand.substring(0, importStatementOnDemand.length() - 2);
        } else {
            boolean hasClassname = StringUtil.hasUpperCaseChar(importStatementOnDemand);

            if (hasClassname) {
                // extract packagename part
                int pos = importStatementOnDemand.lastIndexOf(".");
                return importStatementOnDemand.substring(0, pos);
            } else {
                // it is a pure packagename
                return importStatementOnDemand;
            }
        }
    }

    /**
     * Does this class have a super class?
     * <p/>
     * If the class just extends java.lang.Object then false is returned.
     * Extending java.lang.Object is <b>not</b> concidered the class to have a super class.
     *
     * @param project the IDEA project
     * @param clazz   the class to test
     * @return true if this class extends another class.
     */
    public boolean hasSuperClass(Project project, PsiClass clazz) {
        PsiClass superClass = getSuperClass(project, clazz);
        if (superClass == null) {
            return false;
        }

        return (!"Object".equals(superClass.getName()));
    }

    /**
     * Optimizes the imports on the given java file.
     *
     * @param project  the PSI project
     * @param javaFile the java file
     * @throws IncorrectOperationException error optimizing imports.
     */
    public void optimizeImports(Project project, PsiJavaFile javaFile) throws IncorrectOperationException {
        CodeStyleManager csm = getCodeStyleManager(project);
        csm.optimizeImports(javaFile);
    }

    /**
     * Get's the fields fully qualified classname (etc java.lang.String, java.util.ArrayList)
     *
     * @param type the type.
     * @return the fully qualified classname, null if the field is a primitive.
     * @see #getTypeClassName(com.intellij.psi.PsiType) for the non qualified version.
     */
    public String getTypeQualifiedClassName(PsiType type) {
        // special for primitives
        if (isPrimitiveType(type)) {
            return getPrimitiveQualifiedTypeName(type);
        }

        String name = type.getCanonicalText();
        if (name.endsWith("[]")) {
            // avoid [] if the type is an array
            return name.substring(0, name.length() - 2);
        }

        if (isSimpleGenericType(type)) {
            // handle simple generics
            return StringUtil.middle(name, "<", ">");
        }

        return name;
    }

    /**
     * Is it a simple generics type (contain only one < and > in the qualified name)
     * @param type the type.
     * @return
     */
    public boolean isSimpleGenericType(PsiType type) {
        String name = type.getCanonicalText();
        return (StringUtil.countTokens(name, '<') == 1 && StringUtil.countTokens(name, '>') == 1);
    }

    /**
     * Get's the fields classname (etc. String, ArrayList)
     *
     * @param type the type.
     * @return the classname, null if the field is a primitive.
     * @see #getTypeQualifiedClassName(com.intellij.psi.PsiType) for the qualified version.
     */
    public String getTypeClassName(PsiType type) {
        String name = getTypeQualifiedClassName(type);

        // return null if it was a primitive type
        if (name == null) {
            return null;
        }

        int i = name.lastIndexOf('.');
        return name.substring(i + 1, name.length());
    }


    /**
     * Removes the action from the menu.
     * <p/>
     * The group must be of a DefaultActionGroup instance, if not this method returns false.
     *
     * @param actionId group id of the action to remove.
     * @param menuId   id of the menu that contains the action. See ActionManager.xml in the IDEA openapi folder.
     * @return true if the action was remove, false if not (action could not be found)
     */
    public boolean removeActionFromMenu(String actionId, String menuId) {
        ActionManager am = ActionManager.getInstance();
        AnAction group = am.getAction(menuId);

        // must be default action group
        if (group instanceof DefaultActionGroup) {
            DefaultActionGroup defGroup = (DefaultActionGroup) group;

            // loop children (actions) and remove the matching id
            AnAction[] actions = defGroup.getChildren(null);
            for (AnAction action : actions) {
                String id = am.getId(action);

                // if match id then remove action from menu
                if (actionId.equals(id)) {
                    defGroup.remove(action);
                    return true;
                }

            }

        }

        // action to remove not found
        return false;
    }

    /**
     * Adds the action to the menu.
     * <p/>
     * The group must be of a DefaultActionGroup instance, if not this method returns false.
     *
     * @param actionId    group id of the action to add.
     * @param menuId      id of the menu the action should be added to. See ActionManager.xml in the IDEA openapi folder.
     * @param anchorId    id of the action to position relative to.  See ActionManager.xml in the IDEA openapi folder.
     * @param afterAnchor true if the action should be added after the anchorId, false if before.
     * @return true if the action was added, false if not.
     */
    public boolean addActionToMenu(String actionId, String menuId, String anchorId, boolean afterAnchor) {
        ActionManager am = ActionManager.getInstance();
        AnAction group = am.getAction(menuId);

        // must be default action group
        if (group instanceof DefaultActionGroup) {
            DefaultActionGroup defGroup = (DefaultActionGroup) group;

            // loop children (actions) and remove the matching id
            AnAction[] actions = defGroup.getChildren(null);
            for (AnAction action : actions) {
                String id = am.getId(action);

                // if match id then add action to menu
                if (anchorId.equals(id)) {
                    AnAction actionToAdd = am.getAction(actionId); // find the action to add
                    defGroup.add(actionToAdd, new Constraints(afterAnchor ? Anchor.AFTER : Anchor.BEFORE, anchorId));
                    return true;
                }

            }

        }

        // action to add next to not found
        return false;
    }

    /**
     * Finds the public static void main(String[] args) method.
     *
     * @param clazz the class.
     * @return the method if it exists, null if not.
     */
    public PsiMethod findPublicStaticVoidMainMethod(PsiClass clazz) {
        PsiMethod[] methods = clazz.findMethodsByName("main", false);

        // is it public static void main(String[] args)
        for (PsiMethod method : methods) {
            // must be public
            if (!method.getModifierList().hasModifierProperty("public")) {
                continue;
            }

            // must be static
            if (!method.getModifierList().hasModifierProperty("static")) {
                continue;
            }

            // must have void as return type
            PsiType returnType = method.getReturnType();
            if (returnType == null || returnType.equalsToText("void")) {
                continue;
            }

            // must have one parameter
            PsiParameter[] parameters = method.getParameterList().getParameters();
            if (parameters.length != 1) {
                continue;
            }

            // parameter must be string array
            if (!isStringArrayType(parameters[0].getType())) {
                continue;
            }

            // public static void main(String[] args) method found
            return method;
        }

        // main not found
        return null;
    }

    /**
     * Add or replaces the javadoc comment to the given method.
     *
     * @param factory          element factory.
     * @param codeStyleManager CodeStyleManager.
     * @param method           the method the javadoc should be added/set to.
     * @param javadoc          the javadoc comment.
     * @param replace          true if any existing javadoc should be replaced. false will not replace any existing javadoc and thus leave the javadoc untouched.
     * @return the added/replace javadoc comment, null if the was an existing javadoc and it should <b>not</b> be replaced.
     * @throws IncorrectOperationException is thrown if error adding/replacing the javadoc comment.
     */
    public PsiComment addOrReplaceJavadoc(PsiElementFactory factory, CodeStyleManager codeStyleManager, PsiMethod method, String javadoc, boolean replace) throws IncorrectOperationException {
        PsiComment comment = factory.createCommentFromText(javadoc, null);

        // does a method already exists?
        PsiDocComment doc = method.getDocComment();
        if (doc != null) {
            if (replace) {
                // javadoc already exists, so replace
                doc.replace(comment);
                codeStyleManager.reformat(method); // to reformat javadoc
                return comment;
            } else {
                // do not replace existing javadoc
                return null;
            }
        } else {
            // add new javadoc
            method.addBefore(comment, method.getFirstChild());
            codeStyleManager.reformat(method); // to reformat javadoc
            return comment;
        }
    }

    /**
     * Get's the methods for the class.
     *
     * @param clazz class.
     * @return the methods for the class. If the class doesn't have any methods the array's size is 0.
     */
    public PsiMethod[] getMethods(PsiClass clazz) {
        return clazz.getMethods();
    }

    /**
     * Find's an existing field with the given name.
     * If there isn't a field with the name, null is returned.
     *
     * @param clazz the class
     * @param name  name of field to find
     * @return the found field, null if none exist
     */
    public PsiField findFieldByName(PsiClass clazz, String name) {
        PsiField[] fields = clazz.getFields();

        // use reverse to find from botton as the duplicate conflict resolution policy requires this
        for (int i = fields.length - 1; i >= 0; i--) {
            PsiField field = fields[i];
            if (name.equals(field.getName()))
                return field;
        }

        return null;
    }

    /**
     * Is the given type a "void" type.
     *
     * @param type the type.
     * @return true if a void type, false if not.
     */
    public boolean isTypeOfVoid(PsiType type) {
        return (type != null && type.equalsToText("void"));
    }

    /**
     * Is the method a getter method?
     * <p/>
     * The name of the method must start with <code>get</code> or <code>is</code>.
     * And if the method is a <code>isXXX</code> then the method must return a java.lang.Boolean or boolean.
     *
     * @param factory element factory.
     * @param method  the method
     * @return true if a getter method, false if not.
     */
    public boolean isGetterMethod(PsiElementFactory factory, PsiMethod method) {
        // must not be a void method
        if (isTypeOfVoid(method.getReturnType())) {
            return false;
        }

        if (method.getName().matches("^(is|has)\\p{Upper}.*")) {
            return isBooleanType(factory, method.getReturnType());
        } else if (method.getName().matches("^(get)\\p{Upper}.*")) {
            return true;
        }

        return false;
    }

    /**
     * Get's the field name of the getter method.
     * <p/>
     * The method must be a getter method for a field.
     * Returns null if this method is not a getter.
     * <p/>
     * The fieldname is the part of the name that is after the <code>get</code> or <code>is</code> part
     * of the name.
     * <p/>
     * Example: methodName=getName will return fieldname=name
     *
     * @param factory element factory.
     * @param method  the method
     * @return the fieldname if this is a getter method.
     * @see #isGetterMethod(com.intellij.psi.PsiElementFactory,com.intellij.psi.PsiMethod) for the getter check
     */
    public String getGetterFieldName(PsiElementFactory factory, PsiMethod method) {
        // must be a getter
        if (!isGetterMethod(factory, method)) {
            return null;
        }

        // return part after get
        String getName = StringUtil.after(method.getName(), "get");
        if (getName != null) {
            getName = StringUtil.firstLetterToLowerCase(getName);
            return getName;
        }

        // return part after is
        String isName = StringUtil.after(method.getName(), "is");
        if (isName != null) {
            isName = StringUtil.firstLetterToLowerCase(isName);
            return isName;
        }

        return null;
    }

    /**
     * Get's the javadoc for the given method as a String.
     *
     * @param method the method
     * @return the javadoc, null if no javadoc.
     */
    public String getJavaDoc(PsiMethod method) {
        if (method == null) {
            return null;
        }

        PsiDocComment doc = method.getDocComment();
        if (doc != null) {
            return doc.getText();
        }

        return null;
    }

    /**
     * Returns true if the field is enum (JDK1.5).
     *
     * @param manager the psi manager
     * @param field   field to check if it's a enum
     * @return true if enum.
     */
    public boolean isEnumField(PsiManager manager, PsiField field) {
        PsiType type = field.getType();

        // must not be an primitive type
        if (isPrimitiveType(type)) {
            return false;
        }

        GlobalSearchScope scope = type.getResolveScope();
        if (scope == null) {
            return false;
        }

        // find the class
        String name = type.getCanonicalText();
        PsiClass clazz = manager.findClass(name, scope);
        if (clazz == null) {
            return false;
        }

        return isEnumClass(clazz);
    }

    /**
     * Returns true if the class is enum (JDK1.5).
     *
     * @param clazz class to check if it's a enum
     * @return true if enum.
     */
    public boolean isEnumClass(PsiClass clazz) {
        return clazz.isEnum();
    }

    /**
     * Returns true if the class is deprecated.
     *
     * @param clazz class to check if it's deprecated
     * @return true if deprecated.
     */
    public boolean isDeprecatedClass(PsiClass clazz) {
        return clazz.isDeprecated();
    }

    /**
     * Returns true if the method is deprecated.
     *
     * @param method method to check if it's deprecated
     * @return true if deprecated.
     */
    public boolean isDeprecatedMethod(PsiMethod method) {
        return method.isDeprecated();
    }

    /**
     * Is the class an exception - extends Throwable (will check super).
     *
     * @param clazz class to check.
     * @return true if class is an exception.
     */
    public boolean isExceptionClass(PsiClass clazz) {
        PsiClass[] supers = clazz.getSupers();
        for (PsiClass sup : supers) {
            if ("java.lang.Throwable".equals(sup.getQualifiedName())) {
                return true;
            } else if (isExceptionClass(sup)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Is the class an abstract class
     *
     * @param clazz class to check.
     * @return true if class is abstract.
     */
    public boolean isAbstractClass(PsiClass clazz) {
        PsiModifierList list = clazz.getModifierList();
        if (list == null) {
            return false;
        }
        return isModifierAbstract(clazz.getModifierList());
    }

    /**
     * Finds the PsiElement at the current cursor position in the javafile.
     *
     * @param javaFile javafile for the class to find.
     * @param editor   the editor.
     * @return the elemenet, null if not found or not possible to find.
     */
    public PsiElement findElementAtCursorPosition(PsiJavaFile javaFile, Editor editor) {
        if (javaFile == null) {
            return null;
        }
        return javaFile.findElementAt(editor.getCaretModel().getOffset());
    }

    /**
     * Finds the public boolean equals(Object o) method.
     *
     * @param clazz the class.
     * @return the method if it exists, null if not.
     */
    public PsiMethod findEqualsMethod(PsiClass clazz) {
        PsiMethod[] methods = clazz.findMethodsByName("equals", false);

        // is it public boolean equals(Object o)
        for (PsiMethod method : methods) {
            // must be public
            if (!method.getModifierList().hasModifierProperty("public")) {
                continue;
            }

            // must not be static
            if (method.getModifierList().hasModifierProperty("static")) {
                continue;
            }

            // must have boolean as return type
            PsiType returnType = method.getReturnType();
            if (returnType == null || !returnType.equalsToText("boolean")) {
                continue;
            }

            // must have one parameter
            PsiParameter[] parameters = method.getParameterList().getParameters();
            if (parameters.length != 1) {
                continue;
            }

            // parameter must be Object
            if (!(parameters[0].getType().getCanonicalText().equals("java.lang.Object"))) {
                continue;
            }

            // equals method found
            return method;
        }

        // equals not found
        return null;
    }

    /**
     * Finds the public int hashCode() method.
     *
     * @param clazz the class.
     * @return the method if it exists, null if not.
     */
    public PsiMethod findHashCodeMethod(PsiClass clazz) {
        PsiMethod[] methods = clazz.findMethodsByName("hashCode", false);

        // is it public int hashCode()
        for (PsiMethod method : methods) {
            // must be public
            if (!method.getModifierList().hasModifierProperty("public")) {
                continue;
            }

            // must not be static
            if (method.getModifierList().hasModifierProperty("static")) {
                continue;
            }

            // must have int as return type
            PsiType returnType = method.getReturnType();
            if (returnType == null || !returnType.equalsToText("int")) {
                continue;
            }

            // must not have a parameter
            PsiParameter[] parameters = method.getParameterList().getParameters();
            if (parameters.length != 0) {
                continue;
            }

            // hashCode method found
            return method;
        }

        // hashCode not found
        return null;
    }

    /**
     * Adds/replaces the given annotation text to the method.
     *
     * @param factory    element factory.
     * @param method     the method the javadoc should be added/set to.
     * @param annotation the annotation as text.
     * @return the added annotation object
     * @throws IncorrectOperationException is thrown if error adding/replacing the javadoc comment.
     */
    public PsiAnnotation addAnnotationToMethod(PsiElementFactory factory, PsiMethod method, String annotation) throws IncorrectOperationException {
        PsiAnnotation ann = method.getModifierList().findAnnotation(annotation);
        if (ann == null) {
            // add new annotation
            ann = factory.createAnnotationFromText(annotation, method.getModifierList());
            PsiModifierList modifierList = method.getModifierList();
            modifierList.addBefore(ann, modifierList.getFirstChild());
        } else {
            PsiModifierList modifierList = method.getModifierList();
            modifierList.replace(ann); // already exist so replace
        }

        return ann;
    }

    /**
     * Get's the selected java file in the editor.
     *
     * @param project IDEA project
     * @param manager IDEA manager
     * @return the selected java file, null if none found.
     */
    public PsiJavaFile getSelectedJavaFile(Project project, PsiManager manager) {
        Editor editor = getSelectedEditor(project);
        if (editor == null) {
            return null;
        }

        VirtualFile vFile = FileDocumentManager.getInstance().getFile(editor.getDocument());
        PsiFile psiFile = manager.findFile(vFile);

        // we only want it if its a java file
        if (!(psiFile instanceof PsiJavaFile)) {
            return null;
        } else {
            return (PsiJavaFile) psiFile;
        }
    }

    /**
     * Get's the current selected text editor.
     *
     * @param project IDEA project
     * @return the selected editor, null if none selected
     */
    public Editor getSelectedEditor(Project project) {
        return FileEditorManager.getInstance(project).getSelectedTextEditor();
    }

    /**
     * Check if the given type against a FQ classname (assignable).
     *
     * @param factory         IDEA factory
     * @param type            the type
     * @param typeFQClassName the FQ classname to test against.
     * @return true if the given type is assigneable of FQ classname.
     */
    protected boolean isTypeOf(PsiElementFactory factory, PsiType type, String typeFQClassName) {
        // fix for IDEA where fields can have 'void' type and generate NPE.
        if (isTypeOfVoid(type)) {
            return false;
        }

        if (isPrimitiveType(type)) {
            return false;
        }

        GlobalSearchScope scope = type.getResolveScope();
        if (scope == null) {
            return false;
        }
        PsiType typeTarget = factory.createTypeByFQClassName(typeFQClassName, scope);
        return typeTarget.isAssignableFrom(type);
    }

    /**
     * Get's the superclass.
     *
     * @param project IDEA project
     * @param clazz   the class
     * @return the super, null if not found.
     */
    public PsiClass getSuperClass(Project project, PsiClass clazz) {
        PsiReferenceList list = clazz.getExtendsList();

        // check if no superclass at all
        if (list == null || list.getReferencedTypes().length != 1) {
            return null;
        }

        // have superclass get it [0] is the index of the superclass (a class can not extend more than one class)
        GlobalSearchScope scope = list.getReferencedTypes()[0].getResolveScope();
        String classname = list.getReferencedTypes()[0].getCanonicalText();

        PsiManager manager = getPsiManager(project);
        return manager.findClass(classname, scope);
    }

    /**
     * Get's the names the given class implements (not FQ names).
     *
     * @param clazz the class
     * @return the names.
     */
    public String[] getImplementsClassnames(PsiClass clazz) {
        PsiClass[] interfaces = clazz.getInterfaces();

        if (interfaces == null || interfaces.length == 0) {
            return new String[0];
        }

        String[] names = new String[interfaces.length];
        for (int i = 0; i < interfaces.length; i++) {
            PsiClass anInterface = interfaces[i];
            names[i] = anInterface.getName();
        }

        return names;
    }

    /**
     * Is the given type a primitive?
     *
     * @param type the type.
     * @return true if primitive, false if not.
     */
    public boolean isPrimitiveType(PsiType type) {
        // shortcut - skip java.* and javax.* packages
        if (type.getCanonicalText().startsWith("java")) {
            return false;
        }

        if (type.isAssignableFrom(PsiType.BOOLEAN) ||
                type.isAssignableFrom(PsiType.BYTE) ||
                type.isAssignableFrom(PsiType.CHAR) ||
                type.isAssignableFrom(PsiType.DOUBLE) ||
                type.isAssignableFrom(PsiType.FLOAT) ||
                type.isAssignableFrom(PsiType.INT) ||
                type.isAssignableFrom(PsiType.LONG) ||
                type.isAssignableFrom(PsiType.SHORT)) {
            return true;
        }

        PsiType subType = type.getDeepComponentType();
        if (subType.isAssignableFrom(PsiType.BOOLEAN) ||
                subType.isAssignableFrom(PsiType.BYTE) ||
                subType.isAssignableFrom(PsiType.CHAR) ||
                subType.isAssignableFrom(PsiType.DOUBLE) ||
                subType.isAssignableFrom(PsiType.FLOAT) ||
                subType.isAssignableFrom(PsiType.INT) ||
                subType.isAssignableFrom(PsiType.LONG) ||
                subType.isAssignableFrom(PsiType.SHORT)) {
            return true;
        }

        return false;
    }

    /**
     * Gets the qualified type name of the primitive type (eg: java.lang.Integer)
     *
     * @param type  the type.
     * @return the qualified type name, <tt>null</tt> if not a primitive.
     */
    public String getPrimitiveQualifiedTypeName(PsiType type) {
        if (! isPrimitiveType(type)) {
            return null;
        }

        if (type.isAssignableFrom(PsiType.BOOLEAN)) {
            return "java.lang.Boolean";
        } else if (type.isAssignableFrom(PsiType.BYTE)) {
            return "java.lang.Byte";
        } else if (type.isAssignableFrom(PsiType.CHAR)) {
            return "java.lang.Character";
        } else if (type.isAssignableFrom(PsiType.DOUBLE)) {
            return "java.lang.Double";
        } else if (type.isAssignableFrom(PsiType.FLOAT)) {
            return "java.lang.Float";
        } else if (type.isAssignableFrom(PsiType.INT)) {
            return "java.lang.Integer";
        } else if (type.isAssignableFrom(PsiType.LONG)) {
            return "java.lang.Long";
        } else if (type.isAssignableFrom(PsiType.SHORT)) {
            return "java.lang.Short";
        }

        return null;
    }

    /**
     * Executes the given runable in IDEA command.
     *
     * @param project IDEA project
     * @param runable the runable task to exexute.
     */
    public void executeCommand(Project project, Runnable runable) {
        CommandProcessor.getInstance().executeCommand(project, runable, "GenerateToString", null);
    }

    /**
     * Add's the interface name to the class implementation list.
     *
     * @param project       IDEA project
     * @param clazz         the class
     * @param interfaceName the interface name the class should implement
     * @throws IncorrectOperationException is thrown by IDEA.
     */
    public void addImplements(Project project, PsiClass clazz, String interfaceName) throws IncorrectOperationException {
        PsiManager manager = getPsiManager(project);
        PsiElementFactory factory = getPsiElemetFactory(manager);

        // get the interface class
        PsiClass interfaceClass = manager.findClass(interfaceName, GlobalSearchScope.allScope(project));

        // if the interface exists add it as a reference in the implements list
        if (interfaceClass != null) {
            PsiJavaCodeReferenceElement ref = factory.createClassReferenceElement(interfaceClass);
            PsiReferenceList list = clazz.getImplementsList();
            if (list != null) {
                list.add(ref);
            }
        }
    }

    /**
     * Looks for a PsiMethod within the elements parent.
     *
     * @param elem the element
     * @return the parent method if found, <tt>null</tt> if not found.
     */
    public static PsiMethod findParentMethod(PsiElement elem) {
        if (elem == null) {
            return null;
        } else if (elem instanceof PsiMethod) {
            return (PsiMethod) elem;
        }
        return findParentMethod(elem.getParent());
    }

    /**
     * Get's the full filename to this plugin .jar file
     *
     * @return the full filename to this plugin .jar file
     */
    public abstract String getPluginFilename();

}
