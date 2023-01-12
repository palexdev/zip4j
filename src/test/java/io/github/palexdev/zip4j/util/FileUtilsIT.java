package io.github.palexdev.zip4j.util;

import io.github.palexdev.zip4j.AbstractIT;
import io.github.palexdev.zip4j.exception.ZipException;
import io.github.palexdev.zip4j.model.ExcludeFileFilter;
import io.github.palexdev.zip4j.model.ZipParameters;
import io.github.palexdev.zip4j.model.enums.RandomAccessFileMode;
import io.github.palexdev.zip4j.progress.ProgressMonitor;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static io.github.palexdev.zip4j.testutils.TestUtils.getTestFileFromResources;
import static io.github.palexdev.zip4j.util.InternalZipConstants.BUFF_SIZE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FileUtilsIT extends AbstractIT {

  private ProgressMonitor progressMonitor = new ProgressMonitor();

  @Test
  public void testCopyFileThrowsExceptionWhenStartsIsLessThanZero() throws IOException {
    testInvalidOffsetsScenario(-1, 100);
  }

  @Test
  public void testCopyFileThrowsExceptionWhenEndIsLessThanZero() throws IOException {
    testInvalidOffsetsScenario(0, -1);
  }

  @Test
  public void testCopyFileThrowsExceptionWhenStartIsGreaterThanEnd() throws IOException {
    testInvalidOffsetsScenario(300, 100);
  }

  @Test
  public void testCopyFilesWhenStartIsSameAsEndDoesNothing() throws IOException {
    File sourceFile = getTestFileFromResources("sample.pdf");
    File outputFile = Files.createTempFile(temporaryFolder, "zip4j_test", "").toFile();
    try (RandomAccessFile randomAccessFile = new RandomAccessFile(sourceFile, RandomAccessFileMode.READ.getValue());
         OutputStream outputStream = new FileOutputStream(outputFile)) {
      FileUtils.copyFile(randomAccessFile, outputStream, 100, 100, progressMonitor, BUFF_SIZE);
    }

    assertThat(outputFile.exists()).isTrue();
    assertThat(outputFile.length()).isZero();
  }

  @Test
  public void testCopyFilesCopiesCompleteFile() throws IOException {
    File sourceFile = getTestFileFromResources("sample.pdf");
    File outputFile = Files.createTempFile(temporaryFolder, "zip4j_test", "").toFile();
    try (RandomAccessFile randomAccessFile = new RandomAccessFile(sourceFile, RandomAccessFileMode.READ.getValue());
         OutputStream outputStream = new FileOutputStream(outputFile)) {
      FileUtils.copyFile(randomAccessFile, outputStream, 0, sourceFile.length(), progressMonitor, BUFF_SIZE);
    }

    assertThat(outputFile.length()).isEqualTo(sourceFile.length());
  }

  @Test
  public void testCopyFilesCopiesPartOfFile() throws IOException {
    File sourceFile = getTestFileFromResources("sample.pdf");
    File outputFile = Files.createTempFile(temporaryFolder, "zip4j_test", "").toFile();
    try (RandomAccessFile randomAccessFile = new RandomAccessFile(sourceFile, RandomAccessFileMode.READ.getValue());
         OutputStream outputStream = new FileOutputStream(outputFile)) {
      FileUtils.copyFile(randomAccessFile, outputStream, 500, 800, progressMonitor, BUFF_SIZE);
    }

    assertThat(outputFile.length()).isEqualTo(300);
  }

  @Test
  public void testGetAllSortedNumberedSplitFilesReturnsEmptyForNoFiles() throws IOException {
    File file = Files.createFile(temporaryFolder.resolve("somename")).toFile();
    assertThat(FileUtils.getAllSortedNumberedSplitFiles(file)).isEmpty();
  }

  @Test
  public void testGetAllSortedNumberedSplitFilesReturnsSortedList() throws IOException {
    File file001 = Files.createFile(temporaryFolder.resolve("somename.zip.001")).toFile();
    File file003 = Files.createFile(temporaryFolder.resolve("somename.zip.003")).toFile();
    File file002 = Files.createFile(temporaryFolder.resolve("somename.zip.002")).toFile();
    File file006 = Files.createFile(temporaryFolder.resolve("somename.zip.006")).toFile();
    File file005 = Files.createFile(temporaryFolder.resolve("somename.zip.005")).toFile();
    File file004 = Files.createFile(temporaryFolder.resolve("somename.zip.004")).toFile();

    File[] sortedList = FileUtils.getAllSortedNumberedSplitFiles(file001);

    assertThat(sortedList).containsExactly(file001, file002, file003, file004, file005, file006);
  }

  @Test
  public void testIsSymbolicLinkReturnsFalseWhenNotALink() throws IOException {
    File targetFile = Files.createFile(temporaryFolder.resolve("target.file")).toFile();
    assertThat(FileUtils.isSymbolicLink(targetFile)).isFalse();
  }

  @Test
  public void testIsSymbolicLinkReturnsTrueForALink() throws IOException {
    Path targetFile = Files.createFile(temporaryFolder.resolve("target.file"));
    Path linkFile = Paths.get(temporaryFolder.toString(), "source.link");
    Files.createSymbolicLink(linkFile, targetFile);

    assertThat(FileUtils.isSymbolicLink(linkFile.toFile()));
  }

  @Test
  public void testGetFilesInDirectoryRecursiveWithExcludeFileFilter() throws IOException {
    File rootFolder = getTestFileFromResources("");
    final List<File> filesToExclude = Arrays.asList(
            getTestFileFromResources("бореиская.txt"),
            getTestFileFromResources("sample_directory/favicon.ico")
    );
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setExcludeFileFilter(new ExcludeFileFilter() {
      @Override
      public boolean isExcluded(File o) {
        return filesToExclude.contains(o);
      }
    });
    List<File> allFiles = FileUtils.getFilesInDirectoryRecursive(rootFolder, zipParameters);

    assertThat(allFiles).hasSize(10);
    for (File file : allFiles) {
      assertThat(filesToExclude).doesNotContain(file);
    }
  }

  @Test
  public void testAssertFilesExistWhenFileExistsDoesNotThrowException() throws IOException {
    File newFile = Files.createFile(temporaryFolder.resolve("new-file")).toFile();
    FileUtils.assertFilesExist(Collections.singletonList(newFile), ZipParameters.SymbolicLinkAction.INCLUDE_LINK_ONLY);
  }

  @Test
  public void testAssertFilesExistWhenFileDoesNotExistThrowsException() throws IOException {
    File newFile = Paths.get(temporaryFolder.toString(), "file-which-does-not-exist").toFile();
    assertThrows(
            ZipException.class,
            () -> FileUtils.assertFilesExist(Collections.singletonList(newFile), ZipParameters.SymbolicLinkAction.INCLUDE_LINK_ONLY),
            "File does not exist: " + newFile
    );
  }

  @Test
  public void testAssertFilesExistForSymLinkWhenTargetDoesNotExistIncludeLinkOnlySuccess() throws IOException {
    File targetFile = Paths.get(temporaryFolder.toString(), "file-which-does-not-exist").toFile();
    File symlink = Paths.get(temporaryFolder.toString(), "symlink.link").toFile();
    Files.createSymbolicLink(symlink.toPath(), targetFile.toPath());

    FileUtils.assertFilesExist(Collections.singletonList(symlink), ZipParameters.SymbolicLinkAction.INCLUDE_LINK_ONLY);
  }

  @Test
  public void testAssertFilesExistForSymLinkWhenTargetDoesNotExistIncludeLinkedFileOnlyThrowsException() throws IOException {
    testAssertFileExistsForSymLinkWhenTargetDoesNotExist(ZipParameters.SymbolicLinkAction.INCLUDE_LINKED_FILE_ONLY);
  }

  @Test
  public void testAssertFilesExistForSymLinkWhenTargetDoesNotExistIncludeLinkAndLinkedFileThrowsException() throws IOException {
    testAssertFileExistsForSymLinkWhenTargetDoesNotExist(ZipParameters.SymbolicLinkAction.INCLUDE_LINK_AND_LINKED_FILE);
  }

  @Test
  public void testReadSymbolicLink() throws IOException {
    File targetFile = Files.createFile(temporaryFolder.resolve("target-file")).toFile();
    File symlink = Paths.get(temporaryFolder.toString(), "symlink.link").toFile();
    Files.createSymbolicLink(symlink.toPath(), targetFile.toPath());

    assertThat(FileUtils.readSymbolicLink(symlink)).isEqualTo(targetFile.getPath());
  }

  private void testAssertFileExistsForSymLinkWhenTargetDoesNotExist(ZipParameters.SymbolicLinkAction symbolicLinkAction) throws IOException {
    File targetFile = Paths.get(temporaryFolder.toString(), "file-which-does-not-exist").toFile();
    File symlink = Paths.get(temporaryFolder.toString(), "symlink.link").toFile();
    Files.createSymbolicLink(symlink.toPath(), targetFile.toPath());

    assertThrows(
            ZipException.class,
            () -> FileUtils.assertFilesExist(Collections.singletonList(symlink), symbolicLinkAction),
            "Symlink target '" + targetFile + "' does not exist for link '" + symlink + "'"
    );

  }

  private void testInvalidOffsetsScenario(int start, int offset) throws IOException {
    assertThrows(
            ZipException.class,
            () -> {
              File sourceFile = getTestFileFromResources("sample.pdf");
              try (RandomAccessFile randomAccessFile = new RandomAccessFile(sourceFile, RandomAccessFileMode.READ.getValue());
                   OutputStream outputStream = new FileOutputStream(Files.createTempFile(temporaryFolder, "zip4j_test", "").toFile())) {
                FileUtils.copyFile(randomAccessFile, outputStream, start, offset, progressMonitor, BUFF_SIZE);
              }
            },
            "invalid offsets"
    );

  }
}
