package net.lecousin.webrank.core;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import net.lecousin.framework.IDManager;
import net.lecousin.framework.log.Log;
import net.lecousin.framework.progress.WorkProgress;
import net.lecousin.framework.strings.StringPattern;
import net.lecousin.framework.ui.eclipse.dialog.MyDialog;
import net.lecousin.framework.ui.eclipse.progress.WorkProgressDialog;
import net.lecousin.framework.xml.XmlUtil;
import net.lecousin.framework.xml.XmlWriter;
import net.lecousin.web.search.SearchEngine;
import net.lecousin.web.search.SearchEngineException;
import net.lecousin.web.search.SearchEngineManager;
import net.lecousin.web.search.query.SearchWebQuery;
import net.lecousin.web.search.result.SearchWebResult;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.w3c.dom.Element;

public class Project {
	
	private Project() { load(); }
	private static Project instance = null;
	public static Project getInstance() { return instance != null ? instance : (instance = new Project()); }

	private IProject project;
	private List<StringPattern> patterns = new LinkedList<StringPattern>();
	private List<String> engines = new LinkedList<String>();
	private IDManager searchIDManager = new IDManager();
	private List<Search> searches = new LinkedList<Search>();
	
	public boolean hasEngine(String id) { return engines.contains(id); }
	public void addEngine(String id) { if (!hasEngine(id)) engines.add(id); }
	public void removeEngine(String id) { engines.remove(id); }
	public List<String> getEngines() { return engines; }
	
	public List<StringPattern> getPatterns() { return patterns; }
	public void addPattern(StringPattern p) { patterns.add(p); }
	public void removePattern(StringPattern p) { patterns.remove(p); }
	
	public void addQuery(SearchWebQuery q) {
		long id = searchIDManager.allocate();
		Search s = new Search(id, q);
		searches.add(s);
	}
	public List<Search> getSearches() { return searches; }
	public void removeSearch(Search s) {
		try {
			IFolder folder = project.getFolder("searches");
			IFile file = folder.getFile(Long.toString(s.getID()));
			file.delete(true, null);
		} catch (CoreException e) {
			if (Log.error(this))
				Log.error(this, "Unable to remove search file ID " + s.getID(), e);
		}
		searches.remove(s);
		searchIDManager.free(s.getID());
	}
	
	public void refresh() {
		WorkProgress progress = new WorkProgress("Retrieve searches ranking...", engines.size()*searches.size(), true);
		WorkProgressDialog dlg = new WorkProgressDialog(MyDialog.getPlatformShell(), progress);
		for (String id : engines) {
			if (progress.isCancelled()) break;
			SearchEngine engine = SearchEngineManager.getEngine(id);
			if (engine == null) { progress.progress(searches.size()); continue; }
			for (Search s : searches) {
				if (progress.isCancelled()) break;
				try { 
					List<SearchWebResult> list = engine.searchWeb(s.getQuery(), 0, 1000, progress, 1);
					int index = 1;
					boolean found = false;
					for (SearchWebResult r : list) {
						for (StringPattern p : patterns)
							if (p.matches(r.getURL())) {
								found = true;
								break;
							}
						if (found) break;
						index++;
					}
					s.setResult(id, found ? new Integer(index) : null);
				} catch (SearchEngineException e) {
					if (Log.error(this))
						Log.error(this, "Error when searching '" + s.getQuery().toString() + "' on '" + SearchEngineManager.getEngineName(engine) + "'", e);
				}
			}
		}
		dlg.close();
	}
	
	private void load() {
		project = ResourcesPlugin.getWorkspace().getRoot().getProject("webrank");
		try { project.open(null); }
		catch (CoreException e){}
		try { 
			if (!project.exists()) {
				project.create(null);
				project.open(null);
			}
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
			IFile file = project.getFile("params");
			if (file.exists())
				loadParams(file);
			IFolder folder = project.getFolder("searches");
			if (folder.exists()) {
				for (IResource r : folder.members()) {
					if (r instanceof IFile) {
						try {
							long id = Long.parseLong(r.getName());
							searchIDManager.allocate(id);
							Search s = loadSearch(id, (IFile)r);
							if (s != null)
								searches.add(s);
						} catch (NumberFormatException e) {
							if (Log.warning(this))
								Log.warning(this, "Unexpected file '" + r.getName() + "' in searches folder.");
						}
					}
				}
			}
		} catch (CoreException e) {
			if (Log.error(this))
				Log.error(this, "Unable to load project", e);
		}
	}
	private void loadParams(IFile file) {
		try {
			Element root = XmlUtil.loadFile(file.getContents());
			for (Element e : XmlUtil.get_childs_element(root, "pattern"))
				patterns.add(new StringPattern(XmlUtil.get_inner_text(e)));
			for (Element e : XmlUtil.get_childs_element(root, "engine"))
				engines.add(XmlUtil.get_inner_text(e));
		} catch (Throwable t) {
			if (Log.error(this))
				Log.error(this, "Unable to load parameters", t);
		}
	}
	private Search loadSearch(long id, IFile file) {
		try {
			Element root = XmlUtil.loadFile(file.getContents());
			return new Search(id, root);
		} catch (Throwable t) {
			if (Log.error(this))
				Log.error(this, "Unable to load results", t);
			return null;
		}
	}

	public void save() {
		XmlWriter xml;
		xml = new XmlWriter();
		saveParams(xml);
		try { xml.writeToFile(project.getFile("params").getLocation().toFile().getAbsolutePath()); }
		catch (IOException e) {
			if (Log.error(this))
				Log.error(this, "Unable to save parameters", e);
		}
		IFolder folder = project.getFolder("searches");
		try { if (!folder.exists()) folder.create(true, true, null); }
		catch (CoreException e) {
			if (Log.error(this))
				Log.error(this, "Unable to save searches", e);
			return;
		}
		for (Search s : searches) {
			IFile file = folder.getFile(Long.toString(s.getID()));
			xml = new XmlWriter();
			saveSearch(xml, s);
			try { xml.writeToFile(file.getLocation().toFile().getAbsolutePath()); }
			catch (IOException e) {
				if (Log.error(this))
					Log.error(this, "Unable to save search " + s.getID(), e);
			}
		}
	}
	private void saveParams(XmlWriter xml) {
		xml.openTag("webrank.parameters");
		for (StringPattern p : patterns)
			xml.openTag("pattern").addText(p.getString()).closeTag();
		for (String e : engines)
			xml.openTag("engine").addText(e).closeTag();
		xml.closeTag();
	}
	private void saveSearch(XmlWriter xml, Search s) {
		xml.openTag("webrank.search");
		s.save(xml);
		xml.closeTag();
	}
}
