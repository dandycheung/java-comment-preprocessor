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
import org.junit.Test;

public class FunctionSTRLENTest extends AbstractFunctionTest {

  private static final FunctionSTRLEN HANDLER = new FunctionSTRLEN();

  @Test
  public void testExecution_Str() throws Exception {
    assertFunction("strlen(\"hello world\")", Value.valueOf(11L));
    assertDestinationFolderEmpty();
  }

  @Test
  public void testExecution_wrongCases() throws Exception {
    assertFunctionException("strlen()");
    assertFunctionException("strlen(11)");
    assertFunctionException("strlen(true)");
    assertFunctionException("strlen(1,2)");
    assertFunctionException("strlen(,)");
    assertDestinationFolderEmpty();
  }

  @Override
  public void testName() {
    assertEquals("strlen", HANDLER.getName());
  }

  @Override
  public void testReference() {
    assertReference(HANDLER);
  }

  @Override
  public void testArity() {
    assertEquals(1, HANDLER.getArity());
  }

  @Override
  public void testAllowedArgumentTypes() {
    assertAllowedArguments(HANDLER, new ValueType[][] {{ValueType.STRING}});
  }

  @Override
  public void testResultType() {
    assertEquals(ValueType.INT, HANDLER.getResultType());
  }

}
