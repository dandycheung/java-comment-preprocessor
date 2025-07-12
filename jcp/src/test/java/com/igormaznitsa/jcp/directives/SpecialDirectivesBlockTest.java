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

// This test checks work of //$$""", //$"""
// Those directives are very specific and they don't have any distinguished handler
public class SpecialDirectivesBlockTest extends AbstractDirectiveHandlerAcceptanceTest {

  @Override
  public void testExecution() throws Exception {
    assertFilePreprocessing("directive_special_block.txt", false, null, null);
  }

  @Override
  public void testKeyword() throws Exception {
    // do nothing because it is a group test
  }

  @Override
  public void testExecutionCondition() throws Exception {
    // do nothing because it is a group test
  }

  @Override
  public void testReference() throws Exception {
    // do nothing because it is a group test
  }

  @Override
  public void testArgumentType() throws Exception {
    // do nothing because it is a group test
  }

  @Override
  public void testPhase() throws Exception {
    // do nothing because it is a group test
  }

}
