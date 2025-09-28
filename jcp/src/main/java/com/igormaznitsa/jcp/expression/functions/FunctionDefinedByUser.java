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

package com.igormaznitsa.jcp.expression.functions;

import static java.util.Objects.requireNonNull;

import com.igormaznitsa.jcp.context.PreprocessorContext;
import com.igormaznitsa.jcp.expression.Value;
import com.igormaznitsa.jcp.expression.ValueType;
import com.igormaznitsa.jcp.extension.PreprocessorExtension;
import java.util.Arrays;
import java.util.List;

/**
 * The class implements the user defined function handler (a function which name
 * starts with $)
 *
 * @author Igor Maznitsa (igor.maznitsa@igormaznitsa.com)
 */
public final class FunctionDefinedByUser extends AbstractFunction {

  private final String name;
  private final int argsNumber;
  private final ValueType[][] argTypes;

  public FunctionDefinedByUser(final String name, final int argsNumber,
                               final PreprocessorContext context) {
    super();
    requireNonNull(name, "Name is null");
    requireNonNull(context, "Context is null");

    if (argsNumber < 0) {
      throw context.makeException("Unexpected argument number [" + argsNumber + ']', null);
    }

    this.name = name;
    this.argsNumber = argsNumber;

    final ValueType[] types = new ValueType[argsNumber];

    Arrays.fill(types, ValueType.ANY);
    this.argTypes = new ValueType[][] {types};
  }

  @Override

  public String getName() {
    return name;
  }

  @Override
  public int getArity() {
    return argsNumber;
  }


  public Value execute(final PreprocessorContext context, final Value[] values) {
    final List<PreprocessorExtension> extensionList = context.getPreprocessorExtensions();
    if (extensionList.isEmpty()) {
      throw context
          .makeException(
              "Found user defined function, but there is not any preprocessor extension to process it",
              null);
    }

    final PreprocessorExtension extension =
        extensionList.stream().filter(x -> x.hasUserFunction(this.name, values.length))
            .findFirst().orElseThrow(() -> context
                .makeException(
                    "Can't find any preprocessor extension to process function " + this.name + " for " +
                        values.length + " argument(s)", null));
    context.logDebug("Processing " + this.name + '/' + values.length + " by " +
        extension.getClass().getCanonicalName());
    return extension.processUserFunction(context, name, values);
  }

  @Override


  public ValueType[][] getAllowedArgumentTypes() {
    return argTypes;
  }

  @Override

  public String getReference() {
    return "user defined function";
  }

  @Override

  public ValueType getResultType() {
    return ValueType.ANY;
  }

}
