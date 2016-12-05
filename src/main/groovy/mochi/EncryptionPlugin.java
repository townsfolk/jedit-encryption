package mochi;

import org.gjt.sp.jedit.EBMessage;
import org.gjt.sp.jedit.EBPlugin;
import org.gjt.sp.jedit.msg.BufferUpdate;
import org.gjt.sp.util.Log;
import org.jasypt.encryption.pbe.StandardPBEByteEncryptor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Created by elberry on 11/15/16.
 */
public class EncryptionPlugin extends EBPlugin {

	public static final String JASYPT = "jasypt";
	public static final EncryptionPlugin instance = new EncryptionPlugin();

	public void decryptFile(String path) {
		Log.log(Log.DEBUG, this, "Decrypting file: " + path);
	}

	public void encryptFile(String path) {
		Log.log(Log.DEBUG, this, "Encrypting file: " + path);
		StandardPBEByteEncryptor encryptor = new StandardPBEByteEncryptor();
		encryptor.setPassword(JASYPT);
		try {
			Path encryptedPath = Paths.get(path + ".jenc");
			byte[] encryptedData = encryptor.encrypt(Files.readAllBytes(Paths.get(path)));
			Files.write(encryptedPath, encryptedData, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
			Log.log(Log.DEBUG, this, "Encrypted file: " + encryptedPath);
		} catch (Exception exception) {
			Log.log(Log.ERROR, this, "Could not encrypt file: " + Paths.get(path), exception);
		}
	}

	@Override
	public void handleMessage(EBMessage message) {
		Log.log(Log.DEBUG, this, "handling message: " + message);
		if (message instanceof BufferUpdate) {
			BufferUpdate updateMessage = (BufferUpdate) message;
			if (BufferUpdate.CLOSED == updateMessage.getWhat()) {
				// TODO: if (buffer was decrypted buffer) then delete buffer's file on file system
			}
			if (BufferUpdate.LOADED == updateMessage.getWhat()) {
				// TODO: if (buffer.path is encrypted) then: close buffer, create new decrypted file, decrypt content, save to decrypted file, finally open new buffer with decrypted path
			}
		}
		// listen for buffer.loaded - check if encrypted, then decrypt to new file
		// listen for buffer.saving - may not be needed
		// listen for buffer.closed
	}
}