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

import static org.junit.Assert.assertEquals;

import com.igormaznitsa.jcp.expression.Value;
import com.igormaznitsa.jcp.expression.ValueType;
import com.igormaznitsa.jcp.utils.PreprocessorUtils;
import java.util.List;
import java.util.Set;
import org.junit.Test;

public class FunctionTRIMLINESTest extends AbstractFunctionTest {

  private static final FunctionTRIMLINES HANDLER = new FunctionTRIMLINES();

  @Test
  public void testExecution_WorkingCases() throws Exception {
    assertFunction("trimlines(\"\")", Value.valueOf(""));
    assertFunction("trimlines(\"hello world\")", Value.valueOf("hello world"));
    assertFunction("trimlines(\"  hello   \n   \n   world\n\")",
        Value.valueOf("hello" + PreprocessorUtils.getNextLineCodes() + "world"));
    assertDestinationFolderEmpty();
  }

  @Test
  public void testExecution_wrongCases() throws Exception {
    assertFunctionException("trimlines()");
    assertFunctionException("trimlines(1)");
    assertFunctionException("trimlines(true)");
    assertFunctionException("trimlines(true,\"ss\")");
    assertFunctionException("trimlines(\"ss\",3)");
    assertDestinationFolderEmpty();

  }

  @Override
  public void testName() {
    assertEquals("trimlines", HANDLER.getName());
  }

  @Override
  public void testReference() {
    assertReference(HANDLER);
  }

  @Override
  public void testArity() {
    assertEquals(Set.of(1), HANDLER.getArity());
  }

  @Override
  public void testAllowedArgumentTypes() {
    assertAllowedArguments(HANDLER, List.of(List.of(ValueType.STRING)));
  }

  @Override
  public void testResultType() {
    assertEquals(ValueType.STRING, HANDLER.getResultType());
  }
}
