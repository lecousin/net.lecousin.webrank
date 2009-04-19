package net.lecousin.webrank.ui.dialog;

import java.util.LinkedList;
import java.util.List;

import net.lecousin.framework.strings.StringUtil;
import net.lecousin.framework.ui.eclipse.UIUtil;
import net.lecousin.framework.ui.eclipse.control.LCCombo;
import net.lecousin.framework.ui.eclipse.control.buttonbar.OkCancelButtonsPanel;
import net.lecousin.framework.ui.eclipse.dialog.FlatDialog;
import net.lecousin.web.search.query.SearchQuery;
import net.lecousin.web.search.query.SearchWebQuery;
import net.lecousin.web.search.query.SearchQuery.Language;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class SearchWebQueryDialog extends FlatDialog {

	public SearchWebQueryDialog(Shell shell) {
		super(shell, "Pattern", false, false);
		setMinWidth(400);
	}

	@Override
	protected void createContent(Composite container) {
		UIUtil.gridLayout(container, 2);
		UIUtil.newLabel(container, "Language:");
		comboLang = new LCCombo(container, null);
		for (Language lang : SearchQuery.Language.values())
			comboLang.addItem(null, lang.toString(), lang);
		comboLang.setSelection("ENGLISH");
		comboLang.setEditable(false);
		UIUtil.newLabel(container, "All words:");
		textAll = new Text(container, SWT.BORDER);
		textAll.setLayoutData(UIUtil.gridDataHoriz(1, true));
		UIUtil.newLabel(container, "One word among:");
		textOne = new Text(container, SWT.BORDER);
		textOne.setLayoutData(UIUtil.gridDataHoriz(1, true));
		UIUtil.newLabel(container, "None of words:");
		textNone = new Text(container, SWT.BORDER);
		textNone.setLayoutData(UIUtil.gridDataHoriz(1, true));
		new OkCancelButtonsPanel(container, true) {
			@Override
			protected boolean handleOk() {
				query = new SearchWebQuery();
				query.lang = (Language)comboLang.getSelectionData();
				query.all = parse(textAll.getText());
				query.one = parse(textOne.getText());
				query.none = parse(textNone.getText());
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
	private String[] parse(String s) {
		List<String> list = new LinkedList<String>();
		StringBuilder str = new StringBuilder();
		boolean quote = false;
		for (int i = 0; i < s.length(); ++i) {
			char c = s.charAt(i);
			if (c == '\"') {
				quote = !quote;
			} else if (!quote && StringUtil.isSpace(c)) {
				if (str.length() > 0)
					list.add(str.toString());
				str = new StringBuilder();
			} else
				str.append(c);
		}
		if (str.length() > 0)
			list.add(str.toString());
		return list.toArray(new String[list.size()]);
	}
	
	private LCCombo comboLang;
	private Text textAll, textOne, textNone;
	private boolean ok = false;
	private SearchWebQuery query;

	public SearchWebQuery open() {
		super.open(true);
		if (!ok) return null;
		return query;
	}
}
