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

import com.igormaznitsa.jcp.context.PreprocessorContext;
import com.igormaznitsa.jcp.expression.Value;
import com.igormaznitsa.jcp.expression.ValueType;
import java.util.List;
import java.util.Set;

/**
 * The class implements the ISSUBSTR function handler
 *
 * @author Igor Maznitsa (igor.maznitsa@igormaznitsa.com)
 */
public final class FunctionISSUBSTR extends AbstractFunction {

  private static final List<List<ValueType>> ARG_TYPES =
      List.of(List.of(ValueType.STRING, ValueType.STRING));

  @Override
  public String getName() {
    return "issubstr";
  }

  public Value executeStrStr(final PreprocessorContext context, final Value subStrValue,
                             final Value strValue) {
    final String str = strValue.asString().toLowerCase();
    final String subStr = subStrValue.asString().toLowerCase();
    return Value.valueOf(str.contains(subStr));
  }

  @Override
  public Set<Integer> getArity() {
    return ARITY_2;
  }

  @Override
  public List<List<ValueType>> getAllowedArgumentTypes() {
    return ARG_TYPES;
  }

  @Override
  public String getReference() {
    return "check that string contains substring (case insensitive)";
  }

  @Override
  public ValueType getResultType() {
    return ValueType.BOOLEAN;
  }

}
