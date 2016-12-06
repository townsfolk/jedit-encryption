package mochi;

import org.gjt.sp.jedit.AbstractOptionPane;

import javax.swing.*;

/**
 * Created by elberry on 11/15/16.
 */
public class EncryptionPluginOptions extends AbstractOptionPane {

	public EncryptionPluginOptions() {
		super("encryption");
	}

	@Override
	protected void _init() {
		add(new JLabel("Poomp"));
	}

	@Override
	protected void _save() {
	}
}