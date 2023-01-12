package io.github.palexdev.zip4j.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class FileUtilsTestLinuxAndMac {

	private static final String ACTUAL_OS = System.getProperty("os.name");

	@BeforeAll
	public static void setup() {
		System.setProperty("os.name", "linux");
	}

	@AfterAll
	public static void cleanup() {
		System.setProperty("os.name", ACTUAL_OS);
	}

	@Test
	public void testSetFileAttributesWhenAttributesIsNullDoesNothing() {
		FileUtils.setFileAttributes(mock(Path.class), null);
	}

	@Test
	public void testSetFileAttributesWhenAttributesIsEmptyDoesNothing() {
		FileUtils.setFileAttributes(mock(Path.class), new byte[0]);
	}

	@Test
	public void testSetFileAttributesWhenNoAttributesSetDoesNothing() throws IOException {
		Path path = mock(Path.class);
		PosixFileAttributeView posixFileAttributeView = mockPosixFileAttributeView(path, false);
		FileUtils.setFileAttributes(path, new byte[4]);
		Mockito.verifyNoInteractions(posixFileAttributeView);
	}

	@Test
	public void testSetFileAttributesWhenAttributesSet() throws IOException {
		Path path = mock(Path.class);
		PosixFileAttributeView posixFileAttributeView = mockPosixFileAttributeView(path, false);

		byte fourthByte = 1; // Only first bit is set
		byte thirdByte = -1; // All bits are set

		FileUtils.setFileAttributes(path, new byte[]{0, 0, thirdByte, fourthByte});

		ArgumentCaptor<Set> permissionsCaptor = ArgumentCaptor.forClass(Set.class);
		verify(posixFileAttributeView).setPermissions(permissionsCaptor.capture());
		verifyAllPermissionSet(permissionsCaptor.getValue());
	}

	@Test
	public void testGetFileAttributesWhenFileIsNullReturnsEmptyBytes() {
		byte[] attributes = FileUtils.getFileAttributes(null);
		assertThat(attributes).contains(0, 0, 0, 0);
	}

	@Test
	public void testGetFileAttributesAsDefinedForRegularFile() throws IOException {
		testGetFileAttributesGetsAsDefined(false);
	}

	@Test
	public void testGetFileAttributesAsDefinedForDirectory() throws IOException {
		testGetFileAttributesGetsAsDefined(true);
	}

	@Test
	public void testIsWindowsReturnsFalse() {
		assertThat(FileUtils.isWindows()).isFalse();
	}

	private void testGetFileAttributesGetsAsDefined(boolean isDirectory) throws IOException {
		File file = mock(File.class);
		Path path = mock(Path.class);
		when(file.toPath()).thenReturn(path);
		when(file.exists()).thenReturn(true);
		PosixFileAttributeView posixFileAttributeView = mockPosixFileAttributeView(path, isDirectory);
		PosixFileAttributes posixFileAttributes = mock(PosixFileAttributes.class);
		Set<PosixFilePermission> posixFilePermissions = getAllPermissions();
		when(posixFileAttributes.permissions()).thenReturn(posixFilePermissions);
		when(posixFileAttributeView.readAttributes()).thenReturn(posixFileAttributes);

		byte[] fileAttributes = FileUtils.getFileAttributes(file);

		assertThat(fileAttributes).hasSize(4);
		assertThat(fileAttributes[0]).isEqualTo((byte) 0);
		assertThat(fileAttributes[1]).isEqualTo((byte) 0);
		assertThat(fileAttributes[2]).isEqualTo((byte) -1);

		if (isDirectory) {
			assertThat(fileAttributes[3]).isEqualTo((byte) 65);
		} else {
			assertThat(fileAttributes[3]).isEqualTo((byte) -127);
		}
	}

	private Set<PosixFilePermission> getAllPermissions() {
		Set<PosixFilePermission> permissions = new HashSet<>();
		permissions.add(PosixFilePermission.OWNER_READ);
		permissions.add(PosixFilePermission.OWNER_WRITE);
		permissions.add(PosixFilePermission.OWNER_EXECUTE);
		permissions.add(PosixFilePermission.GROUP_READ);
		permissions.add(PosixFilePermission.GROUP_WRITE);
		permissions.add(PosixFilePermission.GROUP_EXECUTE);
		permissions.add(PosixFilePermission.OTHERS_READ);
		permissions.add(PosixFilePermission.OTHERS_WRITE);
		permissions.add(PosixFilePermission.OTHERS_EXECUTE);
		return permissions;
	}

	private void verifyAllPermissionSet(Set<PosixFilePermission> permissions) {
		assertThat(permissions).contains(
				PosixFilePermission.OWNER_READ,
				PosixFilePermission.OWNER_WRITE,
				PosixFilePermission.OWNER_EXECUTE,
				PosixFilePermission.GROUP_READ,
				PosixFilePermission.GROUP_WRITE,
				PosixFilePermission.GROUP_EXECUTE,
				PosixFilePermission.OTHERS_READ,
				PosixFilePermission.OTHERS_WRITE,
				PosixFilePermission.OTHERS_EXECUTE
		);
	}

	private PosixFileAttributeView mockPosixFileAttributeView(Path path, boolean isDirectory) throws IOException {
		FileSystemProvider fileSystemProvider = mock(FileSystemProvider.class);
		FileSystem fileSystem = mock(FileSystem.class);
		PosixFileAttributeView posixFileAttributeView = mock(PosixFileAttributeView.class);

		when(path.getFileSystem()).thenReturn(fileSystem);
		when(fileSystemProvider.getFileAttributeView(path, PosixFileAttributeView.class)).thenReturn(posixFileAttributeView);
		when(fileSystemProvider.getFileAttributeView(path, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS))
				.thenReturn(posixFileAttributeView);
		when(path.getFileSystem().provider()).thenReturn(fileSystemProvider);

		mockRegularFileOrDirectory(fileSystemProvider, path, isDirectory);

		return posixFileAttributeView;
	}

	private void mockRegularFileOrDirectory(FileSystemProvider fileSystemProvider, Path path, boolean isDirectory)
			throws IOException {
		BasicFileAttributes basicFileAttributes = mock(BasicFileAttributes.class);
		when(basicFileAttributes.isRegularFile()).thenReturn(!isDirectory);
		when(basicFileAttributes.isDirectory()).thenReturn(isDirectory);
		when(fileSystemProvider.readAttributes(path, BasicFileAttributes.class)).thenReturn(basicFileAttributes);
		when(fileSystemProvider.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS))
				.thenReturn(basicFileAttributes);
	}
}
