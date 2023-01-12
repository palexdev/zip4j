package io.github.palexdev.zip4j;

import org.junit.jupiter.api.Test;

public class Testing extends AbstractIT {

	@Test
	void testDirs() {
		System.out.println(temporaryFolder);
		System.out.println(generatedZipFile.getPath());
		System.out.println(outputFolder.getPath());
	}
}
