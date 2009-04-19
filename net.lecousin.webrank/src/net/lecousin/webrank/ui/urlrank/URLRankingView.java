package net.lecousin.webrank.ui.urlrank;

import net.lecousin.framework.Pair;
import net.lecousin.framework.event.Event.Listener;
import net.lecousin.framework.strings.StringPattern;
import net.lecousin.framework.thread.RunnableWithData;
import net.lecousin.framework.ui.eclipse.SharedImages;
import net.lecousin.framework.ui.eclipse.UIUtil;
import net.lecousin.framework.ui.eclipse.control.ImageAndTextButton;
import net.lecousin.framework.ui.eclipse.control.UIControlUtil;
import net.lecousin.framework.ui.eclipse.control.list.LCContentProvider;
import net.lecousin.framework.ui.eclipse.control.list.LCTable;
import net.lecousin.framework.ui.eclipse.control.list.LCTable.ColumnProvider;
import net.lecousin.framework.ui.eclipse.control.list.LCTable.ColumnProviderText;
import net.lecousin.framework.ui.eclipse.control.list.LCTable.TableConfig;
import net.lecousin.framework.ui.eclipse.dialog.FlatPopupMenu;
import net.lecousin.framework.ui.eclipse.dialog.MyDialog;
import net.lecousin.framework.ui.eclipse.dialog.MyDialog.Orientation;
import net.lecousin.web.search.SearchEngineManager;
import net.lecousin.web.search.query.SearchWebQuery;
import net.lecousin.webrank.core.Project;
import net.lecousin.webrank.core.RankResult;
import net.lecousin.webrank.core.Search;
import net.lecousin.webrank.ui.dialog.PatternDialog;
import net.lecousin.webrank.ui.dialog.SearchWebQueryDialog;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.part.ViewPart;

public class URLRankingView extends ViewPart {

	public static final String ID = "net.lecousin.webrank.ui.urlrank";
	
	public URLRankingView() {
	}

	@Override
	public void createPartControl(Composite parent) {
		SashForm sash = new SashForm(parent, SWT.VERTICAL);
		createParametersControls(sash);
		createResultsControls(sash);
		sash.setWeights(new int[] { 25, 75 });
	}
	
	private Composite urlsPanel;
	private Composite paramsPanel;
	private ImageAndTextButton addPatternButton;
	
	private LCTable<Search> table;
	
	private void createParametersControls(Composite parent) {
		ScrolledComposite scroll = new ScrolledComposite(parent, SWT.V_SCROLL);
		scroll.setExpandHorizontal(true);
		paramsPanel = UIUtil.newGridComposite(scroll, 2, 2, 2);
		scroll.setContent(paramsPanel);
		UIUtil.newLabel(paramsPanel, "Search Engines:");
		Composite row = UIUtil.newRowComposite(paramsPanel, SWT.HORIZONTAL, 0, 0, 5, true);
		for (String id : SearchEngineManager.getEngineIDs())
			UIUtil.newCheck(row, SearchEngineManager.getEngineName(id), new Listener<Pair<Boolean,String>>() {
				public void fire(Pair<Boolean, String> event) {
					if (event.getValue1())
						Project.getInstance().addEngine(event.getValue2());
					else
						Project.getInstance().removeEngine(event.getValue2());
				}
			}, id).setSelection(Project.getInstance().hasEngine(id));
		UIUtil.newLabel(paramsPanel, "URLs:").setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
		urlsPanel = UIUtil.newGridComposite(paramsPanel, 0, 0, 2, 0, 0);
		addPatternButton = UIUtil.newImageTextButton(urlsPanel, SharedImages.getImage(SharedImages.icons.x16.basic.ADD), "Add URL pattern...", new Listener<Object>() {
			public void fire(Object event) {
				PatternDialog dlg = new PatternDialog(MyDialog.getPlatformShell());
				StringPattern p = dlg.open();
				if (p != null) {
					Project.getInstance().addPattern(p);
					refreshURLs();
				}
			}
		}, null);
		addPatternButton.setLayoutData(UIUtil.gridDataHoriz(2, false));
		refreshURLs();
		Composite buttonsPanel = UIUtil.newRowComposite(paramsPanel, SWT.HORIZONTAL, 0, 0, 5, true);
		buttonsPanel.setLayoutData(UIUtil.gridDataHoriz(2, false));
		UIUtil.newImageTextButton(buttonsPanel, SharedImages.getImage(SharedImages.icons.x16.basic.ADD), "New search...", new Listener<Object>() {
			public void fire(Object event) {
				SearchWebQueryDialog dlg = new SearchWebQueryDialog(MyDialog.getPlatformShell());
				SearchWebQuery q = dlg.open();
				if (q != null) {
					Project.getInstance().addQuery(q);
					table.refresh(true);
				}
			}
		}, null);
		UIUtil.newImageTextButton(buttonsPanel, SharedImages.getImage(SharedImages.icons.x16.basic.REFRESH), "Refresh Web Ranking...", new Listener<Object>() {
			public void fire(Object event) {
				Project.getInstance().refresh();
				table.refresh(true);
			}
		}, null);
		UIControlUtil.resize(paramsPanel);
	}
	private void refreshURLs() {
		for (Control c : urlsPanel.getChildren())
			if (c != addPatternButton)
				c.dispose();
		for (StringPattern p : Project.getInstance().getPatterns()) {
			UIUtil.newLabel(urlsPanel, p.getString()).moveAbove(addPatternButton);
			UIUtil.newImageButton(urlsPanel, SharedImages.getImage(SharedImages.icons.x16.basic.DEL), new Listener<StringPattern>() {
				public void fire(StringPattern event) {
					Project.getInstance().removePattern(event);
					refreshURLs();
				}
			}, p).moveAbove(addPatternButton);
		}
		urlsPanel.layout(true, true);
		UIControlUtil.resize(urlsPanel);
		UIControlUtil.resize(paramsPanel);
	}
	
	@SuppressWarnings("unchecked")
	private void createResultsControls(Composite parent) {
		TableConfig cfg = new TableConfig();
		cfg.fixedRowHeight = 18;
		cfg.multiSelection = false;
		cfg.sortable = true;
		table = new LCTable<Search>(parent, new LCContentProvider<Search>() {
			public Iterable<Search> getElements() { return Project.getInstance().getSearches(); }
		}, (ColumnProvider<Search>[])new ColumnProvider[] {
				new ColumnProviderText<Search>() {
					public String getTitle() { return "Search"; }
					public int getDefaultWidth() { return 300; }
					public int getAlignment() { return SWT.LEFT; }
					public String getText(Search element) { return element.getQuery().toString(); }
					public Font getFont(Search element) { return null; }
					public Image getImage(Search element) { return null; }
					public int compare(Search element1, String text1, Search element2, String text2) {
						return text1.compareTo(text2);
					}
				},
		}, cfg);
		for (String id : Project.getInstance().getEngines())
			table.addColumn(new EngineColumn(id));
		table.addRightClickListener(new Listener<Search>() {
			public void fire(Search event) {
				FlatPopupMenu menu = new FlatPopupMenu(null, event.getQuery().toString(), false, true, false, false);
				new FlatPopupMenu.Menu(menu, "Delete", SharedImages.getImage(SharedImages.icons.x16.basic.DEL), false, false, new RunnableWithData<Search>(event) {
					public void run() {
						if (MessageDialog.openConfirm(MyDialog.getPlatformShell(), "Delete search", "Are you sure you want to remove search '" + data().getQuery().toString() + "' and all related information ?")) {
							Project.getInstance().removeSearch(data());
							table.refresh(true);
						}
					}
				});
				menu.show(null, Orientation.BOTTOM, true);
			}
		});
	}
	
	private static class EngineColumn implements ColumnProviderText<Search> {
		public EngineColumn(String id) {
			this.id = id;
		}
		private String id;
		public String getTitle() { return SearchEngineManager.getEngineName(id); }
		public int getDefaultWidth() { return 75; }
		public int getAlignment() { return SWT.CENTER; }
		public String getText(Search element) { 
			RankResult r = element.getLatestResult();
			if (r == null) return "No data";
			Integer rank = r.getRank(id);
			if (rank == null) return "Not ranked";
			return rank.toString();
		}
		public Font getFont(Search element) { return null; }
		public Image getImage(Search element) { return null; }
		public int compare(Search element1, String text1, Search element2, String text2) {
			return text1.compareTo(text2);
		}
	}

	@Override
	public void setFocus() {
	}

}
