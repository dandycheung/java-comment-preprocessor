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

package com.igormaznitsa.jcp.directives;

import com.igormaznitsa.jcp.context.PreprocessorContext;
import java.util.ArrayList;
import java.util.List;

/**
 * The class is the abstract parent for all classes process preprocessor
 * directives
 *
 * @author Igor Maznitsa (igor.maznitsa@igormaznitsa.com)
 */
public abstract class AbstractDirectiveHandler {

  /**
   * The common preprocessor prefix for all directives
   */
  public static final String DIRECTIVE_PREFIX = "//#";

  /**
   * The prefix for lines to be kept by preprocessor
   */
  public static final String PREFIX_FOR_KEEPING_LINES = "//JCP> ";

  /**
   * The prefix for lines to be kept by preprocessor, which contain processed
   * directives
   */
  public static final String PREFIX_FOR_KEEPING_LINES_PROCESSED_DIRECTIVES = "//JCP! ";

  /**
   * The prefix for one line comment
   */
  public static final String ONE_LINE_COMMENT = "//";

  public static List<AbstractDirectiveHandler> findAllDirectives() {
    final List<AbstractDirectiveHandler> result = new ArrayList<>();
    result.add(new LocalDirectiveHandler());
    result.add(new IfDefinedDirectiveHandler());
    result.add(new IfNDefDirectiveHandler());
    result.add(new IfDefDirectiveHandler());
    result.add(new IfDirectiveHandler());
    result.add(new ElseDirectiveHandler());
    result.add(new EndIfDirectiveHandler());
    result.add(new WhileDirectiveHandler());
    result.add(new BreakDirectiveHandler());
    result.add(new ContinueDirectiveHandler());
    result.add(new EndDirectiveHandler());
    result.add(new ExitIfDirectiveHandler());
    result.add(new ExitDirectiveHandler());
    result.add(new OutdirDirectiveHandler());
    result.add(new OutEnabledDirectiveHandler());
    result.add(new OutNameDirectiveHandler());
    result.add(new OutDisabledDirectiveHandler());
    result.add(new CommentNextLineDirectiveHandler());
    result.add(new DefinelDirectiveHandler());
    result.add(new DefineDirectiveHandler());
    result.add(new UndefDirectiveHandler());
    result.add(new FlushDirectiveHandler());
    result.add(new IncludeDirectiveHandler());
    result.add(new ActionDirectiveHandler());
    result.add(new PostfixDirectiveHandler());
    result.add(new PrefixDirectiveHandler());
    result.add(new GlobalDirectiveHandler());
    result.add(new GlobalElseDirectiveHandler());
    result.add(new GlobalEndIfDirectiveHandler());
    result.add(new GlobalIfDirectiveHandler());
    result.add(new ExcludeIfDirectiveHandler());
    result.add(new ErrorDirectiveHandler());
    result.add(new WarningDirectiveHandler());
    result.add(new EchoDirectiveHandler());
    result.add(new MsgDirectiveHandler());
    result.add(new NoAutoFlushHandler());
    result.add(new AbortDirectiveHandler());
    return result;
  }

  /**
   * Get the name of the directive without prefix
   *
   * @return the directive name, must not be null
   */

  public abstract String getName();

  /**
   * Get the directive reference, it will be printed for a help request
   *
   * @return the directive reference as a String, must not be null
   */

  public abstract String getReference();

  /**
   * Get the directive name with prefix
   *
   * @return the full directive name (it including prefix)
   */

  public String getFullName() {
    return DIRECTIVE_PREFIX + getName();
  }

  /**
   * Get the argument type needed by the directive
   *
   * @return the argument type needed by the directive, it can't be null
   */

  public DirectiveArgumentType getArgumentType() {
    return DirectiveArgumentType.NONE;
  }

  /**
   * Execute directive
   *
   * @param tailString the tail of the string where the directive has been met, must not be null but can be empty
   * @param context    the preprocessor context
   * @return the needed preprocessor behavior, must not be null
   */

  public abstract AfterDirectiveProcessingBehaviour execute(String tailString,
                                                            PreprocessorContext context);

  /**
   * Shows that the directive can be executed only when the preprocessing n
   * active state i.e. if it is in active block //#if..//#endif of //#while
   *
   * @return true if the directive can be executed only if it is in active
   * block, else the directive will be called in any case
   */
  public boolean executeOnlyWhenExecutionAllowed() {
    return true;
  }

  /**
   * Shows that the directive can be executed during a global preprocessing
   * phase
   *
   * @return true if the directive allows the global directive phase, false if
   * the directive must be ignored during that phase
   */
  public boolean isGlobalPhaseAllowed() {
    return false;
  }

  /**
   * Shows that the directive can be executed during the second preprocessing
   * phase
   *
   * @return true uf the directive can be executed during the second
   * preprocessing phase else false if the directive must be ignored
   */
  public boolean isPreprocessingPhaseAllowed() {
    return true;
  }

  /**
   * Check that the directive is deprecated one and can be removed in a next release
   *
   * @return true if the directive is deprecated, false otherwise
   */
  public boolean isDeprecated() {
    return false;
  }
}
