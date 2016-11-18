package mochi;

import org.gjt.sp.jedit.gui.EnhancedDialog;

import java.awt.*;

/**
 * Created by elberry on 11/15/16.
 */
public class PasswordDialog extends EnhancedDialog {
	public PasswordDialog(Frame parent, String title, boolean modal) {
		super(parent, title, modal);
	}

	public PasswordDialog(Dialog parent, String title, boolean modal) {
		super(parent, title, modal);
	}

	@Override
	public void cancel() {

	}

	@Override
	public void ok() {

	}
}
