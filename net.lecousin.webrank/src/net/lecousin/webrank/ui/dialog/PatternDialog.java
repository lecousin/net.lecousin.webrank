package net.lecousin.webrank.ui.dialog;

import net.lecousin.framework.strings.StringPattern;
import net.lecousin.framework.ui.eclipse.UIUtil;
import net.lecousin.framework.ui.eclipse.control.buttonbar.OkCancelButtonsPanel;
import net.lecousin.framework.ui.eclipse.dialog.FlatDialog;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class PatternDialog extends FlatDialog {

	public PatternDialog(Shell shell) {
		super(shell, "Pattern", false, false);
		setMinWidth(400);
	}

	@Override
	protected void createContent(Composite container) {
		UIUtil.gridLayout(container, 2);
		UIUtil.newLabel(container, "Pattern:");
		text = new Text(container, SWT.BORDER);
		text.setLayoutData(UIUtil.gridDataHoriz(1, true));
		new OkCancelButtonsPanel(container, true) {
			@Override
			protected boolean handleOk() {
				pattern = text.getText();
				if (pattern.length() == 0) return false;
				ok = true;
				return true;
			}
			@Override
			protected boolean handleCancel() {
				ok = false;
				return true;
			}
		}.centerAndFillInGrid();
	}
	
	private Text text;
	private boolean ok = false;
	private String pattern;

	public StringPattern open() {
		super.open(true);
		if (!ok) return null;
		return new StringPattern(pattern);
	}
}
