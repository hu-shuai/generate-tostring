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
package generate.tostring.inspection;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import generate.tostring.GenerateToStringContext;
import generate.tostring.GenerateToStringUtils;
import generate.tostring.psi.PsiAdapter;
import generate.tostring.psi.PsiAdapterFactory;
import generate.tostring.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Intention to check if the current class overwrites the toString() method.
 * <p/>
 * This inspection will use filter information from the settings to exclude certain fields (eg. constants etc.).
 * <p/>
 * This inspection will only perform inspection if the class have fields to be dumped but
 * does not have a toString method.
 */
public class ClassHasNoToStringMethodInspection extends AbstractToStringInsepction {

    private AbstractGenerateToStringQuickFix fix = new ClassHasNoToStringQuickFix();

    /** User options for classes to exclude. Must be a regexp pattern */
    public String excludeClassNames = "";  // must be public for JDOMSerialization
    /** User options for excluded exception classes */
    public boolean excludeException = true; // must be public for JDOMSerialization
    /** User options for excluded deprecated classes */
    public boolean excludeDeprecated = true; // must be public for JDOMSerialization
    /** User options for excluded enum classes */
    public boolean excludeEnum = false; // must be public for JDOMSerialization
    /** User options for excluded abstract classes */
    public boolean excludeAbstract = false; // must be public for JDOMSerialization

    @NotNull
    public String getDisplayName() {
        return "Class does not overwrite toString() method";
    }

    @NotNull
    public String getShortName() {
        return "ClassHasNoToStringMethod";
    }

    public ProblemDescriptor[] checkClass(PsiClass clazz, InspectionManager im, boolean onTheFly) {
        if (log.isDebugEnabled()) log.debug("checkClass: clazz=" + clazz + ", onTheFly=" + onTheFly + ", onTheFlyEnabled=" + onTheFlyEnabled());

        // must be enabled to do check on the fly
        if (onTheFly && ! onTheFlyEnabled())
            return null;

        // must be a class
        if (clazz == null || clazz.getName() == null)
            return null;

        PsiAdapter psi = PsiAdapterFactory.getPsiAdapter();
        List<ProblemDescriptor> problems = new ArrayList<ProblemDescriptor>();

        // must not be an exception
        if (excludeException && psi.isExceptionClass(clazz)) {
            log.debug("This class is an exception");
            return null;
        }

        // must not be deprecated
        if (excludeDeprecated && psi.isDeprecatedClass(clazz)) {
            log.debug("Class is deprecated");
            return null;
        }

        // must not be enum
        if (excludeEnum && psi.isEnumClass(clazz)) {
            log.debug("Class is an enum");
            return null;
        }

        if (excludeAbstract && psi.isAbstractClass(clazz)) {
            log.debug("Class is abstract");
            return null;
        }

        // if it is an excluded class - then skip
        if (StringUtil.isNotEmpty(excludeClassNames)) {
            String name = clazz.getName();
            if (name != null && name.matches(excludeClassNames)) {
                log.debug("This class is excluded");
                return null;
            }
        }

        // must have fields
        PsiField[] fields = psi.getFields(clazz);
        if (fields.length == 0) {
            log.debug("Class does not have any fields");
            return null;
        }

        // get list of fields and getter methods supposed to be dumped in the toString method
        Project project = im.getProject();
        PsiManager manager = psi.getPsiManager(project);
        PsiElementFactory elementFactory = psi.getPsiElemetFactory(manager);
        fields = GenerateToStringUtils.filterAvailableFields(project, psi, elementFactory, clazz, GenerateToStringContext.getConfig().getFilterPattern());
        PsiMethod[] methods = null;
        if (GenerateToStringContext.getConfig().isEnableMethods()) {
            // okay 'getters in code generation' is enabled so check
            methods = GenerateToStringUtils.filterAvailableMethods(psi, elementFactory, clazz, GenerateToStringContext.getConfig().getFilterPattern());
        }

        // there should be any fields
        if (fields == null && methods == null)
            return null;
        else if (Math.max( fields == null ? 0 : fields.length, methods == null ? 0 : methods.length) == 0)
            return null;

        // okay some fields/getter methods are supposed to dumped, does a toString method exist
        PsiMethod toStringMethod = psi.findMethodByName(clazz, "toString");
        if (toStringMethod == null) {
            // a toString() method is missing
            if (log.isDebugEnabled()) log.debug("Class does not overwrite toString() method: " + clazz.getQualifiedName());
            ProblemDescriptor problem = im.createProblemDescriptor(clazz.getNameIdentifier(), "Class '" + clazz.getName() + "' does not overwrite toString() method", fix, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
            problems.add(problem);
        }

        // any problems?
        if (problems.size() > 0) {
            if (log.isDebugEnabled()) log.debug("Number of problems found: " + problems.size());
            return problems.toArray(new ProblemDescriptor[problems.size()]);
        } else {
            log.debug("No problems found");
            return null; // no problems
        }
    }

    /**
     * Creates the options panel in the settings for user changeable options.
     *
     * @return the options panel
     */
    public JComponent createOptionsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1.0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Exclude classes (reg exp):"), constraints);

        final JTextField excludeClassNamesField = new JTextField(excludeClassNames, 40);
        excludeClassNamesField.setMinimumSize(new Dimension(140, 20));
        Document document = excludeClassNamesField.getDocument();
        document.addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                textChanged();
            }

            public void insertUpdate(DocumentEvent e) {
                textChanged();
            }

            public void removeUpdate(DocumentEvent e) {
                textChanged();
            }

            private void textChanged() {
                excludeClassNames = excludeClassNamesField.getText();
            }
        });
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.weightx = 1.0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(excludeClassNamesField, constraints);

        final JCheckBox excludeExceptionCheckBox = new JCheckBox("Exclude exception classes", excludeException);
        final ButtonModel bmException = excludeExceptionCheckBox.getModel();
        bmException.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent changeEvent) {
                excludeException = bmException.isSelected();
            }
        });
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(excludeExceptionCheckBox, constraints);

        final JCheckBox excludeDeprectedCheckBox = new JCheckBox("Exclude deprecated classes", excludeDeprecated);
        final ButtonModel bmDeprecated = excludeDeprectedCheckBox.getModel();
        bmDeprecated.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent changeEvent) {
                excludeDeprecated = bmDeprecated.isSelected();
            }
        });
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(excludeDeprectedCheckBox, constraints);

        final JCheckBox excludeEnumCheckBox = new JCheckBox("Exclude enum classes", excludeEnum);
        final ButtonModel bmEnum = excludeEnumCheckBox.getModel();
        bmEnum.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent changeEvent) {
                excludeEnum = bmEnum.isSelected();
            }
        });
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(excludeEnumCheckBox, constraints);

        final JCheckBox excludeAbstractCheckBox = new JCheckBox("Exclude abstract classes", excludeAbstract);
        final ButtonModel bmAbstract = excludeAbstractCheckBox.getModel();
        bmAbstract.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent changeEvent) {
                excludeAbstract = bmAbstract.isSelected();
            }
        });
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(excludeAbstractCheckBox, constraints);

        return panel;
    }


}
