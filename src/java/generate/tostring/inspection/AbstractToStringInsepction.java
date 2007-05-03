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

import com.intellij.codeInspection.LocalInspectionTool;
import org.apache.log4j.Logger;
import generate.tostring.GenerateToStringContext;

/**
 * Base class for inspection support.
 */
public abstract class AbstractToStringInsepction extends LocalInspectionTool {

    protected Logger log = Logger.getLogger(getClass());

    public String getGroupDisplayName() {
        return "toString() issues";
    }

    /**
     * Is the on the fly code inspection enabled?
     *
     * @return  true if enabled, false if not.
     */
    protected boolean onTheFlyEnabled() {
        return GenerateToStringContext.getConfig().isInspectionOnTheFly();
    }

}
