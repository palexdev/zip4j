package io.github.palexdev.zip4j.headers;

import io.github.palexdev.zip4j.exception.ZipException;
import io.github.palexdev.zip4j.model.AESExtraDataRecord;
import io.github.palexdev.zip4j.model.FileHeader;
import io.github.palexdev.zip4j.model.LocalFileHeader;
import io.github.palexdev.zip4j.model.ZipParameters;
import io.github.palexdev.zip4j.model.enums.AesKeyStrength;
import io.github.palexdev.zip4j.model.enums.CompressionLevel;
import io.github.palexdev.zip4j.model.enums.CompressionMethod;
import io.github.palexdev.zip4j.model.enums.EncryptionMethod;
import io.github.palexdev.zip4j.util.FileUtils;
import io.github.palexdev.zip4j.util.InternalZipConstants;
import io.github.palexdev.zip4j.util.RawIO;
import io.github.palexdev.zip4j.util.Zip4jUtil;

import java.nio.charset.Charset;

import static io.github.palexdev.zip4j.util.BitUtils.setBit;
import static io.github.palexdev.zip4j.util.BitUtils.unsetBit;
import static io.github.palexdev.zip4j.util.FileUtils.isZipEntryDirectory;
import static io.github.palexdev.zip4j.util.ZipVersionUtils.determineVersionMadeBy;
import static io.github.palexdev.zip4j.util.ZipVersionUtils.determineVersionNeededToExtract;

public class FileHeaderFactory {

	public FileHeader generateFileHeader(ZipParameters zipParameters, boolean isSplitZip, int currentDiskNumberStart,
	                                     Charset charset, RawIO rawIO)
			throws ZipException {

		FileHeader fileHeader = new FileHeader();
		fileHeader.setSignature(HeaderSignature.CENTRAL_DIRECTORY);
		fileHeader.setVersionMadeBy(determineVersionMadeBy(zipParameters, rawIO));
		fileHeader.setVersionNeededToExtract(determineVersionNeededToExtract(zipParameters).getCode());

		if (zipParameters.isEncryptFiles() && zipParameters.getEncryptionMethod() == EncryptionMethod.AES) {
			fileHeader.setCompressionMethod(CompressionMethod.AES_INTERNAL_ONLY);
			fileHeader.setAesExtraDataRecord(generateAESExtraDataRecord(zipParameters));
			fileHeader.setExtraFieldLength(fileHeader.getExtraFieldLength() + InternalZipConstants.AES_EXTRA_DATA_RECORD_SIZE);
		} else {
			fileHeader.setCompressionMethod(zipParameters.getCompressionMethod());
		}

		if (zipParameters.isEncryptFiles()) {
			if (zipParameters.getEncryptionMethod() == null || zipParameters.getEncryptionMethod() == EncryptionMethod.NONE) {
				throw new ZipException("Encryption method has to be set when encryptFiles flag is set in zip parameters");
			}

			fileHeader.setEncrypted(true);
			fileHeader.setEncryptionMethod(zipParameters.getEncryptionMethod());
		}

		String fileName = validateAndGetFileName(zipParameters.getFileNameInZip());
		fileHeader.setFileName(fileName);
		fileHeader.setFileNameLength(determineFileNameLength(fileName, charset));
		fileHeader.setDiskNumberStart(isSplitZip ? currentDiskNumberStart : 0);
		fileHeader.setLastModifiedTime(Zip4jUtil.epochToExtendedDosTime(zipParameters.getLastModifiedFileTime()));

		boolean isDirectory = isZipEntryDirectory(fileName);
		fileHeader.setDirectory(isDirectory);
		fileHeader.setExternalFileAttributes(FileUtils.getDefaultFileAttributes(isDirectory));

		if (zipParameters.isWriteExtendedLocalFileHeader() && zipParameters.getEntrySize() == -1) {
			fileHeader.setUncompressedSize(0);
		} else {
			fileHeader.setUncompressedSize(zipParameters.getEntrySize());
		}

		if (zipParameters.isEncryptFiles() && zipParameters.getEncryptionMethod() == EncryptionMethod.ZIP_STANDARD) {
			fileHeader.setCrc(zipParameters.getEntryCRC());
		}

		fileHeader.setGeneralPurposeFlag(determineGeneralPurposeBitFlag(fileHeader.isEncrypted(), zipParameters, charset));
		fileHeader.setDataDescriptorExists(zipParameters.isWriteExtendedLocalFileHeader());
		fileHeader.setFileComment(zipParameters.getFileComment());
		return fileHeader;
	}

	public LocalFileHeader generateLocalFileHeader(FileHeader fileHeader) {
		LocalFileHeader localFileHeader = new LocalFileHeader();
		localFileHeader.setSignature(HeaderSignature.LOCAL_FILE_HEADER);
		localFileHeader.setVersionNeededToExtract(fileHeader.getVersionNeededToExtract());
		localFileHeader.setCompressionMethod(fileHeader.getCompressionMethod());
		localFileHeader.setLastModifiedTime(fileHeader.getLastModifiedTime());
		localFileHeader.setUncompressedSize(fileHeader.getUncompressedSize());
		localFileHeader.setFileNameLength(fileHeader.getFileNameLength());
		localFileHeader.setFileName(fileHeader.getFileName());
		localFileHeader.setEncrypted(fileHeader.isEncrypted());
		localFileHeader.setEncryptionMethod(fileHeader.getEncryptionMethod());
		localFileHeader.setAesExtraDataRecord(fileHeader.getAesExtraDataRecord());
		localFileHeader.setCrc(fileHeader.getCrc());
		localFileHeader.setCompressedSize(fileHeader.getCompressedSize());
		localFileHeader.setGeneralPurposeFlag(fileHeader.getGeneralPurposeFlag().clone());
		localFileHeader.setDataDescriptorExists(fileHeader.isDataDescriptorExists());
		localFileHeader.setExtraFieldLength(fileHeader.getExtraFieldLength());
		return localFileHeader;
	}

	private byte[] determineGeneralPurposeBitFlag(boolean isEncrypted, ZipParameters zipParameters, Charset charset) {
		byte[] generalPurposeBitFlag = new byte[2];
		generalPurposeBitFlag[0] = generateFirstGeneralPurposeByte(isEncrypted, zipParameters);
		if (charset == null || InternalZipConstants.CHARSET_UTF_8.equals(charset)) {
			// set 3rd bit which corresponds to utf-8 file name charset
			generalPurposeBitFlag[1] = setBit(generalPurposeBitFlag[1], 3);
		}
		return generalPurposeBitFlag;
	}

	private byte generateFirstGeneralPurposeByte(boolean isEncrypted, ZipParameters zipParameters) {

		byte firstByte = 0;

		if (isEncrypted) {
			firstByte = setBit(firstByte, 0);
		}

		if (CompressionMethod.DEFLATE.equals(zipParameters.getCompressionMethod())) {
			if (CompressionLevel.NORMAL.equals(zipParameters.getCompressionLevel())) {
				firstByte = unsetBit(firstByte, 1);
				firstByte = unsetBit(firstByte, 2);
			} else if (CompressionLevel.MAXIMUM.equals(zipParameters.getCompressionLevel())) {
				firstByte = setBit(firstByte, 1);
				firstByte = unsetBit(firstByte, 2);
			} else if (CompressionLevel.FAST.equals(zipParameters.getCompressionLevel())) {
				firstByte = unsetBit(firstByte, 1);
				firstByte = setBit(firstByte, 2);
			} else if (CompressionLevel.FASTEST.equals(zipParameters.getCompressionLevel())
					|| CompressionLevel.ULTRA.equals(zipParameters.getCompressionLevel())) {
				firstByte = setBit(firstByte, 1);
				firstByte = setBit(firstByte, 2);
			}
		}

		if (zipParameters.isWriteExtendedLocalFileHeader()) {
			firstByte = setBit(firstByte, 3);
		}

		return firstByte;
	}

	private String validateAndGetFileName(String fileNameInZip) throws ZipException {
		if (!Zip4jUtil.isStringNotNullAndNotEmpty(fileNameInZip)) {
			throw new ZipException("fileNameInZip is null or empty");
		}
		return fileNameInZip;
	}

	private AESExtraDataRecord generateAESExtraDataRecord(ZipParameters parameters) throws ZipException {
		AESExtraDataRecord aesExtraDataRecord = new AESExtraDataRecord();

		if (parameters.getAesVersion() != null) {
			aesExtraDataRecord.setAesVersion(parameters.getAesVersion());
		}

		if (parameters.getAesKeyStrength() == AesKeyStrength.KEY_STRENGTH_128) {
			aesExtraDataRecord.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_128);
		} else if (parameters.getAesKeyStrength() == AesKeyStrength.KEY_STRENGTH_192) {
			aesExtraDataRecord.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_192);
		} else if (parameters.getAesKeyStrength() == AesKeyStrength.KEY_STRENGTH_256) {
			aesExtraDataRecord.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);
		} else {
			throw new ZipException("invalid AES key strength");
		}

		aesExtraDataRecord.setCompressionMethod(parameters.getCompressionMethod());
		return aesExtraDataRecord;
	}

	private int determineFileNameLength(String fileName, Charset charset) {
		return HeaderUtil.getBytesFromString(fileName, charset).length;
	}
}
