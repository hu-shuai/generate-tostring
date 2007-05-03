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

/**
 * Options for the various policies.
 */
public class PolicyOptions {

    private static transient InsertNewMethodPolicy[] newMethodOptions = {InsertAtCaretPolicy.getInstance(), InsertAfterEqualsHashCodePolicy.getInstance(), InsertLastPolicy.getInstance()};

    private static transient ConflictResolutionPolicy[] conflictOptions = {ReplacePolicy.getInstance(), DuplicatePolicy.getInstance(), CancelPolicy.getInstance()};

    /**
     * Get's the options for the the insert new method policy.
     * @return the options for the the insert new method policy.
     */
    public static InsertNewMethodPolicy[] getNewMethodOptions() {
        return newMethodOptions;
    }

    /**
     * Get's the options for the the conflict resolution policy.
     * @return the options for the the conflict resolution policy.
     */
    public static ConflictResolutionPolicy[] getConflictOptions() {
        return conflictOptions;
    }

}
