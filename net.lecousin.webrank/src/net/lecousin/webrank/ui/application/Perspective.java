package net.lecousin.webrank.ui.application;

import net.lecousin.webrank.ui.urlrank.URLRankingView;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class Perspective implements IPerspectiveFactory {

	public void createInitialLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(false);
		
		IFolderLayout folder = layout.createFolder("folder", IPageLayout.TOP, 0.5f, editorArea);
		folder.addPlaceholder("*");
		folder.addView(URLRankingView.ID);
		
		layout.getViewLayout(URLRankingView.ID).setCloseable(false);
	}
}
