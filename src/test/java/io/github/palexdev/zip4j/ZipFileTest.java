package io.github.palexdev.zip4j;

import io.github.palexdev.zip4j.exception.ZipException;
import io.github.palexdev.zip4j.model.FileHeader;
import io.github.palexdev.zip4j.model.ZipParameters;
import io.github.palexdev.zip4j.progress.ProgressMonitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collections;

import static io.github.palexdev.zip4j.util.InternalZipConstants.MIN_BUFF_SIZE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

// Tests only failure scenarios. All other tests are covered in the corresponding Integration test
public class ZipFileTest {

	private File sourceZipFile;
	private ZipFile zipFile;

	@BeforeEach
	public void setup() {
		sourceZipFile = mockFile(false);
		zipFile = new ZipFile(sourceZipFile);
	}

	@Test
	public void testZipFileConstructorThrowsIllegalArgumentExceptionWhenFileParameterIsNull() {
		assertThrows(IllegalArgumentException.class, () -> new ZipFile((File) null), "input zip file parameter is null");
	}

	@Test
	public void testZipFileConstructorWithPasswordThrowsIllegalArgumentExceptionWhenFileParameterIsNull() {
		assertThrows(IllegalArgumentException.class, () -> new ZipFile((File) null, "password".toCharArray()), "input zip file parameter is null");
	}

	@Test
	public void testCreateZipFileThrowsExceptionWhenZipFileExists() throws ZipException {
		reset(sourceZipFile);
		when(sourceZipFile.exists()).thenReturn(true);
		assertThrows(
				ZipException.class,
				() -> zipFile.createSplitZipFile(Collections.<File>emptyList(), new ZipParameters(), true, 10000),
				"zip file: " + sourceZipFile + " already exists. " +
						"To add files to existing zip file use addFile method"
		);
	}

	@Test
	public void testCreateZipFileThrowsExceptionWhenFileListIsNull() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> zipFile.createSplitZipFile(null, new ZipParameters(), true, 10000),
				"input file List is null, cannot create zip file"
		);
	}

	@Test
	public void testCreateZipFileThrowsExceptionWhenFileListIsEmpty() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> zipFile.createSplitZipFile(Collections.<File>emptyList(), new ZipParameters(), true, 10000),
				"input file List is null, cannot create zip file"
		);

	}

	@Test
	public void testCreateZipFileFromFolderThrowsExceptionWheFolderIsNull() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> zipFile.createSplitZipFileFromFolder(null, new ZipParameters(), true, 10000),
				"folderToAdd is null, cannot create zip file from folder"
		);

	}

	@Test
	public void testCreateZipFileFromFolderThrowsExceptionWhenParametersAreNull() throws ZipException {
		File folderToAdd = mockFile(true);
		assertThrows(
				ZipException.class,
				() -> zipFile.createSplitZipFileFromFolder(folderToAdd, null, true, 10000),
				"input parameters are null, cannot create zip file from folder"
		);
	}

	@Test
	public void testCreateZipFileFromFolderThrowsExceptionWhenZipFileExists() throws ZipException {
		reset(sourceZipFile);
		when(sourceZipFile.exists()).thenReturn(true);
		File folderToAdd = mockFile(true);

		assertThrows(
				ZipException.class,
				() -> zipFile.createSplitZipFileFromFolder(folderToAdd, new ZipParameters(), true, 10000),
				"zip file: " + sourceZipFile
						+ " already exists. To add files to existing zip file use addFolder method"
		);
	}

	@Test
	public void testAddFileAsStringThrowsExceptionWhenFileIsNull() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> zipFile.addFile((String) null),
				"file to add is null or empty"
		);

	}

	@Test
	public void testAddFileAsStringThrowsExceptionWhenFileIsEmpty() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> zipFile.addFile("   "),
				"file to add is null or empty"
		);

	}

	@Test
	public void testAddFileAsStringWithParametersThrowsExceptionWhenFileIsNull() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> zipFile.addFile((String) null, new ZipParameters()),
				"file to add is null or empty"
		);
	}

	@Test
	public void testAddFileAsStringWithParametersThrowsExceptionWhenFileIsEmpty() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> zipFile.addFile("", new ZipParameters()),
				"file to add is null or empty"
		);

	}

	@Test
	public void testAddFileAsFileThrowsExceptionWhenParametersIsNull() throws ZipException {
		File fileToAdd = mockFile(true);
		assertThrows(
				ZipException.class,
				() -> zipFile.addFile(fileToAdd, null),
				"input parameters are null"
		);

	}

	@Test
	public void testAddFileAsFileThrowsExceptionWhenProgressMonitorStateIsBusy() throws ZipException {
		File fileToAdd = mockFile(true);
		zipFile.setRunInThread(true);
		zipFile.getProgressMonitor().setState(ProgressMonitor.State.BUSY);
		assertThrows(
				ZipException.class,
				() -> zipFile.addFile(fileToAdd, new ZipParameters()),
				"invalid operation - Zip4j is in busy state"
		);

	}

	@Test
	public void testAddFilesThrowsExceptionWhenInputFilesIsNull() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> zipFile.addFiles(null),
				"input file List is null or empty"
		);

	}

	@Test
	public void testAddFilesThrowsExceptionWhenInputFilesIsEmpty() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> zipFile.addFiles(Collections.<File>emptyList()),
				"input file List is null or empty"
		);

	}

	@Test
	public void testAddFilesThrowsExceptionWhenProgressMonitorStateIsBusy() throws ZipException {
		zipFile.getProgressMonitor().setState(ProgressMonitor.State.BUSY);
		zipFile.setRunInThread(true);
		assertThrows(
				ZipException.class,
				() -> zipFile.addFiles(Collections.singletonList(new File("Some_File"))),
				"invalid operation - Zip4j is in busy state"
		);

	}

	@Test
	public void testAddFilesWithParametersThrowsExceptionWhenInputFilesIsNull() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> zipFile.addFiles(null, new ZipParameters()),
				"input file List is null or empty"
		);

	}

	@Test
	public void testAddFilesWithParametersThrowsExceptionWhenInputFilesIsEmpty() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> zipFile.addFiles(Collections.<File>emptyList(), new ZipParameters()),
				"input file List is null or empty"
		);
	}

	@Test
	public void testAddFilesWithParametersThrowsExceptionWhenParametersAreNull() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> zipFile.addFiles(Collections.singletonList(new File("Some_File")), null),
				"input parameters are null"
		);

	}


	@Test
	public void testAddFilesWithParametersThrowsExceptionWhenProgressMonitorStateIsBusy() throws ZipException {
		zipFile.setRunInThread(true);
		zipFile.getProgressMonitor().setState(ProgressMonitor.State.BUSY);
		assertThrows(
				ZipException.class,
				() -> zipFile.addFiles(Collections.singletonList(new File("Some_File")), new ZipParameters()),
				"invalid operation - Zip4j is in busy state"
		);

	}

	@Test
	public void testAddFolderThrowsExceptionWhenFolderIsNull() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> zipFile.addFolder(null),
				"input path is null, cannot add folder to zip file"
		);

	}

	@Test
	public void testAddFolderThrowsExceptionWhenFolderDoesNotExist() throws ZipException {
		File folderToAdd = mockFile(false);
		assertThrows(
				ZipException.class,
				() -> zipFile.addFolder(folderToAdd),
				"folder does not exist"
		);

	}

	@Test
	public void testAddFolderThrowsExceptionWhenFolderNotADirectory() throws ZipException {
		File folderToAdd = mockFile(true);
		when(folderToAdd.isDirectory()).thenReturn(false);
		assertThrows(
				ZipException.class,
				() -> zipFile.addFolder(folderToAdd),
				"input folder is not a directory"
		);


	}

	@Test
	public void testAddFolderThrowsExceptionWhenCannotReadFolder() throws ZipException {
		File folderToAdd = mockFile(true);
		when(folderToAdd.isDirectory()).thenReturn(true);
		when(folderToAdd.canRead()).thenReturn(false);
		assertThrows(
				ZipException.class,
				() -> zipFile.addFolder(folderToAdd),
				"cannot read input folder"
		);

	}

	@Test
	public void testAddFolderWithParametersThrowsExceptionWhenFolderIsNull() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> zipFile.addFolder(null, new ZipParameters()),
				"input path is null, cannot add folder to zip file"
		);

	}

	@Test
	public void testAddFolderWithParametersThrowsExceptionWhenFolderDoesNotExist() throws ZipException {
		File folderToAdd = mockFile(false);
		assertThrows(
				ZipException.class,
				() -> zipFile.addFolder(folderToAdd, new ZipParameters()),
				"folder does not exist"
		);
	}

	@Test
	public void testAddFolderWithParametersThrowsExceptionWhenFolderNotADirectory() throws ZipException {
		File folderToAdd = mockFile(true);
		when(folderToAdd.isDirectory()).thenReturn(false);
		assertThrows(
				ZipException.class,
				() -> zipFile.addFolder(folderToAdd, new ZipParameters()),
				"input folder is not a directory"
		);

	}

	@Test
	public void testAddFolderWithParametersThrowsExceptionWhenCannotReadFolder() throws ZipException {
		File folderToAdd = mockFile(true);
		when(folderToAdd.isDirectory()).thenReturn(true);
		when(folderToAdd.canRead()).thenReturn(false);
		assertThrows(
				ZipException.class,
				() -> zipFile.addFolder(folderToAdd, new ZipParameters()),
				"cannot read input folder"
		);

	}

	@Test
	public void testAddFolderWithParametersThrowsExceptionWhenInputParametersAreNull() throws ZipException {
		File folderToAdd = mockFile(true);
		when(folderToAdd.isDirectory()).thenReturn(true);
		when(folderToAdd.canRead()).thenReturn(true);
		assertThrows(
				ZipException.class,
				() -> zipFile.addFolder(folderToAdd, null),
				"input parameters are null, cannot add folder to zip file"
		);

	}

	@Test
	public void testAddFolderThrowsExceptionWhenProgressMonitorStateIsBusy() throws ZipException {
		File folderToAdd = mockFolder();
		zipFile.setRunInThread(true);
		zipFile.getProgressMonitor().setState(ProgressMonitor.State.BUSY);
		assertThrows(
				ZipException.class,
				() -> zipFile.addFolder(folderToAdd, new ZipParameters()),
				"invalid operation - Zip4j is in busy state"
		);

	}

	@Test
	public void testAddStreamThrowsExceptionWhenInputStreamIsNull() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> zipFile.addStream(null, new ZipParameters()),
				"inputstream is null, cannot add file to zip"
		);
	}

	@Test
	public void testAddStreamThrowsExceptionWhenParametersIsNull() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> zipFile.addStream(new ByteArrayInputStream(new byte[2]), null),
				"zip parameters are null"
		);

	}

	@Test
	public void testExtractAllThrowsExceptionWhenDestinationIsNull() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> zipFile.extractAll(null),
				"output path is null or invalid"
		);

	}

	@Test
	public void testExtractAllThrowsExceptionWhenDestinationIsEmpty() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> zipFile.extractAll(" "),
				"output path is null or invalid"
		);

	}

	@Test
	public void testExtractFileWithFileHeaderWhenFileHeaderIsNullThrowsException() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> zipFile.extractFile((FileHeader) null, "SOME_DESTINATION"),
				"input file header is null, cannot extract file"
		);

	}

	@Test
	public void testExtractFileWithFileHeaderWhenDestinationIsNullThrowsException() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> zipFile.extractFile(createFileHeader("SOME_NAME"), null),
				"destination path is empty or null, cannot extract file"
		);

	}

	@Test
	public void testExtractFileWithFileHeaderWhenDestinationIsEmptyThrowsException() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> zipFile.extractFile(createFileHeader("SOME_NAME"), ""),
				"destination path is empty or null, cannot extract file"
		);

	}

	@Test
	public void testExtractFileWithFileHeaderWhenBusyStateThrowsException() throws ZipException {
		zipFile.setRunInThread(true);
		zipFile.getProgressMonitor().setState(ProgressMonitor.State.BUSY);
		assertThrows(
				ZipException.class,
				() -> zipFile.extractFile(createFileHeader("SOME_NAME"), "SOME_DESTINATION"),
				"invalid operation - Zip4j is in busy state"
		);

	}

	@Test
	public void testExtractFileWithFileNameThrowsExceptionWhenNameIsNull() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> zipFile.extractFile((String) null, "SOME_DESTINATION"),
				"file to extract is null or empty, cannot extract file"
		);

	}

	@Test
	public void testExtractFileWithFileNameThrowsExceptionWhenNameIsEmpty() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> zipFile.extractFile("", "SOME_DESTINATION"),
				"file to extract is null or empty, cannot extract file"
		);

	}

	@Test
	public void testExtractFileWithNewFileNameThrowsExceptionWhenNameIsNull() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> zipFile.extractFile((String) null, "SOME_DESTINATION", "NEW_FILE_NAME"),
				"file to extract is null or empty, cannot extract file"
		);

	}

	@Test
	public void testExtractFileWithNewFileNameThrowsExceptionWhenNameIsEmpty() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> zipFile.extractFile("", "SOME_DESTINATION"),
				"file to extract is null or empty, cannot extract file"
		);

	}

	@Test
	public void testGetFileHeadersReturnsEmptyListWhenZipFileDoesNotExist() throws ZipException {
		File mockFile = mockFile(false);
		ZipFile zipFile = new ZipFile(mockFile);
		assertThat(zipFile.getFileHeaders()).isEmpty();
	}

	@Test
	public void testGetFileHeaderThrowsExceptionWhenFileNameIsNull() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> zipFile.getFileHeader(null),
				"input file name is emtpy or null, cannot get FileHeader"
		);

	}

	@Test
	public void testGetFileHeaderThrowsExceptionWhenFileNameIsEmpty() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> zipFile.getFileHeader(""),
				"input file name is emtpy or null, cannot get FileHeader"
		);

	}

	@Test
	public void testRemoveFileWithFileNameThrowsExceptionWhenFileNameIsNull() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> zipFile.removeFile((String) null),
				"file name is empty or null, cannot remove file"
		);

	}

	@Test
	public void testRemoveFileWithFileNameThrowsExceptionWhenFileNameIsEmpty() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> zipFile.removeFile(""),
				"file name is empty or null, cannot remove file"
		);

	}

	@Test
	public void testRemoveFileWithFileHeaderThrowsExceptionWhenFileNameIsNull() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> zipFile.removeFile((FileHeader) null),
				"input file header is null, cannot remove file"
		);


	}

	@Test
	public void testRemoveFilesWithListThrowsExceptionWhenListIsNull() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> zipFile.removeFiles(null),
				"fileNames list is null"
		);
	}

	@Test
	public void testRenameFileWithFileHeaderThrowsExceptionWhenHeaderIsNull() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> zipFile.renameFile((FileHeader) null, "somename"),
				"File header is null"
		);
	}

	@Test
	public void testRenameFileWithFileHeaderThrowsExceptionWhenNewFileNameIsNull() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> {
					FileHeader fileHeader = new FileHeader();
					fileHeader.setFileName("somename");
					zipFile.renameFile(fileHeader, null);
				},
				"newFileName is null or empty"
		);
	}

	@Test
	public void testRenameFileWithFileHeaderThrowsExceptionWhenNewFileNameIsEmpty() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> {
					FileHeader fileHeader = new FileHeader();
					fileHeader.setFileName("somename");
					zipFile.renameFile(fileHeader, "");
				},
				"newFileName is null or empty"
		);
	}

	@Test
	public void testRenameFileWithFileNameThrowsExceptionWhenFileNameToBeChangedIsNull() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> zipFile.renameFile((String) null, "somename"),
				"file name to be changed is null or empty"
		);
	}

	@Test
	public void testRenameFileWithFileNameThrowsExceptionWhenFileNameToBeChangedIsEmpty() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> zipFile.renameFile("", "somename"),
				"file name to be changed is null or empty"
		);
	}

	@Test
	public void testRenameFileWithFileNameThrowsExceptionWhenNewFileNameIsNull() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> zipFile.renameFile("Somename", null),
				"newFileName is null or empty"
		);
	}

	@Test
	public void testRenameFileWithFileNameThrowsExceptionWhenNewFileNameIsEmpty() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> zipFile.renameFile("Somename", "   "),
				"newFileName is null or empty"
		);

	}

	@Test
	public void testRenameFileWithMapThrowsExceptionWhenMapIsNull() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> zipFile.renameFiles(null),
				"fileNamesMap is null"
		);
	}

	@Test
	public void testMergeSplitFilesWhenOutputFileIsNullThrowsException() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> zipFile.mergeSplitFiles(null),
				"outputZipFile is null, cannot merge split files"
		);
	}

	@Test
	public void testMergeSplitFilesWhenOutputFileDoesAlreadyExistsThrowsException() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> {
					File mergedZipFile = mockFile(true);
					zipFile.mergeSplitFiles(mergedZipFile);
				},
				"output Zip File already exists"
		);
	}

	@Test
	public void testSetCommentWhenCommentIsNullThrowsException() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> zipFile.setComment(null),
				"input comment is null, cannot update zip file"
		);
	}

	@Test
	public void testSetCommentWhenZipFileDoesNotExistsThrowsException() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> zipFile.setComment("Some comment"),
				"zip file does not exist, cannot set comment for zip file"

		);
	}

	@Test
	public void testGetCommentWhenZipFileDoesNotExistThrowsException() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> zipFile.getComment(),
				"zip file does not exist, cannot read comment"
		);
	}

	@Test
	public void testGetInputStreamWhenFileHeaderIsNullThrowsException() throws IOException {
		assertThrows(
				ZipException.class,
				() -> zipFile.getInputStream(null),
				"FileHeader is null, cannot get InputStream"
		);
	}

	@Test
	public void testSetRunInThreadSetsFlag() {
		zipFile.setRunInThread(true);
		assertThat(zipFile.isRunInThread()).isTrue();

		zipFile.setRunInThread(false);
		assertThat(zipFile.isRunInThread()).isFalse();
	}

	@Test
	public void testGetFileReturnsValidFile() {
		assertThat(zipFile.getFile()).isSameAs(sourceZipFile);
	}

	@Test
	public void testToString() {
		assertThat(zipFile.toString()).isEqualTo("SOME_PATH");
	}

	@Test
	public void testSetBufferSizeThrowsExceptionWhenSizeLessThanExpected() {
		assertThrows(
				IllegalArgumentException.class,
				() -> zipFile.setBufferSize(MIN_BUFF_SIZE - 1),
				"Buffer size cannot be less than " + MIN_BUFF_SIZE + " bytes"
		);
	}

	private File mockFile(boolean fileExists) {
		File file = mock(File.class);
		when(file.exists()).thenReturn(fileExists);
		when(file.toString()).thenReturn("SOME_PATH");
		return file;
	}

	private File mockFolder() {
		File folder = mock(File.class);
		when(folder.exists()).thenReturn(true);
		when(folder.toString()).thenReturn("SOME_PATH");
		when(folder.isDirectory()).thenReturn(true);
		when(folder.canRead()).thenReturn(true);
		return folder;
	}

	private FileHeader createFileHeader(String fileName) {
		FileHeader fileHeader = new FileHeader();
		fileHeader.setFileName(fileName);
		return fileHeader;
	}
}
