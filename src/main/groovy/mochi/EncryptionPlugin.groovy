package mochi

import org.gjt.sp.jedit.Buffer
import org.gjt.sp.jedit.EBMessage
import org.gjt.sp.jedit.EBPlugin
import org.gjt.sp.jedit.jEdit
import org.gjt.sp.jedit.msg.BufferUpdate
import org.gjt.sp.util.Log
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor

/**
 * Created by elberry on 11/15/16.
 */
public class EncryptionPlugin extends EBPlugin {

	public static final String JASYPT = "jasypt"
	public static final EncryptionPlugin instance = new EncryptionPlugin()
	public static final String ENCRYPTED_EXTENSION = ".jenc"

	public File decryptFile(String path) {
		Log.log(Log.DEBUG, this, "Decrypting file: $path")
		StandardPBEStringEncryptor decryptor = new StandardPBEStringEncryptor()
		decryptor.password = JASYPT
		File encryptedFile = new File(path)
		try {
			File decryptedFile = new File(createDecryptedPath(path))
			String decryptedData = decryptor.decrypt(encryptedFile.text)
			if (!decryptedFile.exists()) {
				decryptedFile.createNewFile()
			}
			decryptedFile.text = decryptedData
			Log.log(Log.DEBUG, this, "Decrypted file: $decryptedFile")
			return decryptedFile
		} catch (Exception exception) {
			Log.log(Log.ERROR, this, "Could not decrypt file: $encryptedFile", exception)
		}
		return null
	}

	public File encryptFile(String path) {
		Log.log(Log.DEBUG, this, "Encrypting file: $path")
		StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor()
		encryptor.password = JASYPT
		File decryptedFile = new File(path)
		try {
			File encryptedFile = new File("${path}${ENCRYPTED_EXTENSION}")
			String encryptedData = encryptor.encrypt(decryptedFile.text)
			if (!encryptedFile.exists()) {
				encryptedFile.createNewFile()
			}
			encryptedFile.text = encryptedData
			Log.log(Log.DEBUG, this, "Encrypted file: $encryptedFile")
			return encryptedFile
		} catch (Exception exception) {
			Log.log(Log.ERROR, this, "Could not encrypt file: $decryptedFile", exception)
		}
		return null
	}

	@Override
	public void handleMessage(EBMessage message) {
		Log.log(Log.DEBUG, this, "handling message: ${message}")
		if (message instanceof BufferUpdate) {
			BufferUpdate updateMessage = (BufferUpdate) message
			Buffer buffer = updateMessage.buffer
			if (BufferUpdate.CLOSED == updateMessage.getWhat()) {
				// TODO: if (buffer was decrypted buffer) then delete buffer's file on file system
			}
			if (BufferUpdate.LOADED == updateMessage.getWhat() && isEncrypted(updateMessage.buffer.path)) {
				File decryptedFile = decryptFile(buffer.path)
				jEdit.closeBuffer(updateMessage.view, buffer)
				jEdit.openFile(updateMessage.view, decryptedFile.path)
				// TODO: if (buffer.path is encrypted) then: close buffer, create new decrypted file, decrypt content, save to decrypted file, finally open new buffer with decrypted path
			}
		}
		// listen for buffer.loaded - check if encrypted, then decrypt to new file
		// listen for buffer.saving - may not be needed
		// listen for buffer.closed - if file was decrypted, then encrypt contents, and delete decrypted file
	}

	public static EncryptionPluginOptions optionPane() {
		return new EncryptionPluginOptions()
	}

	private boolean isEncrypted(String path) {
		path.endsWith(ENCRYPTED_EXTENSION)
	}

	private String createDecryptedPath(String encryptedPath) {
		encryptedPath.replace(ENCRYPTED_EXTENSION, "")
	}
}