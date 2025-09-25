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

package com.igormaznitsa.jcp.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.igormaznitsa.jcp.expression.Value;
import com.igormaznitsa.jcp.extension.PreprocessorExtension;
import com.igormaznitsa.jcp.logger.PreprocessorLogger;
import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.Test;

@SuppressWarnings("rawtypes")
public class PreprocessorContextTest {

  private static final Random RND = new Random(776655);

  private static Set<Field> extractDeclaredNonStaticNonFinalFields(final Class<?> targetClass) {
    final Set<Field> result = new HashSet<>();
    for (final Field f : targetClass.getDeclaredFields()) {
      if ((f.getModifiers() & (Modifier.STATIC | Modifier.FINAL)) != 0) {
        continue;
      }
      result.add(f);
    }
    return result;
  }

  private static Map<Field, Object> extractValues(final PreprocessorContext context) throws Exception {
    final Map<Field, Object> result = new HashMap<>();
    for (final Field f : extractDeclaredNonStaticNonFinalFields(PreprocessorContext.class)) {
      f.setAccessible(true);
      result.put(f, f.get(context));
    }
    return result;
  }

  private static void assertObjectValue(final String fieldName, final Object value, final Object that) {
    if (value != that) {
      if (value == null || that == null) {
        assertSame(fieldName, value, that);
      } else if (List.class.isAssignableFrom(value.getClass())) {
        final List thisList = (List) value;
        final List thatList = (List) that;
        assertEquals(fieldName, thisList.size(), thatList.size());

        for (int i = 0; i < thisList.size(); i++) {
          assertObjectValue(fieldName, thisList.get(i), thatList.get(i));
        }

      } else if (Map.class.isAssignableFrom(value.getClass())) {
        final Map thisMap = (Map) value;
        final Map thatMap = (Map) that;
        assertEquals(fieldName, thisMap.size(), thatMap.size());

        for (final Object k : thisMap.keySet()) {
          final Object thisValue = thisMap.get(k);
          assertTrue(fieldName, thatMap.containsKey(k));
          assertObjectValue(fieldName, thisValue, thatMap.get(k));
        }

      } else if (Set.class.isAssignableFrom(value.getClass())) {
        final Set thisSet = (Set) value;
        final Set thatSet = (Set) that;
        assertEquals(fieldName, thisSet.size(), thatSet.size());

        for (final Object v : thisSet) {
          assertTrue(fieldName, thatSet.contains(v));
        }

      } else if (value.getClass().isArray()) {
        assertEquals(Array.getLength(value), Array.getLength(that));
        for (int i = 0; i < Array.getLength(value); i++) {
          assertObjectValue(fieldName, Array.get(value, i), Array.get(that, i));
        }
      } else {
        assertEquals(fieldName, value, that);
      }
    }
  }

  private static void assertMapFields(final String mapFieldName, final PreprocessorContext etalon, final PreprocessorContext that) throws Exception {
    Field field = null;
    for (final Field f : PreprocessorContext.class.getDeclaredFields()) {
      if (mapFieldName.equals(f.getName())) {
        field = f;
        field.setAccessible(true);
        break;
      }
    }

    assertNotNull("Can't find field " + mapFieldName, field);

    final Map thisMap = (Map) field.get(etalon);
    final Map thatMap = (Map) field.get(that);

    assertEquals("Map fields must have same size '" + mapFieldName + '\'', thisMap, thatMap);

    for (final Object k : thisMap.keySet()) {
      assertTrue(thatMap.containsKey(k));
      assertSame("Key '" + k + "' at map field '" + mapFieldName + "'", thisMap.get(k), thatMap.get(k));
    }
  }

  private static void assertPreprocessorContextMaps(final PreprocessorContext etalon, final PreprocessorContext that) throws Exception {
    int detected = 0;
    for (final Field f : PreprocessorContext.class.getDeclaredFields()) {
      if (Modifier.isFinal(f.getModifiers()) && Map.class.isAssignableFrom(f.getType())) {
        assertMapFields(f.getName(), etalon, that);
        detected++;
      }
    }
    assertEquals(4, detected);
  }

  private static void assertContextEquals(final Map<Field, Object> etalon, final Map<Field, Object> value) {
    assertEquals("Must have same number of elements", etalon.size(), value.size());

    for (final Field f : etalon.keySet()) {
      assertObjectValue(f.getName(), etalon.get(f), value.get(f));
    }
  }

  private static String randomString() {
    final StringBuilder result = new StringBuilder();

    for (int i = 0; i < 32; i++) {
      result.append((char) ('a' + RND.nextInt(52)));
    }

    return result.toString();
  }

  @SuppressWarnings("unchecked")
  private static void fillByRandomValues(final PreprocessorContext context) throws Exception {

    for (final Field f : extractDeclaredNonStaticNonFinalFields(PreprocessorContext.class)) {
      f.setAccessible(true);

      final Class type = f.getType();

      if (type.isArray()) {
        if (type.getComponentType() == String.class) {
          final String[] arr = new String[RND.nextInt(32) + 1];
          for (int i = 0; i < arr.length; i++) {
            arr[i] = randomString();
          }
          f.set(context, arr);
        } else if (type.getComponentType() == File.class) {
          final File[] arr = new File[RND.nextInt(32) + 1];
          for (int i = 0; i < arr.length; i++) {
            arr[i] = new File(randomString());
          }
          f.set(context, arr);
        } else {
          throw new Error("Unexpected array field type : " + type.getComponentType().getName());
        }
      } else if (type == Boolean.class || type == boolean.class) {
        f.set(context, RND.nextBoolean());
      } else if (type == Integer.class || type == int.class) {
        f.set(context, RND.nextInt(10000));
      } else if (type == String.class) {
        f.set(context, randomString());
      } else if (type == File.class) {
        f.set(context, new File(randomString()));
      } else if (Set.class.isAssignableFrom(type)) {
        final String[] arr = new String[RND.nextInt(32) + 1];
        for (int i = 0; i < arr.length; i++) {
          arr[i] = randomString();
        }
        try {
          f.set(context, new HashSet<>(Arrays.asList(arr)));
        } catch (Exception ex) {
          fail("Can't set value to '" + f.getName() + '\'');
        }
      } else if (type == Charset.class) {
        final Charset charset;
        switch (RND.nextInt(4)) {
          case 0:
            charset = StandardCharsets.ISO_8859_1;
            break;
          case 1:
            charset = StandardCharsets.US_ASCII;
            break;
          case 2:
            charset = StandardCharsets.UTF_16;
            break;
          case 3:
            charset = StandardCharsets.UTF_16BE;
            break;
          default:
            charset = StandardCharsets.UTF_8;
            break;
        }
        f.set(context, charset);
      } else if (type == PreprocessingState.class) {
        f.set(context, new PreprocessingState(context, StandardCharsets.UTF_8, StandardCharsets.UTF_8));
      } else if (type == PreprocessorLogger.class) {
        f.set(context, new PreprocessorLogger() {
          @Override
          public void error(String message) {
          }

          @Override
          public void info(String message) {
          }

          @Override
          public void debug(String message) {
          }

          @Override
          public void warning(String message) {
          }
        });
      } else if (type == PreprocessorExtension.class) {
        final PreprocessorExtension exx = new PreprocessorExtension() {
          @Override
          public boolean hasAction(int arity) {
            return true;
          }

          @Override
          public boolean hasUserFunction(String name, int arity) {
            return true;
          }

          @Override
          public boolean processAction(PreprocessorContext context, Value[] parameters) {
            throw new UnsupportedOperationException(
                "Not supported yet.");
          }

          @Override
          public Value processUserFunction(PreprocessorContext context, String functionName,
                                           Value[] arguments) {
            throw new UnsupportedOperationException(
                "Not supported yet.");
          }

          @Override
          public int getUserFunctionArity(String functionName) {
            throw new UnsupportedOperationException(
                "Not supported yet.");
          }
        };

        f.set(context, exx);
      } else if (type.isAssignableFrom(CommentRemoverType.class)) {
        f.set(context, CommentRemoverType.values()[((int)System.nanoTime() & 0x1FFFFFFF) % CommentRemoverType.values().length]);
      } else if (type.isAssignableFrom(List.class) || type.isAssignableFrom(Set.class)) {
        // ignored
      } else {
        throw new Error(String.format("Unexpected %s %s", f.getName(), type.getName()));
      }
    }

    context.setLocalVariable("LocalHelloOne", Value.INT_ONE);
    context.setGlobalVariable("GlobalHelloOne", Value.INT_FIVE);
    context.setSharedResource("RESOURCE111", "Some string");
    context.registerSpecialVariableProcessor(new SpecialVariableProcessor() {
      @Override
      public String[] getVariableNames() {
        return new String[] {"uno::", "tuo::"};
      }

      @Override
      public Value getVariable(String varName, PreprocessorContext context) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
      }

      @Override
      public void setVariable(String varName, Value value, PreprocessorContext context) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
      }
    });
  }

  @Test
  public void testConstuctorWithBaseContext_DefaultValues() throws Exception {
    final PreprocessorContext baseContext = new PreprocessorContext(new File("some_impossible_folder_121212"));

    final Map<Field, Object> baseContextValues = extractValues(baseContext);
    assertFalse(baseContextValues.isEmpty());

    final PreprocessorContext clonedContext = new PreprocessorContext(baseContext);
    final Map<Field, Object> clonedContextValues = extractValues(clonedContext);

    assertFalse(baseContext.isCloned());
    assertTrue(clonedContext.isCloned());
    assertContextEquals(baseContextValues, clonedContextValues);
    assertPreprocessorContextMaps(baseContext, clonedContext);
  }

  @Test
  public void testConstructorWithBaseContext_RandomValues() throws Exception {
    for (int i = 0; i < 100; i++) {
      final PreprocessorContext etalon = new PreprocessorContext(new File("some_impossible_folder_121212"));
      fillByRandomValues(etalon);
      final PreprocessorContext cloned = new PreprocessorContext(etalon);
      assertFalse(etalon.isCloned());
      assertTrue(cloned.isCloned());

      assertContextEquals(extractValues(etalon), extractValues(cloned));
      assertPreprocessorContextMaps(etalon, cloned);
    }
  }

}
