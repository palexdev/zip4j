package io.github.palexdev.zip4j.util;

import io.github.palexdev.zip4j.exception.ZipException;
import io.github.palexdev.zip4j.model.ZipModel;
import io.github.palexdev.zip4j.model.ZipParameters;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.spi.FileSystemProvider;
import java.util.List;

import static io.github.palexdev.zip4j.util.InternalZipConstants.ZIP_FILE_SEPARATOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class FileUtilsTest {

	@Test
	public void testLastModifiedFileTimeWhenTimeIsLessThanZeroDoesNothing() {
		FileUtils.setFileLastModifiedTime(mock(Path.class), -1);
	}

	@Test
	public void testLastModifiedFileTimeWhenTimeIEqualToZeroDoesNothing() {
		FileUtils.setFileLastModifiedTime(mock(Path.class), 0);
	}

	@Test
	public void testLastModifiedFileTimeWhenFileDoesNotExistDoesNothing() throws IOException {
		Path path = mock(Path.class);
		FileSystemProvider fileSystemProvider = mockPath(path);
		doThrow(new IOException("Some exception")).when(fileSystemProvider).checkAccess(path);

		FileUtils.setFileLastModifiedTime(path, 1);
	}

	@Test
	public void testLastModifiedFileTimeForValidTimeSetsTime() throws IOException {
		Path path = mock(Path.class);
		FileSystemProvider fileSystemProvider = mockPath(path);
		BasicFileAttributeView basicFileAttributeView = mock(BasicFileAttributeView.class);
		when(fileSystemProvider.getFileAttributeView(path, BasicFileAttributeView.class))
				.thenReturn(basicFileAttributeView);

		long currentTime = System.currentTimeMillis();
		FileUtils.setFileLastModifiedTime(path, currentTime);

		verify(basicFileAttributeView).setTimes(FileTime.fromMillis(Zip4jUtil.dosToExtendedEpochTme(currentTime)), null, null);
	}

	@Test
	public void testLastModifiedFileTimeWhenIOExceptionDoesNothing() {
		Path path = mock(Path.class);
		FileSystemProvider fileSystemProvider = mockPath(path);
		when(fileSystemProvider.getFileAttributeView(path, BasicFileAttributeView.class))
				.thenThrow(new RuntimeException());

		long currentTime = System.currentTimeMillis();
		FileUtils.setFileLastModifiedTime(path, currentTime);
	}

	@Test
	public void testLastModifiedFileTimeWithoutNIOSetsSuccessfully() {
		File file = mock(File.class);
		long currentTime = System.currentTimeMillis();

		FileUtils.setFileLastModifiedTimeWithoutNio(file, currentTime);

		verify(file).setLastModified(Zip4jUtil.dosToExtendedEpochTme(currentTime));
	}

	@Test
	public void testGetFileAttributesReturnsEmptyBytesWhenNIONotSupported() {
		File file = mock(File.class);
		when(file.toPath()).thenThrow(new NoSuchMethodError("No method"));

		byte[] fileAttributes = FileUtils.getFileAttributes(file);
		assertThat(fileAttributes).hasSize(4);
		assertThat(fileAttributes).contains(0, 0, 0, 0);
	}

	@Test
	public void testGetFilesInDirectoryRecursiveThrowsExceptionWhenFileIsNull() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> FileUtils.getFilesInDirectoryRecursive(null, new ZipParameters()),
				"input path is null, cannot read files in the directory"
		);
	}

	@Test
	public void testGetFilesInDirectoryRecursiveReturnsEmptyWhenInputFileIsNotDirectory() throws IOException {
		File[] filesInDirectory = generateFilesForDirectory();
		testGetFilesInDirectory(false, true, filesInDirectory, 0, true, true);
	}

	@Test
	public void testGetFilesInDirectoryRecursiveReturnsEmptyWhenCannotReadInputFile() throws IOException {
		File[] filesInDirectory = generateFilesForDirectory();
		testGetFilesInDirectory(true, false, filesInDirectory, 0, true, true);
	}

	@Test
	public void testGetFilesInDirectoryRecursiveReturnsEmptyWhenFilesInDirIsNull() throws IOException {
		testGetFilesInDirectory(true, true, null, 0, true, true);
	}

	@Test
	public void testGetFileNameWithoutExtensionReturnsSameName() throws ZipException {
		String fileNameWithoutExtension = FileUtils.getFileNameWithoutExtension("somename");
		assertThat(fileNameWithoutExtension).isEqualTo("somename");
	}

	@Test
	public void testGetFileNameWithoutExtensionReturnsJustTheName() throws ZipException {
		String fileNameWithoutExtension = FileUtils.getFileNameWithoutExtension("somename.pdf");
		assertThat(fileNameWithoutExtension).isEqualTo("somename");
	}

	@Test
	public void testGetFileNameWithoutExtensionWithTwoExtensions() throws ZipException {
		String fileNameWithoutExtension = FileUtils.getFileNameWithoutExtension("somename.pdf.001");
		assertThat(fileNameWithoutExtension).isEqualTo("somename.pdf");
	}

	@Test
	public void testGetZipFileNameWithoutExtensionThrowsExceptionWhenNull() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> FileUtils.getZipFileNameWithoutExtension(null),
				"zip file name is empty or null, cannot determine zip file name"
		);

	}

	@Test
	public void testGetZipFileNameWithoutExtensionThrowsExceptionWhenEmpty() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> FileUtils.getZipFileNameWithoutExtension(""),
				"zip file name is empty or null, cannot determine zip file name"
		);
	}

	@Test
	public void testGetZipFileNameWithoutExtensionForWindowsFileSeparator() throws ZipException {
		final String ACTUAL_FILE_SEPARATOR = System.getProperty("file.separator");
		System.setProperty("file.separator", "\\");
		assertThat(FileUtils.getZipFileNameWithoutExtension("c:\\mydir\\somefile.zip")).isEqualTo("somefile");
		System.setProperty("file.separator", ACTUAL_FILE_SEPARATOR);
	}

	@Test
	public void testGetZipFileNameWithoutExtensionForUnixFileSeparator() throws ZipException {
		final String ACTUAL_FILE_SEPARATOR = System.getProperty("file.separator");
		System.setProperty("file.separator", "/");
		assertThat(FileUtils.getZipFileNameWithoutExtension("/usr/srikanth/somezip.zip")).isEqualTo("somezip");
		System.setProperty("file.separator", ACTUAL_FILE_SEPARATOR);
	}

	@Test
	public void testGetSplitZipFilesThrowsExceptionWhenZipModelIsNull() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> FileUtils.getSplitZipFiles(null),
				"cannot get split zip files: zipmodel is null"
		);

	}

	@Test
	public void testGetSplitZipFilesReturnsNullWhenEndOfCentralDirectoryIsNull() throws ZipException {
		ZipModel zipModel = new ZipModel();
		zipModel.setEndOfCentralDirectoryRecord(null);

		assertThat(FileUtils.getSplitZipFiles(zipModel)).isNull();
	}

	@Test
	public void testGetSplitZipFilesThrowsExceptionWhenZipFileDoesNotExits() throws ZipException {
		assertThrows(
				ZipException.class,
				() -> {
					ZipModel zipModel = new ZipModel();
					zipModel.setZipFile(new File("Does not exist"));
					FileUtils.getSplitZipFiles(zipModel);
				},
				"zip file does not exist"
		);
	}

	@Test
	public void testGetSplitZipFilesForNonSplitArchiveReturnsSameFile() throws ZipException {
		File zipFile = mockZipFileAsExists("somepath", "somefile");
		ZipModel zipModel = new ZipModel();
		zipModel.setSplitArchive(false);
		zipModel.setZipFile(zipFile);

		List<File> splitFiles = FileUtils.getSplitZipFiles(zipModel);

		assertThat(splitFiles).hasSize(1);
		assertThat(splitFiles.get(0)).isSameAs(zipFile);
	}

	@Test
	public void testGetSplitZipFilesWhenNumberOfDiskIsZeroReturnsSameFile() throws ZipException {
		File zipFile = mockZipFileAsExists("somepath", "somefile");
		ZipModel zipModel = new ZipModel();
		zipModel.setSplitArchive(false);
		zipModel.setZipFile(zipFile);
		zipModel.getEndOfCentralDirectoryRecord().setNumberOfThisDisk(0);

		List<File> splitFiles = FileUtils.getSplitZipFiles(zipModel);

		assertThat(splitFiles).hasSize(1);
		assertThat(splitFiles.get(0)).isSameAs(zipFile);
	}

	@Test
	public void testGetSplitZipFilesReturnsValidWhenSplitFile() throws ZipException {
		final String FILE_SEPARATOR = System.getProperty("file.separator");
		String path = FILE_SEPARATOR + "usr" + FILE_SEPARATOR + "parentdir" + FILE_SEPARATOR;
		String zipFileName = "SomeName";
		File zipFile = mockZipFileAsExists(path, zipFileName);
		ZipModel zipModel = new ZipModel();
		zipModel.setSplitArchive(true);
		zipModel.setZipFile(zipFile);
		zipModel.getEndOfCentralDirectoryRecord().setNumberOfThisDisk(4);

		List<File> splitFiles = FileUtils.getSplitZipFiles(zipModel);

		assertThat(splitFiles).hasSize(5);
		assertThat(splitFiles.get(0).getPath()).isEqualTo(path + zipFileName + ".z01");
		assertThat(splitFiles.get(1).getPath()).isEqualTo(path + zipFileName + ".z02");
		assertThat(splitFiles.get(2).getPath()).isEqualTo(path + zipFileName + ".z03");
		assertThat(splitFiles.get(3).getPath()).isEqualTo(path + zipFileName + ".z04");
		assertThat(splitFiles.get(4).getPath()).isEqualTo(path + zipFileName + ".zip");
	}

	@Test
	public void testGetRelativeFileNameWhenRootFoldersAreNull() throws ZipException {
		assertThat(FileUtils.getRelativeFileName(new File("somefile.txt"), new ZipParameters())).isEqualTo("somefile.txt");
	}

	@Test
	public void testGetRelativeFileWithRootFolderNameInZip() throws ZipException {
		String expectetdFileName = "rootfolder" + ZIP_FILE_SEPARATOR + "somefile.txt";
		ZipParameters zipParameters = new ZipParameters();
		zipParameters.setRootFolderNameInZip("rootfolder");
		assertThat(FileUtils.getRelativeFileName(new File("somefile.txt"), zipParameters)).isEqualTo(expectetdFileName);
	}

	@Test
	public void testGetRelativeFileWithRootFolderNameInZipWithFileSeparator() throws ZipException {
		String expectetdFileName = "rootfolder" + ZIP_FILE_SEPARATOR + "somefile.txt";
		ZipParameters zipParameters = new ZipParameters();
		zipParameters.setRootFolderNameInZip("rootfolder" + File.separator);
		assertThat(FileUtils.getRelativeFileName(new File("somefile.txt"), zipParameters)).isEqualTo(expectetdFileName);
	}

	@Test
	public void testGetRelativeFileWithRootFolderNameInZipWithSeparatorsInName() throws ZipException {
		String expectedRootFolder = "rootfolder" + ZIP_FILE_SEPARATOR + "somefile.txt";
		ZipParameters zipParameters = new ZipParameters();
		zipParameters.setRootFolderNameInZip("rootfolder\\");
		assertThat(FileUtils.getRelativeFileName(new File("somefile.txt"), zipParameters)).isEqualTo(expectedRootFolder);
	}

	@Test
	public void testGetRelativeFileWithDifferentFileNameInZip() throws ZipException {
		ZipParameters zipParameters = new ZipParameters();
		zipParameters.setFileNameInZip("anotherName.txt");
		assertThat(FileUtils.getRelativeFileName(new File("somefile.txt"), zipParameters)).isEqualTo("anotherName.txt");
	}

	@Test
	public void testGetRelativeFileWithDifferentFileNameInZipAndRootFolderName() throws ZipException {
		String expectedRootFolder = "rootfolder" + ZIP_FILE_SEPARATOR + "anotherName.txt";
		ZipParameters zipParameters = new ZipParameters();
		zipParameters.setFileNameInZip("anotherName.txt");
		zipParameters.setRootFolderNameInZip("rootfolder");
		assertThat(FileUtils.getRelativeFileName(new File("somefile.txt"), zipParameters)).isEqualTo(expectedRootFolder);
	}

	@Test
	public void testGetRelativeFileWithDifferentFileNameInZipAndRootFolderNameAndDefaultFolderPath() throws ZipException {
		String expectedRootFolder = "rootfolder" + ZIP_FILE_SEPARATOR + "anotherFolder" + ZIP_FILE_SEPARATOR + "anotherName.txt";
		ZipParameters zipParameters = new ZipParameters();
		zipParameters.setFileNameInZip("anotherName.txt");
		zipParameters.setDefaultFolderPath("defaultFolderPath");
		zipParameters.setRootFolderNameInZip("rootfolder");
		File fileToTest = new File("defaultFolderPath" + ZIP_FILE_SEPARATOR + "anotherFolder" + ZIP_FILE_SEPARATOR
				+ "somefile.txt");
		assertThat(FileUtils.getRelativeFileName(fileToTest, zipParameters)).isEqualTo(expectedRootFolder);
	}

	@Test
	public void testIsZipEntryDirectoryWithWindowsFileSeparatorReturnsTrue() {
		assertThat(FileUtils.isZipEntryDirectory("somename\\")).isTrue();
	}

	@Test
	public void testIsZipEntryDirectoryWithUnixFileSeparatorReturnsTrue() {
		assertThat(FileUtils.isZipEntryDirectory("somename/")).isTrue();
	}

	@Test
	public void testIsZipEntryDirectoryWhichIsNotDirectoryReturnsFalse() {
		assertThat(FileUtils.isZipEntryDirectory("somename")).isFalse();
	}

	@Test
	public void testIsNumberedSplitFileForInvalidReturnsFalse() {
		assertThat(FileUtils.isNumberedSplitFile(new File("somename.xyz"))).isFalse();
	}

	@Test
	public void testIsNumberedSplitFileForValidSpltiFileReturnsTrue() {
		assertThat(FileUtils.isNumberedSplitFile(new File("somename.xyz.001"))).isFalse();
	}

	@Test
	public void testGetFileExtensionWithoutExtensionReturnsEmptyString() {
		assertThat(FileUtils.getFileExtension(new File("somename"))).isEqualTo("");
	}

	@Test
	public void testGetFileExtensionReturnsExtension() {
		assertThat(FileUtils.getFileExtension(new File("somename.pdf"))).isEqualTo("pdf");
	}

	@Test
	public void testGetFileExtensionWithDoubleExtensionReturnsLastExtension() {
		assertThat(FileUtils.getFileExtension(new File("somename.pdf.001"))).isEqualTo("001");
	}

	@Test
	public void testGetNextNumberedSplitFileCounterAsExtensionSingleDigit() {
		assertThat(FileUtils.getNextNumberedSplitFileCounterAsExtension(8)).isEqualTo(".009");
	}

	@Test
	public void testGetNextNumberedSplitFileCounterAsExtensionSkipsToDoubleDigit() {
		assertThat(FileUtils.getNextNumberedSplitFileCounterAsExtension(9)).isEqualTo(".010");
	}

	@Test
	public void testGetNextNumberedSplitFileCounterAsExtensionDoubleDigit() {
		assertThat(FileUtils.getNextNumberedSplitFileCounterAsExtension(99)).isEqualTo(".100");
	}

	@Test
	public void testGetNextNumberedSplitFileCounterAsExtensionThreeDigit() {
		assertThat(FileUtils.getNextNumberedSplitFileCounterAsExtension(100)).isEqualTo(".101");
	}

	@Test
	public void testIsSymbolicLinkWhenNoSuchMethodErrorReturnsFalse() {
		File file = mock(File.class);
		when(file.toPath()).thenThrow(NoSuchMethodError.class);
		assertThat(FileUtils.isSymbolicLink(file)).isFalse();
	}

	private File mockZipFileAsExists(String path, String zipFileNameWithoutExtension) {
		File file = mock(File.class);
		when(file.exists()).thenReturn(true);
		when(file.getName()).thenReturn(zipFileNameWithoutExtension + ".zip");
		when(file.getPath()).thenReturn(path + zipFileNameWithoutExtension + ".zip");
		return file;
	}

	private void testGetFilesInDirectory(boolean isDirectory, boolean canRead, File[] filesInDirectory,
	                                     int expectedReturnSize, boolean shouldReadHiddenFiles,
	                                     boolean shouldReadHiddenFolders) throws IOException {
		File file = mock(File.class);
		when(file.isDirectory()).thenReturn(isDirectory);
		when(file.canRead()).thenReturn(canRead);
		when(file.listFiles()).thenReturn(filesInDirectory);

		ZipParameters zipParameters = new ZipParameters();
		zipParameters.setReadHiddenFiles(shouldReadHiddenFiles);
		zipParameters.setReadHiddenFolders(shouldReadHiddenFolders);
		List<File> returnedFiles = FileUtils.getFilesInDirectoryRecursive(file, zipParameters);

		assertThat(returnedFiles).hasSize(expectedReturnSize);
	}

	private File[] generateFilesForDirectory() throws IOException {
		return new File[]{
				mockFile(false, false),
				mockFile(false, false),
				mockFile(true, false),
				mockFile(true, true),
				mockFile(false, true),
				mockFile(true, true),
		};
	}

	private File mockFile(boolean isHidden, boolean isDirectory) throws IOException {
		File file = mock(File.class);
		Path path = mock(Path.class);
		when(file.toPath()).thenReturn(path);
		when(file.isHidden()).thenReturn(isHidden);
		when(file.isDirectory()).thenReturn(isDirectory);
		when(file.canRead()).thenReturn(true);
		when(file.getCanonicalFile()).thenReturn(file);
		return file;
	}

	private FileSystemProvider mockPath(Path path) {
		FileSystemProvider fileSystemProvider = mock(FileSystemProvider.class);
		FileSystem fileSystem = mock(FileSystem.class);

		when(path.getFileSystem()).thenReturn(fileSystem);
		when(path.getFileSystem().provider()).thenReturn(fileSystemProvider);
		return fileSystemProvider;
	}
}
