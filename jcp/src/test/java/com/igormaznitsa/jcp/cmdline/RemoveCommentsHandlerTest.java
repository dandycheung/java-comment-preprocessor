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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

import com.igormaznitsa.jcp.context.CommentRemoverType;
import com.igormaznitsa.jcp.context.PreprocessorContext;

public class RemoveCommentsHandlerTest extends AbstractCommandLineHandlerTest {

  private static final RemoveCommentsHandler HANDLER = new RemoveCommentsHandler();

  @Override
  public void testThatTheHandlerInTheHandlerList() {
    assertHandlerInTheHandlerList(HANDLER);
  }

  @Override
  public void testExecution() throws Exception {
    final PreprocessorContext mock = prepareMockContext();

    assertFalse(HANDLER.processCommandLineKey("", mock));
    assertFalse(HANDLER.processCommandLineKey("/r:", mock));
    assertFalse(HANDLER.processCommandLineKey("/R:", mock));
    assertFalse(HANDLER.processCommandLineKey("/RR", mock));

    assertTrue(HANDLER.processCommandLineKey("/r", mock));
    verify(mock).setKeepComments(CommentRemoverType.REMOVE_C_STYLE);
  }

  @Override
  public void testName() {
    assertEquals("/R", HANDLER.getKeyName());
  }

  @Override
  public void testDescription() {
    assertDescription(HANDLER);
  }
}
