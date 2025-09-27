/*
 * Copyright 2002-2019 Igor Maznitsa (http://www.igormaznitsa.com)
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.igormaznitsa.jcp.cmdline;

import com.igormaznitsa.jcp.context.PreprocessorContext;

/**
 * Enable merging of text lines found in //$""" and //$$""" into single text block
 * for processing by external processors.
 *
 * @author Igor Maznitsa (igor.maznitsa@igormaznitsa.com)
 * @since 7.2.0
 */
public class AllowMergeBlockLineHandler implements CommandLineHandler {

  private static final String ARG_NAME = "/B";

  @Override
  public String getDescription() {
    return "treat //$\"\"\" and //$$\"\"\" prefixed lines as single text block";
  }

  @Override
  public boolean processCommandLineKey(final String key, final PreprocessorContext context) {
    boolean result = false;

    if (ARG_NAME.equalsIgnoreCase(key)) {
      context.setAllowsBlocks(true);
      result = true;
    }

    return result;
  }

  @Override
  public String getKeyName() {
    return ARG_NAME;
  }

}
