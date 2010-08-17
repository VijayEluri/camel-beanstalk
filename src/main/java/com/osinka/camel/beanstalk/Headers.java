/**
 * Copyright (C) 2010 Alexander Azarov <azarov@osinka.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.osinka.camel.beanstalk;

/**
 *
 * @author <a href="mailto:azarov@osinka.com">Alexander Azarov</a>
 */
public final class Headers {
    // in
    public static final String PRIORITY     = "beanstalk.priority";
    public static final String DELAY        = "beanstalk.delay";
    public static final String TIME_TO_RUN  = "beanstalk.timeToRun";

    // in/out
    public static final String JOB_ID       = "beanstalk.jobId";

    // out
    public static final String RESULT       = "beanstalk.result";
}