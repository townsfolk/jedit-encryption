package mochi

import org.gjt.sp.jedit.Buffer
import org.gjt.sp.jedit.EBMessage
import org.gjt.sp.jedit.EBPlugin
import org.gjt.sp.jedit.jEdit
import org.gjt.sp.jedit.msg.BufferUpdate
import org.gjt.sp.util.Log
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor

import javax.swing.*
import java.awt.*
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

/**
 * Created by elberry on 11/15/16.
 */
public class EncryptionPlugin extends EBPlugin {

	public static final String JASYPT = "jasypt"
	public static final EncryptionPlugin instance = new EncryptionPlugin()
	public static final String ENCRYPTED_EXTENSION = ".jenc"
	private Set<String> sessionFiles = []

	private char[] promptForPassword(String title) {

		JPanel panel = new JPanel(new GridLayout(0, 1))
		panel.add(new JLabel("Enter Password:"))
		JPasswordField pf = new JPasswordField()
		panel.add(pf)

		JOptionPane op = new JOptionPane(panel, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION)

		JDialog dialog = op.createDialog(title)
		dialog.addWindowFocusListener(new WindowAdapter() {
			@Override
			public void windowGainedFocus(WindowEvent e) {
				pf.requestFocusInWindow()
			}
		})

		dialog.setVisible(true)

		if (JOptionPane.OK_OPTION == op.value) {
			Log.log(Log.DEBUG, this, "password: " + String.valueOf(pf.password))
			return pf.password
		}
		Log.log(Log.DEBUG, this, "password: " + String.valueOf(pf.password))
		return null
	}

	public File decryptFile(String path) {
		Log.log(Log.DEBUG, this, "Decrypting file: $path")
		char[] password = promptForPassword("Decrypting File")
		if (password) {
			StandardPBEStringEncryptor decryptor = new StandardPBEStringEncryptor()
			decryptor.passwordCharArray = password
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
		}
		return null
	}

	public File encryptFile(String path) {
		Log.log(Log.DEBUG, this, "Encrypting file: $path")
		char[] password = promptForPassword("Encrypting File")
		if (password) {
			StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor()
			encryptor.passwordCharArray = password
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
		}
		return null
	}

	@Override
	public void handleMessage(EBMessage message) {
		Log.log(Log.DEBUG, this, "handling message: ${message}")
		if (message instanceof BufferUpdate) {
			BufferUpdate updateMessage = (BufferUpdate) message
			Buffer buffer = updateMessage.buffer
			// if (buffer was decrypted buffer) then decrypt current content, and delete decrypted file on file system
			if (BufferUpdate.CLOSED == updateMessage.getWhat() && sessionFiles.contains(buffer.path)) {
				cleanupDecryptedFile(buffer)
			}
			// if (buffer is encrypted) then close buffer, decrypt content to decrypted file, and open decrypted path
			if (BufferUpdate.LOADED == updateMessage.getWhat() && isEncrypted(updateMessage.buffer.path)) {
				decryptAndOpen(updateMessage)
			}
		}
		// listen for buffer.loaded - check if encrypted, then decrypt to new file
		// listen for buffer.saving - may not be needed
		// listen for buffer.closed - if file was decrypted, then encrypt contents, and delete decrypted file
	}

	private void cleanupDecryptedFile(Buffer buffer) {
		String path = buffer.path
		if (encryptFile(path)) {
			try {
				if (!new File(path).delete()) {
					Log.log(Log.ERROR, this, "Unable to delete decrypted file: ${path}")
				} else {
					sessionFiles.remove(path)
				}
			} catch (Exception e) {
				Log.log(Log.ERROR, this, "Unable to delete decrypted file: ${path}", e)
			}
		} else {
			Log.log(Log.ERROR, this, "Unable to encrypt file: ${path}")
			// todo: alert user about error, reopen decrypted file?
		}
	}

	private void decryptAndOpen(BufferUpdate updateMessage) {
		Buffer buffer = updateMessage.buffer
		File decryptedFile = decryptFile(buffer.path)
		if (decryptedFile) {
			jEdit.closeBuffer(updateMessage.view, buffer)
			jEdit.openFile(updateMessage.view, decryptedFile.path)
			sessionFiles << decryptedFile.path
		} else {
			Log.log(Log.ERROR, this, "Unable to decrypt file: ${buffer.path}")
		}
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