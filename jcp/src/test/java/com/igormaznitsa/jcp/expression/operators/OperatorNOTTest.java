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

package com.igormaznitsa.jcp.expression.operators;

import static org.junit.Assert.assertEquals;

import com.igormaznitsa.jcp.expression.ExpressionItemPriority;
import com.igormaznitsa.jcp.expression.Value;

public class OperatorNOTTest extends AbstractOperatorTest {

  private static final OperatorNOT HANDLER = new OperatorNOT();

  @Override
  public void testKeyword() {
    assertEquals("!", HANDLER.getKeyword());
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
  public void testPriority() {
    assertEquals(ExpressionItemPriority.FUNCTION, HANDLER.getExpressionItemPriority());
  }

  @Override
  public void testExecution() throws Exception {
    assertExecution(Value.BOOLEAN_TRUE, "!false");
    assertExecution(Value.BOOLEAN_FALSE, "!true");
    assertExecution(Value.valueOf(0xFFFFFFFFFFFFFFFFL ^ 10L), "!10");
  }

  @Override
  public void testExecution_PreprocessorException() throws Exception {
    assertPreprocessorException("!");
//TODO        assertIllegalStateException("2!");
    assertPreprocessorException("!\"test\"");
    assertPreprocessorException("!3.2");
  }

}
