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
package generate.tostring.view;

import generate.tostring.template.TemplateResource;

import javax.swing.*;
import java.util.List;

/**
 * List model to hold {@link generate.tostring.template.TemplateResource} information
 */
public class TemplateResourceListModel extends AbstractListModel {

    private final List list;

    public TemplateResourceListModel(List<TemplateResource> list) {
        this.list = list;
    }

    public int getSize() {
        return list.size();
    }

    public Object getElementAt(int index) {
        return list.get(index);
    }
    
}
