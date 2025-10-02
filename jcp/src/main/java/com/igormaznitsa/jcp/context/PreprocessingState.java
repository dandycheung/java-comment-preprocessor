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

import static com.igormaznitsa.jcp.removers.AbstractCommentRemover.makeCommentRemover;
import static com.igormaznitsa.jcp.utils.IOUtils.closeQuietly;
import static com.igormaznitsa.jcp.utils.PreprocessorUtils.findFirstActiveFileContainer;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;

import com.igormaznitsa.jcp.containers.FileInfoContainer;
import com.igormaznitsa.jcp.containers.PreprocessingFlag;
import com.igormaznitsa.jcp.containers.TextFileDataContainer;
import com.igormaznitsa.jcp.exceptions.FilePositionInfo;
import com.igormaznitsa.jcp.exceptions.PreprocessorException;
import com.igormaznitsa.jcp.utils.PreprocessorUtils;
import com.igormaznitsa.jcp.utils.ResettablePrinter;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 * The class describes a preprocessor state also it contains inside buffers and save data on disk
 *
 * @author Igor Maznitsa (igor.maznitsa@igormaznitsa.com)
 */
public final class PreprocessingState {

  public static final FilePositionInfo[] EMPTY_STACK = new FilePositionInfo[0];

  public static final int MAX_WRITE_BUFFER_SIZE = 65536;
  public static final String FAKE_FILE_FOLDER = "/fake_test_folder";
  public static final String FAKE_FILE_PATH = FAKE_FILE_FOLDER + "/some_fake_file.txt";
  private final Charset globalInCharacterEncoding;
  private final Charset globalOutCharacterEncoding;
  private final TextFileDataContainer rootReference;
  private final FileInfoContainer rootFileInfo;
  private final LinkedList<TextFileDataContainer> whileStack = new LinkedList<>();
  private final LinkedList<TextFileDataContainer> ifStack = new LinkedList<>();
  private final LinkedList<TextFileDataContainer> includeStack = new LinkedList<>();
  private final LinkedList<ExcludeIfInfo> deferredExcludeStack = new LinkedList<>();
  private final ResettablePrinter prefixPrinter = new ResettablePrinter(1024);
  private final ResettablePrinter postfixPrinter = new ResettablePrinter(64 * 1024);
  private final ResettablePrinter normalPrinter = new ResettablePrinter(1024);
  private final boolean overrideOnlyIfContentChanged;
  private final EnumSet<PreprocessingFlag> preprocessingFlags =
      EnumSet.noneOf(PreprocessingFlag.class);
  private final PreprocessorContext context;
  private final boolean mockMode;
  private ResettablePrinter selectedPrinter;
  private TextFileDataContainer activeIf;
  private TextFileDataContainer activeWhile;
  private String lastReadString;
  private boolean globalPhase;

  PreprocessingState(final PreprocessorContext context, final Charset inEncoding,
                     final Charset outEncoding) {
    this.mockMode = true;
    this.globalInCharacterEncoding = requireNonNull(inEncoding);
    this.globalOutCharacterEncoding = requireNonNull(outEncoding);
    this.rootReference = null;
    this.lastReadString = "";
    this.rootFileInfo = new FileInfoContainer(new File("global"), "global", true);
    this.overrideOnlyIfContentChanged = true;
    this.context = context;
    init();
  }

  PreprocessingState(final PreprocessorContext context, final FileInfoContainer rootFile,
                     final Charset inEncoding, final Charset outEncoding,
                     final boolean overrideOnlyIfContentChanged) throws IOException {
    this.mockMode = false;

    this.context = context;

    this.overrideOnlyIfContentChanged = overrideOnlyIfContentChanged;
    this.globalInCharacterEncoding = requireNonNull(inEncoding);
    this.globalOutCharacterEncoding = requireNonNull(outEncoding);

    this.rootFileInfo = requireNonNull(rootFile, "The root file is null");
    init();
    rootReference = openFile(rootFile.getSourceFile());
  }

  PreprocessingState(final PreprocessorContext context, final FileInfoContainer rootFile,
                     final TextFileDataContainer rootContainer, final Charset inEncoding,
                     final Charset outEncoding, final boolean overrideOnlyIfContentChanged) {
    this.mockMode = false;

    this.context = context;

    this.globalInCharacterEncoding = requireNonNull(inEncoding);
    this.globalOutCharacterEncoding = requireNonNull(outEncoding);
    this.overrideOnlyIfContentChanged = overrideOnlyIfContentChanged;

    this.rootFileInfo = requireNonNull(rootFile, "The root file is null");
    init();
    rootReference = rootContainer;
    includeStack.push(rootContainer);
  }

  public static PreprocessingState makeMock(final PreprocessorContext context) {
    return new PreprocessingState(context, StandardCharsets.UTF_8, StandardCharsets.UTF_8);
  }

  public boolean isMockMode() {
    return this.mockMode;
  }

  public boolean isGlobalPhase() {
    return this.globalPhase;
  }

  public void setGlobalPhase(final boolean flag) {
    this.globalPhase = flag;
  }

  public String getLastReadString() {
    return this.lastReadString;
  }

  public void pushExcludeIfData(final FileInfoContainer infoContainer,
                                final String excludeIfCondition, final int stringIndex) {
    requireNonNull(infoContainer, "File info is null");
    requireNonNull(excludeIfCondition, "Condition is null");

    if (stringIndex < 0) {
      throw new IllegalArgumentException("Unexpected string index [" + stringIndex + ']');
    }

    deferredExcludeStack.push(new ExcludeIfInfo(infoContainer, excludeIfCondition, stringIndex));
  }

  public ResettablePrinter getSelectedPrinter() {
    return this.selectedPrinter;
  }

  public List<ExcludeIfInfo> popAllExcludeIfInfoData() {
    final List<ExcludeIfInfo> result = new ArrayList<>(deferredExcludeStack);
    this.deferredExcludeStack.clear();
    return result;
  }


  public ExcludeIfInfo popExcludeIfData() {
    return this.deferredExcludeStack.pop();
  }


  public Set<PreprocessingFlag> getPreprocessingFlags() {
    return this.preprocessingFlags;
  }

  public ResettablePrinter findPrinter(final PrinterType type) {
    switch (requireNonNull(type, "Type is null")) {
      case NORMAL:
        return this.normalPrinter;
      case POSTFIX:
        return this.postfixPrinter;
      case PREFIX:
        return this.prefixPrinter;
      default:
        throw new IllegalArgumentException("Unsupported type detected [" + type.name() + ']');
    }
  }

  public void selectPrinter(final PrinterType type) {
    switch (requireNonNull(type, "Type is null")) {
      case NORMAL:
        this.selectedPrinter = this.normalPrinter;
        break;
      case POSTFIX:
        this.selectedPrinter = this.postfixPrinter;
        break;
      case PREFIX:
        this.selectedPrinter = this.prefixPrinter;
        break;
      default:
        throw new IllegalArgumentException("Unsupported type detected [" + type.name() + ']');
    }
  }


  public TextFileDataContainer getRootTextContainer() {
    return this.rootReference;
  }


  public TextFileDataContainer openFile(final File file) throws IOException {
    requireNonNull(file, "The file is null");

    final AtomicBoolean endedByNextLineContainer = new AtomicBoolean();

    final String[] texts = PreprocessorUtils
        .readWholeTextFileIntoArray(file, globalInCharacterEncoding, endedByNextLineContainer);
    final TextFileDataContainer newContainer =
        new TextFileDataContainer(file, texts, endedByNextLineContainer.get(), 0);
    includeStack.push(newContainer);
    return newContainer;
  }

  public TextFileDataContainer peekIncludeStackFile() {
    return this.includeStack.peek();
  }

  public List<TextFileDataContainer> getIncludeStack() {
    return this.includeStack;
  }

  public Optional<TextFileDataContainer> findActiveTextFileDataContainer() {
    if (this.isMockMode()) {
      return Optional.of(
          new TextFileDataContainer(new File(FAKE_FILE_PATH), new String[] {""}, false, 0));
    }
    final TextFileDataContainer includeFile = this.peekIncludeStackFile();
    return includeFile == null ? Optional.ofNullable(this.getRootTextContainer()) :
        Optional.of(includeFile);
  }

  public Optional<FilePositionInfo> findFilePositionInfo() {
    return this.findActiveTextFileDataContainer().map(
        x -> new FilePositionInfo(x.getFile(), x.getLastReadStringIndex()));
  }

  public FilePositionInfo[] makeIncludeStack() {
    if (this.isMockMode()) {
      return EMPTY_STACK;
    }

    final FilePositionInfo[] stack = new FilePositionInfo[includeStack.size()];
    for (int i = 0; i < includeStack.size(); i++) {
      final TextFileDataContainer fileContainer = this.includeStack.get(i);
      stack[i] =
          new FilePositionInfo(fileContainer.getFile(), fileContainer.getLastReadStringIndex());
    }
    return stack;
  }

  public TextFileDataContainer getCurrentIncludeFileContainer() {
    return this.includeStack.isEmpty() ? null : this.includeStack.get(this.includeStack.size() - 1);
  }

  public TextFileDataContainer popTextContainer() {
    if (this.includeStack.isEmpty()) {
      throw new IllegalStateException("Include stack is empty");
    }
    return this.includeStack.pop();
  }

  public FileInfoContainer getRootFileInfo() {
    return this.rootFileInfo;
  }

  public boolean isIncludeStackEmpty() {
    return includeStack.isEmpty();
  }

  public boolean isOnlyRootOnStack() {
    return includeStack.size() == 1;
  }


  private TextFileDataContainer cloneTopTextDataContainer() {
    final TextFileDataContainer topElement = requireNonNull(includeStack.peek());
    return new TextFileDataContainer(topElement,
        topElement.getLastReadStringIndex());
  }


  public PreprocessingState popWhile() {
    final TextFileDataContainer whileOnTop = whileStack.pop();
    if (whileOnTop == activeWhile) {
      preprocessingFlags.remove(PreprocessingFlag.BREAK_COMMAND);
      if (whileStack.isEmpty()) {
        activeWhile = null;
      } else {
        activeWhile = whileStack.peek();
      }
    }
    return this;
  }


  public PreprocessingState pushWhile(final boolean makeActive) {
    final TextFileDataContainer whileRef = cloneTopTextDataContainer();
    whileStack.push(whileRef);
    if (makeActive) {
      activeWhile = whileRef;
    }
    return this;
  }


  public TextFileDataContainer peekWhile() {
    return whileStack.peek();
  }

  public boolean hasReadLineNextLineInEnd() {
    return includeStack.peek().isPresentedNextLineOnReadString();
  }


  public String nextLine() {
    final String result = includeStack.peek().nextLine();
    this.lastReadString = result;
    return result;
  }


  public PreprocessingState goToString(final int stringIndex) {
    includeStack.peek().setNextStringIndex(stringIndex);
    return this;
  }


  public PreprocessingState pushIf(final boolean makeActive) {
    final TextFileDataContainer ifRef = cloneTopTextDataContainer();
    ifStack.push(ifRef);
    if (makeActive) {
      activeIf = ifRef;
    }
    return this;
  }

  public void popAllIFUntilContainerWithFile(final TextFileDataContainer container) {
    final File file = container.getFile();
    final int stringIndex = container.getNextStringIndex();
    while (!ifStack.isEmpty()) {
      final TextFileDataContainer top = ifStack.peek();
      if (!top.getFile().equals(file) || top.getNextStringIndex() <= stringIndex) {
        break;
      } else {
        ifStack.pop();
      }
    }
  }


  public PreprocessingState popIf() {
    final TextFileDataContainer ifRef = ifStack.pop();
    if (ifRef == activeIf) {
      if (ifStack.isEmpty()) {
        activeIf = null;
      } else {
        activeIf = ifStack.peek();
      }
    }
    return this;
  }

  public boolean isAtActiveWhile() {
    if (whileStack.isEmpty()) {
      return true;
    } else {
      return activeWhile == whileStack.peek();
    }
  }

  public boolean isAtActiveIf() {
    if (ifStack.isEmpty()) {
      return true;
    } else {
      return ifStack.peek() == activeIf;
    }
  }

  public boolean isDirectiveCanBeProcessedIgnoreBreak() {
    return isAtActiveIf() && isAtActiveWhile() &&
        !preprocessingFlags.contains(PreprocessingFlag.IF_CONDITION_FALSE);
  }

  public boolean isDirectiveCanBeProcessed() {
    return isDirectiveCanBeProcessedIgnoreBreak() &&
        !preprocessingFlags.contains(PreprocessingFlag.BREAK_COMMAND);
  }


  public TextFileDataContainer peekIf() {
    return ifStack.peek();
  }

  public boolean isIfStackEmpty() {
    return ifStack.isEmpty();
  }

  public boolean isWhileStackEmpty() {
    return whileStack.isEmpty();
  }

  private void init() {
    preprocessingFlags.clear();
    resetPrinters();

    selectPrinter(PrinterType.NORMAL);
  }

  public void resetPrinters() {
    normalPrinter.reset();
    prefixPrinter.reset();
    postfixPrinter.reset();

    selectedPrinter = normalPrinter;
  }

  public void setBufferText(final String text) {
    this.prefixPrinter.reset();
    this.normalPrinter.reset();
    this.postfixPrinter.reset();
    this.setBufferText(text, PrinterType.NORMAL);
  }

  public void setBufferText(final String text, final PrinterType printerType) {
    switch (printerType) {
      case NORMAL: {
        this.normalPrinter.reset();
        this.normalPrinter.print(requireNonNullElse(text, ""));
      }
      break;
      case PREFIX: {
        this.prefixPrinter.reset();
        this.prefixPrinter.print(requireNonNullElse(text, ""));
      }
      break;
      case POSTFIX: {
        this.postfixPrinter.reset();
        this.postfixPrinter.print(requireNonNullElse(text, ""));
      }
      break;
      default:
        throw new IllegalArgumentException("Unsupported printer type: " + printerType);
    }
  }

  public String getCurrentText() {
    return this.prefixPrinter.getText()
        + this.normalPrinter.getText()
        + this.postfixPrinter.getText();
  }

  public void saveBuffersToStreams(final OutputStream prefix, final OutputStream normal,
                                   final OutputStream postfix) throws IOException {
    prefixPrinter.writeBufferTo(
        new BufferedWriter(new OutputStreamWriter(prefix, globalOutCharacterEncoding)));
    normalPrinter.writeBufferTo(
        new BufferedWriter(new OutputStreamWriter(normal, globalOutCharacterEncoding)));
    postfixPrinter.writeBufferTo(
        new BufferedWriter(new OutputStreamWriter(postfix, globalOutCharacterEncoding)));
  }

  public boolean saveBuffersToFile(final File outFile, final CommentRemoverType keepComments)
      throws IOException {
    final File path = outFile.getParentFile();

    if (path != null && !path.exists() && !path.mkdirs()) {
      throw new IOException("Can't make directory [" + PreprocessorUtils.getFilePath(path) + ']');
    }

    Writer writer = null;

    boolean wasSaved = false;
    try {
      final int totatBufferedChars =
          prefixPrinter.getSize() + normalPrinter.getSize() + postfixPrinter.getSize();
      final int BUFFER_SIZE =
          Math.max(64, Math.min(totatBufferedChars << 1, MAX_WRITE_BUFFER_SIZE));

      if (this.overrideOnlyIfContentChanged) {
        String content = writePrinterBuffers(new StringWriter(totatBufferedChars)).toString();
        if (keepComments != CommentRemoverType.KEEP_ALL) {
          content = makeCommentRemover(
              keepComments,
              new StringReader(content),
              new StringWriter(totatBufferedChars),
              context.isAllowWhitespaces()).process().toString();
        }

        boolean needWrite = true; // better write than not
        final byte[] contentInBinaryForm = content.getBytes(globalOutCharacterEncoding);
        if (outFile.isFile() && outFile.length() == contentInBinaryForm.length) {
          // If file exists and has the same content, then skip overwriting it
          try (InputStream currentFileInputStream = new BufferedInputStream(
              Files.newInputStream(outFile.toPath()), Math.max(16384, (int) outFile.length()))) {
            needWrite = !IOUtils.contentEquals(currentFileInputStream,
                new ByteArrayInputStream(contentInBinaryForm));
          }
        }
        if (needWrite) {
          FileUtils.writeByteArrayToFile(outFile, contentInBinaryForm, false);
          wasSaved = true;
        } else {
          this.context.logDebug(
              "Ignore writing data for " + outFile + " because its content has not been changed");
        }
      } else if (keepComments != CommentRemoverType.KEEP_ALL) {
        final String joinedBufferContent =
            writePrinterBuffers(new StringWriter(totatBufferedChars)).toString();
        writer = new OutputStreamWriter(
            new BufferedOutputStream(new FileOutputStream(outFile, false), BUFFER_SIZE),
            globalOutCharacterEncoding);
        writer = makeCommentRemover(keepComments,
            new StringReader(joinedBufferContent), writer, context.isAllowWhitespaces()).process();
        wasSaved = true;
      } else {
        writer = new OutputStreamWriter(
            new BufferedOutputStream(new FileOutputStream(outFile, false), BUFFER_SIZE),
            globalOutCharacterEncoding);
        writePrinterBuffers(writer);
        wasSaved = true;
      }
    } finally {
      closeQuietly(writer);
    }

    if (wasSaved) {
      findFirstActiveFileContainer(context).ifPresent(t -> t.getGeneratedResources().add(outFile));
      if (this.context.isKeepAttributes() && outFile.exists()) {
        PreprocessorUtils.copyFileAttributes(this.getRootFileInfo().getSourceFile(), outFile);
      }
    }

    return wasSaved;
  }


  public Writer writePrinterBuffers(final Writer writer) throws IOException {
    if (!prefixPrinter.isEmpty()) {
      prefixPrinter.writeBufferTo(writer);
    }

    if (!normalPrinter.isEmpty()) {
      normalPrinter.writeBufferTo(writer);
    }

    if (!postfixPrinter.isEmpty()) {
      postfixPrinter.writeBufferTo(writer);
    }

    return writer;
  }


  public PreprocessorException makeException(final String message, final String causeString,
                                             final Throwable cause) {
    return new PreprocessorException(message, causeString, makeIncludeStack(), cause);
  }

  public enum PrinterType {

    NORMAL,
    PREFIX,
    POSTFIX
  }

  public static class ExcludeIfInfo {

    private final FileInfoContainer fileInfoContainer;
    private final String condition;
    private final int stringIndex;

    public ExcludeIfInfo(final FileInfoContainer fileInfoContainer, final String condition,
                         final int stringIndex) {
      this.fileInfoContainer = fileInfoContainer;
      this.condition = condition.trim();
      this.stringIndex = stringIndex;
    }

    public int getStringIndex() {
      return this.stringIndex;
    }


    public FileInfoContainer getFileInfoContainer() {
      return fileInfoContainer;
    }


    public String getCondition() {
      return condition;
    }
  }
}
